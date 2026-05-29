package com.darko.speleov1.util

import com.darko.speleov1.ImportedLayer
import com.darko.speleov1.MarkedPoint
import com.darko.speleov1.SavedTrack
import com.darko.speleov1.TrackPoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.OutputStreamWriter

data class SharedLayerEntry(
    val id: String = "",
    val name: String = "",
    val type: String = "point",       // "point" | "track"
    val subtype: String = "kml",      // "kml" | "gpx"
    val author: String = "",
    val createdAt: String = "",
    val description: String = "",
    val tags: String = "",
    val lat: Double? = null,
    val lon: Double? = null,
    val alt: Double? = null,
    val pointsJson: String = "",      // JSON array [{lat,lon,alt}] for tracks
    val visible: Boolean = true
)

private data class SharedLayersListResponse(
    val ok: Boolean = false,
    val layers: List<SharedLayerEntry> = emptyList()
)

private data class SharedLayersActionResponse(
    val ok: Boolean = false,
    val error: String? = null
)

private data class TrackPointDto(
    val lat: Double,
    val lon: Double,
    val alt: Double? = null
)

object SharedLayersSyncClient {


    private const val WEBAPP_URL = "https://script.google.com/macros/s/AKfycbyZ8RgafxfOiWYwBdy8W-5XmxOwdI7kMdqzMhktGW4mSfp4rIVEBz4jUztacYpankgTJw/exec"

    private val gson = Gson()

    // --- READ ---

    suspend fun fetchLayers(): List<SharedLayerEntry> = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = "$WEBAPP_URL?action=listLayers"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 9000
                readTimeout = 9000
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            val body = if (code in 200..299)
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            else ""
            conn.disconnect()
            if (body.isBlank()) emptyList()
            else {
                val parsed = gson.fromJson(body, SharedLayersListResponse::class.java)
                if (parsed.ok) parsed.layers else emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- WRITE ---

    suspend fun addPoint(
        id: String,
        name: String,
        author: String,
        description: String,
        tags: String,
        lat: Double,
        lon: Double,
        alt: Double?
    ): Boolean = withContext(Dispatchers.IO) {
        postJson(
            mapOf(
                "action" to "addLayer",
                "id" to id,
                "name" to name,
                "type" to "point",
                "subtype" to "kml",
                "author" to author,
                "description" to description,
                "tags" to tags,
                "lat" to lat.toString(),
                "lon" to lon.toString(),
                "alt" to (alt?.toString() ?: "")
            )
        )
    }

    suspend fun addTrack(
        id: String,
        name: String,
        author: String,
        description: String,
        tags: String,
        points: List<TrackPoint>
    ): Boolean = withContext(Dispatchers.IO) {
        val dtos = points.map { TrackPointDto(it.point.latitude, it.point.longitude, it.altitudeM) }
        val pointsJson = gson.toJson(dtos)
        postJson(
            mapOf(
                "action" to "addLayer",
                "id" to id,
                "name" to name,
                "type" to "track",
                "subtype" to "gpx",
                "author" to author,
                "description" to description,
                "tags" to tags,
                "pointsJson" to pointsJson
            )
        )
    }

    suspend fun deleteLayer(id: String): Boolean = withContext(Dispatchers.IO) {
        postForm(mapOf("action" to "deleteLayer", "id" to id))
    }

    // --- CONVERT TO LOCAL MODELS ---

    fun toImportedLayer(entry: SharedLayerEntry): ImportedLayer? {
        return when (entry.type) {
            "point" -> {
                val lat = entry.lat
                val lon = entry.lon
                if (lat == null || lon == null) {
                    null
                } else {
                    val conv = CoordinateConverter.wgs84ToHtrs96Tm(lat, lon)
                    val point = MarkedPoint(
                        id = "shared_${entry.id}",
                        name = entry.name,
                        type = "shared",
                        description = buildString {
                            if (entry.description.isNotBlank()) appendLine(entry.description)
                            val altitude = entry.alt
                            if (altitude != null && altitude != 0.0)
                                appendLine("Nadmorska visina: ${String.format(java.util.Locale.US, "%.1f", altitude)} m")
                            append("Autor: ${entry.author}")
                        }.trim(),
                        lat = lat,
                        lon = lon,
                        htrsX = conv.x,
                        htrsY = conv.y
                    )
                    ImportedLayer(
                        id = "shared_layer_${entry.id}",
                        name = entry.name,
                        type = "Zajednički sloj",
                        visible = true,
                        points = listOf(point),
                        tracks = emptyList()
                    )
                }
            }
            "track" -> {
                if (entry.pointsJson.isBlank()) {
                    null
                } else {
                    val dtoType = object : TypeToken<List<TrackPointDto>>() {}.type
                    val dtos: List<TrackPointDto> = runCatching {
                        @Suppress("UNCHECKED_CAST")
                        gson.fromJson(entry.pointsJson, dtoType) as List<TrackPointDto>
                    }.getOrDefault(emptyList())
                    if (dtos.isEmpty()) {
                        null
                    } else {
                        val trackPoints = dtos.map { dto ->
                            TrackPoint(
                                point = GeoPoint(dto.lat, dto.lon),
                                altitudeM = dto.alt
                            )
                        }
                        val track = SavedTrack(
                            id = "shared_track_${entry.id}",
                            name = entry.name,
                            description = buildString {
                                if (entry.description.isNotBlank()) appendLine(entry.description)
                                append("Autor: ${entry.author}")
                            }.trim(),
                            points = trackPoints,
                            visible = true
                        )
                        ImportedLayer(
                            id = "shared_layer_${entry.id}",
                            name = entry.name,
                            type = "Zajednički sloj",
                            visible = true,
                            points = emptyList(),
                            tracks = listOf(track)
                        )
                    }
                }
            }
            else -> null
        }
    }

    // --- HTTP UTILS ---

    private fun postJson(fields: Map<String, String>): Boolean {
        return try {
            val body = fields.entries.joinToString("&") { (k, v) ->
                URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
            }
            val conn = (URL(WEBAPP_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 9000
                readTimeout = 9000
                doOutput = true
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val responseBody = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                ""
            }
            conn.disconnect()
            if (responseBody.isBlank()) {
                code in 200..299
            } else {
                runCatching { gson.fromJson(responseBody, SharedLayersActionResponse::class.java).ok }.getOrDefault(code in 200..299)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun postForm(fields: Map<String, String>) = postJson(fields)
}
