# SOV Admin v1.4.16o — Trip delete hard fix

Base: v1.4.16n calendar google style.

## Changes

- APK trip delete now follows the web v6.1.15 hard-delete flow.
- `FieldPackageSheetSyncClient.deleteTrip()` first calls Supabase RPC:
  - `rpc/sov_delete_trip_admin`
  - body: `{ "p_trip_id": "..." }`
- The APK checks the RPC response `deleted == true` before reporting success.
- Added fallback for older databases without the RPC:
  - REST `DELETE /sov_trips?id=eq...&select=id`
  - `Prefer: return=representation`
  - `[]` is treated as failed delete, not success.
- After a successful delete, the trip is removed from the local cached trips list.

## Required SQL

Run the existing web v6.1.15 SQL patch first:

`SUPABASE_SOV_TRIP_DELETE_HARD_FIX_v6_1_15.sql`

## Version

- versionCode: 900091
- versionName: 1.4.16o-trip-delete-hard-fix

## Not changed

- No web changes.
- No new SQL beyond the already delivered v6.1.15 SQL patch.
