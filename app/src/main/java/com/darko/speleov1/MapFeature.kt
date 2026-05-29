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
import android.view.MotionEvent
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darko.speleov1.model.BoundingBoxFilter
import com.darko.speleov1.model.CadastreFilter
import com.darko.speleov1.model.CaveTypeFilter
import com.darko.speleov1.model.FilterState
import com.darko.speleov1.model.SpeleoRecord
import com.darko.speleov1.model.SourceFilter
import com.darko.speleov1.ui.theme.SpeleoTheme
import com.darko.speleov1.util.CoordinateConverter
import com.darko.speleov1.util.KmlExporter
import com.darko.speleov1.util.LocationHelper
import com.darko.speleov1.util.LocalTileOverlay
import com.darko.speleov1.util.OfflineBoundsOverlay
import com.darko.speleov1.util.MapLayerMode
import com.darko.speleov1.util.MapLayerPrefs
import com.darko.speleov1.util.HillshadePrefs
import com.darko.speleov1.util.FieldVisibilityPrefs
import com.darko.speleov1.util.HillshadeTilesOverlay
import com.darko.speleov1.util.WmsBaseTilesOverlay
import com.darko.speleov1.util.WmsConfig
import com.darko.speleov1.util.WmsPerformanceCache
import com.darko.speleov1.util.WmsTileSource
import com.darko.speleov1.util.HGSSTileSource
import com.darko.speleov1.util.WmsTilesOverlay
import com.darko.speleov1.util.WmsCapabilitiesClient
import com.darko.speleov1.util.WmsLayerOption
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
import java.util.LinkedHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualCoordDialog(
    onDismiss: () -> Unit,
    onConfirm: (GeoPoint) -> Unit
) {
    var inputMode by remember { mutableStateOf("wgs") }
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    var htrsXText by remember { mutableStateOf("") }
    var htrsYText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unesi koordinate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = inputMode == "wgs",
                        onClick = { inputMode = "wgs"; error = null },
                        label = { Text("WGS84") }
                    )
                    FilterChip(
                        selected = inputMode == "htrs",
                        onClick = { inputMode = "htrs"; error = null },
                        label = { Text("HTRS96/TM") }
                    )
                }

                if (inputMode == "wgs") {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it; error = null },
                        label = { Text("Širina (Lat)") },
                        placeholder = { Text("npr. 45.123456") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it; error = null },
                        label = { Text("Dužina (Lon)") },
                        placeholder = { Text("npr. 15.987654") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = htrsXText,
                        onValueChange = { htrsXText = it; error = null },
                        label = { Text("X (Easting)") },
                        placeholder = { Text("npr. 546123.45") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = htrsYText,
                        onValueChange = { htrsYText = it; error = null },
                        label = { Text("Y (Northing)") },
                        placeholder = { Text("npr. 5012345.67") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    val point = if (inputMode == "wgs") {
                        val lat = latText.trim().replace(",", ".").toDouble()
                        val lon = lonText.trim().replace(",", ".").toDouble()
                        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                            throw IllegalArgumentException("Koordinate izvan raspona")
                        }
                        GeoPoint(lat, lon)
                    } else {
                        val x = htrsXText.trim().replace(",", ".").toDouble()
                        val y = htrsYText.trim().replace(",", ".").toDouble()
                        val wgs = CoordinateConverter.htrs96TmToWgs84(x, y)
                        GeoPoint(wgs.lat, wgs.lon)
                    }
                    onConfirm(point)
                } catch (e: NumberFormatException) {
                    error = "Nevažeći format broja — koristi točku kao decimalni separator"
                } catch (e: IllegalArgumentException) {
                    error = e.message ?: "Nevažeće koordinate"
                } catch (e: Exception) {
                    error = "Greška pri konverziji koordinata"
                }
            }) { Text("Potvrdi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Odustani") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapTabScreen(
    records: List<SpeleoRecord>,
    nearRecords: List<SpeleoRecord> = records,
    fieldPackageModeName: String? = null,
    activeFieldPackageBounds: BoundingBoxFilter? = null,
    filters: FilterState,
    locationOptions: List<SearchLocationOption>,
    hasCurrentUserLocation: Boolean,
    isFiltering: Boolean,
    searchReady: Boolean,
    focusRecord: SpeleoRecord?,
    focusedSavedTrack: SavedTrack?,
    focusedSavedTrackNonce: Int,
    onSelect: (SpeleoRecord) -> Unit,
    onFiltersChanged: (FilterState) -> Unit,
    onClearFieldPackageBounds: () -> Unit = {},
    onViewOnMap: (SpeleoRecord) -> Unit,
    onRequestGpsLocation: () -> Unit,
    offlineStateVersion: Int,
    mapLayerStateVersion: Int,
    currentUserLocation: GeoPoint?,
    currentLocationAccuracyM: Double?,
    currentLocationAltitudeM: Double?,
    currentLocationProvider: String?,
    currentLocationSpeedMps: Double?,
    currentGpsBearingDeg: Float?,
    currentGpsBearingAccuracyDeg: Float?,
    waitingForGpsFix: Boolean,
    mapOrientationMode: MapOrientationMode,
    centerOnUserNonce: Int,
    autoCenterOnUserEnabled: Boolean,
    positionEnabled: Boolean,
    liveTrackingEnabled: Boolean,
    trackingSessionNonce: Int,
    currentTrackStartedAtMillis: Long?,
    trackPoints: List<TrackPoint>,
    markedPoints: List<MarkedPoint>,
    savedTracks: List<SavedTrack>,
    importedLayers: List<ImportedLayer>,
    simplePointViewEnabled: Boolean,
    offlineFocusPoint: GeoPoint?,
    offlineFocusZoom: Double,
    offlineFocusNonce: Int,
    importedFocusPoint: GeoPoint?,
    importedFocusZoom: Double,
    importedFocusNonce: Int,
    searchFocusPoints: List<GeoPoint>,
    searchFocusRecordIds: Set<String> = emptySet(),
    searchFocusNonce: Int,
    persistedMapCenter: GeoPoint?,
    persistedMapZoom: Double,
    onMapCameraChanged: (GeoPoint, Double) -> Unit,
    onCenterOnUser: () -> Unit,
    onToggleGps: () -> Unit,
    onToggleAutoCenterOnUser: () -> Unit,
    onToggleLiveTracking: (Boolean) -> Unit,
    onSaveStoppedTrack: () -> Unit,
    onToggleSimplePointView: () -> Unit,
    onMarkPosition: () -> Unit,
    onMarkPositionAtPoint: (GeoPoint) -> Unit,
    onFocusPoint: (GeoPoint) -> Unit,
    onToggleOrientationMode: () -> Unit,
    onEditMarkedPoint: (MarkedPoint) -> Unit,
    onShowMarkedPointActions: (MarkedPoint) -> Unit,
    onShowImportedPointActions: (MarkedPoint) -> Unit,
    navigationTarget: NavigationTarget?,
    onClearNavigationTarget: () -> Unit,
    rulerStartPoint: GeoPoint?,
    rulerEndPoint: GeoPoint?,
    rulerModeEnabled: Boolean,
    onToggleRulerMode: () -> Unit,
    onSetRulerPoints: (GeoPoint?, GeoPoint?) -> Unit,
    onNavigate: (AppTab) -> Unit,
    onShowExport: () -> Unit,
    onOfflineDownloaded: (GeoPoint, Double) -> Unit,
    onImport: () -> Unit,
    onOpenOfflineMenu: () -> Unit,
    drawingModeEnabled: Boolean,
    drawingPoints: List<GeoPoint>,
    drawingStrokeWidthDp: Int,
    drawingSmoothEnabled: Boolean,
    onToggleDrawingMode: () -> Unit,
    onFinishDrawingMode: () -> Unit,
    onUndoDrawingPoint: () -> Unit,
    onClearDrawing: () -> Unit,
    onSaveDrawing: () -> Unit,
    onShareDrawing: () -> Unit,
    onToggleDrawingSmoothing: () -> Unit,
    onSetDrawingStrokeWidthDp: (Int) -> Unit,
    onMapTapForDrawing: (GeoPoint) -> Unit,
    startAreaSelectionNonce: Int = 0,
    cancelAreaSelectionNonce: Int = 0,
    onClearAllUserContent: () -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    var showLayerDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomWmsDialog by rememberSaveable { mutableStateOf(false) }
    var customWmsName by rememberSaveable { mutableStateOf("") }
    var customWmsUrl by rememberSaveable { mutableStateOf("") }
    var customWmsLayers by rememberSaveable { mutableStateOf("") }
    var customWmsCrs by rememberSaveable { mutableStateOf("EPSG:3857") }
    var customWmsVersion by rememberSaveable { mutableStateOf("1.1.1") }
    var customWmsStyles by rememberSaveable { mutableStateOf("") }
    var customWmsTransparent by rememberSaveable { mutableStateOf(false) }
    var customWmsMode by rememberSaveable { mutableStateOf("overlay") }
    var capabilitiesBusy by rememberSaveable { mutableStateOf(false) }
    var capabilitiesMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var capabilitiesVersion by rememberSaveable { mutableStateOf("") }
    var capabilitiesLayers by remember { mutableStateOf<List<WmsLayerOption>>(emptyList()) }
    var selectedCapabilitiesLayer by remember { mutableStateOf<WmsLayerOption?>(null) }
    var localMapLayerVersion by rememberSaveable { mutableIntStateOf(0) }
    var selectingOfflineArea by rememberSaveable { mutableStateOf(false) }
    // Captures the map source visible when area selection starts.
    // Prevents HGSS offline download from falling back to default WMS/TK25.
    var offlineDownloadSourceMode by rememberSaveable { mutableStateOf<MapLayerMode?>(null) }
    var selectedAreaPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var downloadBusy by rememberSaveable { mutableStateOf(false) }
    var downloadMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var downloadProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var downloadProgressLabel by rememberSaveable { mutableStateOf("Priprema downloada…") }
    var offlineMapName by rememberSaveable { mutableStateOf("Offline ${System.currentTimeMillis()}") }
    var showToolsSheet by rememberSaveable { mutableStateOf(false) }
    var showSearchSheet by rememberSaveable { mutableStateOf(false) }
    var localSearchFocusPoints by remember { mutableStateOf(listOf<GeoPoint>()) }
    var localSearchFocusRecordIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var localSearchFocusNonce by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(searchFocusNonce) {
        if (searchFocusNonce > 0) {
            localSearchFocusPoints = emptyList()
            localSearchFocusRecordIds = emptySet()
            localSearchFocusNonce = 0
        }
    }
    val effectiveSearchFocusPoints = if (localSearchFocusNonce > 0) localSearchFocusPoints else searchFocusPoints
    val effectiveSearchFocusRecordIds = if (localSearchFocusNonce > 0) localSearchFocusRecordIds else searchFocusRecordIds
    val effectiveSearchFocusNonce = if (localSearchFocusNonce > 0) searchFocusNonce + localSearchFocusNonce + 100000 else searchFocusNonce
    var gpsPanelExpanded by rememberSaveable { mutableStateOf(false) }
    var showNearMePanel by rememberSaveable { mutableStateOf(false) }
    var nearMeFilter by rememberSaveable { mutableStateOf("all") }
    var fieldVisibilityStateVersion by rememberSaveable { mutableIntStateOf(0) }
    var compassVisible by rememberSaveable { mutableStateOf(true) }
    var uiHideLevel by rememberSaveable { mutableIntStateOf(0) }
    var leftToolbarOpen by rememberSaveable { mutableStateOf(false) }
    var pendingLongPressPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showManualCoordDialog by remember { mutableStateOf(false) }
    var showOrientationDialog by rememberSaveable { mutableStateOf(false) }
    var pendingMapToolAction by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val deviceHeadingDeg = rememberDeviceHeadingDegrees(
        gpsBearingDeg = currentGpsBearingDeg,
        gpsSpeedMps = currentLocationSpeedMps?.toFloat(),
        gpsBearingAccuracyDeg = currentGpsBearingAccuracyDeg,
    )
    val hasActiveSearch = remember(filters) { filters.hasAnyActiveCriteria() }
    val boundedRecords = remember(records, activeFieldPackageBounds) { records.filterByBoundingBox(activeFieldPackageBounds) }
    val recordsToShow = when {
        focusRecord != null -> listOf(focusRecord)
        // Opening results from Search can also mean "Prikaži sve" with no active
        // filters. In that case hasActiveSearch is false, but searchFocusPoints
        // are populated and the user explicitly asked to see database points on
        // the map. When IDs are available, keep only the exact Search result set
        // so trip-create/area-selection mode cannot flood the map with the whole
        // SOV database.
        effectiveSearchFocusPoints.isNotEmpty() && effectiveSearchFocusRecordIds.isNotEmpty() -> boundedRecords.filter { it.id in effectiveSearchFocusRecordIds }
        effectiveSearchFocusPoints.isNotEmpty() && !selectingOfflineArea -> boundedRecords
        hasActiveSearch -> boundedRecords
        // Trip creation / offline-area selection starts as a clean area picker.
        // Search still works normally, but database markers appear only after a
        // search/filter/result focus, not immediately after opening the map.
        selectingOfflineArea -> emptyList()
        fieldPackageModeName != null && activeFieldPackageBounds != null -> boundedRecords
        fieldPackageModeName != null -> emptyList()
        else -> emptyList()
    }
    val offlineAvailable = remember(offlineStateVersion, localMapLayerVersion) { OfflineTileManager.hasOfflineTiles(context) }
    val selectedMode = remember(mapLayerStateVersion, localMapLayerVersion) { MapLayerPrefs.getMode(context) }
    val wmsConfig = remember(mapLayerStateVersion, localMapLayerVersion) { MapLayerPrefs.getWmsConfig(context) }
    val hillshadeEnabled = remember(mapLayerStateVersion, localMapLayerVersion) { HillshadePrefs.isEnabled(context) }
    val hillshadeOpacityPercent = remember(mapLayerStateVersion, localMapLayerVersion) { HillshadePrefs.getOpacityPercent(context) }
    val geologicalOverlayEnabled = remember(mapLayerStateVersion, localMapLayerVersion) { MapLayerPrefs.isGeologicalOverlayEnabled(context) }
    val geologicalOverlayOpacityPercent = remember(mapLayerStateVersion, localMapLayerVersion) { MapLayerPrefs.getGeologicalOverlayOpacityPercent(context) }
    val fieldVisibilityEnabled = remember(fieldVisibilityStateVersion) { FieldVisibilityPrefs.isEnabled(context) }
    val savedCustomWmsConfigs = remember(localMapLayerVersion) { MapLayerPrefs.getCustomWmsConfigs(context) }
    val effectiveMode = when (selectedMode) {
        MapLayerMode.AUTO -> if (offlineAvailable) MapLayerMode.OFFLINE else MapLayerMode.WMS
        MapLayerMode.OFFLINE -> if (offlineAvailable) MapLayerMode.OFFLINE else MapLayerMode.WMS
        else -> selectedMode
    }
    val layerLabel = when (effectiveMode) {
        MapLayerMode.OFFLINE -> "Offline karta"
        MapLayerMode.WMS -> "WMS karta"
        MapLayerMode.OPENTOPO -> "OpenTopo"
        MapLayerMode.HGSS_SIGURNE_STAZE -> "HGSS SigurneStaze"
        MapLayerMode.HGSS_OSM_TEST -> "HGSS SigurneStaze"
        else -> "Auto"
    }
    val hasAnyUserContent = markedPoints.isNotEmpty() || savedTracks.isNotEmpty() || importedLayers.isNotEmpty() || (!liveTrackingEnabled && trackPoints.isNotEmpty())
    val hasClearableState = hasAnyUserContent || navigationTarget != null || rulerStartPoint != null || rulerEndPoint != null || drawingModeEnabled || drawingPoints.isNotEmpty()

    fun beginAreaSelectionMode() {
        selectingOfflineArea = true
        offlineDownloadSourceMode = when (selectedMode) {
            MapLayerMode.HGSS_SIGURNE_STAZE, MapLayerMode.HGSS_OSM_TEST -> MapLayerMode.HGSS_OSM_TEST
            else -> effectiveMode
        }
        selectedAreaPoints = emptyList()
        downloadMessage = "Tapni 1. kut područja na karti."
        showToolsSheet = false
        showSearchSheet = false
        leftToolbarOpen = false
    }

    fun stopAreaSelectionMode() {
        selectingOfflineArea = false
        offlineDownloadSourceMode = null
        selectedAreaPoints = emptyList()
        downloadMessage = null
    }

    val trackStats = remember(trackPoints) { computeTrackStats(trackPoints) }
    val trackPointsDropped by TrackingRuntime.state.collectAsStateWithLifecycle()
    val pointsDropped = trackPointsDropped.pointsDropped
    val hasPausedTrack = !liveTrackingEnabled && trackPoints.size >= 2
    val nearMeSourceRecords = remember(nearRecords, records) {
        if (nearRecords.size >= records.size) nearRecords else records
    }
    val nearMeItems = remember(nearMeSourceRecords, currentUserLocation, nearMeFilter) {
        buildNearMeItems(nearMeSourceRecords, currentUserLocation, nearMeFilter, maxItems = 20)
    }
    val showQuickCenterButton = false // v1.7: removed redundant Center GPS button; use GPS/Follow instead
    val showTrackingFollowButton = currentUserLocation != null || positionEnabled || waitingForGpsFix
    var trackingClockMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var restoredPersistedCamera by remember { mutableStateOf(false) }

    LaunchedEffect(startAreaSelectionNonce) {
        if (startAreaSelectionNonce > 0) {
            beginAreaSelectionMode()
            downloadMessage = "Tapni prvi kut područja na karti."
        }
    }

    LaunchedEffect(cancelAreaSelectionNonce) {
        if (cancelAreaSelectionNonce > 0) {
            stopAreaSelectionMode()
            downloadProgress = 0f
            downloadProgressLabel = "Priprema downloada…"
        }
    }

    LaunchedEffect(downloadBusy, downloadMessage) {
        val currentMessage = downloadMessage
        if (!downloadBusy && currentMessage != null) {
            delay(if (currentMessage.startsWith("Download nije uspio")) 6500L else 3800L)
            if (!downloadBusy && downloadMessage == currentMessage) {
                downloadMessage = null
            }
        }
    }

    LaunchedEffect(liveTrackingEnabled, currentTrackStartedAtMillis, trackPoints.size) {
        if (liveTrackingEnabled && currentTrackStartedAtMillis != null) {
            while (true) {
                trackingClockMillis = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    LaunchedEffect(persistedMapCenter?.latitude, persistedMapCenter?.longitude, persistedMapZoom) {
        if (persistedMapCenter != null) restoredPersistedCamera = false
    }

    val mapHeaderFacts = remember(layerLabel, hasActiveSearch, records.size, liveTrackingEnabled, rulerModeEnabled, mapOrientationMode, offlineAvailable, hillshadeEnabled, geologicalOverlayEnabled, geologicalOverlayOpacityPercent) {
        buildList {
            add(layerLabel)
            if (offlineAvailable && effectiveMode == MapLayerMode.OFFLINE) add("offline")
            if (fieldPackageModeName != null) add("izlet")
            if (hasActiveSearch) add("${records.size} rezultata")
            if (liveTrackingEnabled) add("tracking")
            if (drawingModeEnabled) add("draw")
            if (hillshadeEnabled) add("reljef")
            if (geologicalOverlayEnabled) add("geo ${geologicalOverlayOpacityPercent}%")
            if (fieldVisibilityEnabled) add("field visibility")
            if (rulerModeEnabled) add("ruler")
            if (mapOrientationMode == MapOrientationMode.HEADING_UP) add("compass")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapScreen(
            modifier = Modifier.fillMaxSize(),
            records = recordsToShow,
            activeFieldPackageBounds = activeFieldPackageBounds,
            onClearFieldPackageBounds = onClearFieldPackageBounds,
            focusRecord = focusRecord,
            focusedSavedTrack = focusedSavedTrack,
            focusedSavedTrackNonce = focusedSavedTrackNonce,
            onSelect = onSelect,
            offlineStateVersion = offlineStateVersion,
            mapLayerMode = effectiveMode,
            wmsConfig = wmsConfig,
            hillshadeEnabled = hillshadeEnabled,
            hillshadeOpacityPercent = hillshadeOpacityPercent,
            geologicalOverlayEnabled = geologicalOverlayEnabled,
            geologicalOverlayOpacityPercent = geologicalOverlayOpacityPercent,
            fieldVisibilityEnabled = fieldVisibilityEnabled,
            currentUserLocation = currentUserLocation,
            currentLocationAccuracyM = currentLocationAccuracyM,
            currentLocationAltitudeM = currentLocationAltitudeM,
            currentLocationProvider = currentLocationProvider,
            waitingForGpsFix = waitingForGpsFix,
            deviceHeadingDeg = deviceHeadingDeg,
            mapOrientationMode = mapOrientationMode,
            centerOnUserNonce = centerOnUserNonce,
            autoCenterOnUserEnabled = autoCenterOnUserEnabled,
            positionEnabled = positionEnabled,
            liveTrackingEnabled = liveTrackingEnabled,
            trackingSessionNonce = trackingSessionNonce,
            trackPoints = trackPoints,
            markedPoints = markedPoints,
            savedTracks = savedTracks,
            importedLayers = importedLayers,
            drawingPoints = drawingPoints,
            drawingModeEnabled = drawingModeEnabled,
            drawingStrokeWidthDp = drawingStrokeWidthDp,
            drawingSmoothEnabled = drawingSmoothEnabled,
            simplePointViewEnabled = simplePointViewEnabled,
            offlineFocusPoint = offlineFocusPoint,
            offlineFocusZoom = offlineFocusZoom,
            offlineFocusNonce = offlineFocusNonce,
            importedFocusPoint = importedFocusPoint,
            importedFocusZoom = importedFocusZoom,
            importedFocusNonce = importedFocusNonce,
            searchFocusPoints = effectiveSearchFocusPoints,
            searchFocusRecordIds = effectiveSearchFocusRecordIds,
            searchFocusNonce = effectiveSearchFocusNonce,
            persistedMapCenter = persistedMapCenter,
            persistedMapZoom = persistedMapZoom,
            onMapCameraChanged = onMapCameraChanged,
            areaSelectionMode = selectingOfflineArea,
            selectedAreaPoints = selectedAreaPoints,
            onEditMarkedPoint = onEditMarkedPoint,
            onShowMarkedPointActions = onShowMarkedPointActions,
            onShowImportedPointActions = onShowImportedPointActions,
            navigationTarget = navigationTarget,
            rulerStartPoint = rulerStartPoint,
            rulerEndPoint = rulerEndPoint,
            rulerModeEnabled = rulerModeEnabled,
            onMapTapForRuler = { point ->
                when {
                    point == null -> onSetRulerPoints(null, null)
                    rulerStartPoint == null || (rulerStartPoint != null && rulerEndPoint != null) -> onSetRulerPoints(point, null)
                    else -> onSetRulerPoints(rulerStartPoint, point)
                }
            },
            onMapTapForDrawing = onMapTapForDrawing,
            onAreaCornerSelected = { point ->
                val next = (selectedAreaPoints + point).take(2)
                selectedAreaPoints = next
                downloadMessage = when (next.size) {
                    1 -> "Prvi kut spremljen. Tapni drugi kut područja."
                    2 -> "Područje odabrano. Odaberi detaljnost downloada."
                    else -> null
                }
                if (next.size >= 2) selectingOfflineArea = false
            },
            onLongPressPoint = { point ->
                pendingLongPressPoint = point
            }
        )

        if (uiHideLevel < 2 && (selectingOfflineArea || drawingModeEnabled || rulerModeEnabled)) {
            ActiveMapModeBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(15f)
                    .padding(top = 14.dp, start = 16.dp, end = 16.dp),
                title = when {
                    selectingOfflineArea -> "Map download mode"
                    drawingModeEnabled -> "Drawing pen mode"
                    else -> "Ruler mode"
                },
                body = when {
                    selectingOfflineArea -> if (selectedAreaPoints.isEmpty()) "Tapni prvi kut područja. Alati su zaključani dok ne završiš ili odustaneš." else "Tapni drugi kut područja."
                    drawingModeEnabled -> "Crtanje je aktivno. Karta se neće slučajno pomicati kao običan pan."
                    rulerStartPoint == null -> "Tapni početnu točku mjerenja."
                    rulerEndPoint == null -> "Tapni završnu točku mjerenja."
                    else -> "Mjerenje je spremno."
                },
                actionLabel = when {
                    drawingModeEnabled -> "Gotovo"
                    rulerModeEnabled -> "Očisti"
                    else -> "Odustani"
                },
                onAction = {
                    when {
                        drawingModeEnabled -> onFinishDrawingMode()
                        rulerModeEnabled -> { onSetRulerPoints(null, null); if (rulerModeEnabled) onToggleRulerMode() }
                        selectingOfflineArea -> stopAreaSelectionMode()
                    }
                }
            )
        }


        if (uiHideLevel < 1 && activeFieldPackageBounds != null) {
            FieldPackageBoundsBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(16f)
                    .padding(top = 14.dp, start = 16.dp, end = 16.dp),
                label = activeFieldPackageBounds.label.ifBlank { "područje izleta" },
                onClear = onClearFieldPackageBounds
            )
        }


        if (uiHideLevel < 1 && drawingModeEnabled) {
            DrawingToolbarOverlay(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(14f)
                    .padding(start = if (leftToolbarOpen) 92.dp else 16.dp),
                pointCount = drawingPoints.size,
                drawingStrokeWidthDp = drawingStrokeWidthDp,
                drawingSmoothEnabled = drawingSmoothEnabled,
                canSave = drawingPoints.size >= 2,
                onFinish = onFinishDrawingMode,
                onUndo = onUndoDrawingPoint,
                onClear = onClearDrawing,
                onSave = onSaveDrawing,
                onShare = onShareDrawing,
                onToggleSmooth = onToggleDrawingSmoothing,
                onSetStrokeWidth = onSetDrawingStrokeWidthDp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(11f)
                .padding(top = 4.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MiniMapIconButton(
                onClick = { uiHideLevel = (uiHideLevel + 1) % 3 },
                icon = when (uiHideLevel) {
                    0 -> Icons.Default.VisibilityOff
                    1 -> Icons.Default.Visibility
                    else -> Icons.Default.Visibility
                },
                label = "UI",
                iconTint = when (uiHideLevel) {
                    0 -> Color.White
                    1 -> Color(0xFFFFCA28)
                    else -> Color(0xFF81C784)
                },
                active = uiHideLevel > 0
            )

            if (uiHideLevel < 1) {
                Column(
                    modifier = Modifier
                        .background(Color(0x94121417), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MiniMapIconButton(onClick = { showSearchSheet = true }, icon = Icons.Default.Search, label = "Search", active = showSearchSheet)
                    MiniMapIconButton(onClick = { showLayerDialog = true }, icon = Icons.Default.Map, label = "Maps", active = showLayerDialog)
                    MiniMapIconButton(
                        onClick = onToggleGps,
                        icon = Icons.Default.MyLocation,
                        label = "GPS",
                        iconTint = if (positionEnabled || liveTrackingEnabled) Color(0xFF6EE7A2) else Color.White,
                        active = positionEnabled || liveTrackingEnabled
                    )
                    MiniMapIconButton(
                        onClick = onMarkPosition,
                        icon = Icons.Default.LocationOn,
                        label = "Point"
                    )
                    MiniMapIconButton(
                        onClick = { onToggleLiveTracking(!liveTrackingEnabled) },
                        icon = if (liveTrackingEnabled) Icons.Default.Stop else Icons.Default.PlayArrow,
                        label = if (liveTrackingEnabled) "Stop" else "Track",
                        iconTint = if (liveTrackingEnabled) Color(0xFFFF7A7A) else Color.White,
                        active = liveTrackingEnabled || hasPausedTrack
                    )
                    MiniMapIconButton(
                        onClick = { showToolsSheet = true },
                        icon = Icons.Default.Settings,
                        label = "Tools",
                        active = showToolsSheet
                    )
                }
            }
        }

        if (uiHideLevel < 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = (-88).dp)
                    .zIndex(13f)
                    .width(22.dp)
                    .height(152.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(Color(0x88242A32), Color(0x32121417))),
                        shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
                    )
                    .pointerInput(leftToolbarOpen) {
                        var dragTotal = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragTotal > 36f) leftToolbarOpen = true
                                if (dragTotal < -36f) leftToolbarOpen = false
                                dragTotal = 0f
                            },
                            onDragCancel = { dragTotal = 0f }
                        ) { _, dragAmount ->
                            dragTotal += dragAmount
                            if (dragTotal > 54f) leftToolbarOpen = true
                            if (dragTotal < -54f) leftToolbarOpen = false
                        }
                    }
                    .clickable { leftToolbarOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (leftToolbarOpen) "‹" else "›",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        if (uiHideLevel < 1 && leftToolbarOpen) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = (-88).dp)
                    .zIndex(13f)
                    .padding(start = 12.dp)
                    .pointerInput(Unit) {
                        var dragTotal = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragTotal < -32f) leftToolbarOpen = false
                                dragTotal = 0f
                            },
                            onDragCancel = { dragTotal = 0f }
                        ) { _, dragAmount ->
                            dragTotal += dragAmount
                            if (dragTotal < -48f) leftToolbarOpen = false
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showQuickCenterButton) {
                    MiniMapIconButton(
                        onClick = onCenterOnUser,
                        icon = Icons.Default.MyLocation,
                        label = "Center",
                        iconTint = Color(0xFF6EE7A2),
                        active = true,
                        emphasized = true,
                        strongBackground = true
                    )
                }
                if (showTrackingFollowButton) {
                    MiniMapIconButton(
                        onClick = onToggleAutoCenterOnUser,
                        icon = Icons.Default.Navigation,
                        label = "Follow",
                        iconTint = if (autoCenterOnUserEnabled) Color(0xFFFF7A7A) else Color(0xFFFFB4B4),
                        active = autoCenterOnUserEnabled,
                        emphasized = true,
                        strongBackground = true
                    )
                }

                Column(
                    modifier = Modifier
                        .background(Color(0x94121417), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MiniMapIconButton(
                        onClick = { showNearMePanel = !showNearMePanel },
                        icon = Icons.Default.TravelExplore,
                        label = "Near",
                        iconTint = if (showNearMePanel) Color(0xFF6EE7A2) else Color.White,
                        active = showNearMePanel
                    )
                    MiniMapIconButton(
                        onClick = {
                            if (selectingOfflineArea) stopAreaSelectionMode() else pendingMapToolAction = "AREA"
                        },
                        icon = Icons.Default.Download,
                        label = "Map DL",
                        iconTint = if (selectingOfflineArea) Color(0xFF9CDCFE) else Color.White,
                        active = selectingOfflineArea
                    )
                    MiniMapIconButton(
                        onClick = {
                            if (drawingModeEnabled) onFinishDrawingMode() else pendingMapToolAction = "DRAW"
                        },
                        icon = Icons.Default.Edit,
                        label = "Pen",
                        iconTint = if (drawingModeEnabled) Color(0xFFFFB74D) else Color.White,
                        active = drawingModeEnabled || drawingPoints.isNotEmpty()
                    )
                    MiniMapIconButton(
                        onClick = {
                            if (rulerModeEnabled) onToggleRulerMode() else pendingMapToolAction = "RULER"
                        },
                        icon = Icons.Default.Straighten,
                        label = "Ruler",
                        iconTint = if (rulerModeEnabled || rulerStartPoint != null || rulerEndPoint != null) Color(0xFF9CDCFE) else Color.White,
                        active = rulerModeEnabled || rulerStartPoint != null || rulerEndPoint != null
                    )
                    MiniMapIconButton(
                        onClick = onToggleSimplePointView,
                        icon = Icons.Default.Lens,
                        label = "Simple",
                        iconTint = if (simplePointViewEnabled) Color(0xFFFFCA28) else Color.White,
                        active = simplePointViewEnabled
                    )
                    MiniMapIconButton(
                        onClick = { showManualCoordDialog = true },
                        icon = Icons.Default.LocationOn,
                        label = "Coord",
                        iconTint = Color.White
                    )
                    MiniMapIconButton(
                        onClick = {
                            val enableHillshade = !hillshadeEnabled
                            HillshadePrefs.setEnabled(context, enableHillshade)
                            if (enableHillshade) HillshadePrefs.setOpacityPercent(context, 40)
                            localMapLayerVersion++
                        },
                        icon = Icons.Default.Layers,
                        label = "Shade",
                        iconTint = if (hillshadeEnabled) Color(0xFFFFD166) else Color.White,
                        active = hillshadeEnabled
                    )
                }

            }
        }

        val navInfo = navigationTarget?.let { target ->
            remember(currentUserLocation, currentLocationAltitudeM, deviceHeadingDeg, target) {
                buildNavigationInfo(currentUserLocation, currentLocationAltitudeM, deviceHeadingDeg, target)
            }
        }
        val rulerDistance = if (rulerStartPoint != null && rulerEndPoint != null) {
            distanceMeters(rulerStartPoint, rulerEndPoint)
        } else null

        if (uiHideLevel < 1 && showNearMePanel) {
            NearMeFieldPanel(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(12f)
                    .padding(start = 8.dp, top = 14.dp, end = 8.dp),
                hasGps = currentUserLocation != null,
                accuracyM = currentLocationAccuracyM,
                waitingForGpsFix = waitingForGpsFix,
                filter = nearMeFilter,
                onFilterChange = { nearMeFilter = it },
                items = nearMeItems,
                onOpenRecord = { record ->
                    showNearMePanel = false
                    onViewOnMap(record)
                },
                onClose = { showNearMePanel = false }
            )
        }

        if (compassVisible && uiHideLevel < 2) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .zIndex(10f)
                    .padding(start = 12.dp, bottom = 112.dp)
            ) {
                CompactCompassWidget(
                    orientationMode = mapOrientationMode,
                    headingDeg = deviceHeadingDeg,
                    onToggle = { showOrientationDialog = true }
                )
            }
        }

        if (uiHideLevel < 2 && (
                navigationTarget != null ||
                rulerStartPoint != null ||
                positionEnabled ||
                liveTrackingEnabled ||
                hasPausedTrack
            )) {
            val accuracyLabel = when {
                waitingForGpsFix -> "čekam fix"
                currentLocationAccuracyM != null -> "± " + String.format(Locale.US, "%.1f m", currentLocationAccuracyM)
                else -> "± —"
            }
            val altitudeLabel = currentLocationAltitudeM?.let { String.format(Locale.US, "%.0f m", it) } ?: "—"
            val accuracyColor = when {
                waitingForGpsFix -> Color.White.copy(alpha = 0.72f)
                currentLocationAccuracyM != null && currentLocationAccuracyM > 10.0 -> Color(0xFFFF6B6B)
                currentLocationAccuracyM != null && currentLocationAccuracyM > 5.0 -> Color(0xFFFFCA28)
                currentLocationAccuracyM != null -> Color(0xFF6EE7A2)
                else -> Color.White.copy(alpha = 0.82f)
            }
            val navigationActive = navigationTarget != null
            val gpsBodyColor = when {
                navigationActive -> accuracyColor
                liveTrackingEnabled || hasPausedTrack -> Color.White.copy(alpha = 0.82f)
                else -> accuracyColor
            }
            UnifiedBottomHudBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(10f)
                    .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                navTitle = navigationTarget?.name,
                navBody = when {
                    navInfo != null -> "${formatDistance(navInfo.distanceM)} • Δh ${formatAltitudeDiff(navInfo.altitudeDiffM)} • $accuracyLabel"
                    else -> null
                },
                navBodyColor = accuracyColor,
                navArrowRotationDeg = navInfo?.relativeBearingDeg,
                onClearNav = onClearNavigationTarget,
                rulerBody = when {
                    rulerStartPoint != null && rulerEndPoint == null -> "Ruler • odaberi točku B"
                    rulerDistance != null -> "Ruler • ${formatDistance(rulerDistance)}"
                    else -> null
                },
                onClearRuler = { onSetRulerPoints(null, null) },
                gpsTitle = when {
                    liveTrackingEnabled -> "TRACK"
                    hasPausedTrack -> "PAUSE"
                    positionEnabled -> "GPS"
                    else -> null
                },
                gpsBody = when {
                    liveTrackingEnabled -> buildString {
                        append(formatDuration(currentTrackStartedAtMillis, trackingClockMillis))
                        append(" • ")
                        append(formatDistance(trackStats.distanceM))
                        append(" • ")
                        append(accuracyLabel)
                        append(" • ↑${trackStats.ascentM.roundToInt()} ↓${trackStats.descentM.roundToInt()}")
                    }
                    hasPausedTrack -> "${formatDistance(trackStats.distanceM)} • spremno • $accuracyLabel • ↑${trackStats.ascentM.roundToInt()} ↓${trackStats.descentM.roundToInt()}"
                    positionEnabled -> "$accuracyLabel • $altitudeLabel"
                    else -> null
                },
                gpsBodyColor = gpsBodyColor,
                gpsAccent = when {
                    liveTrackingEnabled -> Color(0xFFFF6B6B)
                    hasPausedTrack -> Color(0xFFFFCA28)
                    else -> Color(0xFF6EE7A2)
                },
                onGpsClick = if (!liveTrackingEnabled && !hasPausedTrack) ({ gpsPanelExpanded = !gpsPanelExpanded }) else null,
                gpsExtra = if (!liveTrackingEnabled && !hasPausedTrack && gpsPanelExpanded) {
                    "Provider: " + (currentLocationProvider?.uppercase(Locale.US) ?: if (waitingForGpsFix) "GPS" else "—")
                } else null
            )
        }

        if (uiHideLevel < 2 && (effectiveMode == MapLayerMode.HGSS_SIGURNE_STAZE || effectiveMode == MapLayerMode.HGSS_OSM_TEST)) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .zIndex(8f)
                    .padding(start = 12.dp, bottom = 92.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "HGSS / SigurneStaze.hr",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiHideLevel < 2 && (downloadBusy || downloadMessage != null)) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(10f)
                    .padding(bottom = 22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (downloadBusy) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(downloadProgressLabel)
                            }
                            LinearProgressIndicator(progress = { downloadProgress.coerceIn(0f, 1f) }, modifier = Modifier.width(220.dp))
                            Text("${(downloadProgress.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    downloadMessage?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }


    pendingLongPressPoint?.let { point ->
        AlertDialog(
            onDismissRequest = { pendingLongPressPoint = null },
            title = { Text("Waypoint") },
            text = {
                Text(
                    text = "Dodaj waypoint na ovoj lokaciji?\n" +
                        String.format(Locale.US, "%.6f, %.6f", point.latitude, point.longitude)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onMarkPositionAtPoint(point)
                    pendingLongPressPoint = null
                }) { Text("Add waypoint") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        pendingLongPressPoint = null
                        showManualCoordDialog = true
                    }) { Text("Izmijeni koordinate") }
                    TextButton(onClick = { pendingLongPressPoint = null }) { Text("Odustani") }
                }
            }
        )
    }

    if (showManualCoordDialog) {
        ManualCoordDialog(
            onDismiss = { showManualCoordDialog = false },
            onConfirm = { point ->
                showManualCoordDialog = false
                onMarkPositionAtPoint(point)
                onFocusPoint(point)
            }
        )
    }

    if (showOrientationDialog) {
        OrientationModeDialog(
            current = mapOrientationMode,
            onDismiss = { showOrientationDialog = false },
            onSelect = { target ->
                applyOrientationMode(mapOrientationMode, target, onToggleOrientationMode)
                showOrientationDialog = false
            }
        )
    }

    pendingMapToolAction?.let { action ->
        ConfirmMapToolDialog(
            action = action,
            onDismiss = { pendingMapToolAction = null },
            onConfirm = {
                when (action) {
                    "RULER" -> if (!rulerModeEnabled) onToggleRulerMode()
                    "DRAW" -> if (!drawingModeEnabled) onToggleDrawingMode()
                    "AREA" -> beginAreaSelectionMode()
                }
                pendingMapToolAction = null
            }
        )
    }

    if (showSearchSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showSearchSheet = false }, sheetState = sheetState) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp)) {
                SearchScreen(
                    filters = filters,
                    records = records,
                    locationOptions = locationOptions,
                    hasCurrentUserLocation = hasCurrentUserLocation,
                    isFiltering = isFiltering,
                    searchReady = searchReady,
                    onFiltersChanged = { updatedFilters ->
                        localSearchFocusPoints = emptyList()
                        localSearchFocusRecordIds = emptySet()
                        localSearchFocusNonce = 0
                        onFiltersChanged(updatedFilters)
                    },
                    onSelect = {
                        onSelect(it)
                        showSearchSheet = false
                    },
                    onViewOnMap = {
                        onViewOnMap(it)
                        showSearchSheet = false
                    },
                    onRequestGpsLocation = onRequestGpsLocation,
                    onShowFilteredOnMap = {
                        val targetRecords = records.filterByBoundingBox(activeFieldPackageBounds)
                        localSearchFocusPoints = targetRecords.mapNotNull { rec ->
                            val lat = rec.location.lat
                            val lon = rec.location.lon
                            if (lat != null && lon != null) GeoPoint(lat, lon) else null
                        }
                        localSearchFocusRecordIds = targetRecords.mapTo(mutableSetOf()) { it.id }
                        localSearchFocusNonce++
                        showSearchSheet = false
                    },
                    onExportKml = {
                        onShowExport()
                        showSearchSheet = false
                    }
                )
            }
        }
    }

    if (showToolsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showToolsSheet = false }, sheetState = sheetState) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(language.pick("Alati karte", "Map tools"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    language.pick("Čisti map-first UX: osnovne radnje su pri ruci, a invazivni modovi se pale samo nakon potvrde.", "Clean map-first UX: core actions stay close, and intrusive modes only start after confirmation."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ToolSheetButton(icon = Icons.Default.FolderOpen, title = "Open KML / GPX / MBTiles", subtitle = language.pick("Uvezi lokalne datoteke", "Import local files")) {
                    showToolsSheet = false
                    onImport()
                }
                ToolSheetButton(icon = Icons.Default.Download, title = "Export KML / GPX", subtitle = language.pick("Spremi rezultate, točke ili trackove", "Save results, points or tracks")) {
                    showToolsSheet = false
                    onShowExport()
                }
                ToolSheetButton(icon = Icons.Default.Storage, title = language.pick("Overlay / spremljeno", "Layers / saved"), subtitle = language.pick("Spremljeni trackovi, točke, karte i importi", "Saved tracks, points, maps and imports")) {
                    showToolsSheet = false
                    onOpenOfflineMenu()
                }
                ToolSheetButton(
                    icon = Icons.Default.Lens,
                    title = if (simplePointViewEnabled) language.pick("Simple view uključen", "Simple view on") else "Simple view",
                    subtitle = if (simplePointViewEnabled) language.pick("Jednostavne točke na karti • ostaje zapamćeno", "Simple points on the map • remembered") else language.pick("Prikaži jednostavne točke na karti • ostaje zapamćeno", "Show simple points on the map • remembered"),
                    accent = if (simplePointViewEnabled) Color(0xFFFFCA28) else null
                ) {
                    onToggleSimplePointView()
                }
                ToolSheetButton(
                    icon = Icons.Default.Straighten,
                    title = if (rulerModeEnabled || rulerStartPoint != null || rulerEndPoint != null) language.pick("Ruler aktivan", "Ruler active") else "Ruler",
                    subtitle = if (rulerModeEnabled || rulerStartPoint != null || rulerEndPoint != null) language.pick("Mjerenje udaljenosti je uključeno", "Distance measuring is on") else language.pick("Uključi mjerenje udaljenosti na karti", "Enable distance measuring on the map"),
                    accent = if (rulerModeEnabled || rulerStartPoint != null || rulerEndPoint != null) Color(0xFF9CDCFE) else null
                ) {
                    showToolsSheet = false
                    if (rulerModeEnabled) onToggleRulerMode() else pendingMapToolAction = "RULER"
                }
                ToolSheetButton(
                    icon = Icons.Default.Edit,
                    title = if (drawingModeEnabled) language.pick("Drawing pen uključen", "Drawing pen on") else "Drawing pen",
                    subtitle = when {
                        drawingModeEnabled && drawingPoints.isEmpty() -> language.pick("Povlači prstom po karti • toolbar je lijevo", "Drag your finger on the map • toolbar is on the left")
                        drawingModeEnabled -> language.pick("${drawingPoints.size} točaka • toolbar za crtanje je lijevo", "${drawingPoints.size} points • drawing toolbar is on the left")
                        drawingPoints.isNotEmpty() -> language.pick("Nastavi ili spremi ručno nacrtanu rutu", "Continue or save the hand-drawn route")
                        else -> language.pick("Ručno crtanje rute po karti", "Manually draw a route on the map")
                    },
                    accent = if (drawingModeEnabled) Color(0xFFFFB74D) else null
                ) {
                    showToolsSheet = false
                    if (drawingModeEnabled) onFinishDrawingMode() else pendingMapToolAction = "DRAW"
                }
                ToolSheetButton(
                    icon = Icons.Default.Delete,
                    title = "Clear",
                    subtitle = if (hasClearableState) language.pick("Obriši privremene točke, track i navigaciju", "Clear temporary points, track and navigation") else language.pick("Nema privremenog sadržaja za obrisati", "No temporary content to clear"),
                    accent = if (hasClearableState) Color(0xFFFFB86B) else null,
                    enabled = hasClearableState
                ) {
                    showToolsSheet = false
                    onClearAllUserContent()
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    if (selectedAreaPoints.size == 2) {
        val p1 = selectedAreaPoints[0]
        val p2 = selectedAreaPoints[1]
        val minLat = minOf(p1.latitude, p2.latitude)
        val maxLat = maxOf(p1.latitude, p2.latitude)
        val minLon = minOf(p1.longitude, p2.longitude)
        val maxLon = maxOf(p1.longitude, p2.longitude)
        val basicSpec = OfflineTileManager.OfflineAreaSpec(minLat, maxLat, minLon, maxLon, 8, 12)
        val terrainSpec = OfflineTileManager.OfflineAreaSpec(minLat, maxLat, minLon, maxLon, 8, 14)
        val detailSpec = OfflineTileManager.OfflineAreaSpec(minLat, maxLat, minLon, maxLon, 9, 15)
        val estimateOfflineSizeText: (OfflineTileManager.OfflineAreaSpec) -> String = { spec ->
            val tiles = OfflineTileManager.estimateTiles(spec).coerceAtLeast(0)
            val estimatedBytes = tiles.toLong() * 45L * 1024L
            when {
                estimatedBytes >= 1024L * 1024L * 1024L -> "≈ ${java.lang.String.format(Locale.US, "%.1f", estimatedBytes / 1024.0 / 1024.0 / 1024.0)} GB"
                estimatedBytes >= 1024L * 1024L -> "≈ ${(estimatedBytes / 1024L / 1024L).coerceAtLeast(1L)} MB"
                estimatedBytes > 0L -> "≈ ${(estimatedBytes / 1024L).coerceAtLeast(1L)} KB"
                else -> "≈ 0 MB"
            }
        }
        val downloadSourceMode = offlineDownloadSourceMode ?: when (selectedMode) {
            MapLayerMode.HGSS_SIGURNE_STAZE, MapLayerMode.HGSS_OSM_TEST -> MapLayerMode.HGSS_OSM_TEST
            else -> effectiveMode
        }
        val downloadSourceIsHgss = downloadSourceMode == MapLayerMode.HGSS_SIGURNE_STAZE || downloadSourceMode == MapLayerMode.HGSS_OSM_TEST
        val downloadSourceLabel = if (downloadSourceIsHgss) "HGSS SigurneStaze" else "trenutni WMS sloj"
        AlertDialog(
            onDismissRequest = {
                selectedAreaPoints = emptyList()
                selectingOfflineArea = false
            },
            title = { Text("Skini offline područje") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = offlineMapName,
                        onValueChange = { offlineMapName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Naziv offline karte") }
                    )
                    Text("Odabrao si 2 kuta područja. Izvor karte: $downloadSourceLabel. Izaberi detaljnost downloada.")
                    Text("Osnovno: z8–12 (${OfflineTileManager.estimateTiles(basicSpec)} tileova • ${estimateOfflineSizeText(basicSpec)})")
                    Text("Teren: z8–14 (${OfflineTileManager.estimateTiles(terrainSpec)} tileova • ${estimateOfflineSizeText(terrainSpec)})")
                    Text("Detaljno: z9–15 (${OfflineTileManager.estimateTiles(detailSpec)} tileova • ${estimateOfflineSizeText(detailSpec)})")
                    Text(
                        "Savjet: za veće područje koristi Osnovno ili Teren. Prevelik odabir će app odbiti.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        downloadBusy = true
                        downloadProgress = 0f
                        downloadProgressLabel = "Pripremam offline kartu…"
                        val spec = basicSpec
                        val mapName = OfflineTileManager.sanitizeMapName(offlineMapName)
                        selectedAreaPoints = emptyList()
                        scope.launch {
                            val result = if (downloadSourceIsHgss) {
                                OfflineTileManager.downloadHgssOsmArea(
                                    context = context,
                                    spec = spec,
                                    mapName = mapName,
                                    onProgress = { done, total, zoom ->
                                        downloadProgress = done.toFloat() / total.coerceAtLeast(1).toFloat()
                                        downloadProgressLabel = "Skidam HGSS offline kartu… z$zoom ($done/$total)"
                                    }
                                )
                            } else {
                                OfflineTileManager.downloadWmsArea(
                                    context = context,
                                    config = wmsConfig,
                                    spec = spec,
                                    mapName = mapName,
                                    onProgress = { done, total, zoom ->
                                        downloadProgress = done.toFloat() / total.coerceAtLeast(1).toFloat()
                                        downloadProgressLabel = "Skidam offline kartu… z$zoom ($done/$total)"
                                    }
                                )
                            }
                            downloadBusy = false
                            MapLayerPrefs.setMode(context, MapLayerMode.OFFLINE)
                            localMapLayerVersion++
                            result.fold(
                                onSuccess = {
                                    downloadProgress = 1f
                                    downloadProgressLabel = "Offline karta spremljena"
                                    val center = GeoPoint((spec.minLat + spec.maxLat) / 2.0, (spec.minLon + spec.maxLon) / 2.0)
                                    val targetZoom = (spec.minZoom + 1).toDouble()
                                    onOfflineDownloaded(center, targetZoom)
                                    downloadMessage = "Offline spremljeno '$mapName': ${it.first} novih, ${it.second} preskočenih tileova."
                                },
                                onFailure = { downloadMessage = "Download nije uspio: ${it.message}" }
                            )
                        }
                    }) { Text("Osnovno") }
                    TextButton(onClick = {
                        downloadBusy = true
                        downloadProgress = 0f
                        downloadProgressLabel = "Pripremam offline kartu…"
                        val spec = terrainSpec
                        val mapName = OfflineTileManager.sanitizeMapName(offlineMapName)
                        selectedAreaPoints = emptyList()
                        scope.launch {
                            val result = if (downloadSourceIsHgss) {
                                OfflineTileManager.downloadHgssOsmArea(
                                    context = context,
                                    spec = spec,
                                    mapName = mapName,
                                    onProgress = { done, total, zoom ->
                                        downloadProgress = done.toFloat() / total.coerceAtLeast(1).toFloat()
                                        downloadProgressLabel = "Skidam HGSS offline kartu… z$zoom ($done/$total)"
                                    }
                                )
                            } else {
                                OfflineTileManager.downloadWmsArea(
                                    context = context,
                                    config = wmsConfig,
                                    spec = spec,
                                    mapName = mapName,
                                    onProgress = { done, total, zoom ->
                                        downloadProgress = done.toFloat() / total.coerceAtLeast(1).toFloat()
                                        downloadProgressLabel = "Skidam offline kartu… z$zoom ($done/$total)"
                                    }
                                )
                            }
                            downloadBusy = false
                            MapLayerPrefs.setMode(context, MapLayerMode.OFFLINE)
                            localMapLayerVersion++
                            result.fold(
                                onSuccess = {
                                    downloadProgress = 1f
                                    downloadProgressLabel = "Offline karta spremljena"
                                    val center = GeoPoint((spec.minLat + spec.maxLat) / 2.0, (spec.minLon + spec.maxLon) / 2.0)
                                    val targetZoom = (spec.minZoom + 1).toDouble()
                                    onOfflineDownloaded(center, targetZoom)
                                    downloadMessage = "Offline spremljeno '$mapName': ${it.first} novih, ${it.second} preskočenih tileova."
                                },
                                onFailure = { downloadMessage = "Download nije uspio: ${it.message}" }
                            )
                        }
                    }) { Text("Teren") }
                    TextButton(onClick = {
                        downloadBusy = true
                        downloadProgress = 0f
                        downloadProgressLabel = "Pripremam offline kartu…"
                        val spec = detailSpec
                        val mapName = OfflineTileManager.sanitizeMapName(offlineMapName)
                        selectedAreaPoints = emptyList()
                        scope.launch {
                            val result = if (downloadSourceIsHgss) {
                                OfflineTileManager.downloadHgssOsmArea(
                                    context = context,
                                    spec = spec,
                                    mapName = mapName,
                                    onProgress = { done, total, zoom ->
                                        downloadProgress = done.toFloat() / total.coerceAtLeast(1).toFloat()
                                        downloadProgressLabel = "Skidam HGSS offline kartu… z$zoom ($done/$total)"
                                    }
                                )
                            } else {
                                OfflineTileManager.downloadWmsArea(
                                    context = context,
                                    config = wmsConfig,
                                    spec = spec,
                                    mapName = mapName,
                                    onProgress = { done, total, zoom ->
                                        downloadProgress = done.toFloat() / total.coerceAtLeast(1).toFloat()
                                        downloadProgressLabel = "Skidam offline kartu… z$zoom ($done/$total)"
                                    }
                                )
                            }
                            downloadBusy = false
                            MapLayerPrefs.setMode(context, MapLayerMode.OFFLINE)
                            localMapLayerVersion++
                            result.fold(
                                onSuccess = {
                                    downloadProgress = 1f
                                    downloadProgressLabel = "Offline karta spremljena"
                                    val center = GeoPoint((spec.minLat + spec.maxLat) / 2.0, (spec.minLon + spec.maxLon) / 2.0)
                                    val targetZoom = (spec.minZoom + 1).toDouble()
                                    onOfflineDownloaded(center, targetZoom)
                                    downloadMessage = "Offline spremljeno '$mapName': ${it.first} novih, ${it.second} preskočenih tileova."
                                },
                                onFailure = { downloadMessage = "Download nije uspio: ${it.message}" }
                            )
                        }
                    }) { Text("Detaljno") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedAreaPoints = emptyList()
                    selectingOfflineArea = false
                    downloadMessage = null
                }) { Text("Odustani") }
            }
        )
    }

    if (showLayerDialog) {
        AlertDialog(
            onDismissRequest = { showLayerDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(language.pick("Karte i slojevi", "Maps & Layers"), fontWeight = FontWeight.Bold)
                    Text(
                        language.pick("Prvo odaberi osnovnu kartu, zatim po želji dodaj reljef ili geo sloj.", "First choose the base map, then optionally add relief or a geo overlay."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 640.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MapLayerPanelSection(
                        title = language.pick("1. Osnovna karta", "1. Base map"),
                        subtitle = language.pick("Samo jedna osnovna karta je aktivna. Ovo je podloga ispod svih objekata i trackova.", "Only one base map is active. It sits under all objects and tracks."),
                        icon = Icons.Default.Map,
                        accent = Color(0xFF72E0C4)
                    ) {
                        MapLayerChoiceButton(
                            title = "Auto",
                            subtitle = if (offlineAvailable) language.pick("koristi offline kartu kad postoji", "uses the offline map when available") else language.pick("koristi zadanu WMS kartu", "uses the default WMS map"),
                            selected = selectedMode == MapLayerMode.AUTO,
                            accent = Color(0xFF72E0C4),
                            icon = Icons.Default.CheckCircle,
                            onClick = {
                                MapLayerPrefs.setMode(context, MapLayerMode.AUTO)
                                localMapLayerVersion++
                                showLayerDialog = false
                            }
                        )

                        WMS_PRESETS.forEach { (label, config) ->
                            MapLayerChoiceButton(
                                title = label,
                                subtitle = language.pick("DGU / WMS osnovna karta", "DGU / WMS base map"),
                                selected = selectedMode == MapLayerMode.WMS && wmsConfig.baseUrl == config.baseUrl && wmsConfig.layers == config.layers,
                                accent = Color(0xFF8EC5FF),
                                icon = Icons.Default.Map,
                                onClick = {
                                    MapLayerPrefs.setWmsConfig(context, config)
                                    MapLayerPrefs.setMode(context, MapLayerMode.WMS)
                                    localMapLayerVersion++
                                    showLayerDialog = false
                                }
                            )
                        }

                        MapLayerChoiceButton(
                            title = "OpenTopo",
                            subtitle = language.pick("online topo podloga", "online topo base map"),
                            selected = selectedMode == MapLayerMode.OPENTOPO,
                            accent = Color(0xFFFFC46B),
                            icon = Icons.Default.Map,
                            onClick = {
                                MapLayerPrefs.setMode(context, MapLayerMode.OPENTOPO)
                                localMapLayerVersion++
                                showLayerDialog = false
                            }
                        )

                        MapLayerChoiceButton(
                            title = "HGSS SigurneStaze",
                            subtitle = language.pick("outdoor raster podloga", "outdoor raster base map"),
                            selected = selectedMode == MapLayerMode.HGSS_SIGURNE_STAZE || selectedMode == MapLayerMode.HGSS_OSM_TEST,
                            accent = Color(0xFFC7A7FF),
                            icon = Icons.Default.TravelExplore,
                            onClick = {
                                MapLayerPrefs.setMode(context, MapLayerMode.HGSS_OSM_TEST)
                                localMapLayerVersion++
                                showLayerDialog = false
                                Toast.makeText(context, "HGSS SigurneStaze layer selected.", Toast.LENGTH_SHORT).show()
                            }
                        )

                        MapLayerChoiceButton(
                            title = language.pick("Offline karta", "Offline map"),
                            subtitle = if (offlineAvailable) language.pick("koristi spremljeni offline paket", "uses the saved offline package") else language.pick("nije spremljena — idi u Offline/Tools", "not saved — go to Offline/Tools"),
                            selected = selectedMode == MapLayerMode.OFFLINE || effectiveMode == MapLayerMode.OFFLINE,
                            enabled = offlineAvailable,
                            accent = Color(0xFF9EE7D8),
                            icon = Icons.Default.Storage,
                            onClick = {
                                MapLayerPrefs.setMode(context, MapLayerMode.OFFLINE)
                                localMapLayerVersion++
                                showLayerDialog = false
                            }
                        )
                    }

                    MapLayerPanelSection(
                        title = language.pick("2. Poboljšanja prikaza", "2. Visual enhancements"),
                        subtitle = language.pick("Ovi slojevi ne mijenjaju osnovnu kartu — samo je čine čitljivijom.", "These layers do not replace the base map — they only make it easier to read."),
                        icon = Icons.Default.Visibility,
                        accent = Color(0xFFFFC46B)
                    ) {
                        MapLayerToggleCard(
                            title = language.pick("Reljef / Hillshade", "Relief / Hillshade"),
                            subtitle = if (hillshadeEnabled) language.pick("Uključen · intenzitet ${hillshadeOpacityPercent}%", "On · intensity ${hillshadeOpacityPercent}%") else language.pick("Isključen · karta ostaje bez dodatnog reljefa", "Off · no extra relief on the map"),
                            selected = hillshadeEnabled,
                            accent = Color(0xFFFFC46B),
                            icon = Icons.Default.Lens,
                            onClick = {
                                HillshadePrefs.setEnabled(context, !hillshadeEnabled)
                                if (!hillshadeEnabled) HillshadePrefs.setOpacityPercent(context, 40)
                                localMapLayerVersion++
                            }
                        )

                        if (hillshadeEnabled) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(25 to language.pick("Blago", "Soft"), 40 to language.pick("Teren", "Field"), 55 to language.pick("Jasno", "Strong")).forEach { (value, label) ->
                                    FilterChip(
                                        selected = hillshadeOpacityPercent == value,
                                        onClick = {
                                            HillshadePrefs.setOpacityPercent(context, value)
                                            localMapLayerVersion++
                                        },
                                        label = { Text("$label · $value%") }
                                    )
                                }
                            }
                            Text(
                                language.pick("Preporuka: 25–40% za teren. 55% koristi samo kad želiš jače naglasiti reljef.", "Recommended: 25–40% in the field. Use 55% only when you want stronger relief."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    MapLayerPanelSection(
                        title = language.pick("3. Overlay slojevi", "3. Overlay layers"),
                        subtitle = language.pick("Overlay ide preko osnovne karte. Koristi ga kad želiš dodatnu informaciju, npr. geologiju.", "Overlays sit above the base map. Use them when you need extra information, such as geology."),
                        icon = Icons.Default.Layers,
                        accent = Color(0xFFC7A7FF)
                    ) {
                        MapLayerToggleCard(
                            title = "Geological Units",
                            subtitle = if (geologicalOverlayEnabled) language.pick("Uključen preko karte · ${geologicalOverlayOpacityPercent}%", "On above the map · ${geologicalOverlayOpacityPercent}%") else language.pick("Isključen · nema geo sloja preko karte", "Off · no geo overlay above the map"),
                            selected = geologicalOverlayEnabled,
                            accent = Color(0xFFC7A7FF),
                            icon = Icons.Default.Layers,
                            onClick = {
                                MapLayerPrefs.setGeologicalOverlayEnabled(context, !geologicalOverlayEnabled)
                                if (!geologicalOverlayEnabled) MapLayerPrefs.setGeologicalOverlayOpacityPercent(context, 40)
                                localMapLayerVersion++
                            }
                        )

                        if (geologicalOverlayEnabled) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(25 to language.pick("Blago", "Soft"), 40 to language.pick("Čitko", "Readable"), 55 to language.pick("Jako", "Strong")).forEach { (value, label) ->
                                    FilterChip(
                                        selected = geologicalOverlayOpacityPercent == value,
                                        onClick = {
                                            MapLayerPrefs.setGeologicalOverlayOpacityPercent(context, value)
                                            localMapLayerVersion++
                                        },
                                        label = { Text("$label · $value%") }
                                    )
                                }
                            }
                            Text(
                                language.pick("Preporuka: Geological Units 25–40%. SOV objekti, trackovi i GPS ostaju iznad overlaya.", "Recommended: Geological Units 25–40%. SOV objects, tracks and GPS stay above the overlay."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    MapLayerPanelSection(
                        title = language.pick("4. Moji WMS slojevi", "4. My WMS layers"),
                        subtitle = language.pick("Dodatni servisi koje sam dodaješ. Mogu biti osnovna karta ili overlay, ovisno o transparentnosti.", "Extra services you add yourself. They can work as a base map or an overlay depending on transparency."),
                        icon = Icons.Default.FolderOpen,
                        accent = Color(0xFF8EC5FF)
                    ) {
                        if (savedCustomWmsConfigs.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                            ) {
                                Text(
                                    language.pick("Nema spremljenih custom WMS slojeva.", "No custom WMS layers saved."),
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            savedCustomWmsConfigs.forEach { saved ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.13f))
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(
                                                if (saved.config.transparent) Icons.Default.Layers else Icons.Default.Map,
                                                contentDescription = null,
                                                tint = Color(0xFF8EC5FF),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(saved.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    if (saved.config.transparent) language.pick("Overlay WMS · ${saved.config.layers}", "Overlay WMS · ${saved.config.layers}") else language.pick("Base WMS · ${saved.config.layers}", "Base WMS · ${saved.config.layers}"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Button(
                                                onClick = {
                                                    MapLayerPrefs.setWmsConfig(context, saved.config)
                                                    MapLayerPrefs.setMode(context, MapLayerMode.WMS)
                                                    localMapLayerVersion++
                                                    showLayerDialog = false
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(16.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
                                            ) { Text(language.pick("Uključi", "Enable"), maxLines = 1) }
                                            OutlinedButton(
                                                onClick = {
                                                    customWmsName = saved.name
                                                    customWmsUrl = saved.config.baseUrl
                                                    customWmsLayers = saved.config.layers
                                                    customWmsCrs = saved.config.crs
                                                    customWmsVersion = saved.config.version
                                                    customWmsStyles = saved.config.styles
                                                    customWmsTransparent = saved.config.transparent
                                                    customWmsMode = if (saved.config.transparent) "overlay" else "base"
                                                    showLayerDialog = false
                                                    showCustomWmsDialog = true
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(16.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
                                            ) { Text(language.pick("Uredi", "Edit"), maxLines = 1) }
                                            OutlinedButton(
                                                onClick = {
                                                    MapLayerPrefs.deleteCustomWmsConfig(context, saved.name)
                                                    localMapLayerVersion++
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(16.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
                                            ) { Text(language.pick("Obriši", "Delete"), maxLines = 1) }
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                customWmsName = ""
                                customWmsUrl = ""
                                customWmsLayers = ""
                                customWmsCrs = "EPSG:3857"
                                customWmsVersion = "1.1.1"
                                customWmsStyles = ""
                                customWmsTransparent = true
                                customWmsMode = "overlay"
                                capabilitiesMessage = null
                                capabilitiesLayers = emptyList()
                                selectedCapabilitiesLayer = null
                                showLayerDialog = false
                                showCustomWmsDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(language.pick("Dodaj WMS sloj", "Add WMS layer"))
                        }

                        Text(
                            language.pick("Savjet: za geologiju i posebne slojeve koristi transparentni overlay. Za potpunu kartu koristi base WMS.", "Tip: use a transparent overlay for geology and special layers. Use base WMS for a full map."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = Color(0xFF151C26),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF9EE7D8), modifier = Modifier.size(20.dp))
                            Text(
                                language.pick("Layer redoslijed: osnovna karta → hillshade/geo overlay/WMS overlay → SOV objekti → trackovi/GPS.", "Layer order: base map → hillshade/geo overlay/WMS overlay → SOV objects → tracks/GPS."),
                                color = Color.White.copy(alpha = 0.74f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLayerDialog = false }) { Text(language.pick("Zatvori", "Close")) } }
        )
    }


    if (showCustomWmsDialog) {
        val cleanUrl = MapLayerPrefs.cleanWmsBaseUrl(customWmsUrl)
        val isValidCustomWms = cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")
        val canSaveCustomWms = isValidCustomWms && customWmsLayers.trim().isNotBlank()
        AlertDialog(
            onDismissRequest = { showCustomWmsDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(language.pick("Dodaj WMS sloj", "Add WMS layer"), fontWeight = FontWeight.Bold)
                    Text(language.pick("Spremi ga kao osnovnu kartu ili transparentni overlay.", "Save it as a base map or a transparent overlay."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        language.pick("Zalijepi WMS URL. Najlakše je prvo učitati layere, a zatim odabrati želiš li ga koristiti kao base kartu ili overlay preko karte.", "Paste the WMS URL. It is easiest to load layers first, then choose whether to use it as a base map or an overlay."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customWmsName,
                        onValueChange = { customWmsName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(language.pick("Naziv u appu", "Name in app")) },
                        placeholder = { Text(language.pick("npr. DGU HOK ili Geologija Istre", "e.g. DGU HOK or Istria geology")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customWmsUrl,
                        onValueChange = {
                            customWmsUrl = it
                            capabilitiesMessage = null
                            capabilitiesLayers = emptyList()
                            selectedCapabilitiesLayer = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("WMS URL") },
                        placeholder = { Text(language.pick("https://.../wms ili .../ows", "https://.../wms or .../ows")) },
                        minLines = 2
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            enabled = isValidCustomWms && !capabilitiesBusy,
                            onClick = {
                                capabilitiesBusy = true
                                capabilitiesMessage = language.pick("Učitavam GetCapabilities…", "Loading GetCapabilities…")
                                capabilitiesLayers = emptyList()
                                selectedCapabilitiesLayer = null
                                scope.launch {
                                    val result = WmsCapabilitiesClient.fetch(customWmsUrl)
                                    result.onSuccess { capabilities ->
                                        capabilitiesVersion = capabilities.version
                                        customWmsUrl = capabilities.serviceUrl
                                        customWmsVersion = capabilities.version.ifBlank { customWmsVersion }
                                        capabilitiesLayers = capabilities.layers
                                        capabilitiesMessage = if (capabilities.layers.isEmpty()) {
                                            language.pick("Servis je dostupan, ali nisam našao layer s imenom. Unesi LAYERS ručno.", "The service is available, but no named layer was found. Enter LAYERS manually.")
                                        } else {
                                            language.pick("Pronađeno layera: ${capabilities.layers.size}. Odaberi jedan iz liste.", "Layers found: ${capabilities.layers.size}. Choose one from the list.")
                                        }
                                    }.onFailure { error ->
                                        capabilitiesMessage = language.pick("Ne mogu učitati capabilities: ${error.message ?: "nepoznata greška"}. Možeš nastaviti ručno.", "Could not load capabilities: ${error.message ?: "unknown error"}. You can continue manually.")
                                    }
                                    capabilitiesBusy = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (capabilitiesBusy) language.pick("Učitavam…", "Loading…") else language.pick("Učitaj layere", "Load layers")) }
                        OutlinedButton(
                            onClick = {
                                customWmsUrl = cleanUrl
                                capabilitiesMessage = language.pick("URL očišćen za GetMap zahtjeve.", "URL cleaned for GetMap requests.")
                            },
                            enabled = customWmsUrl.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text(language.pick("Očisti URL", "Clean URL")) }
                    }
                    capabilitiesMessage?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.startsWith("Ne mogu") || message.startsWith("Could not")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (capabilitiesLayers.isNotEmpty()) {
                        Text(language.pick("Dostupni layeri", "Available layers"), fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            capabilitiesLayers.take(40).forEach { layer ->
                                val selected = selectedCapabilitiesLayer?.name == layer.name
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCapabilitiesLayer = layer
                                            customWmsLayers = layer.name
                                            if (customWmsName.isBlank()) customWmsName = layer.title.ifBlank { layer.name }
                                            val preferredCrs = listOf("EPSG:3857", "EPSG:3765", "CRS:84", "OGC:CRS84", "EPSG:4326")
                                                .firstOrNull { wanted -> layer.crsOptions.any { it.equals(wanted, ignoreCase = true) } }
                                                ?: layer.crsOptions.firstOrNull().orEmpty()
                                            if (preferredCrs.isNotBlank()) customWmsCrs = preferredCrs
                                            customWmsStyles = layer.styleOptions.firstOrNull().orEmpty()
                                        }
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(layer.title.ifBlank { layer.name }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(layer.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "CRS: ${layer.crsOptions.take(4).joinToString().ifBlank { language.pick("nije navedeno", "not listed") }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (capabilitiesLayers.size > 40) {
                                Text(
                                    language.pick("Prikazujem prvih 40 layera. Ako treba drugi, upiši njegov LAYERS naziv ručno.", "Showing the first 40 layers. If you need another one, enter its LAYERS name manually."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    selectedCapabilitiesLayer?.let { layer ->
                        if (layer.crsOptions.isNotEmpty()) {
                            Text(language.pick("CRS iz odabranog layera", "CRS from selected layer"), fontWeight = FontWeight.SemiBold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                layer.crsOptions.take(10).forEach { crsOption ->
                                    FilterChip(
                                        selected = customWmsCrs.equals(crsOption, ignoreCase = true),
                                        onClick = { customWmsCrs = crsOption },
                                        label = { Text(crsOption) }
                                    )
                                }
                            }
                        }
                        if (layer.styleOptions.isNotEmpty()) {
                            Text("Styles", fontWeight = FontWeight.SemiBold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                layer.styleOptions.take(10).forEach { styleOption ->
                                    FilterChip(
                                        selected = customWmsStyles.equals(styleOption, ignoreCase = true),
                                        onClick = { customWmsStyles = styleOption },
                                        label = { Text(styleOption) }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customWmsLayers,
                        onValueChange = { customWmsLayers = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Layer / LAYERS") },
                        placeholder = { Text(language.pick("npr. DOF, HOK ili TK25_NOVI", "e.g. DOF, HOK or TK25_NOVI")) },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customWmsCrs,
                            onValueChange = { customWmsCrs = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("CRS/SRS") },
                            placeholder = { Text("EPSG:3857") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customWmsVersion,
                            onValueChange = { customWmsVersion = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(language.pick("WMS verzija", "WMS version")) },
                            placeholder = { Text("1.3.0") },
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = customWmsStyles,
                        onValueChange = { customWmsStyles = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Styles") },
                        placeholder = { Text(language.pick("prazno ako servis ne traži stil", "empty if the service does not require a style")) },
                        singleLine = true
                    )
                    Text(language.pick("Kako se sloj koristi", "How the layer is used"), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = customWmsMode == "base",
                            onClick = {
                                customWmsMode = "base"
                                customWmsTransparent = false
                            },
                            label = { Text(language.pick("Osnovna karta", "Base map")) }
                        )
                        FilterChip(
                            selected = customWmsMode == "overlay",
                            onClick = {
                                customWmsMode = "overlay"
                                customWmsTransparent = true
                            },
                            label = { Text("Overlay") }
                        )
                    }
                    FilterChip(
                        selected = customWmsTransparent,
                        onClick = { customWmsTransparent = !customWmsTransparent },
                        label = { Text(if (customWmsTransparent) language.pick("Transparentno: uključeno", "Transparent: on") else language.pick("Transparentno: isključeno", "Transparent: off")) }
                    )
                    if (customWmsUrl.isNotBlank() && cleanUrl != customWmsUrl.trim()) {
                        Text(
                            language.pick("App će spremiti očišćeni URL: $cleanUrl", "The app will save the cleaned URL: $cleanUrl"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (customWmsUrl.isNotBlank() && !isValidCustomWms) {
                        Text(
                            language.pick("URL mora početi s http:// ili https://.", "URL must start with http:// or https://."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        language.pick("Napomena: QGIS automatski riješi puno WMS detalja. Ovdje app pokušava pročitati capabilities, ali za problematične servise i dalje možeš ručno upisati LAYERS, CRS i STYLE.", "Note: QGIS automatically handles many WMS details. Here the app tries to read capabilities, but for difficult services you can still enter LAYERS, CRS and STYLE manually."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canSaveCustomWms,
                    onClick = {
                        val config = WmsConfig(
                            baseUrl = cleanUrl,
                            layers = customWmsLayers.trim(),
                            crs = customWmsCrs.trim().ifBlank { "EPSG:3857" },
                            version = customWmsVersion.trim().ifBlank { "1.3.0" },
                            styles = customWmsStyles.trim(),
                            transparent = customWmsTransparent
                        )
                        MapLayerPrefs.saveCustomWmsConfig(context, customWmsName, config)
                        MapLayerPrefs.setWmsConfig(context, config)
                        MapLayerPrefs.setMode(context, MapLayerMode.WMS)
                        localMapLayerVersion++
                        showCustomWmsDialog = false
                        showLayerDialog = false
                    }
                ) { Text(language.pick("Spremi i uključi", "Save and enable")) }
            },
            dismissButton = { TextButton(onClick = { showCustomWmsDialog = false }) { Text(language.pick("Odustani", "Cancel")) } }
        )
    }
}

@Composable
internal fun CompactStatusChip(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xBF111111), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun ToolSheetButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val resolvedAccent = accent ?: premiumIconTint(title, active = enabled)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(20.dp), tint = resolvedAccent)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, textAlign = TextAlign.Start, modifier = Modifier.weight(1f, fill = false))
                    if (enabled) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(resolvedAccent, RoundedCornerShape(99.dp))
                        )
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}



@Composable
internal fun ActiveMapModeBanner(
    modifier: Modifier = Modifier,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF8E7DFF), RoundedCornerShape(999.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.72f), maxLines = 2)
            }
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
internal fun ConfirmMapToolDialog(
    action: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = when (action) {
        "RULER" -> "Uključi ruler?"
        "DRAW" -> "Uključi drawing pen?"
        "AREA" -> "Uključi map download mode?"
        else -> "Uključi alat?"
    }
    val body = when (action) {
        "RULER" -> "U ruler modu tapovi na kartu postavljaju mjerenje. Uključi ga samo kad stvarno mjeriš."
        "DRAW" -> "Drawing pen preuzima dodire po karti za crtanje. Uključi ga samo kad želiš crtati."
        "AREA" -> "U ovom modu tapneš dva kuta područja za offline kartu. Ostali map alati su privremeno zaključani."
        else -> "Ovaj alat mijenja ponašanje karte dok je aktivan."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Uključi") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

@Composable
internal fun OrientationModeDialog(
    current: MapOrientationMode,
    onDismiss: () -> Unit,
    onSelect: (MapOrientationMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Orijentacija karte") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kompas više ne mijenja mod slučajnim tapom. Odaberi način prikaza karte.")
                OrientationOptionButton("North-up", "Karta je uvijek okrenuta prema sjeveru", current == MapOrientationMode.NORTH_UP) {
                    onSelect(MapOrientationMode.NORTH_UP)
                }
                OrientationOptionButton("Heading-up", "Karta se okreće prema smjeru kretanja", current == MapOrientationMode.HEADING_UP) {
                    onSelect(MapOrientationMode.HEADING_UP)
                }
                OrientationOptionButton("Static", "Zaključan statični prikaz bez automatskog okretanja", current == MapOrientationMode.STATIC) {
                    onSelect(MapOrientationMode.STATIC)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } }
    )
}

@Composable
internal fun OrientationOptionButton(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Navigation,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, textAlign = TextAlign.Start, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
            }
        }
    }
}

internal fun applyOrientationMode(
    current: MapOrientationMode,
    target: MapOrientationMode,
    onToggle: () -> Unit
) {
    if (current == target) return
    val order = listOf(MapOrientationMode.NORTH_UP, MapOrientationMode.HEADING_UP, MapOrientationMode.STATIC)
    val currentIndex = order.indexOf(current).coerceAtLeast(0)
    val targetIndex = order.indexOf(target).coerceAtLeast(0)
    val steps = (targetIndex - currentIndex + order.size) % order.size
    repeat(steps) { onToggle() }
}

@Composable
internal fun DrawingToolbarOverlay(
    modifier: Modifier = Modifier,
    pointCount: Int,
    drawingStrokeWidthDp: Int,
    drawingSmoothEnabled: Boolean,
    canSave: Boolean,
    onFinish: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onToggleSmooth: () -> Unit,
    onSetStrokeWidth: (Int) -> Unit
) {
    Card(
        modifier = modifier.width(86.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pen",
                color = Color.White.copy(alpha = 0.94f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${pointCount} t.",
                color = Color.White.copy(alpha = 0.68f),
                style = MaterialTheme.typography.labelSmall
            )
            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            MiniMapIconButton(
                onClick = onUndo,
                icon = Icons.Default.RemoveCircleOutline,
                label = "Undo",
                iconTint = Color(0xFFFFD66B),
                emphasized = true,
                strongBackground = true
            )
            MiniMapIconButton(
                onClick = {
                    onClear()
                    onFinish()
                },
                icon = Icons.Default.Close,
                label = "Cancel",
                iconTint = Color(0xFFFF9578),
                emphasized = true,
                strongBackground = true
            )
            if (canSave) {
                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                MiniMapIconButton(
                    onClick = onSave,
                    icon = Icons.Default.Save,
                    label = "Save",
                    iconTint = Color(0xFF84E1A0),
                    emphasized = true,
                    strongBackground = true
                )
            }
        }
    }
}

@Composable
internal fun CompactTrackHudAction(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    tint: Color = Color.White
) {
    val resolvedTint = if (tint == Color.White) premiumIconTint(label) else tint
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(resolvedTint.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = resolvedTint, modifier = Modifier.size(16.dp))
    }
}

private data class NearMeFieldItem(
    val record: SpeleoRecord,
    val point: GeoPoint,
    val distanceM: Double,
    val badge: String,
    val subline: String
)

private fun buildNearMeItems(
    records: List<SpeleoRecord>,
    currentUserLocation: GeoPoint?,
    filter: String,
    maxItems: Int
): List<NearMeFieldItem> {
    if (currentUserLocation == null) return emptyList()
    return records.mapNotNull { record ->
        val lat = record.location.lat
        val lon = record.location.lon
        if (lat == null || lon == null) return@mapNotNull null
        val point = GeoPoint(lat, lon)
        val distanceM = distanceMeters(currentUserLocation, point)
        val fieldTasks = record.classification.field_tasks.orEmpty()
        val hasTask = fieldTasks.isNotEmpty() || record.research.survey_in_digital_base == false || record.content.clean_cave_report.isNullOrBlank()
        val needsReview = record.classification.field_tasks.orEmpty().isNotEmpty()
        val type = record.classification.object_type.orEmpty().lowercase(Locale.ROOT)
        val passesFilter = when (filter) {
            "needs" -> hasTask || needsReview
            "jama" -> type.contains("jama") || type.contains("pit")
            "spilja" -> type.contains("spilja") || type.contains("cave")
            else -> true
        }
        if (!passesFilter) return@mapNotNull null
        val badge = when {
            fieldTasks.any { it.contains("nacrt", ignoreCase = true) } || record.research.survey_in_digital_base == false -> "treba nacrt"
            hasTask -> "treba provjeru"
            else -> record.classification.object_type?.take(12).orEmpty().ifBlank { "objekt" }
        }
        val place = listOfNotNull(record.location.nearest_place, record.location.municipality).firstOrNull { it.isNotBlank() }
        val plate = record.condition.plate_number?.takeIf { it.isNotBlank() }?.let { "pločica $it" }
        NearMeFieldItem(
            record = record,
            point = point,
            distanceM = distanceM,
            badge = badge,
            subline = listOfNotNull(place, plate).take(2).joinToString(" · ").ifBlank { "tapni za prikaz na karti" }
        )
    }
        .sortedBy { it.distanceM }
        .take(maxItems)
}

@Composable
private fun NearMeFieldPanel(
    modifier: Modifier = Modifier,
    hasGps: Boolean,
    accuracyM: Double?,
    waitingForGpsFix: Boolean,
    filter: String,
    onFilterChange: (String) -> Unit,
    items: List<NearMeFieldItem>,
    onOpenRecord: (SpeleoRecord) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(0.99f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color(0xFF1C2A22),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF6EE7A2), modifier = Modifier.size(20.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Najbliže oko mene", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val status = when {
                        waitingForGpsFix -> "Čekam GPS fix…"
                        !hasGps -> "Uključi GPS za najbliže objekte"
                        accuracyM != null -> "GPS preciznost ± " + String.format(Locale.US, "%.1f m", accuracyM)
                        else -> "Najbliži speleo objekti oko tebe"
                    }
                    Text(status, color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.75f))
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NearMeFilterChip("all", "Sve", filter, onFilterChange)
                NearMeFilterChip("needs", "Treba", filter, onFilterChange)
                NearMeFilterChip("jama", "Jame", filter, onFilterChange)
                NearMeFilterChip("spilja", "Špilje", filter, onFilterChange)
            }

            if (!hasGps) {
                Text(
                    "Panel će se popuniti čim app dobije tvoju lokaciju.",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            } else if (items.isEmpty()) {
                Text(
                    "Nema najbližih objekata za ovaj filter oko tvoje GPS lokacije.",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items.forEach { item ->
                        NearMeResultRow(item = item, onClick = { onOpenRecord(item.record) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NearMeFilterChip(
    id: String,
    label: String,
    selected: String,
    onFilterChange: (String) -> Unit
) {
    FilterChip(
        selected = selected == id,
        onClick = { onFilterChange(id) },
        label = { Text(label, fontSize = 11.sp) }
    )
}

@Composable
private fun NearMeResultRow(item: NearMeFieldItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.055f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(58.dp)) {
                Text(formatDistance(item.distanceM), color = Color(0xFF6EE7A2), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(item.badge, color = Color.White.copy(alpha = 0.62f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.record.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.subline, color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White.copy(alpha = 0.62f), modifier = Modifier.size(16.dp))
        }
    }
}


@Composable
private fun MapLayerPanelSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = accent.copy(alpha = 0.16f),
                    contentColor = accent,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
                ) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(23.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun MapLayerChoiceButton(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean = true,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)
            selected -> accent.copy(alpha = 0.17f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
        },
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = when {
                !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                selected -> accent.copy(alpha = 0.48f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.13f)
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (selected) accent else LocalContentColor.current.copy(alpha = 0.78f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                contentColor = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (selected) "ON" else if (enabled) "OFF" else "N/A",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MapLayerToggleCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.13f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilterChip(
                selected = selected,
                onClick = onClick,
                label = { Text(if (selected) "ON" else "OFF") }
            )
        }
    }
}


@Composable
fun MiniMapIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    iconTint: Color? = null,
    active: Boolean = false,
    emphasized: Boolean = false,
    strongBackground: Boolean = false
) {
    val resolvedIconTint = iconTint ?: premiumIconTint(label, active = active)
    val accentGlow = resolvedIconTint.copy(alpha = if (strongBackground) 0.34f else if (active) 0.26f else if (emphasized) 0.16f else 0.12f)
    val backgroundColors = when {
        strongBackground && active -> listOf(Color(0xF0212932), accentGlow)
        strongBackground -> listOf(Color(0xEC1A2027), Color(0xD014191F))
        active -> listOf(Color(0xD21B222A), accentGlow)
        emphasized -> listOf(Color(0xC814171C), Color(0xA1121418))
        else -> listOf(Color(0x9914171C), Color(0x73121418))
    }
    val borderColor = resolvedIconTint.copy(alpha = if (strongBackground) 0.38f else if (active) 0.28f else if (emphasized) 0.18f else 0.10f)
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 54.dp)
            .background(
                brush = Brush.verticalGradient(colors = backgroundColors),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = resolvedIconTint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = if (active) 0.96f else 0.84f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(6.dp)
                    .background(resolvedIconTint, RoundedCornerShape(99.dp))
            )
        }
    }
}

@Composable
internal fun FieldHeaderPill(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UnifiedBottomHudBar(
    modifier: Modifier = Modifier,
    navTitle: String?,
    navBody: String?,
    navBodyColor: Color = Color.White.copy(alpha = 0.82f),
    navArrowRotationDeg: Float? = null,
    onClearNav: () -> Unit,
    rulerBody: String?,
    onClearRuler: () -> Unit,
    gpsTitle: String?,
    gpsBody: String?,
    gpsBodyColor: Color = Color.White.copy(alpha = 0.82f),
    gpsAccent: Color,
    onGpsClick: (() -> Unit)? = null,
    gpsExtra: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (navTitle != null && navBody != null) {
                HudSegment(
                    title = navTitle,
                    body = navBody,
                    bodyColor = navBodyColor,
                    accent = Color(0xFF6EE7A2),
                    arrowRotationDeg = navArrowRotationDeg,
                    onClick = onClearNav
                )
            }
            if (rulerBody != null) {
                HudSegment(
                    title = "Ruler",
                    body = rulerBody.removePrefix("Ruler • "),
                    accent = Color(0xFF9CDCFE),
                    icon = Icons.Default.Straighten,
                    onClick = onClearRuler
                )
            }
            if (gpsTitle != null && gpsBody != null) {
                HudSegment(
                    title = gpsTitle,
                    body = gpsBody,
                    bodyColor = gpsBodyColor,
                    extra = gpsExtra,
                    accent = gpsAccent,
                    icon = null,
                    onClick = onGpsClick
                )
            }
        }
    }
}

@Composable
internal fun HudSegment(
    title: String,
    body: String,
    accent: Color,
    bodyColor: Color = Color.White.copy(alpha = 0.82f),
    icon: ImageVector? = null,
    arrowRotationDeg: Float? = null,
    extra: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                arrowRotationDeg != null -> Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp).rotate(arrowRotationDeg)
                )
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                else -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accent, RoundedCornerShape(99.dp))
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Text(body, color = bodyColor, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            extra?.let {
                Text(it, color = Color.White.copy(alpha = 0.60f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun CompactCompassWidget(
    orientationMode: MapOrientationMode,
    headingDeg: Float,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        CompassWidget(
            orientationMode = orientationMode,
            headingDeg = headingDeg,
            onToggle = onToggle
        )
    }
}

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    records: List<SpeleoRecord>,
    activeFieldPackageBounds: BoundingBoxFilter? = null,
    onClearFieldPackageBounds: () -> Unit = {},
    focusRecord: SpeleoRecord? = null,
    focusedSavedTrack: SavedTrack? = null,
    focusedSavedTrackNonce: Int = 0,
    onSelect: (SpeleoRecord) -> Unit,
    offlineStateVersion: Int,
    mapLayerMode: MapLayerMode,
    wmsConfig: WmsConfig,
    hillshadeEnabled: Boolean,
    hillshadeOpacityPercent: Int,
    geologicalOverlayEnabled: Boolean,
    geologicalOverlayOpacityPercent: Int,
    fieldVisibilityEnabled: Boolean,
    currentUserLocation: GeoPoint?,
    currentLocationAccuracyM: Double?,
    currentLocationAltitudeM: Double?,
    currentLocationProvider: String?,
    waitingForGpsFix: Boolean,
    deviceHeadingDeg: Float,
    mapOrientationMode: MapOrientationMode,
    centerOnUserNonce: Int,
    autoCenterOnUserEnabled: Boolean,
    positionEnabled: Boolean,
    liveTrackingEnabled: Boolean,
    trackingSessionNonce: Int,
    trackPoints: List<TrackPoint>,
    markedPoints: List<MarkedPoint>,
    savedTracks: List<SavedTrack>,
    importedLayers: List<ImportedLayer>,
    drawingPoints: List<GeoPoint>,
    drawingModeEnabled: Boolean,
    drawingStrokeWidthDp: Int,
    drawingSmoothEnabled: Boolean,
    simplePointViewEnabled: Boolean,
    offlineFocusPoint: GeoPoint?,
    offlineFocusZoom: Double,
    offlineFocusNonce: Int,
    importedFocusPoint: GeoPoint?,
    importedFocusZoom: Double,
    importedFocusNonce: Int,
    searchFocusPoints: List<GeoPoint>,
    searchFocusRecordIds: Set<String> = emptySet(),
    searchFocusNonce: Int,
    persistedMapCenter: GeoPoint?,
    persistedMapZoom: Double,
    onMapCameraChanged: (GeoPoint, Double) -> Unit,
    areaSelectionMode: Boolean,
    selectedAreaPoints: List<GeoPoint>,
    onEditMarkedPoint: (MarkedPoint) -> Unit,
    onShowMarkedPointActions: (MarkedPoint) -> Unit,
    onShowImportedPointActions: (MarkedPoint) -> Unit,
    navigationTarget: NavigationTarget?,
    rulerStartPoint: GeoPoint?,
    rulerEndPoint: GeoPoint?,
    rulerModeEnabled: Boolean,
    onMapTapForRuler: (GeoPoint?) -> Unit,
    onMapTapForDrawing: (GeoPoint) -> Unit,
    onAreaCornerSelected: (GeoPoint) -> Unit,
    onLongPressPoint: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val offlineTileRoot = remember(context, offlineStateVersion) { OfflineTileManager.tileRoot(context) }
    val enabledCustomOverlayNames = remember(offlineStateVersion) {
        OfflineTileManager.getEnabledCustomOverlayNames(context).toList().sorted()
    }
    val selectedAreaSignature = remember(selectedAreaPoints) { buildGeoPointsSignature(selectedAreaPoints) }
    val liveTrackSignature = remember(trackPoints) { buildTrackStateSignature(trackPoints) }
    val focusedTrackSignature = remember(focusedSavedTrack?.id, focusedSavedTrack?.points) {
        buildTrackStateSignature(focusedSavedTrack?.points.orEmpty())
    }
    val markedPointsSignature = remember(markedPoints.toList()) { buildMarkedPointsSignature(markedPoints) }
    val savedTracksSignature = remember(savedTracks.toList()) { buildSavedTracksSignature(savedTracks) }
    val importedLayersSignature = remember(importedLayers.toList()) { buildImportedLayersSignature(importedLayers) }
    val enabledCustomOverlayNamesSignature = remember(enabledCustomOverlayNames) { enabledCustomOverlayNames.joinToString() }
    val openTopoTileSource = remember {
        XYTileSource("OpenTopoMap", 0, 17, 256, ".png", arrayOf(
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        ))
    }
    val blankOfflineTileSource = remember {
        XYTileSource("BlankOffline", 0, 19, 256, ".png", emptyArray())
    }
    val hgssOsmTileSource = remember { HGSSTileSource() }
    val drawingTouchOverlay = remember(onMapTapForDrawing) { DrawingTouchOverlay(onMapTapForDrawing) }
    var scaleBarOverlay by remember { mutableStateOf<ScaleBarOverlay?>(null) }
    val userNonce = centerOnUserNonce
    val activeTracking = liveTrackingEnabled
    val trackingNonce = trackingSessionNonce
    var lastAppliedFocusId by remember { mutableStateOf<String?>(null) }
    var lastAppliedTrackId by remember { mutableStateOf<String?>(null) }
    var lastAppliedTrackFocusNonce by remember { mutableIntStateOf(-1) }
    var lastAppliedUserNonce by remember { mutableIntStateOf(-1) }
    var lastAppliedAutoCenterSignature by remember { mutableStateOf<String?>(null) }
    var lastAppliedOfflineNonce by remember { mutableIntStateOf(-1) }
    var lastAppliedImportedNonce by remember { mutableIntStateOf(-1) }
    var lastAppliedSearchFocusNonce by remember { mutableIntStateOf(-1) }
    var restoredPersistedCamera by remember { mutableStateOf(false) }
    var lastAppliedTileMode by remember { mutableStateOf<MapLayerMode?>(null) }
    var lastAppliedWmsKey by remember { mutableStateOf<String?>(null) }
    var lastOverlaySignature by remember { mutableStateOf<String?>(null) }
    var lastRecordPlanKey by remember { mutableStateOf<String?>(null) }
    var lastRecordRenderPlan by remember { mutableStateOf<RecordRenderPlan?>(null) }
    var lastReportedCameraKey by remember { mutableStateOf<String?>(null) }
    var userLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var lastAppliedMapOrientation by remember { mutableFloatStateOf(Float.NaN) }

    LaunchedEffect(persistedMapCenter?.latitude, persistedMapCenter?.longitude, persistedMapZoom) {
        // Reset only when no new search focus is pending. Search focus has higher priority
        // than restoring the persisted camera when both are triggered by opening the Map tab.
        if (persistedMapCenter != null && searchFocusNonce == lastAppliedSearchFocusNonce) {
            restoredPersistedCamera = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            onRelease = { mapView -> mapView.onDetach() },
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(8.0)
                    controller.setCenter(GeoPoint(44.7, 15.0))
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            mapCenter?.let { center ->
                                val key = buildCameraStateKey(center.latitude, center.longitude, zoomLevelDouble)
                                if (key != lastReportedCameraKey) {
                                    lastReportedCameraKey = key
                                    onMapCameraChanged(GeoPoint(center.latitude, center.longitude), zoomLevelDouble)
                                }
                            }
                            return false
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            mapCenter?.let { center ->
                                val key = buildCameraStateKey(center.latitude, center.longitude, zoomLevelDouble)
                                if (key != lastReportedCameraKey) {
                                    lastReportedCameraKey = key
                                    onMapCameraChanged(GeoPoint(center.latitude, center.longitude), zoomLevelDouble)
                                }
                            }
                            return false
                        }
                    })
                    ScaleBarOverlay(this).apply {
                        setAlignBottom(true)
                        setAlignRight(true)
                        setScaleBarOffset(24, 204)
                        setCentred(false)
                    }.also {
                        scaleBarOverlay = it
                        overlays.add(it)
                    }
                }
            },
            update = { mapView ->
                var shouldInvalidate = false
                val targetMapOrientation = if (mapOrientationMode == MapOrientationMode.HEADING_UP) -deviceHeadingDeg else 0f
                if (lastAppliedMapOrientation.isNaN() || kotlin.math.abs(lastAppliedMapOrientation - targetMapOrientation) > 0.1f) {
                    mapView.mapOrientation = targetMapOrientation
                    lastAppliedMapOrientation = targetMapOrientation
                    shouldInvalidate = true
                }

                if (drawingModeEnabled) {
                    mapView.setOnTouchListener { _, event ->
                        drawingTouchOverlay.onTouchEvent(event, mapView)
                    }
                } else {
                    mapView.setOnTouchListener(null)
                }

                val currentWmsKey = "${wmsConfig.baseUrl}|${wmsConfig.layers}|${wmsConfig.crs}|${wmsConfig.version}|${wmsConfig.styles}|${wmsConfig.transparent}"
                if (lastAppliedTileMode != mapLayerMode || (mapLayerMode == MapLayerMode.WMS && lastAppliedWmsKey != currentWmsKey)) {
                    when (mapLayerMode) {
                        MapLayerMode.OFFLINE -> {
                            mapView.setUseDataConnection(false)
                            mapView.setTileSource(blankOfflineTileSource)
                            mapView.overlayManager.tilesOverlay?.isEnabled = true
                            mapView.setBackgroundColor(AndroidColor.WHITE)
                            mapView.overlayManager.tilesOverlay?.loadingBackgroundColor = AndroidColor.WHITE
                        }
                        MapLayerMode.OPENTOPO -> {
                            mapView.setUseDataConnection(true)
                            mapView.setTileSource(openTopoTileSource)
                            mapView.overlayManager.tilesOverlay?.isEnabled = true
                            mapView.setBackgroundColor(AndroidColor.WHITE)
                            mapView.overlayManager.tilesOverlay?.loadingBackgroundColor = AndroidColor.WHITE
                        }
                        MapLayerMode.HGSS_SIGURNE_STAZE,
                        MapLayerMode.HGSS_OSM_TEST -> {
                            mapView.setUseDataConnection(true)
                            mapView.setTileSource(hgssOsmTileSource)
                            mapView.overlayManager.tilesOverlay?.isEnabled = true
                            mapView.setBackgroundColor(AndroidColor.WHITE)
                            mapView.overlayManager.tilesOverlay?.loadingBackgroundColor = AndroidColor.WHITE
                        }
                        else -> {
                            mapView.setUseDataConnection(false)
                            mapView.setTileSource(blankOfflineTileSource)
                            mapView.overlayManager.tilesOverlay?.isEnabled = false
                            mapView.setBackgroundColor(AndroidColor.WHITE)
                            mapView.overlayManager.tilesOverlay?.loadingBackgroundColor = AndroidColor.WHITE
                        }
                    }
                    mapView.tileProvider.clearTileCache()
                    lastAppliedTileMode = mapLayerMode
                    lastAppliedWmsKey = currentWmsKey
                    shouldInvalidate = true
                }

                val activeSearchFocus = searchFocusPoints.isNotEmpty()
                val pendingSearchFocus = activeSearchFocus && searchFocusNonce != lastAppliedSearchFocusNonce
                // Search results arrive from the Search tab as an explicit result set.
                // Do not re-plan markers from the whole map dataset while the camera is
                // focused on that result area: large all-database lists can be sampled
                // outside the current search bounds, which centers correctly but leaves
                // the visible map without object markers.
                val markerSourceRecords = if (activeSearchFocus && searchFocusRecordIds.isNotEmpty()) {
                    val searchMatches = records.filter { it.id in searchFocusRecordIds }
                    val withFocus = if (focusRecord != null && focusRecord.id !in searchFocusRecordIds) {
                        searchMatches + focusRecord
                    } else searchMatches
                    withFocus.filterByBoundingBox(activeFieldPackageBounds)
                } else {
                    records.filterByBoundingBox(activeFieldPackageBounds)
                }
                val searchPlanningBox = if (pendingSearchFocus) boundingBoxFor(searchFocusPoints) else null
                val planningBoundingBox = searchPlanningBox ?: mapView.boundingBox
                val planningZoom = if (pendingSearchFocus && searchPlanningBox != null) {
                    if (searchFocusPoints.size == 1) 14.0 else boundsToZoom(
                        GeoPoint(searchPlanningBox.latSouth, searchPlanningBox.lonWest),
                        GeoPoint(searchPlanningBox.latNorth, searchPlanningBox.lonEast)
                    )
                } else {
                    mapView.zoomLevelDouble
                }
                val recordPlanKey = buildString {
                    append(buildRecordPlanKey(markerSourceRecords, planningBoundingBox, planningZoom))
                    if (activeSearchFocus) {
                        append("|searchFocus=").append(searchFocusNonce)
                        append(':').append(searchFocusPoints.size)
                        append(':').append(searchFocusRecordIds.size)
                    }
                }
                val recordRenderPlan = if (recordPlanKey == lastRecordPlanKey && lastRecordRenderPlan != null) {
                    lastRecordRenderPlan!!
                } else {
                    planRecordsForMap(
                        markerSourceRecords,
                        planningBoundingBox,
                        planningZoom,
                        if (activeSearchFocus) MAX_SEARCH_RESULT_MARKERS_ON_MAP else MAX_RECORD_MARKERS_ON_MAP
                    ).also {
                        lastRecordPlanKey = recordPlanKey
                        lastRecordRenderPlan = it
                    }
                }
                val desiredGeologicalOverlayKey = if (geologicalOverlayEnabled && geologicalOverlayOpacityPercent > 0) {
                    WmsTilesOverlay.stableKeyFor(MapLayerPrefs.GEOLOGICAL_UNITS_OVERLAY_CONFIG, geologicalOverlayOpacityPercent)
                } else null

                val overlaySignature = buildString {
                    append(mapLayerMode.name)
                    append("|wms=").append(currentWmsKey)
                    append('|').append(hillshadeEnabled)
                    append(':').append(hillshadeOpacityPercent)
                    append("|geo=").append(geologicalOverlayEnabled)
                    append(':').append(geologicalOverlayOpacityPercent)
                    append('|').append(areaSelectionMode)
                    append('|').append(rulerModeEnabled)
                    append('|').append(buildRecordOverlaySignature(recordRenderPlan.singles))
                    append('|').append(buildClusterSignature(recordRenderPlan.clusters))
                    append("|searchFocus=").append(searchFocusNonce).append(':').append(searchFocusPoints.size).append(':').append(searchFocusRecordIds.size)
                    append("|bbox=").append(activeFieldPackageBounds?.toMapBoundsSignature() ?: "none")
                    append('|').append(liveTrackSignature)
                    append('|').append((focusedSavedTrack?.id ?: ""))
                    append(':').append(focusedTrackSignature)
                    append('|').append(selectedAreaSignature)
                    append('|').append(markedPointsSignature)
                    append('|').append(savedTracksSignature)
                    append('|').append(importedLayersSignature)
                    append('|').append(simplePointViewEnabled)
                    append("|field=").append(fieldVisibilityEnabled)
                    append('|').append(enabledCustomOverlayNamesSignature)
                    append("|geoOverlay=").append(geologicalOverlayEnabled).append(':').append(geologicalOverlayOpacityPercent)
                    append('|').append(rulerStartPoint?.let { "${it.latitude}:${it.longitude}" } ?: "")
                    append('|').append(rulerEndPoint?.let { "${it.latitude}:${it.longitude}" } ?: "")
                    append('|').append(drawingModeEnabled)
                    append('|').append(drawingStrokeWidthDp)
                    append('|').append(drawingSmoothEnabled)
                    append('|').append(buildDrawingPointsSignature(drawingPoints))
                }

                if (lastOverlaySignature != overlaySignature) {
                    mapView.overlays.removeAll { overlay ->
                        when (overlay) {
                            userLocationMarker -> false
                            is MapEventsOverlay, is LocalTileOverlay, is OfflineBoundsOverlay, is Polyline, is DrawingTouchOverlay, is HillshadeTilesOverlay, is WmsBaseTilesOverlay -> true
                            is WmsTilesOverlay -> overlay.stableKey != desiredGeologicalOverlayKey
                            is Marker -> overlay != userLocationMarker
                            else -> false
                        }
                    }

                    if (mapLayerMode == MapLayerMode.OFFLINE && offlineTileRoot.exists()) {
                        mapView.overlays.add(0, LocalTileOverlay(offlineTileRoot))
                    }
                    if (mapLayerMode == MapLayerMode.WMS) {
                        val baseWmsConfig = wmsConfig.copy(transparent = false)
                        WmsPerformanceCache.install(context)
                        mapView.overlays.add(0, WmsBaseTilesOverlay(baseWmsConfig))
                    }
                    val baseMapName = if (mapLayerMode == MapLayerMode.OFFLINE) OfflineTileManager.getActiveMapName(context) else null
                    enabledCustomOverlayNames
                        .filter { it != baseMapName }
                        .forEach { overlayName ->
                            val overlayRoot = OfflineTileManager.tileRootForName(context, overlayName)
                            if (overlayRoot.exists()) {
                                mapView.overlays.add(LocalTileOverlay(overlayRoot))
                            }
                        }


                    if (geologicalOverlayEnabled && geologicalOverlayOpacityPercent > 0) {
                        val hasStableGeologicalOverlay = mapView.overlays.any { overlay ->
                            overlay is WmsTilesOverlay && overlay.stableKey == desiredGeologicalOverlayKey
                        }
                        if (!hasStableGeologicalOverlay) {
                            mapView.overlays.add(
                                WmsTilesOverlay(
                                    MapLayerPrefs.GEOLOGICAL_UNITS_OVERLAY_CONFIG,
                                    geologicalOverlayOpacityPercent
                                )
                            )
                        }
                    }

                    if (hillshadeEnabled && hillshadeOpacityPercent > 0) {
                        mapView.overlays.add(HillshadeTilesOverlay(context, hillshadeOpacityPercent))
                    }


                    recordRenderPlan.singles.forEach { record ->
                        val lat = record.location.lat ?: return@forEach
                        val lon = record.location.lon ?: return@forEach
                        Marker(mapView).apply {
                            position = GeoPoint(lat, lon)
                            title = record.name
                            snippet = listOfNotNull(
                                record.classification.record_status,
                                record.location.municipality
                            ).joinToString(" • ")
                            icon = recordMarkerDrawable(
                                context = context,
                                record = record,
                                sourceFilter = com.darko.speleov1.model.SourceFilter.ALL,
                                fieldVisibilityEnabled = fieldVisibilityEnabled,
                                simplePointViewEnabled = simplePointViewEnabled
                            )
                            setAnchor(Marker.ANCHOR_CENTER, if (fieldVisibilityEnabled || simplePointViewEnabled) Marker.ANCHOR_CENTER else Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { marker, _ ->
                                marker.showInfoWindow()
                                onSelect(record)
                                true
                            }
                        }.also { mapView.overlays.add(it) }
                    }

                    recordRenderPlan.clusters.forEach { cluster ->
                        Marker(mapView).apply {
                            position = cluster.center
                            title = "${cluster.count} objekata"
                            snippet = "Dodirni za približavanje"
                            icon = buildClusterDrawable(context, cluster.count, fieldVisibilityEnabled = fieldVisibilityEnabled)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            setOnMarkerClickListener { _, _ ->
                                mapView.controller.animateTo(cluster.center)
                                mapView.controller.setZoom((mapView.zoomLevelDouble + 1.5).coerceAtMost(18.0))
                                true
                            }
                        }.also { mapView.overlays.add(it) }
                    }

                    buildTrackOverlays(trackPoints, mapView, context, isLiveTracking = true, fieldVisibilityEnabled = fieldVisibilityEnabled).forEach { mapView.overlays.add(it) }
                    savedTracks.filter { it.visible }.forEach { track ->
                        buildTrackOverlays(track.points, mapView, context, fieldVisibilityEnabled = fieldVisibilityEnabled).forEach { mapView.overlays.add(it) }
                    }
                    focusedSavedTrack?.takeIf { focused -> savedTracks.none { it.id == focused.id && it.visible } }?.let { track ->
                        buildTrackOverlays(track.points, mapView, context, fieldVisibilityEnabled = fieldVisibilityEnabled).forEach { mapView.overlays.add(it) }
                    }

                    buildDrawingOverlays(drawingPoints, mapView, drawingStrokeWidthDp = drawingStrokeWidthDp, smooth = drawingSmoothEnabled).forEach { mapView.overlays.add(it) }

                    listOfNotNull(rulerStartPoint, rulerEndPoint).forEachIndexed { index, point ->
                        Marker(mapView).apply {
                            position = point
                            title = if (index == 0) "Točka A" else "Točka B"
                            icon = cachedDrawable(context, R.drawable.marker_ruler_point)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }.also { mapView.overlays.add(it) }
                    }
                    if (rulerStartPoint != null && rulerEndPoint != null) {
                        Polyline().apply {
                            outlinePaint.color = AndroidColor.RED
                            outlinePaint.strokeWidth = 5f
                            setPoints(listOf(rulerStartPoint, rulerEndPoint))
                        }.also { mapView.overlays.add(it) }
                    }

                    selectedAreaPoints.forEachIndexed { index, point ->
                        Marker(mapView).apply {
                            position = point
                            title = "Offline kut ${index + 1}"
                            icon = cachedDrawable(context, R.drawable.marker_offline_corner)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }.also { mapView.overlays.add(it) }
                    }

                    markedPoints.filter { it.visible }.forEach { point ->
                        Marker(mapView).apply {
                            position = GeoPoint(point.lat, point.lon)
                            title = point.name
                            snippet = listOf(point.type, point.description.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" • ")
                            icon = when {
                                fieldVisibilityEnabled -> fieldPointDrawable(AndroidColor.rgb(255, 193, 7))
                                simplePointViewEnabled -> simplePointDrawable(AndroidColor.rgb(255, 193, 7))
                                else -> cachedDrawable(context, R.drawable.marker_yellow)
                            }
                            setAnchor(Marker.ANCHOR_CENTER, if (fieldVisibilityEnabled || simplePointViewEnabled) Marker.ANCHOR_CENTER else Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { marker, _ ->
                                marker.showInfoWindow()
                                onShowMarkedPointActions(point)
                                true
                            }
                        }.also { mapView.overlays.add(it) }
                    }

                    buildImportedOverlays(importedLayers, mapView, context, simplePointViewEnabled, fieldVisibilityEnabled, onShowImportedPointActions).forEach { mapView.overlays.add(it) }

                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                when {
                                    areaSelectionMode -> {
                                        onAreaCornerSelected(p)
                                        mapView.invalidate()
                                        return true
                                    }
                                    drawingModeEnabled -> {
                                        return false
                                    }
                                    rulerModeEnabled -> {
                                        onMapTapForRuler(p)
                                        mapView.invalidate()
                                        return true
                                    }
                                    else -> {
                                        closeAllMarkerInfoWindows(mapView)
                                        mapView.invalidate()
                                        return true
                                    }
                                }
                            }
                            closeAllMarkerInfoWindows(mapView)
                            mapView.invalidate()
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            if (p != null && !areaSelectionMode && !rulerModeEnabled && !drawingModeEnabled) {
                                onLongPressPoint(p)
                                return true
                            }
                            return false
                        }
                    })
                    // Keep empty-map tap handling BELOW interactive markers/waypoints/KML points.
                    // osmdroid dispatches touch events from the topmost overlay first; when this
                    // MapEventsOverlay was added last it swallowed marker taps, so object bubbles
                    // and point action sheets no longer opened. Index 0 lets markers receive taps
                    // first, while empty taps still close open info windows.
                    mapView.overlays.add(0, eventsOverlay)

                    lastOverlaySignature = overlaySignature
                    shouldInvalidate = true
                }

                val shouldShowUserMarker = currentUserLocation != null && (
                    focusedSavedTrack == null ||
                        activeTracking ||
                        focusedSavedTrack.points.lastOrNull()?.point?.let { distanceMeters(currentUserLocation, it) > 15.0 } != false
                    )
                if (shouldShowUserMarker && currentUserLocation != null) {
                    val marker = userLocationMarker ?: Marker(mapView).apply {
                        icon = cachedDrawable(context, R.drawable.marker_location)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setFlat(true)
                    }.also {
                        userLocationMarker = it
                        mapView.overlays.add(it)
                        shouldInvalidate = true
                    }
                    val nextTitle = if (activeTracking) "Moja lokacija • live" else "Moja lokacija"
                    val nextRotation = if (mapOrientationMode == MapOrientationMode.HEADING_UP || mapOrientationMode == MapOrientationMode.STATIC) 0f else ((360f - (deviceHeadingDeg % 360f)) + 360f) % 360f
                    val positionChanged = marker.position?.let {
                        kotlin.math.abs(it.latitude - currentUserLocation.latitude) > 0.00001 || kotlin.math.abs(it.longitude - currentUserLocation.longitude) > 0.00001
                    } ?: true
                    val titleChanged = marker.title != nextTitle
                    val rotationChanged = kotlin.math.abs(marker.rotation - nextRotation) > 0.5f
                    if (positionChanged) marker.position = currentUserLocation
                    if (titleChanged) marker.title = nextTitle
                    if (rotationChanged) marker.rotation = nextRotation
                    if (positionChanged || titleChanged || rotationChanged) shouldInvalidate = true
                } else {
                    userLocationMarker?.let {
                        mapView.overlays.remove(it)
                        userLocationMarker = null
                        shouldInvalidate = true
                    }
                }

                if (!autoCenterOnUserEnabled) {
                    lastAppliedAutoCenterSignature = null
                }

                trackingNonce
                when {
                    autoCenterOnUserEnabled && activeTracking && currentUserLocation != null -> {
                        val autoCenterSignature = buildString {
                            append(currentUserLocation.latitude)
                            append(":")
                            append(currentUserLocation.longitude)
                            append(":")
                            append(activeTracking)
                        }
                        if (autoCenterSignature != lastAppliedAutoCenterSignature) {
                            val currentCenter = GeoPoint(mapView.mapCenter.latitude, mapView.mapCenter.longitude)
                            val centerDistanceM = distanceMeters(currentCenter, currentUserLocation)
                            val shouldFollow = lastAppliedAutoCenterSignature == null || centerDistanceM < 80.0
                            if (shouldFollow) {
                                val targetZoom = if (activeTracking) 16.0 else 14.0
                                if (mapView.zoomLevelDouble < targetZoom) {
                                    mapView.controller.setZoom(targetZoom)
                                }
                                mapView.controller.animateTo(currentUserLocation)
                                shouldInvalidate = true
                            }
                            lastAppliedAutoCenterSignature = autoCenterSignature
                        }
                    }
                    mapLayerMode == MapLayerMode.OFFLINE && offlineFocusPoint != null && offlineFocusNonce != lastAppliedOfflineNonce -> {
                        mapView.controller.setZoom(offlineFocusZoom)
                        mapView.controller.animateTo(offlineFocusPoint)
                        lastAppliedOfflineNonce = offlineFocusNonce
                        shouldInvalidate = true
                    }
                    importedFocusPoint != null && importedFocusNonce != lastAppliedImportedNonce -> {
                        mapView.controller.setZoom(importedFocusZoom)
                        mapView.controller.animateTo(importedFocusPoint)
                        lastAppliedImportedNonce = importedFocusNonce
                        shouldInvalidate = true
                    }
                    searchFocusPoints.isNotEmpty() && searchFocusNonce != lastAppliedSearchFocusNonce -> {
                        // Search focus must be deterministic: one result centers exactly on
                        // that object's coordinates, while multiple results center between
                        // all returned points. Use setCenter instead of animateTo so a cold
                        // map/layer switch cannot leave the camera between the old viewport
                        // and the requested search target.
                        val targetBox = boundingBoxFor(searchFocusPoints)
                        val targetCenter = when {
                            searchFocusPoints.size == 1 -> searchFocusPoints.first()
                            targetBox != null -> GeoPoint(
                                (targetBox.latNorth + targetBox.latSouth) / 2.0,
                                (targetBox.lonEast + targetBox.lonWest) / 2.0
                            )
                            else -> searchFocusPoints.first()
                        }
                        val targetZoom = when {
                            searchFocusPoints.size == 1 -> 15.0
                            targetBox != null -> boundsToZoom(
                                GeoPoint(targetBox.latSouth, targetBox.lonWest),
                                GeoPoint(targetBox.latNorth, targetBox.lonEast)
                            )
                            else -> 13.0
                        }

                        fun applySearchCameraFocus() {
                            mapView.controller.setZoom(targetZoom)
                            mapView.controller.setCenter(targetCenter)
                            mapView.invalidate()
                        }

                        applySearchCameraFocus()
                        mapView.post { applySearchCameraFocus() }
                        // Search focus has just won this frame; block persisted camera from overriding it.
                        restoredPersistedCamera = true
                        lastAppliedSearchFocusNonce = searchFocusNonce
                        shouldInvalidate = true
                    }
                    persistedMapCenter != null && !restoredPersistedCamera && focusRecord == null && focusedSavedTrack == null &&
                        offlineFocusPoint == null && importedFocusPoint == null && userNonce == 0 && !autoCenterOnUserEnabled -> {
                        mapView.controller.setZoom(persistedMapZoom)
                        mapView.controller.animateTo(persistedMapCenter)
                        restoredPersistedCamera = true
                        shouldInvalidate = true
                    }
                    userNonce > 0 && currentUserLocation != null && userNonce != lastAppliedUserNonce -> {
                        val zoom = if (activeTracking) 16.0 else 14.0
                        mapView.controller.setZoom(zoom)
                        mapView.controller.animateTo(currentUserLocation)
                        lastAppliedUserNonce = userNonce
                        shouldInvalidate = true
                    }
                    focusedSavedTrack != null && focusedSavedTrack.points.isNotEmpty() &&
                        (focusedSavedTrackNonce != lastAppliedTrackFocusNonce || focusedSavedTrack.id != lastAppliedTrackId) -> {
                        val displayTrackPoints = trackPointsForDisplay(focusedSavedTrack.points).map { it.point }
                        val box = boundingBoxFor(displayTrackPoints)
                        if (box != null) {
                            val center = GeoPoint((box.latNorth + box.latSouth) / 2.0, (box.lonEast + box.lonWest) / 2.0)
                            val zoom = boundsToZoom(GeoPoint(box.latSouth, box.lonWest), GeoPoint(box.latNorth, box.lonEast))
                            mapView.controller.setZoom(zoom)
                            mapView.controller.animateTo(center)
                        } else {
                            mapView.controller.setZoom(15.0)
                            mapView.controller.animateTo(focusedSavedTrack.points.last().point)
                        }
                        lastAppliedTrackId = focusedSavedTrack.id
                        lastAppliedTrackFocusNonce = focusedSavedTrackNonce
                        shouldInvalidate = true
                    }
                    focusRecord?.location?.lat != null && focusRecord.location.lon != null && focusRecord.id != lastAppliedFocusId -> {
                        val target = GeoPoint(focusRecord.location.lat!!, focusRecord.location.lon!!)
                        fun applyRecordCameraFocus() {
                            mapView.controller.setZoom(15.0)
                            mapView.controller.setCenter(target)
                            mapView.invalidate()
                        }
                        applyRecordCameraFocus()
                        mapView.post { applyRecordCameraFocus() }
                        lastAppliedFocusId = focusRecord.id
                        shouldInvalidate = true
                    }
                }
                if (shouldInvalidate) {
                    mapView.invalidate()
                }
            }
        )
    }

}

@Composable
fun EmptyMapHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Text("Karta je trenutno prazna", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Upiši ime jame, mjesto ili lokaciju na tabu Pretraga. Kad dobiješ rezultate, otvori kartu ili klikni 'Vidi na karti'. Gumb 'Moja lokacija' može te centrirati na GPS.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Dugi klik na kartu = dodaj točku ili postavi navigaciju.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "GPS gumb = gornji desni toolbar → aktivira lokaciju i praćenje.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

internal fun appendTrackPoint(existing: List<TrackPoint>, location: Location): List<TrackPoint> {
    val next = TrackPoint(
        point = GeoPoint(location.latitude, location.longitude),
        altitudeM = location.takeIf { it.hasAltitude() }?.altitude
    )
    val last = existing.lastOrNull()
    if (last != null) {
        val results = FloatArray(1)
        Location.distanceBetween(
            last.point.latitude, last.point.longitude,
            next.point.latitude, next.point.longitude,
            results
        )
        val horizontalM = results.firstOrNull()?.toDouble() ?: 0.0
        if (horizontalM < 2.0) return existing
    }
    val trimmed = (existing + next)
    return if (trimmed.size > 5000) trimmed.takeLast(5000) else trimmed
}

data class TrackOverlayPlan(
    val routePoints: List<GeoPoint>,
    val directionArrows: List<Pair<GeoPoint, Float>>
)

private const val TRACK_OVERLAY_PLAN_CACHE_LIMIT = 96
private val trackOverlayPlanCache = LinkedHashMap<String, TrackOverlayPlan>()

internal fun cachedTrackOverlayPlan(trackPoints: List<TrackPoint>): TrackOverlayPlan? {
    if (trackPoints.size < 2) return null
    val key = buildTrackStateSignature(trackPoints)
    trackOverlayPlanCache[key]?.let { return it }
    val displayPoints = trackPointsForDisplay(trackPoints)
    if (displayPoints.size < 2) return null
    val plan = TrackOverlayPlan(
        routePoints = displayPoints.map { it.point },
        directionArrows = arrowMarkerPoints(displayPoints, minStepMeters = directionArrowSpacingMeters(displayPoints.size))
    )
    if (trackOverlayPlanCache.size >= TRACK_OVERLAY_PLAN_CACHE_LIMIT) {
        val firstKey = trackOverlayPlanCache.keys.firstOrNull()
        if (firstKey != null) trackOverlayPlanCache.remove(firstKey)
    }
    trackOverlayPlanCache[key] = plan
    return plan
}

internal fun buildTrackOverlays(
    trackPoints: List<TrackPoint>,
    mapView: MapView,
    context: Context,
    isLiveTracking: Boolean = false,
    fieldVisibilityEnabled: Boolean = false,
    mainColorOverride: Int? = null
): List<Overlay> {
    val plan = cachedTrackOverlayPlan(trackPoints) ?: return emptyList()
    val routePoints = plan.routePoints
    val overlays = mutableListOf<Overlay>()
    val outlineColor = if (isLiveTracking) AndroidColor.argb(170, 70, 0, 0) else AndroidColor.argb(120, 0, 0, 0)
    val outlineWidth = when {
        fieldVisibilityEnabled && isLiveTracking -> 22f
        fieldVisibilityEnabled -> 17f
        isLiveTracking -> 16f
        else -> 12f
    }
    val mainColor = mainColorOverride ?: if (isLiveTracking) AndroidColor.rgb(255, 36, 64) else AndroidColor.rgb(33, 170, 255)
    val mainWidth = when {
        fieldVisibilityEnabled && isLiveTracking -> 14f
        fieldVisibilityEnabled -> 10f
        isLiveTracking -> 10f
        else -> 7f
    }

    Polyline(mapView).apply {
        setPoints(routePoints)
        outlinePaint.color = outlineColor
        outlinePaint.strokeWidth = outlineWidth
        outlinePaint.isAntiAlias = true
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
    }.also { overlays += it }

    Polyline(mapView).apply {
        setPoints(routePoints)
        outlinePaint.color = mainColor
        outlinePaint.strokeWidth = mainWidth
        outlinePaint.isAntiAlias = true
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
    }.also { overlays += it }

    plan.directionArrows.forEach { (point, bearing) ->
        Marker(mapView).apply {
            position = point
            icon = cachedDrawable(context, R.drawable.marker_track_direction)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            rotation = bearing
            infoWindow = null
            alpha = if (fieldVisibilityEnabled) 1.0f else 0.9f
            title = null
        }.also { overlays += it }
    }

    // During active live tracking we keep the map clean: no fixed start/end dots yet.
    // Start and end markers appear only after the track is stopped/saved/paused.
    if (!isLiveTracking) {
        Marker(mapView).apply {
            position = routePoints.first()
            title = "Start"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = cachedDrawable(context, R.drawable.marker_track_start)
            infoWindow = null
            alpha = 0.96f
        }.also { overlays += it }

        Marker(mapView).apply {
            position = routePoints.last()
            title = "Kraj"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = cachedDrawable(context, R.drawable.marker_track_end)
            infoWindow = null
            alpha = 0.96f
        }.also { overlays += it }
    }

    return overlays
}

internal fun trackPointsForDisplay(trackPoints: List<TrackPoint>): List<TrackPoint> {
    if (trackPoints.size <= 2) return trackPoints
    val minSpacingM = when {
        trackPoints.size >= 2000 -> 18.0
        trackPoints.size >= 1200 -> 14.0
        trackPoints.size >= 600 -> 10.0
        trackPoints.size >= 250 -> 7.0
        else -> 4.0
    }
    val simplified = mutableListOf(trackPoints.first())
    trackPoints.drop(1).forEach { point ->
        val previous = simplified.last()
        if (distanceMeters(previous.point, point.point) >= minSpacingM) {
            simplified += point
        } else if (point == trackPoints.last()) {
            simplified[simplified.lastIndex] = point
        }
    }
    if (simplified.last() != trackPoints.last()) simplified += trackPoints.last()
    return simplified
}

internal fun directionArrowSpacingMeters(pointCount: Int): Double = when {
    pointCount >= 1500 -> 120.0
    pointCount >= 700 -> 90.0
    pointCount >= 250 -> 65.0
    else -> 45.0
}

internal fun arrowMarkerPoints(trackPoints: List<TrackPoint>, minStepMeters: Double): List<Pair<GeoPoint, Float>> {
    if (trackPoints.size < 2) return emptyList()
    val arrows = mutableListOf<Pair<GeoPoint, Float>>()
    var accumulated = 0.0
    trackPoints.zipWithNext().forEach { (a, b) ->
        accumulated += distanceMeters(a.point, b.point)
        if (accumulated >= minStepMeters) {
            arrows += GeoPoint(
                (a.point.latitude + b.point.latitude) / 2.0,
                (a.point.longitude + b.point.longitude) / 2.0
            ) to bearingBetween(a.point, b.point)
            accumulated = 0.0
        }
    }
    return arrows
}

internal fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val results = FloatArray(1)
    Location.distanceBetween(
        a.latitude, a.longitude,
        b.latitude, b.longitude,
        results
    )
    return results.firstOrNull()?.toDouble() ?: 0.0
}

internal fun bearingBetween(a: GeoPoint, b: GeoPoint): Float {
    val startLat = Math.toRadians(a.latitude)
    val startLon = Math.toRadians(a.longitude)
    val endLat = Math.toRadians(b.latitude)
    val endLon = Math.toRadians(b.longitude)
    val dLon = endLon - startLon
    val y = kotlin.math.sin(dLon) * kotlin.math.cos(endLat)
    val x = kotlin.math.cos(startLat) * kotlin.math.sin(endLat) - kotlin.math.sin(startLat) * kotlin.math.cos(endLat) * kotlin.math.cos(dLon)
    return (((Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0)).toFloat()
}



data class TrackStats(
    val distanceM: Double,
    val ascentM: Double,
    val descentM: Double
)

internal fun computeTrackStats(trackPoints: List<TrackPoint>): TrackStats {
    if (trackPoints.size < 2) return TrackStats(0.0, 0.0, 0.0)
    var distanceM = 0.0
    var ascentM = 0.0
    var descentM = 0.0
    val results = FloatArray(1)
    trackPoints.zipWithNext().forEach { (a, b) ->
        Location.distanceBetween(
            a.point.latitude, a.point.longitude,
            b.point.latitude, b.point.longitude,
            results
        )
        distanceM += results.firstOrNull()?.toDouble() ?: 0.0
        val da = (b.altitudeM ?: return@forEach) - (a.altitudeM ?: return@forEach)
        if (da >= 2.0) ascentM += da
        else if (da <= -2.0) descentM += -da
    }
    return TrackStats(distanceM, ascentM, descentM)
}

internal fun Double.format1(): String = String.format(Locale.US, "%.1f", this)

internal fun altitudeColor(value: Double, min: Double, max: Double): Int {
    if (max - min < 0.1) return AndroidColor.rgb(255, 193, 7)
    val t = (((value - min) / (max - min)).coerceIn(0.0, 1.0))
    return when {
        t < 0.25 -> AndroidColor.rgb(46, 125, 50)
        t < 0.5 -> AndroidColor.rgb(251, 192, 45)
        t < 0.75 -> AndroidColor.rgb(251, 140, 0)
        else -> AndroidColor.rgb(198, 40, 40)
    }
}

data class NavigationInfo(
    val relativeBearingDeg: Float,
    val distanceM: Double,
    val altitudeDiffM: Double?
)

internal fun buildNavigationInfo(
    currentUserLocation: GeoPoint?,
    currentLocationAltitudeM: Double?,
    deviceHeadingDeg: Float,
    target: NavigationTarget
): NavigationInfo {
    if (currentUserLocation == null) return NavigationInfo(0f, 0.0, null)
    val results = FloatArray(3)
    Location.distanceBetween(
        currentUserLocation.latitude,
        currentUserLocation.longitude,
        target.point.latitude,
        target.point.longitude,
        results
    )
    val bearingToTarget = results.getOrNull(1)?.toFloat() ?: 0f
    val relative = (((bearingToTarget - deviceHeadingDeg) % 360f) + 360f) % 360f
    val altitudeDiff = if (currentLocationAltitudeM != null && target.targetAltitudeM != null) {
        target.targetAltitudeM - currentLocationAltitudeM
    } else null
    return NavigationInfo(relativeBearingDeg = relative, distanceM = results[0].toDouble(), altitudeDiffM = altitudeDiff)
}

internal fun formatDuration(startedAtMillis: Long?, endedAtMillis: Long?): String {
    if (startedAtMillis == null || endedAtMillis == null || endedAtMillis <= startedAtMillis) return "—"
    val totalSeconds = ((endedAtMillis - startedAtMillis) / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> String.format(Locale.US, "%dh %02dmin", hours, minutes)
        minutes > 0 -> String.format(Locale.US, "%dmin %02ds", minutes, seconds)
        else -> String.format(Locale.US, "%ds", seconds)
    }
}

internal fun formatDistance(distanceM: Double): String = when {
    distanceM >= 1000 -> String.format(Locale.US, "%.2f km", distanceM / 1000.0)
    else -> String.format(Locale.US, "%.0f m", distanceM)
}

internal fun formatAltitudeDiff(altitudeDiffM: Double?): String = when {
    altitudeDiffM == null -> "—"
    altitudeDiffM >= 0 -> "+" + String.format(Locale.US, "%.0f m", altitudeDiffM)
    else -> String.format(Locale.US, "%.0f m", altitudeDiffM)
}

internal fun importedBounds(layer: ImportedLayer): Pair<GeoPoint, GeoPoint>? {
    val allPoints = layer.points.map { GeoPoint(it.lat, it.lon) } + layer.tracks.flatMap { it.points.map { tp -> tp.point } }
    if (allPoints.isEmpty()) return null
    val minLat = allPoints.minOf { it.latitude }
    val maxLat = allPoints.maxOf { it.latitude }
    val minLon = allPoints.minOf { it.longitude }
    val maxLon = allPoints.maxOf { it.longitude }
    return GeoPoint(minLat, minLon) to GeoPoint(maxLat, maxLon)
}

internal fun boundsToZoom(min: GeoPoint, max: GeoPoint): Double {
    val span = maxOf(kotlin.math.abs(max.latitude - min.latitude), kotlin.math.abs(max.longitude - min.longitude))
    return when {
        span <= 0.005 -> 17.0
        span <= 0.01 -> 16.0
        span <= 0.03 -> 15.0
        span <= 0.08 -> 14.0
        span <= 0.2 -> 13.0
        span <= 0.5 -> 12.0
        span <= 1.0 -> 11.0
        span <= 2.0 -> 10.0
        span <= 4.0 -> 8.5
        span <= 7.0 -> 7.2
        span <= 12.0 -> 6.3
        else -> 5.5
    }
}


internal val DISTANCE_FILTER_OPTIONS: List<Pair<Int?, String>> = listOf(
    null to "Sve udaljenosti",
    5 to "Do 5 km",
    25 to "Do 25 km",
    50 to "Do 50 km",
    100 to "Do 100 km"
)

private fun List<SpeleoRecord>.filterByBoundingBox(bounds: BoundingBoxFilter?): List<SpeleoRecord> {
    if (bounds == null) return this
    return filter { record ->
        val lat = record.location.lat ?: return@filter false
        val lon = record.location.lon ?: return@filter false
        lat >= bounds.minLat && lat <= bounds.maxLat && lon >= bounds.minLon && lon <= bounds.maxLon
    }
}

private fun BoundingBoxFilter.toMapBoundsSignature(): String =
    "${(minLat * 100000.0).roundToInt()}:${(maxLat * 100000.0).roundToInt()}:${(minLon * 100000.0).roundToInt()}:${(maxLon * 100000.0).roundToInt()}"

@Composable
private fun FieldPackageBoundsBanner(
    modifier: Modifier = Modifier,
    label: String,
    onClear: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFFFA726).copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📍 Izlet: $label",
                color = Color(0xFF201309),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            IconButton(onClick = onClear, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Ukloni filter izleta", tint = Color(0xFF201309))
            }
        }
    }
}

internal const val MAX_VISIBLE_SEARCH_RESULTS = 180

internal const val MAX_IMPORTED_POINTS_ON_MAP = 1500
internal const val MAX_RECORD_MARKERS_ON_MAP = 240
internal const val MAX_SEARCH_RESULT_MARKERS_ON_MAP = 240

internal fun buildSearchRecordPlanKey(records: List<SpeleoRecord>, searchFocusPoints: List<GeoPoint>, searchFocusNonce: Int): String {
    var hash = 41
    records.forEach { record ->
        hash = 31 * hash + record.id.hashCode()
        hash = 31 * hash + ((record.location.lat ?: 0.0) * 100000.0).roundToInt()
        hash = 31 * hash + ((record.location.lon ?: 0.0) * 100000.0).roundToInt()
    }
    searchFocusPoints.forEach { point ->
        hash = 31 * hash + (point.latitude * 100000.0).roundToInt()
        hash = 31 * hash + (point.longitude * 100000.0).roundToInt()
    }
    return "search|$searchFocusNonce|${records.size}|${searchFocusPoints.size}|$hash"
}

internal fun planSearchRecordsForMap(
    records: List<SpeleoRecord>,
    maxPoints: Int = MAX_SEARCH_RESULT_MARKERS_ON_MAP
): RecordRenderPlan {
    val withCoords = records.filter { it.location.lat != null && it.location.lon != null }
    if (withCoords.isEmpty()) return RecordRenderPlan(emptyList(), emptyList())
    return RecordRenderPlan(
        singles = if (withCoords.size <= maxPoints) withCoords else sampleRecordsByGrid(withCoords, maxPoints),
        clusters = emptyList()
    )
}

internal fun buildRecordPlanKey(records: List<SpeleoRecord>, boundingBox: BoundingBox?, zoomLevel: Double): String {
    val box = boundingBox
    val zoomBucket = (zoomLevel * 2.0).roundToInt()
    if (box == null) return "${System.identityHashCode(records)}|${records.size}|$zoomBucket|none"
    return buildString {
        append(System.identityHashCode(records))
        append('|').append(records.size)
        append('|').append(zoomBucket)
        val cameraBucket = when {
            zoomLevel < 10.5 -> 600.0
            zoomLevel < 12.5 -> 1200.0
            else -> 2500.0
        }
        append('|').append((box.latNorth * cameraBucket).roundToInt())
        append('|').append((box.latSouth * cameraBucket).roundToInt())
        append('|').append((box.lonEast * cameraBucket).roundToInt())
        append('|').append((box.lonWest * cameraBucket).roundToInt())
    }
}

internal fun buildCameraStateKey(latitude: Double, longitude: Double, zoomLevel: Double): String = buildString {
    append((latitude * 10000).roundToInt())
    append('|').append((longitude * 10000).roundToInt())
    append('|').append((zoomLevel * 20.0).roundToInt())
}

internal fun sampleRecordsForViewport(
    records: List<SpeleoRecord>,
    boundingBox: BoundingBox?,
    maxPoints: Int = MAX_RECORD_MARKERS_ON_MAP
): List<SpeleoRecord> {
    val withCoords = records.filter { it.location.lat != null && it.location.lon != null }
    if (withCoords.size <= maxPoints) return withCoords
    val box = boundingBox ?: return sampleRecordsByGrid(withCoords, maxPoints)
    val latMargin = (box.latNorth - box.latSouth).coerceAtLeast(0.02) * 0.18
    val lonMargin = (box.lonEast - box.lonWest).coerceAtLeast(0.02) * 0.18
    val north = box.latNorth + latMargin
    val south = box.latSouth - latMargin
    val west = box.lonWest - lonMargin
    val east = box.lonEast + lonMargin
    val inView = withCoords.filter { record ->
        val lat = record.location.lat ?: return@filter false
        val lon = record.location.lon ?: return@filter false
        lat in south..north && lon in west..east
    }
    val base = when {
        inView.isEmpty() -> withCoords
        inView.size > maxPoints -> inView
        else -> inView + withCoords.asSequence().filter { candidate ->
            inView.none { it.id == candidate.id }
        }.take(maxPoints - inView.size).toList()
    }
    return sampleRecordsByGrid(base, maxPoints)
}

internal fun sampleRecordsByGrid(records: List<SpeleoRecord>, maxPoints: Int): List<SpeleoRecord> {
    if (records.size <= maxPoints) return records
    val minLat = records.minOf { it.location.lat ?: 0.0 }
    val maxLat = records.maxOf { it.location.lat ?: 0.0 }
    val minLon = records.minOf { it.location.lon ?: 0.0 }
    val maxLon = records.maxOf { it.location.lon ?: 0.0 }
    val latSpan = (maxLat - minLat).takeIf { it > 0.0 } ?: 1.0
    val lonSpan = (maxLon - minLon).takeIf { it > 0.0 } ?: 1.0
    val gridSize = kotlin.math.ceil(kotlin.math.sqrt(maxPoints.toDouble())).toInt().coerceAtLeast(2)
    val chosen = LinkedHashMap<String, SpeleoRecord>()
    records.forEach { record ->
        val lat = record.location.lat ?: return@forEach
        val lon = record.location.lon ?: return@forEach
        val row = (((lat - minLat) / latSpan) * (gridSize - 1)).toInt().coerceIn(0, gridSize - 1)
        val col = (((lon - minLon) / lonSpan) * (gridSize - 1)).toInt().coerceIn(0, gridSize - 1)
        val key = "$row:$col"
        if (!chosen.containsKey(key)) chosen[key] = record
    }
    if (chosen.size < maxPoints) {
        records.forEach { record ->
            if (chosen.size >= maxPoints) return@forEach
            chosen.putIfAbsent("extra:${record.id}", record)
        }
    }
    return chosen.values.take(maxPoints)
}

internal fun recordsForViewportWindow(records: List<SpeleoRecord>, boundingBox: BoundingBox?): List<SpeleoRecord> {
    val withCoords = records.filter { it.location.lat != null && it.location.lon != null }
    if (boundingBox == null || withCoords.isEmpty() || withCoords.size <= MAX_RECORD_MARKERS_ON_MAP) return withCoords
    val latMargin = (boundingBox.latNorth - boundingBox.latSouth).coerceAtLeast(0.02) * 0.18
    val lonMargin = (boundingBox.lonEast - boundingBox.lonWest).coerceAtLeast(0.02) * 0.18
    val north = boundingBox.latNorth + latMargin
    val south = boundingBox.latSouth - latMargin
    val west = boundingBox.lonWest - lonMargin
    val east = boundingBox.lonEast + lonMargin
    val inView = withCoords.filter { record ->
        val lat = record.location.lat ?: return@filter false
        val lon = record.location.lon ?: return@filter false
        lat in south..north && lon in west..east
    }
    return if (inView.isNotEmpty()) inView else withCoords
}

internal fun planRecordsForMap(
    records: List<SpeleoRecord>,
    boundingBox: BoundingBox?,
    zoomLevel: Double,
    maxPoints: Int = MAX_RECORD_MARKERS_ON_MAP
): RecordRenderPlan {
    val viewportRecords = recordsForViewportWindow(records, boundingBox)
    if (viewportRecords.isEmpty()) return RecordRenderPlan(emptyList(), emptyList())
    if (zoomLevel >= 13.4) {
        return RecordRenderPlan(
            singles = sampleRecordsByGrid(viewportRecords, maxPoints),
            clusters = emptyList()
        )
    }

    val box = boundingBox
    if (box == null) {
        return RecordRenderPlan(
            singles = sampleRecordsByGrid(viewportRecords, maxPoints),
            clusters = emptyList()
        )
    }

    val columns = when {
        zoomLevel < 8.0 -> 6
        zoomLevel < 9.5 -> 7
        zoomLevel < 11.0 -> 8
        zoomLevel < 12.5 -> 9
        else -> 10
    }
    val rows = when {
        zoomLevel < 8.0 -> 5
        zoomLevel < 9.5 -> 6
        zoomLevel < 11.0 -> 7
        zoomLevel < 12.5 -> 8
        else -> 9
    }
    val latSpan = (box.latNorth - box.latSouth).coerceAtLeast(0.05)
    val lonSpan = (box.lonEast - box.lonWest).coerceAtLeast(0.05)
    val buckets = LinkedHashMap<String, MutableList<SpeleoRecord>>()
    viewportRecords.forEach { record ->
        val lat = record.location.lat ?: return@forEach
        val lon = record.location.lon ?: return@forEach
        val row = (((box.latNorth - lat) / latSpan) * rows).toInt().coerceIn(0, rows - 1)
        val col = (((lon - box.lonWest) / lonSpan) * columns).toInt().coerceIn(0, columns - 1)
        val key = "$row:$col"
        buckets.getOrPut(key) { mutableListOf() }.add(record)
    }

    val singles = ArrayList<SpeleoRecord>()
    val clusters = ArrayList<RecordCluster>()
    buckets.values.forEach { bucket ->
        if (bucket.size <= 1) {
            singles += bucket.first()
        } else {
            val avgLat = bucket.mapNotNull { it.location.lat }.average()
            val avgLon = bucket.mapNotNull { it.location.lon }.average()
            clusters += RecordCluster(
                center = GeoPoint(avgLat, avgLon),
                count = bucket.size,
                representative = bucket.first()
            )
        }
    }

    val limitedSingles = if (singles.size > maxPoints) sampleRecordsByGrid(singles, maxPoints) else singles
    return RecordRenderPlan(singles = limitedSingles, clusters = clusters)
}

internal fun buildClusterSignature(clusters: List<RecordCluster>): String {
    var hash = 37
    clusters.forEach { cluster ->
        hash = 31 * hash + cluster.count
        hash = 31 * hash + (cluster.center.latitude * 10000).roundToInt()
        hash = 31 * hash + (cluster.center.longitude * 10000).roundToInt()
    }
    return "${clusters.size}:$hash"
}

private val clusterDrawableStateCache = mutableMapOf<String, Drawable.ConstantState?>()

internal fun buildClusterDrawable(context: Context, count: Int, fieldVisibilityEnabled: Boolean = false): Drawable {
    val cacheKey = count.toString() + if (fieldVisibilityEnabled) ":field" else ":normal"
    val cachedState = clusterDrawableStateCache[cacheKey]
    if (cachedState != null) {
        return cachedState.newDrawable(context.resources).mutate()
    }

    val sizePx = if (fieldVisibilityEnabled) 74 else 58
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(226, 58, 58, 66)
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(220, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textAlign = Paint.Align.CENTER
        textSize = if (fieldVisibilityEnabled) 27f else 22f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    val radius = sizePx / 2f - 4f
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, fillPaint)
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, strokePaint)
    val label = when {
        count >= 1000 -> "${count / 1000}k+"
        count >= 100 -> count.toString()
        else -> count.toString()
    }
    val textBounds = Rect()
    textPaint.getTextBounds(label, 0, label.length, textBounds)
    val textY = sizePx / 2f - textBounds.exactCenterY()
    canvas.drawText(label, sizePx / 2f, textY, textPaint)
    val created = BitmapDrawable(context.resources, bitmap)
    clusterDrawableStateCache[cacheKey] = created.constantState
    return created.constantState?.newDrawable(context.resources)?.mutate() ?: created
}

internal fun buildRecordOverlaySignature(records: List<SpeleoRecord>): String {
    var hash = 17
    records.forEach { record ->
        hash = 31 * hash + record.id.hashCode()
        hash = 31 * hash + (record.location.lat?.times(1000)?.roundToInt() ?: 0)
        hash = 31 * hash + (record.location.lon?.times(1000)?.roundToInt() ?: 0)
        hash = 31 * hash + (record.classification.record_status?.hashCode() ?: 0)
    }
    return "${records.size}:$hash"
}

internal fun buildTrackStateSignature(points: List<TrackPoint>): String {
    if (points.isEmpty()) return "0"
    val first = points.first().point
    val last = points.last().point
    return buildString {
        append(points.size)
        append(':').append(String.format(Locale.US, "%.5f", first.latitude))
        append(':').append(String.format(Locale.US, "%.5f", first.longitude))
        append(':').append(String.format(Locale.US, "%.5f", last.latitude))
        append(':').append(String.format(Locale.US, "%.5f", last.longitude))
        append(':').append(points.last().altitudeM?.roundToInt() ?: 0)
    }
}

internal fun buildGeoPointsSignature(points: List<GeoPoint>): String {
    if (points.isEmpty()) return "0"
    var hash = 13
    points.forEach { point ->
        hash = 31 * hash + (point.latitude * 10000).roundToInt()
        hash = 31 * hash + (point.longitude * 10000).roundToInt()
    }
    return "${points.size}:$hash"
}

internal fun buildMarkedPointsSignature(points: List<MarkedPoint>): String {
    var hash = 19
    var visibleCount = 0
    points.forEach { point ->
        if (point.visible) visibleCount += 1
        hash = 31 * hash + point.id.hashCode()
        hash = 31 * hash + if (point.visible) 1 else 0
        hash = 31 * hash + (point.lat * 10000).roundToInt()
        hash = 31 * hash + (point.lon * 10000).roundToInt()
    }
    return "${points.size}:$visibleCount:$hash"
}

internal fun buildSavedTracksSignature(tracks: List<SavedTrack>): String {
    var hash = 23
    tracks.forEach { track ->
        hash = 31 * hash + track.id.hashCode()
        hash = 31 * hash + if (track.visible) 1 else 0
        hash = 31 * hash + track.points.size
        val last = track.points.lastOrNull()?.point
        if (last != null) {
            hash = 31 * hash + (last.latitude * 10000).roundToInt()
            hash = 31 * hash + (last.longitude * 10000).roundToInt()
        }
    }
    return "${tracks.size}:$hash"
}

internal fun buildImportedLayersSignature(layers: List<ImportedLayer>): String {
    var hash = 29
    layers.forEach { layer ->
        hash = 31 * hash + layer.id.hashCode()
        hash = 31 * hash + if (layer.visible) 1 else 0
        hash = 31 * hash + layer.points.size
        hash = 31 * hash + layer.tracks.size
    }
    return "${layers.size}:$hash"
}

private const val IMPORTED_POINT_SAMPLE_CACHE_LIMIT = 96
private val importedPointSampleCache = LinkedHashMap<String, List<MarkedPoint>>()

internal fun buildBoundingBoxSignature(boundingBox: BoundingBox?): String {
    val box = boundingBox ?: return "none"
    return buildString {
        append((box.latNorth * 1000).roundToInt())
        append('|').append((box.latSouth * 1000).roundToInt())
        append('|').append((box.lonEast * 1000).roundToInt())
        append('|').append((box.lonWest * 1000).roundToInt())
    }
}

internal fun buildImportedPointSampleCacheKey(points: List<MarkedPoint>, boundingBox: BoundingBox?, maxPoints: Int): String {
    val firstId = points.firstOrNull()?.id ?: "0"
    val lastId = points.lastOrNull()?.id ?: "0"
    return buildString {
        append(System.identityHashCode(points))
        append('|').append(points.size)
        append('|').append(firstId)
        append('|').append(lastId)
        append('|').append(maxPoints)
        append('|').append(buildBoundingBoxSignature(boundingBox))
    }
}

internal fun sampleImportedPointsForMap(points: List<MarkedPoint>, boundingBox: BoundingBox?, maxPoints: Int = MAX_IMPORTED_POINTS_ON_MAP): List<MarkedPoint> {
    if (points.size <= maxPoints) return points
    if (maxPoints <= 1) return listOf(points.first())

    val cacheKey = buildImportedPointSampleCacheKey(points, boundingBox, maxPoints)
    importedPointSampleCache[cacheKey]?.let { return it }

    val base = boundingBox?.let { box ->
        val latMargin = (box.latNorth - box.latSouth).coerceAtLeast(0.02) * 0.18
        val lonMargin = (box.lonEast - box.lonWest).coerceAtLeast(0.02) * 0.18
        val north = box.latNorth + latMargin
        val south = box.latSouth - latMargin
        val west = box.lonWest - lonMargin
        val east = box.lonEast + lonMargin
        val inView = points.filter { point -> point.lat in south..north && point.lon in west..east }
        if (inView.isNotEmpty()) inView else points
    } ?: points

    val minLat = base.minOf { it.lat }
    val maxLat = base.maxOf { it.lat }
    val minLon = base.minOf { it.lon }
    val maxLon = base.maxOf { it.lon }
    val latSpan = (maxLat - minLat).takeIf { it > 0.0 } ?: 1.0
    val lonSpan = (maxLon - minLon).takeIf { it > 0.0 } ?: 1.0

    val gridSize = kotlin.math.ceil(kotlin.math.sqrt(maxPoints.toDouble())).toInt().coerceAtLeast(2)
    val chosen = LinkedHashMap<String, MarkedPoint>()

    base.forEach { point ->
        val row = (((point.lat - minLat) / latSpan) * (gridSize - 1)).toInt().coerceIn(0, gridSize - 1)
        val col = (((point.lon - minLon) / lonSpan) * (gridSize - 1)).toInt().coerceIn(0, gridSize - 1)
        val key = "$row:$col"
        if (!chosen.containsKey(key)) chosen[key] = point
    }

    if (chosen.size < maxPoints) {
        base.forEach { point ->
            if (chosen.size >= maxPoints) return@forEach
            if (!chosen.values.any { it.id == point.id }) chosen["extra:${point.id}"] = point
        }
    }

    val result = chosen.values.take(maxPoints)
    if (importedPointSampleCache.size >= IMPORTED_POINT_SAMPLE_CACHE_LIMIT) {
        val firstKey = importedPointSampleCache.keys.firstOrNull()
        if (firstKey != null) importedPointSampleCache.remove(firstKey)
    }
    importedPointSampleCache[cacheKey] = result
    return result
}

internal fun closeAllMarkerInfoWindows(mapView: MapView) {
    mapView.overlays.forEach { overlay ->
        if (overlay is Marker && overlay.isInfoWindowShown) {
            overlay.closeInfoWindow()
        }
    }
}

internal fun buildImportedOverlays(importedLayers: List<ImportedLayer>, mapView: MapView, context: Context, simplePointViewEnabled: Boolean, fieldVisibilityEnabled: Boolean, onShowImportedPointActions: (MarkedPoint) -> Unit): List<Overlay> {
    val overlays = mutableListOf<Overlay>()
    importedLayers.filter { it.visible }.forEach { layer ->
        val isTopoDroidLayer = layer.type.equals("topodroid", ignoreCase = true)
        if (isTopoDroidLayer) {
            overlays += buildTopoDroidImportedOverlays(layer, mapView, context, fieldVisibilityEnabled, onShowImportedPointActions)
            return@forEach
        }

        val isSharedLayer = layer.type.equals("Zajednički sloj", ignoreCase = true)
        layer.tracks.forEach { track ->
            overlays += buildTrackOverlays(
                track.points,
                mapView,
                context,
                fieldVisibilityEnabled = fieldVisibilityEnabled,
                mainColorOverride = if (isSharedLayer) AndroidColor.rgb(0, 150, 136) else null
            )
        }
        val visiblePoints = sampleImportedPointsForMap(layer.points, mapView.boundingBox)
        visiblePoints.forEach { point ->
            Marker(mapView).apply {
                position = GeoPoint(point.lat, point.lon)
                title = point.name
                snippet = buildString {
                    append(layer.type).append(" import")
                    if (layer.points.size > visiblePoints.size) append(" • prikazano ").append(visiblePoints.size).append('/').append(layer.points.size)
                }
                val isSharedPoint = isSharedLayer || point.type.equals("shared", ignoreCase = true)
                val markerColor = if (isSharedPoint) AndroidColor.rgb(0, 150, 136) else AndroidColor.rgb(156, 39, 176)
                icon = when {
                    fieldVisibilityEnabled -> fieldPointDrawable(markerColor)
                    simplePointViewEnabled -> simplePointDrawable(markerColor)
                    isSharedPoint -> cachedDrawable(context, R.drawable.marker_shared_diamond)
                    else -> cachedDrawable(context, R.drawable.marker_purple)
                }
                setAnchor(Marker.ANCHOR_CENTER, if (fieldVisibilityEnabled || simplePointViewEnabled) Marker.ANCHOR_CENTER else Marker.ANCHOR_BOTTOM)

                setOnMarkerClickListener { marker, _ ->
                    marker.showInfoWindow()
                    onShowImportedPointActions(point)
                    true
                }
            }.also { overlays += it }
        }
    }
    return overlays
}


internal fun buildTopoDroidImportedOverlays(
    layer: ImportedLayer,
    mapView: MapView,
    context: Context,
    fieldVisibilityEnabled: Boolean,
    onShowImportedPointActions: (MarkedPoint) -> Unit
): List<Overlay> {
    val overlays = mutableListOf<Overlay>()
    val corridorWidth = if (fieldVisibilityEnabled) 20f else 15f
    val centerWidth = if (fieldVisibilityEnabled) 8f else 5.5f
    val corridorColor = AndroidColor.argb(72, 17, 78, 150)
    val centerColor = AndroidColor.rgb(65, 225, 240)

    layer.tracks.forEach { track ->
        val routePoints = track.points.map { it.point }
        if (routePoints.size < 2) return@forEach

        Polyline(mapView).apply {
            setPoints(routePoints)
            outlinePaint.color = corridorColor
            outlinePaint.strokeWidth = corridorWidth
            outlinePaint.isAntiAlias = true
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
        }.also { overlays += it }

        Polyline(mapView).apply {
            setPoints(routePoints)
            outlinePaint.color = centerColor
            outlinePaint.strokeWidth = centerWidth
            outlinePaint.isAntiAlias = true
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
        }.also { overlays += it }
    }

    val visibleStations = sampleImportedPointsForMap(layer.points, mapView.boundingBox).take(if (fieldVisibilityEnabled) 140 else 90)
    visibleStations.forEachIndexed { index, point ->
        Marker(mapView).apply {
            position = GeoPoint(point.lat, point.lon)
            title = if (index == 0) "Ulaz / origin" else point.name
            snippet = "TopoDroid station • orijentacijski prikaz"
            icon = if (fieldVisibilityEnabled) {
                fieldPointDrawable(if (index == 0) AndroidColor.rgb(255, 214, 92) else AndroidColor.rgb(65, 225, 240))
            } else {
                simplePointDrawable(if (index == 0) AndroidColor.rgb(255, 214, 92) else AndroidColor.rgb(65, 225, 240))
            }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            alpha = if (index == 0) 1.0f else 0.82f
            setOnMarkerClickListener { marker, _ ->
                marker.showInfoWindow()
                onShowImportedPointActions(point)
                true
            }
        }.also { overlays += it }
    }

    return overlays
}



internal class DrawingTouchOverlay(
    private val onAppendPoint: (GeoPoint) -> Unit
) : Overlay() {
    private var drawingActive = false
    private var lastX = Float.NaN
    private var lastY = Float.NaN
    private val minDistancePx = 8f

    override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
        // Pen mode rule:
        // - 1 finger/stylus = draw
        // - 2+ fingers = let osmdroid handle map pan/zoom/rotate gestures
        // This keeps drawing mode active without locking the user out of map movement.
        if (event.pointerCount > 1 || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            drawingActive = false
            lastX = Float.NaN
            lastY = Float.NaN
            return false
        }

        fun appendPointFromEvent(e: MotionEvent, force: Boolean = false) {
            val x = e.x
            val y = e.y
            val dx = x - lastX
            val dy = y - lastY
            val farEnough = force || lastX.isNaN() || (dx * dx + dy * dy) >= (minDistancePx * minDistancePx)
            if (!farEnough) return
            val projection = mapView.projection ?: return
            val point = projection.fromPixels(x.toInt(), y.toInt()) as? GeoPoint ?: return
            onAppendPoint(point)
            lastX = x
            lastY = y
            mapView.invalidate()
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                drawingActive = true
                lastX = Float.NaN
                lastY = Float.NaN
                appendPointFromEvent(event, force = true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!drawingActive) return false
                val historySize = event.historySize
                for (i in 0 until historySize) {
                    val histX = event.getHistoricalX(i)
                    val histY = event.getHistoricalY(i)
                    val dx = histX - lastX
                    val dy = histY - lastY
                    val farEnough = lastX.isNaN() || (dx * dx + dy * dy) >= (minDistancePx * minDistancePx)
                    if (farEnough) {
                        val projection = mapView.projection ?: continue
                        val point = projection.fromPixels(histX.toInt(), histY.toInt()) as? GeoPoint ?: continue
                        onAppendPoint(point)
                        lastX = histX
                        lastY = histY
                    }
                }
                appendPointFromEvent(event)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (drawingActive) appendPointFromEvent(event)
                drawingActive = false
                lastX = Float.NaN
                lastY = Float.NaN
                return true
            }
        }
        return false
    }
}

internal fun buildDrawingPointsSignature(points: List<GeoPoint>): String {
    if (points.isEmpty()) return "0"
    val first = points.first()
    val last = points.last()
    return buildString {
        append(points.size)
        append(':').append(String.format(Locale.US, "%.5f", first.latitude))
        append(',').append(String.format(Locale.US, "%.5f", first.longitude))
        append(':').append(String.format(Locale.US, "%.5f", last.latitude))
        append(',').append(String.format(Locale.US, "%.5f", last.longitude))
    }
}

internal fun buildDrawingOverlays(
    points: List<GeoPoint>,
    mapView: MapView,
    drawingStrokeWidthDp: Int = 7,
    smooth: Boolean = true
): List<Overlay> {
    if (points.isEmpty()) return emptyList()
    val overlays = mutableListOf<Overlay>()
    val density = mapView.context.resources.displayMetrics.density
    val displayPoints = drawingDisplayPoints(points, smooth)
    if (displayPoints.size >= 2) {
        overlays += Polyline().apply {
            outlinePaint.color = AndroidColor.argb(120, 0, 0, 0)
            outlinePaint.strokeWidth = drawingStrokeWidthDp * density + 4f
            outlinePaint.isAntiAlias = true
            setPoints(displayPoints)
        }
        overlays += Polyline().apply {
            outlinePaint.color = AndroidColor.rgb(255, 171, 64)
            outlinePaint.strokeWidth = drawingStrokeWidthDp * density
            outlinePaint.isAntiAlias = true
            setPoints(displayPoints)
        }
    }
    val firstPoint = displayPoints.firstOrNull()
    val lastPoint = displayPoints.lastOrNull()
    if (firstPoint != null) {
        overlays += Marker(mapView).apply {
            position = firstPoint
            title = if (displayPoints.size >= 2) "Početak crteža" else "Točka crteža"
            icon = simplePointDrawable(AndroidColor.rgb(255, 213, 79), coreSizePx = 16, haloSizePx = 24, strokePx = 2)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
    }
    if (lastPoint != null && (firstPoint == null || distanceMeters(firstPoint, lastPoint) > 0.5)) {
        overlays += Marker(mapView).apply {
            position = lastPoint
            title = "Zadnja točka crteža"
            icon = simplePointDrawable(AndroidColor.rgb(255, 138, 101), coreSizePx = 18, haloSizePx = 26, strokePx = 2)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
    }
    return overlays
}

internal fun drawingDisplayPoints(points: List<GeoPoint>, smooth: Boolean): List<GeoPoint> {
    if (!smooth || points.size < 3) return points
    return smoothDrawingPoints(points, iterations = 1)
}

internal fun smoothDrawingPoints(points: List<GeoPoint>, iterations: Int = 1): List<GeoPoint> {
    var current = points
    repeat(iterations.coerceAtLeast(1)) {
        if (current.size < 3) return current
        val next = mutableListOf<GeoPoint>()
        next += current.first()
        for (i in 0 until current.lastIndex) {
            val p0 = current[i]
            val p1 = current[i + 1]
            val q = GeoPoint(0.75 * p0.latitude + 0.25 * p1.latitude, 0.75 * p0.longitude + 0.25 * p1.longitude)
            val r = GeoPoint(0.25 * p0.latitude + 0.75 * p1.latitude, 0.25 * p0.longitude + 0.75 * p1.longitude)
            next += q
            next += r
        }
        next += current.last()
        current = next
    }
    return current
}
