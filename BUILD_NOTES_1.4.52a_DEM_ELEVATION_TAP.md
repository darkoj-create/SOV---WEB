# SOV Admin v1.4.52a — DEM elevation tap

## Opseg
Korak 1 iz DEM plana: temeljni `ElevationRepository` + prikaz visine na long-press/tap dijalogu za waypoint.

## Verzija
- `versionCode = 900150`
- `versionName = "1.4.52a-dem-elevation-tap"`

## Što je dodano
- `util/ElevationRepository.kt`
  - AWS Terrain Tiles terrarium endpoint:
    `https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png`
  - default query zoom `z=13`
  - Web Mercator z/x/y + pixel matematika
  - terrarium formula: `(R * 256 + G + B / 256) - 32768`
  - invalid height filter: `< -11000` ili `> 9000`
  - bilinearna interpolacija iz 4 susjedna pixela
  - in-memory LRU za zadnjih 30 tileova
  - disk cache pod `cacheDir/dem_terrarium/`
  - tihi offline/null fallback bez spam logova
  - batch API `elevationsFor(points)` koji grupira po tileu

- `MapFeature.kt`
  - postojeći long-press waypoint dijalog sada prikazuje:
    - `Visina: 723 m`
    - ili `Visina: — (offline)`
  - nije dodan novi gesture listener; koristi postojeći `onLongPressPoint` tok.

- `ElevationRepositoryTest.kt`
  - test terrarium RGB dekodiranja
  - test invalid visina
  - test tile/pixel matematike za Zavižan na z=13
  - test clampanja rubnih koordinata

## Namjerno nije rađeno
- Korak 2 offline DEM download nije rađen u ovoj verziji.
- Korak 3 visinski profil tracka i DEM visina objekta nisu rađeni u ovoj verziji.
- Nisu dirani postojeći WMS/hillshade/offline slojevi.
- Nisu dirani `SovHttpClient`, `SovPermissionsStore`, `SovNetworkSecurity`.

## Test u sandboxu
Gradle compile nije potvrđen jer wrapper ne može dohvatiti distribuciju:
`UnknownHostException: services.gradle.org`

## Lokalna provjera
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

## Test na uređaju
1. Otvori kartu online.
2. Long-press na lokaciju oko Velebita/Zavižana.
3. Dijaloški tekst mora prikazati koordinate i red `Visina: NNN m`.
4. Uključi airplane mode, ponovi na već dohvaćenom području.
5. Ako je tile bio cacheiran, visina se i dalje prikazuje.
6. Ako nije cacheiran, prikazuje se `Visina: — (offline)` bez rušenja.
