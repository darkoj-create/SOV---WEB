package com.darko.speleov1.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevationRepositoryTest {
    @Test
    fun decodesTerrariumRgbToMeters() {
        assertEquals(0.0, ElevationTileMath.terrariumHeightMeters(128, 0, 0), 0.0001)
        assertEquals(1.0, ElevationTileMath.terrariumHeightMeters(128, 1, 0), 0.0001)
        assertEquals(-1.0, ElevationTileMath.terrariumHeightMeters(127, 255, 0), 0.0001)
        assertEquals(0.5, ElevationTileMath.terrariumHeightMeters(128, 0, 128), 0.0001)
    }

    @Test
    fun rejectsPhysicallyInvalidHeights() {
        assertTrue(ElevationTileMath.isValidHeight(0.0))
        assertTrue(ElevationTileMath.isValidHeight(1594.0))
        assertTrue(!ElevationTileMath.isValidHeight(-11001.0))
        assertTrue(!ElevationTileMath.isValidHeight(9001.0))
    }

    @Test
    fun mapsZavizanPointToExpectedWebMercatorTileAtZoom13() {
        val coord = ElevationTileMath.tilePixelFor(lat = 44.8147, lon = 14.9819, z = 13)
        assertEquals(13, coord.z)
        assertEquals(4436, coord.x)
        assertEquals(2952, coord.y)
        assertEquals(235.89, coord.pixelX, 0.02)
        assertEquals(210.19, coord.pixelY, 0.02)
    }

    @Test
    fun clampsCoordinatesToValidTileRange() {
        val coord = ElevationTileMath.tilePixelFor(lat = 95.0, lon = 190.0, z = 13)
        val max = (1 shl 13) - 1
        assertTrue(coord.x in 0..max)
        assertTrue(coord.y in 0..max)
        assertTrue(coord.pixelX in 0.0..255.0)
        assertTrue(coord.pixelY in 0.0..255.0)
    }
}
