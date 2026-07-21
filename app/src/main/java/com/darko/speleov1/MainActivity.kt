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
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
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
import androidx.lifecycle.lifecycleScope
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
import com.darko.speleov1.util.SovNativeOfflineFolders
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

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val pendingExternalOpenUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        debugImportLog("intent: action=${intent.action} type=${intent.type} hasData=${intent.data != null}")
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // WMS performanse: drzi TCP/TLS veze prema tile serverima zive i u poolu, umjesto da
        // svaka pločica radi novi handshake. http.maxConnections >= broj WMS download threadova
        // (base 4 + fallback 2 + prefetch 1) da se veze ne istiskuju jedna drugu.
        runCatching {
            System.setProperty("http.keepAlive", "true")
            System.setProperty("http.maxConnections", "8")
        }
        // SOV Observability v2: baseline Crashlytics context for startup/fatal crashes.
        runCatching {
            FirebaseCrashlytics.getInstance().setCustomKey("app_version", BuildConfig.VERSION_NAME)
            FirebaseCrashlytics.getInstance().setCustomKey("current_screen", "Startup")
            FirebaseCrashlytics.getInstance().setCustomKey("last_action", "MainActivity.onCreate")
        }
        queueExternalOpenIntent(intent)
        OfflineTileManager.configureOsmdroidPaths(this)
        // Ne skeniramo velike korisničke foldere sinkrono u onCreate.
        // To je znalo smrznuti startup na ~49% kod velikih GPX/KML/MBTiles/Moje baze.
        Configuration.getInstance().userAgentValue = packageName
        enableEdgeToEdge()
        setContent {
            var appThemeMode by rememberSaveable {
                mutableStateOf(AppSessionStore.load(this@MainActivity).appThemeMode)
            }
            val systemDark = isSystemInDarkTheme()
            SpeleoTheme(darkTheme = appThemeMode.isDark(systemDark)) {
                SpeleoApp(
                    viewModel = viewModel,
                    incomingOpenUri = pendingExternalOpenUri.value,
                    onIncomingOpenConsumed = { pendingExternalOpenUri.value = null },
                    appThemeMode = appThemeMode,
                    onThemeModeChange = { mode ->
                        appThemeMode = mode
                        val current = AppSessionStore.load(this@MainActivity)
                        AppSessionStore.save(this@MainActivity, current.copy(appThemeMode = mode))
                    }
                )
            }
        }
    }

    private fun debugImportLog(message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (throwable != null) {
            Log.w("SOV_IMPORT", message, throwable)
        } else {
            Log.d("SOV_IMPORT", message)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        queueExternalOpenIntent(intent)
    }

    private fun queueExternalOpenIntent(intent: Intent?) {
        val action = intent?.action ?: return
        val uris = when (action) {
            Intent.ACTION_VIEW -> listOfNotNull(intent.data) + getClipDataUris(intent)
            Intent.ACTION_SEND -> listOfNotNull(getStreamUri(intent)) + getClipDataUris(intent)
            Intent.ACTION_SEND_MULTIPLE -> getStreamUris(intent)
            else -> emptyList()
        }.distinct()

        val firstSupported = uris.firstOrNull { uri -> isSupportedOpenUri(intent, uri) } ?: return
        lifecycleScope.launch {
            val stableUri = withContext(Dispatchers.IO) {
                copyExternalOpenUriToImportCache(intent, firstSupported)
            } ?: firstSupported
            pendingExternalOpenUri.value = stableUri
        }
    }

    @Suppress("DEPRECATION")
    private fun getStreamUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    @Suppress("DEPRECATION")
    private fun getStreamUris(intent: Intent): List<Uri> {
        val fromExtras = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }.orEmpty()
        return (fromExtras + getClipDataUris(intent)).distinct()
    }

    private fun getClipDataUris(intent: Intent): List<Uri> {
        return (0 until (intent.clipData?.itemCount ?: 0)).mapNotNull { index ->
            intent.clipData?.getItemAt(index)?.uri
        }
    }

    private fun copyExternalOpenUriToImportCache(intent: Intent, uri: Uri): Uri? {
        return runCatching {
            val scheme = uri.scheme.orEmpty().lowercase(Locale.ROOT)
            if (scheme == "file") return@runCatching uri

            val displayName = buildImportCacheDisplayName(intent, uri)
            val safeName = sanitizeImportCacheFileName(displayName)
            val importCacheDir = File(cacheDir, "external_imports").apply { mkdirs() }
            cleanupOldExternalImports(importCacheDir)
            val target = uniqueImportCacheFile(importCacheDir, safeName)

            tryTakeReadPermission(uri)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            if (target.length() <= 0L) return@runCatching null
            Uri.fromFile(target)
        }.onFailure { throwable ->
            debugImportLog("Cannot copy external import URI to cache: scheme=${uri.scheme.orEmpty()}", throwable)
            runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
        }.getOrNull()
    }

    private fun buildImportCacheDisplayName(intent: Intent, uri: Uri): String {
        val rawName = getOpenableDisplayName(uri)
            ?: uri.lastPathSegment
            ?: "sov_import_${System.currentTimeMillis()}"
        val cleanName = rawName.substringAfterLast('/').substringAfterLast('\\').ifBlank { "sov_import_${System.currentTimeMillis()}" }
        if (cleanName.substringAfterLast('.', missingDelimiterValue = "").isNotBlank()) return cleanName

        val type = (intent.type ?: runCatching { contentResolver.getType(uri) }.getOrNull()).orEmpty().lowercase(Locale.ROOT)
        val extension = when {
            type.contains("gpx") -> "gpx"
            type.contains("kml") || type.contains("google-earth") -> "kml"
            type.contains("kmz") -> "kmz"
            type.contains("geo+json") || type.contains("geojson") -> "geojson"
            type.contains("geopackage") -> "gpkg"
            type.contains("mbtiles") -> "mbtiles"
            type.contains("shapefile") -> "shp"
            type.contains("tiff") || type.contains("geotiff") -> "tif"
            type.contains("sov.field-package") -> "sovpkg"
            else -> ""
        }
        return if (extension.isBlank()) cleanName else "$cleanName.$extension"
    }

    private fun sanitizeImportCacheFileName(value: String): String {
        val clean = value
            .replace(Regex("[\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim('_', '.', ' ')
        return clean.ifBlank { "sov_import_${System.currentTimeMillis()}" }
    }

    private fun uniqueImportCacheFile(dir: File, fileName: String): File {
        val base = fileName.substringBeforeLast('.', fileName).ifBlank { "sov_import" }
        val ext = fileName.substringAfterLast('.', missingDelimiterValue = "")
        var candidate = File(dir, fileName)
        var counter = 1
        while (candidate.exists()) {
            val nextName = if (ext.isBlank()) "${base}_${counter}" else "${base}_${counter}.${ext}"
            candidate = File(dir, nextName)
            counter++
        }
        return candidate
    }

    private fun cleanupOldExternalImports(dir: File) {
        runCatching {
            dir.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?.drop(40)
                ?.forEach { it.delete() }
        }
    }

    private fun tryTakeReadPermission(uri: Uri) {
        if (!uri.scheme.equals("content", ignoreCase = true)) return
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure { throwable ->
            debugImportLog("External URI has transient or non-persistable read permission (${throwable.javaClass.simpleName})")
        }
    }

    private fun isSupportedOpenUri(intent: Intent, uri: Uri): Boolean {
        val type = intent.type.orEmpty().lowercase(Locale.ROOT)
        val uriText = uri.toString().lowercase(Locale.ROOT)
        val displayName = getOpenableDisplayName(uri).orEmpty().lowercase(Locale.ROOT)
        val candidate = "$uriText $displayName"
        // v1.4.41b: generički MIME tipovi (json/xml/txt/zip/octet-stream)
        // više sami po sebi nisu dovoljni. Provideri ih često koriste za nepovezane
        // datoteke, pa SOV provjerava stvarni naziv/ekstenziju prije uvoza.
        return type.contains("kml") ||
            type.contains("kmz") ||
            type.contains("gpx") ||
            type.contains("geo+json") ||
            type.contains("geojson") ||
            type.contains("geopackage") ||
            type.contains("mbtiles") ||
            type.contains("shapefile") ||
            type.contains("tiff") ||
            type.contains("geotiff") ||
            type.contains("vnd.sov.field-package") ||
            candidate.endsWithSupportedImportExtension()
    }

    private fun String.endsWithSupportedImportExtension(): Boolean {
        val supported = listOf(
            ".kml", ".kmz", ".gpx", ".geojson",
            ".gpkg", ".geopackage", ".mbtiles", ".shp",
            ".tif", ".tiff", ".sovpkg"
        )
        return supported.any { ext -> this.endsWith(ext) || this.contains("$ext?") || this.contains("$ext%") }
    }

    private fun getOpenableDisplayName(uri: Uri): String? {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
        }.getOrNull()
    }
}
