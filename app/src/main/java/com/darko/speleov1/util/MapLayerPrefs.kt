package com.darko.speleov1.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val mapLayerPrefsGson = Gson()

enum class MapLayerMode { AUTO, OFFLINE, WMS, OPENTOPO, HGSS_SIGURNE_STAZE, HGSS_OSM_TEST }

fun MapLayerMode.normalizedBaseLayer(): MapLayerMode = when (this) {
    MapLayerMode.HGSS_SIGURNE_STAZE -> MapLayerMode.HGSS_OSM_TEST
    else -> this
}

data class WmsConfig(
    val baseUrl: String,
    val layers: String,
    val crs: String = "EPSG:3857",
    val version: String = "1.1.1",
    val styles: String = "",
    val transparent: Boolean = false
)

data class SavedWmsConfig(
    val name: String,
    val config: WmsConfig
)

object MapLayerPrefs {
    private const val PREFS = "map_layer_prefs"
    private const val KEY_MODE = "mode"
    private const val KEY_WMS_URL = "wms_url"
    private const val KEY_WMS_LAYERS = "wms_layers"
    private const val KEY_WMS_CRS = "wms_crs"
    private const val KEY_WMS_VERSION = "wms_version"
    private const val KEY_WMS_STYLES = "wms_styles"
    private const val KEY_WMS_TRANSPARENT = "wms_transparent"
    private const val KEY_CUSTOM_WMS_JSON = "custom_wms_json"
    private const val KEY_GEO_OVERLAY_ENABLED = "geo_overlay_enabled"
    private const val KEY_GEO_OVERLAY_OPACITY = "geo_overlay_opacity"

    const val DEFAULT_WMS_URL = "https://geoportal.dgu.hr/services/tk/ows"
    const val DEFAULT_WMS_LAYERS = "TK25_NOVI"


    val GEOLOGICAL_UNITS_OVERLAY_CONFIG = WmsConfig(
        baseUrl = "https://transformiraj.nipp.hr/ows/services/org.2.abf7ddc6-7578-4070-a9db-c291a42e55c6_wms",
        layers = "GE.GeologicUnit.AgeOfRocks",
        crs = "EPSG:4326",
        version = "1.3.0",
        styles = "",
        transparent = true
    )

    private fun normalizeConfig(
        baseUrl: String,
        layers: String,
        crs: String = "EPSG:3857",
        version: String = "1.1.1",
        styles: String = "",
        transparent: Boolean = false
    ): WmsConfig {
        val cleanBase = cleanWmsBaseUrl(baseUrl).ifBlank { DEFAULT_WMS_URL }
        val cleanLayers = layers.trim().ifBlank { DEFAULT_WMS_LAYERS }
        val isDguHok = cleanLayers.equals("HOK", ignoreCase = true) &&
            (cleanBase.contains("geoportal.dgu.hr/services/hok/wms", ignoreCase = true) ||
                cleanBase.equals("https://geoportal.dgu.hr/wms", ignoreCase = true) ||
                cleanBase.equals("http://geoportal.dgu.hr/wms", ignoreCase = true))
        return when {
            isDguHok -> WmsConfig(
                baseUrl = "https://geoportal.dgu.hr/wms",
                layers = "HOK",
                crs = "EPSG:3765",
                version = "1.3.0",
                styles = styles.trim().ifBlank { "raster" },
                transparent = true
            )
            cleanBase.contains("transformiraj.nipp.hr/ows/services/org.2.abf7ddc6-7578-4070-a9db-c291a42e55c6_wms", ignoreCase = true) &&
                (cleanLayers.equals("GE.GEOLOGICUNIT.AGEOFROCKS", ignoreCase = true) ||
                    cleanLayers.equals("GE.GeologicUnit.AgeOfRocks", ignoreCase = true) ||
                    cleanLayers.equals("Geological Units", ignoreCase = true)) -> WmsConfig(
                baseUrl = cleanBase,
                layers = "GE.GeologicUnit.AgeOfRocks",
                crs = crs.trim().ifBlank { "EPSG:4326" },
                version = when (version.trim()) {
                    "", "1.2" -> "1.3.0"
                    else -> version.trim()
                },
                styles = styles.trim(),
                transparent = true
            )
            else -> WmsConfig(
                baseUrl = cleanBase,
                layers = cleanLayers,
                crs = crs.trim().ifBlank { "EPSG:3857" },
                version = version.trim().ifBlank { "1.1.1" },
                styles = styles.trim(),
                transparent = transparent
            )
        }
    }

    fun cleanWmsBaseUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (!trimmed.contains('?')) return trimmed.trimEnd('?', '&')
        val beforeQuery = trimmed.substringBefore('?')
        val query = trimmed.substringAfter('?', "")
        val ignoredKeys = setOf(
            "service", "request", "version", "layers", "styles", "format", "transparent",
            "srs", "crs", "width", "height", "bbox", "exceptions", "time"
        )
        val kept = query.split('&')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { part -> part.substringBefore('=', part).lowercase() !in ignoredKeys }
        return if (kept.isEmpty()) beforeQuery.trimEnd('?', '&') else beforeQuery.trimEnd('?', '&') + "?" + kept.joinToString("&")
    }

    fun getMode(context: Context): MapLayerMode {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, MapLayerMode.WMS.name)
        return runCatching { MapLayerMode.valueOf(raw ?: MapLayerMode.WMS.name) }.getOrDefault(MapLayerMode.WMS).normalizedBaseLayer()
    }

    fun setMode(context: Context, mode: MapLayerMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getWmsConfig(context: Context): WmsConfig {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return normalizeConfig(
            prefs.getString(KEY_WMS_URL, DEFAULT_WMS_URL) ?: DEFAULT_WMS_URL,
            prefs.getString(KEY_WMS_LAYERS, DEFAULT_WMS_LAYERS) ?: DEFAULT_WMS_LAYERS,
            prefs.getString(KEY_WMS_CRS, "EPSG:3857") ?: "EPSG:3857",
            prefs.getString(KEY_WMS_VERSION, "1.1.1") ?: "1.1.1",
            prefs.getString(KEY_WMS_STYLES, "") ?: "",
            prefs.getBoolean(KEY_WMS_TRANSPARENT, false)
        )
    }

    fun setWmsConfig(context: Context, config: WmsConfig) {
        val normalized = normalizeConfig(config.baseUrl, config.layers, config.crs, config.version, config.styles, config.transparent)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_WMS_URL, normalized.baseUrl)
            .putString(KEY_WMS_LAYERS, normalized.layers)
            .putString(KEY_WMS_CRS, normalized.crs)
            .putString(KEY_WMS_VERSION, normalized.version)
            .putString(KEY_WMS_STYLES, normalized.styles)
            .putBoolean(KEY_WMS_TRANSPARENT, normalized.transparent)
            .apply()
    }

    fun getCustomWmsConfigs(context: Context): List<SavedWmsConfig> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CUSTOM_WMS_JSON, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<SavedWmsConfig>>() {}.type
            mapLayerPrefsGson.fromJson<List<SavedWmsConfig>>(raw, type).orEmpty()
                .mapNotNull { saved ->
                    val name = saved.name.trim()
                    val config = normalizeConfig(saved.config.baseUrl, saved.config.layers, saved.config.crs, saved.config.version, saved.config.styles, saved.config.transparent)
                    if (name.isBlank() || config.baseUrl.isBlank() || config.layers.isBlank()) null else SavedWmsConfig(name, config)
                }
                .distinctBy { it.name.lowercase() }
        }.getOrDefault(emptyList())
    }

    fun saveCustomWmsConfig(context: Context, name: String, config: WmsConfig) {
        val cleanName = name.trim().ifBlank { config.layers.trim().ifBlank { "Custom WMS" } }
        val normalized = normalizeConfig(config.baseUrl, config.layers, config.crs, config.version, config.styles, config.transparent)
        val next = getCustomWmsConfigs(context).filterNot { it.name.equals(cleanName, ignoreCase = true) }.toMutableList()
        next.add(0, SavedWmsConfig(cleanName, normalized))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_CUSTOM_WMS_JSON, mapLayerPrefsGson.toJson(next.take(20)))
            .apply()
    }

    fun deleteCustomWmsConfig(context: Context, name: String) {
        val remaining = getCustomWmsConfigs(context).filterNot { it.name.equals(name.trim(), ignoreCase = true) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_CUSTOM_WMS_JSON, mapLayerPrefsGson.toJson(remaining))
            .apply()
    }


    fun isGeologicalOverlayEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_GEO_OVERLAY_ENABLED, false)
    }

    fun setGeologicalOverlayEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_GEO_OVERLAY_ENABLED, enabled)
            .apply()
    }

    fun getGeologicalOverlayOpacityPercent(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_GEO_OVERLAY_OPACITY, 45)
            .coerceIn(10, 90)
    }

    fun setGeologicalOverlayOpacityPercent(context: Context, opacityPercent: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_GEO_OVERLAY_OPACITY, opacityPercent.coerceIn(10, 90))
            .apply()
    }
}
