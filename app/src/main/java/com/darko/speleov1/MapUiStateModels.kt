package com.darko.speleov1

import org.osmdroid.util.GeoPoint

/** Small map UI state models used while splitting map ownership out of SpeleoAppRoot. */
data class MapCameraSnapshot(
    val center: GeoPoint? = null,
    val zoom: Double = 8.0
)

data class MapGpsSnapshot(
    val location: GeoPoint? = null,
    val accuracyM: Double? = null,
    val altitudeM: Double? = null,
    val provider: String? = null,
    val speedMps: Double? = null,
    val bearingDeg: Float? = null,
    val bearingAccuracyDeg: Float? = null,
    val waitingForFix: Boolean = false,
    val positionEnabled: Boolean = false,
    val autoCenterEnabled: Boolean = false
)
