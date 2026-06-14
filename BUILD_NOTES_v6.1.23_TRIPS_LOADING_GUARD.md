# SOV Web v6.1.23 — Trips loading guard

Fix for trips dashboard stuck on loading.

- Bumped `sov-trips-cloud.js` asset URL to `v=6.1.23` to break browser cache.
- `listTrips()` now uses timeouts and tolerant RPC JSON parsing.
- If RPC hangs/fails, it falls back to direct `sov_trips` read, then old mobile feed.
- `loadTrips()` can no longer leave the UI permanently on “Učitavam izlete…”.
- If DB fails and cache exists, cached trips are shown.
- If DB fails and no cache exists, status shows a real error state instead of spinner.
