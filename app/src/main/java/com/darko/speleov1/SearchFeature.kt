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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.darko.speleov1.model.SourceFilter
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
import com.darko.speleov1.util.SearchPreset
import com.darko.speleov1.util.SovPermissionsStore
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

internal val DEPTH_FILTER_OPTIONS: List<Pair<Int, String>> = listOf(
    50 to "50+ m",
    100 to "100+ m",
    200 to "200+ m",
    500 to "500+ m"
)

@Composable
fun SearchScreen(
    filters: FilterState,
    records: List<SpeleoRecord>,
    recentSearchQueries: List<String> = emptyList(),
    savedSearchPresets: List<SearchPreset> = emptyList(),
    onSavePreset: (String, FilterState) -> Unit = { _, _ -> },
    onDeletePreset: (SearchPreset) -> Unit = {},
    locationOptions: List<SearchLocationOption>,
    hasCurrentUserLocation: Boolean,
    isFiltering: Boolean,
    searchReady: Boolean,
    onFiltersChanged: (FilterState) -> Unit,
    onSelect: (SpeleoRecord) -> Unit,
    onViewOnMap: (SpeleoRecord) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onShowFilteredOnMap: () -> Unit,
    onExportKml: () -> Unit,
    onClearSearchFocus: () -> Unit = {}
) {
    val language = LocalAppLanguage.current
    val hasActiveSearch = remember(filters) { filters.hasAnyActiveCriteria() }
    val visibleRecords = remember(records, hasActiveSearch) {
        if (hasActiveSearch) records.take(MAX_VISIBLE_SEARCH_RESULTS) else emptyList()
    }

    CaveScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 96.dp)
        ) {
        item {
            FilterPanel(
                filters = filters,
                locationOptions = locationOptions,
                hasCurrentUserLocation = hasCurrentUserLocation,
                resultCount = records.size,
                isFiltering = isFiltering,
                searchReady = searchReady,
                onFiltersChanged = onFiltersChanged,
                onRequestGpsLocation = onRequestGpsLocation,
                onShowFilteredOnMap = onShowFilteredOnMap,
                onExportKml = onExportKml
            )
        }

        when {
            !searchReady -> {
                item {
                    SearchMessageCard(
                        title = language.pick("Pripremam pretragu", "Preparing search"),
                        body = "Karta je spremna odmah, a indeks za search i lokacije dovršava se u pozadini. Za trenutak će sve biti aktivno i glađe."
                    )
                }
            }
            !hasActiveSearch -> {
                item {
                    SearchMessageCard(
                        title = "Search je čist: prvo pojam, pa filteri",
                        body = "Kreni s nazivom, lokacijom ili presetom Terenski rad. Mapa se ne puni kaosom dok sam ne klikneš Prikaži na karti."
                    )
                }
            }
            isFiltering -> {
                item {
                    SearchMessageCard(
                        title = "Filtriram...",
                        body = "Obrađujem rezultate u pozadini da search ostane gladak i bez štekanja."
                    )
                }
            }
            records.isEmpty() -> {
                item {
                    SearchMessageCard(
                        title = "Nema rezultata",
                        body = "Probaj širi pojam, drugu lokaciju ili očisti dio filtera."
                    )
                }
            }
            else -> {
                itemsIndexed(
                    visibleRecords,
                    key = { index, record ->
                        "${record.id}|${record.source.orEmpty()}|${record.name}|${record.location.lat}|${record.location.lon}|$index"
                    }
                ) { _, record ->
                    RecordCard(record = record, onClick = { onSelect(record) }, onViewOnMap = { onViewOnMap(record) })
                }
                if (records.size > visibleRecords.size) {
                    item {
                        SearchMessageCard(
                            title = "Prikaz skraćen radi brzine",
                            body = "Prikazano je prvih ${visibleRecords.size} od ${records.size} rezultata da prvi veliki search ne zakoči mobitel. Suzi pretragu ili otvori kartu za prostorni prikaz."
                        )
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterPanel(
    filters: FilterState,
    locationOptions: List<SearchLocationOption>,
    hasCurrentUserLocation: Boolean,
    resultCount: Int,
    isFiltering: Boolean,
    searchReady: Boolean,
    onFiltersChanged: (FilterState) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onShowFilteredOnMap: () -> Unit,
    onExportKml: () -> Unit
) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val sovPermissions = remember { SovPermissionsStore.loadPermissions(context) }
    val canViewKatastar = sovPermissions.canViewKatastar
    var showAdvanced by rememberSaveable {
        mutableStateOf(
            filters.distanceFilterKm != null ||
                filters.depthMinM != null ||
                filters.onlyWithDescription ||
                filters.fieldTaskFilters.isNotEmpty()
        )
    }
    var distanceExpanded by rememberSaveable { mutableStateOf(false) }
    var queryDraft by rememberSaveable { mutableStateOf(filters.query) }
    val latestFilters by rememberUpdatedState(filters)
    val selectedDistanceLabel = remember(filters.distanceFilterKm) {
        DISTANCE_FILTER_OPTIONS.firstOrNull { it.first == filters.distanceFilterKm }?.second ?: "Sve udaljenosti"
    }
    val selectedSourceFilter: SourceFilter = filters.sourceFilter
    val selectedCaveType: CaveTypeFilter = filters.caveTypeFilter
    val hasActiveFilters = filters.hasAnyActiveCriteria()
    val activeFilterPills = remember(filters, selectedDistanceLabel) {
        buildSearchActiveFilterPills(filters, selectedDistanceLabel)
    }
    LaunchedEffect(canViewKatastar, filters.sourceFilter) {
        if (!canViewKatastar && filters.sourceFilter == SourceFilter.KATASTAR) {
            onFiltersChanged(filters.copy(sourceFilter = SourceFilter.SOV, cadastreFilter = CadastreFilter.ALL))
        }
    }
    val searchController = remember(hasCurrentUserLocation, onFiltersChanged, onRequestGpsLocation) {
        SearchFilterController(
            hasCurrentUserLocation = hasCurrentUserLocation,
            onFiltersChanged = onFiltersChanged,
            onRequestGpsLocation = onRequestGpsLocation
        )
    }
    val smartHint = remember(filters, resultCount, isFiltering, searchReady, hasCurrentUserLocation) {
        buildSearchSmartHint(filters, resultCount, isFiltering, searchReady, hasCurrentUserLocation)
    }

    fun updateFilters(next: FilterState) {
        onFiltersChanged(next)
    }

    fun resetAll() {
        queryDraft = ""
        searchController.resetAll()
    }

    fun setPresetFieldWork() {
        queryDraft = ""
        searchController.applyFieldWorkPreset()
    }

    fun setPresetNearby() {
        queryDraft = ""
        searchController.applyNearbyPreset()
    }

    LaunchedEffect(filters.query) {
        if (filters.query != queryDraft) queryDraft = filters.query
    }
    LaunchedEffect(queryDraft) {
        if (queryDraft != latestFilters.query) {
            delay(220)
            if (queryDraft != latestFilters.query) {
                onFiltersChanged(latestFilters.copy(query = queryDraft))
            }
        }
    }
    LaunchedEffect(filters.areaFilter) {
        if (filters.areaFilter.isNotBlank()) {
            onFiltersChanged(filters.copy(areaFilter = ""))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(28.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SearchSectionCard(
                title = "Baza",
                subtitle = if (canViewKatastar) "Unified role mode: SOV, Katastar, moja baza ili svi lokalni izvori." else "SOV, Moja baza i dozvoljeni izvori prema Supabase roli."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchSourceButton(
                            title = "SOV",
                            subtitle = "glavna baza",
                            selected = selectedSourceFilter == SourceFilter.SOV,
                            onClick = { updateFilters(filters.copy(sourceFilter = SourceFilter.SOV, cadastreFilter = CadastreFilter.ALL)) },
                            modifier = Modifier.weight(1f)
                        )
                        if (canViewKatastar) {
                            SearchSourceButton(
                                title = "Katastar",
                                subtitle = "role dopušteno",
                                selected = selectedSourceFilter == SourceFilter.KATASTAR,
                                onClick = { updateFilters(filters.copy(sourceFilter = SourceFilter.KATASTAR, cadastreFilter = CadastreFilter.ALL)) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            SearchSourceButton(
                                title = "Katastar",
                                subtitle = "nije dopušteno",
                                selected = false,
                                onClick = {},
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchSourceButton(
                            title = "Moja baza",
                            subtitle = "KML / CSV",
                            selected = selectedSourceFilter == SourceFilter.MY_BASE,
                            onClick = { updateFilters(filters.copy(sourceFilter = SourceFilter.MY_BASE, cadastreFilter = CadastreFilter.ALL)) },
                            modifier = Modifier.weight(1f)
                        )
                        SearchSourceButton(
                            title = "Sve",
                            subtitle = if (canViewKatastar) "SOV + Katastar + Moja" else "SOV + Moja baza",
                            selected = selectedSourceFilter == SourceFilter.ALL,
                            onClick = { updateFilters(filters.copy(sourceFilter = SourceFilter.ALL, cadastreFilter = CadastreFilter.ALL)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { resetAll() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text("Očisti sve filtere", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }


            SearchSectionCard(
                title = "Pretraga i rezultati",
                subtitle = "Upiši pojam i odmah vidi koliko ima rezultata."
            ) {
                OutlinedTextField(
                    value = queryDraft,
                    onValueChange = { queryDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Traži naziv, lokaciju ili broj pločice") },
                    placeholder = { Text("npr. Vražja jama, Ogulin, 124") },
                    supportingText = {
                        Text("Najbrže: upiši naziv, lokaciju ili broj pločice.")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )


                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(language.pick("Rezultati pretrage", "Search results"), fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (isFiltering) language.pick("Ažuriram popis...", "Updating list...") else if (resultCount > 0) language.pick("Spremno za prikaz na karti", "Ready to show on the map") else language.pick("Nema pronađenih rezultata", "No results found"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SearchSummaryBadge(if (isFiltering) "..." else resultCount.toString(), active = resultCount > 0 || isFiltering)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onShowFilteredOnMap,
                                enabled = resultCount > 0,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (hasActiveFilters) language.pick("Prikaži na karti", "Show on map") else language.pick("Prikaži sve", "Show all"), fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onExportKml,
                                enabled = resultCount > 0,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("KML", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            SearchSectionCard(
                title = language.pick("Pametni početak", "Smart start"),
                subtitle = language.pick("Najčešći terenski scenariji, bez ručnog slaganja filtera.", "Common field scenarios without manually combining filters.")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchScenarioCard(
                        title = language.pick("Terenski rad", "Field work"),
                        subtitle = language.pick("SOV baza, teren i najkorisniji zapisi", "SOV database, field work and useful records"),
                        icon = Icons.Default.MyLocation,
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = { setPresetFieldWork() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchScenarioCard(
                            title = language.pick("Moja baza", "My Base"),
                            subtitle = language.pick("vlastiti KML/CSV zapisi", "your KML/CSV records"),
                            icon = Icons.Default.UploadFile,
                            accent = MaterialTheme.colorScheme.tertiary,
                            onClick = { updateFilters(FilterState(sourceFilter = SourceFilter.MY_BASE)) },
                            modifier = Modifier.weight(1f)
                        )
                        SearchScenarioCard(
                            title = language.pick("U blizini", "Nearby"),
                            subtitle = language.pick("25 km oko mene", "25 km around me"),
                            icon = Icons.Default.LocationOn,
                            accent = MaterialTheme.colorScheme.tertiary,
                            onClick = { setPresetNearby() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SearchSectionCard(
                title = language.pick("Brzi filteri", "Quick filters"),
                subtitle = language.pick("Sivo = isključeno. Boja + kvačica = uključeno i odmah mijenja rezultate.", "Gray = off. Color + checkmark = on and updates results immediately.")
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchQuickChip(
                        label = language.pick("U blizini", "Nearby"),
                        selected = filters.distanceFilterKm == 25,
                        accent = MaterialTheme.colorScheme.tertiary,
                        helper = "25 km",
                        onClick = {
                            if (!hasCurrentUserLocation) onRequestGpsLocation()
                            updateFilters(filters.copy(distanceFilterKm = if (filters.distanceFilterKm == 25) null else 25))
                        }
                    )
                    SearchQuickChip(
                        label = language.pick("Jame", "Pits"),
                        selected = selectedCaveType == CaveTypeFilter.JAMA,
                        accent = MaterialTheme.colorScheme.secondary,
                        helper = language.pick("samo jame", "pits only"),
                        onClick = { updateFilters(filters.copy(caveTypeFilter = if (selectedCaveType == CaveTypeFilter.JAMA) CaveTypeFilter.ALL else CaveTypeFilter.JAMA)) }
                    )
                    SearchQuickChip(
                        label = language.pick("Špilje", "Caves"),
                        selected = selectedCaveType == CaveTypeFilter.SPILJA,
                        accent = MaterialTheme.colorScheme.tertiary,
                        helper = language.pick("samo špilje", "caves only"),
                        onClick = { updateFilters(filters.copy(caveTypeFilter = if (selectedCaveType == CaveTypeFilter.SPILJA) CaveTypeFilter.ALL else CaveTypeFilter.SPILJA)) }
                    )
                    SearchQuickChip(
                        label = language.pick("Ima opis", "Has description"),
                        selected = filters.onlyWithDescription,
                        accent = MaterialTheme.colorScheme.primary,
                        helper = language.pick("opis postoji", "description exists"),
                        onClick = { updateFilters(filters.copy(onlyWithDescription = !filters.onlyWithDescription)) }
                    )
                    SearchQuickChip(
                        label = language.pick("Treba nacrt", "Needs drawing"),
                        selected = filters.fieldTaskFilters.contains("ponoviti_nacrt") || filters.fieldTaskFilters.contains("digitalizirati_nacrt") || filters.fieldTaskFilters.contains("srediti_nacrt") || filters.fieldTaskFilters.contains("nastaviti_nacrt"),
                        accent = MaterialTheme.colorScheme.tertiary,
                        helper = "nacrt",
                        onClick = {
                            val drawingKeys = listOf("ponoviti_nacrt", "digitalizirati_nacrt", "srediti_nacrt", "nastaviti_nacrt")
                            val active = drawingKeys.any { filters.fieldTaskFilters.contains(it) }
                            val next = if (active) {
                                filters.fieldTaskFilters.filterNot { it in drawingKeys }
                            } else {
                                (filters.fieldTaskFilters + drawingKeys).distinct()
                            }
                            updateFilters(filters.copy(fieldTaskFilters = next))
                        }
                    )
                    SearchQuickChip(
                        label = language.pick("Treba koordinate", "Needs coordinates"),
                        selected = filters.fieldTaskFilters.contains("koordinate"),
                        accent = MaterialTheme.colorScheme.secondary,
                        helper = "GPS",
                        onClick = {
                            val next = filters.fieldTaskFilters.toMutableList().apply {
                                if (contains("koordinate")) remove("koordinate") else add("koordinate")
                            }
                            updateFilters(filters.copy(fieldTaskFilters = next.distinct()))
                        }
                    )
                }
                SearchFilterLegend()
            }

            OutlinedButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (showAdvanced) "Sakrij fino podešavanje" else "Fino podešavanje")
            }

            if (!searchReady) {
                SearchMessageCard(
                    title = language.pick("Pripremam pretragu", "Preparing search"),
                    body = "Search indeks se grije u pozadini. Karta i osnovne funkcije ostaju dostupne."
                )
            }

            if (showAdvanced) {
                SearchSectionCard(
                    title = "Fino podešavanje",
                    subtitle = "Za precizno filtriranje kad quick chipovi nisu dovoljni."
                ) {
                    SearchSelectField(
                        title = "Udaljenost",
                        value = selectedDistanceLabel,
                        icon = Icons.Default.MyLocation,
                        onClick = {
                            if (!hasCurrentUserLocation) onRequestGpsLocation()
                            distanceExpanded = true
                        }
                    )

                    Text("Tip objekta", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchQuickChip(
                            label = "Sve",
                            selected = selectedCaveType == CaveTypeFilter.ALL,
                            accent = MaterialTheme.colorScheme.onSurfaceVariant,
                            helper = "jame + špilje",
                            onClick = { updateFilters(filters.copy(caveTypeFilter = CaveTypeFilter.ALL)) }
                        )
                        SearchQuickChip(
                            label = language.pick("Jame", "Pits"),
                            selected = selectedCaveType == CaveTypeFilter.JAMA,
                            accent = MaterialTheme.colorScheme.secondary,
                            helper = language.pick("samo jame", "pits only"),
                            onClick = { updateFilters(filters.copy(caveTypeFilter = CaveTypeFilter.JAMA)) }
                        )
                        SearchQuickChip(
                            label = language.pick("Špilje", "Caves"),
                            selected = selectedCaveType == CaveTypeFilter.SPILJA,
                            accent = MaterialTheme.colorScheme.tertiary,
                            helper = language.pick("samo špilje", "caves only"),
                            onClick = { updateFilters(filters.copy(caveTypeFilter = CaveTypeFilter.SPILJA)) }
                        )
                    }

                    Text("Dubina", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DEPTH_FILTER_OPTIONS.forEach { (minDepth, label) ->
                            SearchQuickChip(
                                label = label,
                                selected = filters.depthMinM == minDepth,
                                accent = MaterialTheme.colorScheme.tertiary,
                                helper = "min dubina",
                                onClick = { updateFilters(filters.copy(depthMinM = if (filters.depthMinM == minDepth) null else minDepth)) }
                            )
                        }
                    }

                    Text("Što treba", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (option in FIELD_TASK_FILTERS) {
                            val key = option.first
                            val label = option.second
                            val selected = filters.fieldTaskFilters.contains(key)
                            SearchQuickChip(
                                label = label,
                                selected = selected,
                                accent = MaterialTheme.colorScheme.tertiary,
                                helper = "zadatak",
                                onClick = {
                                    val next = filters.fieldTaskFilters.toMutableList().apply {
                                        if (selected) remove(key) else add(key)
                                    }
                                    updateFilters(filters.copy(fieldTaskFilters = next.distinct()))
                                }
                            )
                        }
                    }
                }
            }

            if (distanceExpanded) {
                AlertDialog(
                    onDismissRequest = { distanceExpanded = false },
                    title = { Text("Udaljenost od mene") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DISTANCE_FILTER_OPTIONS.forEach { (distanceKm, label) ->
                                OutlinedButton(
                                    onClick = {
                                        distanceExpanded = false
                                        updateFilters(filters.copy(distanceFilterKm = distanceKm))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { distanceExpanded = false }) { Text("Zatvori") }
                    }
                )
            }
        }
    }
}



@Composable
internal fun SearchStepPill(number: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.075f), RoundedCornerShape(999.dp))
            .border(1.dp, Color.White.copy(alpha = 0.075f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(number, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
internal fun SearchSourceButton(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (title.lowercase(Locale.ROOT)) {
        "sov" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val bg = if (selected) {
        Brush.linearGradient(listOf(accent.copy(alpha = 0.30f), MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surface))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainer))
    }
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(18.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.09f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Lens,
                    contentDescription = null,
                    tint = if (selected) accent else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(if (selected) 16.dp else 10.dp)
                )
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = if (selected) "UKLJUČENO" else subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SearchScenarioCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainer)),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun SearchQuickChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color = Color.Unspecified,
    helper: String? = null
) {
    val chipAccent = if (accent == Color.Unspecified) MaterialTheme.colorScheme.primary else accent
    val background = if (selected) {
        Brush.linearGradient(listOf(chipAccent.copy(alpha = 0.34f), MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surface))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainer))
    }
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(18.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) chipAccent.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.085f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (selected) chipAccent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(999.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Lens,
                    contentDescription = null,
                    tint = if (selected) chipAccent else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(if (selected) 16.dp else 9.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (selected) "UKLJUČENO" else (helper ?: "isključeno"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) chipAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun SearchFilterLegend() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.045f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(16.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("Boja + kvačica = filter je ON", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Lens, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(9.dp))
                Text("sivo = OFF", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }
    }
}

internal fun buildSearchSmartHint(
    filters: FilterState,
    resultCount: Int,
    isFiltering: Boolean,
    searchReady: Boolean,
    hasCurrentUserLocation: Boolean
): String {
    if (!searchReady) return "Indeks se priprema. Search će se sam osvježiti čim bude spreman."
    if (isFiltering) return "Radim u pozadini da lista i karta ostanu glatke."
    if (!hasCurrentUserLocation && filters.distanceFilterKm != null) return "Uključen je filter udaljenosti. GPS će pomoći da rezultati budu precizni."
    if (!filters.hasAnyActiveCriteria()) return "Nema dodatnih filtera. Odabrana baza je aktivna; dodaj obojeni chip samo kad želiš suziti rezultate."
    if (resultCount == 0) return "Nema rezultata. Probaj očistiti jedan filter ili izabrati Sve baze."
    if (resultCount > 250) return "Puno rezultata. Dodaj lokaciju, tip objekta ili udaljenost za čišći popis."
    return "Dobar set filtera. Sad možeš otvoriti listu ili prikazati rezultate na karti."
}

@Composable
internal fun SearchSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
internal fun SearchSummaryBadge(text: String, active: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun SearchActiveFilterPill(label: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.46f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun SearchSelectField(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        val fieldTint = premiumIconTint(title)
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(premiumIconContainer(title), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = fieldTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, textAlign = TextAlign.Start, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = premiumIconTint(title).copy(alpha = 0.92f)
        )
    }
}

internal fun normalizeUiSearchText(input: String): String {
    return Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace("""\p{M}+""".toRegex(), "")
        .replace("""[^\p{L}\p{N}\s-]""".toRegex(), " ")
        .replace("""\s+""".toRegex(), " ")
        .trim()
}

@Composable
internal fun SearchMessageCard(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            val messageTint = premiumIconTint("search")
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(messageTint.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = messageTint, modifier = Modifier.size(18.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
