-- SOV ECOSYSTEM ROLES / PERMISSIONS v5.31
-- Purpose: priprema za jedan unified/admin-capable APK + web role gating.
-- Safe/additive: ne dira SQL bazu objekata niti staging/live object tablice.
-- Runs in Supabase SQL editor.

-- 1) Ensure profiles can store known roles as text or enum-compatible text.
-- Existing projects may already have public.profiles. This script does not recreate it.

-- 2) Role permissions table: backend-readable map of what each role may see/use.
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

alter table public.sov_role_permissions enable row level security;

drop policy if exists "sov role permissions readable" on public.sov_role_permissions;
create policy "sov role permissions readable"
  on public.sov_role_permissions for select
  using (true);

-- 3) View for current user's effective permissions.
-- The app can query this after login to decide which modules to show.
create or replace view public.sov_current_user_permissions as
select
  auth.uid() as user_id,
  coalesce(p.email, auth.email()) as email,
  coalesce(p.full_name, auth.email()) as full_name,
  coalesce(p.role::text, 'user') as role,
  coalesce(p.status, 'pending') as status,
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

-- 4) Optional helper for RLS policies.
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
    and p.status = 'approved'
  limit 1;
$$;

-- 5) Optional: harden drawing write access without blocking normal read.
-- Select remains public/approved depending on your existing policy; this only adds safer role-based insert/update policies if table exists.
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
