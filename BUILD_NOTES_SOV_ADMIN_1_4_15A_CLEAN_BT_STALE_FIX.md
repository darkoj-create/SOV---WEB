# SOV Admin v1.4.15a — clean armory web v6.1.5 sync

This is a clean source package based on v1.4.15. It intentionally contains no stale Bluetooth draft store files:

- `app/src/main/java/com/darko/speleov1/util/FieldBluetoothExportStore.kt`
- `app/src/main/java/com/darko/speleov1/util/FieldTrackingBluetoothTeamLayoutStore.kt`

Those files were not part of the v1.4.15 armory sync source and can break Kotlin compilation when an older local Android Studio project is reused/overwritten instead of extracted into a clean folder.

Build recommendation:

1. Extract this ZIP into a brand new empty folder.
2. Do not copy it over an older `sov-admin` folder.
3. In Android Studio: File → Open → select the extracted folder.
4. Sync Gradle.
5. Build `:app:assembleRelease`.

If you must reuse an old folder, delete the two stale Bluetooth `.kt` files above before building.

Armory logic remains aligned with web v6.1.5 XLS canonical inventory and Supabase SQL v6.1.5c.
