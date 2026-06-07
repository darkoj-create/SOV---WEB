# SOV Admin v1.4.16j — Tools Calendar Events

Baseline: `sov-admin-v1_4_16i-trips-collapsible-cards-source.zip`

## Added

- Tools → Kalendar no longer opens external web calendar.
- Added native APK route `SovAppRoutes.TOOLS_CALENDAR`.
- Added `SovToolsCalendarScreen` in `HomeAndToolsScreens.kt`.
- Calendar merges two sources:
  - SOV Cloud trips from `FieldPackageSheetSyncClient.fetchTripsWithStatus()` / `sov_trips_mobile_feed`
  - custom events from `sov_calendar_events`
- Month grid view with day dots:
  - blue = izlet
  - amber = custom event
- Tap a trip event to enter `Izleti`.
- `+` button adds custom event to Supabase.
- Added `SovCalendarCloudRepository.kt` for calendar event read/write.

## Requires SQL

Run:

`SUPABASE_SOV_CALENDAR_EVENTS_v6_1_11.sql`

This creates `sov_calendar_events` and RLS policies.

## Version

- versionCode: 900086
- versionName: 1.4.16j-tools-calendar-events
