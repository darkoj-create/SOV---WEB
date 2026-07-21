package com.darko.speleov1.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Offline DEM spremnik za AWS Terrain Tiles terrarium PNG.
 *
 * DEM tileovi se ne crtaju kao kartografski sloj. Spremaju se unutar offline paketa
 * pod dem_terrarium/z/x/y.png i koriste se samo za izračun visine.
 */
internal object OfflineDemTileStore {
    const val DEM_DIR_NAME = "dem_terrarium"
    const val DEM_MIN_ZOOM = 9
    const val DEM_MAX_ZOOM = 13
    const val ESTIMATED_TILE_BYTES = 35L * 1024L
    private const val MAX_DOWNLOAD_TILES = 6000

    fun demSpecFor(boundsSpec: OfflineTileManager.OfflineAreaSpec): OfflineTileManager.OfflineAreaSpec =
        OfflineTileManager.OfflineAreaSpec(
            minLat = boundsSpec.minLat,
            maxLat = boundsSpec.maxLat,
            minLon = boundsSpec.minLon,
            maxLon = boundsSpec.maxLon,
            minZoom = DEM_MIN_ZOOM,
            maxZoom = DEM_MAX_ZOOM
        )

    fun estimateDemTiles(spec: OfflineTileManager.OfflineAreaSpec): Int =
        OfflineTileManager.estimateTiles(demSpecFor(spec)).coerceAtLeast(0)

    fun estimateDemBytes(spec: OfflineTileManager.OfflineAreaSpec): Long =
        estimateDemTiles(spec).toLong() * ESTIMATED_TILE_BYTES

    fun demRootForMap(context: Context, mapName: String): File =
        File(OfflineTileManager.tileRootForName(context, mapName), DEM_DIR_NAME)

    fun demTileFileForMap(context: Context, mapName: String, z: Int, x: Int, y: Int): File =
        File(File(File(demRootForMap(context, mapName), z.toString()), x.toString()), "$y.png")

    fun localDemTileCount(context: Context, mapName: String): Int {
        val root = demRootForMap(context, mapName)
        if (!root.exists()) return 0
        return root.walkTopDown().count { it.isFile && it.extension.equals("png", ignoreCase = true) }
    }

    fun findOfflineTileFile(context: Context, z: Int, x: Int, y: Int): File? {
        val candidates = buildList {
            OfflineTileManager.getActiveMapName(context)?.let { add(it) }
            OfflineTileManager.listOfflineMaps(context).forEach { if (!contains(it)) add(it) }
        }
        for (mapName in candidates) {
            val file = demTileFileForMap(context, mapName, z, x, y)
            if (file.exists() && file.length() > 0L) return file
        }
        return null
    }

    suspend fun downloadDemArea(
        context: Context,
        spec: OfflineTileManager.OfflineAreaSpec,
        mapName: String,
        clearExisting: Boolean = false,
        onProgress: ((done: Int, total: Int, zoom: Int) -> Unit)? = null
    ): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        val demSpec = demSpecFor(spec)
        val root = demRootForMap(context, mapName)
        if (clearExisting && root.exists()) root.deleteRecursively()
        root.mkdirs()
        runCatching {
            val total = OfflineTileManager.estimateTiles(demSpec).coerceAtLeast(1)
            if (total > MAX_DOWNLOAD_TILES) {
                throw IllegalStateException("DEM područje je preveliko ($total tileova). Smanji područje.")
            }
            var downloaded = 0
            var skipped = 0
            var processed = 0
            onProgress?.invoke(0, total, demSpec.minZoom)
            for (z in demSpec.minZoom..demSpec.maxZoom) {
                val limit = (1 shl z) - 1
                val xMin = max(0, min(WmsTileSource.lonToTileX(demSpec.minLon, z), WmsTileSource.lonToTileX(demSpec.maxLon, z))).coerceAtMost(limit)
                val xMax = max(WmsTileSource.lonToTileX(demSpec.minLon, z), WmsTileSource.lonToTileX(demSpec.maxLon, z)).coerceIn(0, limit)
                val yMin = max(0, min(WmsTileSource.latToTileY(demSpec.maxLat, z), WmsTileSource.latToTileY(demSpec.minLat, z))).coerceAtMost(limit)
                val yMax = max(WmsTileSource.latToTileY(demSpec.maxLat, z), WmsTileSource.latToTileY(demSpec.minLat, z)).coerceIn(0, limit)
                for (x in xMin..xMax) {
                    for (y in yMin..yMax) {
                        val outFile = demTileFileForMap(context, mapName, z, x, y)
                        if (outFile.exists() && outFile.length() > 0L) {
                            skipped++
                            processed++
                            onProgress?.invoke(processed, total, z)
                            continue
                        }
                        val url = ElevationRepository.TERRARIUM_URL_TEMPLATE
                            .replace("{z}", z.toString())
                            .replace("{x}", x.toString())
                            .replace("{y}", y.toString())
                        val result = SovTileHttp.get(
                            urlText = url,
                            accept = "image/png,image/*,*/*",
                            userAgent = "SOV Android/${context.packageName}",
                            connectTimeoutMs = 6000,
                            readTimeoutMs = 12000
                        )
                        val bytes = result.bytes
                        if (result.code !in 200..299 || bytes == null || bytes.isEmpty()) {
                            throw IllegalStateException("DEM HTTP ${result.code}")
                        }
                        outFile.parentFile?.mkdirs()
                        WmsTileImageCache.writeCacheFile(outFile, bytes)
                        downloaded++
                        processed++
                        onProgress?.invoke(processed, total, z)
                    }
                }
            }
            downloaded to skipped
        }
    }
}
