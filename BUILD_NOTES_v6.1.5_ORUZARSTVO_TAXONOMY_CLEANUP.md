# SOV web v6.1.5 — Oružarstvo taxonomy cleanup

## Problem
Oružarstvo categories were drifting between JSON, frontend JS and Supabase fallback logic, so items like batteries, busole/kompasi, drill batteries and random tools could appear in confusing buckets.

## Fix
- Adds a canonical taxonomy file: `data/oruzarstvo-taxonomy-v615.json`.
- Adds a frontend taxonomy helper: `assets/oruzarstvo-taxonomy-v615.js`.
- Normalizes `data/oruzarstvo-data.json` and adds `taxonomy_version = 6.1.5` per item.
- Keeps ordinary AA/AAA/9V/USB/powerbank batteries in `Elektro, rasvjeta i foto`.
- Keeps Bosch/Hilti/Makita/SDS batteries, chargers and drill bits in `Bušilice i svrdla`.
- Keeps busole, kompasi, Suunto, Disto and TopoDroid in `Oprema za crtanje`.
- Adds optional safe Supabase SQL: `SUPABASE_ORUZARSTVO_TAXONOMY_CLEANUP_v6_1_5.sql`.

## SQL safety
SQL is additive and guarded by column/table existence checks. It creates taxonomy helpers and applies them only to existing armory/equipment tables with matching columns. No table deletion and no hard data deletion.

## Rollback
Frontend rollback: deploy v6.1.4. SQL rollback is not required because it is additive; if needed, categories can be re-applied from old source/import.
