package com.darko.speleov1.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * Repository za DEM visine iz AWS Terrain Tiles terrarium PNG pločica.
 *
 * Terrarium PNG se NIKAD ne crta kao sloj na karti; koristi se samo kao izvor podataka za visinu.
 * Offline/no-network je normalan scenarij na terenu, zato sve greške vraćaju null bez bučnog logiranja.
 */
class ElevationRepository(
    context: Context,
    private val defaultZoom: Int = DEFAULT_QUERY_ZOOM
) {
    private val appContext = context.applicationContext
    private val diskCacheDir: File = File(appContext.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }

    suspend fun elevationAt(lat: Double, lon: Double): Double? = withContext(Dispatchers.IO) {
        runCatching {
            val coord = ElevationTileMath.tilePixelFor(lat, lon, defaultZoom)
            val tile = loadTile(coord.z, coord.x, coord.y) ?: return@runCatching null
            tile.sampleBilinear(coord.pixelX, coord.pixelY)
        }.getOrNull()
    }

    /**
     * Blocking variant for use on background/IO threads (e.g. tracking service).
     * Returns null if tile not cached locally and network unavailable.
     */
    fun elevationAtBlocking(lat: Double, lon: Double): Double? = runCatching {
        val coord = ElevationTileMath.tilePixelFor(lat, lon, defaultZoom)
        val tile = loadTile(coord.z, coord.x, coord.y) ?: return@runCatching null
        tile.sampleBilinear(coord.pixelX, coord.pixelY)
    }.getOrNull()

    suspend fun elevationsFor(points: List<GeoPoint>): List<Double?> = withContext(Dispatchers.IO) {
        if (points.isEmpty()) return@withContext emptyList()
        val coords = points.map { point ->
            runCatching { ElevationTileMath.tilePixelFor(point.latitude, point.longitude, defaultZoom) }.getOrNull()
        }
        val tiles = mutableMapOf<String, DemTile?>()
        coords.filterNotNull().forEach { coord ->
            val key = ElevationTileMath.tileKey(coord.z, coord.x, coord.y)
            if (!tiles.containsKey(key)) {
                tiles[key] = loadTile(coord.z, coord.x, coord.y)
            }
        }
        coords.map { coord ->
            coord ?: return@map null
            val key = ElevationTileMath.tileKey(coord.z, coord.x, coord.y)
            tiles[key]?.sampleBilinear(coord.pixelX, coord.pixelY)
        }
    }

    private fun loadTile(z: Int, x: Int, y: Int): DemTile? {
        val key = ElevationTileMath.tileKey(z, x, y)
        memoryTile(key)?.let { return it }

        readOfflinePackageTile(z, x, y)?.let { tile ->
            putMemoryTile(key, tile)
            return tile
        }

        readDiskTile(z, x, y)?.let { tile ->
            putMemoryTile(key, tile)
            return tile
        }

        val bytes = downloadTileBytes(z, x, y) ?: return null
        WmsTileImageCache.writeCacheFile(diskFileFor(z, x, y), bytes)
        val tile = decodeTileBytes(key, bytes) ?: return null
        putMemoryTile(key, tile)
        return tile
    }


    private fun readOfflinePackageTile(z: Int, x: Int, y: Int): DemTile? {
        val file = OfflineTileManager.findOfflineDemTile(appContext, z, x, y) ?: return null
        return runCatching {
            decodeTileBytes(ElevationTileMath.tileKey(z, x, y), file.readBytes())
        }.getOrNull()
    }

    private fun readDiskTile(z: Int, x: Int, y: Int): DemTile? {
        val file = diskFileFor(z, x, y)
        if (!file.exists() || file.length() <= 0L) return null
        return runCatching {
            decodeTileBytes(ElevationTileMath.tileKey(z, x, y), file.readBytes())
        }.getOrNull()
    }

    private fun downloadTileBytes(z: Int, x: Int, y: Int): ByteArray? {
        val url = TERRARIUM_URL_TEMPLATE
            .replace("{z}", z.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
        return try {
            val result = SovTileHttp.get(
                urlText = url,
                accept = "image/png,image/*,*/*",
                userAgent = "SOV Android/${appContext.packageName}",
                connectTimeoutMs = 3500,
                readTimeoutMs = 6500
            )
            result.bytes?.takeIf { result.code in 200..299 && it.isNotEmpty() }
        } catch (_: Throwable) {
            null
        }
    }

    private fun diskFileFor(z: Int, x: Int, y: Int): File = File(File(File(diskCacheDir, z.toString()), x.toString()), "$y.png")

    private fun decodeTileBytes(key: String, bytes: ByteArray): DemTile? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }) ?: return null
        if (bitmap.width <= 0 || bitmap.height <= 0) return null
        return bitmap.toDemTile(key)
    }

    private fun Bitmap.toDemTile(key: String): DemTile {
        val widthPx = width
        val heightPx = height
        val pixels = IntArray(widthPx * heightPx)
        getPixels(pixels, 0, widthPx, 0, 0, widthPx, heightPx)
        val heights = DoubleArray(pixels.size)
        for (i in pixels.indices) {
            val color = pixels[i]
            val raw = ElevationTileMath.terrariumHeightMeters(
                red = Color.red(color),
                green = Color.green(color),
                blue = Color.blue(color)
            )
            heights[i] = if (ElevationTileMath.isValidHeight(raw)) raw else Double.NaN
        }
        return DemTile(key, widthPx, heightPx, heights)
    }

    companion object {
        const val DEFAULT_QUERY_ZOOM = 13
        const val CACHE_DIR_NAME = "dem_terrarium"
        const val TERRARIUM_URL_TEMPLATE = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"

        private val memoryCache = object : LruCache<String, DemTile>(30) {
            override fun sizeOf(key: String, value: DemTile): Int = 1
        }

        private fun memoryTile(key: String): DemTile? = synchronized(memoryCache) { memoryCache.get(key) }
        private fun putMemoryTile(key: String, tile: DemTile) = synchronized(memoryCache) { memoryCache.put(key, tile) }
    }
}

internal data class DemTile(
    val key: String,
    val width: Int,
    val height: Int,
    val heights: DoubleArray
) {
    fun sampleBilinear(pixelX: Double, pixelY: Double): Double? {
        if (width <= 0 || height <= 0) return null
        val x = pixelX.coerceIn(0.0, (width - 1).toDouble())
        val y = pixelY.coerceIn(0.0, (height - 1).toDouble())
        val x0 = floor(x).toInt().coerceIn(0, width - 1)
        val y0 = floor(y).toInt().coerceIn(0, height - 1)
        val x1 = (x0 + 1).coerceAtMost(width - 1)
        val y1 = (y0 + 1).coerceAtMost(height - 1)
        val dx = (x - x0).coerceIn(0.0, 1.0)
        val dy = (y - y0).coerceIn(0.0, 1.0)
        val samples = listOf(
            WeightedHeight(heightAt(x0, y0), (1.0 - dx) * (1.0 - dy)),
            WeightedHeight(heightAt(x1, y0), dx * (1.0 - dy)),
            WeightedHeight(heightAt(x0, y1), (1.0 - dx) * dy),
            WeightedHeight(heightAt(x1, y1), dx * dy)
        ).filter { it.weight > 0.0 && !it.height.isNaN() }
        val weightSum = samples.sumOf { it.weight }
        if (weightSum <= 0.0) return null
        return samples.sumOf { it.height * it.weight } / weightSum
    }

    private fun heightAt(x: Int, y: Int): Double = heights.getOrElse(y * width + x) { Double.NaN }
}

private data class WeightedHeight(val height: Double, val weight: Double)

internal object ElevationTileMath {
    private const val TILE_SIZE = 256
    private const val MIN_VALID_HEIGHT = -11000.0
    private const val MAX_VALID_HEIGHT = 9000.0

    data class TilePixelCoordinate(
        val z: Int,
        val x: Int,
        val y: Int,
        val pixelX: Double,
        val pixelY: Double
    )

    fun terrariumHeightMeters(red: Int, green: Int, blue: Int): Double =
        (red.coerceIn(0, 255) * 256.0) + green.coerceIn(0, 255) + (blue.coerceIn(0, 255) / 256.0) - 32768.0

    fun isValidHeight(heightMeters: Double): Boolean = heightMeters in MIN_VALID_HEIGHT..MAX_VALID_HEIGHT

    fun tilePixelFor(lat: Double, lon: Double, z: Int): TilePixelCoordinate {
        val zoom = z.coerceIn(0, 15)
        val n = 1 shl zoom
        val lonNormalized = (lon.takeIf { it.isFinite() } ?: 0.0).coerceIn(-180.0, 180.0)
        val latRad = Math.toRadians((lat.takeIf { it.isFinite() } ?: 0.0).coerceIn(-85.05112878, 85.05112878))
        val globalX = ((lonNormalized + 180.0) / 360.0) * n
        val globalY = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0) * n
        val tileX = floor(globalX).toInt().coerceIn(0, n - 1)
        val tileY = floor(globalY).toInt().coerceIn(0, n - 1)
        val pixelX = ((globalX - tileX) * TILE_SIZE).coerceIn(0.0, (TILE_SIZE - 1).toDouble())
        val pixelY = ((globalY - tileY) * TILE_SIZE).coerceIn(0.0, (TILE_SIZE - 1).toDouble())
        return TilePixelCoordinate(zoom, tileX, tileY, pixelX, pixelY)
    }

    fun tileKey(z: Int, x: Int, y: Int): String = "$z/$x/$y"
}
