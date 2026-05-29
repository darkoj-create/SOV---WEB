package com.darko.speleov1

import com.darko.speleov1.model.SourceFilter
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
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.Overlay
import android.location.Location
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

enum class AppTab { HOME, SEARCH, MAP, CLOUD, TOOLS, SETTINGS, CALCULATOR, SPELEO_RUNNER, OFFLINE, FIELD_PACKAGES }

enum class MapOrientationMode { NORTH_UP, HEADING_UP, STATIC }

data class TrackPoint(val point: GeoPoint, val altitudeM: Double?)

data class MarkedPoint(
    val id: String,
    val name: String,
    val type: String,
    val description: String,
    val lat: Double,
    val lon: Double,
    val htrsX: Double,
    val htrsY: Double,
    val visible: Boolean = true
)

data class SavedTrack(
    val id: String,
    val name: String,
    val description: String,
    val points: List<TrackPoint>,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val visible: Boolean = false
)

data class ImportedLayer(
    val id: String,
    val name: String,
    val type: String,
    val visible: Boolean = true,
    val points: List<MarkedPoint> = emptyList(),
    val tracks: List<SavedTrack> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
)

internal data class RecordCluster(
    val center: GeoPoint,
    val count: Int,
    val representative: SpeleoRecord
)

internal data class RecordRenderPlan(
    val singles: List<SpeleoRecord>,
    val clusters: List<RecordCluster>
)

private val SOV_GREEN_STATUSES = setOf("evidentirano", "evidentirano_editirati", "u_katastru", "u_katastru_editirati", "objekt_unesen", "ok")
private val SOV_RED_STATUSES = setOf(
    "za_provjeru",
    "za_unos",
    "spreman_za_unos",
    "nije_u_katastru",
    "za_unos_u_katastar",
    "spreman_za_unos_u_katastar",
    "izvan_hrvatske_baze",
    "za_provjeru_bih",
    "nije_u_katastru_bih",
    "krug_13",
    "krug_14"
)
private val SOV_GREY_STATUSES = setOf("nije_objekt", "objekt_druge_udruge", "neodredeno", "zatrpano")

internal fun normalizedRecordSource(record: SpeleoRecord): String =
    record.source?.trim()?.lowercase(Locale.ROOT).orEmpty()

internal fun recordHasSourceLabel(record: SpeleoRecord, label: String): Boolean {
    val normalizedLabel = label.trim().lowercase(Locale.ROOT)
    return record.source_labels.orEmpty().any { it.trim().lowercase(Locale.ROOT) == normalizedLabel }
}

internal fun recordIsSovSource(record: SpeleoRecord): Boolean {
    val source = normalizedRecordSource(record)
    return source == "sov" || source == "both" || recordHasSourceLabel(record, "sov")
}

internal fun recordIsKatastarSource(record: SpeleoRecord): Boolean {
    val source = normalizedRecordSource(record)
    return source == "katastar" || source == "both" || recordHasSourceLabel(record, "katastar")
}

internal fun recordIsMyBaseSource(record: SpeleoRecord): Boolean {
    val source = normalizedRecordSource(record)
    return source == "my_base" || source == "mybase" || recordHasSourceLabel(record, "my_base") || recordHasSourceLabel(record, "mybase")
}

internal fun recordIsKatastarOnly(record: SpeleoRecord): Boolean =
    recordIsKatastarSource(record) && !recordIsSovSource(record)

private fun normalizedRecordStatus(record: SpeleoRecord): String =
    record.classification.record_status?.trim()?.lowercase(Locale.ROOT).orEmpty()

private fun normalizedCadastreStatus(record: SpeleoRecord): String =
    record.cadastre.status?.trim()?.lowercase(Locale.ROOT).orEmpty()

internal fun recordIsSovNonObject(record: SpeleoRecord): Boolean {
    val status = normalizedRecordStatus(record)
    val cadastreStatus = normalizedCadastreStatus(record)
    return status in SOV_GREY_STATUSES || cadastreStatus in SOV_GREY_STATUSES
}

internal fun recordIsInCadastre(record: SpeleoRecord): Boolean {
    val status = normalizedRecordStatus(record)
    val cadastreStatus = normalizedCadastreStatus(record)
    return record.cadastre.in_cadastre == true || status in SOV_GREEN_STATUSES || cadastreStatus in SOV_GREEN_STATUSES
}

internal fun recordIsOutOfCadastre(record: SpeleoRecord): Boolean {
    val status = normalizedRecordStatus(record)
    val cadastreStatus = normalizedCadastreStatus(record)
    return record.cadastre.in_cadastre == false ||
        record.cadastre.not_in_cadastre_candidate == true ||
        status in SOV_RED_STATUSES ||
        cadastreStatus in SOV_RED_STATUSES
}

internal fun markerDrawableRes(record: SpeleoRecord, sourceFilter: SourceFilter): Int {
    return when {
        // User-created local KML database points stay dark blue.
        recordIsMyBaseSource(record) -> R.drawable.marker_mybase_dark_blue

        // SOV records keep their own operational meaning even when source filter is ALL.
        recordIsSovNonObject(record) -> R.drawable.marker_grey
        recordIsSovSource(record) && recordIsInCadastre(record) -> R.drawable.marker_green
        recordIsSovSource(record) && recordIsOutOfCadastre(record) -> R.drawable.marker_red

        // Safe status-based fallback for legacy/imported records.
        recordIsInCadastre(record) -> R.drawable.marker_green
        recordIsOutOfCadastre(record) -> R.drawable.marker_red
        else -> R.drawable.marker_grey
    }
}

internal fun recordStatusColor(record: SpeleoRecord, sourceFilter: SourceFilter): Int {
    return when {
        recordIsMyBaseSource(record) -> AndroidColor.rgb(11, 61, 145)
        recordIsSovNonObject(record) -> AndroidColor.rgb(158, 158, 158)
        recordIsSovSource(record) && recordIsInCadastre(record) -> AndroidColor.rgb(76, 175, 80)
        recordIsSovSource(record) && recordIsOutOfCadastre(record) -> AndroidColor.rgb(244, 67, 54)
        recordIsInCadastre(record) -> AndroidColor.rgb(76, 175, 80)
        recordIsOutOfCadastre(record) -> AndroidColor.rgb(244, 67, 54)
        else -> AndroidColor.rgb(158, 158, 158)
    }
}

private val simplePointDrawableStateCache = mutableMapOf<String, Drawable.ConstantState?>()
private val resourceDrawableStateCache = mutableMapOf<Int, Drawable.ConstantState?>()

internal fun cachedDrawable(context: Context, resId: Int): Drawable? {
    val cachedState = resourceDrawableStateCache[resId]
    if (cachedState != null) {
        return cachedState.newDrawable(context.resources).mutate()
    }
    val created = ContextCompat.getDrawable(context, resId) ?: return null
    resourceDrawableStateCache[resId] = created.constantState
    return created.constantState?.newDrawable(context.resources)?.mutate() ?: created
}

internal fun simplePointDrawable(
    color: Int,
    coreSizePx: Int = 28,
    haloSizePx: Int = 40,
    strokePx: Int = 3
): Drawable {
    val cacheKey = "$color|$coreSizePx|$haloSizePx|$strokePx"
    val cachedState = simplePointDrawableStateCache[cacheKey]
    if (cachedState != null) {
        return cachedState.newDrawable().mutate()
    }

    val halo = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(AndroidColor.argb(112, 255, 255, 255))
        setStroke(2, AndroidColor.argb(90, 0, 0, 0))
        setSize(haloSizePx, haloSizePx)
    }

    val core = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(strokePx, AndroidColor.argb(190, 0, 0, 0))
        setSize(coreSizePx, coreSizePx)
    }

    val created = LayerDrawable(arrayOf(halo, core)).apply {
        setLayerGravity(0, Gravity.CENTER)
        setLayerGravity(1, Gravity.CENTER)
    }
    simplePointDrawableStateCache[cacheKey] = created.constantState
    return created.constantState?.newDrawable()?.mutate() ?: created
}

internal fun fieldPointDrawable(color: Int): Drawable = simplePointDrawable(
    color = color,
    coreSizePx = 38,
    haloSizePx = 58,
    strokePx = 4
)

internal fun recordMarkerDrawable(
    context: Context,
    record: SpeleoRecord,
    sourceFilter: SourceFilter,
    fieldVisibilityEnabled: Boolean,
    simplePointViewEnabled: Boolean
): Drawable? {
    val color = recordStatusColor(record, sourceFilter)
    return when {
        fieldVisibilityEnabled -> fieldPointDrawable(color)
        simplePointViewEnabled -> simplePointDrawable(color)
        else -> cachedDrawable(context, markerDrawableRes(record, sourceFilter))
    }
}

internal val FIELD_TASK_FILTERS = listOf(
    "ponoviti_nacrt" to "Treba nacrt",
    "digitalizirati_nacrt" to "Treba digitalizirati nacrt",
    "srediti_nacrt" to "Treba srediti nacrt",
    "nastaviti_nacrt" to "Treba nastaviti nacrt",
    "koordinate" to "Treba koordinate",
    "fotka" to "Treba fotka",
    "plocica" to "Treba pločica",
    "zapisnik" to "Treba zapisnik",
    "provjeriti" to "Treba provjeriti"
)

internal val WMS_PRESETS = listOf(
    "TK25" to WmsConfig(MapLayerPrefs.DEFAULT_WMS_URL, MapLayerPrefs.DEFAULT_WMS_LAYERS),
    "DOF" to WmsConfig("https://geoportal.dgu.hr/services/geoportal/ows", "DOF"),
    "DOF LiDAR 2022/23" to WmsConfig(
        baseUrl = "https://geoportal.dgu.hr/services/inspire/orthophoto_lidar_2022_2023/ows",
        layers = "OI.OrthoimageCoverage",
        crs = "EPSG:3857",
        version = "1.3.0",
        styles = "",
        transparent = true
    ),
    "HOK" to WmsConfig(
        baseUrl = "https://geoportal.dgu.hr/wms",
        layers = "HOK",
        crs = "EPSG:3765",
        version = "1.3.0",
        styles = "raster",
        transparent = true
    ),
    "Geological Units" to WmsConfig(
        baseUrl = "https://transformiraj.nipp.hr/ows/services/org.2.abf7ddc6-7578-4070-a9db-c291a42e55c6_wms",
        layers = "GE.GeologicUnit.AgeOfRocks",
        crs = "EPSG:4326",
        version = "1.3.0",
        styles = "",
        transparent = true
    ),
    "Bioportal staništa 2016" to WmsConfig(
        baseUrl = "https://services.bioportal.hr/wms",
        layers = "dzzpnpis:kopnena_stanista_2016",
        crs = "EPSG:3857",
        version = "1.3.0",
        styles = "",
        transparent = true
    )
)

internal const val SPELEO_ZAPISNIK_URL = "https://docs.google.com/forms/d/e/1FAIpQLSen7p6l5t5F4uQiDpFQ7uzjnRdbJyKWwpZnnwGoKah5fI_ZpA/viewform"
internal const val CALENDAR_URL = "https://docs.google.com/spreadsheets/d/1aVdMHwYwll-KD0jGbz-fMEG5y0hq_v98/edit?gid=613670311#gid=613670311"
internal const val MEMBERSHIP_URL = "https://www.pdsvelebit.hr/wp-content/uploads/2025/12/PDSVelebit_Upute_uclanjenje_v2026_Web.pdf"

data class NavigationTarget(
    val id: String,
    val name: String,
    val point: GeoPoint,
    val targetAltitudeM: Double? = null
)
