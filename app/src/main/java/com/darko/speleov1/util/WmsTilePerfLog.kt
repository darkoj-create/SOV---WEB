package com.darko.speleov1.util

import android.os.SystemClock
import android.util.Log

/**
 * Lagani mjerni log za WMS dohvat pločica.
 *
 * Svrha: razdvojiti "time-to-first-byte" (TCP + TLS handshake + DGU render + cekanje na zaglavlja)
 * od samog downloada tijela pločice. Ako keep-alive radi, PRVA pločica nakon otvaranja mape
 * placa handshake (visok TTFB), a sve sljedece pločice prema istom hostu imaju bitno nizi TTFB
 * jer se TCP/TLS veza ponovo koristi iz poola.
 *
 * Citanje u Logcatu (filter tag: SOV_WMS_PERF):
 *   base z=15 ttfb=412ms body=23ms size=18KB   <- prva pločica (handshake + render)
 *   base z=15 ttfb=180ms body=19ms size=17KB   <- sljedece (samo render, veza reused)
 *
 * Ako je razlika prve vs ostalih velika -> keep-alive radi i handshake je bio glavni trosak.
 * Ako su svi TTFB visoki i slicni -> usko grlo je DGU render po pločici (rjesenje: cachirani/tiled endpoint).
 *
 * Ostavi ENABLED = true dok mjeris na terenu, pa prebaci na false za produkciju da nema log spam-a.
 */
internal object WmsTilePerfLog {
    @Volatile var enabled: Boolean = true

    private const val TAG = "SOV_WMS_PERF"

    /**
     * @param layer  "base", "overlay" ili "prefetch"
     * @param z      zoom level pločice
     * @param tStart elapsedRealtime prije connection.responseCode
     * @param tHeaders elapsedRealtime nakon sto su zaglavlja stigla (responseCode se vratio)
     * @param tBody  elapsedRealtime nakon sto je tijelo procitano
     * @param bytes  velicina pločice u bajtovima
     */
    fun log(layer: String, z: Int, tStart: Long, tHeaders: Long, tBody: Long, bytes: Int) {
        if (!enabled) return
        val ttfbMs = tHeaders - tStart
        val bodyMs = tBody - tHeaders
        val sizeKb = (bytes + 512) / 1024
        Log.d(TAG, "$layer z=$z ttfb=${ttfbMs}ms body=${bodyMs}ms size=${sizeKb}KB")
    }

    /** Varijanta kad su ttfb/body vec izmjereni (npr. iz SovTileHttp.Result). */
    fun log(layer: String, z: Int, ttfbMs: Long, bodyMs: Long, bytes: Int) {
        if (!enabled) return
        val sizeKb = (bytes + 512) / 1024
        Log.d(TAG, "$layer z=$z ttfb=${ttfbMs}ms body=${bodyMs}ms size=${sizeKb}KB")
    }

    fun now(): Long = SystemClock.elapsedRealtime()
}
