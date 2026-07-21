# SOV Admin APK 1.4.29j — Linked SOV folder

Baseline: `1.4.29i-katastar-visible-mybase-startup-source`

## Fix
Android ne vidi uvijek javni `Download/SOV/Offline` folder zbog scoped storage pravila. Direktno skeniranje foldera zato nije pouzdano.

## Novo
- Offline ekran ima gumb **Poveži SOV folder**.
- Korisnik jednom odabere `Download/SOV/Offline` preko Android folder pickera.
- App sprema trajnu dozvolu za taj folder.
- Gumb **Osvježi** ponovno skenira povezani folder.
- Ako je folder već povezan, app ga skenira i na otvaranju Offline ekrana.

## Što se vraća u UI
- `.gpx` → GPX/track/import slojevi
- `.kml`, `.kmz` → KML/import slojevi + Moja baza kad ima točke
- `.geojson`, `.json`, `.csv`, `.gpkg` → import slojevi
- `.mbtiles` → offline/custom karte

## Sigurnost
- Ne briše postojeće fileove.
- Ne parsira velike baze na startupu prije UI-ja.
- Radi add-only copy u app working foldere.
- Nema SQL promjena.

## Datoteke
- `app/src/main/java/com/darko/speleov1/util/SovLinkedOfflineFolder.kt`
- `app/src/main/java/com/darko/speleov1/util/SovNativeOfflineFolders.kt`
- `app/src/main/java/com/darko/speleov1/OfflineFeature.kt`
- `app/build.gradle.kts`
