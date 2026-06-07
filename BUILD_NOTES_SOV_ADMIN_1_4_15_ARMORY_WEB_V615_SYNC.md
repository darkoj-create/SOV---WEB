# SOV Admin 1.4.15 — Armory Web v6.1.5 / XLS canonical sync

Baseline: `sov-admin-v1.4.14.10-trip-assets-offline-ready-source.zip`.

## Goal
Make Android Oružarstvo match the web v6.1.5 armory logic: same XLS canonical catalog, same Supabase views, same category taxonomy, and no parallel mobile-only inventory brain.

## Changes
- Added bundled offline asset: `app/src/main/assets/oruzarstvo-xls-canonical-v6.1.5.json`.
- Android fallback now shows the full XLS canonical catalog instead of the old 3-row demo catalog.
- Category order and labels now match web/Supabase v6.1.5 canonical taxonomy:
  1. Osobna oprema
  2. Oprema za postavljanje
  3. Čisto podzemlje
  4. Oprema za crtanje
  5. Oprema za proširivanje
  6. Elektro i foto oprema
  7. Alpinistička oprema
  8. Ronilačka oprema
  9. Ostali alat
  10. Užeta
  11. Oprema za logor
  12. Medicinska oprema
- Old APK labels are normalized away:
  - `Užad` → `Užeta`
  - `Bušilice i baterije` → `Elektro i foto oprema`
  - `Osobna oprema - komplet` → `Osobna oprema`
- Sync status now shows catalog version + XLS row counts.
- Inventory default is now `Sve`, not old `Užad`.
- Inventory PATCH now updates `equipment_items` by `legacy_id` when source rows are XLS IDs such as `XLS-0001`, not only by UUID.
- Zero-quantity XLS rows display as `Nije dostupno`, not falsely as `Izdano`.
- Included current DB patch: `SUPABASE_ORUZARSTVO_XLS_CANONICAL_v6_1_5c_NO_TEMP.sql`.

## Deploy order
1. Run `SUPABASE_ORUZARSTVO_XLS_CANONICAL_v6_1_5c_NO_TEMP.sql` in Supabase.
2. Build/install Android from this source.
3. Log into SOV Cloud and use Oružarstvo refresh.

## Notes
The APK still does not become the master editor for catalog structure. Catalog structure remains web/Supabase-led. APK can read catalog, create requests, process issue/return flow, and perform inventory counts against the same rows.
