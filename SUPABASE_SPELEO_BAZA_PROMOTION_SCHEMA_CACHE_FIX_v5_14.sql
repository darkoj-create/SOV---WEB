-- SOV Speleo Baza v5.13 — CONTROLLED SQL PROMOTION
-- SIGURNO: NE mijenja postojeću live JSON Baza stranicu.
-- Kreira odvojenu SQL live tablicu i batch promociju iz staginga u SQL live.
-- Baza web prikaz se NE prebacuje automatski na SQL.

create extension if not exists pgcrypto;

-- SQL live kandidat koji ćemo kasnije, nakon potvrde, koristiti kao pravi source.
create table if not exists public.speleo_objects_live_sql (
  source_id text primary key,
  source_system text not null default 'sql_promoted_from_staging',
  name text not null,
  lat double precision,
  lon double precision,
  cadastre_status text,
  cadastral_number text,
  record_status text,
  object_type_final text,
  county text,
  municipality text,
  nearest_place text,
  locality text,
  depth_m double precision,
  length_m double precision,
  field_tasks text,
  workflow_raw text,
  edit_note text,
  raw jsonb not null default '{}'::jsonb,
  promoted_from_staging_at timestamptz not null default now(),
  promoted_by text,
  promotion_batch_id uuid,
  updated_at timestamptz not null default now()
);

create index if not exists speleo_objects_live_sql_name_idx on public.speleo_objects_live_sql using gin (to_tsvector('simple', coalesce(name,'')));
create index if not exists speleo_objects_live_sql_coord_idx on public.speleo_objects_live_sql (lat, lon);
create index if not exists speleo_objects_live_sql_status_idx on public.speleo_objects_live_sql (record_status, cadastre_status);
create index if not exists speleo_objects_live_sql_batch_idx on public.speleo_objects_live_sql (promotion_batch_id);

create table if not exists public.speleo_sql_promotion_batches (
  id uuid primary key default gen_random_uuid(),
  batch_label text,
  status text not null default 'promoted' check (status in ('preview','promoted','rolled_back')),
  promoted_by text,
  promoted_count integer not null default 0,
  source_filter text,
  note text,
  created_at timestamptz not null default now(),
  rolled_back_at timestamptz,
  rollback_note text
);

create table if not exists public.speleo_sql_promotion_audit (
  id bigserial primary key,
  batch_id uuid,
  source_id text not null,
  action text not null check (action in ('promote_insert','promote_update','rollback_restore','rollback_delete','manual_note')),
  actor text,
  before_live jsonb,
  staging_snapshot jsonb,
  after_live jsonb,
  created_at timestamptz not null default now()
);

create index if not exists speleo_sql_promotion_audit_batch_idx on public.speleo_sql_promotion_audit (batch_id, created_at desc);
create index if not exists speleo_sql_promotion_audit_source_idx on public.speleo_sql_promotion_audit (source_id, created_at desc);

create or replace function public.touch_speleo_objects_live_sql_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_touch_speleo_objects_live_sql on public.speleo_objects_live_sql;
create trigger trg_touch_speleo_objects_live_sql
before update on public.speleo_objects_live_sql
for each row execute function public.touch_speleo_objects_live_sql_updated_at();

alter table public.speleo_objects_live_sql enable row level security;
alter table public.speleo_sql_promotion_batches enable row level security;
alter table public.speleo_sql_promotion_audit enable row level security;

-- OPEN PREVIEW policies za trenutni test/demo način. Kasnije suziti na admin/arhivar.
drop policy if exists "open preview select speleo live sql" on public.speleo_objects_live_sql;
create policy "open preview select speleo live sql" on public.speleo_objects_live_sql for select to anon, authenticated using (true);
drop policy if exists "open preview insert speleo live sql" on public.speleo_objects_live_sql;
create policy "open preview insert speleo live sql" on public.speleo_objects_live_sql for insert to anon, authenticated with check (true);
drop policy if exists "open preview update speleo live sql" on public.speleo_objects_live_sql;
create policy "open preview update speleo live sql" on public.speleo_objects_live_sql for update to anon, authenticated using (true) with check (true);
-- Namjerno nema DELETE policy za live SQL objekte.

drop policy if exists "open preview select speleo promotion batches" on public.speleo_sql_promotion_batches;
create policy "open preview select speleo promotion batches" on public.speleo_sql_promotion_batches for select to anon, authenticated using (true);
drop policy if exists "open preview insert speleo promotion batches" on public.speleo_sql_promotion_batches;
create policy "open preview insert speleo promotion batches" on public.speleo_sql_promotion_batches for insert to anon, authenticated with check (true);
drop policy if exists "open preview update speleo promotion batches" on public.speleo_sql_promotion_batches;
create policy "open preview update speleo promotion batches" on public.speleo_sql_promotion_batches for update to anon, authenticated using (true) with check (true);

drop policy if exists "open preview select speleo promotion audit" on public.speleo_sql_promotion_audit;
create policy "open preview select speleo promotion audit" on public.speleo_sql_promotion_audit for select to anon, authenticated using (true);
drop policy if exists "open preview insert speleo promotion audit" on public.speleo_sql_promotion_audit;
create policy "open preview insert speleo promotion audit" on public.speleo_sql_promotion_audit for insert to anon, authenticated with check (true);

-- View za usporedbu staging vs SQL live.
create or replace view public.speleo_sql_promotion_status as
select
  s.source_id,
  s.name as staging_name,
  l.name as live_sql_name,
  case
    when l.source_id is null then 'not_promoted'
    when coalesce(s.updated_at, s.imported_at) > l.updated_at then 'staging_newer'
    else 'in_sync_or_live_newer'
  end as status,
  s.updated_at as staging_updated_at,
  l.updated_at as live_sql_updated_at,
  l.promotion_batch_id
from public.speleo_objects_staging s
left join public.speleo_objects_live_sql l on l.source_id = s.source_id;

-- v5.14 hardfix: force PostgREST/Supabase schema cache refresh
notify pgrst, 'reload schema';
