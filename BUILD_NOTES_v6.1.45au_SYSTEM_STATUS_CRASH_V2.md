# SOV Web v6.1.45au — System status / crash reporting v2

## Scope

This release upgrades only observability and the internal Admin/Webmaster System status screen.
It does **not** change trips, maps, archive, armory, Nacrt rendering, tracking payloads or role rules.

## Supabase

Migration: `SUPABASE_OBSERVABILITY_SYSTEM_STATUS_V2.sql`

Applied to production project `ncomefzkuixyfixisrhi` on 2026-07-20.

Adds:

- `public.sov_system_status_snapshot(integer)` — SECURITY DEFINER, approved Admin/Webmaster only.
- composite index for platform/severity/time client-error queries.
- partial index for unhandled/fatal events.
- ecosystem contract `2026.07.20-observability-v2`.

Existing infrastructure retained:

- `public.sov_client_error_logs`
- `public.sov_log_client_error(...)`
- `public.sov_list_client_errors(...)`
- RLS admin-only SELECT policy
- Firebase Crashlytics in Android

## Web System status

New bundle:

- `assets/sov-system-status-wow-v6145au.css`
- `assets/sov-system-status-wow-v6145au.js`

The page now shows:

- Android fatal crashes in the last 24 hours
- Android handled errors + fatal events
- Android unhandled events over 7 days
- web fatal events
- last APK version/report timestamp
- recent client errors with platform, version, screen/action, severity and device
- real RLS/public-write-policy counts from an admin-only server RPC

If the new RPC is unavailable, the page falls back to the previous direct browser-safe checks.

## Android contract

Target APK: `1.4.56b-crash-bridge` (`versionCode 900159`).

The Android patch installs a process-wide uncaught-exception bridge. A fatal crash is written synchronously to a small local queue, the original Crashlytics handler is called, and the queued report is uploaded to `sov_log_client_error` on the next process start. Successful uploads are removed from the local queue; failed uploads remain for a later retry.

## Verification

- production migration applied successfully
- RPC tested with a real approved Webmaster identity
- anonymous/non-admin execution remains denied
- existing client-error table and RPC signatures were not removed
- web keeps fallback checks
- no destructive DDL
