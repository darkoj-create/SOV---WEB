-- SOV v1.4.29h CroSpeleo Katastar registry shell
-- Applied to project ncomefzkuixyfixisrhi on 2026-07-03.
-- Actual APK search uses the generated locked asset:
-- app/src/main/assets/katastar_crospeleo_2026_android_v1.json.gz

create table if not exists public.sov_crospeleo_katastar_objects (
  id text primary key,
  katastar_number text,
  name text not null,
  object_type text,
  status text,
  lat double precision,
  lon double precision,
  county text,
  nearest_place text,
  locality text,
  depth_m double precision,
  length_m double precision,
  plate_number text,
  access_description text,
  technical_description text,
  search_text text,
  raw jsonb not null default '{}'::jsonb,
  source_file text not null default 'CroSpeleo - objekti.xlsx',
  imported_at timestamptz not null default now()
);

alter table public.sov_crospeleo_katastar_objects enable row level security;

create policy if not exists sov_crospeleo_katastar_registered_read
on public.sov_crospeleo_katastar_objects
for select to authenticated
using (true);

revoke all on public.sov_crospeleo_katastar_objects from anon;
grant select on public.sov_crospeleo_katastar_objects to authenticated;

create index if not exists sov_crospeleo_katastar_name_idx
on public.sov_crospeleo_katastar_objects using gin (to_tsvector('simple', coalesce(search_text,'')));
create index if not exists sov_crospeleo_katastar_lat_lon_idx
on public.sov_crospeleo_katastar_objects (lat, lon);
create index if not exists sov_crospeleo_katastar_number_idx
on public.sov_crospeleo_katastar_objects (katastar_number);
