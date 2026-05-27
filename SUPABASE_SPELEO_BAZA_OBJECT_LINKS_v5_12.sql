-- SOV Speleo Baza v5.12 — SAFE object-linked modules
-- SIGURNO: ne mijenja live JSON bazu i ne prebacuje Baza prikaz na SQL.
-- Dodaje staging-linked tablice za nacrte/dokumente/zahvate i audit view za SQL sandbox.

create extension if not exists pgcrypto;

-- Veza na objekt ide preko source_id iz speleo_objects_staging.
-- Ne radimo foreign key constraint namjerno, da test ne pukne ako staging nije importan.
create table if not exists public.speleo_object_links_staging (
  id uuid primary key default gen_random_uuid(),
  source_id text not null,
  link_type text not null check (link_type in ('drawing','document','photo','track','other')),
  title text not null,
  url text,
  drive_file_id text,
  file_name text,
  mime_type text,
  match_status text not null default 'manual',
  note text,
  metadata jsonb not null default '{}'::jsonb,
  created_by text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists speleo_object_links_staging_source_idx on public.speleo_object_links_staging (source_id, link_type, created_at desc);
create index if not exists speleo_object_links_staging_drive_idx on public.speleo_object_links_staging (drive_file_id);

create table if not exists public.speleo_object_reports_staging (
  id uuid primary key default gen_random_uuid(),
  source_id text not null,
  report_date date,
  object_name text,
  plate_number text,
  report_type text,
  performed_actions text,
  team_members text,
  hydrology text,
  hydrogeology text,
  hazards text,
  pollution text,
  notes text,
  raw_payload jsonb not null default '{}'::jsonb,
  created_by text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists speleo_object_reports_staging_source_idx on public.speleo_object_reports_staging (source_id, report_date desc, created_at desc);

create table if not exists public.speleo_object_audit_staging (
  id bigserial primary key,
  source_id text not null,
  action text not null,
  actor text,
  note text,
  before_snapshot jsonb,
  after_snapshot jsonb,
  created_at timestamptz not null default now()
);

create index if not exists speleo_object_audit_staging_source_idx on public.speleo_object_audit_staging (source_id, created_at desc);

alter table public.speleo_object_links_staging enable row level security;
alter table public.speleo_object_reports_staging enable row level security;
alter table public.speleo_object_audit_staging enable row level security;

-- OPEN PREVIEW policies zbog trenutnog test moda. Kasnije suziti na admin/arhivar.
drop policy if exists "open preview select speleo links staging" on public.speleo_object_links_staging;
create policy "open preview select speleo links staging" on public.speleo_object_links_staging for select to anon, authenticated using (true);
drop policy if exists "open preview insert speleo links staging" on public.speleo_object_links_staging;
create policy "open preview insert speleo links staging" on public.speleo_object_links_staging for insert to anon, authenticated with check (true);
drop policy if exists "open preview update speleo links staging" on public.speleo_object_links_staging;
create policy "open preview update speleo links staging" on public.speleo_object_links_staging for update to anon, authenticated using (true) with check (true);

-- Namjerno nema DELETE policy za linkove; koristi hide/ignore kasnije ako treba.

drop policy if exists "open preview select speleo reports staging" on public.speleo_object_reports_staging;
create policy "open preview select speleo reports staging" on public.speleo_object_reports_staging for select to anon, authenticated using (true);
drop policy if exists "open preview insert speleo reports staging" on public.speleo_object_reports_staging;
create policy "open preview insert speleo reports staging" on public.speleo_object_reports_staging for insert to anon, authenticated with check (true);
drop policy if exists "open preview update speleo reports staging" on public.speleo_object_reports_staging;
create policy "open preview update speleo reports staging" on public.speleo_object_reports_staging for update to anon, authenticated using (true) with check (true);

-- Namjerno nema DELETE policy za izvještaje.

drop policy if exists "open preview select speleo audit staging" on public.speleo_object_audit_staging;
create policy "open preview select speleo audit staging" on public.speleo_object_audit_staging for select to anon, authenticated using (true);
drop policy if exists "open preview insert speleo audit staging" on public.speleo_object_audit_staging;
create policy "open preview insert speleo audit staging" on public.speleo_object_audit_staging for insert to anon, authenticated with check (true);

-- Helper: view-like function nije potrebna; frontend čita tablice direktno.
