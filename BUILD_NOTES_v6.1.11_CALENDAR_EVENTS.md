# SOV Web v6.1.11 — Calendar Events

Baseline: `sov-web-build-v6.1.10-documents-full-archive-ready.zip`

## Added

- `kalendar-izleta.html` now supports additional SOV calendar events.
- Added `＋ Event` button.
- Added custom event dialog.
- Added side panel with upcoming custom events.
- Custom events are stored in Supabase table `sov_calendar_events`.

## Not changed

- Existing trip calendar and signup flow stay in place.
- Existing documents archive pages stay in place.

## Requires SQL

Run `SUPABASE_SOV_CALENDAR_EVENTS_v6_1_11.sql` before using custom events.
