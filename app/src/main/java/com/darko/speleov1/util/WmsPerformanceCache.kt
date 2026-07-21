package com.darko.speleov1.util

import android.content.Context
import android.net.http.HttpResponseCache
import java.io.File

/**
 * Shared WMS cache helpers.
 *
 * Interactive prefetch used to live here, but visible overlays now own their request queues and
 * center-first scheduling. This object remains the stable home for cache installation, source keys
 * and the on-disk cache paths; cache schema/version is intentionally unchanged.
 */
object WmsPerformanceCache {
    private const val HTTP_CACHE_MB = 128L
    private const val CACHE_SCHEMA_VERSION = "v2"

    @Volatile private var installed = false

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
