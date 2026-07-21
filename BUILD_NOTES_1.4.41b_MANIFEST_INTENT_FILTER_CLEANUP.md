# SOV Admin v1.4.41b — manifest intent-filter cleanup

## Važno

Ovo je nastavak nakon rollbacka `v1.4.41a-apk-sha256-update`.
Self-update sustav NIJE diran u ovom buildu. Bazna verzija je `v1.4.40a-apps-script-key`.

## Version

- `versionCode = 900139`
- `versionName = "1.4.41b-manifest-intent-filter-cleanup"`

## Problem

`AndroidManifest.xml` je imao široke `VIEW` i `SEND` intent-filtere za generičke MIME tipove:

- `application/json`
- `application/xml`
- `text/plain`
- `application/zip`
- `application/octet-stream`
- spreadsheet/csv varijante

Zbog toga se SOV nudio u Android "Otvori pomoću" za previše nepovezanih datoteka.

## Promjene

### AndroidManifest.xml

`ACTION_VIEW` je sužen na:

- `.sovpkg`
- `.kml`
- `.kmz`
- `.gpx`
- `.geojson`
- `.gpkg` / `.geopackage`
- `.mbtiles`
- `.shp`
- `.tif` / `.tiff`
- konkretne geo MIME tipove

Iz `VIEW` filtera su maknuti generički MIME tipovi:

- `application/json`
- `application/xml`
- `text/xml`
- `text/plain`
- `text/csv`
- Excel MIME tipovi
- `application/zip`
- `application/octet-stream`

`ACTION_SEND` i `SEND_MULTIPLE` zadržavaju `application/zip` i `application/octet-stream` samo zbog WhatsApp/Drive slučaja gdje `.sovpkg` ili `.kmz` dolazi kao generički zip/binary.

### MainActivity.kt

`isSupportedOpenUri(...)` više ne prihvaća generički `json/xml/plain/zip/octet-stream` samo po MIME tipu.
Ako provider pošalje generički MIME, app provjerava stvarni naziv/ekstenziju:

- `.sovpkg`
- `.kml`, `.kmz`, `.gpx`, `.geojson`
- `.gpkg`, `.geopackage`, `.mbtiles`, `.shp`
- `.tif`, `.tiff`

Time se smanjuje lažno otvaranje nepovezanih datoteka.

## Namjerno nije dirano

- `util/AppUpdateManager.kt`
- self-update logika
- Supabase/RPC
- import parseri
- UI

## Test checklist

1. File manager → `.gpx` → SOV se nudi i import radi.
2. File manager → `.kml` / `.kmz` → SOV se nudi i import radi.
3. File manager → `.mbtiles` → SOV se nudi i import radi.
4. File manager → `.sovpkg` → SOV se nudi i import radi.
5. WhatsApp/Drive share `.sovpkg` → SOV se nudi i import radi.
6. Obični `.txt`, `.json`, `.xml`, `.xlsx`, generički `.zip` → SOV se više ne bi trebao nuditi preko VIEW.
7. Ako se SOV ipak pojavi preko share sheeta za generički binary, app mora ignorirati nepodržanu datoteku bez crasha.

## Build status

Build nije potvrđen u sandboxu zbog Gradle wrapper DNS problema:

```text
UnknownHostException: services.gradle.org
```

Lokalno pokrenuti:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```
