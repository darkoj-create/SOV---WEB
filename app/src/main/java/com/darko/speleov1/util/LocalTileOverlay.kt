package com.darko.speleov1.util

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.LruCache
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.io.File
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh

class LocalTileOverlay(private val root: File) : Overlay() {
    private val cache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        isFilterBitmap = true
    }
    private val mbtilesFile: File? = detectMbtilesFile(root)
    private val decodeExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "sov-local-tile-decode").apply { isDaemon = true }
    }
    private val pending = Collections.synchronizedSet(mutableSetOf<String>())
    private val zoomLoadPending = AtomicBoolean(false)

    @Volatile private var cachedZoomLevels: List<Int> = emptyList()
    @Volatile private var cachedRootStamp: Long = Long.MIN_VALUE
    @Volatile private var preferredDecodeConfig: Bitmap.Config? = null
    private var db: SQLiteDatabase? = null

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !root.exists()) return
        val zoomDirs = availableZoomLevels(mapView) ?: return
        if (zoomDirs.isEmpty()) return

        val requestedZoom = mapView.zoomLevelDouble.roundToInt()
        val z = chooseZoom(requestedZoom, zoomDirs)
        val bb: BoundingBox = mapView.boundingBox ?: return
        val xMin = lonToTileX(bb.lonWest, z)
        val xMax = lonToTileX(bb.lonEast, z)
        val yMin = latToTileY(bb.latNorth, z)
        val yMax = latToTileY(bb.latSouth, z)
        val centerX = (xMin + xMax) / 2.0
        val centerY = (yMin + yMax) / 2.0

        val projection = mapView.projection ?: return
        val p1 = Point()
        val p2 = Point()
        val rect = Rect()
        val missing = ArrayList<TileCoord>(24)

        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val key = tileCacheKey(z, x, y)
                val bitmap = getCachedBitmap(key)
                if (bitmap == null) {
                    drawParentFallback(canvas, projection, z, x, y, p1, p2, rect)
                    missing += TileCoord(z, x, y)
                    continue
                }
                drawTileBitmap(canvas, projection, bitmap, z, x, y, p1, p2, rect)
            }
        }

        requestDecodeTiles(missing, centerX, centerY, mapView, visible = true)
        prefetchTiles(z, xMin, xMax, yMin, yMax, mapView)
    }

    override fun onDetach(mapView: MapView?) {
        cache.evictAll()
        pending.clear()
        synchronized(this) {
            runCatching { db?.close() }
            db = null
        }
        decodeExecutor.shutdownNow()
        super.onDetach(mapView)
    }

    private fun detectMbtilesFile(root: File): File? {
        if (root.isFile && root.extension.equals("mbtiles", ignoreCase = true)) return root
        return root.listFiles()?.firstOrNull { it.isFile && it.extension.equals("mbtiles", ignoreCase = true) }
    }

    private fun availableZoomLevels(mapView: MapView): List<Int>? {
        val stamp = mbtilesFile?.lastModified() ?: root.lastModified()
        if (cachedZoomLevels.isNotEmpty() && cachedRootStamp == stamp) return cachedZoomLevels
        if (mbtilesFile != null) {
            requestMbtilesZoomLevels(stamp, mapView)
            return null
        }
        val zooms = root.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory }
            ?.mapNotNull { it.name.toIntOrNull() }
            ?.sorted()
            ?.toList()
            .orEmpty()
        cachedZoomLevels = zooms
        cachedRootStamp = stamp
        return zooms
    }

    private fun requestMbtilesZoomLevels(stamp: Long, mapView: MapView) {
        val file = mbtilesFile ?: return
        if (!zoomLoadPending.compareAndSet(false, true)) return
        decodeExecutor.execute {
            try {
                val zooms = queryMbtilesZoomLevels(file)
                cachedZoomLevels = zooms
                cachedRootStamp = stamp
                MapInvalidateCoalescer.requestInvalidate(mapView)
            } finally {
                zoomLoadPending.set(false)
            }
        }
    }

    private fun queryMbtilesZoomLevels(file: File): List<Int> {
        val database = openDb(file) ?: return emptyList()
        return runCatching {
            database.rawQuery("SELECT DISTINCT zoom_level FROM tiles ORDER BY zoom_level", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getInt(0))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun chooseZoom(requestedZoom: Int, availableZooms: List<Int>): Int {
        availableZooms.firstOrNull { it == requestedZoom }?.let { return it }
        val lower = availableZooms.filter { it <= requestedZoom }.maxOrNull()
        if (lower != null) return lower
        return availableZooms.minOrNull() ?: requestedZoom
    }

    private fun requestDecodeTiles(
        tiles: List<TileCoord>,
        centerX: Double,
        centerY: Double,
        mapView: MapView,
        visible: Boolean
    ) {
        if (tiles.isEmpty()) return
        tiles.sortedBy { tile -> kotlin.math.abs(tile.x - centerX) + kotlin.math.abs(tile.y - centerY) }
            .take(if (visible) MAX_VISIBLE_DECODE_TILES else MAX_PREFETCH_DECODE_TILES)
            .forEach { tile -> requestDecodeTile(tile, mapView, visible) }
    }

    private fun requestDecodeTile(tile: TileCoord, mapView: MapView, visible: Boolean) {
        val key = tileCacheKey(tile.z, tile.x, tile.y)
        if (getCachedBitmap(key) != null) return
        if (!pending.add(key)) return
        decodeExecutor.execute {
            try {
                val bitmap = loadTileBitmap(tile.z, tile.x, tile.y)
                if (bitmap != null) {
                    putCachedBitmap(key, WmsTileImageCache.toHardware(bitmap))
                    if (visible) MapInvalidateCoalescer.requestInvalidate(mapView)
                }
            } catch (_: Throwable) {
                // Offline tile decode is best-effort; missing/corrupt local tiles must not crash the map.
            } finally {
                pending.remove(key)
            }
        }
    }

    private fun prefetchTiles(z: Int, xMin: Int, xMax: Int, yMin: Int, yMax: Int, mapView: MapView) {
        val limit = (1 shl z) - 1
        val pxMin = (xMin - 1).coerceAtLeast(0)
        val pxMax = (xMax + 1).coerceAtMost(limit)
        val pyMin = (yMin - 1).coerceAtLeast(0)
        val pyMax = (yMax + 1).coerceAtMost(limit)
        val centerX = (xMin + xMax) / 2.0
        val centerY = (yMin + yMax) / 2.0
        val tiles = ArrayList<TileCoord>(32)
        for (x in pxMin..pxMax) {
            for (y in pyMin..pyMax) {
                if (x in xMin..xMax && y in yMin..yMax) continue
                val key = tileCacheKey(z, x, y)
                if (getCachedBitmap(key) == null) tiles += TileCoord(z, x, y)
                if (tiles.size >= MAX_PREFETCH_DECODE_TILES) break
            }
            if (tiles.size >= MAX_PREFETCH_DECODE_TILES) break
        }
        requestDecodeTiles(tiles, centerX, centerY, mapView, visible = false)
    }

    private fun loadTileBitmap(z: Int, x: Int, y: Int): Bitmap? {
        return if (mbtilesFile != null) {
            loadMbtilesBitmap(z, x, y)
        } else {
            val file = File(root, "$z/$x/$y.png")
            if (!file.exists() || file.length() <= 0L) null else decodeLocalBytes(runCatching { file.readBytes() }.getOrNull())
        }
    }

    private fun decodeLocalBytes(bytes: ByteArray?): Bitmap? {
        if (bytes == null || bytes.isEmpty()) return null
        val preferred = preferredDecodeConfig ?: Bitmap.Config.ARGB_8888
        val options = BitmapFactory.Options().apply { inPreferredConfig = preferred }
        val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }.getOrNull() ?: return null
        if (preferredDecodeConfig == null) {
            preferredDecodeConfig = if (bitmap.hasAlpha()) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        }
        return bitmap
    }

    @Synchronized
    private fun openDb(file: File): SQLiteDatabase? {
        db?.let { if (it.isOpen) return it }
        return runCatching {
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        }.getOrNull()?.also { db = it }
    }

    private fun loadMbtilesBitmap(z: Int, x: Int, y: Int): Bitmap? {
        val file = mbtilesFile ?: return null
        val dbRef = openDb(file) ?: return null
        val tmsY = ((1 shl z) - 1) - y
        return runCatching {
            dbRef.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=? LIMIT 1",
                arrayOf(z.toString(), x.toString(), tmsY.toString())
            ).use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val bytes = cursor.getBlob(0) ?: return@use null
                decodeLocalBytes(bytes)
            }
        }.getOrNull()
    }


    private fun getCachedBitmap(key: String): Bitmap? = synchronized(cache) { cache.get(key) }

    private fun putCachedBitmap(key: String, bitmap: Bitmap) {
        synchronized(cache) { cache.put(key, bitmap) }
    }

    private fun tileCacheKey(z: Int, x: Int, y: Int): String = if (mbtilesFile != null) "mb:$z/$x/$y" else "$z/$x/$y"

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
            val bitmap = getCachedBitmap(tileCacheKey(parentZ, parentX, parentY)) ?: continue
            val tileRect = tileScreenRect(projection, z, x, y, p1, p2, rect) ?: continue
            val cropW = bitmap.width / factor
            val cropH = bitmap.height / factor
            val cropX = (x % factor) * cropW
            val cropY = (y % factor) * cropH
            val src = Rect(cropX, cropY, cropX + cropW, cropY + cropH)
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

    private fun lonToTileX(lon: Double, z: Int): Int {
        val n = 2.0.pow(z.toDouble())
        return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n.toInt() - 1)
    }

    private fun latToTileY(lat: Double, z: Int): Int {
        val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
        val n = 2.0.pow(z.toDouble())
        val y = floor((1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1 / cos(latRad)) / Math.PI) / 2.0 * n).toInt()
        return y.coerceIn(0, n.toInt() - 1)
    }

    private fun tileXToLon(x: Int, z: Int): Double = x / 2.0.pow(z.toDouble()) * 360.0 - 180.0

    private fun tileYToLat(y: Int, z: Int): Double {
        val n = Math.PI - 2.0 * Math.PI * y / 2.0.pow(z.toDouble())
        return Math.toDegrees(atan(sinh(n)))
    }

    private data class TileCoord(val z: Int, val x: Int, val y: Int)

    companion object {
        private const val MAX_PARENT_FALLBACK_DEPTH = 4
        private const val MAX_VISIBLE_DECODE_TILES = 24
        private const val MAX_PREFETCH_DECODE_TILES = 24
    }
}
