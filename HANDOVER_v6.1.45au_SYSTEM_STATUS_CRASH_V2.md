# Handover — SOV Observability v2

## Frozen baselines

- Web base: `6.1.45at-nacrt-corpus-style-v6`
- Web target: `6.1.45au-system-status-crash-v2`
- Backend contract: `2026.07.20-observability-v2`
- APK base: `1.4.56a-dem-track-layers-nacrt-v6`
- APK target: `1.4.56b-crash-bridge` / versionCode `900159`

## Production Supabase

Migration `sov_observability_system_status_v2` is already applied and tested on project `ncomefzkuixyfixisrhi`.
Do not re-run it during normal deploy. SQL files are retained for reproducibility and future environments.

## Crash flow

1. Existing handled web/Android errors use `sov_log_client_error` immediately.
2. Firebase Crashlytics remains enabled for Android.
3. APK 1.4.56b installs a process-wide uncaught-exception bridge.
4. A fatal crash is stored synchronously in private local storage.
5. The original Crashlytics handler still terminates/reports normally.
6. On the next APK start, queued crash rows are uploaded to Supabase with `severity=fatal` and `handled=false`.
7. System Status reads aggregates and recent rows through the Admin/Webmaster-only `sov_system_status_snapshot` RPC.

## Safety boundaries

No changes were made to trips, tracking payloads, maps, Nacrt rendering, archive, armory data models or existing RLS policies.
The web page keeps the previous direct checks as a compatibility fallback if the new snapshot RPC is temporarily unavailable.
