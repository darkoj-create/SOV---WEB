# SOV Web v6.1.12 — Calendar Google Style

- `kalendar-izleta.html` now renders a Google Calendar style month grid.
- Trip titles and additional events are visible directly inside day cells.
- Selecting a day shows detailed trip/event cards below the calendar.
- Web calendar can delete cloud trips from Supabase via `SOVTripsCloud.deleteTrip(id)` with a destructive warning confirm prompt.
- Keeps existing custom event creation and Supabase calendar events.
- No SQL change required beyond existing `SUPABASE_SOV_CALENDAR_EVENTS_v6_1_11.sql`.
