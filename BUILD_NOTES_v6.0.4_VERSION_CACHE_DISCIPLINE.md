# SOV web v6.0.5 — version/cache discipline

Base: `sov-web-build-v6.0.3-velebiten-submission-form-export.zip`

## Purpose
This is the first safe stabilization build from the audit plan. It does not change database schema or data. It only introduces version discipline, cache-control headers, and visible version consistency across key production pages.

## Changed
- `update.json`, `VERSION.txt`, and `BUILD_VERSION.txt` bumped to `v6.0.5`.
- Added `vercel.json` with no-store/no-cache rules for manifests and operational pages.
- Added `assets/sov-version.js` as a frontend version helper.
- Updated visible versions on dashboard, sync-status, tracking, role-manager, Oružarstvo and related pages.
- Added a Build/cache discipline card to `sync-status.html`.
- Updated armory cache labels/cache-bust from v601/v602/v603 to v605.

## SQL
No SQL required.

## Smoke test
1. Open `update.json?cb=605` and confirm version `6.0.5`.
2. Open `sync-status.html?b=604`, click `Provjeri sve`, and confirm `Version contract` is green.
3. Open `dashboard.html?b=604`, `oruzarstvo.html?b=604`, `tracking.html?b=604`, `role-manager.html?b=604`.
4. Confirm no page still displays old production build labels such as `v5.59.5`, `v5.58.26`, or `v6.0.3` in the visible shell.

## Rollback
Rollback to v6.0.3 is safe because this release has no database migration.
