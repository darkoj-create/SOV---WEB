-- =============================================================
-- SOV TRIPS CLOUD v5.49 — ALL-IN-ONE SAFE SQL
-- Purpose:
--   Replace Google Sheets / Apps Script as the source-of-truth for Izleti.
--   Creates a Supabase-backed "sheet-like" trips model for Web + APK sync.
--
-- Safe design:
--   - Does NOT drop old tables.
--   - Does NOT touch existing SOV object database.
--   - Uses IF NOT EXISTS where possible.
--   - RLS stays ON.
--   - Existing Google Sheet can remain as backup/import source.
--
-- Recommended order:
--   1) Run this entire file once in Supabase SQL editor.
--   2) Run the verification queries at the bottom.
--   3) Only after green result, deploy Web/APK build that uses these tables.
-- =============================================================

begin;

-- -------------------------------------------------------------
-- 0. Extensions
-- -------------------------------------------------------------
create extension if not exists pgcrypto;

-- -------------------------------------------------------------
-- 1. Safe helper functions
-- -------------------------------------------------------------

-- Current app role. Uses sov_profiles if present, otherwise profiles.
-- Avoids hard dependency on one specific profile table shape.
create or replace function public.sov_app_role_safe()
returns text
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_role text;
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    return 'anon';
  end if;

  if to_regclass('public.sov_profiles') is not null then
    begin
      execute 'select role::text from public.sov_profiles where id = $1 limit 1'
      into v_role
      using v_uid;
    exception when others then
      v_role := null;
    end;
  end if;

  if v_role is null and to_regclass('public.profiles') is not null then
    begin
      execute 'select role::text from public.profiles where id = $1 limit 1'
      into v_role
      using v_uid;
    exception when others then
      v_role := null;
    end;
  end if;

  return lower(coalesce(v_role, 'user'));
end;
$$;

-- Generic role checker. Supports both Croatian and English role names.
create or replace function public.sov_has_any_role_safe(required_roles text[])
returns boolean
language plpgsql
stable
security definer
set search_path = public, auth
as $$
declare
  v_role text := public.sov_app_role_safe();
  normalized text[];
begin
  normalized := array(
    select lower(trim(x))
    from unnest(required_roles) as x
  );

  return v_role = any(normalized)
    or (v_role = 'urednik' and 'editor' = any(normalized))
    or (v_role = 'editor' and 'urednik' = any(normalized))
    or (v_role = 'oruzar' and 'oružar' = any(normalized))
    or (v_role = 'oružar' and 'oruzar' = any(normalized));
end;
$$;

create or replace function public.sov_can_manage_trips_safe()
returns boolean
language sql
stable
security definer
set search_path = public, auth
as $$
  select public.sov_has_any_role_safe(array['admin','editor','urednik']);
$$;

-- updated_at helper trigger
create or replace function public.sov_touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- -------------------------------------------------------------
-- 2. Canonical trips tables
-- -------------------------------------------------------------

create table if not exists public.sov_trips (
  id uuid primary key default gen_random_uuid(),

  -- Sheet-like fields
  title text,
  start_date date not null default current_date,
  end_date date,
  leader_name text,
  leader_user_id uuid references auth.users(id) on delete set null,
  location_name text,
  objective text,
  description text,

  -- State / visibility
  status text not null default 'planned'
    check (status in ('draft','planned','active','done','cancelled','archived')),
  visibility text not null default 'club'
    check (visibility in ('private','club','public')),

  -- Spatial envelope, optional
  min_lat double precision,
  max_lat double precision,
  min_lon double precision,
  max_lon double precision,
  center_lat double precision,
  center_lon double precision,

  -- Sync/import compatibility
  source text not null default 'supabase',
  legacy_sheet_name text,
  legacy_sheet_row integer,
  legacy_external_id text,
  import_batch_id uuid,

  -- Audit/meta
  created_by uuid references auth.users(id) on delete set null default auth.uid(),
  updated_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  last_synced_at timestamptz,

  -- Flexible app/web metadata without future migrations for tiny additions
  meta jsonb not null default '{}'::jsonb
);

create table if not exists public.sov_trip_members (
  id uuid primary key default gen_random_uuid(),
  trip_id uuid not null references public.sov_trips(id) on delete cascade,
  user_id uuid references auth.users(id) on delete set null,
  member_name text,
  member_email text,
  role text not null default 'participant'
    check (role in ('leader','co_leader','participant','guest','driver')),
  attendance_status text not null default 'planned'
    check (attendance_status in ('planned','confirmed','declined','maybe','cancelled')),
  created_by uuid references auth.users(id) on delete set null default auth.uid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  meta jsonb not null default '{}'::jsonb
);

create table if not exists public.sov_trip_files (
  id uuid primary key default gen_random_uuid(),
  trip_id uuid not null references public.sov_trips(id) on delete cascade,
  file_type text not null default 'other'
    check (file_type in ('gpx','kml','kmz','geojson','photo','pdf','zip','other')),
  file_name text not null,
  storage_bucket text not null default 'sov-trip-files',
  storage_path text,
  public_url text,
  mime_type text,
  size_bytes bigint,
  checksum text,

  -- Parsed metadata, optional
  min_lat double precision,
  max_lat double precision,
  min_lon double precision,
  max_lon double precision,
  distance_m double precision,
  elevation_gain_m double precision,
  duration_s integer,
  point_count integer,

  is_primary boolean not null default false,
  uploaded_by uuid references auth.users(id) on delete set null default auth.uid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  meta jsonb not null default '{}'::jsonb
);

create table if not exists public.sov_trip_waypoints (
  id uuid primary key default gen_random_uuid(),
  trip_id uuid not null references public.sov_trips(id) on delete cascade,
  file_id uuid references public.sov_trip_files(id) on delete set null,
  name text not null,
  description text,
  waypoint_type text not null default 'custom'
    check (waypoint_type in ('custom','object','danger','water','camp','entrance','parking','note','other')),
  lat double precision,
  lon double precision,
  elevation_m double precision,
  htrs_x double precision,
  htrs_y double precision,
  created_by uuid references auth.users(id) on delete set null default auth.uid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  meta jsonb not null default '{}'::jsonb
);

create table if not exists public.sov_trip_tracks (
  id uuid primary key default gen_random_uuid(),
  trip_id uuid not null references public.sov_trips(id) on delete cascade,
  file_id uuid references public.sov_trip_files(id) on delete set null,
  name text,
  track_type text not null default 'track'
    check (track_type in ('track','route','planned','recorded','imported','other')),
  distance_m double precision,
  elevation_gain_m double precision,
  duration_s integer,
  min_lat double precision,
  max_lat double precision,
  min_lon double precision,
  max_lon double precision,
  geometry_json jsonb,
  created_by uuid references auth.users(id) on delete set null default auth.uid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  meta jsonb not null default '{}'::jsonb
);

create table if not exists public.sov_trip_sync_events (
  id uuid primary key default gen_random_uuid(),
  entity_type text not null,
  entity_id uuid,
  action text not null,
  source text not null default 'web',
  device_id text,
  payload jsonb not null default '{}'::jsonb,
  status text not null default 'pending'
    check (status in ('pending','processed','failed','ignored')),
  error_message text,
  created_by uuid references auth.users(id) on delete set null default auth.uid(),
  created_at timestamptz not null default now(),
  processed_at timestamptz
);

-- Optional import staging table for existing Google Sheet rows.
create table if not exists public.sov_trip_import_staging (
  id uuid primary key default gen_random_uuid(),
  import_batch_id uuid not null default gen_random_uuid(),
  source_name text not null default 'google_sheet',
  source_row integer,
  raw_data jsonb not null default '{}'::jsonb,
  normalized_trip_id uuid references public.sov_trips(id) on delete set null,
  import_status text not null default 'pending'
    check (import_status in ('pending','imported','skipped','failed')),
  error_message text,
  created_at timestamptz not null default now(),
  imported_at timestamptz
);

-- -------------------------------------------------------------
-- 3. Indexes
-- -------------------------------------------------------------

create index if not exists idx_sov_trips_start_date on public.sov_trips(start_date desc);
create index if not exists idx_sov_trips_status on public.sov_trips(status);
create index if not exists idx_sov_trips_visibility on public.sov_trips(visibility);
create index if not exists idx_sov_trips_created_by on public.sov_trips(created_by);
create index if not exists idx_sov_trips_leader_user_id on public.sov_trips(leader_user_id);
create index if not exists idx_sov_trips_updated_at on public.sov_trips(updated_at desc);
create index if not exists idx_sov_trips_legacy_external_id on public.sov_trips(legacy_external_id) where legacy_external_id is not null;

create index if not exists idx_sov_trip_members_trip_id on public.sov_trip_members(trip_id);
create index if not exists idx_sov_trip_members_user_id on public.sov_trip_members(user_id);
create index if not exists idx_sov_trip_files_trip_id on public.sov_trip_files(trip_id);
create index if not exists idx_sov_trip_files_file_type on public.sov_trip_files(file_type);
create index if not exists idx_sov_trip_waypoints_trip_id on public.sov_trip_waypoints(trip_id);
create index if not exists idx_sov_trip_tracks_trip_id on public.sov_trip_tracks(trip_id);
create index if not exists idx_sov_trip_sync_events_status on public.sov_trip_sync_events(status, created_at desc);
create index if not exists idx_sov_trip_import_staging_batch on public.sov_trip_import_staging(import_batch_id);

-- -------------------------------------------------------------
-- 4. Triggers
-- -------------------------------------------------------------

drop trigger if exists trg_sov_trips_touch_updated_at on public.sov_trips;
create trigger trg_sov_trips_touch_updated_at
before update on public.sov_trips
for each row execute function public.sov_touch_updated_at();

drop trigger if exists trg_sov_trip_members_touch_updated_at on public.sov_trip_members;
create trigger trg_sov_trip_members_touch_updated_at
before update on public.sov_trip_members
for each row execute function public.sov_touch_updated_at();

drop trigger if exists trg_sov_trip_files_touch_updated_at on public.sov_trip_files;
create trigger trg_sov_trip_files_touch_updated_at
before update on public.sov_trip_files
for each row execute function public.sov_touch_updated_at();

drop trigger if exists trg_sov_trip_waypoints_touch_updated_at on public.sov_trip_waypoints;
create trigger trg_sov_trip_waypoints_touch_updated_at
before update on public.sov_trip_waypoints
for each row execute function public.sov_touch_updated_at();

drop trigger if exists trg_sov_trip_tracks_touch_updated_at on public.sov_trip_tracks;
create trigger trg_sov_trip_tracks_touch_updated_at
before update on public.sov_trip_tracks
for each row execute function public.sov_touch_updated_at();

-- -------------------------------------------------------------
-- 5. Permission helper functions for RLS
-- -------------------------------------------------------------

create or replace function public.sov_trip_can_read(p_trip_id uuid)
returns boolean
language plpgsql
stable
security definer
set search_path = public, auth
as $$
declare
  v_uid uuid := auth.uid();
  v_can_manage boolean := public.sov_can_manage_trips_safe();
  v_exists boolean;
begin
  select exists (
    select 1
    from public.sov_trips t
    where t.id = p_trip_id
      and (
        t.visibility = 'public'
        or (v_uid is not null and t.visibility = 'club')
        or t.created_by = v_uid
        or t.leader_user_id = v_uid
        or v_can_manage
        or exists (
          select 1 from public.sov_trip_members m
          where m.trip_id = t.id and m.user_id = v_uid
        )
      )
  ) into v_exists;

  return coalesce(v_exists, false);
end;
$$;

create or replace function public.sov_trip_can_edit(p_trip_id uuid)
returns boolean
language plpgsql
stable
security definer
set search_path = public, auth
as $$
declare
  v_uid uuid := auth.uid();
  v_can_manage boolean := public.sov_can_manage_trips_safe();
  v_exists boolean;
begin
  if v_uid is null then
    return false;
  end if;

  select exists (
    select 1
    from public.sov_trips t
    where t.id = p_trip_id
      and (
        v_can_manage
        or t.created_by = v_uid
        or t.leader_user_id = v_uid
        or exists (
          select 1 from public.sov_trip_members m
          where m.trip_id = t.id
            and m.user_id = v_uid
            and m.role in ('leader','co_leader')
        )
      )
  ) into v_exists;

  return coalesce(v_exists, false);
end;
$$;

-- -------------------------------------------------------------
-- 6. RLS policies
-- -------------------------------------------------------------

alter table public.sov_trips enable row level security;
alter table public.sov_trip_members enable row level security;
alter table public.sov_trip_files enable row level security;
alter table public.sov_trip_waypoints enable row level security;
alter table public.sov_trip_tracks enable row level security;
alter table public.sov_trip_sync_events enable row level security;
alter table public.sov_trip_import_staging enable row level security;

-- Trips
DROP POLICY IF EXISTS sov_trips_select ON public.sov_trips;
CREATE POLICY sov_trips_select ON public.sov_trips
FOR SELECT
USING (
  visibility = 'public'
  OR (auth.uid() IS NOT NULL AND visibility = 'club')
  OR created_by = auth.uid()
  OR leader_user_id = auth.uid()
  OR public.sov_can_manage_trips_safe()
  OR EXISTS (
    SELECT 1 FROM public.sov_trip_members m
    WHERE m.trip_id = id AND m.user_id = auth.uid()
  )
);

DROP POLICY IF EXISTS sov_trips_insert ON public.sov_trips;
CREATE POLICY sov_trips_insert ON public.sov_trips
FOR INSERT
WITH CHECK (
  auth.uid() IS NOT NULL
  AND (
    created_by = auth.uid()
    OR created_by IS NULL
    OR public.sov_can_manage_trips_safe()
  )
);

DROP POLICY IF EXISTS sov_trips_update ON public.sov_trips;
CREATE POLICY sov_trips_update ON public.sov_trips
FOR UPDATE
USING (
  public.sov_can_manage_trips_safe()
  OR created_by = auth.uid()
  OR leader_user_id = auth.uid()
)
WITH CHECK (
  public.sov_can_manage_trips_safe()
  OR created_by = auth.uid()
  OR leader_user_id = auth.uid()
);

DROP POLICY IF EXISTS sov_trips_delete ON public.sov_trips;
CREATE POLICY sov_trips_delete ON public.sov_trips
FOR DELETE
USING (
  public.sov_can_manage_trips_safe()
  OR (created_by = auth.uid() AND status IN ('draft','cancelled'))
);

-- Child tables read/edit through parent trip.
DROP POLICY IF EXISTS sov_trip_members_select ON public.sov_trip_members;
CREATE POLICY sov_trip_members_select ON public.sov_trip_members
FOR SELECT USING (public.sov_trip_can_read(trip_id));

DROP POLICY IF EXISTS sov_trip_members_insert ON public.sov_trip_members;
CREATE POLICY sov_trip_members_insert ON public.sov_trip_members
FOR INSERT WITH CHECK (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_members_update ON public.sov_trip_members;
CREATE POLICY sov_trip_members_update ON public.sov_trip_members
FOR UPDATE USING (public.sov_trip_can_edit(trip_id))
WITH CHECK (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_members_delete ON public.sov_trip_members;
CREATE POLICY sov_trip_members_delete ON public.sov_trip_members
FOR DELETE USING (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_files_select ON public.sov_trip_files;
CREATE POLICY sov_trip_files_select ON public.sov_trip_files
FOR SELECT USING (public.sov_trip_can_read(trip_id));

DROP POLICY IF EXISTS sov_trip_files_insert ON public.sov_trip_files;
CREATE POLICY sov_trip_files_insert ON public.sov_trip_files
FOR INSERT WITH CHECK (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_files_update ON public.sov_trip_files;
CREATE POLICY sov_trip_files_update ON public.sov_trip_files
FOR UPDATE USING (public.sov_trip_can_edit(trip_id))
WITH CHECK (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_files_delete ON public.sov_trip_files;
CREATE POLICY sov_trip_files_delete ON public.sov_trip_files
FOR DELETE USING (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_waypoints_select ON public.sov_trip_waypoints;
CREATE POLICY sov_trip_waypoints_select ON public.sov_trip_waypoints
FOR SELECT USING (public.sov_trip_can_read(trip_id));

DROP POLICY IF EXISTS sov_trip_waypoints_insert ON public.sov_trip_waypoints;
CREATE POLICY sov_trip_waypoints_insert ON public.sov_trip_waypoints
FOR INSERT WITH CHECK (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_waypoints_update ON public.sov_trip_waypoints;
CREATE POLICY sov_trip_waypoints_update ON public.sov_trip_waypoints
FOR UPDATE USING (public.sov_trip_can_edit(trip_id))
WITH CHECK (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_waypoints_delete ON public.sov_trip_waypoints;
CREATE POLICY sov_trip_waypoints_delete ON public.sov_trip_waypoints
FOR DELETE USING (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_tracks_select ON public.sov_trip_tracks;
CREATE POLICY sov_trip_tracks_select ON public.sov_trip_tracks
FOR SELECT USING (public.sov_trip_can_read(trip_id));

DROP POLICY IF EXISTS sov_trip_tracks_insert ON public.sov_trip_tracks;
CREATE POLICY sov_trip_tracks_insert ON public.sov_trip_tracks
FOR INSERT WITH CHECK (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_tracks_update ON public.sov_trip_tracks;
CREATE POLICY sov_trip_tracks_update ON public.sov_trip_tracks
FOR UPDATE USING (public.sov_trip_can_edit(trip_id))
WITH CHECK (public.sov_trip_can_edit(trip_id));

DROP POLICY IF EXISTS sov_trip_tracks_delete ON public.sov_trip_tracks;
CREATE POLICY sov_trip_tracks_delete ON public.sov_trip_tracks
FOR DELETE USING (public.sov_trip_can_edit(trip_id));

-- Sync events: user sees own events, admins/editors see all.
DROP POLICY IF EXISTS sov_trip_sync_events_select ON public.sov_trip_sync_events;
CREATE POLICY sov_trip_sync_events_select ON public.sov_trip_sync_events
FOR SELECT USING (created_by = auth.uid() OR public.sov_can_manage_trips_safe());

DROP POLICY IF EXISTS sov_trip_sync_events_insert ON public.sov_trip_sync_events;
CREATE POLICY sov_trip_sync_events_insert ON public.sov_trip_sync_events
FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS sov_trip_sync_events_update ON public.sov_trip_sync_events;
CREATE POLICY sov_trip_sync_events_update ON public.sov_trip_sync_events
FOR UPDATE USING (public.sov_can_manage_trips_safe())
WITH CHECK (public.sov_can_manage_trips_safe());

-- Import staging: admin/editor only.
DROP POLICY IF EXISTS sov_trip_import_staging_all ON public.sov_trip_import_staging;
CREATE POLICY sov_trip_import_staging_all ON public.sov_trip_import_staging
FOR ALL USING (public.sov_can_manage_trips_safe())
WITH CHECK (public.sov_can_manage_trips_safe());

-- -------------------------------------------------------------
-- 7. Sheet-like and mobile views
-- -------------------------------------------------------------

create or replace view public.sov_trips_sheet_view as
select
  t.id,
  t.start_date,
  coalesce(t.end_date, t.start_date) as end_date,
  t.leader_name,
  t.leader_user_id,
  t.location_name,
  t.objective,
  t.description,
  coalesce(t.title, trim(concat_ws(' · ', t.location_name, t.objective, t.start_date::text))) as title,
  t.status,
  t.visibility,
  t.min_lat,
  t.max_lat,
  t.min_lon,
  t.max_lon,
  t.center_lat,
  t.center_lon,
  t.created_by,
  t.updated_by,
  t.created_at,
  t.updated_at,
  t.last_synced_at,
  t.source,
  t.legacy_sheet_name,
  t.legacy_sheet_row,
  t.legacy_external_id,
  coalesce(files.file_count, 0) as file_count,
  coalesce(files.gpx_count, 0) as gpx_count,
  coalesce(files.kml_count, 0) as kml_count,
  coalesce(members.member_count, 0) as member_count,
  t.meta
from public.sov_trips t
left join lateral (
  select
    count(*)::int as file_count,
    count(*) filter (where file_type = 'gpx')::int as gpx_count,
    count(*) filter (where file_type in ('kml','kmz'))::int as kml_count
  from public.sov_trip_files f
  where f.trip_id = t.id
) files on true
left join lateral (
  select count(*)::int as member_count
  from public.sov_trip_members m
  where m.trip_id = t.id
) members on true;

create or replace view public.sov_trips_mobile_feed as
select
  v.*,
  public.sov_trip_can_edit(v.id) as can_edit,
  public.sov_can_manage_trips_safe() as can_manage_all,
  case
    when v.status in ('planned','active') and v.end_date >= current_date - interval '7 days' then true
    else false
  end as is_relevant_now
from public.sov_trips_sheet_view v
where v.status <> 'archived';

create or replace view public.sov_trip_file_list as
select
  f.id,
  f.trip_id,
  f.file_type,
  f.file_name,
  f.storage_bucket,
  f.storage_path,
  f.public_url,
  f.mime_type,
  f.size_bytes,
  f.distance_m,
  f.elevation_gain_m,
  f.duration_s,
  f.point_count,
  f.is_primary,
  f.uploaded_by,
  f.created_at,
  f.updated_at,
  f.meta
from public.sov_trip_files f;

-- Manifest for fast sync. Small enough for APK/web to check first.
create or replace view public.sov_trips_sync_manifest as
select
  'sov_trips_cloud_v5_49'::text as manifest_name,
  coalesce(max(updated_at), '1970-01-01'::timestamptz) as last_changed_at,
  count(*)::int as trip_count,
  count(*) filter (where status in ('planned','active'))::int as active_trip_count,
  count(*) filter (where created_at >= now() - interval '30 days')::int as new_last_30d,
  md5(
    coalesce(count(*)::text, '0') || ':' ||
    coalesce(extract(epoch from max(updated_at))::bigint::text, '0')
  ) as checksum
from public.sov_trips;

-- -------------------------------------------------------------
-- 8. RPC functions for APK/Web convenience
-- -------------------------------------------------------------

-- Upsert a trip from app/web with one call.
create or replace function public.sov_upsert_trip(
  p_id uuid,
  p_start_date date,
  p_end_date date,
  p_leader_name text,
  p_location_name text,
  p_objective text,
  p_description text,
  p_status text default 'planned',
  p_visibility text default 'club',
  p_meta jsonb default '{}'::jsonb
)
returns public.sov_trips
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_trip public.sov_trips;
  v_id uuid := coalesce(p_id, gen_random_uuid());
begin
  if auth.uid() is null then
    raise exception 'Not authenticated';
  end if;

  if p_id is not null and not public.sov_trip_can_edit(p_id) then
    raise exception 'No permission to edit this trip';
  end if;

  insert into public.sov_trips (
    id,
    start_date,
    end_date,
    leader_name,
    leader_user_id,
    location_name,
    objective,
    description,
    title,
    status,
    visibility,
    created_by,
    updated_by,
    meta,
    last_synced_at
  ) values (
    v_id,
    p_start_date,
    p_end_date,
    p_leader_name,
    case when lower(coalesce(p_leader_name,'')) in ('me','ja') then auth.uid() else null end,
    p_location_name,
    p_objective,
    p_description,
    trim(concat_ws(' · ', p_location_name, p_objective, p_start_date::text)),
    coalesce(nullif(p_status,''), 'planned'),
    coalesce(nullif(p_visibility,''), 'club'),
    auth.uid(),
    auth.uid(),
    coalesce(p_meta, '{}'::jsonb),
    now()
  )
  on conflict (id) do update set
    start_date = excluded.start_date,
    end_date = excluded.end_date,
    leader_name = excluded.leader_name,
    location_name = excluded.location_name,
    objective = excluded.objective,
    description = excluded.description,
    title = excluded.title,
    status = excluded.status,
    visibility = excluded.visibility,
    updated_by = auth.uid(),
    meta = public.sov_trips.meta || excluded.meta,
    last_synced_at = now(),
    updated_at = now()
  returning * into v_trip;

  insert into public.sov_trip_sync_events(entity_type, entity_id, action, source, payload, status, created_by, processed_at)
  values ('trip', v_trip.id, case when p_id is null then 'create' else 'upsert' end, 'rpc', to_jsonb(v_trip), 'processed', auth.uid(), now());

  return v_trip;
end;
$$;

-- Register file metadata after upload to Supabase Storage or external URL.
create or replace function public.sov_add_trip_file(
  p_trip_id uuid,
  p_file_type text,
  p_file_name text,
  p_storage_path text default null,
  p_public_url text default null,
  p_meta jsonb default '{}'::jsonb
)
returns public.sov_trip_files
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_file public.sov_trip_files;
begin
  if not public.sov_trip_can_edit(p_trip_id) then
    raise exception 'No permission to add file to this trip';
  end if;

  insert into public.sov_trip_files(
    trip_id,
    file_type,
    file_name,
    storage_path,
    public_url,
    uploaded_by,
    meta
  ) values (
    p_trip_id,
    coalesce(nullif(p_file_type,''), 'other'),
    p_file_name,
    p_storage_path,
    p_public_url,
    auth.uid(),
    coalesce(p_meta, '{}'::jsonb)
  ) returning * into v_file;

  insert into public.sov_trip_sync_events(entity_type, entity_id, action, source, payload, status, created_by, processed_at)
  values ('trip_file', v_file.id, 'create', 'rpc', to_jsonb(v_file), 'processed', auth.uid(), now());

  return v_file;
end;
$$;

-- -------------------------------------------------------------
-- 9. Optional Supabase Storage bucket setup
-- -------------------------------------------------------------

do $$
begin
  if to_regclass('storage.buckets') is not null then
    execute $q$
      insert into storage.buckets (id, name, public)
      values ('sov-trip-files', 'sov-trip-files', false)
      on conflict (id) do nothing
    $q$;
  end if;
end;
$$;

-- Storage policies are optional and guarded by dynamic SQL.
do $$
begin
  if to_regclass('storage.objects') is not null then
    execute $q$
      drop policy if exists sov_trip_files_storage_select on storage.objects;
    $q$;
    execute $q$
      create policy sov_trip_files_storage_select on storage.objects
      for select using (
        bucket_id = 'sov-trip-files'
        and auth.uid() is not null
      );
    $q$;

    execute $q$
      drop policy if exists sov_trip_files_storage_insert on storage.objects;
    $q$;
    execute $q$
      create policy sov_trip_files_storage_insert on storage.objects
      for insert with check (
        bucket_id = 'sov-trip-files'
        and auth.uid() is not null
      );
    $q$;

    execute $q$
      drop policy if exists sov_trip_files_storage_update on storage.objects;
    $q$;
    execute $q$
      create policy sov_trip_files_storage_update on storage.objects
      for update using (
        bucket_id = 'sov-trip-files'
        and auth.uid() is not null
      ) with check (
        bucket_id = 'sov-trip-files'
        and auth.uid() is not null
      );
    $q$;
  end if;
end;
$$;

-- -------------------------------------------------------------
-- 10. Grants
-- -------------------------------------------------------------

grant usage on schema public to anon, authenticated;

grant select on public.sov_trips_sheet_view to authenticated;
grant select on public.sov_trips_mobile_feed to authenticated;
grant select on public.sov_trip_file_list to authenticated;
grant select on public.sov_trips_sync_manifest to authenticated;

grant select, insert, update, delete on public.sov_trips to authenticated;
grant select, insert, update, delete on public.sov_trip_members to authenticated;
grant select, insert, update, delete on public.sov_trip_files to authenticated;
grant select, insert, update, delete on public.sov_trip_waypoints to authenticated;
grant select, insert, update, delete on public.sov_trip_tracks to authenticated;
grant select, insert on public.sov_trip_sync_events to authenticated;
grant execute on function public.sov_upsert_trip(uuid, date, date, text, text, text, text, text, text, jsonb) to authenticated;
grant execute on function public.sov_add_trip_file(uuid, text, text, text, text, jsonb) to authenticated;

commit;

-- =============================================================
-- VERIFICATION QUERIES — run after migration
-- =============================================================

-- 1) Table presence
select
  table_name
from information_schema.tables
where table_schema = 'public'
  and table_name in (
    'sov_trips',
    'sov_trip_members',
    'sov_trip_files',
    'sov_trip_waypoints',
    'sov_trip_tracks',
    'sov_trip_sync_events',
    'sov_trip_import_staging'
  )
order by table_name;

-- 2) Fast manifest
select * from public.sov_trips_sync_manifest;

-- 3) Empty sheet view should work even before import
select count(*) as trips_in_sheet_view from public.sov_trips_sheet_view;

-- 4) RLS quick check helper
select public.sov_app_role_safe() as current_role;
