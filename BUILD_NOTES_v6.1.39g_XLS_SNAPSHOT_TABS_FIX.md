# SOV web v6.1.39g — XLS snapshot tabs fix

## Problem
Excel export in Inventar/Inventura created 3 tabs, but only the first tab was filled. The old-base and combined tabs were empty because the browser attempted to read `equipment_catalog_snapshot_items` directly. Depending on RLS/browser access, the snapshot item list could come back empty even though the snapshot existed.

## Fix
- Added Supabase RPC `public.sov_armory_get_catalog_snapshot(p_snapshot_id uuid)` as SECURITY DEFINER.
- Frontend `SOVArmoryDB.loadCatalogSnapshot()` now tries the RPC first.
- Direct table read remains as fallback.
- Export logic remains 3 tabs:
  1. Aktualna baza
  2. Stara baza
  3. Kombinirano
- Combined tab keeps `Baza` column and largest-quantity sorting.

## Changed files
- `assets/oruzarstvo-supabase.js`
- cache-bust references in Oružar pages
- `SUPABASE_ORUZARSTVO_v6_1_39g_SNAPSHOT_EXPORT_RPC.sql`

## Verified
- RPC returns 558 items for `v6.1.39c-initial` snapshot.
- No item/category/location/loan data was modified by this fix.
- SQL is function-only; no destructive data operation.
