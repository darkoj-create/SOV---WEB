# SOV Admin v1.4.14.0 — Field Tracking Lite MVP

Bazirano na v1.4.13.10.

Dodano:
- SOV Field Tracking Lite kartica na Supabase izletima.
- Start/Stop tracking za aktivni izlet.
- Lokalni SQLite queue za tracking točke.
- Batch sync prema Supabase RPC-u `sov_tracking_ingest_batch`.
- Foreground service + notification: jasno vidljivo da tracking radi.
- Low battery intervali: ispod 30% rjeđe spremanje točaka.
- Ručni Sync gumb na trip kartici.

Popravljeno:
- Home login ikonica gore lijevo dobila veći z-index i ostaje klikabilna; vodi na `SOV Cloud login` screen.

Potrebno:
- Pokrenuti `SUPABASE_SOV_FIELD_TRACKING_LITE_v5_59_0.sql`.
- Web v5.59.0 za pregled trackinga na karti.

Napomena:
Tracking je pomoćni alat, nije službeni/spasilački sustav.
