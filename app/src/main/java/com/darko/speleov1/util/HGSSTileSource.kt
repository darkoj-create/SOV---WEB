package com.darko.speleov1.util

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

/**
 * Stable raster WMTS REST tile source for SigurneStaze.hr / HGSS.
 *
 * Important: GeoServer GWC REST WMTS path order is:
 * {TileMatrix}/{TileRow}/{TileCol} -> z/y/x, not z/x/y.
 *
 * We intentionally use HGSS:OSM as the public app layer because it behaves like a
 * continuous base map for Croatia. HGSS:HGSS_Karte is not exposed in the UI anymore:
 * it has visible gaps in many areas and should stay an internal/test layer only.
 * This is a visual map layer only, not a searchable trails/POI database.
 */
class HGSSTileSource(
    @Suppress("UNUSED_PARAMETER") private val fallbackOsmTest: Boolean = true
) : OnlineTileSourceBase(
    // Keep a dedicated cache namespace for the stable HGSS OSM raster.
    // Older builds used HGSS_SIGURNE_STAZE for HGSS:HGSS_Karte, so reusing
    // that tile-source name can show stale cached broken/patchy tiles.
    "HGSS_OSM_STABLE_V2",
    0,
    19,
    256,
    ".png",
    arrayOf("https://sigurnestaze.hr/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return buildHgssOsmTileUrl(z, x, y)
    }

    companion object {
        fun buildHgssOsmTileUrl(z: Int, x: Int, y: Int): String =
            "https://sigurnestaze.hr/geoserver/gwc/service/wmts/rest/HGSS:OSM/raster/EPSG:900913/EPSG:900913:$z/$y/$x?format=image/png8"
    }
}
