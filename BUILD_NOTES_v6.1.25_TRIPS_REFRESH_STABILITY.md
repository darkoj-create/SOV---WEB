# SOV Web v6.1.25 — Trips Refresh Stability

Baseline: `sov-web-build-v6.1.24-teams-flow.zip`.

## Changed
- Centralized web trips loading flow in `izleti-cloud.html`.
- Added guarded request token/loading state so older/parallel loads cannot overwrite newer state.
- Trips cache is now only a temporary display, not the final state.
- Create/edit/delete update the local list immediately and then run a silent background reload.
- Removed user-facing cached-data toast during normal fallback.
- Cache-busted `assets/sov-trips-cloud.js` to `v=6.1.25`.
- Updated trips cache key and kept legacy cache migration.
- Added single in-flight guard inside `SOVTripsCloud.listTrips()`.

## Not changed
- No SQL changes.
- No APK changes.
- No teams schema changes.
