-- SOV Roles v5.58.13
-- Hotfix for PostgreSQL 42P13: cannot change name of input parameter on sov_has_permission(text).
-- Supersedes v5.58.11 and v5.58.12 SQL. Safe to run after failed v5.58.11.
-- Key point: drop sov_has_permission(text) first, because older SOV role SQL used parameter name "permission_name".
-- The function is then recreated with the same stable parameter name and the Webmaster/Admin split remains active.

-- Compatibility cleanup: dropping this function with CASCADE removes dependent policies/functions created by partial prior attempts.
-- They are recreated below and in the postflight section.
do $$
begin
  if to_regprocedure('public.sov_has_permission(text)') is not null then
    execute 'drop function public.sov_has_permission(text) cascade';
  end if;
exception when others then
  raise notice 'Could not pre-drop sov_has_permission(text): %', sqlerrm;
end $$;

-- Optional cleanup of debug/gate helpers if a partial v5.58.11/v5.58.12 left invalid dependencies.
do $$
begin
  if to_regprocedure('public.sov_can_edit_news()') is not null then
    execute 'drop function public.sov_can_edit_news() cascade';
  end if;
exception when others then
  raise notice 'Could not pre-drop sov_can_edit_news(): %', sqlerrm;
end $$;

-- SOV Roles v5.58.12 core, embedded in v5.58.13
-- Webmaster/Admin split:
-- - Webmaster = Darko / super-role / tech tools / SQL / sync / role manager.
-- - Admin = operational admin: approves users, sends notifications, can see/use normal operational modules incl. news.
-- - Admin no longer gets can_use_sql_tools.

create extension if not exists unaccent;

-- If profiles.role is an enum, try to add webmaster. If not possible in this environment,
-- the email-based webmaster override below still makes darko.jeras@gmail.com Webmaster.
do $$
declare
  v_schema text;
  v_type text;
  v_typtype text;
begin
  select c.udt_schema, c.udt_name, t.typtype
    into v_schema, v_type, v_typtype
  from information_schema.columns c
  join pg_type t on t.typname = c.udt_name
  where c.table_schema='public' and c.table_name='profiles' and c.column_name='role'
  limit 1;

  if v_typtype = 'e' then
    begin
      execute format('alter type %I.%I add value if not exists %L', v_schema, v_type, 'webmaster');
    exception when others then
      raise notice 'Could not add webmaster to enum %.%: %', v_schema, v_type, sqlerrm;
    end;
  end if;
end $$;

create or replace function public.sov_role_slug(p_role text)
returns text
language sql
immutable
as $$
  select nullif(regexp_replace(lower(unaccent(trim(coalesce(p_role,'')))),'[^a-z0-9_\-]+','','g'),'')
$$;

grant execute on function public.sov_role_slug(text) to anon, authenticated;

create or replace function public.sov_is_webmaster_email(p_email text)
returns boolean
language sql
immutable
as $$
  select lower(trim(coalesce(p_email,''))) = 'darko.jeras@gmail.com'
$$;

grant execute on function public.sov_is_webmaster_email(text) to anon, authenticated;

-- Role permissions table + new permission column.
create table if not exists public.sov_role_permissions (
  role text primary key,
  label text not null,
  can_view_sov_base boolean not null default true,
  can_view_katastar boolean not null default false,
  can_edit_objects boolean not null default false,
  can_upload_drawings boolean not null default false,
  can_verify_drawings boolean not null default false,
  can_manage_trips boolean not null default false,
  can_manage_equipment boolean not null default false,
  can_edit_news boolean not null default false,
  can_use_sql_tools boolean not null default false,
  can_manage_users boolean not null default false,
  updated_at timestamptz not null default now()
);

alter table public.sov_role_permissions add column if not exists can_send_notifications boolean not null default false;

insert into public.sov_role_permissions(role,label,can_view_sov_base,can_view_katastar,can_edit_objects,can_upload_drawings,can_verify_drawings,can_manage_trips,can_manage_equipment,can_edit_news,can_use_sql_tools,can_manage_users,can_send_notifications)
values
  ('user','Član',true,false,false,true,false,false,false,false,false,false,false),
  ('editor','Urednik',true,false,true,true,false,true,false,true,false,false,false),
  ('arhivar','Arhivar',true,false,true,true,true,true,false,false,false,false,false),
  ('oruzar','Oružar',true,false,false,true,false,false,true,false,false,false,false),
  ('admin','Admin',true,true,true,true,true,true,true,true,false,true,true),
  ('webmaster','Webmaster',true,true,true,true,true,true,true,true,true,true,true)
on conflict(role) do update set
  label=excluded.label,
  can_view_sov_base=excluded.can_view_sov_base,
  can_view_katastar=excluded.can_view_katastar,
  can_edit_objects=excluded.can_edit_objects,
  can_upload_drawings=excluded.can_upload_drawings,
  can_verify_drawings=excluded.can_verify_drawings,
  can_manage_trips=excluded.can_manage_trips,
  can_manage_equipment=excluded.can_manage_equipment,
  can_edit_news=excluded.can_edit_news,
  can_use_sql_tools=excluded.can_use_sql_tools,
  can_manage_users=excluded.can_manage_users,
  can_send_notifications=excluded.can_send_notifications,
  updated_at=now();

-- Current role: Darko email always wins as Webmaster, then metadata/profile fallback.
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
  v_email text := auth.email();
  v_role text;
  v_jwt jsonb := auth.jwt();
  v_id_col text;
  v_role_col text;
begin
  if v_uid is null then return 'anon'; end if;
  if public.sov_is_webmaster_email(v_email) then return 'webmaster'; end if;

  v_role := public.sov_role_slug(coalesce(
    v_jwt -> 'app_metadata' ->> 'sov_role',
    v_jwt -> 'app_metadata' ->> 'app_role',
    v_jwt -> 'app_metadata' ->> 'role',
    v_jwt -> 'user_metadata' ->> 'sov_role',
    v_jwt -> 'user_metadata' ->> 'app_role',
    v_jwt -> 'user_metadata' ->> 'role'
  ));
  if v_role is not null and v_role not in ('authenticated','anon','service_role') then return v_role; end if;

  if to_regclass('public.profiles') is not null then
    v_id_col := null;
    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='id') then v_id_col := 'id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='user_id') then v_id_col := 'user_id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='auth_user_id') then v_id_col := 'auth_user_id';
    end if;
    if v_id_col is not null then
      foreach v_role_col in array array['role','app_role','sov_role','user_role','permissions_role'] loop
        if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name=v_role_col) then
          begin
            execute format('select public.sov_role_slug(%I::text) from public.profiles where %I = $1 limit 1', v_role_col, v_id_col) into v_role using v_uid;
            if v_role is not null and v_role not in ('authenticated','anon','service_role') then return v_role; end if;
          exception when others then null;
          end;
        end if;
      end loop;
    end if;
  end if;
  return 'user';
exception when others then
  return case when public.sov_is_webmaster_email(auth.email()) then 'webmaster' else 'user' end;
end;
$$;

grant execute on function public.sov_current_role() to anon, authenticated;

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
  v_id_col text;
  v_status_col text;
begin
  if v_uid is null then return 'anon'; end if;
  if public.sov_is_webmaster_email(auth.email()) then return 'approved'; end if;
  if to_regclass('public.profiles') is not null then
    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='id') then v_id_col := 'id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='user_id') then v_id_col := 'user_id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='auth_user_id') then v_id_col := 'auth_user_id';
    end if;
    if v_id_col is not null then
      foreach v_status_col in array array['status','account_status','approval_status','user_status'] loop
        if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name=v_status_col) then
          begin
            execute format('select public.sov_role_slug(%I::text) from public.profiles where %I = $1 limit 1', v_status_col, v_id_col) into v_status using v_uid;
            if v_status is not null then return v_status; end if;
          exception when others then null;
          end;
        end if;
      end loop;
    end if;
  end if;
  return 'unknown';
exception when others then return 'unknown';
end;
$$;

grant execute on function public.sov_current_status() to anon, authenticated;

create or replace function public.sov_is_webmaster()
returns boolean
language sql
stable
security definer
set search_path = public, auth
set row_security = off
as $$ select public.sov_current_role() = 'webmaster' and public.sov_current_status() in ('approved','unknown') $$;

create or replace function public.sov_is_admin()
returns boolean
language sql
stable
security definer
set search_path = public, auth
set row_security = off
as $$ select public.sov_current_role() in ('webmaster','admin') and public.sov_current_status() in ('approved','unknown') $$;

grant execute on function public.sov_is_webmaster() to anon, authenticated;
grant execute on function public.sov_is_admin() to anon, authenticated;

create or replace function public.sov_has_permission(permission_name text)
returns boolean
language plpgsql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_role text := public.sov_current_role();
  v_col text;
  v_can boolean := false;
begin
  if auth.uid() is null then return false; end if;
  if public.sov_current_status() not in ('approved','unknown') then return false; end if;
  if v_role = 'webmaster' then return true; end if;

  v_col := case permission_name
    when 'view_sov_base' then 'can_view_sov_base'
    when 'view_katastar' then 'can_view_katastar'
    when 'edit_objects' then 'can_edit_objects'
    when 'upload_drawings' then 'can_upload_drawings'
    when 'verify_drawings' then 'can_verify_drawings'
    when 'manage_trips' then 'can_manage_trips'
    when 'manage_equipment' then 'can_manage_equipment'
    when 'edit_news' then 'can_edit_news'
    when 'use_sql_tools' then 'can_use_sql_tools'
    when 'manage_users' then 'can_manage_users'
    when 'send_notifications' then 'can_send_notifications'
    else null end;
  if v_col is null then return false; end if;
  if to_regclass('public.sov_role_permissions') is not null and exists (select 1 from information_schema.columns where table_schema='public' and table_name='sov_role_permissions' and column_name=v_col) then
    execute format('select coalesce(%I,false) from public.sov_role_permissions where role=$1 limit 1', v_col) into v_can using v_role;
    return coalesce(v_can,false);
  end if;
  return false;
exception when others then return false;
end;
$$;

grant execute on function public.sov_has_permission(text) to anon, authenticated;

-- Try to persist Darko's profile as webmaster; if role is an enum and the new value is not usable yet, helper still works by email.
do $$
begin
  if to_regclass('public.profiles') is not null then
    begin
      update public.profiles set role='webmaster', status='approved', approved_at=coalesce(approved_at, now()) where lower(email)='darko.jeras@gmail.com';
    exception when others then
      raise notice 'Could not persist Darko as webmaster in profiles.role, using email override instead: %', sqlerrm;
      begin update public.profiles set status='approved', approved_at=coalesce(approved_at, now()) where lower(email)='darko.jeras@gmail.com'; exception when others then null; end;
    end;
  end if;
end $$;

-- RLS for role permissions: readable to approved users, editable only by Webmaster.
alter table public.sov_role_permissions enable row level security;
do $$
declare r record;
begin
  for r in select policyname from pg_policies where schemaname='public' and tablename='sov_role_permissions' loop
    execute format('drop policy if exists %I on public.sov_role_permissions', r.policyname);
  end loop;
end $$;
create policy "sov role permissions readable v55812" on public.sov_role_permissions for select using (auth.uid() is not null);
create policy "sov role permissions webmaster insert v55812" on public.sov_role_permissions for insert with check (public.sov_is_webmaster());
create policy "sov role permissions webmaster update v55812" on public.sov_role_permissions for update using (public.sov_is_webmaster()) with check (public.sov_is_webmaster());
create policy "sov role permissions webmaster delete v55812" on public.sov_role_permissions for delete using (public.sov_is_webmaster());

-- Safer profile RLS: self read/update basic. Admin/Webmaster can approve/update users, but only Webmaster can assign/update Webmaster rows.
do $$
declare r record;
begin
  if to_regclass('public.profiles') is not null then
    execute 'alter table public.profiles enable row level security';
    for r in select policyname from pg_policies where schemaname='public' and tablename='profiles' loop
      execute format('drop policy if exists %I on public.profiles', r.policyname);
    end loop;
    execute 'create policy "profiles self read v55812" on public.profiles for select using (id = auth.uid() or public.sov_has_permission(''manage_users''))';
    execute 'create policy "profiles self insert v55812" on public.profiles for insert with check (id = auth.uid())';
    execute 'create policy "profiles self update basic v55812" on public.profiles for update using (id = auth.uid() or (public.sov_has_permission(''manage_users'') and coalesce(role::text, '''') <> ''webmaster'')) with check (id = auth.uid() or public.sov_is_webmaster() or (public.sov_has_permission(''manage_users'') and coalesce(role::text, '''') <> ''webmaster''))';
  end if;
end $$;

-- Current user's effective permissions view.
drop view if exists public.sov_current_user_permissions;
create view public.sov_current_user_permissions as
select
  auth.uid() as user_id,
  coalesce(p.email, auth.email()) as email,
  coalesce(p.full_name, auth.email()) as full_name,
  case when public.sov_is_webmaster_email(coalesce(p.email, auth.email())) then 'webmaster' else coalesce(p.role::text, 'user') end as role,
  case when public.sov_is_webmaster_email(coalesce(p.email, auth.email())) then 'approved' else coalesce(p.status::text, 'pending') end as status,
  rp.label,
  rp.can_view_sov_base,
  rp.can_view_katastar,
  rp.can_edit_objects,
  rp.can_upload_drawings,
  rp.can_verify_drawings,
  rp.can_manage_trips,
  rp.can_manage_equipment,
  rp.can_edit_news,
  rp.can_use_sql_tools,
  rp.can_manage_users,
  coalesce(rp.can_send_notifications,false) as can_send_notifications
from public.profiles p
left join public.sov_role_permissions rp on rp.role = case when public.sov_is_webmaster_email(coalesce(p.email, auth.email())) then 'webmaster' else coalesce(p.role::text, 'user') end
where p.id = auth.uid();

create or replace view public.sov_role_manifest as
select role,label,jsonb_build_object(
  'can_view_sov_base',can_view_sov_base,
  'can_view_katastar',can_view_katastar,
  'can_edit_objects',can_edit_objects,
  'can_upload_drawings',can_upload_drawings,
  'can_verify_drawings',can_verify_drawings,
  'can_manage_trips',can_manage_trips,
  'can_manage_equipment',can_manage_equipment,
  'can_edit_news',can_edit_news,
  'can_use_sql_tools',can_use_sql_tools,
  'can_manage_users',can_manage_users,
  'can_send_notifications',coalesce(can_send_notifications,false)
) as permissions, updated_at
from public.sov_role_permissions
order by case role when 'webmaster' then 0 when 'admin' then 1 when 'editor' then 2 when 'arhivar' then 3 when 'oruzar' then 4 when 'user' then 5 else 9 end, role;

grant usage on schema public to anon, authenticated;
grant select on public.sov_role_permissions to anon, authenticated;
grant insert, update, delete on public.sov_role_permissions to authenticated;
grant select on public.sov_current_user_permissions to authenticated;
grant select on public.sov_role_manifest to anon, authenticated;

-- News editor remains Admin/Webmaster/Editor via edit_news permission.
create or replace function public.sov_can_edit_news()
returns boolean
language sql
stable
security definer
set search_path = public, auth
set row_security = off
as $$ select public.sov_has_permission('edit_news') $$;

grant execute on function public.sov_can_edit_news() to anon, authenticated;

-- Notification table/policies: Admin and Webmaster can send; everyone can read visible active notifications.
create table if not exists public.sov_notifications (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  body text not null,
  target text default 'all',
  target_role text default 'all',
  priority text default 'normal',
  status text default 'active',
  author text,
  created_by uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.sov_notifications enable row level security;
do $$
declare r record;
begin
  for r in select policyname from pg_policies where schemaname='public' and tablename='sov_notifications' loop
    execute format('drop policy if exists %I on public.sov_notifications', r.policyname);
  end loop;
end $$;
create policy "sov notifications read active v55812" on public.sov_notifications for select using (status='active' and auth.uid() is not null);
create policy "sov notifications admin insert v55812" on public.sov_notifications for insert with check (public.sov_has_permission('send_notifications'));
create policy "sov notifications admin update v55812" on public.sov_notifications for update using (public.sov_has_permission('send_notifications')) with check (public.sov_has_permission('send_notifications'));
create policy "sov notifications webmaster delete v55812" on public.sov_notifications for delete using (public.sov_is_webmaster());

grant select, insert, update, delete on public.sov_notifications to authenticated;

-- Debug helper for this role split.
create or replace function public.sov_role_debug()
returns jsonb
language sql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
  select jsonb_build_object(
    'email', auth.email(),
    'role', public.sov_current_role(),
    'status', public.sov_current_status(),
    'is_webmaster', public.sov_is_webmaster(),
    'is_admin', public.sov_is_admin(),
    'can_manage_users', public.sov_has_permission('manage_users'),
    'can_send_notifications', public.sov_has_permission('send_notifications'),
    'can_edit_news', public.sov_has_permission('edit_news'),
    'can_use_sql_tools', public.sov_has_permission('use_sql_tools')
  )
$$;

grant execute on function public.sov_role_debug() to authenticated;


-- -------------------------------------------------------------------
-- v5.58.13 postflight: restore News CMS policies that may have been
-- dropped by CASCADE when replacing sov_has_permission(text).
-- -------------------------------------------------------------------
do $$
declare
  r record;
begin
  if to_regclass('public.sov_news') is not null then
    execute 'alter table public.sov_news enable row level security';

    for r in select policyname from pg_policies where schemaname='public' and tablename='sov_news' loop
      execute format('drop policy if exists %I on public.sov_news', r.policyname);
    end loop;

    execute 'create policy "sov_news public read published v55813" on public.sov_news for select to anon, authenticated using (published = true)';
    execute 'create policy "sov_news authenticated read all v55813" on public.sov_news for select to authenticated using (auth.uid() is not null)';
    execute 'create policy "sov_news editor insert v55813" on public.sov_news for insert to authenticated with check (public.sov_can_edit_news())';
    execute 'create policy "sov_news editor update v55813" on public.sov_news for update to authenticated using (public.sov_can_edit_news()) with check (public.sov_can_edit_news())';
    execute 'create policy "sov_news editor delete v55813" on public.sov_news for delete to authenticated using (public.sov_can_edit_news())';
  end if;
end $$;

grant usage on schema public to anon, authenticated;
do $$
begin
  if to_regclass('public.sov_news') is not null then
    execute 'grant select on public.sov_news to anon, authenticated';
    execute 'grant insert, update, delete on public.sov_news to authenticated';
  end if;
exception when others then
  raise notice 'sov_news grants skipped: %', sqlerrm;
end $$;

-- Keep old news debug RPC available for sync/status pages that still call it.
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
    'email', auth.email(),
    'jwt_system_role', auth.jwt() ->> 'role',
    'sov_role', public.sov_current_role(),
    'sov_status', public.sov_current_status(),
    'can_edit_news', public.sov_can_edit_news(),
    'has_edit_news_permission', public.sov_has_permission('edit_news'),
    'can_use_sql_tools', public.sov_has_permission('use_sql_tools'),
    'fixed_in', 'v5.58.13'
  );
$$;

grant execute on function public.sov_news_auth_debug() to anon, authenticated;

-- Keep sov-news Storage policy stable: public read, authenticated upload. This avoids editor upload false negatives.
do $$
declare
  r record;
begin
  if to_regclass('storage.buckets') is not null then
    insert into storage.buckets (id, name, public)
    values ('sov-news', 'sov-news', true)
    on conflict (id) do update set public = true, name = excluded.name;
  end if;

  if to_regclass('storage.objects') is not null then
    for r in
      select policyname from pg_policies
      where schemaname='storage' and tablename='objects' and policyname like 'sov_news_storage%'
    loop
      execute format('drop policy if exists %I on storage.objects', r.policyname);
    end loop;

    execute 'create policy "sov_news_storage_public_read v55813" on storage.objects for select to anon, authenticated using (bucket_id = ''sov-news'')';
    execute 'create policy "sov_news_storage_authenticated_insert v55813" on storage.objects for insert to authenticated with check (bucket_id = ''sov-news'' and auth.uid() is not null)';
    execute 'create policy "sov_news_storage_authenticated_update v55813" on storage.objects for update to authenticated using (bucket_id = ''sov-news'' and auth.uid() is not null) with check (bucket_id = ''sov-news'' and auth.uid() is not null)';
    execute 'create policy "sov_news_storage_authenticated_delete v55813" on storage.objects for delete to authenticated using (bucket_id = ''sov-news'' and auth.uid() is not null)';
  end if;
exception when others then
  raise notice 'sov-news storage policy postflight skipped: %', sqlerrm;
end $$;

-- Final smoke/debug helper comment.
comment on function public.sov_has_permission(text) is 'SOV v5.58.13: stable permission helper; parameter name kept as permission_name to avoid PostgreSQL 42P13 on upgrades.';
comment on function public.sov_can_edit_news() is 'SOV v5.58.13: news editor gate via sov_has_permission(edit_news).';

select public.sov_role_debug() as sov_role_debug_v55813;
