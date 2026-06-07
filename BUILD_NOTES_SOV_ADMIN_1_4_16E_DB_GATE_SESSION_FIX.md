# SOV Admin 1.4.16e DB Gate Session Fix

Baseline: `sov-admin-v1.4.15d-armory-db-gate-sdk35-source.zip`

## Fixed
- Re-applied session/JWT refresh fix on top of the DB-gate APK baseline, not the older 1.4.15b baseline.
- Added `SovHttpClient.kt` for proactive and reactive Supabase JWT refresh.
- Migrated Supabase REST repositories used by archive, submissions, armory, trip assets, field tracking lite, and field trips sync to token-safe calls.
- Preserved 1.4.15d armory DB-gate behavior and Oruzar/Admin view logic.
- Preserved 1.4.15d future trips / trip sync logic.
- Added max 3 retry guard to armory database loading loop.
- Added queued trip flush before fetching trips.

## Version
- versionCode: 900081
- versionName: 1.4.16e-db-gate-session-fix
