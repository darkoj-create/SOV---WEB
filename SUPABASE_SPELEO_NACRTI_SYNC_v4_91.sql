-- SOV web v4.91 — Speleo nacrti / TopoDroid sync
-- Svi useri mogu syncati/pregledati indeks nacrta. Folder link se ne izlaže u UI-u.

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

create unique index if not exists speleo_object_drawings_drive_file_id_uq
  on public.speleo_object_drawings (drive_file_id);
create index if not exists speleo_object_drawings_object_id_idx
  on public.speleo_object_drawings (object_id);
create index if not exists speleo_object_drawings_match_status_idx
  on public.speleo_object_drawings (match_status);

alter table public.speleo_object_drawings enable row level security;

drop policy if exists "speleo drawings open select" on public.speleo_object_drawings;
drop policy if exists "speleo drawings open insert" on public.speleo_object_drawings;
drop policy if exists "speleo drawings open update" on public.speleo_object_drawings;

create policy "speleo drawings open select"
  on public.speleo_object_drawings for select
  using (true);

create policy "speleo drawings open insert"
  on public.speleo_object_drawings for insert
  with check (true);

create policy "speleo drawings open update"
  on public.speleo_object_drawings for update
  using (true)
  with check (true);
