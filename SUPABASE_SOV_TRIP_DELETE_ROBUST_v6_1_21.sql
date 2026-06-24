-- SOV Trips delete robust fix v6.1.21
-- Fixes cases where delete RPC exists but trip stays because related rows/FKs or role detection block the delete.
-- Run after v6.1.20 hard repair.

begin;

-- More robust role normalization/current role helpers. Safe if already present.
create or replace function public.sov_role_slug(p_role text)
returns text
language sql
immutable
as $$
  select nullif(
    regexp_replace(
      lower(trim(coalesce(p_role,''))),
      '[^a-z0-9_]+', '', 'g'
    ),
    ''
  );
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
  -- 1) JWT app/user metadata. Ignore top-level Supabase role=authenticated.
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

  if v_uid is null then
    return null;
  end if;

  -- 2) Existing canonical helper if present.
  if to_regprocedure('public.sov_current_role()') is not null then
    begin
      execute 'select public.sov_current_role()::text' into v_role;
      v_role := public.sov_role_slug(v_role);
      if v_role is not null and v_role not in ('authenticated','anon','service_role') then
        return v_role;
      end if;
    exception when others then
      null;
    end;
  end if;

  -- 3) Known profile tables, flexible id/role columns.
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
              exception when others then
                null;
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
  if auth.uid() is null then
    return false;
  end if;

  if v_role in ('webmaster','admin','administrator','editor','urednik','arhivar','oruzar','voditelj') then
    return true;
  end if;

  -- Ecosystem permissions table, if present.
  if to_regclass('public.sov_role_permissions') is not null and v_role is not null then
    begin
      if exists (select 1 from information_schema.columns where table_schema='public' and table_name='sov_role_permissions' and column_name='can_manage_trips') then
        execute 'select coalesce(can_manage_trips,false) from public.sov_role_permissions where role = $1 limit 1'
        into v_can using v_role;
        if coalesce(v_can,false) then
          return true;
        end if;
      end if;
    exception when others then
      null;
    end;
  end if;

  return false;
end;
$$;

grant execute on function public.sov_can_manage_trips_safe() to authenticated;

-- Tiny internal helper: delete rows from table if table and column exist.
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

  if v_schema is null or v_table is null then
    return 0;
  end if;

  if not exists (
    select 1 from information_schema.columns
    where table_schema = v_schema and table_name = v_table and column_name = p_column
  ) then
    return 0;
  end if;

  execute format('delete from %s where %I = $1', p_table, p_column) using p_id;
  get diagnostics v_count = row_count;
  return coalesce(v_count,0);
exception when undefined_table or undefined_column then
  return 0;
end;
$$;

revoke all on function public.sov_try_delete_by_uuid(regclass,text,uuid) from public;

-- Robust delete RPC used by web and APK.
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

  -- Allow creator/leader to delete their own trip even if role helper is stale.
  if not v_can then
    begin
      select exists(
        select 1 from public.sov_trips t
        where t.id = p_trip_id
          and (
            (exists(select 1 from information_schema.columns where table_schema='public' and table_name='sov_trips' and column_name='created_by') and t.created_by = v_uid)
            or (exists(select 1 from information_schema.columns where table_schema='public' and table_name='sov_trips' and column_name='leader_user_id') and t.leader_user_id = v_uid)
          )
      ) into v_can;
    exception when others then
      v_can := false;
    end;
  end if;

  if not v_can then
    raise exception 'Nemaš ovlasti za brisanje izleta.' using errcode = '42501';
  end if;

  -- Explicitly remove dependent rows before deleting the trip. This fixes old FKs
  -- that were created without ON DELETE CASCADE.
  if to_regclass('public.sov_tracking_points') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_tracking_points'::regclass, 'trip_id', p_trip_id);
  end if;
  if to_regclass('public.sov_tracking_sessions') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_tracking_sessions'::regclass, 'trip_id', p_trip_id);
  end if;
  if to_regclass('public.sov_tracking_trip_members') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_tracking_trip_members'::regclass, 'trip_id', p_trip_id);
  end if;
  if to_regclass('public.sov_tracking_field_events') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_tracking_field_events'::regclass, 'source_trip_id', p_trip_id);
  end if;
  if to_regclass('public.sov_trip_assets') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_assets'::regclass, 'trip_id', p_trip_id);
  end if;
  if to_regclass('public.sov_trip_files') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_files'::regclass, 'trip_id', p_trip_id);
  end if;
  if to_regclass('public.sov_trip_members') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_members'::regclass, 'trip_id', p_trip_id);
  end if;
  if to_regclass('public.sov_trip_tracks') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_tracks'::regclass, 'trip_id', p_trip_id);
  end if;
  if to_regclass('public.sov_trip_waypoints') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_waypoints'::regclass, 'trip_id', p_trip_id);
  end if;
  if to_regclass('public.sov_trip_sync_events') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_sync_events'::regclass, 'trip_id', p_trip_id);
  end if;
  if to_regclass('public.sov_trip_import_staging') is not null then
    v_child_deleted := v_child_deleted + public.sov_try_delete_by_uuid('public.sov_trip_import_staging'::regclass, 'trip_id', p_trip_id);
  end if;

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

commit;
