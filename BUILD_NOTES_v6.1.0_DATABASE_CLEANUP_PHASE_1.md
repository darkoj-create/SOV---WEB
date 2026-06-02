# SOV web v6.1.0 — Database cleanup phase 1

Base: `sov-web-build-v6.0.9-predane-jame-workflow-hardening.zip`

## Goal

Start bringing order to the database without risky destructive changes.

## Added

- `SUPABASE_SOV_DATABASE_CLEANUP_PHASE_1_v6_1_0.sql`
- `DATABASE_MAP.md`
- `sov_schema_registry`
- `sov_release_registry`
- `sov_audit_log`
- `sov_armory_taxonomy`
- `sov_database_cleanup_health()`
- `sov_log_event()`

## Frontend

- `sync-status.html` bumped to v6.1.0.
- Added Database cleanup health card.
- `update.json`, `VERSION.txt`, `BUILD_VERSION.txt` bumped to v6.1.0.

## Safety

No table is dropped. No table is renamed. No existing column type is changed. SQL is additive and idempotent.

## Rollback

Frontend rollback: redeploy v6.0.9 ZIP.

SQL rollback is not needed for normal rollback because the patch only adds registry/audit/taxonomy objects and safe RPCs. If you want to remove them manually later, do it only after confirming no v6.1.x pages use them.
