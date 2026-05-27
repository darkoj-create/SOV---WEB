-- SOV Speleo Baza v5.15 CONSOLIDATED SQL GO LIVE
-- Pokreni jednom. Idempotentno: create if not exists / drop policy if exists.
begin;

-- ===== SUPABASE_SPELEO_BAZA_SAFE_STAGING_v5_08.sql =====
-- SOV Speleo Baza SAFE SQL staging v5.08
-- Ovo NE prebacuje web na SQL i NE dira postojeću Baza stranicu.
-- Služi samo za siguran import/provjeru podataka prije buduće migracije.

create table if not exists public.speleo_objects_staging (
  source_id text primary key,
  source_system text not null default 'baza_velebit_2026_appready',
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
  raw jsonb not null default '{}'::jsonb,
  import_batch text,
  imported_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists speleo_objects_staging_name_idx on public.speleo_objects_staging using gin (to_tsvector('simple', coalesce(name,'')));
create index if not exists speleo_objects_staging_coord_idx on public.speleo_objects_staging (lat, lon);
create index if not exists speleo_objects_staging_status_idx on public.speleo_objects_staging (record_status, cadastre_status);

create or replace function public.touch_speleo_objects_staging_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_touch_speleo_objects_staging on public.speleo_objects_staging;
create trigger trg_touch_speleo_objects_staging
before update on public.speleo_objects_staging
for each row execute function public.touch_speleo_objects_staging_updated_at();

alter table public.speleo_objects_staging enable row level security;

drop policy if exists "open preview select speleo staging" on public.speleo_objects_staging;
create policy "open preview select speleo staging"
on public.speleo_objects_staging for select
to anon, authenticated
using (true);

drop policy if exists "open preview insert speleo staging" on public.speleo_objects_staging;
create policy "open preview insert speleo staging"
on public.speleo_objects_staging for insert
to anon, authenticated
with check (true);

drop policy if exists "open preview update speleo staging" on public.speleo_objects_staging;
create policy "open preview update speleo staging"
on public.speleo_objects_staging for update
to anon, authenticated
using (true)
with check (true);

-- Namjerno nema DELETE policy.


-- ===== SUPABASE_SPELEO_BAZA_SQL_EDIT_SANDBOX_v5_09.sql =====
-- SOV Speleo Baza SQL EDIT SANDBOX v5.09
-- SIGURNO: radi samo sa staging tablicom speleo_objects_staging.
-- NE prebacuje live Baza prikaz na SQL i NE dira JSON izvor.

-- Osnovna staging tablica iz v5.08, ako još nije pokrenuta.
create table if not exists public.speleo_objects_staging (
  source_id text primary key,
  source_system text not null default 'baza_velebit_2026_appready',
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
  raw jsonb not null default '{}'::jsonb,
  import_batch text,
  imported_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.speleo_objects_staging add column if not exists edit_note text;
alter table public.speleo_objects_staging add column if not exists sandbox_status text not null default 'staging';
alter table public.speleo_objects_staging add column if not exists edited_by text;
alter table public.speleo_objects_staging add column if not exists edited_at timestamptz;

create index if not exists speleo_objects_staging_name_idx on public.speleo_objects_staging using gin (to_tsvector('simple', coalesce(name,'')));
create index if not exists speleo_objects_staging_coord_idx on public.speleo_objects_staging (lat, lon);
create index if not exists speleo_objects_staging_status_idx on public.speleo_objects_staging (record_status, cadastre_status);

create or replace function public.touch_speleo_objects_staging_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  if new is distinct from old then
    new.edited_at = now();
  end if;
  return new;
end;
$$;

drop trigger if exists trg_touch_speleo_objects_staging on public.speleo_objects_staging;
create trigger trg_touch_speleo_objects_staging
before update on public.speleo_objects_staging
for each row execute function public.touch_speleo_objects_staging_updated_at();

-- Audit log za sandbox izmjene.
create table if not exists public.speleo_objects_staging_edits (
  id bigserial primary key,
  source_id text not null,
  action text not null check (action in ('insert','update','review','promote_preview')),
  edited_by text,
  note text,
  before_data jsonb,
  after_data jsonb,
  created_at timestamptz not null default now()
);

create index if not exists speleo_objects_staging_edits_source_idx on public.speleo_objects_staging_edits (source_id, created_at desc);

alter table public.speleo_objects_staging enable row level security;
alter table public.speleo_objects_staging_edits enable row level security;

-- OPEN PREVIEW policies zbog trenutnog full-open web test moda.
-- Kasnije ovo suziti na admin/arhivar.
drop policy if exists "open preview select speleo staging" on public.speleo_objects_staging;
create policy "open preview select speleo staging"
on public.speleo_objects_staging for select
to anon, authenticated
using (true);

drop policy if exists "open preview insert speleo staging" on public.speleo_objects_staging;
create policy "open preview insert speleo staging"
on public.speleo_objects_staging for insert
to anon, authenticated
with check (true);

drop policy if exists "open preview update speleo staging" on public.speleo_objects_staging;
create policy "open preview update speleo staging"
on public.speleo_objects_staging for update
to anon, authenticated
using (true)
with check (true);

-- Namjerno nema DELETE policy za staging objekte.

drop policy if exists "open preview select speleo staging edits" on public.speleo_objects_staging_edits;
create policy "open preview select speleo staging edits"
on public.speleo_objects_staging_edits for select
to anon, authenticated
using (true);

drop policy if exists "open preview insert speleo staging edits" on public.speleo_objects_staging_edits;
create policy "open preview insert speleo staging edits"
on public.speleo_objects_staging_edits for insert
to anon, authenticated
with check (true);

-- Helper view za pregled problematičnih/izmijenjenih objekata.
create or replace view public.speleo_objects_staging_review as
select
  source_id,
  name,
  lat,
  lon,
  cadastre_status,
  record_status,
  object_type_final,
  county,
  municipality,
  nearest_place,
  locality,
  depth_m,
  length_m,
  field_tasks,
  sandbox_status,
  edit_note,
  edited_by,
  edited_at,
  updated_at
from public.speleo_objects_staging
where sandbox_status <> 'staging'
   or edit_note is not null
   or field_tasks is not null
order by updated_at desc;


-- ===== SUPABASE_SPELEO_BAZA_COMPARE_v5_11.sql =====
-- SOV Speleo Baza SAFE COMPARE v5.11
-- SIGURNO: ne mijenja live JSON bazu i ne prebacuje Baza prikaz na SQL.
-- Dodaje samo tablicu za promovirane kandidate i audit log akcija iz compare stranice.

create table if not exists public.speleo_objects_live_candidates (
  source_id text primary key,
  source_system text not null default 'sql_staging_candidate',
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
  candidate_status text not null default 'candidate_ready',
  promoted_by text,
  promoted_at timestamptz not null default now(),
  staging_snapshot jsonb not null default '{}'::jsonb,
  live_json_snapshot jsonb not null default '{}'::jsonb,
  diff_summary jsonb not null default '[]'::jsonb
);

create table if not exists public.speleo_compare_actions (
  id bigserial primary key,
  source_id text not null,
  action text not null,
  actor text,
  note text,
  diff_summary jsonb,
  created_at timestamptz not null default now()
);

create index if not exists speleo_live_candidates_status_idx on public.speleo_objects_live_candidates (candidate_status, promoted_at desc);
create index if not exists speleo_compare_actions_source_idx on public.speleo_compare_actions (source_id, created_at desc);

alter table public.speleo_objects_live_candidates enable row level security;
alter table public.speleo_compare_actions enable row level security;

-- OPEN PREVIEW policies zbog trenutnog demo/test moda. Kasnije suziti na admin/arhivar.
drop policy if exists "open preview select speleo live candidates" on public.speleo_objects_live_candidates;
create policy "open preview select speleo live candidates"
on public.speleo_objects_live_candidates for select
to anon, authenticated
using (true);

drop policy if exists "open preview insert speleo live candidates" on public.speleo_objects_live_candidates;
create policy "open preview insert speleo live candidates"
on public.speleo_objects_live_candidates for insert
to anon, authenticated
with check (true);

drop policy if exists "open preview update speleo live candidates" on public.speleo_objects_live_candidates;
create policy "open preview update speleo live candidates"
on public.speleo_objects_live_candidates for update
to anon, authenticated
using (true)
with check (true);

-- Namjerno nema DELETE policy.

drop policy if exists "open preview select speleo compare actions" on public.speleo_compare_actions;
create policy "open preview select speleo compare actions"
on public.speleo_compare_actions for select
to anon, authenticated
using (true);

drop policy if exists "open preview insert speleo compare actions" on public.speleo_compare_actions;
create policy "open preview insert speleo compare actions"
on public.speleo_compare_actions for insert
to anon, authenticated
with check (true);


-- ===== SUPABASE_SPELEO_BAZA_OBJECT_LINKS_v5_12.sql =====
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


-- ===== SUPABASE_SPELEO_BAZA_PROMOTION_SCHEMA_CACHE_FIX_v5_14.sql =====
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



-- ===== v5.15 app source helpers =====
create or replace view public.speleo_objects_app_source as
select * from public.speleo_objects_live_sql;

-- Promote all staging rows into SQL live in one SQL call if needed.
create or replace function public.speleo_promote_all_staging_to_live(p_label text default 'SQL go live')
returns integer language plpgsql security definer as $$
declare
  v_batch uuid;
  v_count integer;
begin
  select count(*) into v_count from public.speleo_objects_staging;
  insert into public.speleo_sql_promotion_batches(batch_label,status,promoted_by,promoted_count,source_filter,note)
  values (p_label,'promoted','sql_function',v_count,'all staging','Promoted by speleo_promote_all_staging_to_live')
  returning id into v_batch;

  insert into public.speleo_objects_live_sql(
    source_id,source_system,name,lat,lon,cadastre_status,cadastral_number,record_status,object_type_final,county,municipality,nearest_place,locality,depth_m,length_m,field_tasks,workflow_raw,edit_note,raw,promoted_by,promotion_batch_id
  )
  select source_id,'sql_live_from_staging_v5_15',name,lat,lon,cadastre_status,cadastral_number,record_status,object_type_final,county,municipality,nearest_place,locality,depth_m,length_m,field_tasks,workflow_raw,edit_note,raw,'sql_function',v_batch
  from public.speleo_objects_staging
  on conflict (source_id) do update set
    source_system=excluded.source_system,
    name=excluded.name, lat=excluded.lat, lon=excluded.lon,
    cadastre_status=excluded.cadastre_status, cadastral_number=excluded.cadastral_number,
    record_status=excluded.record_status, object_type_final=excluded.object_type_final,
    county=excluded.county, municipality=excluded.municipality, nearest_place=excluded.nearest_place, locality=excluded.locality,
    depth_m=excluded.depth_m, length_m=excluded.length_m, field_tasks=excluded.field_tasks, workflow_raw=excluded.workflow_raw,
    edit_note=excluded.edit_note, raw=excluded.raw, promoted_by=excluded.promoted_by, promotion_batch_id=excluded.promotion_batch_id, updated_at=now();

  return v_count;
end;
$$;

commit;
notify pgrst, 'reload schema';
