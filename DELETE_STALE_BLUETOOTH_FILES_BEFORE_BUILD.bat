@echo off
echo Removing stale Bluetooth draft files that are not part of v1.4.15a...
del /f /q "app\src\main\java\com\darko\speleov1\util\FieldBluetoothExportStore.kt" 2>nul
del /f /q "app\src\main\java\com\darko\speleov1\util\FieldTrackingBluetoothTeamLayoutStore.kt" 2>nul
echo Done. Now reopen/sync Android Studio and build :app:assembleRelease.
pause
