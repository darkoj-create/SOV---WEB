-- SOV Arhivar v5.57.2 — FULL OBJECT DETAILS FIX
-- Problem v5.57.1: klik na objekt prikazuje samo checklist, ne puni tekst iz speleo baze.
-- Fix: worklist sada nosi puni opis/status/raw podatke, izvještaje i nacrte za detail panel.
-- Fix: status za katastar se primarno računa iz postojeće speleo baze:
--   record_status, cadastre_status, field_tasks, workflow_raw, lat/lon i raw JSON.
-- Uploadani nacrti/zapisnici u SOV arhivi ostaju samo kao privitci/brojila, ne kao glavni dokaz.
-- RLS ostaje uključen. Ne briše raw speleo bazu ni archive upload tablice.

create extension if not exists pgcrypto;

-- Minimal helper: hrvatski znakovi + lowercase, bez unaccent extensiona.
create or replace function public.sov_txt_norm(p text)
returns text
language sql
immutable
as $$
  select lower(translate(coalesce(p,''), 'ČĆŠŽĐčćšžđ', 'CCSZDccszd'))
$$;

-- Dodaj korisne compatibility kolone ako staging postoji bez njih. Ne puni ništa nasilno.
alter table public.speleo_objects_staging add column if not exists in_cadastre_bool boolean;
alter table public.speleo_objects_staging add column if not exists plate_number text;
alter table public.speleo_objects_staging add column if not exists digital_survey_status text;
alter table public.speleo_objects_staging add column if not exists bibliography_status text;
alter table public.speleo_objects_staging add column if not exists gps_tracklog text;
alter table public.speleo_objects_staging add column if not exists georef_record text;

-- Glavni fix: worklist više NE koristi drawing_count/report_count kao dokaz da nešto postoji/fali.
drop view if exists public.sov_arhivar_dashboard cascade;
drop view if exists public.sov_arhivar_worklist cascade;

create view public.sov_arhivar_worklist as
with drawings as (
  select
    coalesce(nullif(object_id,''), lower(regexp_replace(coalesce(object_name,''),'\s+','_','g'))) as object_key,
    count(*)::integer as archive_drawing_count,
    max(updated_at) as last_archive_drawing_at,
    string_agg(
      concat_ws(E'\n',
        nullif('Nacrt: ' || coalesce(drawing_title, object_name), 'Nacrt: '),
        nullif('Tip: ' || coalesce(drawing_type,''), 'Tip: '),
        nullif('Autor: ' || coalesce(author_name,''), 'Autor: '),
        case when survey_year is not null then 'Godina: ' || survey_year::text else null end,
        nullif('URL: ' || coalesce(drive_url, preview_url, ''), 'URL: '),
        nullif('Napomena: ' || coalesce(note,''), 'Napomena: ')
      ),
      E'\n\n---\n\n' order by updated_at desc nulls last
    ) as drawing_details_text
  from public.speleo_object_drawings
  group by 1
), reports as (
  select
    lower(regexp_replace(coalesce(object_name,''),'\s+','_','g')) as object_key,
    count(*)::integer as archive_report_count,
    max(created_at) as last_archive_report_at,
    string_agg(
      concat_ws(E'\n',
        nullif('Zapisnik/zahvat: ' || coalesce(object_name,''), 'Zapisnik/zahvat: '),
        case when date_start is not null or date_end is not null then 'Datum: ' || concat_ws(' – ', date_start::text, date_end::text) else null end,
        nullif('Svrha: ' || coalesce(purpose,''), 'Svrha: '),
        nullif('Opis: ' || coalesce(activity_description,''), 'Opis: '),
        nullif('Članovi: ' || coalesce(members,''), 'Članovi: '),
        nullif('Napomena: ' || coalesce(note,''), 'Napomena: ')
      ),
      E'\n\n---\n\n' order by date_start desc nulls last, created_at desc nulls last
    ) as report_details_text
  from public.speleo_activity_reports
  group by 1
), base as (
  select
    s.source_id as object_id,
    coalesce(nullif(o.data->>'name',''), s.name) as object_name,
    coalesce(nullif(o.data->>'plate_number',''), nullif(s.plate_number,''), nullif(s.cadastral_number,''), nullif(s.raw->>'plate_number','')) as plate_number,
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
      nullif('Pločica: ' || coalesce(coalesce(nullif(o.data->>'plate_number',''), nullif(s.plate_number,''), nullif(s.cadastral_number,''), nullif(s.raw->>'plate_number','')),''), 'Pločica: '),
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
      nullif('Opis: ' || coalesce(s.raw->>'description', s.raw->>'opis', s.raw->>'Opis', s.raw->>'description_hr', ''), 'Opis: '),
      nullif('Istraživanje: ' || coalesce(s.raw->>'research', s.raw->>'istrazivanje', s.raw->>'istraživanje', s.raw->>'exploration', ''), 'Istraživanje: '),
      nullif('Članovi / tko: ' || coalesce(s.raw->>'members', s.raw->>'clanovi', s.raw->>'članovi', s.raw->>'tko', ''), 'Članovi / tko: '),
      nullif('Napomena: ' || coalesce(s.raw->>'note', s.raw->>'napomena', s.raw->>'Napomena', ''), 'Napomena: ')
    ) as base_details_text,
    s.raw::text as raw_text,
    s.updated_at,
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
      s.raw->>'note'
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
    -- Važno: ovo su eksplicitni zadaci/falinke iz postojeće baze, ne zaključak iz uploadane arhive.
    (
      b.status_text_norm like '%koordinate%'
      or b.status_text_norm like '%nema tocne koordinate%'
      or b.status_text_norm like '%nemamo tocne koordinate%'
    ) as explicit_missing_coordinates,
    (
      b.status_text_norm like '%ponoviti nacrt%'
      or b.status_text_norm like '%ponoviti_nacrt%'
      or b.status_text_norm like '%srediti nacrt%'
      or b.status_text_norm like '%srediti_nacrt%'
      or b.status_text_norm like '%digitalizirati nacrt%'
      or b.status_text_norm like '%digitalizirati_nacrt%'
      or b.status_text_norm like '%nastaviti nacrt%'
      or b.status_text_norm like '%nastaviti_nacrt%'
      or b.status_text_norm like '%nema tlocrta%'
      or b.status_text_norm like '%nema tlocrt%'
      or b.status_text_norm like '%treba ga nacrtati%'
      or b.status_text_norm like '%nacrtati%'
    ) as explicit_missing_drawing,
    (
      b.status_text_norm like '%zapisnik%'
      or b.status_text_norm like '%izvjestaj%'
      or b.status_text_norm like '%izvještaj%'
      or b.status_text_norm like '%nema zapisnika%'
    ) as explicit_missing_record
  from base b
), enriched as (
  select
    r.*,
    st.archive_status,
    st.priority,
    st.last_note,
    coalesce(d.archive_drawing_count,0) as archive_drawing_count,
    coalesce(rep.archive_report_count,0) as archive_report_count,
    d.last_archive_drawing_at,
    rep.last_archive_report_at,
    d.drawing_details_text,
    rep.report_details_text,

    -- Manualni arhivar status smije overrideati bazu. Ako nema manualnog statusa, koristi se postojeća speleo baza.
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
        else true -- nema eksplicitnog zadatka "fali nacrt" u bazi; ne optužuj objekt samo zato što nije u našoj upload arhivi
      end
    ) as has_drawing,

    coalesce(st.has_record,
      case
        when r.explicit_missing_record then false
        when r.base_in_cadastre or r.base_ready_for_katastar then true
        else true -- isto: zapisnik se ne zaključuje iz upload arhive nego iz zadataka/statusa baze
      end
    ) as has_record
  from rules r
  left join public.sov_archive_object_status st on st.object_id = r.object_id
  left join drawings d on d.object_key = r.object_id or d.object_key = lower(regexp_replace(coalesce(r.object_name,''),'\s+','_','g'))
  left join reports rep on rep.object_key = lower(regexp_replace(coalesce(r.object_name,''),'\s+','_','g'))
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
  archive_drawing_count,
  archive_report_count,
  archive_drawing_count as drawing_count, -- compatibility: UI još može čitati staro ime
  archive_report_count as report_count,
  base_details_text,
  report_details_text,
  drawing_details_text,
  raw_text,
  concat_ws(E'\n\n', base_details_text, nullif('--- ZAPISNICI / ISTRAŽIVANJA ---' || E'\n' || coalesce(report_details_text,''), '--- ZAPISNICI / ISTRAŽIVANJA ---' || E'\n'), nullif('--- NACRTI ---' || E'\n' || coalesce(drawing_details_text,''), '--- NACRTI ---' || E'\n')) as full_details_text,
  last_archive_drawing_at,
  last_archive_report_at,
  base_in_cadastre,
  base_ready_for_katastar,
  explicit_missing_coordinates,
  explicit_missing_drawing,
  explicit_missing_record,
  (not has_coordinates) as missing_coordinates,
  (not has_drawing) as missing_drawing,
  (not has_record) as missing_record,
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
   + case when coalesce(priority,'normal')='high' then 20 else 0 end
   + case when base_ready_for_katastar then 10 else 0 end
  )::integer as priority_score,
  'speleo_baza_status'::text as completeness_source,
  lower(coalesce(object_name,'') || ' ' || coalesce(plate_number,'') || ' ' || coalesce(nearest_place,'') || ' ' || coalesce(object_type,'') || ' ' || coalesce(field_tasks,'') || ' ' || coalesce(workflow_raw,'') || ' ' || coalesce(base_details_text,'')) as search_text,
  updated_at
from enriched;

create view public.sov_arhivar_dashboard as
select
  count(*)::integer as total_objects,
  count(*) filter (where missing_coordinates)::integer as missing_coordinates,
  count(*) filter (where missing_drawing)::integer as missing_drawings,
  count(*) filter (where missing_record)::integer as missing_records,
  count(*) filter (where katastar_readiness = 'spremno_za_katastar')::integer as ready_for_katastar,
  count(*) filter (where katastar_readiness = 'u_katastru')::integer as in_katastar,
  count(*) filter (where katastar_readiness = 'nije_u_katastru_provjeriti')::integer as not_in_katastar_review,
  count(*) filter (where katastar_readiness in ('nepotpuno','provjeriti','nije_u_katastru_provjeriti'))::integer as incomplete_objects,
  max(updated_at) as last_object_update
from public.sov_arhivar_worklist;

-- Verification: brojke poslije fixa ne bi smjele masovno tvrditi da fali nacrt/zapisnik samo zato što nisu uploadani u novu arhivu.
select 'sov_arhivar_dashboard' as check_name, to_jsonb(d.*) as result
from public.sov_arhivar_dashboard d;
