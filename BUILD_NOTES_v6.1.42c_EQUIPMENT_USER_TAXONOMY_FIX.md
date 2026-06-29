# SOV web v6.1.42c - Equipment user taxonomy fix

Date: 2026-06-15

## Problem

Member equipment borrowing page could still show old equipment category labels from the static JSON fallback or a stale browser catalog cache, even though Supabase live catalog already had the current taxonomy.

## Fix

- Updated `data/oruzarstvo-data.json` category labels and searchable text to the current app/Supabase taxonomy.
- Updated `data/oruzarstvo-xls-canonical-v6.1.5.json` category labels and searchable text to the current app/Supabase taxonomy.
- Bumped armory catalog cache key to `sov_armory_catalog_cache_v6142c_taxonomy`.
- Explicitly clears older catalog cache keys including `sov_armory_catalog_cache_v607`.
- Bumped `oruzarstvo.html` script/cache query strings to `6.1.42c`.
- Updated member quick-pack labels to current category names.
- Updated DB-gate category metadata in `assets/oruzarstvo-boot-v615.js`.

## Supabase

No SQL migration required.

Live `public.sov_equipment_app_catalog_grouped` already exposes the current category names:

- Logor, ekspedicija i kuhinja
- Medicinska oprema
- Sidrišta i opremanje
- Rasvjeta, elektronika i komunikacija
- Proširivanje i regulirana oprema
- Alat i održavanje
- Užad
- Mjerenje, crtanje i dokumentacija
- Osobni SRT komplet
- Alpinistička i penjačka oprema
- Tehničko spašavanje i Čisto podzemlje
- Ronilačka oprema

## Verification

- `node --check assets/oruzarstvo-supabase.js`
- `node --check assets/oruzarstvo-boot-v615.js`
- Parsed both static equipment JSON files and confirmed no old category names remain as exact category values.
- Queried Supabase live grouped catalog and confirmed category names match the static fallback category set.
