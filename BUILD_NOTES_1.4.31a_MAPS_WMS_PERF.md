# SOV Admin v1.4.31a — Maps/WMS performance patch

Bazirano na: v1.4.30a Runner perf/graphics source.

## Promjene

### LocalTileOverlay
- Uklonjen sync decode iz `draw()` za offline folder tiles i MBTiles.
- `draw()` sada čita samo memory cache; missing tileovi se šalju na single-thread `decodeExecutor`.
- Dodan `pending` set da se isti tile ne dekodira više puta.
- Dodan parent-tile fallback do dubine 4, da panning/zoom ne ostavlja rupe dok pravi tile stiže.
- MBTiles zoom-level query ide asinkrono preko decode executora; DB inicijalizacija je sinkronizirana.
- LRU cache prebačen na byte-based 48 MB limit.
- Prvi dekodirani offline tile određuje ARGB_8888/RGB_565 preferencu za tu mapu.

### Map invalidate coalescing
- Dodan `MapInvalidateCoalescer`.
- `WmsBaseTilesOverlay`, `WmsTilesOverlay`, `HillshadeTilesOverlay` i `LocalTileOverlay` više ne okidaju redraw po svakom završenom tileu, nego coalescaju redraw na ~60 ms.

### Hardware bitmap cache
- `WmsTileImageCache` dobio `toHardware(bitmap)` helper za API 26+.
- WMS base, WMS overlay, hillshade i local offline tileovi konvertiraju tile u hardware bitmap nakon validacije/decodea i prije memory cachea.
- Redoslijed validacije ostaje decode → pixel validation → hardware conversion, da `rejectMostlyBlack` ne čita hardware bitmapu.

### WmsPerformanceCache cleanup
- Uklonjen mrtvi `prefetchVisible()` i pripadni state/executor koji se više ne pozivaju.
- Ostavljeni su `install()`, `sourceKey()`, `overlayCacheFile()` i `baseCacheFile()`; cache schema ostaje `v2`.

## Nije dirano u ovom patchu
- Nije uveden OkHttp dependency; `SovTileHttp` ostaje `HttpURLConnection` + postojeći stale retry.
- Nije rađen metatile 512×512 zahvat.
- Nisu mijenjani javni konstruktori overlay klasa ni `stableKey`.
- Nije mijenjana disk cache struktura `sov_wms_base_cache/...` ni schema verzija.

## Test matrica
- WMS TK25 preset
- WMS custom 1.1.1 / 1.3.0 / EPSG:3765
- OFFLINE folder tiles
- OFFLINE MBTiles
- OPENTOPO i HGSS
- hillshade ON
- geološki overlay ON
- custom offline overlayi
- markeri/klasteri, trackovi, ruler, crtanje
- rotacija karte / heading-up

## Build status
- Sandbox build nije mogao završiti jer Gradle wrapper pokušava skinuti Gradle 8.7 sa `services.gradle.org`, a runtime nema DNS/internet.
- Lokalno pokrenuti: `./gradlew :app:compileDebugKotlin` i zatim `./gradlew :app:assembleDebug`.
