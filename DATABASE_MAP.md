# SOV database map — v6.1.0 cleanup phase 1

This is the first non-destructive database map for the SOV web/cloud ecosystem.

## Rule for this phase

No table is dropped, renamed, or destructively changed in v6.1.0. This build only adds registry/health/taxonomy/audit objects so we can clean the database with less chaos later.

## Core modules

| Module | Current source of truth | Notes |
|---|---|---|
| Identity / roles | `auth.users`, `profiles`, `sov_user_permissions` | `profiles` must exist for every Auth user. Approval and role manager should not rely on Auth-only users being visible manually. |
| Health | `sov_system_health()`, `sov_database_cleanup_health()` | `sync-status.html` should use these first, then fallback detail checks. |
| Oružarstvo | `sov_oruzarstvo_grouped_catalog`, `data/oruzarstvo-data.json`, `sov_armory_taxonomy` | Static JSON renders first; Supabase refreshes in background. Taxonomy table is the new canonical category dictionary. |
| Arhivar / predane jame | `speleo_object_submissions`, `speleo_object_submission_files`, `sov_speleo_submissions_health()` | Workflow statuses: submitted/pending/review/needs_changes/approved/rejected. |
| Baza / karta | `sov_map_objects_page()`, `speleo_objects_staging`, optional `speleo_objects_live_sql` | Paged RPC avoids statement timeouts. |
| Vijesti | `sov_news`, storage bucket `sov-news` | Editor/Admin CMS. |
| Izleti / field packages | trip tables/views, `sov_trip_assets`, bucket `sov-trip-assets` | Keep cloud trip assets separated from static public web. |
| Tracking | `sov_tracking_sync_status` and tracking tables | Lite tracking is a field module; archived trips should hide live functions. |

## New v6.1.0 objects

| Object | Type | Purpose |
|---|---|---|
| `sov_schema_registry` | table | Registry of known database objects and lifecycle status. |
| `sov_release_registry` | table | Registry of applied SOV web/database release patches. |
| `sov_audit_log` | table | Generic append-only log for future admin actions. |
| `sov_armory_taxonomy` | table | Canonical Oružarstvo category/subcategory dictionary. |
| `sov_database_cleanup_health()` | RPC | Read-only health summary for database cleanup phase. |
| `sov_log_event()` | RPC | Safe audit event insert helper. |

## Lifecycle statuses

- `active`: use this going forward.
- `compat`: allowed compatibility source; do not build new features on it unless needed.
- `legacy`: old but still used somewhere.
- `deprecated`: keep temporarily, do not use in new code.
- `unknown`: needs review.

## Next cleanup step

v6.1.1 should read from `sov_schema_registry` in `sync-status.html` and optionally show a Webmaster-only database map table. After that, we can mark legacy SQL files and old views as compatibility/deprecated without deleting anything.
