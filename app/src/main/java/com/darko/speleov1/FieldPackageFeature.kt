@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.darko.speleov1

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.darko.speleov1.model.SpeleoRecord
import com.darko.speleov1.util.ImportParser
import com.darko.speleov1.util.OfflineTileManager
import com.darko.speleov1.util.UserContentStore
import com.darko.speleov1.util.TopoDroidBridgeStore
import com.darko.speleov1.util.FieldTrackingLiteApi
import com.darko.speleov1.util.FieldTrackingLitePrefs
import com.darko.speleov1.util.FieldTrackingLiteStore
import com.darko.speleov1.util.FieldTrackingBatteryDiagnosticsStore
import com.darko.speleov1.util.TrackingForegroundService
import com.darko.speleov1.util.TripAssetCloudRepository
import com.darko.speleov1.util.TripCloudAsset
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import com.darko.speleov1.util.SovNetworkSecurity

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
    val messenger = LocalSovMessenger.current
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
    var visibleTripsMonthMillis by rememberSaveable { mutableStateOf(sovFieldStartOfMonth(System.currentTimeMillis())) }
    var selectedTripCategory by rememberSaveable { mutableStateOf("Sve") }
    val tripCategoryOptions = listOf("Sve", "Izlet", "Seminar", "Skup", "Ekspedicija", "Inventura", "Skupština", "Predavanje")
    val weatherCache = remember { mutableStateMapOf<Int, FieldWeatherResult>() }
    var hubSettings by remember { mutableStateOf(SovFieldHubClient.loadSettings(context)) }
    var hubRoster by remember { mutableStateOf(SovFieldHubClient.loadCachedRoster(context)) }
    var hubBusy by remember { mutableStateOf(false) }
    var hubMessage by remember { mutableStateOf<String?>(null) }
    fun refreshSheetTrips() {
        sheetLoading = true
        scope.launch {
            runCatching { FieldPackageSheetSyncClient.flushPending(context) }
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
                    messenger.error("Paket nije prepoznat")
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
                                    Text(language.pick("Cloud", "Cloud"), color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
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
                                Icon(Icons.Default.Add, contentDescription = "Dodaj")
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
                                Icon(Icons.Default.Refresh, contentDescription = "Osvježi", tint = Color.White)
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
                        onSendToHub = {
                            busy = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val file = FieldPackageManager.exportPackage(context, pkg, records, markedPoints, savedTracks)
                                        SovFieldHubClient.uploadPackage(hubSettings, file)
                                    }
                                }
                                Toast.makeText(
                                    context,
                                    result.getOrElse { SovFieldHubUploadResult(false, it.message ?: "Slanje na laptop hub nije uspjelo.") }.message,
                                    Toast.LENGTH_LONG
                                ).show()
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
                            "Nema izleta.",
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
                        Text(language.pick("Izleti iz Clouda — isti izvor za web i APK", "Trips from Cloud — same source for web and app"), color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
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
            val monthSheetTrips = upcomingSheetTrips
                .filter { sheetTripOverlapsMonth(it, visibleTripsMonthMillis) }
            val pastSheetTrips = sheetTrips
                .filter { sheetTripIsOver(it.date) }
                .sortedByDescending { parseSheetTripEndMillis(it.date) ?: 0L }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121A24)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { visibleTripsMonthMillis = sovFieldShiftMonth(visibleTripsMonthMillis, -1) }) { Icon(Icons.Default.ArrowBack, contentDescription = "Prethodni mjesec", tint = Color.White) }
                            Text(sovFieldMonthTitle(visibleTripsMonthMillis), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = { visibleTripsMonthMillis = sovFieldShiftMonth(visibleTripsMonthMillis, 1) }) { Icon(Icons.Default.ArrowUpward, contentDescription = "Sljedeći mjesec", tint = Color.White, modifier = Modifier.rotate(90f)) }
                        }
                    }
                }
            }

            if (monthSheetTrips.isEmpty()) {
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
                            }
                        }
                    }
                }
            }

            val visibleSheetTrips = monthSheetTrips

            items(visibleSheetTrips, key = { "sheet_" + it.rowNumber.toString() + "_" + it.date + "_" + it.location }) { trip ->
                val matchedLocal = packages.firstOrNull { local -> fieldPackageSheetKey(local) == fieldPackageSheetKey(trip) }
                val adminPackage = matchedLocal ?: trip.toAdminFieldPackageSummary()
                SheetTripCard(
                    trip = trip,
                    mine = true,
                    localPackage = adminPackage,
                    records = records,
                    markedPoints = markedPoints,
                    savedTracks = savedTracks,
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
                                Toast.makeText(context, "Obrisano", Toast.LENGTH_SHORT).show()
                                refreshSheetTrips()
                            } else {
                                messenger.error("Brisanje nije uspjelo")
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
                            records = records,
                            markedPoints = markedPoints,
                            savedTracks = savedTracks,
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
                                        Toast.makeText(context, "Obrisano", Toast.LENGTH_SHORT).show()
                                        refreshSheetTrips()
                                    } else {
                                        messenger.error("Brisanje nije uspjelo")
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
                            "Izlet dodan.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Spremljeno lokalno. Provjeri internet.",
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
                        messenger.error("Prijava nije uspjela")
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
                if (activeMap != null) "Offline karta je aktivna." else "Za najbolji paket prvo odaberi područje na karti.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FieldPackageCard(
    pkg: FieldPackageSummary,
    onOpenMap: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onSendToHub: () -> Unit,
    onDelete: () -> Unit
) {
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
                FieldPackageStatChip(Icons.Default.CheckCircle, if (pkg.sheetSynced) "u Cloudu" else "čeka", Color(0xFF8BE9B5), Color(0xFF163425))
                FieldPackageStatChip(Icons.Default.Event, pkg.goal.orEmpty().ifBlank { "Izlet" }, Color(0xFFC7A7FF), Color(0xFF2D2340))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Podijeli", modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Share", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onSendToHub,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF83E6C2), contentColor = Color(0xFF07120F))
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Uvoz datoteke", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Na laptop", fontWeight = FontWeight.Bold)
                }
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
            Icon(icon, contentDescription = "Ikona", modifier = Modifier.size(16.dp), tint = tint)
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
            Icon(icon, contentDescription = "Ikona", tint = tint, modifier = Modifier.size(25.dp))
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
private fun SovFieldHubSettingsCard(
    settings: SovFieldHubSettings,
    roster: SovFieldHubRoster,
    busy: Boolean,
    message: String?,
    onSave: (SovFieldHubSettings) -> Unit,
    onPing: () -> Unit,
    onFetchRoster: () -> Unit
) {
    var baseUrl by remember(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var pin by remember(settings.pin) { mutableStateOf(settings.pin) }
    val rosterTeamCount = roster.trips.sumOf { it.teams.size }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071A17), contentColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF83E6C2).copy(alpha = 0.24f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Laptop hub", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Bez interneta: mobitel ↔ laptop.",
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(
                    color = Color(0xFF83E6C2).copy(alpha = 0.10f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color(0xFF83E6C2).copy(alpha = 0.25f))
                ) {
                    Text("TEREN", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB8FFD0))
                }
            }

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Adresa laptop huba") },
                placeholder = { Text("http://192.168.43.1:8080") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN s laptopa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onSave(SovFieldHubSettings(baseUrl = baseUrl, pin = pin)) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) { Text("Spremi", color = Color.White) }
                OutlinedButton(
                    onClick = onPing,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF83E6C2).copy(alpha = 0.40f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Osvježi", modifier = Modifier.size(16.dp), tint = Color(0xFFB8FFD0))
                    Spacer(Modifier.width(6.dp))
                    Text("Test", color = Color(0xFFB8FFD0))
                }
            }

            Button(
                onClick = onFetchRoster,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF83E6C2), contentColor = Color(0xFF07120F))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Osvježi", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Povuci", fontWeight = FontWeight.Bold)
            }

            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF83E6C2))
            Text(
                "Spremljeno: ${roster.trips.size} izleta, $rosterTeamCount ekipa",
                color = Color.White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodySmall
            )
            message?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = if (it.contains("nije", true) || it.contains("greška", true) || it.contains("HTTP", true)) Color(0xFFFFA0A0) else Color(0xFFB8FFD0),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SheetTripCard(
    trip: FieldPackageSheetTrip,
    mine: Boolean,
    localPackage: FieldPackageSummary? = null,
    records: List<SpeleoRecord> = emptyList(),
    markedPoints: List<MarkedPoint> = emptyList(),
    savedTracks: List<SavedTrack> = emptyList(),
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
    val messenger = LocalSovMessenger.current
    val scope = rememberCoroutineScope()
    var descExpanded by remember(trip.rowNumber, trip.description) { mutableStateOf(false) }
    var tripExpanded by rememberSaveable(trip.rowNumber, trip.date, trip.location) { mutableStateOf(false) }
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
    var weatherError by remember(trip.rowNumber) { mutableStateOf<String?>(null) }
    val sheetTripStartMillis = remember(trip.rowNumber, trip.date) { parseSheetTripStartMillis(trip.date) }
    val sheetTripEndMillis = remember(trip.rowNumber, trip.date) { parseSheetTripEndMillis(trip.date) ?: sheetTripStartMillis }
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
    val nowForWeather = System.currentTimeMillis()
    val tripIsRelevantForWeather = run {
        val endMillis = parseSheetTripEndMillis(trip.date)
        val sevenDaysAgoMs = nowForWeather - 7L * 24 * 60 * 60 * 1000L
        endMillis == null || endMillis >= sevenDaysAgoMs
    }
    val weatherTooFarAhead = sheetTripStartMillis != null &&
        sheetTripStartMillis > nowForWeather + 16L * 24 * 60 * 60 * 1000L
    val canShowWeatherSection = tripExpanded && showWeather && tripIsRelevantForWeather
    val canFetchWeather = canShowWeatherSection && !weatherTooFarAhead &&
        (trip.weatherCity.isNotBlank() || trip.location.length >= 3 || sheetWeatherCoords != null)

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
                messenger.error(language.pick("Prijevoz nije dostupan.", "Transport is not available."))
            }
            rasporedLoading = false
        }
    }

    suspend fun loadTripWeather(force: Boolean = false) {
        if (!canFetchWeather || weatherLoading) return
        if (!force && weatherResult != null) return
        weatherLoading = true
        weatherError = null
        val result = runCatching {
            if (sheetWeatherCoords != null) {
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
        }
        weatherLoading = false
        weatherResult = result.getOrNull()
        if (weatherResult != null) {
            weatherResult?.let { onWeatherFetched?.invoke(it) }
        } else if (result.isFailure) {
            weatherError = language.pick("Prognoza nije dostupna.", "Forecast is not available.")
        } else {
            weatherError = language.pick(
                "Prognoza nije pronađena za ovu lokaciju. Dodaj grad/regiju u izlet.",
                "Forecast was not found for this location. Add a city/region to the trip."
            )
        }
    }

    LaunchedEffect(trip.rowNumber, canFetchWeather, trip.location, trip.weatherCity, sheetWeatherCoords, weatherFetchMillis, weatherFetchEndMillis) {
        loadTripWeather(force = false)
    }

    val accent = if (mine) Color(0xFF72E0C4) else Color(0xFF8EC5FF)
    val bg = if (mine) Color(0xFF10281E) else Color(0xFF111B27)
    val infoAccent = Color(0xFFFFD36F)
    val weatherAccent = Color(0xFF8EC5FF)
    val terrainAccent = Color(0xFF67D6B1)
    val trackingAccent = Color(0xFF6AB7FF)
    val messagesAccent = Color(0xFF7FE2D1)
    val materialsAccent = Color(0xFF8EC5FF)
    val manageAccent = Color(0xFFD7F66F)
    val infoCtaAvailable = onTransport != null || onSignup != null
    val infoCtaLabel = when {
        onTransport != null && onSignup != null -> language.pick("Prijave i prijevoz", "Signups and transport")
        onSignup != null -> language.pick("Prijavi se i prijevoz", "Sign up and transport")
        onTransport != null -> language.pick("Pregled prijava i prijevoza", "View signups and transport")
        else -> language.pick("Prijave i prijevoz", "Signups and transport")
    }
    val infoCtaHint = when {
        onTransport != null -> language.pick(
            "Otvara popis prijavljenih, vozača i gumb za tvoju prijavu.",
            "Opens participants, drivers, and your signup button."
        )
        onSignup != null -> language.pick(
            "Otvara tvoju prijavu s izborom prijevoza.",
            "Opens your signup with transport choice."
        )
        else -> language.pick("Prijave trenutno nisu dostupne.", "Signups are not available right now.")
    }

    @Composable
    fun SectionShell(
        title: String,
        subtitle: String,
        icon: ImageVector,
        sectionAccent: Color,
        sectionBg: Color,
        content: @Composable () -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = sectionBg,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, sectionAccent.copy(alpha = 0.26f))
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FieldPackageIconBadge(icon, sectionAccent, sectionAccent.copy(alpha = 0.13f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                        if (subtitle.isNotBlank()) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.62f)
                            )
                        }
                    }
                }
                content()
            }
        }
    }

    if (showSheetDeleteConfirm && onDeleteFromSheet != null) {
        AlertDialog(
            onDismissRequest = { showSheetDeleteConfirm = false },
            title = { Text(language.pick("Obriši?", "Delete?")) },
            text = {
                Text(
                    language.pick(
                        "Izlet ${trip.location.ifBlank { "iz rasporeda" }} (${trip.date.ifBlank { "bez datuma" }}) bit će obrisan. Nastaviti?",
                        "Trip ${trip.location.ifBlank { "from schedule" }} (${trip.date.ifBlank { "no date" }}) will be deleted. Continue?"
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSheetDeleteConfirm = false
                    onDeleteFromSheet()
                }) {
                    Text(language.pick("Obriši", "Delete"), color = Color(0xFFFFA0A0), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSheetDeleteConfirm = false }) { Text(language.pick("Odustani", "Cancel")) }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = if (mine) 0.34f else 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { tripExpanded = !tripExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FieldPackageIconBadge(Icons.Default.Event, accent, if (mine) Color(0xFF163425) else Color(0xFF17334A))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = accent.copy(alpha = 0.15f),
                            contentColor = accent,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.26f))
                        ) {
                            Text(
                                if (mine) language.pick("MOJ", "MINE") else "CLOUD",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            trip.date.ifBlank { language.pick("bez datuma", "no date") },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.70f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        trip.location.ifBlank { language.pick("Izlet iz rasporeda", "Scheduled trip") },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Text(
                        language.pick("Cilj: ", "Goal: ") + trip.goal.ifBlank { "—" } + "  •  " +
                            language.pick("Voditelj: ", "Leader: ") + trip.leader.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    if (tripExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (tripExpanded) language.pick("Sakrij izlet", "Collapse trip") else language.pick("Prikaži izlet", "Expand trip"),
                    tint = Color.White.copy(alpha = 0.76f)
                )
            }

            if (trip.participants.isNotBlank() || trip.drivers.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (trip.participants.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFFFC46B).copy(alpha = 0.10f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFC46B).copy(alpha = 0.20f))
                        ) {
                            Text(
                                "👥 " + trip.participants,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.76f),
                                maxLines = if (tripExpanded) 2 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (trip.drivers.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF72E0C4).copy(alpha = 0.09f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF72E0C4).copy(alpha = 0.18f))
                        ) {
                            Text(
                                "🚗 " + trip.drivers,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.76f),
                                maxLines = if (tripExpanded) 2 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else if (!tripExpanded) {
                Text(
                    language.pick(
                        "Dodirni za opis, prognozu, teren, materijale i prijave.",
                        "Tap for description, forecast, field teams, materials, and signups."
                    ),
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.50f)
                )
            }

            if (tripExpanded) {
                SectionShell(
                    title = language.pick("Info i prijave", "Info and signups"),
                    subtitle = language.pick("Opis, prognoza i jedan ulaz za prijavu/prijevoz.", "Description, forecast, and one signup/transport entry."),
                    icon = Icons.Default.WbSunny,
                    sectionAccent = infoAccent,
                    sectionBg = Color(0xFF211C10)
                ) {
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
                                        maxLines = if (descExpanded) Int.MAX_VALUE else 3,
                                        overflow = if (descExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        if (descExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (descExpanded) language.pick("Zatvori opis", "Collapse description") else language.pick("Proširi opis", "Expand description"),
                                        tint = Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.size(18.dp).padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            language.pick("Opis nije upisan.", "No description entered."),
                            color = Color.White.copy(alpha = 0.58f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (canShowWeatherSection) {
                        Surface(
                            color = Color(0xFF0B1824),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, weatherAccent.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(language.pick("Prognoza", "Forecast"), fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.92f))
                                    TextButton(
                                        onClick = { scope.launch { loadTripWeather(force = true) } },
                                        enabled = canFetchWeather && !weatherLoading
                                    ) {
                                        Text(language.pick("Osvježi", "Refresh"), color = weatherAccent, fontSize = 12.sp)
                                    }
                                }

                                if (!canFetchWeather) {
                                    Text(
                                        if (weatherTooFarAhead) {
                                            language.pick(
                                                "Prognoza je dostupna tek oko 16 dana prije izleta.",
                                                "Forecast is available about 16 days before the trip."
                                            )
                                        } else {
                                            language.pick(
                                                "Za prognozu dodaj grad/regiju ili lokaciju izleta.",
                                                "For forecast, add a city/region or trip location."
                                            )
                                        },
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.62f)
                                    )
                                } else if (weatherLoading) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = weatherAccent)
                                        Spacer(Modifier.size(8.dp))
                                        Text(language.pick("Učitavam prognozu…", "Loading forecast…"), fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f))
                                    }
                                } else if (weatherResult != null && weatherResult!!.days.isNotEmpty()) {
                                    FieldWeatherCard(weather = weatherResult!!)
                                } else {
                                    Text(
                                        weatherError ?: language.pick(
                                            "Prognoza će se učitati nakon otvaranja izleta.",
                                            "Forecast will load after opening the trip."
                                        ),
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.62f)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            when {
                                onTransport != null -> onTransport.invoke()
                                onSignup != null -> onSignup.invoke()
                            }
                        },
                        enabled = infoCtaAvailable,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = infoAccent, contentColor = Color(0xFF241A00))
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = "Prijevoz", modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(infoCtaLabel, fontWeight = FontWeight.Bold)
                    }
                    Text(infoCtaHint, color = Color.White.copy(alpha = 0.56f), style = MaterialTheme.typography.bodySmall)
                }

                SectionShell(
                    title = language.pick("Teren", "Field"),
                    subtitle = language.pick("Ekipe, tracking i poruke ekipe na jednom mjestu.", "Teams, tracking, and team messages in one place."),
                    icon = Icons.Default.Terrain,
                    sectionAccent = terrainAccent,
                    sectionBg = Color(0xFF071A17)
                ) {
                    FieldTripTeamsCard(trip = trip)
                    FieldTrackingLiteTripCard(
                        trip = trip,
                        showMessagesEntry = false,
                        accent = trackingAccent,
                        containerColor = Color(0xFF071722)
                    )
                    FieldTripMessagesEntry(
                        trip = trip,
                        accent = messagesAccent,
                        containerColor = Color(0xFF0C211D)
                    )
                }

                SectionShell(
                    title = language.pick("Materijali", "Materials"),
                    subtitle = language.pick(
                        "Cloud paketi imaju zaseban plavi toggle unutar ove sekcije.",
                        "Cloud packages have a separate blue toggle inside this section."
                    ),
                    icon = Icons.Default.FolderOpen,
                    sectionAccent = materialsAccent,
                    sectionBg = Color(0xFF081926)
                ) {
                    TripAssetsCloudCard(
                        trip = trip,
                        localPackage = localPackage,
                        records = records,
                        markedPoints = markedPoints,
                        savedTracks = savedTracks
                    )
                }

                if (mine) {
                    SectionShell(
                        title = language.pick("Upravljanje", "Management"),
                        subtitle = language.pick("Administracija izleta odvojena od podataka za teren.", "Trip administration separated from field data."),
                        icon = Icons.Default.Edit,
                        sectionAccent = manageAccent,
                        sectionBg = Color(0xFF172007)
                    ) {
                        Button(
                            onClick = { sendTripAnnouncementMail(context, trip) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = manageAccent, contentColor = Color(0xFF132000))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Podijeli", modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(language.pick("Pošalji najavu", "Send announcement"), fontWeight = FontWeight.Bold)
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onEdit != null) {
                                OutlinedButton(
                                    onClick = onEdit,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.40f))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Uredi", modifier = Modifier.size(18.dp), tint = accent)
                                    Spacer(Modifier.size(8.dp))
                                    Text(language.pick("Uredi", "Edit"), color = Color.White)
                                }
                            }
                            if (onDeleteFromSheet != null) {
                                OutlinedButton(
                                    onClick = { showSheetDeleteConfirm = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFA0A0).copy(alpha = 0.42f))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Obriši", modifier = Modifier.size(18.dp), tint = Color(0xFFFFA0A0))
                                    Spacer(Modifier.size(8.dp))
                                    Text(language.pick("Obriši", "Delete"), color = Color(0xFFFFD0D0), fontWeight = FontWeight.SemiBold)
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
private fun TripAssetsCloudCard(
    trip: FieldPackageSheetTrip,
    localPackage: FieldPackageSummary?,
    records: List<SpeleoRecord>,
    markedPoints: List<MarkedPoint>,
    savedTracks: List<SavedTrack>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tripId = trip.cloudId.trim()
    var expanded by rememberSaveable(tripId) { mutableStateOf(false) }
    var assets by remember(tripId) { mutableStateOf<List<TripCloudAsset>>(emptyList()) }
    var loading by remember(tripId) { mutableStateOf(false) }
    var message by remember(tripId) { mutableStateOf<String?>(null) }

    fun refreshAssets() {
        if (tripId.isBlank()) return
        scope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) { runCatching { TripAssetCloudRepository.listAssets(context, tripId) } }
            result.onSuccess {
                assets = it
                message = if (it.isEmpty()) "Nema zajedničkih paketa za ovaj izlet." else null
            }.onFailure {
                message = "Greška učitavanja paketa: ${it.message ?: "nepoznato"}"
            }
            loading = false
        }
    }

    LaunchedEffect(tripId) { if (tripId.isNotBlank()) refreshAssets() }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF091E22), contentColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF8EC5FF).copy(alpha = 0.24f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FieldPackageIconBadge(Icons.Default.FolderOpen, Color(0xFF8EC5FF), Color(0xFF17334A))
                Column(Modifier.weight(1f)) {
                    Text("Paketi za teren", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Offline karta, GPX/KML i TopoDroid za sve članove izleta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.66f)
                    )
                }
                Surface(
                    color = Color(0xFF8EC5FF).copy(alpha = 0.10f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color(0xFF8EC5FF).copy(alpha = 0.22f))
                ) {
                    Text(
                        if (assets.isEmpty()) "PAKET" else "${assets.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCDE8FF)
                    )
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Akcija", tint = Color.White.copy(alpha = 0.70f))
            }

            if (expanded) {
                Surface(
                    color = Color(0xFFFFD36F).copy(alpha = 0.10f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD36F).copy(alpha = 0.20f))
                ) {
                    Text(
                        "Kad izlet završi, zajednički paketi se skrivaju iz aktivnog prikaza. Ako je paket već lokalno preuzet i checksum je isti, app ga ne skida ponovno nego ga otvara offline.",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFE6AD)
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !loading && tripId.isNotBlank(),
                        onClick = { refreshAssets() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF8EC5FF).copy(alpha = 0.35f))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Osvježi", modifier = Modifier.size(16.dp), tint = Color(0xFFCDE8FF))
                        Spacer(Modifier.width(6.dp))
                        Text("Osvježi", color = Color(0xFFCDE8FF))
                    }
                    Button(
                        enabled = !loading && tripId.isNotBlank() && localPackage != null,
                        onClick = {
                            val pkg = localPackage ?: return@Button
                            scope.launch {
                                loading = true
                                message = "Pripremam paket izleta…"
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val file = FieldPackageManager.exportPackage(context, pkg, records, markedPoints, savedTracks)
                                        TripAssetCloudRepository.uploadPackage(
                                            context = context,
                                            tripId = tripId,
                                            file = file,
                                            title = pkg.name.ifBlank { trip.location.ifBlank { "Paket izleta" } },
                                            description = "Paket izleta"
                                        )
                                    }
                                }
                                result.onSuccess {
                                    message = "Paket objavljen za ekipu: ${it.title} (${it.sizeLabel})"
                                    refreshAssets()
                                }.onFailure {
                                    message = "Upload nije uspio: ${it.message ?: "nepoznato"}"
                                }
                                loading = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8EC5FF), contentColor = Color(0xFF06151B))
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Uvoz datoteke", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Objavi paket", fontWeight = FontWeight.Bold)
                    }
                }

                if (loading) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF8EC5FF))
                        Spacer(Modifier.width(8.dp))
                        Text("Radim…", color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (assets.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        assets.forEach { asset ->
                            Surface(
                                color = Color.White.copy(alpha = 0.055f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            val downloaded = remember(asset.id, asset.sizeBytes, asset.checksumSha256) { TripAssetCloudRepository.isAssetDownloaded(context, asset) }
                                            Text(asset.title.ifBlank { asset.originalFilename.ifBlank { "Paket izleta" } }, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${asset.assetType.uppercase(Locale.getDefault())} · ${asset.sizeLabel} · v${asset.packageVersion}", color = Color.White.copy(alpha = 0.60f), style = MaterialTheme.typography.bodySmall)
                                            Text(if (downloaded) "Offline spremno — već je na uređaju" else "Nije preuzeto na ovaj uređaj", color = if (downloaded) Color(0xFFB8FFD0) else Color(0xFFFFE6AD), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (asset.description.isNotBlank()) {
                                        Text(asset.description, color = Color.White.copy(alpha = 0.64f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Button(
                                        enabled = !loading,
                                        onClick = {
                                            scope.launch {
                                                loading = true
                                                message = "Preuzimam paket…"
                                                val result = withContext(Dispatchers.IO) {
                                                    runCatching {
                                                        val file = TripAssetCloudRepository.downloadAsset(context, asset)
                                                        val imported = if (file.extension.equals("sovpkg", ignoreCase = true) || file.name.endsWith(".zip", ignoreCase = true)) {
                                                            FieldPackageManager.importPackage(context, Uri.fromFile(file))
                                                        } else null
                                                        file to imported
                                                    }
                                                }
                                                result.onSuccess { (_, imported) ->
                                                    if (imported != null) {
                                                        val next = listOf(imported) + FieldPackageManager.list(context).filterNot { it.id == imported.id }
                                                        FieldPackageManager.save(context, next)
                                                        message = "Paket preuzet i otvoren lokalno: ${imported.name}"
                                                    } else {
                                                        message = "Paket preuzet."
                                                    }
                                                }.onFailure {
                                                    message = "Preuzimanje nije uspjelo: ${it.message ?: "nepoznato"}"
                                                }
                                                loading = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF72E0C4), contentColor = Color(0xFF06130F))
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = "Otvori mapu", modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (TripAssetCloudRepository.isAssetDownloaded(context, asset)) "Otvori offline" else "Skini i otvori", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (!message.isNullOrBlank()) {
                    Text(
                        message.orEmpty(),
                        color = if (message!!.contains("Greška", true) || message!!.contains("nije uspio", true)) Color(0xFFFFA0A0) else Color(0xFFB8FFD0),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}



@Composable
private fun FieldTripHubCard(
    trip: FieldPackageSheetTrip,
    localPackage: FieldPackageSummary?,
    records: List<SpeleoRecord>,
    markedPoints: List<MarkedPoint>,
    savedTracks: List<SavedTrack>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(SovFieldHubClient.loadSettings(context)) }
    var roster by remember { mutableStateOf(SovFieldHubClient.loadCachedRoster(context)) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val matchingTeams = remember(roster, trip.cloudId, trip.date, trip.location) {
        SovFieldHubClient.cachedTeamsForTrip(context, trip)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C2A), contentColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF8EC5FF).copy(alpha = 0.24f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Laptop hub", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        settings.normalizedBaseUrl.ifBlank { "Adresa/PIN se postavljaju gore u kartici Laptop hub." },
                        color = Color.White.copy(alpha = 0.58f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = Color(0xFF8EC5FF).copy(alpha = 0.10f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color(0xFF8EC5FF).copy(alpha = 0.25f))
                ) {
                    Text("LOCAL", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCDE8FF))
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { runCatching { SovFieldHubClient.fetchRoster(context, settings) } }
                            result.onSuccess {
                                roster = it
                                message = "Ekipe povučene s laptopa."
                            }.onFailure {
                                message = "Ne mogu povući ekipe: ${it.message ?: "nepoznata greška"}"
                            }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF8EC5FF).copy(alpha = 0.40f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Osvježi", modifier = Modifier.size(16.dp), tint = Color(0xFFCDE8FF))
                    Spacer(Modifier.width(6.dp))
                    Text("Povuci", color = Color(0xFFCDE8FF), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        val pkg = localPackage
                        if (pkg == null) {
                            message = "Nema lokalnog paketa za ovaj izlet."
                        } else {
                            busy = true
                            message = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val file = FieldPackageManager.exportPackage(context, pkg, records, markedPoints, savedTracks)
                                        SovFieldHubClient.uploadPackage(settings, file)
                                    }
                                }
                                message = result.getOrElse { SovFieldHubUploadResult(false, it.message ?: "Slanje nije uspjelo.") }.message
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8EC5FF), contentColor = Color(0xFF06121F))
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Uvoz datoteke", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Na laptop", fontWeight = FontWeight.Bold)
                }
            }

            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF8EC5FF))

            if (matchingTeams.isEmpty()) {
                Text(
                    "Nema ekipa. Klikni Povuci.",
                    color = Color.White.copy(alpha = 0.58f),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                matchingTeams.forEach { team ->
                    Surface(
                        color = Color.White.copy(alpha = 0.055f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(team.name.ifBlank { "Ekipa" }, fontWeight = FontWeight.Bold, color = Color.White)
                            if (team.leaderName.isNotBlank()) Text("Voditelj: ${team.leaderName}", color = Color.White.copy(alpha = 0.70f), style = MaterialTheme.typography.bodySmall)
                            if (team.membersText.isNotBlank()) Text(team.membersText, color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.bodySmall)
                            if (team.note.isNotBlank()) Text("Napomena: ${team.note}", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            message?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = if (it.contains("ne mogu", true) || it.contains("nije", true) || it.contains("HTTP", true)) Color(0xFFFFA0A0) else Color(0xFFB8FFD0),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FieldTripTeamsCard(trip: FieldPackageSheetTrip) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var teams by remember(trip.cloudId) { mutableStateOf<List<FieldPackageTripTeam>>(emptyList()) }
    var loading by remember(trip.cloudId) { mutableStateOf(false) }
    var message by remember(trip.cloudId) { mutableStateOf<String?>(null) }
    var editingTeam by remember { mutableStateOf<FieldPackageTripTeam?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var teamName by remember { mutableStateOf("") }
    var teamLeader by remember { mutableStateOf("") }
    var teamMembers by remember { mutableStateOf("") }
    var teamNote by remember { mutableStateOf("") }

    fun openEditor(team: FieldPackageTripTeam?) {
        editingTeam = team
        teamName = team?.name.orEmpty().ifBlank { "Ekipa" }
        teamLeader = team?.leaderName.orEmpty()
        teamMembers = team?.membersText.orEmpty()
        teamNote = team?.note.orEmpty()
        showEditor = true
    }

    fun refreshTeams() {
        if (trip.cloudId.isBlank()) return
        scope.launch {
            loading = true
            message = null
            val result = withContext(Dispatchers.IO) { runCatching { FieldPackageSheetSyncClient.fetchTripTeams(trip) } }
            result.onSuccess { teams = it }.onFailure { message = "Ekipe nisu dostupne." }
            loading = false
        }
    }

    LaunchedEffect(trip.cloudId) { refreshTeams() }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingTeam == null) "Dodaj ekipu" else "Uredi ekipu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = teamName, onValueChange = { teamName = it }, label = { Text("Naziv ekipe") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = teamLeader, onValueChange = { teamLeader = it }, label = { Text("Voditelj ekipe") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = teamMembers, onValueChange = { teamMembers = it }, label = { Text("Članovi") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = teamNote, onValueChange = { teamNote = it }, label = { Text("Napomena") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val draft = FieldPackageTripTeam(
                        id = editingTeam?.id.orEmpty(),
                        tripId = trip.cloudId,
                        name = teamName.trim().ifBlank { "Ekipa" },
                        leaderName = teamLeader.trim(),
                        membersText = teamMembers.trim(),
                        note = teamNote.trim(),
                        sortOrder = editingTeam?.sortOrder ?: teams.size
                    )
                    scope.launch {
                        loading = true
                        val ok = withContext(Dispatchers.IO) { FieldPackageSheetSyncClient.saveTripTeam(trip, draft) }
                        loading = false
                        if (ok) {
                            showEditor = false
                            Toast.makeText(context, "Ekipa spremljena", Toast.LENGTH_SHORT).show()
                            refreshTeams()
                        } else {
                            message = "Spremanje ekipe nije uspjelo."
                        }
                    }
                }) { Text("Spremi") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Odustani") } }
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF081D18), contentColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF83E6C2).copy(alpha = 0.22f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ekipe", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${teams.size} ekip${if (teams.size == 1) "a" else "e"}", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    enabled = trip.cloudId.isNotBlank() && !loading,
                    onClick = { openEditor(null) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF83E6C2), contentColor = Color(0xFF07120F))
                ) { Text("+ Ekipa", fontWeight = FontWeight.Bold) }
            }

            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (trip.cloudId.isBlank()) {
                Text("Izlet još nema cloud ID.", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
            } else if (teams.isEmpty() && !loading) {
                Text("Nema ekipa.", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
            } else {
                teams.sortedWith(compareBy<FieldPackageTripTeam> { it.sortOrder }.thenBy { it.name }).forEach { team ->
                    Surface(
                        color = Color.White.copy(alpha = 0.055f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text(team.name.ifBlank { "Ekipa" }, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (team.leaderName.isNotBlank()) Text("Voditelj: ${team.leaderName}", color = Color.White.copy(alpha = 0.70f), style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(onClick = { openEditor(team) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Edit, contentDescription = "Uredi", tint = Color(0xFFFFD27A)) }
                                    IconButton(onClick = {
                                        scope.launch {
                                            loading = true
                                            val ok = withContext(Dispatchers.IO) { FieldPackageSheetSyncClient.deleteTripTeam(team) }
                                            loading = false
                                            if (ok) refreshTeams() else message = "Brisanje ekipe nije uspjelo."
                                        }
                                    }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, contentDescription = "Obriši", tint = Color(0xFFFFA0A0)) }
                                }
                            }
                            if (team.membersText.isNotBlank()) Text(team.membersText, color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.bodySmall)
                            if (team.note.isNotBlank()) Text("Napomena: ${team.note}", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (!message.isNullOrBlank()) Text(message.orEmpty(), color = Color(0xFFFFA0A0), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FieldTrackingLiteTripCard(
    trip: FieldPackageSheetTrip,
    showMessagesEntry: Boolean = true,
    accent: Color = Color(0xFF6AB7FF),
    containerColor: Color = Color(0xFF071722)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var teamName by rememberSaveable(trip.cloudId, trip.location, trip.date) {
        mutableStateOf(listOf(trip.location.ifBlank { "SOV teren" }, trip.date).filter { it.isNotBlank() }.joinToString(" · "))
    }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var selectedTrackingMode by rememberSaveable { mutableStateOf("lite") }
    val tripId = trip.cloudId.trim()

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = Color.White),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tracking", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Naziv teama, mod praćenja i join kod.",
                        color = Color.White.copy(alpha = 0.58f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(
                    color = accent.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
                ) {
                    Text("TRACK", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD6EEFF))
                }
            }

            if (tripId.isBlank()) {
                Text("Tracking radi za Cloud izlete.", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
            } else {
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Naziv teama / terena") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Preporučeni mod za team", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTrackingMode != "route",
                        onClick = { selectedTrackingMode = "lite" },
                        label = { Text("Lite auto ping") }
                    )
                    FilterChip(
                        selected = selectedTrackingMode == "route",
                        onClick = { selectedTrackingMode = "route" },
                        label = { Text("Ruta / GPX") }
                    )
                }
                Text(
                    if (selectedTrackingMode == "route") "Ruta/GPX: gušći trag za trail i GPX export." else "Lite: rjeđi auto ping da se zna gdje je tko, uz manju potrošnju.",
                    color = Color.White.copy(alpha = 0.66f),
                    style = MaterialTheme.typography.bodySmall
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                message = null
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        FieldTrackingLiteApi.createFieldEvent(
                                            context = context,
                                            sourceTripId = tripId,
                                            title = teamName.ifBlank { trip.location.ifBlank { "SOV teren" } },
                                            location = trip.location,
                                            trackingMode = selectedTrackingMode
                                        )
                                    }
                                }
                                result
                                    .onSuccess { event -> message = "Team otvoren. Kod ekipe: ${event.joinCode.ifBlank { "—" }}. Tracking uključi na Karti → Team." }
                                    .onFailure { message = "Greška: ${FieldTrackingLiteApi.friendlyError(it)}" }
                                busy = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF07120F))
                    ) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF07120F)) else Text("Formiraj team", fontWeight = FontWeight.Bold)
                    }
                }

                if (showMessagesEntry) {
                    FieldTripMessagesEntry(trip = trip)
                }

                Divider(color = Color.White.copy(alpha = 0.08f))
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it.uppercase(Locale.getDefault()).take(12) },
                    label = { Text("Kod ekipe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedButton(
                    enabled = !busy && joinCode.isNotBlank(),
                    onClick = {
                        scope.launch {
                            busy = true
                            message = null
                            val result = withContext(Dispatchers.IO) { runCatching { FieldTrackingLiteApi.joinFieldEvent(context, joinCode) } }
                            result
                                .onSuccess { event -> message = "Pridružen si teamu: ${event.title}. Tracking uključi na Karti → Team." }
                                .onFailure { message = "Greška: ${FieldTrackingLiteApi.friendlyError(it)}" }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.40f))
                ) { Text("Pridruži se kodom") }
            }

            if (!message.isNullOrBlank()) {
                Text(
                    message.orEmpty(),
                    color = if (message!!.startsWith("Greška")) Color(0xFFFFA0A0) else Color(0xFFB8FFD0),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}



@Composable
private fun FieldTripMessagesEntry(
    trip: FieldPackageSheetTrip,
    accent: Color = Color(0xFF7FE2D1),
    containerColor: Color = Color(0xFF10231F)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tripId = trip.cloudId.trim()
    var open by rememberSaveable(tripId) { mutableStateOf(false) }
    var pendingCount by remember(tripId) { mutableStateOf(0) }
    var previewCount by remember(tripId) { mutableStateOf(0) }

    fun refreshBadge() {
        if (tripId.isBlank()) return
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    FieldPackageSheetSyncClient.pendingTripMessageCount(context, tripId) to
                        FieldPackageSheetSyncClient.fetchTripMessages(trip, limit = 20).size
                }
            }
            result.onSuccess { (pending, total) ->
                pendingCount = pending
                previewCount = total
            }
        }
    }

    LaunchedEffect(tripId) {
        if (tripId.isNotBlank()) refreshBadge()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(enabled = tripId.isNotBlank()) { open = true }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("💬 Poruke ekipe", fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    when {
                        tripId.isBlank() -> "Dostupno nakon synca izleta."
                        pendingCount > 0 -> "$pendingCount čeka signal"
                        previewCount > 0 -> "$previewCount poruka"
                        else -> "Svi / ekipa"
                    },
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Surface(
                color = if (pendingCount > 0) Color(0xFFFFC46B) else accent,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    if (pendingCount > 0) "!" else "Otvori",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color(0xFF07120F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (open) {
        FieldTripMessagesDialog(
            trip = trip,
            onDismiss = {
                open = false
                refreshBadge()
            }
        )
    }
}


@Composable
private fun FieldTripMessagesDialog(
    trip: FieldPackageSheetTrip,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tripId = trip.cloudId.trim()
    var messages by remember(tripId) { mutableStateOf<List<FieldPackageTripMessage>>(emptyList()) }
    var loading by remember(tripId) { mutableStateOf(false) }
    var status by remember(tripId) { mutableStateOf<String?>(null) }
    var targetScope by rememberSaveable(tripId) { mutableStateOf("team") }
    var draft by rememberSaveable(tripId) { mutableStateOf("") }

    fun refreshMessages() {
        if (tripId.isBlank()) return
        scope.launch {
            loading = true
            status = null
            val result = withContext(Dispatchers.IO) { runCatching { FieldPackageSheetSyncClient.fetchTripMessages(trip) } }
            result.onSuccess {
                messages = it
                status = if (it.isEmpty()) "Nema poruka." else null
            }.onFailure {
                status = "Ne mogu učitati poruke."
            }
            loading = false
        }
    }

    LaunchedEffect(tripId) {
        refreshMessages()
        while (true) {
            delay(60_000L)
            if (tripId.isNotBlank()) refreshMessages()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Zatvori") }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Poruke izleta", fontWeight = FontWeight.Bold)
                Text(trip.location.ifBlank { trip.goal.ifBlank { "Izlet" } }, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.62f))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetScope == "team",
                        onClick = { targetScope = "team" },
                        label = { Text("Moj team") }
                    )
                    FilterChip(
                        selected = targetScope == "all",
                        onClick = { targetScope = "all" },
                        label = { Text("Svi") }
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { refreshMessages() }, enabled = !loading) { Text("Osvježi") }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    color = Color.White.copy(alpha = 0.045f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    if (loading && messages.isEmpty()) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Učitavam…", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (messages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(status ?: "Nema poruka.", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages.takeLast(80)) { msg ->
                                FieldTripMessageBubble(msg)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(280) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    placeholder = { Text("Kratka poruka…") },
                    supportingText = { Text("${draft.length}/280") }
                )

                Button(
                    enabled = draft.trim().isNotEmpty() && !loading && tripId.isNotBlank(),
                    onClick = {
                        val text = draft.trim()
                        draft = ""
                        scope.launch {
                            loading = true
                            val sent = withContext(Dispatchers.IO) { FieldPackageSheetSyncClient.sendTripMessage(trip, targetScope, text) }
                            status = if (sent) null else "Čeka signal — poslat će se kasnije."
                            messages = withContext(Dispatchers.IO) { FieldPackageSheetSyncClient.fetchTripMessages(trip) }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF83E6C2), contentColor = Color(0xFF07120F))
                ) {
                    Text("Pošalji", fontWeight = FontWeight.Bold)
                }

                if (!status.isNullOrBlank()) {
                    Text(status.orEmpty(), color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

@Composable
private fun FieldTripMessageBubble(message: FieldPackageTripMessage) {
    val isAll = message.scope == "all"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (message.pending) Color(0xFFFFC46B).copy(alpha = 0.12f) else if (isAll) Color(0xFF8EC5FF).copy(alpha = 0.12f) else Color(0xFF83E6C2).copy(alpha = 0.11f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOf(message.senderName.ifBlank { "Član" }, if (isAll) "Svi" else "Team").joinToString(" · "),
                    color = Color.White.copy(alpha = 0.78f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(if (message.pending) "čeka" else formatTripMessageTime(message.createdAt.orEmpty()), color = Color.White.copy(alpha = 0.50f), fontSize = 11.sp)
            }
            Text(message.messageText, color = Color.White.copy(alpha = 0.90f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatTripMessageTime(value: String): String {
    val clean = value.trim()
    if (clean.isBlank()) return ""
    return runCatching {
        val normalized = clean.replace("T", " ").replace("Z", "")
        if (normalized.length >= 16) normalized.substring(8, 10) + "/" + normalized.substring(5, 7) + " " + normalized.substring(11, 16) else clean.take(16)
    }.getOrDefault(clean.take(16))
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
    var tripDateText by remember(pkg.id) { mutableStateOf(pkg.tripDateText.orEmpty()) }
    var organizer by remember(pkg.id) { mutableStateOf(pkg.organizer.orEmpty()) }
    var locationName by remember(pkg.id) { mutableStateOf(pkg.locationName.orEmpty()) }
    var goal by remember(pkg.id) { mutableStateOf(pkg.goal.orEmpty().ifBlank { "Izlet" }) }
    var description by remember(pkg.id) { mutableStateOf(pkg.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        pkg.copy(
                            name = buildFieldPackageTitle(locationName, goal),
                            tripDateText = tripDateText.trim(),
                            organizer = organizer.trim(),
                            locationName = locationName.trim(),
                            weatherCity = pkg.weatherCity.orEmpty().ifBlank { locationName.trim() },
                            goal = goal.trim().ifBlank { "Izlet" },
                            description = description.trim()
                        )
                    )
                },
                enabled = tripDateText.trim().isNotEmpty() && organizer.trim().isNotEmpty() && locationName.trim().isNotEmpty()
            ) { Text("Spremi izmjene") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
        title = { Text("Uredi izlet", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tripDateText,
                    onValueChange = { tripDateText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Datum") },
                    placeholder = { Text("dd/mm/yyyy ili dd/mm/yyyy – dd/mm/yyyy") },
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
                FieldGoalPicker(goal = goal, onGoalChanged = { goal = it })
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().height(92.dp),
                    label = { Text("Opis") }
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
                            trackCount = 0,
                            topoDroidAttachmentCount = 0,
                            offlineMapName = null,
                            includesOfflineMap = false,
                            imported = false,
                            minLat = null,
                            maxLat = null,
                            minLon = null,
                            maxLon = null,
                            includeTracks = false,
                            selectedTrackIds = emptyList(),
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
        title = { Text(language.pick("Novi izlet", "New trip"), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

private const val FIELD_WEATHER_CONNECT_TIMEOUT_MS = 10_000
private const val FIELD_WEATHER_READ_TIMEOUT_MS = 12_000
private const val FIELD_WEATHER_MAX_FORECAST_DAYS = 16

private fun fieldWeatherDateRange(startMillis: Long, endMillis: Long): Pair<String, String>? {
    val now = System.currentTimeMillis()
    val todayStart = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val maxForecastEnd = todayStart + (FIELD_WEATHER_MAX_FORECAST_DAYS - 1L) * 24L * 60L * 60L * 1000L
    val clampedStart = startMillis.coerceAtLeast(todayStart)
    if (clampedStart > maxForecastEnd) return null
    val clampedEnd = endMillis.coerceAtLeast(clampedStart).coerceAtMost(maxForecastEnd)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date(clampedStart)) to sdf.format(Date(clampedEnd))
}

private fun openMeteoReadText(url: String): String? {
    val conn = SovNetworkSecurity.openHttpConnection(url, "Vremenska prognoza").apply {
        requestMethod = "GET"
        connectTimeout = FIELD_WEATHER_CONNECT_TIMEOUT_MS
        readTimeout = FIELD_WEATHER_READ_TIMEOUT_MS
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "SOV-Android")
    }
    return try {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code in 200..299 && body.isNotBlank()) body else null
    } catch (_: Exception) {
        null
    } finally {
        runCatching { conn.disconnect() }
    }
}

private fun weatherJsonArray(daily: com.google.gson.JsonObject, vararg keys: String): com.google.gson.JsonArray? {
    for (key in keys) {
        val element = daily.get(key)
        if (element != null && element.isJsonArray) return element.asJsonArray
    }
    return null
}

private fun fieldWeatherLocationCandidates(location: String): List<String> {
    val cleaned = location.trim()
        .replace(Regex("^(Croatia|Hrvatska)\\s*,?", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(.*?\\)"), " ")
        .replace(Regex("[;/|]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimEnd(',')
    if (cleaned.isBlank()) return emptyList()

    val parts = cleaned
        .split(",", " - ", " – ", ":")
        .map { it.trim() }
        .filter { it.length >= 2 }

    val normalized = normalizeWeatherCityQuery(cleaned)
    val suggestionMatches = WeatherCitySuggestions.filter { city ->
        val n = normalizeWeatherCityQuery(city)
        normalized.contains(n) || n.contains(normalized)
    }

    val areaFallbacks = buildList {
        if (normalized.contains("velebit") || normalized.contains("krasno") || normalized.contains("sjeverni velebit")) add("Gospić")
        if (normalized.contains("paklenica") || normalized.contains("starigrad")) add("Starigrad")
        if (normalized.contains("lika") || normalized.contains("lovinac") || normalized.contains("gracac")) add("Gračac")
        if (normalized.contains("plitvic") || normalized.contains("korenic")) add("Korenica")
        if (normalized.contains("gorski kotar") || normalized.contains("risnjak")) add("Delnice")
        if (normalized.contains("istra") || normalized.contains("buzet") || normalized.contains("pazin")) add("Pazin")
        if (normalized.contains("zumber") || normalized.contains("samobor")) add("Samobor")
        if (normalized.contains("kordun") || normalized.contains("slunj")) add("Slunj")
        if (normalized.contains("dalmatinska zagora") || normalized.contains("dinara")) add("Knin")
        if (normalized.contains("biokovo") || normalized.contains("makarska")) add("Makarska")
    }

    val firstWords = cleaned.split(" ").take(2).joinToString(" ").trim()

    return (listOf(cleaned) + parts + suggestionMatches + areaFallbacks + firstWords)
        .map { it.trim() }
        .filter { it.length >= 2 }
        .distinctBy { normalizeWeatherCityQuery(it) }
}

internal suspend fun fetchFieldWeatherForLocation(
    location: String,
    startMillis: Long,
    endMillis: Long
): FieldWeatherResult? = withContext(Dispatchers.IO) {
    val candidates = fieldWeatherLocationCandidates(location)
    if (candidates.isEmpty()) return@withContext null

    for (candidate in candidates) {
        val queries = listOf(candidate, "$candidate, Croatia").distinct()
        for (queryText in queries) {
            try {
                val query = java.net.URLEncoder.encode(queryText, "UTF-8")
                val url = "https://geocoding-api.open-meteo.com/v1/search" +
                    "?name=$query&count=5&language=hr&format=json&countryCode=HR"
                val body = openMeteoReadText(url) ?: continue
                val json = com.google.gson.JsonParser.parseString(body).asJsonObject
                val results = json.getAsJsonArray("results") ?: continue
                if (results.size() == 0) continue

                val best = (0 until results.size())
                    .map { results[it].asJsonObject }
                    .firstOrNull { item ->
                        item.get("country_code")?.asString.equals("HR", ignoreCase = true) ||
                            item.get("country")?.asString.equals("Croatia", ignoreCase = true) ||
                            item.get("country")?.asString.equals("Hrvatska", ignoreCase = true)
                    } ?: results[0].asJsonObject

                val lat = best.get("latitude")?.asDouble ?: continue
                val lon = best.get("longitude")?.asDouble ?: continue
                val weather = fetchFieldWeather(lat, lon, startMillis, endMillis)
                if (weather != null && weather.days.isNotEmpty()) return@withContext weather
            } catch (_: Exception) {
                // Try the next cleaned location candidate.
            }
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
        val range = fieldWeatherDateRange(startMillis, endMillis) ?: return@withContext null
        val dailyParams = "temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max,weather_code"
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${lat}&longitude=${lon}" +
            "&daily=${dailyParams}" +
            "&timezone=auto" +
            "&start_date=${range.first}&end_date=${range.second}"

        val body = openMeteoReadText(url) ?: return@withContext null
        val json = com.google.gson.JsonParser.parseString(body).asJsonObject
        val daily = json.getAsJsonObject("daily") ?: return@withContext null
        val dates = weatherJsonArray(daily, "time") ?: return@withContext null
        val tempMax = weatherJsonArray(daily, "temperature_2m_max") ?: return@withContext null
        val tempMin = weatherJsonArray(daily, "temperature_2m_min") ?: return@withContext null
        val precip = weatherJsonArray(daily, "precipitation_sum") ?: return@withContext null
        val wind = weatherJsonArray(daily, "wind_speed_10m_max", "windspeed_10m_max") ?: return@withContext null
        val wmo = weatherJsonArray(daily, "weather_code", "weathercode") ?: return@withContext null

        val size = listOf(dates.size(), tempMax.size(), tempMin.size(), precip.size(), wind.size(), wmo.size()).minOrNull() ?: 0
        if (size <= 0) return@withContext null

        val days = (0 until size).map { i ->
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
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun FieldWeatherCard(weather: FieldWeatherResult) {
    val updatedAt = remember(weather.fetchedAtMillis) {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(weather.fetchedAtMillis))
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
                Icon(Icons.Default.WbSunny, contentDescription = "Ikona", tint = Color(0xFFFFC46B), modifier = Modifier.size(20.dp))
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
                Icon(Icons.Default.Map, contentDescription = "Karta", tint = premiumIconTint("field map", active = true), modifier = Modifier.size(30.dp))
                Column(Modifier.weight(1f)) {
                    Text("Karta i područje", fontWeight = FontWeight.Bold)
                    Text("Označi područje na karti.", color = Color.White.copy(alpha = 0.72f))
                }
            }
            Button(onClick = onFindAreaOnMap, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Map, contentDescription = "Karta", modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Izaberi područje na karti")
            }
            OutlinedButton(onClick = onUseGps, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.MyLocation, contentDescription = "Moja lokacija", modifier = Modifier.size(18.dp))
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
                Icon(Icons.Default.Route, contentDescription = "Ikona", tint = premiumIconTint("field tracks", active = includeTracks), modifier = Modifier.size(26.dp))
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
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Potvrđeno", modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = includeTracks && manualTrackSelection,
                    onClick = {
                        if (!includeTracks) onIncludeTracksChanged(true)
                        if (!manualTrackSelection) onToggleManual()
                    },
                    label = { Text("Ručno odaberi") },
                    leadingIcon = { Icon(Icons.Default.Route, contentDescription = "Ikona", modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = !includeTracks,
                    onClick = { onIncludeTracksChanged(false) },
                    label = { Text("Bez trackova") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Obriši", modifier = Modifier.size(16.dp)) }
                )
            }
            OutlinedButton(onClick = onImportTrack, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.UploadFile, contentDescription = "Uvoz datoteke", modifier = Modifier.size(18.dp))
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
                        { IconButton(onClick = { trackFilter = "" }) { Icon(Icons.Default.Clear, contentDescription = "Očisti", modifier = Modifier.size(16.dp)) } }
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
                        "Prvih 25 trackova.",
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
                Icon(Icons.Default.UploadFile, contentDescription = "Uvoz datoteke", tint = Color(0xFF8EC5FF), modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dodaci po želji", fontWeight = FontWeight.Bold)
                    Text(
                        "Karta, KML/GPX ili track nisu obavezni za izlet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Akcija", tint = Color(0xFF8EC5FF))
            }
            if (expanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = attachActiveMap,
                        onClick = { onAttachActiveMapChanged(!attachActiveMap) },
                        enabled = activeMapName != null,
                        label = { Text(activeMapName?.let { "Aktivna karta: $it" } ?: "Nema aktivne karte") },
                        leadingIcon = { Icon(Icons.Default.Map, contentDescription = "Karta", modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = includeSavedTracks,
                        onClick = { onIncludeTracksChanged(!includeSavedTracks) },
                        enabled = savedTracks.isNotEmpty(),
                        label = { Text(if (savedTracks.isEmpty()) "Nema spremljenih trackova" else "Spremljeni trackovi") },
                        leadingIcon = { Icon(Icons.Default.Route, contentDescription = "Ikona", modifier = Modifier.size(16.dp)) }
                    )
                    if (includeSavedTracks && savedTracks.isNotEmpty()) {
                        FilterChip(
                            selected = manualTrackSelection,
                            onClick = onToggleManual,
                            label = { Text(if (manualTrackSelection) "Ručno: ${selectedTrackIds.size}" else "Auto / bez ručnog odabira") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Potvrđeno", modifier = Modifier.size(16.dp)) }
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
                            Text("Zadnji trackovi.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text(
                    "GPX/KML dodaj iz detalja izleta.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FieldGoalPicker(goal: String, onGoalChanged: (String) -> Unit) {
    val goals = listOf("Izlet", "Seminar", "Skup", "Ekspedicija", "Inventura", "Skupština", "Predavanje", "Istraživanje", "Rekognosciranje")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Cilj izleta", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            goals.forEach { item ->
                FilterChip(
                    selected = goal == item,
                    onClick = { onGoalChanged(item) },
                    label = { Text(item) },
                    leadingIcon = if (goal == item) ({ Icon(Icons.Default.CheckCircle, contentDescription = "Potvrđeno", modifier = Modifier.size(16.dp)) }) else null
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
                Icon(Icons.Default.CalendarToday, contentDescription = "Kalendar", modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Početak: ${if (startMillis != null) formatFieldTripDate(startMillis) else "Odaberi"}")
            }
            OutlinedButton(onClick = { openPicker(endMillis ?: startMillis, onEndChanged) }, modifier = Modifier.weight(1f), enabled = startMillis != null) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Kalendar", modifier = Modifier.size(16.dp))
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
                    if (done) Icon(Icons.Default.CheckCircle, contentDescription = "Potvrđeno", modifier = Modifier.size(15.dp), tint = accent)
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


private fun sheetTripCategory(trip: FieldPackageSheetTrip): String {
    val raw = trip.tripCategory.ifBlank { trip.goal }.trim()
    val n = raw.lowercase(Locale.getDefault())
    return when {
        "seminar" in n -> "Seminar"
        "skupšt" in n || "skupst" in n -> "Skupština"
        "skup" in n -> "Skup"
        "eksp" in n -> "Ekspedicija"
        "invent" in n -> "Inventura"
        "pred" in n -> "Predavanje"
        else -> "Izlet"
    }
}

private fun sheetTripOverlapsMonth(trip: FieldPackageSheetTrip, monthMillis: Long): Boolean {
    val start = parseSheetTripStartMillis(trip.date) ?: return false
    val end = parseSheetTripEndMillis(trip.date) ?: start
    val monthStart = sovFieldStartOfMonth(monthMillis)
    val monthEnd = sovFieldShiftMonth(monthStart, 1) - 1L
    return end >= monthStart && start <= monthEnd
}

private fun sovFieldStartOfMonth(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun sovFieldShiftMonth(monthMillis: Long, delta: Int): Long = Calendar.getInstance().apply {
    timeInMillis = sovFieldStartOfMonth(monthMillis)
    add(Calendar.MONTH, delta)
}.timeInMillis

private fun sovFieldMonthTitle(millis: Long): String = SimpleDateFormat("MMMM yyyy", Locale("hr", "HR")).format(Date(millis)).replaceFirstChar { it.uppercase() }

private fun sheetTripIsOver(dateStr: String): Boolean {
    if (dateStr.isBlank()) return false
    val endMillis = parseSheetTripEndMillis(dateStr) ?: return false
    return endMillis < System.currentTimeMillis()
}

private fun formatFieldTripDate(millis: Long?): String {
    if (millis == null) return "odaberi"
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
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

private fun formatPackageDateShort(millis: Long): String = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))

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
