# SOV Admin APK v1.4.16v — Teams Flow

Baseline: `sov-admin-v1_4_16u-calendar-date-build-fix-source.zip`

## Changed
- Added simple teams card inside expanded trip card.
- Teams can be added, edited and deleted from APK.
- Added `FieldPackageTripTeam` model.
- Added `fetchTripTeams`, `saveTripTeam`, `deleteTripTeam` to `FieldPackageSheetSyncClient`.

## Version
- versionCode: 900098
- versionName: 1.4.16v-teams-flow

## SQL
Requires `SUPABASE_SOV_TRIP_TEAMS_FLOW_v6_1_24.sql`.

## Not changed
- Trips DB repair logic is not reworked in this APK build.
- Armory/Documents/Archive modules are not changed.
