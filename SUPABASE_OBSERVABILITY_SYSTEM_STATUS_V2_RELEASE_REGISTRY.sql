-- Administrative registry companion for SUPABASE_OBSERVABILITY_SYSTEM_STATUS_V2.sql
-- Production equivalent was applied in migration sov_observability_system_status_v2.

insert into public.sov_release_registry(build_version, build_name, base_build, sql_files, notes)
values (
  'backend-2026.07.20-observability-v2',
  'SOV observability and System status v2',
  '2026.06.27',
  array['sov_observability_system_status_v2'],
  'Adds admin-only status snapshot RPC, crash/error aggregation indexes and ecosystem contract alignment.'
)
on conflict (build_version) do update
set build_name = excluded.build_name,
    base_build = excluded.base_build,
    sql_files = excluded.sql_files,
    notes = excluded.notes;
