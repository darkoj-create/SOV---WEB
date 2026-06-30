# SOV Admin 1.4.14.3 — Field Tracking build fix

Fix za Kotlin compile error u `MapFeature.kt`:

- `FieldTrackingLatestPosition` i `FieldTrackingRemotePoint` više nisu `internal`, jer ih javni `MapScreen()` prima kao parametre.
- Time se uklanja greška: `public function exposes its internal parameter type`.
- Field Tracking team map viewer iz 1.4.14.2 ostaje funkcionalno isti.
- Login screen / persistent session / Arhivar full worklist ostaju uključeni.

Verzija:

- `versionCode = 900060`
- `versionName = 1.4.14.3-field-tracking-buildfix`
- expected APK: `SOV-ADMIN-1.4.14.3.apk`
