# SOV Web v6.1.35 — Restore old Oružarstvo catalog + save fix

## Decision
Old Oružarstvo catalog is the main source of truth. The XLS/Klaićeva inventory import is not used as the main catalog.

## What this package does
- Keeps v6.1.35 web as the base.
- Keeps the typo-tolerant inventory search from v6.1.35.
- Keeps the manual inventory save RPC (`sov_armory_save_inventory_count`).
- Adds SQL cleanup that archives and removes only bad/new XLS/Klaićeva opening-balance import rows from `equipment_items`.
- Does not restore the whole Supabase database.
- Does not delete users, roles, trips, news, Arhivar data, requests, loans, etc.

## Workflow after deploy
1. Run `SUPABASE_ORUZARSTVO_v6_1_35_PREVIEW_BAD_IMPORT_ROWS.sql` if you want to preview candidates.
2. Run `SUPABASE_ORUZARSTVO_v6_1_35_RESTORE_OLD_CATALOG_CLEAN_IMPORTS.sql`.
3. Deploy the web ZIP.
4. Open Oružarstvo → Inventura.
5. Search old catalog item and manually set physical Klaićeva count.
6. Add genuinely new items manually in Inventar.
7. Missing old items get available/count 0 or status `za provjeru`.

## Safety
Removed rows are archived in:
`public.sov_armory_removed_inventory_import_archive`
