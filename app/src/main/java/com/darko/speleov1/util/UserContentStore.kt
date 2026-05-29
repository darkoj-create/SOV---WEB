package com.darko.speleov1.util

import android.content.Context
import com.darko.speleov1.ImportedLayer
import com.darko.speleov1.MarkedPoint
import com.darko.speleov1.SavedTrack
import com.darko.speleov1.TrackPoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.util.GeoPoint

object UserContentStore {
    private const val PREFS = "user_content_store"
    private const val KEY_MARKED_POINTS = "marked_points"
    private const val KEY_SAVED_TRACKS = "saved_tracks"
    private const val KEY_IMPORTED_LAYERS = "imported_layers"
    private const val KEY_RECORD_NOTES = "record_notes"
    private const val KEY_FAVORITE_RECORDS = "favorite_records"
    private const val KEY_RECENT_RECORDS = "recent_records"
    private val gson = Gson()
    private const val MAX_PREFS_BYTES = 1_500_000 // ~1.5 MB safe limit per SharedPreferences key
    private const val MAX_TRACK_POINTS_PER_TRACK = 3000 // trim per-track before serializing

    private data class TrackPointDto(val lat: Double, val lon: Double, val altitudeM: Double?)
    private data class MarkedPointDto(
        val id: String,
        val name: String,
        val type: String,
        val description: String,
        val lat: Double,
        val lon: Double,
        val htrsX: Double,
        val htrsY: Double,
        val visible: Boolean? = null
    )
    private data class SavedTrackDto(
        val id: String,
        val name: String,
        val description: String,
        val createdAtMillis: Long,
        val points: List<TrackPointDto>,
        val visible: Boolean? = null
    )

    private data class ImportedLayerDto(
        val id: String,
        val name: String,
        val type: String,
        val visible: Boolean,
        val createdAtMillis: Long,
        val points: List<MarkedPointDto>,
        val tracks: List<SavedTrackDto>
    )

    fun loadMarkedPoints(context: Context): List<MarkedPoint> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MARKED_POINTS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<MarkedPointDto>>() {}.type
            gson.fromJson<List<MarkedPointDto>>(raw, type).orEmpty().map { dto ->
                MarkedPoint(
                    id = dto.id,
                    name = dto.name,
                    type = dto.type,
                    description = dto.description,
                    lat = dto.lat,
                    lon = dto.lon,
                    htrsX = dto.htrsX,
                    htrsY = dto.htrsY,
                    visible = dto.visible ?: true
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveMarkedPoints(context: Context, points: List<MarkedPoint>) {
        val dto = points.map { point ->
            MarkedPointDto(
                id = point.id,
                name = point.name,
                type = point.type,
                description = point.description,
                lat = point.lat,
                lon = point.lon,
                htrsX = point.htrsX,
                htrsY = point.htrsY,
                visible = point.visible
            )
        }
        val json = gson.toJson(dto)
        if (json.toByteArray(Charsets.UTF_8).size > MAX_PREFS_BYTES) {
            android.util.Log.w("UserContentStore", "saveMarkedPoints: payload too large (${json.length} chars), skipping write to protect existing data")
            return
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MARKED_POINTS, json).apply()
    }

    fun loadSavedTracks(context: Context): List<SavedTrack> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SAVED_TRACKS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<SavedTrackDto>>() {}.type
            gson.fromJson<List<SavedTrackDto>>(raw, type).orEmpty().map { dto ->
                SavedTrack(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    createdAtMillis = dto.createdAtMillis,
                    points = dto.points.map { TrackPoint(GeoPoint(it.lat, it.lon), it.altitudeM) },
                    visible = dto.visible ?: false
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveSavedTracks(context: Context, tracks: List<SavedTrack>) {
        val dto = tracks.map { track ->
            val trimmedPoints = if (track.points.size > MAX_TRACK_POINTS_PER_TRACK) {
                track.points.takeLast(MAX_TRACK_POINTS_PER_TRACK)
            } else {
                track.points
            }
            SavedTrackDto(
                id = track.id,
                name = track.name,
                description = track.description,
                createdAtMillis = track.createdAtMillis,
                points = trimmedPoints.map { TrackPointDto(it.point.latitude, it.point.longitude, it.altitudeM) },
                visible = track.visible
            )
        }
        val json = gson.toJson(dto)
        if (json.toByteArray(Charsets.UTF_8).size > MAX_PREFS_BYTES) {
            android.util.Log.w("UserContentStore", "saveSavedTracks: payload too large (${json.length} chars), skipping write to protect existing data")
            return
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SAVED_TRACKS, json).apply()
    }

    fun loadImportedLayers(context: Context): List<ImportedLayer> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_IMPORTED_LAYERS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<ImportedLayerDto>>() {}.type
            gson.fromJson<List<ImportedLayerDto>>(raw, type).orEmpty().map { dto ->
                ImportedLayer(
                    id = dto.id,
                    name = dto.name,
                    type = dto.type,
                    visible = dto.visible,
                    createdAtMillis = dto.createdAtMillis,
                    points = dto.points.map { point ->
                        MarkedPoint(
                            id = point.id,
                            name = point.name,
                            type = point.type,
                            description = point.description,
                            lat = point.lat,
                            lon = point.lon,
                            htrsX = point.htrsX,
                            htrsY = point.htrsY,
                            visible = point.visible ?: true
                        )
                    },
                    tracks = dto.tracks.map { track ->
                        SavedTrack(
                            id = track.id,
                            name = track.name,
                            description = track.description,
                            createdAtMillis = track.createdAtMillis,
                            points = track.points.map { TrackPoint(GeoPoint(it.lat, it.lon), it.altitudeM) },
                            visible = track.visible ?: true
                        )
                    }
                )
            }
        }.getOrDefault(emptyList())
    }


    fun loadRecordNote(context: Context, recordId: String): String {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_RECORD_NOTES, null) ?: return ""
        return runCatching {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(raw, type).orEmpty()[recordId].orEmpty()
        }.getOrDefault("")
    }

    fun saveRecordNote(context: Context, recordId: String, note: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECORD_NOTES, null)
        val current = runCatching {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(raw, type).orEmpty().toMutableMap()
        }.getOrDefault(mutableMapOf())
        if (note.isBlank()) current.remove(recordId) else current[recordId] = note.trim()
        prefs.edit().putString(KEY_RECORD_NOTES, gson.toJson(current)).apply()
    }


    fun loadFavoriteRecordIds(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY_FAVORITE_RECORDS, emptySet()).orEmpty()

    fun isFavoriteRecord(context: Context, recordId: String): Boolean =
        loadFavoriteRecordIds(context).contains(recordId)

    fun toggleFavoriteRecord(context: Context, recordId: String): Boolean {
        val next = loadFavoriteRecordIds(context).toMutableSet()
        val nowFavorite = if (next.contains(recordId)) {
            next.remove(recordId)
            false
        } else {
            next.add(recordId)
            true
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(KEY_FAVORITE_RECORDS, next).apply()
        return nowFavorite
    }

    fun loadRecentRecordIds(context: Context, limit: Int = 20): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_RECENT_RECORDS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(raw, type).orEmpty().take(limit)
        }.getOrDefault(emptyList())
    }

    fun pushRecentRecord(context: Context, recordId: String, limit: Int = 20) {
        val next = loadRecentRecordIds(context, limit = limit).toMutableList()
        next.remove(recordId)
        next.add(0, recordId)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_RECENT_RECORDS, gson.toJson(next.take(limit))).apply()
    }

    fun saveImportedLayers(context: Context, layers: List<ImportedLayer>) {
        val dto = layers.map { layer ->
            ImportedLayerDto(
                id = layer.id,
                name = layer.name,
                type = layer.type,
                visible = layer.visible,
                createdAtMillis = layer.createdAtMillis,
                points = layer.points.map { point ->
                    MarkedPointDto(
                        id = point.id,
                        name = point.name,
                        type = point.type,
                        description = point.description,
                        lat = point.lat,
                        lon = point.lon,
                        htrsX = point.htrsX,
                        htrsY = point.htrsY,
                        visible = point.visible
                    )
                },
                tracks = layer.tracks.map { track ->
                    SavedTrackDto(
                        id = track.id,
                        name = track.name,
                        description = track.description,
                        createdAtMillis = track.createdAtMillis,
                        points = track.points.map { TrackPointDto(it.point.latitude, it.point.longitude, it.altitudeM) },
                        visible = track.visible
                    )
                }
            )
        }
        val json = gson.toJson(dto)
        if (json.toByteArray(Charsets.UTF_8).size > MAX_PREFS_BYTES) {
            android.util.Log.w("UserContentStore", "saveImportedLayers: payload too large (${json.length} chars), skipping write to protect existing data")
            return
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_IMPORTED_LAYERS, json).apply()
    }
}
