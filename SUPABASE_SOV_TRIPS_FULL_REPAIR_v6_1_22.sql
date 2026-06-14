-- SOV Trips full repair v6.1.22
-- All-in-one repair for visible trips, save, multiday/category fields, and robust delete.
-- Use this instead of separate v6.1.20/v6.1.21 trip patches.

begin;

create extension if not exists pgcrypto;

-- 1) Base table and required columns. Existing rows are preserved.
create table if not exists public.sov_trips (
  id uuid primary key default gen_random_uuid(),
  start_date date,
  end_date date,
  title text,
  leader_name text,
  leader_user_id uuid,
  location_name text,
  objective text,
  description text,
  status text default 'planned',
  visibility text default 'club',
  trip_category text default 'Izlet',
  min_lat double precision,
  max_lat double precision,
  min_lon double precision,
  max_lon double precision,
  center_lat double precision,
  center_lon double precision,
  source text default 'web',
  legacy_sheet_name text,
  legacy_sheet_row integer,
  legacy_external_id text,
  last_synced_at timestamptz,
  created_by uuid,
  updated_by uuid,
  created_at timestamptz default now(),
  updated_at timestamptz default now(),
  meta jsonb default '{}'::jsonb
);

alter table public.sov_trips add column if not exists start_date date;
alter table public.sov_trips add column if not exists end_date date;
alter table public.sov_trips add column if not exists title text;
alter table public.sov_trips add column if not exists leader_name text;
alter table public.sov_trips add column if not exists leader_user_id uuid;
alter table public.sov_trips add column if not exists location_name text;
alter table public.sov_trips add column if not exists objective text;
alter table public.sov_trips add column if not exists description text;
alter table public.sov_trips add column if not exists status text default 'planned';
alter table public.sov_trips add column if not exists visibility text default 'club';
alter table public.sov_trips add column if not exists trip_category text default 'Izlet';
alter table public.sov_trips add column if not exists min_lat double precision;
alter table public.sov_trips add column if not exists max_lat double precision;
alter table public.sov_trips add column if not exists min_lon double precision;
alter table public.sov_trips add column if not exists max_lon double precision;
alter table public.sov_trips add column if not exists center_lat double precision;
alter table public.sov_trips add column if not exists center_lon double precision;
alter table public.sov_trips add column if not exists source text default 'web';
alter table public.sov_trips add column if not exists legacy_sheet_name text;
alter table public.sov_trips add column if not exists legacy_sheet_row integer;
alter table public.sov_trips add column if not exists legacy_external_id text;
alter table public.sov_trips add column if not exists last_synced_at timestamptz;
alter table public.sov_trips add column if not exists created_by uuid;
alter table public.sov_trips add column if not exists updated_by uuid;
alter table public.sov_trips add column if not exists created_at timestamptz default now();
alter table public.sov_trips add column if not exists updated_at timestamptz default now();
alter table public.sov_trips add column if not exists meta jsonb default '{}'::jsonb;
alter table public.sov_trips alter column meta set default '{}'::jsonb;
alter table public.sov_trips alter column status set default 'planned';
alter table public.sov_trips alter column visibility set default 'club';
alter table public.sov_trips alter column trip_category set default 'Izlet';

update public.sov_trips set end_date = start_date where end_date is null and start_date is not null;
update public.sov_trips set meta = '{}'::jsonb where meta is null;
update public.sov_trips set status = 'planned' where status is null or status = '';
update public.sov_trips set visibility = 'club' where visibility is null or visibility = '';
update public.sov_trips
set trip_category = coalesce(nullif(trip_category,''), meta->>'trip_category', 'Izlet')
where trip_category is null or trip_category = '';

-- 2) Child tables used by counts and delete cleanup.
create table if not exists public.sov_trip_files (
  id uuid primary key default gen_random_uuid(),
  trip_id uuid references public.sov_trips(id) on delete cascade,
  file_type text,
  file_name text,
  storage_bucket text,
  storage_path text,
  mime_type text,
  size_bytes bigint,
  meta jsonb default '{}'::jsonb,
  created_at timestamptz default now()
);
alter table public.sov_trip_files add column if not exists trip_id uuid references public.sov_trips(id) on delete cascade;
alter table public.sov_trip_files add column if not exists file_type text;
alter table public.sov_trip_files add column if not exists file_name text;
alter table public.sov_trip_files add column if not exists storage_bucket text;
alter table public.sov_trip_files add column if not exists storage_path text;
alter table public.sov_trip_files add column if not exists mime_type text;
alter table public.sov_trip_files add column if not exists size_bytes bigint;
alter table public.sov_trip_files add column if not exists meta jsonb default '{}'::jsonb;
alter table public.sov_trip_files add column if not exists created_at timestamptz default now();

create table if not exists public.sov_trip_members (
  id uuid primary key default gen_random_uuid(),
  trip_id uuid references public.sov_trips(id) on delete cascade,
  member_name text,
  member_email text,
  attendance_status text,
  transport_mode text,
  seats_available integer default 0,
  departure_place text,
  note text,
  created_at timestamptz default now()
);
alter table public.sov_trip_members add column if not exists trip_id uuid references public.sov_trips(id) on delete cascade;
alter table public.sov_trip_members add column if not exists member_name text;
alter table public.sov_trip_members add column if not exists member_email text;
alter table public.sov_trip_members add column if not exists attendance_status text;
alter table public.sov_trip_members add column if not exists transport_mode text;
alter table public.sov_trip_members add column if not exists seats_available integer default 0;
alter table public.sov_trip_members add column if not exists departure_place text;
alter table public.sov_trip_members add column if not exists note text;
alter table public.sov_trip_members add column if not exists created_at timestamptz default now();

-- 3) Role helpers.
create or replace function public.sov_role_slug(p_role text)
returns text
language sql
immutable
as $$
  select nullif(regexp_replace(lower(trim(coalesce(p_role,''))), '[^a-z0-9_]+', '', 'g'), '');
$$;

grant execute on function public.sov_role_slug(text) to anon, authenticated;

create or replace function public.sov_current_role_safe()
returns text
language plpgsql
stable
security definer
set search_path = public, auth
as $$
declare
  v_uid uuid := auth.uid();
  v_jwt jsonb := coalesce(auth.jwt(), '{}'::jsonb);
  v_role text;
  v_table text;
  v_schema text;
  v_name text;
  v_id_col text;
  v_role_col text;
begin
  v_role := public.sov_role_slug(coalesce(
    v_jwt -> 'app_metadata' ->> 'sov_role',
    v_jwt -> 'app_metadata' ->> 'app_role',
    v_jwt -> 'app_metadata' ->> 'role',
    v_jwt -> 'user_metadata' ->> 'sov_role',
    v_jwt -> 'user_metadata' ->> 'app_role',
    v_jwt -> 'user_metadata' ->> 'role'
  ));
  if v_role is not null and v_role not in ('authenticated','anon','service_role') then
    return v_role;
  end if;

  if v_uid is null then return null; end if;

  if to_regprocedure('public.sov_current_role()') is not null then
    begin
      execute 'select public.sov_current_role()::text' into v_role;
      v_role := public.sov_role_slug(v_role);
      if v_role is not null and v_role not in ('authenticated','anon','service_role') then
        return v_role;
      end if;
    exception when others then null;
    end;
  end if;

  foreach v_table in array array['public.profiles','public.sov_profiles','public.sov_user_profiles'] loop
    if to_regclass(v_table) is not null then
      v_schema := split_part(v_table,'.',1);
      v_name := split_part(v_table,'.',2);
      foreach v_id_col in array array['id','user_id','auth_user_id','uid'] loop
        if exists (select 1 from information_schema.columns where table_schema=v_schema and table_name=v_name and column_name=v_id_col) then
          foreach v_role_col in array array['role','app_role','sov_role','user_role','permissions_role'] loop
            if exists (select 1 from information_schema.columns where table_schema=v_schema and table_name=v_name and column_name=v_role_col) then
              begin
                execute format('select public.sov_role_slug(%I::text) from %s where %I = $1 limit 1', v_role_col, v_table, v_id_col)
                into v_role using v_uid;
                if v_role is not null and v_role not in ('authenticated','anon','service_role') then
                  return v_role;
                end if;
              exception when others then null;
              end;
            end if;
          end loop;
        end if;
      end loop;
    end if;
  end loop;

  return null;
end;
$$;

grant execute on function public.sov_current_role_safe() to anon, authenticated;

create or replace function public.sov_can_manage_trips_safe()
returns boolean
language plpgsql
stable
security definer
set search_path = public, auth
as $$
declare
  v_role text := public.sov_current_role_safe();
  v_can boolean := false;
begin
  if auth.uid() is null then return false; end if;

  if v_role in ('webmaster','admin','administrator','editor','urednik','arhivar','oruzar','voditelj') then
    return true;
  end if;

  if to_regclass('public.sov_role_permissions') is not null and v_role is not null then
    begin
      if exists (select 1 from information_schema.columns where table_schema='public' and table_name='sov_role_permissions' and column_name='can_manage_trips') then
        execute 'select coalesce(can_manage_trips,false) from public.sov_role_permissions where role = $1 limit 1'
        into v_can using v_role;
        if coalesce(v_can,false) then return true; end if;
      end if;
    exception when others then null;
    end;
  end if;

  return false;
end;
$$;

grant execute on function public.sov_can_manage_trips_safe() to authenticated;

-- 4) Feed RPC. This is what web/APK should use first. It is SECURITY DEFINER so RLS/view issues do not hide trips.
create or replace function public.sov_list_trips_feed()
returns jsonb
language plpgsql
stable
security definer
set search_path = public, auth
as $$
declare
  v_rows jsonb;
begin
  if auth.uid() is null then
    raise exception 'Nisi prijavljen.' using errcode = '42501';
  end if;

  select coalesce(jsonb_agg(to_jsonb(q) order by q.start_date nulls last, q.created_at nulls last), '[]'::jsonb)
  into v_rows
  from (
    select
      t.id,
      t.start_date,
      coalesce(t.end_date, t.start_date) as end_date,
      coalesce(t.title, trim(concat_ws(' · ', nullif(t.location_name,''), nullif(t.objective,''), t.start_date::text))) as title,
      t.leader_name,
      t.leader_user_id,
      t.location_name,
      t.objective,
      t.description,
      coalesce(t.status, 'planned') as status,
      coalesce(t.visibility, 'club') as visibility,
      coalesce(nullif(t.trip_category,''), t.meta->>'trip_category', 'Izlet') as trip_category,
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
      coalesce(t.source, 'web') as source,
      t.legacy_sheet_name,
      t.legacy_sheet_row,
      t.legacy_external_id,
      coalesce(files.file_count, 0) as file_count,
      coalesce(files.gpx_count, 0) as gpx_count,
      coalesce(files.kml_count, 0) as kml_count,
      coalesce(members.member_count, 0) as member_count,
      coalesce(t.meta, '{}'::jsonb) as meta,
      public.sov_can_manage_trips_safe() or t.created_by = auth.uid() or t.leader_user_id = auth.uid() as can_edit,
      public.sov_can_manage_trips_safe() as can_manage_all,
      case when coalesce(t.status,'planned') in ('planned','active') and coalesce(t.end_date, t.start_date) >= current_date - interval '7 days' then true else false end as is_relevant_now
    from public.sov_trips t
    left join lateral (
      select
        count(*)::int as file_count,
        count(*) filter (where f.file_type = 'gpx')::int as gpx_count,
        count(*) filter (where f.file_type in ('kml','kmz'))::int as kml_count
      from public.sov_trip_files f
      where f.trip_id = t.id
    ) files on true
    left join lateral (
      select count(*)::int as member_count
      from public.sov_trip_members m
      where m.trip_id = t.id
    ) members on true
    where coalesce(t.status,'planned') <> 'archived'
  ) q;

  return v_rows;
end;
$$;

grant execute on function public.sov_list_trips_feed() to authenticated;

-- 5) Save RPC.
create or replace function public.sov_save_trip(p_trip_id uuid, p_payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_uid uuid := auth.uid();
  v_existing public.sov_trips%rowtype;
  v_row public.sov_trips%rowtype;
  v_start date;
  v_end date;
  v_category text;
  v_meta jsonb;
begin
  if v_uid is null then
    raise exception 'Nisi prijavljen.' using errcode = '42501';
  end if;

  v_start := nullif(coalesce(p_payload->>'start_date', p_payload->>'startDate', p_payload->>'date'), '')::date;
  v_end := nullif(coalesce(p_payload->>'end_date', p_payload->>'endDate', p_payload->>'to'), '')::date;
  if v_end is null then v_end := v_start; end if;
  if v_start is not null and v_end is not null and v_end < v_start then v_end := v_start; end if;

  v_category := coalesce(nullif(p_payload->>'trip_category',''), nullif(p_payload->>'category',''), p_payload #>> '{meta,trip_category}', 'Izlet');
  if v_category not in ('Izlet','Seminar','Skup','Ekspedicija','Inventura','Skupština','Predavanje') then
    v_category := 'Izlet';
  end if;
  v_meta := coalesce(p_payload->'meta', '{}'::jsonb) || jsonb_build_object('trip_category', v_category);

  if p_trip_id is null then
    insert into public.sov_trips(
      start_date,end_date,title,leader_name,location_name,objective,description,status,visibility,
      trip_category,min_lat,max_lat,min_lon,max_lon,center_lat,center_lon,source,legacy_external_id,meta,
      created_by,updated_by,created_at,updated_at
    ) values (
      v_start,
      v_end,
      nullif(p_payload->>'title',''),
      nullif(p_payload->>'leader_name',''),
      nullif(p_payload->>'location_name',''),
      nullif(p_payload->>'objective',''),
      nullif(p_payload->>'description',''),
      coalesce(nullif(p_payload->>'status',''), 'planned'),
      coalesce(nullif(p_payload->>'visibility',''), 'club'),
      v_category,
      nullif(p_payload->>'min_lat','')::double precision,
      nullif(p_payload->>'max_lat','')::double precision,
      nullif(p_payload->>'min_lon','')::double precision,
      nullif(p_payload->>'max_lon','')::double precision,
      nullif(p_payload->>'center_lat','')::double precision,
      nullif(p_payload->>'center_lon','')::double precision,
      coalesce(nullif(p_payload->>'source',''), 'web'),
      nullif(p_payload->>'legacy_external_id',''),
      v_meta,
      v_uid,
      v_uid,
      now(),
      now()
    ) returning * into v_row;

    return to_jsonb(v_row);
  end if;

  select * into v_existing from public.sov_trips where id = p_trip_id;
  if not found then
    raise exception 'Izlet nije pronađen.' using errcode = 'P0002';
  end if;

  if not (public.sov_can_manage_trips_safe() or v_existing.created_by = v_uid or v_existing.leader_user_id = v_uid) then
    raise exception 'Nemaš ovlasti za uređivanje izleta.' using errcode = '42501';
  end if;

  update public.sov_trips
  set
    start_date = coalesce(v_start, start_date),
    end_date = coalesce(v_end, end_date, start_date),
    title = coalesce(nullif(p_payload->>'title',''), title),
    leader_name = coalesce(nullif(p_payload->>'leader_name',''), leader_name),
    location_name = coalesce(nullif(p_payload->>'location_name',''), location_name),
    objective = coalesce(nullif(p_payload->>'objective',''), objective),
    description = coalesce(p_payload->>'description', description),
    status = coalesce(nullif(p_payload->>'status',''), status),
    visibility = coalesce(nullif(p_payload->>'visibility',''), visibility),
    trip_category = v_category,
    min_lat = coalesce(nullif(p_payload->>'min_lat','')::double precision, min_lat),
    max_lat = coalesce(nullif(p_payload->>'max_lat','')::double precision, max_lat),
    min_lon = coalesce(nullif(p_payload->>'min_lon','')::double precision, min_lon),
    max_lon = coalesce(nullif(p_payload->>'max_lon','')::double precision, max_lon),
    center_lat = coalesce(nullif(p_payload->>'center_lat','')::double precision, center_lat),
    center_lon = coalesce(nullif(p_payload->>'center_lon','')::double precision, center_lon),
    meta = coalesce(meta, '{}'::jsonb) || v_meta,
    updated_by = v_uid,
    updated_at = now()
  where id = p_trip_id
  returning * into v_row;

  return to_jsonb(v_row);
end;
$$;

grant execute on function public.sov_save_trip(uuid, jsonb) to authenticated;

-- 6) Robust delete RPC.
create or replace function public.sov_try_delete_by_uuid(p_table regclass, p_column text, p_id uuid)
returns integer
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_schema text;
  v_table text;
  v_count integer := 0;
begin
  select n.nspname, c.relname into v_schema, v_table
  from pg_class c join pg_namespace n on n.oid = c.relnamespace
  where c.oid = p_table;

  if v_schema is null or v_table is null then return 0; end if;
  if not exists (select 1 from information_schema.columns where table_schema = v_schema and table_name = v_table and column_name = p_column) then return 0; end if;

  execute format('delete from %s where %I = $1', p_table, p_column) using p_id;
  get diagnostics v_count = row_count;
  return coalesce(v_count,0);
exception when undefined_table or undefined_column then
  return 0;
end;
$$;

revoke all on function public.sov_try_delete_by_uuid(regclass,text,uuid) from public;

create or replace function public.sov_delete_trip_admin(p_trip_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_deleted uuid;
  v_exists boolean;
  v_uid uuid := auth.uid();
  v_can boolean := false;
  v_child_deleted integer := 0;
  v_error text;
begin
  if v_uid is null then
    raise exception 'Nisi prijavljen.' using errcode = '42501';
  end if;

  select exists(select 1 from public.sov_trips where id = p_trip_id) into v_exists;
  if not v_exists then
    return jsonb_build_object('deleted', false, 'id', p_trip_id, 'message', 'Izlet nije pronađen u bazi.');
  end if;

  v_can := public.sov_can_manage_trips_safe();
  if not v_can then
    select exists(
      select 1 from public.sov_trips t
      where t.id = p_trip_id and (t.created_by = v_uid or t.leader_user_id = v_uid)
    ) into v_can;
  end if;

  if not v_can then
    raise exception 'Nemaš ovlasti za brisanje izleta.' using errcode = '42501';
  end if;

  if to_regclass('public.sov_tracking_points') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_tracking_points'::regclass, 'trip_id', p_trip_id); end if;
  if to_regclass('public.sov_tracking_sessions') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_tracking_sessions'::regclass, 'trip_id', p_trip_id); end if;
  if to_regclass('public.sov_tracking_trip_members') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_tracking_trip_members'::regclass, 'trip_id', p_trip_id); end if;
  if to_regclass('public.sov_tracking_field_events') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_tracking_field_events'::regclass, 'source_trip_id', p_trip_id); end if;
  if to_regclass('public.sov_trip_assets') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_assets'::regclass, 'trip_id', p_trip_id); end if;
  if to_regclass('public.sov_trip_files') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_files'::regclass, 'trip_id', p_trip_id); end if;
  if to_regclass('public.sov_trip_members') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_members'::regclass, 'trip_id', p_trip_id); end if;
  if to_regclass('public.sov_trip_tracks') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_tracks'::regclass, 'trip_id', p_trip_id); end if;
  if to_regclass('public.sov_trip_waypoints') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_waypoints'::regclass, 'trip_id', p_trip_id); end if;
  if to_regclass('public.sov_trip_sync_events') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_sync_events'::regclass, 'trip_id', p_trip_id); end if;
  if to_regclass('public.sov_trip_import_staging') is not null then v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_import_staging'::regclass, 'trip_id', p_trip_id); end if;

  begin
    delete from public.sov_trips where id = p_trip_id returning id into v_deleted;
  exception when others then
    get stacked diagnostics v_error = message_text;
    raise exception 'Izlet nije obrisan: %', v_error;
  end;

  if v_deleted is null then
    return jsonb_build_object('deleted', false, 'id', p_trip_id, 'message', 'Izlet nije obrisan.');
  end if;

  return jsonb_build_object('deleted', true, 'id', v_deleted, 'child_rows_deleted', v_child_deleted);
end;
$$;

grant execute on function public.sov_delete_trip_admin(uuid) to authenticated;

-- 7) Compatibility views for older web/APK paths.
drop view if exists public.sov_trips_mobile_feed cascade;
drop view if exists public.sov_trips_sheet_view cascade;

create or replace view public.sov_trips_sheet_view as
select
  t.id,
  t.start_date,
  coalesce(t.end_date, t.start_date) as end_date,
  t.title,
  t.leader_name,
  t.leader_user_id,
  t.location_name,
  t.objective,
  t.description,
  coalesce(t.status,'planned') as status,
  coalesce(t.visibility,'club') as visibility,
  coalesce(nullif(t.trip_category,''), t.meta->>'trip_category', 'Izlet') as trip_category,
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
  coalesce(t.source,'web') as source,
  t.legacy_sheet_name,
  t.legacy_sheet_row,
  t.legacy_external_id,
  coalesce(files.file_count, 0) as file_count,
  coalesce(files.gpx_count, 0) as gpx_count,
  coalesce(files.kml_count, 0) as kml_count,
  coalesce(members.member_count, 0) as member_count,
  coalesce(t.meta, '{}'::jsonb) as meta
from public.sov_trips t
left join lateral (
  select count(*)::int as file_count,
         count(*) filter (where file_type = 'gpx')::int as gpx_count,
         count(*) filter (where file_type in ('kml','kmz'))::int as kml_count
  from public.sov_trip_files f where f.trip_id = t.id
) files on true
left join lateral (
  select count(*)::int as member_count from public.sov_trip_members m where m.trip_id = t.id
) members on true;

create or replace view public.sov_trips_mobile_feed as
select
  v.*,
  public.sov_can_manage_trips_safe() or v.created_by = auth.uid() or v.leader_user_id = auth.uid() as can_edit,
  public.sov_can_manage_trips_safe() as can_manage_all,
  case when v.status in ('planned','active') and coalesce(v.end_date, v.start_date) >= current_date - interval '7 days' then true else false end as is_relevant_now
from public.sov_trips_sheet_view v
where coalesce(v.status,'planned') <> 'archived';

grant select on public.sov_trips_sheet_view to authenticated;
grant select on public.sov_trips_mobile_feed to authenticated;
grant select, insert, update, delete on public.sov_trips to authenticated;
grant select, insert, update, delete on public.sov_trip_files to authenticated;
grant select, insert, update, delete on public.sov_trip_members to authenticated;

commit;
