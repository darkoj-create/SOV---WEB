# SOV Web v6.1.13 — Trip delete on normal dashboard

## Change
- Added visible **Obriši iz baze** button next to every trip card on the normal `Izleti` dashboard (`izleti-cloud.html`).
- Added the same destructive action in the trip detail panel.
- Delete action uses existing `SOVTripsCloud.deleteTrip(id)` / Supabase delete flow.
- Delete action requires two browser confirmation prompts before deleting.
- After delete, the trip list refreshes and the detail panel clears if the selected trip was removed.

## Not changed
- No SQL changes.
- No APK changes.
- Calendar delete from v6.1.12 remains unchanged.
