package com.darko.speleov1.util

import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * App-side cleartext policy.
 *
 * Android Network Security Config cannot express CIDR/private ranges like 192.168.0.0/16
 * or 172.16.0.0/12. SOV needs arbitrary local IPs for Field Hub / hotspot WMS on terrain,
 * so the manifest-level cleartext flag must remain compatible, while every app HTTP entry
 * point enforces this allowlist before opening a connection.
 */
internal object SovNetworkSecurity {
    private const val BLOCKED_CLEARTEXT_MESSAGE =
        "HTTP je dopušten samo za lokalni SOV Hub/private LAN. Za vanjske servere koristi HTTPS."

    fun openHttpConnection(urlText: String, feature: String = "SOV network"): HttpURLConnection {
        val url = URL(urlText)
        requireCleartextAllowed(url, feature)
        return url.openConnection() as HttpURLConnection
    }

    fun openHttpConnection(url: URL, feature: String = "SOV network"): HttpURLConnection {
        requireCleartextAllowed(url, feature)
        return url.openConnection() as HttpURLConnection
    }

    fun requireCleartextAllowed(urlText: String, feature: String = "SOV network") {
        val url = URL(urlText)
        requireCleartextAllowed(url, feature)
    }

    fun isAllowedCleartextUrl(urlText: String): Boolean = runCatching {
        val url = URL(urlText)
        !url.protocol.equals("http", ignoreCase = true) || isAllowedLocalCleartextHost(url.host)
    }.getOrDefault(false)

    fun isAllowedLocalCleartextHost(rawHost: String?): Boolean {
        val host = rawHost
            ?.trim()
            ?.removePrefix("[")
            ?.removeSuffix("]")
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        if (host.isBlank()) return false
        if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "0:0:0:0:0:0:0:1") return true
        if (host == "10.0.2.2" || host.endsWith(".local")) return true

        val octets = host.split('.').map { it.toIntOrNull() }
        if (octets.size != 4) return false
        val parts = octets.map { it ?: return false }
        if (parts.any { it !in 0..255 }) return false
        val a = parts[0]
        val b = parts[1]
        return when {
            a == 10 -> true
            a == 127 -> true
            a == 169 && b == 254 -> true
            a == 172 && b in 16..31 -> true
            a == 192 && b == 168 -> true
            a == 100 && b in 64..127 -> true // CGNAT / hotspot-ish private carrier range.
            else -> false
        }
    }

    private fun requireCleartextAllowed(url: URL, feature: String) {
        if (!url.protocol.equals("http", ignoreCase = true)) return
        if (isAllowedLocalCleartextHost(url.host)) return
        throw SecurityException("$feature: $BLOCKED_CLEARTEXT_MESSAGE (${url.host})")
    }
}
