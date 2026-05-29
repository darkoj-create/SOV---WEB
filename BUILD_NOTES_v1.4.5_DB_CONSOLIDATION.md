# SOV Admin APK 1.4.5 — DB consolidation compatibility

This source build keeps the APK catalog/inventory API stable while the Supabase database is consolidated.

## Required SQL
Run `SUPABASE_ORUZARSTVO_DB_CONSOLIDATION_v5_47.sql` in Supabase with RLS enabled.

## What changes
- APK still reads `sov_equipment_app_catalog_grouped` for catalog/search.
- APK still reads `sov_equipment_app_catalog` for Inventura/raw counting.
- Those views are now backed by unified `equipment_assets`, not by ad-hoc runtime merging of three legacy tables.
- Legacy `equipment_items`, `equipment_ropes`, and `equipment_pieces` remain intact and sync into `equipment_assets` through triggers.

No UI change in this APK step. This is a database safety migration.
