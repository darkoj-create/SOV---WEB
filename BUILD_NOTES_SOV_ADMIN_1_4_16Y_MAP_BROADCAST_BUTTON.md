# SOV Admin v1.4.16y — Map Broadcast Button

Baseline: `v1.4.16x-team-broadcast-build-fix`.

## Fixed

- Team broadcast messages are now visible directly on the map/tracking toolbar.
- Added a small `💬 Msg` floating map button.
- The button appears only when Field Team Tracking is active for the selected trip/team.
- Tapping the button opens a clean bottom-sheet message panel.
- Message panel supports:
  - recipient scope: `Moj team` / `Svi`
  - fetch messages
  - send short message
  - local queued state when signal/API fails
- No web changes.
- No SQL changes beyond existing `SUPABASE_SOV_TRIP_MESSAGES_v6_1_25.sql`.

## Version

- versionCode: 900101
- versionName: 1.4.16y-map-broadcast-button
