# SOV web v6.0.6 — Oružarstvo single boot

## Purpose
Stabilization build after v6.0.5. This release collapses the visible Oružarstvo catalog boot into one explicit external layer so `/oruzarstvo.html` cannot remain stuck on a loader while waiting for Supabase, requests or legacy inline fragments.

## Contract
1. Render a loading skeleton immediately.
2. Load `data/oruzarstvo-data.json` with `cache: no-store`.
3. Render the member catalog from local JSON immediately.
4. Load requests and auth lightly in the background.
5. Try Supabase live catalog in the background with timeout.
6. If Supabase fails, keep the static catalog visible and show only a discreet status.

## Changed files
- `oruzarstvo.html`
- `assets/oruzarstvo-boot-v606.js`
- `assets/oruzarstvo-supabase.js`
- `sync-status.html`
- `update.json`
- `VERSION.txt`
- `BUILD_VERSION.txt`

## SQL
No SQL required.

## Cache
Active armory cache key: `sov_armory_catalog_cache_v606`.
Old v605/v604/v548 keys are purged by the boot layer.

## Rollback
Rollback to v6.0.5 is frontend-only. No database changes are introduced by this build.
