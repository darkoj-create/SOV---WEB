
-- SOV Speleo Baza SQL v1 / build v5.01
-- Pokreni jednom u Supabase SQL editoru.
-- Nakon toga Baza stranice mogu citati speleo_objects kao glavni izvor,
-- a data/sov-baza.json ostaje fallback.

create table if not exists public.speleo_objects (
  id bigint primary key,
  name text not null,
  cadastre_status text,
  cadastral_number text,
  in_cadastre_bool boolean,
  not_in_cadastre_candidate boolean,
  record_status text,
  field_tasks text,
  object_type_final text,
  object_type_source text,
  lat double precision,
  lon double precision,
  county text,
  municipality text,
  nearest_place text,
  locality text,
  altitude_m double precision,
  depth_m double precision,
  length_m double precision,
  vertical_range_m double precision,
  entrance_count double precision,
  plate_number text,
  main_entrance_status text,
  access_description text,
  technical_description text,
  research_perspective boolean,
  research_perspective_note text,
  last_research_year double precision,
  last_research_date text,
  last_research_date_iso date,
  clubs text,
  survey_authors text,
  team_members text,
  hazards text,
  pollution text,
  digital_survey_status text,
  bibliography_status text,
  ice_present text,
  hydrology text,
  hydrogeology text,
  georef_record text,
  gps_tracklog text,
  note text,
  workflow_raw text,
  source_json jsonb default '{}'::jsonb,
  updated_at timestamptz default now(),
  updated_by uuid null
);

create index if not exists speleo_objects_name_idx on public.speleo_objects using gin (to_tsvector('simple', coalesce(name,'')));
create index if not exists speleo_objects_status_idx on public.speleo_objects(record_status, cadastre_status);
create index if not exists speleo_objects_coords_idx on public.speleo_objects(lat, lon);
create index if not exists speleo_objects_cadastral_number_idx on public.speleo_objects(cadastral_number);

create table if not exists public.speleo_object_aliases (
  id bigserial primary key,
  object_id bigint references public.speleo_objects(id) on delete cascade,
  alias text not null,
  source text default 'manual',
  created_at timestamptz default now(),
  unique(object_id, alias)
);

create table if not exists public.speleo_object_import_log (
  id bigserial primary key,
  imported_at timestamptz default now(),
  imported_by uuid null,
  source text,
  rows_seen integer default 0,
  rows_upserted integer default 0,
  note text
);

alter table public.speleo_objects enable row level security;
alter table public.speleo_object_aliases enable row level security;
alter table public.speleo_object_import_log enable row level security;

-- Open preview compatible policies. Safe enough for current demo; tighten later when login roles settle.
do $$ begin
  if not exists (select 1 from pg_policies where schemaname='public' and tablename='speleo_objects' and policyname='speleo_objects_select_all') then
    create policy speleo_objects_select_all on public.speleo_objects for select using (true);
  end if;
  if not exists (select 1 from pg_policies where schemaname='public' and tablename='speleo_objects' and policyname='speleo_objects_insert_all') then
    create policy speleo_objects_insert_all on public.speleo_objects for insert with check (true);
  end if;
  if not exists (select 1 from pg_policies where schemaname='public' and tablename='speleo_objects' and policyname='speleo_objects_update_all') then
    create policy speleo_objects_update_all on public.speleo_objects for update using (true) with check (true);
  end if;

  if not exists (select 1 from pg_policies where schemaname='public' and tablename='speleo_object_aliases' and policyname='speleo_object_aliases_all') then
    create policy speleo_object_aliases_all on public.speleo_object_aliases for all using (true) with check (true);
  end if;
  if not exists (select 1 from pg_policies where schemaname='public' and tablename='speleo_object_import_log' and policyname='speleo_object_import_log_all') then
    create policy speleo_object_import_log_all on public.speleo_object_import_log for all using (true) with check (true);
  end if;
end $$;

-- Helper function: one JSON array import/upsert from frontend or SQL RPC.
create or replace function public.import_speleo_objects_from_json(payload jsonb, source_name text default 'baza_velebit_2026_appready.json')
returns jsonb
language plpgsql
security definer
as $$
declare
  elem jsonb;
  seen int := 0;
  upserted int := 0;
begin
  if jsonb_typeof(payload) <> 'array' then
    raise exception 'Payload must be a JSON array';
  end if;

  for elem in select * from jsonb_array_elements(payload) loop
    seen := seen + 1;
    if nullif(elem->>'id','') is null or nullif(elem->>'name','') is null then
      continue;
    end if;

    insert into public.speleo_objects (
      id,name,cadastre_status,cadastral_number,in_cadastre_bool,not_in_cadastre_candidate,record_status,field_tasks,
      object_type_final,object_type_source,lat,lon,county,municipality,nearest_place,locality,altitude_m,depth_m,length_m,vertical_range_m,
      entrance_count,plate_number,main_entrance_status,access_description,technical_description,research_perspective,research_perspective_note,
      last_research_year,last_research_date,last_research_date_iso,clubs,survey_authors,team_members,hazards,pollution,digital_survey_status,
      bibliography_status,ice_present,hydrology,hydrogeology,georef_record,gps_tracklog,note,workflow_raw,source_json,updated_at
    ) values (
      (elem->>'id')::bigint,
      elem->>'name', elem->>'cadastre_status', elem->>'cadastral_number', nullif(elem->>'in_cadastre_bool','')::boolean,
      nullif(elem->>'not_in_cadastre_candidate','')::boolean, elem->>'record_status', elem->>'field_tasks', elem->>'object_type_final', elem->>'object_type_source',
      nullif(elem->>'lat','')::double precision, nullif(elem->>'lon','')::double precision, elem->>'county', elem->>'municipality', elem->>'nearest_place', elem->>'locality',
      nullif(elem->>'altitude_m','')::double precision, nullif(elem->>'depth_m','')::double precision, nullif(elem->>'length_m','')::double precision, nullif(elem->>'vertical_range_m','')::double precision,
      nullif(elem->>'entrance_count','')::double precision, elem->>'plate_number', elem->>'main_entrance_status', elem->>'access_description', elem->>'technical_description',
      nullif(elem->>'research_perspective','')::boolean, elem->>'research_perspective_note', nullif(elem->>'last_research_year','')::double precision, elem->>'last_research_date', nullif(elem->>'last_research_date_iso','')::date,
      elem->>'clubs', elem->>'survey_authors', elem->>'team_members', elem->>'hazards', elem->>'pollution', elem->>'digital_survey_status', elem->>'bibliography_status', elem->>'ice_present',
      elem->>'hydrology', elem->>'hydrogeology', elem->>'georef_record', elem->>'gps_tracklog', elem->>'note', elem->>'workflow_raw', elem, now()
    )
    on conflict (id) do update set
      name=excluded.name,
      cadastre_status=excluded.cadastre_status,
      cadastral_number=excluded.cadastral_number,
      in_cadastre_bool=excluded.in_cadastre_bool,
      not_in_cadastre_candidate=excluded.not_in_cadastre_candidate,
      record_status=excluded.record_status,
      field_tasks=excluded.field_tasks,
      object_type_final=excluded.object_type_final,
      object_type_source=excluded.object_type_source,
      lat=excluded.lat, lon=excluded.lon,
      county=excluded.county, municipality=excluded.municipality, nearest_place=excluded.nearest_place, locality=excluded.locality,
      altitude_m=excluded.altitude_m, depth_m=excluded.depth_m, length_m=excluded.length_m, vertical_range_m=excluded.vertical_range_m,
      entrance_count=excluded.entrance_count, plate_number=excluded.plate_number, main_entrance_status=excluded.main_entrance_status,
      access_description=excluded.access_description, technical_description=excluded.technical_description,
      research_perspective=excluded.research_perspective, research_perspective_note=excluded.research_perspective_note,
      last_research_year=excluded.last_research_year, last_research_date=excluded.last_research_date, last_research_date_iso=excluded.last_research_date_iso,
      clubs=excluded.clubs, survey_authors=excluded.survey_authors, team_members=excluded.team_members,
      hazards=excluded.hazards, pollution=excluded.pollution, digital_survey_status=excluded.digital_survey_status, bibliography_status=excluded.bibliography_status,
      ice_present=excluded.ice_present, hydrology=excluded.hydrology, hydrogeology=excluded.hydrogeology, georef_record=excluded.georef_record, gps_tracklog=excluded.gps_tracklog,
      note=excluded.note, workflow_raw=excluded.workflow_raw, source_json=excluded.source_json, updated_at=now();
    upserted := upserted + 1;
  end loop;

  insert into public.speleo_object_import_log(source, rows_seen, rows_upserted, note)
  values(source_name, seen, upserted, 'frontend/json import');

  return jsonb_build_object('rows_seen', seen, 'rows_upserted', upserted);
end;
$$;

grant execute on function public.import_speleo_objects_from_json(jsonb, text) to anon, authenticated;
