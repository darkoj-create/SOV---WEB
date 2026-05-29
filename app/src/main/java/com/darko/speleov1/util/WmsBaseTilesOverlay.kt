package com.darko.speleov1.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.LruCache
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sinh

/**
 * Opaque WMS base-map renderer backed by its own file cache.
 *
 * This version is deliberately less aggressive than the first cached renderer: it draws the
 * current zoom first, uses cropped parent tiles only as a fallback for missing tiles, and keeps
 * prefetch small so WMS downloads/bitmap decoding do not compete with pan/zoom rendering.
 */
class WmsBaseTilesOverlay(
    config: WmsConfig
) : Overlay() {
    private val baseConfig = config.copy(transparent = false)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        isFilterBitmap = true
        alpha = 255
    }
    private val sourceKey = WmsPerformanceCache.sourceKey(baseConfig)
    private val fileExtension = WmsTileSource.preferredFileExtension(baseConfig)

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        WmsPerformanceCache.install(mapView.context)
        val bb: BoundingBox = mapView.boundingBox ?: return
        val z = floor(mapView.zoomLevelDouble).toInt().coerceIn(0, 19)
        // Viewport-snapshot path removed: the ad-hoc full-screen WMS GetMap was the slow part of
        // zoom in/out because DGU has to render an off-grid image every time. Parent-tile
        // fallback (already in drawZoom) covers missing tiles instantly from RAM.
        val result = drawZoom(canvas, mapView, bb, z, requestMissing = true)
        if (result.missingTiles.isNotEmpty()) {
            requestVisibleMissingTiles(result.missingTiles, result.centerTileX, result.centerTileY, mapView)
        }
        requestPrefetchRing(z, result.xMin, result.xMax, result.yMin, result.yMax, mapView)
    }

    private fun requestVisibleMissingTiles(
        missingTiles: List<MissingTile>,
        centerTileX: Double,
        centerTileY: Double,
        mapView: MapView
    ) {
        missingTiles.sortedBy { tile ->
            kotlin.math.abs(tile.x - centerTileX) + kotlin.math.abs(tile.y - centerTileY)
        }.forEach { tile ->
            val key = "$sourceKey/${tile.z}/${tile.x}/${tile.y}"
            requestTile(key, tile.z, tile.x, tile.y, mapView)
        }
        missingTiles.forEach { tile ->
            requestParentFallbacks(tile.z, tile.x, tile.y, mapView)
        }
    }

    private fun drawZoom(
        canvas: Canvas,
        mapView: MapView,
        bb: BoundingBox,
        z: Int,
        requestMissing: Boolean
    ): DrawPassResult {
        val xA = lonToTileX(bb.lonWest, z)
        val xB = lonToTileX(bb.lonEast, z)
        val yA = latToTileY(bb.latNorth, z)
        val yB = latToTileY(bb.latSouth, z)
        val xMin = min(xA, xB)
        val xMax = max(xA, xB)
        val yMin = min(yA, yB)
        val yMax = max(yA, yB)
        val centerTileX = lonToTileXDouble((bb.lonWest + bb.lonEast) / 2.0, z)
        val centerTileY = latToTileYDouble((bb.latNorth + bb.latSouth) / 2.0, z)
        val projection = mapView.projection ?: return DrawPassResult(xMin, xMax, yMin, yMax, centerTileX, centerTileY)
        val p1 = Point()
        val p2 = Point()
        val rect = Rect()
        val missingTiles = if (requestMissing) mutableListOf<MissingTile>() else null

        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val key = "$sourceKey/$z/$x/$y"
                val bitmap = memoryCache.get(key)
                if (bitmap == null) {
                    if (requestMissing) {
                        drawParentFallback(canvas, projection, z, x, y, p1, p2, rect)
                        missingTiles?.add(MissingTile(z, x, y))
                    }
                    continue
                }
                drawTileBitmap(canvas, projection, bitmap, z, x, y, p1, p2, rect)
            }
        }

        return DrawPassResult(
            xMin = xMin,
            xMax = xMax,
            yMin = yMin,
            yMax = yMax,
            centerTileX = centerTileX,
            centerTileY = centerTileY,
            missingTiles = missingTiles.orEmpty()
        )
    }

    private fun drawParentFallback(
        canvas: Canvas,
        projection: org.osmdroid.views.Projection,
        z: Int,
        x: Int,
        y: Int,
        p1: Point,
        p2: Point,
        rect: Rect
    ): Boolean {
        for (depth in 1..MAX_PARENT_FALLBACK_DEPTH) {
            val parentZ = z - depth
            if (parentZ < 0) continue
            val factor = 1 shl depth
            val parentX = x / factor
            val parentY = y / factor
            val key = "$sourceKey/$parentZ/$parentX/$parentY"
            val bitmap = memoryCache.get(key) ?: continue
            val tileRect = tileScreenRect(projection, z, x, y, p1, p2, rect) ?: continue
            val cropSize = 256 / factor
            val cropX = (x % factor) * cropSize
            val cropY = (y % factor) * cropSize
            val src = Rect(cropX, cropY, cropX + cropSize, cropY + cropSize)
            canvas.drawBitmap(bitmap, src, tileRect, paint)
            return true
        }
        return false
    }

    private fun drawTileBitmap(
        canvas: Canvas,
        projection: org.osmdroid.views.Projection,
        bitmap: Bitmap,
        z: Int,
        x: Int,
        y: Int,
        p1: Point,
        p2: Point,
        rect: Rect
    ) {
        val tileRect = tileScreenRect(projection, z, x, y, p1, p2, rect) ?: return
        canvas.drawBitmap(bitmap, null, tileRect, paint)
    }

    private fun tileScreenRect(
        projection: org.osmdroid.views.Projection,
        z: Int,
        x: Int,
        y: Int,
        p1: Point,
        p2: Point,
        rect: Rect
    ): Rect? {
        val north = tileYToLat(y, z)
        val south = tileYToLat(y + 1, z)
        val west = tileXToLon(x, z)
        val east = tileXToLon(x + 1, z)
        projection.toPixels(GeoPoint(north, west), p1)
        projection.toPixels(GeoPoint(south, east), p2)
        rect.left = min(p1.x, p2.x)
        rect.top = min(p1.y, p2.y)
        rect.right = max(p1.x, p2.x)
        rect.bottom = max(p1.y, p2.y)
        if (rect.right <= rect.left || rect.bottom <= rect.top) return null
        return rect
    }

    private fun requestParentFallbacks(z: Int, x: Int, y: Int, mapView: MapView) {
        for (depth in PARENT_FALLBACK_DEPTHS) {
            val parentZ = z - depth
            if (parentZ < 0) return
            val factor = 1 shl depth
            val parentX = x / factor
            val parentY = y / factor
            val key = "$sourceKey/$parentZ/$parentX/$parentY"
            if (memoryCache.get(key) == null) requestTile(key, parentZ, parentX, parentY, mapView, fallbackOnly = true)
        }
    }

    private fun requestPrefetchRing(z: Int, xMin: Int, xMax: Int, yMin: Int, yMax: Int, mapView: MapView) {
        val nMax = (1 shl z) - 1
        val pxMin = max(0, xMin - PREFETCH_MARGIN_TILES)
        val pxMax = min(nMax, xMax + PREFETCH_MARGIN_TILES)
        val pyMin = max(0, yMin - PREFETCH_MARGIN_TILES)
        val pyMax = min(nMax, yMax + PREFETCH_MARGIN_TILES)
        var queued = 0
        for (x in pxMin..pxMax) {
            for (y in pyMin..pyMax) {
                if (x in xMin..xMax && y in yMin..yMax) continue
                val key = "$sourceKey/$z/$x/$y"
                if (memoryCache.get(key) != null) continue
                requestTile(key, z, x, y, mapView, prefetchOnly = true)
                queued++
                if (queued >= MAX_PREFETCH_TILES) return
            }
        }
    }

    private fun requestTile(
        key: String,
        z: Int,
        x: Int,
        y: Int,
        mapView: MapView,
        prefetchOnly: Boolean = false,
        fallbackOnly: Boolean = false
    ) {
        if (memoryCache.get(key) != null) return
        if (!pending.add(key)) return
        val appContext = mapView.context.applicationContext
        val executor = when {
            prefetchOnly -> prefetchExecutor
            fallbackOnly -> fallbackExecutor
            else -> visibleExecutor
        }
        executor.execute {
            try {
                val cacheFile = WmsPerformanceCache.baseCacheFile(appContext, sourceKey, z, x, y, fileExtension)
                val cachedBitmap = WmsTileImageCache.decodeCachedBitmap(cacheFile, Bitmap.Config.RGB_565, rejectMostlyBlack = true)
                if (cachedBitmap != null) {
                    memoryCache.put(key, cachedBitmap)
                    if (!prefetchOnly) mapView.postInvalidate()
                } else {
                    val url = URL(WmsTileSource.buildTileUrl(baseConfig, z, x, y))
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 3000
                        readTimeout = 6500
                        requestMethod = "GET"
                        useCaches = true
                        setRequestProperty("User-Agent", "SOV Android/${appContext.packageName}")
                        setRequestProperty("Accept", "image/jpeg,image/png,image/*,*/*")
                        setRequestProperty("Cache-Control", "max-age=604800")
                    }
                    try {
                        val code = connection.responseCode
                        if (code in 200..299) {
                            val bytes = connection.inputStream.use { it.readBytes() }
                            WmsTileImageCache.decodeBytes(bytes, Bitmap.Config.RGB_565, rejectMostlyBlack = true)?.let { bitmap ->
                                WmsTileImageCache.writeCacheFile(cacheFile, bytes)
                                memoryCache.put(key, bitmap)
                                if (!prefetchOnly) mapView.postInvalidate()
                            }
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
            } catch (_: Throwable) {
                // Base WMS rendering is best-effort. Missing/no-network tiles must not crash the map.
            } finally {
                pending.remove(key)
            }
        }
    }

    private data class MissingTile(val z: Int, val x: Int, val y: Int)

    private data class DrawPassResult(
        val xMin: Int,
        val xMax: Int,
        val yMin: Int,
        val yMax: Int,
        val centerTileX: Double,
        val centerTileY: Double,
        val missingTiles: List<MissingTile> = emptyList()
    )

    /*
     * The functions below used to request tiles in screen scan order. Keeping the exact visible
     * queue center-first makes zooming feel sharper because the tile under the user's focus wins
     * worker slots before off-screen-adjacent fallback/prefetch work.
     */
    private fun lonToTileXDouble(lon: Double, z: Int): Double {
        val n = 2.0.pow(z.toDouble()).coerceAtLeast(1.0)
        return ((lon + 180.0) / 360.0 * n).coerceIn(0.0, n - 1.0)
    }

    private fun latToTileYDouble(lat: Double, z: Int): Double {
        val n = 2.0.pow(z.toDouble()).coerceAtLeast(1.0)
        val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
        return ((1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1 / cos(latRad)) / Math.PI) / 2.0 * n).coerceIn(0.0, n - 1.0)
    }

    companion object {
        private const val MAX_PARENT_FALLBACK_DEPTH = 6
        private val PARENT_FALLBACK_DEPTHS = intArrayOf(2, 4, 6)
        private const val PREFETCH_MARGIN_TILES = 1
        private const val MAX_PREFETCH_TILES = 12

        private val memoryCache = object : LruCache<String, Bitmap>(40 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }
        private val pending = Collections.synchronizedSet(mutableSetOf<String>())
        private val visibleExecutor = Executors.newFixedThreadPool(4) { runnable ->
            Thread(runnable, "sov-wms-base").apply { isDaemon = true }
        }
        private val fallbackExecutor = Executors.newFixedThreadPool(2) { runnable ->
            Thread(runnable, "sov-wms-base-fallback").apply { isDaemon = true }
        }
        private val prefetchExecutor = Executors.newFixedThreadPool(1) { runnable ->
            Thread(runnable, "sov-wms-base-prefetch").apply { isDaemon = true }
        }

        private fun lonToTileX(lon: Double, z: Int): Int {
            val n = 2.0.pow(z.toDouble()).toInt().coerceAtLeast(1)
            return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        }

        private fun latToTileY(lat: Double, z: Int): Int {
            val n = 2.0.pow(z.toDouble()).toInt().coerceAtLeast(1)
            val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
            val y = floor((1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1 / cos(latRad)) / Math.PI) / 2.0 * n).toInt()
            return y.coerceIn(0, n - 1)
        }

        private fun tileXToLon(x: Int, z: Int): Double = x / 2.0.pow(z.toDouble()) * 360.0 - 180.0

        private fun tileYToLat(y: Int, z: Int): Double {
            val n = Math.PI - 2.0 * Math.PI * y / 2.0.pow(z.toDouble())
            return Math.toDegrees(atan(sinh(n)))
        }
    }
}
