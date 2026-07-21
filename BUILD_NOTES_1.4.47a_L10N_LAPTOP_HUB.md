# SOV Admin v1.4.47a — L10N Laptop Hub

Base: `v1.4.46a-l10n-my-base`

## Scope
Small FAZA 3 / 3.4 localization pass for the Laptop Hub screen only.

## Version
- `versionCode = 900145`
- `versionName = "1.4.47a-l10n-laptop-hub"`

## Changed files
- `app/build.gradle.kts`
- `app/src/main/java/com/darko/speleov1/HomeAndToolsScreens.kt`

## Changes
- Added `LocalAppLanguage.current` in `SovLaptopHubScreen`.
- Localized Laptop Hub title/subtitle, connect instructions, address label, buttons and status messages.
- Localized back/refresh icon content descriptions in Laptop Hub.
- Kept endpoint placeholder, PIN, and Test label unchanged because they are technical/universal labels.

## Not changed
- No Supabase changes.
- No security/RLS changes.
- No update-system changes.
- No Field Hub network/client logic changes.
- No business logic changes.

## Local verification
Run locally:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

## Manual smoke test
- Open Settings / Laptop Hub.
- Switch HR/EN language.
- Verify screen labels and status messages switch language.
- Save settings, test hub, fetch roster.
