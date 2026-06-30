# SOV Admin 1.4.21a — External KML/GPX open crash fix

Baseline: `sov-admin-v1_4_21-inventory-search-source.zip`

## Problem
Opening KML/GPX from inside SOV worked, but opening the same file from outside apps such as Android Files, file explorers, WhatsApp or Drive could crash because those apps often provide a transient `content://` URI with unstable display-name/permission behaviour.

## Fix
- External `ACTION_VIEW`, `ACTION_SEND` and `ACTION_SEND_MULTIPLE` imports now gather `data`, `EXTRA_STREAM` and `ClipData` URIs.
- Before Compose/import parsing starts, external non-file URIs are copied into app cache under `cache/external_imports/`.
- The app then imports from its own stable cached `file://` URI instead of relying on WhatsApp/Files/Drive provider lifetime.
- Persistable URI handling now requests only `FLAG_GRANT_READ_URI_PERMISSION`; it no longer passes `FLAG_GRANT_PERSISTABLE_URI_PERMISSION` to `takePersistableUriPermission`.
- Old external import cache copies are pruned, keeping the newest 40 files.
- MainActivity now uses `singleTop` so repeated external opens reuse the existing top instance instead of stacking duplicate import activities.

## Scope
Only external file-open/import handling was changed. Inventory, Oružarstvo, Arhivar, trips, map layers and Supabase logic were not touched.

## Version
- versionCode: `900108`
- versionName: `1.4.21a-external-file-open-fix`
