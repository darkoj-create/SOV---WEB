# SOV web v6.1.38 — Oružarstvo category icon + note

Baseline: `sov-web-build-v6.1.37-oruzar-locations.zip`.

## DB

SQL: `SUPABASE_ORUZARSTVO_v6_1_38_CATEGORY_ICON_NOTE.sql`

Adds to `equipment_categories`:
- `icon text`
- `note text`

Creates backup table:
- `sov_armory_category_meta_backup_20260614`

Seeds default icons for the 12 curated Oružarstvo categories. Does not touch `equipment_items`, quantities, loans, or locations.

## Web

Changed:
- `assets/oruzar-master-clean.js`
- `assets/oruzarstvo-supabase.js`
- cache-busted `oruzar-master*.html` / `oruzarstvo.html` references where relevant

### Behavior

In both Inventar and Inventura:
- category tiles display the persisted category icon if present
- clicking a category opens a category metadata panel
- panel has:
  - icon input
  - persistent note textarea
  - save button
- note is stored on `equipment_categories.note`
- icon is stored on `equipment_categories.icon`
- notes are shown on category tiles as a preview and in the category panel

## Safety

No item quantities changed.
No item names changed.
No loans changed.
No RLS changes included in this build.
