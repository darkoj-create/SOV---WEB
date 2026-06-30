# SOV Admin 1.4.15b — normal build package

Purpose: same APK source as v1.4.15a, but packaged like a clean Android Studio project.

Fixes in this package:
- Removed `.idea/` so Android Studio does not inherit a broken project Gradle JDK setting.
- Removed `local.properties` so Android Studio recreates it for the local Android SDK.
- Removed stale Bluetooth Kotlin files if present:
  - `FieldBluetoothExportStore.kt`
  - `FieldTrackingBluetoothTeamLayoutStore.kt`
- Kept armory/Oružarstvo sync aligned to web v6.1.5 XLS canonical data.
- Version bumped to `1.4.15b-armory-web-v615-normal-build`, versionCode `900070`.

Open the extracted folder in Android Studio and build normally.
