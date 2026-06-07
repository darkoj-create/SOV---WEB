# SOV Admin v1.4.16f — DB gate session cache-first

Baseline: v1.4.16e-db-gate-session-fix.

## Changed

- `HomeAndToolsScreens.kt`
  - Armory `LaunchedEffect(Unit)` now loads `EquipmentSupabaseRepository.loadCachedSnapshot(context)` immediately.
  - Applies cached snapshot before network refresh, so inventory appears instantly when local cache exists.
  - Then refreshes Supabase in background with message `Osvježavam oružarstvo… / Refreshing equipment…`.

## Preserved

- DB-gate logic from v1.4.15d/v1.4.16e.
- JWT/session refresh fix.
- Queued trip flush fix.
- Armory retry guard.
- SDK 35 / AGP 8.6 setup.

## Version

- versionCode: 900082
- versionName: 1.4.16f-db-gate-session-cache-first
