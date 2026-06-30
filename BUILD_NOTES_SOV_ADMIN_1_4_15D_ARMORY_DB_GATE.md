# SOV Admin 1.4.15d — Armory DB Gate / SDK 35

## Scope
- Oružarstvo in APK no longer displays cached, demo, or bundled XLS fallback inventory while the database is empty or still importing.
- The screen shows a loading/status card with a progress bar until the live Supabase catalog returns real inventory rows.
- Auto-retry runs while catalog is not ready; user can also tap refresh.
- SDK 35 / AGP 8.6.1 / Gradle 8.7 tooling from v1.4.15c is preserved.

## Changed files
- `HomeAndToolsScreens.kt`
  - Initial inventory state is empty.
  - Shows `EquipmentDatabaseLoadingCard` until live catalog rows exist.
  - Hides tabs, catalog, inventory, queue, and request cart until catalog is ready.
  - Adds auto-retry while catalog is missing.
- `EquipmentSupabaseRepository.kt`
  - `loadSnapshot` returns an empty DB-gate snapshot when not logged in, DB is empty, or cloud fails.
  - Does not use cached or asset fallback for inventory display before DB readiness.
  - Saves cache only after a live non-empty catalog is returned.

## Required SQL
Use existing canonical SQL:
`SUPABASE_ORUZARSTVO_XLS_CANONICAL_v6_1_5c_NO_TEMP.sql`
