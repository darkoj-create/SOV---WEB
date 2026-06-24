-- SOV v4.89 — Arhivar role + Speleo Baza edit
-- Run once in Supabase SQL Editor.

-- 1) Add role value if profiles.role uses enum sov_user_role.
do $$
begin
  if exists (select 1 from pg_type where typname = 'sov_user_role') then
    alter type public.sov_user_role add value if not exists 'arhivar';
  end if;
exception when duplicate_object then null;
end $$;

-- 2) Tables for editable overrides on top of static Speleo JSON/base data.
create table if not exists public.speleo_object_overrides (
  object_id text primary key,
  data jsonb not null default '{}'::jsonb,
  updated_by uuid null references auth.users(id) on delete set null,
  updated_at timestamptz not null default now()
);

create table if not exists public.speleo_object_edits (
  id bigserial primary key,
  object_id text not null,
  edited_by uuid null references auth.users(id) on delete set null,
  edited_at timestamptz not null default now(),
  changed_fields text[] not null default '{}',
  old_values jsonb null,
  new_values jsonb not null default '{}'::jsonb,
  note text null
);

alter table public.speleo_object_overrides enable row level security;
alter table public.speleo_object_edits enable row level security;

create or replace function public.sov_current_role_text()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((select p.role::text from public.profiles p where p.id = auth.uid()), 'user')
$$;

create or replace function public.sov_is_admin_or_arhivar()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.sov_current_role_text() in ('admin','arhivar')
$$;

-- Read overrides: approved/logged users can see applied changes.
drop policy if exists speleo_overrides_select on public.speleo_object_overrides;
create policy speleo_overrides_select
on public.speleo_object_overrides
for select
to authenticated
using (true);

-- Admin/arhivar can insert/update overrides. No hard delete for arhivar workflow.
drop policy if exists speleo_overrides_insert_admin_arhivar on public.speleo_object_overrides;
create policy speleo_overrides_insert_admin_arhivar
on public.speleo_object_overrides
for insert
to authenticated
with check (public.sov_is_admin_or_arhivar());

drop policy if exists speleo_overrides_update_admin_arhivar on public.speleo_object_overrides;
create policy speleo_overrides_update_admin_arhivar
on public.speleo_object_overrides
for update
to authenticated
using (public.sov_is_admin_or_arhivar())
with check (public.sov_is_admin_or_arhivar());

-- Edit log.
drop policy if exists speleo_edits_select_admin_arhivar on public.speleo_object_edits;
create policy speleo_edits_select_admin_arhivar
on public.speleo_object_edits
for select
to authenticated
using (public.sov_is_admin_or_arhivar());

drop policy if exists speleo_edits_insert_admin_arhivar on public.speleo_object_edits;
create policy speleo_edits_insert_admin_arhivar
on public.speleo_object_edits
for insert
to authenticated
with check (public.sov_is_admin_or_arhivar());

-- Optional: allow open preview anon read/edit only if you intentionally use no-login demo builds.
-- Keep authenticated policies above for production.
