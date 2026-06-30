# SOV Admin 1.4.21b — Imports layer persistence

Base: `sov-admin-v1_4_21a-external-file-open-fix-source.zip`

## Scope

This patch keeps externally/imported KML/GPX/KMZ/GeoJSON/CSV/XLSX/GPKG/GeoTIFF/ZIP layers visible and available in the app under **Slojevi / OFFLINE → Imports** after import and after app restart.

## Changes

- Imported layers are now saved to an app-private JSON file: `filesDir/user_content_store/imported_layers_v2.json`.
- SharedPreferences remains as a small compatibility/migration fallback, but is no longer the only storage for imported layers.
- Large imported layers are no longer silently skipped because of the previous ~1.5 MB SharedPreferences guard.
- Existing SharedPreferences imported-layer data is migrated to the new file-backed store on first load.
- The **Imports** category in Slojevi/OFFLINE is selected by default: `maps,imports`.
- When imported layers exist and a new layer is added, **Imports** is auto-selected so the user sees the list immediately in Slojevi/OFFLINE.

## Not changed

- No Supabase / SQL changes.
- No Oružarstvo logic changes.
- No Izleti logic changes.
- No parser format changes.
- No public/user role logic changes.

## Version

- `versionCode = 900109`
- `versionName = 1.4.21b-imports-layer-persistence`
- APK name: `SOV-ADMIN-1.4.21b.apk`
