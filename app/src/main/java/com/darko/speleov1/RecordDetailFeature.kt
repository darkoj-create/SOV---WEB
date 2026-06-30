@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.darko.speleov1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.ParcelFileDescriptor
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
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.darko.speleov1.util.TopoDroidBridgeStore
import com.darko.speleov1.util.DriveDrawing
import com.darko.speleov1.util.DriveDrawingMatch
import com.darko.speleov1.util.DriveDrawingsRepository
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
import android.graphics.ImageDecoder
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.View
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.Overlay
import android.location.Location
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalLayoutApi::class)
@Composable

fun RecordCard(record: SpeleoRecord, onClick: () -> Unit, onViewOnMap: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF232832), contentColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(record.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val locationLine = listOfNotNull(
                        record.location.locality,
                        record.location.municipality,
                        record.location.county
                    ).distinct().joinToString(" • ")
                    if (locationLine.isNotBlank()) {
                        Text(
                            locationLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC5CCD7)
                        )
                    }
                }
                val mapButtonTint = Color(0xFFD6DCE6)
                val hasCoordinates = record.location.lat != null && record.location.lon != null
                OutlinedButton(
                    onClick = onViewOnMap,
                    enabled = hasCoordinates,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, if (hasCoordinates) Color(0xFF7A8290) else Color(0xFF7A8290).copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF2E3440),
                        contentColor = if (hasCoordinates) mapButtonTint else mapButtonTint.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (hasCoordinates) premiumIconTint("map karta") else premiumIconTint("map karta").copy(alpha = 0.4f))
                    Spacer(Modifier.width(6.dp))
                    Text(if (hasCoordinates) "Karta" else "Nema GPS")
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SourceBadge(record)
                StatusBadge(record.classification.record_status ?: "-")
                record.classification.object_type?.takeIf { it.isNotBlank() }?.let {
                    SearchMetaPill(it.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) })
                }
                displayPlateNumber(record)?.let {
                    SearchMetaPill("Pločica $it")
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SearchStatPill(label = "Dubina", value = record.metrics.depth_m?.let { "${it} m" } ?: "-", modifier = Modifier.weight(1f, fill = false))
                SearchStatPill(label = "Duljina", value = record.metrics.length_m?.let { "${it} m" } ?: "-", modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}


private fun displayPlateNumber(record: SpeleoRecord): String? {
    val directPlate = record.condition.plate_number?.takeIf { it.isNotBlank() }
    if (directPlate != null) return directPlate
    val source = (record.source ?: "").lowercase(Locale.ROOT)
    if (source == "katastar" || source == "both") {
        return record.cadastre.cadastral_number?.takeIf { it.isNotBlank() }
    }
    return null
}

@Composable
internal fun SearchMetaPill(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF313744), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color(0xFFF7F8FB))
    }
}

@Composable
internal fun SearchStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF303742), contentColor = Color.White),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD0D6DF))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

private fun sourceLabelForRecord(record: SpeleoRecord): String {
    return when ((record.source ?: "").lowercase(Locale.ROOT)) {
        "both" -> "SoV"
        "katastar" -> "SoV"
        else -> "SoV"
    }
}

@Composable
fun SourceBadge(record: SpeleoRecord) {
    val (bg, fg, label) = when ((record.source ?: "").lowercase(Locale.ROOT)) {
        "both" -> Triple(Color(0xFFE2F0FF), Color(0xFF245B96), "SoV")
        "katastar" -> Triple(Color(0xFFE2F0FF), Color(0xFF245B96), "SoV")
        else -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "SoV")
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusBadge(status: String) {
    val normalized = normalizeForSearch(status).replace(' ', '_')
    val (bg, fg, label) = when (normalized) {
        "evidentirano" -> Triple(Color(0xFFDDF4E3), Color(0xFF2E7D32), "Evidentirano")
        "u_katastru" -> Triple(Color(0xFFDDF4E3), Color(0xFF2E7D32), "Evidentirano")
        "za_provjeru" -> Triple(Color(0xFFFFE8CC), Color(0xFFA65B00), "Za provjeru")
        "nije_u_katastru" -> Triple(Color(0xFFFFE8CC), Color(0xFFA65B00), "Za provjeru")
        "na_provjeri" -> Triple(Color(0xFFFFF4CC), Color(0xFF8D6E00), "Na provjeri")
        else -> Triple(Color(0xFFE7E7E7), Color(0xFF444444), status.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.getDefault()) })
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailSheet(

    record: SpeleoRecord,
    photoUris: List<String>,
    onAddPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onGetTo: () -> Unit,
    onFillRecord: () -> Unit,
    onOpenOfflineZapisnik: () -> Unit,
    onShareRecord: () -> Unit,
    onImportAttachmentToMap: (Uri) -> Unit = {},
    onOpenGeneratedLayerOnMap: (ImportedLayer) -> Unit = {},
    onChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var showNoteDialog by remember(record.id) { mutableStateOf(false) }
    var localNote by remember(record.id) { mutableStateOf(UserContentStore.loadRecordNote(context, record.id)) }
    var isFavorite by remember(record.id) { mutableStateOf(UserContentStore.isFavoriteRecord(context, record.id)) }
    var topoAttachments by remember(record.id) { mutableStateOf(TopoDroidBridgeStore.loadForRecord(context, record.id)) }
    val isNotInCadastre = record.classification.record_status == "nije_u_katastru" || record.cadastre.not_in_cadastre_candidate == true || record.cadastre.in_cadastre == false
    val fieldTasks = record.classification.field_tasks.orEmpty()
    val topodroidFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        var added = 0
        uris.forEach { uri ->
            if (TopoDroidBridgeStore.attachUri(context, record, uri, "Manual") != null) added++
        }
        topoAttachments = TopoDroidBridgeStore.loadForRecord(context, record.id)
        Toast.makeText(context, if (added > 0) "Dodano nacrta/exporta: $added" else "Nijedna odabrana datoteka nije podržana", Toast.LENGTH_LONG).show()
    }
    val topodroidFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val result = TopoDroidBridgeStore.scanTreeForRecord(context, record, uri)
            topoAttachments = TopoDroidBridgeStore.loadForRecord(context, record.id)
            Toast.makeText(context, "Skenirano: ${result.scanned}, povezano: ${result.matched}", Toast.LENGTH_LONG).show()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(record.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(listOfNotNull(record.location.county, record.location.municipality, record.location.locality).joinToString(" • "))
        if (isNotInCadastre && fieldTasks.isNotEmpty()) {
            FieldTasksChips(fieldTasks)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceBadge(record)
            StatusBadge(record.classification.record_status ?: "-")
            record.classification.object_type?.let { AssistChip(onClick = {}, label = { Text(it) }) }
            record.classification.priority?.let { AssistChip(onClick = {}, label = { Text("Prioritet: $it") }) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialogActionButton(
                    text = "Go to",
                    icon = Icons.Default.Navigation,
                    onClick = onGetTo,
                    modifier = Modifier.weight(1f)
                )
                record.location.lat?.let { lat ->
                    record.location.lon?.let { lon ->
                        DialogActionButton(
                            text = "Open Gmaps",
                            icon = Icons.Default.OpenInNew,
                            onClick = { openGoogleMaps(context, lat, lon) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } ?: Spacer(Modifier.weight(1f))
            }

            if (isNotInCadastre) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DialogActionButton(
                        text = "Zapisnik",
                        icon = Icons.Default.Description,
                        onClick = onFillRecord,
                        modifier = Modifier.weight(1f)
                    )
                    DialogActionButton(
                        text = "Offline zapisnik",
                        icon = Icons.Default.Save,
                        onClick = onOpenOfflineZapisnik,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialogActionButton(
                    text = if (isFavorite) "Favorite" else "Add fav",
                    icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    onClick = {
                        isFavorite = UserContentStore.toggleFavoriteRecord(context, record.id)
                        Toast.makeText(context, if (isFavorite) "Dodano u favorite" else "Maknuto iz favorita", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                )
                DialogActionButton(
                    text = "Share",
                    icon = Icons.Default.Share,
                    onClick = onShareRecord,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialogActionButton(
                    text = "Copy WGS84",
                    icon = Icons.Default.Description,
                    onClick = {
                        val text = record.location.lat?.let { lat -> record.location.lon?.let { lon -> String.format(Locale.US, "%.6f, %.6f", lat, lon) } }.orEmpty()
                        if (text.isNotBlank()) {
                            clipboard.setText(AnnotatedString(text))
                            Toast.makeText(context, "WGS84 kopiran", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                DialogActionButton(
                    text = "Note",
                    icon = Icons.Default.Description,
                    onClick = { showNoteDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Divider()
        DetailLine("Izvor", sourceLabelForRecord(record))
        DetailLine(
            "WGS84",
            record.location.lat?.let { lat -> record.location.lon?.let { lon -> String.format(Locale.US, "%.6f, %.6f", lat, lon) } },
            onClick = {
                val text = record.location.lat?.let { lat -> record.location.lon?.let { lon -> String.format(Locale.US, "%.6f, %.6f", lat, lon) } }.orEmpty()
                if (text.isNotBlank()) {
                    clipboard.setText(AnnotatedString(text))
                    Toast.makeText(context, "WGS84 kopiran", Toast.LENGTH_SHORT).show()
                }
            }
        )
        record.location.lat?.let { lat ->
            record.location.lon?.let { lon ->
                val htrs = CoordinateConverter.wgs84ToHtrs96Tm(lat, lon)
                val htrsText = String.format(Locale.US, "X %.2f, Y %.2f", htrs.x, htrs.y)
                DetailLine("HTRS96/TM", htrsText, onClick = {
                    clipboard.setText(AnnotatedString(htrsText))
                    Toast.makeText(context, "HTRS kopiran", Toast.LENGTH_SHORT).show()
                })
            }
        }
        DetailLine("Pločica", displayPlateNumber(record))
        DetailLine("Dubina", record.metrics.depth_m?.let { "$it m" })
        DetailLine("Duljina", record.metrics.length_m?.let { "$it m" })
        DetailLine("Vertikalna razlika", record.metrics.vertical_range_m?.let { "$it m" })
        DetailLine("Stanje ulaza", record.condition.main_entrance_status)
        DetailLine("Onečišćenje", record.condition.pollution)
        DetailLine("Zadnje istraživanje", record.research.last_research_date ?: record.research.last_research_year?.toString())
        DetailLine("Udruge", record.research.clubs)
        DetailLine("Ekipa", record.research.team_members)
        DetailLine("Daljnje istraživanje", record.research.further_research_note)

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pristup", fontWeight = FontWeight.SemiBold)
                Text(record.content.access_description ?: "-")
                Text("Opis", fontWeight = FontWeight.SemiBold)
                Text(record.content.technical_description ?: "-")
                if (localNote.isNotBlank()) {
                    Divider()
                    Text("Note", fontWeight = FontWeight.SemiBold)
                    Text(localNote)
                }
            }
        }

        DriveDrawingsCard(record = record)


        TopoDroidBridgeCard(
            record = record,
            attachments = topoAttachments,
            onManualAttach = { topodroidFileLauncher.launch(arrayOf("application/zip", "application/pdf", "image/*", "text/*", "application/octet-stream", "*/*")) },
            onScanFolder = { topodroidFolderLauncher.launch(null) },
            onExportArchive = {
                runCatching {
                    val archive = TopoDroidBridgeStore.createObjectArchive(context, record, topoAttachments)
                    shareFileFromCache(context, archive, "application/zip", "Podijeli arhivu nacrta")
                }.onFailure {
                    Toast.makeText(context, "Ne mogu izraditi arhivu nacrta", Toast.LENGTH_LONG).show()
                }
            },
            onOpen = { attachment -> openAttachmentUri(context, attachment.uri) },
            onShare = { attachment -> shareAttachmentUri(context, attachment.uri, attachment.originalFilename) },
            onDetach = { attachment ->
                TopoDroidBridgeStore.detach(context, attachment.id)
                topoAttachments = TopoDroidBridgeStore.loadForRecord(context, record.id)
                Toast.makeText(context, "Nacrt odspojen", Toast.LENGTH_SHORT).show()
            },
            onShowOnMap = { attachment -> onImportAttachmentToMap(Uri.parse(attachment.uri)) },
            onGeoreferenceToMap = { attachment ->
                val entranceLat = record.location.lat
                val entranceLon = record.location.lon
                if (entranceLat != null && entranceLon != null) {
                    coroutineScope.launch {
                        val layer = withContext(Dispatchers.IO) {
                            TopoDroidBridgeStore.georeferenceToLayer(context, attachment, entranceLat, entranceLon)
                        }
                        if (layer != null) {
                            val current = UserContentStore.loadImportedLayers(context)
                            UserContentStore.saveImportedLayers(context, current.filterNot { it.id == layer.id } + layer.copy(visible = true))
                            onChanged()
                            Toast.makeText(context, "Topo plan je dodan na kartu i otvoren kao sloj.", Toast.LENGTH_LONG).show()
                            onOpenGeneratedLayerOnMap(layer.copy(visible = true))
                        } else {
                            Toast.makeText(context, "Ne mogu georeferencirati — nema survey.sql u ovom attachmentu.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Objekt nema koordinate ulaza — ne mogu georeferencirati.", Toast.LENGTH_LONG).show()
                }
            }
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Fotografije", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PhotoActionCard(
                        title = "Snimi foto",
                        subtitle = "Dodaj novu fotku kamerom",
                        icon = Icons.Default.AddAPhoto,
                        onClick = onTakePhoto
                    )
                    PhotoActionCard(
                        title = "Iz galerije",
                        subtitle = "Uvezi postojeću fotografiju",
                        icon = Icons.Default.PhotoLibrary,
                        onClick = onAddPhoto
                    )
                }
                if (photoUris.isEmpty()) {
                    Text(
                        "Još nema dodanih fotografija za ovu jamu. Snimljene i uvezene fotke spremaju se u app folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC5CCD7)
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        photoUris.forEachIndexed { index, uriString ->
                            RecordPhotoPreviewCard(
                                uriString = uriString,
                                label = "Fotka ${index + 1}",
                                onOpen = { onOpenPhoto(uriString) },
                                onRemove = { onRemovePhoto(uriString) }
                            )
                        }
                    }
                }
            }
        }

        if (!isNotInCadastre && fieldTasks.isNotEmpty()) {
            FieldTasksChips(fieldTasks)
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dodaj kratku privatnu bilješku za ovu točku.")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = isFavorite,
                                onValueChange = { isFavorite = it }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Favorit")
                        Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null, tint = premiumIconTint("favorite star"))
                    }
                    OutlinedTextField(
                        value = localNote,
                        onValueChange = { localNote = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        label = { Text("Moja bilješka") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    UserContentStore.saveRecordNote(context, record.id, localNote)
                    val nowFavorite = UserContentStore.isFavoriteRecord(context, record.id)
                    if (isFavorite != nowFavorite) { UserContentStore.toggleFavoriteRecord(context, record.id) }
                    showNoteDialog = false
                }) { Text("Spremi") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        localNote = ""
                        UserContentStore.saveRecordNote(context, record.id, "")
                        showNoteDialog = false
                    }) { Text("Obriši") }
                    TextButton(onClick = {
                        localNote = UserContentStore.loadRecordNote(context, record.id)
                        showNoteDialog = false
                    }) { Text("Odustani") }
                }
            }
        )
    }
}


@Composable
private fun FieldTasksChips(tasks: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Teren zadaci", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tasks.forEach { task ->
                AssistChip(
                    onClick = {},
                    label = { Text(task, color = premiumIconTint(task)) },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = premiumIconTint(task)) }
                )
            }
        }
    }
}


@Composable
private fun DriveDrawingsCard(record: SpeleoRecord) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webAppUrl = remember(record.id) { DriveDrawingsRepository.loadWebAppUrl(context) }
    var isLoading by remember(record.id) { mutableStateOf(false) }
    var statusText by remember(record.id) { mutableStateOf<String?>(null) }
    var matches by remember(record.id) { mutableStateOf<List<DriveDrawingMatch>>(emptyList()) }
    var drawingsTotalCount by remember(record.id) { mutableStateOf<Int?>(null) }
    var localUploads by remember(record.id) { mutableStateOf(DriveDrawingsRepository.loadUserDrawings(context, record)) }
    var previewDrawing by remember(record.id) { mutableStateOf<DriveDrawing?>(null) }
    val uploadDrawingLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isLoading = true
            val result = withContext(Dispatchers.IO) {
                runCatching { DriveDrawingsRepository.importUserDrawing(context, record, uri) }
            }
            isLoading = false
            result.onSuccess { drawing ->
                localUploads = DriveDrawingsRepository.loadUserDrawings(context, record)
                statusText = null
                previewDrawing = drawing
                Toast.makeText(context, "Nacrt dodan lokalno", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "Ne mogu dodati nacrt", Toast.LENGTH_LONG).show()
            }
        }
    }

    previewDrawing?.let { drawing ->
        val localFile = DriveDrawingsRepository.localFileFor(context, drawing)
        DrawingViewerDialog(
            title = drawing.displayName,
            drawing = drawing,
            localFile = localFile,
            onDismiss = { previewDrawing = null },
            onOpenExternal = {
                if (!DriveDrawingsRepository.openLocalDrawing(context, drawing)) {
                    val message = if (drawing.isImage && drawing.extension in setOf("tif", "tiff")) {
                        "Ovaj format možda treba vanjsku aplikaciju za pregled."
                    } else {
                        "Ne mogu otvoriti nacrt"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    fun loadMatches(forceNetwork: Boolean) {
        scope.launch {
            isLoading = true
            statusText = null
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    DriveDrawingsRepository.fetchMatchesForRecord(context, record, forceNetwork = forceNetwork)
                }
            }
            result.onSuccess { lookup ->
                matches = lookup.matches
                drawingsTotalCount = lookup.totalCount
                statusText = when {
                    !lookup.ok -> lookup.error ?: "Ne mogu učitati nacrte za objekt."
                    lookup.matches.isEmpty() && localUploads.isEmpty() -> "Nema pronađenih nacrta iz Drivea. Ako imaš svoj nacrt, dodaj ga desnim gumbom."
                    else -> null
                }
            }.onFailure { error ->
                matches = emptyList()
                statusText = error.message ?: "Ne mogu učitati nacrte za objekt."
            }
            isLoading = false
        }
    }

    LaunchedEffect(record.id, webAppUrl) {
        if (webAppUrl.isNotBlank()) loadMatches(forceNetwork = false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2835), contentColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFF78A6FF).copy(alpha = 0.26f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Nacrti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        drawingsTotalCount?.let { total ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0xFF132D3D),
                                border = BorderStroke(1.dp, Color(0xFF78A6FF).copy(alpha = 0.28f))
                            ) {
                                Text("Baza: $total", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFBBD7FF))
                            }
                        }
                    }
                    Text(
                        "Nacrti se sada traže direktno po objektu, bez učitavanja cijele Drive baze. Ako nema nacrta iz Drivea, možeš dodati svoj lokalni nacrt. Podržani su JPG, PNG, TIFF, WEBP i PDF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8C7DD)
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF9FC2FF))
                } else {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF9FC2FF))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { loadMatches(forceNetwork = true) },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Osvježi")
                }
                OutlinedButton(
                    onClick = { uploadDrawingLauncher.launch(arrayOf("image/*", "application/pdf", "image/tiff", "application/octet-stream")) },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Dodaj svoj")
                }
            }

                statusText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD28A))
                }

                val localMatches = localUploads.map { DriveDrawingMatch(it, 1.0, "verified", "ručno dodano") }
                if (localMatches.isNotEmpty()) {
                    Text("Moji dodani nacrti", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9BE7B2), fontWeight = FontWeight.SemiBold)
                    localMatches.forEach { match ->
                        DriveDrawingMatchRow(
                            match = match,
                            isLocal = true,
                            onPreview = { previewDrawing = match.drawing },
                            onDownloadOrOpen = { previewDrawing = match.drawing },
                            onOpenDrive = {
                                if (!DriveDrawingsRepository.openLocalDrawing(context, match.drawing)) {
                                    Toast.makeText(context, "Ne mogu otvoriti lokalni nacrt", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                if (matches.isNotEmpty()) {
                    matches.forEach { match ->
                        DriveDrawingMatchRow(
                            match = match,
                            isLocal = DriveDrawingsRepository.hasLocalFile(context, match.drawing),
                            onPreview = { previewDrawing = match.drawing },
                            onDownloadOrOpen = {
                                scope.launch {
                                    val alreadyLocal = DriveDrawingsRepository.hasLocalFile(context, match.drawing)
                                    if (alreadyLocal) {
                                        previewDrawing = match.drawing
                                        return@launch
                                    }
                                    isLoading = true
                                    val downloaded = withContext(Dispatchers.IO) {
                                        runCatching { DriveDrawingsRepository.downloadDrawing(context, match.drawing) }
                                    }
                                    isLoading = false
                                    downloaded.onSuccess {
                                        Toast.makeText(context, "Nacrt spremljen u Offline/nacrti", Toast.LENGTH_SHORT).show()
                                        previewDrawing = match.drawing
                                    }.onFailure { error ->
                                        Toast.makeText(context, error.message ?: "Download nacrta nije uspio", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onOpenDrive = {
                                if (!DriveDrawingsRepository.openDrive(context, match.drawing)) {
                                    Toast.makeText(context, "Nema Drive linka", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

@Composable
private fun DriveDrawingMatchRow(
    match: DriveDrawingMatch,
    isLocal: Boolean,
    onPreview: () -> Unit,
    onDownloadOrOpen: () -> Unit,
    onOpenDrive: () -> Unit
) {
    val strong = match.status == "verified"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (strong) Color(0xFF213A2B) else Color(0xFF2A3040),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, if (strong) Color(0xFF76E0A1).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(match.drawing.displayName, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(match.drawing.drawingKindLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFFB8C7DD))
                    Text(
                        buildString {
                            append(if (strong) "Siguran nacrt" else "Mogući nacrt")
                            append(" • ")
                            append((match.confidence * 100).roundToInt())
                            append("%")
                            if (match.reason.isNotBlank()) {
                                append(" • ")
                                append(match.reason)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (strong) Color(0xFF9BE7B2) else Color(0xFFFFD28A)
                    )
                }
                Icon(if (isLocal) Icons.Default.CheckCircle else Icons.Default.Download, contentDescription = null, tint = if (isLocal) Color(0xFF9BE7B2) else Color(0xFF9FC2FF))
            }
            if (isLocal) {
                val context = LocalContext.current
                DrawingThumbnail(
                    drawing = match.drawing,
                    localFile = DriveDrawingsRepository.localFileFor(context, match.drawing),
                    title = match.drawing.displayName,
                    onClick = onPreview
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onDownloadOrOpen, modifier = Modifier.weight(1f)) {
                    Icon(if (isLocal) Icons.Default.Visibility else Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isLocal) "Pregledaj" else "Preuzmi")
                }
                OutlinedButton(onClick = onOpenDrive, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Drive")
                }
            }
        }
    }
}


@Composable
private fun DrawingThumbnail(
    drawing: DriveDrawing,
    localFile: File,
    title: String,
    onClick: () -> Unit
) {
    var bitmap by remember(localFile.absolutePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(localFile.absolutePath) {
        bitmap = withContext(Dispatchers.IO) {
            when {
                drawing.isPdf -> renderPdfPageBitmap(localFile, pageIndex = 0, targetWidthPx = 520)
                drawing.isImage -> renderImageBitmap(localFile, targetWidthPx = 520)
                else -> null
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color(0xFF111720), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val pageBitmap = bitmap
        if (pageBitmap != null) {
            Image(
                bitmap = pageBitmap.asImageBitmap(),
                contentDescription = title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("Tap za fullscreen", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF9FC2FF))
                Text(if (drawing.isImage && drawing.extension in setOf("tif", "tiff")) "TIFF možda treba vanjski preglednik…" else "Učitavam pregled nacrta…", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C7DD))
            }
        }
    }
}

@Composable
private fun DrawingViewerDialog(
    title: String,
    drawing: DriveDrawing,
    localFile: File,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit
) {
    var pageCount by remember(localFile.absolutePath) { mutableIntStateOf(1) }
    var pageIndex by remember(localFile.absolutePath) { mutableIntStateOf(0) }
    var bitmap by remember(localFile.absolutePath, pageIndex) { mutableStateOf<Bitmap?>(null) }
    var errorText by remember(localFile.absolutePath) { mutableStateOf<String?>(null) }
    var scale by remember(localFile.absolutePath, pageIndex) { mutableFloatStateOf(1f) }
    var offsetX by remember(localFile.absolutePath, pageIndex) { mutableFloatStateOf(0f) }
    var offsetY by remember(localFile.absolutePath, pageIndex) { mutableFloatStateOf(0f) }

    LaunchedEffect(localFile.absolutePath) {
        pageCount = withContext(Dispatchers.IO) { if (drawing.isPdf) readPdfPageCount(localFile).coerceAtLeast(1) else 1 }
    }
    LaunchedEffect(localFile.absolutePath, pageIndex) {
        errorText = null
        bitmap = null
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        val rendered = withContext(Dispatchers.IO) { if (drawing.isPdf) renderPdfPageBitmap(localFile, pageIndex, targetWidthPx = 1800) else renderImageBitmap(localFile, targetWidthPx = 1800) }
        if (rendered == null) {
            errorText = "Ne mogu prikazati ovaj nacrt u appu. Probaj vanjski preglednik."
        } else {
            bitmap = rendered
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF05070B),
            contentColor = Color.White
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111720))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Clear, contentDescription = "Zatvori") }
                    Column(Modifier.weight(1f)) {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(if (drawing.isPdf) "Stranica ${pageIndex + 1}/$pageCount" else drawing.drawingKindLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFFB8C7DD))
                    }
                    if (pageCount > 1) {
                        OutlinedButton(
                            onClick = { if (pageIndex > 0) pageIndex-- },
                            enabled = pageIndex > 0,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) { Text("‹") }
                        OutlinedButton(
                            onClick = { if (pageIndex < pageCount - 1) pageIndex++ },
                            enabled = pageIndex < pageCount - 1,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) { Text("›") }
                    }
                    IconButton(onClick = onOpenExternal) { Icon(Icons.Default.OpenInNew, contentDescription = "Otvori vanjskim appom") }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF05070B))
                        .pointerInput(bitmap, pageIndex) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 6f)
                                scale = newScale
                                if (newScale <= 1.02f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    offsetX = (offsetX + pan.x).coerceIn(-2200f, 2200f)
                                    offsetY = (offsetY + pan.y).coerceIn(-2600f, 2600f)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val pageBitmap = bitmap
                    when {
                        errorText != null -> {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFFFD28A), modifier = Modifier.size(36.dp))
                                Text(errorText.orEmpty(), textAlign = TextAlign.Center, color = Color(0xFFFFD28A))
                                Button(onClick = onOpenExternal) { Text("Otvori vanjskim preglednikom") }
                            }
                        }
                        pageBitmap == null -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(color = Color(0xFF9FC2FF))
                                Text("Pripremam nacrt…", color = Color(0xFFB8C7DD))
                            }
                        }
                        else -> {
                            Image(
                                bitmap = pageBitmap.asImageBitmap(),
                                contentDescription = title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offsetX
                                        translationY = offsetY
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun readPdfPageCount(file: File): Int = runCatching {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
    }
}.getOrDefault(1)

private fun renderPdfPageBitmap(file: File, pageIndex: Int, targetWidthPx: Int): Bitmap? = runCatching {
    if (!file.exists() || file.length() <= 0L) return@runCatching null
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            if (renderer.pageCount <= 0) return@runCatching null
            val safeIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
            renderer.openPage(safeIndex).use { page ->
                val safeWidth = targetWidthPx.coerceIn(320, 2200)
                val ratio = page.height.toFloat() / max(page.width.toFloat(), 1f)
                val safeHeight = (safeWidth * ratio).roundToInt().coerceIn(320, 3200)
                val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }
}.getOrNull()

private fun renderImageBitmap(file: File, targetWidthPx: Int): Bitmap? = runCatching {
    if (!file.exists() || file.length() <= 0L) return@runCatching null
    val original = decodeDrawingImageBitmap(file) ?: return@runCatching null
    val safeWidth = targetWidthPx.coerceIn(320, 2200)
    if (original.width <= safeWidth) return@runCatching original
    val ratio = original.height.toFloat() / max(original.width.toFloat(), 1f)
    val safeHeight = (safeWidth * ratio).roundToInt().coerceIn(320, 4200)
    Bitmap.createScaledBitmap(original, safeWidth, safeHeight, true)
}.getOrNull()

private fun decodeDrawingImageBitmap(file: File): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching {
            val source = ImageDecoder.createSource(file)
            return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        }
    }
    return BitmapFactory.decodeFile(file.absolutePath)
}

@Composable
fun DetailLine(label: String, value: String?, onClick: (() -> Unit)? = null) {
    if (!value.isNullOrBlank()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFFC5CCD7))
            Text(value)
        }
    }
}


@Composable
private fun PhotoActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val actionTint = premiumIconTint("$title $subtitle")
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 148.dp).heightIn(min = 74.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = premiumIconContainer("$title $subtitle"),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, actionTint.copy(alpha = 0.44f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = premiumIconContainer(title, active = true)
            ) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = actionTint)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PhotoMiniActionChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val chipTint = premiumIconTint(text)
    AssistChip(
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = text, modifier = Modifier.size(16.dp), tint = chipTint) },
        label = { Text(text) }
    )
}

@Composable
private fun RecordPhotoPreviewCard(
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
        modifier = Modifier.width(162.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpen),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = premiumIconTint("photo gallery"))
                }
            }
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhotoMiniActionChip(text = "Otvori", icon = Icons.Default.OpenInNew, onClick = onOpen)
                PhotoMiniActionChip(text = "Obriši", icon = Icons.Default.Delete, onClick = onRemove)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopoDroidBridgeCard(
    record: SpeleoRecord,
    attachments: List<TopoDroidBridgeStore.Attachment>,
    onManualAttach: () -> Unit,
    onScanFolder: () -> Unit,
    onExportArchive: () -> Unit,
    onOpen: (TopoDroidBridgeStore.Attachment) -> Unit,
    onShare: (TopoDroidBridgeStore.Attachment) -> Unit,
    onDetach: (TopoDroidBridgeStore.Attachment) -> Unit,
    onShowOnMap: (TopoDroidBridgeStore.Attachment) -> Unit,
    onGeoreferenceToMap: ((TopoDroidBridgeStore.Attachment) -> Unit)? = null
) {
    var viewerAttachment by remember { mutableStateOf<TopoDroidBridgeStore.Attachment?>(null) }
    viewerAttachment?.let { attachment ->
        TopoDroidViewerDialog(
            attachment = attachment,
            onDismiss = { viewerAttachment = null },
            onOpenExternal = { onOpen(attachment) },
            onShare = { onShare(attachment) }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF202631), contentColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("TopoDroid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (attachments.isEmpty()) "Nema povezanih TopoDroid datoteka" else "Objekt ima TopoDroid datoteke • ${attachments.size} file${if (attachments.size == 1) "" else "ova"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (attachments.isEmpty()) Color(0xFFB8C0CC) else Color(0xFF9BE7B2)
                    )
                    if (attachments.isNotEmpty()) {
                        Text(
                            "Uključuje se u .sovpkg terenski paket i može se izvesti kao arhiva objekta.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9FB7D8)
                        )
                        Text(
                            "Topo plan na karti prikazuje centerline, stanice i blagi koridor; ulaz je sidren na koordinate objekta.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8FDDE6)
                        )
                    }
                }
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF9FB7D8))
            }

            if (attachments.isEmpty()) {
                Text(
                    "Nema povezanih TopoDroid datoteka. Dodaj ZIP/TDR ili drugi TopoDroid paket za ovaj objekt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC5CCD7)
                )
                Text(
                    "Auto match koristi: ${TopoDroidBridgeStore.normalizedMatchPreview(record)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8D98A8)
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onManualAttach, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Dodaj TopoDroid")
                }
                OutlinedButton(onClick = onScanFolder, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Skeniraj folder")
                }
                if (attachments.isNotEmpty()) {
                    OutlinedButton(onClick = onExportArchive, shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export arhive")
                    }
                }
            }

            if (attachments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    attachments.forEach { attachment ->
                        TopoDroidAttachmentRow(
                            attachment = attachment,
                            onViewer = if (attachment.hasSurveySql == true) { { viewerAttachment = attachment } } else null,
                            onOpen = { onOpen(attachment) },
                            onShare = { onShare(attachment) },
                            onDetach = { onDetach(attachment) },
                            onShowOnMap = if (TopoDroidBridgeStore.isMapCandidate(attachment.originalFilename)) {
                                { onShowOnMap(attachment) }
                            } else null,
                            onGeoreferenceToMap = if (TopoDroidBridgeStore.isTopoDroidPackage(attachment.originalFilename) && onGeoreferenceToMap != null) {
                                { onGeoreferenceToMap(attachment) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopoDroidAttachmentRow(
    attachment: TopoDroidBridgeStore.Attachment,
    onViewer: (() -> Unit)?,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDetach: () -> Unit,
    onShowOnMap: (() -> Unit)?,
    onGeoreferenceToMap: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.055f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF314056), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Description, null, tint = Color(0xFFDDE8F8))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(attachment.originalFilename, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        TopoDroidBridgeStore.buildAttachmentSummary(attachment),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB6C0CE),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (attachment.hasSurveySql == true || attachment.qcWarnings.orEmpty().isNotEmpty()) {
                Text(
                    TopoDroidBridgeStore.buildQcText(attachment),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (attachment.qcWarnings.orEmpty().isEmpty()) Color(0xFF9BE7B2) else Color(0xFFFFD27A)
                )
            }
            if (attachment.hasPlanDrawing == true || attachment.hasProfileDrawing == true || attachment.hasImageOrPdfPreview == true) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (attachment.hasPlanDrawing == true) AssistChip(onClick = {}, label = { Text("Plan") })
                    if (attachment.hasProfileDrawing == true) AssistChip(onClick = {}, label = { Text("Profil") })
                    if (attachment.hasImageOrPdfPreview == true) AssistChip(onClick = {}, label = { Text("Preview") })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onViewer != null) TextButton(onClick = onViewer) { Text("Pregled") }
                TextButton(onClick = onOpen) { Text("Otvori original") }
                if (onShowOnMap != null) TextButton(onClick = onShowOnMap) { Text("Prikaži na karti") }
                if (onGeoreferenceToMap != null) {
                    TextButton(onClick = onGeoreferenceToMap) {
                        Icon(Icons.Default.Map, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Centerline na kartu", style = MaterialTheme.typography.labelSmall)
                    }
                }
                TextButton(onClick = onShare) { Text("Podijeli") }
                TextButton(onClick = onDetach) { Text("Odspoji") }
            }
        }
    }
}


@Composable
fun TopoDroidViewerDialog(
    attachment: TopoDroidBridgeStore.Attachment,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("Plan") }
    var loading by remember(attachment.id) { mutableStateOf(true) }
    var model by remember(attachment.id) { mutableStateOf<TopoDroidBridgeStore.SurveyViewerModel?>(null) }

    LaunchedEffect(attachment.id) {
        loading = true
        model = withContext(Dispatchers.IO) {
            TopoDroidBridgeStore.buildViewerModel(context, attachment)
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("TopoDroid viewer", fontWeight = FontWeight.Bold)
                Text(
                    attachment.originalFilename,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7D8795),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Čitam survey.sql i pripremam centerline…")
                    }
                } else {
                    val viewerModel = model
                    if (viewerModel == null) {
                        Text(
                            "Ovaj attachment nema čitljiv survey.sql za interni viewer. Original se i dalje može otvoriti ili podijeliti.",
                            color = Color(0xFF5F6875)
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(onClick = {}, label = { Text("${viewerModel.stations.size} stanica") })
                            AssistChip(onClick = {}, label = { Text("${viewerModel.centerline.size} centerline") })
                            AssistChip(onClick = {}, label = { Text("${formatTopoMeters(viewerModel.totalLengthM)} m") })
                            AssistChip(onClick = {}, label = { Text("ΔZ ${formatTopoMeters(viewerModel.verticalRangeM)} m") })
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Plan", "Profil", "Mjerenja", "Info").forEach { tab ->
                                FilterChip(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    label = { Text(tab) }
                                )
                            }
                        }

                        when (selectedTab) {
                            "Plan" -> TopoDroidSurveyPanel(model = viewerModel, profile = false)
                            "Profil" -> TopoDroidSurveyPanel(model = viewerModel, profile = true)
                            "Mjerenja" -> TopoDroidMeasurementsPanel(viewerModel)
                            else -> TopoDroidInfoPanel(attachment, viewerModel)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenExternal) { Text("Otvori original") }
                TextButton(onClick = onDismiss) { Text("Zatvori") }
            }
        },
        dismissButton = {
            TextButton(onClick = onShare) { Text("Podijeli") }
        }
    )
}

@Composable
private fun TopoDroidSurveyPanel(
    model: TopoDroidBridgeStore.SurveyViewerModel,
    profile: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFF101820), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { TopoDroidSurveyAndroidView(it) },
                update = { it.setSurvey(model, profile) }
            )
        }
        Text(
            if (profile) "Profil je izveden iz duljine i nagiba centerline mjerenja." else "Plan je izveden iz azimuta i duljine centerline mjerenja.",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF7D8795)
        )
    }
}

@Composable
private fun TopoDroidMeasurementsPanel(model: TopoDroidBridgeStore.SurveyViewerModel) {
    var stationFilter by remember { mutableStateOf("") }
    val filtered = remember(model.centerline, stationFilter) {
        if (stationFilter.isBlank()) model.centerline
        else model.centerline.filter {
            it.from.contains(stationFilter.trim(), ignoreCase = true) ||
            it.to.contains(stationFilter.trim(), ignoreCase = true)
        }
    }
    val visible = filtered.take(150)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = stationFilter,
            onValueChange = { stationFilter = it },
            placeholder = { Text("Filtriraj po stanici (npr. 0, A1...)", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = if (stationFilter.isNotBlank()) {
                { IconButton(onClick = { stationFilter = "" }) { Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp)) } }
            } else null
        )
        if (filtered.isEmpty()) {
            Text(
                "Nema mjerenja za stanicu \"$stationFilter\".",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F6875)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(visible) { segment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF4F7FB), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${segment.from} → ${segment.to}", fontWeight = FontWeight.SemiBold)
                            Text(
                                "az ${formatTopoMeters(segment.bearing)}° • nagib ${formatTopoMeters(segment.clino)}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF5E6672)
                            )
                        }
                        Text("${formatTopoMeters(segment.length)} m", fontWeight = FontWeight.Bold)
                    }
                }
                if (filtered.size > 150) {
                    item {
                        Text(
                            "Prikazujem prvih 150 od ${filtered.size} mjerenja. Koristi filter za sužavanje.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF5F6875)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopoDroidInfoPanel(
    attachment: TopoDroidBridgeStore.Attachment,
    model: TopoDroidBridgeStore.SurveyViewerModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Survey: ${model.surveyName ?: attachment.surveyName ?: "-"}", fontWeight = FontWeight.SemiBold)
        Text("Datum: ${model.surveyDate ?: attachment.surveyDate ?: "-"}")
        Text("Ekipa: ${model.surveyTeam ?: attachment.surveyTeam ?: "-"}")
        Text("Duljina: ${formatTopoMeters(model.totalLengthM)} m")
        Text("Visinska razlika: ${formatTopoMeters(model.verticalRangeM)} m")
        Divider()
        Text("QC", fontWeight = FontWeight.SemiBold)
        val warnings = model.qcWarnings.filter { it.isNotBlank() }
        if (warnings.isEmpty()) {
            Text("✓ Osnovna provjera bez upozorenja", color = Color(0xFF2E7D32))
        } else {
            warnings.forEach { Text("⚠ $it", color = Color(0xFF8A5A00)) }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "ℹ Orijentacijski prikaz: centerline se može dodati na kartu s ulazom sidrenim na koordinate objekta. Preciznost ovisi o TopoDroid mjerenju i početnoj stanici.",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF5F6875)
        )
    }
}

private fun formatTopoMeters(value: Double): String =
    ((value * 10.0).roundToInt() / 10.0).toString()

private class TopoDroidSurveyAndroidView(context: Context) : View(context) {
    private var model: TopoDroidBridgeStore.SurveyViewerModel? = null
    private var profile: Boolean = false

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(45, 255, 255, 255)
        strokeWidth = 1.2f
        style = Paint.Style.STROKE
    }
    private val splayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(95, 160, 185, 210)
        strokeWidth = 1.6f
        style = Paint.Style.STROKE
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(45, 210, 235)
        strokeWidth = 4.4f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private val stationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(255, 214, 92)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
    }
    private var dynamicTextSize: Float = 24f
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(210, 220, 232)
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    fun setSurvey(newModel: TopoDroidBridgeStore.SurveyViewerModel, showProfile: Boolean) {
        model = newModel
        profile = showProfile
        val count = newModel.stations.size.coerceAtLeast(1)
        dynamicTextSize = (24f / (count / 20f).coerceAtLeast(1f)).coerceIn(8f, 24f)
        textPaint.textSize = dynamicTextSize
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = model ?: return
        if (width <= 0 || height <= 0) return

        val centerline = data.centerline
        if (centerline.isEmpty()) {
            canvas.drawText("Nema centerline mjerenja za prikaz", width / 2f, height / 2f, emptyPaint)
            return
        }

        val allSegments = if (profile) centerline else data.splays + centerline
        val xs = mutableListOf<Double>()
        val ys = mutableListOf<Double>()
        allSegments.forEach { segment ->
            if (profile) {
                xs += segment.chain1
                xs += segment.chain2
                ys += segment.z1
                ys += segment.z2
            } else {
                xs += segment.x1
                xs += segment.x2
                ys += segment.y1
                ys += segment.y2
            }
        }

        val minX = xs.minOrNull() ?: 0.0
        val maxX = xs.maxOrNull() ?: 1.0
        val minY = ys.minOrNull() ?: 0.0
        val maxY = ys.maxOrNull() ?: 1.0
        val rangeX = (maxX - minX).coerceAtLeast(1.0)
        val rangeY = (maxY - minY).coerceAtLeast(1.0)
        val pad = 36f
        val usableW = (width - pad * 2f).coerceAtLeast(1f)
        val usableH = (height - pad * 2f).coerceAtLeast(1f)
        val scale = minOf((usableW / rangeX).toFloat(), (usableH / rangeY).toFloat())
        val drawW = (rangeX * scale).toFloat()
        val drawH = (rangeY * scale).toFloat()
        val offsetX = (width - drawW) / 2f
        val offsetY = (height - drawH) / 2f

        fun mapX(x: Double): Float = offsetX + ((x - minX) * scale).toFloat()
        fun mapY(y: Double): Float = offsetY + ((maxY - y) * scale).toFloat()

        drawGrid(canvas, offsetX, offsetY, drawW, drawH)

        if (!profile) {
            data.splays.take(400).forEach { segment ->
                canvas.drawLine(mapX(segment.x1), mapY(segment.y1), mapX(segment.x2), mapY(segment.y2), splayPaint)
            }
        }

        centerline.forEach { segment ->
            val x1 = if (profile) segment.chain1 else segment.x1
            val y1 = if (profile) segment.z1 else segment.y1
            val x2 = if (profile) segment.chain2 else segment.x2
            val y2 = if (profile) segment.z2 else segment.y2
            canvas.drawLine(mapX(x1), mapY(y1), mapX(x2), mapY(y2), centerPaint)
        }

        data.stations.take(90).forEach { station ->
            val x = if (profile) station.chain else station.x
            val y = if (profile) station.z else station.y
            val sx = mapX(x)
            val sy = mapY(y)
            canvas.drawCircle(sx, sy, 5.5f, stationPaint)
            canvas.drawText(station.name, sx + 7f, sy - 7f, textPaint)
        }
    }

    private fun drawGrid(canvas: Canvas, left: Float, top: Float, width: Float, height: Float) {
        val steps = 4
        for (i in 0..steps) {
            val x = left + width * i / steps
            canvas.drawLine(x, top, x, top + height, gridPaint)
            val y = top + height * i / steps
            canvas.drawLine(left, y, left + width, y, gridPaint)
        }
    }
}


fun openAttachmentUri(context: Context, uriString: String) {
    val uri = Uri.parse(uriString)
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
    val baseIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newUri(context.contentResolver, "attachment", uri)
    }

    val packageManager = context.packageManager
    val candidates = packageManager.queryIntentActivities(baseIntent, PackageManager.MATCH_DEFAULT_ONLY)
        .filter { it.activityInfo.packageName != context.packageName }

    val target = candidates.firstOrNull()
    if (target == null) {
        Toast.makeText(
            context,
            "Nema druge aplikacije za otvaranje originala. Za TopoDroid ZIP koristi Pregled ili Dodaj topo plan na kartu.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    val explicitIntent = Intent(baseIntent).apply {
        setClassName(target.activityInfo.packageName, target.activityInfo.name)
    }
    runCatching { context.startActivity(explicitIntent) }
        .onFailure { Toast.makeText(context, "Ne mogu otvoriti original ovom aplikacijom", Toast.LENGTH_LONG).show() }
}

fun shareAttachmentUri(context: Context, uriString: String, filename: String) {
    val uri = Uri.parse(uriString)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(uri) ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, filename)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Podijeli datoteku")) }
        .onFailure { Toast.makeText(context, "Ne mogu podijeliti datoteku", Toast.LENGTH_LONG).show() }
}


fun shareFileFromCache(context: Context, file: File, mimeType: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, chooserTitle)) }
        .onFailure { Toast.makeText(context, "Ne mogu podijeliti datoteku", Toast.LENGTH_LONG).show() }
}
