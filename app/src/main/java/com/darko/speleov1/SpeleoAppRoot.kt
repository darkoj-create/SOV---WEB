@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.darko.speleov1.model.BoundingBoxFilter
import com.darko.speleov1.model.CadastreFilter
import com.darko.speleov1.model.CaveTypeFilter
import com.darko.speleov1.model.FilterState
import com.darko.speleov1.model.SourceFilter
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
import com.darko.speleov1.util.OfflineZapisnikStore
import com.darko.speleov1.util.OfflineZapisnikTarget
import com.darko.speleov1.util.createOfflineZapisnikTarget
import com.darko.speleov1.util.buildOfflineZapisnikShareText
import com.darko.speleov1.util.AppSessionStore
import com.darko.speleov1.util.SearchPreset
import com.darko.speleov1.util.SharedLayersSyncClient
import com.darko.speleov1.util.SharedLayerEntry
import com.darko.speleov1.util.AppUpdateInfo
import com.darko.speleov1.util.AppUpdateManager
import com.darko.speleov1.util.UpdateCheckResult
import com.darko.speleov1.util.AppSessionSnapshot
import com.darko.speleov1.util.BatteryOptimizationPrefs
import com.darko.speleov1.util.TrackingNotificationHelper
import com.darko.speleov1.util.TrackingForegroundService
import com.darko.speleov1.util.TrackingRuntime
import com.darko.speleov1.util.SovPermissionsStore
import com.darko.speleov1.util.SovRoleSyncManager
import com.darko.speleov1.util.ArchiveWorkItem
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
import kotlin.math.roundToInt
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpeleoApp(
    viewModel: MainViewModel = viewModel(),
    incomingOpenUri: Uri? = null,
    onIncomingOpenConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mapViewModel: MapScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current
    val hasActiveSearch = remember(uiState.filters) { uiState.filters.hasAnyActiveCriteria() }
    val effectiveRecords = remember(uiState.allRecords, uiState.filteredRecords, hasActiveSearch) {
        if (hasActiveSearch) uiState.filteredRecords else uiState.allRecords
    }
    val initialSession = remember { AppSessionStore.load(context) }
    fun persistSession(update: (AppSessionSnapshot) -> AppSessionSnapshot) {
        AppSessionStore.save(context, update(AppSessionStore.load(context)))
    }
    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var appLanguage by rememberSaveable { mutableStateOf(initialSession.appLanguage) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    fun isTabAllowedByCurrentRole(tab: AppTab): Boolean {
        val permissions = SovPermissionsStore.loadPermissions(context)
        if (!permissions.isApproved) return tab == AppTab.HOME || tab == AppTab.SETTINGS
        return when (tab) {
            AppTab.HOME, AppTab.CLOUD, AppTab.SETTINGS, AppTab.CALCULATOR, AppTab.SPELEO_RUNNER -> true
            AppTab.SEARCH, AppTab.MAP -> permissions.canViewSovBase || permissions.canViewKatastar
            AppTab.FIELD_PACKAGES -> permissions.canManageTrips
            AppTab.OFFLINE -> permissions.canViewSovBase || permissions.canManageTrips
            AppTab.TOOLS -> true
        }
    }

    fun navigateTo(tab: AppTab) {
        if (!isTabAllowedByCurrentRole(tab)) {
            val permissions = SovPermissionsStore.loadPermissions(context)
            Toast.makeText(
                context,
                "SOV role: ${permissions.roleLabel} nema pristup ovom modulu.",
                Toast.LENGTH_SHORT
            ).show()
            if (currentTab != AppTab.HOME) currentTab = AppTab.HOME
            if (navController.currentDestination?.route != SovAppRoutes.HOME) {
                navController.navigate(SovAppRoutes.HOME) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(SovAppRoutes.HOME) { saveState = true }
                }
            }
            return
        }
        if (currentTab != tab) currentTab = tab
        val route = tab.route
        if (navController.currentDestination?.route != route) {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(SovAppRoutes.HOME) { saveState = true }
            }
        }
    }

    LaunchedEffect(navBackStackEntry?.destination?.route) {
        navBackStackEntry?.destination?.route?.let { route ->
            appTabFromRoute(route)?.let { tab ->
                if (currentTab != tab) currentTab = tab
            }
        }
    }

    var mapFocusRecord by remember { mutableStateOf<SpeleoRecord?>(null) }
    var focusedSavedTrack by remember { mutableStateOf<SavedTrack?>(null) }
    var focusedSavedTrackNonce by rememberSaveable { mutableIntStateOf(0) }
    var offlineStateVersion by rememberSaveable { mutableIntStateOf(0) }
    var mapLayerStateVersion by rememberSaveable { mutableIntStateOf(0) }
    var photoStateVersion by rememberSaveable { mutableIntStateOf(0) }
    var centerOnUserNonce by rememberSaveable { mutableIntStateOf(0) }
    var autoCenterOnUserEnabled by rememberSaveable { mutableStateOf(initialSession.autoCenterOnUserEnabled) }
    val mapActions = MapActionController(
        context = context,
        onMapLayerChanged = { mapLayerStateVersion++ },
        onNavigateToMap = { navigateTo(AppTab.MAP) },
        onDisableAutoCenter = { autoCenterOnUserEnabled = false }
    )
    var currentUserLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var currentLocationAccuracyM by rememberSaveable { mutableStateOf<Double?>(null) }
    var currentLocationAltitudeM by rememberSaveable { mutableStateOf<Double?>(null) }
    var currentLocationProvider by rememberSaveable { mutableStateOf<String?>(null) }
    var currentLocationSpeedMps by rememberSaveable { mutableStateOf<Double?>(null) }
    var currentGpsBearingDeg by rememberSaveable { mutableStateOf<Float?>(null) }
    var currentGpsBearingAccuracyDeg by rememberSaveable { mutableStateOf<Float?>(null) }
    var waitingForGpsFix by rememberSaveable { mutableStateOf(false) }
    var positionEnabled by rememberSaveable { mutableStateOf(false) }
    var mapOrientationMode by rememberSaveable { mutableStateOf(initialSession.mapOrientationMode) }
    var offlineFocusPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var offlineFocusZoom by rememberSaveable { mutableStateOf(11.0) }
    var offlineFocusNonce by rememberSaveable { mutableIntStateOf(0) }
    var startOfflineAreaSelectionNonce by rememberSaveable { mutableIntStateOf(0) }
    var cancelOfflineAreaSelectionNonce by rememberSaveable { mutableIntStateOf(0) }
    var returnToFieldPackageFlowAfterDownload by rememberSaveable { mutableStateOf(false) }
    var resumeFieldPackageWizardNonce by rememberSaveable { mutableIntStateOf(0) }
    var resumeFieldPackageWizardPending by rememberSaveable { mutableStateOf(false) }
    var activeFieldPackage by remember { mutableStateOf<FieldPackageSummary?>(null) }
    fun boundsForFieldPackage(pkg: FieldPackageSummary): BoundingBoxFilter? {
        val minLat = pkg.minLat ?: return null
        val maxLat = pkg.maxLat ?: return null
        val minLon = pkg.minLon ?: return null
        val maxLon = pkg.maxLon ?: return null
        return BoundingBoxFilter(
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            label = pkg.locationName.ifBlank { pkg.name }
        )
    }
    fun clearFieldPackageMapContext() {
        activeFieldPackage = null
        if (uiState.filters.boundingBoxFilter != null) {
            viewModel.updateFilters(uiState.filters.copy(boundingBoxFilter = null))
        }
    }
    fun activateFieldPackageMapContext(pkg: FieldPackageSummary) {
        activeFieldPackage = pkg
        viewModel.updateFilters(uiState.filters.copy(boundingBoxFilter = boundsForFieldPackage(pkg)))
    }
    var navigationTarget by remember { mutableStateOf<NavigationTarget?>(null) }
    var rulerStartPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var rulerEndPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var rulerModeEnabled by rememberSaveable { mutableStateOf(false) }
    var selectedMarkedPointForActions by remember { mutableStateOf<MarkedPoint?>(null) }
    var selectedImportedPointForActions by remember { mutableStateOf<MarkedPoint?>(null) }
    var offlineZapisnikTarget by remember { mutableStateOf<OfflineZapisnikTarget?>(null) }
    var hideUserContentOnMap by rememberSaveable { mutableStateOf(initialSession.hideUserContentOnMap) }
    var simplePointViewEnabled by rememberSaveable { mutableStateOf(initialSession.simplePointViewEnabled) }
    val snackbarHostState = remember { SnackbarHostState() }
    var updateDialogInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updateCheckInProgress by rememberSaveable { mutableStateOf(false) }
    var updateInstallInProgress by rememberSaveable { mutableStateOf(false) }
    var startupUpdateCheckDone by rememberSaveable { mutableStateOf(false) }

    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var exportMode by rememberSaveable { mutableStateOf("all") }
    var pendingPhotoRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoRecordLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraPhoto by remember { mutableStateOf<PendingCameraPhoto?>(null) }
    var liveTrackingEnabled by rememberSaveable { mutableStateOf(false) }
    var trackingSessionNonce by rememberSaveable { mutableIntStateOf(0) }
    val trackingRuntimeState by TrackingRuntime.state.collectAsStateWithLifecycle()
    var positionHandle by remember { mutableStateOf<LocationHelper.TrackingHandle?>(null) }
    var liveTrackPoints by remember { mutableStateOf(listOf<TrackPoint>()) }
    val markedPoints = remember { mutableStateListOf<MarkedPoint>() }
    val savedTracks = remember { mutableStateListOf<SavedTrack>() }
    val importedLayers = remember { mutableStateListOf<ImportedLayer>() }
    var sharedLayers by remember { mutableStateOf<List<SharedLayerEntry>>(emptyList()) }
    var sharedLayersLoading by remember { mutableStateOf(false) }
    var importedFocusPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var importedFocusZoom by rememberSaveable { mutableStateOf(14.0) }
    var importedFocusNonce by rememberSaveable { mutableIntStateOf(0) }
    var searchFocusPoints by remember { mutableStateOf(listOf<GeoPoint>()) }
    var searchFocusRecordIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    fun validSearchGeoPoint(lat: Double?, lon: Double?): GeoPoint? {
        val safeLat = lat ?: return null
        val safeLon = lon ?: return null
        if (!safeLat.isFinite() || !safeLon.isFinite()) return null
        if (safeLat !in -90.0..90.0 || safeLon !in -180.0..180.0) return null
        return GeoPoint(safeLat, safeLon)
    }
    fun clearAllSearchState() {
        searchFocusPoints = emptyList()
        searchFocusRecordIds = emptySet()
        mapFocusRecord = null
        viewModel.updateFilters(FilterState())
    }
    var searchFocusNonce by rememberSaveable { mutableIntStateOf(0) }
    var persistedMapCenter by remember {
        mutableStateOf(
            if (initialSession.lastMapCenterLat != null && initialSession.lastMapCenterLon != null) {
                GeoPoint(initialSession.lastMapCenterLat, initialSession.lastMapCenterLon)
            } else null
        )
    }
    var persistedMapZoom by rememberSaveable { mutableStateOf(initialSession.lastMapZoom ?: 8.0) }
    var importBusy by rememberSaveable { mutableStateOf(false) }
    var importProgressTitle by rememberSaveable { mutableStateOf("Uvoz u tijeku") }
    var importProgressDetail by rememberSaveable { mutableStateOf("Velike datoteke učitavaju se u pozadini bez blokiranja karte.") }
    var importCanCancel by rememberSaveable { mutableStateOf(false) }
    val importCancelRequested = remember { AtomicBoolean(false) }
    var importSummaryLayer by remember { mutableStateOf<ImportedLayer?>(null) }
    LaunchedEffect(Unit) {
        val snapshot = withContext(Dispatchers.IO) {
            Triple(
                UserContentStore.loadMarkedPoints(context),
                UserContentStore.loadSavedTracks(context),
                UserContentStore.loadImportedLayers(context)
            )
        }
        markedPoints.clear()
        markedPoints.addAll(snapshot.first)
        savedTracks.clear()
        savedTracks.addAll(snapshot.second)
        importedLayers.clear()
        importedLayers.addAll(snapshot.third)
    }

    LaunchedEffect(trackingRuntimeState) {
        liveTrackingEnabled = trackingRuntimeState.active
        if (trackingRuntimeState.currentLocation != null) {
            currentUserLocation = trackingRuntimeState.currentLocation
        }
        if (trackingRuntimeState.accuracyM != null) currentLocationAccuracyM = trackingRuntimeState.accuracyM
        if (trackingRuntimeState.altitudeM != null) currentLocationAltitudeM = trackingRuntimeState.altitudeM
        if (trackingRuntimeState.provider != null) currentLocationProvider = trackingRuntimeState.provider
        if (trackingRuntimeState.speedMps != null) currentLocationSpeedMps = trackingRuntimeState.speedMps
        if (trackingRuntimeState.gpsBearingDeg != null) currentGpsBearingDeg = trackingRuntimeState.gpsBearingDeg
        if (trackingRuntimeState.gpsBearingAccuracyDeg != null) currentGpsBearingAccuracyDeg = trackingRuntimeState.gpsBearingAccuracyDeg
        if (trackingRuntimeState.active) waitingForGpsFix = trackingRuntimeState.waitingForGpsFix
        liveTrackPoints = trackingRuntimeState.points
    }

    LaunchedEffect(trackingRuntimeState.points.size, trackingRuntimeState.active) {
        if (trackingRuntimeState.active && trackingRuntimeState.points.size == 1) {
            centerOnUserNonce++
        }
    }

    LaunchedEffect(currentUserLocation?.latitude, currentUserLocation?.longitude) {
        viewModel.updateCurrentUserLocation(currentUserLocation)
    }

    LaunchedEffect(
        currentUserLocation?.latitude,
        currentUserLocation?.longitude,
        currentLocationAccuracyM,
        currentLocationAltitudeM,
        currentLocationProvider,
        currentLocationSpeedMps,
        currentGpsBearingDeg,
        currentGpsBearingAccuracyDeg,
        waitingForGpsFix,
        positionEnabled,
        autoCenterOnUserEnabled
    ) {
        mapViewModel.updateGps(
            MapGpsSnapshot(
                location = currentUserLocation,
                accuracyM = currentLocationAccuracyM,
                altitudeM = currentLocationAltitudeM,
                provider = currentLocationProvider,
                speedMps = currentLocationSpeedMps,
                bearingDeg = currentGpsBearingDeg,
                bearingAccuracyDeg = currentGpsBearingAccuracyDeg,
                waitingForFix = waitingForGpsFix,
                positionEnabled = positionEnabled,
                autoCenterEnabled = autoCenterOnUserEnabled
            )
        )
    }

    LaunchedEffect(currentTab, simplePointViewEnabled, hideUserContentOnMap, autoCenterOnUserEnabled, positionEnabled, mapOrientationMode, persistedMapCenter?.latitude, persistedMapCenter?.longitude, persistedMapZoom) {
        delay(450)
        persistSession { session ->
            session.copy(
                currentTab = currentTab,
                simplePointViewEnabled = simplePointViewEnabled,
                hideUserContentOnMap = hideUserContentOnMap,
                autoCenterOnUserEnabled = autoCenterOnUserEnabled,
                positionEnabled = positionEnabled,
                mapOrientationMode = mapOrientationMode,
                lastMapCenterLat = persistedMapCenter?.latitude,
                lastMapCenterLon = persistedMapCenter?.longitude,
                lastMapZoom = persistedMapZoom
            )
        }
    }
    var showMarkDialog by rememberSaveable { mutableStateOf(false) }
    var markDraftId by rememberSaveable { mutableStateOf("") }
    var markName by rememberSaveable { mutableStateOf("") }
    var markType by rememberSaveable { mutableStateOf("ostalo") }
    var markDescription by rememberSaveable { mutableStateOf("") }
    var markBaseLat by rememberSaveable { mutableStateOf(0.0) }
    var markBaseLon by rememberSaveable { mutableStateOf(0.0) }
    var pendingMarkExportId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingMarkId by rememberSaveable { mutableStateOf<String?>(null) }
    var showTrackSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showTrackResumeDialog by rememberSaveable { mutableStateOf(false) }
    var showBatteryDialog by rememberSaveable { mutableStateOf(false) }
    var pendingBatteryTrackContinueExisting by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var pendingTrackContinueExisting by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var locationRequestShouldOpenMap by rememberSaveable { mutableStateOf(true) }
    var trackName by rememberSaveable { mutableStateOf("") }
    var trackDescription by rememberSaveable { mutableStateOf("") }
    var trackSaveStartedAtMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var trackSaveEndedAtMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingTrackExportId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingTrackKmlExportId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSharePoint by remember { mutableStateOf<MarkedPoint?>(null) }
    var showSharePointDialog by remember { mutableStateOf(false) }
    var pendingShareTrack by remember { mutableStateOf<SavedTrack?>(null) }
    var showShareTrackDialog by remember { mutableStateOf(false) }
    var shareDialogNameDraft by remember { mutableStateOf("") }
    var shareDialogDescDraft by remember { mutableStateOf("") }
    var shareDialogTagsDraft by remember { mutableStateOf("") }
    var drawingModeEnabled by rememberSaveable { mutableStateOf(false) }
    var drawingPoints by remember { mutableStateOf(listOf<GeoPoint>()) }
    var drawingStrokeWidthDp by rememberSaveable { mutableIntStateOf(7) }
    var drawingSmoothEnabled by rememberSaveable { mutableStateOf(true) }
    var showDrawingSaveDialog by rememberSaveable { mutableStateOf(false) }
    var drawingName by rememberSaveable { mutableStateOf("") }
    var drawingDescription by rememberSaveable { mutableStateOf("") }
    val launcherScope = rememberCoroutineScope()

    suspend fun refreshSharedLayers() {
        sharedLayersLoading = true
        sharedLayers = withContext(Dispatchers.IO) { SharedLayersSyncClient.fetchLayers() }
        sharedLayersLoading = false
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.google-earth.kml+xml")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val exportRecords = when (exportMode) {
            "not_in_cadastre" -> effectiveRecords.filter { it.cadastre.not_in_cadastre_candidate == true }
            else -> effectiveRecords
        }.filter { it.location.lat != null && it.location.lon != null }
        val exportCustomPoints = if (exportMode == "custom_points") markedPoints.toList() else emptyList()

        if (exportMode == "custom_points" && exportCustomPoints.isEmpty()) {
            Toast.makeText(context, "Nema custom KML točaka za export", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        if (exportMode != "custom_points" && exportRecords.isEmpty()) {
            Toast.makeText(context, "Nema objekata za export", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }

        runCatching {
            val content = when (exportMode) {
                "not_in_cadastre" -> KmlExporter.toKml(exportRecords, "Speleo export - za provjeru")
                "custom_points" -> KmlExporter.toMarkedPointsKml(exportCustomPoints, "Speleo export - custom KML točke")
                else -> KmlExporter.toKml(exportRecords, "Speleo export - svi pronađeni")
            }
            writeTextToUri(context, uri, content)
            writeTextToOfflinePublicFolder(
                context = context,
                folder = OfflineTileManager.publicKmlRoot(),
                displayName = suggestedKmlExportName(exportMode),
                extension = "kml",
                content = content
            )
        }.onSuccess {
            val count = if (exportMode == "custom_points") exportCustomPoints.size else exportRecords.size
            Toast.makeText(context, "KML exportiran ($count)", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(context, "Greška pri exportu", Toast.LENGTH_LONG).show()
        }
    }

    val markExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.google-earth.kml+xml")
    ) { uri ->
        val mark = markedPoints.firstOrNull { it.id == pendingMarkExportId }
        if (uri == null || mark == null) {
            pendingMarkExportId = null
            return@rememberLauncherForActivityResult
        }
        runCatching {
            val content = markedPointToKml(mark)
            writeTextToUri(context, uri, content)
            writeTextToOfflinePublicFolder(
                context = context,
                folder = OfflineTileManager.publicKmlRoot(),
                displayName = mark.name.ifBlank { "custom_point" },
                extension = "kml",
                content = content
            )
        }.onSuccess {
            Toast.makeText(context, "KML točka exportirana", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(context, "Greška pri exportu točke", Toast.LENGTH_LONG).show()
        }
        pendingMarkExportId = null
    }

    val trackExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri ->
        val track = savedTracks.firstOrNull { it.id == pendingTrackExportId }
        if (uri == null || track == null) {
            pendingTrackExportId = null
            return@rememberLauncherForActivityResult
        }
        runCatching {
            val content = trackToGpx(track)
            writeTextToUri(context, uri, content)
            writeTextToOfflinePublicFolder(
                context = context,
                folder = OfflineTileManager.publicGpxRoot(),
                displayName = track.name.ifBlank { "Track" },
                extension = "gpx",
                content = content
            )
        }.onSuccess {
            Toast.makeText(context, "GPX exportiran", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(context, "Greška pri exportu GPX-a", Toast.LENGTH_LONG).show()
        }
        pendingTrackExportId = null
    }

    val trackKmlExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.google-earth.kml+xml")
    ) { uri ->
        val track = savedTracks.firstOrNull { it.id == pendingTrackKmlExportId }
        pendingTrackKmlExportId = null
        if (uri == null || track == null) return@rememberLauncherForActivityResult
        launcherScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                            writer.write(KmlExporter.toTrackKml(track))
                        }
                    }
                }
            }
            Toast.makeText(context, "KML track exportiran", Toast.LENGTH_LONG).show()
        }
    }


    fun handleImportedUri(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        val displayName = queryDisplayName(context, uri) ?: (uri.lastPathSegment ?: "Import")
        val lowerName = displayName.lowercase(Locale.ROOT)
        if (lowerName.endsWith(".sovpkg")) {
            launcherScope.launch {
                importBusy = true
                importCanCancel = false
                importCancelRequested.set(false)
                importProgressTitle = "Uvoz SOV izleta"
                importProgressDetail = displayName
                val importedPackage = withContext(Dispatchers.IO) {
                    runCatching { FieldPackageManager.importPackage(context, uri) }.getOrNull()
                }
                importBusy = false
                importCanCancel = false
                if (importedPackage == null) {
                    Toast.makeText(context, "Ne mogu otvoriti SOV paket", Toast.LENGTH_LONG).show()
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    val currentPackages = FieldPackageManager.list(context)
                    val nextPackages = listOf(importedPackage) + currentPackages.filterNot { it.id == importedPackage.id }
                    FieldPackageManager.save(context, nextPackages)
                }
                val refreshedUserContent = withContext(Dispatchers.IO) {
                    Pair(UserContentStore.loadMarkedPoints(context), UserContentStore.loadSavedTracks(context))
                }
                markedPoints.clear()
                markedPoints.addAll(refreshedUserContent.first)
                savedTracks.clear()
                savedTracks.addAll(refreshedUserContent.second)
                if (importedPackage.includesOfflineMap) {
                    offlineStateVersion++
                    mapLayerStateVersion++
                }
                activeFieldPackage = importedPackage
                navigateTo(AppTab.FIELD_PACKAGES)
                Toast.makeText(context, "SOV izlet uvezen: " + importedPackage.name, Toast.LENGTH_LONG).show()
            }
            return
        }
        if (lowerName.endsWith(".mbtiles")) {
            launcherScope.launch {
                importBusy = true
                importCanCancel = false
                importCancelRequested.set(false)
                importProgressTitle = "Import MBTiles karte"
                importProgressDetail = displayName
                val result = OfflineTileManager.importMbtiles(context, uri, displayName)
                if (result.isSuccess) {
                    withContext(Dispatchers.IO) { backupImportToOfflineFolder(context, uri, displayName) }
                }
                result.onSuccess { importedName ->
                    hideUserContentOnMap = false
                    OfflineTileManager.setCustomOverlayEnabled(context, importedName, true)
                    val bounds = OfflineTileManager.getOfflineBounds(context, importedName)
                    if (bounds != null) {
                        offlineFocusPoint = GeoPoint((bounds.minLat + bounds.maxLat) / 2.0, (bounds.minLon + bounds.maxLon) / 2.0)
                        offlineFocusZoom = boundsToZoom(
                            GeoPoint(bounds.minLat, bounds.minLon),
                            GeoPoint(bounds.maxLat, bounds.maxLon)
                        )
                        offlineFocusNonce++
                    }
                    MapLayerPrefs.setMode(context, MapLayerMode.OFFLINE)
                    offlineStateVersion++
                    mapLayerStateVersion++
                    navigateTo(AppTab.MAP)
                    Toast.makeText(context, "Importirana MBTiles overlay karta: $importedName", Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(context, "Ne mogu uvesti MBTiles", Toast.LENGTH_LONG).show()
                }
                importBusy = false
                importCanCancel = false
            }
            return
        }

        if (lowerName.endsWith(".zip")) {
            launcherScope.launch {
                importBusy = true
                importCanCancel = false
                importCancelRequested.set(false)
                importProgressTitle = "Analiziram ZIP"
                importProgressDetail = displayName
                when (withContext(Dispatchers.IO) { sniffZipImportKind(context, uri) }) {
                    ZipImportKind.TILES -> {
                        importProgressTitle = "Import offline karte"
                        importProgressDetail = "Priprema…"
                        val result = OfflineTileManager.importTileZip(
                            context, uri, displayName, asOfflineMap = true,
                            onProgress = { done, total, zoom ->
                                launcherScope.launch {
                                    importProgressTitle = "Import offline karte"
                                    importProgressDetail = if (total > 0)
                                        "Zoom $zoom • $done / $total pločica (${(done * 100 / total)}%)"
                                    else
                                        "Zoom $zoom • $done pločica…"
                                }
                            }
                        )
                        result.onSuccess { importedName ->
                            val bounds = OfflineTileManager.getOfflineBounds(context, importedName)
                            if (bounds != null) {
                                offlineFocusPoint = GeoPoint((bounds.minLat + bounds.maxLat) / 2.0, (bounds.minLon + bounds.maxLon) / 2.0)
                                offlineFocusZoom = boundsToZoom(
                                    GeoPoint(bounds.minLat, bounds.minLon),
                                    GeoPoint(bounds.maxLat, bounds.maxLon)
                                )
                                offlineFocusNonce++
                            }
                            MapLayerPrefs.setMode(context, MapLayerMode.OFFLINE)
                            offlineStateVersion++
                            mapLayerStateVersion++
                            navigateTo(AppTab.MAP)
                            Toast.makeText(context, "Importirana offline karta: $importedName", Toast.LENGTH_LONG).show()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "Ne mogu uvesti ZIP kartu", Toast.LENGTH_LONG).show()
                        }
                    }
                    ZipImportKind.GENERIC_IMPORT -> {
                        importCanCancel = true
                        importProgressTitle = "Uvoz SHP/KMZ/ZIP layera"
                        val importCancelled = AtomicBoolean(false)
                        val imported = withContext(Dispatchers.IO) {
                            runCatching {
                                context.contentResolver.openInputStream(uri)?.use {
                                    ImportParser.parse(
                                        input = it,
                                        suggestedName = displayName,
                                        tempDir = context.cacheDir,
                                        onProgress = { progress ->
                                            launcherScope.launch {
                                                importProgressTitle = progress.stage
                                                importProgressDetail = buildString {
                                                    append(displayName)
                                                    if (progress.processed > 0) append(" • ").append(progress.processed).append(" elemenata")
                                                    if (progress.points > 0 || progress.tracks > 0) {
                                                        append(" • ").append(progress.points).append(" toč.")
                                                        append(" • ").append(progress.tracks).append(" lin.")
                                                    }
                                                }
                                            }
                                        },
                                        isCancelled = { importCancelRequested.get() }
                                    )
                                }
                            }.getOrElse { throwable ->
                                if (throwable is CancellationException) {
                                    importCancelled.set(true)
                                    null
                                } else {
                                    null
                                }
                            }
                        }
                        if (importCancelled.get()) {
                            Toast.makeText(context, "Uvoz prekinut", Toast.LENGTH_LONG).show()
                            importBusy = false
                            importCanCancel = false
                            return@launch
                        }
                        if (imported == null) {
                            Toast.makeText(context, "Ne mogu uvesti ZIP layer", Toast.LENGTH_LONG).show()
                            importBusy = false
                            importCanCancel = false
                            return@launch
                        }
                        val layer = imported.copy(id = "import_" + System.currentTimeMillis(), visible = true)
                        if (layer.points.isEmpty() && layer.tracks.isEmpty()) {
                            Toast.makeText(context, "ZIP je uvezen, ali nije pronađena nijedna podržana geometrija", Toast.LENGTH_LONG).show()
                            importBusy = false
                            importCanCancel = false
                            return@launch
                        }
                        hideUserContentOnMap = false
                        importedLayers.add(0, layer)
                        withContext(Dispatchers.IO) {
                            UserContentStore.saveImportedLayers(context, importedLayers.toList())
                            backupImportToOfflineFolder(context, uri, displayName)
                        }
                        val bounds = importedBounds(layer)
                        if (bounds != null) {
                            importedFocusPoint = GeoPoint((bounds.first.latitude + bounds.second.latitude) / 2.0, (bounds.first.longitude + bounds.second.longitude) / 2.0)
                            importedFocusZoom = boundsToZoom(bounds.first, bounds.second)
                            importedFocusNonce++
                        }
                        navigateTo(AppTab.MAP)
                        importSummaryLayer = layer
                        Toast.makeText(context, "Importiran ${layer.type}: ${layer.name}", Toast.LENGTH_LONG).show()
                    }
                    ZipImportKind.UNKNOWN -> {
                        Toast.makeText(context, "ZIP nije prepoznat kao SHP/KMZ bundle ni kao offline tiles paket", Toast.LENGTH_LONG).show()
                    }
                }
                importBusy = false
                importCanCancel = false
            }
            return
        }

        launcherScope.launch {
            importBusy = true
            importCanCancel = true
            importCancelRequested.set(false)
            importProgressTitle = "Uvoz KML/GPX/KMZ/GeoJSON/CSV/XLSX/GPKG/GeoTIFF"
            importProgressDetail = displayName
            val importCancelled = AtomicBoolean(false)
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        ImportParser.parse(
                            input = it,
                            suggestedName = displayName,
                            tempDir = context.cacheDir,
                            onProgress = { progress ->
                                launcherScope.launch {
                                    importProgressTitle = progress.stage
                                    importProgressDetail = buildString {
                                        append(displayName)
                                        if (progress.processed > 0) append(" • ").append(progress.processed).append(" elemenata")
                                        if (progress.points > 0 || progress.tracks > 0) {
                                            append(" • ").append(progress.points).append(" toč.")
                                            append(" • ").append(progress.tracks).append(" lin.")
                                        }
                                    }
                                }
                            },
                            isCancelled = { importCancelRequested.get() }
                        )
                    }
                }.getOrElse { throwable ->
                    if (throwable is CancellationException) {
                        importCancelled.set(true)
                        null
                    } else {
                        null
                    }
                }
            }
            importBusy = false
            importCanCancel = false
            if (importCancelled.get()) {
                Toast.makeText(context, "Uvoz prekinut", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (imported == null) {
                Toast.makeText(context, "Ne mogu uvesti KML/GPX/KMZ/GeoJSON/CSV/XLSX/GPKG/GeoTIFF", Toast.LENGTH_LONG).show()
                return@launch
            }
            val layer = imported.copy(id = "import_" + System.currentTimeMillis(), visible = true)
            if (layer.points.isEmpty() && layer.tracks.isEmpty()) {
                Toast.makeText(context, "Import je pročitan, ali nema podržanih geometrija za prikaz", Toast.LENGTH_LONG).show()
                return@launch
            }
            hideUserContentOnMap = false
            importedLayers.add(0, layer)
            withContext(Dispatchers.IO) {
                UserContentStore.saveImportedLayers(context, importedLayers.toList())
                backupImportToOfflineFolder(context, uri, displayName)
            }
            val bounds = importedBounds(layer)
            if (bounds != null) {
                importedFocusPoint = GeoPoint((bounds.first.latitude + bounds.second.latitude) / 2.0, (bounds.first.longitude + bounds.second.longitude) / 2.0)
                importedFocusZoom = boundsToZoom(bounds.first, bounds.second)
                importedFocusNonce++
            }
            navigateTo(AppTab.MAP)
            importSummaryLayer = layer
            Toast.makeText(context, "Importiran ${layer.type}: ${layer.name}", Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        handleImportedUri(uri)
    }

    LaunchedEffect(incomingOpenUri) {
        val uri = incomingOpenUri ?: return@LaunchedEffect
        handleImportedUri(uri)
        onIncomingOpenConsumed()
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val recordId = pendingPhotoRecordId
        if (uri != null && recordId != null) {
            val saved = PhotoStore.addPhotoCopyFromUri(context, recordId, uri, pendingPhotoRecordLabel)
            if (saved != null) {
                photoStateVersion++
                Toast.makeText(context, "Fotografija spremljena u SOV/photos", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Ne mogu spremiti fotografiju", Toast.LENGTH_LONG).show()
            }
        }
        pendingPhotoRecordId = null
        pendingPhotoRecordLabel = null
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val recordId = pendingPhotoRecordId
        val pending = pendingCameraPhoto
        if (success && recordId != null && pending != null) {
            PhotoStore.addStoredPath(context, recordId, pending.absolutePath, pendingPhotoRecordLabel)
            photoStateVersion++
            Toast.makeText(context, "Fotografija snimljena u SOV/photos", Toast.LENGTH_LONG).show()
        } else {
            pending?.absolutePath?.let { runCatching { java.io.File(it).delete() } }
        }
        pendingPhotoRecordId = null
        pendingPhotoRecordLabel = null
        pendingCameraPhoto = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            pendingCameraPhoto?.absolutePath?.let { runCatching { java.io.File(it).delete() } }
            pendingCameraPhoto = null
            pendingPhotoRecordId = null
            pendingPhotoRecordLabel = null
            Toast.makeText(context, "Treba dozvola za kameru", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        val pending = pendingCameraPhoto
        if (pending != null) {
            cameraLauncher.launch(pending.contentUri)
        }
    }


    fun updateLocationState(location: Location) {
        currentUserLocation = GeoPoint(location.latitude, location.longitude)
        currentLocationAccuracyM = if (location.hasAccuracy()) location.accuracy.toDouble() else null
        currentLocationAltitudeM = if (location.hasAltitude()) location.altitude else null
        currentLocationProvider = location.provider
        currentLocationSpeedMps = if (location.hasSpeed()) location.speed.toDouble() else null
        currentGpsBearingDeg = if (location.hasBearing()) location.bearing else null
        currentGpsBearingAccuracyDeg = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && location.hasBearingAccuracy()) location.bearingAccuracyDegrees else null
        waitingForGpsFix = !LocationHelper.isGoodNavigationFix(location)
    }

    fun stopPositionUpdates(clearState: Boolean = false) {
        LocationHelper.stopLocationUpdates(positionHandle)
        positionHandle = null
        if (clearState && !liveTrackingEnabled) {
            currentUserLocation = null
            currentLocationAccuracyM = null
            currentLocationAltitudeM = null
            currentLocationProvider = null
            currentLocationSpeedMps = null
            currentGpsBearingDeg = null
            currentGpsBearingAccuracyDeg = null
            waitingForGpsFix = false
        }
    }

    fun openLocationSettings() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun batteryOptimizationIntent(): Intent {
        val packageUri = Uri.parse("package:${context.packageName}")
        val packageManager = context.packageManager
        val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
        val batterySettingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        return listOf(directIntent, batterySettingsIntent, appDetailsIntent)
            .map { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            .firstOrNull { it.resolveActivity(packageManager) != null }
            ?: appDetailsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun openExternalUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            Toast.makeText(context, "Ne mogu otvoriti link", Toast.LENGTH_LONG).show()
        }
    }

    fun startPositionUpdates(openMapAfterStart: Boolean = true, allowBootstrapLocation: Boolean = true) {
        stopPositionUpdates()
        currentUserLocation = null
        currentLocationAccuracyM = null
        currentLocationAltitudeM = null
        currentLocationProvider = null
        currentLocationSpeedMps = null
        currentGpsBearingDeg = null
        currentGpsBearingAccuracyDeg = null
        waitingForGpsFix = true
        if (allowBootstrapLocation) {
            LocationHelper.getRecentNavigationBootstrapLocation(context)?.let { location ->
                val hadNoFix = currentUserLocation == null
                updateLocationState(location)
                waitingForGpsFix = !LocationHelper.isGoodNavigationFix(location)
                if (hadNoFix && !waitingForGpsFix) centerOnUserNonce++
            }
        }
        positionHandle = LocationHelper.startLocationUpdates(
            context = context,
            minTimeMs = 800L,
            minDistanceM = 1f,
            mode = LocationHelper.LocationMode.GPS_ONLY
        ) { location ->
            val hadNoFix = currentUserLocation == null
            updateLocationState(location)
            if (hadNoFix) centerOnUserNonce++
        }
        if (positionHandle == null) {
            positionEnabled = false
            waitingForGpsFix = false
            if (!LocationHelper.isGpsProviderEnabled(context)) {
                Toast.makeText(context, "GPS je ugašen. Uključi GPS u postavkama.", Toast.LENGTH_LONG).show()
                openLocationSettings()
            } else {
                Toast.makeText(context, "GPS pozicija nije dostupna", Toast.LENGTH_LONG).show()
            }
        } else if (openMapAfterStart) {
            navigateTo(AppTab.MAP)
        }
    }

    fun openTrackSaveDialog() {
        if (liveTrackPoints.size < 2) return
        trackName = "Track ${savedTracks.size + 1}"
        trackDescription = ""
        trackSaveStartedAtMillis = TrackingRuntime.state.value.startedAtMillis
        trackSaveEndedAtMillis = System.currentTimeMillis()
        showTrackSaveDialog = true
    }

    fun sanitizeOfflineFileName(raw: String, fallback: String): String =
        raw.ifBlank { fallback }
            .replace(Regex("[\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim('_')
            .ifBlank { fallback }

    fun backupTrackToOfflineGpx(track: SavedTrack) {
        runCatching {
            val fileName = sanitizeOfflineFileName(track.name, "Track") + ".gpx"
            val content = trackToGpx(track)
            listOf(OfflineTileManager.gpxRoot(context), OfflineTileManager.publicGpxRoot()).forEach { root ->
                root.mkdirs()
                File(root, fileName).writeText(content)
            }
        }
    }


    fun buildPausedTrackForOffline(points: List<TrackPoint> = liveTrackPoints): SavedTrack? {
        if (points.size < 2) return null
        val startedAt = TrackingRuntime.state.value.startedAtMillis ?: System.currentTimeMillis()
        return SavedTrack(
            id = "track_" + System.currentTimeMillis(),
            name = "Track ${savedTracks.size + 1}",
            description = "",
            createdAtMillis = startedAt,
            points = points
        )
    }

    fun movePausedTrackToOffline(clearOverlay: Boolean = true): SavedTrack? {
        val archived = buildPausedTrackForOffline() ?: run {
            if (clearOverlay) liveTrackPoints = emptyList()
            return null
        }
        savedTracks.add(0, archived)
        UserContentStore.saveSavedTracks(context, savedTracks.toList())
        backupTrackToOfflineGpx(archived)
        if (clearOverlay) {
            liveTrackPoints = emptyList()
        }
        return archived
    }

    fun stopLiveTracking(promptToSave: Boolean = false) {
        val capturedPoints = TrackingRuntime.state.value.points.ifEmpty { liveTrackPoints }
        TrackingForegroundService.stop(context)
        liveTrackPoints = capturedPoints
        waitingForGpsFix = false
        if (promptToSave && capturedPoints.size >= 2) {
            openTrackSaveDialog()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun startTrackingNow(continueExistingTrack: Boolean = false) {
        val pausedPoints = liveTrackPoints
        val pausedStartedAt = TrackingRuntime.state.value.startedAtMillis
        if (continueExistingTrack && pausedPoints.isNotEmpty()) {
            TrackingRuntime.startSession(existingPoints = pausedPoints, startedAtMillis = pausedStartedAt)
        } else {
            TrackingRuntime.clearPoints()
            liveTrackPoints = emptyList()
        }
        if (currentUserLocation == null) {
            LocationHelper.bootstrapLastKnownLocation(context) { location ->
                updateLocationState(location)
                waitingForGpsFix = !LocationHelper.isGoodNavigationFix(location)
            }
        }
        if (currentLocationAccuracyM == null) {
            currentLocationAccuracyM = null
            currentLocationAltitudeM = null
            currentLocationProvider = null
        }
        waitingForGpsFix = true
        TrackingForegroundService.start(context)
        liveTrackingEnabled = true
        navigateTo(AppTab.MAP)
        trackingSessionNonce++
        Toast.makeText(context, if (continueExistingTrack && pausedPoints.isNotEmpty()) "Nastavljen postojeći track" else "Live GPS tracking uključen", Toast.LENGTH_SHORT).show()
    }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val continueExisting = pendingBatteryTrackContinueExisting == true
        pendingBatteryTrackContinueExisting = null
        if (isBatteryOptimizationIgnored()) {
            Toast.makeText(context, appLanguage.pick("Tracking u pozadini je spreman", "Background tracking ready"), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, appLanguage.pick("Tracking pokrenut, ali Android ga još može zaustaviti u pozadini", "Tracking started, but Android may still stop it in background"), Toast.LENGTH_LONG).show()
        }
        startTrackingNow(continueExistingTrack = continueExisting)
    }

    fun requestBatteryOptimizationForTracking() {
        runCatching {
            batteryOptimizationLauncher.launch(batteryOptimizationIntent())
        }.onFailure {
            val continueExisting = pendingBatteryTrackContinueExisting == true
            pendingBatteryTrackContinueExisting = null
            Toast.makeText(context, appLanguage.pick("Nisam mogao otvoriti Androidov upit. Tracking je pokrenut svejedno.", "Could not open Android prompt. Tracking started anyway."), Toast.LENGTH_LONG).show()
            startTrackingNow(continueExistingTrack = continueExisting)
        }
    }

    fun startLiveTracking(continueExistingTrack: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!LocationHelper.isGpsProviderEnabled(context)) {
            Toast.makeText(context, "GPS je ugašen. Uključi GPS u postavkama.", Toast.LENGTH_LONG).show()
            openLocationSettings()
            liveTrackingEnabled = false
            return
        }
        if (!isBatteryOptimizationIgnored()) {
            pendingBatteryTrackContinueExisting = continueExistingTrack
            showBatteryDialog = true
            liveTrackingEnabled = false
            return
        }
        startTrackingNow(continueExistingTrack = continueExistingTrack)
    }


    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            when {
                pendingTrackContinueExisting != null -> {
                    val continueExisting = pendingTrackContinueExisting == true
                    pendingTrackContinueExisting = null
                    startLiveTracking(continueExistingTrack = continueExisting)
                }
                positionEnabled -> startPositionUpdates(openMapAfterStart = locationRequestShouldOpenMap, allowBootstrapLocation = false)
                else -> Toast.makeText(context, "Lokacija trenutno nije aktivna", Toast.LENGTH_LONG).show()
            }
        } else {
            pendingTrackContinueExisting = null
            locationRequestShouldOpenMap = true
            liveTrackingEnabled = false
            positionEnabled = false
            waitingForGpsFix = false
            Toast.makeText(context, "Treba dozvola za točan GPS", Toast.LENGTH_LONG).show()
        }
    }

    fun togglePosition(enabled: Boolean) {
        positionEnabled = enabled
        if (enabled) {
            locationRequestShouldOpenMap = true
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) startPositionUpdates(allowBootstrapLocation = false)
            else locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            locationRequestShouldOpenMap = true
            if (liveTrackingEnabled) {
                liveTrackingEnabled = false
                stopLiveTracking(promptToSave = true)
            }
            stopPositionUpdates(clearState = true)
            Toast.makeText(context, "GPS pozicija isključena", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerMyLocation() {
        togglePosition(!positionEnabled)
    }

    fun toggleAutoCenterOnUser() {
        autoCenterOnUserEnabled = !autoCenterOnUserEnabled
        if (autoCenterOnUserEnabled) {
            if (!positionEnabled && !liveTrackingEnabled) {
                togglePosition(true)
            }
            if (currentUserLocation != null) centerOnUserNonce++
        }
    }

    fun requestSearchGpsFix() {
        if (currentUserLocation != null) return
        positionEnabled = true
        locationRequestShouldOpenMap = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startPositionUpdates(openMapAfterStart = false, allowBootstrapLocation = false)
        } else {
            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun openMapsTk25() {
        mapActions.openTk25BaseMap()
    }

    fun pauseUserAutoFocusForManualMapTarget() {
        mapActions.pauseUserAutoFocusForManualTarget()
    }

    fun toggleLiveTracking(enabled: Boolean) {
        if (enabled) {
            positionEnabled = true
            stopPositionUpdates()
            val hasPausedTrack = !TrackingRuntime.state.value.active && liveTrackPoints.isNotEmpty()
            if (hasPausedTrack) {
                showTrackResumeDialog = true
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationRequestShouldOpenMap = true
                pendingTrackContinueExisting = null
                startLiveTracking()
            } else {
                locationRequestShouldOpenMap = true
                pendingTrackContinueExisting = false
                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            liveTrackingEnabled = false
            pendingTrackContinueExisting = null
            stopLiveTracking(promptToSave = true)
            if (positionEnabled) startPositionUpdates(allowBootstrapLocation = false) else stopPositionUpdates(clearState = true)
            Toast.makeText(context, "Live GPS tracking isključen", Toast.LENGTH_SHORT).show()
        }
    }

    fun upsertMarkedPoint(point: MarkedPoint) {
        val idx = markedPoints.indexOfFirst { it.id == point.id }
        if (idx >= 0) {
            markedPoints[idx] = point.copy(visible = markedPoints[idx].visible)
        } else {
            markedPoints.add(0, point.copy(visible = true))
        }
        UserContentStore.saveMarkedPoints(context, markedPoints.toList())
    }

    fun deleteMarkedPoint(point: MarkedPoint) {
        markedPoints.removeAll { it.id == point.id }
        if (selectedMarkedPointForActions?.id == point.id) selectedMarkedPointForActions = null
        if (selectedImportedPointForActions?.id == point.id) selectedImportedPointForActions = null
        UserContentStore.saveMarkedPoints(context, markedPoints.toList())
    }

    fun openExistingMarkDialog(point: MarkedPoint) {
        editingMarkId = point.id
        markDraftId = point.id
        markName = point.name
        markType = point.type
        markDescription = point.description
        markBaseLat = point.lat
        markBaseLon = point.lon
        showMarkDialog = true
    }

    fun setNavigationTarget(target: NavigationTarget?) {
        navigationTarget = target
        target?.let {
            navigateTo(AppTab.MAP)
        }
    }

    fun focusRecordOnMap(record: SpeleoRecord, navigation: Boolean = false) {
        clearFieldPackageMapContext()
        focusedSavedTrack = null
        mapFocusRecord = record
        autoCenterOnUserEnabled = false
        offlineFocusPoint = null
        importedFocusPoint = null
        val focusPoint = validSearchGeoPoint(record.location.lat, record.location.lon)
        searchFocusPoints = listOfNotNull(focusPoint)
        searchFocusRecordIds = if (focusPoint != null) setOf(record.id) else emptySet()
        if (navigation && focusPoint != null) {
            navigationTarget = NavigationTarget(
                id = record.id,
                name = record.name,
                point = focusPoint,
                targetAltitudeM = record.location.altitude_m
            )
        }
        searchFocusNonce++
        openMapsTk25()
    }

    fun focusArchiveItemOnMap(item: ArchiveWorkItem) {
        val lat = item.lat.trim().replace(',', '.').toDoubleOrNull()
        val lon = item.lon.trim().replace(',', '.').toDoubleOrNull()
        val point = validSearchGeoPoint(lat, lon)
        if (point == null) {
            Toast.makeText(context, "Ovaj objekt nema valjane koordinate za kartu.", Toast.LENGTH_LONG).show()
            return
        }
        clearFieldPackageMapContext()
        focusedSavedTrack = null
        autoCenterOnUserEnabled = false
        offlineFocusPoint = null
        importedFocusPoint = null
        val archiveRecord = SpeleoRecord(
            id = item.objectId.ifBlank { "archive_${lat}_${lon}" },
            source = "sov_archive",
            source_labels = listOf("sov", "arhivar"),
            name = item.objectName.ifBlank { "Arhivar objekt" },
            location = com.darko.speleov1.model.Location(
                lat = lat,
                lon = lon,
                county = item.county.ifBlank { null },
                municipality = item.municipality.ifBlank { null },
                nearest_place = item.nearestPlace.ifBlank { null },
                locality = null,
                island = null,
                altitude_m = null,
                protected_area = null
            ),
            cadastre = com.darko.speleov1.model.Cadastre(
                status = item.cadastreStatus.ifBlank { null },
                cadastral_number = item.plateNumber.ifBlank { null },
                in_cadastre = null,
                not_in_cadastre_candidate = null
            ),
            classification = com.darko.speleov1.model.Classification(
                object_type = item.objectType.ifBlank { null },
                object_type_source = null,
                record_status = item.recordStatus.ifBlank { null },
                field_tasks = item.fieldTasks.split(';', ',', '|').map { it.trim() }.filter { it.isNotBlank() },
                priority = null,
                kml_export_candidate = null
            ),
            metrics = com.darko.speleov1.model.Metrics(null, null, null, null),
            condition = com.darko.speleov1.model.Condition(
                plate_number = item.plateNumber.ifBlank { null },
                main_entrance_status = null,
                hazards = null,
                pollution = null
            ),
            research = com.darko.speleov1.model.Research(null, null, null, null, null, null, null, null, null, null),
            content = com.darko.speleov1.model.Content(
                access_description = null,
                technical_description = item.baseDetailsText.ifBlank { item.fullDetailsText }.take(1000).ifBlank { null },
                note = item.note.ifBlank { null },
                literature = null,
                name_origin = null,
                synonyms = null,
                other_synonyms = null,
                clean_cave_report = null,
                geological_or_anthropogenic_activities = null
            ),
            raw = null,
            search_text = listOf(item.objectName, item.plateNumber, item.nearestPlace, item.objectType).joinToString(" ")
        )
        mapFocusRecord = archiveRecord
        searchFocusPoints = listOf(point)
        searchFocusRecordIds = setOf(archiveRecord.id)
        searchFocusNonce++
        openMapsTk25()
    }

    fun saveCurrentTrack(): SavedTrack {
        val track = SavedTrack(
            id = "track_" + System.currentTimeMillis(),
            name = trackName.ifBlank { "Track ${savedTracks.size + 1}" },
            description = trackDescription,
            points = liveTrackPoints
        )
        savedTracks.add(0, track)
        UserContentStore.saveSavedTracks(context, savedTracks.toList())
        backupTrackToOfflineGpx(track)
        liveTrackPoints = emptyList()
        return track
    }

    fun openDrawingSaveDialog() {
        if (drawingPoints.size < 2) return
        drawingName = "Ruta ${savedTracks.size + 1}"
        drawingDescription = ""
        showDrawingSaveDialog = true
    }

    fun buildCurrentDrawingTrack(): SavedTrack? {
        if (drawingPoints.size < 2) return null
        return SavedTrack(
            id = "draw_" + System.currentTimeMillis(),
            name = drawingName.ifBlank { "Ruta ${savedTracks.size + 1}" },
            description = drawingDescription,
            points = drawingPoints.map { TrackPoint(it, null) }
        )
    }

    fun saveCurrentDrawing(): SavedTrack? {
        val track = buildCurrentDrawingTrack() ?: return null
        savedTracks.add(0, track)
        UserContentStore.saveSavedTracks(context, savedTracks.toList())
        backupTrackToOfflineGpx(track)
        drawingPoints = emptyList()
        drawingModeEnabled = false
        return track
    }

    fun deleteSavedTrack(track: SavedTrack) {
        savedTracks.removeAll { it.id == track.id }
        if (focusedSavedTrack?.id == track.id) {
            focusedSavedTrack = null
            focusedSavedTrackNonce++
        }
        UserContentStore.saveSavedTracks(context, savedTracks.toList())
    }


    fun setMarkedPointVisibility(point: MarkedPoint, visible: Boolean): MarkedPoint? {
        val idx = markedPoints.indexOfFirst { it.id == point.id }
        if (idx < 0) return null
        val updated = markedPoints[idx].copy(visible = visible)
        markedPoints[idx] = updated
        UserContentStore.saveMarkedPoints(context, markedPoints.toList())
        return updated
    }

    fun toggleMarkedPointVisibility(point: MarkedPoint) {
        setMarkedPointVisibility(point, !point.visible)
    }

    fun setSavedTrackVisibility(track: SavedTrack, visible: Boolean): SavedTrack? {
        val idx = savedTracks.indexOfFirst { it.id == track.id }
        if (idx < 0) return null
        val updated = savedTracks[idx].copy(visible = visible)
        savedTracks[idx] = updated
        if (!visible && focusedSavedTrack?.id == track.id) {
            focusedSavedTrack = null
            focusedSavedTrackNonce++
        }
        UserContentStore.saveSavedTracks(context, savedTracks.toList())
        return updated
    }

    fun toggleSavedTrackVisibility(track: SavedTrack) {
        setSavedTrackVisibility(track, !track.visible)
    }

    fun renameSavedTrack(track: SavedTrack, newName: String): Boolean {
        val idx = savedTracks.indexOfFirst { it.id == track.id }
        if (idx < 0) return false
        val cleaned = newName.trim()
        if (cleaned.isBlank()) return false
        val updated = savedTracks[idx].copy(name = cleaned)
        savedTracks[idx] = updated
        if (focusedSavedTrack?.id == track.id) {
            focusedSavedTrack = updated
            focusedSavedTrackNonce++
        }
        UserContentStore.saveSavedTracks(context, savedTracks.toList())
        return true
    }

    fun setImportedLayerVisibility(layer: ImportedLayer, visible: Boolean): ImportedLayer? {
        val idx = importedLayers.indexOfFirst { it.id == layer.id }
        if (idx < 0) return null
        val updated = importedLayers[idx].copy(visible = visible)
        importedLayers[idx] = updated
        UserContentStore.saveImportedLayers(context, importedLayers.toList())
        return updated
    }

    fun toggleImportedLayerVisibility(layer: ImportedLayer) {
        setImportedLayerVisibility(layer, !layer.visible)
    }

    fun renameImportedLayer(layer: ImportedLayer, newName: String): Boolean {
        val idx = importedLayers.indexOfFirst { it.id == layer.id }
        if (idx < 0) return false
        val cleaned = newName.trim()
        if (cleaned.isBlank()) return false
        importedLayers[idx] = importedLayers[idx].copy(name = cleaned)
        UserContentStore.saveImportedLayers(context, importedLayers.toList())
        return true
    }

    fun deleteImportedLayer(layer: ImportedLayer) {
        importedLayers.removeAll { it.id == layer.id }
        UserContentStore.saveImportedLayers(context, importedLayers.toList())
    }

    fun hideAllOverlays() {
        markedPoints.indices.forEach { idx ->
            if (markedPoints[idx].visible) markedPoints[idx] = markedPoints[idx].copy(visible = false)
        }
        savedTracks.indices.forEach { idx ->
            if (savedTracks[idx].visible) savedTracks[idx] = savedTracks[idx].copy(visible = false)
        }
        importedLayers.indices.forEach { idx ->
            if (importedLayers[idx].visible) importedLayers[idx] = importedLayers[idx].copy(visible = false)
        }
        UserContentStore.saveMarkedPoints(context, markedPoints.toList())
        UserContentStore.saveSavedTracks(context, savedTracks.toList())
        UserContentStore.saveImportedLayers(context, importedLayers.toList())
        OfflineTileManager.clearAllCustomOverlays(context)
        hideUserContentOnMap = true
        focusedSavedTrack = null
        focusedSavedTrackNonce++
        selectedMarkedPointForActions = null
        selectedImportedPointForActions = null
        navigationTarget = null
        rulerStartPoint = null
        rulerEndPoint = null
        drawingModeEnabled = false
        Toast.makeText(context, "Svi overlayi su ugašeni. Ništa nije obrisano.", Toast.LENGTH_LONG).show()
    }

    fun clearAllUserContent() {
        hideAllOverlays()
        if (!liveTrackingEnabled) {
            liveTrackPoints = emptyList()
        }
        drawingPoints = emptyList()
    }

    fun openImportedLayerOnMap(layer: ImportedLayer) {
        pauseUserAutoFocusForManualMapTarget()
        setImportedLayerVisibility(layer, true)
        val bounds = importedBounds(layer) ?: return
        importedFocusPoint = GeoPoint((bounds.first.latitude + bounds.second.latitude)/2.0, (bounds.first.longitude + bounds.second.longitude)/2.0)
        importedFocusZoom = boundsToZoom(bounds.first, bounds.second)
        importedFocusNonce++
        navigateTo(AppTab.MAP)
    }


    fun nearestHumanPlace(loc: GeoPoint?): String? {
        loc ?: return null
        return effectiveRecords.asSequence()
            .filter { it.location.lat != null && it.location.lon != null }
            .mapNotNull { rec ->
                val lat = rec.location.lat ?: return@mapNotNull null
                val lon = rec.location.lon ?: return@mapNotNull null
                val place = rec.location.locality ?: rec.location.nearest_place ?: rec.location.municipality ?: return@mapNotNull null
                val result = FloatArray(1)
                Location.distanceBetween(loc.latitude, loc.longitude, lat, lon, result)
                place to (result.firstOrNull() ?: Float.MAX_VALUE)
            }
            .minByOrNull { it.second }
            ?.first
    }

    fun suggestWaypointName(loc: GeoPoint): String {
        return "Waypoint"
    }

    fun openMarkPositionDialog(atPoint: GeoPoint? = null) {
        val loc = atPoint ?: currentUserLocation
        if (loc == null) {
            Toast.makeText(context, "Prvo učitaj GPS poziciju ili zadrži prst na karti", Toast.LENGTH_LONG).show()
            return
        }
        editingMarkId = null
        markDraftId = "mark_" + System.currentTimeMillis()
        markName = suggestWaypointName(loc)
        markType = "ostalo"
        markDescription = ""
        markBaseLat = loc.latitude
        markBaseLon = loc.longitude
        showMarkDialog = true
    }

    fun currentDraftMarkedPoint(): MarkedPoint {
        val conv = CoordinateConverter.wgs84ToHtrs96Tm(markBaseLat, markBaseLon)
        return MarkedPoint(
            id = markDraftId,
            name = markName.ifBlank { "Točka ${System.currentTimeMillis()}" },
            type = markType,
            description = markDescription,
            lat = markBaseLat,
            lon = markBaseLon,
            htrsX = conv.x,
            htrsY = conv.y,
            visible = true
        )
    }

    fun shareMarkedCoordinates(point: MarkedPoint) {
        val text = buildString {
            appendLine(point.name)
            appendLine("Tip: ${point.type}")
            appendLine("WGS84: ${"%.6f".format(Locale.US, point.lat)}, ${"%.6f".format(Locale.US, point.lon)}")
            appendLine("HTRS96/TM X: ${"%.2f".format(Locale.US, point.htrsX)}")
            appendLine("HTRS96/TM Y: ${"%.2f".format(Locale.US, point.htrsY)}")
            if (point.description.isNotBlank()) appendLine("Opis: ${point.description}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Export koordinata"))
    }

    DisposableEffect(Unit) {
        onDispose { stopPositionUpdates(clearState = false) }
    }

    val currentRoute = navBackStackEntry?.destination?.route
    val isOnSubRoute = currentRoute in listOf(
        SovAppRoutes.GPS_STATUS,
        SovAppRoutes.COMPASS_STATUS,
        SovAppRoutes.SIGNAL_STATUS
    )
    BackHandler(enabled = currentTab != AppTab.HOME && !isOnSubRoute) {
        // Field UX rule: one Android Back from any SOV screen returns to Home.
        // A second Back on Home is handled by the system and exits the app.
        // Sub-routes (GPS/Compass/Signal status) handle their own Back via onBack = { navController.popBackStack() }.
        navigateTo(AppTab.HOME)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    suspend fun runUpdateCheck(manual: Boolean) {
        if (updateCheckInProgress || updateInstallInProgress) return
        updateCheckInProgress = true
        when (val result = AppUpdateManager.checkForUpdate(context)) {
            is UpdateCheckResult.Available -> updateDialogInfo = result.info
            is UpdateCheckResult.UpToDate -> if (manual) snackbarHostState.showSnackbar("Već imaš zadnju verziju (${result.currentVersionName}).")
            is UpdateCheckResult.Error -> if (manual) snackbarHostState.showSnackbar(result.message)
        }
        updateCheckInProgress = false
    }

    fun startAppUpdate(info: AppUpdateInfo) {
        if (updateCheckInProgress || updateInstallInProgress) return
        launcherScope.launch {
            if (!AppUpdateManager.canRequestPackageInstalls(context)) {
                AppUpdateManager.openUnknownSourcesSettings(context)
                snackbarHostState.showSnackbar("Dopusti instalaciju iz ovog izvora pa zatim opet klikni Ažuriraj.")
                return@launch
            }
            updateInstallInProgress = true
            val result = AppUpdateManager.downloadApk(context, info)
            updateInstallInProgress = false
            result.fold(
                onSuccess = { file ->
                    val launched = runCatching {
                        AppUpdateManager.launchApkInstaller(context, file)
                    }.isSuccess
                    if (!launched) {
                        snackbarHostState.showSnackbar("Download je gotov, ali instalacija se nije otvorila.")
                    }
                },
                onFailure = {
                    snackbarHostState.showSnackbar("Greška pri downloadu ažuriranja.")
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!startupUpdateCheckDone) {
            startupUpdateCheckDone = true
            delay(1600)
            runUpdateCheck(manual = false)
        }
    }

    CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
    Scaffold(
        topBar = {
            if (currentTab == AppTab.CALCULATOR) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_sov),
                                contentDescription = "SoV logo",
                                modifier = Modifier.size(40.dp)
                            )
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("SOV")
                                Text(
                                    "${effectiveRecords.size} / ${uiState.allRecords.size} objekata",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Preuzmi KML",
                                tint = premiumIconTint("download export")
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentTab == AppTab.SEARCH) {
                SovBottomNavigationBar(
                    currentTab = currentTab,
                    onSearch = { navigateTo(AppTab.SEARCH) },
                    onMap = { openMapsTk25() },
                    onFieldPackages = { navigateTo(AppTab.FIELD_PACKAGES) },
                    onTools = { navigateTo(AppTab.TOOLS) },
                    onOffline = { navigateTo(AppTab.OFFLINE) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp).widthIn(min = 260.dp, max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(uiState.loadingMessage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(progress = { uiState.loadingProgress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "${(uiState.loadingProgress.coerceIn(0f, 1f) * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(innerPadding)) {
                if (importBusy) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 18.dp)
                            .zIndex(5f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.2.dp)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(importProgressTitle, fontWeight = FontWeight.SemiBold)
                                Text(importProgressDetail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (importCanCancel) {
                                TextButton(onClick = { importCancelRequested.set(true) }) {
                                    Text("Prekini")
                                }
                            }
                        }
                    }
                }
                CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
                NavHost(
                    navController = navController,
                    startDestination = AppTab.HOME.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(AppTab.HOME.route) {
                        HomeScreen(
                        onOpen = { tab ->
                            if (tab == AppTab.MAP) openMapsTk25() else navigateTo(tab)
                        },
                        onCheckForUpdates = { launcherScope.launch { runUpdateCheck(manual = true) } },
                        updateCheckInProgress = updateCheckInProgress
                    )
                    }
                    composable(AppTab.SEARCH.route) {
                        SearchScreen(
                        filters = uiState.filters,
                        records = effectiveRecords,
                        recentSearchQueries = remember(initialSession) { initialSession.recentSearchQueries },
                        savedSearchPresets = remember(initialSession) { initialSession.savedSearchPresets },
                        onSavePreset = { name, filterState ->
                            launcherScope.launch(Dispatchers.IO) {
                                val current = AppSessionStore.load(context)
                                val updated = (listOf(SearchPreset(name, filterState)) +
                                    current.savedSearchPresets.filterNot { it.name == name }).take(5)
                                AppSessionStore.save(context, current.copy(savedSearchPresets = updated))
                            }
                        },
                        onDeletePreset = { preset ->
                            launcherScope.launch(Dispatchers.IO) {
                                val current = AppSessionStore.load(context)
                                AppSessionStore.save(context, current.copy(
                                    savedSearchPresets = current.savedSearchPresets.filterNot { it.name == preset.name }
                                ))
                            }
                        },
                        locationOptions = uiState.locationOptions,
                        hasCurrentUserLocation = currentUserLocation != null,
                        isFiltering = uiState.isFiltering,
                        searchReady = uiState.isSearchIndexReady,
                        onFiltersChanged = { newFilters ->
                            viewModel.updateFilters(newFilters)
                            if (newFilters.query.length >= 2 && uiState.filteredRecords.isNotEmpty()) {
                                launcherScope.launch(Dispatchers.IO) {
                                    AppSessionStore.addRecentSearchQuery(context, newFilters.query)
                                }
                            }
                        },
                        onSelect = viewModel::selectRecord,
                        onViewOnMap = { record ->
                            focusRecordOnMap(record)
                        },
                        onRequestGpsLocation = ::requestSearchGpsFix,
                        onShowFilteredOnMap = {
                            clearFieldPackageMapContext()
                            focusedSavedTrack = null
                            mapFocusRecord = null
                            autoCenterOnUserEnabled = false
                            val targetRecords = if (uiState.filters.hasAnyActiveCriteria()) uiState.filteredRecords else effectiveRecords
                            val mapCapableRecords = targetRecords.mapNotNull { rec ->
                                validSearchGeoPoint(rec.location.lat, rec.location.lon)?.let { point -> rec.id to point }
                            }
                            searchFocusPoints = mapCapableRecords.map { it.second }
                            searchFocusRecordIds = mapCapableRecords.mapTo(mutableSetOf()) { it.first }
                            searchFocusNonce++
                            navigateTo(AppTab.MAP)
                        },
                        onExportKml = {
                            exportMode = "all"
                            showExportDialog = true
                        },
                        onClearSearchFocus = { clearAllSearchState() }
                    )
                    }
                    composable(AppTab.MAP.route) {
                        DisposableEffect(Unit) {
                            onDispose { clearFieldPackageMapContext() }
                        }
                        MapTabScreen(
                        records = when {
                            activeFieldPackage != null -> filterRecordsForFieldPackage(uiState.allRecords, activeFieldPackage!!)
                            else -> effectiveRecords
                        },
                        nearRecords = uiState.allRecords,
                        fieldPackageModeName = activeFieldPackage?.name,
                        activeFieldPackageBounds = uiState.filters.boundingBoxFilter,
                        filters = uiState.filters,
                        locationOptions = uiState.locationOptions,
                        hasCurrentUserLocation = currentUserLocation != null,
                        isFiltering = uiState.isFiltering,
                        searchReady = uiState.isSearchIndexReady,
                        focusRecord = mapFocusRecord,
                        focusedSavedTrack = if (hideUserContentOnMap) null else focusedSavedTrack,
                        focusedSavedTrackNonce = focusedSavedTrackNonce,
                        onSelect = viewModel::selectRecord,
                        onFiltersChanged = viewModel::updateFilters,
                        onClearFieldPackageBounds = { clearFieldPackageMapContext() },
                        onViewOnMap = { record ->
                            pauseUserAutoFocusForManualMapTarget()
                            focusRecordOnMap(record)
                        },
                        onRequestGpsLocation = ::requestSearchGpsFix,
                        offlineStateVersion = offlineStateVersion,
                        mapLayerStateVersion = mapLayerStateVersion,
                        currentUserLocation = currentUserLocation,
                        currentLocationAccuracyM = currentLocationAccuracyM,
                        currentLocationAltitudeM = currentLocationAltitudeM,
                        currentLocationProvider = currentLocationProvider,
                        currentLocationSpeedMps = currentLocationSpeedMps,
                        currentGpsBearingDeg = currentGpsBearingDeg,
                        currentGpsBearingAccuracyDeg = currentGpsBearingAccuracyDeg,
                        waitingForGpsFix = waitingForGpsFix,
                        mapOrientationMode = mapOrientationMode,
                        centerOnUserNonce = centerOnUserNonce,
                        autoCenterOnUserEnabled = autoCenterOnUserEnabled,
                        positionEnabled = positionEnabled,
                        liveTrackingEnabled = liveTrackingEnabled,
                        trackingSessionNonce = trackingSessionNonce,
                        currentTrackStartedAtMillis = trackingRuntimeState.startedAtMillis,
                        trackPoints = if (hideUserContentOnMap && !liveTrackingEnabled) emptyList() else liveTrackPoints,
                        markedPoints = if (hideUserContentOnMap) emptyList() else activeFieldPackage?.let { filterMarkedPointsForFieldPackage(markedPoints, it).map { point -> point.copy(visible = true) } } ?: markedPoints,
                        savedTracks = if (hideUserContentOnMap) emptyList() else activeFieldPackage?.let { filterSavedTracksForFieldPackage(savedTracks, it).map { track -> track.copy(visible = true) } } ?: savedTracks,
                        importedLayers = if (hideUserContentOnMap) {
                            emptyList<ImportedLayer>()
                        } else {
                            activeFieldPackage?.let { pkg -> filterImportedLayersForFieldPackage(importedLayers.toList(), pkg) } ?: importedLayers.toList()
                        },
                        simplePointViewEnabled = simplePointViewEnabled,
                        offlineFocusPoint = offlineFocusPoint,
                        offlineFocusZoom = offlineFocusZoom,
                        offlineFocusNonce = offlineFocusNonce,
                        importedFocusPoint = importedFocusPoint,
                        importedFocusZoom = importedFocusZoom,
                        importedFocusNonce = importedFocusNonce,
                        searchFocusPoints = searchFocusPoints,
                        searchFocusRecordIds = searchFocusRecordIds,
                        searchFocusNonce = searchFocusNonce,
                        persistedMapCenter = persistedMapCenter,
                        persistedMapZoom = persistedMapZoom,
                        onMapCameraChanged = { center, zoom ->
                            persistedMapCenter = center
                            persistedMapZoom = zoom
                            mapViewModel.updateCamera(center, zoom)
                        },
                        onCenterOnUser = {
                            if (currentUserLocation != null) {
                                centerOnUserNonce++
                            } else {
                                requestSearchGpsFix()
                            }
                        },
                        onToggleGps = { triggerMyLocation() },
                        onToggleAutoCenterOnUser = { toggleAutoCenterOnUser() },
                        onToggleLiveTracking = { toggleLiveTracking(it) },
                        onSaveStoppedTrack = { openTrackSaveDialog() },
                        onToggleSimplePointView = { simplePointViewEnabled = !simplePointViewEnabled },
                        onMarkPosition = {
                            hideUserContentOnMap = false
                            openMarkPositionDialog()
                        },
                        onMarkPositionAtPoint = { point ->
                            hideUserContentOnMap = false
                            openMarkPositionDialog(point)
                        },
                        onFocusPoint = { point ->
                            offlineFocusPoint = point
                            offlineFocusZoom = 15.0
                            offlineFocusNonce++
                        },
                        onToggleOrientationMode = {
                            mapOrientationMode = when (mapOrientationMode) {
                                MapOrientationMode.NORTH_UP -> MapOrientationMode.HEADING_UP
                                MapOrientationMode.HEADING_UP -> MapOrientationMode.STATIC
                                MapOrientationMode.STATIC -> MapOrientationMode.NORTH_UP
                            }
                        },
                        onEditMarkedPoint = { point ->
                            hideUserContentOnMap = false
                            offlineFocusPoint = GeoPoint(point.lat, point.lon)
                            offlineFocusZoom = 16.0
                            offlineFocusNonce++
                            openExistingMarkDialog(point)
                        },
                        onShowMarkedPointActions = { point ->
                            hideUserContentOnMap = false
                            selectedMarkedPointForActions = point
                        },
                        onShowImportedPointActions = { point ->
                            hideUserContentOnMap = false
                            selectedImportedPointForActions = point
                        },
                        navigationTarget = navigationTarget,
                        onClearNavigationTarget = { navigationTarget = null },
                        rulerStartPoint = rulerStartPoint,
                        rulerEndPoint = rulerEndPoint,
                        rulerModeEnabled = rulerModeEnabled,
                        onToggleRulerMode = {
                            rulerModeEnabled = !rulerModeEnabled
                            if (!rulerModeEnabled) {
                                rulerStartPoint = null
                                rulerEndPoint = null
                            }
                        },
                        onSetRulerPoints = { start, end ->
                            rulerStartPoint = start
                            rulerEndPoint = end
                        },
                        onNavigate = { navigateTo(it) },
                        onShowExport = {
                            exportMode = "all"
                            showExportDialog = true
                        },
                        onOfflineDownloaded = { center, zoom ->
                            mapFocusRecord = null
                            offlineFocusPoint = center
                            offlineFocusZoom = zoom
                            offlineFocusNonce++
                            offlineStateVersion++
                            mapLayerStateVersion++
                            if (returnToFieldPackageFlowAfterDownload) {
                                returnToFieldPackageFlowAfterDownload = false
                                clearAllSearchState()
                                resumeFieldPackageWizardNonce++
                                resumeFieldPackageWizardPending = true
                                navigateTo(AppTab.FIELD_PACKAGES)
                                Toast.makeText(context, "Offline karta je spremljena. Vraćam te u flow izleta.", Toast.LENGTH_LONG).show()
                            } else {
                                openMapsTk25()
                            }
                        },
                        onImport = { importLauncher.launch(arrayOf("*/*")) },
                        onOpenOfflineMenu = { navigateTo(AppTab.OFFLINE) },
                        drawingModeEnabled = drawingModeEnabled,
                        drawingPoints = drawingPoints,
                        drawingStrokeWidthDp = drawingStrokeWidthDp,
                        drawingSmoothEnabled = drawingSmoothEnabled,
                        onToggleDrawingMode = {
                            if (drawingModeEnabled) {
                                drawingModeEnabled = false
                                drawingPoints = emptyList()
                            } else {
                                drawingModeEnabled = true
                            }
                        },
                        onFinishDrawingMode = {
                            drawingModeEnabled = false
                            drawingPoints = emptyList()
                        },
                        onUndoDrawingPoint = {
                            if (drawingPoints.isNotEmpty()) drawingPoints = drawingPoints.dropLast(1)
                        },
                        onClearDrawing = {
                            drawingPoints = emptyList()
                            drawingModeEnabled = false
                        },
                        onSaveDrawing = { openDrawingSaveDialog() },
                        onShareDrawing = {
                            val shareTrackItem = SavedTrack(
                                id = "draw_share_" + System.currentTimeMillis(),
                                name = if (drawingName.isBlank()) "crtana_ruta" else drawingName,
                                description = drawingDescription,
                                points = drawingPoints.map { TrackPoint(it, null) }
                            )
                            shareTrack(context, shareTrackItem)
                        },
                        onToggleDrawingSmoothing = {
                            drawingSmoothEnabled = !drawingSmoothEnabled
                        },
                        onSetDrawingStrokeWidthDp = { width ->
                            drawingStrokeWidthDp = width.coerceIn(4, 10)
                        },
                        onMapTapForDrawing = { point ->
                            drawingPoints = drawingPoints + point
                        },
                        startAreaSelectionNonce = startOfflineAreaSelectionNonce,
                        cancelAreaSelectionNonce = cancelOfflineAreaSelectionNonce,
                        onClearAllUserContent = { clearAllUserContent() }
                    )
                    }
                    composable(AppTab.CLOUD.route) {
                        CloudScreen(
                            onOpenTrips = { navigateTo(AppTab.FIELD_PACKAGES) },
                            onOpenEquipment = { navController.navigate(SovAppRoutes.ORUZARSTVO) },
                            onOpenArchive = { navController.navigate(SovAppRoutes.ARHIVA_NACRTI) }
                        )
                    }
                    composable(AppTab.TOOLS.route) {
                        ToolsScreen(
                        onOpenCalculator = { navigateTo(AppTab.CALCULATOR) },
                        onOpenSpeleoRunner = { navigateTo(AppTab.SPELEO_RUNNER) },
                        onOpenSpeleoZapisnik = { openExternalUrl(SPELEO_ZAPISNIK_URL) },
                        onOpenCalendar = { openExternalUrl(CALENDAR_URL) },
                        onOpenMembership = { openExternalUrl(MEMBERSHIP_URL) },
                        onOpenEquipment = { navController.navigate(SovAppRoutes.ORUZARSTVO) },
                        onOpenArchive = { navController.navigate(SovAppRoutes.ARHIVA_NACRTI) }
                    )
                    }
                    composable(SovAppRoutes.ORUZARSTVO) { EquipmentReadOnlyScreen(onBack = { navController.popBackStack() }) }
                    composable(SovAppRoutes.ARHIVA_NACRTI) {
                        ArchiveDrawingsReadOnlyScreen(
                            onBack = { navController.popBackStack() },
                            onOpenOnMap = { item -> focusArchiveItemOnMap(item) }
                        )
                    }
                    composable(AppTab.SETTINGS.route) { SettingsScreen(
                        language = appLanguage,
                        onLanguageChange = { language ->
                            appLanguage = language
                            persistSession { it.copy(appLanguage = language) }
                        },
                        onOpenGpsStatus = { navController.navigate(SovAppRoutes.GPS_STATUS) },
                        onOpenCompassStatus = { navController.navigate(SovAppRoutes.COMPASS_STATUS) },
                        onOpenSignalStatus = { navController.navigate(SovAppRoutes.SIGNAL_STATUS) },
                        myBaseSummary = uiState.myBaseSummary,
                        myBaseMessage = uiState.myBaseMessage,
                        onImportMyBaseKml = { uri -> viewModel.importMyBaseKml(uri) },
                        onClearMyBase = { viewModel.clearMyBase() },
                        onMyBaseMessageShown = { viewModel.consumeMyBaseMessage() }
                    ) }
                    composable(SovAppRoutes.GPS_STATUS) { GpsStatusScreen(onBack = { navController.popBackStack() }) }
                    composable(SovAppRoutes.COMPASS_STATUS) { CompassStatusScreen(onBack = { navController.popBackStack() }) }
                    composable(SovAppRoutes.SIGNAL_STATUS) { SignalStatusScreen(onBack = { navController.popBackStack() }) }
                    composable(AppTab.CALCULATOR.route) { CoordinateCalculatorScreen() }
                    composable(AppTab.SPELEO_RUNNER.route) { SpeleoRunnerScreen() }
                    composable(AppTab.FIELD_PACKAGES.route) {
                        FieldPackagesScreen(
                        records = uiState.allRecords,
                        markedPoints = markedPoints,
                        savedTracks = savedTracks,
                        importedLayers = importedLayers,
                        currentUserLocation = currentUserLocation,
                        resumeCreateWizardNonce = if (resumeFieldPackageWizardPending) resumeFieldPackageWizardNonce else 0,
                        onResumeCreateWizardConsumed = { resumeFieldPackageWizardPending = false },
                        onOpenPackageMap = { pkg ->
                            returnToFieldPackageFlowAfterDownload = false
                            cancelOfflineAreaSelectionNonce++
                            activateFieldPackageMapContext(pkg)
                            val openMapPlan = FieldPackageActionController.buildOpenMapPlan(
                                context = context,
                                pkg = pkg,
                                records = uiState.allRecords,
                                markedPoints = markedPoints,
                                savedTracks = savedTracks
                            )
                            searchFocusPoints = openMapPlan.focusPoints
                            searchFocusRecordIds = emptySet()
                            searchFocusNonce++
                            openMapPlan.focusCenter?.let { center ->
                                offlineFocusPoint = center
                                offlineFocusZoom = openMapPlan.focusZoom ?: offlineFocusZoom
                                offlineFocusNonce++
                            }
                            if (openMapPlan.shouldEnableOfflineMap && openMapPlan.offlineMapName != null) {
                                OfflineTileManager.setCustomOverlayEnabled(context, openMapPlan.offlineMapName, true)
                                MapLayerPrefs.setMode(context, MapLayerMode.OFFLINE)
                                offlineStateVersion++
                                mapLayerStateVersion++
                            }
                            hideUserContentOnMap = false
                            focusedSavedTrack = null
                            navigateTo(AppTab.MAP)
                            Toast.makeText(context, "Otvaram područje izleta: ${pkg.name}", Toast.LENGTH_SHORT).show()
                        },
                        onRequestGpsLocation = {
                            requestSearchGpsFix()
                            Toast.makeText(context, "Tražim GPS lokaciju…", Toast.LENGTH_SHORT).show()
                        },
                        onFindAreaOnMap = {
                            clearFieldPackageMapContext()
                            cancelOfflineAreaSelectionNonce++
                            returnToFieldPackageFlowAfterDownload = true
                            startOfflineAreaSelectionNonce++
                            navigateTo(AppTab.MAP)
                            Toast.makeText(context, "Odaberi područje za download na karti, pa spremi. Vraćam te u izlet automatski.", Toast.LENGTH_LONG).show()
                        },
                        onChanged = {
                            markedPoints.clear()
                            markedPoints.addAll(UserContentStore.loadMarkedPoints(context))
                            savedTracks.clear()
                            savedTracks.addAll(UserContentStore.loadSavedTracks(context))
                            offlineStateVersion++
                            mapLayerStateVersion++
                        }
                    )
                    }
                    composable(AppTab.OFFLINE.route) {
                        OfflineMapsScreen(
                        markedPoints = markedPoints,
                        savedTracks = savedTracks,
                        importedLayers = importedLayers,
                        onChanged = {
                            offlineStateVersion++
                            mapLayerStateVersion++
                        },
                        onOpenPoint = { point ->
                            clearFieldPackageMapContext()
                            hideUserContentOnMap = false
                            pauseUserAutoFocusForManualMapTarget()
                            focusedSavedTrack = null
                            setMarkedPointVisibility(point, true)
                            openMapsTk25()
                            offlineFocusPoint = GeoPoint(point.lat, point.lon)
                            offlineFocusZoom = 16.0
                            offlineFocusNonce++
                        },
                        onEditPoint = { point ->
                            hideUserContentOnMap = false
                            openExistingMarkDialog(point)
                        },
                        onTogglePointVisibility = { point ->
                            hideUserContentOnMap = false
                            toggleMarkedPointVisibility(point)
                        },
                        onExportPoint = { point ->
                            pendingMarkExportId = point.id
                            markExportLauncher.launch((point.name.ifBlank { "tocka" }).replace(" ", "_") + ".kml")
                        },
                        onSharePoint = { point -> shareMarkedPoint(context, point) },
                        onDeletePoint = { point -> deleteMarkedPoint(point) },
                        onOpenTrack = { track ->
                            hideUserContentOnMap = false
                            pauseUserAutoFocusForManualMapTarget()
                            if (track.points.isNotEmpty()) {
                                val updatedTrack = setSavedTrackVisibility(track, true) ?: track
                                focusedSavedTrack = updatedTrack
                                focusedSavedTrackNonce++
                                mapFocusRecord = null
                                openMapsTk25()
                            }
                        },
                        onExportTrack = { track ->
                            pendingTrackExportId = track.id
                            trackExportLauncher.launch((track.name.ifBlank { "track" }).replace(" ", "_") + ".gpx")
                        },
                        onExportTrackKml = { track ->
                            pendingTrackKmlExportId = track.id
                            trackKmlExportLauncher.launch((track.name.ifBlank { "track" }).replace(" ", "_") + ".kml")
                        },
                        onExportTrackToDarkoOs = { track ->
                            launcherScope.launch {
                                val result = DarkoOsTrackSyncClient.exportTrack(track)
                                if (result.ok) {
                                    Toast.makeText(context, "Track exportan u Darko OS", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Darko OS export nije uspio: ${result.error ?: "nepoznata greška"}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onShareTrack = { track -> shareTrack(context, track) },
                        onToggleTrackVisibility = { track ->
                            hideUserContentOnMap = false
                            toggleSavedTrackVisibility(track)
                        },
                        onRenameTrack = { track, newName -> renameSavedTrack(track, newName) },
                        onDeleteTrack = { track -> deleteSavedTrack(track) },
                        onOpenImportedLayer = { layer ->
                            hideUserContentOnMap = false
                            pauseUserAutoFocusForManualMapTarget()
                            focusedSavedTrack = null
                            openImportedLayerOnMap(layer)
                        },
                        onToggleImportedLayerVisibility = { layer ->
                            hideUserContentOnMap = false
                            toggleImportedLayerVisibility(layer)
                        },
                        onRenameImportedLayer = { layer, newName -> renameImportedLayer(layer, newName) },
                        onDeleteImportedLayer = { layer -> deleteImportedLayer(layer) },
                        onShareImportedLayer = { layer -> shareImportedLayer(context, layer) },
                        onDeleteCustomMap = { mapName -> OfflineTileManager.deleteCustomMap(context, mapName) },
                        onRenameCustomMap = { oldName, newName -> OfflineTileManager.renameCustomMap(context, oldName, newName).isSuccess },
                        onShareOfflineMap = { mapName -> shareOfflineMap(context, mapName) },
                        onShareOfflineMapImage = { mapName -> shareOfflineMapPrintImage(context, mapName) },
                        onShareCustomMap = { mapName -> shareCustomMap(context, mapName) },
                        onShareCustomMapImage = { mapName -> shareCustomMapPrintImage(context, mapName) },
                        onHideAllOverlays = { hideAllOverlays() },
                        onImport = { importLauncher.launch(arrayOf("*/*")) },
                        onOpenOfflineMap = { mapName, center, zoom ->
                            hideUserContentOnMap = false
                            pauseUserAutoFocusForManualMapTarget()
                            OfflineTileManager.setActiveMapName(context, mapName)
                            MapLayerPrefs.setMode(context, MapLayerMode.OFFLINE)
                            navigateTo(AppTab.MAP)
                            offlineFocusPoint = center
                            offlineFocusZoom = zoom
                            offlineFocusNonce++
                            offlineStateVersion++
                            mapLayerStateVersion++
                        },
                        sharedLayers = sharedLayers,
                        sharedLayersLoading = sharedLayersLoading,
                        onRefreshSharedLayers = { launcherScope.launch { refreshSharedLayers() } },
                        onShowSharedLayerOnMap = { entry ->
                            val layer = SharedLayersSyncClient.toImportedLayer(entry)
                            if (layer != null) {
                                importedLayers.removeAll { it.id == layer.id }
                                importedLayers.add(0, layer.copy(visible = true))
                                UserContentStore.saveImportedLayers(context, importedLayers.toList())
                                hideUserContentOnMap = false
                                openImportedLayerOnMap(layer.copy(visible = true))
                                offlineStateVersion++
                                mapLayerStateVersion++
                            }
                        },
                        onToggleSharedLayerVisibility = { entry ->
                            val layer = SharedLayersSyncClient.toImportedLayer(entry)
                            if (layer != null) {
                                val existingIndex = importedLayers.indexOfFirst { it.id == layer.id }
                                if (existingIndex >= 0) {
                                    val currentlyVisible = importedLayers[existingIndex].visible
                                    importedLayers[existingIndex] = importedLayers[existingIndex].copy(visible = !currentlyVisible)
                                    if (!currentlyVisible) hideUserContentOnMap = false
                                } else {
                                    importedLayers.add(0, layer.copy(visible = true))
                                    hideUserContentOnMap = false
                                }
                                UserContentStore.saveImportedLayers(context, importedLayers.toList())
                                offlineStateVersion++
                                mapLayerStateVersion++
                            }
                        },
                        onDownloadSharedLayer = { entry ->
                            launcherScope.launch {
                                val layer = SharedLayersSyncClient.toImportedLayer(entry) ?: return@launch
                                withContext(Dispatchers.IO) {
                                    val content = when (entry.type) {
                                        "point" -> layer.points.firstOrNull()?.let { pt ->
                                            KmlExporter.toMarkedPointsKml(listOf(pt))
                                        }
                                        "track" -> layer.tracks.firstOrNull()?.let { tr ->
                                            if (entry.subtype == "gpx") trackToGpx(tr) else KmlExporter.toTrackKml(tr)
                                        }
                                        else -> null
                                    }
                                    content?.let { text ->
                                        val fileName = entry.name.replace(" ", "_").replace("/", "-") +
                                            if (entry.subtype == "gpx") ".gpx" else ".kml"
                                        val file = java.io.File(context.cacheDir, fileName)
                                        file.writeText(text, Charsets.UTF_8)
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            context.packageName + ".fileprovider",
                                            file
                                        )
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, if (entry.subtype == "gpx") "application/gpx+xml" else "application/vnd.google-earth.kml+xml")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        withContext(Dispatchers.Main) {
                                            context.startActivity(android.content.Intent.createChooser(intent, "Otvori ${entry.name}"))
                                        }
                                    }
                                }
                            }
                        },
                        onShareSharedLayerEntry = { entry ->
                            launcherScope.launch {
                                withContext(Dispatchers.IO) {
                                    val layer = SharedLayersSyncClient.toImportedLayer(entry) ?: return@withContext
                                    val content = when (entry.type) {
                                        "point" -> layer.points.firstOrNull()?.let { KmlExporter.toMarkedPointsKml(listOf(it)) }
                                        "track" -> layer.tracks.firstOrNull()?.let { if (entry.subtype == "gpx") trackToGpx(it) else KmlExporter.toTrackKml(it) }
                                        else -> null
                                    } ?: return@withContext
                                    val ext = if (entry.subtype == "gpx") ".gpx" else ".kml"
                                    val file = java.io.File(context.cacheDir, entry.name.replace(" ", "_") + ext)
                                    file.writeText(content, Charsets.UTF_8)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context, context.packageName + ".fileprovider", file
                                    )
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = if (entry.subtype == "gpx") "application/gpx+xml" else "application/vnd.google-earth.kml+xml"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Zajednički sloj iz SOV: ${entry.name}")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    withContext(Dispatchers.Main) {
                                        context.startActivity(android.content.Intent.createChooser(intent, "Podijeli ${entry.name}"))
                                    }
                                }
                            }
                        },
                        onDeleteSharedLayer = { entry ->
                            launcherScope.launch {
                                val ok = SharedLayersSyncClient.deleteLayer(entry.id)
                                if (ok) {
                                    sharedLayers = sharedLayers.filterNot { it.id == entry.id }
                                    Toast.makeText(context, "Sloj '${entry.name}' obrisan.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Greška pri brisanju — provjeri internet.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onAddPointToShared = { point ->
                            pendingSharePoint = point
                            showSharePointDialog = true
                        },
                        onAddTrackToShared = { track ->
                            pendingShareTrack = track
                            showShareTrackDialog = true
                        }
                    )
                    }
                }
            }
        }
    }

    if (showSharePointDialog && pendingSharePoint != null) {
        SharedLayerUploadDialog(
            title = "Podijeli točku s grupom",
            defaultName = pendingSharePoint!!.name,
            defaultDesc = pendingSharePoint!!.description,
            onDismiss = { showSharePointDialog = false; pendingSharePoint = null },
            onConfirm = { name, desc, tags ->
                val pt = pendingSharePoint!!
                showSharePointDialog = false
                pendingSharePoint = null
                launcherScope.launch {
                    val ok = SharedLayersSyncClient.addPoint(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name, author = "SOV",
                        description = desc, tags = tags,
                        lat = pt.lat, lon = pt.lon, alt = null
                    )
                    Toast.makeText(context,
                        if (ok) "✓ Točka '$name' podijeljena s grupom!" else "Greška — provjeri internet.",
                        Toast.LENGTH_LONG).show()
                    if (ok) refreshSharedLayers()
                }
            }
        )
    }

    if (showShareTrackDialog && pendingShareTrack != null) {
        SharedLayerUploadDialog(
            title = "Podijeli track s grupom",
            defaultName = pendingShareTrack!!.name,
            defaultDesc = pendingShareTrack!!.description,
            onDismiss = { showShareTrackDialog = false; pendingShareTrack = null },
            onConfirm = { name, desc, tags ->
                val tr = pendingShareTrack!!
                showShareTrackDialog = false
                pendingShareTrack = null
                launcherScope.launch {
                    val ok = SharedLayersSyncClient.addTrack(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name, author = "SOV",
                        description = desc, tags = tags,
                        points = tr.points
                    )
                    Toast.makeText(context,
                        if (ok) "✓ Track '$name' podijeljen s grupom!" else "Greška — provjeri internet.",
                        Toast.LENGTH_LONG).show()
                    if (ok) refreshSharedLayers()
                }
            }
        )
    }

    if (showExportDialog) {
        val allCount = effectiveRecords.count { it.location.lat != null && it.location.lon != null }
        val notInCadastreCount = effectiveRecords.count {
            it.location.lat != null && it.location.lon != null && it.cadastre.not_in_cadastre_candidate == true
        }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Preuzmi KML") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Odaberi što želiš skinuti iz trenutnih rezultata pretrage.")
                    Text(
                        "KML format možeš otvoriti u Google Maps, Google Earth ili bilo kojem GIS programu (QGIS, OruxMaps...).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            exportMode = "all"
                            showExportDialog = false
                            exportLauncher.launch("sov_sve_pronadjene.kml")
                        },
                        enabled = allCount > 0
                    ) { Text("Download sve pronađene ($allCount)") }
                    TextButton(
                        onClick = {
                            exportMode = "not_in_cadastre"
                            showExportDialog = false
                            exportLauncher.launch("sov_za_provjeru.kml")
                        },
                        enabled = notInCadastreCount > 0
                    ) { Text("Download samo zapisi za provjeru ($notInCadastreCount)") }
                    TextButton(
                        onClick = {
                            exportMode = "custom_points"
                            showExportDialog = false
                            exportLauncher.launch("sov_moje_custom_tocke.kml")
                        },
                        enabled = markedPoints.isNotEmpty()
                    ) { Text("Download moje custom KML točke (${markedPoints.size})") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("Odustani") } }
        )
    }

    if (showMarkDialog) {
        val markPhotos = remember(markDraftId, photoStateVersion) { if (markDraftId.isBlank()) emptyList() else PhotoStore.getPhotos(context, markDraftId) }
        val draftPoint = remember(markDraftId, markName, markType, markDescription, markBaseLat, markBaseLon) { currentDraftMarkedPoint() }
        AlertDialog(
            onDismissRequest = { showMarkDialog = false },
            title = { Text(if (editingMarkId == null) "Add waypoint" else "Edit point") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("GPS pozicija će biti spremljena kao žuta KML točka. Kasnije je možeš otvoriti i editirati iz Overlay taba ili klikom na marker.")
                    OutlinedTextField(value = markName, onValueChange = { markName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Ime točke") })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("jama", "spilja", "ostalo").forEach { option ->
                            FilterChip(selected = markType == option, onClick = { markType = option }, label = { Text(option) })
                        }
                    }
                    OutlinedTextField(value = markDescription, onValueChange = { markDescription = it }, modifier = Modifier.fillMaxWidth().height(190.dp), label = { Text("Opis") })
                    Text("WGS84: ${"%.6f".format(Locale.US, draftPoint.lat)}, ${"%.6f".format(Locale.US, draftPoint.lon)}", style = MaterialTheme.typography.bodySmall)
                    Text("HTRS96/TM: X ${"%.2f".format(Locale.US, draftPoint.htrsX)}  Y ${"%.2f".format(Locale.US, draftPoint.htrsY)}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            pendingPhotoRecordId = markDraftId
                            pendingPhotoRecordLabel = markName.ifBlank { markDraftId }
                            val pending = PhotoStore.createCameraPhoto(context, markDraftId, pendingPhotoRecordLabel)
                            pendingCameraPhoto = pending
                            val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) cameraLauncher.launch(pending.contentUri) else cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }) { Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.size(6.dp)); Text("Take photo") }
                        OutlinedButton(onClick = {
                            pendingPhotoRecordId = markDraftId
                            pendingPhotoRecordLabel = markName.ifBlank { markDraftId }
                            photoLauncher.launch(arrayOf("image/*"))
                        }) { Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.size(6.dp)); Text("Gallery") }
                    }
                    if (markPhotos.isNotEmpty()) {
                        Text("Fotografije: ${markPhotos.size}", style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            markPhotos.forEachIndexed { index, uriString ->
                                MarkDialogPhotoPreview(
                                    uriString = uriString,
                                    label = "Foto ${index + 1}",
                                    onOpen = { openUri(context, uriString) },
                                    onRemove = {
                                        PhotoStore.removePhoto(context, markDraftId, uriString)
                                        photoStateVersion++
                                    }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            upsertMarkedPoint(draftPoint)
                            shareMarkedCoordinates(draftPoint)
                        }) { Text("Export koordinata") }
                        OutlinedButton(onClick = {
                            upsertMarkedPoint(draftPoint)
                            pendingMarkExportId = draftPoint.id
                            markExportLauncher.launch((draftPoint.name.ifBlank { "tocka" }).replace(" ", "_") + ".kml")
                        }) { Text("Export KML") }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = {
                            upsertMarkedPoint(draftPoint)
                            showMarkDialog = false
                            openExternalUrl(SPELEO_ZAPISNIK_URL)
                        }) {
                            Icon(Icons.Default.Description, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Online zapisnik")
                        }
                        OutlinedButton(onClick = {
                            upsertMarkedPoint(draftPoint)
                            showMarkDialog = false
                            offlineZapisnikTarget = createOfflineZapisnikTarget(draftPoint, context)
                        }) {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Offline zapisnik")
                        }
                    }
                }
            },
            confirmButton = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingMarkId == null) {
                        TextButton(onClick = {
                            val pointToShare = draftPoint
                            showMarkDialog = false
                            editingMarkId = null
                            launcherScope.launch {
                                val ok = SharedLayersSyncClient.addPoint(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = pointToShare.name,
                                    author = "SOV",
                                    description = pointToShare.description,
                                    tags = pointToShare.type,
                                    lat = pointToShare.lat,
                                    lon = pointToShare.lon,
                                    alt = null
                                )
                                Toast.makeText(
                                    context,
                                    if (ok) "✓ Zajednička točka '${pointToShare.name}' spremljena u tablicu." else "Greška — zajednička točka nije spremljena. Provjeri internet i Apps Script deployment.",
                                    Toast.LENGTH_LONG
                                ).show()
                                if (ok) refreshSharedLayers()
                            }
                        }) { Text("Spremi zajedničku") }
                    }
                    TextButton(onClick = {
                        val isNewPoint = editingMarkId == null
                        upsertMarkedPoint(draftPoint)
                        showMarkDialog = false
                        openMapsTk25()
                        if (isNewPoint) {
                            selectedMarkedPointForActions = draftPoint
                        }
                        Toast.makeText(context, if (isNewPoint) "Točka spremljena" else "Točka ažurirana", Toast.LENGTH_SHORT).show()
                        editingMarkId = null
                    }) { Text(if (editingMarkId == null) "Spremi" else "Ažuriraj") }
                }
            },
            dismissButton = { TextButton(onClick = { showMarkDialog = false; editingMarkId = null }) { Text("Odustani") } }
        )
    }


    if (showTrackResumeDialog) {
        AlertDialog(
            onDismissRequest = {
                showTrackResumeDialog = false
                pendingTrackContinueExisting = null
                if (positionEnabled) startPositionUpdates(allowBootstrapLocation = false) else stopPositionUpdates(clearState = true)
            },
            title = { Text("Postojeći track") },
            text = {
                Text("Na karti već postoji zaustavljeni track. Želiš li nastaviti postojeći ili pokrenuti novi? Ako pokreneš novi, stari će se spremiti u spremljene trackove i maknuti s karte.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showTrackResumeDialog = false
                    pendingTrackContinueExisting = true
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        startLiveTracking(continueExistingTrack = true)
                        pendingTrackContinueExisting = null
                    } else {
                        locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) { Text("Nastavi postojeći") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTrackResumeDialog = false
                    val archived = movePausedTrackToOffline(clearOverlay = true)
                    if (archived != null) {
                        Toast.makeText(context, "Stari track spremljen u Offline", Toast.LENGTH_SHORT).show()
                    }
                    pendingTrackContinueExisting = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        startLiveTracking(continueExistingTrack = false)
                        pendingTrackContinueExisting = null
                    } else {
                        locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) { Text("Novi track") }
            }
        )
    }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text(appLanguage.pick("Pouzdan tracking", "Reliable tracking")) },
            text = {
                Text(
                    appLanguage.pick(
                        "Za snimanje traga dok je ekran ugašen, Android mora dopustiti SOV-u rad u pozadini.\n\n" +
                            "Pritisni Dopusti sada i potvrdi Androidov upit. Ne moraš ručno tražiti postavke.",
                        "To record tracks while the screen is off, Android must allow SOV to run in the background.\n\n" +
                            "Tap Allow now and confirm the Android prompt. You do not need to search through settings."
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    showBatteryDialog = false
                    requestBatteryOptimizationForTracking()
                }) { Text(appLanguage.pick("Dopusti sada", "Allow now")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val continueExisting = pendingBatteryTrackContinueExisting == true
                    pendingBatteryTrackContinueExisting = null
                    showBatteryDialog = false
                    startTrackingNow(continueExistingTrack = continueExisting)
                }) { Text(appLanguage.pick("Nastavi bez toga", "Continue without it")) }
            }
        )
    }

    if (showTrackSaveDialog) {
        val trackSaveStats = remember(liveTrackPoints) { computeTrackStats(liveTrackPoints) }
        val trackSaveDurationText = formatDuration(trackSaveStartedAtMillis, trackSaveEndedAtMillis)
        Dialog(onDismissRequest = {
            trackSaveStartedAtMillis = null
            trackSaveEndedAtMillis = null
            showTrackSaveDialog = false
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101827)),
                elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF172033),
                                    Color(0xFF0D1422)
                                )
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF1E40AF).copy(alpha = 0.26f), RoundedCornerShape(15.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(24.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Spremi track", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(
                                "Dodaj ime, opis i odaberi što želiš napraviti s tragom.",
                                color = Color.White.copy(alpha = 0.68f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = trackName,
                            onValueChange = { trackName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Ime tracka") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = trackDescription,
                            onValueChange = { trackDescription = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 112.dp, max = 150.dp),
                            label = { Text("Opis") }
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Sažetak tracka", color = Color.White, fontWeight = FontWeight.Bold)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AssistChip(onClick = {}, label = { Text("Trajanje: $trackSaveDurationText") })
                                    AssistChip(onClick = {}, label = { Text("Duljina: ${formatDistance(trackSaveStats.distanceM)}") })
                                    AssistChip(onClick = {}, label = { Text("↑ ${String.format(Locale.US, "%.0f", trackSaveStats.ascentM)} m") })
                                    AssistChip(onClick = {}, label = { Text("↓ ${String.format(Locale.US, "%.0f", trackSaveStats.descentM)} m") })
                                    AssistChip(onClick = {}, label = { Text("GPS: ${liveTrackPoints.size}") })
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                saveCurrentTrack()
                                showTrackSaveDialog = false
                                trackSaveStartedAtMillis = null
                                trackSaveEndedAtMillis = null
                                navigateTo(AppTab.OFFLINE)
                                Toast.makeText(context, "Track spremljen", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Spremi track", fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    val sharedTrack = SavedTrack(
                                        id = "shared_track_draft_" + System.currentTimeMillis(),
                                        name = trackName.ifBlank { "Track ${savedTracks.size + 1}" },
                                        description = trackDescription,
                                        points = liveTrackPoints
                                    )
                                    showTrackSaveDialog = false
                                    trackSaveStartedAtMillis = null
                                    trackSaveEndedAtMillis = null
                                    launcherScope.launch {
                                        val ok = SharedLayersSyncClient.addTrack(
                                            id = java.util.UUID.randomUUID().toString(),
                                            name = sharedTrack.name,
                                            author = "SOV",
                                            description = sharedTrack.description,
                                            tags = "track",
                                            points = sharedTrack.points
                                        )
                                        Toast.makeText(
                                            context,
                                            if (ok) "✓ Zajednički trag '${sharedTrack.name}' spremljen u tablicu." else "Greška — zajednički trag nije spremljen. Provjeri internet i Apps Script deployment.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        if (ok) {
                                            liveTrackPoints = emptyList()
                                            TrackingRuntime.stopSession(keepPoints = false)
                                            TrackingRuntime.clearPoints()
                                            refreshSharedLayers()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Zajednički", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    val saved = saveCurrentTrack()
                                    pendingTrackExportId = saved.id
                                    showTrackSaveDialog = false
                                    trackSaveStartedAtMillis = null
                                    trackSaveEndedAtMillis = null
                                    navigateTo(AppTab.OFFLINE)
                                    trackExportLauncher.launch((saved.name.ifBlank { "track" }).replace(" ", "_") + ".gpx")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export GPX", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = {
                                    trackSaveStartedAtMillis = null
                                    trackSaveEndedAtMillis = null
                                    showTrackSaveDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Kasnije") }
                            TextButton(
                                onClick = {
                                    TrackingRuntime.stopSession(keepPoints = false)
                                    TrackingRuntime.clearPoints()
                                    liveTrackPoints = emptyList()
                                    trackSaveStartedAtMillis = null
                                    trackSaveEndedAtMillis = null
                                    showTrackSaveDialog = false
                                    Toast.makeText(context, "Track obrisan", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Obriši")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDrawingSaveDialog) {
        AlertDialog(
            onDismissRequest = { showDrawingSaveDialog = false },
            title = { Text("Spremi ručno nacrtanu rutu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Crtež s karte spremit će se kao track i kasnije se može exportirati ili shareati kao GPX.")
                    OutlinedTextField(
                        value = drawingName,
                        onValueChange = { drawingName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ime rute") }
                    )
                    OutlinedTextField(
                        value = drawingDescription,
                        onValueChange = { drawingDescription = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        label = { Text("Opis") }
                    )
                    Divider()
                    Text("Točke crteža: ${drawingPoints.size}", style = MaterialTheme.typography.bodySmall)
                    if (drawingPoints.size >= 2) {
                        val drawingStats = remember(drawingPoints) { computeTrackStats(drawingPoints.map { TrackPoint(it, null) }) }
                        Text("Duljina: ${formatDistance(drawingStats.distanceM)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val saved = saveCurrentDrawing()
                        showDrawingSaveDialog = false
                        if (saved != null) {
                            navigateTo(AppTab.OFFLINE)
                            Toast.makeText(context, "Ruta spremljena", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Spremi") }
                    TextButton(onClick = {
                        val saved = saveCurrentDrawing()
                        showDrawingSaveDialog = false
                        if (saved != null) {
                            pendingTrackExportId = saved.id
                            navigateTo(AppTab.OFFLINE)
                            trackExportLauncher.launch((saved.name.ifBlank { "ruta" }).replace(" ", "_") + ".gpx")
                        }
                    }) { Text("Export GPX") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDrawingSaveDialog = false }) { Text("Kasnije") }
            }
        )
    }

    selectedMarkedPointForActions?.let { point ->
        AlertDialog(
            onDismissRequest = { selectedMarkedPointForActions = null },
            title = { Text(point.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val subtitle = listOf(point.type, point.description.takeIf { it.isNotBlank() })
                        .filterNotNull()
                        .joinToString(" • ")
                    if (subtitle.isNotBlank()) {
                        Text(subtitle)
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Koordinate", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format(Locale.US, "%.6f, %.6f", point.lat, point.lon),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        "Akcije",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DialogActionButton(
                                text = "Go to",
                                icon = Icons.Default.Navigation,
                                onClick = {
                                    setNavigationTarget(NavigationTarget(point.id, point.name, GeoPoint(point.lat, point.lon)))
                                    selectedMarkedPointForActions = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DialogActionButton(
                                text = "Open Gmaps",
                                icon = Icons.Default.OpenInNew,
                                onClick = {
                                    openGoogleMaps(context, point.lat, point.lon)
                                    selectedMarkedPointForActions = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DialogActionButton(
                                text = "Zapisnik",
                                icon = Icons.Default.Description,
                                onClick = {
                                    openExternalUrl(SPELEO_ZAPISNIK_URL)
                                    selectedMarkedPointForActions = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DialogActionButton(
                                text = "Offline zapisnik",
                                icon = Icons.Default.Save,
                                onClick = {
                                    offlineZapisnikTarget = createOfflineZapisnikTarget(point, context)
                                    selectedMarkedPointForActions = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DialogActionButton(
                                text = "Edit",
                                icon = Icons.Default.Edit,
                                onClick = {
                                    selectedMarkedPointForActions = null
                                    openExistingMarkDialog(point)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DialogActionButton(
                                text = "Obriši",
                                icon = Icons.Default.Delete,
                                onClick = {
                                    deleteMarkedPoint(point)
                                    Toast.makeText(context, "Waypoint obrisan", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMarkedPointForActions = null }) { Text("Zatvori") }
            }
        )
    }


    selectedImportedPointForActions?.let { point ->
        AlertDialog(
            onDismissRequest = { selectedImportedPointForActions = null },
            title = { Text(point.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(listOf(point.type, point.description.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" • "))
                    Text(String.format(Locale.US, "%.6f, %.6f", point.lat, point.lon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("HTRS96/TM X: ${"%.2f".format(Locale.US, point.htrsX)}  Y: ${"%.2f".format(Locale.US, point.htrsY)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionIconButton(
                        text = "Go to",
                        icon = Icons.Default.Navigation,
                        onClick = {
                            setNavigationTarget(NavigationTarget(point.id, point.name, GeoPoint(point.lat, point.lon)))
                            selectedImportedPointForActions = null
                        }
                    )
                    ActionIconButton(
                        text = "Open Gmaps",
                        icon = Icons.Default.OpenInNew,
                        onClick = {
                            openGoogleMaps(context, point.lat, point.lon)
                            selectedImportedPointForActions = null
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedImportedPointForActions = null }) { Text("Zatvori") }
            }
        )
    }

    offlineZapisnikTarget?.let { target ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { offlineZapisnikTarget = null },
            sheetState = sheetState
        ) {
            OfflineZapisnikSheet(
                target = target,
                onSaveDraft = { draft ->
                    OfflineZapisnikStore.saveDraft(context, target.storageKey, draft)
                },
                onShareDraft = { draft ->
                    OfflineZapisnikStore.saveDraft(context, target.storageKey, draft)
                    shareText(context, "Offline zapisnik - ${target.title}", buildOfflineZapisnikShareText(draft))
                },
                onOpenOnlineForm = {
                    openExternalUrl(SPELEO_ZAPISNIK_URL)
                }
            )
        }
    }

    uiState.selectedRecord?.let { record ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val photos = remember(record.id, photoStateVersion) { PhotoStore.getPhotos(context, record.id) }
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectRecord(null) },
            sheetState = sheetState
        ) {
            DetailSheet(
                record = record,
                photoUris = photos,
                onAddPhoto = {
                    pendingPhotoRecordId = record.id
                    pendingPhotoRecordLabel = record.name
                    photoLauncher.launch(arrayOf("image/*"))
                },
                onTakePhoto = {
                    pendingPhotoRecordId = record.id
                    pendingPhotoRecordLabel = record.name
                    val pending = PhotoStore.createCameraPhoto(context, record.id, record.name)
                    pendingCameraPhoto = pending
                    val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        cameraLauncher.launch(pending.contentUri)
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                onOpenPhoto = { uriString -> openUri(context, uriString) },
                onRemovePhoto = { uriString ->
                    PhotoStore.removePhoto(context, record.id, uriString)
                    photoStateVersion++
                },
                onGetTo = {
                    if (record.location.lat != null && record.location.lon != null) {
                        viewModel.selectRecord(null)
                        focusRecordOnMap(record, navigation = true)
                    }
                },
                onFillRecord = {
                    openExternalUrl(SPELEO_ZAPISNIK_URL)
                },
                onOpenOfflineZapisnik = {
                    if (record.classification.record_status == "nije_u_katastru" || record.cadastre.not_in_cadastre_candidate == true || record.cadastre.in_cadastre == false) {
                        offlineZapisnikTarget = createOfflineZapisnikTarget(record, context)
                    }
                },
                onShareRecord = { shareText(context, record.name, buildRecordShareText(record)) },
                onImportAttachmentToMap = { uri -> handleImportedUri(uri) },
                onOpenGeneratedLayerOnMap = { layer ->
                    viewModel.selectRecord(null)
                    importedLayers.removeAll { it.id == layer.id }
                    importedLayers.add(0, layer.copy(visible = true))
                    UserContentStore.saveImportedLayers(context, importedLayers.toList())
                    openImportedLayerOnMap(layer.copy(visible = true))
                }
            )
        }
    }
    if (importSummaryLayer != null) {
        val layer = importSummaryLayer!!
        AlertDialog(
            onDismissRequest = { importSummaryLayer = null },
            title = { Text("Import završen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(layer.name, fontWeight = FontWeight.SemiBold)
                    Text("Format: ${layer.type}")
                    Text("Točke: ${layer.points.size}")
                    Text("Linije / poligoni: ${layer.tracks.size}")
                    Text(
                        when {
                            layer.points.isNotEmpty() && layer.tracks.isNotEmpty() -> "Sloj je spreman za prikaz na karti i u OFFLINE tabu."
                            layer.points.isNotEmpty() -> "Uvezen je točkasti sloj spreman za prikaz na karti."
                            layer.tracks.isNotEmpty() -> "Uvezen je linijski sloj spreman za prikaz na karti."
                            else -> "Datoteka je učitana, ali nije pronađen nijedan iskoristiv objekt."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { importSummaryLayer = null }) {
                    Text("U redu")
                }
            }
        )
    }

    if (updateDialogInfo != null) {
        val info = updateDialogInfo!!
        AlertDialog(
            onDismissRequest = {
                if (!updateInstallInProgress) updateDialogInfo = null
            },
            title = { Text("Nova verzija dostupna") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Instalirana verzija: ${info.currentVersionName}")
                    Text("Dostupna verzija: ${info.latestVersionName}")
                    if (info.releaseNotes.isNotBlank()) {
                        Text(
                            info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (updateInstallInProgress) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Skidam APK i pripremam instalaciju…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { startAppUpdate(info) },
                    enabled = !updateInstallInProgress
                ) {
                    Text(if (updateInstallInProgress) "Skidam…" else "Ažuriraj")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { updateDialogInfo = null },
                    enabled = !updateInstallInProgress
                ) {
                    Text("Kasnije")
                }
            }
        )
    }
}
}

}

@Composable
private fun SharedLayerUploadDialog(
    title: String,
    defaultName: String,
    defaultDesc: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, tags: String) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    var desc by remember { mutableStateOf(defaultDesc) }
    var tags by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Sloj će biti vidljiv svim korisnicima SOV appa koji osvježe Zajedničke slojeve.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ime") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Opis (opcionalno)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tagovi, npr. velebit,ulaz (opcionalno)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.trim().isNotBlank(),
                onClick = { onConfirm(name.trim(), desc.trim(), tags.trim()) }
            ) { Text("Podijeli") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Odustani") }
        }
    )
}
