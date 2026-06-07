# SOV Admin 1.4.16g — Session refresh race fix

Baseline: `1.4.16f-db-gate-session-cache-first`

## Implemented

- `SovPermissionsStore.kt`
  - legacy sessions with `expiresAtMillis == 0L` are treated as expired
  - `saveSession()` now uses synchronous `.commit()` so all threads immediately see refreshed tokens

- `SovHttpClient.kt`
  - proactive refresh also handles legacy sessions with no expiry timestamp
  - reactive HTTP 401 refresh retries the request once
  - refresh operations are serialized with a process-wide lock
  - session is reloaded inside the lock to avoid consuming a refresh token already rotated by another request
  - role startup sync and Field Tracking refresh now use the same synchronized refresh path
  - refreshed session is synchronously persisted before returning the access token
  - clear re-login message when refresh is impossible or rejected

- `SpeleoAppRoot.kt`
  - startup role/session sync now uses `forceNetwork = true`

- Version
  - versionCode: `900083`
  - versionName: `1.4.16g-session-refresh-race-fix`

## Preserved

- DB-gate baseline and live Supabase armory logic
- Oružar/Admin armory view
- future trips behavior
- cache-first armory rendering
- existing repository migrations to `SovHttpClient`

## Not changed

- Web build
- Supabase SQL/database schema
- Login screen flow
