# SOV Admin v1.4.46a — L10N My Base

## Base
- Source base: `v1.4.45a-first-unit-tests`
- New version: `v1.4.46a-l10n-my-base`
- `versionCode = 900144`

## Scope
FAZA 3 / 3.4 — localization cleanup, intentionally small and screen-scoped.

This patch localizes the **Moja baza / My Base** settings/status area only. It does not start a large cross-app string migration.

## Changed
- `app/build.gradle.kts`
- `app/src/main/java/com/darko/speleov1/HomeAndToolsScreens.kt`

## Details
- Localized the empty My Base summary:
  - HR: `Nema učitanih KML/CSV točaka`
  - EN: `No KML/CSV points loaded`
- Localized hardcoded My Base icon/content descriptions:
  - My Base / Search / Status / File import / Download / Open menu / Document / Map / Delete
- Kept dynamic repository messages untouched unless they match the known empty-state text.

## Not changed
- Supabase
- security / RLS
- self-update
- Apps Script
- networking
- business logic
- large localization system
- other screens

## Build status
Gradle build was not verified in this environment because the wrapper cannot download Gradle:
`UnknownHostException: services.gradle.org`.

Run locally:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

## Smoke test
- Settings → Moja baza in Croatian.
- Settings → My Base in English.
- Empty-state summary should switch language.
- Import/export/clear buttons should behave exactly as before.
