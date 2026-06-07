package com.darko.speleov1

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class DarkoOsTrackExportResult(
    val ok: Boolean = false,
    val row: Int = 0,
    val url: String = "",
    val error: String? = null
)

private data class DarkoOsTrackExportResponse(
    val ok: Boolean = false,
    val row: Int = 0,
    val url: String = "",
    val error: String? = null
)

private data class DarkoOsTrackStats(
    val distanceM: Double,
    val ascentM: Double,
    val descentM: Double,
    val durationMin: Double,
    val startMillis: Long,
    val endMillis: Long,
    val startLat: Double?,
    val startLon: Double?,
    val endLat: Double?,
    val endLon: Double?,
    val minLat: Double?,
    val maxLat: Double?,
    val minLon: Double?,
    val maxLon: Double?
)

internal object DarkoOsTrackSyncClient {
    /**
     * Darko OS admin endpoint.
     * 1) Deploy DARKO_OS_TRACK_EXPORT_WEBAPP.gs as a Web App.
     * 2) Paste the /exec URL here.
     * 3) Rebuild SOV Admin.
     */
    private const val DARKO_OS_WEBAPP_URL = "https://script.google.com/macros/s/AKfycbxVrStMsScP6UWVESnC6hcQV7MM2XRpzEQYZYblOgjHt1gzIhiinUfpQewjAY3ZnR2w/exec"
    private val gson = Gson()

    suspend fun exportTrack(track: SavedTrack): DarkoOsTrackExportResult = withContext(Dispatchers.IO) {
        if (DARKO_OS_WEBAPP_URL.startsWith("PASTE_")) {
            return@withContext DarkoOsTrackExportResult(ok = false, error = "Darko OS endpoint nije postavljen u admin buildu.")
        }
        if (track.points.size < 2) {
            return@withContext DarkoOsTrackExportResult(ok = false, error = "Track nema dovoljno GPS točaka za export.")
        }

        val stats = computeStats(track)
        val gpx = buildGpx(track)
        val params = linkedMapOf(
            "action" to "appendTrack",
            "source" to "SOV_ADMIN",
            "trackId" to track.id,
            "name" to track.name,
            "description" to track.description,
            "createdAtMillis" to track.createdAtMillis.toString(),
            "createdAt" to isoDateTime(track.createdAtMillis),
            "startTime" to isoDateTime(stats.startMillis),
            "endTime" to isoDateTime(stats.endMillis),
            "durationMin" to formatNumber(stats.durationMin),
            "distanceKm" to formatNumber(stats.distanceM / 1000.0),
            "ascentM" to formatNumber(stats.ascentM),
            "descentM" to formatNumber(stats.descentM),
            "pointCount" to track.points.size.toString(),
            "startLat" to stats.startLat.orBlank(),
            "startLon" to stats.startLon.orBlank(),
            "endLat" to stats.endLat.orBlank(),
            "endLon" to stats.endLon.orBlank(),
            "minLat" to stats.minLat.orBlank(),
            "maxLat" to stats.maxLat.orBlank(),
            "minLon" to stats.minLon.orBlank(),
            "maxLon" to stats.maxLon.orBlank(),
            "gpx" to gpx
        )

        return@withContext try {
            val body = params.entries.joinToString("&") { (k, v) ->
                URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
            }
            val conn = (URL(DARKO_OS_WEBAPP_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val responseBody = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }
            val parsed = gson.fromJson(responseBody, DarkoOsTrackExportResponse::class.java)
            DarkoOsTrackExportResult(ok = parsed.ok, row = parsed.row, url = parsed.url, error = parsed.error)
        } catch (err: Exception) {
            DarkoOsTrackExportResult(ok = false, error = err.message ?: err.toString())
        }
    }

    private fun computeStats(track: SavedTrack): DarkoOsTrackStats {
        var distance = 0.0
        var ascent = 0.0
        var descent = 0.0
        var lastAltitude: Double? = null
        val lats = track.points.map { it.point.latitude }
        val lons = track.points.map { it.point.longitude }
        for (i in 1 until track.points.size) {
            val a = track.points[i - 1]
            val b = track.points[i]
            distance += haversineMeters(a.point.latitude, a.point.longitude, b.point.latitude, b.point.longitude)
        }
        track.points.forEach { p ->
            val alt = p.altitudeM
            if (alt != null && alt.isFinite()) {
                lastAltitude?.let { prev ->
                    val delta = alt - prev
                    if (abs(delta) >= 1.0) {
                        if (delta > 0) ascent += delta else descent += abs(delta)
                    }
                }
                lastAltitude = alt
            }
        }
        val start = track.createdAtMillis
        val durationMin = if (distance > 0.0) 0.0 else 0.0
        return DarkoOsTrackStats(
            distanceM = distance,
            ascentM = ascent,
            descentM = descent,
            durationMin = durationMin,
            startMillis = start,
            endMillis = start,
            startLat = track.points.firstOrNull()?.point?.latitude,
            startLon = track.points.firstOrNull()?.point?.longitude,
            endLat = track.points.lastOrNull()?.point?.latitude,
            endLon = track.points.lastOrNull()?.point?.longitude,
            minLat = lats.minOrNull(),
            maxLat = lats.maxOrNull(),
            minLon = lons.minOrNull(),
            maxLon = lons.maxOrNull()
        )
    }

    private fun buildGpx(track: SavedTrack): String {
        val safeName = xml(track.name.ifBlank { "SOV track" })
        val safeDesc = xml(track.description)
        return buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<gpx version=\"1.1\" creator=\"SOV Admin Darko OS\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
            appendLine("  <trk>")
            appendLine("    <name>$safeName</name>")
            if (safeDesc.isNotBlank()) appendLine("    <desc>$safeDesc</desc>")
            appendLine("    <trkseg>")
            track.points.forEach { p ->
                append("      <trkpt lat=\"")
                append(p.point.latitude)
                append("\" lon=\"")
                append(p.point.longitude)
                append("\">")
                val alt = p.altitudeM
                if (alt != null && alt.isFinite()) append("<ele>").append(formatNumber(alt)).append("</ele>")
                appendLine("</trkpt>")
            }
            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun isoDateTime(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(millis))
    private fun formatNumber(value: Double): String = String.format(Locale.US, "%.3f", value)
    private fun Double?.orBlank(): String = this?.let { formatNumber(it) } ?: ""
    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
