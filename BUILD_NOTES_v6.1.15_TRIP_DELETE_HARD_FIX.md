# SOV Web v6.1.15 — Trip delete hard fix

## Problem
Web delete button could report success even when Supabase/RLS deleted 0 rows, so the trip stayed visible after refresh.

## Fix
- `assets/sov-trips-cloud.js`
  - `deleteTrip(id)` now calls RPC `sov_delete_trip_admin(p_trip_id)` first.
  - fallback direct delete now uses `.select('id')` and throws if 0 rows are deleted.
  - local cache is immediately filtered after successful delete.
- `izleti-cloud.html`
  - dashboard list removes the trip immediately after successful delete.
  - error toast now displays the real error message.
- SQL patch added:
  - `SUPABASE_SOV_TRIP_DELETE_HARD_FIX_v6_1_15.sql`
  - adds Webmaster support to `sov_can_manage_trips_safe()`
  - adds hard delete RPC `sov_delete_trip_admin(uuid)`.

## Deploy
1. Run SQL patch.
2. Deploy web ZIP.
3. Hard refresh browser.
