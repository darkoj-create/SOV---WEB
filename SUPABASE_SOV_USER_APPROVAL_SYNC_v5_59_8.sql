-- SOV Web v5.59.8 — User approval sync / invisible registered users fix
-- Problem fixed:
--   Supabase Auth signup can succeed while public.profiles insert is blocked by RLS/email-confirm session.
--   Admin UI then reads only public.profiles, so the registered user is invisible in approval/role lists.
-- This patch:
--   1) ensures public.profiles has the expected columns,
--   2) creates an Auth trigger that always creates a pending profile,
--   3) backfills profiles for already-created auth.users,
--   4) adds admin RPCs used by the v5.59.8 web frontend.

create extension if not exists pgcrypto;
create extension if not exists unaccent;

-- -------------------------------------------------------------------
-- 0) Compatibility helpers
-- -------------------------------------------------------------------
create or replace function public.sov_is_webmaster_email(p_email text)
returns boolean
language sql
immutable
as $$
  select lower(trim(coalesce(p_email,''))) = 'darko.jeras@gmail.com'
$$;

grant execute on function public.sov_is_webmaster_email(text) to anon, authenticated;

-- If role/status are enum columns in an older install, make sure values exist.
do $$
declare
  v_schema text;
  v_type text;
  v_typtype text;
begin
  if to_regclass('public.profiles') is not null then
    select c.udt_schema, c.udt_name, t.typtype
      into v_schema, v_type, v_typtype
    from information_schema.columns c
    join pg_type t on t.typname = c.udt_name
    where c.table_schema='public' and c.table_name='profiles' and c.column_name='role'
    limit 1;
    if v_typtype = 'e' then
      begin execute format('alter type %I.%I add value if not exists %L', v_schema, v_type, 'user'); exception when others then null; end;
      begin execute format('alter type %I.%I add value if not exists %L', v_schema, v_type, 'editor'); exception when others then null; end;
      begin execute format('alter type %I.%I add value if not exists %L', v_schema, v_type, 'arhivar'); exception when others then null; end;
      begin execute format('alter type %I.%I add value if not exists %L', v_schema, v_type, 'oruzar'); exception when others then null; end;
      begin execute format('alter type %I.%I add value if not exists %L', v_schema, v_type, 'admin'); exception when others then null; end;
      begin execute format('alter type %I.%I add value if not exists %L', v_schema, v_type, 'webmaster'); exception when others then null; end;
    end if;

    v_schema := null; v_type := null; v_typtype := null;
    select c.udt_schema, c.udt_name, t.typtype
      into v_schema, v_type, v_typtype
    from information_schema.columns c
    join pg_type t on t.typname = c.udt_name
    where c.table_schema='public' and c.table_name='profiles' and c.column_name='status'
    limit 1;
    if v_typtype = 'e' then
      begin execute format('alter type %I.%I add value if not exists %L', v_schema, v_type, 'pending'); exception when others then null; end;
      begin execute format('alter type %I.%I add value if not exists %L', v_schema, v_type, 'approved'); exception when others then null; end;
      begin execute format('alter type %I.%I add value if not exists %L', v_schema, v_type, 'rejected'); exception when others then null; end;
    end if;
  end if;
end $$;

-- -------------------------------------------------------------------
-- 1) Expected profile table/columns
-- -------------------------------------------------------------------
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text unique,
  full_name text,
  role text not null default 'user',
  status text not null default 'pending',
  note text,
  created_at timestamptz default now(),
  approved_at timestamptz,
  approved_by uuid
);

alter table public.profiles add column if not exists email text;
alter table public.profiles add column if not exists full_name text;
alter table public.profiles add column if not exists role text default 'user';
alter table public.profiles add column if not exists status text default 'pending';
alter table public.profiles add column if not exists note text;
alter table public.profiles add column if not exists created_at timestamptz default now();
alter table public.profiles add column if not exists approved_at timestamptz;
alter table public.profiles add column if not exists approved_by uuid;

-- Old setup had a role CHECK without webmaster/arhivar in some installs. Replace role/status checks safely.
do $$
declare r record;
begin
  for r in
    select conname, pg_get_constraintdef(oid) as def
    from pg_constraint
    where conrelid = 'public.profiles'::regclass and contype = 'c'
  loop
    if lower(r.def) like '%role%' or lower(r.def) like '%status%' then
      execute format('alter table public.profiles drop constraint if exists %I', r.conname);
    end if;
  end loop;

  begin
    alter table public.profiles
      add constraint profiles_role_sov_check
      check (coalesce(role::text,'user') in ('user','editor','urednik','arhivar','oruzar','admin','webmaster')) not valid;
  exception when duplicate_object then null;
  end;

  begin
    alter table public.profiles
      add constraint profiles_status_sov_check
      check (coalesce(status::text,'pending') in ('pending','approved','rejected')) not valid;
  exception when duplicate_object then null;
  end;
end $$;

-- -------------------------------------------------------------------
-- 2) Admin capability helper safe for use inside profiles RLS policies.
-- -------------------------------------------------------------------
create or replace function public.sov_admin_can_manage_users()
returns boolean
language plpgsql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_can boolean := false;
begin
  if auth.uid() is null then return false; end if;
  if public.sov_is_webmaster_email(auth.email()) then return true; end if;

  begin
    if to_regprocedure('public.sov_has_permission(text)') is not null then
      execute 'select public.sov_has_permission($1)' into v_can using 'manage_users';
      if coalesce(v_can,false) then return true; end if;
    end if;
  exception when others then
    v_can := false;
  end;

  return exists (
    select 1
    from public.profiles p
    where p.id = auth.uid()
      and lower(coalesce(p.role::text,'')) in ('admin','webmaster')
      and lower(coalesce(p.status::text,'')) = 'approved'
  );
exception when others then
  return false;
end;
$$;

grant execute on function public.sov_admin_can_manage_users() to authenticated;

-- -------------------------------------------------------------------
-- 3) RLS: direct table fallback stays usable, but the new RPCs are primary.
-- -------------------------------------------------------------------
alter table public.profiles enable row level security;

do $$
declare r record;
begin
  for r in select policyname from pg_policies where schemaname='public' and tablename='profiles' loop
    execute format('drop policy if exists %I on public.profiles', r.policyname);
  end loop;
end $$;

create policy "profiles self/admin read v5598"
  on public.profiles for select
  using (id = auth.uid() or public.sov_admin_can_manage_users());

create policy "profiles self insert v5598"
  on public.profiles for insert
  with check (id = auth.uid() or public.sov_admin_can_manage_users());

create policy "profiles self/admin update v5598"
  on public.profiles for update
  using (id = auth.uid() or public.sov_admin_can_manage_users())
  with check (id = auth.uid() or public.sov_admin_can_manage_users());

grant select, insert, update on public.profiles to authenticated;

-- -------------------------------------------------------------------
-- 4) Auth trigger + registration RPC.
-- -------------------------------------------------------------------
create or replace function public.sov_auth_user_to_profile()
returns trigger
language plpgsql
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_full_name text;
  v_note text;
begin
  v_full_name := coalesce(
    nullif(new.raw_user_meta_data ->> 'full_name',''),
    nullif(new.raw_user_meta_data ->> 'name',''),
    split_part(coalesce(new.email,''),'@',1)
  );
  v_note := coalesce(new.raw_user_meta_data ->> 'note','');

  insert into public.profiles(id,email,full_name,role,status,note,created_at)
  values(new.id, lower(new.email), v_full_name, 'user', 'pending', v_note, coalesce(new.created_at, now()))
  on conflict (id) do update set
    email = excluded.email,
    full_name = coalesce(nullif(public.profiles.full_name,''), excluded.full_name),
    created_at = coalesce(public.profiles.created_at, excluded.created_at);

  return new;
exception when others then
  raise warning 'SOV profile sync failed for auth user %: %', new.id, sqlerrm;
  return new;
end;
$$;

drop trigger if exists sov_auth_user_profile_sync on auth.users;
create trigger sov_auth_user_profile_sync
  after insert or update of email, raw_user_meta_data on auth.users
  for each row execute function public.sov_auth_user_to_profile();

create or replace function public.sov_register_pending_profile(
  p_user_id uuid,
  p_email text,
  p_full_name text default null,
  p_note text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_exists boolean;
begin
  select exists(
    select 1 from auth.users au
    where au.id = p_user_id and lower(au.email) = lower(trim(coalesce(p_email,'')))
  ) into v_exists;

  if not coalesce(v_exists,false) then
    raise exception 'Auth user not found for supplied id/email.' using errcode = 'P0001';
  end if;

  insert into public.profiles(id,email,full_name,role,status,note,created_at)
  values(p_user_id, lower(trim(p_email)), nullif(trim(coalesce(p_full_name,'')),''), 'user', 'pending', coalesce(p_note,''), now())
  on conflict (id) do update set
    email = excluded.email,
    full_name = case when lower(coalesce(public.profiles.status::text,'pending')) = 'pending'
      then coalesce(excluded.full_name, public.profiles.full_name) else public.profiles.full_name end,
    note = case when lower(coalesce(public.profiles.status::text,'pending')) = 'pending'
      then coalesce(excluded.note, public.profiles.note) else public.profiles.note end;

  return jsonb_build_object('ok', true, 'user_id', p_user_id);
end;
$$;

grant execute on function public.sov_register_pending_profile(uuid,text,text,text) to anon, authenticated;

-- -------------------------------------------------------------------
-- 5) Backfill / admin list / admin update RPCs.
-- -------------------------------------------------------------------
create or replace function public.sov_admin_sync_missing_profiles()
returns integer
language plpgsql
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_count integer := 0;
begin
  with ins as (
    insert into public.profiles(id,email,full_name,role,status,note,created_at)
    select
      au.id,
      lower(au.email),
      coalesce(nullif(au.raw_user_meta_data ->> 'full_name',''), nullif(au.raw_user_meta_data ->> 'name',''), split_part(coalesce(au.email,''),'@',1)),
      'user',
      'pending',
      coalesce(au.raw_user_meta_data ->> 'note',''),
      coalesce(au.created_at, now())
    from auth.users au
    left join public.profiles p on p.id = au.id
    where p.id is null
    on conflict do nothing
    returning 1
  )
  select count(*)::integer into v_count from ins;

  return coalesce(v_count,0);
end;
$$;

grant execute on function public.sov_admin_sync_missing_profiles() to authenticated;

create or replace function public.sov_admin_list_users()
returns table(
  id uuid,
  email text,
  full_name text,
  role text,
  status text,
  note text,
  created_at timestamptz,
  approved_at timestamptz,
  has_profile boolean,
  auth_created_at timestamptz,
  last_sign_in_at timestamptz
)
language plpgsql
security definer
set search_path = public, auth
set row_security = off
as $$
begin
  if not public.sov_admin_can_manage_users() then
    raise exception 'Not allowed to list SOV users.' using errcode = '42501';
  end if;

  perform public.sov_admin_sync_missing_profiles();

  return query
  select
    au.id,
    lower(coalesce(p.email, au.email))::text as email,
    coalesce(nullif(p.full_name,''), nullif(au.raw_user_meta_data ->> 'full_name',''), nullif(au.raw_user_meta_data ->> 'name',''), au.email)::text as full_name,
    coalesce(p.role::text, 'user')::text as role,
    coalesce(p.status::text, 'pending')::text as status,
    coalesce(p.note,'')::text as note,
    coalesce(p.created_at, au.created_at)::timestamptz as created_at,
    p.approved_at::timestamptz as approved_at,
    (p.id is not null) as has_profile,
    au.created_at::timestamptz as auth_created_at,
    au.last_sign_in_at::timestamptz as last_sign_in_at
  from auth.users au
  left join public.profiles p on p.id = au.id
  order by coalesce(p.created_at, au.created_at) desc nulls last;
end;
$$;

grant execute on function public.sov_admin_list_users() to authenticated;

create or replace function public.sov_admin_update_user_profile(
  p_user_id uuid,
  p_role text default null,
  p_status text default null,
  p_note text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_role_type text;
  v_status_type text;
  v_sql text;
  v_row jsonb;
begin
  if not public.sov_admin_can_manage_users() then
    raise exception 'Not allowed to update SOV users.' using errcode = '42501';
  end if;

  if p_role is not null and lower(p_role) not in ('user','editor','urednik','arhivar','oruzar','admin','webmaster') then
    raise exception 'Invalid SOV role: %', p_role using errcode = '22023';
  end if;

  if lower(coalesce(p_role,'')) = 'webmaster' and not public.sov_is_webmaster_email(auth.email()) then
    raise exception 'Only Webmaster can assign Webmaster role.' using errcode = '42501';
  end if;

  if p_status is not null and lower(p_status) not in ('pending','approved','rejected') then
    raise exception 'Invalid SOV status: %', p_status using errcode = '22023';
  end if;

  perform public.sov_admin_sync_missing_profiles();

  select format_type(a.atttypid, a.atttypmod)
    into v_role_type
  from pg_attribute a
  where a.attrelid = 'public.profiles'::regclass and a.attname='role' and not a.attisdropped;

  select format_type(a.atttypid, a.atttypmod)
    into v_status_type
  from pg_attribute a
  where a.attrelid = 'public.profiles'::regclass and a.attname='status' and not a.attisdropped;

  v_role_type := coalesce(v_role_type, 'text');
  v_status_type := coalesce(v_status_type, 'text');

  v_sql := format($fmt$
    update public.profiles
    set
      role = case when $2::text is null then role else lower($2)::%s end,
      status = case when $3::text is null then status else lower($3)::%s end,
      note = case when $4::text is null then note else $4 end,
      approved_at = case when lower(coalesce($3::text,'')) = 'approved' then coalesce(approved_at, now()) else approved_at end,
      approved_by = case when lower(coalesce($3::text,'')) = 'approved' then auth.uid() else approved_by end
    where id = $1
  $fmt$, v_role_type, v_status_type);

  execute v_sql using p_user_id, nullif(trim(coalesce(p_role,'')),''), nullif(trim(coalesce(p_status,'')),''), p_note;

  select jsonb_build_object(
    'ok', true,
    'id', p.id,
    'email', p.email,
    'full_name', p.full_name,
    'role', p.role::text,
    'status', p.status::text,
    'note', p.note,
    'approved_at', p.approved_at
  ) into v_row
  from public.profiles p
  where p.id = p_user_id;

  if v_row is null then
    raise exception 'Profile not found after sync: %', p_user_id using errcode = 'P0002';
  end if;

  return v_row;
end;
$$;

grant execute on function public.sov_admin_update_user_profile(uuid,text,text,text) to authenticated;

-- -------------------------------------------------------------------
-- 6) Immediate one-time backfill for users already stuck only in Auth.
-- -------------------------------------------------------------------
select public.sov_admin_sync_missing_profiles() as sov_profiles_backfilled;
