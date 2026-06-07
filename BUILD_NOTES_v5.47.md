# SOV Web v5.47 — Oružarstvo DB consolidation

## Required SQL
Run `SUPABASE_ORUZARSTVO_DB_CONSOLIDATION_v5_47.sql` in Supabase with RLS enabled.

## What this build expects
- Catalog views `sov_equipment_app_catalog` and `sov_equipment_app_catalog_grouped` still exist.
- They are now backed by the unified physical table `equipment_assets`.
- Legacy tables stay alive for compatibility/imports: `equipment_items`, `equipment_ropes`, `equipment_pieces`.
- Sync triggers mirror legacy writes into `equipment_assets`.

## Why
This is the safe version of the large DB consolidation: one canonical asset table without breaking the current web/APK surfaces.
