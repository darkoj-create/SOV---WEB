package com.darko.speleov1

import android.content.Context
import com.darko.speleov1.util.MapLayerMode
import com.darko.speleov1.util.MapLayerPrefs
import com.darko.speleov1.util.WmsConfig

/**
 * Map-specific user actions extracted from SpeleoAppRoot as the first safe map refactor pass.
 *
 * This keeps side effects that belong to the map layer out of the giant root composable while
 * preserving the current osmdroid/overlay implementation. Later passes can move GPS, drawing,
 * ruler and overlay state behind the MapScreenViewModel without changing app behaviour.
 */
internal class MapActionController(
    private val context: Context,
    private val onMapLayerChanged: () -> Unit,
    private val onNavigateToMap: () -> Unit,
    private val onDisableAutoCenter: () -> Unit
) {
    fun openTk25BaseMap() {
        MapLayerPrefs.setWmsConfig(
            context,
            WmsConfig(MapLayerPrefs.DEFAULT_WMS_URL, MapLayerPrefs.DEFAULT_WMS_LAYERS)
        )
        MapLayerPrefs.setMode(context, MapLayerMode.WMS)
        onMapLayerChanged()
        onNavigateToMap()
    }

    fun pauseUserAutoFocusForManualTarget() {
        onDisableAutoCenter()
    }
}
