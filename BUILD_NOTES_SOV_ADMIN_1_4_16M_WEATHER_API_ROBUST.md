# SOV Admin v1.4.16m — weather API robust hotfix

Base: v1.4.16l trips clean copy.

## Fixed
- Reworked trip weather API calls in `FieldPackageFeature.kt`.
- Uses current Open-Meteo daily parameter names: `wind_speed_10m_max` and `weather_code`.
- Keeps backward fallback parsing for old names: `windspeed_10m_max` and `weathercode`.
- Adds HTTP error-safe response reading; API 4xx/5xx no longer crashes the weather fetch path.
- Adds forecast range guard: Open-Meteo forecast is shown only inside the supported upcoming window; farther future trips show a clean message instead of failed API calls.
- Improves location geocoding candidates for Croatian trip names and speleo regions.
- Keeps weather lazy-loading: forecast is fetched only after expanding a trip card.

## Version
- versionCode: 900089
- versionName: 1.4.16m-weather-api-robust

## Not changed
- No web changes.
- No SQL changes.
