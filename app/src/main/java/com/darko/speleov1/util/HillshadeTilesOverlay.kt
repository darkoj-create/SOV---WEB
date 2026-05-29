package com.darko.speleov1.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.LruCache
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

class HillshadeTilesOverlay(
    context: Context,
    opacityPercent: Int
) : Overlay() {
    private val appContext = context.applicationContext

    // Smooth-cache hillshade.
    // Uses a global hillshade source first and keeps OSM US hillshade as fallback. The old single
    // source sometimes returned blank/missing tiles for parts of Croatia, which made the relief
    // look like it randomly disappeared while panning.
    private val maxShadowAlpha = ((opacityPercent.coerceIn(0, 75) / 100f) * 255f).toInt().coerceIn(0, 192)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        isFilterBitmap = true
    }
    private val diskCacheDir: File = File(appContext.cacheDir, "sov_hillshade_tiles_v4").apply { mkdirs() }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || maxShadowAlpha <= 0) return
        val bb: BoundingBox = mapView.boundingBox ?: return
        val z = mapView.zoomLevelDouble.roundToInt().coerceIn(HillshadePrefs.MIN_ZOOM, HillshadePrefs.MAX_ZOOM)

        // Draw only the active zoom level. Lower zoom tiles are used only as a per-tile fallback
        // for missing current tiles, otherwise hillshade gets redrawn in full 2-3 times per frame.
        drawZoom(canvas, mapView, bb, z, requestMissing = true, prefetch = true)
    }

    private fun drawZoom(
        canvas: Canvas,
        mapView: MapView,
        bb: BoundingBox,
        z: Int,
        requestMissing: Boolean,
        prefetch: Boolean
    ) {
        val xA = lonToTileX(bb.lonWest, z)
        val xB = lonToTileX(bb.lonEast, z)
        val yA = latToTileY(bb.latNorth, z)
        val yB = latToTileY(bb.latSouth, z)
        val xMin = min(xA, xB)
        val xMax = max(xA, xB)
        val yMin = min(yA, yB)
        val yMax = max(yA, yB)
        val projection = mapView.projection
        val p1 = Point()
        val p2 = Point()
        val rect = Rect()

        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val key = cacheKey(maxShadowAlpha, z, x, y)
                val bitmap = getMemoryTile(key)
                if (bitmap == null) {
                    if (requestMissing) {
                        drawParentFallbackBitmap(canvas, projection, z, x, y, p1, p2, rect)
                        requestParentFallbacks(z, x, y, mapView)
                        requestTile(key, z, x, y, mapView)
                    }
                    continue
                }
                drawTileBitmap(canvas, projection, bitmap, z, x, y, p1, p2, rect)
            }
        }

        if (prefetch) {
            val limit = (1 shl z) - 1
            val pxMin = (xMin - PREFETCH_MARGIN_TILES).coerceAtLeast(0)
            val pxMax = (xMax + PREFETCH_MARGIN_TILES).coerceAtMost(limit)
            val pyMin = (yMin - PREFETCH_MARGIN_TILES).coerceAtLeast(0)
            val pyMax = (yMax + PREFETCH_MARGIN_TILES).coerceAtMost(limit)
            var queued = 0
            for (x in pxMin..pxMax) {
                for (y in pyMin..pyMax) {
                    if (x in xMin..xMax && y in yMin..yMax) continue
                    val key = cacheKey(maxShadowAlpha, z, x, y)
                    if (getMemoryTile(key) == null) {
                        requestTile(key, z, x, y, mapView, prefetchOnly = true)
                        queued++
                        if (queued >= MAX_PREFETCH_TILES) return
                    }
                }
            }
        }
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
        if (rect.right <= rect.left || rect.bottom <= rect.top) return
        canvas.drawBitmap(bitmap, null, rect, paint)
    }

    private fun drawParentFallbackBitmap(
        canvas: Canvas,
        projection: org.osmdroid.views.Projection,
        z: Int,
        x: Int,
        y: Int,
        p1: Point,
        p2: Point,
        rect: Rect
    ): Boolean {
        if (drawParentFallbackBitmap(canvas, projection, z, x, y, parentDelta = 1, p1, p2, rect)) return true
        return drawParentFallbackBitmap(canvas, projection, z, x, y, parentDelta = 2, p1, p2, rect)
    }

    private fun drawParentFallbackBitmap(
        canvas: Canvas,
        projection: org.osmdroid.views.Projection,
        z: Int,
        x: Int,
        y: Int,
        parentDelta: Int,
        p1: Point,
        p2: Point,
        rect: Rect
    ): Boolean {
        val parentZ = z - parentDelta
        if (parentZ < HillshadePrefs.MIN_ZOOM) return false
        val scale = 1 shl parentDelta
        val parentX = x / scale
        val parentY = y / scale
        val parent = getMemoryTile(cacheKey(maxShadowAlpha, parentZ, parentX, parentY)) ?: return false

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
        if (rect.right <= rect.left || rect.bottom <= rect.top) return false

        val childOffsetX = (x - parentX * scale).coerceIn(0, scale - 1)
        val childOffsetY = (y - parentY * scale).coerceIn(0, scale - 1)
        val source = Rect(
            parent.width * childOffsetX / scale,
            parent.height * childOffsetY / scale,
            parent.width * (childOffsetX + 1) / scale,
            parent.height * (childOffsetY + 1) / scale
        )
        canvas.drawBitmap(parent, source, rect, paint)
        return true
    }

    private fun requestParentFallbacks(z: Int, x: Int, y: Int, mapView: MapView) {
        if (z - 1 >= HillshadePrefs.MIN_ZOOM) {
            val parentZ = z - 1
            val parentX = x / 2
            val parentY = y / 2
            val key = cacheKey(maxShadowAlpha, parentZ, parentX, parentY)
            if (getMemoryTile(key) == null) requestTile(key, parentZ, parentX, parentY, mapView, prefetchOnly = false)
        }
        if (z - 2 >= HillshadePrefs.MIN_ZOOM) {
            val parentZ = z - 2
            val parentX = x / 4
            val parentY = y / 4
            val key = cacheKey(maxShadowAlpha, parentZ, parentX, parentY)
            if (getMemoryTile(key) == null) requestTile(key, parentZ, parentX, parentY, mapView, prefetchOnly = false)
        }
    }

    private fun requestTile(key: String, z: Int, x: Int, y: Int, mapView: MapView, prefetchOnly: Boolean = false) {
        if (getMemoryTile(key) != null) return
        if (!pending.add(key)) return
        executor.execute {
            try {
                readDiskTile(key)?.let { bitmap ->
                    putMemoryTile(key, bitmap)
                    if (!prefetchOnly) requestMapRedraw(mapView)
                    return@execute
                }

                for (urlText in hillshadeUrls(z, x, y)) {
                    val bitmap = downloadHillshade(urlText) ?: continue
                    val processed = bitmap.toShadowOnlyBitmap(maxShadowAlpha)
                    putMemoryTile(key, processed)
                    writeDiskTile(key, processed)
                    if (!prefetchOnly) requestMapRedraw(mapView)
                    return@execute
                }
            } catch (_: Throwable) {
                // Silent fallback: missing/no-network hillshade tiles must never affect the base map.
            } finally {
                pending.remove(key)
            }
        }
    }

    private fun downloadHillshade(urlText: String): Bitmap? {
        val connection = (URL(urlText).openConnection() as HttpURLConnection).apply {
            connectTimeout = 3500
            readTimeout = 6500
            requestMethod = "GET"
            useCaches = true
            setRequestProperty("User-Agent", "SOV Android/${appContext.packageName}")
            setRequestProperty("Accept", "image/png,image/jpeg,image/*,*/*")
            setRequestProperty("Cache-Control", "max-age=604800")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) return null
            connection.inputStream.use { input -> BitmapFactory.decodeStream(input) }
        } catch (_: Throwable) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun readDiskTile(key: String): Bitmap? {
        val file = diskFileFor(key)
        if (!file.exists() || file.length() <= 0L) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Throwable) {
            null
        }
    }

    private fun writeDiskTile(key: String, bitmap: Bitmap) {
        try {
            val file = diskFileFor(key)
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            FileOutputStream(tmp).use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 90, output) }
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
            trimDiskCacheIfNeeded()
        } catch (_: Throwable) {
            // Cache is best-effort only.
        }
    }

    private fun diskFileFor(key: String): File = File(diskCacheDir, key.replace('/', '_') + ".png")

    private fun trimDiskCacheIfNeeded() {
        val files = diskCacheDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") } ?: return
        if (files.size <= MAX_DISK_TILES) return
        files.sortedBy { it.lastModified() }
            .take(files.size - MAX_DISK_TILES)
            .forEach { runCatching { it.delete() } }
    }

    private fun Bitmap.toShadowOnlyBitmap(maxAlpha: Int): Bitmap {
        val source = if (config == Bitmap.Config.ARGB_8888) this else copy(Bitmap.Config.ARGB_8888, false)
        val widthPx = source.width
        val heightPx = source.height
        val pixels = IntArray(widthPx * heightPx)
        source.getPixels(pixels, 0, widthPx, 0, 0, widthPx, heightPx)

        for (i in pixels.indices) {
            val c = pixels[i]
            val sourceAlpha = Color.alpha(c)
            if (sourceAlpha == 0) {
                pixels[i] = Color.TRANSPARENT
                continue
            }
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val luminance = ((r * 0.299f) + (g * 0.587f) + (b * 0.114f)).toInt().coerceIn(0, 255)
            val rawDarkness = ((255 - luminance) / 255f).coerceIn(0f, 1f)
            val visibleDarkness = ((rawDarkness - 0.04f) / 0.96f).coerceIn(0f, 1f)
            val boostedDarkness = Math.pow(visibleDarkness.toDouble(), 0.50).toFloat()
            val alphaFactor = sourceAlpha / 255f
            val shadowAlpha = (boostedDarkness * maxAlpha * alphaFactor).toInt().coerceIn(0, maxAlpha)
            pixels[i] = if (shadowAlpha <= 3) Color.TRANSPARENT else Color.argb(shadowAlpha, 0, 0, 0)
        }
        return Bitmap.createBitmap(pixels, widthPx, heightPx, Bitmap.Config.ARGB_8888)
    }

    override fun onDetach(mapView: MapView?) {
        super.onDetach(mapView)
    }

    private fun lonToTileX(lon: Double, z: Int): Int {
        val limit = (1 shl z) - 1
        return floor((lon + 180.0) / 360.0 * (1 shl z)).toInt().coerceIn(0, limit)
    }

    private fun latToTileY(lat: Double, z: Int): Int {
        val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        val n = Math.pow(2.0, z.toDouble())
        val limit = (1 shl z) - 1
        return floor((1.0 - ln(tan(latRad) + 1 / cos(latRad)) / Math.PI) / 2.0 * n).toInt().coerceIn(0, limit)
    }

    private fun tileXToLon(x: Int, z: Int): Double = x / Math.pow(2.0, z.toDouble()) * 360.0 - 180.0

    private fun tileYToLat(y: Int, z: Int): Double {
        val n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z.toDouble())
        return Math.toDegrees(atan(sinh(n)))
    }

    companion object {
        private const val PREFETCH_MARGIN_TILES = 1
        private const val MAX_PREFETCH_TILES = 18
        private const val MAX_DISK_TILES = 5200
        private val memoryCache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }
        private val pending = Collections.synchronizedSet(mutableSetOf<String>())
        private val lastInvalidateAt = AtomicLong(0L)
        private val executor = Executors.newFixedThreadPool(2) { runnable ->
            Thread(runnable, "sov-hillshade-cache").apply { isDaemon = true }
        }

        private fun getMemoryTile(key: String): Bitmap? = synchronized(memoryCache) { memoryCache.get(key) }

        private fun putMemoryTile(key: String, bitmap: Bitmap) {
            synchronized(memoryCache) { memoryCache.put(key, bitmap) }
        }

        private fun cacheKey(alpha: Int, z: Int, x: Int, y: Int): String = "a${alpha}/z${z}/x${x}/y${y}"

        private fun requestMapRedraw(mapView: MapView) {
            val now = System.currentTimeMillis()
            val previous = lastInvalidateAt.get()
            if (now - previous >= 120L && lastInvalidateAt.compareAndSet(previous, now)) {
                mapView.postInvalidate()
            }
        }

        private fun hillshadeUrls(z: Int, x: Int, y: Int): List<String> = listOf(
            "https://server.arcgisonline.com/ArcGIS/rest/services/Elevation/World_Hillshade/MapServer/tile/$z/$y/$x",
            "https://tiles.openstreetmap.us/raster/hillshade/$z/$x/$y.jpg"
        )
    }
}
