# SOV web v6.1.42e - Equipment inventory subcategories fix

Date: 2026-06-15

## Problem

v6.1.42d fixed member equipment category names, but some member-facing subcategories could still show normalized/app labels instead of the labels used in Inventory and Inventory Count.

Example:

- `Karabineri i spojnice` appeared in the member catalog.
- Inventory naming expects `Karabineri`.

## Fix

- Added member display mapping from normalized/app subcategory labels back to inventory labels.
- Applied the same mapping in the DB-gate renderer so early boot and live-render paths match.
- Examples:
  - `Karabineri i spojnice` -> `Karabineri`
  - `Sidrišne pločice` -> `Pločice`
  - `Bušilice, baterije i svrdla` -> `Bušilice i baterije`
  - `Transportne vreće i drybagovi` -> `Transportke`
  - `Statička speleo užad` -> `Statik`
  - `Kuhinjski pribor i posuđe` -> `Posuđe`
- Cache key bumped to `sov_armory_catalog_cache_v6142e_inventory_subcategories`.
- Older `42d`, `42c`, and legacy armory catalog cache keys are cleared.

## Supabase

No SQL migration required.

The fix is intentionally in the display layer because the current database contains both inventory/XLS names and normalized app names. This build makes the member borrowing page follow the Inventory/Inventory Count naming.

## Verification

- `node --check assets/oruzarstvo-supabase.js`
- `node --check assets/oruzarstvo-boot-v615.js`
- Verified display mappings for the problematic normalized labels.
