package com.darko.speleov1.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ImportParserTest {
    private fun bytes(name: String): ByteArray =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("import/$name")) { "Missing fixture $name" }.readBytes()

    private fun parse(name: String) = ImportParser.parse(ByteArrayInputStream(bytes(name)), name)

    @Test
    fun parsesValidFieldFormatsWithCoordinates() {
        val fixtures = listOf("valid.kml", "valid.kmz", "valid.gpx", "valid.geojson", "valid.csv", "valid.xlsx")
        fixtures.forEach { name ->
            val layer = parse(name)
            assertTrue("$name mora vratiti barem dvije točke", layer.points.size >= 2)
            assertTrue("$name mora imati valjane latitude", layer.points.all { it.lat in -90.0..90.0 })
            assertTrue("$name mora imati valjane longitude", layer.points.all { it.lon in -180.0..180.0 })
        }
    }

    @Test
    fun keepsCroatianDiacriticsInNames() {
        val allNames = listOf("valid.kml", "valid.kmz", "valid.gpx", "valid.geojson", "valid.csv", "valid.xlsx")
            .flatMap { parse(it).points.map { p -> p.name } + parse(it).tracks.map { t -> t.name } }
            .joinToString(" ")
        listOf('Č', 'Đ', 'Š', 'Ž').forEach { letter ->
            assertTrue("Dijakritik $letter se ne smije izgubiti", allNames.contains(letter, ignoreCase = true))
        }
    }

    @Test
    fun corruptFilesReturnFailureOrEmptyLayerWithoutEscapingHarness() {
        listOf("corrupt.kml", "corrupt.kmz", "empty.csv").forEach { name ->
            val result = runCatching { parse(name) }
            assertTrue(
                "$name mora završiti kontrolirano",
                result.isFailure || (result.getOrThrow().points.isEmpty() && result.getOrThrow().tracks.isEmpty())
            )
        }
    }

    @Test
    fun preservesCoordinateEdgeCasesForReview() {
        val csv = parse("valid.csv")
        assertTrue(csv.points.any { it.lat == 0.0 && it.lon == 0.0 })
        assertTrue(csv.points.any { it.lat < 0.0 || it.lon < 0.0 || it.lat > 46.7 || it.lon > 19.5 })
        assertEquals(4, csv.points.size)
    }
}
