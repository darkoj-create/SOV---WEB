package com.darko.speleov1.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CoordinateConverterTest {
    @Test
    fun centralMeridianAtEquatorMapsToFalseEasting() {
        val htrs = CoordinateConverter.wgs84ToHtrs96Tm(0.0, 16.5)
        assertEquals(500_000.0, htrs.x, 0.001)
        assertEquals(0.0, htrs.y, 0.001)
    }

    @Test
    fun zagrebReferencePointIsStable() {
        val htrs = CoordinateConverter.wgs84ToHtrs96Tm(45.8150, 15.9819)
        assertEquals(459_736.762, htrs.x, 0.5)
        assertEquals(5_075_146.257, htrs.y, 0.5)
    }

    @Test
    fun splitReferencePointIsStable() {
        val htrs = CoordinateConverter.wgs84ToHtrs96Tm(43.5081, 16.4402)
        assertEquals(495_164.705, htrs.x, 0.5)
        assertEquals(4_818_688.440, htrs.y, 0.5)
    }

    @Test
    fun zagrebRoundTripIsWithinCentimeters() = assertRoundTrip(45.8150, 15.9819)

    @Test
    fun rijekaRoundTripIsWithinCentimeters() = assertRoundTrip(45.3271, 14.4422)

    @Test
    fun pulaRoundTripIsWithinCentimeters() = assertRoundTrip(44.8666, 13.8496)

    @Test
    fun dubrovnikRoundTripIsWithinCentimeters() = assertRoundTrip(42.6507, 18.0944)

    @Test
    fun outOfCroatiaInputStillReturnsFiniteValues() {
        val htrs = CoordinateConverter.wgs84ToHtrs96Tm(47.0, 20.0)
        assertTrue(htrs.x.isFinite())
        assertTrue(htrs.y.isFinite())
        val wgs = CoordinateConverter.htrs96TmToWgs84(htrs.x, htrs.y)
        assertTrue(wgs.lat.isFinite())
        assertTrue(wgs.lon.isFinite())
    }

    private fun assertRoundTrip(lat: Double, lon: Double) {
        val htrs = CoordinateConverter.wgs84ToHtrs96Tm(lat, lon)
        val wgs = CoordinateConverter.htrs96TmToWgs84(htrs.x, htrs.y)
        assertTrue("lat diff=${abs(lat - wgs.lat)}", abs(lat - wgs.lat) < 1e-7)
        assertTrue("lon diff=${abs(lon - wgs.lon)}", abs(lon - wgs.lon) < 1e-7)
    }
}
