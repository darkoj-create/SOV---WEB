package com.darko.speleov1.util

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.io.File

class LocalPngTileSource(private val rootPath: String) : OnlineTileSourceBase(
    "LocalOfflinePng",
    0,
    17,
    256,
    ".png",
    arrayOf("file://$rootPath/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val file = File(rootPath, "$z/$x/$y.png")
        return file.toURI().toString()
    }
}
