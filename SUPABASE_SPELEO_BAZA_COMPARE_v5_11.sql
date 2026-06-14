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
