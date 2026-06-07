package com.darko.speleov1.util

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

object CoordinateConverter {
    data class LatLon(val lat: Double, val lon: Double)

    data class Htrs96Tm(val x: Double, val y: Double)

    fun wgs84ToHtrs96Tm(lat: Double, lon: Double): Htrs96Tm {
        val a = 6378137.0
        val f = 1.0 / 298.257222101
        val e2 = 2 * f - f * f
        val ep2 = e2 / (1.0 - e2)
        val k0 = 0.9999
        val falseEasting = 500000.0
        val lon0 = Math.toRadians(16.5)

        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val tanLat = tan(latRad)

        val n = a / kotlin.math.sqrt(1.0 - e2 * sinLat * sinLat)
        val t = tanLat * tanLat
        val c = ep2 * cosLat * cosLat
        val aTerm = (lonRad - lon0) * cosLat

        val m = a * (
            (1.0 - e2 / 4.0 - 3.0 * e2 * e2 / 64.0 - 5.0 * e2 * e2 * e2 / 256.0) * latRad
                - (3.0 * e2 / 8.0 + 3.0 * e2 * e2 / 32.0 + 45.0 * e2 * e2 * e2 / 1024.0) * sin(2.0 * latRad)
                + (15.0 * e2 * e2 / 256.0 + 45.0 * e2 * e2 * e2 / 1024.0) * sin(4.0 * latRad)
                - (35.0 * e2 * e2 * e2 / 3072.0) * sin(6.0 * latRad)
            )

        val x = falseEasting + k0 * n * (
            aTerm
                + (1.0 - t + c) * aTerm * aTerm * aTerm / 6.0
                + (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * ep2) * aTerm.pow(5) / 120.0
            )

        val y = k0 * (
            m + n * tanLat * (
                aTerm * aTerm / 2.0
                    + (5.0 - t + 9.0 * c + 4.0 * c * c) * aTerm.pow(4) / 24.0
                    + (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * ep2) * aTerm.pow(6) / 720.0
                )
            )

        return Htrs96Tm(x = x, y = y)
    }


    /**
     * Converts HTRS96/TM (EPSG:3765) Easting/Northing to WGS84 decimal lat/lon.
     * Input order: x = Easting, y = Northing.
     */
    fun htrs96TmToWgs84(x: Double, y: Double): LatLon {
        val a = 6378137.0 // GRS80
        val f = 1.0 / 298.257222101
        val e2 = 2 * f - f * f
        val ep2 = e2 / (1.0 - e2)
        val k0 = 0.9999
        val falseEasting = 500000.0
        val falseNorthing = 0.0
        val lon0 = Math.toRadians(16.5)

        val e1 = (1.0 - kotlin.math.sqrt(1.0 - e2)) / (1.0 + kotlin.math.sqrt(1.0 - e2))

        val xAdj = x - falseEasting
        val yAdj = y - falseNorthing
        val m = yAdj / k0
        val mu = m / (a * (1.0 - e2 / 4.0 - 3.0 * e2.pow(2) / 64.0 - 5.0 * e2.pow(3) / 256.0))

        val phi1 = mu +
            (3 * e1 / 2 - 27 * e1.pow(3) / 32) * sin(2 * mu) +
            (21 * e1.pow(2) / 16 - 55 * e1.pow(4) / 32) * sin(4 * mu) +
            (151 * e1.pow(3) / 96) * sin(6 * mu) +
            (1097 * e1.pow(4) / 512) * sin(8 * mu)

        val sinPhi1 = sin(phi1)
        val cosPhi1 = cos(phi1)
        val tanPhi1 = tan(phi1)

        val n1 = a / kotlin.math.sqrt(1.0 - e2 * sinPhi1 * sinPhi1)
        val r1 = a * (1.0 - e2) / (1.0 - e2 * sinPhi1 * sinPhi1).pow(1.5)
        val t1 = tanPhi1 * tanPhi1
        val c1 = ep2 * cosPhi1 * cosPhi1
        val d = xAdj / (n1 * k0)

        val latRad = phi1 - (n1 * tanPhi1 / r1) * (
            d * d / 2.0 -
                (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1 * c1 - 9.0 * ep2) * d.pow(4) / 24.0 +
                (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1 * t1 - 252.0 * ep2 - 3.0 * c1 * c1) * d.pow(6) / 720.0
            )

        val lonRad = lon0 + (
            d - (1.0 + 2.0 * t1 + c1) * d.pow(3) / 6.0 +
                (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1 * c1 + 8.0 * ep2 + 24.0 * t1 * t1) * d.pow(5) / 120.0
            ) / cosPhi1

        return LatLon(Math.toDegrees(latRad), Math.toDegrees(lonRad))
    }
}
