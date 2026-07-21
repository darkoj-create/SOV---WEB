# SOV Admin v1.4.35a — log redaction hardening

Base: v1.4.34a-security-encrypted-session

Scope: Android-side security hardening only. No Supabase REST/RPC endpoint changes, no UI flow changes, no session key changes.

Changes:
- SovClientLogger now uses BuildConfig.VERSION_NAME instead of stale hardcoded app version.
- Supabase handled-error logs and Crashlytics details sanitize obvious secrets before sending/logging:
  - Bearer tokens
  - access_token / refresh_token
  - apikey / authorization values
  - embedded email values in generic strings
  - Supabase anon key string
- Crashlytics/Supabase device_info no longer includes raw email; it stores only a short SHA-256 email hash.
- SovHttpClient no longer sends full failing URL into remote logs; it sends only the REST/RPC endpoint path without query string.
- External file import debug logs no longer print raw content/file URIs, and import debug logging is debug-build only.
- WMS perf Logcat output is debug-build only.

Intentional non-change:
- AndroidManifest still has usesCleartextTraffic=true because SOV Field Hub and custom/local WMS workflows still support plain HTTP endpoints such as local laptop/hotspot addresses. This needs a separate compatibility migration if we want strict cleartext blocking later.

Version:
- versionCode 900136
- versionName 1.4.35a-log-redaction-hardening

Manual test:
1. Build: ./gradlew :app:compileDebugKotlin && ./gradlew :app:assembleDebug
2. Login/logout still works.
3. Open external GPX/KML/MBTiles from file manager/WhatsApp; import still works.
4. Trigger a harmless network error and verify the app does not crash.
5. Debug build: WMS perf logs may appear. Release build: WMS perf logs must not spam Logcat.
