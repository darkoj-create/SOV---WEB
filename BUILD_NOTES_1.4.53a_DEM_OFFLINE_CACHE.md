# SOV Admin v1.4.53a — DEM offline cache

## Cilj
Korak 2 za DEM visine: offline paketi sada mogu skinuti AWS Terrain Tiles terrarium DEM podatke uz postojeće karte, bez crtanja DEM-a kao sloja.

## Verzija
- `versionCode = 900151`
- `versionName = "1.4.53a-dem-offline-cache"`

## Promjene
- Dodan `OfflineDemTileStore.kt` za offline DEM tileove pod `dem_terrarium/z/x/y.png`.
- `OfflineTileManager` dobio DEM wrapper funkcije:
  - `estimateDemTiles(...)`
  - `estimateDemBytes(...)`
  - `downloadDemArea(...)`
  - `findOfflineDemTile(...)`
  - `localDemTileCount(...)`
- `ElevationRepository` sada traži tile ovim redom:
  1. memory LRU
  2. offline paket `dem_terrarium/`
  3. disk cache `cacheDir/dem_terrarium/`
  4. mreža AWS terrarium endpoint
- Offline download dialog u `MapFeature.kt` dobio checkbox `Visine (DEM)`.
- DEM download koristi z9–13, procjenu `broj tileova × ~35 KB`.
- Offline/Maps ekran sada prikazuje DEM tile count za spremljene mape.
- DEM tileovi se eksplicitno preskaču u vizualnom tile countu, bounds računu i MBTiles exportu.

## Namjerno nije mijenjano
- Nije diran `SovNetworkSecurity`.
- Nisu mijenjani postojeći WMS/HGSS/hillshade/LocalTile overlayi.
- DEM se ne crta na karti.
- Nije rađen Korak 3.

## Provjera na uređaju
1. Karta → Tools → Download.
2. Označi malo područje.
3. Ostavi uključen checkbox `Visine (DEM)`.
4. Skini `Osnovno` ili `Teren`.
5. Uključi airplane mode.
6. Long-press/tap na području koje je skinuto.
7. Visina se mora prikazati iz offline DEM-a ili `— (offline)` bez rušenja ako tile fali.

## Build
Sandbox build nije potvrđen zbog Gradle wrapper DNS problema:
`UnknownHostException: services.gradle.org`.
