-- SOV News CMS v5.58.11
-- Fix: editor/admin role detection for news save RPC.
-- Cause fixed: Supabase JWT top-level "role" can be "authenticated" and must NOT override profiles.role.

begin;

create extension if not exists unaccent;

-- Normalize app roles used by SOV. Returns NULL for Supabase system roles.
create or replace function public.sov_role_slug(p_role text)
returns text
language sql
immutable
as $$
  select nullif(
    regexp_replace(
      lower(unaccent(trim(coalesce(p_role,'')))),
      '[^a-z0-9_\-]+',
      '',
      'g'
    ),
    ''
  )
$$;

grant execute on function public.sov_role_slug(text) to anon, authenticated;

-- Current SOV app role. Important: ignore auth.jwt()->>'role' because that is normally
-- Supabase's system role: authenticated / anon / service_role.
create or replace function public.sov_current_role()
returns text
language plpgsql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_uid uuid := auth.uid();
  v_role text;
  v_jwt jsonb := auth.jwt();
  v_id_col text;
  v_role_col text;
begin
  if v_uid is null then
    return 'anon';
  end if;

  -- Only app/user metadata roles are app roles. Do not use top-level JWT role.
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

  -- Profiles fallback. Supports common SOV schema variants.
  if to_regclass('public.profiles') is not null then
    v_id_col := null;
    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='id') then
      v_id_col := 'id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='user_id') then
      v_id_col := 'user_id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='auth_user_id') then
      v_id_col := 'auth_user_id';
    end if;

    if v_id_col is not null then
      foreach v_role_col in array array['role','app_role','sov_role','user_role','permissions_role'] loop
        if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name=v_role_col) then
          begin
            execute format('select public.sov_role_slug(%I::text) from public.profiles where %I = $1 limit 1', v_role_col, v_id_col)
              into v_role using v_uid;
            if v_role is not null and v_role not in ('authenticated','anon','service_role') then
              return v_role;
            end if;
          exception when others then
            -- Try next column.
          end;
        end if;
      end loop;
    end if;
  end if;

  return 'user';
exception when others then
  return 'user';
end;
$$;

-- Keep status helper but make it resilient. Status is informational for news edit now;
-- we do not block editor/admin solely because a legacy status column is missing/null.
create or replace function public.sov_current_status()
returns text
language plpgsql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_uid uuid := auth.uid();
  v_status text;
  v_jwt jsonb := auth.jwt();
  v_id_col text;
  v_status_col text;
begin
  if v_uid is null then
    return 'anon';
  end if;

  v_status := public.sov_role_slug(coalesce(
    v_jwt -> 'app_metadata' ->> 'status',
    v_jwt -> 'user_metadata' ->> 'status'
  ));
  if v_status is not null then
    return v_status;
  end if;

  if to_regclass('public.profiles') is not null then
    v_id_col := null;
    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='id') then
      v_id_col := 'id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='user_id') then
      v_id_col := 'user_id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='auth_user_id') then
      v_id_col := 'auth_user_id';
    end if;

    if v_id_col is not null then
      foreach v_status_col in array array['status','account_status','approval_status','user_status'] loop
        if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name=v_status_col) then
          begin
            execute format('select public.sov_role_slug(%I::text) from public.profiles where %I = $1 limit 1', v_status_col, v_id_col)
              into v_status using v_uid;
            if v_status is not null then
              return v_status;
            end if;
          exception when others then
            -- Try next column.
          end;
        end if;
      end loop;
    end if;
  end if;

  return 'unknown';
exception when others then
  return 'unknown';
end;
$$;

-- Permission helper used by news policies/RPCs.
create or replace function public.sov_has_permission(p_permission text)
returns boolean
language plpgsql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_uid uuid := auth.uid();
  v_role text := public.sov_current_role();
  v_can boolean := false;
  v_id_col text;
  v_sql text;
begin
  if v_uid is null then
    return false;
  end if;

  if v_role = 'admin' then
    return true;
  end if;

  -- Role permissions table, if the ecosystem role SQL exists.
  if to_regclass('public.sov_role_permissions') is not null then
    begin
      execute format(
        'select coalesce(%I,false) from public.sov_role_permissions where role = $1 limit 1',
        case p_permission
          when 'edit_news' then 'can_edit_news'
          when 'manage_users' then 'can_manage_users'
          when 'manage_equipment' then 'can_manage_equipment'
          when 'edit_objects' then 'can_edit_objects'
          when 'verify_drawings' then 'can_verify_drawings'
          when 'use_sql_tools' then 'can_use_sql_tools'
          else 'can_edit_news'
        end
      ) into v_can using v_role;
      if coalesce(v_can,false) then return true; end if;
    exception when others then
      null;
    end;
  end if;

  -- Direct boolean permission columns on profiles, if present.
  if to_regclass('public.profiles') is not null then
    v_id_col := null;
    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='id') then
      v_id_col := 'id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='user_id') then
      v_id_col := 'user_id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='auth_user_id') then
      v_id_col := 'auth_user_id';
    end if;

    if v_id_col is not null then
      if p_permission = 'edit_news' and exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='can_edit_news') then
        execute format('select coalesce(can_edit_news,false) from public.profiles where %I = $1 limit 1', v_id_col) into v_can using v_uid;
        if coalesce(v_can,false) then return true; end if;
      end if;
      if p_permission = 'manage_users' and exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='can_manage_users') then
        execute format('select coalesce(can_manage_users,false) from public.profiles where %I = $1 limit 1', v_id_col) into v_can using v_uid;
        if coalesce(v_can,false) then return true; end if;
      end if;
    end if;
  end if;

  return false;
exception when others then
  return false;
end;
$$;

-- News editor gate. Do NOT block editor/admin because legacy status is null/pending.
-- Only explicit negative statuses block.
create or replace function public.sov_can_edit_news()
returns boolean
language sql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
  select auth.uid() is not null
     and public.sov_current_status() not in ('blocked','disabled','rejected','banned','suspended','deleted')
     and (
       public.sov_current_role() in ('admin','editor','urednik','uredjnik','newseditor','news_editor','urednikvijesti','urednik_vijesti')
       or public.sov_has_permission('edit_news')
       or public.sov_has_permission('manage_users')
     );
$$;

grant execute on function public.sov_current_role() to anon, authenticated;
grant execute on function public.sov_current_status() to anon, authenticated;
grant execute on function public.sov_has_permission(text) to anon, authenticated;
grant execute on function public.sov_can_edit_news() to anon, authenticated;

-- Tiny debug RPC for sync-status / manual SQL checks.
create or replace function public.sov_news_auth_debug()
returns jsonb
language sql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
  select jsonb_build_object(
    'uid', auth.uid(),
    'jwt_system_role', auth.jwt() ->> 'role',
    'sov_role', public.sov_current_role(),
    'sov_status', public.sov_current_status(),
    'can_edit_news', public.sov_can_edit_news(),
    'has_edit_news_permission', public.sov_has_permission('edit_news'),
    'fixed_in', 'v5.58.11'
  );
$$;

grant execute on function public.sov_news_auth_debug() to anon, authenticated;

-- Make sure editor/update policies still point at the fixed helper.
alter table if exists public.sov_news enable row level security;

do $$
begin
  if to_regclass('public.sov_news') is not null then
    drop policy if exists "sov_news editor select all v55811" on public.sov_news;
    create policy "sov_news editor select all v55811"
      on public.sov_news for select
      to authenticated
      using (public.sov_can_edit_news() or published = true);
  end if;
exception when duplicate_object then
  null;
end $$;

commit;
