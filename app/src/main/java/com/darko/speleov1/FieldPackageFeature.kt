@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.darko.speleov1

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.darko.speleov1.model.SpeleoRecord
import com.darko.speleov1.util.ImportParser
import com.darko.speleov1.util.OfflineTileManager
import com.darko.speleov1.util.UserContentStore
import com.darko.speleov1.util.TopoDroidBridgeStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// v1.7: Field Packages / Izleti
private const val SOV_TRIPS_SHEET_URL = "https://docs.google.com/spreadsheets/d/1g93ZqKOJD2gLcIxZPfHokNcktbDEPivEItp7VRpnfWc/edit?usp=sharing"
private const val RASPORED_URL_PREFS = "raspored_urls"

internal fun spremRasporedUrl(context: Context, packageId: String, url: String) {
    context.getSharedPreferences(RASPORED_URL_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(packageId, url)
        .apply()
}

internal fun dohvatiRasporedUrl(context: Context, packageId: String): String? {
    return context.getSharedPreferences(RASPORED_URL_PREFS, Context.MODE_PRIVATE)
        .getString(packageId, null)
}

internal fun fieldPackageSharedTripKey(date: String, location: String): String =
    listOf(date.trim(), location.trim()).joinToString("_")


// Lightweight trip-planning and share package layer. This deliberately avoids a map-engine rewrite:
// it packages existing app data plus the active offline map folder/MBTiles into a portable .sovpkg ZIP.

data class FieldPackageSummary(
    val id: String,
    val name: String,
    val tripDateText: String? = null,
    val tripStartMillis: Long? = null,
    val tripEndMillis: Long? = null,
    val organizer: String = "",
    val locationName: String = "",
    val goal: String = "Izletiranje",
    val description: String,
    val createdAtMillis: Long,
    val radiusKm: Double,
    val centerLat: Double?,
    val centerLon: Double?,
    val objectCount: Int,
    val pointCount: Int,
    val trackCount: Int,
    val topoDroidAttachmentCount: Int = 0,
    val offlineMapName: String?,
    val includesOfflineMap: Boolean,
    val imported: Boolean = false,
    val minLat: Double? = null,
    val maxLat: Double? = null,
    val minLon: Double? = null,
    val maxLon: Double? = null,
    val includeTracks: Boolean? = true,
    val selectedTrackIds: List<String>? = null,
    val sheetSynced: Boolean = false,
    val rasporedUrl: String? = null,
    val weatherCity: String? = null
)

private data class FieldPackageManifest(
    val schema: String = "sov.field_package.v1",
    val appVersion: String = "1.7.56",
    val id: String,
    val name: String,
    val tripDateText: String? = null,
    val tripStartMillis: Long? = null,
    val tripEndMillis: Long? = null,
    val organizer: String = "",
    val locationName: String = "",
    val goal: String = "Izletiranje",
    val description: String,
    val createdAtMillis: Long,
    val radiusKm: Double,
    val centerLat: Double?,
    val centerLon: Double?,
    val offlineMapName: String?,
    val includesOfflineMap: Boolean,
    val minLat: Double? = null,
    val maxLat: Double? = null,
    val minLon: Double? = null,
    val maxLon: Double? = null,
    val objects: List<FieldPackageObject>,
    val points: List<FieldPackagePoint>,
    val tracks: List<FieldPackageTrack>,
    val topoDroidAttachments: List<FieldPackageTopoDroidAttachment>? = emptyList(),
    val includeTracks: Boolean? = true,
    val selectedTrackIds: List<String>? = null
)

private data class FieldPackageObject(
    val id: String,
    val name: String,
    val lat: Double?,
    val lon: Double?,
    val plate: String?,
    val cadastralNumber: String?,
    val municipality: String?,
    val objectType: String?
)

private data class FieldPackagePoint(
    val id: String,
    val name: String,
    val type: String,
    val description: String,
    val lat: Double,
    val lon: Double
)

// === WEATHER FORECAST ===

internal data class FieldWeatherDay(
    val date: String,
    val tempMin: Float,
    val tempMax: Float,
    val precipMm: Float,
    val windKmh: Float,
    val wmoCode: Int
)

internal data class FieldWeatherResult(
    val days: List<FieldWeatherDay>,
    val timezone: String = "",
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

internal fun FieldPackageSummary.weatherCoordinates(): Pair<Double, Double>? {
    if (centerLat != null && centerLon != null) {
        return Pair(centerLat, centerLon)
    }
    if (minLat != null && maxLat != null && minLon != null && maxLon != null) {
        return Pair((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0)
    }
    return null
}

internal fun FieldPackageSheetTrip.weatherCoordinates(): Pair<Double, Double>? {
    if (centerLat != null && centerLon != null) {
        return Pair(centerLat, centerLon)
    }
    if (minLat != null && maxLat != null && minLon != null && maxLon != null) {
        return Pair((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0)
    }
    return null
}

internal fun wmoCodeToLabel(code: Int): String = when (code) {
    0 -> "☀️ Vedro"
    1, 2 -> "🌤 Pretežno vedro"
    3 -> "☁️ Oblačno"
    45, 48 -> "🌫 Magla"
    51, 53, 55 -> "🌦 Rosulja"
    61, 63 -> "🌧 Kiša"
    65 -> "🌧 Jaka kiša"
    71, 73 -> "🌨 Snijeg"
    75, 77 -> "❄️ Jak snijeg"
    80, 81, 82 -> "⛈ Pljuskovi"
    85, 86 -> "🌨 Snježni pljuskovi"
    95 -> "⛈ Grmljavina"
    96, 99 -> "⛈ Grmljavina s tučom"
    else -> "🌡 Nepoznato"
}

internal fun wmoCodeToSpeleoWarning(code: Int, precipMm: Float): String? = when {
    precipMm >= 20f -> "⚠️ Visoke oborine — rizik od poplave špilje!"
    precipMm >= 8f -> "⚠️ Značajne oborine — pazi na vodotoke u špilji"
    code in listOf(95, 96, 99) -> "⚠️ Grmljavina — ne ulazi u metalnu opremu"
    code in listOf(71, 73, 75, 77, 85, 86) -> "⚠️ Snijeg — provjeri pristupni put"
    else -> null
}

private data class FieldPackageTrack(
    val id: String,
    val name: String,
    val description: String,
    val createdAtMillis: Long,
    val points: List<FieldPackageTrackPoint>
)

private data class FieldPackageTrackPoint(val lat: Double, val lon: Double, val altitudeM: Double?)

private data class FieldPackageTopoDroidAttachment(
    val objectId: String,
    val objectName: String,
    val plateNumber: String?,
    val cadastralNumber: String?,
    val filename: String,
    val fileType: String,
    val source: String,
    val archivePath: String,
    val surveyName: String? = null,
    val surveyDate: String? = null,
    val surveyTeam: String? = null,
    val shotCount: Int? = null,
    val centerlineShotCount: Int? = null,
    val splayShotCount: Int? = null,
    val stationCount: Int? = null,
    val totalLengthM: Double? = null,
    val verticalRangeM: Double? = null,
    val qcWarnings: List<String>? = emptyList()
)

private data class FieldPackageDraft(
    val name: String = "",
    val tripDateText: String = "",
    val tripStartMillis: Long? = null,
    val tripEndMillis: Long? = null,
    val organizer: String = "",
    val locationName: String = "",
    val weatherCity: String = "",
    val goal: String = "Izletiranje",
    val description: String = "",
    val includeOfflineMap: Boolean = true,
    val includeTracks: Boolean = true,
    val manualTrackSelection: Boolean = false,
    val selectedTrackIds: List<String> = emptyList()
)

private object FieldPackageDraftStore {
    private const val PREFS = "field_package_draft_v1"
    private const val KEY_DRAFT = "draft"
    private val gson = Gson()

    fun save(context: Context, draft: FieldPackageDraft) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DRAFT, gson.toJson(draft)).apply()
    }

    fun load(context: Context): FieldPackageDraft? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DRAFT, null) ?: return null
        return runCatching { gson.fromJson(raw, FieldPackageDraft::class.java) }.getOrNull()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_DRAFT).apply()
    }
}

object FieldPackageManager {
    private const val PREFS = "field_packages_v1"
    private const val KEY_PACKAGES = "packages"
    private val gson = Gson()

    fun list(context: Context): List<FieldPackageSummary> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PACKAGES, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<FieldPackageSummary>>() {}.type
            gson.fromJson<List<FieldPackageSummary>>(raw, type).orEmpty().sortedByDescending { it.createdAtMillis }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, packages: List<FieldPackageSummary>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PACKAGES, gson.toJson(packages)).apply()
    }

    fun delete(context: Context, id: String) {
        save(context, list(context).filterNot { it.id == id })
    }

    fun createSummary(
        context: Context,
        name: String,
        tripDateText: String = "",
        tripStartMillis: Long? = null,
        tripEndMillis: Long? = null,
        organizer: String = "",
        locationName: String = "",
        weatherCity: String = "",
        goal: String = "Izletiranje",
        description: String,
        radiusKm: Double,
        center: GeoPoint?,
        records: List<SpeleoRecord>,
        markedPoints: List<MarkedPoint>,
        savedTracks: List<SavedTrack>,
        includeOfflineMap: Boolean,
        includeTracks: Boolean = true,
        selectedTrackIds: Set<String> = emptySet()
    ): FieldPackageSummary {
        val offlineMap = OfflineTileManager.getActiveMapName(context)
        // Coordinates/bounds are metadata for the trip itself, not only for packaging the offline map.
        // If the user picked/downloaded an area, keep its center + bbox even when the offline-map
        // attachment checkbox is later disabled or the shared Sheet only needs weather/map focus data.
        val offlineBounds = offlineMap?.let { OfflineTileManager.getOfflineBounds(context, it) }
        val bboxCenter = offlineBounds?.let { bounds ->
            GeoPoint((bounds.minLat + bounds.maxLat) / 2.0, (bounds.minLon + bounds.maxLon) / 2.0)
        }
        val initialCenter = center ?: bboxCenter
        val inferredRadiusKm = if (center == null && offlineBounds != null) {
            val centerPoint = initialCenter
            val cornerDistance = if (centerPoint != null) {
                distanceKm(centerPoint.latitude, centerPoint.longitude, offlineBounds.maxLat, offlineBounds.maxLon)
            } else radiusKm
            maxOf(radiusKm, cornerDistance.coerceAtLeast(0.5))
        } else {
            radiusKm
        }
        val selectedObjects = selectRecords(records.filter { isSovRecord(it) }, offlineBounds, initialCenter, inferredRadiusKm)
        val weatherCenter = calculateFieldWeatherCenter(selectedObjects, offlineBounds, initialCenter)
        val inferredCenter = if (offlineBounds != null) {
            // Shared-trip weather should describe the downloaded field area: average cave/object
            // coordinates inside the offline bbox, or bbox center when there are no objects.
            weatherCenter ?: initialCenter
        } else {
            center ?: weatherCenter ?: initialCenter
        }
        val selectedPoints = selectMarkedPoints(markedPoints, offlineBounds, inferredCenter, inferredRadiusKm)
        val selectedTracks = selectTracks(savedTracks, offlineBounds, inferredCenter, inferredRadiusKm, includeTracks, selectedTrackIds)
        val selectedObjectIds = selectedObjects.map { it.id }.toSet()
        val topoAttachmentCount = TopoDroidBridgeStore.loadAll(context).count { it.objectId in selectedObjectIds }
        return FieldPackageSummary(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { buildFieldPackageTitle(locationName, goal) },
            tripDateText = tripDateText.trim(),
            tripStartMillis = tripStartMillis,
            tripEndMillis = tripEndMillis,
            organizer = organizer.trim(),
            locationName = locationName.trim(),
            goal = goal.trim().ifBlank { "Izletiranje" },
            weatherCity = weatherCity.trim(),
            description = description,
            createdAtMillis = System.currentTimeMillis(),
            radiusKm = inferredRadiusKm,
            centerLat = inferredCenter?.latitude,
            centerLon = inferredCenter?.longitude,
            objectCount = selectedObjects.size,
            pointCount = selectedPoints.size,
            trackCount = selectedTracks.size,
            topoDroidAttachmentCount = topoAttachmentCount,
            offlineMapName = offlineMap,
            includesOfflineMap = includeOfflineMap && offlineMap != null,
            minLat = offlineBounds?.minLat,
            maxLat = offlineBounds?.maxLat,
            minLon = offlineBounds?.minLon,
            maxLon = offlineBounds?.maxLon,
            includeTracks = includeTracks,
            selectedTrackIds = selectedTrackIds.takeIf { it.isNotEmpty() }?.toList()
        )
    }

    suspend fun exportPackage(
        context: Context,
        summary: FieldPackageSummary,
        records: List<SpeleoRecord>,
        markedPoints: List<MarkedPoint>,
        savedTracks: List<SavedTrack>
    ): File = withContext(Dispatchers.IO) {
        val center = summary.centerLat?.let { lat -> summary.centerLon?.let { lon -> GeoPoint(lat, lon) } }
        val packageBounds = summary.toOfflineBounds()
        val selectedObjects = selectRecords(records.filter { isSovRecord(it) }, packageBounds, center, summary.radiusKm)
        val selectedPoints = selectMarkedPoints(markedPoints, packageBounds, center, summary.radiusKm)
        val selectedTracks = selectTracks(savedTracks, packageBounds, center, summary.radiusKm, summary.includeTracks != false, summary.selectedTrackIds.orEmpty().toSet())
        val selectedObjectIds = selectedObjects.map { it.id }.toSet()
        val selectedTopoDroidAttachments = TopoDroidBridgeStore.loadAll(context).filter { it.objectId in selectedObjectIds }
        val manifest = FieldPackageManifest(
            id = summary.id,
            name = summary.name,
            tripDateText = summary.tripDateText,
            tripStartMillis = summary.tripStartMillis,
            tripEndMillis = summary.tripEndMillis,
            organizer = summary.organizer,
            locationName = summary.locationName,
            goal = summary.goal,
            description = summary.description,
            createdAtMillis = summary.createdAtMillis,
            radiusKm = summary.radiusKm,
            centerLat = summary.centerLat,
            centerLon = summary.centerLon,
            offlineMapName = summary.offlineMapName,
            includesOfflineMap = summary.includesOfflineMap,
            minLat = summary.minLat,
            maxLat = summary.maxLat,
            minLon = summary.minLon,
            maxLon = summary.maxLon,
            includeTracks = summary.includeTracks,
            selectedTrackIds = summary.selectedTrackIds,
            objects = selectedObjects.map {
                FieldPackageObject(
                    id = it.id,
                    name = it.name,
                    lat = it.location.lat,
                    lon = it.location.lon,
                    plate = it.condition.plate_number,
                    cadastralNumber = it.cadastre.cadastral_number,
                    municipality = it.location.municipality,
                    objectType = it.classification.object_type
                )
            },
            points = selectedPoints.map { FieldPackagePoint(it.id, it.name, it.type, it.description, it.lat, it.lon) },
            tracks = selectedTracks.map { track ->
                FieldPackageTrack(
                    id = track.id,
                    name = track.name,
                    description = track.description,
                    createdAtMillis = track.createdAtMillis,
                    points = track.points.map { FieldPackageTrackPoint(it.point.latitude, it.point.longitude, it.altitudeM) }
                )
            },
            topoDroidAttachments = selectedTopoDroidAttachments.mapIndexed { index, attachment ->
                val archivePath = "topodroid/${sanitizeFileName(attachment.objectId)}/${index + 1}_${sanitizeFileName(attachment.originalFilename)}"
                FieldPackageTopoDroidAttachment(
                    objectId = attachment.objectId,
                    objectName = attachment.objectName,
                    plateNumber = attachment.plateNumber,
                    cadastralNumber = attachment.cadastralNumber,
                    filename = attachment.originalFilename,
                    fileType = attachment.fileType,
                    source = attachment.source,
                    archivePath = archivePath,
                    surveyName = attachment.surveyName,
                    surveyDate = attachment.surveyDate,
                    surveyTeam = attachment.surveyTeam,
                    shotCount = attachment.shotCount,
                    centerlineShotCount = attachment.centerlineShotCount,
                    splayShotCount = attachment.splayShotCount,
                    stationCount = attachment.stationCount,
                    totalLengthM = attachment.totalLengthM,
                    verticalRangeM = attachment.verticalRangeM,
                    qcWarnings = attachment.qcWarnings
                )
            }
        )
        val outDir = File(context.cacheDir, "field_packages").apply { mkdirs() }
        val outFile = File(outDir, sanitizeFileName(summary.name) + ".sovpkg")
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(gson.toJson(manifest).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("README.txt"))
            zip.write(buildReadme(summary).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            if (summary.includesOfflineMap && summary.offlineMapName != null) {
                val mapRoot = OfflineTileManager.tileRootForName(context, summary.offlineMapName)
                if (mapRoot.exists()) addDirectoryToZip(zip, mapRoot, "maps/${sanitizeFileName(summary.offlineMapName)}/")
            }
            selectedTopoDroidAttachments.forEachIndexed { index, attachment ->
                val entryPath = manifest.topoDroidAttachments.orEmpty().getOrNull(index)?.archivePath ?: "topodroid/${sanitizeFileName(attachment.objectId)}/${index + 1}_${sanitizeFileName(attachment.originalFilename)}"
                runCatching {
                    TopoDroidBridgeStore.openInputStream(context, attachment)?.use { input ->
                        zip.putNextEntry(ZipEntry(entryPath))
                        input.copyTo(zip)
                        zip.closeEntry()
                    }
                }
                val measurementsCsv = TopoDroidBridgeStore.buildMeasurementsCsv(context, attachment)
                if (measurementsCsv.isNotBlank()) {
                    zip.putNextEntry(ZipEntry(entryPath.substringBeforeLast('.') + "_measurements.csv"))
                    zip.write(measurementsCsv.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        }
        outFile
    }

    suspend fun importPackage(context: Context, uri: Uri): FieldPackageSummary? = withContext(Dispatchers.IO) {
        val importRoot = File(context.filesDir, "imported_field_packages").apply { mkdirs() }
        var manifest: FieldPackageManifest? = null
        val tempDir = File(importRoot, "import_${System.currentTimeMillis()}").apply { mkdirs() }
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val target = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { out -> zip.copyTo(out) }
                        if (entry.name == "manifest.json") {
                            manifest = runCatching { gson.fromJson(target.readText(), FieldPackageManifest::class.java) }.getOrNull()
                        }
                    }
                    zip.closeEntry()
                }
            }
        }
        val m = manifest ?: return@withContext null
        val mapsDir = File(tempDir, "maps")
        var installedMapName: String? = null
        if (mapsDir.exists()) {
            mapsDir.listFiles()?.firstOrNull { it.isDirectory }?.let { mapDir ->
                val mapName = OfflineTileManager.sanitizeMapName("Izlet - ${m.name}")
                val destination = File(context.filesDir, "custom_maps/$mapName")
                val incomingFileCount = mapDir.walkTopDown().count { it.isFile }
                val existingFileCount = if (destination.exists()) destination.walkTopDown().count { it.isFile } else -1
                if (existingFileCount == incomingFileCount && existingFileCount >= 0) {
                    // Map appears identical — skip overwrite to preserve existing tiles
                    installedMapName = mapName
                } else {
                    if (destination.exists()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Karta '$mapName' bit će zamijenjena novom verzijom iz paketa.", Toast.LENGTH_LONG).show()
                        }
                        destination.deleteRecursively()
                    }
                    copyDirectory(mapDir, destination)
                    installedMapName = mapName
                }
            }
        }
        importTopoDroidAttachmentsFromPackage(context, m, tempDir)
        val importedPoints = m.points.map { p ->
            MarkedPoint(
                id = "pkg_${m.id}_${p.id}",
                name = p.name,
                type = p.type.ifBlank { "izlet" },
                description = "${p.description}\nIz terenskog paketa: ${m.name}".trim(),
                lat = p.lat,
                lon = p.lon,
                htrsX = 0.0,
                htrsY = 0.0,
                visible = true
            )
        }
        if (importedPoints.isNotEmpty()) {
            val current = UserContentStore.loadMarkedPoints(context)
            UserContentStore.saveMarkedPoints(context, current + importedPoints)
        }
        val importedTracks = m.tracks.map { t ->
            SavedTrack(
                id = "pkg_${m.id}_${t.id}",
                name = t.name,
                description = "${t.description}\nIz terenskog paketa: ${m.name}".trim(),
                createdAtMillis = t.createdAtMillis,
                points = t.points.map { TrackPoint(GeoPoint(it.lat, it.lon), it.altitudeM) },
                visible = true
            )
        }
        if (importedTracks.isNotEmpty()) {
            val current = UserContentStore.loadSavedTracks(context)
            UserContentStore.saveSavedTracks(context, current + importedTracks)
        }
        FieldPackageSummary(
            id = m.id,
            name = m.name,
            tripDateText = m.tripDateText.orEmpty(),
            tripStartMillis = m.tripStartMillis,
            tripEndMillis = m.tripEndMillis,
            organizer = m.organizer.orEmpty(),
            locationName = m.locationName.orEmpty(),
            goal = m.goal.orEmpty().ifBlank { "Izletiranje" },
            description = m.description,
            createdAtMillis = m.createdAtMillis,
            radiusKm = m.radiusKm,
            centerLat = m.centerLat,
            centerLon = m.centerLon,
            objectCount = m.objects.size,
            pointCount = m.points.size,
            trackCount = m.tracks.size,
            topoDroidAttachmentCount = m.topoDroidAttachments.orEmpty().size,
            offlineMapName = installedMapName ?: m.offlineMapName,
            includesOfflineMap = installedMapName != null || m.includesOfflineMap,
            imported = true,
            minLat = m.minLat,
            maxLat = m.maxLat,
            minLon = m.minLon,
            maxLon = m.maxLon,
            includeTracks = m.includeTracks,
            selectedTrackIds = m.selectedTrackIds
        )
    }


    private fun calculateFieldWeatherCenter(
        selectedObjects: List<SpeleoRecord>,
        offlineBounds: OfflineTileManager.OfflineBounds?,
        fallbackCenter: GeoPoint?
    ): GeoPoint? {
        val objectCoordinates = selectedObjects.mapNotNull { record ->
            val lat = record.location.lat
            val lon = record.location.lon
            if (lat != null && lon != null) lat to lon else null
        }
        if (objectCoordinates.isNotEmpty()) {
            return GeoPoint(
                objectCoordinates.map { it.first }.average(),
                objectCoordinates.map { it.second }.average()
            )
        }
        return offlineBounds?.let { bounds ->
            GeoPoint((bounds.minLat + bounds.maxLat) / 2.0, (bounds.minLon + bounds.maxLon) / 2.0)
        } ?: fallbackCenter
    }

    private fun importTopoDroidAttachmentsFromPackage(context: Context, manifest: FieldPackageManifest, tempDir: File) {
        if (manifest.topoDroidAttachments.orEmpty().isEmpty()) return
        val importRoot = File(context.filesDir, "imported_topodroid_attachments/${sanitizeFileName(manifest.id)}").apply { mkdirs() }
        manifest.topoDroidAttachments.orEmpty().forEach { meta ->
            val sourceFile = File(tempDir, meta.archivePath)
            if (!sourceFile.exists() || !sourceFile.isFile) return@forEach
            val objectDir = File(importRoot, sanitizeFileName(meta.objectId)).apply { mkdirs() }
            val target = File(objectDir, sanitizeFileName(meta.filename))
            runCatching { sourceFile.copyTo(target, overwrite = true) }
            if (target.exists()) {
                TopoDroidBridgeStore.attachImportedFile(
                    context = context,
                    objectId = meta.objectId,
                    objectName = meta.objectName,
                    plateNumber = meta.plateNumber,
                    cadastralNumber = meta.cadastralNumber,
                    file = target,
                    originalFilename = meta.filename,
                    source = "SOV package: ${manifest.name}"
                )
            }
        }
    }

    private fun buildReadme(summary: FieldPackageSummary): String = buildString {
        val goalText = summary.goal.orEmpty().ifBlank { "Izletiranje" }
        appendLine("SOV terenski paket / Field Package")
        appendLine("Naziv: ${summary.name}")
        if (summary.tripDateText.orEmpty().isNotBlank()) appendLine("Datum izleta: ${summary.tripDateText.orEmpty()}")
        if (summary.organizer.orEmpty().isNotBlank()) appendLine("Voditelj: ${summary.organizer.orEmpty()}")
        if (summary.locationName.orEmpty().isNotBlank()) appendLine("Lokacija: ${summary.locationName.orEmpty()}")
        appendLine("Cilj: $goalText")
        appendLine("Opis: ${summary.description}")
        appendLine("Objekti: ${summary.objectCount}")
        appendLine("Custom točke: ${summary.pointCount}")
        appendLine("Trackovi: ${summary.trackCount}")
        appendLine("TopoDroid/nacrti: ${summary.topoDroidAttachmentCount}")
        appendLine("Offline karta: ${summary.offlineMapName ?: "nije uključena"}")
        appendLine()
        appendLine("Otvori .sovpkg datoteku u SOV appu za uvoz paketa.")
    }

    private fun sanitizeFileName(raw: String): String = raw.trim()
        .replace(Regex("[\\/:*?\"<>|]"), "_")
        .replace(Regex("\\s+"), "_")
        .ifBlank { "SOV_FieldPackage" }

    private fun selectRecords(records: List<SpeleoRecord>, bounds: OfflineTileManager.OfflineBounds?, center: GeoPoint?, radiusKm: Double): List<SpeleoRecord> {
        return when {
            bounds != null -> records.filter { r ->
                val lat = r.location.lat
                val lon = r.location.lon
                lat != null && lon != null && isInsideBounds(lat, lon, bounds)
            }
            center != null && radiusKm > 0.0 -> records.filter { r ->
                val lat = r.location.lat
                val lon = r.location.lon
                lat != null && lon != null && distanceKm(center.latitude, center.longitude, lat, lon) <= radiusKm
            }
            else -> emptyList()
        }
    }

    private fun selectMarkedPoints(points: List<MarkedPoint>, bounds: OfflineTileManager.OfflineBounds?, center: GeoPoint?, radiusKm: Double): List<MarkedPoint> {
        return when {
            bounds != null -> points.filter { isInsideBounds(it.lat, it.lon, bounds) }
            center != null && radiusKm > 0.0 -> points.filter { distanceKm(center.latitude, center.longitude, it.lat, it.lon) <= radiusKm }
            else -> emptyList()
        }
    }

    private fun selectTracks(
        tracks: List<SavedTrack>,
        bounds: OfflineTileManager.OfflineBounds?,
        center: GeoPoint?,
        radiusKm: Double,
        includeTracks: Boolean = true,
        selectedTrackIds: Set<String> = emptySet()
    ): List<SavedTrack> {
        if (!includeTracks) return emptyList()
        if (selectedTrackIds.isNotEmpty()) return tracks.filter { it.id in selectedTrackIds }
        return when {
            bounds != null -> tracks.filter { track -> track.points.any { isInsideBounds(it.point.latitude, it.point.longitude, bounds) } }
            center != null && radiusKm > 0.0 -> tracks.filter { track ->
                track.points.any { distanceKm(center.latitude, center.longitude, it.point.latitude, it.point.longitude) <= radiusKm }
            }
            else -> emptyList()
        }
    }

    private fun isSovRecord(record: SpeleoRecord): Boolean {
        val source = record.source?.trim()?.lowercase(Locale.ROOT)
        return source == "sov" || source == "both" || record.source_labels.orEmpty().any { it.trim().lowercase(Locale.ROOT) == "sov" }
    }

    private fun isInsideBounds(lat: Double, lon: Double, bounds: OfflineTileManager.OfflineBounds): Boolean =
        lat >= bounds.minLat && lat <= bounds.maxLat && lon >= bounds.minLon && lon <= bounds.maxLon

    private fun FieldPackageSummary.toOfflineBounds(): OfflineTileManager.OfflineBounds? {
        val minLatValue = minLat
        val maxLatValue = maxLat
        val minLonValue = minLon
        val maxLonValue = maxLon
        return if (minLatValue != null && maxLatValue != null && minLonValue != null && maxLonValue != null) {
            OfflineTileManager.OfflineBounds(minLatValue, maxLatValue, minLonValue, maxLonValue)
        } else null
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun addDirectoryToZip(zip: ZipOutputStream, dir: File, prefix: String) {
        if (!dir.exists()) return
        dir.walkTopDown().filter { it.isFile }.forEach { file ->
            val rel = file.relativeTo(dir).invariantSeparatorsPath
            zip.putNextEntry(ZipEntry(prefix + rel))
            file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }

    private fun copyDirectory(from: File, to: File) {
        if (!from.exists()) return
        from.walkTopDown().forEach { source ->
            val target = File(to, source.relativeTo(from).path)
            if (source.isDirectory) target.mkdirs() else {
                target.parentFile?.mkdirs()
                source.copyTo(target, overwrite = true)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FieldPackagesScreen(
    records: List<SpeleoRecord>,
    markedPoints: List<MarkedPoint>,
    savedTracks: List<SavedTrack>,
    importedLayers: List<ImportedLayer>,
    currentUserLocation: GeoPoint?,
    resumeCreateWizardNonce: Int = 0,
    onResumeCreateWizardConsumed: () -> Unit = {},
    onOpenPackageMap: (FieldPackageSummary) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onFindAreaOnMap: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    AppContextHolder.context = context
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var packages by remember { mutableStateOf(FieldPackageManager.list(context)) }
    var showCreate by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf<FieldPackageSummary?>(null) }
    var createInitialStep by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var sheetTrips by remember { mutableStateOf(FieldPackageSheetSyncClient.loadCachedTrips(context)) }
    var sheetLoading by remember { mutableStateOf(false) }
    var sheetStatusText by remember { mutableStateOf<String?>(null) }
    var signupTrip by remember { mutableStateOf<FieldPackageSheetTrip?>(null) }
    var transportTrip by remember { mutableStateOf<FieldPackageSheetTrip?>(null) }
    var showPastSheetTrips by rememberSaveable { mutableStateOf(false) }
    val weatherCache = remember { mutableStateMapOf<Int, FieldWeatherResult>() }
    fun refreshSheetTrips() {
        sheetLoading = true
        scope.launch {
            val result = FieldPackageSheetSyncClient.fetchTripsWithStatus(context)
            if (result.ok) {
                Toast.makeText(context, "Učitano ${result.trips.size} izleta iz SOV Clouda", Toast.LENGTH_SHORT).show()
                val currentLocal = FieldPackageManager.list(context)
                val reconciled = reconcileFieldPackagesWithSheet(context, currentLocal, result.trips)
                if (reconciled.packages != currentLocal) {
                    FieldPackageManager.save(context, reconciled.packages)
                    packages = reconciled.packages
                    onChanged()
                    if (reconciled.removedCount > 0) {
                        Toast.makeText(
                            context,
                            "${reconciled.removedCount} izlet${if (reconciled.removedCount == 1) "" else "a"} uklonjen iz lokalnog popisa jer više nije u zajedničkom rasporedu.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    packages = currentLocal
                }
                sheetTrips = mergeSheetTripsWithLocalPackages(result.trips, packages)
            } else if (sheetTrips.isEmpty()) {
                sheetTrips = mergeSheetTripsWithLocalPackages(FieldPackageSheetSyncClient.loadCachedTrips(context), packages)
            }
            sheetLoading = false
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val imported = FieldPackageManager.importPackage(context, uri)
                if (imported != null) {
                    val next = listOf(imported) + FieldPackageManager.list(context).filterNot { it.id == imported.id }
                    FieldPackageManager.save(context, next)
                    packages = next
                    onChanged()
                    Toast.makeText(context, "Uvezeno", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Paket nije prepoznat", Toast.LENGTH_SHORT).show()
                }
                busy = false
            }
        }
    }

    var lastAutoRefreshMillis by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        if (now - lastAutoRefreshMillis > 60_000L) {
            lastAutoRefreshMillis = now
            refreshSheetTrips()
        }
    }

    var lastHandledResumeCreateWizardNonce by remember { mutableStateOf(0) }
    LaunchedEffect(resumeCreateWizardNonce) {
        if (resumeCreateWizardNonce > 0 && resumeCreateWizardNonce != lastHandledResumeCreateWizardNonce) {
            lastHandledResumeCreateWizardNonce = resumeCreateWizardNonce
            createInitialStep = 2
            showCreate = true
            onResumeCreateWizardConsumed()
            Toast.makeText(context, "Karta spremna", Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(30.dp),
                    border = BorderStroke(1.dp, Color(0xFFC7A7FF).copy(alpha = 0.16f))
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FieldPackageIconBadge(Icons.Default.Event, Color(0xFFC7A7FF), Color(0xFF2D2340))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(language.pick("Izleti", "Trips"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(language.pick("Jednostavno upiši izlet u SOV Cloud. Bez karte, točaka i lokalnih paketa.", "Create a SOV Cloud trip. No map, points or local package wizard."), color = Color.White.copy(alpha = 0.72f))
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.055f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("1", color = Color(0xFF72E0C4), fontWeight = FontWeight.Bold)
                                    Text(language.pick("Podaci", "Data"), color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(language.pick("datum, lokacija", "date, location"), color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.055f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("2", color = Color(0xFFC7A7FF), fontWeight = FontWeight.Bold)
                                    Text(language.pick("Cloud", "Cloud"), color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(language.pick("Supabase", "Supabase"), color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.055f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("3", color = Color(0xFFFFC46B), fontWeight = FontWeight.Bold)
                                    Text(language.pick("Raspored", "Schedule"), color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(language.pick("web + APK", "web + app"), color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { createInitialStep = 0; showCreate = true },
                                enabled = !busy,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.size(8.dp))
                                Text("Novi izlet", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { refreshSheetTrips() },
                                enabled = !busy && !sheetLoading,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                            ) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White)
                                Spacer(Modifier.size(8.dp))
                                Text("Osvježi", color = Color.White)
                            }
                        }
                    }
                }
            }

            val now = System.currentTimeMillis()
            val visiblePackages = packages.filter { pkg ->
                val end = pkg.tripEndMillis ?: pkg.tripStartMillis
                end == null || end >= now - 24 * 60 * 60 * 1000L
            }

            if (visiblePackages.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FieldPackageIconBadge(Icons.Default.Event, Color(0xFFC7A7FF), Color(0xFF2D2340))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Moji izleti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Lokalno spremljeni terenski paketi", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                items(visiblePackages, key = { "local_" + it.id }) { pkg ->
                    FieldPackageCard(
                        pkg = pkg,
                        onOpenMap = { onOpenPackageMap(pkg) },
                        onEdit = { editingPackage = pkg },
                        onShare = {
                            busy = true
                            scope.launch {
                                val file = FieldPackageManager.exportPackage(context, pkg, records, markedPoints, savedTracks)
                                shareFieldPackage(context, file)
                                busy = false
                            }
                        },
                        onDelete = {
                            FieldPackageManager.delete(context, pkg.id)
                            packages = FieldPackageManager.list(context)
                            onChanged()
                        }
                    )
                }
            } else if (packages.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
                        Text(
                            "Nema lokalnih izleta. Kreiraj novi klikom na 'Novi izlet'.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FieldPackageIconBadge(Icons.Default.Share, Color(0xFF8EC5FF), Color(0xFF17334A))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(language.pick("SOV Cloud izleti", "SOV Cloud trips"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(language.pick("Izleti iz Supabasea — isti izvor za web i APK", "Trips from Supabase — same source for web and app"), color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOV_TRIPS_SHEET_URL))) }
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Otvori Cloud", tint = Color(0xFF8EC5FF))
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                lastAutoRefreshMillis = System.currentTimeMillis()
                                refreshSheetTrips()
                            }
                        },
                        enabled = !sheetLoading
                    ) {
                        if (sheetLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF8EC5FF))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Osvježi", tint = Color(0xFF8EC5FF))
                        }
                    }
                }
            }

            sheetStatusText?.takeIf { it.isNotBlank() }?.let { message ->
                item {
                    Text(
                        message,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD28A)
                    )
                }
            }

            val upcomingSheetTrips = sheetTrips
                .filterNot { sheetTripIsOver(it.date) }
                .sortedBy { parseSheetTripStartMillis(it.date) ?: Long.MAX_VALUE }
            val pastSheetTrips = sheetTrips
                .filter { sheetTripIsOver(it.date) }
                .sortedByDescending { parseSheetTripEndMillis(it.date) ?: 0L }

            if (upcomingSheetTrips.isEmpty()) {
                if (sheetLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            repeat(3) {
                                SheetTripSkeletonCard()
                            }
                        }
                    }
                } else {
                    item {
                        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    if (pastSheetTrips.isNotEmpty()) "Nema budućih izleta u rasporedu" else "Nema učitanih izleta iz rasporeda",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (pastSheetTrips.isNotEmpty()) "Prošli izleti su spremljeni dolje u sklopljenoj arhivi." else "Ovdje vidiš SOV Cloud raspored. Ako nema interneta, prikazujemo zadnji spremljeni cache.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            val visibleSheetTrips = upcomingSheetTrips

            items(visibleSheetTrips, key = { "sheet_" + it.rowNumber.toString() + "_" + it.date + "_" + it.location }) { trip ->
                val matchedLocal = packages.firstOrNull { local -> fieldPackageSheetKey(local) == fieldPackageSheetKey(trip) }
                val adminPackage = matchedLocal ?: trip.toAdminFieldPackageSummary()
                SheetTripCard(
                    trip = trip,
                    mine = true,
                    localPackage = adminPackage,
                    onEdit = { editingPackage = adminPackage },
                    onShare = {
                        busy = true
                        scope.launch {
                            val file = FieldPackageManager.exportPackage(context, adminPackage, records, markedPoints, savedTracks)
                            shareFieldPackage(context, file)
                            busy = false
                        }
                    },
                    onDeleteFromSheet = {
                        busy = true
                        scope.launch {
                            val deleted = FieldPackageSheetSyncClient.deleteTrip(trip)
                            if (deleted) {
                                Toast.makeText(context, "ADMIN: obrisano iz zajedničkog rasporeda", Toast.LENGTH_SHORT).show()
                                refreshSheetTrips()
                            } else {
                                Toast.makeText(context, "Brisanje nije uspjelo", Toast.LENGTH_SHORT).show()
                            }
                            busy = false
                        }
                    },
                    onSignup = { signupTrip = trip },
                    onTransport = { transportTrip = trip },
                    cachedWeather = weatherCache[trip.rowNumber],
                    onWeatherFetched = { result -> weatherCache[trip.rowNumber] = result }
                )
            }

            if (pastSheetTrips.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPastSheetTrips = !showPastSheetTrips },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A24)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FieldPackageIconBadge(Icons.Default.Event, Color.White.copy(alpha = 0.70f), Color(0xFF2A3140))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(language.pick("Prošli izleti (${pastSheetTrips.size})", "Past trips (${pastSheetTrips.size})"), fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.90f))
                                Text(
                                    if (showPastSheetTrips) "Sakrij arhivu prošlih izleta" else "Prikaži arhivu — bez vremenske prognoze",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.58f)
                                )
                            }
                            Icon(
                                if (showPastSheetTrips) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showPastSheetTrips) "Sakrij prošle izlete" else "Prikaži prošle izlete",
                                tint = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }
                }

                if (showPastSheetTrips) {
                    items(pastSheetTrips, key = { "past_sheet_" + it.rowNumber.toString() + "_" + it.date + "_" + it.location }) { trip ->
                        val matchedLocal = packages.firstOrNull { local -> fieldPackageSheetKey(local) == fieldPackageSheetKey(trip) }
                        val adminPackage = matchedLocal ?: trip.toAdminFieldPackageSummary()
                        SheetTripCard(
                            trip = trip,
                            mine = true,
                            localPackage = adminPackage,
                            onEdit = { editingPackage = adminPackage },
                            onShare = {
                                busy = true
                                scope.launch {
                                    val file = FieldPackageManager.exportPackage(context, adminPackage, records, markedPoints, savedTracks)
                                    shareFieldPackage(context, file)
                                    busy = false
                                }
                            },
                            onDeleteFromSheet = {
                                busy = true
                                scope.launch {
                                    val deleted = FieldPackageSheetSyncClient.deleteTrip(trip)
                                    if (deleted) {
                                        Toast.makeText(context, "ADMIN: obrisano iz zajedničkog rasporeda", Toast.LENGTH_SHORT).show()
                                        refreshSheetTrips()
                                    } else {
                                        Toast.makeText(context, "Brisanje nije uspjelo", Toast.LENGTH_SHORT).show()
                                    }
                                    busy = false
                                }
                            },
                            onSignup = null,
                            onTransport = { transportTrip = trip },
                            showWeather = false
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateFieldPackageDialog(
            records = records,
            markedPoints = markedPoints,
            savedTracks = savedTracks,
            currentUserLocation = currentUserLocation,
            initialStep = createInitialStep,
            onRequestGpsLocation = onRequestGpsLocation,
            onFindAreaOnMap = {
                showCreate = false
                onFindAreaOnMap()
            },
            onDismiss = {
                FieldPackageDraftStore.clear(context)
                showCreate = false
            },
            onCreate = { summary ->
                val next = listOf(summary) + packages
                FieldPackageManager.save(context, next)
                packages = next
                showCreate = false
                onChanged()
                scope.launch {
                    val synced = FieldPackageSheetSyncClient.submitOrQueue(context, summary)
                    if (synced) {
                        val existingRasporedUrl = dohvatiRasporedUrl(context, summary.id)
                        val newRasporedUrl = existingRasporedUrl ?: FieldPackageSheetSyncClient.kreirajRasporedTab(summary)?.also { url ->
                            spremRasporedUrl(context, summary.id, url)
                            spremRasporedUrl(context, fieldPackageSharedTripKey(summary.tripDateText.orEmpty(), summary.locationName.orEmpty().ifBlank { summary.name }), url)
                        }
                        val marked = FieldPackageManager.list(context).map { local ->
                            if (local.id == summary.id) local.copy(sheetSynced = true, rasporedUrl = newRasporedUrl ?: local.rasporedUrl) else local
                        }
                        FieldPackageManager.save(context, marked)
                        packages = marked
                        sheetTrips = mergeSheetTripsWithLocalPackages(sheetTrips, marked)
                    }
                    if (synced) {
                        Toast.makeText(
                            context,
                            "✓ Izlet dodan u zajednički raspored.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Izlet spremljen lokalno — nije još u zajedničkom rasporedu. Provjeri internet i osvježi popis.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    refreshSheetTrips()
                }
            }
        )
    }

    signupTrip?.let { trip ->
        FieldPackageSignupDialog(
            trip = trip,
            onDismiss = { signupTrip = null },
            onSubmit = { name, attendance, transport, seats, departure, note ->
                busy = true
                scope.launch {
                    val ok = FieldPackageSheetSyncClient.signupForTrip(trip, name, attendance, transport, seats, departure, note)
                    if (ok) {
                        Toast.makeText(context, "Prijava spremljena", Toast.LENGTH_SHORT).show()
                        signupTrip = null
                        refreshSheetTrips()
                    } else {
                        Toast.makeText(context, "Prijava nije uspjela", Toast.LENGTH_SHORT).show()
                    }
                    busy = false
                }
            }
        )
    }

    transportTrip?.let { trip ->
        FieldPackageTransportDialog(
            trip = trip,
            onDismiss = { transportTrip = null },
            onSignup = { transportTrip = null; signupTrip = trip }
        )
    }

    editingPackage?.let { pkg ->
        EditFieldPackageDialog(
            pkg = pkg,
            onDismiss = { editingPackage = null },
            onSave = { updated ->
                val next = if (packages.any { it.id == updated.id }) {
                    packages.map { if (it.id == updated.id) updated else it }
                } else {
                    listOf(updated) + packages
                }
                FieldPackageManager.save(context, next)
                packages = FieldPackageManager.list(context)
                editingPackage = null
                onChanged()
                Toast.makeText(context, "Izlet ažuriran: ${updated.name}", Toast.LENGTH_SHORT).show()
                if (updated.sheetSynced) {
                    scope.launch {
                        val oldTrip = sheetTrips.firstOrNull { trip ->
                            fieldPackageSheetKey(pkg) == fieldPackageSheetKey(trip)
                        }
                        if (oldTrip != null) {
                            FieldPackageSheetSyncClient.deleteTrip(oldTrip)
                        }
                        FieldPackageSheetSyncClient.submitOrQueue(context, updated)
                        refreshSheetTrips()
                    }
                }
            }
        )
    }
}

@Composable
private fun FieldReadinessCard(
    context: Context,
    records: List<SpeleoRecord>,
    markedPoints: List<MarkedPoint>,
    savedTracks: List<SavedTrack>,
    currentUserLocation: GeoPoint?
) {
    val activeMap = OfflineTileManager.getActiveMapName(context)
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Spremno za teren", fontWeight = FontWeight.Bold)
            Text(
                if (activeMap != null) "Imaš aktivnu offline kartu. Novi izlet može odmah povući objekte iz tog područja." else "Za najbolji paket prvo odaberi područje na karti.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FieldPackageCard(pkg: FieldPackageSummary, onOpenMap: () -> Unit, onEdit: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var showDeleteConfirm by remember(pkg.id) { mutableStateOf(false) }
    var descExpanded by remember(pkg.id) { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Obriši izlet?") },
            text = { Text("\"${pkg.name}\" i svi vezani podaci bit će trajno obrisani.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Obriši", color = Color(0xFFFFA0A0))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Odustani") }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(1.5.dp, Color(0xFFC7A7FF).copy(alpha = 0.26f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFC7A7FF).copy(alpha = 0.16f),
                    contentColor = Color(0xFFE8DEFF),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFC7A7FF).copy(alpha = 0.28f))
                ) {
                    Text("MOJ IZLET", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Uredi", tint = Color(0xFFFFD27A))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Obriši", tint = Color(0xFFFFA0A0))
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FieldPackageIconBadge(Icons.Default.Event, Color(0xFFC7A7FF), Color(0xFF33234C))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        pkg.locationName.orEmpty().ifBlank { pkg.name },
                        color = Color.White.copy(alpha = 0.97f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Datum: ${pkg.tripDateText.orEmpty().ifBlank { formatPackageDateShort(pkg.createdAtMillis) }}  •  Cilj: ${pkg.goal.orEmpty().ifBlank { "Izletiranje" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (pkg.organizer.orEmpty().isNotBlank()) {
                        Text(
                            "Voditelj: ${pkg.organizer.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.62f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (pkg.description.isNotBlank()) {
                Surface(
                    color = Color.White.copy(alpha = 0.055f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.clickable { descExpanded = !descExpanded }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            pkg.description,
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (descExpanded) Int.MAX_VALUE else 2,
                            overflow = if (descExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
                        )
                        if (!descExpanded && pkg.description.length > 120) {
                            Text(
                                "Više...",
                                color = Color(0xFFC7A7FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }


            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldPackageStatChip(Icons.Default.CheckCircle, if (pkg.sheetSynced) "u SOV Cloudu" else "čeka sync", Color(0xFF8BE9B5), Color(0xFF163425))
                FieldPackageStatChip(Icons.Default.Event, pkg.goal.orEmpty().ifBlank { "Izlet" }, Color(0xFFC7A7FF), Color(0xFF2D2340))
            }
        }
    }
}


@Composable
private fun FieldPackageStatChip(icon: ImageVector, text: String, tint: Color, bg: Color) {
    Surface(
        color = bg.copy(alpha = 0.86f),
        contentColor = tint,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
            Text(text, color = Color.White.copy(alpha = 0.90f), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun FieldPackageIconBadge(icon: ImageVector, tint: Color, bg: Color) {
    Surface(
        modifier = Modifier.size(46.dp),
        color = bg.copy(alpha = 0.90f),
        contentColor = tint,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.22f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(25.dp))
        }
    }
}




@Composable
private fun FieldPackageSignupDialog(
    trip: FieldPackageSheetTrip,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var attendance by remember { mutableStateOf("confirmed") }
    var transport by remember { mutableStateOf("needs_ride") }
    var seats by remember { mutableStateOf(3) }
    var departure by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prijava na izlet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    trip.location.ifBlank { "Izlet iz rasporeda" } + " • " + trip.date.ifBlank { "bez datuma" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ime i prezime") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Dolazak", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = attendance == "confirmed", onClick = { attendance = "confirmed" }, label = { Text("Idem") })
                    FilterChip(selected = attendance == "maybe", onClick = { attendance = "maybe" }, label = { Text("Možda") })
                    FilterChip(selected = attendance == "declined", onClick = { attendance = "declined" }, label = { Text("Ne idem") })
                }
                Text("Prijevoz", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = transport == "needs_ride", onClick = { transport = "needs_ride" }, label = { Text("Trebam prijevoz") })
                    FilterChip(selected = transport == "driver", onClick = { transport = "driver" }, label = { Text("Imam auto") })
                    FilterChip(selected = transport == "own", onClick = { transport = "own" }, label = { Text("Snalazim se") })
                }
                if (transport == "driver") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Slobodna mjesta", modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { seats = (seats - 1).coerceAtLeast(0) }) { Text("−") }
                        Text(seats.toString(), fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { seats = (seats + 1).coerceAtMost(8) }) { Text("+") }
                    }
                }
                OutlinedTextField(
                    value = departure,
                    onValueChange = { departure = it },
                    label = { Text("Mjesto polaska") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Napomena") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name.trim(), attendance, transport, if (transport == "driver") seats else 0, departure.trim(), note.trim()) },
                enabled = name.trim().isNotBlank()
            ) {
                Text("Spremi prijavu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Odustani") }
        }
    )
}

@Composable
private fun FieldPackageTransportDialog(
    trip: FieldPackageSheetTrip,
    onDismiss: () -> Unit,
    onSignup: () -> Unit
) {
    var loading by remember(trip.cloudId) { mutableStateOf(true) }
    var signups by remember(trip.cloudId) { mutableStateOf<List<FieldPackageTripSignup>>(emptyList()) }
    LaunchedEffect(trip.cloudId) {
        loading = true
        signups = FieldPackageSheetSyncClient.fetchTripSignups(trip)
        loading = false
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prijave i prijevoz") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    trip.location.ifBlank { "Izlet" } + " • " + trip.date.ifBlank { "bez datuma" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (loading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Učitavam prijave…")
                    }
                } else if (signups.isEmpty()) {
                    Text("Još nema prijava za ovaj izlet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val drivers = signups.filter { it.transportMode == "driver" && it.attendanceStatus != "declined" }
                    val needsRide = signups.filter { it.transportMode == "needs_ride" && it.attendanceStatus != "declined" }
                    val maybe = signups.filter { it.attendanceStatus == "maybe" }
                    Text("Sažetak", fontWeight = FontWeight.Bold)
                    Text("Prijavljenih: ${signups.count { it.attendanceStatus != "declined" }} · auta: ${drivers.size} · slobodnih mjesta: ${drivers.sumOf { it.seatsAvailable }} · treba prijevoz: ${needsRide.size}")
                    Divider()
                    Text("Vozači", fontWeight = FontWeight.Bold)
                    if (drivers.isEmpty()) Text("Nema upisanih vozača.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    drivers.forEach { row ->
                        Text("🚗 ${row.memberName} · ${row.seatsAvailable} mjesta" + row.departurePlace.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty())
                    }
                    Text("Trebaju prijevoz", fontWeight = FontWeight.Bold)
                    if (needsRide.isEmpty()) Text("Nitko nije označio da treba prijevoz.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    needsRide.forEach { row ->
                        Text("🙋 ${row.memberName}" + row.departurePlace.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty())
                    }
                    if (maybe.isNotEmpty()) {
                        Text("Možda", fontWeight = FontWeight.Bold)
                        maybe.forEach { row -> Text("? ${row.memberName}") }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onSignup) { Text("Moja prijava") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } }
    )
}

@Composable
private fun SheetTripCard(
    trip: FieldPackageSheetTrip,
    mine: Boolean,
    localPackage: FieldPackageSummary? = null,
    onEdit: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDeleteFromSheet: (() -> Unit)? = null,
    onSignup: (() -> Unit)? = null,
    onTransport: (() -> Unit)? = null,
    cachedWeather: FieldWeatherResult? = null,
    onWeatherFetched: ((FieldWeatherResult) -> Unit)? = null,
    showWeather: Boolean = true
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var descExpanded by remember(trip.rowNumber, trip.description) { mutableStateOf(false) }
    var showSheetDeleteConfirm by remember(trip.rowNumber) { mutableStateOf(false) }
    val rasporedKey = remember(trip.date, trip.location) { fieldPackageSharedTripKey(trip.date, trip.location) }
    var rasporedUrl by remember(trip.rowNumber, rasporedKey, trip.rasporedUrl, localPackage?.id, localPackage?.rasporedUrl) {
        mutableStateOf(
            trip.rasporedUrl.takeIf { it.isNotBlank() }
                ?: localPackage?.rasporedUrl?.takeIf { it.isNotBlank() }
                ?: dohvatiRasporedUrl(context, rasporedKey)
                ?: dohvatiRasporedUrl(context, trip.rowNumber.toString())
                ?: localPackage?.id?.let { dohvatiRasporedUrl(context, it) }
        )
    }
    var rasporedLoading by remember(trip.rowNumber, rasporedKey, localPackage?.id) { mutableStateOf(false) }
    var weatherResult by remember(trip.rowNumber) { mutableStateOf(cachedWeather) }
    var weatherLoading by remember(trip.rowNumber) { mutableStateOf(false) }
    val sheetTripStartMillis = remember(trip.rowNumber, trip.date) { parseSheetTripStartMillis(trip.date) }
    val sheetTripEndMillis = remember(trip.rowNumber, trip.date) { parseSheetTripEndMillis(trip.date) ?: sheetTripStartMillis }
    // If the shared Sheet date cannot be parsed, still try weather from location using today as fallback.
    val weatherFetchMillis = sheetTripStartMillis ?: System.currentTimeMillis()
    val weatherFetchEndMillis = sheetTripEndMillis ?: (weatherFetchMillis + 24L * 60 * 60 * 1000L)
    val sheetWeatherCoords = remember(
        trip.rowNumber,
        trip.centerLat,
        trip.centerLon,
        trip.minLat,
        trip.maxLat,
        trip.minLon,
        trip.maxLon,
        localPackage?.id,
        localPackage?.centerLat,
        localPackage?.centerLon,
        localPackage?.minLat,
        localPackage?.maxLat,
        localPackage?.minLon,
        localPackage?.maxLon
    ) {
        trip.weatherCoordinates() ?: localPackage?.weatherCoordinates()
    }
    val tripIsRelevantForWeather = run {
        val endMillis = parseSheetTripEndMillis(trip.date)
        val now = System.currentTimeMillis()
        val sevenDaysAgoMs = now - 7L * 24 * 60 * 60 * 1000L
        endMillis == null || endMillis >= sevenDaysAgoMs
    }
    val canFetchWeather = showWeather &&
        tripIsRelevantForWeather &&
        (trip.weatherCity.isNotBlank() || trip.location.length >= 3 || sheetWeatherCoords != null)

    LaunchedEffect(trip.rowNumber, rasporedKey, rasporedUrl, trip.rasporedUrl, localPackage?.id, localPackage?.rasporedUrl, mine) {
        if (false && rasporedUrl == null && mine && !rasporedLoading) {
            rasporedLoading = true
            val createdUrl = if (localPackage != null) {
                FieldPackageSheetSyncClient.kreirajRasporedTab(localPackage)
            } else {
                FieldPackageSheetSyncClient.kreirajRasporedTab(trip)
            }
            if (!createdUrl.isNullOrBlank()) {
                spremRasporedUrl(context, rasporedKey, createdUrl)
                if (trip.rowNumber > 0) spremRasporedUrl(context, trip.rowNumber.toString(), createdUrl)
                localPackage?.id?.let { spremRasporedUrl(context, it, createdUrl) }
                if (trip.rowNumber >= 2) FieldPackageSheetSyncClient.updateRasporedUrlOnSheet(trip.rowNumber, createdUrl)
                rasporedUrl = createdUrl
            }
            rasporedLoading = false
        }
    }

    fun openOrCreateRaspored() {
        rasporedUrl?.takeIf { it.isNotBlank() }?.let { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            return
        }
        if (rasporedLoading) return
        scope.launch {
            rasporedLoading = true
            val createdUrl = if (localPackage != null) {
                FieldPackageSheetSyncClient.kreirajRasporedTab(localPackage)
            } else {
                FieldPackageSheetSyncClient.kreirajRasporedTab(trip)
            }
            if (!createdUrl.isNullOrBlank()) {
                spremRasporedUrl(context, rasporedKey, createdUrl)
                if (trip.rowNumber > 0) spremRasporedUrl(context, trip.rowNumber.toString(), createdUrl)
                localPackage?.id?.let { spremRasporedUrl(context, it, createdUrl) }
                if (trip.rowNumber >= 2) FieldPackageSheetSyncClient.updateRasporedUrlOnSheet(trip.rowNumber, createdUrl)
                rasporedUrl = createdUrl
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(createdUrl)))
            } else {
                Toast.makeText(context, "Nisam uspio otvoriti tablicu prijevoza. Provjeri internet i pokušaj opet.", Toast.LENGTH_LONG).show()
            }
            rasporedLoading = false
        }
    }

    LaunchedEffect(trip.rowNumber, canFetchWeather, trip.location, trip.weatherCity, sheetWeatherCoords, weatherFetchMillis, weatherFetchEndMillis) {
        if (canFetchWeather && weatherResult == null && !weatherLoading) {
            weatherLoading = true
            weatherResult = if (sheetWeatherCoords != null) {
                fetchFieldWeather(
                    lat = sheetWeatherCoords.first,
                    lon = sheetWeatherCoords.second,
                    startMillis = weatherFetchMillis,
                    endMillis = weatherFetchEndMillis
                )
            } else {
                val locationQuery = trip.weatherCity.trim().ifBlank { trip.location.trim() }
                if (locationQuery.isNotBlank()) {
                    fetchFieldWeatherForLocation(
                        location = locationQuery,
                        startMillis = weatherFetchMillis,
                        endMillis = weatherFetchEndMillis
                    )
                } else null
            }
            weatherResult?.let { onWeatherFetched?.invoke(it) }
            weatherLoading = false
        }
    }

    val accent = if (mine) Color(0xFF72E0C4) else Color(0xFF8EC5FF)
    val bg = if (mine) Color(0xFF10281E) else Color(0xFF111B27)

    if (showSheetDeleteConfirm && onDeleteFromSheet != null) {
        AlertDialog(
            onDismissRequest = { showSheetDeleteConfirm = false },
            title = { Text("Obriši iz zajedničkog rasporeda?") },
            text = {
                Text(
                    "Izlet ${trip.location.ifBlank { "iz rasporeda" }} (${trip.date.ifBlank { "bez datuma" }}) bit će obrisan iz zajedničke tablice.\n\nAko je ovo tvoj lokalni paket, bit će uklonjen i iz Mojih izleta pri sljedećem osvježavanju. Nastaviti?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSheetDeleteConfirm = false
                    onDeleteFromSheet()
                }) {
                    Text("Obriši", color = Color(0xFFFFA0A0), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSheetDeleteConfirm = false }) { Text("Odustani") }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = if (mine) 0.34f else 0.22f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accent.copy(alpha = 0.15f),
                    contentColor = accent,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.26f))
                ) {
                    Text(
                        if (mine) "MOJ U RASPOREDU" else "ZAJEDNIČKI RASPORED",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldPackageIconBadge(Icons.Default.Share, accent, if (mine) Color(0xFF163425) else Color(0xFF17334A))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        trip.location.ifBlank { "Izlet iz rasporeda" },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Text(
                        "Datum: " + trip.date.ifBlank { "—" } + "  •  Cilj: " + trip.goal.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                    Text(
                        "Voditelj: " + trip.leader.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.58f)
                    )
                }
            }

            if (trip.description.isNotBlank()) {
                Surface(
                    color = Color.White.copy(alpha = 0.055f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.clickable { descExpanded = !descExpanded }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                trip.description,
                                color = Color.White.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = if (descExpanded) Int.MAX_VALUE else 2,
                                overflow = if (descExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (descExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (descExpanded) "Zatvori" else "Proširi",
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp).padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { onTransport?.invoke() },
                enabled = onTransport != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFC46B).copy(alpha = 0.50f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD27A))
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFD27A))
                Spacer(Modifier.width(6.dp))
                Text("🚗 Prijave i prijevoz", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { sendTripAnnouncementMail(context, trip) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD7F66F), contentColor = Color(0xFF132000))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("📧 Pošalji najavu na mailing listu", fontWeight = FontWeight.Bold)
            }

            if (trip.participants.isNotBlank()) {
                Surface(
                    color = Color(0xFFFFC46B).copy(alpha = 0.11f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFC46B).copy(alpha = 0.26f))
                ) {
                    Text(
                        text = "👥 Prijavljeni: " + trip.participants,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trip.drivers.isNotBlank()) {
                Surface(
                    color = Color(0xFF72E0C4).copy(alpha = 0.10f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF72E0C4).copy(alpha = 0.24f))
                ) {
                    Text(
                        text = "🚗 Voze: " + trip.drivers,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (mine && onEdit != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF72E0C4).copy(alpha = 0.40f))
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = Color(0xFF72E0C4))
                        Spacer(Modifier.size(8.dp))
                        Text("Uredi", color = Color.White)
                    }
                    if (onSignup != null) {
                        OutlinedButton(
                            onClick = onSignup,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFC46B).copy(alpha = 0.46f))
                        ) {
                            Text("👥 Prijava", color = Color(0xFFFFE0A3), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (onDeleteFromSheet != null) {
                        OutlinedButton(
                            onClick = { showSheetDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFA0A0).copy(alpha = 0.42f))
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = Color(0xFFFFA0A0))
                            Spacer(Modifier.size(8.dp))
                            Text("Obriši iz zajedničkog rasporeda", color = Color(0xFFFFD0D0), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (onSignup != null) {
                        Button(
                            onClick = onSignup,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC46B), contentColor = Color(0xFF241A00))
                        ) {
                            Text("👥 Prijava na izlet", fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.045f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
                    ) {
                        Text(
                            "Samo pregled — nije tvoj lokalni paket.",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.64f)
                        )
                    }
                }
            }

            if (canFetchWeather) {
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )
                if (weatherLoading) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF4A90D9))
                        Spacer(Modifier.size(8.dp))
                        Text("Učitavam prognozu…", fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
                    }
                } else if (weatherResult != null && weatherResult!!.days.isNotEmpty()) {
                    FieldWeatherCard(weather = weatherResult!!)
                }
            }
        }
    }
}

private data class FieldPackageSheetReconcileResult(
    val packages: List<FieldPackageSummary>,
    val removedCount: Int
)

private const val FIELD_PACKAGE_SHEET_RECONCILE_PREFS = "field_package_sheet_reconcile_v1"
private const val FIELD_PACKAGE_SHEET_SEEN_KEYS = "seen_sheet_trip_keys"

private fun reconcileFieldPackagesWithSheet(
    context: Context,
    localPackages: List<FieldPackageSummary>,
    remoteTrips: List<FieldPackageSheetTrip>
): FieldPackageSheetReconcileResult {
    val currentSheetKeys = remoteTrips.map { fieldPackageSheetKey(it) }.filter { it.isNotBlank() }.toSet()
    val previouslySeenSheetKeys = loadSeenSheetTripKeys(context)

    var removedCount = 0
    val next = localPackages.mapNotNull { pkg ->
        val key = fieldPackageSheetKey(pkg)
        val wasKnownFromSheet = key in previouslySeenSheetKeys
        val stillExistsInSheet = key in currentSheetKeys

        if (wasKnownFromSheet && !stillExistsInSheet && pkg.sheetSynced) {
            removedCount++
            null
        } else {
            // If a trip has just been submitted successfully, the Sheet POST can finish before
            // the list endpoint returns the new row. Keep it marked as synced until the row has
            // been seen at least once, otherwise it disappears from the shared trips card right
            // after creation even though it was written to the Sheet.
            val syncedNow = stillExistsInSheet || (pkg.sheetSynced && !wasKnownFromSheet)
            if (pkg.sheetSynced == syncedNow) pkg else pkg.copy(sheetSynced = syncedNow)
        }
    }

    saveSeenSheetTripKeys(context, currentSheetKeys)
    return FieldPackageSheetReconcileResult(packages = next, removedCount = removedCount)
}

private fun loadSeenSheetTripKeys(context: Context): Set<String> {
    val raw = context
        .getSharedPreferences(FIELD_PACKAGE_SHEET_RECONCILE_PREFS, Context.MODE_PRIVATE)
        .getString(FIELD_PACKAGE_SHEET_SEEN_KEYS, "")
        .orEmpty()
    return raw.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toSet()
}

private fun saveSeenSheetTripKeys(context: Context, keys: Set<String>) {
    context
        .getSharedPreferences(FIELD_PACKAGE_SHEET_RECONCILE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(FIELD_PACKAGE_SHEET_SEEN_KEYS, keys.sorted().joinToString("\n"))
        .apply()
}

private fun fieldPackageSheetKey(pkg: FieldPackageSummary): String = listOf(
    pkg.tripDateText.orEmpty(),
    pkg.organizer.orEmpty(),
    pkg.locationName.orEmpty().ifBlank { pkg.name },
    pkg.description,
    pkg.goal.orEmpty().ifBlank { "Izletiranje" }
).joinToString("|") { normalizeSheetTripValue(it) }

private fun fieldPackageSheetKey(trip: FieldPackageSheetTrip): String = listOf(
    trip.date,
    trip.leader,
    trip.location,
    trip.description,
    trip.goal
).joinToString("|") { normalizeSheetTripValue(it) }

private fun mergeSheetTripsWithLocalPackages(
    remoteTrips: List<FieldPackageSheetTrip>,
    localPackages: List<FieldPackageSummary>
): List<FieldPackageSheetTrip> {
    if (localPackages.isEmpty()) return remoteTrips
    val existingKeys = remoteTrips.map { fieldPackageSheetKey(it) }.filter { it.isNotBlank() }.toMutableSet()
    val localMirrors = localPackages
        .filter { it.sheetSynced }
        .mapNotNull { pkg ->
            val trip = pkg.toFieldPackageSheetTripMirror()
            val key = fieldPackageSheetKey(trip)
            if (key.isBlank() || key in existingKeys) {
                null
            } else {
                existingKeys += key
                trip
            }
        }
    return remoteTrips + localMirrors
}

private fun FieldPackageSummary.toFieldPackageSheetTripMirror(): FieldPackageSheetTrip = FieldPackageSheetTrip(
    rowNumber = -kotlin.math.abs(id.hashCode().takeIf { it != Int.MIN_VALUE } ?: 1),
    date = tripDateText.orEmpty(),
    leader = organizer.orEmpty(),
    location = locationName.orEmpty().ifBlank { name },
    description = description,
    goal = goal.orEmpty().ifBlank { "Izletiranje" },
    rasporedUrl = rasporedUrl.orEmpty(),
    weatherCity = weatherCity.orEmpty(),
    centerLat = null,
    centerLon = null,
    minLat = null,
    maxLat = null,
    minLon = null,
    maxLon = null
)

private fun FieldPackageSheetTrip.toAdminFieldPackageSummary(): FieldPackageSummary {
    val startMillis = parseSheetTripStartMillis(date)
    val endMillis = parseSheetTripEndMillis(date) ?: startMillis
    val safeName = location.trim().ifBlank { "Izlet ${rowNumber.takeIf { it > 0 } ?: "Sheet"}" }
    return FieldPackageSummary(
        id = "admin_sheet_${rowNumber}_${kotlin.math.abs(fieldPackageSheetKey(this).hashCode())}",
        name = safeName,
        tripDateText = date,
        tripStartMillis = startMillis,
        tripEndMillis = endMillis,
        organizer = leader,
        locationName = safeName,
        goal = goal.ifBlank { "Izletiranje" },
        description = description,
        createdAtMillis = System.currentTimeMillis(),
        radiusKm = 5.0,
        centerLat = centerLat,
        centerLon = centerLon,
        objectCount = 0,
        pointCount = 0,
        trackCount = 0,
        topoDroidAttachmentCount = 0,
        offlineMapName = null,
        includesOfflineMap = false,
        imported = false,
        minLat = minLat,
        maxLat = maxLat,
        minLon = minLon,
        maxLon = maxLon,
        includeTracks = true,
        selectedTrackIds = emptyList(),
        sheetSynced = true,
        rasporedUrl = rasporedUrl,
        weatherCity = weatherCity
    )
}

private fun normalizeSheetTripValue(value: String): String = value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")


private val WeatherCitySuggestions = listOf(
    "Gospić", "Karlovac", "Ogulin", "Otočac", "Senj", "Delnice", "Rijeka", "Zadar",
    "Šibenik", "Split", "Knin", "Gračac", "Obrovac", "Pazin", "Pula", "Labin",
    "Makarska", "Imotski", "Zagreb", "Samobor", "Krapina", "Varaždin", "Čakovec",
    "Bjelovar", "Koprivnica", "Sisak", "Petrinja", "Slunj", "Duga Resa", "Drežnik Grad",
    "Plitvička Jezera", "Korenica", "Udbina", "Perušić", "Lovinac", "Sveti Rok",
    "Paklenica", "Starigrad", "Nin", "Benkovac", "Drniš", "Sinj", "Vrgorac", "Metković",
    "Dubrovnik", "Čabar", "Fužine", "Lokve", "Mrkopalj", "Begovo Razdolje", "Crni Lug",
    "Risnjak", "Buzet", "Roč", "Hum", "Motovun", "Žminj", "Poreč", "Rovinj"
)

private fun normalizeWeatherCityQuery(value: String): String = value
    .trim()
    .lowercase(Locale.ROOT)
    .replace('č', 'c')
    .replace('ć', 'c')
    .replace('š', 's')
    .replace('đ', 'd')
    .replace('ž', 'z')

@Composable
private fun WeatherCityAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val query = value.trim()
    val normalizedQuery = normalizeWeatherCityQuery(query)
    val suggestions = remember(value) {
        if (normalizedQuery.length < 2) {
            emptyList()
        } else {
            WeatherCitySuggestions
                .filter { city -> normalizeWeatherCityQuery(city).contains(normalizedQuery) }
                .take(6)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Grad / regija za vremensku prognozu") },
            placeholder = { Text("npr. Gospić, Karlovac, Zadar…") },
            singleLine = true
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                suggestions.forEach { city ->
                    AssistChip(
                        onClick = { onValueChange(city) },
                        label = { Text(city) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditFieldPackageDialog(
    pkg: FieldPackageSummary,
    onDismiss: () -> Unit,
    onSave: (FieldPackageSummary) -> Unit
) {
    var name by remember(pkg.id) { mutableStateOf(pkg.name) }
    var tripDateText by remember(pkg.id) { mutableStateOf(pkg.tripDateText.orEmpty()) }
    var organizer by remember(pkg.id) { mutableStateOf(pkg.organizer.orEmpty()) }
    var locationName by remember(pkg.id) { mutableStateOf(pkg.locationName.orEmpty()) }
    var weatherCity by remember(pkg.id) { mutableStateOf(pkg.weatherCity.orEmpty()) }
    var goal by remember(pkg.id) { mutableStateOf(pkg.goal.orEmpty().ifBlank { "Izletiranje" }) }
    var description by remember(pkg.id) { mutableStateOf(pkg.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        pkg.copy(
                            name = name.trim().ifBlank { pkg.name },
                            tripDateText = tripDateText.trim(),
                            organizer = organizer.trim(),
                            locationName = locationName.trim(),
                            weatherCity = weatherCity.trim(),
                            goal = goal.trim().ifBlank { "Izletiranje" },
                            description = description.trim()
                        )
                    )
                },
                enabled = name.trim().isNotEmpty()
            ) { Text("Spremi izmjene") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
        title = { Text("Uredi izlet", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Naziv izleta / aktivnost") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = tripDateText,
                    onValueChange = { tripDateText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Datum izleta") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = organizer,
                    onValueChange = { organizer = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Voditelj") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lokacija") },
                    singleLine = true
                )
                WeatherCityAutocompleteField(
                    value = weatherCity,
                    onValueChange = { weatherCity = it },
                    modifier = Modifier.fillMaxWidth()
                )
                FieldGoalPicker(goal = goal, onGoalChanged = { goal = it })
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().height(92.dp),
                    label = { Text("Opis izleta") }
                )
            }
        }
    )
}

@Composable
private fun CreateFieldPackageDialog(
    records: List<SpeleoRecord>,
    markedPoints: List<MarkedPoint>,
    savedTracks: List<SavedTrack>,
    currentUserLocation: GeoPoint?,
    initialStep: Int = 0,
    onRequestGpsLocation: () -> Unit,
    onFindAreaOnMap: () -> Unit,
    onDismiss: () -> Unit,
    onCreate: (FieldPackageSummary) -> Unit
) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val activeMapName = remember { OfflineTileManager.getActiveMapName(context) }
    var tripStartMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var tripEndMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var organizer by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var weatherCity by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("Izletiranje") }
    var description by remember { mutableStateOf("") }
    var showOptionalAddons by remember { mutableStateOf(false) }
    var attachActiveMap by remember { mutableStateOf(false) }
    var includeSavedTracks by remember { mutableStateOf(false) }
    var manualTrackSelection by remember { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectedTracks = remember(savedTracks, selectedTrackIds) { savedTracks.filter { it.id in selectedTrackIds } }
    val tripDateText = formatFieldTripDateRange(tripStartMillis, tripEndMillis)
    val generatedName = buildFieldPackageTitle(locationName, goal)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    onCreate(
                        FieldPackageSummary(
                            id = "cloud_trip_" + UUID.randomUUID().toString(),
                            name = generatedName,
                            tripDateText = tripDateText,
                            tripStartMillis = tripStartMillis,
                            tripEndMillis = tripEndMillis,
                            organizer = organizer.trim(),
                            locationName = locationName.trim(),
                            goal = goal.trim().ifBlank { "Izletiranje" },
                            description = description.trim(),
                            createdAtMillis = now,
                            radiusKm = 0.0,
                            centerLat = null,
                            centerLon = null,
                            objectCount = 0,
                            pointCount = 0,
                            trackCount = if (includeSavedTracks) selectedTracks.size else 0,
                            topoDroidAttachmentCount = 0,
                            offlineMapName = if (attachActiveMap) activeMapName else null,
                            includesOfflineMap = attachActiveMap && activeMapName != null,
                            imported = false,
                            minLat = null,
                            maxLat = null,
                            minLon = null,
                            maxLon = null,
                            includeTracks = includeSavedTracks,
                            selectedTrackIds = if (includeSavedTracks) selectedTrackIds.toList() else emptyList(),
                            sheetSynced = false,
                            rasporedUrl = null,
                            weatherCity = weatherCity.trim()
                        )
                    )
                },
                enabled = tripStartMillis != null && organizer.trim().isNotEmpty() && locationName.trim().isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(language.pick("Spremi izlet", "Save trip"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.pick("Odustani", "Cancel")) } },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(language.pick("Novi SOV Cloud izlet", "New SOV Cloud trip"), fontWeight = FontWeight.Bold)
                Text(
                    language.pick("Osnovni izlet ostaje čist. Kartu, KML/GPX ili track dodaješ samo ako treba.", "Basic trip stays clean. Map, KML/GPX or track are optional add-ons."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FieldWizardInfoCard(
                    icon = Icons.Default.Event,
                    title = "Jednostavni unos",
                    body = "Prvo spremi jednostavni izlet. Dodaci su opcionalni: aktivna karta ili spremljeni trackovi mogu se vezati uz izlet bez vraćanja starog wizard kaosa.",
                    accent = Color(0xFF72E0C4)
                )
                FieldDateRangePicker(
                    startMillis = tripStartMillis,
                    endMillis = tripEndMillis,
                    onStartChanged = { picked ->
                        tripStartMillis = picked
                        if (tripEndMillis == null || (tripEndMillis ?: 0L) < picked) tripEndMillis = picked
                    },
                    onEndChanged = { tripEndMillis = it }
                )
                OutlinedTextField(
                    value = organizer,
                    onValueChange = { organizer = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Voditelj") },
                    placeholder = { Text("Ime voditelja") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lokacija") },
                    placeholder = { Text("npr. Krasno, Crnopac, Medvednica…") },
                    singleLine = true
                )
                WeatherCityAutocompleteField(
                    value = weatherCity,
                    onValueChange = { weatherCity = it },
                    modifier = Modifier.fillMaxWidth()
                )
                FieldGoalPicker(goal = goal, onGoalChanged = { goal = it })
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().height(118.dp),
                    label = { Text("Opis izleta") },
                    placeholder = { Text("Plan izleta, pristup, logistika, posebne napomene…") }
                )
                Text(
                    language.pick("Opis izleta vide članovi na webu i u appu. Dodaci kao GPX/KML nisu obavezni.", "Trip description is visible on web and in the app. GPX/KML add-ons are optional."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TripOptionalAddonsCard(
                    expanded = showOptionalAddons,
                    onExpandedChange = { showOptionalAddons = it },
                    activeMapName = activeMapName,
                    attachActiveMap = attachActiveMap,
                    onAttachActiveMapChanged = { attachActiveMap = it },
                    savedTracks = savedTracks,
                    includeSavedTracks = includeSavedTracks,
                    manualTrackSelection = manualTrackSelection,
                    selectedTrackIds = selectedTrackIds,
                    onIncludeTracksChanged = { includeSavedTracks = it },
                    onToggleManual = { manualTrackSelection = !manualTrackSelection },
                    onTrackToggle = { id, checked -> selectedTrackIds = if (checked) selectedTrackIds + id else selectedTrackIds - id }
                )
            }
        }
    )
}


internal fun parseSheetTripStartMillis(dateStr: String): Long? = parseSheetTripDateMillis(dateStr, preferEnd = false)

internal fun parseSheetTripEndMillis(dateStr: String): Long? = parseSheetTripDateMillis(dateStr, preferEnd = true)

private fun parseSheetTripDateMillis(dateStr: String, preferEnd: Boolean): Long? {
    if (dateStr.isBlank()) return null
    val tokens = dateStr.split(Regex("[^0-9]+"))
        .mapNotNull { it.trim().toIntOrNull() }
    if (tokens.size < 2) return null

    return try {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val year = tokens.lastOrNull { it >= 1900 } ?: currentYear
        val yearIndex = tokens.indexOfLast { it == year }
        val beforeYear = if (yearIndex > 0) tokens.take(yearIndex) else tokens

        val day: Int
        val month: Int
        if (!preferEnd) {
            day = beforeYear.getOrNull(0) ?: return null
            month = when {
                beforeYear.size >= 2 && beforeYear[1] in 1..12 -> beforeYear[1]
                beforeYear.size >= 4 && beforeYear[3] in 1..12 -> beforeYear[3]
                beforeYear.lastOrNull() in 1..12 -> beforeYear.last()
                else -> return null
            }
        } else {
            month = when {
                beforeYear.size >= 2 && beforeYear.last() in 1..12 -> beforeYear.last()
                beforeYear.size >= 4 && beforeYear[3] in 1..12 -> beforeYear[3]
                else -> return null
            }
            day = when {
                beforeYear.size >= 2 && beforeYear[beforeYear.size - 2] in 1..31 -> beforeYear[beforeYear.size - 2]
                else -> beforeYear.firstOrNull() ?: return null
            }
        }

        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, if (preferEnd) 23 else 0)
            set(Calendar.MINUTE, if (preferEnd) 59 else 0)
            set(Calendar.SECOND, if (preferEnd) 59 else 0)
            set(Calendar.MILLISECOND, if (preferEnd) 999 else 0)
        }.timeInMillis
    } catch (_: Exception) {
        null
    }
}

internal suspend fun fetchFieldWeatherForLocation(
    location: String,
    startMillis: Long,
    endMillis: Long
): FieldWeatherResult? = withContext(Dispatchers.IO) {
    val cleanLocation = location.trim()
        .replace(Regex("^(Croatia|Hrvatska)\\s*,?", RegexOption.IGNORE_CASE), "")
        .trim()
        .trimEnd(',')
    val cleaned = cleanLocation
        .replace(Regex("\\(.*?\\)"), " ")
        .replace(Regex("[;/|]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (cleaned.isBlank()) return@withContext null

    val candidates = listOf(
        cleaned,
        cleaned.substringBefore(" - ").trim(),
        cleaned.substringBefore(" – ").trim(),
        cleaned.split(" ").take(2).joinToString(" ").trim()
    ).filter { it.length >= 2 }.distinct()

    for (candidate in candidates) {
        try {
            val query = java.net.URLEncoder.encode(
                if (candidate.contains("Croatia", ignoreCase = true) || candidate.contains("Hrvatska", ignoreCase = true)) candidate else "$candidate, Croatia",
                "UTF-8"
            )
            val url = "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=$query&count=1&language=hr&format=json&countryCode=HR"
            val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val json = com.google.gson.JsonParser.parseString(body).asJsonObject
            val results = json.getAsJsonArray("results") ?: continue
            if (results.size() == 0) continue
            val first = results[0].asJsonObject
            return@withContext fetchFieldWeather(
                lat = first.get("latitude").asDouble,
                lon = first.get("longitude").asDouble,
                startMillis = startMillis,
                endMillis = endMillis
            )
        } catch (_: Exception) {
            // Try the next cleaned location candidate.
        }
    }
    null
}

internal suspend fun fetchFieldWeather(
    lat: Double,
    lon: Double,
    startMillis: Long,
    endMillis: Long
): FieldWeatherResult? = withContext(Dispatchers.IO) {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startDate = sdf.format(Date(startMillis))
        val endDate = sdf.format(Date(endMillis.coerceAtLeast(startMillis)))
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${lat}&longitude=${lon}" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,windspeed_10m_max,weathercode" +
            "&timezone=auto" +
            "&start_date=${startDate}&end_date=${endDate}"

        val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
        }

        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val json = com.google.gson.JsonParser.parseString(body).asJsonObject
        val daily = json.getAsJsonObject("daily")
        val dates = daily.getAsJsonArray("time")
        val tempMax = daily.getAsJsonArray("temperature_2m_max")
        val tempMin = daily.getAsJsonArray("temperature_2m_min")
        val precip = daily.getAsJsonArray("precipitation_sum")
        val wind = daily.getAsJsonArray("windspeed_10m_max")
        val wmo = daily.getAsJsonArray("weathercode")

        val days = (0 until dates.size()).map { i ->
            FieldWeatherDay(
                date = dates[i].asString,
                tempMax = tempMax[i].asFloat,
                tempMin = tempMin[i].asFloat,
                precipMm = precip[i].asFloat,
                windKmh = wind[i].asFloat,
                wmoCode = wmo[i].asInt
            )
        }

        FieldWeatherResult(
            days = days,
            timezone = json.get("timezone")?.asString ?: "",
            fetchedAtMillis = System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun FieldWeatherCard(weather: FieldWeatherResult) {
    val updatedAt = remember(weather.fetchedAtMillis) {
        SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(Date(weather.fetchedAtMillis))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1B2A),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF4A90D9).copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.WbSunny, null, tint = Color(0xFFFFC46B), modifier = Modifier.size(20.dp))
                Text("Vremenska prognoza", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("open-meteo.com", fontSize = 9.sp, color = Color(0xFF8EC5FF).copy(alpha = 0.70f))
                    Text("Ažurirano: $updatedAt", fontSize = 9.sp, color = Color(0xFF8EC5FF).copy(alpha = 0.70f))
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(weather.days) { day ->
                    val label = try {
                        val parts = day.date.split("-")
                        "${parts[2].trimStart('0')}.${parts[1].trimStart('0')}"
                    } catch (e: Exception) {
                        day.date
                    }

                    val borderColor = Color(0xFF4A90D9).copy(alpha = 0.22f)

                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.60f), fontWeight = FontWeight.Bold)
                                Text(wmoCodeToLabel(day.wmoCode).split(" ").firstOrNull() ?: "🌡", fontSize = 20.sp)
                                Text(
                                    "${day.tempMax.toInt()}°/${day.tempMin.toInt()}°",
                                    fontSize = 10.sp,
                                    color = when {
                                        day.tempMax < 0 -> Color(0xFF90CAF9)
                                        day.tempMax < 10 -> Color(0xFFB0BEC5)
                                        else -> Color(0xFFFFCC80)
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (day.precipMm > 0.5f) {
                                    Text("💧${day.precipMm.toInt()}mm", fontSize = 9.sp, color = Color(0xFF81D4FA))
                                }
                                if (day.windKmh > 20f) {
                                    Text("💨${day.windKmh.toInt()}", fontSize = 9.sp, color = Color.White.copy(alpha = 0.55f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldAreaPickerCard(
    activeMap: String?,
    activeBounds: OfflineTileManager.OfflineBounds?,
    preview: FieldPackageSummary,
    currentUserLocation: GeoPoint?,
    useCurrentLocation: Boolean,
    onUseGps: () -> Unit,
    onFindAreaOnMap: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFC7A7FF).copy(alpha = 0.16f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Map, null, tint = premiumIconTint("field map", active = true), modifier = Modifier.size(30.dp))
                Column(Modifier.weight(1f)) {
                    Text("Karta i područje", fontWeight = FontWeight.Bold)
                    Text("Klik otvara kartu u SOV-only download modu. Označi područje i vrati se u ovaj flow.", color = Color.White.copy(alpha = 0.72f))
                }
            }
            Button(onClick = onFindAreaOnMap, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Izaberi područje na karti")
            }
            OutlinedButton(onClick = onUseGps, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (currentUserLocation != null || useCurrentLocation) "GPS centar" else "Upali GPS")
            }
            FieldWizardPreviewCard(preview = preview, activeMap = activeMap, activeBounds = activeBounds)
        }
    }
}

@Composable
private fun FieldTrackPickerCard(
    savedTracks: List<SavedTrack>,
    includeTracks: Boolean,
    manualTrackSelection: Boolean,
    selectedTrackIds: Set<String>,
    autoTrackCount: Int,
    onIncludeTracksChanged: (Boolean) -> Unit,
    onToggleManual: () -> Unit,
    onTrackToggle: (String, Boolean) -> Unit,
    onImportTrack: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFFFFC46B).copy(alpha = 0.14f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Route, null, tint = premiumIconTint("field tracks", active = includeTracks), modifier = Modifier.size(26.dp))
                Column(Modifier.weight(1f)) {
                    Text("Trackovi", fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            savedTracks.isEmpty() -> "Nema spremljenih trackova."
                            !includeTracks -> "Bez trackova."
                            manualTrackSelection -> "Ručno odabrano: ${selectedTrackIds.size}"
                            else -> "Automatski: $autoTrackCount trackova u području"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = includeTracks && !manualTrackSelection,
                    onClick = {
                        if (!includeTracks) onIncludeTracksChanged(true)
                        if (manualTrackSelection) onToggleManual()
                    },
                    label = { Text("Automatski iz područja") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = includeTracks && manualTrackSelection,
                    onClick = {
                        if (!includeTracks) onIncludeTracksChanged(true)
                        if (!manualTrackSelection) onToggleManual()
                    },
                    label = { Text("Ručno odaberi") },
                    leadingIcon = { Icon(Icons.Default.Route, null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = !includeTracks,
                    onClick = { onIncludeTracksChanged(false) },
                    label = { Text("Bez trackova") },
                    leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)) }
                )
            }
            OutlinedButton(onClick = onImportTrack, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Dodaj GPX/KML track s mobitela")
            }
            var trackFilter by remember { mutableStateOf("") }
            if (includeTracks && savedTracks.isNotEmpty() && manualTrackSelection) {
                val sortedTracks = remember(savedTracks) {
                    savedTracks.sortedByDescending { it.createdAtMillis }
                }
                val filteredTracks = remember(sortedTracks, trackFilter) {
                    if (trackFilter.isBlank()) sortedTracks
                    else sortedTracks.filter { it.name.contains(trackFilter.trim(), ignoreCase = true) }
                }
                val visibleTracks = filteredTracks.take(25)

                OutlinedTextField(
                    value = trackFilter,
                    onValueChange = { trackFilter = it },
                    placeholder = { Text("Filtriraj po imenu...", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = if (trackFilter.isNotBlank()) {
                        { IconButton(onClick = { trackFilter = "" }) { Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp)) } }
                    } else null
                )

                visibleTracks.forEach { track ->
                    val checked = track.id in selectedTrackIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackToggle(track.id, !checked) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onTrackToggle(track.id, it) })
                        Column(Modifier.weight(1f)) {
                            Text(track.name.ifBlank { "Track" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${track.points.size} točaka", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (filteredTracks.size > 25) {
                    Text(
                        "Prikazujem prvih 25 od ${filteredTracks.size} trackova (poredano od najnovijeg).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (trackFilter.isNotBlank() && filteredTracks.isEmpty()) {
                    Text(
                        "Nema trackova s imenom \"$trackFilter\".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}



@Composable
private fun TripOptionalAddonsCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    activeMapName: String?,
    attachActiveMap: Boolean,
    onAttachActiveMapChanged: (Boolean) -> Unit,
    savedTracks: List<SavedTrack>,
    includeSavedTracks: Boolean,
    manualTrackSelection: Boolean,
    selectedTrackIds: Set<String>,
    onIncludeTracksChanged: (Boolean) -> Unit,
    onToggleManual: () -> Unit,
    onTrackToggle: (String, Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFF8EC5FF).copy(alpha = 0.16f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onExpandedChange(!expanded) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.UploadFile, null, tint = Color(0xFF8EC5FF), modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dodaci po želji", fontWeight = FontWeight.Bold)
                    Text(
                        "Karta, KML/GPX ili track nisu obavezni za izlet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color(0xFF8EC5FF))
            }
            if (expanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = attachActiveMap,
                        onClick = { onAttachActiveMapChanged(!attachActiveMap) },
                        enabled = activeMapName != null,
                        label = { Text(activeMapName?.let { "Aktivna karta: $it" } ?: "Nema aktivne karte") },
                        leadingIcon = { Icon(Icons.Default.Map, null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = includeSavedTracks,
                        onClick = { onIncludeTracksChanged(!includeSavedTracks) },
                        enabled = savedTracks.isNotEmpty(),
                        label = { Text(if (savedTracks.isEmpty()) "Nema spremljenih trackova" else "Spremljeni trackovi") },
                        leadingIcon = { Icon(Icons.Default.Route, null, modifier = Modifier.size(16.dp)) }
                    )
                    if (includeSavedTracks && savedTracks.isNotEmpty()) {
                        FilterChip(
                            selected = manualTrackSelection,
                            onClick = onToggleManual,
                            label = { Text(if (manualTrackSelection) "Ručno: ${selectedTrackIds.size}" else "Auto / bez ručnog odabira") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                if (includeSavedTracks && savedTracks.isNotEmpty() && manualTrackSelection) {
                    val visible = savedTracks.sortedByDescending { it.createdAtMillis }.take(12)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        visible.forEach { track ->
                            val checked = track.id in selectedTrackIds
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onTrackToggle(track.id, !checked) }.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = checked, onCheckedChange = { onTrackToggle(track.id, it) })
                                Column(Modifier.weight(1f)) {
                                    Text(track.name.ifBlank { "Track" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${track.points.size} točaka", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (savedTracks.size > visible.size) {
                            Text("Prikazujem zadnjih ${visible.size} trackova. Ostale dodaj kasnije kroz detalje izleta.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text(
                    "GPX/KML file s mobitela dodavat ćemo kroz detalje izleta; web već podržava upload fileova. Ovo ne vraća stari wizard za izradu izleta.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FieldGoalPicker(goal: String, onGoalChanged: (String) -> Unit) {
    val goals = listOf("Istraživanje", "Izletiranje", "Rekognosciranje")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Cilj izleta", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            goals.forEach { item ->
                FilterChip(
                    selected = goal == item,
                    onClick = { onGoalChanged(item) },
                    label = { Text(item) },
                    leadingIcon = if (goal == item) ({ Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp)) }) else null
                )
            }
        }
    }
}

@Composable
private fun FieldDateRangePicker(
    startMillis: Long?,
    endMillis: Long?,
    onStartChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit
) {
    val context = LocalContext.current
    fun openPicker(current: Long?, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current ?: System.currentTimeMillis() }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onPicked(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Datum / trajanje", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { openPicker(startMillis, onStartChanged) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Početak: ${if (startMillis != null) formatFieldTripDate(startMillis) else "Odaberi"}")
            }
            OutlinedButton(onClick = { openPicker(endMillis ?: startMillis, onEndChanged) }, modifier = Modifier.weight(1f), enabled = startMillis != null) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Kraj: ${if (endMillis != null) formatFieldTripDate(endMillis) else if (startMillis != null) formatFieldTripDate(startMillis) else "—"}")
            }
        }
    }
}



@Composable
private fun FieldWizardPremiumHeader(step: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Color(0xFFC7A7FF).copy(alpha = 0.16f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldPackageIconBadge(
                    icon = when (step) {
                        0 -> Icons.Default.Event
                        1 -> Icons.Default.Map
                        else -> Icons.Default.Route
                    },
                    tint = when (step) {
                        0 -> Color(0xFF72E0C4)
                        1 -> Color(0xFFC7A7FF)
                        else -> Color(0xFFFFC46B)
                    },
                    bg = Color(0xFF2D2340)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Novi izlet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        when (step) {
                            0 -> "Korak 1/3 — Datum i lokacija (obavezno)"
                            1 -> "Korak 2/3 — Offline karta i područje"
                            else -> "Korak 3/3 — Trackovi i završi"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                }
            }
            FieldWizardStepHeader(step)
        }
    }
}

@Composable
private fun FieldWizardStepHeader(step: Int) {
    val labels = listOf("1 Podaci", "2 Karta", "3 Trackovi")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        labels.forEachIndexed { index, label ->
            val active = index == step
            val done = index < step
            val accent = when (index) {
                0 -> Color(0xFF72E0C4)
                1 -> Color(0xFFC7A7FF)
                else -> Color(0xFFFFC46B)
            }
            Surface(
                color = if (active || done) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.055f),
                contentColor = if (active || done) accent else Color.White.copy(alpha = 0.62f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (active || done) accent.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (done) Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(15.dp), tint = accent)
                    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold)
                }
            }
        }
    }

    val progress = (step + 1) / 3f
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = when (step) {
            0 -> Color(0xFF72E0C4)
            1 -> Color(0xFFC7A7FF)
            else -> Color(0xFFFFC46B)
        },
        trackColor = Color.White.copy(alpha = 0.10f)
    )
}

@Composable
private fun FieldWizardInfoCard(
    icon: ImageVector,
    title: String,
    body: String,
    accent: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FieldPackageIconBadge(icon, accent, Color(0xFF202838))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(body, color = Color.White.copy(alpha = 0.70f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FieldWizardPreviewCard(preview: FieldPackageSummary, activeMap: String?, activeBounds: OfflineTileManager.OfflineBounds?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Pregled paketa", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${preview.objectCount} SOV objekata") })
                AssistChip(onClick = {}, label = { Text("${preview.pointCount} točaka") })
                AssistChip(onClick = {}, label = { Text("${preview.trackCount} trackova") })
            }
            Text(
                when {
                    activeBounds != null && activeMap != null -> "Područje je spremno. Točke se prikazuju odmah kada otvoriš izlet."
                    activeMap != null -> "Karta je odabrana."
                    else -> "Još trebaš odabrati područje na karti."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
private fun buildFieldPackageTitle(locationName: String, goal: String): String {
    val loc = locationName.trim()
    val g = goal.trim().ifBlank { "Izletiranje" }
    return if (loc.isNotBlank()) "$loc · $g" else g
}

private fun formatFieldTripDateRange(startMillis: Long?, endMillis: Long?): String {
    if (startMillis == null) return ""
    val start = formatFieldTripDate(startMillis)
    val end = formatFieldTripDate(endMillis ?: startMillis)
    return if (start == end) start else "$start - $end"
}

private fun sheetTripIsOver(dateStr: String): Boolean {
    if (dateStr.isBlank()) return false
    val endMillis = parseSheetTripEndMillis(dateStr) ?: return false
    return endMillis < System.currentTimeMillis()
}

private fun formatFieldTripDate(millis: Long?): String {
    if (millis == null) return "odaberi"
    return SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault()).format(Date(millis))
}


private fun trackIntersectsFieldArea(track: SavedTrack, bounds: OfflineTileManager.OfflineBounds?, center: GeoPoint?, radiusKm: Double): Boolean {
    return when {
        bounds != null -> track.points.any { point ->
            val lat = point.point.latitude
            val lon = point.point.longitude
            lat >= bounds.minLat && lat <= bounds.maxLat && lon >= bounds.minLon && lon <= bounds.maxLon
        }
        center != null && radiusKm > 0.0 -> track.points.any { point ->
            distanceKmForFieldDraft(center.latitude, center.longitude, point.point.latitude, point.point.longitude) <= radiusKm
        }
        else -> false
    }
}

private fun distanceKmForFieldDraft(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}


private fun buildTripAnnouncementSubject(trip: FieldPackageSheetTrip): String {
    val location = trip.location.trim().ifBlank { "Izlet" }
    val date = trip.date.trim().ifBlank { "bez datuma" }
    return "Najava izleta: $location — $date"
}

private fun buildTripAnnouncementBody(trip: FieldPackageSheetTrip): String = buildString {
    appendLine("Pozdrav svima,")
    appendLine()
    appendLine("najavljuje se izlet:")
    appendLine()
    appendLine("Datum: ${trip.date.trim().ifBlank { "—" }}")
    appendLine("Voditelj: ${trip.leader.trim().ifBlank { "—" }}")
    appendLine("Lokacija: ${trip.location.trim().ifBlank { "—" }}")
    appendLine("Cilj: ${trip.goal.trim().ifBlank { "—" }}")
    appendLine()
    appendLine("Opis izleta:")
    appendLine(trip.description.trim().ifBlank { "—" })
    if (trip.participants.trim().isNotBlank()) {
        appendLine()
        appendLine("Prijavljeni: ${trip.participants.trim()}")
    }
    if (trip.drivers.trim().isNotBlank()) {
        appendLine("Voze: ${trip.drivers.trim()}")
    }
    appendLine()
    appendLine("Lijep pozdrav,")
    appendLine("SOV")
}

private fun sendTripAnnouncementMail(context: Context, trip: FieldPackageSheetTrip) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:sovelebit@googlegroups.com")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("sovelebit@googlegroups.com"))
        putExtra(Intent.EXTRA_SUBJECT, buildTripAnnouncementSubject(trip))
        putExtra(Intent.EXTRA_TEXT, buildTripAnnouncementBody(trip))
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Pošalji najavu izleta"))
    }.onFailure {
        Toast.makeText(context, "Nema mail aplikacije za slanje najave.", Toast.LENGTH_LONG).show()
    }
}

private fun shareFieldPackage(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.sov.field-package"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Podijeli SOV izlet"))
}

private fun formatPackageDate(millis: Long): String = SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatPackageDateShort(millis: Long): String = SimpleDateFormat("d.M.", Locale.getDefault()).format(Date(millis))

@Composable
private fun SheetTripSkeletonCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    val shimmerColor = Color.White.copy(alpha = alpha)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2030))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .background(shimmerColor, RoundedCornerShape(6.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(16.dp)
                    .background(shimmerColor, RoundedCornerShape(6.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
                    .background(shimmerColor, RoundedCornerShape(6.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(shimmerColor.copy(alpha = shimmerColor.alpha * 0.5f), RoundedCornerShape(6.dp))
            )
        }
    }
}

