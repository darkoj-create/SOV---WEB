-- SOV v4.99 Arhivar: Zahvati i radnje izvještaji
create extension if not exists pgcrypto;

create table if not exists public.speleo_activity_reports (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  created_by uuid null,
  created_by_email text null,
  object_name text not null,
  plate_number text null,
  object_type text null,
  hydrology text[] not null default '{}',
  hydrogeology text[] not null default '{}',
  nearest_place text null,
  coordinate_system text null,
  x_coord text null,
  y_coord text null,
  date_start date null,
  date_end date null,
  purpose text null,
  purpose_other text null,
  activity_description text null,
  execution_method text null,
  observed_threats text[] not null default '{}',
  bat_colony text null,
  bivalve text null,
  sponge text null,
  olm text null,
  fossils text null,
  members text null,
  note text null,
  raw jsonb not null default '{}'::jsonb
);

alter table public.speleo_activity_reports enable row level security;

drop policy if exists "speleo_activity_reports_select_open" on public.speleo_activity_reports;
drop policy if exists "speleo_activity_reports_insert_open" on public.speleo_activity_reports;
drop policy if exists "speleo_activity_reports_update_archive" on public.speleo_activity_reports;

create policy "speleo_activity_reports_select_open"
on public.speleo_activity_reports for select
using (true);

create policy "speleo_activity_reports_insert_open"
on public.speleo_activity_reports for insert
to anon, authenticated
with check (true);

create policy "speleo_activity_reports_update_archive"
on public.speleo_activity_reports for update
to anon, authenticated
using (true)
with check (true);

create index if not exists idx_speleo_activity_reports_object_name on public.speleo_activity_reports using gin (to_tsvector('simple', coalesce(object_name,'')));
create index if not exists idx_speleo_activity_reports_created_at on public.speleo_activity_reports (created_at desc);
