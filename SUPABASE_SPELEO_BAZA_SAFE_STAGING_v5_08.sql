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
