package com.darko.speleov1

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

/**
 * Thin screen-scoped ViewModel introduced as the first step of the SpeleoAppRoot split.
 * Keep map-only UI state here as we move logic out of SpeleoApp() in small safe patches.
 */
class MapScreenViewModel : ViewModel() {
    private val _state = MutableStateFlow(MapScreenState())
    val state: StateFlow<MapScreenState> = _state.asStateFlow()

    fun focus(point: GeoPoint?, zoom: Double = 14.0) {
        _state.value = _state.value.copy(focusPoint = point, focusZoom = zoom, focusNonce = _state.value.focusNonce + 1)
    }

    fun updateCamera(center: GeoPoint?, zoom: Double) {
        _state.value = _state.value.copy(camera = MapCameraSnapshot(center = center, zoom = zoom))
    }

    fun updateGps(snapshot: MapGpsSnapshot) {
        _state.value = _state.value.copy(gps = snapshot)
    }
}


data class MapScreenState(
    val focusPoint: GeoPoint? = null,
    val focusZoom: Double = 14.0,
    val focusNonce: Int = 0,
    val camera: MapCameraSnapshot = MapCameraSnapshot(),
    val gps: MapGpsSnapshot = MapGpsSnapshot()
)
