# SOV web v6.1.35-save-fix — Oružarstvo inventura save fix

Baseline: `sov-web-build-v6.1.35-oruzar-inventura-search.zip`.

## Decision
- Old Oružarstvo catalog remains the main catalog.
- Inventura is only a manual correction tool for current Klaićeva counts.
- XLS/OCR inventory is not imported as the main catalog in this build.

## Fixed
- Inventory count changes now persist to Supabase instead of only changing local browser state.
- Status changes from inventory also persist.
- Browser cache for armory catalog is cleared after successful save so refresh does not show stale counts.
- Existing loan counter is preserved: manual count is treated as current available count in Klaićeva; total quantity becomes `available + loaned` when loaned exists.

## Required SQL
Run first:
`SUPABASE_ORUZARSTVO_v6_1_35_SAVE_INVENTORY_FIX.sql`

This creates RPC:
`public.sov_armory_save_inventory_count(...)`

The web uses that RPC to avoid RLS problems on direct table updates.
