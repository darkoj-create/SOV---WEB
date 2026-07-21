package com.darko.speleov1.util

import org.osmdroid.views.MapView
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coalesces tile-worker redraw requests so a burst of completed tiles does not trigger one full
 * map redraw per tile. Tiles still appear quickly, but redraws are capped to roughly one per 60 ms.
 */
internal object MapInvalidateCoalescer {
    private val pendingInvalidate = AtomicBoolean(false)

    fun requestInvalidate(mapView: MapView) {
        if (!pendingInvalidate.compareAndSet(false, true)) return
        mapView.postDelayed({
            pendingInvalidate.set(false)
            mapView.invalidate()
        }, 60L)
    }
}
