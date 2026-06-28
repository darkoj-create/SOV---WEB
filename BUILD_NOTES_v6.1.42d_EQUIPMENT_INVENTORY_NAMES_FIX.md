# SOV web v6.1.42d - Equipment inventory names fix

Date: 2026-06-15

## Problem

v6.1.42c made the member equipment catalog display the app/main taxonomy names. That was not the desired behavior. The member borrowing page must show category names exactly like the inventory and inventory-count screens.

## Fix

- User equipment catalog now prefers `xls_category` / inventory category names for display.
- Supabase grouped catalog normalization now carries `xls_category`, `raw_category`, `main_category`, `xls_subcategory`, and `raw_subcategory`.
- DB-gate renderer groups categories by `xls_category` first.
- Static fallback JSON files were restored to inventory/XLS category names:
  - Osobna oprema
  - Oprema za postavljanje
  - Čisto podzemlje
  - Oprema za crtanje
  - Oprema za proširivanje
  - Elektro i foto oprema
  - Alpinistička oprema
  - Ronilačka oprema
  - Ostali alat
  - Užeta
  - Oprema za logor
  - Medicinska oprema
- Cache key bumped to `sov_armory_catalog_cache_v6142d_inventory_names`.
- Older cache keys including the v6.1.42c taxonomy cache are cleared.

## Supabase

No SQL migration required.

Verified that `equipment_items.xls_category` contains the inventory/inventory-count display names, while `category_name` / `main_category` contain normalized app taxonomy names.

## Verification

- `node --check assets/oruzarstvo-supabase.js`
- `node --check assets/oruzarstvo-boot-v615.js`
- Parsed both static equipment JSON files and confirmed top-level categories are the inventory/XLS names.
