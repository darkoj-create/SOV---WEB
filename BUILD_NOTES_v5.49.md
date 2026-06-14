# SOV Web v5.49 — Izleti Cloud / Supabase Sheet View

- Dodan `izleti-cloud.html`: sheet-like editor koji piše direktno u Supabase.
- Dodan `assets/sov-trips-cloud.js` kao shared web client za `sov_trips_*`.
- `izleti.html` objavljuje novi izlet u Supabase, ne u Google Sheet.
- `kalendar-izleta.html` čita Supabase `sov_trips_mobile_feed` i prijave šalje u `sov_trip_members`.
- Stari Google Apps Script endpointi ostaju samo kao legacy fallback / pomoćni cars sheet link, ne kao source of truth.
- U ZIP-u je all-in-one SQL `SUPABASE_SOV_TRIPS_CLOUD_v5_49_ALL_IN_ONE.sql`.
