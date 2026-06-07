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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.darko.speleov1.util.SharedLayerEntry
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt
import java.util.concurrent.CancellationException

import java.util.concurrent.atomic.AtomicBoolean

data class StoredMapUiInfo(
    val tileCount: Int,
    val bounds: OfflineTileManager.OfflineBounds?,
    val isMbtiles: Boolean
)

private fun describeStoredMap(info: StoredMapUiInfo, preferMbtilesLabel: Boolean = false): String = buildString {
    append(if (preferMbtilesLabel || info.isMbtiles) "MBTiles" else "PNG tiles")
    if (info.tileCount > 0) append(" • tileova ${info.tileCount}")
    info.bounds?.let { append(" • ${formatBoundsSummary(it)}") }
}

private fun formatBoundsSummary(bounds: OfflineTileManager.OfflineBounds): String =
    "lat ${String.format(Locale.US, "%.3f", bounds.minLat)}–${String.format(Locale.US, "%.3f", bounds.maxLat)} • lon ${String.format(Locale.US, "%.3f", bounds.minLon)}–${String.format(Locale.US, "%.3f", bounds.maxLon)}"


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineMapsScreen(
    markedPoints: List<MarkedPoint>,
    savedTracks: List<SavedTrack>,
    importedLayers: List<ImportedLayer>,
    onChanged: () -> Unit,
    onOpenPoint: (MarkedPoint) -> Unit,
    onEditPoint: (MarkedPoint) -> Unit,
    onTogglePointVisibility: (MarkedPoint) -> Unit,
    onExportPoint: (MarkedPoint) -> Unit,
    onSharePoint: (MarkedPoint) -> Unit,
    onDeletePoint: (MarkedPoint) -> Unit,
    onOpenTrack: (SavedTrack) -> Unit,
    onExportTrack: (SavedTrack) -> Unit,
    onExportTrackKml: (SavedTrack) -> Unit,
    onExportTrackToDarkoOs: (SavedTrack) -> Unit = {},
    onShareTrack: (SavedTrack) -> Unit,
    onToggleTrackVisibility: (SavedTrack) -> Unit,
    onRenameTrack: (SavedTrack, String) -> Unit,
    onDeleteTrack: (SavedTrack) -> Unit,
    onOpenOfflineMap: (String, GeoPoint, Double) -> Unit,
    onShareOfflineMap: (String) -> Unit,
    onShareOfflineMapImage: (String) -> Unit,
    onOpenImportedLayer: (ImportedLayer) -> Unit,
    onToggleImportedLayerVisibility: (ImportedLayer) -> Unit,
    onRenameImportedLayer: (ImportedLayer, String) -> Unit,
    onDeleteImportedLayer: (ImportedLayer) -> Unit,
    onShareImportedLayer: (ImportedLayer) -> Unit,
    onDeleteCustomMap: (String) -> Unit,
    onRenameCustomMap: (String, String) -> Boolean,
    onShareCustomMap: (String) -> Unit,
    onShareCustomMapImage: (String) -> Unit,
    onHideAllOverlays: () -> Unit,
    onImport: () -> Unit,
    sharedLayers: List<SharedLayerEntry> = emptyList(),
    sharedLayersLoading: Boolean = false,
    onRefreshSharedLayers: () -> Unit = {},
    onShowSharedLayerOnMap: (SharedLayerEntry) -> Unit = {},
    onToggleSharedLayerVisibility: (SharedLayerEntry) -> Unit = {},
    onDownloadSharedLayer: (SharedLayerEntry) -> Unit = {},
    onShareSharedLayerEntry: (SharedLayerEntry) -> Unit = {},
    onDeleteSharedLayer: (SharedLayerEntry) -> Unit = {},
    onAddPointToShared: (MarkedPoint) -> Unit = {},
    onAddTrackToShared: (SavedTrack) -> Unit = {}
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val offlineActions = remember(context) { OfflineActionController(context) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            delay(3_000L)
            message = null
        }
    }
    var stateVersion by rememberSaveable { mutableIntStateOf(0) }
    var renameTarget by remember { mutableStateOf<LayerRenameTarget?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }
    var deleteConfirmTarget by remember { mutableStateOf<String?>(null) }
    var deleteConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val offlineSnapshot = remember(stateVersion, markedPoints, savedTracks, importedLayers) {
        offlineActions.snapshot(markedPoints, savedTracks, importedLayers)
    }
    var selectedOfflineMap by rememberSaveable { mutableStateOf(offlineSnapshot.activeMapName.orEmpty()) }
    val offlineMaps = offlineSnapshot.offlineMaps
    val customMaps = offlineSnapshot.customMaps
    val enabledCustomOverlays = offlineSnapshot.enabledCustomOverlays
    val hasOffline = offlineSnapshot.hasOffline
    val tileCount = offlineSnapshot.tileCount
    val selectedMode = offlineSnapshot.selectedMode
    val offlineMapInfoByName = remember(stateVersion, offlineMaps) {
        offlineMaps.associateWith { mapName -> offlineActions.mapInfo(mapName) }
    }
    val customMapInfoByName = remember(stateVersion, customMaps) {
        customMaps.associateWith { mapName -> offlineActions.mapInfo(mapName) }
    }
    var userLayerSearch by rememberSaveable { mutableStateOf("") }
    var offlineCategoriesCsv by rememberSaveable { mutableStateOf("maps") }
    val selectedOfflineCategories = remember(offlineCategoriesCsv) {
        offlineCategoriesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }
    fun toggleOfflineCategory(category: String) {
        val next = selectedOfflineCategories.toMutableSet()
        if (next.contains(category)) next.remove(category) else next.add(category)
        offlineCategoriesCsv = next.joinToString(",")
    }
    val normalizedUserLayerSearch = remember(userLayerSearch) { userLayerSearch.trim().lowercase(Locale.ROOT) }
    val filteredImportedLayers = remember(importedLayers, normalizedUserLayerSearch) {
        if (normalizedUserLayerSearch.isBlank()) importedLayers
        else importedLayers.filter { layer ->
            layer.name.lowercase(Locale.ROOT).contains(normalizedUserLayerSearch) ||
                layer.type.lowercase(Locale.ROOT).contains(normalizedUserLayerSearch) ||
                layer.points.any { point ->
                    point.name.lowercase(Locale.ROOT).contains(normalizedUserLayerSearch) ||
                        point.description.lowercase(Locale.ROOT).contains(normalizedUserLayerSearch)
                }
        }
    }
    val filteredMarkedPoints = remember(markedPoints, normalizedUserLayerSearch) {
        if (normalizedUserLayerSearch.isBlank()) markedPoints
        else markedPoints.filter { point ->
            point.name.lowercase(Locale.ROOT).contains(normalizedUserLayerSearch) ||
                point.type.lowercase(Locale.ROOT).contains(normalizedUserLayerSearch) ||
                point.description.lowercase(Locale.ROOT).contains(normalizedUserLayerSearch)
        }
    }
    val filteredSavedTracks = remember(savedTracks, normalizedUserLayerSearch) {
        if (normalizedUserLayerSearch.isBlank()) savedTracks
        else savedTracks.filter { track ->
            track.name.lowercase(Locale.ROOT).contains(normalizedUserLayerSearch) ||
                track.description.lowercase(Locale.ROOT).contains(normalizedUserLayerSearch)
        }
    }

    LaunchedEffect(Unit) {
        offlineActions.ensureFolders()
    }

    LaunchedEffect(stateVersion) {
        selectedOfflineMap = offlineSnapshot.activeMapName.orEmpty()
    }

    CaveScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        OfflineMapsHeroCard(
            activeMapName = selectedOfflineMap.ifBlank { "online / auto" },
            offlineCount = offlineMaps.size,
            mbtilesCount = customMaps.size,
            importedCount = importedLayers.size,
            visibleOverlayCount = enabledCustomOverlays.size + importedLayers.count { it.visible } + markedPoints.count { it.visible } + savedTracks.count { it.visible }
        )

        OfflineCategorySelector(
            selected = selectedOfflineCategories,
            onToggle = { toggleOfflineCategory(it) },
            offlineCount = offlineMaps.size,
            overlayCount = customMaps.size + enabledCustomOverlays.size,
            importCount = importedLayers.size,
            pointCount = markedPoints.size,
            trackCount = filteredSavedTracks.size,
            sharedCount = sharedLayers.size
        )

        if (offlineMaps.isEmpty() && customMaps.isEmpty() && importedLayers.isEmpty() && markedPoints.isEmpty() && savedTracks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(language.pick("Nemaš offline karata", "You have no offline maps"), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        language.pick("Klikni Import za uvoz MBTiles ili ZIP karte s mobitela, ili idi na Kartu → Tools → Download za preuzimanje offline karte direktno u aplikaciji.", "Tap Import to add MBTiles or ZIP maps from your phone, or go to Map → Tools → Download to download offline maps directly in the app."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onImport,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("Import karte", "Import map"))
                    }
                }
            }
        }

        OfflineQuickActionsCard(
            visibleOverlayCount = enabledCustomOverlays.size + importedLayers.count { it.visible } + markedPoints.count { it.visible } + savedTracks.count { it.visible },
            onHideAll = {
                onHideAllOverlays()
                offlineActions.clearCustomOverlays()
                stateVersion++
                onChanged()
                message = language.pick("Svi overlayi su ugašeni. Ništa nije obrisano.", "All overlays are hidden. Nothing was deleted.")
            },
            onOpenFolder = {
                val opened = openSovOfflineFolder(context)
                message = if (opened) language.pick("Otvaram Offline folder.", "Opening Offline folder.") else "Offline folder: ${OfflineTileManager.ensureOfflineFolderStructure(context).absolutePath}"
            },
            onImport = onImport
        )

        OutlinedTextField(
            value = userLayerSearch,
            onValueChange = { userLayerSearch = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text(language.pick("Pretraži", "Search")) },
            placeholder = { Text(language.pick("karta, layer, točka, track...", "map, layer, point, track...")) }
        )

        OfflineSectionCard(title = language.pick("Trenutno", "Current"), subtitle = null) {
            val activeLabel = when (selectedMode) {
                MapLayerMode.AUTO -> if (hasOffline) "Auto (offline → WMS)" else "Auto (WMS)"
                MapLayerMode.OFFLINE -> if (hasOffline) "Offline" else language.pick("Offline nije dostupna, koristit će se WMS", "Offline is not available, WMS will be used")
                MapLayerMode.OPENTOPO -> "OpenTopo"
                MapLayerMode.HGSS_SIGURNE_STAZE -> "HGSS SigurneStaze"
                MapLayerMode.HGSS_OSM_TEST -> "HGSS SigurneStaze"
                MapLayerMode.WMS -> "WMS"
            }
            val activeMapName = OfflineTileManager.getActiveMapName(context)
            val activeInfo = activeMapName?.let { offlineMapInfoByName[it] ?: customMapInfoByName[it] }
            OfflineStorageCard(
                title = activeLabel,
                subtitle = if (hasOffline && activeMapName != null) language.pick("Aktivna karta: $activeMapName", "Active map: $activeMapName") else language.pick("Offline karta nije spremljena.", "No offline map saved."),
                leadingIcon = if (activeInfo?.isMbtiles == true) Icons.Default.Storage else Icons.Default.Layers,
                status = when {
                    activeInfo?.isMbtiles == true -> "MBTiles"
                    hasOffline && activeMapName != null -> "PNG"
                    else -> "WMS"
                },
                highlighted = true,
                infoChips = buildList {
                    activeInfo?.let {
                        add((if (it.isMbtiles) Icons.Default.Storage else Icons.Default.Image) to if (it.isMbtiles) "MBTiles" else language.pick("PNG tileovi", "PNG tiles"))
                        if (it.tileCount > 0) add(Icons.Default.Map to language.pick("${it.tileCount} tileova", "${it.tileCount} tiles"))
                        it.bounds?.let { bounds -> add(Icons.Default.LocationOn to formatBoundsSummary(bounds)) }
                    }
                    if (enabledCustomOverlays.isNotEmpty()) {
                        add(Icons.Default.Visibility to language.pick("Overlayi ${enabledCustomOverlays.size}", "Overlays ${enabledCustomOverlays.size}"))
                    }
                }
            ) {}
        }

        if (selectedOfflineCategories.contains("maps") && offlineMaps.isNotEmpty()) {
            OfflineSectionCard(title = language.pick("Offline karte", "Offline maps"), subtitle = null) {
                offlineMaps.forEach { mapName ->
                    val mapInfo = offlineMapInfoByName[mapName] ?: StoredMapUiInfo(0, null, false)
                    OfflineStorageCard(
                        title = mapName,
                        subtitle = if (selectedOfflineMap == mapName) language.pick("Aktivna karta", "Active map") else language.pick("Spremljena offline karta", "Saved offline map"),
                        leadingIcon = Icons.Default.Map,
                        status = if (selectedOfflineMap == mapName) language.pick("Aktivna", "Active") else "PNG",
                        highlighted = selectedOfflineMap == mapName,
                        infoChips = buildList {
                            add(Icons.Default.Image to language.pick("PNG tileovi", "PNG tiles"))
                            if (mapInfo.tileCount > 0) add(Icons.Default.Storage to "${mapInfo.tileCount} tileova")
                            mapInfo.bounds?.let { bounds -> add(Icons.Default.LocationOn to formatBoundsSummary(bounds)) }
                        }
                    ) {
                        if (selectedOfflineMap == mapName) {
                            OfflineWideActionChip(
                                icon = Icons.Default.CheckCircle,
                                label = language.pick("Već aktivna ✓", "Already active ✓"),
                                onClick = {
                                    val bounds = mapInfo.bounds ?: OfflineTileManager.getOfflineBounds(context, mapName)
                                    val center = bounds?.let { GeoPoint((it.minLat + it.maxLat) / 2.0, (it.minLon + it.maxLon) / 2.0) }
                                    val zoom = OfflineTileManager.suggestedZoom(bounds)
                                    if (center != null) onOpenOfflineMap(mapName, center, zoom)
                                },
                                active = true
                            )
                        } else {
                            OfflineWideActionChip(
                                icon = Icons.Default.CheckCircle,
                                label = language.pick("Aktiviraj i prikaži", "Activate and show"),
                                onClick = {
                                    selectedOfflineMap = mapName
                                    offlineActions.setActiveMap(mapName)
                                    MapLayerPrefs.setMode(context, MapLayerMode.OFFLINE)
                                    stateVersion++
                                    onChanged()
                                    val bounds = mapInfo.bounds ?: OfflineTileManager.getOfflineBounds(context, mapName)
                                    val center = bounds?.let { GeoPoint((it.minLat + it.maxLat) / 2.0, (it.minLon + it.maxLon) / 2.0) }
                                    val zoom = OfflineTileManager.suggestedZoom(bounds)
                                    if (center != null) onOpenOfflineMap(mapName, center, zoom)
                                    else message = "Aktivna offline karta: $mapName"
                                }
                            )
                        }
                        OfflineWideActionChip(icon = Icons.Default.Share, label = "Share", onClick = { onShareOfflineMap(mapName) })
                        OfflineWideActionChip(icon = Icons.Default.Image, label = "PNG", onClick = { onShareOfflineMapImage(mapName) })
                        OfflineWideActionChip(icon = Icons.Default.Delete, label = language.pick("Obriši", "Delete"), onClick = {
                            val nextActiveMap = offlineActions.deleteOfflineMap(mapName)
                            if (selectedOfflineMap == mapName) {
                                selectedOfflineMap = nextActiveMap.orEmpty()
                            }
                            stateVersion++
                            onChanged()
                            message = "Offline karta obrisana: $mapName"
                        })
                    }
                }
            }
        }

        if (selectedOfflineCategories.contains("overlays") && customMaps.isNotEmpty()) {
            OfflineSectionCard(title = language.pick("MBTiles overlayi", "MBTiles overlays"), subtitle = null) {
                customMaps.forEach { mapName ->
                    val overlayEnabled = enabledCustomOverlays.contains(mapName)
                    val mapInfo = customMapInfoByName[mapName] ?: StoredMapUiInfo(0, null, true)
                    val activeAsBase = selectedOfflineMap == mapName && selectedMode == MapLayerMode.OFFLINE
                    OfflineStorageCard(
                        title = mapName,
                        subtitle = when {
                            activeAsBase && overlayEnabled -> language.pick("Bazna karta + overlay", "Base map + overlay")
                            activeAsBase -> language.pick("Trenutno bazna karta", "Current base map")
                            overlayEnabled -> language.pick("Overlay uključen", "Overlay on")
                            else -> language.pick("Overlay spreman", "Overlay ready")
                        },
                        leadingIcon = Icons.Default.Storage,
                        status = when {
                            activeAsBase && overlayEnabled -> language.pick("Baza + overlay", "Base + overlay")
                            activeAsBase -> language.pick("Bazna", "Base")
                            overlayEnabled -> "Overlay ON"
                            else -> "MBTiles"
                        },
                        highlighted = overlayEnabled || activeAsBase,
                        infoChips = buildList {
                            add(Icons.Default.Storage to "MBTiles")
                            if (mapInfo.tileCount > 0) add(Icons.Default.Map to language.pick("${mapInfo.tileCount} tileova", "${mapInfo.tileCount} tiles"))
                            mapInfo.bounds?.let { bounds -> add(Icons.Default.LocationOn to formatBoundsSummary(bounds)) }
                        }
                    ) {
                        OfflineWideActionChip(
                            icon = if (overlayEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            label = if (overlayEnabled) "Overlay ON" else "Overlay OFF",
                            active = overlayEnabled,
                            onClick = {
                                offlineActions.setCustomOverlayEnabled(mapName, !overlayEnabled)
                                stateVersion++
                                onChanged()
                                message = if (!overlayEnabled) "MBTiles overlay uključen: $mapName" else "MBTiles overlay isključen: $mapName"
                            }
                        )
                        OfflineWideActionChip(icon = Icons.Default.Map, label = "Na karti", onClick = {
                            val bounds = mapInfo.bounds ?: OfflineTileManager.getOfflineBounds(context, mapName)
                            val center = bounds?.let { GeoPoint((it.minLat + it.maxLat) / 2.0, (it.minLon + it.maxLon) / 2.0) }
                            val zoom = OfflineTileManager.suggestedZoom(bounds)
                            if (!overlayEnabled) {
                                offlineActions.enableCustomOverlay(mapName)
                                stateVersion++
                                onChanged()
                            }
                            if (center != null) onOpenOfflineMap(mapName, center, zoom) else message = "Ne mogu odrediti područje karte: $mapName"
                        })
                        OfflineWideActionChip(icon = Icons.Default.FolderOpen, label = "Kao baza", onClick = {
                            selectedOfflineMap = mapName
                            offlineActions.setActiveMap(mapName)
                            MapLayerPrefs.setMode(context, MapLayerMode.OFFLINE)
                            stateVersion++
                            onChanged()
                            message = "MBTiles postavljen kao bazna karta: $mapName"
                        })
                        OfflineWideActionChip(icon = Icons.Default.Edit, label = "Uredi ime", onClick = {
                            renameTarget = LayerRenameTarget(LayerRenameKind.CUSTOM_MBTILES, mapName)
                            renameValue = mapName
                        })
                        OfflineWideActionChip(icon = Icons.Default.Share, label = "Share", onClick = { onShareCustomMap(mapName) })
                        OfflineWideActionChip(icon = Icons.Default.Image, label = "PNG", onClick = { onShareCustomMapImage(mapName) })
                        OfflineWideActionChip(icon = Icons.Default.Delete, label = language.pick("Obriši", "Delete"), onClick = {
                            onDeleteCustomMap(mapName)
                            selectedOfflineMap = offlineActions.afterCustomMapDeleted(selectedOfflineMap, mapName).orEmpty()
                            stateVersion++
                            onChanged()
                            message = "Custom karta obrisana: $mapName"
                        })
                    }
                }
            }
        }

        if (selectedOfflineCategories.contains("imports")) {
        OfflineSectionCard(title = "Import KML / GPX", subtitle = null) {
            if (filteredImportedLayers.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                filteredImportedLayers.forEach { layer ->
                    OfflineStorageCard(
                        title = layer.name,
                        subtitle = if (layer.visible) "Layer prikazan na karti" else "Layer spremljen i spreman",
                        leadingIcon = Icons.Default.Layers,
                        status = layer.type,
                        highlighted = layer.visible,
                        infoChips = buildList {
                            add(Icons.Default.LocationOn to "Točaka ${layer.points.size}")
                            if (layer.tracks.isNotEmpty()) add(Icons.Default.Navigation to "Trackova ${layer.tracks.size}")
                        }
                    ) {
                        OfflineWideActionChip(
                            icon = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            label = if (layer.visible) "Sakrij" else "Prikaži",
                            active = layer.visible,
                            onClick = { onToggleImportedLayerVisibility(layer) }
                        )
                        OfflineWideActionChip(icon = Icons.Default.Map, label = "Na karti", onClick = { onOpenImportedLayer(layer) })
                        OfflineWideActionChip(icon = Icons.Default.Edit, label = "Uredi ime", onClick = {
                            renameTarget = LayerRenameTarget(LayerRenameKind.IMPORTED_LAYER, layer.id)
                            renameValue = layer.name
                        })
                        OfflineWideActionChip(icon = Icons.Default.Share, label = "Share", onClick = { onShareImportedLayer(layer) })
                        OfflineWideActionChip(icon = Icons.Default.Delete, label = language.pick("Obriši", "Delete"), onClick = {
                            deleteConfirmTarget = layer.name.ifBlank { "Layer" }
                            deleteConfirmAction = { onDeleteImportedLayer(layer); stateVersion++; onChanged() }
                        })
                    }
                }
            }
        }
        }

        if (deleteConfirmTarget != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmTarget = null; deleteConfirmAction = null },
                title = { Text("Obriši zauvijek?") },
                text = { Text("\"$deleteConfirmTarget\" bit će trajno obrisan. Ova radnja se ne može poništiti.") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteConfirmAction?.invoke()
                        deleteConfirmTarget = null
                        deleteConfirmAction = null
                    }) { Text("Obriši", color = Color(0xFFFF7D7D), fontWeight = FontWeight.SemiBold) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmTarget = null; deleteConfirmAction = null }) { Text("Odustani") }
                }
            )
        }

        renameTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { renameTarget = null },
                title = { Text("Uredi ime") },
                text = {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        label = { Text("Ime layera / tracka") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = renameValue.trim().isNotBlank(),
                        onClick = {
                            val cleaned = renameValue.trim()
                            if (cleaned.isBlank()) {
                                message = "Ime ne može biti prazno."
                                return@TextButton
                            }
                        val ok = when (target.kind) {
                            LayerRenameKind.CUSTOM_MBTILES -> onRenameCustomMap(target.id, cleaned)
                            LayerRenameKind.IMPORTED_LAYER -> {
                                importedLayers.firstOrNull { it.id == target.id }?.let { onRenameImportedLayer(it, cleaned); true } ?: false
                            }
                            LayerRenameKind.SAVED_TRACK -> {
                                savedTracks.firstOrNull { it.id == target.id }?.let { onRenameTrack(it, cleaned); true } ?: false
                            }
                        }
                        if (ok) {
                            stateVersion++
                            onChanged()
                            message = "Ime promijenjeno: $cleaned"
                            renameTarget = null
                        } else {
                            message = "Ne mogu promijeniti ime."
                        }
                    }) { Text("Spremi") }
                },
                dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Odustani") } }
            )
        }

        message?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(it, modifier = Modifier.padding(16.dp))
            }
        }

        if (normalizedUserLayerSearch.isNotBlank() && filteredImportedLayers.isEmpty() && filteredMarkedPoints.isEmpty() && filteredSavedTracks.isEmpty() && (importedLayers.isNotEmpty() || markedPoints.isNotEmpty() || savedTracks.isNotEmpty())) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) {
                Text("Nema pogodaka u tvojim kartama, layerima, točkama ili trackovima.", modifier = Modifier.padding(16.dp))
            }
        }

        if (selectedOfflineCategories.contains("shared")) {
            OfflineSectionCard(
                title = "Zajednički slojevi",
                subtitle = "Točke i trackovi koje su članovi podijelili s grupom."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (sharedLayersLoading) "Učitavam..." else "${sharedLayers.size} slojeva",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onRefreshSharedLayers,
                        shape = RoundedCornerShape(14.dp),
                        enabled = !sharedLayersLoading,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Osvježi", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (sharedLayersLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(8.dp)
                            .size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (sharedLayers.isEmpty()) {
                    Text(
                        "Nema zajedničkih slojeva. Podijeli svoju točku ili track klikom na 'Podijeli s grupom' u sekciji Moje točke ili Trackovi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    sharedLayers.forEach { entry ->
                        val sharedLocalLayerId = "shared_layer_${entry.id}"
                        val sharedVisibleOnMap = importedLayers.any { it.id == sharedLocalLayerId && it.visible }
                        OfflineStorageCard(
                            title = entry.name,
                            subtitle = "Autor: ${entry.author}${if (entry.tags.isNotBlank()) " • ${entry.tags}" else ""}",
                            leadingIcon = if (entry.type == "track") Icons.Default.Route else Icons.Default.Place,
                            status = if (entry.type == "track") "Track" else "Točka",
                            infoChips = buildList {
                                if (entry.description.isNotBlank())
                                    add(Icons.Default.Info to entry.description.take(60))
                            }
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OfflineWideActionChip(
                                    icon = if (sharedVisibleOnMap) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    label = if (sharedVisibleOnMap) "Sakrij" else "Prikaži",
                                    onClick = { onToggleSharedLayerVisibility(entry) }
                                )
                                OfflineWideActionChip(
                                    icon = Icons.Default.Map,
                                    label = "Otvori",
                                    onClick = { onShowSharedLayerOnMap(entry) }
                                )
                                OfflineWideActionChip(
                                    icon = Icons.Default.Download,
                                    label = if (entry.subtype == "gpx") "GPX" else "KML",
                                    onClick = { onDownloadSharedLayer(entry) }
                                )
                                OfflineWideActionChip(
                                    icon = Icons.Default.Share,
                                    label = "Share",
                                    onClick = { onShareSharedLayerEntry(entry) }
                                )
                                OfflineWideActionChip(
                                    icon = Icons.Default.Delete,
                                    label = language.pick("Obriši", "Delete"),
                                    onClick = {
                                        deleteConfirmTarget = entry.name
                                        deleteConfirmAction = {
                                            onDeleteSharedLayer(entry)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedOfflineCategories.contains("points") && filteredMarkedPoints.isNotEmpty()) {
            OfflineSectionCard(title = "Moje točke", subtitle = null) {
                filteredMarkedPoints.forEach { point ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(point.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${point.type} • ${String.format(Locale.US, "%.6f, %.6f", point.lat, point.lon)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (point.description.isNotBlank()) {
                                Text(point.description, style = MaterialTheme.typography.bodySmall)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OfflineIconActionButton(
                                    icon = if (point.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    label = if (point.visible) "Sakrij" else "Prikaži",
                                    active = point.visible,
                                    onClick = { onTogglePointVisibility(point) }
                                )
                                OfflineIconActionButton(icon = Icons.Default.Map, label = "Na karti", onClick = { onOpenPoint(point) })
                                OfflineIconActionButton(icon = Icons.Default.Edit, label = "Uredi", onClick = { onEditPoint(point) })
                                OfflineIconActionButton(icon = Icons.Default.OpenInNew, label = "GMaps", onClick = { openGoogleMaps(context, point.lat, point.lon) })
                                OfflineIconActionButton(icon = Icons.Default.Description, label = "Zapisnik", onClick = { openUri(context, SPELEO_ZAPISNIK_URL) })
                                OfflineIconActionButton(icon = Icons.Default.Download, label = "KML", onClick = { onExportPoint(point) })
                                OfflineIconActionButton(icon = Icons.Default.Share, label = "Share", onClick = { onSharePoint(point) })
                                OfflineIconActionButton(
                                    icon = Icons.Default.GroupAdd,
                                    label = "Podijeli s grupom",
                                    onClick = { onAddPointToShared(point) }
                                )
                                OfflineIconActionButton(icon = Icons.Default.Delete, label = language.pick("Obriši", "Delete"), onClick = {
                                    deleteConfirmTarget = point.name.ifBlank { "Točka" }
                                    deleteConfirmAction = { onDeletePoint(point); stateVersion++; onChanged() }
                                })
                            }
                        }
                    }
                }
            }
        }

        if (selectedOfflineCategories.contains("tracks") && filteredSavedTracks.isNotEmpty()) {
            OfflineSectionCard(title = "Trackovi", subtitle = null) {
                filteredSavedTracks.forEach { track ->
                    val stats = remember(track.id, track.points) { computeTrackStats(track.points) }
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(track.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Snimljeno: ${formatTrackDateTime(track.createdAtMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = FieldAmber
                            )
                            Text(
                                "GPS uzoraka: ${track.points.size} • ${(stats.distanceM / 1000.0).format1()} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Uspon: ${stats.ascentM.roundToInt()} m • Silazak: ${stats.descentM.roundToInt()} m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (track.description.isNotBlank()) {
                                Text(track.description, style = MaterialTheme.typography.bodySmall)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OfflineIconActionButton(
                                    icon = if (track.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    label = if (track.visible) "Sakrij" else "Prikaži",
                                    active = track.visible,
                                    onClick = { onToggleTrackVisibility(track) }
                                )
                                OfflineIconActionButton(icon = Icons.Default.Map, label = "Na karti", onClick = { onOpenTrack(track) })
                                OfflineIconActionButton(icon = Icons.Default.Edit, label = "Uredi ime", onClick = {
                                    renameTarget = LayerRenameTarget(LayerRenameKind.SAVED_TRACK, track.id)
                                    renameValue = track.name
                                })
                                OfflineIconActionButton(icon = Icons.Default.Download, label = "GPX", onClick = { onExportTrack(track) })
                                OfflineIconActionButton(icon = Icons.Default.Download, label = "KML", onClick = { onExportTrackKml(track) })
                                OfflineIconActionButton(icon = Icons.Default.UploadFile, label = "Darko OS", onClick = { onExportTrackToDarkoOs(track) })
                                OfflineIconActionButton(icon = Icons.Default.Share, label = "Share", onClick = { onShareTrack(track) })
                                OfflineIconActionButton(
                                    icon = Icons.Default.GroupAdd,
                                    label = "Podijeli s grupom",
                                    onClick = { onAddTrackToShared(track) }
                                )
                                OfflineIconActionButton(icon = Icons.Default.Delete, label = language.pick("Obriši", "Delete"), onClick = {
                                    deleteConfirmTarget = track.name.ifBlank { "Track" }
                                    deleteConfirmAction = { onDeleteTrack(track); stateVersion++; onChanged() }
                                })
                            }
                        }
                    }
                }
            }
        }
        }
    }
}


private data class LayerRenameTarget(val kind: LayerRenameKind, val id: String)
private enum class LayerRenameKind { CUSTOM_MBTILES, IMPORTED_LAYER, SAVED_TRACK }

private fun formatTrackDateTime(createdAtMillis: Long): String = runCatching {
    SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()).format(Date(createdAtMillis))
}.getOrDefault("nepoznato")

private val FieldInk = Color(0xFFEAF4EF)
private val FieldMuted = Color(0xFF96A79E)
private val FieldPanel = Color(0xFF0D1714)
private val FieldPanelSoft = Color(0xFF13211D)
private val FieldGreen = Color(0xFF7EE0A3)
private val FieldAmber = Color(0xFFE6C36A)
private val FieldRed = Color(0xFFFF7D7D)
private val FieldBlue = Color(0xFF86C5FF)
private val FieldDisabled = Color(0xFF8A958F)

@Composable
private fun FieldDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.07f))
    )
}

@Composable
private fun FieldStatusPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.13f),
        contentColor = accent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Text(
            text = text.uppercase(Locale.ROOT),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FieldStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.055f),
        contentColor = FieldInk,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label.uppercase(Locale.ROOT), color = FieldMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(value, color = accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}


@Composable
private fun OfflineCategorySelector(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    offlineCount: Int,
    overlayCount: Int,
    importCount: Int,
    pointCount: Int,
    trackCount: Int,
    sharedCount: Int
) {
    val items = listOf(
        OfflineCategoryUi("maps", "Maps", offlineCount, Icons.Default.Map, FieldBlue),
        OfflineCategoryUi("overlays", "MBTiles", overlayCount, Icons.Default.Storage, FieldAmber),
        OfflineCategoryUi("imports", "Imports", importCount, Icons.Default.UploadFile, Color(0xFFBFA7FF)),
        OfflineCategoryUi("points", "Waypoints", pointCount, Icons.Default.Place, FieldGreen),
        OfflineCategoryUi("tracks", "Tracks", trackCount, Icons.Default.Route, Color(0xFFFFB38A)),
        OfflineCategoryUi("shared", "Shared", sharedCount, Icons.Default.GroupAdd, Color(0xFF7FDBFF))
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FieldPanel.copy(alpha = 0.96f),
        contentColor = FieldInk,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowItems.forEach { item ->
                        OfflineCategoryTile(
                            item = item,
                            selected = selected.contains(item.key),
                            onClick = { onToggle(item.key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            if (selected.isEmpty()) {
                Text(
                    "Odaberi jednu ili više kategorija za prikaz.",
                    color = FieldMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private data class OfflineCategoryUi(
    val key: String,
    val label: String,
    val count: Int,
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun OfflineCategoryTile(
    item: OfflineCategoryUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) item.accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.045f),
        contentColor = if (selected) item.accent else FieldMuted,
        border = BorderStroke(1.dp, if (selected) item.accent.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(item.icon, null, modifier = Modifier.size(18.dp), tint = if (selected) item.accent else FieldMuted.copy(alpha = 0.82f))
                if (selected) Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(15.dp), tint = item.accent)
            }
            Text(item.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (selected) FieldInk else FieldMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.count.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selected) item.accent else FieldMuted.copy(alpha = 0.72f), maxLines = 1)
        }
    }
}


@Composable
private fun OfflineMapsHeroCard(
    activeMapName: String,
    offlineCount: Int,
    mbtilesCount: Int,
    importedCount: Int,
    visibleOverlayCount: Int
) {
    val language = LocalAppLanguage.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = FieldInk),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF17221E), Color(0xFF08100E), Color(0xFF111A17))
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = FieldGreen.copy(alpha = 0.14f),
                    contentColor = FieldGreen,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, FieldGreen.copy(alpha = 0.28f))
                ) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = FieldGreen, modifier = Modifier.size(28.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("FIELD LAYERS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = FieldGreen, letterSpacing = 1.2.sp)
                    Text(language.pick("Karte i slojevi", "Maps & Layers"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = FieldInk)
                    Text("Aktivno: $activeMapName", color = FieldMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FieldStatusPill(
                    text = if (visibleOverlayCount > 0) "$visibleOverlayCount ON" else "CLEAN",
                    accent = if (visibleOverlayCount > 0) FieldGreen else FieldMuted
                )
            }
            FieldDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldStat("Maps", offlineCount.toString(), FieldBlue, Modifier.weight(1f))
                FieldStat("MBTiles", mbtilesCount.toString(), FieldAmber, Modifier.weight(1f))
                FieldStat("Imports", importedCount.toString(), Color(0xFFBFA7FF), Modifier.weight(1f))
            }
        }
    }
}



@Composable
private fun OfflineQuickActionsCard(
    visibleOverlayCount: Int,
    onHideAll: () -> Unit,
    onOpenFolder: () -> Unit,
    onImport: () -> Unit
) {
    val language = LocalAppLanguage.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FieldPanel.copy(alpha = 0.97f),
        contentColor = FieldInk,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(language.pick("TERENSKA KONTROLA", "FIELD CONTROL"), fontWeight = FontWeight.Black, color = FieldInk, style = MaterialTheme.typography.titleSmall, letterSpacing = 0.8.sp)
                    Text(language.pick("Brze akcije za teren", "Quick field actions"), color = FieldMuted, style = MaterialTheme.typography.bodySmall)
                }
                FieldStatusPill(
                    text = if (visibleOverlayCount > 0) "$visibleOverlayCount visible" else "clear",
                    accent = if (visibleOverlayCount > 0) FieldGreen else FieldMuted
                )
            }
            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(language.pick("Import karte / layera", "Import map / layer"), fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OfflineWideActionChip(icon = Icons.Default.FolderOpen, label = "Folder", onClick = onOpenFolder, modifier = Modifier.weight(1f))
                OfflineWideActionChip(
                    icon = Icons.Default.VisibilityOff,
                    label = if (visibleOverlayCount > 0) language.pick("Sakrij sve", "Hide all") else language.pick("Sve sakriveno", "All hidden"),
                    onClick = onHideAll,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Download/SOV/Offline  ·  maps  ·  mbtiles  ·  gpx  ·  kml  ·  photos  ·  geojson/xml/tables",
                style = MaterialTheme.typography.labelMedium,
                color = FieldMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun openSovOfflineFolder(context: Context): Boolean {
    val folder = OfflineTileManager.syncPublicOfflineExports(context)

    // Android blocks app-private folders such as /Android/data/... on many
    // devices. Open the public mirror in Downloads instead:
    // Download/SOV/Offline/maps|mbtiles|gpx|kml|photos|tables|geojson|databases|rasters|packages.
    val documentsUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FSOV%2FOffline")
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(documentsUri, "vnd.android.document/directory")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    val opened = runCatching {
        context.startActivity(viewIntent)
        true
    }.getOrDefault(false)
    if (opened) {
        Toast.makeText(context, "SOV export folder: ${folder.absolutePath}", Toast.LENGTH_LONG).show()
        return true
    }

    return runCatching {
        val pickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            putExtra("android.content.extra.SHOW_ADVANCED", true)
            putExtra("android.provider.extra.INITIAL_URI", documentsUri)
        }
        context.startActivity(Intent.createChooser(pickerIntent, "Open SOV export folder"))
        Toast.makeText(context, "Otvori: Download/SOV/Offline", Toast.LENGTH_LONG).show()
        true
    }.getOrElse {
        Toast.makeText(context, "SOV export folder: ${folder.absolutePath}", Toast.LENGTH_LONG).show()
        false
    }
}

@Composable
private fun OfflineHeroMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.055f),
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}



@Composable
fun OfflineStorageCard(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector,
    status: String? = null,
    highlighted: Boolean = false,
    infoChips: List<Pair<ImageVector, String>> = emptyList(),
    actions: @Composable () -> Unit
) {
    val leadingTint = premiumIconTint(title, active = highlighted)
    val leadingContainer = if (highlighted) premiumIconContainer(title, active = true) else premiumIconContainer(title)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) FieldPanelSoft.copy(alpha = 0.98f) else FieldPanel.copy(alpha = 0.94f),
            contentColor = FieldInk
        ),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (highlighted) FieldGreen.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.07f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = leadingContainer
                ) {
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = leadingTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        color = FieldInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = FieldMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                status?.takeIf { it.isNotBlank() }?.let {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (highlighted) FieldGreen.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f)
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (highlighted) FieldGreen else FieldMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (infoChips.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    infoChips.forEach { (icon, text) ->
                        OfflineInfoChip(icon = icon, text = text)
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions()
            }
        }
    }
}

@Composable
fun OfflineInfoChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = FieldAmber)
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = FieldMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OfflineWideActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean? = null
) {
    val lower = label.lowercase(Locale.ROOT)
    val chipTint = when {
        active == true -> FieldGreen
        active == false -> FieldDisabled
        lower.contains("hide") || lower.contains("hidden") || lower.contains("sakrij") -> FieldRed
        lower.contains("import") || lower.contains("folder") || lower.contains("gpx") || lower.contains("kml") -> FieldAmber
        lower.contains("on") || lower.contains("prika") || lower.contains("aktiv") -> FieldGreen
        lower.contains("delete") || lower.contains("obri") -> FieldRed
        else -> FieldBlue
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 46.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = chipTint.copy(alpha = 0.10f),
            contentColor = chipTint
        ),
        border = BorderStroke(1.dp, chipTint.copy(alpha = 0.34f))
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp), tint = chipTint)
        Spacer(Modifier.width(8.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactLayerRow(
    title: String,
    subtitle: String,
    badge: String? = null,
    actions: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = FieldMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                badge?.takeIf { it.isNotBlank() }?.let {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions()
            }
        }
    }
}

@Composable
fun OfflineSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    val accent = offlineSectionAccent(title)
    val icon = offlineSectionIcon(title)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = accent.copy(alpha = 0.12f),
                contentColor = accent,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
            ) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title.uppercase(Locale.ROOT), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall, color = FieldInk, letterSpacing = 0.8.sp)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = FieldMuted)
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FieldPanel.copy(alpha = 0.45f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.045f))
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}



private fun offlineSectionAccent(title: String): Color = when {
    title.contains("Trenutno", ignoreCase = true) || title.contains("Current", ignoreCase = true) -> Color(0xFF72E0C4)
    title.contains("Offline", ignoreCase = true) || title.contains("Karte", ignoreCase = true) -> Color(0xFFC7A7FF)
    title.contains("MBTiles", ignoreCase = true) || title.contains("Overlay", ignoreCase = true) -> Color(0xFF8EC5FF)
    title.contains("KML", ignoreCase = true) || title.contains("GPX", ignoreCase = true) || title.contains("Import", ignoreCase = true) -> Color(0xFFFFC46B)
    title.contains("točke", ignoreCase = true) -> Color(0xFF9EE7D8)
    title.contains("track", ignoreCase = true) -> Color(0xFFFF9FB2)
    else -> Color(0xFFBFA7FF)
}

private fun offlineSectionIcon(title: String): ImageVector = when {
    title.contains("Trenutno", ignoreCase = true) || title.contains("Current", ignoreCase = true) -> Icons.Default.CheckCircle
    title.contains("Offline", ignoreCase = true) || title.contains("Karte", ignoreCase = true) -> Icons.Default.Map
    title.contains("MBTiles", ignoreCase = true) || title.contains("Overlay", ignoreCase = true) -> Icons.Default.Storage
    title.contains("KML", ignoreCase = true) || title.contains("GPX", ignoreCase = true) || title.contains("Import", ignoreCase = true) -> Icons.Default.Layers
    title.contains("točke", ignoreCase = true) -> Icons.Default.Place
    title.contains("track", ignoreCase = true) -> Icons.Default.Route
    else -> Icons.Default.FolderOpen
}



@Composable
fun OfflineIconActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean? = null
) {
    val iconTint = when (active) {
        true -> FieldGreen
        false -> FieldDisabled
        null -> premiumIconTint(label)
    }
    val containerTint = when (active) {
        true -> FieldGreen.copy(alpha = 0.14f)
        false -> FieldDisabled.copy(alpha = 0.10f)
        null -> premiumIconContainer(label)
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.42f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerTint,
            contentColor = iconTint
        )
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = iconTint)
    }
}

@Composable
fun OfflinePainterActionButton(
    drawableRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = premiumIconTint(label)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.42f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = premiumIconContainer(label),
            contentColor = iconTint
        )
    ) {
        Icon(
            painter = painterResource(id = drawableRes),
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
    }
}

@Composable
fun ActionIconButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionTint = premiumIconTint(text)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .widthIn(min = 140.dp)
            .heightIn(min = 54.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = premiumIconContainer(text)),
        border = BorderStroke(1.dp, actionTint.copy(alpha = 0.42f))
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp), tint = actionTint)
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DialogActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dialogTint = premiumIconTint(text)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 82.dp),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = premiumIconContainer(text),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, dialogTint.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp), tint = dialogTint)
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DialogActionChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dialogTint = premiumIconTint(text)
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        leadingIcon = {
            Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp), tint = dialogTint)
        },
        label = { Text(text) }
    )
}

@Composable
fun ActionPainterButton(
    text: String,
    drawableRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val painterTint = premiumIconTint(text)
    OutlinedButton(onClick = onClick, modifier = modifier, colors = ButtonDefaults.outlinedButtonColors(containerColor = premiumIconContainer(text))) {
        Icon(
            painter = painterResource(id = drawableRes),
            contentDescription = text,
            modifier = Modifier.size(18.dp),
            tint = painterTint
        )
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}
