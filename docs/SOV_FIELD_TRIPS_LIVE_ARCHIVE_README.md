# Correct deployed Izleti endpoint

Current confirmed shared Izleti deployment URL:

`https://script.google.com/macros/s/AKfycbybGi7p6_ImXAXEErJ6P9K0GYHy8lHW850K9cQe2py8yUV2oJO6UW1DJi00quorVTHOGQ/exec`

Current matching Apps Script copy bundled in this ZIP:

`docs/scripts/FIELD_TRIPS_SHEET_WEBAPP_v1_0_89_CURRENT_DEPLOYED.gs`

This script uses columns A:N: Datum, Voditelj, Lokacija, Opis, Cilj, Prijavljeni, CenterLat, CenterLon, MinLat, MaxLat, MinLon, MaxLon, RasporedUrl, WeatherCity.

---

# SOV Izleti — live + past archive sheet sync

Build patch scope:

- App now keeps the last successful shared trip list cached locally, so refresh failures do not immediately erase the visible shared schedule.
- Shared trips are split in the app into:
  - current/upcoming trips
  - `Prošli izleti` archive
- Sorting is date-based: upcoming trips ascending, past trips descending.
- Added Apps Script: `docs/scripts/FIELD_TRIPS_SHEET_WEBAPP_v1_1_48_LIVE_ARCHIVE.gs`.

## Apps Script notes

Deploy the script as a Web App using the same deployment URL already built into the app if possible.

Supported web actions:

- `GET ?action=listTrips`
- `POST action=addTripV2`
- `POST action=deleteTrip`
- `POST action=signupTrip`
- `POST action=updateRasporedUrl`

The script can read both:

1. Normal SOV app trip schema:
   `Datum | Voditelj | Lokacija | Opis | Cilj | Sudionici | Vozači | Raspored URL | Weather city ...`
2. Legacy DOCX-import schema:
   `Datum | Lokacija | Ljudi`

This prevents the app from showing “no trips” just because the shared sheet was filled by the older DOCX parser.
