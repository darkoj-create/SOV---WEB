# SOV Web v6.1.6 — Armory DB Gate

## Scope
- Public `oruzarstvo.html` and Oružar Master pages now use a strict database gate for inventory/catalog rendering.
- While Supabase inventory/catalog views are empty, unavailable, timing out, or still being seeded, the UI shows a premium loading/status card with an animated progress bar.
- No inventory rows, category cards, static XLS JSON, cached catalog, or partial fallback catalog are rendered before the live database returns a real catalog.

## Changed files
- `assets/oruzarstvo-supabase.js`
  - Adds `strictLive` support to `SOVArmoryDB.loadAllData`.
  - Bypasses local cache/static fallback when `strictLive: true`.
- `assets/oruzarstvo-boot-v615.js`
  - Rebuilt as v6.1.6 DB-gate boot.
  - Polls Supabase live until catalog rows are available.
- `assets/oruzar-master-clean.js`
  - Master/inventory/inventura screens render DB loading state until live data exists.
- `oruzarstvo.html`, `oruzar-master*.html`
  - Query strings bumped to v6.1.6.

## Required SQL
Use existing canonical SQL:
`SUPABASE_ORUZARSTVO_XLS_CANONICAL_v6_1_5c_NO_TEMP.sql`

Deploy SQL first, then deploy this web build.
