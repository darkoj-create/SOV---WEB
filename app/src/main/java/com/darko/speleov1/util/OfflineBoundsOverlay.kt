package com.darko.speleov1.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class OfflineBoundsOverlay(private val bounds: OfflineTileManager.OfflineBounds) : Overlay() {
    private val strokePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        color = Color.argb(28, 255, 0, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection
        val nw = Point()
        val ne = Point()
        val se = Point()
        val sw = Point()
        projection.toPixels(GeoPoint(bounds.maxLat, bounds.minLon), nw)
        projection.toPixels(GeoPoint(bounds.maxLat, bounds.maxLon), ne)
        projection.toPixels(GeoPoint(bounds.minLat, bounds.maxLon), se)
        projection.toPixels(GeoPoint(bounds.minLat, bounds.minLon), sw)

        val path = Path().apply {
            moveTo(nw.x.toFloat(), nw.y.toFloat())
            lineTo(ne.x.toFloat(), ne.y.toFloat())
            lineTo(se.x.toFloat(), se.y.toFloat())
            lineTo(sw.x.toFloat(), sw.y.toFloat())
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }
}
