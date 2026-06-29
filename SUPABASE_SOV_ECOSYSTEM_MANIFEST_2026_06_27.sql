-- SOV Ecosystem Manifest / Backend Contract
-- Safe schema addition: creates a small manifest table and read-only current view.

create table if not exists public.sov_ecosystem_manifest (
  id text primary key default 'current',
  backend_contract text not null,
  web_version text not null,
  apk_min_version text not null,
  apk_target_version text not null,
  supabase_project_ref text not null,
  release_channel text not null default 'baseline',
  notes jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.sov_ecosystem_manifest enable row level security;

drop policy if exists sov_ecosystem_manifest_read_all on public.sov_ecosystem_manifest;
create policy sov_ecosystem_manifest_read_all
on public.sov_ecosystem_manifest
for select
to anon, authenticated
using (id = 'current');

insert into public.sov_ecosystem_manifest (
  id, backend_contract, web_version, apk_min_version, apk_target_version, supabase_project_ref, release_channel, notes, updated_at
) values (
  'current',
  '2026.06.27',
  '6.1.45-ecosystem-baseline',
  '1.4.28-ecosystem-baseline',
  '1.4.28-ecosystem-baseline',
  'ncomefzkuixyfixisrhi',
  'baseline',
  '["Instrumentation baseline only", "Security hardening is separate", "No DOCX files are deployed in web bundle"]'::jsonb,
  now()
)
on conflict (id) do update set
  backend_contract = excluded.backend_contract,
  web_version = excluded.web_version,
  apk_min_version = excluded.apk_min_version,
  apk_target_version = excluded.apk_target_version,
  supabase_project_ref = excluded.supabase_project_ref,
  release_channel = excluded.release_channel,
  notes = excluded.notes,
  updated_at = now();

create or replace view public.sov_ecosystem_manifest_current
with (security_invoker = true)
as
select * from public.sov_ecosystem_manifest where id = 'current';
