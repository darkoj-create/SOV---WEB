package com.darko.speleov1

import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darko.speleov1.model.SpeleoRecord
import com.darko.speleov1.util.OfflineTileManager
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

@Composable
fun MarkDialogPhotoPreview(
    uriString: String,
    label: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(uriString) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    Card(
        modifier = Modifier.width(136.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpen),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = label, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpen, contentPadding = PaddingValues(0.dp)) { Text("Open") }
                IconButton(onClick = onRemove) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Makni fotografiju") }
            }
        }
    }
}

enum class ZipImportKind {
    TILES,
    GENERIC_IMPORT,
    UNKNOWN
}

fun sniffZipImportKind(context: Context, uri: Uri): ZipImportKind {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var sawTilePath = false
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.replace('\\', '/').trimStart('/').lowercase(Locale.ROOT)
                        if (
                            name.endsWith(".shp") ||
                            name.endsWith(".dbf") ||
                            name.endsWith(".prj") ||
                            name.endsWith(".kml") ||
                            name.endsWith(".gpx") ||
                            name.endsWith(".geojson") ||
                            name.endsWith(".json") ||
                            name.endsWith(".csv") ||
                            name.endsWith(".xlsx") ||
                            name.endsWith(".xlsm") ||
                            name.endsWith(".gpkg") ||
                            name.endsWith(".geopackage") ||
                            name.endsWith(".tif") ||
                            name.endsWith(".tiff")
                        ) {
                            return ZipImportKind.GENERIC_IMPORT
                        }
                        if (looksLikeTileZipEntry(name)) {
                            sawTilePath = true
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                if (sawTilePath) ZipImportKind.TILES else ZipImportKind.UNKNOWN
            }
        } ?: ZipImportKind.UNKNOWN
    }.getOrDefault(ZipImportKind.UNKNOWN)
}

fun looksLikeTileZipEntry(name: String): Boolean {
    val parts = name.split('/').filter { it.isNotBlank() }
    if (parts.size < 3) return false
    val zIndex = parts.indexOfFirst { it.all(Char::isDigit) }
    if (zIndex == -1 || zIndex + 2 >= parts.size) return false
    val z = parts[zIndex]
    val x = parts[zIndex + 1]
    val yWithExt = parts[zIndex + 2]
    val y = yWithExt.substringBefore('.')
    val ext = yWithExt.substringAfter('.', "").lowercase(Locale.ROOT)
    if (!z.all(Char::isDigit) || !x.all(Char::isDigit) || !y.all(Char::isDigit)) return false
    return ext == "png" || ext == "jpg" || ext == "jpeg" || ext == "webp"
}



fun filterRecordsForFieldPackage(records: List<SpeleoRecord>, pkg: FieldPackageSummary): List<SpeleoRecord> {
    return records.filter { rec ->
        if (!isSovSourceRecord(rec)) return@filter false
        val lat = rec.location.lat ?: return@filter false
        val lon = rec.location.lon ?: return@filter false
        isInsideFieldPackage(lat, lon, pkg)
    }
}

fun isSovSourceRecord(record: SpeleoRecord): Boolean {
    val source = record.source?.trim()?.lowercase(Locale.ROOT)
    return source == "sov" || source == "both" || record.source_labels.orEmpty().any { it.trim().lowercase(Locale.ROOT) == "sov" }
}

fun filterMarkedPointsForFieldPackage(points: List<MarkedPoint>, pkg: FieldPackageSummary): List<MarkedPoint> {
    return points.filter { point -> isInsideFieldPackage(point.lat, point.lon, pkg) }
}

fun filterSavedTracksForFieldPackage(tracks: List<SavedTrack>, pkg: FieldPackageSummary): List<SavedTrack> {
    if (pkg.includeTracks == false) return emptyList()
    val selectedIds = pkg.selectedTrackIds.orEmpty().toSet()
    if (selectedIds.isNotEmpty()) return tracks.filter { it.id in selectedIds }
    return tracks.filter { track -> track.points.any { isInsideFieldPackage(it.point.latitude, it.point.longitude, pkg) } }
}

fun filterImportedLayersForFieldPackage(layers: List<ImportedLayer>, pkg: FieldPackageSummary): List<ImportedLayer> {
    return layers.mapNotNull { layer ->
        val filteredPoints = layer.points.filter { point -> isInsideFieldPackage(point.lat, point.lon, pkg) }
        val filteredTracks = layer.tracks.filter { track ->
            track.points.any { trackPoint ->
                isInsideFieldPackage(trackPoint.point.latitude, trackPoint.point.longitude, pkg)
            }
        }
        if (filteredPoints.isEmpty() && filteredTracks.isEmpty()) {
            null
        } else {
            layer.copy(points = filteredPoints, tracks = filteredTracks, visible = true)
        }
    }
}

fun isInsideFieldPackage(lat: Double, lon: Double, pkg: FieldPackageSummary): Boolean {
    val minLat = pkg.minLat
    val maxLat = pkg.maxLat
    val minLon = pkg.minLon
    val maxLon = pkg.maxLon
    if (minLat != null && maxLat != null && minLon != null && maxLon != null) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon
    }
    val centerLat = pkg.centerLat
    val centerLon = pkg.centerLon
    if (centerLat != null && centerLon != null && pkg.radiusKm > 0.0) {
        return distanceKmForFieldPackage(centerLat, centerLon, lat, lon) <= pkg.radiusKm
    }
    return false
}

fun distanceKmForFieldPackage(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}


fun suggestedKmlExportName(exportMode: String): String = when (exportMode) {
    "not_in_cadastre" -> "sov_not_in_cadastre"
    "custom_points" -> "sov_custom_points"
    else -> "sov_export"
}

fun writeTextToOfflinePublicFolder(
    context: Context,
    folder: File,
    displayName: String,
    extension: String,
    content: String
) {
    runCatching {
        OfflineTileManager.ensurePublicOfflineFolderStructure()
        folder.mkdirs()
        val baseName = sanitizeOfflineFileNameForBackup(displayName.substringBeforeLast('.'), extension)
        val fileName = if (baseName.endsWith("." + extension, ignoreCase = true)) baseName else "$baseName.$extension"
        File(folder, fileName).writeText(content)
    }
}

fun backupImportToOfflineFolder(context: Context, uri: Uri, displayName: String) {
    val extension = displayName.substringAfterLast('.', "import").lowercase(Locale.ROOT)
    val internalRoots = when (extension) {
        "gpx" -> listOf(OfflineTileManager.gpxRoot(context))
        "kml", "kmz" -> listOf(OfflineTileManager.kmlRoot(context))
        "mbtiles", "pmtiles" -> listOf(OfflineTileManager.mbtilesRoot(context), OfflineTileManager.mapsPackageRoot(context))
        else -> emptyList()
    }
    val targetRoots = internalRoots + listOf(OfflineTileManager.publicRootForExtension(extension))
    runCatching {
        OfflineTileManager.ensurePublicOfflineFolderStructure()
        val baseName = displayName.substringBeforeLast('.', displayName)
        val safeName = sanitizeOfflineFileNameForBackup(baseName, "import") + "." + extension
        val bytes = context.contentResolver.openInputStream(uri)?.use { input -> input.readBytes() } ?: return@runCatching
        targetRoots.distinctBy { it.absolutePath }.forEach { targetRoot ->
            targetRoot.mkdirs()
            FileOutputStream(File(targetRoot, safeName)).use { output -> output.write(bytes) }
        }
    }
}

fun sanitizeOfflineFileNameForBackup(raw: String, fallback: String): String =
    raw.ifBlank { fallback }
        .replace(Regex("[\\/:*?\"<>|]"), "_")
        .replace(Regex("\\s+"), "_")
        .trim('_')
        .ifBlank { fallback }
