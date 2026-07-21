# SOV Admin v1.4.48a — l10n Field Status

## Scope
Small Phase 3.4 localization pass focused only on field-status utility screens.

Base: `v1.4.47a-l10n-laptop-hub`
New version: `v1.4.48a-l10n-field-status`

## Changed files
- `app/build.gradle.kts`
- `app/src/main/java/com/darko/speleov1/GpsStatusScreen.kt`
- `app/src/main/java/com/darko/speleov1/CompassStatusScreen.kt`
- `app/src/main/java/com/darko/speleov1/SignalStatusScreen.kt`

## Changes
- Bumped versionCode to `900146`.
- Bumped versionName to `1.4.48a-l10n-field-status`.
- Localized top-bar titles/back content descriptions for:
  - GPS status
  - Compass status
  - Signal and coverage
- Localized visible text in `SignalStatusScreen`:
  - header/subtitle
  - permission messages
  - network status labels
  - nearby networks section
  - refresh cells button
  - empty/error states
  - connected/visible labels in cell rows
- Kept technical radio labels unchanged where appropriate:
  - dBm
  - Cell ID
  - TAC / PCI / EARFCN / NRARFCN / LAC / PSC / ARFCN / BSIC

## Not changed
- No Supabase changes.
- No security/RLS changes.
- No self-update changes.
- No network/Field Hub logic changes.
- No UI layout changes beyond localized strings.
- No sensor/telephony logic changes.

## Local validation requested
```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

## Smoke test
- Open GPS status screen in HR and EN.
- Open Compass status screen in HR and EN.
- Open Signal and coverage screen in HR and EN.
- Deny and allow permissions to confirm permission messages still display normally.
