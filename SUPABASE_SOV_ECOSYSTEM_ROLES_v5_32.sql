-- SOV ECOSYSTEM ROLES / ROLE MANAGER v5.32
-- Purpose: web Role manager + future unified/admin-capable APK permissions.
-- Safe/additive: does NOT touch speleo object live/staging data.
-- Run in Supabase SQL editor.

-- 0) If role is enum-based, make sure all required role values exist.
do $$
begin
  if exists (select 1 from pg_type where typname = 'sov_user_role') then
    begin alter type public.sov_user_role add value if not exists 'user'; exception when others then null; end;
    begin alter type public.sov_user_role add value if not exists 'editor'; exception when others then null; end;
    begin alter type public.sov_user_role add value if not exists 'arhivar'; exception when others then null; end;
    begin alter type public.sov_user_role add value if not exists 'oruzar'; exception when others then null; end;
    begin alter type public.sov_user_role add value if not exists 'admin'; exception when others then null; end;
  end if;
end $$;

-- 1) Canonical role permissions table.
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

insert into public.sov_role_permissions(role,label,can_view_sov_base,can_view_katastar,can_edit_objects,can_upload_drawings,can_verify_drawings,can_manage_trips,can_manage_equipment,can_edit_news,can_use_sql_tools,can_manage_users)
values
  ('user','Član',true,false,false,true,false,false,false,false,false,false),
  ('editor','Urednik',true,false,true,true,false,true,false,true,false,false),
  ('arhivar','Arhivar',true,false,true,true,true,true,false,false,false,false),
  ('oruzar','Oružar',true,false,false,true,false,false,true,false,false,false),
  ('admin','Admin',true,true,true,true,true,true,true,true,true,true)
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
  updated_at=now();

-- 2) Helper functions for RLS and app checks.
create or replace function public.sov_current_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((select p.role::text from public.profiles p where p.id = auth.uid()), 'user')
$$;

create or replace function public.sov_current_status()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((select p.status::text from public.profiles p where p.id = auth.uid()), 'pending')
$$;

create or replace function public.sov_is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles p
    where p.id = auth.uid()
      and p.role::text = 'admin'
      and coalesce(p.status::text,'pending') = 'approved'
  )
$$;

create or replace function public.sov_has_permission(permission_name text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select case permission_name
    when 'view_sov_base' then coalesce(rp.can_view_sov_base,false)
    when 'view_katastar' then coalesce(rp.can_view_katastar,false)
    when 'edit_objects' then coalesce(rp.can_edit_objects,false)
    when 'upload_drawings' then coalesce(rp.can_upload_drawings,false)
    when 'verify_drawings' then coalesce(rp.can_verify_drawings,false)
    when 'manage_trips' then coalesce(rp.can_manage_trips,false)
    when 'manage_equipment' then coalesce(rp.can_manage_equipment,false)
    when 'edit_news' then coalesce(rp.can_edit_news,false)
    when 'use_sql_tools' then coalesce(rp.can_use_sql_tools,false)
    when 'manage_users' then coalesce(rp.can_manage_users,false)
    else false
  end
  from public.profiles p
  left join public.sov_role_permissions rp on rp.role = coalesce(p.role::text, 'user')
  where p.id = auth.uid()
    and coalesce(p.status::text,'pending') = 'approved'
  limit 1;
$$;

-- 3) RLS for role manager table.
alter table public.sov_role_permissions enable row level security;

drop policy if exists "sov role permissions readable" on public.sov_role_permissions;
drop policy if exists "sov role permissions admin insert" on public.sov_role_permissions;
drop policy if exists "sov role permissions admin update" on public.sov_role_permissions;
drop policy if exists "sov role permissions admin delete" on public.sov_role_permissions;

create policy "sov role permissions readable"
  on public.sov_role_permissions for select
  using (true);

create policy "sov role permissions admin insert"
  on public.sov_role_permissions for insert
  with check (public.sov_is_admin());

create policy "sov role permissions admin update"
  on public.sov_role_permissions for update
  using (public.sov_is_admin())
  with check (public.sov_is_admin());

create policy "sov role permissions admin delete"
  on public.sov_role_permissions for delete
  using (public.sov_is_admin());

-- 4) Profiles policies for admin user/role management.
-- Existing projects may already have these; this keeps them explicit for v5.32.
do $$
begin
  if exists (select 1 from information_schema.tables where table_schema='public' and table_name='profiles') then
    execute 'alter table public.profiles enable row level security';

    execute 'drop policy if exists "profiles self read" on public.profiles';
    execute 'drop policy if exists "profiles self insert" on public.profiles';
    execute 'drop policy if exists "profiles self update basic" on public.profiles';
    execute 'drop policy if exists "profiles admin read all" on public.profiles';
    execute 'drop policy if exists "profiles admin update all" on public.profiles';

    execute 'create policy "profiles self read" on public.profiles for select using (id = auth.uid() or public.sov_is_admin())';
    execute 'create policy "profiles self insert" on public.profiles for insert with check (id = auth.uid())';
    execute 'create policy "profiles self update basic" on public.profiles for update using (id = auth.uid()) with check (id = auth.uid())';
    execute 'create policy "profiles admin read all" on public.profiles for select using (public.sov_is_admin())';
    execute 'create policy "profiles admin update all" on public.profiles for update using (public.sov_is_admin()) with check (public.sov_is_admin())';
  end if;
end $$;

-- 5) Current user's effective permissions. App can read this after login.
create or replace view public.sov_current_user_permissions as
select
  auth.uid() as user_id,
  coalesce(p.email, auth.email()) as email,
  coalesce(p.full_name, auth.email()) as full_name,
  coalesce(p.role::text, 'user') as role,
  coalesce(p.status::text, 'pending') as status,
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
  rp.can_manage_users
from public.profiles p
left join public.sov_role_permissions rp on rp.role = coalesce(p.role::text, 'user')
where p.id = auth.uid();

-- 6) Role manifest view: useful for web debug and future app bootstrap.
create or replace view public.sov_role_manifest as
select
  role,
  label,
  jsonb_build_object(
    'can_view_sov_base', can_view_sov_base,
    'can_view_katastar', can_view_katastar,
    'can_edit_objects', can_edit_objects,
    'can_upload_drawings', can_upload_drawings,
    'can_verify_drawings', can_verify_drawings,
    'can_manage_trips', can_manage_trips,
    'can_manage_equipment', can_manage_equipment,
    'can_edit_news', can_edit_news,
    'can_use_sql_tools', can_use_sql_tools,
    'can_manage_users', can_manage_users
  ) as permissions,
  updated_at
from public.sov_role_permissions
order by role;

-- 7) Drawing write guard stays role based; does not change object SQL logic.
do $$
begin
  if exists (select 1 from information_schema.tables where table_schema='public' and table_name='speleo_object_drawings') then
    execute 'alter table public.speleo_object_drawings enable row level security';
    execute 'drop policy if exists "drawings role based insert" on public.speleo_object_drawings';
    execute 'drop policy if exists "drawings role based update" on public.speleo_object_drawings';
    execute 'create policy "drawings role based insert" on public.speleo_object_drawings for insert with check (public.sov_has_permission(''upload_drawings'') or public.sov_has_permission(''verify_drawings''))';
    execute 'create policy "drawings role based update" on public.speleo_object_drawings for update using (public.sov_has_permission(''verify_drawings'') or public.sov_has_permission(''upload_drawings'')) with check (public.sov_has_permission(''verify_drawings'') or public.sov_has_permission(''upload_drawings''))';
  end if;
end $$;

grant usage on schema public to anon, authenticated;
grant select on public.sov_role_permissions to anon, authenticated;
grant insert, update, delete on public.sov_role_permissions to authenticated;
grant select on public.sov_current_user_permissions to authenticated;
grant select on public.sov_role_manifest to anon, authenticated;
