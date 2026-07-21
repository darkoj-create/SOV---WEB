package com.darko.speleov1.util

import android.os.SystemClock
import java.io.EOFException
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Zajednicki dohvat WMS/tile pločica s keep-alive vezama.
 *
 * Zasto postoji: keep-alive (bez disconnect()) drasticno ubrza ucitavanje jer izbjegava
 * TCP+TLS handshake po pločici. Ali server tiho zatvori neaktivne keep-alive veze; kad ih
 * klijent pokusa ponovo iskoristiti nakon pauze, dobije SocketException/EOFException/
 * ProtocolException. Bez obrade, ta pločica tiho "padne" i izoštravanje zooma postane sporo
 * sto se app vise koristi (sve vise veza odstoji i postane stale).
 *
 * Rjesenje: na TAKVE greske odmah ponovi zahtjev JEDNOM. Prvi neuspjeh izbaci mrtvu vezu iz
 * keep-alive poola, pa drugi pokusaj dobije svjezu vezu. NE ponavljamo na pravom timeoutu
 * (SocketTimeoutException) jer to znaci spor/nedostupan server, a ne stale vezu — inace bi
 * offline scenarij postao dvostruko sporiji.
 */
internal object SovTileHttp {

    data class Result(
        val code: Int,
        val bytes: ByteArray?,
        val ttfbMs: Long,
        val bodyMs: Long
    )

    fun get(
        urlText: String,
        accept: String,
        userAgent: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): Result {
        var lastStale: Throwable? = null
        repeat(2) { attempt ->
            try {
                return doGet(urlText, accept, userAgent, connectTimeoutMs, readTimeoutMs)
            } catch (e: SocketTimeoutException) {
                // Spor/nedostupan server, ne stale veza -> ne ponavljamo.
                throw e
            } catch (e: Throwable) {
                if (attempt == 0 && isStaleConnectionError(e)) {
                    // Mrtva keep-alive veza; prvi pokusaj ju je izbacio iz poola -> ponovi svjeze.
                    lastStale = e
                } else {
                    throw e
                }
            }
        }
        throw lastStale ?: java.io.IOException("tile fetch failed")
    }

    private fun doGet(
        urlText: String,
        accept: String,
        userAgent: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): Result {
        val url = URL(urlText)
        val connection = SovNetworkSecurity.openHttpConnection(url, "Karta/WMS tile").apply {
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            requestMethod = "GET"
            useCaches = true
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", accept)
            setRequestProperty("Cache-Control", "max-age=604800")
        }
        val tStart = SystemClock.elapsedRealtime()
        val code = connection.responseCode
        val tHeaders = SystemClock.elapsedRealtime()
        val bytes: ByteArray? = if (code in 200..299) {
            connection.inputStream.use { it.readBytes() }
        } else {
            // Drenira error body da se veza vrati cista u keep-alive pool.
            connection.errorStream?.use { it.readBytes() }
            null
        }
        val tBody = SystemClock.elapsedRealtime()
        // NAMJERNO bez connection.disconnect(): potpuno iscitan + zatvoren stream vraca vezu u pool.
        return Result(code, bytes, tHeaders - tStart, tBody - tHeaders)
    }

    private fun isStaleConnectionError(e: Throwable): Boolean =
        e is SocketException || e is EOFException || e is ProtocolException
}
