-- SOV Oružarstvo v5.41 — Inventura offline-first support
-- Run in Supabase SQL editor with RLS enabled.
-- Adds history tables used by APK 1.3.9 Inventura mode.
-- Does NOT touch the speleo object database.

create extension if not exists pgcrypto;

create or replace function public.sov_can_manage_equipment()
returns boolean
language sql
security definer
set search_path = public
as $$
  select public.sov_has_role(array['admin','oruzar'])
$$;

create table if not exists public.equipment_inventory_sessions (
  id uuid primary key default gen_random_uuid(),
  location_name text not null default 'Sve lokacije',
  category_name text not null default 'Sve kategorije',
  status text not null default 'closed',
  started_by uuid default auth.uid(),
  started_by_email text,
  started_by_name text,
  item_count integer not null default 0,
  mismatch_count integer not null default 0,
  shortage_count integer not null default 0,
  surplus_count integer not null default 0,
  note text,
  synced_from text not null default 'android',
  started_at timestamptz not null default now(),
  closed_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create table if not exists public.equipment_inventory_counts (
  id uuid primary key default gen_random_uuid(),
  session_id uuid not null references public.equipment_inventory_sessions(id) on delete cascade,
  app_id text,
  source_table text,
  source_id text,
  item_code text,
  item_name text not null,
  category_name text,
  subcategory text,
  location_name text,
  expected_qty integer not null default 0,
  counted_qty integer not null default 0,
  difference_qty integer generated always as (counted_qty - expected_qty) stored,
  unit text not null default 'kom',
  note text,
  created_at timestamptz not null default now()
);

create index if not exists equipment_inventory_sessions_created_idx
  on public.equipment_inventory_sessions(created_at desc);

create index if not exists equipment_inventory_sessions_scope_idx
  on public.equipment_inventory_sessions(location_name, category_name, created_at desc);

create index if not exists equipment_inventory_counts_session_idx
  on public.equipment_inventory_counts(session_id);

create index if not exists equipment_inventory_counts_item_idx
  on public.equipment_inventory_counts(source_table, source_id);

alter table public.equipment_inventory_sessions enable row level security;
alter table public.equipment_inventory_counts enable row level security;

drop policy if exists equipment_inventory_sessions_manage on public.equipment_inventory_sessions;
drop policy if exists equipment_inventory_sessions_read on public.equipment_inventory_sessions;
drop policy if exists equipment_inventory_counts_manage on public.equipment_inventory_counts;
drop policy if exists equipment_inventory_counts_read on public.equipment_inventory_counts;

create policy equipment_inventory_sessions_read
on public.equipment_inventory_sessions
for select
using (public.sov_can_manage_equipment());

create policy equipment_inventory_sessions_manage
on public.equipment_inventory_sessions
for all
using (public.sov_can_manage_equipment())
with check (public.sov_can_manage_equipment());

create policy equipment_inventory_counts_read
on public.equipment_inventory_counts
for select
using (public.sov_can_manage_equipment());

create policy equipment_inventory_counts_manage
on public.equipment_inventory_counts
for all
using (public.sov_can_manage_equipment())
with check (public.sov_can_manage_equipment());

-- Helpful admin view for web/debug later.
create or replace view public.sov_equipment_inventory_latest_differences as
select
  s.id as session_id,
  s.created_at,
  s.location_name,
  s.category_name,
  s.started_by_email,
  c.item_code,
  c.item_name,
  c.category_name as item_category,
  c.subcategory,
  c.expected_qty,
  c.counted_qty,
  c.difference_qty,
  c.unit,
  c.note
from public.equipment_inventory_sessions s
join public.equipment_inventory_counts c on c.session_id = s.id
where c.difference_qty <> 0
order by s.created_at desc, c.item_name asc;

grant select on public.sov_equipment_inventory_latest_differences to authenticated;
