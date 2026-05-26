-- SOV Oružarstvo v4.87 — FIX: inventory_sessions / inventory_session_items RLS za open preview/import
-- Problem: import ili nova inventura upisuje u public.inventory_sessions, a RLS nema otvoreni insert/update policy.
-- Simptom: new row violates row-level security policy for table "inventory_sessions".
-- Pokrenuti jednom. Ne brise podatke.

begin;

create extension if not exists pgcrypto;

create table if not exists public.inventory_sessions (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  name text not null,
  inventory_date date not null default current_date,
  owner_name text,
  created_by uuid references auth.users(id) on delete set null,
  status text not null default 'draft',
  note text,
  locked_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.inventory_session_items (
  id uuid primary key default gen_random_uuid(),
  inventory_session_id uuid not null references public.inventory_sessions(id) on delete cascade,
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_legacy_id text,
  item_name text not null,
  expected numeric,
  counted numeric,
  difference numeric generated always as (coalesce(counted,0) - coalesce(expected,0)) stored,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.inventory_sessions enable row level security;
alter table public.inventory_session_items enable row level security;

-- Makni stare armory-only / preview policyje koji blokiraju anon import u open preview modu.
drop policy if exists "Armory full access" on public.inventory_sessions;
drop policy if exists "SOV v4.77 open select" on public.inventory_sessions;
drop policy if exists "SOV v4.77 open insert" on public.inventory_sessions;
drop policy if exists "SOV v4.77 open update" on public.inventory_sessions;
drop policy if exists "SOV v4.77 open delete" on public.inventory_sessions;
drop policy if exists "SOV v4.87 open select" on public.inventory_sessions;
drop policy if exists "SOV v4.87 open insert" on public.inventory_sessions;
drop policy if exists "SOV v4.87 open update" on public.inventory_sessions;
drop policy if exists "SOV v4.87 open delete" on public.inventory_sessions;

drop policy if exists "Armory full access" on public.inventory_session_items;
drop policy if exists "SOV v4.77 open select" on public.inventory_session_items;
drop policy if exists "SOV v4.77 open insert" on public.inventory_session_items;
drop policy if exists "SOV v4.77 open update" on public.inventory_session_items;
drop policy if exists "SOV v4.77 open delete" on public.inventory_session_items;
drop policy if exists "SOV v4.87 open select" on public.inventory_session_items;
drop policy if exists "SOV v4.87 open insert" on public.inventory_session_items;
drop policy if exists "SOV v4.87 open update" on public.inventory_session_items;
drop policy if exists "SOV v4.87 open delete" on public.inventory_session_items;

-- FULL OPEN PREVIEW: svi s linkom mogu testirati inventuru.
create policy "SOV v4.87 open select" on public.inventory_sessions for select using (true);
create policy "SOV v4.87 open insert" on public.inventory_sessions for insert with check (true);
create policy "SOV v4.87 open update" on public.inventory_sessions for update using (true) with check (true);
create policy "SOV v4.87 open delete" on public.inventory_sessions for delete using (true);

create policy "SOV v4.87 open select" on public.inventory_session_items for select using (true);
create policy "SOV v4.87 open insert" on public.inventory_session_items for insert with check (true);
create policy "SOV v4.87 open update" on public.inventory_session_items for update using (true) with check (true);
create policy "SOV v4.87 open delete" on public.inventory_session_items for delete using (true);

grant select, insert, update, delete on public.inventory_sessions to anon, authenticated;
grant select, insert, update, delete on public.inventory_session_items to anon, authenticated;

commit;
