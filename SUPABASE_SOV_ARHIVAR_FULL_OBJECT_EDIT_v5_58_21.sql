-- SOV web v5.58.21 — Arhivar full object edit + Karta full-detail override
-- Run after v5.58.20 / existing Arhivar SQL.
-- Adds a safe full-object edit RPC and a v2 detail RPC that applies human overrides.

create extension if not exists pgcrypto;

create table if not exists public.speleo_object_overrides (
  object_id text primary key,
  data jsonb not null default '{}'::jsonb,
  updated_by uuid null references auth.users(id) on delete set null,
  updated_at timestamptz not null default now()
);

alter table public.speleo_object_overrides add column if not exists data jsonb not null default '{}'::jsonb;
alter table public.speleo_object_overrides add column if not exists updated_by uuid null references auth.users(id) on delete set null;
alter table public.speleo_object_overrides add column if not exists updated_at timestamptz not null default now();

create table if not exists public.speleo_object_edits (
  id bigserial primary key,
  object_id text not null,
  edited_by uuid null references auth.users(id) on delete set null,
  edited_at timestamptz not null default now(),
  changed_fields text[] not null default '{}',
  old_values jsonb null,
  new_values jsonb not null default '{}'::jsonb,
  note text null
);

create or replace function public.sov_arhivar_can_edit_object()
returns boolean
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_email text := lower(coalesce(auth.jwt()->>'email',''));
  v_ok boolean := false;
begin
  if v_email = 'darko.jeras@gmail.com' then
    return true;
  end if;

  if to_regclass('public.profiles') is not null then
    execute $q$
      select exists(
        select 1
        from public.profiles p
        where p.id = auth.uid()
          and lower(coalesce(p.role::text,'')) in ('webmaster','admin','arhivar')
      )
    $q$ into v_ok;
    if coalesce(v_ok,false) then
      return true;
    end if;
  end if;

  -- Metadata fallback for older auth setups.
  return lower(coalesce(auth.jwt()->'user_metadata'->>'role', auth.jwt()->'app_metadata'->>'role', '')) in ('webmaster','admin','arhivar');
end;
$$;

create or replace function public.sov_arhivar_text_label_value(p_text text, p_label text)
returns text
language plpgsql
immutable
as $$
declare
  v_lines text[] := regexp_split_to_array(coalesce(p_text,''), E'\\r?\\n');
  v_line text;
  v_current text := null;
  v_out text := '';
  v_label_norm text := lower(trim(coalesce(p_label,'')));
  v_match text[];
begin
  foreach v_line in array v_lines loop
    v_line := trim(coalesce(v_line,''));
    if v_line = '' then
      continue;
    end if;

    v_match := regexp_match(v_line, '^([^:]{2,90}):\s*(.*)$');
    if v_match is not null then
      if lower(trim(v_match[1])) = v_label_norm then
        v_current := trim(v_match[1]);
        if nullif(trim(v_match[2]), '') is not null then
          v_out := concat_ws(E'\n', nullif(v_out,''), trim(v_match[2]));
        end if;
      else
        v_current := null;
      end if;
    elsif v_current is not null then
      v_out := concat_ws(E'\n', nullif(v_out,''), v_line);
    end if;
  end loop;

  return nullif(trim(v_out),'');
end;
$$;

create or replace function public.sov_arhivar_update_object_full(
  p_object_id text,
  p_data jsonb,
  p_note text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_old jsonb := '{}'::jsonb;
  v_new jsonb := '{}'::jsonb;
  v_fields text[] := '{}';
begin
  if nullif(trim(coalesce(p_object_id,'')), '') is null then
    raise exception 'Nedostaje object_id.';
  end if;

  if not public.sov_arhivar_can_edit_object() then
    raise exception 'Nemaš prava za uređivanje arhive.';
  end if;

  p_data := coalesce(p_data, '{}'::jsonb);
  select coalesce(data,'{}'::jsonb) into v_old
  from public.speleo_object_overrides
  where object_id = p_object_id;
  v_old := coalesce(v_old, '{}'::jsonb);

  insert into public.speleo_object_overrides(object_id, data, updated_by, updated_at)
  values (p_object_id, v_old || p_data, auth.uid(), now())
  on conflict (object_id) do update
    set data = coalesce(public.speleo_object_overrides.data,'{}'::jsonb) || excluded.data,
        updated_by = auth.uid(),
        updated_at = now()
  returning data into v_new;

  select coalesce(array_agg(key order by key), '{}') into v_fields
  from jsonb_object_keys(p_data) as key;

  insert into public.speleo_object_edits(object_id, edited_by, changed_fields, old_values, new_values, note)
  values (p_object_id, auth.uid(), v_fields, v_old, p_data, nullif(trim(coalesce(p_note,'')), ''));

  return jsonb_build_object(
    'ok', true,
    'object_id', p_object_id,
    'changed_fields', v_fields,
    'data', v_new,
    'updated_at', now()
  );
end;
$$;

create or replace function public.sov_arhivar_get_object_detail_v2(p_object_id text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_base jsonb := '{}'::jsonb;
  v_ov jsonb := '{}'::jsonb;
  v_name text;
  v_plate text;
  v_type text;
  v_place text;
  v_county text;
  v_municipality text;
  v_lat text;
  v_lon text;
  v_cadastre text;
  v_record text;
  v_tasks text;
  v_workflow text;
  v_digital text;
  v_biblio text;
  v_gps text;
  v_georef text;
  v_desc text;
  v_access text;
  v_research text;
  v_authors text;
  v_hydrology text;
  v_geology text;
  v_hazards text;
  v_note text;
  v_base_text text;
  v_full text;
begin
  v_base := coalesce(public.sov_arhivar_get_object_detail(p_object_id), '{}'::jsonb);
  select coalesce(data,'{}'::jsonb) into v_ov
  from public.speleo_object_overrides
  where object_id = p_object_id;
  v_ov := coalesce(v_ov,'{}'::jsonb);

  v_name := coalesce(nullif(v_ov->>'object_name',''), nullif(v_ov->>'name',''), v_base->>'object_name');
  v_plate := coalesce(nullif(v_ov->>'plate_number',''), nullif(v_ov->>'cadastral_number',''), v_base->>'plate_number');
  v_type := coalesce(nullif(v_ov->>'object_type',''), nullif(v_ov->>'object_type_final',''), v_base->>'object_type');
  v_place := coalesce(nullif(v_ov->>'nearest_place',''), nullif(v_ov->>'locality',''), v_base->>'nearest_place');
  v_county := coalesce(nullif(v_ov->>'county',''), v_base->>'county');
  v_municipality := coalesce(nullif(v_ov->>'municipality',''), v_base->>'municipality');
  v_lat := coalesce(nullif(v_ov->>'lat',''), v_base->>'lat');
  v_lon := coalesce(nullif(v_ov->>'lon',''), v_base->>'lon');
  v_cadastre := coalesce(nullif(v_ov->>'cadastre_status',''), v_base->>'cadastre_status', public.sov_arhivar_text_label_value(v_base->>'base_details_text','Katastarski status'));
  v_record := coalesce(nullif(v_ov->>'record_status',''), v_base->>'record_status', public.sov_arhivar_text_label_value(v_base->>'base_details_text','Status zapisa'));
  v_tasks := coalesce(nullif(v_ov->>'field_tasks',''), v_base->>'field_tasks', public.sov_arhivar_text_label_value(v_base->>'base_details_text','Zadaci / što fali'));
  v_workflow := coalesce(nullif(v_ov->>'workflow_raw',''), v_base->>'workflow_raw', public.sov_arhivar_text_label_value(v_base->>'base_details_text','Workflow'));
  v_digital := coalesce(nullif(v_ov->>'digital_survey_status',''), v_base->>'digital_survey_status', public.sov_arhivar_text_label_value(v_base->>'base_details_text','Digitalni nacrt'));
  v_biblio := coalesce(nullif(v_ov->>'bibliography_status',''), v_base->>'bibliography_status', public.sov_arhivar_text_label_value(v_base->>'base_details_text','Bibliografija/zapisnik'));
  v_gps := coalesce(nullif(v_ov->>'gps_tracklog',''), v_base->>'gps_tracklog', public.sov_arhivar_text_label_value(v_base->>'base_details_text','GPS tracklog'));
  v_georef := coalesce(nullif(v_ov->>'georef_record',''), v_base->>'georef_record', public.sov_arhivar_text_label_value(v_base->>'base_details_text','Georef zapis'));

  v_desc := coalesce(nullif(v_ov->>'description',''), nullif(v_ov->>'technical_description',''), nullif(v_ov->>'opis',''), public.sov_arhivar_text_label_value(v_base->>'base_details_text','Opis'));
  v_access := coalesce(nullif(v_ov->>'access_description',''), nullif(v_ov->>'access',''), nullif(v_ov->>'pristup',''), public.sov_arhivar_text_label_value(v_base->>'base_details_text','Pristup'));
  v_research := coalesce(nullif(v_ov->>'research',''), nullif(v_ov->>'history',''), nullif(v_ov->>'istrazivanje',''), public.sov_arhivar_text_label_value(v_base->>'base_details_text','Istraživanje / povijest'));
  v_authors := coalesce(nullif(v_ov->>'authors',''), nullif(v_ov->>'members',''), nullif(v_ov->>'team',''), public.sov_arhivar_text_label_value(v_base->>'base_details_text','Autori / ekipa'));
  v_hydrology := coalesce(nullif(v_ov->>'hydrology',''), nullif(v_ov->>'hidrologija',''), public.sov_arhivar_text_label_value(v_base->>'base_details_text','Hidrologija'));
  v_geology := coalesce(nullif(v_ov->>'geology',''), nullif(v_ov->>'morphology',''), public.sov_arhivar_text_label_value(v_base->>'base_details_text','Geologija / morfologija'));
  v_hazards := coalesce(nullif(v_ov->>'hazards',''), nullif(v_ov->>'protection',''), nullif(v_ov->>'observed_threats',''), public.sov_arhivar_text_label_value(v_base->>'base_details_text','Opasnosti / zaštita'));
  v_note := coalesce(nullif(v_ov->>'note',''), nullif(v_ov->>'remarks',''), public.sov_arhivar_text_label_value(v_base->>'base_details_text','Napomena'));

  v_base_text := concat_ws(E'\n',
    nullif('Naziv: ' || coalesce(v_name,''), 'Naziv: '),
    nullif('Tip: ' || coalesce(v_type,''), 'Tip: '),
    nullif('Pločica: ' || coalesce(v_plate,''), 'Pločica: '),
    nullif('Najbliže mjesto: ' || coalesce(v_place,''), 'Najbliže mjesto: '),
    nullif('Županija/regija: ' || coalesce(v_county,''), 'Županija/regija: '),
    nullif('Općina: ' || coalesce(v_municipality,''), 'Općina: '),
    nullif('Katastarski status: ' || coalesce(v_cadastre,''), 'Katastarski status: '),
    nullif('Status zapisa: ' || coalesce(v_record,''), 'Status zapisa: '),
    nullif('Zadaci / što fali: ' || coalesce(v_tasks,''), 'Zadaci / što fali: '),
    nullif('Workflow: ' || coalesce(v_workflow,''), 'Workflow: '),
    nullif('Digitalni nacrt: ' || coalesce(v_digital,''), 'Digitalni nacrt: '),
    nullif('Bibliografija/zapisnik: ' || coalesce(v_biblio,''), 'Bibliografija/zapisnik: '),
    nullif('GPS tracklog: ' || coalesce(v_gps,''), 'GPS tracklog: '),
    nullif('Georef zapis: ' || coalesce(v_georef,''), 'Georef zapis: '),
    nullif('Opis: ' || coalesce(v_desc,''), 'Opis: '),
    nullif('Pristup: ' || coalesce(v_access,''), 'Pristup: '),
    nullif('Istraživanje / povijest: ' || coalesce(v_research,''), 'Istraživanje / povijest: '),
    nullif('Autori / ekipa: ' || coalesce(v_authors,''), 'Autori / ekipa: '),
    nullif('Hidrologija: ' || coalesce(v_hydrology,''), 'Hidrologija: '),
    nullif('Geologija / morfologija: ' || coalesce(v_geology,''), 'Geologija / morfologija: '),
    nullif('Opasnosti / zaštita: ' || coalesce(v_hazards,''), 'Opasnosti / zaštita: '),
    nullif('Napomena: ' || coalesce(v_note,''), 'Napomena: ')
  );

  v_full := concat_ws(E'\n\n',
    v_base_text,
    nullif('--- ZAPISNICI / ISTRAŽIVANJA ---' || E'\n' || coalesce(v_base->>'report_details_text',''), '--- ZAPISNICI / ISTRAŽIVANJA ---' || E'\n'),
    nullif('--- NACRTI ---' || E'\n' || coalesce(v_base->>'drawing_details_text',''), '--- NACRTI ---' || E'\n')
  );

  return v_base || jsonb_build_object(
    'object_id', p_object_id,
    'object_name', v_name,
    'name', v_name,
    'plate_number', v_plate,
    'cadastral_number', v_plate,
    'object_type', v_type,
    'object_type_final', v_type,
    'nearest_place', v_place,
    'county', v_county,
    'municipality', v_municipality,
    'lat', v_lat,
    'lon', v_lon,
    'cadastre_status', v_cadastre,
    'record_status', v_record,
    'field_tasks', v_tasks,
    'workflow_raw', v_workflow,
    'digital_survey_status', v_digital,
    'bibliography_status', v_biblio,
    'gps_tracklog', v_gps,
    'georef_record', v_georef,
    'description', v_desc,
    'technical_description', v_desc,
    'access_description', v_access,
    'research', v_research,
    'authors', v_authors,
    'hydrology', v_hydrology,
    'geology', v_geology,
    'hazards', v_hazards,
    'note', v_note,
    'base_details_text', v_base_text,
    'full_details_text', v_full,
    'override_data', v_ov,
    'details_source', 'sov_arhivar_get_object_detail_v2_5.58.21'
  );
end;
$$;

grant execute on function public.sov_arhivar_update_object_full(text,jsonb,text) to authenticated;
grant execute on function public.sov_arhivar_get_object_detail_v2(text) to authenticated;
grant execute on function public.sov_arhivar_can_edit_object() to authenticated;
grant execute on function public.sov_arhivar_text_label_value(text,text) to authenticated;
