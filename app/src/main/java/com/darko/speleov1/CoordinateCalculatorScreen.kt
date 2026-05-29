package com.darko.speleov1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Intent
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
import kotlin.math.roundToInt
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoordinateCalculatorScreen() {
    val context = LocalContext.current
    val modes = listOf(
        "WGS84 decimal",
        "WGS84 DMS",
        "WGS84 DM",
        "HTRS96/TM",
        "UTM",
        "Web Mercator"
    )
    var mode by rememberSaveable { mutableStateOf("WGS84 decimal") }
    var aText by rememberSaveable { mutableStateOf("") }
    var bText by rememberSaveable { mutableStateOf("") }
    var zoneText by rememberSaveable { mutableStateOf("33") }
    var hemisphereNorth by rememberSaveable { mutableStateOf(true) }
    var result by remember { mutableStateOf<CoordinateCalcResult?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun resetOutput() {
        result = null
        error = null
    }

    fun convert() {
        val converted = runCatching {
            when (mode) {
                "WGS84 decimal" -> {
                    val lat = parseCoordinateNumber(aText) ?: error("Upiši latitude / širinu.")
                    val lon = parseCoordinateNumber(bText) ?: error("Upiši longitude / dužinu.")
                    validateLatLon(lat, lon)
                    CoordinateConverter.LatLon(lat, lon)
                }
                "WGS84 DMS" -> {
                    val lat = parseDmsCoordinate(aText) ?: error("Upiši DMS širinu, npr. 44° 52' 12.5\" N")
                    val lon = parseDmsCoordinate(bText) ?: error("Upiši DMS dužinu, npr. 15° 32' 10\" E")
                    validateLatLon(lat, lon)
                    CoordinateConverter.LatLon(lat, lon)
                }
                "WGS84 DM" -> {
                    val lat = parseDmCoordinate(aText) ?: error("Upiši DM širinu, npr. 44° 52.125' N")
                    val lon = parseDmCoordinate(bText) ?: error("Upiši DM dužinu, npr. 15° 32.250' E")
                    validateLatLon(lat, lon)
                    CoordinateConverter.LatLon(lat, lon)
                }
                "HTRS96/TM" -> {
                    val x = parseCoordinateNumber(aText) ?: error("Upiši HTRS X / Easting.")
                    val y = parseCoordinateNumber(bText) ?: error("Upiši HTRS Y / Northing.")
                    CoordinateConverter.htrs96TmToWgs84(x, y)
                }
                "UTM" -> {
                    val easting = parseCoordinateNumber(aText) ?: error("Upiši UTM Easting.")
                    val northing = parseCoordinateNumber(bText) ?: error("Upiši UTM Northing.")
                    val zone = zoneText.trim().toIntOrNull() ?: error("Upiši UTM zonu, npr. 33 ili 34.")
                    utmToWgs84(easting, northing, zone, hemisphereNorth)
                }
                "Web Mercator" -> {
                    val x = parseCoordinateNumber(aText) ?: error("Upiši Web Mercator X.")
                    val y = parseCoordinateNumber(bText) ?: error("Upiši Web Mercator Y.")
                    webMercatorToWgs84(x, y)
                }
                else -> error("Nepoznat format.")
            }
        }
        converted.onSuccess { latLon ->
            result = buildCoordinateCalcResult(latLon)
            error = null
        }.onFailure {
            result = null
            error = it.message ?: "Ne mogu pročitati koordinate."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Kalkulator koordinata", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Terenski konverter za najčešće speleo/GIS formate: WGS84, DMS, DM, HTRS96/TM, UTM i Web Mercator.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { item ->
                FilterChip(
                    selected = mode == item,
                    onClick = {
                        mode = item
                        aText = ""
                        bText = ""
                        resetOutput()
                    },
                    label = { Text(item) }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(inputTitleForMode(mode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(inputHintForMode(mode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = aText,
                    onValueChange = { aText = it; resetOutput() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(firstLabelForMode(mode)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = bText,
                    onValueChange = { bText = it; resetOutput() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(secondLabelForMode(mode)) },
                    singleLine = true
                )
                if (mode == "UTM") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = zoneText,
                            onValueChange = { zoneText = it.filter { ch -> ch.isDigit() }.take(2); resetOutput() },
                            modifier = Modifier.weight(1f),
                            label = { Text("Zona") },
                            singleLine = true
                        )
                        FilterChip(selected = hemisphereNorth, onClick = { hemisphereNorth = true; resetOutput() }, label = { Text("N") })
                        FilterChip(selected = !hemisphereNorth, onClick = { hemisphereNorth = false; resetOutput() }, label = { Text("S") })
                    }
                }
                Button(onClick = { convert() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Konvertiraj")
                }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        result?.let { output ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Rezultat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    CoordinateResultLine("WGS84 decimal", output.decimal)
                    CoordinateResultLine("WGS84 DMS", output.dms)
                    CoordinateResultLine("WGS84 DM", output.dm)
                    CoordinateResultLine("HTRS96/TM", output.htrs)
                    CoordinateResultLine("UTM", output.utm)
                    CoordinateResultLine("Web Mercator", output.webMercator)
                    OutlinedButton(onClick = { openGoogleMaps(context, output.lat, output.lon) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Open in Google Maps")
                    }
                }
            }
        }
    }
}

@Composable
private fun CoordinateResultLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private data class CoordinateCalcResult(
    val lat: Double,
    val lon: Double,
    val decimal: String,
    val dms: String,
    val dm: String,
    val htrs: String,
    val utm: String,
    val webMercator: String
)

private data class UtmCoordinate(val easting: Double, val northing: Double, val zone: Int, val hemisphere: String)
private data class WebMercatorCoordinate(val x: Double, val y: Double)

private fun buildCoordinateCalcResult(latLon: CoordinateConverter.LatLon): CoordinateCalcResult {
    val htrs = CoordinateConverter.wgs84ToHtrs96Tm(latLon.lat, latLon.lon)
    val utm = wgs84ToUtm(latLon.lat, latLon.lon)
    val web = wgs84ToWebMercator(latLon.lat, latLon.lon)
    return CoordinateCalcResult(
        lat = latLon.lat,
        lon = latLon.lon,
        decimal = String.format(Locale.US, "%.7f, %.7f", latLon.lat, latLon.lon),
        dms = "${formatDms(latLon.lat, true)}  ${formatDms(latLon.lon, false)}",
        dm = "${formatDm(latLon.lat, true)}  ${formatDm(latLon.lon, false)}",
        htrs = String.format(Locale.US, "X %.2f   Y %.2f", htrs.x, htrs.y),
        utm = String.format(Locale.US, "Zone %d%s  E %.2f  N %.2f", utm.zone, utm.hemisphere, utm.easting, utm.northing),
        webMercator = String.format(Locale.US, "X %.2f   Y %.2f", web.x, web.y)
    )
}

private fun parseCoordinateNumber(raw: String): Double? = raw.trim().replace(" ", "").replace(",", ".").toDoubleOrNull()

private fun validateLatLon(lat: Double, lon: Double) {
    require(lat in -90.0..90.0) { "Latitude mora biti između -90 i 90." }
    require(lon in -180.0..180.0) { "Longitude mora biti između -180 i 180." }
}

private fun parseDmsCoordinate(raw: String): Double? {
    val cleaned = raw.trim().uppercase(Locale.ROOT)
    val sign = when {
        cleaned.contains("S") || cleaned.contains("W") || cleaned.startsWith("-") -> -1.0
        else -> 1.0
    }
    val parts = Regex("[-+]?\\d+(?:[.,]\\d+)?").findAll(cleaned).map { it.value.replace(",", ".").toDouble() }.toList()
    if (parts.isEmpty()) return null
    val degrees = kotlin.math.abs(parts.getOrNull(0) ?: return null)
    val minutes = parts.getOrNull(1) ?: 0.0
    val seconds = parts.getOrNull(2) ?: 0.0
    return sign * (degrees + minutes / 60.0 + seconds / 3600.0)
}

private fun parseDmCoordinate(raw: String): Double? {
    val cleaned = raw.trim().uppercase(Locale.ROOT)
    val sign = when {
        cleaned.contains("S") || cleaned.contains("W") || cleaned.startsWith("-") -> -1.0
        else -> 1.0
    }
    val parts = Regex("[-+]?\\d+(?:[.,]\\d+)?").findAll(cleaned).map { it.value.replace(",", ".").toDouble() }.toList()
    if (parts.isEmpty()) return null
    val degrees = kotlin.math.abs(parts.getOrNull(0) ?: return null)
    val minutes = parts.getOrNull(1) ?: 0.0
    return sign * (degrees + minutes / 60.0)
}

private fun formatDms(value: Double, isLat: Boolean): String {
    val hemi = when {
        isLat && value >= 0 -> "N"
        isLat -> "S"
        value >= 0 -> "E"
        else -> "W"
    }
    val abs = kotlin.math.abs(value)
    val deg = abs.toInt()
    val minFull = (abs - deg) * 60.0
    val min = minFull.toInt()
    val sec = (minFull - min) * 60.0
    return String.format(Locale.US, "%d° %02d' %.2f\" %s", deg, min, sec, hemi)
}

private fun formatDm(value: Double, isLat: Boolean): String {
    val hemi = when {
        isLat && value >= 0 -> "N"
        isLat -> "S"
        value >= 0 -> "E"
        else -> "W"
    }
    val abs = kotlin.math.abs(value)
    val deg = abs.toInt()
    val min = (abs - deg) * 60.0
    return String.format(Locale.US, "%d° %.5f' %s", deg, min, hemi)
}

private fun inputTitleForMode(mode: String): String = when (mode) {
    "WGS84 decimal" -> "WGS84 decimal degrees"
    "WGS84 DMS" -> "Degrees / Minutes / Seconds"
    "WGS84 DM" -> "Degrees / Decimal Minutes"
    "HTRS96/TM" -> "HTRS96/TM — EPSG:3765"
    "UTM" -> "UTM"
    "Web Mercator" -> "Web Mercator — EPSG:3857"
    else -> mode
}

private fun inputHintForMode(mode: String): String = when (mode) {
    "WGS84 decimal" -> "Primjer: 44.870100 i 15.535200"
    "WGS84 DMS" -> "Primjer: 44° 52' 12.5\" N i 15° 32' 10\" E"
    "WGS84 DM" -> "Primjer: 44° 52.125' N i 15° 32.250' E"
    "HTRS96/TM" -> "Unesi X/Easting i Y/Northing iz HTRS96/TM sustava."
    "UTM" -> "Za Hrvatsku su najčešće zone 33N i 34N."
    "Web Mercator" -> "EPSG:3857 metri, često iz web mapa i tile sustava."
    else -> ""
}

private fun firstLabelForMode(mode: String): String = when (mode) {
    "WGS84 decimal", "WGS84 DMS", "WGS84 DM" -> "Latitude / širina"
    "HTRS96/TM", "UTM", "Web Mercator" -> "X / Easting"
    else -> "A"
}

private fun secondLabelForMode(mode: String): String = when (mode) {
    "WGS84 decimal", "WGS84 DMS", "WGS84 DM" -> "Longitude / dužina"
    "HTRS96/TM", "UTM", "Web Mercator" -> "Y / Northing"
    else -> "B"
}

private fun wgs84ToWebMercator(lat: Double, lon: Double): WebMercatorCoordinate {
    val radius = 6378137.0
    val clippedLat = lat.coerceIn(-85.05112878, 85.05112878)
    val x = radius * Math.toRadians(lon)
    val y = radius * kotlin.math.ln(kotlin.math.tan(Math.PI / 4.0 + Math.toRadians(clippedLat) / 2.0))
    return WebMercatorCoordinate(x, y)
}

private fun webMercatorToWgs84(x: Double, y: Double): CoordinateConverter.LatLon {
    val radius = 6378137.0
    val lon = Math.toDegrees(x / radius)
    val lat = Math.toDegrees(2.0 * kotlin.math.atan(kotlin.math.exp(y / radius)) - Math.PI / 2.0)
    validateLatLon(lat, lon)
    return CoordinateConverter.LatLon(lat, lon)
}

private fun wgs84ToUtm(lat: Double, lon: Double): UtmCoordinate {
    validateLatLon(lat, lon)
    val zone = ((lon + 180.0) / 6.0).toInt() + 1
    val a = 6378137.0
    val eccSquared = 0.00669438
    val k0 = 0.9996
    val latRad = Math.toRadians(lat)
    val lonRad = Math.toRadians(lon)
    val lonOrigin = (zone - 1) * 6 - 180 + 3
    val lonOriginRad = Math.toRadians(lonOrigin.toDouble())
    val eccPrimeSquared = eccSquared / (1.0 - eccSquared)
    val n = a / kotlin.math.sqrt(1.0 - eccSquared * kotlin.math.sin(latRad) * kotlin.math.sin(latRad))
    val t = kotlin.math.tan(latRad) * kotlin.math.tan(latRad)
    val c = eccPrimeSquared * kotlin.math.cos(latRad) * kotlin.math.cos(latRad)
    val aTerm = kotlin.math.cos(latRad) * (lonRad - lonOriginRad)
    val m = a * ((1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64 - 5 * eccSquared * eccSquared * eccSquared / 256) * latRad
        - (3 * eccSquared / 8 + 3 * eccSquared * eccSquared / 32 + 45 * eccSquared * eccSquared * eccSquared / 1024) * kotlin.math.sin(2 * latRad)
        + (15 * eccSquared * eccSquared / 256 + 45 * eccSquared * eccSquared * eccSquared / 1024) * kotlin.math.sin(4 * latRad)
        - (35 * eccSquared * eccSquared * eccSquared / 3072) * kotlin.math.sin(6 * latRad))
    val easting = k0 * n * (aTerm + (1 - t + c) * aTerm * aTerm * aTerm / 6.0 + (5 - 18 * t + t * t + 72 * c - 58 * eccPrimeSquared) * aTerm * aTerm * aTerm * aTerm * aTerm / 120.0) + 500000.0
    var northing = k0 * (m + n * kotlin.math.tan(latRad) * (aTerm * aTerm / 2.0 + (5 - t + 9 * c + 4 * c * c) * aTerm * aTerm * aTerm * aTerm / 24.0 + (61 - 58 * t + t * t + 600 * c - 330 * eccPrimeSquared) * aTerm * aTerm * aTerm * aTerm * aTerm * aTerm / 720.0))
    if (lat < 0) northing += 10000000.0
    return UtmCoordinate(easting, northing, zone, if (lat >= 0) "N" else "S")
}

private fun utmToWgs84(easting: Double, northing: Double, zone: Int, northernHemisphere: Boolean): CoordinateConverter.LatLon {
    require(zone in 1..60) { "UTM zona mora biti 1–60." }
    val a = 6378137.0
    val eccSquared = 0.00669438
    val k0 = 0.9996
    val eccPrimeSquared = eccSquared / (1.0 - eccSquared)
    val e1 = (1.0 - kotlin.math.sqrt(1.0 - eccSquared)) / (1.0 + kotlin.math.sqrt(1.0 - eccSquared))
    val x = easting - 500000.0
    var y = northing
    if (!northernHemisphere) y -= 10000000.0
    val lonOrigin = (zone - 1) * 6 - 180 + 3
    val m = y / k0
    val mu = m / (a * (1.0 - eccSquared / 4.0 - 3.0 * eccSquared * eccSquared / 64.0 - 5.0 * eccSquared * eccSquared * eccSquared / 256.0))
    val phi1Rad = mu + (3 * e1 / 2 - 27 * e1 * e1 * e1 / 32) * kotlin.math.sin(2 * mu)
        + (21 * e1 * e1 / 16 - 55 * e1 * e1 * e1 * e1 / 32) * kotlin.math.sin(4 * mu)
        + (151 * e1 * e1 * e1 / 96) * kotlin.math.sin(6 * mu)
    val n1 = a / kotlin.math.sqrt(1.0 - eccSquared * kotlin.math.sin(phi1Rad) * kotlin.math.sin(phi1Rad))
    val t1 = kotlin.math.tan(phi1Rad) * kotlin.math.tan(phi1Rad)
    val c1 = eccPrimeSquared * kotlin.math.cos(phi1Rad) * kotlin.math.cos(phi1Rad)
    val r1 = a * (1.0 - eccSquared) / Math.pow(1.0 - eccSquared * kotlin.math.sin(phi1Rad) * kotlin.math.sin(phi1Rad), 1.5)
    val d = x / (n1 * k0)
    val lat = phi1Rad - (n1 * kotlin.math.tan(phi1Rad) / r1) * (d * d / 2.0 - (5 + 3 * t1 + 10 * c1 - 4 * c1 * c1 - 9 * eccPrimeSquared) * d * d * d * d / 24.0 + (61 + 90 * t1 + 298 * c1 + 45 * t1 * t1 - 252 * eccPrimeSquared - 3 * c1 * c1) * d * d * d * d * d * d / 720.0)
    val lon = Math.toRadians(lonOrigin.toDouble()) + (d - (1 + 2 * t1 + c1) * d * d * d / 6.0 + (5 - 2 * c1 + 28 * t1 - 3 * c1 * c1 + 8 * eccPrimeSquared + 24 * t1 * t1) * d * d * d * d * d / 120.0) / kotlin.math.cos(phi1Rad)
    val latDeg = Math.toDegrees(lat)
    val lonDeg = Math.toDegrees(lon)
    validateLatLon(latDeg, lonDeg)
    return CoordinateConverter.LatLon(latDeg, lonDeg)
}
