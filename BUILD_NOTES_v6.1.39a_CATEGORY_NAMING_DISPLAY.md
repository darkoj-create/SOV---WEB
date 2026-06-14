# SOV Web v6.1.39a — Oružarstvo category naming display layer

Baseline: `sov-web-build-v6.1.38-oruzar-category-icon-note.zip` / live Supabase v6.1.38.

## Goal

Make Oružarstvo labels less književno/formal without changing the stable inventory base.

## Database

SQL: `SUPABASE_ORUZARSTVO_v6_1_39a_CATEGORY_NAMING_DISPLAY.sql`

Adds to `equipment_categories`:
- `display_name`
- `short_name`
- `search_terms`

Adds new table:
- `equipment_subcategory_meta`

Backup table:
- `sov_armory_category_naming_backup_20260614`

## Current mapping

Category:
- `Osobni SRT komplet` → `Osobna oprema`

Subcategories:
- `Penjalice i blokeri` → `Blokeri i Croll`
- `Pupci i lanyardi` → `Pupci`
- `Spojni elementi osobni` → `Centralni/pomoćni karabineri`
- `Spuštalice` → `Descenderi`
- `Karabineri i spojnice` → `Karabineri`
- `Sidrišni okov` → `Fix / Spit`

## Web

`assets/oruzar-master-clean.js` now:
- keeps raw DB category/subcategory keys internally
- displays `display_name` / subcategory meta in Inventar and Inventura
- includes display names and aliases in fuzzy search
- exports display labels in XLS
- item edit modal still uses raw/stable category keys to avoid breaking saves

`assets/oruzarstvo-supabase.js` now loads:
- category display metadata
- `equipment_subcategory_meta`

## Not changed

- No quantity changes
- No location changes
- No loan/request changes
- No restore/snapshot system yet
- No raw category rewrite in `equipment_items`
