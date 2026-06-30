# SOV APK v1.4.19 — Crashlytics

Baseline: sov-admin-v1_4_18-client-error-logs-source.zip

## Changed
- Added Firebase Google Services Gradle plugin.
- Added Firebase Crashlytics Gradle plugin.
- Added Firebase BoM, Crashlytics and Analytics dependencies.
- Added `app/google-services.json` for Firebase project `sov-app-3ecd1`.
- Added baseline Crashlytics keys on app startup:
  - `app_version`
  - `current_screen`
  - `last_action`
- Connected `SovClientLogger` to Crashlytics:
  - handled `error` / `fatal` events are recorded as non-fatal exceptions
  - `warning` / `info` events are added as Crashlytics logs
  - Supabase client error logging remains active
- Added Crashlytics custom keys:
  - `app_version`
  - `user_role`
  - `user_email_hash`
  - `current_screen`
  - `last_action`
  - `severity`
  - `trip_id`
  - `team_id`
  - `device_model`
  - `android_version`
- Bumped APK version:
  - versionCode `900105`
  - versionName `1.4.19-crashlytics`

## Not changed
- No SQL changes.
- No web changes.
- Existing Supabase error log stays in place.
- Trips, tracking, broadcast, Oružarstvo and Arhivar business logic not changed.
- Navigation not changed.

## Deploy
1. Open this source in Android Studio.
2. Let Gradle sync download Firebase dependencies.
3. Build/install the APK.
4. Crashlytics will start reporting after the first real crash or non-fatal handled error is sent.

## Notes
- Crashlytics is for real APK crashes and non-fatal handled errors.
- Supabase `sov_client_error_logs` remains the operational dashboard for handled API/sync problems.
- This build requires the existing SQL from Faza 1: `SUPABASE_SOV_CLIENT_ERROR_LOGS_v6_1_31.sql`.
