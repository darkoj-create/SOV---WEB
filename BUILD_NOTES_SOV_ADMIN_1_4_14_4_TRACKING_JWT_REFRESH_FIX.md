# SOV Admin 1.4.14.4 — Tracking JWT refresh fix

Fix for Field Tracking Lite sync error:

- Supabase HTTP 401 / PGRST303 / `JWT expired` no longer permanently blocks tracking sync.
- Tracking API now refreshes the stored Supabase session automatically before requests when token is near expiry.
- If Supabase still returns JWT expired, the request refreshes the session once and retries.
- Tracking queue remains local until sync succeeds; no points are deleted on auth/network errors.
- UI now shows a Croatian friendly message instead of raw Supabase JSON.
- Keeps Field Tracking team map viewer and previous 1.4.14.3 build fix.

Expected APK: `SOV-ADMIN-1.4.14.4.apk`
VersionCode: `900061`
VersionName: `1.4.14.4-tracking-jwt-refresh-fix`
