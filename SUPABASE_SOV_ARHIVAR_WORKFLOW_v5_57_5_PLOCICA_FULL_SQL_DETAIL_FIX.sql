-- SOV Arhivar v5.57.5 — PLOČICA + FULL SQL DETAIL FIX / split list vs detail
-- Problem v5.57.3: web lista je čitala puni worklist view koji je za svaki objekt radio raw::text,
-- regexe i string_agg nacrta/zapisnika. Na većoj bazi to može završiti s: canceling statement due to statement timeout.
-- Fix:
-- 1) sov_arhivar_worklist postaje LIGHT/Fast list view: bez velikog raw_text/full_details/report/drawing string_agg.
-- 2) puni tekst se vadi samo za jedan odabrani objekt preko RPC: sov_arhivar_get_object_detail(p_object_id).
-- 3) dashboard se računa iz light viewa.
-- RLS ostaje uključen. Ne briše speleo bazu, nacrte, zapisnike ni overrideove.
-- v5.57.5 dodatno: field_tasks tokeni tipa "plocica" se računaju kao falinka, a detail RPC vraća sva raw SQL polja.

create extension if not exists pgcrypto;

create or replace function public.sov_txt_norm(p text)
returns text
language sql
immutable
as $$
  select lower(translate(coalesce(p,''), 'ČĆŠŽĐčćšžđ', 'CCSZDccszd'))
$$;

create or replace function public.sov_json_first_text(p jsonb, variadic p_keys text[])
returns text
language plpgsql
immutable
as $$
declare
  k text;
  v text;
begin
  if p is null then
    return null;
  end if;
  foreach k in array p_keys loop
    if p ? k then
      v := nullif(trim(p->>k), '');
      if v is not null and lower(v) not in ('null','undefined','-','—') then
        return v;
      end if;
    end if;
  end loop;
  return null;
end;
$$;

create or replace function public.sov_json_pretty_lines(p jsonb)
returns text
language sql
stable
as $$
  select string_agg(format('%s: %s', e.key, e.value), E'
' order by e.key)
  from jsonb_each_text(
    case
      when p is null then '{}'::jsonb
      when jsonb_typeof(p) = 'object' then p
      else jsonb_build_object('raw', p)
    end
  ) as e(key, value)
  where nullif(trim(e.value), '') is not null
    and lower(trim(e.value)) not in ('null','undefined','');
$$;


-- Helpful indexes for the filtered/detail path. Safe if columns/tables exist.
create index if not exists idx_speleo_objects_staging_source_id on public.speleo_objects_staging(source_id);
create index if not exists idx_speleo_object_overrides_object_id on public.speleo_object_overrides(object_id);
create index if not exists idx_sov_archive_object_status_object_id on public.sov_archive_object_status(object_id);
create index if not exists idx_speleo_object_drawings_object_id on public.speleo_object_drawings(object_id);
create index if not exists idx_speleo_object_drawings_object_name on public.speleo_object_drawings(object_name);
create index if not exists idx_speleo_activity_reports_object_name on public.speleo_activity_reports(object_name);

alter table public.speleo_objects_staging add column if not exists in_cadastre_bool boolean;
alter table public.speleo_objects_staging add column if not exists plate_number text;
alter table public.speleo_objects_staging add column if not exists digital_survey_status text;
alter table public.speleo_objects_staging add column if not exists bibliography_status text;
alter table public.speleo_objects_staging add column if not exists gps_tracklog text;
alter table public.speleo_objects_staging add column if not exists georef_record text;

-- Drop dependent dashboard first.
drop view if exists public.sov_arhivar_dashboard cascade;
drop view if exists public.sov_arhivar_worklist cascade;

create view public.sov_arhivar_worklist as
with base as (
  select
    s.source_id as object_id,
    coalesce(nullif(o.data->>'name',''), s.name) as object_name,
    coalesce(nullif(o.data->>'plate_number',''), nullif(o.data->>'plocica',''), nullif(o.data->>'pločica',''), nullif(o.data->>'cadastral_number',''), nullif(s.plate_number,''), nullif(s.cadastral_number,''), public.sov_json_first_text(s.raw,'plate_number','plocica','pločica','broj_plocice','broj_pločice','broj plocice','broj pločice','broj_plocice_na_objektu','broj_pločice_na_objektu','cadastral_number','katastarski_broj','katastarski broj','kat_broj','kbr','oznaka','plocica_broj','pločica_broj')) as plate_number,
    coalesce(nullif(o.data->>'object_type',''), s.object_type_final) as object_type,
    coalesce(nullif(o.data->>'nearest_place',''), s.nearest_place, s.locality, s.municipality) as nearest_place,
    coalesce(case when (o.data->>'lat') ~ '^-?[0-9]+(\.[0-9]+)?$' then (o.data->>'lat')::double precision else null end, s.lat) as lat,
    coalesce(case when (o.data->>'lon') ~ '^-?[0-9]+(\.[0-9]+)?$' then (o.data->>'lon')::double precision else null end, s.lon) as lon,
    s.cadastre_status,
    s.record_status,
    s.field_tasks,
    s.workflow_raw,
    coalesce(s.digital_survey_status, s.raw->>'digital_survey_status') as digital_survey_status,
    coalesce(s.bibliography_status, s.raw->>'bibliography_status') as bibliography_status,
    coalesce(s.gps_tracklog, s.raw->>'gps_tracklog') as gps_tracklog,
    coalesce(s.georef_record, s.raw->>'georef_record') as georef_record,
    s.county,
    s.municipality,
    s.updated_at,

    -- FAST status text: only known status/task columns and specific raw keys, never raw::text for every row.
    public.sov_txt_norm(concat_ws(' ',
      s.cadastre_status,
      s.record_status,
      s.field_tasks,
      s.workflow_raw,
      s.digital_survey_status,
      s.bibliography_status,
      s.gps_tracklog,
      s.georef_record,
      s.raw->>'cadastre_status',
      s.raw->>'record_status',
      s.raw->>'field_tasks',
      s.raw->>'workflow_raw',
      s.raw->>'digital_survey_status',
      s.raw->>'bibliography_status',
      s.raw->>'gps_tracklog',
      s.raw->>'georef_record',
      s.raw->>'plocica',
      s.raw->>'pločica',
      s.raw->>'broj_plocice',
      s.raw->>'broj_pločice',
      s.raw->>'katastarski_broj',
      s.raw->>'oznaka',
      s.raw->>'note',
      s.raw->>'napomena'
    )) as status_text_norm,

    case
      when coalesce(s.in_cadastre_bool, false) = true then true
      when public.sov_txt_norm(coalesce(s.raw->>'in_cadastre_bool','')) in ('true','t','1','da','yes') then true
      when public.sov_txt_norm(coalesce(s.cadastre_status,'')) in ('u_katastru','objekt_unesen') then true
      when public.sov_txt_norm(coalesce(s.record_status,'')) in ('u_katastru','u_katastru_editirati','objekt_unesen') then true
      when public.sov_txt_norm(coalesce(s.workflow_raw,'')) like '%objekt u katastru%' then true
      else false
    end as base_in_cadastre,
    case
      when public.sov_txt_norm(coalesce(s.record_status,'')) = 'za_unos_u_katastar' then true
      when public.sov_txt_norm(coalesce(s.workflow_raw,'')) like '%ima podatke za unos u katastar%' then true
      else false
    end as base_ready_for_katastar
  from public.speleo_objects_staging s
  left join public.speleo_object_overrides o on o.object_id = s.source_id
), rules as (
  select
    b.*,
    concat_ws(E'\n',
      nullif('record_status: ' || coalesce(b.record_status,''), 'record_status: '),
      nullif('cadastre_status: ' || coalesce(b.cadastre_status,''), 'cadastre_status: '),
      nullif('field_tasks: ' || coalesce(b.field_tasks,''), 'field_tasks: '),
      nullif('workflow_raw: ' || coalesce(b.workflow_raw,''), 'workflow_raw: '),
      nullif('digital_survey_status: ' || coalesce(b.digital_survey_status,''), 'digital_survey_status: '),
      nullif('bibliography_status: ' || coalesce(b.bibliography_status,''), 'bibliography_status: '),
      nullif('gps_tracklog: ' || coalesce(b.gps_tracklog,''), 'gps_tracklog: '),
      nullif('georef_record: ' || coalesce(b.georef_record,''), 'georef_record: ')
    ) as source_missing_text,
    (
      public.sov_txt_norm(coalesce(b.field_tasks,'')) ~ '(^|[^a-z0-9])(koordinat|gps|wgs|htrs|lokacij|pozicij|georef)([^a-z0-9]|$)'
      or public.sov_txt_norm(coalesce(b.workflow_raw,'')) ~ '(^|[^a-z0-9])(koordinat|gps|wgs|htrs|lokacij|pozicij|georef)([^a-z0-9]|$)'
      or b.status_text_norm ~ '((fali|nedostaje|nema|bez|treba|trebaju|potrebno|ponoviti|srediti|dopuniti|prikupiti|pronaci|naci).{0,80}(koordinat|gps|wgs|htrs|lokacij|pozicij|georef))'
      or b.status_text_norm ~ '((koordinat|gps|wgs|htrs|lokacij|pozicij|georef).{0,80}(fali|nedostaje|nema|bez|treba|trebaju|potrebno|ponoviti|srediti|dopuniti|prikupiti|pronaci|naci))'
      or b.status_text_norm like '%nema tocne koordinate%'
      or b.status_text_norm like '%nemamo tocne koordinate%'
    ) as explicit_missing_coordinates,
    (
      public.sov_txt_norm(coalesce(b.field_tasks,'')) ~ '(^|[^a-z0-9])(nacrt|tlocrt|profil|poligon|crtez|crtanje|topodroid)([^a-z0-9]|$)'
      or public.sov_txt_norm(coalesce(b.workflow_raw,'')) ~ '(^|[^a-z0-9])(nacrt|tlocrt|profil|poligon|crtez|crtanje|topodroid)([^a-z0-9]|$)'
      or b.status_text_norm ~ '((fali|nedostaje|nema|bez|treba|trebaju|potrebno|ponoviti|srediti|digitalizirati|napraviti|nacrtati|dopuniti|prikupiti|pronaci|naci).{0,80}(nacrt|tlocrt|profil|poligon|crtez|crtanje|topodroid))'
      or b.status_text_norm ~ '((nacrt|tlocrt|profil|poligon|crtez|crtanje|topodroid).{0,80}(fali|nedostaje|nema|bez|treba|trebaju|potrebno|ponoviti|srediti|digitalizirati|napraviti|nacrtati|dopuniti|prikupiti|pronaci|naci))'
      or b.status_text_norm like '%nema tlocrta%'
      or b.status_text_norm like '%nema tlocrt%'
    ) as explicit_missing_drawing,
    (
      public.sov_txt_norm(coalesce(b.field_tasks,'')) ~ '(^|[^a-z0-9])(zapisnik|tzapisnik|izvjestaj|izvještaj|bibliograf)([^a-z0-9]|$)'
      or public.sov_txt_norm(coalesce(b.workflow_raw,'')) ~ '(^|[^a-z0-9])(zapisnik|tzapisnik|izvjestaj|izvještaj|bibliograf)([^a-z0-9]|$)'
      or b.status_text_norm ~ '((fali|nedostaje|nema|bez|treba|trebaju|potrebno|srediti|napraviti|dopuniti|prikupiti|pronaci|naci).{0,80}(zapisnik|tzapisnik|izvjestaj|izvještaj|bibliograf))'
      or b.status_text_norm ~ '((zapisnik|tzapisnik|izvjestaj|izvještaj|bibliograf).{0,80}(fali|nedostaje|nema|bez|treba|trebaju|potrebno|srediti|napraviti|dopuniti|prikupiti|pronaci|naci))'
      or b.status_text_norm like '%nema zapisnika%'
    ) as explicit_missing_record,
    (
      public.sov_txt_norm(coalesce(b.field_tasks,'')) ~ '(^|[^a-z0-9])(plocic|plocica|plocicu|plocice|broj plocice|oznaka)([^a-z0-9]|$)'
      or public.sov_txt_norm(coalesce(b.workflow_raw,'')) ~ '(^|[^a-z0-9])(plocic|plocica|plocicu|plocice|broj plocice|oznaka)([^a-z0-9]|$)'
      or b.status_text_norm ~ '((fali|nedostaje|nema|bez|treba|trebaju|potrebno|postaviti|dodati|upisati|nabaviti).{0,80}(plocic|plocica|plocicu|plocice|broj plocice|oznaka))'
      or b.status_text_norm ~ '((plocic|plocica|plocicu|plocice|broj plocice|oznaka).{0,80}(fali|nedostaje|nema|bez|treba|trebaju|potrebno|postaviti|dodati|upisati|nabaviti))'
      or (coalesce(b.plate_number,'') = '' and not b.base_in_cadastre and b.status_text_norm ~ '(plocic|plocica|plocicu|plocice|katastar|nepotpun|fali|nema|treba)')
    ) as explicit_missing_plate
  from base b
), enriched as (
  select
    r.*,
    st.archive_status,
    st.priority,
    st.last_note,
    coalesce(st.has_coordinates,
      case
        when r.base_in_cadastre or r.base_ready_for_katastar then true
        when r.explicit_missing_coordinates then false
        else (r.lat is not null and r.lon is not null)
      end
    ) as has_coordinates,
    coalesce(st.has_drawing,
      case
        when r.explicit_missing_drawing then false
        when r.base_in_cadastre or r.base_ready_for_katastar then true
        when public.sov_txt_norm(coalesce(r.digital_survey_status,'')) = 'da' then true
        else true
      end
    ) as has_drawing,
    coalesce(st.has_record,
      case
        when r.explicit_missing_record then false
        when r.base_in_cadastre or r.base_ready_for_katastar then true
        else true
      end
    ) as has_record
  from rules r
  left join public.sov_archive_object_status st on st.object_id = r.object_id
)
select
  object_id,
  object_name,
  plate_number,
  object_type,
  nearest_place,
  lat,
  lon,
  cadastre_status,
  record_status,
  field_tasks,
  workflow_raw,
  digital_survey_status,
  bibliography_status,
  county,
  municipality,
  archive_status,
  priority,
  last_note,
  has_coordinates,
  has_drawing,
  has_record,
  0::integer as archive_drawing_count,
  0::integer as archive_report_count,
  0::integer as drawing_count,
  0::integer as report_count,
  null::text as base_details_text,
  null::text as report_details_text,
  null::text as drawing_details_text,
  null::text as raw_text,
  null::text as full_details_text,
  null::timestamptz as last_archive_drawing_at,
  null::timestamptz as last_archive_report_at,
  base_in_cadastre,
  base_ready_for_katastar,
  explicit_missing_coordinates,
  explicit_missing_drawing,
  explicit_missing_record,
  explicit_missing_plate,
  source_missing_text,
  (not has_coordinates) as missing_coordinates,
  (not has_drawing) as missing_drawing,
  (not has_record) as missing_record,
  explicit_missing_plate as missing_plate,
  array_remove(array[
    case when explicit_missing_plate then 'pločica' end,
    case when not has_coordinates then 'koordinate' end,
    case when not has_drawing then 'nacrt' end,
    case when not has_record then 'zapisnik' end
  ], null)::text[] as missing_categories,
  array_to_string(array_remove(array[
    case when explicit_missing_plate then 'pločica' end,
    case when not has_coordinates then 'koordinate' end,
    case when not has_drawing then 'nacrt' end,
    case when not has_record then 'zapisnik' end
  ], null), ', ') as missing_categories_text,
  case
    when base_in_cadastre then 'u_katastru'
    when base_ready_for_katastar then 'spremno_za_katastar'
    when (not has_coordinates) or (not has_drawing) or (not has_record) then 'nepotpuno'
    when public.sov_txt_norm(coalesce(record_status,'')) like '%nije_u_katastru%' or public.sov_txt_norm(coalesce(cadastre_status,'')) like '%nije_u_katastru%' then 'nije_u_katastru_provjeriti'
    else 'provjeriti'
  end as katastar_readiness,
  (case when not has_coordinates then 30 else 0 end
   + case when not has_drawing then 25 else 0 end
   + case when not has_record then 25 else 0 end
   + case when explicit_missing_plate then 15 else 0 end
   + case when coalesce(priority,'normal')='high' then 20 else 0 end
   + case when base_ready_for_katastar then 10 else 0 end
  )::integer as priority_score,
  'speleo_baza_status_light'::text as completeness_source,
  lower(coalesce(object_name,'') || ' ' || coalesce(plate_number,'') || ' ' || coalesce(nearest_place,'') || ' ' || coalesce(object_type,'') || ' ' || coalesce(field_tasks,'') || ' ' || coalesce(workflow_raw,'') || ' ' || coalesce(source_missing_text,'')) as search_text,
  updated_at
from enriched;

create view public.sov_arhivar_dashboard as
select
  count(*)::integer as total_objects,
  count(*) filter (where missing_coordinates)::integer as missing_coordinates,
  count(*) filter (where missing_drawing)::integer as missing_drawings,
  count(*) filter (where missing_record)::integer as missing_records,
  count(*) filter (where missing_plate)::integer as missing_plates,
  count(*) filter (where katastar_readiness = 'spremno_za_katastar')::integer as ready_for_katastar,
  count(*) filter (where katastar_readiness = 'u_katastru')::integer as in_katastar,
  count(*) filter (where katastar_readiness = 'nije_u_katastru_provjeriti')::integer as not_in_katastar_review,
  count(*) filter (where katastar_readiness in ('nepotpuno','provjeriti','nije_u_katastru_provjeriti') or missing_plate)::integer as incomplete_objects,
  max(updated_at) as last_object_update
from public.sov_arhivar_worklist;

-- Full detail is intentionally one-object-at-a-time.
create or replace function public.sov_arhivar_get_object_detail(p_object_id text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v jsonb;
begin
  with base as (
    select
      s.source_id as object_id,
      coalesce(nullif(o.data->>'name',''), s.name) as object_name,
      coalesce(nullif(o.data->>'plate_number',''), nullif(o.data->>'plocica',''), nullif(o.data->>'pločica',''), nullif(o.data->>'cadastral_number',''), nullif(s.plate_number,''), nullif(s.cadastral_number,''), public.sov_json_first_text(s.raw,'plate_number','plocica','pločica','broj_plocice','broj_pločice','broj plocice','broj pločice','broj_plocice_na_objektu','broj_pločice_na_objektu','cadastral_number','katastarski_broj','katastarski broj','kat_broj','kbr','oznaka','plocica_broj','pločica_broj')) as plate_number,
      coalesce(nullif(o.data->>'object_type',''), s.object_type_final) as object_type,
      coalesce(nullif(o.data->>'nearest_place',''), s.nearest_place, s.locality, s.municipality) as nearest_place,
      coalesce(case when (o.data->>'lat') ~ '^-?[0-9]+(\.[0-9]+)?$' then (o.data->>'lat')::double precision else null end, s.lat) as lat,
      coalesce(case when (o.data->>'lon') ~ '^-?[0-9]+(\.[0-9]+)?$' then (o.data->>'lon')::double precision else null end, s.lon) as lon,
      s.cadastre_status,
      s.record_status,
      s.field_tasks,
      s.workflow_raw,
      coalesce(s.digital_survey_status, s.raw->>'digital_survey_status') as digital_survey_status,
      coalesce(s.bibliography_status, s.raw->>'bibliography_status') as bibliography_status,
      coalesce(s.gps_tracklog, s.raw->>'gps_tracklog') as gps_tracklog,
      coalesce(s.georef_record, s.raw->>'georef_record') as georef_record,
      s.county,
      s.municipality,
      s.raw,
      concat_ws(E'\n',
        nullif('Naziv: ' || coalesce(coalesce(nullif(o.data->>'name',''), s.name),''), 'Naziv: '),
        nullif('Tip: ' || coalesce(coalesce(nullif(o.data->>'object_type',''), s.object_type_final),''), 'Tip: '),
        nullif('Pločica: ' || coalesce(coalesce(nullif(o.data->>'plate_number',''), nullif(o.data->>'plocica',''), nullif(o.data->>'pločica',''), nullif(o.data->>'cadastral_number',''), nullif(s.plate_number,''), nullif(s.cadastral_number,''), public.sov_json_first_text(s.raw,'plate_number','plocica','pločica','broj_plocice','broj_pločice','broj plocice','broj pločice','broj_plocice_na_objektu','broj_pločice_na_objektu','cadastral_number','katastarski_broj','katastarski broj','kat_broj','kbr','oznaka','plocica_broj','pločica_broj')),''), 'Pločica: '),
        nullif('Najbliže mjesto: ' || coalesce(coalesce(nullif(o.data->>'nearest_place',''), s.nearest_place, s.locality, s.municipality),''), 'Najbliže mjesto: '),
        nullif('Županija/regija: ' || coalesce(s.county,''), 'Županija/regija: '),
        nullif('Općina: ' || coalesce(s.municipality,''), 'Općina: '),
        nullif('Katastarski status: ' || coalesce(s.cadastre_status,''), 'Katastarski status: '),
        nullif('Status zapisa: ' || coalesce(s.record_status,''), 'Status zapisa: '),
        nullif('Zadaci / što fali: ' || coalesce(s.field_tasks,''), 'Zadaci / što fali: '),
        nullif('Workflow: ' || coalesce(s.workflow_raw,''), 'Workflow: '),
        nullif('Digitalni nacrt: ' || coalesce(coalesce(s.digital_survey_status, s.raw->>'digital_survey_status'),''), 'Digitalni nacrt: '),
        nullif('Bibliografija/zapisnik: ' || coalesce(coalesce(s.bibliography_status, s.raw->>'bibliography_status'),''), 'Bibliografija/zapisnik: '),
        nullif('GPS tracklog: ' || coalesce(coalesce(s.gps_tracklog, s.raw->>'gps_tracklog'),''), 'GPS tracklog: '),
        nullif('Georef zapis: ' || coalesce(coalesce(s.georef_record, s.raw->>'georef_record'),''), 'Georef zapis: '),
        nullif('Opis: ' || coalesce(public.sov_json_first_text(s.raw,'technical_description','opis_objekta','opis objekta','tehnicki_opis','tehnički_opis','description','opis','Opis','description_hr','morphology','morfologija'), ''), 'Opis: '),
        nullif('Pristup: ' || coalesce(public.sov_json_first_text(s.raw,'access_description','pristup','opis_pristupa','access','approach','lokacija','location_description'), ''), 'Pristup: '),
        nullif('Istraživanje / povijest: ' || coalesce(public.sov_json_first_text(s.raw,'research','istrazivanje','istraživanje','exploration','history','povijest','opis_istrazivanja','opis istraživanja'), ''), 'Istraživanje / povijest: '),
        nullif('Autori / ekipa: ' || coalesce(public.sov_json_first_text(s.raw,'authors','autori','author','autor','members','clanovi','članovi','ekipa','tko','team'), ''), 'Autori / ekipa: '),
        nullif('Hidrologija: ' || coalesce(public.sov_json_first_text(s.raw,'hydrology','hidrologija','hydrogeology','hidrogeologija'), ''), 'Hidrologija: '),
        nullif('Geologija / morfologija: ' || coalesce(public.sov_json_first_text(s.raw,'geology','geologija','morphology','morfologija'), ''), 'Geologija / morfologija: '),
        nullif('Opasnosti / zaštita: ' || coalesce(public.sov_json_first_text(s.raw,'hazards','opasnosti','observed_threats','ugroze','protection','zastita','zaštita'), ''), 'Opasnosti / zaštita: '),
        nullif('Napomena: ' || coalesce(public.sov_json_first_text(s.raw,'note','napomena','Napomena','remarks','komentar'), ''), 'Napomena: '),
        nullif('Sva raw polja iz SQL-a:' || E'\n' || coalesce(public.sov_json_pretty_lines(s.raw),''), 'Sva raw polja iz SQL-a:' || E'\n')
      ) as base_details_text,
      jsonb_pretty(coalesce(s.raw, '{}'::jsonb)) as raw_text,
      lower(regexp_replace(coalesce(nullif(o.data->>'name',''), s.name,''),'\s+','_','g')) as name_key
    from public.speleo_objects_staging s
    left join public.speleo_object_overrides o on o.object_id = s.source_id
    where s.source_id = p_object_id
    limit 1
  ), drawings as (
    select
      count(*)::integer as archive_drawing_count,
      max(d.updated_at) as last_archive_drawing_at,
      string_agg(
        concat_ws(E'\n',
          nullif('Nacrt: ' || coalesce(d.drawing_title, d.object_name), 'Nacrt: '),
          nullif('Tip: ' || coalesce(d.drawing_type,''), 'Tip: '),
          nullif('Autor: ' || coalesce(d.author_name,''), 'Autor: '),
          case when d.survey_year is not null then 'Godina: ' || d.survey_year::text else null end,
          nullif('URL: ' || coalesce(d.drive_url, d.preview_url, ''), 'URL: '),
          nullif('Napomena: ' || coalesce(d.note,''), 'Napomena: ')
        ),
        E'\n\n---\n\n' order by d.updated_at desc nulls last
      ) as drawing_details_text
    from public.speleo_object_drawings d, base b
    where coalesce(nullif(d.object_id,''), lower(regexp_replace(coalesce(d.object_name,''),'\s+','_','g'))) = b.object_id
       or lower(regexp_replace(coalesce(d.object_name,''),'\s+','_','g')) = b.name_key
  ), reports as (
    select
      count(*)::integer as archive_report_count,
      max(r.created_at) as last_archive_report_at,
      string_agg(
        concat_ws(E'\n',
          nullif('Zapisnik/zahvat: ' || coalesce(r.object_name,''), 'Zapisnik/zahvat: '),
          case when r.date_start is not null or r.date_end is not null then 'Datum: ' || concat_ws(' – ', r.date_start::text, r.date_end::text) else null end,
          nullif('Svrha: ' || coalesce(r.purpose,''), 'Svrha: '),
          nullif('Opis: ' || coalesce(r.activity_description,''), 'Opis: '),
          nullif('Članovi: ' || coalesce(r.members,''), 'Članovi: '),
          nullif('Napomena: ' || coalesce(r.note,''), 'Napomena: ')
        ),
        E'\n\n---\n\n' order by r.date_start desc nulls last, r.created_at desc nulls last
      ) as report_details_text
    from public.speleo_activity_reports r, base b
    where lower(regexp_replace(coalesce(r.object_name,''),'\s+','_','g')) = b.name_key
  )
  select jsonb_build_object(
    'object_id', b.object_id,
    'object_name', b.object_name,
    'plate_number', b.plate_number,
    'object_type', b.object_type,
    'nearest_place', b.nearest_place,
    'lat', b.lat,
    'lon', b.lon,
    'cadastre_status', b.cadastre_status,
    'record_status', b.record_status,
    'field_tasks', b.field_tasks,
    'workflow_raw', b.workflow_raw,
    'digital_survey_status', b.digital_survey_status,
    'bibliography_status', b.bibliography_status,
    'county', b.county,
    'municipality', b.municipality,
    'base_details_text', b.base_details_text,
    'raw_text', b.raw_text,
    'archive_drawing_count', coalesce(d.archive_drawing_count,0),
    'archive_report_count', coalesce(r.archive_report_count,0),
    'drawing_count', coalesce(d.archive_drawing_count,0),
    'report_count', coalesce(r.archive_report_count,0),
    'last_archive_drawing_at', d.last_archive_drawing_at,
    'last_archive_report_at', r.last_archive_report_at,
    'drawing_details_text', d.drawing_details_text,
    'report_details_text', r.report_details_text,
    'full_details_text', concat_ws(E'\n\n', b.base_details_text, nullif('--- ZAPISNICI / ISTRAŽIVANJA ---' || E'\n' || coalesce(r.report_details_text,''), '--- ZAPISNICI / ISTRAŽIVANJA ---' || E'\n'), nullif('--- NACRTI ---' || E'\n' || coalesce(d.drawing_details_text,''), '--- NACRTI ---' || E'\n'))
  ) into v
  from base b
  left join drawings d on true
  left join reports r on true;

  return coalesce(v, '{}'::jsonb);
end;
$$;

grant execute on function public.sov_arhivar_get_object_detail(text) to authenticated;

-- Verification / smoke checks.
select 'sov_arhivar_dashboard' as check_name, to_jsonb(d.*) as result
from public.sov_arhivar_dashboard d;
