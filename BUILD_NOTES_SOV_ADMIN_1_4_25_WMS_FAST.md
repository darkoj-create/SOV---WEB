# SOV Admin 1.4.25 — WMS Fast (HTTP keep-alive)

**versionName:** `1.4.25-wms-fast`
**versionCode:** `900113`
**releaseDate:** 2026-06-15
**Tip builda:** cisto klijentski (bez novog SQL-a, bez diranja arhitekture mapa)

---

## Problem

WMS karte (DGU TK25/HOK base + geoloski overlay) ucitavale su se sporo, narocito
na zoomu, i osjetno sporije od npr. OruxMapsa. Pan unutar istog zooma je bio ok
(cache pogodak), ali svaka promjena zooma je izazvala nalet novih tile zahtjeva
koji su bili spori.

## Glavni uzrok

U sva tri WMS download puta (`WmsBaseTilesOverlay`, `WmsTilesOverlay`,
`WmsPerformanceCache`) tile fetch je zavrsavao s `connection.disconnect()` u
`finally` bloku.

Na Androidu je `HttpURLConnection` implementiran preko OkHttp-a, sto znaci da
**besplatno** dobivamo connection pooling (i HTTP/2). Ali `disconnect()` nakon
svake pločice eksplicitno zatvara socket i sprjecava ponovnu upotrebu veze ->
svaka pojedina pločica radi novi TCP + TLS handshake prema `geoportal.dgu.hr`.
Na mobilnoj mreži je to ~100-500 ms rezijskog troska po pločici prije nego DGU
uopce pocne renderirati. Na ekranu s ~16 pločica + prefetch to se zbraja.

Zasto bas na zoomu: memorijski/disk cache su ključani po `z/x/y`. Promjena zooma
znaci da su sve vidljive pločice na novom zoom levelu odjednom "missing" ->
istovremeni nalet zahtjeva, svaki sa svojim handshakeom.

## Promjene

### 1. HTTP keep-alive (glavni fix)
- Maknut `connection.disconnect()` iz sva tri tile fetch puta.
- Na ne-2xx odgovorima sada se drenira `errorStream` (`.use { readBytes() }`)
  da se veza vrati u pool umjesto da ostane "prljava".
- `MainActivity.onCreate`: `http.keepAlive=true`, `http.maxConnections=8`
  (>= broj WMS download threadova: base 4 + fallback 2 + prefetch 1).

Posljedica: TCP/TLS handshake se placa jednom (prva pločica), a sve sljedece
pločice prema istom hostu reuse-aju vezu iz poola.

### 2. Mjerni logovi (`WmsTilePerfLog`)
- Novi fajl: `util/WmsTilePerfLog.kt`.
- Logira po pločici: `layer z=.. ttfb=..ms body=..ms size=..KB` pod Logcat
  tagom **`SOV_WMS_PERF`**.
- `ttfb` = TCP+TLS handshake + DGU render + cekanje na zaglavlja; `body` = download.
- Citanje: ako je PRVA pločica visok ttfb a sljedece bitno nizi -> keep-alive
  radi i handshake je bio glavni trosak. Ako su svi ttfb visoki i slicni ->
  usko grlo je DGU render po pločici (sljedeci korak: cachirani/tiled endpoint).
- Toggle: `WmsTilePerfLog.enabled` (default `true` za mjerenje na terenu;
  prebaci na `false` za produkciju da nema log spam-a).

## Sto NIJE dirano

- Arhitektura mapa, osmdroid setup, CRS logika (`WMSTileSource`, EPSG:3765 itd.).
- SOV Cloud, inventura RPC, Arhivar _v2, posudbe read-RPC, Field Hub (1.4.24).
- Nijedan Supabase poziv ni RPC. Nema novog SQL-a.

## Build / deploy

1. Otvori projekt u Android Studiju, Gradle sync.
2. Build APK -> `SOV-ADMIN-1.4.25.apk`.
3. (Mjerenje na terenu) Logcat filter `SOV_WMS_PERF`, otvori mapu, zoomiraj
   par puta, gledaj ttfb prve vs ostalih pločica.
4. Kad si zadovoljan, u `WmsTilePerfLog.kt` postavi `enabled = false` za sljedeci
   produkcijski build.

## Iduci moguci korak (nije u ovom buildu)

- Provjeriti nudi li DGU pre-cachirani/tiled (WMTS) endpoint za TK25/HOK; ako da,
  prelazak s on-the-fly GetMap na tiled servis obara DGU render pod (~300+ ms ->
  ~30-80 ms po pločici).
- Pre-warm susjednih zoom levela (z+1/z-1) u idle stanju za "instant" zoom feel.
- Konsolidacija tri download puta u jedan dijeljeni OkHttp klijent s zajednickim
  ConnectionPool-om.
