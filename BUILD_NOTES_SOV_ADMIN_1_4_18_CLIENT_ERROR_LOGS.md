# SOV APK v1.4.18 — Client error logs

Baseline: v1.4.17-oruzar-arhivar-ux
Requires SQL: SUPABASE_SOV_CLIENT_ERROR_LOGS_v6_1_31.sql

## Changed
- Added `SovClientLogger.kt`.
- APK now sends handled HTTP/API failures to Supabase via `sov_log_client_error`.
- `SovHttpClient` logs failed REST/RPC responses before throwing the existing error.
- Logged context includes:
  - platform = android
  - app version
  - inferred screen/module
  - action / REST path
  - HTTP status and response excerpt
  - device model and Android version
  - cached role and email
- Version bump:
  - versionCode 900104
  - versionName 1.4.18-client-error-logs

## Not changed
- No SQL bundled inside APK.
- No web changes inside APK ZIP.
- No Firebase Crashlytics yet.
- Repositories remain functionally unchanged.
- Navigation unchanged.
- Trips, tracking and broadcast logic unchanged except central failed HTTP logging.

## Deploy
1. Run `SUPABASE_SOV_CLIENT_ERROR_LOGS_v6_1_31.sql` first.
2. Build/install this APK.
3. Check logs in web `admin-client-errors.html`.
