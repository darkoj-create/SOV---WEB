package com.darko.speleov1.util

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.util.LruCache
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh

class LocalTileOverlay(private val root: File) : Overlay() {
    private val cache = object : LruCache<String, Bitmap>(160) {}
    private val decodeOptions = BitmapFactory.Options().apply {
        // Offline WMS tile PNG-ovi često imaju transparentnu pozadinu.
        // RGB_565 odbaci alpha kanal i transparentno pretvori u crno,
        // pa cijela offline karta može izgledati kao crni ekran.
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    private val mbtilesFile: File? = detectMbtilesFile(root)
    private val prefetchExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "sov-mbtiles-prefetch").apply { isDaemon = true }
    }

    private var cachedZoomLevels: List<Int> = emptyList()
    private var cachedRootStamp: Long = Long.MIN_VALUE
    private var db: SQLiteDatabase? = null

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !root.exists()) return
        val zoomDirs = availableZoomLevels() ?: return
        if (zoomDirs.isEmpty()) return

        val requestedZoom = mapView.zoomLevelDouble.roundToInt()
        val z = chooseZoom(requestedZoom, zoomDirs)
        val bb: BoundingBox = mapView.boundingBox ?: return
        val xMin = lonToTileX(bb.lonWest, z)
        val xMax = lonToTileX(bb.lonEast, z)
        val yMin = latToTileY(bb.latNorth, z)
        val yMax = latToTileY(bb.latSouth, z)

        val projection = mapView.projection
        val p1 = Point()
        val p2 = Point()
        val rect = Rect()

        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val bitmap = bitmapForTile(z, x, y) ?: continue
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
                if (rect.right <= rect.left || rect.bottom <= rect.top) continue
                canvas.drawBitmap(bitmap, null, rect, null)
            }
        }

        prefetchTiles(z, xMin - 1, xMax + 1, yMin - 1, yMax + 1)
    }

    override fun onDetach(mapView: MapView?) {
        cache.evictAll()
        runCatching { db?.close() }
        db = null
        prefetchExecutor.shutdownNow()
        super.onDetach(mapView)
    }

    private fun detectMbtilesFile(root: File): File? {
        if (root.isFile && root.extension.equals("mbtiles", ignoreCase = true)) return root
        return root.listFiles()?.firstOrNull { it.isFile && it.extension.equals("mbtiles", ignoreCase = true) }
    }

    private fun availableZoomLevels(): List<Int>? {
        val stamp = mbtilesFile?.lastModified() ?: root.lastModified()
        if (cachedZoomLevels.isNotEmpty() && cachedRootStamp == stamp) return cachedZoomLevels
        val zooms = if (mbtilesFile != null) {
            queryMbtilesZoomLevels(mbtilesFile)
        } else {
            root.listFiles()
                ?.asSequence()
                ?.filter { it.isDirectory }
                ?.mapNotNull { it.name.toIntOrNull() }
                ?.sorted()
                ?.toList()
                .orEmpty()
        }
        cachedZoomLevels = zooms
        cachedRootStamp = stamp
        return zooms
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

    private fun bitmapForTile(z: Int, x: Int, y: Int): Bitmap? {
        return if (mbtilesFile != null) {
            val key = "mb:$z/$x/$y"
            cache.get(key) ?: loadMbtilesBitmap(z, x, y)?.also { cache.put(key, it) }
        } else {
            val file = File(root, "$z/$x/$y.png")
            if (!file.exists() || file.length() <= 0L) return null
            val key = file.absolutePath
            cache.get(key) ?: BitmapFactory.decodeFile(file.absolutePath, decodeOptions)?.also { cache.put(key, it) }
        }
    }

    private fun prefetchTiles(z: Int, xMin: Int, xMax: Int, yMin: Int, yMax: Int) {
        if (mbtilesFile == null) return
        val file = mbtilesFile ?: return
        val keysToWarm = ArrayList<Triple<Int, Int, Int>>(24)
        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val key = "mb:$z/$x/$y"
                if (cache.get(key) == null) keysToWarm += Triple(z, x, y)
                if (keysToWarm.size >= 24) break
            }
            if (keysToWarm.size >= 24) break
        }
        if (keysToWarm.isEmpty()) return
        prefetchExecutor.execute {
            val database = openDb(file) ?: return@execute
            keysToWarm.forEach { (zoom, x, y) ->
                val key = "mb:$zoom/$x/$y"
                if (cache.get(key) == null) {
                    loadMbtilesBitmap(zoom, x, y, database)?.also { cache.put(key, it) }
                }
            }
        }
    }

    private fun openDb(file: File): SQLiteDatabase? {
        db?.let { if (it.isOpen) return it }
        return runCatching {
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        }.getOrNull()?.also { db = it }
    }

    private fun loadMbtilesBitmap(z: Int, x: Int, y: Int, database: SQLiteDatabase? = null): Bitmap? {
        val file = mbtilesFile ?: return null
        val dbRef = database ?: openDb(file) ?: return null
        val tmsY = ((1 shl z) - 1) - y
        return runCatching {
            dbRef.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=? LIMIT 1",
                arrayOf(z.toString(), x.toString(), tmsY.toString())
            ).use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val bytes = cursor.getBlob(0) ?: return@use null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            }
        }.getOrNull()
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
}
