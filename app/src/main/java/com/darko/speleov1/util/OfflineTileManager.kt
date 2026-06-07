package com.darko.speleov1.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.ZipInputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object OfflineTileManager {
    const val TILE_SOURCE_NAME = "OpenTopoOffline"
    private const val PREFS = "offline_tile_manager"
    private const val KEY_ACTIVE_MAP = "active_map_name"
    private const val KEY_ENABLED_CUSTOM_OVERLAYS = "enabled_custom_overlays"
    private const val KEY_EXACT_BOUNDS_PREFIX = "exact_bounds_"
    private const val LEGACY_MAP_NAME = "Legacy offline"
    private const val MBTILES_FILE_NAME = "tiles.mbtiles"

    data class OfflineAreaSpec(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double,
        val minZoom: Int,
        val maxZoom: Int
    )

    data class OfflineBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )

    private fun exactBoundsKey(mapName: String): String = KEY_EXACT_BOUNDS_PREFIX + sanitizeMapName(mapName)

    fun setOfflineBounds(context: Context, mapName: String, bounds: OfflineBounds) {
        val value = listOf(bounds.minLat, bounds.maxLat, bounds.minLon, bounds.maxLon).joinToString("|")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(exactBoundsKey(mapName), value)
            .apply()
    }

    private fun getStoredOfflineBounds(context: Context, mapName: String): OfflineBounds? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(exactBoundsKey(mapName), null) ?: return null
        val parts = raw.split('|').mapNotNull { it.toDoubleOrNull() }
        if (parts.size != 4) return null
        return OfflineBounds(
            minLat = min(parts[0], parts[1]),
            maxLat = max(parts[0], parts[1]),
            minLon = min(parts[2], parts[3]),
            maxLon = max(parts[2], parts[3])
        )
    }

    fun configureOsmdroidPaths(context: Context) {
        val base = File(context.filesDir, "osmdroid").apply { mkdirs() }
        val tileCache = File(base, "tile_cache").apply { mkdirs() }
        Configuration.getInstance().osmdroidBasePath = base
        Configuration.getInstance().osmdroidTileCache = tileCache
    }

    private fun mapsRoot(context: Context): File {
        configureOsmdroidPaths(context)
        return File(Configuration.getInstance().osmdroidBasePath, "tiles/$TILE_SOURCE_NAME").apply { mkdirs() }
    }

    private fun legacyRoot(context: Context): File = mapsRoot(context)

    fun offlineRoot(context: Context): File = File(context.filesDir, "Offline").apply { mkdirs() }
    fun gpxRoot(context: Context): File = File(offlineRoot(context), "gpx").apply { mkdirs() }
    fun kmlRoot(context: Context): File = File(offlineRoot(context), "kml").apply { mkdirs() }
    fun mbtilesRoot(context: Context): File = File(offlineRoot(context), "mbtiles").apply { mkdirs() }
    fun mapsPackageRoot(context: Context): File = File(offlineRoot(context), "maps").apply { mkdirs() }

    fun ensureOfflineFolderStructure(context: Context): File {
        val root = offlineRoot(context)
        gpxRoot(context)
        kmlRoot(context)
        mbtilesRoot(context)
        mapsPackageRoot(context)
        return root
    }

    /**
     * User-facing export bridge.
     *
     * The real working folders stay in app-private storage so Android cannot
     * break imports/tracks/maps. This public Downloads/SOV/Offline mirror is only
     * for humans/file managers, because Android blocks direct use of
     * /Android/data/... on many devices.
     */
    fun publicOfflineRoot(): File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "SOV/Offline"
    ).apply { mkdirs() }

    fun publicGpxRoot(): File = File(publicOfflineRoot(), "gpx").apply { mkdirs() }
    fun publicKmlRoot(): File = File(publicOfflineRoot(), "kml").apply { mkdirs() }
    fun publicMbtilesRoot(): File = File(publicOfflineRoot(), "mbtiles").apply { mkdirs() }
    fun publicMapsRoot(): File = File(publicOfflineRoot(), "maps").apply { mkdirs() }
    fun publicPhotosRoot(): File = File(publicOfflineRoot(), "photos").apply { mkdirs() }
    fun publicGeojsonRoot(): File = File(publicOfflineRoot(), "geojson").apply { mkdirs() }
    fun publicXmlRoot(): File = File(publicOfflineRoot(), "xml").apply { mkdirs() }
    fun publicTablesRoot(): File = File(publicOfflineRoot(), "tables").apply { mkdirs() }
    fun publicDatabasesRoot(): File = File(publicOfflineRoot(), "databases").apply { mkdirs() }
    fun publicRastersRoot(): File = File(publicOfflineRoot(), "rasters").apply { mkdirs() }
    fun publicPackagesRoot(): File = File(publicOfflineRoot(), "packages").apply { mkdirs() }
    fun publicOtherRoot(): File = File(publicOfflineRoot(), "other").apply { mkdirs() }

    fun publicRootForExtension(extensionRaw: String): File {
        val extension = extensionRaw.trim().trimStart('.').lowercase(Locale.ROOT)
        return when (extension) {
            "gpx" -> publicGpxRoot()
            "kml", "kmz" -> publicKmlRoot()
            "mbtiles", "pmtiles" -> publicMbtilesRoot()
            "geojson", "json" -> publicGeojsonRoot()
            "xml" -> publicXmlRoot()
            "csv", "xlsx", "xlsm", "xls" -> publicTablesRoot()
            "gpkg", "geopackage", "sqlite", "db" -> publicDatabasesRoot()
            "tif", "tiff" -> publicRastersRoot()
            "sovpkg", "zip" -> publicPackagesRoot()
            "jpg", "jpeg", "png", "webp" -> publicPhotosRoot()
            else -> publicOtherRoot()
        }
    }

    fun ensurePublicOfflineFolderStructure(): File {
        val root = publicOfflineRoot()
        publicGpxRoot()
        publicKmlRoot()
        publicMbtilesRoot()
        publicMapsRoot()
        publicPhotosRoot()
        publicGeojsonRoot()
        publicXmlRoot()
        publicTablesRoot()
        publicDatabasesRoot()
        publicRastersRoot()
        publicPackagesRoot()
        publicOtherRoot()
        return root
    }

    fun syncPublicOfflineExports(context: Context): File {
        ensureOfflineFolderStructure(context)
        val publicRoot = ensurePublicOfflineFolderStructure()
        copyPlainFiles(gpxRoot(context), publicGpxRoot(), setOf("gpx"))
        copyPlainFiles(kmlRoot(context), publicKmlRoot(), setOf("kml", "kmz"))
        copyMbtilesExports(mbtilesRoot(context), publicMbtilesRoot())
        copyPlainFiles(mapsPackageRoot(context), publicMapsRoot(), setOf("mbtiles", "pmtiles"))
        copyMbtilesExports(mapsPackageRoot(context), publicMbtilesRoot())
        return publicRoot
    }

    private fun copyPlainFiles(from: File, to: File, extensions: Set<String>) {
        if (!from.exists()) return
        to.mkdirs()
        from.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in extensions }
            ?.forEach { source ->
                runCatching { source.copyTo(File(to, source.name), overwrite = true) }
            }
    }

    private fun copyMbtilesExports(from: File, to: File) {
        if (!from.exists()) return
        to.mkdirs()
        from.listFiles()?.forEach { source ->
            runCatching {
                when {
                    source.isFile && source.extension.equals("mbtiles", ignoreCase = true) -> {
                        source.copyTo(File(to, source.name), overwrite = true)
                    }
                    source.isDirectory -> {
                        val mbtiles = source.listFiles()?.firstOrNull { it.isFile && it.extension.equals("mbtiles", ignoreCase = true) }
                        if (mbtiles != null) {
                            mbtiles.copyTo(File(to, sanitizeMapName(source.name) + ".mbtiles"), overwrite = true)
                        } else {
                            Unit
                        }
                    }
                    else -> Unit
                }
            }
        }
    }


    private fun legacyCustomMapsRoot(context: Context): File = File(context.filesDir, "custom_maps").apply { mkdirs() }

    private fun customMapsRoot(context: Context): File = mbtilesRoot(context)

    fun sanitizeMapName(raw: String): String {
        val clean = raw.trim().replace(Regex("[\\/:*?\"<>|]"), "_").replace(Regex("\\s+"), " ").trim()
        return clean.ifBlank { "Offline karta" }
    }

    private fun containsTileContent(dir: File): Boolean {
        if (!dir.exists()) return false
        if (mbtilesFile(dir) != null) return true
        return dir.walkTopDown().any { it.isFile && it.extension.lowercase() == "png" }
    }

    private fun mbtilesFile(dir: File): File? {
        if (!dir.exists()) return null
        if (dir.isFile && dir.extension.equals("mbtiles", ignoreCase = true)) return dir
        return dir.listFiles()?.firstOrNull { it.isFile && it.extension.equals("mbtiles", ignoreCase = true) }
    }

    fun listOfflineMaps(context: Context): List<String> {
        val root = mapsRoot(context)
        val names = mutableListOf<String>()
        val hasLegacyTiles = root.walkTopDown().maxDepth(3).any { file ->
            file.isFile && file.extension.lowercase() == "png" && file.parentFile?.parentFile == root
        }
        if (hasLegacyTiles) names += LEGACY_MAP_NAME
        root.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() }?.forEach { dir ->
            if (containsTileContent(dir)) names += dir.name
        }
        return names.distinct()
    }

    private fun listAllMaps(context: Context): List<String> = (listCustomMaps(context) + listOfflineMaps(context)).distinct()

    fun getActiveMapName(context: Context): String? {
        val names = listAllMaps(context)
        if (names.isEmpty()) return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_ACTIVE_MAP, null)
        names.firstOrNull { it == saved }?.let { return it }
        return names.first()
    }

    fun setActiveMapName(context: Context, mapName: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ACTIVE_MAP, sanitizeMapName(mapName)).apply()
    }

    fun getEnabledCustomOverlayNames(context: Context): Set<String> {
        val available = listCustomMaps(context).toSet()
        if (available.isEmpty()) return emptySet()
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_ENABLED_CUSTOM_OVERLAYS, emptySet())
            .orEmpty()
            .map { sanitizeMapName(it) }
            .toSet()
        return stored.intersect(available)
    }

    fun isCustomOverlayEnabled(context: Context, mapName: String): Boolean =
        getEnabledCustomOverlayNames(context).contains(sanitizeMapName(mapName))

    fun setCustomOverlayEnabled(context: Context, mapName: String, enabled: Boolean) {
        val normalized = sanitizeMapName(mapName)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_ENABLED_CUSTOM_OVERLAYS, emptySet()).orEmpty().toMutableSet()
        if (enabled) current += normalized else current -= normalized
        prefs.edit().putStringSet(KEY_ENABLED_CUSTOM_OVERLAYS, current).apply()
    }

    fun clearCustomOverlay(context: Context, mapName: String) {
        setCustomOverlayEnabled(context, mapName, false)
    }


    private fun offlineMapDir(context: Context, mapName: String): File {
        val normalized = sanitizeMapName(mapName)
        return if (normalized == LEGACY_MAP_NAME) legacyRoot(context) else File(mapsRoot(context), normalized)
    }

    private fun customMapDir(context: Context, mapName: String): File = File(customMapsRoot(context), sanitizeMapName(mapName))
    private fun legacyCustomMapDir(context: Context, mapName: String): File = File(legacyCustomMapsRoot(context), sanitizeMapName(mapName))

    private suspend fun packAndExportAsMbtiles(
        context: Context,
        mapName: String,
        onProgress: ((String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val srcDir = offlineMapDir(context, mapName)
        if (!srcDir.exists()) return@withContext
        val destFile = File(publicMapsRoot(), "${sanitizeMapName(mapName)}.mbtiles")
        destFile.parentFile?.mkdirs()
        if (destFile.exists()) destFile.delete()
        onProgress?.invoke("Pakiram kartu...")
        val db = SQLiteDatabase.openOrCreateDatabase(destFile, null)
        db.use {
            it.execSQL("CREATE TABLE IF NOT EXISTS metadata (name TEXT, value TEXT)")
            it.execSQL("CREATE TABLE IF NOT EXISTS tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB, PRIMARY KEY (zoom_level, tile_column, tile_row))")
            it.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS tile_index ON tiles (zoom_level, tile_column, tile_row)")
            it.execSQL("DELETE FROM metadata")
            it.execSQL("DELETE FROM tiles")
            it.execSQL("INSERT INTO metadata VALUES ('name', ?)", arrayOf(mapName))
            it.execSQL("INSERT INTO metadata VALUES ('format', 'png')")
            it.beginTransaction()
            try {
                srcDir.walkTopDown()
                    .filter { file -> file.isFile && file.extension.equals("png", ignoreCase = true) }
                    .forEach { file ->
                        val parts = file.relativeTo(srcDir).invariantSeparatorsPath.split("/")
                        if (parts.size != 3) return@forEach
                        val z = parts[0].toIntOrNull() ?: return@forEach
                        val x = parts[1].toIntOrNull() ?: return@forEach
                        val yPng = parts[2].removeSuffix(".png").toIntOrNull() ?: return@forEach
                        val yTms = (1 shl z) - 1 - yPng
                        val bytes = file.readBytes()
                        it.execSQL(
                            "INSERT OR REPLACE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?,?,?,?)",
                            arrayOf(z, x, yTms, bytes)
                        )
                    }
                it.setTransactionSuccessful()
            } finally {
                it.endTransaction()
            }
        }
    }

    private fun mapDir(context: Context, mapName: String): File {
        val normalized = sanitizeMapName(mapName)
        val custom = customMapDir(context, normalized)
        if (custom.exists()) return custom
        val legacyCustom = legacyCustomMapDir(context, normalized)
        if (legacyCustom.exists()) return legacyCustom
        return offlineMapDir(context, normalized)
    }

    fun tileRoot(context: Context): File {
        val activeName = getActiveMapName(context) ?: return legacyRoot(context)
        return mapDir(context, activeName)
    }

    fun tileRootForName(context: Context, mapName: String): File = mapDir(context, mapName)

    fun listCustomMaps(context: Context): List<String> {
        val roots = listOf(customMapsRoot(context), legacyCustomMapsRoot(context))
        return roots.flatMap { root ->
            root.listFiles()
                ?.filter { it.isDirectory && containsTileContent(it) }
                ?.map { it.name }
                .orEmpty()
        }.distinct().sortedBy { it.lowercase() }
    }

    fun hasOfflineTiles(context: Context): Boolean = containsTileContent(tileRoot(context))

    fun localTileCount(context: Context): Int {
        val root = tileRoot(context)
        val mbtiles = mbtilesFile(root)
        if (mbtiles != null) return queryMbtilesTileCount(mbtiles)
        if (!root.exists()) return 0
        return root.walkTopDown().count { it.isFile && it.extension.lowercase() == "png" }
    }

    fun localTileCount(context: Context, mapName: String): Int {
        val root = mapDir(context, mapName)
        val mbtiles = mbtilesFile(root)
        if (mbtiles != null) return queryMbtilesTileCount(mbtiles)
        if (!root.exists()) return 0
        return root.walkTopDown().count { it.isFile && it.extension.lowercase() == "png" }
    }

    fun isMbtilesMap(context: Context, mapName: String): Boolean = mbtilesFile(mapDir(context, mapName)) != null

    fun suggestedZoom(bounds: OfflineBounds?): Double {
        if (bounds == null) return 11.0
        val latSpan = kotlin.math.abs(bounds.maxLat - bounds.minLat)
        val lonSpan = kotlin.math.abs(bounds.maxLon - bounds.minLon)
        val span = max(latSpan, lonSpan)
        return when {
            span <= 0.01 -> 16.0
            span <= 0.03 -> 15.0
            span <= 0.08 -> 14.0
            span <= 0.2 -> 13.0
            span <= 0.5 -> 12.0
            else -> 11.0
        }
    }

    fun getOfflineBounds(context: Context): OfflineBounds? = getOfflineBounds(context, getActiveMapName(context))

    fun getOfflineBounds(context: Context, mapName: String?): OfflineBounds? {
        val normalizedName = mapName?.let { sanitizeMapName(it) }
        if (normalizedName != null) {
            getStoredOfflineBounds(context, normalizedName)?.let { return it }
        }
        val root = normalizedName?.let { mapDir(context, it) } ?: tileRoot(context)
        val mbtiles = mbtilesFile(root)
        if (mbtiles != null) return getMbtilesBounds(mbtiles)
        if (!root.exists()) return null
        var minLat = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        var minLon = Double.POSITIVE_INFINITY
        var maxLon = Double.NEGATIVE_INFINITY
        root.walkTopDown().filter { it.isFile && it.extension.lowercase() == "png" }.forEach { file ->
            val rel = file.relativeTo(root).invariantSeparatorsPath
            val parts = rel.split('/')
            if (parts.size < 3) return@forEach
            val z = parts[0].toIntOrNull() ?: return@forEach
            val x = parts[1].toIntOrNull() ?: return@forEach
            val y = parts[2].substringBefore('.').toIntOrNull() ?: return@forEach
            val west = WmsTileSource.tileXToLon(x, z)
            val east = WmsTileSource.tileXToLon(x + 1, z)
            val north = WmsTileSource.tileYToLat(y, z)
            val south = WmsTileSource.tileYToLat(y + 1, z)
            minLon = min(minLon, west)
            maxLon = max(maxLon, east)
            minLat = min(minLat, south)
            maxLat = max(maxLat, north)
        }
        if (!minLat.isFinite() || !maxLat.isFinite() || !minLon.isFinite() || !maxLon.isFinite()) return null
        return OfflineBounds(minLat = minLat, maxLat = maxLat, minLon = minLon, maxLon = maxLon)
    }

    private fun queryMbtilesTileCount(file: File): Int = runCatching {
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM tiles", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }
    }.getOrDefault(0)


    private fun queryMbtilesHasRasterTile(file: File): Boolean = runCatching {
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT tile_data FROM tiles LIMIT 12", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val bytes = cursor.getBlob(0) ?: continue
                    if (bytes.size >= 4) {
                        val isPng = bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
                        val isJpg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
                        if (isPng || isJpg || BitmapFactory.decodeByteArray(bytes, 0, bytes.size) != null) return@runCatching true
                    }
                }
                false
            }
        }
    }.getOrDefault(false)

    private fun queryMbtilesMetadataValue(file: File, key: String): String? = runCatching {
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT value FROM metadata WHERE name=? LIMIT 1", arrayOf(key)).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }
    }.getOrNull()

    private fun queryMbtilesIsVectorTile(file: File): Boolean {
        val format = queryMbtilesMetadataValue(file, "format")?.lowercase(Locale.ROOT).orEmpty()
        if (format in setOf("pbf", "mvt", "vector", "protobuf")) return true
        return runCatching {
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT tile_data FROM tiles LIMIT 8", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val bytes = cursor.getBlob(0) ?: continue
                        val gzip = bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
                        if (gzip) return@runCatching true
                    }
                    false
                }
            }
        }.getOrDefault(false)
    }

    private fun getMbtilesBounds(file: File): OfflineBounds? {
        return runCatching {
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT value FROM metadata WHERE name='bounds' LIMIT 1", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val parts = cursor.getString(0).split(',').mapNotNull { it.trim().toDoubleOrNull() }
                        if (parts.size == 4) {
                            return@runCatching OfflineBounds(
                                minLat = parts[1],
                                maxLat = parts[3],
                                minLon = parts[0],
                                maxLon = parts[2]
                            )
                        }
                    }
                }
                db.rawQuery("SELECT MAX(zoom_level) FROM tiles", null).use { zoomCursor ->
                    if (!zoomCursor.moveToFirst()) return@use
                    val z = zoomCursor.getInt(0)
                    val n = 1 shl z
                    db.rawQuery(
                        "SELECT MIN(tile_column), MAX(tile_column), MIN(tile_row), MAX(tile_row) FROM tiles WHERE zoom_level=?",
                        arrayOf(z.toString())
                    ).use { cursor ->
                        if (!cursor.moveToFirst()) return@use
                        val minX = cursor.getInt(0)
                        val maxX = cursor.getInt(1)
                        val minTmsY = cursor.getInt(2)
                        val maxTmsY = cursor.getInt(3)
                        val northXyzY = (n - 1) - maxTmsY
                        val southXyzY = (n - 1) - minTmsY
                        return@runCatching OfflineBounds(
                            minLat = WmsTileSource.tileYToLat(southXyzY + 1, z),
                            maxLat = WmsTileSource.tileYToLat(northXyzY, z),
                            minLon = WmsTileSource.tileXToLon(minX, z),
                            maxLon = WmsTileSource.tileXToLon(maxX + 1, z)
                        )
                    }
                }
                null
            }
        }.getOrNull()
    }

    suspend fun downloadAndExtractZip(context: Context, url: String, mapName: String, onProgress: ((done: Int, total: Int, zoom: Int) -> Unit)? = null) = withContext(Dispatchers.IO) {
        configureOsmdroidPaths(context)
        val tempZip = File(context.cacheDir, "offline_tiles_download.zip")
        val root = offlineMapDir(context, mapName).apply { mkdirs() }
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
        }
        try {
            conn.connect()
            if (conn.responseCode !in 200..299) return@withContext Result.failure(IllegalStateException("HTTP ${conn.responseCode}"))
            conn.inputStream.use { input -> FileOutputStream(tempZip).use { output -> input.copyTo(output) } }
            if (root.exists()) root.deleteRecursively()
            root.mkdirs()
            unzipIntoTileRoot(tempZip, root, onProgress)
            if (!root.walkTopDown().any { it.isFile && it.extension.lowercase() == "png" }) {
                return@withContext Result.failure(IllegalStateException("ZIP je skinut, ali nisam našao PNG tileove. Očekivani format je z/x/y.png"))
            }
            setActiveMapName(context, mapName)
            Result.success(root.walkTopDown().count { it.isFile && it.extension.lowercase() == "png" })
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            conn.disconnect()
            tempZip.delete()
        }
    }

    suspend fun downloadWmsArea(
        context: Context,
        config: WmsConfig,
        spec: OfflineAreaSpec,
        mapName: String,
        clearExisting: Boolean = false,
        onProgress: ((done: Int, total: Int, zoom: Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        configureOsmdroidPaths(context)
        val root = offlineMapDir(context, mapName)
        if (clearExisting && root.exists()) root.deleteRecursively()
        root.mkdirs()
        try {
            val estimates = estimateTiles(spec)
            if (estimates > 4000) return@withContext Result.failure(IllegalStateException("Područje je preveliko za jedan download ($estimates tileova). Suzi okvir ili smanji detaljnost."))
            var downloaded = 0
            var skipped = 0
            val total = estimates.coerceAtLeast(1)
            var processed = 0
            onProgress?.invoke(0, total, spec.minZoom)
            for (z in spec.minZoom..spec.maxZoom) {
                val xMin = max(0, min(WmsTileSource.lonToTileX(spec.minLon, z), WmsTileSource.lonToTileX(spec.maxLon, z)))
                val xMax = max(WmsTileSource.lonToTileX(spec.minLon, z), WmsTileSource.lonToTileX(spec.maxLon, z))
                val yMin = max(0, min(WmsTileSource.latToTileY(spec.maxLat, z), WmsTileSource.latToTileY(spec.minLat, z)))
                val yMax = max(WmsTileSource.latToTileY(spec.maxLat, z), WmsTileSource.latToTileY(spec.minLat, z))
                for (x in xMin..xMax) {
                    for (y in yMin..yMax) {
                        val outFile = File(root, "$z/$x/$y.png")
                        if (outFile.exists()) {
                            skipped++
                            processed++
                            onProgress?.invoke(processed, total, z)
                            continue
                        }
                        outFile.parentFile?.mkdirs()
                        val url = WmsTileSource.buildTileUrl(config, z, x, y)
                        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 15000
                            readTimeout = 30000
                            instanceFollowRedirects = true
                        }
                        try {
                            conn.connect()
                            if (conn.responseCode !in 200..299) return@withContext Result.failure(IllegalStateException("WMS HTTP ${conn.responseCode}"))
                            conn.inputStream.use { input -> FileOutputStream(outFile).use { output -> input.copyTo(output) } }
                            if (outFile.length() < 100L) {
                                outFile.delete()
                                return@withContext Result.failure(IllegalStateException("WMS je vratio premali/neosnovan tile. Provjeri odabrano područje ili WMS postavke."))
                            }
                            downloaded++
                            processed++
                            onProgress?.invoke(processed, total, z)
                        } finally { conn.disconnect() }
                    }
                }
            }
            setActiveMapName(context, mapName)
            setOfflineBounds(context, mapName, OfflineBounds(spec.minLat, spec.maxLat, spec.minLon, spec.maxLon))
            packAndExportAsMbtiles(context, mapName) { onProgress?.invoke(-1, -1, -1) }
            Result.success(downloaded to skipped)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun downloadHgssOsmArea(
        context: Context,
        spec: OfflineAreaSpec,
        mapName: String,
        clearExisting: Boolean = false,
        onProgress: ((done: Int, total: Int, zoom: Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        configureOsmdroidPaths(context)
        val root = offlineMapDir(context, mapName)
        if (clearExisting && root.exists()) root.deleteRecursively()
        root.mkdirs()
        try {
            val estimates = estimateTiles(spec)
            if (estimates > 4000) return@withContext Result.failure(IllegalStateException("Podrucje je preveliko za jedan HGSS offline download ($estimates tileova). Suzi okvir ili smanji detaljnost."))
            var downloaded = 0
            var skipped = 0
            val total = estimates.coerceAtLeast(1)
            var processed = 0
            onProgress?.invoke(0, total, spec.minZoom)
            for (z in spec.minZoom..spec.maxZoom) {
                val xMin = max(0, min(WmsTileSource.lonToTileX(spec.minLon, z), WmsTileSource.lonToTileX(spec.maxLon, z)))
                val xMax = max(WmsTileSource.lonToTileX(spec.minLon, z), WmsTileSource.lonToTileX(spec.maxLon, z))
                val yMin = max(0, min(WmsTileSource.latToTileY(spec.maxLat, z), WmsTileSource.latToTileY(spec.minLat, z)))
                val yMax = max(WmsTileSource.latToTileY(spec.maxLat, z), WmsTileSource.latToTileY(spec.minLat, z))
                for (x in xMin..xMax) {
                    for (y in yMin..yMax) {
                        val outFile = File(root, "$z/$x/$y.png")
                        if (outFile.exists()) {
                            skipped++
                            processed++
                            onProgress?.invoke(processed, total, z)
                            continue
                        }
                        outFile.parentFile?.mkdirs()
                        val url = HGSSTileSource.buildHgssOsmTileUrl(z, x, y)
                        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 15000
                            readTimeout = 30000
                            instanceFollowRedirects = true
                        }
                        try {
                            conn.connect()
                            if (conn.responseCode !in 200..299) return@withContext Result.failure(IllegalStateException("HGSS HTTP ${conn.responseCode}"))
                            conn.inputStream.use { input -> FileOutputStream(outFile).use { output -> input.copyTo(output) } }
                            if (outFile.length() < 50L) {
                                outFile.delete()
                                return@withContext Result.failure(IllegalStateException("HGSS je vratio premali tile. Probaj manji/drukciji odabir podrucja."))
                            }
                            downloaded++
                            processed++
                            onProgress?.invoke(processed, total, z)
                        } finally { conn.disconnect() }
                    }
                }
            }
            setActiveMapName(context, mapName)
            setOfflineBounds(context, mapName, OfflineBounds(spec.minLat, spec.maxLat, spec.minLon, spec.maxLon))
            packAndExportAsMbtiles(context, mapName) { onProgress?.invoke(-1, -1, -1) }
            Result.success(downloaded to skipped)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun estimateTiles(spec: OfflineAreaSpec): Int {
        var total = 0
        for (z in spec.minZoom..spec.maxZoom) {
            val xMin = max(0, min(WmsTileSource.lonToTileX(spec.minLon, z), WmsTileSource.lonToTileX(spec.maxLon, z)))
            val xMax = max(WmsTileSource.lonToTileX(spec.minLon, z), WmsTileSource.lonToTileX(spec.maxLon, z))
            val yMin = max(0, min(WmsTileSource.latToTileY(spec.maxLat, z), WmsTileSource.latToTileY(spec.minLat, z)))
            val yMax = max(WmsTileSource.latToTileY(spec.maxLat, z), WmsTileSource.latToTileY(spec.minLat, z))
            total += (xMax - xMin + 1) * (yMax - yMin + 1)
        }
        return total
    }

    private fun unzipIntoTileRoot(zipFile: File, root: File, onProgress: ((done: Int, total: Int, zoom: Int) -> Unit)? = null) {
        val total = runCatching {
            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                var count = 0
                var entry = zis.nextEntry
                while (entry != null) {
                    val originalName = entry.name.replace('\\', '/').trimStart('/')
                    if (!entry.isDirectory && originalName.isNotBlank()) {
                        val relative = normalizeEntryPath(originalName) ?: ""
                        if (relative.isNotBlank()) count++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                count
            }
        }.getOrDefault(0)
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var done = 0
            var entry = zis.nextEntry
            while (entry != null) {
                val originalName = entry.name.replace('\\', '/').trimStart('/')
                if (!entry.isDirectory && originalName.isNotBlank()) {
                    val relative = normalizeEntryPath(originalName) ?: ""
                    if (relative.isNotBlank()) {
                        val outFile = File(root, relative)
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                        done++
                        val zoom = relative.substringBefore('/').toIntOrNull() ?: 0
                        onProgress?.invoke(done, total, zoom)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun normalizeEntryPath(path: String): String? {
        val parts = path.split('/').filter { it.isNotBlank() }
        if (parts.size < 3) return null
        val zIndex = parts.indexOfFirst { it.all(Char::isDigit) }
        if (zIndex == -1 || zIndex + 2 >= parts.size) return null
        val z = parts[zIndex]
        val x = parts[zIndex + 1]
        val y = parts[zIndex + 2]
        if (!z.all(Char::isDigit) || !x.all(Char::isDigit) || !y.substringBefore('.').all(Char::isDigit)) return null
        return "$z/$x/$y.png"
    }

    suspend fun importTileZip(context: Context, uri: Uri, displayName: String, asOfflineMap: Boolean = true, onProgress: ((done: Int, total: Int, zoom: Int) -> Unit)? = null): Result<String> = withContext(Dispatchers.IO) {
        configureOsmdroidPaths(context)
        val mapName = sanitizeMapName(displayName.substringBeforeLast('.'))
        return@withContext try {
            val root = if (asOfflineMap) offlineMapDir(context, mapName) else customMapDir(context, mapName)
            if (root.exists()) root.deleteRecursively()
            root.mkdirs()
            val tempZip = File(context.cacheDir, "offline_tiles_import.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempZip).use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(IllegalStateException("ZIP nije dostupan"))

            unzipIntoTileRoot(tempZip, root, onProgress)
            tempZip.delete()
            val hasTiles = root.walkTopDown().any { it.isFile && it.extension.lowercase() == "png" }
            if (!hasTiles) {
                root.deleteRecursively()
                return@withContext Result.failure(IllegalStateException("ZIP ne sadrži tileove u formatu z/x/y.png"))
            }
            if (asOfflineMap) setActiveMapName(context, mapName)
            Result.success(mapName)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun importMbtiles(context: Context, uri: Uri, displayName: String): Result<String> = withContext(Dispatchers.IO) {
        configureOsmdroidPaths(context)
        val mapName = sanitizeMapName(displayName.substringBeforeLast('.'))
        return@withContext try {
            val root = customMapDir(context, mapName)
            if (root.exists()) root.deleteRecursively()
            root.mkdirs()
            val targetFile = File(root, MBTILES_FILE_NAME)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(IllegalStateException("MBTiles nije dostupan"))

            SQLiteDatabase.openDatabase(targetFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT COUNT(*) FROM tiles", null).use { cursor ->
                    if (!cursor.moveToFirst() || cursor.getInt(0) <= 0) {
                        targetFile.delete()
                        root.deleteRecursively()
                        return@withContext Result.failure(IllegalStateException("MBTiles nema čitljive tileove"))
                    }
                }
            }
            runCatching {
                val publicFile = File(publicMbtilesRoot(), sanitizeMapName(mapName) + ".mbtiles")
                targetFile.copyTo(publicFile, overwrite = true)
            }
            Result.success(mapName)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }


    fun renameCustomMap(context: Context, oldName: String, newNameRaw: String): Result<String> {
        val oldNormalized = sanitizeMapName(oldName)
        val newName = sanitizeMapName(newNameRaw)
        if (newName.isBlank()) return Result.failure(IllegalArgumentException("Ime ne može biti prazno"))
        if (oldNormalized == newName) return Result.success(oldNormalized)
        val oldDir = customMapDir(context, oldNormalized)
        val oldLegacyDir = legacyCustomMapDir(context, oldNormalized)
        val source = when {
            oldDir.exists() -> oldDir
            oldLegacyDir.exists() -> oldLegacyDir
            else -> return Result.failure(IllegalStateException("MBTiles layer nije pronađen"))
        }
        val target = customMapDir(context, newName)
        if (target.exists()) return Result.failure(IllegalStateException("Već postoji layer s tim imenom"))
        return runCatching {
            target.parentFile?.mkdirs()
            if (!source.renameTo(target)) {
                source.copyRecursively(target, overwrite = false)
                source.deleteRecursively()
            }
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val enabled = prefs.getStringSet(KEY_ENABLED_CUSTOM_OVERLAYS, emptySet()).orEmpty().toMutableSet()
            if (enabled.remove(oldNormalized)) enabled += newName
            val active = prefs.getString(KEY_ACTIVE_MAP, null)
            val edit = prefs.edit().putStringSet(KEY_ENABLED_CUSTOM_OVERLAYS, enabled)
            if (active == oldNormalized) edit.putString(KEY_ACTIVE_MAP, newName)
            edit.apply()
            runCatching { File(publicMbtilesRoot(), oldNormalized + ".mbtiles").delete() }
            mbtilesFile(target)?.let { file ->
                runCatching { file.copyTo(File(publicMbtilesRoot(), newName + ".mbtiles"), overwrite = true) }
            }
            newName
        }
    }

    fun clearAllCustomOverlays(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_ENABLED_CUSTOM_OVERLAYS, emptySet())
            .apply()
    }

    fun deleteCustomMap(context: Context, mapName: String) {
        clearCustomOverlay(context, mapName)
        customMapDir(context, mapName).deleteRecursively()
        legacyCustomMapDir(context, mapName).deleteRecursively()
        val next = listAllMaps(context).firstOrNull()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_ACTIVE_MAP, null) == sanitizeMapName(mapName)) {
            prefs.edit().putString(KEY_ACTIVE_MAP, next).apply()
        }
    }

    fun deleteOfflineTiles(context: Context) {
        getActiveMapName(context)?.let { deleteOfflineMap(context, it) }
    }

    fun deleteOfflineMap(context: Context, mapName: String) {
        offlineMapDir(context, mapName).deleteRecursively()
        val next = listAllMaps(context).firstOrNull()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ACTIVE_MAP, next).apply()
    }
}
