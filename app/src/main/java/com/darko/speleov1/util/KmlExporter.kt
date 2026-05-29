package com.darko.speleov1.util

import com.darko.speleov1.MarkedPoint
import com.darko.speleov1.SavedTrack
import com.darko.speleov1.model.SpeleoRecord
import java.util.Locale

object KmlExporter {
    fun toKml(records: List<SpeleoRecord>, documentName: String = "Speleo export"): String {
        val placemarks = records.mapNotNull { record ->
            val lat = record.location.lat
            val lon = record.location.lon
            if (lat == null || lon == null) return@mapNotNull null

            val description = buildString {
                appendLine("Izvor: ${record.source?.ifBlank { "-" } ?: "-"}")
                appendLine("Status: ${record.classification.record_status?.ifBlank { "-" } ?: "-"}")
                appendLine("Pločica: ${displayPlateNumber(record) ?: "-"}")
                appendLine("Vrsta: ${record.classification.object_type?.ifBlank { "-" } ?: "-"}")
                appendLine("Županija: ${record.location.county?.ifBlank { "-" } ?: "-"}")
                appendLine("Općina: ${record.location.municipality?.ifBlank { "-" } ?: "-"}")
                appendLine("Mjesto: ${(record.location.nearest_place ?: record.location.locality)?.ifBlank { "-" } ?: "-"}")
                appendLine("Dubina: ${record.metrics.depth_m?.toString() ?: "-"}")
                appendLine("Duljina: ${record.metrics.length_m?.toString() ?: "-"}")
                val access = record.content.access_description?.trim().orEmpty()
                if (access.isNotBlank()) appendLine("Pristup: $access")
            }.trim()

            """
            <Placemark>
                <name>${escapeXml(record.name)}</name>
                <description>${escapeXml(description)}</description>
                <ExtendedData>
                    <Data name="source"><value>${escapeXml(record.source.orEmpty())}</value></Data>
                    <Data name="record_status"><value>${escapeXml(record.classification.record_status.orEmpty())}</value></Data>
                        <Data name="plate_number"><value>${escapeXml(displayPlateNumber(record).orEmpty())}</value></Data>
                    <Data name="cave_type"><value>${escapeXml(record.classification.object_type.orEmpty())}</value></Data>
                    <Data name="county"><value>${escapeXml(record.location.county.orEmpty())}</value></Data>
                    <Data name="municipality"><value>${escapeXml(record.location.municipality.orEmpty())}</value></Data>
                    <Data name="settlement"><value>${escapeXml((record.location.nearest_place ?: record.location.locality).orEmpty())}</value></Data>
                </ExtendedData>
                <Point>
                    <coordinates>${formatLon(lon)},${formatLat(lat)},0</coordinates>
                </Point>
            </Placemark>
            """.trimIndent()
        }.joinToString("\n")

        return buildKmlDocument(documentName, placemarks)
    }

    fun toMarkedPointsKml(points: List<MarkedPoint>, documentName: String = "Custom KML točke"): String {
        val placemarks = points.map { point ->
            val description = buildString {
                appendLine("Tip: ${point.type}")
                appendLine("Opis: ${point.description.ifBlank { "-" }}")
                appendLine("HTRS96/TM X: ${String.format(Locale.US, "%.2f", point.htrsX)}")
                appendLine("HTRS96/TM Y: ${String.format(Locale.US, "%.2f", point.htrsY)}")
            }.trim()

            """
            <Placemark>
                <name>${escapeXml(point.name)}</name>
                <description>${escapeXml(description)}</description>
                <ExtendedData>
                    <Data name="type"><value>${escapeXml(point.type)}</value></Data>
                    <Data name="description"><value>${escapeXml(point.description)}</value></Data>
                    <Data name="htrs_x"><value>${String.format(Locale.US, "%.2f", point.htrsX)}</value></Data>
                    <Data name="htrs_y"><value>${String.format(Locale.US, "%.2f", point.htrsY)}</value></Data>
                </ExtendedData>
                <Point>
                    <coordinates>${formatLon(point.lon)},${formatLat(point.lat)},0</coordinates>
                </Point>
            </Placemark>
            """.trimIndent()
        }.joinToString("\n")

        return buildKmlDocument(documentName, placemarks)
    }

    fun toTrackKml(track: SavedTrack, documentName: String = track.name.ifBlank { "Track" }): String {
        if (track.points.isEmpty()) return buildKmlDocument(documentName, "")
        val coords = track.points.joinToString(" ") { tp ->
            String.format(
                java.util.Locale.US,
                "%.7f,%.7f,%.1f",
                tp.point.longitude,
                tp.point.latitude,
                tp.altitudeM ?: 0.0
            )
        }
        val description = buildString {
            appendLine("Točke: ${track.points.size}")
            if (track.description.isNotBlank()) appendLine(track.description)
        }.trim()

        val placemark = """
            <Placemark>
                <name>${escapeXml(track.name.ifBlank { "Track" })}</name>
                <description>${escapeXml(description)}</description>
                <LineString>
                    <tessellate>1</tessellate>
                    <coordinates>$coords</coordinates>
                </LineString>
            </Placemark>
        """.trimIndent()

        return buildKmlDocument(documentName, placemark)
    }

    private fun buildKmlDocument(documentName: String, placemarks: String): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        append("  <Document>\n")
        append("    <name>")
        append(escapeXml(documentName))
        append("</name>\n")
        if (placemarks.isNotBlank()) {
            append(placemarks.trim())
            append('\n')
        }
        append("  </Document>\n")
        append("</kml>\n")
    }

    private fun formatLat(value: Double): String = String.format(Locale.US, "%.7f", value)
    private fun formatLon(value: Double): String = String.format(Locale.US, "%.7f", value)

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

private fun displayPlateNumber(record: SpeleoRecord): String? {
    val directPlate = record.condition.plate_number?.takeIf { it.isNotBlank() }
    if (directPlate != null) return directPlate
    val source = (record.source ?: "").lowercase()
    if (source == "katastar" || source == "both") {
        return record.cadastre.cadastral_number?.takeIf { it.isNotBlank() }
    }
    return null
}
