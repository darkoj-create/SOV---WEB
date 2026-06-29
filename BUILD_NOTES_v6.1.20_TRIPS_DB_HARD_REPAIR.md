# SOV Web v6.1.20 — Trips DB hard repair

- `assets/sov-trips-cloud.js` now tries `sov_list_trips_feed()` RPC before views/direct table.
- Trip create/update now tries `sov_save_trip()` RPC before direct table fallback.
- Direct fallback now removes missing columns dynamically instead of failing after `trip_category/end_date` schema mismatch.
- Cache-bust updated for `sov-trips-cloud.js?v=6.1.20`.
- SQL patch creates/repairs required trip columns, feed RPC, save RPC, feed views and delete RPC.
