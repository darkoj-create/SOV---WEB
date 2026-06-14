# SOV Web v6.1.39b — Oružarstvo icon fit + user lending category naming

Baseline: `sov-web-build-v6.1.39a-oruzar-category-naming.zip` / live Supabase category display-name layer.

## What changed

### 1. Icon/text sizing fix
- Tightened category card layout in Inventar and Inventura.
- Category icons now render inside fixed square icon wells instead of pushing text out of cards.
- Long category names wrap safely with proper line-height and no overflow.
- Category meta panel icon input is fixed-height and no longer spills layout.

Affected:
- `assets/oruzar-master-clean.css`
- `assets/oruzar-master-clean.js`

### 2. User lending catalog category naming
- User-facing `oruzarstvo.html` now reads `equipment_categories.display_name`, `short_name`, `icon`, `note` and `equipment_subcategory_meta` from `SOVArmoryDB.loadAllData()`.
- User catalog keeps raw/internal category values for filtering and adding requests, but displays friendly names.
- Subcategories show display names from `equipment_subcategory_meta`, e.g.:
  - `Penjalice i blokeri` → `Blokeri i Croll`
  - `Pupci i lanyardi` → `Pupci`
  - `Spojni elementi osobni` → `Centralni/pomoćni karabineri`
  - `Spuštalice` → `Descenderi`
  - `Karabineri i spojnice` → `Karabineri`
  - `Sidrišni okov` → `Fix / Spit`
- Category filter dropdown also shows display names while values stay internal/stable.
- Item badges in the user request catalog show display category/subcategory names.

Affected:
- `assets/oruzarstvo-boot-v615.js`
- `oruzarstvo.html`

## What did NOT change
- No SQL changes in this build.
- No item count changes.
- No location changes.
- No quantity/inventory changes.
- No RLS/policy changes.
- Existing category display-name SQL from v6.1.39a remains the source of truth.

## Deploy notes
- Deploy the full ZIP.
- Hard refresh browser or clear cache after deploy.
- User lending page uses cache-busted `oruzarstvo-boot-v615.js?v=6.1.39b-user-category-naming`.
- Master pages use cache-busted `oruzar-master-clean.js?v=6.1.39b-iconfit-usercats` and CSS `v=6.1.39b-iconfit`.
