# SOV web v5.31 — ecosystem sync dashboard

Ovaj build dodaje prvi pravi "SOV ekosustav" kontrolni sloj bez diranja SQL baze objekata.

## Dodano

- `sync-status.html` — read-only kontrolna ploča za:
  - Nacrti GS v2.0.2 fast-search endpoint
  - Izleti GS endpoint
  - Supabase counts za `speleo_object_drawings`, `speleo_objects_live_sql`, `profiles`
  - Trenutne app/web verzije i unified APK smjer
- Link "SOV Sync" na dashboardu.
- `SUPABASE_SOV_ECOSYSTEM_ROLES_v5_31.sql` — pripremna role/permissions shema za budući unified/admin-capable APK.

## Nije dirano

- `data/sov-baza.json`
- `assets/sov-speleo-sql.js`
- live/staging SQL logika objekata
- Izleti GS kod
- nacrti sync u `baza.html` / `pregled-baze.html` osim što ih dashboard sada može provjeriti

## Smjer dalje

Odvojeni public/admin APK se može zamrznuti i dalje razvijati jedan full APK kojem Supabase role odlučuju vidljivost modula.
