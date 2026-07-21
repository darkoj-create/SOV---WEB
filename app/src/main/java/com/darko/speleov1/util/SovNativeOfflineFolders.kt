package com.darko.speleov1.util

import android.content.Context

/**
 * Rebuilds the app UI from the durable user folders:
 * Download/SOV/Offline/{gpx,kml,mbtiles,maps,geojson,tables,databases}.
 *
 * This is intentionally additive: it imports only missing items and never deletes
 * existing app data. It is safe to run on every startup, update, reinstall, and
 * whenever the user opens Offline/Laptop hub screens.
 */
object SovNativeOfflineFolders {
    data class Result(
        val maps: Int = 0,
        val layers: Int = 0,
        val myBase: Int = 0
    ) {
        val total: Int get() = maps + layers + myBase
    }

    fun scanAndRestore(context: Context): Result {
        OfflineTileManager.ensureOfflineFolderStructure(context)
        OfflineTileManager.ensurePublicOfflineFolderStructure()
        val linked = SovLinkedOfflineFolder.scanAndRestore(context)
        val maps = OfflineTileManager.restoreFromPublicOfflineFolders(context) + linked.maps
        val myBase = MyBaseRepository.restoreFromOfflineFolders(context)
        val layers = UserContentStore.restoreImportedLayersFromOfflineFolders(context) + linked.layers
        return Result(maps = maps, layers = layers, myBase = myBase)
    }
}
