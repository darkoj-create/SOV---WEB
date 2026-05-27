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
