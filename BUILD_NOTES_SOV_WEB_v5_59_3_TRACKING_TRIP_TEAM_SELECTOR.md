# SOV Web v5.59.3 — Tracking trip/team selector

Baseline: v5.59.2 Field Tracking SQL syntax fix.

## Promjene
- `tracking.html` dobiva dva jasna filtera: Izlet i Team/teren.
- Team selector podržava pojedinačni team ili sve teamove u izletu.
- Web povlači pozicije i trailove za sve odabrane teamove i prikazuje ih na jednoj karti.
- Kreiranje novog teama koristi `sov_tracking_create_field_event_v2()` kada je SQL v5.59.3 pokrenut.
- Fallback na stari RPC ostaje radi kompatibilnosti.
- `sync-status.html` usklađen na v5.59.3.

## SQL
Pokrenuti `SUPABASE_SOV_FIELD_TRACKING_LITE_v5_59_3_TRIP_TEAM_SELECTOR.sql` nakon v5.59.2.

## APK
Nije potreban novi APK za ovaj web selector. APK v1.4.14.2 već šalje i čita team/field event podatke.
