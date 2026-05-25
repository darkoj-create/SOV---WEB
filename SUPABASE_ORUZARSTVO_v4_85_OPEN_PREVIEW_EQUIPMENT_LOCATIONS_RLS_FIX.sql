-- SOV Oružarstvo v4.85 — FIX: equipment_locations RLS za import
-- Problem: import upisuje u public.equipment_locations, a v4.77 open preview nije otvorio tu tablicu.
-- Simptom: new row violates row-level security policy for table "equipment_locations".
-- Pokrenuti jednom. Ne brise podatke.

begin;

create extension if not exists pgcrypto;

-- Osiguraj da tablica postoji ako se SQL izvodi na nepotpunoj bazi.
create table if not exists public.equipment_locations (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  name text not null,
  description text,
  type text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.equipment_locations enable row level security;

-- Ocisti stare/konfliktne preview policyje za ovu tablicu.
drop policy if exists "Approved users read equipment locations" on public.equipment_locations;
drop policy if exists "SOV v4.77 open select" on public.equipment_locations;
drop policy if exists "SOV v4.77 open insert" on public.equipment_locations;
drop policy if exists "SOV v4.77 open update" on public.equipment_locations;
drop policy if exists "SOV v4.77 open delete" on public.equipment_locations;
drop policy if exists "SOV v4.85 open select" on public.equipment_locations;
drop policy if exists "SOV v4.85 open insert" on public.equipment_locations;
drop policy if exists "SOV v4.85 open update" on public.equipment_locations;
drop policy if exists "SOV v4.85 open delete" on public.equipment_locations;

-- FULL OPEN PREVIEW za lokacije: citanje, import, edit, brisanje.
create policy "SOV v4.85 open select"
on public.equipment_locations
for select
using (true);

create policy "SOV v4.85 open insert"
on public.equipment_locations
for insert
with check (true);

create policy "SOV v4.85 open update"
on public.equipment_locations
for update
using (true)
with check (true);

create policy "SOV v4.85 open delete"
on public.equipment_locations
for delete
using (true);

-- Za Supabase/PostgREST anon/auth pristup u preview modu.
grant select, insert, update, delete on public.equipment_locations to anon, authenticated;

commit;
