-- SOV Web v5.57 / APK 1.4.13 — Arhivar workflow ALL-IN-ONE
-- RLS ostaje uključen. Siguran sloj za arhivara: status arhive, što fali za katastar,
-- povezivanje zapisnika/nacrta s objektom, novi objekt i edit postojećeg kroz override/edit log.

create extension if not exists pgcrypto;

-- Role helper fallback. Ne mijenja postojeću funkciju ako već postoji.
do $$
begin
  if not exists (
    select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname='public' and p.proname='sov_has_role' and p.pronargs=1
  ) then
    execute $fn$
    create function public.sov_has_role(required_roles text[])
    returns boolean
    language sql
    stable
    security definer
    set search_path = public, auth
    as $body$
      select exists (
        select 1
        from public.profiles p
        where p.id = auth.uid()
          and coalesce(p.status::text,'') = 'approved'
          and lower(coalesce(p.role::text,'user')) = any(required_roles)
      )
    $body$;
    $fn$;
  end if;
end $$;

create or replace function public.sov_is_arhivar_or_admin()
returns boolean
language sql
stable
security definer
set search_path = public, auth
as $$
  select public.sov_has_role(array['admin','arhivar'])
$$;

-- Minimal speleo object staging compatibility. Existing table is not replaced.
create table if not exists public.speleo_objects_staging (
  source_id text primary key,
  source_system text not null default 'sov_archive_manual',
  name text not null,
  lat double precision,
  lon double precision,
  cadastre_status text,
  cadastral_number text,
  record_status text,
  object_type_final text,
  county text,
  municipality text,
  nearest_place text,
  locality text,
  depth_m double precision,
  length_m double precision,
  field_tasks text,
  workflow_raw text,
  raw jsonb not null default '{}'::jsonb,
  import_batch text,
  imported_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.speleo_objects_staging add column if not exists edit_note text;
alter table public.speleo_objects_staging add column if not exists sandbox_status text not null default 'staging';
alter table public.speleo_objects_staging add column if not exists edited_by text;
alter table public.speleo_objects_staging add column if not exists edited_at timestamptz;

create index if not exists speleo_objects_staging_name_idx on public.speleo_objects_staging using gin (to_tsvector('simple', coalesce(name,'')));
create index if not exists speleo_objects_staging_coord_idx on public.speleo_objects_staging (lat, lon);
create index if not exists speleo_objects_staging_status_idx on public.speleo_objects_staging (record_status, cadastre_status);

create or replace function public.touch_speleo_objects_staging_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end $$;

drop trigger if exists trg_touch_speleo_objects_staging on public.speleo_objects_staging;
create trigger trg_touch_speleo_objects_staging
before update on public.speleo_objects_staging
for each row execute function public.touch_speleo_objects_staging_updated_at();

-- Existing edit/override tables from speleo baza layer, made safe.
create table if not exists public.speleo_object_overrides (
  object_id text primary key,
  data jsonb not null default '{}'::jsonb,
  updated_by uuid null,
  updated_at timestamptz not null default now()
);

create table if not exists public.speleo_object_edits (
  id bigserial primary key,
  object_id text not null,
  edited_by uuid null,
  edited_at timestamptz not null default now(),
  changed_fields text[] not null default '{}',
  old_values jsonb null,
  new_values jsonb not null default '{}'::jsonb,
  note text null
);

-- Drawings/nacrti compatibility.
create table if not exists public.speleo_object_drawings (
  id uuid primary key default gen_random_uuid(),
  object_id text,
  object_name text,
  object_slug text,
  plate_number text,
  region text,
  drawing_title text,
  drawing_type text not null default 'nacrt',
  archive_status text not null default 'draft',
  file_format text,
  mime_type text,
  file_size text,
  drive_file_id text,
  drive_file_name text,
  drive_url text,
  preview_url text,
  source text not null default 'manual',
  author_name text,
  survey_year integer,
  match_score integer,
  match_status text not null default 'manual_review',
  public_visible boolean not null default true,
  verified_by uuid,
  verified_at timestamptz,
  synced_by uuid,
  synced_at timestamptz,
  created_by uuid default auth.uid(),
  created_at timestamptz not null default now(),
  updated_by uuid,
  updated_at timestamptz not null default now(),
  metadata jsonb not null default '{}'::jsonb,
  note text
);

-- Reports/zapisnici compatibility.
create table if not exists public.speleo_activity_reports (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  created_by uuid null,
  created_by_email text null,
  object_name text not null,
  plate_number text null,
  object_type text null,
  hydrology text[] not null default '{}',
  hydrogeology text[] not null default '{}',
  nearest_place text null,
  coordinate_system text null,
  x_coord text null,
  y_coord text null,
  date_start date null,
  date_end date null,
  purpose text null,
  purpose_other text null,
  activity_description text null,
  execution_method text null,
  observed_threats text[] not null default '{}',
  bat_colony text null,
  bivalve text null,
  sponge text null,
  olm text null,
  fossils text null,
  members text null,
  note text null,
  raw jsonb not null default '{}'::jsonb
);

-- Arhivar-specific status table: one row per object.
create table if not exists public.sov_archive_object_status (
  object_id text primary key,
  object_name text,
  plate_number text,
  has_coordinates boolean,
  has_drawing boolean,
  has_record boolean,
  has_katastar_ready boolean,
  archive_status text not null default 'needs_review',
  priority text not null default 'normal',
  assigned_to uuid null,
  last_note text,
  source text not null default 'arhivar',
  metadata jsonb not null default '{}'::jsonb,
  created_by uuid default auth.uid(),
  created_at timestamptz not null default now(),
  updated_by uuid,
  updated_at timestamptz not null default now()
);

alter table public.sov_archive_object_status add column if not exists has_coordinates boolean;
alter table public.sov_archive_object_status add column if not exists has_drawing boolean;
alter table public.sov_archive_object_status add column if not exists has_record boolean;
alter table public.sov_archive_object_status add column if not exists has_katastar_ready boolean;
alter table public.sov_archive_object_status add column if not exists archive_status text not null default 'needs_review';
alter table public.sov_archive_object_status add column if not exists priority text not null default 'normal';
alter table public.sov_archive_object_status add column if not exists last_note text;
alter table public.sov_archive_object_status add column if not exists metadata jsonb not null default '{}'::jsonb;
alter table public.sov_archive_object_status add column if not exists updated_by uuid;
alter table public.sov_archive_object_status add column if not exists updated_at timestamptz not null default now();

create table if not exists public.sov_archive_actions (
  id uuid primary key default gen_random_uuid(),
  object_id text,
  object_name text,
  action_type text not null,
  title text,
  note text,
  before_data jsonb,
  after_data jsonb,
  created_by uuid default auth.uid(),
  created_at timestamptz not null default now(),
  metadata jsonb not null default '{}'::jsonb
);

create index if not exists sov_archive_status_status_idx on public.sov_archive_object_status (archive_status, priority);
create index if not exists sov_archive_status_name_idx on public.sov_archive_object_status using gin (to_tsvector('simple', coalesce(object_name,'')));
create index if not exists sov_archive_actions_object_idx on public.sov_archive_actions (object_id, created_at desc);
create index if not exists speleo_drawings_object_idx on public.speleo_object_drawings (object_id, object_name);
create index if not exists speleo_activity_object_idx on public.speleo_activity_reports using gin (to_tsvector('simple', coalesce(object_name,'')));

create or replace function public.sov_archive_touch_status()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  new.updated_by = auth.uid();
  return new;
end $$;

drop trigger if exists trg_sov_archive_touch_status on public.sov_archive_object_status;
create trigger trg_sov_archive_touch_status
before update on public.sov_archive_object_status
for each row execute function public.sov_archive_touch_status();

-- Simple logger for status changes.
create or replace function public.sov_archive_log_status_change()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  insert into public.sov_archive_actions(object_id, object_name, action_type, title, note, before_data, after_data, metadata)
  values (
    coalesce(new.object_id, old.object_id),
    coalesce(new.object_name, old.object_name),
    lower(tg_op),
    'Status arhive',
    coalesce(new.last_note, old.last_note),
    case when tg_op in ('UPDATE','DELETE') then to_jsonb(old) else null end,
    case when tg_op in ('INSERT','UPDATE') then to_jsonb(new) else null end,
    jsonb_build_object('module','arhivar','table','sov_archive_object_status')
  );
  return coalesce(new, old);
exception when others then
  return coalesce(new, old);
end $$;

drop trigger if exists trg_sov_archive_status_audit on public.sov_archive_object_status;
create trigger trg_sov_archive_status_audit
after insert or update or delete on public.sov_archive_object_status
for each row execute function public.sov_archive_log_status_change();

-- RLS.
alter table public.speleo_objects_staging enable row level security;
alter table public.speleo_object_overrides enable row level security;
alter table public.speleo_object_edits enable row level security;
alter table public.speleo_object_drawings enable row level security;
alter table public.speleo_activity_reports enable row level security;
alter table public.sov_archive_object_status enable row level security;
alter table public.sov_archive_actions enable row level security;

-- Safe policy reset for new tables.
drop policy if exists sov_archive_status_select on public.sov_archive_object_status;
drop policy if exists sov_archive_status_insert on public.sov_archive_object_status;
drop policy if exists sov_archive_status_update on public.sov_archive_object_status;
drop policy if exists sov_archive_actions_select on public.sov_archive_actions;
drop policy if exists sov_archive_actions_insert on public.sov_archive_actions;

create policy sov_archive_status_select on public.sov_archive_object_status for select to authenticated using (true);
create policy sov_archive_status_insert on public.sov_archive_object_status for insert to authenticated with check (public.sov_is_arhivar_or_admin());
create policy sov_archive_status_update on public.sov_archive_object_status for update to authenticated using (public.sov_is_arhivar_or_admin()) with check (public.sov_is_arhivar_or_admin());
create policy sov_archive_actions_select on public.sov_archive_actions for select to authenticated using (public.sov_is_arhivar_or_admin());
create policy sov_archive_actions_insert on public.sov_archive_actions for insert to authenticated with check (public.sov_is_arhivar_or_admin());

-- Compatibility policies: open reads to approved users where useful, writes to arhivar/admin.
drop policy if exists speleo_staging_select_arhivar_v557 on public.speleo_objects_staging;
drop policy if exists speleo_staging_insert_arhivar_v557 on public.speleo_objects_staging;
drop policy if exists speleo_staging_update_arhivar_v557 on public.speleo_objects_staging;
create policy speleo_staging_select_arhivar_v557 on public.speleo_objects_staging for select to authenticated using (true);
create policy speleo_staging_insert_arhivar_v557 on public.speleo_objects_staging for insert to authenticated with check (public.sov_is_arhivar_or_admin());
create policy speleo_staging_update_arhivar_v557 on public.speleo_objects_staging for update to authenticated using (public.sov_is_arhivar_or_admin()) with check (public.sov_is_arhivar_or_admin());

drop policy if exists speleo_overrides_select_arhivar_v557 on public.speleo_object_overrides;
drop policy if exists speleo_overrides_insert_arhivar_v557 on public.speleo_object_overrides;
drop policy if exists speleo_overrides_update_arhivar_v557 on public.speleo_object_overrides;
create policy speleo_overrides_select_arhivar_v557 on public.speleo_object_overrides for select to authenticated using (true);
create policy speleo_overrides_insert_arhivar_v557 on public.speleo_object_overrides for insert to authenticated with check (public.sov_is_arhivar_or_admin());
create policy speleo_overrides_update_arhivar_v557 on public.speleo_object_overrides for update to authenticated using (public.sov_is_arhivar_or_admin()) with check (public.sov_is_arhivar_or_admin());

drop policy if exists speleo_edits_select_arhivar_v557 on public.speleo_object_edits;
drop policy if exists speleo_edits_insert_arhivar_v557 on public.speleo_object_edits;
create policy speleo_edits_select_arhivar_v557 on public.speleo_object_edits for select to authenticated using (public.sov_is_arhivar_or_admin());
create policy speleo_edits_insert_arhivar_v557 on public.speleo_object_edits for insert to authenticated with check (public.sov_is_arhivar_or_admin());

drop policy if exists speleo_drawings_select_arhivar_v557 on public.speleo_object_drawings;
drop policy if exists speleo_drawings_insert_arhivar_v557 on public.speleo_object_drawings;
drop policy if exists speleo_drawings_update_arhivar_v557 on public.speleo_object_drawings;
create policy speleo_drawings_select_arhivar_v557 on public.speleo_object_drawings for select to authenticated using (coalesce(public_visible,true) = true or public.sov_is_arhivar_or_admin());
create policy speleo_drawings_insert_arhivar_v557 on public.speleo_object_drawings for insert to authenticated with check (public.sov_is_arhivar_or_admin());
create policy speleo_drawings_update_arhivar_v557 on public.speleo_object_drawings for update to authenticated using (public.sov_is_arhivar_or_admin()) with check (public.sov_is_arhivar_or_admin());

drop policy if exists speleo_reports_select_arhivar_v557 on public.speleo_activity_reports;
drop policy if exists speleo_reports_insert_arhivar_v557 on public.speleo_activity_reports;
create policy speleo_reports_select_arhivar_v557 on public.speleo_activity_reports for select to authenticated using (true);
create policy speleo_reports_insert_arhivar_v557 on public.speleo_activity_reports for insert to authenticated with check (public.sov_is_arhivar_or_admin());

-- Search/worklist view. This is intentionally based on speleo_objects_staging + manual status.
drop view if exists public.sov_arhivar_worklist cascade;
create view public.sov_arhivar_worklist as
with drawings as (
  select
    coalesce(nullif(object_id,''), lower(regexp_replace(coalesce(object_name,''),'\s+','_','g'))) as object_key,
    count(*)::integer as drawing_count,
    max(updated_at) as last_drawing_at
  from public.speleo_object_drawings
  group by 1
), reports as (
  select
    lower(regexp_replace(coalesce(object_name,''),'\s+','_','g')) as object_key,
    count(*)::integer as report_count,
    max(created_at) as last_report_at
  from public.speleo_activity_reports
  group by 1
), base as (
  select
    s.source_id as object_id,
    coalesce(nullif(o.data->>'name',''), s.name) as object_name,
    coalesce(nullif(o.data->>'plate_number',''), nullif(s.cadastral_number,''), nullif(s.raw->>'plate_number','')) as plate_number,
    coalesce(nullif(o.data->>'object_type',''), s.object_type_final) as object_type,
    coalesce(nullif(o.data->>'nearest_place',''), s.nearest_place, s.locality, s.municipality) as nearest_place,
    coalesce((nullif(o.data->>'lat',''))::double precision, s.lat) as lat,
    coalesce((nullif(o.data->>'lon',''))::double precision, s.lon) as lon,
    s.cadastre_status,
    s.record_status,
    s.county,
    s.municipality,
    s.updated_at
  from public.speleo_objects_staging s
  left join public.speleo_object_overrides o on o.object_id = s.source_id
), enriched as (
  select
    b.*,
    st.archive_status,
    st.priority,
    st.last_note,
    coalesce(st.has_coordinates, (b.lat is not null and b.lon is not null)) as has_coordinates,
    coalesce(st.has_drawing, coalesce(d.drawing_count,0) > 0) as has_drawing,
    coalesce(st.has_record, coalesce(r.report_count,0) > 0) as has_record,
    coalesce(d.drawing_count,0) as drawing_count,
    coalesce(r.report_count,0) as report_count,
    d.last_drawing_at,
    r.last_report_at
  from base b
  left join public.sov_archive_object_status st on st.object_id = b.object_id
  left join drawings d on d.object_key = b.object_id or d.object_key = lower(regexp_replace(coalesce(b.object_name,''),'\s+','_','g'))
  left join reports r on r.object_key = lower(regexp_replace(coalesce(b.object_name,''),'\s+','_','g'))
)
select
  *,
  (not has_coordinates) as missing_coordinates,
  (not has_drawing) as missing_drawing,
  (not has_record) as missing_record,
  case
    when has_coordinates and has_drawing and has_record then 'spremno_za_katastar'
    when not has_coordinates and not has_drawing and not has_record then 'kritično_fali_sve'
    else 'nepotpuno'
  end as katastar_readiness,
  (case when not has_coordinates then 30 else 0 end + case when not has_drawing then 25 else 0 end + case when not has_record then 25 else 0 end + case when coalesce(priority,'normal')='high' then 20 else 0 end)::integer as priority_score,
  lower(coalesce(object_name,'') || ' ' || coalesce(plate_number,'') || ' ' || coalesce(nearest_place,'') || ' ' || coalesce(object_type,'')) as search_text
from enriched;

drop view if exists public.sov_arhivar_dashboard cascade;
create view public.sov_arhivar_dashboard as
select
  count(*)::integer as total_objects,
  count(*) filter (where missing_coordinates)::integer as missing_coordinates,
  count(*) filter (where missing_drawing)::integer as missing_drawings,
  count(*) filter (where missing_record)::integer as missing_records,
  count(*) filter (where katastar_readiness = 'spremno_za_katastar')::integer as ready_for_katastar,
  count(*) filter (where katastar_readiness <> 'spremno_za_katastar')::integer as incomplete_objects,
  max(updated_at) as last_object_update
from public.sov_arhivar_worklist;

-- RPC used by web/APK to update checklist status in one safe call.
create or replace function public.sov_archive_update_object_status(
  p_object_id text,
  p_object_name text,
  p_plate_number text,
  p_has_coordinates boolean,
  p_has_drawing boolean,
  p_has_record boolean,
  p_archive_status text default 'needs_review',
  p_priority text default 'normal',
  p_note text default null
) returns public.sov_archive_object_status
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  row_out public.sov_archive_object_status;
begin
  if not public.sov_is_arhivar_or_admin() then
    raise exception 'Samo Admin/Arhivar može mijenjati status arhive.';
  end if;
  insert into public.sov_archive_object_status(
    object_id, object_name, plate_number, has_coordinates, has_drawing, has_record,
    has_katastar_ready, archive_status, priority, last_note, updated_by, updated_at
  ) values (
    p_object_id, p_object_name, p_plate_number, p_has_coordinates, p_has_drawing, p_has_record,
    coalesce(p_has_coordinates,false) and coalesce(p_has_drawing,false) and coalesce(p_has_record,false),
    coalesce(nullif(p_archive_status,''),'needs_review'),
    coalesce(nullif(p_priority,''),'normal'),
    p_note,
    auth.uid(), now()
  )
  on conflict (object_id) do update set
    object_name = excluded.object_name,
    plate_number = excluded.plate_number,
    has_coordinates = excluded.has_coordinates,
    has_drawing = excluded.has_drawing,
    has_record = excluded.has_record,
    has_katastar_ready = excluded.has_katastar_ready,
    archive_status = excluded.archive_status,
    priority = excluded.priority,
    last_note = excluded.last_note,
    updated_by = auth.uid(),
    updated_at = now()
  returning * into row_out;
  return row_out;
end $$;

-- Verification.
select 'sov_arhivar_worklist' as object, count(*) as rows from public.sov_arhivar_worklist
union all
select 'sov_archive_object_status', count(*) from public.sov_archive_object_status
union all
select 'speleo_object_drawings', count(*) from public.speleo_object_drawings
union all
select 'speleo_activity_reports', count(*) from public.speleo_activity_reports;
