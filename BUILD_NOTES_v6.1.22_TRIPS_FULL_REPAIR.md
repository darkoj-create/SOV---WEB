# SOV Web v6.1.22 — Trips full repair

This build fixes the regression where the trips database disappears after the delete-only v6.1.21 patch.

Cause: v6.1.21 was only a delete patch. If it was run without the full v6.1.20 repair, the web could call `sov_list_trips_feed()` while the database did not have the full feed/save repair installed.

This package includes one all-in-one SQL:

- `SUPABASE_SOV_TRIPS_FULL_REPAIR_v6_1_22.sql`

It creates/repairs:

- `sov_trips` required columns
- `end_date`
- `trip_category`
- `sov_list_trips_feed()`
- `sov_save_trip()`
- `sov_delete_trip_admin()`
- `sov_trips_sheet_view`
- `sov_trips_mobile_feed`
- child tables used by trip counts/files/members

Deploy order:

1. Run `SUPABASE_SOV_TRIPS_FULL_REPAIR_v6_1_22.sql`.
2. Deploy web ZIP.
3. Hard refresh browser.
