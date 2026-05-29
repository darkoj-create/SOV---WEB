package com.darko.speleov1.util

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.net.URLEncoder
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

class WmsTileSource(
    private val config: WmsConfig
) : OnlineTileSourceBase(
    "WMS_" + (config.baseUrl + "|" + config.layers + "|" + config.crs + "|" + config.version + "|" + config.styles + "|" + config.transparent).hashCode(),
    0,
    19,
    256,
    ".png",
    arrayOf(config.baseUrl)
) {

    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return buildTileUrl(config, z, x, y)
    }

    companion object {
        fun buildTileUrl(config: WmsConfig, z: Int, x: Int, y: Int): String {
            val leftLon = tileXToLon(x, z)
            val rightLon = tileXToLon(x + 1, z)
            val topLat = tileYToLat(y, z)
            val bottomLat = tileYToLat(y + 1, z)
            return buildMapUrl(
                config = config,
                leftLon = leftLon,
                bottomLat = bottomLat,
                rightLon = rightLon,
                topLat = topLat,
                width = 256,
                height = 256
            )
        }

        fun buildMapUrl(
            config: WmsConfig,
            leftLon: Double,
            bottomLat: Double,
            rightLon: Double,
            topLat: Double,
            width: Int,
            height: Int
        ): String {
            val safeBase = if (config.baseUrl.contains('?')) "${config.baseUrl}&" else "${config.baseUrl}?"
            val version = config.version.ifBlank { "1.1.1" }
            val crs = config.crs.ifBlank { "EPSG:3857" }
            val isWms13 = version == "1.3.0"
            val crsParam = if (isWms13) "CRS" else "SRS"
            val bbox = when {
                crs.equals("CRS:84", ignoreCase = true) || crs.equals("OGC:CRS84", ignoreCase = true) -> {
                    "${leftLon},${bottomLat},${rightLon},${topLat}"
                }
                crs.equals("EPSG:4326", ignoreCase = true) -> {
                    if (isWms13) {
                        "${bottomLat},${leftLon},${topLat},${rightLon}"
                    } else {
                        "${leftLon},${bottomLat},${rightLon},${topLat}"
                    }
                }
                crs.equals("EPSG:3765", ignoreCase = true) -> {
                    val corners = listOf(
                        CoordinateConverter.wgs84ToHtrs96Tm(bottomLat, leftLon),
                        CoordinateConverter.wgs84ToHtrs96Tm(topLat, leftLon),
                        CoordinateConverter.wgs84ToHtrs96Tm(bottomLat, rightLon),
                        CoordinateConverter.wgs84ToHtrs96Tm(topLat, rightLon)
                    )
                    val minX = corners.minOf { it.x }
                    val minY = corners.minOf { it.y }
                    val maxX = corners.maxOf { it.x }
                    val maxY = corners.maxOf { it.y }
                    "$minX,$minY,$maxX,$maxY"
                }
                else -> {
                    val min = lonLatToMercator(leftLon, bottomLat)
                    val max = lonLatToMercator(rightLon, topLat)
                    "${min.first},${min.second},${max.first},${max.second}"
                }
            }

            return safeBase + listOf(
                "SERVICE=WMS",
                "REQUEST=GetMap",
                "VERSION=${urlEncode(version)}",
                "LAYERS=${urlEncode(config.layers)}",
                "STYLES=${urlEncode(config.styles)}",
                "FORMAT=${urlEncode(preferredImageFormat(config))}",
                "TRANSPARENT=${if (config.transparent) "true" else "false"}",
                "$crsParam=${urlEncode(crs)}",
                "WIDTH=${width.coerceIn(64, 2048)}",
                "HEIGHT=${height.coerceIn(64, 2048)}",
                "BBOX=$bbox"
            ).joinToString("&")
        }

        fun preferredImageFormat(config: WmsConfig): String {
            val text = (config.baseUrl + " " + config.layers).lowercase()
            return when {
                config.transparent -> "image/png"
                text.contains("ortho") || text.contains("dof") || text.contains("oi.orthoimagecoverage") -> "image/jpeg"
                // Raster base maps (TK25, HOK and similar DGU/IGN raster topo layers) decode
                // ~2x faster as JPEG and produce ~4x smaller disk cache than PNG. Used only for
                // opaque base layers; transparent overlays still get PNG above.
                text.contains("tk25") || text.contains("tk_25") || text.contains("hok") ||
                    text.contains("topo") || text.contains("htrs") -> "image/jpeg"
                else -> "image/png"
            }
        }

        fun preferredFileExtension(config: WmsConfig): String =
            if (preferredImageFormat(config).contains("jpeg", ignoreCase = true)) "jpg" else "png"

        fun buildTileUrl(baseUrl: String, layers: String, z: Int, x: Int, y: Int): String {
            return buildTileUrl(WmsConfig(baseUrl = baseUrl, layers = layers), z, x, y)
        }

        fun lonToTileX(lon: Double, z: Int): Int = floor((lon + 180.0) / 360.0 * (1 shl z)).toInt()

        fun latToTileY(lat: Double, z: Int): Int {
            val latRad = Math.toRadians(lat)
            val n = Math.pow(2.0, z.toDouble())
            return floor((1.0 - ln(tan(latRad) + 1 / cos(latRad)) / Math.PI) / 2.0 * n).toInt()
        }

        private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

        fun tileXToLon(x: Int, z: Int): Double = x / Math.pow(2.0, z.toDouble()) * 360.0 - 180.0

        fun tileYToLat(y: Int, z: Int): Double {
            val n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z.toDouble())
            return Math.toDegrees(atan(sinh(n)))
        }

        private fun lonLatToMercator(lon: Double, lat: Double): Pair<Double, Double> {
            val originShift = 20037508.342789244
            val mx = lon * originShift / 180.0
            var my = ln(Math.tan((90.0 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0)
            my *= originShift / 180.0
            return mx to my
        }
    }
}
