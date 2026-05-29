package com.darko.speleov1

import com.darko.speleov1.model.SourceFilter
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Intent
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.provider.OpenableColumns
import android.Manifest
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Settings as SettingsGearIcon
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darko.speleov1.model.CadastreFilter
import com.darko.speleov1.model.CaveTypeFilter
import com.darko.speleov1.model.FilterState
import com.darko.speleov1.model.SpeleoRecord
import com.darko.speleov1.ui.theme.SpeleoTheme
import com.darko.speleov1.util.CoordinateConverter
import com.darko.speleov1.util.KmlExporter
import com.darko.speleov1.util.LocationHelper
import com.darko.speleov1.util.LocalTileOverlay
import com.darko.speleov1.util.OfflineBoundsOverlay
import com.darko.speleov1.util.MapLayerMode
import com.darko.speleov1.util.MapLayerPrefs
import com.darko.speleov1.util.WmsConfig
import com.darko.speleov1.util.WmsTileSource
import com.darko.speleov1.util.OfflineTileManager
import com.darko.speleov1.util.PendingCameraPhoto
import com.darko.speleov1.util.PhotoStore
import com.darko.speleov1.util.UserContentStore
import com.darko.speleov1.util.ImportParser
import com.darko.speleov1.util.AppSessionStore
import com.darko.speleov1.util.AppSessionSnapshot
import com.darko.speleov1.util.TrackingNotificationHelper
import com.darko.speleov1.util.TrackingForegroundService
import com.darko.speleov1.util.TrackingRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import java.io.OutputStreamWriter
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.Overlay
import android.location.Location
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean


internal fun displayPlateNumberForSharing(record: SpeleoRecord): String? {
    val directPlate = record.condition.plate_number?.takeIf { it.isNotBlank() }
    if (directPlate != null) return directPlate
    val source = (record.source ?: "").lowercase(Locale.ROOT)
    if (source == "katastar" || source == "both") {
        return record.cadastre.cadastral_number?.takeIf { it.isNotBlank() }
    }
    return null
}

internal fun buildRecordShareText(record: SpeleoRecord): String = buildString {
    appendLine(record.name)
    appendLine("Pločica: ${displayPlateNumberForSharing(record) ?: "-"}")
    appendLine("Vrsta: ${record.classification.object_type ?: "-"}")
    record.location.lat?.let { lat -> record.location.lon?.let { lon -> appendLine("WGS84: ${String.format(Locale.US, "%.6f, %.6f", lat, lon)}") } }
    appendLine("Županija: ${record.location.county ?: "-"}")
    appendLine("Lokalitet: ${record.location.locality ?: record.location.nearest_place ?: "-"}")
    appendLine("Dubina: ${record.metrics.depth_m?.let { "$it m" } ?: "-"}")
    appendLine("Duljina: ${record.metrics.length_m?.let { "$it m" } ?: "-"}")
    if (!record.content.access_description.isNullOrBlank()) appendLine("Pristup: ${record.content.access_description}")
    if (!record.content.technical_description.isNullOrBlank()) appendLine("Opis: ${record.content.technical_description}")
}

internal fun shareText(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

internal fun shareMarkedPoint(context: Context, point: MarkedPoint) {
    val file = writeShareTextFile(context, (point.name.ifBlank { "tocka" }).sanitizeForFile() + ".kml", markedPointToKml(point))
    shareFile(context, file, "application/vnd.google-earth.kml+xml")
}

internal fun shareTrack(context: Context, track: SavedTrack) {
    val file = writeShareTextFile(context, (track.name.ifBlank { "track" }).sanitizeForFile() + ".gpx", trackToGpx(track))
    shareFile(context, file, "application/gpx+xml")
}

internal fun shareImportedLayer(context: Context, layer: ImportedLayer) {
    val file = when {
        layer.type.equals("GPX", ignoreCase = true) -> {
            writeShareTextFile(context, layer.name.sanitizeForFile() + ".gpx", importedLayerToGpx(layer))
        }
        layer.type.equals("GeoJSON", ignoreCase = true) || layer.type.equals("CSV", ignoreCase = true) -> {
            writeShareTextFile(context, layer.name.sanitizeForFile() + ".geojson", importedLayerToGeoJson(layer))
        }
        else -> {
            writeShareTextFile(context, layer.name.sanitizeForFile() + ".kml", importedLayerToKml(layer))
        }
    }
    val mime = when {
        file.extension.equals("gpx", true) -> "application/gpx+xml"
        file.extension.equals("geojson", true) -> "application/geo+json"
        else -> "application/vnd.google-earth.kml+xml"
    }
    shareFile(context, file, mime)
}

internal fun shareOfflineMap(context: Context, mapName: String) {
    val file = exportMapAsMbtilesForShare(context, mapName)
    shareFile(context, file, "application/x-sqlite3")
}

internal fun shareCustomMap(context: Context, mapName: String) {
    val file = exportMapAsMbtilesForShare(context, mapName)
    shareFile(context, file, "application/x-sqlite3")
}

private fun exportMapAsMbtilesForShare(context: Context, mapName: String): File {
    val sourceRoot = OfflineTileManager.tileRootForName(context, mapName)
    val directMbtiles = when {
        sourceRoot.isFile && sourceRoot.extension.equals("mbtiles", ignoreCase = true) -> sourceRoot
        else -> sourceRoot.listFiles()?.firstOrNull { it.isFile && it.extension.equals("mbtiles", ignoreCase = true) }
    }
    val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
    val outFile = File(shareDir, mapName.sanitizeForFile() + ".mbtiles")
    if (outFile.exists()) outFile.delete()
    if (directMbtiles != null) {
        directMbtiles.copyTo(outFile, overwrite = true)
        return outFile
    }
    directoryTilesToMbtiles(context, sourceRoot, mapName, outFile)
    return outFile
}

private fun directoryTilesToMbtiles(context: Context, sourceRoot: File, mapName: String, outFile: File) {
    val tileFiles = sourceRoot.walkTopDown()
        .filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
        .toList()
    require(tileFiles.isNotEmpty()) { "Nema PNG tileova za export." }

    val zooms = mutableListOf<Int>()
    SQLiteDatabase.openOrCreateDatabase(outFile, null).use { db ->
        db.beginTransaction()
        try {
            db.execSQL("CREATE TABLE metadata (name TEXT, value TEXT)")
            db.execSQL("CREATE TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)")
            db.execSQL("CREATE UNIQUE INDEX tile_index on tiles (zoom_level, tile_column, tile_row)")
            db.execSQL("CREATE UNIQUE INDEX metadata_name on metadata (name)")

            tileFiles.forEach { file ->
                val rel = file.relativeTo(sourceRoot).invariantSeparatorsPath
                val parts = rel.split('/')
                if (parts.size < 3) return@forEach
                val z = parts[0].toIntOrNull() ?: return@forEach
                val x = parts[1].toIntOrNull() ?: return@forEach
                val y = parts[2].substringBefore('.').toIntOrNull() ?: return@forEach
                zooms += z
                val tmsY = ((1 shl z) - 1) - y
                val values = ContentValues().apply {
                    put("zoom_level", z)
                    put("tile_column", x)
                    put("tile_row", tmsY)
                    put("tile_data", file.readBytes())
                }
                db.insertOrThrow("tiles", null, values)
            }

            val bounds = OfflineTileManager.getOfflineBounds(context, mapName)
            val minZoom = zooms.minOrNull() ?: 0
            val maxZoom = zooms.maxOrNull() ?: 0
            val format = "png"
            val metadata = linkedMapOf<String, String>()
            metadata["name"] = mapName
            metadata["type"] = "baselayer"
            metadata["version"] = "1.0"
            metadata["description"] = "SoV offline map export"
            metadata["format"] = format
            metadata["minzoom"] = minZoom.toString()
            metadata["maxzoom"] = maxZoom.toString()
            if (bounds != null) {
                metadata["bounds"] = listOf(bounds.minLon, bounds.minLat, bounds.maxLon, bounds.maxLat)
                    .joinToString(",") { String.format(Locale.US, "%.6f", it) }
                metadata["center"] = listOf(
                    (bounds.minLon + bounds.maxLon) / 2.0,
                    (bounds.minLat + bounds.maxLat) / 2.0,
                    maxZoom.toDouble()
                ).joinToString(",") { value ->
                    if (value == maxZoom.toDouble()) value.toInt().toString() else String.format(Locale.US, "%.6f", value)
                }
            }
            metadata.forEach { (name, value) ->
                val row = ContentValues().apply {
                    put("name", name)
                    put("value", value)
                }
                db.insertOrThrow("metadata", null, row)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}



internal fun shareOfflineMapPrintImage(context: Context, mapName: String) {
    val file = exportMapPreviewImageForShare(context, mapName)
    shareFile(context, file, "image/png")
}

internal fun shareCustomMapPrintImage(context: Context, mapName: String) {
    val file = exportMapPreviewImageForShare(context, mapName)
    shareFile(context, file, "image/png")
}

private data class PreviewTile(val x: Int, val y: Int, val bytes: ByteArray)
private data class PreviewSelection(val zoom: Int, val tiles: List<PreviewTile>, val isMbtiles: Boolean)

private fun exportMapPreviewImageForShare(context: Context, mapName: String): File {
    val sourceRoot = OfflineTileManager.tileRootForName(context, mapName)
    val bitmap = renderMapPreviewBitmap(context, mapName, sourceRoot)
        ?: throw IllegalStateException("Ne mogu izvesti sliku karte.")
    val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
    val outFile = File(shareDir, mapName.sanitizeForFile() + "_karta.png")
    if (outFile.exists()) outFile.delete()
    FileOutputStream(outFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return outFile
}

private fun renderMapPreviewBitmap(context: Context, mapName: String, sourceRoot: File): Bitmap? {
    val directMbtiles = when {
        sourceRoot.isFile && sourceRoot.extension.equals("mbtiles", ignoreCase = true) -> sourceRoot
        else -> sourceRoot.listFiles()?.firstOrNull { it.isFile && it.extension.equals("mbtiles", ignoreCase = true) }
    }
    val selection = if (directMbtiles != null) {
        chooseMbtilesPreviewSet(directMbtiles)
    } else {
        chooseDirectoryPreviewSet(sourceRoot)
    } ?: return null
    val tileSet = selection.tiles

    val minX = tileSet.minOfOrNull { it.x } ?: return null
    val maxX = tileSet.maxOfOrNull { it.x } ?: return null
    val minY = tileSet.minOfOrNull { it.y } ?: return null
    val maxY = tileSet.maxOfOrNull { it.y } ?: return null
    val cols = (maxX - minX + 1).coerceAtLeast(1)
    val rows = (maxY - minY + 1).coerceAtLeast(1)
    val tileSize = 256
    val mosaicWidth = cols * tileSize
    val mosaicHeight = rows * tileSize
    val landscape = mosaicWidth >= mosaicHeight
    val pageWidth = if (landscape) 2480 else 1754
    val pageHeight = if (landscape) 1754 else 2480
    val headerHeight = 170
    val footerHeight = 220
    val outerPadding = 88
    val contentLeft = outerPadding
    val contentTop = headerHeight
    val contentRight = pageWidth - outerPadding
    val contentBottom = pageHeight - footerHeight
    val availableWidth = (contentRight - contentLeft).coerceAtLeast(1)
    val availableHeight = (contentBottom - contentTop).coerceAtLeast(1)
    val scale = minOf(availableWidth.toFloat() / mosaicWidth.toFloat(), availableHeight.toFloat() / mosaicHeight.toFloat())
    val drawWidth = (mosaicWidth * scale).toInt().coerceAtLeast(1)
    val drawHeight = (mosaicHeight * scale).toInt().coerceAtLeast(1)
    val drawLeft = contentLeft + (availableWidth - drawWidth) / 2
    val drawTop = contentTop + (availableHeight - drawHeight) / 2

    val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.parseColor("#F7F8FA"))

    val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#D7DCE3")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#D6D9DE")
        strokeWidth = 3f
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#111827")
        textSize = 44f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#4B5563")
        textSize = 28f
    }
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#374151")
        textSize = 26f
    }

    canvas.drawRect(outerPadding.toFloat(), 36f, (pageWidth - outerPadding).toFloat(), (pageHeight - 36).toFloat(), framePaint)
    canvas.drawRect(outerPadding.toFloat(), 36f, (pageWidth - outerPadding).toFloat(), (pageHeight - 36).toFloat(), borderPaint)

    val decodeOptions = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    tileSet.forEach { tile ->
        val tileBitmap = BitmapFactory.decodeByteArray(tile.bytes, 0, tile.bytes.size, decodeOptions) ?: return@forEach
        val left = drawLeft + ((tile.x - minX) * tileSize * scale).toInt()
        val top = drawTop + ((tile.y - minY) * tileSize * scale).toInt()
        val right = drawLeft + (((tile.x - minX + 1) * tileSize) * scale).toInt()
        val bottom = drawTop + (((tile.y - minY + 1) * tileSize) * scale).toInt()
        canvas.drawBitmap(tileBitmap, null, Rect(left, top, right, bottom), null)
        if (!tileBitmap.isRecycled) tileBitmap.recycle()
    }

    canvas.drawRect(drawLeft.toFloat(), drawTop.toFloat(), (drawLeft + drawWidth).toFloat(), (drawTop + drawHeight).toFloat(), borderPaint)

    val bounds = OfflineTileManager.getOfflineBounds(context, mapName)
    val sourceLabel = if (selection.isMbtiles) "MBTiles" else "PNG tiles"
    val summaryLine = "$sourceLabel • preview z${selection.zoom} • ${tileSet.size} tileova"
    val boundsText = bounds?.let {
        "Bounds: " + listOf(it.minLat, it.minLon, it.maxLat, it.maxLon)
            .joinToString("  ") { value -> String.format(Locale.US, "%.5f", value) }
    } ?: "Bounds nisu dostupni"

    canvas.drawText(mapName, (outerPadding + 18).toFloat(), 102f, titlePaint)
    canvas.drawText("PNG pregled za share / print", (outerPadding + 18).toFloat(), 144f, subtitlePaint)
    canvas.drawLine((outerPadding + 18).toFloat(), (pageHeight - footerHeight).toFloat(), (pageWidth - outerPadding - 18).toFloat(), (pageHeight - footerHeight).toFloat(), dividerPaint)
    canvas.drawText(summaryLine, (outerPadding + 18).toFloat(), (pageHeight - 132).toFloat(), metaPaint)
    canvas.drawText(boundsText, (outerPadding + 18).toFloat(), (pageHeight - 86).toFloat(), metaPaint)
    canvas.drawText("SoV Speleo export", (outerPadding + 18).toFloat(), (pageHeight - 40).toFloat(), subtitlePaint)
    return bitmap
}

private fun chooseDirectoryPreviewSet(sourceRoot: File): PreviewSelection? {
    val tilesByZoom = linkedMapOf<Int, MutableList<PreviewTile>>()
    sourceRoot.walkTopDown()
        .filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
        .forEach { file ->
            val rel = file.relativeTo(sourceRoot).invariantSeparatorsPath
            val parts = rel.split('/')
            if (parts.size < 3) return@forEach
            val z = parts[0].toIntOrNull() ?: return@forEach
            val x = parts[1].toIntOrNull() ?: return@forEach
            val y = parts[2].substringBefore('.').toIntOrNull() ?: return@forEach
            tilesByZoom.getOrPut(z) { mutableListOf() }.add(PreviewTile(x, y, file.readBytes()))
        }
    if (tilesByZoom.isEmpty()) return null
    val candidateZoom = selectPreviewZoom(tilesByZoom.map { it.key to it.value.size })
    val tiles = tilesByZoom[candidateZoom]?.toList().orEmpty()
    return tiles.takeIf { it.isNotEmpty() }?.let { PreviewSelection(candidateZoom, it, false) }
}

private fun chooseMbtilesPreviewSet(file: File): PreviewSelection? {
    SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        val zoomCounts = mutableListOf<Pair<Int, Int>>()
        db.rawQuery("SELECT zoom_level, COUNT(*) FROM tiles GROUP BY zoom_level ORDER BY zoom_level ASC", null).use { c ->
            while (c.moveToNext()) {
                zoomCounts += c.getInt(0) to c.getInt(1)
            }
        }
        if (zoomCounts.isEmpty()) return null
        val chosenZoom = selectPreviewZoom(zoomCounts)
        val tiles = mutableListOf<PreviewTile>()
        db.rawQuery(
            "SELECT tile_column, tile_row, tile_data FROM tiles WHERE zoom_level = ? ORDER BY tile_column, tile_row",
            arrayOf(chosenZoom.toString())
        ).use { c ->
            val maxIndex = (1 shl chosenZoom) - 1
            while (c.moveToNext()) {
                val x = c.getInt(0)
                val tmsY = c.getInt(1)
                val y = maxIndex - tmsY
                val data = c.getBlob(2)
                tiles += PreviewTile(x, y, data)
            }
        }
        return tiles.takeIf { it.isNotEmpty() }?.let { PreviewSelection(chosenZoom, it, true) }
    }
}

private fun selectPreviewZoom(zoomCounts: List<Pair<Int, Int>>): Int {
    val sorted = zoomCounts.sortedBy { it.first }
    return sorted.filter { it.second in 4..64 }.maxByOrNull { it.first }?.first
        ?: sorted.filter { it.second <= 100 }.maxByOrNull { it.first }?.first
        ?: sorted.minByOrNull { kotlin.math.abs(it.second - 36) }?.first
        ?: sorted.last().first
}

internal fun shareFile(context: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

internal fun writeShareTextFile(context: Context, fileName: String, text: String): File {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeText(text)
    return file
}

internal fun zipDirectoryForShare(context: Context, sourceDir: File, outputName: String): File {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val outFile = File(dir, outputName)
    ZipOutputStream(FileOutputStream(outFile)).use { zos ->
        sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val entryName = file.relativeTo(sourceDir).invariantSeparatorsPath
            zos.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }
    return outFile
}

internal fun importedLayerToKml(layer: ImportedLayer): String {
    val placemarks = buildString {
        layer.points.forEach { point ->
            val desc = buildString {
                appendLine("Tip: ${point.type}")
                appendLine("Opis: ${point.description.ifBlank { "-" }}")
                appendLine("HTRS96/TM X: ${String.format(Locale.US, "%.2f", point.htrsX)}")
                appendLine("HTRS96/TM Y: ${String.format(Locale.US, "%.2f", point.htrsY)}")
            }.trim()
            appendLine("<Placemark>")
            appendLine("<name>${xmlEscape(point.name)}</name>")
            appendLine("<description>${xmlEscape(desc)}</description>")
            appendLine("<ExtendedData>")
            appendLine("<Data name=\"type\"><value>${xmlEscape(point.type)}</value></Data>")
            appendLine("<Data name=\"description\"><value>${xmlEscape(point.description)}</value></Data>")
            appendLine("</ExtendedData>")
            appendLine("<Point><coordinates>${String.format(Locale.US, "%.7f", point.lon)},${String.format(Locale.US, "%.7f", point.lat)},0</coordinates></Point>")
            appendLine("</Placemark>")
        }
        layer.tracks.forEach { track ->
            val coords = track.points.joinToString(" ") {
                "${String.format(Locale.US, "%.7f", it.point.longitude)},${String.format(Locale.US, "%.7f", it.point.latitude)},${String.format(Locale.US, "%.2f", it.altitudeM ?: 0.0)}"
            }
            appendLine("<Placemark>")
            appendLine("<name>${xmlEscape(track.name)}</name>")
            appendLine("<description>${xmlEscape(track.description)}</description>")
            appendLine("<LineString><tessellate>1</tessellate><coordinates>$coords</coordinates></LineString>")
            appendLine("</Placemark>")
        }
    }
    return """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
<Document>
  <name>${xmlEscape(layer.name)}</name>
$placemarks
</Document>
</kml>"""
}

internal fun importedLayerToGeoJson(layer: ImportedLayer): String {
    val features = JSONArray()
    layer.points.forEach { point ->
        val properties = JSONObject().apply {
            put("name", point.name)
            put("description", point.description)
            put("type", point.type)
            put("htrs_x", point.htrsX)
            put("htrs_y", point.htrsY)
        }
        val geometry = JSONObject().apply {
            put("type", "Point")
            put("coordinates", JSONArray().put(point.lon).put(point.lat))
        }
        features.put(
            JSONObject().apply {
                put("type", "Feature")
                put("properties", properties)
                put("geometry", geometry)
            }
        )
    }
    layer.tracks.forEach { track ->
        val coords = JSONArray()
        track.points.forEach { tp ->
            val pair = JSONArray().put(tp.point.longitude).put(tp.point.latitude)
            tp.altitudeM?.let { pair.put(it) }
            coords.put(pair)
        }
        val properties = JSONObject().apply {
            put("name", track.name)
            put("description", track.description)
        }
        val geometry = JSONObject().apply {
            put("type", "LineString")
            put("coordinates", coords)
        }
        features.put(
            JSONObject().apply {
                put("type", "Feature")
                put("properties", properties)
                put("geometry", geometry)
            }
        )
    }
    return JSONObject().apply {
        put("type", "FeatureCollection")
        put("name", layer.name)
        put("features", features)
    }.toString(2)
}

internal fun importedLayerToGpx(layer: ImportedLayer): String = buildString {
    val timeFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
    appendLine("""<gpx version="1.1" creator="SoV Speleo" xmlns="http://www.topografix.com/GPX/1/1">""")
    layer.points.forEach { point ->
        appendLine("""  <wpt lat="${String.format(Locale.US, "%.6f", point.lat)}" lon="${String.format(Locale.US, "%.6f", point.lon)}">""")
        appendLine("    <name>${escapeXml(point.name)}</name>")
        if (point.description.isNotBlank()) appendLine("    <desc>${escapeXml(point.description)}</desc>")
        appendLine("  </wpt>")
    }
    layer.tracks.forEach { track ->
        appendLine("  <trk>")
        appendLine("    <name>${escapeXml(track.name)}</name>")
        if (track.description.isNotBlank()) appendLine("    <desc>${escapeXml(track.description)}</desc>")
        appendLine("    <trkseg>")
        track.points.forEachIndexed { idx, tp ->
            appendLine("""      <trkpt lat="${String.format(Locale.US, "%.6f", tp.point.latitude)}" lon="${String.format(Locale.US, "%.6f", tp.point.longitude)}">""")
            tp.altitudeM?.let { appendLine("        <ele>${String.format(Locale.US, "%.2f", it)}</ele>") }
            appendLine("        <time>${timeFmt.format(java.util.Date(track.createdAtMillis + idx * 1000L))}</time>")
            appendLine("      </trkpt>")
        }
        appendLine("    </trkseg>")
        appendLine("  </trk>")
    }
    appendLine("</gpx>")
}

internal fun String.sanitizeForFile(): String = trim()
    .replace(Regex("[\\/:*?\"<>|]"), "_")
    .replace(Regex("\\s+"), "_")
    .ifBlank { "share" }

fun FilterState.hasAnyActiveCriteria(): Boolean = query.isNotBlank() || hasAdvancedFilters()

fun FilterState.hasAdvancedFilters(): Boolean =
    sourceFilter != SourceFilter.ALL ||
        cadastreFilter != CadastreFilter.ALL ||
        caveTypeFilter != CaveTypeFilter.ALL ||
        areaFilter.isNotBlank() ||
        distanceFilterKm != null ||
        depthMinM != null ||
        onlyWithDescription ||
        fieldTaskFilters.isNotEmpty() ||
        boundingBoxFilter != null



internal fun normalizeForSearch(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val replaced = value.trim().replace('đ', 'd').replace('Đ', 'D')
    return Normalizer.normalize(replaced, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun boundingBoxFor(points: List<GeoPoint>): BoundingBox? {
    if (points.isEmpty()) return null
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }
    return BoundingBox(maxLat, maxLon, minLat, minLon)
}

internal fun writeTextToUri(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        OutputStreamWriter(stream).use { it.write(text) }
    }
}


internal fun trackToGpx(track: SavedTrack): String {
    val timeFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    sb.append("<gpx version=\"1.1\" creator=\"SoV Speleo\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
    sb.append("  <trk>\n")
    sb.append("    <name>").append(escapeXml(track.name)).append("</name>\n")
    if (track.description.isNotBlank()) sb.append("    <desc>").append(escapeXml(track.description)).append("</desc>\n")
    sb.append("    <trkseg>\n")
    track.points.forEachIndexed { idx, tp ->
        sb.append("      <trkpt lat=\"").append(String.format(Locale.US, "%.6f", tp.point.latitude)).append("\" lon=\"").append(String.format(Locale.US, "%.6f", tp.point.longitude)).append("\">\n")
        tp.altitudeM?.let { sb.append("        <ele>").append(String.format(Locale.US, "%.2f", it)).append("</ele>\n") }
        sb.append("        <time>").append(timeFmt.format(java.util.Date(track.createdAtMillis + idx * 1000L))).append("</time>\n")
        sb.append("      </trkpt>\n")
    }
    sb.append("    </trkseg>\n")
    sb.append("  </trk>\n")
    sb.append("</gpx>\n")
    return sb.toString()
}

internal fun escapeXml(input: String): String = input
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

internal fun markedPointToKml(point: MarkedPoint): String {
    val desc = buildString {
        appendLine("Tip: ${point.type}")
        appendLine("Opis: ${point.description.ifBlank { "-" }}")
        appendLine("HTRS X: ${String.format(Locale.US, "%.2f", point.htrsX)}")
        appendLine("HTRS Y: ${String.format(Locale.US, "%.2f", point.htrsY)}")
    }.trim()
    return """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
<Document>
  <name>${xmlEscape(point.name)}</name>
  <Placemark>
    <name>${xmlEscape(point.name)}</name>
    <description>${xmlEscape(desc)}</description>
    <ExtendedData>
      <Data name="type"><value>${xmlEscape(point.type)}</value></Data>
      <Data name="description"><value>${xmlEscape(point.description)}</value></Data>
      <Data name="htrs_x"><value>${String.format(Locale.US, "%.2f", point.htrsX)}</value></Data>
      <Data name="htrs_y"><value>${String.format(Locale.US, "%.2f", point.htrsY)}</value></Data>
    </ExtendedData>
    <Point><coordinates>${String.format(Locale.US, "%.7f", point.lon)},${String.format(Locale.US, "%.7f", point.lat)},0</coordinates></Point>
  </Placemark>
</Document>
</kml>
"""
}

internal fun xmlEscape(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

internal fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
}

internal fun openGoogleMaps(context: Context, lat: Double, lon: Double) {
    val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lon"))
    runCatching {
        if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
        else context.startActivity(fallback)
    }
}

internal fun openUri(context: Context, uriString: String) {
    if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        runCatching { context.startActivity(intent) }
        return
    }
    val uri = PhotoStore.resolvePhotoUri(context, uriString)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}
