# SOV Admin 1.4.14.5 — Battery & Tracking Diagnostics

Base: `sov-admin-v1.4.14.4-tracking-jwt-refresh-fix-source.zip`

## New

- Added a Settings card: **Baterija i tracking**.
- Shows current battery %, charging state, temperature/voltage when Android exposes them.
- Tracks Field Tracking session duration, mode, sync/queue counts, estimated battery drain per hour, and estimated time until 15%.
- Added **Resetiraj mjerenje** to reset the local battery baseline for the current tracking session.
- Field Tracking trip card now shows a compact battery line while tracking is active.

## Notes

- No SQL changes.
- No web changes.
- Android cannot expose the exact system Battery Usage percentage per app; this is a practical field estimate based on battery delta over tracking time.
- Existing JWT refresh fix, team map viewer, persistent login and Arhivar full worklist remain included.

## Version

- `versionCode = 900062`
- `versionName = 1.4.14.5-battery-tracking-diagnostics`
- Expected APK: `SOV-ADMIN-1.4.14.5.apk`
