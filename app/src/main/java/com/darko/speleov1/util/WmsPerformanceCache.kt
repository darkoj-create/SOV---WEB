package com.darko.speleov1.util

import android.content.Context
import android.graphics.Bitmap
import android.net.http.HttpResponseCache
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Small WMS performance layer for interactive map use.
 *
 * It does not replace osmdroid's own tile provider. Instead it installs Android's
 * HttpURLConnection response cache and gently warms the tiles around the current
 * viewport, so base WMS layers feel closer to dedicated map apps when panning.
 */
object WmsPerformanceCache {
    private const val HTTP_CACHE_MB = 128L
    private const val BASE_CACHE_MAX_MB = 384L // Stored under cacheDir; Android/osmdroid may evict cache files when needed.
    private const val CACHE_SCHEMA_VERSION = "v2"
    private const val PREFETCH_MARGIN_TILES = 1
    private const val MAX_PREFETCH_TILES_PER_VIEWPORT = 16

    @Volatile private var installed = false
    @Volatile private var lastViewportKey: String? = null

    private val pending = Collections.synchronizedSet(mutableSetOf<String>())
    private val recent = Collections.synchronizedMap(object : LinkedHashMap<String, Long>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > 1800
    })
    private val executor = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "sov-wms-prefetch").apply { isDaemon = true }
    }

    fun install(context: Context) {
        if (installed) return
        synchronized(this) {
            if (installed) return
            runCatching {
                val dir = File(context.cacheDir, "sov_http_wms_cache").apply { mkdirs() }
                if (HttpResponseCache.getInstalled() == null) {
                    HttpResponseCache.install(dir, HTTP_CACHE_MB * 1024L * 1024L)
                }
                installed = true
            }.onFailure {
                // Cache is an optimization only. WMS must still work if Android refuses the cache.
                installed = true
            }
        }
    }

    fun prefetchVisible(mapView: MapView, config: WmsConfig) {
        val context = mapView.context ?: return
        install(context)
        val bb: BoundingBox = mapView.boundingBox ?: return
        val z = mapView.zoomLevelDouble.roundToInt().coerceIn(0, 19)
        val worldMax = (1 shl z) - 1
        val xWest = WmsTileSource.lonToTileX(bb.lonWest, z).coerceIn(0, worldMax)
        val xEast = WmsTileSource.lonToTileX(bb.lonEast, z).coerceIn(0, worldMax)
        val yNorth = WmsTileSource.latToTileY(bb.latNorth, z).coerceIn(0, worldMax)
        val ySouth = WmsTileSource.latToTileY(bb.latSouth, z).coerceIn(0, worldMax)
        val xMin = max(0, min(xWest, xEast) - PREFETCH_MARGIN_TILES)
        val xMax = min(worldMax, max(xWest, xEast) + PREFETCH_MARGIN_TILES)
        val yMin = max(0, min(yNorth, ySouth) - PREFETCH_MARGIN_TILES)
        val yMax = min(worldMax, max(yNorth, ySouth) + PREFETCH_MARGIN_TILES)
        val sourceKey = sourceKey(config)
        val fileExtension = WmsTileSource.preferredFileExtension(config)
        val viewportKey = "$sourceKey|$z|$xMin|$xMax|$yMin|$yMax"
        if (viewportKey == lastViewportKey) return
        lastViewportKey = viewportKey

        val centerX = (xMin + xMax) / 2.0
        val centerY = (yMin + yMax) / 2.0
        val candidates = buildList {
            for (x in xMin..xMax) {
                for (y in yMin..yMax) {
                    add(x to y)
                }
            }
        }.sortedBy { (x, y) ->
            kotlin.math.abs(x - centerX) + kotlin.math.abs(y - centerY)
        }.take(MAX_PREFETCH_TILES_PER_VIEWPORT)

        candidates.forEach { (x, y) ->
            val key = "$sourceKey/$z/$x/$y"
            if (recent.containsKey(key) || !pending.add(key)) return@forEach
            executor.execute {
                try {
                    val cacheFile = baseCacheFile(context, sourceKey, z, x, y, fileExtension)
                    if (cacheFile.exists() && cacheFile.length() > 0L) {
                        recent[key] = System.currentTimeMillis()
                        return@execute
                    }
                    val url = URL(WmsTileSource.buildTileUrl(config, z, x, y))
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 3000
                        readTimeout = 6500
                        requestMethod = "GET"
                        useCaches = true
                        setRequestProperty("User-Agent", "SOV Android/${context.packageName}")
                        setRequestProperty("Accept", "image/jpeg,image/png,image/*,*/*")
                        setRequestProperty("Cache-Control", "max-age=604800")
                    }
                    try {
                        val code = connection.responseCode
                        if (code in 200..299) {
                            val bytes = connection.inputStream.use { it.readBytes() }
                            WmsTileImageCache.decodeBytes(bytes, Bitmap.Config.RGB_565, rejectMostlyBlack = true)?.let {
                                WmsTileImageCache.writeCacheFile(cacheFile, bytes)
                                recent[key] = System.currentTimeMillis()
                            }
                        }
                    } finally {
                        connection.disconnect()
                    }
                } catch (_: Throwable) {
                    // Best effort only. Prefetch must never affect map stability.
                } finally {
                    pending.remove(key)
                }
            }
        }
    }

    internal fun sourceKey(config: WmsConfig): String =
        (CACHE_SCHEMA_VERSION + "|" + config.baseUrl + "|" + config.layers + "|" + config.crs + "|" + config.version + "|" + config.styles + "|" + config.transparent + "|" + WmsTileSource.preferredImageFormat(config)).hashCode().toString()

    internal fun overlayCacheFile(context: Context, sourceKey: String, z: Int, x: Int, y: Int, extension: String): File {
        val cleanExt = extension.trimStart('.').ifBlank { "png" }
        return File(context.cacheDir, "sov_wms_overlay_cache/$sourceKey/$z/$x/$y.$cleanExt")
    }

    internal fun baseCacheFile(context: Context, sourceKey: String, z: Int, x: Int, y: Int, extension: String): File {
        val cleanExt = extension.trimStart('.').ifBlank { "png" }
        return File(context.cacheDir, "sov_wms_base_cache/$sourceKey/$z/$x/$y.$cleanExt")
    }
}
