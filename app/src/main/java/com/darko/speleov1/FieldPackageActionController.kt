package com.darko.speleov1

import android.content.Context
import com.darko.speleov1.model.SpeleoRecord
import com.darko.speleov1.util.OfflineTileManager
import org.osmdroid.util.GeoPoint
import kotlin.math.abs

/**
 * v1.7.89 refactor pass 1.
 *
 * Keeps Field Package / Izleti map-opening calculations out of SpeleoAppRoot.
 * This controller is deliberately pure/small: it does not mutate app state, navigate,
 * or show Toasts. SpeleoAppRoot decides how to apply the returned plan.
 */
object FieldPackageActionController {
    fun buildOpenMapPlan(
        context: Context,
        pkg: FieldPackageSummary,
        records: List<SpeleoRecord>,
        markedPoints: List<MarkedPoint>,
        savedTracks: List<SavedTrack>
    ): FieldPackageOpenMapPlan {
        val packageRecords = filterRecordsForFieldPackage(records, pkg)
        val packagePoints = filterMarkedPointsForFieldPackage(markedPoints, pkg)
        val packageTracks = filterSavedTracksForFieldPackage(savedTracks, pkg)

        val focusPoints = packageRecords.mapNotNull { rec ->
            rec.location.lat?.let { lat -> rec.location.lon?.let { lon -> GeoPoint(lat, lon) } }
        } + packagePoints.map { GeoPoint(it.lat, it.lon) } +
            packageTracks.flatMap { track -> track.points.map { it.point } }

        val packageCenter = pkg.centerLat?.let { lat -> pkg.centerLon?.let { lon -> GeoPoint(lat, lon) } }
        val packageBounds = if (pkg.minLat != null && pkg.maxLat != null && pkg.minLon != null && pkg.maxLon != null) {
            OfflineTileManager.OfflineBounds(pkg.minLat, pkg.maxLat, pkg.minLon, pkg.maxLon)
        } else {
            null
        }
        val offlineBounds = pkg.offlineMapName?.let { mapName ->
            OfflineTileManager.getOfflineBounds(context, mapName)
        } ?: packageBounds

        val boundsCenter = offlineBounds?.let { bounds ->
            GeoPoint((bounds.minLat + bounds.maxLat) / 2.0, (bounds.minLon + bounds.maxLon) / 2.0)
        }
        val focusCenter = packageCenter ?: boundsCenter
        val focusZoom = focusCenter?.let { computeFocusZoom(pkg, offlineBounds) }

        return FieldPackageOpenMapPlan(
            focusPoints = focusPoints,
            focusCenter = focusCenter,
            focusZoom = focusZoom,
            shouldEnableOfflineMap = pkg.includesOfflineMap && pkg.offlineMapName != null,
            offlineMapName = pkg.offlineMapName
        )
    }

    private fun computeFocusZoom(pkg: FieldPackageSummary, bounds: OfflineTileManager.OfflineBounds?): Double {
        val boundsSpan = bounds?.let { maxOf(abs(it.maxLat - it.minLat), abs(it.maxLon - it.minLon)) }
        return when {
            boundsSpan != null && boundsSpan <= 0.01 -> 16.0
            boundsSpan != null && boundsSpan <= 0.03 -> 15.0
            boundsSpan != null && boundsSpan <= 0.08 -> 14.0
            boundsSpan != null && boundsSpan <= 0.2 -> 13.0
            boundsSpan != null && boundsSpan <= 0.5 -> 12.0
            boundsSpan != null -> 11.0
            pkg.radiusKm <= 0.7 -> 16.0
            pkg.radiusKm <= 1.5 -> 15.0
            pkg.radiusKm <= 3.0 -> 14.0
            pkg.radiusKm <= 7.0 -> 13.0
            pkg.radiusKm <= 15.0 -> 12.0
            else -> 11.0
        }
    }
}

data class FieldPackageOpenMapPlan(
    val focusPoints: List<GeoPoint>,
    val focusCenter: GeoPoint?,
    val focusZoom: Double?,
    val shouldEnableOfflineMap: Boolean,
    val offlineMapName: String?
)
