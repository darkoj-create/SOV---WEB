# SOV Admin APK 1.4.7 — Izleti Cloud Supabase

- Izleti više ne koriste Google Sheet / Apps Script kao source of truth.
- `FieldPackageSheetSyncClient` sada čita `sov_trips_mobile_feed`.
- Novi izleti iz APK-a se spremaju u `sov_trips`.
- Prijave idu u `sov_trip_members`.
- Lokalni cache i pending queue ostaju za offline rad.
- Legacy Raspored autiju GAS ostaje samo opcionalni pomoćni link, ne backend za izlete.
