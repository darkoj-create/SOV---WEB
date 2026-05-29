package com.darko.speleov1.util

import android.location.Location
import android.os.SystemClock
import com.darko.speleov1.TrackPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

object TrackingRuntime {
    private const val MIN_TRACK_POINT_DISTANCE_M = 2.0
    private const val MAX_TRACK_POINTS = 5000
    private const val MAX_WALKING_SPEED_MPS = 8.0
    private const val HARD_SHORT_JUMP_M = 35.0
    private const val HARD_SHORT_JUMP_SECONDS = 3.0
    private const val HARD_MEDIUM_JUMP_M = 120.0
    private const val HARD_MEDIUM_JUMP_SECONDS = 20.0
    private const val HARD_LONG_JUMP_M = 300.0
    private const val HARD_LONG_JUMP_SECONDS = 60.0
    private const val BAD_ACCURACY_M = 80.0

    data class State(
        val active: Boolean = false,
        val waitingForGpsFix: Boolean = false,
        val currentLocation: GeoPoint? = null,
        val accuracyM: Double? = null,
        val altitudeM: Double? = null,
        val provider: String? = null,
        val speedMps: Double? = null,
        val gpsBearingDeg: Float? = null,
        val gpsBearingAccuracyDeg: Float? = null,
        val points: List<TrackPoint> = emptyList(),
        val startedAtMillis: Long? = null,
        val lastAcceptedTimeMillis: Long? = null,
        val lastAcceptedRealtimeMillis: Long? = null,
        val pointsDropped: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun startSession(existingPoints: List<TrackPoint> = emptyList(), startedAtMillis: Long? = null) {
        val lastPoint = existingPoints.lastOrNull()
        _state.value = State(
            active = true,
            waitingForGpsFix = true,
            currentLocation = lastPoint?.point,
            altitudeM = lastPoint?.altitudeM,
            points = existingPoints,
            startedAtMillis = startedAtMillis ?: System.currentTimeMillis(),
            lastAcceptedTimeMillis = null,
            lastAcceptedRealtimeMillis = null,
        )
    }

    fun bootstrapLocation(location: Location) {
        val current = _state.value
        val point = GeoPoint(location.latitude, location.longitude)
        _state.value = current.copy(
            active = true,
            waitingForGpsFix = !LocationHelper.isGoodNavigationFix(location),
            currentLocation = point,
            accuracyM = if (location.hasAccuracy()) location.accuracy.toDouble() else current.accuracyM,
            altitudeM = if (location.hasAltitude()) location.altitude else current.altitudeM,
            provider = location.provider,
            speedMps = if (location.hasSpeed()) location.speed.toDouble() else current.speedMps,
            gpsBearingDeg = if (location.hasBearing()) location.bearing else current.gpsBearingDeg,
            gpsBearingAccuracyDeg = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && location.hasBearingAccuracy()) location.bearingAccuracyDegrees else current.gpsBearingAccuracyDeg,
        )
    }

    fun onLocation(location: Location) {
        val current = _state.value
        val nextPoint = TrackPoint(
            point = GeoPoint(location.latitude, location.longitude),
            altitudeM = location.takeIf { it.hasAltitude() }?.altitude,
        )
        val appendResult = appendTrackPoint(
            existing = current.points,
            next = nextPoint,
            location = location,
            lastAcceptedTimeMillis = current.lastAcceptedTimeMillis,
            lastAcceptedRealtimeMillis = current.lastAcceptedRealtimeMillis,
        )
        _state.value = current.copy(
            active = true,
            waitingForGpsFix = !LocationHelper.isGoodNavigationFix(location),
            currentLocation = nextPoint.point,
            accuracyM = if (location.hasAccuracy()) location.accuracy.toDouble() else current.accuracyM,
            altitudeM = if (location.hasAltitude()) location.altitude else current.altitudeM,
            provider = location.provider,
            speedMps = if (location.hasSpeed()) location.speed.toDouble() else current.speedMps,
            gpsBearingDeg = if (location.hasBearing()) location.bearing else current.gpsBearingDeg,
            gpsBearingAccuracyDeg = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && location.hasBearingAccuracy()) location.bearingAccuracyDegrees else current.gpsBearingAccuracyDeg,
            points = appendResult.points,
            lastAcceptedTimeMillis = appendResult.lastAcceptedTimeMillis,
            lastAcceptedRealtimeMillis = appendResult.lastAcceptedRealtimeMillis,
            pointsDropped = current.pointsDropped || appendResult.didTrim || appendResult.didDropSpike,
        )
    }

    fun stopSession(keepPoints: Boolean = true) {
        val current = _state.value
        _state.value = current.copy(
            active = false,
            waitingForGpsFix = false,
            points = if (keepPoints) current.points else emptyList(),
        )
    }

    fun clearPoints() {
        _state.value = _state.value.copy(points = emptyList(), pointsDropped = false)
    }

    private data class AppendResult(
        val points: List<TrackPoint>,
        val lastAcceptedTimeMillis: Long?,
        val lastAcceptedRealtimeMillis: Long?,
        val didTrim: Boolean = false,
        val didDropSpike: Boolean = false,
    )

    private fun appendTrackPoint(
        existing: List<TrackPoint>,
        next: TrackPoint,
        location: Location,
        lastAcceptedTimeMillis: Long?,
        lastAcceptedRealtimeMillis: Long?
    ): AppendResult {
        val nowRealtimeMillis = SystemClock.elapsedRealtime()
        val last = existing.lastOrNull()
        if (last != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                last.point.latitude,
                last.point.longitude,
                next.point.latitude,
                next.point.longitude,
                results,
            )
            val horizontalM = results.firstOrNull()?.toDouble() ?: 0.0
            if (horizontalM < MIN_TRACK_POINT_DISTANCE_M) {
                return AppendResult(existing, lastAcceptedTimeMillis, lastAcceptedRealtimeMillis, didTrim = false)
            }
            if (shouldIgnoreTrackSpike(horizontalM, location, lastAcceptedTimeMillis, lastAcceptedRealtimeMillis, nowRealtimeMillis)) {
                return AppendResult(existing, lastAcceptedTimeMillis, lastAcceptedRealtimeMillis, didDropSpike = true)
            }
        }
        val trimmed = existing + next
        val didTrim = trimmed.size > MAX_TRACK_POINTS
        val kept = if (didTrim) trimmed.takeLast(MAX_TRACK_POINTS) else trimmed
        val acceptedTime = when {
            location.time > 0L -> location.time
            else -> System.currentTimeMillis()
        }
        return AppendResult(kept, acceptedTime, nowRealtimeMillis, didTrim = didTrim)
    }

    private fun shouldIgnoreTrackSpike(
        horizontalM: Double,
        location: Location,
        lastAcceptedTimeMillis: Long?,
        lastAcceptedRealtimeMillis: Long?,
        nowRealtimeMillis: Long,
    ): Boolean {
        val accuracyM = if (location.hasAccuracy()) location.accuracy.toDouble() else 999.0
        if (accuracyM >= BAD_ACCURACY_M && horizontalM >= 20.0) return true
        if (horizontalM >= 250.0 && accuracyM >= 20.0) return true
        if (horizontalM >= 120.0 && accuracyM >= 35.0) return true

        val realtimeDeltaSeconds = lastAcceptedRealtimeMillis
            ?.takeIf { nowRealtimeMillis > it }
            ?.let { (nowRealtimeMillis - it) / 1000.0 }

        val locationTimeDeltaSeconds = lastAcceptedTimeMillis
            ?.takeIf { location.time > it }
            ?.let { (location.time - it) / 1000.0 }

        val deltaSeconds = realtimeDeltaSeconds ?: locationTimeDeltaSeconds
        val inferredSpeedMps = deltaSeconds
            ?.takeIf { it >= 0.5 }
            ?.let { horizontalM / it }

        if (deltaSeconds != null) {
            if (deltaSeconds <= HARD_SHORT_JUMP_SECONDS && horizontalM >= HARD_SHORT_JUMP_M) return true
            if (deltaSeconds <= HARD_MEDIUM_JUMP_SECONDS && horizontalM >= HARD_MEDIUM_JUMP_M) return true
            if (deltaSeconds <= HARD_LONG_JUMP_SECONDS && horizontalM >= HARD_LONG_JUMP_M) return true
        }

        if (inferredSpeedMps != null && inferredSpeedMps >= MAX_WALKING_SPEED_MPS && horizontalM >= 25.0) return true

        if (location.hasSpeed()) {
            val reportedSpeedMps = location.speed.toDouble()
            if (reportedSpeedMps >= 12.0 && horizontalM >= 25.0) return true
            if (reportedSpeedMps >= 8.0 && accuracyM >= 20.0 && horizontalM >= 25.0) return true
        }

        return false
    }
}
