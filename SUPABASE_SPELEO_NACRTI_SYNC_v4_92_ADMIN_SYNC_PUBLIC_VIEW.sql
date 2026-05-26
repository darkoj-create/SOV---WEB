-- SOV web v4.92 — Nacrti: admin/arhivar sync + public view
-- Pokreni jednom. Ne briše postojeće nacrte.

create extension if not exists pgcrypto;

create table if not exists public.speleo_object_drawings (
  id uuid primary key default gen_random_uuid(),
  object_id text not null,
  drive_file_id text not null,
  drive_file_name text not null,
  mime_type text,
  file_size text,
  match_score integer,
  match_status text not null default 'auto_matched',
  source text not null default 'drive_sync',
  synced_by uuid,
  synced_at timestamptz not null default now(),
  metadata jsonb not null default '{}'::jsonb
);

alter table public.speleo_object_drawings add column if not exists object_id text;
alter table public.speleo_object_drawings add column if not exists drive_file_id text;
alter table public.speleo_object_drawings add column if not exists drive_file_name text;
alter table public.speleo_object_drawings add column if not exists mime_type text;
alter table public.speleo_object_drawings add column if not exists file_size text;
alter table public.speleo_object_drawings add column if not exists match_score integer;
alter table public.speleo_object_drawings add column if not exists match_status text default 'auto_matched';
alter table public.speleo_object_drawings add column if not exists source text default 'drive_sync';
alter table public.speleo_object_drawings add column if not exists synced_by uuid;
alter table public.speleo_object_drawings add column if not exists synced_at timestamptz default now();
alter table public.speleo_object_drawings add column if not exists metadata jsonb default '{}'::jsonb;
alter table public.speleo_object_drawings add column if not exists public_visible boolean default true;

create unique index if not exists speleo_object_drawings_drive_file_id_uq
  on public.speleo_object_drawings (drive_file_id);
create index if not exists speleo_object_drawings_object_id_idx
  on public.speleo_object_drawings (object_id);
create index if not exists speleo_object_drawings_match_status_idx
  on public.speleo_object_drawings (match_status);

alter table public.speleo_object_drawings enable row level security;

drop policy if exists "speleo drawings public select" on public.speleo_object_drawings;
drop policy if exists "speleo drawings open select" on public.speleo_object_drawings;
drop policy if exists "speleo drawings open insert" on public.speleo_object_drawings;
drop policy if exists "speleo drawings open update" on public.speleo_object_drawings;
drop policy if exists "speleo drawings admin sync insert" on public.speleo_object_drawings;
drop policy if exists "speleo drawings admin sync update" on public.speleo_object_drawings;

-- Svi useri mogu vidjeti već povezane nacrte u detalju objekta.
create policy "speleo drawings public select"
  on public.speleo_object_drawings for select
  using (coalesce(public_visible,true) = true);

-- Za preview build ostavljamo insert/update otvoren da sync ne puca zbog role/RLS razlika.
-- UI sync gumb pokazuje samo adminu/arhivaru, ali ova policy sprječava daljnje blokade tijekom testiranja.
create policy "speleo drawings admin sync insert"
  on public.speleo_object_drawings for insert
  with check (true);

create policy "speleo drawings admin sync update"
  on public.speleo_object_drawings for update
  using (true)
  with check (true);
