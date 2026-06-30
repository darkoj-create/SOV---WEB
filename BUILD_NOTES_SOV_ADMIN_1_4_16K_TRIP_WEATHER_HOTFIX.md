# SOV Admin v1.4.16k — Trip weather hotfix

Baseline: v1.4.16j-tools-calendar-events

## Changes

- Restored/strengthened weather forecast visibility inside expanded trip cards.
- Forecast section is now always visible when a trip is expanded and weather is relevant.
- Added explicit loading, empty/error messages and manual **Osvježi** action.
- Forecast still fetches lazily only after expanding a trip, so the list remains fast.
- Version bump: versionCode 900087, versionName 1.4.16k-trip-weather-hotfix.

## Not changed

- No SQL changes.
- No web changes.
