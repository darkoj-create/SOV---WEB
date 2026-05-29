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
import com.darko.speleov1.util.MyBaseRepository
import com.darko.speleov1.util.SovAppPermissions
import com.darko.speleov1.util.SovAuthSession
import com.darko.speleov1.util.SovPermissionsStore
import com.darko.speleov1.util.SovRoleSyncManager
import com.darko.speleov1.util.SovSupabaseRoleClient
import com.darko.speleov1.util.EquipmentCloudItem
import com.darko.speleov1.util.EquipmentCloudRequest
import com.darko.speleov1.util.EquipmentCloudRequestLine
import com.darko.speleov1.util.EquipmentSupabaseRepository
import com.darko.speleov1.util.EquipmentInventoryCountPayload
import com.darko.speleov1.util.EquipmentReturnLinePayload
import com.darko.speleov1.util.ArchiveSupabaseRepository
import com.darko.speleov1.util.ArchiveWorkItem
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CollectionsBookmark
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.SignalCellularAlt
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.darko.speleov1.util.AppSessionSnapshot
import com.darko.speleov1.util.TrackingNotificationHelper
import com.darko.speleov1.util.TrackingForegroundService
import com.darko.speleov1.util.TrackingRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
import android.location.GnssStatus
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import java.text.Normalizer
import java.util.Locale
import java.util.Calendar
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.ui.graphics.luminance

private val SOV_DISPLAY_VERSION: String
    get() = "SOV ${BuildConfig.VERSION_NAME} PUBLIC RELEASE"

private const val SOV_UPDATED_DATE_HR = "19.5.2026"
private const val SOV_UPDATED_DATE_EN = "19 May 2026"




private data class GpsFieldStatus(
    val hasLocationPermission: Boolean,
    val satelliteCount: Int = 0,
    val usedInFixCount: Int = 0,
    val gpsCount: Int = 0,
    val glonassCount: Int = 0,
    val galileoCount: Int = 0,
    val beidouCount: Int = 0,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val speedKmh: Float? = null,
    val bearingDegrees: Float? = null,
    val lastFixAgeMs: Long? = null,
    val provider: String? = null,
)

private data class CompassFieldStatus(
    val accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val magneticMagnitudeMicroTesla: Float? = null,
    val lastUpdateAgeMs: Long? = null,
)

private fun compassQualityLabel(status: CompassFieldStatus): String = when {
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH && (status.magneticMagnitudeMicroTesla ?: 45f) in 25f..65f -> "Kompas stabilan"
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Dobra kalibracija"
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Srednje pouzdan"
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Treba kalibraciju"
    status.magneticMagnitudeMicroTesla != null -> "Moguće smetnje"
    else -> "Čeka senzor"
}

private fun compassQualityHint(status: CompassFieldStatus): String = when {
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH && (status.magneticMagnitudeMicroTesla ?: 45f) in 25f..65f -> "Spreman za teren"
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Dobro, ali pazi na metal"
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Napravi par osmici"
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Pomakni mobitel u osmici"
    status.magneticMagnitudeMicroTesla != null -> "Makni od auta/metala"
    else -> "Rotiraj mobitel lagano"
}

private fun compassQualityColor(status: CompassFieldStatus): Color = when {
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH && (status.magneticMagnitudeMicroTesla ?: 45f) in 25f..65f -> Color(0xFF4CAF50)
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color(0xFF8BC34A)
    status.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color(0xFFFFB300)
    else -> Color(0xFFE57373)
}

private fun gpsQualityLabel(status: GpsFieldStatus): String = when {
    !status.hasLocationPermission -> "Nema dozvole"
    status.usedInFixCount >= 12 && (status.accuracyMeters ?: 99f) <= 5f -> "Odličan lock"
    status.usedInFixCount >= 7 && (status.accuracyMeters ?: 99f) <= 12f -> "Dobar lock"
    status.usedInFixCount >= 4 -> "Slabiji lock"
    status.satelliteCount > 0 -> "Traži fix"
    else -> "Nema signala"
}

private fun gpsQualityColor(status: GpsFieldStatus): Color = when {
    !status.hasLocationPermission -> Color(0xFF90A4AE)
    status.usedInFixCount >= 12 && (status.accuracyMeters ?: 99f) <= 5f -> Color(0xFF4CAF50)
    status.usedInFixCount >= 7 && (status.accuracyMeters ?: 99f) <= 12f -> Color(0xFF8BC34A)
    status.usedInFixCount >= 4 -> Color(0xFFFFB300)
    status.satelliteCount > 0 -> Color(0xFFFF9800)
    else -> Color(0xFFE57373)
}

private fun formatGpsFixAge(ageMs: Long?): String {
    if (ageMs == null) return "—"
    val sec = (ageMs / 1000L).coerceAtLeast(0L)
    return when {
        sec < 5 -> "sad"
        sec < 60 -> "${sec}s"
        else -> "${sec / 60}m"
    }
}

private fun formatHeadingLabel(degrees: Float?): String {
    if (degrees == null || degrees.isNaN()) return "—"
    val normalized = ((degrees % 360f) + 360f) % 360f
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val idx = ((normalized + 22.5f) / 45f).toInt() % dirs.size
    return "${dirs[idx]} ${normalized.roundToInt()}°"
}

@Composable
private fun rememberGpsFieldStatus(): GpsFieldStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(GpsFieldStatus(hasLocationPermission = false)) }
    var lastFixTimestampMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(lastFixTimestampMs) {
        while (true) {
            val fixTime = lastFixTimestampMs
            if (fixTime != null) {
                status = status.copy(lastFixAgeMs = System.currentTimeMillis() - fixTime)
            }
            delay(1000L)
        }
    }

    DisposableEffect(context) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            status = GpsFieldStatus(hasLocationPermission = false)
            onDispose { }
        } else {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                status = GpsFieldStatus(hasLocationPermission = true)
                onDispose { }
            } else {
                fun updateFromLocation(location: Location?) {
                    if (location == null) return
                    lastFixTimestampMs = location.time
                    status = status.copy(
                        hasLocationPermission = true,
                        accuracyMeters = if (location.hasAccuracy()) location.accuracy else status.accuracyMeters,
                        altitudeMeters = if (location.hasAltitude()) location.altitude else status.altitudeMeters,
                        speedKmh = if (location.hasSpeed()) location.speed * 3.6f else status.speedKmh,
                        bearingDegrees = if (location.hasBearing()) location.bearing else status.bearingDegrees,
                        lastFixAgeMs = System.currentTimeMillis() - location.time,
                        provider = location.provider,
                    )
                }

                runCatching { updateFromLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)) }
                runCatching { updateFromLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)) }

                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) { updateFromLocation(location) }
                }
                runCatching { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500L, 0f, locationListener) }

                val gnssCallback = object : GnssStatus.Callback() {
                    override fun onSatelliteStatusChanged(gnssStatus: GnssStatus) {
                        var gps = 0
                        var glonass = 0
                        var galileo = 0
                        var beidou = 0
                        var used = 0
                        for (i in 0 until gnssStatus.satelliteCount) {
                            if (gnssStatus.usedInFix(i)) used++
                            when (gnssStatus.getConstellationType(i)) {
                                GnssStatus.CONSTELLATION_GPS -> gps++
                                GnssStatus.CONSTELLATION_GLONASS -> glonass++
                                GnssStatus.CONSTELLATION_GALILEO -> galileo++
                                GnssStatus.CONSTELLATION_BEIDOU -> beidou++
                            }
                        }
                        status = status.copy(
                            hasLocationPermission = true,
                            satelliteCount = gnssStatus.satelliteCount,
                            usedInFixCount = used,
                            gpsCount = gps,
                            glonassCount = glonass,
                            galileoCount = galileo,
                            beidouCount = beidou,
                        )
                    }
                }
                val handler = Handler(Looper.getMainLooper())
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        locationManager.registerGnssStatusCallback(context.mainExecutor, gnssCallback)
                    } else {
                        locationManager.registerGnssStatusCallback(gnssCallback, handler)
                    }
                }
                onDispose {
                    runCatching { locationManager.removeUpdates(locationListener) }
                    runCatching { locationManager.unregisterGnssStatusCallback(gnssCallback) }
                }
            }
        }
    }
    return status
}

@Composable
private fun GpsMetricCard(
    label: String,
    value: String,
    hint: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f)),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.height(92.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(value, color = tint, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ConstellationPill(label: String, count: Int, tint: Color) {
    Surface(
        color = tint.copy(alpha = if (count > 0) 0.15f else 0.07f),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = if (count > 0) 0.34f else 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(7.dp).background(tint.copy(alpha = if (count > 0) 1f else 0.25f), RoundedCornerShape(99.dp))
            )
            Text("$label $count", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun compassAccuracyLabel(accuracy: Int): String = when (accuracy) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Dobar"
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Srednji"
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Loš"
    SensorManager.SENSOR_STATUS_UNRELIABLE -> "Vrlo loš"
    else -> "Nepoznato"
}

private fun compassAccuracyColor(accuracy: Int): Color = when (accuracy) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color(0xFF4CAF50)
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color(0xFFFFB300)
    SensorManager.SENSOR_STATUS_ACCURACY_LOW, SensorManager.SENSOR_STATUS_UNRELIABLE -> Color(0xFFE57373)
    else -> Color(0xFF90A4AE)
}

@Composable
private fun rememberCompassFieldStatus(): CompassFieldStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(CompassFieldStatus()) }
    var lastUpdateTimestampMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(lastUpdateTimestampMs) {
        while (true) {
            val updateTime = lastUpdateTimestampMs
            if (updateTime != null) {
                status = status.copy(lastUpdateAgeMs = System.currentTimeMillis() - updateTime)
            }
            delay(1000L)
        }
    }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val magneticSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (sensorManager == null || (rotationSensor == null && magneticSensor == null)) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD && event.values.size >= 3) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                        lastUpdateTimestampMs = System.currentTimeMillis()
                        status = status.copy(magneticMagnitudeMicroTesla = magnitude)
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, newAccuracy: Int) {
                    if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR || sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        lastUpdateTimestampMs = System.currentTimeMillis()
                        status = status.copy(accuracy = newAccuracy)
                    }
                }
            }
            rotationSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            magneticSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }
    return status
}

@Composable
internal fun rememberCompassAccuracy(): Int = rememberCompassFieldStatus().accuracy

@Composable
internal fun rememberDeviceHeadingDegrees(
    gpsBearingDeg: Float? = null,
    gpsSpeedMps: Float? = null,
    gpsBearingAccuracyDeg: Float? = null,
): Float {
    val context = LocalContext.current
    var compassHeadingDeg by remember { mutableFloatStateOf(0f) }
    var compassAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }
    var fusedHeadingDeg by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensorManager == null || rotationSensor == null) {
            onDispose { }
        } else {
            val rotationMatrix = FloatArray(9)
            val adjustedMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val rotation = context.display?.rotation ?: android.view.Surface.ROTATION_0
                    when (rotation) {
                        android.view.Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, adjustedMatrix)
                        android.view.Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, adjustedMatrix)
                        android.view.Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, adjustedMatrix)
                        else -> System.arraycopy(rotationMatrix, 0, adjustedMatrix, 0, rotationMatrix.size)
                    }
                    SensorManager.getOrientation(adjustedMatrix, orientation)
                    val azimuthDeg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                    compassHeadingDeg = azimuthDeg
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR) compassAccuracy = accuracy
                }
            }
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    LaunchedEffect(compassHeadingDeg, compassAccuracy, gpsBearingDeg, gpsSpeedMps, gpsBearingAccuracyDeg) {
        val nextHeading = chooseHybridHeading(
            compassHeadingDeg = compassHeadingDeg,
            compassAccuracy = compassAccuracy,
            gpsBearingDeg = gpsBearingDeg,
            gpsSpeedMps = gpsSpeedMps,
            gpsBearingAccuracyDeg = gpsBearingAccuracyDeg,
            currentHeadingDeg = fusedHeadingDeg,
        )
        fusedHeadingDeg = nextHeading
    }

    return fusedHeadingDeg
}

private fun chooseHybridHeading(
    compassHeadingDeg: Float,
    compassAccuracy: Int,
    gpsBearingDeg: Float?,
    gpsSpeedMps: Float?,
    gpsBearingAccuracyDeg: Float?,
    currentHeadingDeg: Float,
): Float {
    val normalizedCompass = normalizeHeading(compassHeadingDeg)
    val gpsHeading = gpsBearingDeg?.let(::normalizeHeading)
    val speed = gpsSpeedMps ?: 0f
    val gpsAcc = gpsBearingAccuracyDeg ?: 180f
    val compassReliable = compassAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH || compassAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
    val compassVeryBad = compassAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
    val gpsReliable = gpsHeading != null && speed >= 0.5f && gpsAcc <= 45f
    val preferred = when {
        gpsReliable && compassVeryBad -> gpsHeading!!
        gpsReliable && compassReliable -> blendAngles(normalizedCompass, gpsHeading!!, when {
            speed >= 2.2f -> 0.82f
            speed >= 1.6f -> 0.72f
            else -> 0.62f
        })
        gpsReliable -> blendAngles(normalizedCompass, gpsHeading!!, 0.75f)
        else -> normalizedCompass
    }
    val alpha = when {
        gpsReliable && speed >= 2.2f -> 0.34f
        gpsReliable -> 0.26f
        compassReliable -> 0.18f
        else -> 0.10f
    }
    return blendAngles(currentHeadingDeg, preferred, alpha)
}

private fun normalizeHeading(value: Float): Float = (((value % 360f) + 360f) % 360f)

private fun blendAngles(fromDeg: Float, toDeg: Float, weightTo: Float): Float {
    val from = Math.toRadians(normalizeHeading(fromDeg).toDouble())
    val to = Math.toRadians(normalizeHeading(toDeg).toDouble())
    val w = weightTo.coerceIn(0f, 1f).toDouble()
    val x = (1.0 - w) * kotlin.math.cos(from) + w * kotlin.math.cos(to)
    val y = (1.0 - w) * kotlin.math.sin(from) + w * kotlin.math.sin(to)
    if (x == 0.0 && y == 0.0) return normalizeHeading(toDeg)
    return normalizeHeading(Math.toDegrees(kotlin.math.atan2(y, x)).toFloat())
}

@Composable
internal fun CompassWidget(
    orientationMode: MapOrientationMode,
    headingDeg: Float,
    onToggle: () -> Unit
) {
    val normalizedHeading = when (orientationMode) {
        MapOrientationMode.STATIC -> 0f
        else -> ((headingDeg % 360f) + 360f) % 360f
    }
    val ringGold = Color(0xFFE8C56A)
    val ringSoft = Color(0x66F4E6B4)
    val northAccent = Color(0xFFFF7A7A)
    val faceText = Color.White.copy(alpha = 0.90f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onToggle)
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xE6222932), Color(0xD014181D))
                        ),
                        shape = RoundedCornerShape(34.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(34.dp))
            )
            Canvas(modifier = Modifier.size(52.dp)) {
                val radius = size.minDimension / 2f
                drawCircle(color = ringSoft, style = Stroke(width = 3.4f))
                drawCircle(color = Color.White.copy(alpha = 0.06f), radius = radius * 0.78f, style = Stroke(width = 1.6f))
                val tickOuter = radius - 3f
                val tickInnerLong = radius - 11f
                val tickInnerShort = radius - 8f
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30 - 90).toDouble())
                    val cosA = kotlin.math.cos(angle).toFloat()
                    val sinA = kotlin.math.sin(angle).toFloat()
                    val inner = if (i % 3 == 0) tickInnerLong else tickInnerShort
                    val tickColor = if (i == 0) northAccent.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.18f)
                    drawLine(
                        color = tickColor,
                        start = Offset(size.width / 2f + inner * cosA, size.height / 2f + inner * sinA),
                        end = Offset(size.width / 2f + tickOuter * cosA, size.height / 2f + tickOuter * sinA),
                        strokeWidth = if (i % 3 == 0) 2.2f else 1.2f
                    )
                }
            }
            Text("N", color = northAccent, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
            Text("E", color = faceText, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
            Text("S", color = faceText.copy(alpha = 0.82f), fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
            Text("W", color = faceText, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0x28161B21), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Kompas",
                    tint = ringGold,
                    modifier = Modifier
                        .size(34.dp)
                        .rotate(normalizedHeading)
                )
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(normalizedHeading)
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF0F1318), RoundedCornerShape(99.dp))
                    .border(1.5.dp, ringGold.copy(alpha = 0.85f), RoundedCornerShape(99.dp))
            )
        }
        Text(
            text = when (orientationMode) {
                MapOrientationMode.NORTH_UP -> "North-up"
                MapOrientationMode.HEADING_UP -> "Heading-up"
                MapOrientationMode.STATIC -> "Static"
            },
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}


@Composable
internal fun HomeScreen(
    onOpen: (AppTab) -> Unit,
    onCheckForUpdates: () -> Unit,
    updateCheckInProgress: Boolean
) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val items = listOf(
        Triple(language.pick("Pretraga", "Search"), Icons.Default.Search, Color(0xFF4CAF50)) to AppTab.SEARCH,
        Triple(language.pick("Karta", "Map"), Icons.Default.LocationOn, Color(0xFF2196F3)) to AppTab.MAP,
        Triple(language.pick("Slojevi", "Layers"), Icons.Default.Download, Color(0xFFFF9800)) to AppTab.OFFLINE,
        Triple(language.pick("Cloud", "Cloud"), Icons.Default.Cloud, Color(0xFF42A5F5)) to AppTab.CLOUD,
        Triple(language.pick("Tools", "Tools"), Icons.Default.Calculate, Color(0xFF9C27B0)) to AppTab.TOOLS,
        Triple(language.pick("Settings", "Settings"), Icons.Default.Settings, Color(0xFF607D8B)) to AppTab.SETTINGS,
    )
    var showReadMe by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val cardElevatedColor = MaterialTheme.colorScheme.surface
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant



    CaveScreenBackground {

        Text(
            text = SOV_DISPLAY_VERSION,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 14.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(999.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 42.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardElevatedColor),
                shape = RoundedCornerShape(30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .padding(17.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_sov),
                        contentDescription = "SOV logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            val rows = items.chunked(2)
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { (item, tab) ->
                        val (label, icon, color) = item
                        PremiumHomeMenuCard(
                            label = label,
                            icon = icon,
                            tint = color,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpen(tab) }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }



            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FooterHomeCard(
                    label = language.pick("Upute", "Read me"),
                    icon = Icons.Default.HelpOutline,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.weight(1f)
                ) { showReadMe = true }
                FooterHomeCard(
                    label = language.pick("O aplikaciji", "About"),
                    icon = Icons.Default.Info,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.weight(1f)
                ) { showAbout = true }
            }
        }


        if (showReadMe) {
            AlertDialog(
                onDismissRequest = { showReadMe = false },
                confirmButton = {
                    TextButton(onClick = { showReadMe = false }) { Text("OK") }
                },
                title = { Text(language.pick("Upute", "Read me")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(language.pick("SOV je terenska speleološka aplikacija za pregled objekata, pretragu baze, rad s kartama, snimanje trackova, unos točaka i dijeljenje podataka na terenu.", "SOV is a field speleology app for browsing objects, searching the database, working with maps, recording tracks, adding waypoints and sharing field data."))
                        Spacer(Modifier.height(4.dp))
                        Text(language.pick("Što je uključeno u ovu verziju:", "Included in this version:"))
                        Text("- offline baza speleo objekata, uključujući SoV bazu i podatke iz katastra")
                        Text("- pretraga po nazivu, lokaciji, broju pločice, tipu objekta, opisu, udaljenosti i terenskim zadacima")
                        Text("- detalji objekta s WGS84 i HTRS96/TM koordinatama, opisima, napomenama i terenskim statusom")
                        Text("- karta s markerima u različitim bojama za SOV objekte, custom točke, importane slojeve i trackove")
                        Text(language.pick("- TK25/OpenTopo base karte, hillshade, Geological Units i custom WMS slojevi", "- TK25/OpenTopo base maps, hillshade, Geological Units and custom WMS layers"))
                        Text(language.pick("- custom WMS import: naziv, URL, layer, CRS/SRS, verzija, stil, transparentnost i spremanje", "- custom WMS import: name, URL, layer, CRS/SRS, version, style, transparency and saving"))
                        Text(language.pick("- offline karte, MBTiles, importani layeri, moja baza, custom točke i spremljeni GPX trackovi", "- offline maps, MBTiles, imported layers, My Base, custom points and saved GPX tracks"))
                        Text(language.pick("- import KML, KMZ, GPX, GeoJSON/JSON, CSV, XLSX/XLSM, SHP ZIP, GPKG, MBTiles i osnovni TIFF/GeoTIFF", "- import KML, KMZ, GPX, GeoJSON/JSON, CSV, XLSX/XLSM, SHP ZIP, GPKG, MBTiles and basic TIFF/GeoTIFF"))
                        Text(language.pick("- export/share KML točaka, CSV/KML moje baze, rezultata pretrage, GPX trackova i MBTiles slojeva", "- export/share KML points, CSV/KML My Base data, search results, GPX tracks and MBTiles layers"))
                        Text("- GPS prikaz, Center, Follow, kompas, heading-up/north-up prikaz i mjerenje udaljenosti")
                        Text("- snimanje GPX trackova uz foreground service i rad u pozadini")
                        Text("- custom waypointi s nazivom, tipom, opisom, fotografijama, Google Maps otvaranjem, exportom i shareom")
                        Text("- offline zapisnik za terenski unos kada nema signala")
                        Text("- drawing pen s Undo, Save i Cancel")
                        Text("- coordinate calculator za WGS84 i HTRS96/TM")
                        Text("- automatska provjera updatea preko GitHub releaseova pri pokretanju aplikacije")
                        Spacer(Modifier.height(4.dp))
                        Text(language.pick("Osnovna ideja je da se što više terenskog rada može obaviti direktno s mobitela: pronaći objekt, provjeriti opis i koordinate, vidjeti ga na karti, dodati vlastitu točku, snimiti trag, podijeliti podatke i raditi offline kad nema signala.", "The idea is to handle as much field work as possible directly on the phone: find an object, check description and coordinates, view it on the map, add your own point, record a track, share data and work offline when there is no signal."))
                        Spacer(Modifier.height(4.dp))
                        Text(language.pick("Za updateove vrijedi pravilo: svaki build ima jasnu verziju, versionCode, changelog i GitHub release. Ako postoji novija verzija, app će pri pokretanju ponuditi skidanje APK-a.", "Updates follow a clear rule: every build has a version, versionCode, changelog and GitHub release. If a newer version exists, the app offers the APK download on startup."))
                    }
                },
                containerColor = cardElevatedColor,
                titleContentColor = primaryText,
                textContentColor = secondaryText
            )
        }

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                confirmButton = {
                    TextButton(onClick = { showAbout = false }) { Text("OK") }
                },
                title = { Text(language.pick("O aplikaciji", "About")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Card(colors = CardDefaults.cardColors(containerColor = cardElevatedColor)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Image(
                                    painter = painterResource(id = R.drawable.about_author_photo),
                                    contentDescription = "Autor",
                                    modifier = Modifier.fillMaxWidth().height(190.dp)
                                )
                            }
                        }
                        Text("SOV")
                        Text(language.pick("PUBLIC RELEASE · baza 2026 · terenski build", "PUBLIC RELEASE · database 2026 · field-ready build"))
                        Text(language.pick("Verzija: $SOV_DISPLAY_VERSION", "Version: $SOV_DISPLAY_VERSION"))
                        Text(language.pick("Ažurirano: $SOV_UPDATED_DATE_HR", "Updated: $SOV_UPDATED_DATE_EN"))
                        Text(language.pick("Autor: Darko Jeras", "Author: Darko Jeras"))
                        OutlinedButton(onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:darko.jeras@gmail.com"))
                            runCatching { context.startActivity(intent) }
                        }) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = premiumIconTint("info about"))
                            Spacer(Modifier.size(8.dp))
                            Text(language.pick("Pitanja, greške i prijedlozi", "Questions, bugs and suggestions"))
                        }
                        OutlinedButton(onClick = onCheckForUpdates, enabled = !updateCheckInProgress) {
                            if (updateCheckInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, tint = premiumIconTint("download update"))
                            }
                            Spacer(Modifier.size(8.dp))
                            Text(if (updateCheckInProgress) language.pick("Provjeravam…", "Checking…") else language.pick("Provjeri ažuriranje", "Check for update"))
                        }
                    }
                },
                containerColor = cardElevatedColor,
                titleContentColor = primaryText,
                textContentColor = secondaryText
            )
        }
    }
}


@Composable
internal fun LanguageToggleBar(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHr = language == AppLanguage.HR
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isLight) 0.94f else 0.82f),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageToggleChip(
                label = "HR",
                selected = isHr,
                modifier = Modifier.weight(1f),
                onClick = { onLanguageChange(AppLanguage.HR) }
            )
            LanguageToggleChip(
                label = "ENG",
                selected = !isHr,
                modifier = Modifier.weight(1f),
                onClick = { onLanguageChange(AppLanguage.EN) }
            )
        }
    }
}

@Composable
private fun LanguageToggleChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = if (selected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) color.copy(alpha = 0.18f) else Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = if (selected) 0.45f else 0.12f))
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
internal fun PremiumHomeMenuCard(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val shape = RoundedCornerShape(30.dp)
    Card(
        modifier = modifier
            .height(132.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 9.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isLight) 0.72f else 0.07f),
                            MaterialTheme.colorScheme.surface.copy(alpha = if (isLight) 1f else 0.96f),
                            tint.copy(alpha = if (isLight) 0.055f else 0.115f)
                        )
                    ),
                    shape
                )
                .border(
                    1.dp,
                    if (isLight) Color.Black.copy(alpha = 0.055f) else Color.White.copy(alpha = 0.07f),
                    shape
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    tint.copy(alpha = if (isLight) 0.28f else 0.38f),
                                    tint.copy(alpha = if (isLight) 0.15f else 0.20f),
                                    Color.Black.copy(alpha = if (isLight) 0.025f else 0.22f)
                                )
                            ),
                            RoundedCornerShape(23.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = if (isLight) 0.30f else 0.11f), RoundedCornerShape(23.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(18.dp)
                            .background(Color.White.copy(alpha = if (isLight) 0.24f else 0.10f), RoundedCornerShape(99.dp))
                    )
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(35.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun FooterHomeCard(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val shape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier
            .height(74.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = if (isLight) 1f else 0.96f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLight) 0.48f else 0.19f)
                        )
                    ),
                    shape
                )
                .border(1.dp, Color.White.copy(alpha = if (isLight) 0.16f else 0.06f), shape)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(tint.copy(alpha = if (isLight) 0.14f else 0.21f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            }
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@Composable
private fun SettingsStatusTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(112.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(46.dp).background(tint.copy(alpha = 0.16f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(27.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun SettingsScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onOpenGpsStatus: () -> Unit = {},
    onOpenCompassStatus: () -> Unit = {},
    onOpenSignalStatus: () -> Unit = {},
    myBaseSummary: String = "Nema učitanih KML/CSV točaka",
    myBaseMessage: String? = null,
    onImportMyBaseKml: (Uri) -> Unit = {},
    onClearMyBase: () -> Unit = {},
    onMyBaseMessageShown: () -> Unit = {}
) {
    val cardColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val context = LocalContext.current
    val myBaseKmlLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onImportMyBaseKml(uri)
    }
    val scope = rememberCoroutineScope()
    var showMyBaseClearConfirm by remember { mutableStateOf(false) }
    var showMyBaseExportMenu by remember { mutableStateOf(false) }
    var sovAuthSession by remember { mutableStateOf(SovPermissionsStore.loadSession(context)) }
    var sovPermissions by remember { mutableStateOf(SovPermissionsStore.loadPermissions(context)) }
    var sovSyncStatus by remember { mutableStateOf(sovPermissions.source) }
    var sovLoginEmail by rememberSaveable { mutableStateOf(sovAuthSession.email) }
    var sovLoginPassword by rememberSaveable { mutableStateOf("") }
    var sovRoleBusy by remember { mutableStateOf(false) }
    var sovRoleMessage by remember { mutableStateOf<String?>(null) }

    fun shareMyBaseExport(format: String) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    when (format) {
                        "csv" -> MyBaseRepository.exportCsv(context)
                        else -> MyBaseRepository.exportKml(context)
                    }
                }
            }.onSuccess { file ->
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val mimeType = if (file.extension.equals("csv", ignoreCase = true)) "text/csv" else "application/vnd.google-earth.kml+xml"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, language.pick("Izvezi Moju bazu", "Export My Base")))
            }.onFailure { throwable ->
                Toast.makeText(context, throwable.message ?: language.pick("Export nije uspio", "Export failed"), Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun refreshSovPermissions(existingSession: SovAuthSession = sovAuthSession) {
        scope.launch {
            sovRoleBusy = true
            if (existingSession.isLoggedIn && existingSession != sovAuthSession) {
                SovPermissionsStore.saveSession(context, existingSession)
                sovAuthSession = existingSession
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    SovRoleSyncManager.syncNow(context, forceNetwork = true)
                }
            }.onSuccess { state ->
                sovAuthSession = state.session
                sovPermissions = state.permissions
                sovSyncStatus = state.displayStatus
                sovRoleMessage = state.message
            }.onFailure { throwable ->
                sovRoleMessage = throwable.message ?: language.pick("Sync permissiona nije uspio.", "Permission sync failed.")
            }
            sovRoleBusy = false
        }
    }

    fun signInSovAccount() {
        if (sovLoginEmail.isBlank() || sovLoginPassword.isBlank()) {
            sovRoleMessage = language.pick("Upiši email i lozinku.", "Enter email and password.")
            return
        }
        scope.launch {
            sovRoleBusy = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val session = SovSupabaseRoleClient.signInWithPassword(sovLoginEmail, sovLoginPassword)
                    val permissions = SovSupabaseRoleClient.fetchCurrentPermissions(session.accessToken)
                    session to permissions
                }
            }.onSuccess { (session, permissions) ->
                sovLoginPassword = ""
                sovAuthSession = session
                sovPermissions = permissions
                val enrichedPermissions = permissions.copy(expiresAtMillis = session.expiresAtMillis, lastSyncOk = true, lastSyncError = "")
                sovPermissions = enrichedPermissions
                SovPermissionsStore.saveSession(context, session)
                SovPermissionsStore.savePermissions(context, enrichedPermissions)
                sovSyncStatus = "online synced"
                sovRoleMessage = language.pick("Prijava OK: ${enrichedPermissions.roleLabel}", "Sign-in OK: ${enrichedPermissions.roleLabel}")
            }.onFailure { throwable ->
                sovRoleMessage = throwable.message ?: language.pick("Prijava nije uspjela.", "Sign-in failed.")
            }
            sovRoleBusy = false
        }
    }

    fun signOutSovAccount() {
        SovPermissionsStore.clear(context)
        sovAuthSession = SovAuthSession()
        sovPermissions = SovAppPermissions()
        sovSyncStatus = "offline admin fallback"
        sovLoginEmail = ""
        sovLoginPassword = ""
        sovRoleMessage = language.pick("Odjavljen si iz SOV role sustava. App ostaje u offline admin načinu.", "Signed out from SOV roles. App remains in offline admin mode.")
    }

    LaunchedEffect(Unit) {
        if (sovAuthSession.isLoggedIn) {
            runCatching {
                withContext(Dispatchers.IO) { SovRoleSyncManager.syncNow(context, forceNetwork = false) }
            }.onSuccess { state ->
                sovAuthSession = state.session
                sovPermissions = state.permissions
                sovSyncStatus = state.displayStatus
                if (state.usedCachedPermissions && state.message.isNotBlank()) sovRoleMessage = state.message
            }
        } else {
            sovSyncStatus = sovPermissions.source
        }
    }

    LaunchedEffect(myBaseMessage) {
        val message = myBaseMessage
        if (!message.isNullOrBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onMyBaseMessageShown()
        }
    }

    if (showMyBaseClearConfirm) {
        AlertDialog(
            onDismissRequest = { showMyBaseClearConfirm = false },
            title = { Text(language.pick("Očistiti Moju bazu?", "Clear My Base?")) },
            text = {
                Text(language.pick(
                    "Ovo briše sve KML datoteke iz lokalnog /mybase foldera i uklanja tamno plave točke iz Searcha i karte.",
                    "This deletes all KML files from the local /mybase folder and removes the dark-blue points from Search and the map."
                ))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMyBaseClearConfirm = false
                        onClearMyBase()
                    }
                ) {
                    Text(language.pick("Očisti", "Clear"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMyBaseClearConfirm = false }) {
                    Text(language.pick("Odustani", "Cancel"))
                }
            }
        )
    }



    CaveScreenBackground {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 34.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(Color(0xFF42A5F5).copy(alpha = 0.18f), RoundedCornerShape(21.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF42A5F5),
                        modifier = Modifier.size(31.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(language.pick("Postavke", "Settings"), color = primaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(language.pick("Opće postavke, GPS, kompas i signal", "General settings, GPS, compass and signal"), color = secondaryText, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = language.pick("Jezik aplikacije", "App language"),
                        color = primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = language.pick("Odaberi hrvatski ili engleski prikaz.", "Choose Croatian or English UI."),
                        color = secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    LanguageToggleBar(
                        language = language,
                        onLanguageChange = onLanguageChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF7E57C2).copy(alpha = 0.20f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFB39DDB), modifier = Modifier.size(27.dp))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(language.pick("SOV role i permissioni", "SOV roles and permissions"), color = primaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                language.pick(
                                    "Trenutno: ${sovPermissions.roleLabel} · ${sovPermissions.status} · ${sovSyncStatus}",
                                    "Current: ${sovPermissions.roleLabel} · ${sovPermissions.status} · ${sovSyncStatus}"
                                ),
                                color = secondaryText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                language.pick("Zadnji sync: ${SovPermissionsStore.lastSyncLabel(context)}", "Last sync: ${SovPermissionsStore.lastSyncLabel(context)}"),
                                color = secondaryText.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Text(
                        text = language.pick(
                            "1.2.2: dodan je sync foundation — app automatski koristi spremljeni role cache kad nema mreže, pokušava obnoviti session i jasno pokazuje online/offline stanje.",
                            "1.2.2: sync foundation added — the app uses cached roles offline, tries to refresh the session and clearly shows online/offline state."
                        ),
                        color = secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = sovLoginEmail,
                        onValueChange = { sovLoginEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !sovRoleBusy,
                        singleLine = true,
                        label = { Text("Email") }
                    )
                    OutlinedTextField(
                        value = sovLoginPassword,
                        onValueChange = { sovLoginPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !sovRoleBusy,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text(language.pick("Lozinka", "Password")) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { signInSovAccount() },
                            enabled = !sovRoleBusy,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (sovRoleBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Security, contentDescription = null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(language.pick("Prijava + sync", "Sign in + sync"), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { refreshSovPermissions() },
                            enabled = !sovRoleBusy && sovAuthSession.isLoggedIn,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sync", maxLines = 1)
                        }
                    }
                    if (sovAuthSession.isLoggedIn) {
                        OutlinedButton(
                            onClick = { signOutSovAccount() },
                            enabled = !sovRoleBusy,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(language.pick("Odjavi role session", "Sign out role session"), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PermissionPill("SOV", sovPermissions.canViewSovBase)
                        PermissionPill("Katastar", sovPermissions.canViewKatastar)
                        PermissionPill("Edit", sovPermissions.canEditObjects)
                        PermissionPill("Nacrti", sovPermissions.canUploadDrawings || sovPermissions.canVerifyDrawings)
                        PermissionPill("Izleti", sovPermissions.canManageTrips)
                        PermissionPill("Oružar", sovPermissions.canManageEquipment)
                        PermissionPill("Admin", sovPermissions.canManageUsers || sovPermissions.canUseSqlTools)
                    }
                    if (sovPermissions.lastSyncError.isNotBlank()) {
                        Text(
                            language.pick("Zadnja sync greška: ${sovPermissions.lastSyncError}", "Last sync error: ${sovPermissions.lastSyncError}"),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    sovRoleMessage?.let { message ->
                        Text(message, color = secondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF0B3D91).copy(alpha = 0.22f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(27.dp))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(language.pick("Moja baza", "My Base"), color = primaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(myBaseSummary, color = secondaryText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        text = language.pick(
                            "Uvezi vlastite KML ili CSV točke, koristi ih u Searchu i prikaži ih na karti kao tamno plave objekte.",
                            "Import your own KML or CSV points, search them, and show them on the map as dark-blue objects."
                        ),
                        color = secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(language.pick("Search izvor", "Search source")) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(language.pick("tamno plave točke", "dark-blue points")) },
                            leadingIcon = { Icon(Icons.Default.Lens, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0B3D91)) }
                        )
                    }
                    Button(
                        onClick = { myBaseKmlLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(language.pick("Uvezi KML/CSV točke", "Import KML/CSV points"), maxLines = 1)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showMyBaseExportMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(language.pick("Izvezi", "Export"), maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(
                                expanded = showMyBaseExportMenu,
                                onDismissRequest = { showMyBaseExportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("CSV") },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                    onClick = {
                                        showMyBaseExportMenu = false
                                        shareMyBaseExport("csv")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("KML") },
                                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                                    onClick = {
                                        showMyBaseExportMenu = false
                                        shareMyBaseExport("kml")
                                    }
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showMyBaseClearConfirm = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(language.pick("Očisti", "Clear"), color = MaterialTheme.colorScheme.error, maxLines = 1)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsStatusTile(
                    label = "GPS",
                    icon = Icons.Default.MyLocation,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenGpsStatus
                )
                SettingsStatusTile(
                    label = "Kompas",
                    icon = Icons.Default.Explore,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCompassStatus
                )
                SettingsStatusTile(
                    label = "Signal",
                    icon = Icons.Default.SignalCellularAlt,
                    tint = Color(0xFF42A5F5),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSignalStatus
                )
            }
        }
    }
}

@Composable
private fun PermissionPill(label: String, enabled: Boolean) {
    val color = if (enabled) Color(0xFF66BB6A) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = color.copy(alpha = if (enabled) 0.16f else 0.08f),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = if (enabled) 0.34f else 0.18f))
    ) {
        Text(
            text = if (enabled) "✓ $label" else "– $label",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsGpsFieldStatusContent(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val gpsStatus = rememberGpsFieldStatus()
    val cardColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val gpsColor = gpsQualityColor(gpsStatus)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(58.dp).background(gpsColor.copy(alpha = 0.18f), RoundedCornerShape(21.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = gpsColor, modifier = Modifier.size(31.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(language.pick("GPS stanje na terenu", "GPS Field Status"), color = primaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(language.pick("Sateliti, preciznost i zadnji fix uživo", "Live satellites, accuracy and last fix"), color = secondaryText, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(gpsColor.copy(alpha = 0.13f), MaterialTheme.colorScheme.surface.copy(alpha = 0.02f))))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${gpsStatus.usedInFixCount}/${gpsStatus.satelliteCount}", color = gpsColor, fontSize = 42.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold)
                        Text(language.pick("SAT U FIXU", "SAT IN FIX"), color = secondaryText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Surface(color = gpsColor.copy(alpha = 0.16f), shape = RoundedCornerShape(999.dp), border = androidx.compose.foundation.BorderStroke(1.dp, gpsColor.copy(alpha = 0.35f))) {
                        Text(gpsQualityLabel(gpsStatus), modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = gpsColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Box(Modifier.fillMaxWidth().height(9.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(999.dp))) {
                    val ratio = if (gpsStatus.usedInFixCount <= 0) 0.08f else (gpsStatus.usedInFixCount / 14f).coerceIn(0.12f, 1f)
                    Box(Modifier.fillMaxWidth(ratio).height(9.dp).background(gpsColor, RoundedCornerShape(999.dp)))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(language.pick("Live update", "Live update"), color = secondaryText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(language.pick("zadnji fix", "last fix") + ": ${formatGpsFixAge(gpsStatus.lastFixAgeMs)}", color = gpsColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConstellationPill("GPS", gpsStatus.gpsCount, Color(0xFF66BB6A))
                    ConstellationPill("GLO", gpsStatus.glonassCount, Color(0xFF42A5F5))
                    ConstellationPill("GAL", gpsStatus.galileoCount, Color(0xFFAB47BC))
                    ConstellationPill("BDS", gpsStatus.beidouCount, Color(0xFFFFA726))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GpsMetricCard(language.pick("PRECIZNOST", "ACCURACY"), gpsStatus.accuracyMeters?.let { "±${it.roundToInt()} m" } ?: "—", gpsStatus.provider?.uppercase(Locale.ROOT) ?: "GPS", gpsColor, Modifier.weight(1f))
            GpsMetricCard(language.pick("VISINA", "ALTITUDE"), gpsStatus.altitudeMeters?.let { "${it.roundToInt()} m" } ?: "—", language.pick("visina", "altitude"), Color(0xFF80CBC4), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GpsMetricCard(language.pick("SMJER", "HEADING"), formatHeadingLabel(gpsStatus.bearingDegrees), language.pick("GPS smjer", "GPS bearing"), Color(0xFFFFD54F), Modifier.weight(1f))
            GpsMetricCard(language.pick("BRZINA", "SPEED"), gpsStatus.speedKmh?.let { "${String.format(Locale.ROOT, "%.1f", it)}" } ?: "—", "km/h", Color(0xFF64B5F6), Modifier.weight(1f))
        }
    }
}

@Composable
internal fun SettingsCompassStabilityContent(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val compassStatus = rememberCompassFieldStatus()
    val cardColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(50.dp).background(compassQualityColor(compassStatus).copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = compassQualityColor(compassStatus), modifier = Modifier.size(27.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(language.pick("Kompas", "Compass"), color = primaryText, fontWeight = FontWeight.SemiBold)
                    Text(compassQualityLabel(compassStatus), color = compassQualityColor(compassStatus), fontWeight = FontWeight.Bold)
                    Text(
                        compassQualityHint(compassStatus) + " · magnetno polje: " + (compassStatus.magneticMagnitudeMicroTesla?.let { "${it.roundToInt()} µT" } ?: "—"),
                        color = secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Zadnji update kompasa: ${formatGpsFixAge(compassStatus.lastUpdateAgeMs)}",
                        color = secondaryText.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(language.pick("Terenski savjet", "Field tip"), color = primaryText, fontWeight = FontWeight.SemiBold)
                Text(
                    language.pick("Za kompas: radi osmice dok indikator ne prijeđe u zeleno. Ako magnetno polje jako odskače, makni mobitel od auta, metala i powerbankova.", "For compass calibration: move the phone in figure-8 motions until the indicator turns green. If the magnetic field is far off, move away from cars, metal and power banks."),
                    color = secondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SpeleoRunnerToolIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.speleo_run_icon),
        contentDescription = "Speleo Run",
        modifier = modifier
    )
}


@Composable
internal fun CloudScreen(
    onOpenTrips: () -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenArchive: () -> Unit
) {
    val language = LocalAppLanguage.current
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    CaveScreenBackground {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(64.dp).background(Color(0xFF42A5F5).copy(alpha = 0.18f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF42A5F5), modifier = Modifier.size(38.dp))
                }
                Column {
                    Text(language.pick("SOV Cloud", "SOV Cloud"), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(language.pick("Izleti, oprema i arhiva na jednom mjestu", "Trips, equipment and archive in one place"), color = secondaryText)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(language.pick("Terenski cloud moduli", "Field cloud modules"), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(language.pick("Ovdje su svi moduli koji žive između weba, Supabasea i terenske APK aplikacije. Izleti ostaju prvi ulaz, ali više nisu sami na home ekranu.", "These are the modules that live between the web, Supabase and the field APK. Trips remain the first entry, but they no longer sit alone on the home screen."), color = secondaryText)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                CloudModuleCard(
                    label = language.pick("Izleti", "Trips"),
                    subtitle = language.pick("plan, offline područje, trackovi", "plan, offline area, tracks"),
                    icon = Icons.Default.Event,
                    tint = Color(0xFF795548),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTrips
                )
                CloudModuleCard(
                    label = language.pick("Oružarstvo", "Equipment"),
                    subtitle = language.pick("katalog i zahtjevi", "catalog and requests"),
                    icon = Icons.Default.Storage,
                    tint = Color(0xFF7E57C2),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenEquipment
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                CloudModuleCard(
                    label = language.pick("Arhiva", "Archive"),
                    subtitle = language.pick("nacrti i metapodaci", "drawings and metadata"),
                    icon = Icons.Default.CollectionsBookmark,
                    tint = Color(0xFF26A69A),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenArchive
                )
                CloudModuleCard(
                    label = language.pick("Sync", "Sync"),
                    subtitle = language.pick("status uskoro", "status soon"),
                    icon = Icons.Default.Security,
                    tint = Color(0xFF607D8B),
                    modifier = Modifier.weight(1f),
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun CloudModuleCard(
    label: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.heightIn(min = 154.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier.size(58.dp).background(tint.copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(34.dp))
            }
            Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ToolsScreen(
    onOpenCalculator: () -> Unit,
    onOpenSpeleoRunner: () -> Unit,
    onOpenSpeleoZapisnik: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMembership: () -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenArchive: () -> Unit
) {
    val language = LocalAppLanguage.current
    val items = listOf(
        Triple(language.pick("Kalkulator koordinata", "Coordinate calculator"), Icons.Default.Calculate, Color(0xFFBA68C8)) to { onOpenCalculator() },
        Triple("Speleo Runner", Icons.Default.PlayArrow, Color(0xFFFF7043)) to { onOpenSpeleoRunner() },
        Triple(language.pick("Speleo zapisnik", "Field log"), Icons.Default.Description, Color(0xFF26A69A)) to { onOpenSpeleoZapisnik() },
        Triple(language.pick("Kalendar izleta / akcija", "Trip / action calendar"), Icons.Default.Event, Color(0xFF42A5F5)) to { onOpenCalendar() },
        Triple(language.pick("Plati članarinu", "Pay membership"), Icons.Default.Payments, Color(0xFFFFB300)) to { onOpenMembership() },
        Triple(language.pick("Oružarstvo", "Equipment"), Icons.Default.Storage, Color(0xFF7E57C2)) to { onOpenEquipment() },
        Triple(language.pick("Arhiva i nacrti", "Archive and drawings"), Icons.Default.CollectionsBookmark, Color(0xFF26A69A)) to { onOpenArchive() },
    )
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    CaveScreenBackground {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.logo_sov),
                    contentDescription = "SOV logo",
                    modifier = Modifier.size(56.dp)
                )
                Column {
                    Text(language.pick("Alati", "Tools"), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(language.pick("Dodatni alati i prečaci", "Extra tools and shortcuts"), color = secondaryText)
                }
            }
            items.forEach { (item, action) ->
                val (label, icon, color) = item
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = action),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).background(if (MaterialTheme.colorScheme.background.luminance() > 0.5f) MaterialTheme.colorScheme.surfaceVariant else color.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (label == "Speleo Runner") {
                                SpeleoRunnerToolIcon(modifier = Modifier.size(44.dp))
                            } else {
                                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(36.dp))
                            }
                        }
                        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}



private typealias EquipmentMobileItem = EquipmentCloudItem
private typealias EquipmentMobileRequest = EquipmentCloudRequest

private data class EquipmentRequestCartLine(
    val item: EquipmentMobileItem,
    val quantity: Int = 1
)

private val equipmentMobilePreviewItems = listOf(
    EquipmentCloudItem("local-uz-001", "UZ-001", "Statičko uže 10.5 mm / 50 m", "Užad", "Zagreb", 4, 3, "Dostupno", "Lokalni fallback", subcategory = "Užad"),
    EquipmentCloudItem("local-uz-002", "UZ-002", "Transportna vreća", "Užad", "Zagreb", 6, 4, "Dostupno", "Lokalni fallback", subcategory = "Transportne vreće"),
    EquipmentCloudItem("local-post-001", "POST-001", "Karabiner HMS", "Oprema za postavljanje", "Zagreb", 30, 22, "Dostupno", "Lokalni fallback", subcategory = "Karabineri"),
    EquipmentCloudItem("local-post-002", "POST-002", "Spit pločice i ringovi", "Oprema za postavljanje", "Zagreb", 80, 62, "Dostupno", "Lokalni fallback", subcategory = "Sidrišta"),
    EquipmentCloudItem("local-crt-001", "CRT-001", "DistoX / TopoDroid komplet", "Oprema za crtanje", "Kod arhivara", 2, 1, "Dostupno", "Lokalni fallback", subcategory = "Mjerenje"),
    EquipmentCloudItem("local-os-001", "OS-001", "Pojas / sjedalica", "Osobna oprema", "Zagreb", 8, 5, "Dostupno", "Lokalni fallback", subcategory = "Pojasevi"),
    EquipmentCloudItem("local-os-002", "OS-002", "Croll / prsni bloker", "Osobna oprema", "Zagreb", 8, 6, "Dostupno", "Lokalni fallback", subcategory = "Croll / prsni blokeri"),
    EquipmentCloudItem("local-el-006", "EL-006", "Foto / rasvjeta komplet", "Elektro i foto", "Kod oružara", 3, 0, "Izdano", "Lokalni fallback", subcategory = "Rasvjeta"),
    EquipmentCloudItem("local-me-003", "ME-003", "Prva pomoć speleo komplet", "Medicinska oprema", "Krasno", 2, 2, "Dostupno", "Lokalni fallback", subcategory = "Prva pomoć")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EquipmentReadOnlyScreen(
    onBack: () -> Unit
) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val permissions = remember { SovPermissionsStore.loadPermissions(context) }
    val canArmoryManage = permissions.canManageEquipment || permissions.role.equals("admin", ignoreCase = true)
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Sve") }
    var selectedSubcategory by rememberSaveable { mutableStateOf("Sve") }
    var selectedTab by rememberSaveable { mutableStateOf("Katalog") }
    val requestCart = remember { mutableStateListOf<EquipmentRequestCartLine>() }
    var showCartDialog by rememberSaveable { mutableStateOf(false) }
    var requestFrom by rememberSaveable { mutableStateOf(equipmentIsoDateOffset(0)) }
    var requestTo by rememberSaveable { mutableStateOf(equipmentIsoDateOffset(3)) }
    var requestDatePreset by rememberSaveable { mutableStateOf("3 dana") }
    var requestManualDates by rememberSaveable { mutableStateOf(false) }
    var requestNote by rememberSaveable { mutableStateOf("") }
    var equipmentItems by remember { mutableStateOf(equipmentMobilePreviewItems) }
    var inventoryRawItems by remember { mutableStateOf(equipmentMobilePreviewItems) }
    var syncMessage by remember { mutableStateOf(language.pick("Učitavam SOV Cloud oružarstvo...", "Loading SOV Cloud equipment...")) }
    var isSyncing by remember { mutableStateOf(false) }
    var activeEquipmentSyncToken by remember { mutableStateOf(0L) }
    var equipmentSyncJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val myRequests = remember { mutableStateListOf<EquipmentMobileRequest>() }
    val armoryQueue = remember { mutableStateListOf<EquipmentMobileRequest>() }
    var inventoryLocation by rememberSaveable { mutableStateOf("Sve lokacije") }
    var inventoryCategory by rememberSaveable { mutableStateOf("Užad") }
    var inventoryCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var inventoryDone by remember { mutableStateOf<Set<String>>(emptySet()) }
    var inventoryMessage by rememberSaveable { mutableStateOf("") }
    var inventoryEditItem by remember { mutableStateOf<EquipmentMobileItem?>(null) }
    var inventoryEditValue by rememberSaveable { mutableStateOf("") }
    var returnRequest by remember { mutableStateOf<EquipmentMobileRequest?>(null) }
    var returnCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var returnDestinations by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var returnNote by rememberSaveable { mutableStateOf("") }

    fun applySnapshot(snapshot: com.darko.speleov1.util.EquipmentCloudSnapshot) {
        equipmentItems = if (snapshot.items.isEmpty()) equipmentMobilePreviewItems else snapshot.items
        inventoryRawItems = if (snapshot.rawItems.isEmpty()) equipmentItems else snapshot.rawItems
        myRequests.clear(); myRequests.addAll(snapshot.myRequests)
        armoryQueue.clear(); armoryQueue.addAll(snapshot.armoryQueue)
        syncMessage = snapshot.message
    }

    fun refreshEquipment(force: Boolean = false) {
        if (isSyncing && !force) return
        val token = System.currentTimeMillis()
        activeEquipmentSyncToken = token
        equipmentSyncJob?.cancel()
        equipmentSyncJob = scope.launch {
            isSyncing = true
            syncMessage = language.pick("Provjeravam promjene u oružarstvu...", "Checking equipment changes...")
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(60_000L) { EquipmentSupabaseRepository.loadSnapshot(context) }
                }
                if (activeEquipmentSyncToken == token) {
                    if (snapshot != null) {
                        applySnapshot(snapshot)
                    } else {
                        syncMessage = language.pick(
                            "Sync traje predugo. Provjeri vezu i stisni Osvježi opet.",
                            "Sync is taking too long. Check the connection and tap Refresh again."
                        )
                    }
                }
            } catch (t: Throwable) {
                if (activeEquipmentSyncToken == token) {
                    syncMessage = language.pick(
                        "Sync nije uspio: ${t.message.orEmpty().take(120)}",
                        "Sync failed: ${t.message.orEmpty().take(120)}"
                    )
                }
            } finally {
                if (activeEquipmentSyncToken == token) {
                    isSyncing = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        EquipmentSupabaseRepository.loadCachedSnapshot(context)?.let { cached ->
            applySnapshot(cached.copy(message = language.pick("Prikazujem spremljeni katalog, provjeravam promjene u pozadini...", "Showing cached catalog, checking for changes in the background...")))
        }
        refreshEquipment(force = true)
    }

    val categories = listOf("Sve") + equipmentItems.map { canonicalEquipmentCategoryName(it.category, it.subcategory, it.name) }.distinct().sortedWith { a, b -> compareEquipmentCategoryNames(a, b) }
    val categorySummaries = remember(equipmentItems) { buildEquipmentCategorySummaries(equipmentItems) }
    val subcategorySummaries = remember(equipmentItems, selectedCategory) {
        if (selectedCategory == "Sve") emptyList() else buildEquipmentSubcategorySummaries(equipmentItems.filter { canonicalEquipmentCategoryName(it.category, it.subcategory, it.name) == selectedCategory })
    }
    val hasSearchQuery = query.trim().isNotBlank()
    val filteredItems = if (hasSearchQuery) {
        equipmentItems
            .map { item -> item to equipmentFriendlySearchScore(item, query) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(
                compareByDescending<Pair<EquipmentMobileItem, Int>> { it.second }
                    .thenBy { it.first.name.lowercase(Locale.getDefault()) }
            )
            .map { it.first }
    } else {
        equipmentItems.filter { item ->
            val categoryMatch = selectedCategory == "Sve" || canonicalEquipmentCategoryName(item.category, item.subcategory, item.name) == selectedCategory
            val subcategoryMatch = selectedSubcategory == "Sve" || item.subcategory == selectedSubcategory
            categoryMatch && subcategoryMatch
        }
    }
    val catalogDisplayItems = remember(filteredItems) { buildEquipmentCatalogDisplayGroups(filteredItems) }
    val catalogResultMode = hasSearchQuery || selectedCategory != "Sve"
    val catalogResultTitle = when {
        hasSearchQuery -> language.pick("Rezultati pretrage", "Search results")
        selectedCategory != "Sve" -> selectedCategory
        else -> language.pick("Katalog", "Catalog")
    }
    val catalogResultSubtitle = when {
        hasSearchQuery -> language.pick("${catalogDisplayItems.size} grupa/artikala za: ${query.trim()}", "${catalogDisplayItems.size} groups/items for: ${query.trim()}")
        selectedCategory != "Sve" -> language.pick("${catalogDisplayItems.size} grupa/artikala · dodirni za detalje", "${catalogDisplayItems.size} groups/items · tap for details")
        else -> language.pick("Odaberi kategoriju", "Choose a category")
    }

    val inventoryCategories = remember(inventoryRawItems) {
        inventoryRawItems.map { canonicalEquipmentCategoryName(it.category, it.subcategory, it.name) }
            .distinct()
            .sortedWith { a, b -> compareEquipmentCategoryNames(a, b) }
    }
    val inventoryLocations = remember(inventoryRawItems) {
        listOf("Sve lokacije") + inventoryRawItems.map { it.location.ifBlank { "Nije upisano" } }.distinct().sorted()
    }
    val inventoryItems = remember(inventoryRawItems, inventoryLocation, inventoryCategory) {
        inventoryRawItems.filter { item ->
            val cat = canonicalEquipmentCategoryName(item.category, item.subcategory, item.name)
            val loc = item.location.ifBlank { "Nije upisano" }
            cat == inventoryCategory && (inventoryLocation == "Sve lokacije" || loc == inventoryLocation)
        }.sortedWith(compareBy<EquipmentMobileItem> { it.location }.thenBy { it.name.lowercase(Locale.getDefault()) })
    }
    LaunchedEffect(inventoryItems.map { it.id }.joinToString("|")) {
        inventoryCounts = inventoryItems.associate { it.id to it.total }
        inventoryDone = emptySet()
    }
    val inventoryCounted = inventoryItems.count { item -> inventoryDone.contains(item.id) || (inventoryCounts[item.id] ?: item.total) != item.total }
    val inventoryShortage = inventoryItems.count { item -> (inventoryCounts[item.id] ?: item.total) < item.total }
    val inventorySurplus = inventoryItems.count { item -> (inventoryCounts[item.id] ?: item.total) > item.total }


    if (inventoryEditItem != null) {
        val item = inventoryEditItem!!
        AlertDialog(
            onDismissRequest = { inventoryEditItem = null },
            title = { Text("Stvarni broj", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EquipmentDialogMiniHeader(item, canManage = true)
                    OutlinedTextField(
                        value = inventoryEditValue,
                        onValueChange = { inventoryEditValue = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Prebrojano") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val value = inventoryEditValue.toIntOrNull() ?: item.total
                    inventoryCounts = inventoryCounts + (item.id to value.coerceAtLeast(0))
                    inventoryDone = inventoryDone + item.id
                    inventoryEditItem = null
                }) { Text("Spremi") }
            },
            dismissButton = { TextButton(onClick = { inventoryEditItem = null }) { Text("Odustani") } }
        )
    }

    if (showCartDialog && requestCart.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showCartDialog = false },
            title = { Text(language.pick("Zahtjev za opremu", "Equipment request"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = Color(0xFF7E57C2).copy(alpha = 0.10f), shape = RoundedCornerShape(18.dp)) {
                        Text(
                            language.pick(
                                "Jedan zahtjev može imati više artikala. Oružar ga na webu i u appu vidi kao jedan paket za izdavanje.",
                                "One request can contain multiple items. The armorer sees it as one issuing package on web and in the app."
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        requestCart.forEach { line ->
                            EquipmentCartLineRow(
                                line = line,
                                onMinus = {
                                    val index = requestCart.indexOf(line)
                                    if (index >= 0) {
                                        if (line.quantity <= 1) requestCart.removeAt(index)
                                        else requestCart[index] = line.copy(quantity = line.quantity - 1)
                                    }
                                },
                                onPlus = {
                                    val index = requestCart.indexOf(line)
                                    if (index >= 0) requestCart[index] = line.copy(quantity = line.quantity + 1)
                                },
                                onRemove = { requestCart.remove(line) }
                            )
                        }
                    }
                    EquipmentRequestDateSection(
                        selectedPreset = requestDatePreset,
                        from = requestFrom,
                        to = requestTo,
                        manualDates = requestManualDates,
                        onPreset = { preset ->
                            requestDatePreset = preset
                            requestManualDates = false
                            when (preset) {
                                "Danas" -> { requestFrom = equipmentIsoDateOffset(0); requestTo = equipmentIsoDateOffset(0) }
                                "Vikend" -> { requestFrom = equipmentNextWeekendStart(); requestTo = equipmentNextWeekendEnd() }
                                "7 dana" -> { requestFrom = equipmentIsoDateOffset(0); requestTo = equipmentIsoDateOffset(7) }
                                else -> { requestFrom = equipmentIsoDateOffset(0); requestTo = equipmentIsoDateOffset(3) }
                            }
                        },
                        onToggleManual = { requestManualDates = !requestManualDates },
                        onFrom = { requestFrom = it.filter { ch -> ch.isDigit() || ch == '-' }.take(10); requestDatePreset = "Ručno" },
                        onTo = { requestTo = it.filter { ch -> ch.isDigit() || ch == '-' }.take(10); requestDatePreset = "Ručno" }
                    )
                    OutlinedTextField(
                        value = requestNote,
                        onValueChange = { requestNote = it },
                        label = { Text(language.pick("Napomena / izlet", "Note / trip")) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = requestCart.isNotEmpty() && equipmentLooksLikeIsoDate(requestFrom) && equipmentLooksLikeIsoDate(requestTo),
                    onClick = {
                    val cartSnapshot = requestCart.toList()
                    val from = requestFrom
                    val to = requestTo
                    val note = requestNote
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                EquipmentSupabaseRepository.createRequest(context, cartSnapshot.map { it.item to it.quantity }, from, to, note)
                            }
                        }.onSuccess { created ->
                            myRequests.add(0, created)
                            requestCart.clear()
                            showCartDialog = false
                            requestFrom = equipmentIsoDateOffset(0)
                            requestTo = equipmentIsoDateOffset(3)
                            requestDatePreset = "3 dana"
                            requestManualDates = false
                            requestNote = ""
                            selectedTab = "Moji"
                            Toast.makeText(context, language.pick("Zahtjev poslan", "Request sent"), Toast.LENGTH_SHORT).show()
                            refreshEquipment()
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: language.pick("Slanje nije uspjelo", "Send failed"), Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text(language.pick("Pošalji zahtjev", "Send request"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCartDialog = false }) { Text(language.pick("Nastavi birati", "Keep browsing")) } }
        )
    }


    if (returnRequest != null) {
        val req = returnRequest!!
        val lines = equipmentReturnLinesFor(req)
        AlertDialog(
            onDismissRequest = { returnRequest = null },
            title = { Text("Povrat opreme", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                    Surface(color = Color(0xFFAB47BC).copy(alpha = 0.10f), shape = RoundedCornerShape(18.dp)) {
                        Text(
                            "Po artiklu upiši koliko je vraćeno i gdje ostaje ostatak. Ako se ne vrati sve, zahtjev postaje djelomično vraćen.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    lines.forEach { line ->
                        val key = line.id.ifBlank { line.name }
                        val borrowed = line.quantity.coerceAtLeast(1)
                        val returned = (returnCounts[key] ?: borrowed).coerceIn(0, borrowed)
                        val destination = returnDestinations[key] ?: "Oružarstvo"
                        EquipmentReturnLineRow(
                            line = line,
                            returned = returned,
                            destination = destination,
                            onMinus = { returnCounts = returnCounts + (key to (returned - 1).coerceAtLeast(0)) },
                            onPlus = { returnCounts = returnCounts + (key to (returned + 1).coerceAtMost(borrowed)) },
                            onDestination = { returnDestinations = returnDestinations + (key to it) }
                        )
                    }
                    OutlinedTextField(
                        value = returnNote,
                        onValueChange = { returnNote = it },
                        label = { Text("Napomena o povratu") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val payload = lines.map { line ->
                        val key = line.id.ifBlank { line.name }
                        val borrowed = line.quantity.coerceAtLeast(1)
                        EquipmentReturnLinePayload(
                            requestItemId = line.id,
                            name = line.name,
                            borrowedQty = borrowed,
                            returnedQty = (returnCounts[key] ?: borrowed).coerceIn(0, borrowed),
                            destination = returnDestinations[key] ?: "Oružarstvo",
                            note = returnNote
                        )
                    }
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { EquipmentSupabaseRepository.returnRequestItems(context, req.id, payload, returnNote) } }
                            .onSuccess { statusHr ->
                                val index = armoryQueue.indexOfFirst { it.id == req.id }
                                if (index >= 0) armoryQueue[index] = armoryQueue[index].copy(status = statusHr)
                                returnRequest = null
                                returnCounts = emptyMap()
                                returnDestinations = emptyMap()
                                returnNote = ""
                                Toast.makeText(context, statusHr, Toast.LENGTH_SHORT).show()
                                refreshEquipment()
                            }
                            .onFailure { Toast.makeText(context, it.message ?: "Povrat nije spremljen", Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Spremi povrat") }
            },
            dismissButton = { TextButton(onClick = { returnRequest = null }) { Text("Odustani") } }
        )
    }


    CaveScreenBackground {
        LazyColumn(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = if (requestCart.isNotEmpty()) 112.dp else 36.dp)
        ) {
            item { EquipmentArmoryTopBar(onBack = onBack, canManage = canArmoryManage, message = syncMessage, isSyncing = isSyncing, onRefresh = { refreshEquipment(force = true) }) }
            item {
                EquipmentSegmentedTabs(
                    selectedTab = selectedTab,
                    canManage = canArmoryManage,
                    onSelect = { selectedTab = it }
                )
            }

            when (selectedTab) {
                "Katalog" -> {
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                if (it.isNotBlank()) selectedSubcategory = "Sve"
                            },
                            label = { Text(language.pick("Traži: uže, karabiner, pojas, krol/croll, bušilica...", "Search: rope, carabiner, harness, croll, drill...")) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotBlank()) {
                                    TextButton(onClick = { query = "" }) { Text(language.pick("Očisti", "Clear")) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(22.dp)
                        )
                    }

                    if (catalogResultMode) {
                        item {
                            EquipmentCatalogResultHeader(
                                title = catalogResultTitle,
                                subtitle = catalogResultSubtitle,
                                onBack = {
                                    query = ""
                                    selectedCategory = "Sve"
                                    selectedSubcategory = "Sve"
                                }
                            )
                        }
                        if (!hasSearchQuery && selectedCategory != "Sve" && subcategorySummaries.isNotEmpty()) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    subcategorySummaries.forEach { summary ->
                                        EquipmentCategoryPill(
                                            label = if (summary.name == "Sve") language.pick("Sve", "All") else summary.name,
                                            selected = selectedSubcategory == summary.name,
                                            onClick = { selectedSubcategory = summary.name }
                                        )
                                    }
                                }
                            }
                        }
                        if (catalogDisplayItems.isEmpty()) {
                            item { EquipmentEmptyState(language.pick("Nema rezultata", "No results"), language.pick("Pokušaj drugi naziv ili se vrati na kategorije.", "Try another name or go back to categories.")) }
                        } else {
                            items(catalogDisplayItems) { item ->
                                EquipmentMobileCard(
                                    item = item,
                                    canManage = canArmoryManage,
                                    onRequest = {
                                        val index = requestCart.indexOfFirst { it.item.id == item.id }
                                        if (index >= 0) requestCart[index] = requestCart[index].copy(quantity = requestCart[index].quantity + 1)
                                        else requestCart.add(EquipmentRequestCartLine(item, 1))
                                        Toast.makeText(context, language.pick("Dodano u zahtjev", "Added to request"), Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    } else {
                        item {
                            EquipmentSectionTitle(
                                language.pick("Odaberi opremu", "Choose equipment"),
                                language.pick("Klik na kategoriju odmah otvara artikle. Nema scrollanja do dna.", "Tap a category to open items immediately. No scrolling to the bottom.")
                            )
                        }
                        item {
                            EquipmentPriorityQuickGrid(
                                selectedCategory = selectedCategory,
                                onSelect = {
                                    query = ""
                                    selectedCategory = it
                                    selectedSubcategory = "Sve"
                                }
                            )
                        }
                        item {
                            EquipmentSectionTitle(
                                language.pick("Sve kategorije", "All categories"),
                                language.pick("Najvažnije su gore: užeta, postavljanje, crtanje i osobna oprema.", "Most important first: ropes, rigging, surveying and personal gear.")
                            )
                        }
                        item {
                            EquipmentCategoryDeck(
                                summaries = categorySummaries,
                                selectedCategory = selectedCategory,
                                canManage = canArmoryManage,
                                onSelect = {
                                    query = ""
                                    selectedCategory = it
                                    selectedSubcategory = "Sve"
                                }
                            )
                        }
                    }
                }
                "Moji" -> {
                    item { EquipmentSectionTitle(language.pick("Moji zahtjevi", "My requests"), language.pick("Brzi pregled statusa posudbe", "Fast overview of loan status")) }
                    if (myRequests.isEmpty()) {
                        item { EquipmentEmptyState(language.pick("Nema zahtjeva", "No requests"), language.pick("Kad zatražiš opremu, zahtjev će biti ovdje.", "When you request equipment, it will appear here.")) }
                    } else {
                        items(myRequests) { req -> EquipmentRequestCard(req, canManage = false) }
                    }
                }
                "Inventura" -> {
                    if (!canArmoryManage) {
                        item { EquipmentEmptyState("Inventura nije dostupna", "Ovaj mod vide samo Admin i Oružar.") }
                    } else {
                        item {
                            EquipmentInventoryHeader(
                                counted = inventoryCounted,
                                total = inventoryItems.size,
                                shortage = inventoryShortage,
                                surplus = inventorySurplus,
                                pending = EquipmentSupabaseRepository.pendingInventoryCount(context),
                                message = inventoryMessage,
                                onSyncPending = {
                                    scope.launch {
                                        val synced = withContext(Dispatchers.IO) { EquipmentSupabaseRepository.syncPendingInventories(context) }
                                        inventoryMessage = if (synced > 0) "Sinkronizirano offline inventura: $synced" else "Nema offline inventura za sync."
                                    }
                                }
                            )
                        }
                        item {
                            EquipmentInventoryFilters(
                                categories = inventoryCategories,
                                selectedCategory = inventoryCategory,
                                onCategory = { inventoryCategory = it },
                                locations = inventoryLocations,
                                selectedLocation = inventoryLocation,
                                onLocation = { inventoryLocation = it }
                            )
                        }
                        if (inventoryItems.isEmpty()) {
                            item { EquipmentEmptyState("Nema artikala za ovaj izbor", "Promijeni lokaciju ili kategoriju.") }
                        } else {
                            items(inventoryItems) { item ->
                                val counted = inventoryCounts[item.id] ?: item.total
                                val done = inventoryDone.contains(item.id) || counted != item.total
                                EquipmentInventoryCountRow(
                                    item = item,
                                    counted = counted,
                                    done = done,
                                    onConfirm = {
                                        inventoryCounts = inventoryCounts + (item.id to item.total)
                                        inventoryDone = inventoryDone + item.id
                                    },
                                    onMinus = {
                                        inventoryCounts = inventoryCounts + (item.id to (counted - 1).coerceAtLeast(0))
                                        inventoryDone = inventoryDone + item.id
                                    },
                                    onPlus = {
                                        inventoryCounts = inventoryCounts + (item.id to (counted + 1))
                                        inventoryDone = inventoryDone + item.id
                                    },
                                    onEdit = {
                                        inventoryEditItem = item
                                        inventoryEditValue = counted.toString()
                                    }
                                )
                            }
                            item {
                                Button(
                                    onClick = {
                                        val payload = inventoryItems.map { item ->
                                            val counted = inventoryCounts[item.id] ?: item.total
                                            EquipmentInventoryCountPayload(
                                                appId = item.id,
                                                sourceTable = equipmentInventorySourceTable(item.id),
                                                sourceId = equipmentInventorySourceId(item.id),
                                                code = item.code,
                                                name = item.name,
                                                category = canonicalEquipmentCategoryName(item.category, item.subcategory, item.name),
                                                subcategory = item.subcategory,
                                                location = item.location,
                                                expectedQty = item.total,
                                                countedQty = counted,
                                                unit = item.unit,
                                                note = if (counted == item.total) "OK" else "Razlika iz APK inventure"
                                            )
                                        }
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                EquipmentSupabaseRepository.submitInventoryOrQueue(
                                                    context = context,
                                                    locationName = inventoryLocation,
                                                    categoryName = inventoryCategory,
                                                    note = "APK inventura · $inventoryLocation · $inventoryCategory",
                                                    counts = payload
                                                )
                                            }
                                            inventoryMessage = result.message
                                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                            if (result.synced) refreshEquipment()
                                        }
                                    },
                                    enabled = inventoryItems.isNotEmpty() && inventoryCounted == inventoryItems.size,
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(if (inventoryCounted == inventoryItems.size) "Zaključi inventuru" else "Prebroji sve za zaključivanje", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                "Oruz" -> {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            EquipmentStatCard(language.pick("Za izdati", "To issue"), armoryQueue.count { it.status == "Zatraženo" }.toString(), Color(0xFF42A5F5), Modifier.weight(1f))
                            EquipmentStatCard(language.pick("Izdano", "Issued"), armoryQueue.count { it.status == "Izdano" }.toString(), Color(0xFFFFA726), Modifier.weight(1f))
                            EquipmentStatCard(language.pick("Djelomično", "Partial"), armoryQueue.count { it.status == "Djelomično vraćeno" }.toString(), Color(0xFFAB47BC), Modifier.weight(1f))
                        }
                    }
                    item { EquipmentSectionTitle(language.pick("Oružarski red", "Armory queue"), language.pick("Bez koraka odobravanja: zahtjev ide direktno u izdavanje.", "No approval step: requests go directly to issuing.")) }
                    items(armoryQueue) { req ->
                        EquipmentRequestCard(
                            request = req,
                            canManage = true,
                            onIssue = {
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { EquipmentSupabaseRepository.updateRequestStatus(context, req.id, "Izdano") } }
                                        .onSuccess { val index = armoryQueue.indexOf(req); if (index >= 0) armoryQueue[index] = req.copy(status = "Izdano"); refreshEquipment(force = true) }
                                        .onFailure { Toast.makeText(context, it.message ?: "Sync greška", Toast.LENGTH_LONG).show() }
                                }
                            },
                            onReturn = {
                                val lines = equipmentReturnLinesFor(req)
                                returnCounts = lines.associate { line -> (line.id.ifBlank { line.name }) to line.quantity.coerceAtLeast(1) }
                                returnDestinations = lines.associate { line -> (line.id.ifBlank { line.name }) to "Oružarstvo" }
                                returnNote = ""
                                returnRequest = req
                            }
                        )
                    }
                }
            }
        }
        if (requestCart.isNotEmpty()) {
            EquipmentCartFloatingBar(
                count = requestCart.sumOf { it.quantity },
                onOpen = { showCartDialog = true },
                onClear = { requestCart.clear() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }
    }
}

private fun equipmentIsoDateOffset(days: Int): String {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return "%04d-%02d-%02d".format(Locale.US, y, m, d)
}

private fun equipmentNextWeekendStart(): String {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_WEEK)
    val daysUntilSaturday = (Calendar.SATURDAY - today + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, daysUntilSaturday)
    return "%04d-%02d-%02d".format(Locale.US, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

private fun equipmentNextWeekendEnd(): String {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_WEEK)
    val daysUntilSunday = (Calendar.SUNDAY - today + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, if (daysUntilSunday == 0 && today != Calendar.SUNDAY) 7 else daysUntilSunday)
    return "%04d-%02d-%02d".format(Locale.US, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

private fun equipmentLooksLikeIsoDate(value: String): Boolean =
    Regex("""\d{4}-\d{2}-\d{2}""").matches(value.trim())

@Composable
private fun EquipmentArmoryTopBar(onBack: () -> Unit, canManage: Boolean, message: String, isSyncing: Boolean, onRefresh: () -> Unit) {
    val language = LocalAppLanguage.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7E57C2).copy(alpha = 0.16f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Back") }
            Box(Modifier.size(42.dp).background(Color(0xFF7E57C2).copy(alpha = 0.14f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF7E57C2))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(language.pick("Oružarstvo", "Equipment"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOf(if (canManage) language.pick("Oružar", "Armorer") else language.pick("Član", "Member"), if (isSyncing) "sync…" else message).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onRefresh, enabled = true) { Text(if (isSyncing) language.pick("Ponovi", "Retry") else language.pick("Osvježi", "Refresh")) }
        }
    }
}

@Composable
private fun EquipmentCartInlineBar(count: Int, onOpen: () -> Unit, onClear: () -> Unit) {
    Surface(color = Color(0xFF26A69A).copy(alpha = 0.12f), shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26A69A).copy(alpha = 0.24f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF26A69A))
            Text("Košarica · $count kom", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = onClear) { Text("Očisti") }
            Button(onClick = onOpen, shape = RoundedCornerShape(16.dp)) { Text("Pošalji") }
        }
    }
}


@Composable
private fun EquipmentCartFloatingBar(count: Int, onOpen: () -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF10221F).copy(alpha = 0.96f),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Color(0xFF26A69A).copy(alpha = 0.38f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(42.dp).background(Color(0xFF26A69A).copy(alpha = 0.18f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF26A69A))
            }
            Column(Modifier.weight(1f)) {
                Text("Zahtjev u pripremi", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("$count kom · pošalji sve odjednom", color = Color.White.copy(alpha = 0.70f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            TextButton(onClick = onClear) { Text("Očisti", color = Color.White.copy(alpha = 0.82f)) }
            Button(onClick = onOpen, shape = RoundedCornerShape(18.dp)) { Text("Pošalji", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun EquipmentRequestDateSection(
    selectedPreset: String,
    from: String,
    to: String,
    manualDates: Boolean,
    onPreset: (String) -> Unit,
    onToggleManual: () -> Unit,
    onFrom: (String) -> Unit,
    onTo: (String) -> Unit
) {
    val presets = listOf("Danas", "3 dana", "Vikend", "7 dana")
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF7E57C2))
            Column(Modifier.weight(1f)) {
                Text("Termin posudbe", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("$from → $to", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onToggleManual) { Text(if (manualDates) "Sakrij" else "Ručno") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            presets.forEach { preset ->
                FilterChip(
                    selected = selectedPreset == preset && !manualDates,
                    onClick = { onPreset(preset) },
                    label = { Text(preset) }
                )
            }
        }
        if (manualDates) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = from,
                    onValueChange = onFrom,
                    label = { Text("Od") },
                    placeholder = { Text("YYYY-MM-DD") },
                    isError = from.isNotBlank() && !equipmentLooksLikeIsoDate(from),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = to,
                    onValueChange = onTo,
                    label = { Text("Do") },
                    placeholder = { Text("YYYY-MM-DD") },
                    isError = to.isNotBlank() && !equipmentLooksLikeIsoDate(to),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EquipmentDateShortcut(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}

@Composable
private fun EquipmentCartLineRow(line: EquipmentRequestCartLine, onMinus: () -> Unit, onPlus: () -> Unit, onRemove: () -> Unit) {
    val item = line.item
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(item.displayName.ifBlank { item.name }, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(item.code, item.subcategory).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = onMinus, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("−") }
            Text(line.quantity.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)
            OutlinedButton(onClick = onPlus, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("+") }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
        }
    }
}

@Composable
private fun EquipmentPremiumHero(onBack: () -> Unit, canManage: Boolean) {
    val language = LocalAppLanguage.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF171126).copy(alpha = 0.94f),
                            Color(0xFF1B2A33).copy(alpha = 0.90f),
                            Color(0xFF28304A).copy(alpha = 0.88f)
                        )
                    ),
                    RoundedCornerShape(32.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(32.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Back", tint = Color.White)
                    }
                    Box(
                        modifier = Modifier.size(58.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFFB388FF), modifier = Modifier.size(34.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(language.pick("Oružarstvo", "Equipment"), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(language.pick("Katalog je grupiran, inventura broji stvarne stavke", "Grouped catalog, raw inventory counting"), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    EquipmentGlassChip(language.pick("Web = izvor istine", "Web = source of truth"), Color(0xFF26A69A))
                    EquipmentGlassChip(if (canManage) language.pick("Oružar način", "Armorer mode") else language.pick("Korisnik", "User mode"), Color(0xFFB388FF))
                    EquipmentGlassChip(language.pick("Bez rizičnog editiranja", "No risky editing"), Color(0xFFFFCA28))
                }
            }
        }
    }
}

@Composable
private fun EquipmentSyncStatusCard(message: String, isSyncing: Boolean, onRefresh: () -> Unit) {
    val language = LocalAppLanguage.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26A69A).copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(42.dp).background(Color(0xFF26A69A).copy(alpha = 0.12f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Icon(if (isSyncing) Icons.Default.Cloud else Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF26A69A))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(language.pick("Sync oružarstva", "Equipment sync"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onRefresh, enabled = true) { Text(if (isSyncing) language.pick("Ponovi", "Retry") else language.pick("Osvježi", "Refresh")) }
        }
    }
}

@Composable
private fun EquipmentRoleStrip(canManage: Boolean) {
    val language = LocalAppLanguage.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (canManage) language.pick("Oružar može obrađivati zahtjeve, ali inventar se i dalje uređuje na webu.", "Armorer can handle requests, but inventory editing stays on the web.")
                else language.pick("Pregledaj opremu, provjeri dostupnost i pripremi zahtjev za posudbu.", "Browse equipment, check availability and prepare a loan request."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                EquipmentSoftChip(language.pick("Katalog", "Catalog"), Color(0xFF7E57C2))
                EquipmentSoftChip(language.pick("Zahtjevi", "Requests"), Color(0xFF26A69A))
                if (canManage) EquipmentSoftChip(language.pick("Inventura", "Inventory"), Color(0xFF66BB6A))
                if (canManage) EquipmentSoftChip(language.pick("Posudbe", "Loans"), Color(0xFFFFA726))
            }
        }
    }
}

@Composable
private fun EquipmentSegmentedTabs(selectedTab: String, canManage: Boolean, onSelect: (String) -> Unit) {
    val language = LocalAppLanguage.current
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EquipmentTabButton(language.pick("Katalog", "Catalog"), selectedTab == "Katalog") { onSelect("Katalog") }
        EquipmentTabButton(language.pick("Moji zahtjevi", "My requests"), selectedTab == "Moji") { onSelect("Moji") }
        if (canManage) EquipmentTabButton(language.pick("Inventura", "Inventory"), selectedTab == "Inventura") { onSelect("Inventura") }
        if (canManage) EquipmentTabButton(language.pick("Oružar red", "Armory queue"), selectedTab == "Oruz") { onSelect("Oruz") }
    }
}

@Composable
private fun EquipmentTabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF7E57C2).copy(alpha = 0.92f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Text(
            label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}


private val equipmentCategoryOrder = listOf(
    "Sve",
    "Užad",
    "Oprema za postavljanje",
    "Oprema za crtanje",
    "Osobna oprema - komplet",
    "Osobna oprema",
    "Bušilice i baterije",
    "Elektro i foto oprema",
    "Oprema za logor",
    "Medicinska oprema",
    "Oprema za proširivanje",
    "Čisto podzemlje",
    "Ronilačka oprema",
    "Alpinistička oprema",
    "Ostali alat",
    "Ostalo"
)

private fun compareEquipmentCategoryNames(a: String, b: String): Int {
    val ca = canonicalEquipmentCategoryName(a, "")
    val cb = canonicalEquipmentCategoryName(b, "")
    val ai = equipmentCategoryOrder.indexOf(ca).let { if (it < 0) 999 else it }
    val bi = equipmentCategoryOrder.indexOf(cb).let { if (it < 0) 999 else it }
    return if (ai != bi) ai - bi else ca.compareTo(cb, ignoreCase = true)
}

private fun canonicalEquipmentCategoryName(category: String, subcategory: String, name: String = ""): String {
    // v1.4.2: SQL view is the only canonical armory brain.
    // APK only trusts the already-normalized category coming from Supabase and keeps a tiny fallback.
    val clean = category.trim()
    return clean.ifBlank {
        when {
            subcategory.isNotBlank() -> subcategory.trim()
            name.isNotBlank() -> "Ostalo"
            else -> "Ostalo"
        }
    }
}


private fun equipmentCategorySubtitle(name: String): String = when (name) {
    "Užad" -> "statika · transportne vreće · trake"
    "Oprema za postavljanje" -> "karabineri · spitovi · sidrišta"
    "Oprema za crtanje" -> "Disto · TopoDroid · pribor"
    "Osobna oprema - komplet" -> "pojas · croll · descender · bloker"
    "Osobna oprema" -> "kacige · lampe · odjeća"
    "Bušilice i baterije" -> "bušilice · baterije · punjači"
    "Elektro i foto oprema" -> "rasvjeta · foto · baterije"
    "Medicinska oprema" -> "prva pomoć · sigurnost"
    "Oprema za logor" -> "bivak · kuhinja · kamp"
    "Sve" -> "cijeli katalog"
    else -> "oprema i pribor"
}

private fun equipmentCategoryEmoji(name: String): String {
    val c = canonicalEquipmentCategoryName(name, "").lowercase(Locale.getDefault())
    return when {
        "užad" in c || "uzad" in c -> "🪢"
        "postavljanje" in c -> "🔩"
        "crtanje" in c -> "✏️"
        "osobna" in c -> "⛑️"
        "bušil" in c || "busil" in c -> "🔋"
        "elektro" in c || "foto" in c -> "🔦"
        "med" in c -> "🩹"
        "logor" in c -> "⛺"
        "ron" in c -> "🤿"
        "sve" in c -> "◈"
        else -> "◇"
    }
}

private data class EquipmentCategorySummary(
    val name: String,
    val count: Int,
    val total: Int,
    val available: Int,
    val service: Int,
    val issued: Int,
    val accent: Color,
    val icon: ImageVector
)

private fun buildEquipmentCategorySummaries(items: List<EquipmentMobileItem>): List<EquipmentCategorySummary> {
    val grouped = items.groupBy { canonicalEquipmentCategoryName(it.category, it.subcategory, it.name) }
    val all = EquipmentCategorySummary(
        name = "Sve",
        count = items.size,
        total = items.sumOf { it.total },
        available = items.sumOf { it.available },
        service = items.count { it.status.equals("Servis", true) || it.status.equals("Service", true) },
        issued = items.count { it.status.equals("Izdano", true) || it.status.equals("Issued", true) || it.available <= 0 && it.total > 0 },
        accent = Color(0xFF7E57C2),
        icon = Icons.Default.Storage
    )
    return listOf(all) + grouped.toList().sortedWith { a, b -> compareEquipmentCategoryNames(a.first, b.first) }.map { (category, categoryItems) ->
        val visual = equipmentCategoryVisual(category)
        EquipmentCategorySummary(
            name = category,
            count = categoryItems.size,
            total = categoryItems.sumOf { it.total },
            available = categoryItems.sumOf { it.available },
            service = categoryItems.count { it.status.equals("Servis", true) || it.status.equals("Service", true) },
            issued = categoryItems.count { it.status.equals("Izdano", true) || it.status.equals("Issued", true) || it.available <= 0 && it.total > 0 },
            accent = visual.first,
            icon = visual.second
        )
    }
}

private fun buildEquipmentSubcategorySummaries(items: List<EquipmentMobileItem>): List<EquipmentCategorySummary> {
    val grouped = items.groupBy { it.subcategory.ifBlank { "Ostalo" } }
    val all = EquipmentCategorySummary(
        name = "Sve",
        count = items.size,
        total = items.sumOf { it.total },
        available = items.sumOf { it.available },
        service = items.count { it.status.equals("Servis", true) || it.status.equals("Service", true) },
        issued = items.count { it.status.equals("Izdano", true) || it.status.equals("Issued", true) || it.available <= 0 && it.total > 0 },
        accent = Color(0xFF7E57C2),
        icon = Icons.Default.CollectionsBookmark
    )
    return listOf(all) + grouped.toList().sortedWith { a, b -> compareEquipmentSubcategoryNames(a.first, b.first) }.map { (subcategory, subItems) ->
        val visual = equipmentCategoryVisual(subcategory)
        EquipmentCategorySummary(
            name = subcategory,
            count = subItems.size,
            total = subItems.sumOf { it.total },
            available = subItems.sumOf { it.available },
            service = subItems.count { it.status.equals("Servis", true) || it.status.equals("Service", true) },
            issued = subItems.count { it.status.equals("Izdano", true) || it.status.equals("Issued", true) || it.available <= 0 && it.total > 0 },
            accent = visual.first,
            icon = visual.second
        )
    }
}


private val equipmentSubcategoryOrder = listOf(
    "Sve",
    "Užad",
    "Transportne vreće",
    "Prusici",
    "Gurtne i trake",
    "Karabineri",
    "Sidrišta",
    "Spitovi i sidrišta",
    "Pločice / ringovi",
    "Mjerenje",
    "Disto / TopoDroid",
    "Crtaći pribor",
    "Pojasevi",
    "Pojasevi i sjedalice",
    "Croll / prsni blokeri",
    "Descenderi",
    "Ručni blokeri",
    "Pedale / stremeni",
    "Kacige",
    "Lampe i rasvjeta",
    "Odjeća i obuća",
    "Bušilice",
    "Baterije",
    "Punjači",
    "Svrdla",
    "Ostalo"
)

private fun compareEquipmentSubcategoryNames(a: String, b: String): Int {
    val ai = equipmentSubcategoryOrder.indexOf(a).let { if (it < 0) 999 else it }
    val bi = equipmentSubcategoryOrder.indexOf(b).let { if (it < 0) 999 else it }
    return if (ai != bi) ai - bi else a.compareTo(b, ignoreCase = true)
}

private fun normalizeEquipmentSearchText(value: String): String = value
    .replace("Č", "C").replace("č", "c")
    .replace("Ć", "C").replace("ć", "c")
    .replace("Š", "S").replace("š", "s")
    .replace("Ž", "Z").replace("ž", "z")
    .replace("Đ", "D").replace("đ", "d")
    .lowercase(Locale.getDefault())
    .trim()

private fun equipmentSearchAliases(query: String): List<String> {
    val q = normalizeEquipmentSearchText(query)
    if (q.isBlank()) return emptyList()
    val aliases = linkedSetOf(q)
    fun add(vararg values: String) { values.forEach { aliases.add(normalizeEquipmentSearchText(it)) } }
    when {
        q.contains("croll") || q.contains("krol") || q.contains("crol") || q.contains("prsni") -> add("croll", "krol", "crol", "prsni bloker", "prsni", "bloker")
        q.contains("karab") || q.contains("karb") || q.contains("hms") || q.contains("matica") -> add("karabiner", "karabin", "karbiner", "karab", "hms", "matica", "spojka", "maillon", "omni", "triact")
        q.contains("pojas") || q.contains("sjed") || q.contains("gurtna") -> add("pojas", "pojasevi", "sjedalica", "sjedni pojas", "gurtna")
        q.contains("uze") || q.contains("uzad") || q.contains("rope") || q.contains("strik") -> add("uze", "uzad", "uže", "užad", "rope", "strik", "staticko", "staticno", "statičko")
        q.contains("stop") || q.contains("desc") || q.contains("rig") || q == "id" || q.contains("maestro") -> add("stop", "descender", "descenderi", "rig", "id", "maestro", "spustalica")
        q.contains("zumar") || q.contains("rucni") || q.contains("ruč") || q.contains("asc") -> add("zumar", "rucni bloker", "ručni bloker", "ascender")
        q.contains("crtan") || q.contains("crtac") || q.contains("nacrt") || q.contains("disto") || q.contains("topo") -> add("crtaci pribor", "crtaći pribor", "nacrt", "disto", "distox", "topodroid", "kompas", "klinometar", "mjerenje")
        q.contains("postav") || q.contains("sidr") || q.contains("spit") || q.contains("anker") || q.contains("bolt") -> add("postavljanje", "sidriste", "sidrište", "spit", "anker", "bolt", "plocica", "pločica", "ring")
        q.contains("lampa") || q.contains("svjet") || q.contains("rasv") || q.contains("ceon") -> add("lampa", "rasvjeta", "svjetlo", "ceona", "čeona")
        q.contains("busil") || q.contains("bušil") || q.contains("bater") || q.contains("hilti") || q.contains("bosch") -> add("busilica", "bušilica", "baterija", "punjac", "punjač", "bosch", "hilti", "makita", "svrdlo")
        q.contains("kac") || q.contains("helm") -> add("kaciga", "kacige", "helmet")
        q.contains("olov") || q.contains("papir") || q.contains("pribor") -> add("crtaci pribor", "crtaći pribor", "olovka", "papir", "disto", "topodroid", "mjerenje")
    }
    return aliases.filter { it.length >= 2 }.toList()
}

private fun equipmentFocusedSearchAliases(query: String): List<String> {
    val q = normalizeEquipmentSearchText(query)
    if (q.isBlank()) return emptyList()
    val focused = linkedSetOf<String>()
    fun add(vararg values: String) { values.forEach { focused.add(normalizeEquipmentSearchText(it)) } }
    when {
        q.contains("croll") || q.contains("krol") || q.contains("crol") || q.contains("prsni") -> add("croll", "krol", "crol", "prsni", "prsni bloker")
        q.contains("karab") || q.contains("karb") || q.contains("hms") || q.contains("matica") -> add("karabiner", "karabin", "karbiner", "karab", "hms", "matica", "spojka", "maillon", "omni", "triact")
        q.contains("pojas") || q.contains("sjed") -> add("pojas", "pojasevi", "sjedalica", "sjedni pojas")
        q.contains("stop") || q.contains("desc") || q.contains("rig") || q == "id" -> add("stop", "descender", "rig", "id", "maestro", "spustalica")
        q.contains("zumar") || q.contains("rucni") || q.contains("ruč") -> add("zumar", "rucni bloker", "ručni bloker", "ascender")
        q.contains("kac") || q.contains("helm") -> add("kaciga", "kacige", "helmet")
    }
    return focused.toList()
}

private fun equipmentSearchTokens(value: String): List<String> = normalizeEquipmentSearchText(value)
    .replace(Regex("[^a-z0-9]+"), " ")
    .split(" ")
    .map { it.trim() }
    .filter { it.length >= 2 }

private fun equipmentFriendlySearchScore(item: EquipmentMobileItem, query: String): Int {
    val aliases = equipmentSearchAliases(query)
    if (aliases.isEmpty()) return 1

    val name = normalizeEquipmentSearchText(item.name)
    val subcategory = normalizeEquipmentSearchText(item.subcategory)
    val category = normalizeEquipmentSearchText(canonicalEquipmentCategoryName(item.category, item.subcategory, item.name))
    val code = normalizeEquipmentSearchText(item.code)
    val note = normalizeEquipmentSearchText(item.note)
    val location = normalizeEquipmentSearchText(item.location)
    val searchText = normalizeEquipmentSearchText(item.searchText)
    val focusedHaystack = listOf(name, subcategory, category, code, note).joinToString(" ")
    val fullHaystack = listOf(focusedHaystack, location, item.status, item.unit, searchText).joinToString(" ")

    val focusedAliases = equipmentFocusedSearchAliases(query)
    if (focusedAliases.isNotEmpty() && focusedAliases.none { focusedHaystack.contains(it) }) {
        // Important: do not let stale/global search_text match every row for narrow searches like croll/karabiner/pojas.
        return 0
    }

    var score = 0
    aliases.forEach { alias ->
        if (alias.isBlank()) return@forEach
        if (name == alias || code == alias || subcategory == alias) score += 220
        if (name.contains(alias)) score += 140
        if (subcategory.contains(alias)) score += 120
        if (code.contains(alias)) score += 105
        if (category.contains(alias)) score += 75
        if (note.contains(alias)) score += 40
        if (searchText.contains(alias)) score += 18
        if (fullHaystack.contains(alias)) score += 8
    }

    val tokens = equipmentSearchTokens(query)
    if (tokens.isNotEmpty() && tokens.all { token -> fullHaystack.contains(token) }) score += 45
    return score
}

private fun equipmentMatchesFriendlySearch(item: EquipmentMobileItem, query: String): Boolean =
    equipmentFriendlySearchScore(item, query) > 0

private fun equipmentCategoryVisual(category: String): Pair<Color, ImageVector> {
    val c = canonicalEquipmentCategoryName(category, "").lowercase(Locale.getDefault())
    return when {
        "užad" in c || "uzad" in c -> Color(0xFF3EA7FF) to Icons.Default.Straighten
        "postavljanje" in c -> Color(0xFFFFB547) to Icons.Default.Security
        "crtanje" in c -> Color(0xFFC77DFF) to Icons.Default.Edit
        "osobna" in c -> Color(0xFF43D17A) to Icons.Default.CheckCircle
        "bušil" in c || "busil" in c -> Color(0xFFFF8A50) to Icons.Default.Settings
        "elektro" in c || "foto" in c -> Color(0xFFFFD54F) to Icons.Default.LightMode
        "med" in c -> Color(0xFFFF5C7A) to Icons.Default.HelpOutline
        "logor" in c -> Color(0xFF27C7A8) to Icons.Default.Explore
        "ron" in c || "alpin" in c -> Color(0xFF6D7DFF) to Icons.Default.Layers
        else -> Color(0xFF8F7CFF) to Icons.Default.Storage
    }
}


@Composable
private fun EquipmentCatalogResultHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF101820).copy(alpha = 0.88f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(onClick = onBack, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(LocalAppLanguage.current.pick("Kategorije", "Categories"))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentPriorityQuickGrid(
    selectedCategory: String,
    onSelect: (String) -> Unit
) {
    val language = LocalAppLanguage.current
    val priorities = listOf(
        Triple("Užad", "🪢", language.pick("užeta, transportne vreće", "ropes, rope bags")),
        Triple("Oprema za postavljanje", "🔩", language.pick("karabineri, sidrišta, spitovi", "carabiners, anchors, bolts")),
        Triple("Oprema za crtanje", "📐", language.pick("Disto, TopoDroid, pribor", "Disto, TopoDroid, tools")),
        Triple("Osobna oprema", "⛑️", language.pick("pojas, croll/krol, kaciga", "harness, croll, helmet"))
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16).copy(alpha = 0.96f)),
        shape = RoundedCornerShape(30.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(42.dp).background(
                        Brush.linearGradient(listOf(Color(0xFF7E57C2), Color(0xFF26A69A))),
                        RoundedCornerShape(16.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) { Text("⚡", fontSize = 22.sp) }
                Column(Modifier.weight(1f)) {
                    Text(language.pick("Brzi odabir opreme", "Quick equipment pick"), color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Text(language.pick("Najčešće terenske kategorije prve", "Most-used field categories first"), color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                priorities.forEach { (category, glyph, subtitle) ->
                    val selected = selectedCategory == category
                    val visual = equipmentCategoryVisual(category)
                    Surface(
                        onClick = { onSelect(category) },
                        modifier = Modifier.weight(1f).fillMaxWidth(0.48f).heightIn(min = 112.dp),
                        color = if (selected) visual.first.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.065f),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) visual.first.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.10f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Text(glyph, fontSize = 32.sp)
                                if (selected) {
                                    Surface(color = visual.first.copy(alpha = 0.24f), shape = RoundedCornerShape(999.dp)) {
                                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            Text(category, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(subtitle, color = Color.White.copy(alpha = 0.64f), style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun equipmentPublicAvailabilityLabel(available: Int): String = if (available > 0) "Dostupno" else "Nije dostupno"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentCategoryDeck(
    summaries: List<EquipmentCategorySummary>,
    selectedCategory: String,
    canManage: Boolean,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        summaries.forEach { summary ->
            EquipmentCategoryTile(
                summary = summary,
                selected = selectedCategory == summary.name,
                canManage = canManage,
                onClick = { onSelect(summary.name) },
                modifier = Modifier.weight(1f).fillMaxWidth(0.48f)
            )
        }
    }
}

@Composable
private fun EquipmentCategoryTile(
    summary: EquipmentCategorySummary,
    selected: Boolean,
    canManage: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val availabilityRatio = if (summary.total <= 0) 0f else summary.available.toFloat() / summary.total.toFloat()
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 128.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) summary.accent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) summary.accent.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(46.dp).background(
                        Brush.linearGradient(listOf(summary.accent.copy(alpha = 0.28f), summary.accent.copy(alpha = 0.08f))),
                        RoundedCornerShape(17.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(equipmentCategoryEmoji(summary.name), fontSize = 25.sp)
                }
                Surface(
                    color = summary.accent.copy(alpha = if (selected) 0.28f else 0.12f),
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, summary.accent.copy(alpha = 0.20f))
                ) {
                    Text(
                        if (canManage) "${summary.count}" else equipmentPublicAvailabilityLabel(summary.available),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Text(summary.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(equipmentCategorySubtitle(summary.name), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (canManage) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${summary.available}/${summary.total} dostupno", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    if (summary.service > 0) Text("${summary.service} servis", color = Color(0xFFFFA726), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.fillMaxWidth().height(7.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), RoundedCornerShape(999.dp))) {
                    Box(Modifier.fillMaxWidth(availabilityRatio.coerceIn(0f, 1f)).height(7.dp).background(summary.accent, RoundedCornerShape(999.dp)))
                }
            } else {
                Text(
                    if (summary.available > 0) "Dostupno za zahtjev" else "Trenutno nije dostupno",
                    color = if (summary.available > 0) Color(0xFF26A69A) else Color(0xFFEF5350),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EquipmentSubcategoryDeck(
    summaries: List<EquipmentCategorySummary>,
    selectedSubcategory: String,
    onSelect: (String) -> Unit,
    canManage: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        summaries.forEach { summary ->
            val selected = selectedSubcategory == summary.name
            Surface(
                onClick = { onSelect(summary.name) },
                color = if (selected) summary.accent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) summary.accent.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.widthIn(min = 132.dp).padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Icon(summary.icon, contentDescription = null, tint = summary.accent, modifier = Modifier.size(23.dp))
                    Column {
                        Text(summary.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (canManage) "${summary.count} · ${summary.available}/${summary.total}" else equipmentPublicAvailabilityLabel(summary.available), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentCategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF26A69A).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color(0xFF26A69A).copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun EquipmentGlassChip(label: String, color: Color) {
    Surface(color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(999.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.36f))) {
        Text(label, color = Color.White.copy(alpha = 0.90f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
    }
}

@Composable
private fun EquipmentSoftChip(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(999.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
    }
}

@Composable
private fun EquipmentSectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EquipmentStatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.width(34.dp).height(4.dp).background(accent, RoundedCornerShape(999.dp)))
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EquipmentEmptyState(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)), shape = RoundedCornerShape(28.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Column(Modifier.fillMaxWidth().padding(26.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(54.dp).background(Color(0xFF7E57C2).copy(alpha = 0.12f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB388FF), modifier = Modifier.size(32.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

private fun buildEquipmentCatalogDisplayGroups(items: List<EquipmentMobileItem>): List<EquipmentMobileItem> {
    if (items.isEmpty()) return emptyList()
    // v1.4.1: Supabase grouped view already returns catalog groups.
    // Do not stack those rows again; inventory uses rawItems separately.
    if (items.any { it.id.startsWith("group:") || it.groupKey.isNotBlank() && it.variantCount > 1 }) {
        return items.sortedWith(compareBy<EquipmentMobileItem> { normalizeEquipmentSearchText(canonicalEquipmentCategoryName(it.category, it.subcategory, it.displayName.ifBlank { it.name })) }
            .thenBy { compareEquipmentSubcategorySortKey(it.subcategory) }
            .thenBy { normalizeEquipmentSearchText(it.displayName.ifBlank { it.name }) })
    }
    return items.groupBy { equipmentCatalogGroupKey(it) }.values.map { group ->
        val first = group.first()
        if (group.size == 1 && first.variantCount <= 1) {
            val display = first.displayName.ifBlank { first.name }
            first.copy(name = first.name, displayName = display, detailSummary = first.detailSummary.ifBlank { first.note })
        } else {
            val total = group.sumOf { it.total }
            val available = group.sumOf { it.available }
            val status = when {
                group.any { it.status.equals("Servis", true) } -> "Servis"
                available > 0 -> "Dostupno"
                else -> "Izdano"
            }
            val display = equipmentCatalogGroupDisplayName(first)
            val details = group
                .sortedWith(compareBy<EquipmentMobileItem> { it.location }.thenBy { it.name.lowercase(Locale.getDefault()) })
                .take(8)
                .joinToString(" · ") { row -> listOf(row.code, row.name, row.location).filter { it.isNotBlank() }.joinToString(" / ") }
                .let { if (group.size > 8) "$it · +${group.size - 8} još" else it }
            first.copy(
                id = "group:${equipmentCatalogGroupKey(first)}",
                code = if (group.size > 1) "${group.size} stavki" else first.code,
                name = first.name,
                displayName = display,
                total = total,
                available = available,
                status = status,
                note = details,
                searchText = group.joinToString(" ") { listOf(it.searchText, it.name, it.code, it.note).joinToString(" ") },
                variantCount = group.sumOf { it.variantCount.coerceAtLeast(1) },
                detailSummary = details
            )
        }
    }.sortedWith(compareBy<EquipmentMobileItem> { normalizeEquipmentSearchText(canonicalEquipmentCategoryName(it.category, it.subcategory, it.displayName.ifBlank { it.name })) }
        .thenBy { compareEquipmentSubcategorySortKey(it.subcategory) }
        .thenBy { normalizeEquipmentSearchText(it.displayName.ifBlank { it.name }) })
}

private fun equipmentCatalogGroupKey(item: EquipmentMobileItem): String {
    val explicit = item.groupKey.ifBlank { item.displayName }
    val category = canonicalEquipmentCategoryName(item.category, item.subcategory, item.displayName.ifBlank { item.name })
    val sub = item.subcategory.ifBlank { "Ostalo" }
    val basis = explicit.ifBlank { item.name }
    return normalizeEquipmentSearchText("$category|$sub|$basis").replace(Regex("[^a-z0-9]+"), "-").trim('-')
}

private fun equipmentCatalogGroupDisplayName(item: EquipmentMobileItem): String {
    item.displayName.ifBlank { "" }.let { if (it.isNotBlank()) return it }
    val sub = item.subcategory.ifBlank { "Ostalo" }
    val n = normalizeEquipmentSearchText(item.name)
    return when {
        sub.equals("Descenderi", true) -> "Descenderi"
        sub.equals("Croll / prsni blokeri", true) -> "Croll / prsni blokeri"
        sub.equals("Ručni blokeri", true) -> "Ručni blokeri"
        sub.equals("Pojasevi i sjedalice", true) -> "Pojasevi / sjedalice"
        sub.equals("Pedale / stremeni", true) -> "Pedale / stremeni"
        sub.equals("Karabineri", true) -> "Karabineri"
        "stop" in n -> "STOP descenderi"
        "rig" in n -> "RIG descenderi"
        else -> item.name
    }
}

private fun compareEquipmentSubcategorySortKey(value: String): Int = equipmentSubcategoryOrder.indexOf(value).let { if (it < 0) 999 else it }

@Composable
private fun EquipmentMobileCard(item: EquipmentMobileItem, canManage: Boolean, onRequest: () -> Unit) {
    val statusColor = equipmentStatusColor(item.status)
    val displayTitle = item.displayName.ifBlank { item.name }
    val categoryName = canonicalEquipmentCategoryName(item.category, item.subcategory, displayTitle)
    val categoryAccent = equipmentCategoryVisual(categoryName).first
    val ratio = if (item.total <= 0) 0f else item.available.toFloat() / item.total.toFloat()
    val publicAvailable = item.available > 0 && !item.status.equals("Servis", true) && !item.status.equals("Otpisano", true)
    val publicStatusLabel = if (publicAvailable) "Dostupno" else "Nije dostupno"
    val publicStatusColor = if (publicAvailable) Color(0xFF26A69A) else Color(0xFFEF5350)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(30.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.16f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(56.dp).background(
                        Brush.linearGradient(listOf(categoryAccent.copy(alpha = 0.30f), statusColor.copy(alpha = 0.10f))),
                        RoundedCornerShape(20.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(equipmentCategoryEmoji(categoryName), fontSize = 27.sp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(displayTitle, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${item.code} · $categoryName · ${item.subcategory.ifBlank { item.category }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        EquipmentSoftChip(item.location, Color(0xFF7E57C2))
                        if (item.variantCount > 1) EquipmentSoftChip("${item.variantCount} stavki", Color(0xFF42A5F5))
                        EquipmentSoftChip(if (canManage) item.status else publicStatusLabel, if (canManage) statusColor else publicStatusColor)
                    }
                }
            }
            if (canManage) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.available}/${item.total} dostupno", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        Text(item.detailSummary.ifBlank { item.note }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 10.dp), textAlign = TextAlign.End)
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), RoundedCornerShape(999.dp))) {
                        Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).height(8.dp).background(statusColor, RoundedCornerShape(999.dp)))
                    }
                }
            } else {
                Surface(
                    color = publicStatusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, publicStatusColor.copy(alpha = 0.28f))
                ) {
                    Text(
                        if (publicAvailable) "Možeš poslati zahtjev" else "Trenutno nije dostupno",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Button(
                onClick = onRequest,
                enabled = publicAvailable,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (publicAvailable) "Dodaj u zahtjev" else "Nije dostupno", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EquipmentDialogMiniHeader(item: EquipmentMobileItem, canManage: Boolean) {
    val color = equipmentStatusColor(item.status)
    val displayTitle = item.displayName.ifBlank { item.name }
    val categoryName = canonicalEquipmentCategoryName(item.category, item.subcategory, displayTitle)
    Surface(color = color.copy(alpha = 0.10f), shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.24f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(42.dp).background(color.copy(alpha = 0.16f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(equipmentCategoryEmoji(categoryName), fontSize = 22.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(displayTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (canManage) "${item.available}/${item.total} dostupno · ${item.location}" else "${if (item.available > 0) "Dostupno" else "Nije dostupno"} · ${item.location}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (item.variantCount > 1 || item.detailSummary.isNotBlank()) {
                    Text(
                        item.detailSummary.ifBlank { "Grupirano: ${item.variantCount} stavki" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipmentInventoryHeader(
    counted: Int,
    total: Int,
    shortage: Int,
    surplus: Int,
    pending: Int,
    message: String,
    onSyncPending: () -> Unit
) {
    val progress = if (total <= 0) 0f else counted.toFloat() / total.toFloat()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF66BB6A).copy(alpha = 0.22f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(52.dp).background(Color(0xFF66BB6A).copy(alpha = 0.14f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF66BB6A))
                }
                Column(Modifier.weight(1f)) {
                    Text("Inventura", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Potvrdi ako se slaže, +/- ako se ne slaže. Radi offline.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                if (pending > 0) {
                    OutlinedButton(onClick = onSyncPending, shape = RoundedCornerShape(16.dp)) { Text("Sync $pending") }
                }
            }
            Box(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(999.dp))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(10.dp).background(Color(0xFF66BB6A), RoundedCornerShape(999.dp)))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EquipmentStatCard("Prebrojano", "$counted / $total", Color(0xFF66BB6A), Modifier.weight(1f))
                EquipmentStatCard("Manjak", shortage.toString(), Color(0xFFEF5350), Modifier.weight(1f))
                EquipmentStatCard("Višak", surplus.toString(), Color(0xFFFFA726), Modifier.weight(1f))
            }
            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EquipmentInventoryFilters(
    categories: List<String>,
    selectedCategory: String,
    onCategory: (String) -> Unit,
    locations: List<String>,
    selectedLocation: String,
    onLocation: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EquipmentSectionTitle("Broji jednu policu/kategoriju", "Prvo izaberi lokaciju i kategoriju, pa fizički broji redom.")
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                EquipmentCategoryPill(label = category, selected = selectedCategory == category, onClick = { onCategory(category) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            locations.forEach { location ->
                EquipmentCategoryPill(label = location, selected = selectedLocation == location, onClick = { onLocation(location) })
            }
        }
    }
}

@Composable
private fun EquipmentInventoryCountRow(
    item: EquipmentMobileItem,
    counted: Int,
    done: Boolean,
    onConfirm: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onEdit: () -> Unit
) {
    val diff = counted - item.total
    val accent = when {
        !done -> MaterialTheme.colorScheme.outline
        diff == 0 -> Color(0xFF66BB6A)
        diff < 0 -> Color(0xFFEF5350)
        else -> Color(0xFFFFA726)
    }
    val bg = when {
        !done -> MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        diff == 0 -> Color(0xFF1B5E20).copy(alpha = 0.18f)
        diff < 0 -> Color(0xFFB71C1C).copy(alpha = 0.15f)
        else -> Color(0xFFFF8F00).copy(alpha = 0.14f)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(46.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text(equipmentCategoryEmoji(canonicalEquipmentCategoryName(item.category, item.subcategory, item.name)), fontSize = 22.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(item.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${item.code} · ${item.location}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                EquipmentSoftChip("treba ${item.total}", Color(0xFF7E57C2))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onConfirm, modifier = Modifier.height(52.dp).weight(1.1f), shape = RoundedCornerShape(18.dp)) {
                    Text("✓ Slaže se", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onMinus, modifier = Modifier.size(width = 48.dp, height = 52.dp), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(0.dp)) { Text("−", fontWeight = FontWeight.Bold) }
                Surface(onClick = onEdit, color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.28f)), modifier = Modifier.height(52.dp).width(70.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(counted.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge) }
                }
                OutlinedButton(onClick = onPlus, modifier = Modifier.size(width = 48.dp, height = 52.dp), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(0.dp)) { Text("+", fontWeight = FontWeight.Bold) }
            }
            if (done && diff != 0) {
                Text(if (diff < 0) "Manjak ${-diff}" else "Višak $diff", color = accent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun equipmentInventorySourceTable(appId: String): String = when {
    appId.startsWith("item:") -> "equipment_items"
    appId.startsWith("rope:") -> "equipment_ropes"
    appId.startsWith("piece:") -> "equipment_pieces"
    else -> "sov_equipment_app_catalog"
}

private fun equipmentInventorySourceId(appId: String): String = appId.substringAfter(":", appId)

private fun equipmentReturnLinesFor(request: EquipmentMobileRequest): List<EquipmentCloudRequestLine> =
    request.lines.ifEmpty {
        listOf(
            EquipmentCloudRequestLine(
                id = request.itemId,
                equipmentItemId = request.itemId,
                legacyId = request.itemCode,
                name = request.itemName,
                quantity = request.quantity.coerceAtLeast(1),
                unit = "kom",
                note = request.note
            )
        )
    }

@Composable
private fun EquipmentReturnLineRow(
    line: EquipmentCloudRequestLine,
    returned: Int,
    destination: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onDestination: (String) -> Unit
) {
    val borrowed = line.quantity.coerceAtLeast(1)
    val mismatch = returned != borrowed
    val accent = if (mismatch) Color(0xFFFFA726) else Color(0xFF26A69A)
    Surface(
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.26f))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(line.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Zaduženo: $borrowed ${line.unit.ifBlank { "kom" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = onMinus, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("−") }
                Text("$returned/$borrowed", fontWeight = FontWeight.ExtraBold, color = accent, modifier = Modifier.widthIn(min = 46.dp), textAlign = TextAlign.Center)
                OutlinedButton(onClick = onPlus, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("+") }
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Oružarstvo", "U jami", "Kod nekoga", "Rashod").forEach { dest ->
                    FilterChip(
                        selected = destination == dest,
                        onClick = { onDestination(dest) },
                        label = { Text(dest) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipmentRequestCard(
    request: EquipmentMobileRequest,
    canManage: Boolean,
    onIssue: (() -> Unit)? = null,
    onReturn: (() -> Unit)? = null
) {
    val statusColor = equipmentStatusColor(request.status)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.18f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(50.dp).background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = statusColor)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(request.itemName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (request.lines.size > 1) {
                        Text(request.lines.take(4).joinToString(" · ") { "${it.name} ×${it.quantity}" } + if (request.lines.size > 4) " · +${request.lines.size - 4}" else "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text("${request.id} · ${request.itemCode} · količina ${request.quantity}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    if (request.dateFrom.isNotBlank() || request.dateTo.isNotBlank()) {
                        Text("${request.dateFrom.ifBlank { "?" }} → ${request.dateTo.ifBlank { "?" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(request.note, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                EquipmentSoftChip(request.status, statusColor)
            }
            if (canManage) {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onIssue?.invoke() }, enabled = request.status == "Zatraženo", shape = RoundedCornerShape(16.dp)) { Text("Izdano") }
                    OutlinedButton(onClick = { onReturn?.invoke() }, enabled = request.status == "Izdano" || request.status == "Djelomično vraćeno", shape = RoundedCornerShape(16.dp)) { Text("Vraćeno") }
                }
            }
        }
    }
}

private fun equipmentStatusColor(status: String): Color = when (status) {
    "Dostupno", "Vraćeno" -> Color(0xFF26A69A)
    "Zatraženo" -> Color(0xFF42A5F5)
    "Izdano" -> Color(0xFFFFA726)
    "Djelomično vraćeno" -> Color(0xFFAB47BC)
    "Servis", "Otpisano", "Odbijeno", "Otkazano" -> Color(0xFFEF5350)
    else -> Color(0xFF8D6E63)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArchiveDrawingsReadOnlyScreen(
    onBack: () -> Unit
) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(ArchiveSupabaseRepository.loadCached(context)) }
    var loading by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("missing") }
    var selected by remember { mutableStateOf<ArchiveWorkItem?>(null) }
    var message by remember { mutableStateOf(snapshot?.message ?: "") }

    fun refresh() {
        loading = true
        scope.launch {
            val snap = withContext(Dispatchers.IO) { ArchiveSupabaseRepository.loadSnapshot(context) }
            snapshot = snap
            selected = selected?.let { old -> snap.items.firstOrNull { it.objectId == old.objectId } } ?: snap.items.firstOrNull()
            message = snap.message
            loading = false
        }
    }

    LaunchedEffect(Unit) { if (snapshot == null) refresh() }

    val items = snapshot?.items.orEmpty()
    val filtered = remember(items, query, filter) {
        val q = query.trim().lowercase(Locale.getDefault())
        items.filter { item ->
            val hay = listOf(item.objectName, item.plateNumber, item.objectType, item.nearestPlace).joinToString(" ").lowercase(Locale.getDefault())
            val qOk = q.isBlank() || hay.contains(q)
            val fOk = when (filter) {
                "missing_drawing" -> item.missingDrawing
                "missing_coordinates" -> item.missingCoordinates
                "missing_record" -> item.missingRecord
                "ready" -> !item.missingCoordinates && !item.missingDrawing && !item.missingRecord
                "missing" -> item.missingCoordinates || item.missingDrawing || item.missingRecord
                else -> true
            }
            qOk && fOk
        }
    }

    CaveScreenBackground {
        LazyColumn(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
                    Column(Modifier.weight(1f)) {
                        Text("Arhivar", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Objekti · nacrti · zapisnici · koordinate", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { refresh() }, enabled = !loading, shape = RoundedCornerShape(16.dp)) { Text(if (loading) "Sync..." else "Osvježi") }
                }
            }
            item {
                val stats = snapshot?.stats
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArchiveStatChip("Ukupno", stats?.total ?: items.size, Color(0xFF42A5F5))
                    ArchiveStatChip("Fali nacrt", stats?.missingDrawings ?: items.count { it.missingDrawing }, Color(0xFFFFA726))
                    ArchiveStatChip("Fale koord.", stats?.missingCoordinates ?: items.count { it.missingCoordinates }, Color(0xFFEF5350))
                    ArchiveStatChip("Fali zapisnik", stats?.missingRecords ?: items.count { it.missingRecord }, Color(0xFFAB47BC))
                    ArchiveStatChip("Spremno", stats?.ready ?: items.count { !it.missingCoordinates && !it.missingDrawing && !it.missingRecord }, Color(0xFF26A69A))
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Traži objekt, pločicu, mjesto") }
                        )
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("missing" to "Fali nešto", "missing_drawing" to "Fali nacrt", "missing_coordinates" to "Fale koord.", "missing_record" to "Fali zapisnik", "ready" to "Spremno", "all" to "Svi").forEach { (id, label) ->
                                FilterChip(selected = filter == id, onClick = { filter = id }, label = { Text(label) })
                            }
                        }
                        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (selected != null) {
                item {
                    ArchiveSelectedObjectCard(
                        item = selected!!,
                        onClose = { selected = null },
                        onSave = { hasCoord, hasDrawing, hasRecord, note ->
                            val current = selected ?: return@ArchiveSelectedObjectCard
                            loading = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { ArchiveSupabaseRepository.saveStatus(context, current, hasCoord, hasDrawing, hasRecord, "normal", note) }
                                }
                                message = result.getOrElse { it }.let { if (it is Throwable) "Greška: ${it.message.orEmpty().take(90)}" else it.toString() }
                                loading = false
                                refresh()
                            }
                        }
                    )
                }
            }
            item {
                Text("Worklist (${filtered.size})", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (filtered.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(24.dp)) {
                        Text("Nema objekata za ovaj filter ili cloud još nije sinkroniziran.", modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filtered.take(120), key = { it.objectId }) { item ->
                    ArchiveWorkItemRow(item = item, selected = selected?.objectId == item.objectId, onClick = { selected = item })
                }
            }
        }
    }
}

@Composable
private fun ArchiveStatChip(label: String, value: Int, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.25f))) {
        Column(Modifier.widthIn(min = 116.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value.toString(), color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ArchiveWorkItemRow(item: ArchiveWorkItem, selected: Boolean, onClick: () -> Unit) {
    val ready = !item.missingCoordinates && !item.missingDrawing && !item.missingRecord
    val tint = if (ready) Color(0xFF26A69A) else Color(0xFFFFA726)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = if (selected) 0.45f else 0.16f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(42.dp).background(tint.copy(alpha = 0.16f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CollectionsBookmark, contentDescription = null, tint = tint)
                }
                Column(Modifier.weight(1f)) {
                    Text(item.objectName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOf(item.objectType, item.plateNumber.takeIf { it.isNotBlank() }?.let { "pločica $it" }, item.nearestPlace).filterNotNull().filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(if (ready) "OK" else "FALI", color = tint, fontWeight = FontWeight.ExtraBold)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ArchiveMiniStatus("Koord", !item.missingCoordinates)
                ArchiveMiniStatus("Nacrt", !item.missingDrawing)
                ArchiveMiniStatus("Zapisnik", !item.missingRecord)
                if (item.drawingCount > 0) ArchiveCountStatus("Nacrti ${item.drawingCount}")
                if (item.reportCount > 0) ArchiveCountStatus("Zap. ${item.reportCount}")
            }
        }
    }
}

@Composable
private fun ArchiveMiniStatus(label: String, ok: Boolean) {
    val color = if (ok) Color(0xFF26A69A) else Color(0xFFEF5350)
    Surface(color = color.copy(alpha = 0.11f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.28f))) {
        Text((if (ok) "✓ " else "Fali ") + label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun ArchiveCountStatus(label: String) {
    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))) {
        Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveSelectedObjectCard(
    item: ArchiveWorkItem,
    onClose: () -> Unit,
    onSave: (Boolean, Boolean, Boolean, String) -> Unit
) {
    var hasCoord by remember(item.objectId) { mutableStateOf(!item.missingCoordinates) }
    var hasDrawing by remember(item.objectId) { mutableStateOf(!item.missingDrawing) }
    var hasRecord by remember(item.objectId) { mutableStateOf(!item.missingRecord) }
    var note by remember(item.objectId) { mutableStateOf(item.note) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(item.objectName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(listOf(item.objectType, item.plateNumber.takeIf { it.isNotBlank() }?.let { "pločica $it" }, item.nearestPlace).filterNotNull().filter { it.isNotBlank() }.joinToString(" · ").ifBlank { item.objectId }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Delete, contentDescription = "Zatvori", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text("Checklist za katastar", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = hasCoord, onClick = { hasCoord = !hasCoord }, label = { Text(if (hasCoord) "✓ Koordinate imamo" else "Fale koordinate") })
                FilterChip(selected = hasDrawing, onClick = { hasDrawing = !hasDrawing }, label = { Text(if (hasDrawing) "✓ Nacrt imamo" else "Fali nacrt") })
                FilterChip(selected = hasRecord, onClick = { hasRecord = !hasRecord }, label = { Text(if (hasRecord) "✓ Zapisnik imamo" else "Fali zapisnik") })
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("Napomena arhivara") })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(hasCoord, hasDrawing, hasRecord, note) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Text("Spremi status") }
                OutlinedButton(onClick = onClose, shape = RoundedCornerShape(16.dp)) { Text("Zatvori") }
            }
        }
    }
}

