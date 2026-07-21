# SOV Admin v1.4.40a — Apps Script shared-key guard

## Scope

Implements plan item 1.4 phase A: protect legacy Google Apps Script webapp calls with a shared key read from local configuration, without hardcoding the secret in source.

## Android changes

- Added `BuildConfig.SOV_APPS_SCRIPT_KEY` in `app/build.gradle.kts`.
- The value is read from `local.properties` key `SOV_APPS_SCRIPT_KEY`; if missing, Gradle falls back to environment variable `SOV_APPS_SCRIPT_KEY`; if still missing, it builds with an empty string.
- Added `util/SovAppsScriptAuth.kt`.
- The helper sends the shared key as both:
  - HTTP header `X-SOV-KEY`
  - query/form parameter `X-SOV-KEY`

## Protected Android clients

- `DarkoOsTrackSyncClient.kt`
- `FieldPackageSheetSyncClient.kt` (`RASPORED_WEBAPP_URL` legacy helper only)
- `util/DriveDrawingsRepository.kt`
- `util/SharedLayersSyncClient.kt`

## Apps Script side

Added `SOV_APPS_SCRIPT_KEY_GUARD_v1_4_40a.gs` as a drop-in guard. Add the same secret to Script Properties in each target Apps Script project:

```text
SOV_APPS_SCRIPT_KEY=<same long random secret as Android local.properties>
```

Then wrap each Apps Script `doGet(e)` / `doPost(e)` with `sovRequireKey_(e)` before executing the old handler.

## Important behavior

If Android is built without `SOV_APPS_SCRIPT_KEY`, the app still compiles, but guarded Apps Script endpoints will return forbidden. This is intentional so the secret does not live in source control.

## Not changed

- No Supabase changes.
- No UI changes.
- No migration to Supabase RPC yet; that is phase B and remains separate.
- Existing endpoint URLs are unchanged.
