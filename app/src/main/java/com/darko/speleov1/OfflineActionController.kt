package com.darko.speleov1

import android.content.Context
import com.darko.speleov1.util.MapLayerMode
import com.darko.speleov1.util.MapLayerPrefs
import com.darko.speleov1.util.OfflineTileManager

/**
 * Small action/query layer for Offline / Karte i slojevi.
 *
 * This keeps storage and overlay bookkeeping out of the large app root and out
 * of the composable body. It is intentionally thin: rendering, import/export
 * and map logic still stay in their existing tested systems.
 */
class OfflineActionController(private val context: Context) {
    fun ensureFolders() {
        OfflineTileManager.ensureOfflineFolderStructure(context)
        OfflineTileManager.ensurePublicOfflineFolderStructure()
    }

    fun snapshot(
        markedPoints: List<MarkedPoint>,
        savedTracks: List<SavedTrack>,
        importedLayers: List<ImportedLayer>
    ): OfflineLibrarySnapshot {
        val offlineMaps = OfflineTileManager.listOfflineMaps(context)
        val customMaps = OfflineTileManager.listCustomMaps(context)
        val enabledCustomOverlays = OfflineTileManager.getEnabledCustomOverlayNames(context)
        val activeMapName = OfflineTileManager.getActiveMapName(context)
        val selectedMode = MapLayerPrefs.getMode(context)
        val hasOffline = OfflineTileManager.hasOfflineTiles(context)
        return OfflineLibrarySnapshot(
            offlineMaps = offlineMaps,
            customMaps = customMaps,
            enabledCustomOverlays = enabledCustomOverlays,
            hasOffline = hasOffline,
            tileCount = OfflineTileManager.localTileCount(context),
            selectedMode = selectedMode,
            activeMapName = activeMapName,
            visibleOverlayCount = enabledCustomOverlays.size +
                importedLayers.count { it.visible } +
                markedPoints.count { it.visible } +
                savedTracks.count { it.visible }
        )
    }

    fun mapInfo(mapName: String): StoredMapUiInfo = StoredMapUiInfo(
        tileCount = OfflineTileManager.localTileCount(context, mapName),
        bounds = OfflineTileManager.getOfflineBounds(context, mapName),
        isMbtiles = OfflineTileManager.isMbtilesMap(context, mapName)
    )

    fun setActiveMap(mapName: String) {
        OfflineTileManager.setActiveMapName(context, mapName)
    }

    fun deleteOfflineMap(mapName: String): String? {
        OfflineTileManager.deleteOfflineMap(context, mapName)
        if (MapLayerPrefs.getMode(context) == MapLayerMode.OFFLINE && OfflineTileManager.listOfflineMaps(context).isEmpty()) {
            MapLayerPrefs.setMode(context, MapLayerMode.AUTO)
        }
        return OfflineTileManager.getActiveMapName(context)
    }

    fun setCustomOverlayEnabled(mapName: String, enabled: Boolean) {
        OfflineTileManager.setCustomOverlayEnabled(context, mapName, enabled)
    }

    fun clearCustomOverlays() {
        OfflineTileManager.clearAllCustomOverlays(context)
    }

    fun enableCustomOverlay(mapName: String) {
        setCustomOverlayEnabled(mapName, true)
    }

    fun afterCustomMapDeleted(currentSelectedMap: String?, deletedMapName: String): String? {
        if (currentSelectedMap == deletedMapName) {
            val next = (OfflineTileManager.listCustomMaps(context) + OfflineTileManager.listOfflineMaps(context)).firstOrNull()
            if (next != null) OfflineTileManager.setActiveMapName(context, next)
        }
        if (MapLayerPrefs.getMode(context) == MapLayerMode.OFFLINE && !OfflineTileManager.hasOfflineTiles(context)) {
            MapLayerPrefs.setMode(context, MapLayerMode.AUTO)
        }
        return OfflineTileManager.getActiveMapName(context)
    }
}


data class OfflineLibrarySnapshot(
    val offlineMaps: List<String> = emptyList(),
    val customMaps: List<String> = emptyList(),
    val enabledCustomOverlays: Set<String> = emptySet(),
    val hasOffline: Boolean = false,
    val tileCount: Int = 0,
    val selectedMode: MapLayerMode = MapLayerMode.WMS,
    val activeMapName: String? = null,
    val visibleOverlayCount: Int = 0
)
