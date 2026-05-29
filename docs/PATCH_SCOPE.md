# v1.0.148 patch scope

Implemented requested Izleti archive cutoff fix.

Changed:
1. `sheetTripIsOver()` now treats a trip as past once its parsed end date/time is before the current time.
2. Removed the previous extra 24-hour grace period that kept yesterday's trips in the main shared schedule.
3. Bumped app version to 1.0.148 / versionCode 445.
4. Updated `update.json`, `build.json`, README, changelog and baseline notes.

Not changed:
- Weather fetching implementation.
- Raspored auta / transport table button behavior.
- Sheet sync endpoints or Apps Script contract.
- Shared trip card layout apart from which list past trips appear in.


## Izleti correct deployed GS URL update

- Fixed `FieldPackageSheetSyncClient.FIXED_WEBAPP_URL` to the confirmed deployed shared Izleti Apps Script endpoint.
- Bundled the matching current Apps Script as `docs/scripts/FIELD_TRIPS_SHEET_WEBAPP_v1_0_89_CURRENT_DEPLOYED.gs`.
- App now sends both `driving=TRUE/FALSE` and legacy `vozim=da/ne` when signing up, so the current v1.0.89 GS correctly writes the car marker.
