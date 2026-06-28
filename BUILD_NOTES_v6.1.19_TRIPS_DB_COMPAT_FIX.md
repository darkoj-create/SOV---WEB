# SOV Web v6.1.19 — Trips DB compatibility fix

Fix for Izleti dashboard after calendar/detail changes.

## Changed
- `assets/sov-trips-cloud.js`
  - `listTrips()` now falls back from `sov_trips_mobile_feed` to direct `sov_trips` if the feed view is broken/outdated.
  - `createTripFromForm()` retries without new category columns if the DB schema is older.
  - `updateTrip()` retries without new category columns if the DB schema is older.
  - insert/update can succeed even when returning select is restricted by RLS.

## SQL
- Added `SUPABASE_SOV_TRIPS_DASHBOARD_DB_COMPAT_v6_1_19.sql`
- Ensures `end_date` and `trip_category` exist.
- Recreates `sov_trips_sheet_view` and `sov_trips_mobile_feed` in a dependency-safe way.

## Not changed
- No delete logic changes.
- No APK changes.
