# SOV Admin 1.4.26 — WMS Fast (stale-connection fix)

**versionName:** `1.4.26-wms-fast`
**versionCode:** `900114`
**releaseDate:** 2026-06-15
**Tip builda:** cisto klijentski, bez novog SQL-a, bez nove dependencije

---

## Simptom (feedback na 1.4.25)

Odmah nakon 1.4.25 WMS je radio dobro, ali nakon nekog vremena koristenja zoom je
opet postao spor — "presporo izostrava" (predugo ostaje mutna parent pločica prije
nego stigne ostra pločica novog zooma).

## Uzrok: stale keep-alive veze

1.4.25 je uveo keep-alive (maknut `disconnect()`) da se izbjegne TCP+TLS handshake
po pločici. Nuspojava: server (ili proxy na putu) tiho zatvori NEAKTIVNU keep-alive
vezu nakon par sekundi. Kad app nakon pauze (gledas kartu pa zoomiras) pokusa tu
vezu ponovo iskoristiti, dobije `SocketException`/`EOFException`/`ProtocolException`.

Stari kod je tu gresku samo progutao -> pločica tiho padne i ceka sljedeci frame da
se ponovo zatrazi. Sto se app vise koristi, to vise veza odstoji i postane stale ->
sve sporije izostravanje. Klasican "brzo na pocetku, sporo kasnije" obrazac keep-alivea.

## Promjena

### Novi `util/SovTileHttp.kt` — zajednicki tile fetch
- Jedna tocka za dohvat svih pločica (base, geoloski overlay, prefetch, hillshade).
- **Retry-once na stale vezi:** kad prvi pokusaj baci stale-connection gresku
  (`SocketException` / `EOFException` / `ProtocolException`), odmah ponovi JEDNOM.
  Prvi neuspjeh izbaci mrtvu vezu iz keep-alive poola, pa drugi pokusaj dobije svjezu.
- **Ne ponavlja na `SocketTimeoutException`:** pravi spor/nedostupan server nije stale
  veza; ponavljanje bi samo udvostrucilo cekanje (offline bi postao sporiji). Zato se
  timeout propagira odmah.
- Zadrzava keep-alive (bez `disconnect()`) i puno iscitavanje tijela (vraca vezu u pool).
- Vraca i `ttfbMs`/`bodyMs` za `WmsTilePerfLog`.

### Rutirano kroz helper
- `WmsBaseTilesOverlay`, `WmsTilesOverlay`, `WmsPerformanceCache`, `HillshadeTilesOverlay`
  sada svi zovu `SovTileHttp.get(...)` umjesto vlastitog `HttpURLConnection` bloka.
- Uklonjeni nekoristeni `java.net.HttpURLConnection` / `java.net.URL` importi iz tih fajlova.

### Mjerni logovi (iz 1.4.25, i dalje aktivni)
- Logcat tag **`SOV_WMS_PERF`**: `layer z=.. ttfb=..ms body=..ms size=..KB`.
- Dijagnostika:
  - kasnije pločice visok `ttfb` u naletima / s rupama  -> bile su stale veze (ovaj fix to gadja);
  - SVI `ttfb` ravnomjerno visoki                        -> usko grlo je DGU render po pločici
    (sljedeci korak: cachirani/tiled (WMTS) endpoint umjesto on-the-fly GetMap).
- Toggle: `WmsTilePerfLog.enabled` (default `true`; postavi `false` za produkciju).

## Sto NIJE dirano

- Arhitektura mapa, osmdroid, CRS logika, prefetch/fallback strategija.
- SOV Cloud, inventura RPC, Arhivar _v2, posudbe, Field Hub.
- Nijedna nova biblioteka (i dalje cisti `HttpURLConnection`, samo robustnije).

## Build / mjerenje

1. Android Studio -> Gradle sync -> build APK `SOV-ADMIN-1.4.26.apk`.
2. Logcat filter `SOV_WMS_PERF`. Koristi kartu par minuta, zoomiraj, pa pauziraj
   20-30 s i opet zoomiraj (to izaziva stale veze). Usporedi `ttfb` prije/poslije fixa.
3. Ako je i dalje sporo a `ttfb` su ravnomjerno visoki -> problem je DGU render, ne app;
   tada je iduci korak tiled/WMTS endpoint ili agresivniji offline pre-cache podrucja.
