# SOV Admin v1.4.53b — DEM buildfix

## Svrha
Build fix za `v1.4.53a-dem-offline-cache`.

## Problem
`RecordDetailFeature.kt` je koristio `messenger.error(...)` unutar `DriveDrawingsCard`, ali taj composable nije imao lokalno dohvaćen `LocalSovMessenger.current`.

## Rješenje
Dodano je:

```kotlin
val messenger = LocalSovMessenger.current
```

unutar `DriveDrawingsCard`.

## Verzija
- `versionCode = 900152`
- `versionName = "1.4.53b-dem-buildfix"`

## Namjerno nije dirano
- DEM logika
- offline tile download
- MapFeature
- SovHttpClient / SovNetworkSecurity / SovPermissionsStore
- Supabase
- postojeći slojevi karte
