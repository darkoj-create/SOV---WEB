# SOV Admin 1.4.14.6 — Battery Diagnostics buildfix

Fix for Kotlin compile error in `HomeAndToolsScreens.kt` introduced by the Battery & Tracking Diagnostics card.

## Fixed
- Replaced `mutableLongStateOf(System.currentTimeMillis())` in `SettingsBatteryTrackingCard` with `mutableStateOf(System.currentTimeMillis())`.
- Removes compile errors:
  - `Unresolved reference: mutableLongStateOf`
  - `Type 'TypeVariable(T)' has no method 'getValue(...)' and thus it cannot serve as a delegate`

## Preserved
- Battery & Tracking Diagnostics UI in Settings.
- Field Tracking JWT refresh fix.
- Team map viewer.
- Persistent login screen.
- Arhivar full worklist/UX fixes.

## Version
- `versionCode = 900063`
- `versionName = 1.4.14.6-battery-buildfix`
- Expected APK: `SOV-ADMIN-1.4.14.6.apk`
