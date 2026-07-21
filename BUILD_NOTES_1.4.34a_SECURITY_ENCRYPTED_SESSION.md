# SOV Admin v1.4.34a — security encrypted session

## Scope
Phase 1.2 from the security plan: protect SOV Supabase session storage on device.

## Changed files
- `app/src/main/java/com/darko/speleov1/util/SovPermissionsStore.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`

## Changes
- Moved the full `SovPermissionsStore` preferences file from plaintext `SharedPreferences` to `EncryptedSharedPreferences`.
- Added one-time migration from legacy plaintext prefs:
  - copies `session` and `permissions` to encrypted storage;
  - clears legacy plaintext prefs after successful migration.
- Kept the public API unchanged:
  - `loadSession(context)`
  - `saveSession(context, session)`
  - `loadPermissions(context)`
  - `savePermissions(context, permissions)`
  - `clear(context)`
- Added safe fallback for broken/corrupted Android Keystore:
  - never crash during encrypted prefs initialization;
  - clear any legacy plaintext session;
  - return empty session so the app asks for login again.
- Disabled Android backup for the app with `android:allowBackup="false"`.
- Added dependency: `androidx.security:security-crypto:1.1.0-alpha06`.

## Version
- `versionCode = 900135`
- `versionName = "1.4.34a-security-encrypted-session"`

## Test checklist
1. Install over previous build with an existing logged-in session.
2. Open Cloud/Oružarstvo/Katastar and verify user remains logged in after migration.
3. Inspect app data backup settings: app backup disabled.
4. Logout/login and restart app; session should persist through encrypted prefs.
5. Clear app data and verify no crash and clean login flow.

## Build note
Sandbox could not run Gradle because Gradle wrapper download requires network access. Run locally:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```
