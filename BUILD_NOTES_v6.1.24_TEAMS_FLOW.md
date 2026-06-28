# SOV Web v6.1.24 — Teams Flow

Baseline: `sov-web-build-v6.1.23-trips-loading-guard.zip`

## Changed
- Added simple teams section inside trip detail modal on `izleti-cloud.html`.
- Teams can be added, edited and deleted from trip details.
- Added `listTripTeams`, `saveTripTeam`, `deleteTripTeam` to `assets/sov-trips-cloud.js`.
- Cache-busted trips JS to `v=6.1.24`.

## SQL
Requires `SUPABASE_SOV_TRIP_TEAMS_FLOW_v6_1_24.sql`.

## Not changed
- Trips list/save/delete baseline from v6.1.23 is not replaced.
- Calendar logic is not changed.
- Documents/Armory/Archive modules are not changed.
