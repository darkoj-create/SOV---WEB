# SOV Web v5.59.6 — Trip Assets Manager

- `izleti-cloud.html` dobiva sekciju **Paketi za teren** u detalju izleta.
- Voditelj/admin/web user s pravima može uploadati `.sovpkg`, offline ZIP, GPX/KML i druge terenske datoteke u `sov-trip-assets`.
- Web koristi private bucket + signed download URL.
- Prikazuje veličinu, tip, checksum status i napomenu za offline spremnost.
- `sync-status.html` dignut na v5.59.6.
- Potreban SQL: `SUPABASE_SOV_TRIP_ASSETS_v5_59_7_STATUS_MANIFEST.sql`.
