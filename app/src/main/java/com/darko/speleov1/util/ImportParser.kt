package com.darko.speleov1.util

import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.util.Xml
import com.darko.speleov1.ImportedLayer
import com.darko.speleov1.MarkedPoint
import com.darko.speleov1.SavedTrack
import com.darko.speleov1.TrackPoint
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.osmdroid.util.GeoPoint
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.zip.ZipInputStream

object ImportParser {
    data class Progress(
        val stage: String,
        val processed: Int,
        val points: Int,
        val tracks: Int
    )

    fun parse(
        input: InputStream,
        suggestedName: String,
        onProgress: (Progress) -> Unit = {},
        isCancelled: () -> Boolean = { false },
        tempDir: File? = null
    ): ImportedLayer {
        val buffered = if (input is BufferedInputStream) input else BufferedInputStream(input, 32 * 1024)
        buffered.mark(32 * 1024)
        val sniffBytes = ByteArray(8192)
        val count = buffered.read(sniffBytes)
        buffered.reset()
        val sniff = if (count > 0) String(sniffBytes, 0, count, Charsets.UTF_8) else ""
        val lowerName = suggestedName.lowercase(Locale.ROOT)
        val looksLikeZip = count >= 2 && sniffBytes[0] == 'P'.code.toByte() && sniffBytes[1] == 'K'.code.toByte()
        val looksLikeSQLite = count >= 16 && String(sniffBytes, 0, 16, Charsets.US_ASCII).startsWith("SQLite format 3")
        return when {
            lowerName.endsWith(".gpkg") || lowerName.endsWith(".geopackage") || looksLikeSQLite ->
                parseGeoPackage(buffered, suggestedName, onProgress, isCancelled, tempDir)
            lowerName.endsWith(".xlsx") || lowerName.endsWith(".xlsm") || looksLikeXlsx(sniffBytes, count) ->
                parseXlsx(buffered, suggestedName, onProgress, isCancelled)
            lowerName.endsWith(".kmz") ->
                parseKmz(buffered, suggestedName, onProgress, isCancelled)
            lowerName.endsWith(".tif") || lowerName.endsWith(".tiff") || looksLikeTiff(sniffBytes, count) ->
                parseGeoTiff(buffered, suggestedName, onProgress, isCancelled)
            lowerName.endsWith(".zip") || (looksLikeZip && !lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xlsm") && !lowerName.endsWith(".kmz")) ->
                parseZipBundle(buffered, suggestedName, onProgress, isCancelled)
            lowerName.endsWith(".gpx") || sniff.contains("<gpx", ignoreCase = true) -> parseGpx(buffered, suggestedName, onProgress, isCancelled)
            lowerName.endsWith(".geojson") || lowerName.endsWith(".json") || looksLikeGeoJson(sniff) -> parseGeoJson(buffered, suggestedName, onProgress, isCancelled)
            lowerName.endsWith(".csv") || looksLikeCsv(sniff) -> parseCsv(buffered, suggestedName, onProgress, isCancelled)
            else -> parseKml(buffered, suggestedName, onProgress, isCancelled)
        }
    }

    private fun parseKmz(input: InputStream, suggestedName: String, onProgress: (Progress) -> Unit, isCancelled: () -> Boolean): ImportedLayer {
        emitProgress(onProgress, "Čitam KMZ", 0, 0, 0)
        return parseKmzEntries(readZipEntries(input, isCancelled), suggestedName, onProgress, isCancelled)
    }

    private fun parseZipBundle(input: InputStream, suggestedName: String, onProgress: (Progress) -> Unit, isCancelled: () -> Boolean): ImportedLayer {
        emitProgress(onProgress, "Čitam ZIP", 0, 0, 0)
        val entries = readZipEntries(input, isCancelled)
        val lowerNames = entries.keys.map { it.lowercase(Locale.ROOT) }
        return when {
            lowerNames.any { it.endsWith(".shp") } -> parseShapefileBundleEntries(entries, suggestedName, onProgress, isCancelled)
            lowerNames.any { it.endsWith(".kml") } -> parseKmzEntries(entries, suggestedName, onProgress, isCancelled)
            else -> throw IllegalArgumentException("ZIP mora sadržavati SHP bundle ili KML/KMZ sadržaj")
        }
    }

    private fun parseGeoTiff(input: InputStream, suggestedName: String, onProgress: (Progress) -> Unit, isCancelled: () -> Boolean): ImportedLayer {
        emitProgress(onProgress, "Čitam GeoTIFF", 0, 0, 0)
        val bytes = BufferedInputStream(input).readBytes()
        ensureNotCancelled(isCancelled)
        val info = parseGeoTiffInfo(bytes) ?: throw IllegalArgumentException("GeoTIFF nema podržanu georeferencu")
        val bounds = geoTiffInfoToBounds(info)
        val (southWest, northEast) = when (info.coordinateSystem) {
            GeoTiffCoordinateSystem.WGS84 -> {
                val sw = CoordinateConverter.LatLon(lat = bounds.minY, lon = bounds.minX)
                val ne = CoordinateConverter.LatLon(lat = bounds.maxY, lon = bounds.maxX)
                sw to ne
            }
            GeoTiffCoordinateSystem.HTRS -> {
                val sw = CoordinateConverter.htrs96TmToWgs84(bounds.minX, bounds.minY)
                val ne = CoordinateConverter.htrs96TmToWgs84(bounds.maxX, bounds.maxY)
                sw to ne
            }
        }
        val minLat = minOf(southWest.lat, northEast.lat)
        val maxLat = maxOf(southWest.lat, northEast.lat)
        val minLon = minOf(southWest.lon, northEast.lon)
        val maxLon = maxOf(southWest.lon, northEast.lon)
        val centerLat = (minLat + maxLat) / 2.0
        val centerLon = (minLon + maxLon) / 2.0
        val conv = CoordinateConverter.wgs84ToHtrs96Tm(centerLat, centerLon)
        val baseName = suggestedName.substringBeforeLast('.').ifBlank { "GeoTIFF" }
        val point = MarkedPoint(
            id = "imp_geotiff_${System.nanoTime()}_1",
            name = "$baseName centar",
            type = "import",
            description = buildString {
                append("GeoTIFF raster ")
                append(info.width).append("×").append(info.height)
                info.epsg?.let { append(" • EPSG:").append(it) }
            },
            lat = centerLat,
            lon = centerLon,
            htrsX = conv.x,
            htrsY = conv.y
        )
        val track = SavedTrack(
            id = "imp_geotiff_track_${System.nanoTime()}_1",
            name = "$baseName obuhvat",
            description = "GeoTIFF footprint",
            points = listOf(
                TrackPoint(GeoPoint(maxLat, minLon), null),
                TrackPoint(GeoPoint(maxLat, maxLon), null),
                TrackPoint(GeoPoint(minLat, maxLon), null),
                TrackPoint(GeoPoint(minLat, minLon), null),
                TrackPoint(GeoPoint(maxLat, minLon), null)
            )
        )
        emitProgress(onProgress, "GeoTIFF gotov", 1, 1, 1)
        return ImportedLayer(
            id = "",
            name = baseName,
            type = "GeoTIFF",
            points = listOf(point),
            tracks = listOf(track)
        )
    }

    private fun readZipEntries(input: InputStream, isCancelled: () -> Boolean): LinkedHashMap<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                ensureNotCancelled(isCancelled)
                if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun parseKmzEntries(
        entries: Map<String, ByteArray>,
        suggestedName: String,
        onProgress: (Progress) -> Unit,
        isCancelled: () -> Boolean
    ): ImportedLayer {
        val kmlEntry = entries.entries.firstOrNull { it.key.lowercase(Locale.ROOT).endsWith(".kml") }
            ?: throw IllegalArgumentException("KMZ ne sadrži KML zapis")
        val parsed = parseKml(ByteArrayInputStream(kmlEntry.value), suggestedName.substringBeforeLast('.'), onProgress, isCancelled)
        return parsed.copy(type = "KMZ", name = suggestedName.substringBeforeLast('.'))
    }

    private fun parseShapefileBundleEntries(
        entries: Map<String, ByteArray>,
        suggestedName: String,
        onProgress: (Progress) -> Unit,
        isCancelled: () -> Boolean
    ): ImportedLayer {
        emitProgress(onProgress, "Čitam SHP ZIP", 0, 0, 0)
        val shpEntry = entries.entries.firstOrNull { it.key.lowercase(Locale.ROOT).endsWith(".shp") }
            ?: throw IllegalArgumentException("ZIP ne sadrži .shp datoteku")
        val basePath = shpEntry.key.substringBeforeLast('.')
        val dbfBytes = findZipEntryByBase(entries, basePath, ".dbf")
        val prjText = findZipEntryByBase(entries, basePath, ".prj")?.toString(Charsets.UTF_8)
        val dbf = dbfBytes?.let(::parseDbfTable) ?: DbfTable(emptyList(), emptyList())
        val projection = inferShapefileProjection(prjText, shpEntry.value)
        val normalizedFields = dbf.fields.map(::normalizeHeader)
        val nameIdx = normalizedFields.indexOfFirst(::looksLikeNameHeader)
        val descIdx = normalizedFields.indexOfFirst(::looksLikeDescriptionHeader)

        val points = ArrayList<MarkedPoint>()
        val tracks = ArrayList<SavedTrack>()
        var offset = 100
        var processed = 0
        while (offset + 8 <= shpEntry.value.size) {
            ensureNotCancelled(isCancelled)
            val contentLengthBytes = readIntBE(shpEntry.value, offset + 4) * 2
            val recStart = offset + 8
            val recEnd = (recStart + contentLengthBytes).coerceAtMost(shpEntry.value.size)
            if (recStart + 4 > recEnd) break
            val row = dbf.rows.getOrNull(processed)
            val rawName = if (nameIdx >= 0) row?.getOrNull(nameIdx).orEmpty().trim() else ""
            val rawDesc = if (descIdx >= 0) row?.getOrNull(descIdx).orEmpty().trim() else ""
            val defaultNamePrefix = suggestedName.substringBeforeLast('.').ifBlank { "SHP" }
            when (readIntLE(shpEntry.value, recStart)) {
                0 -> Unit
                1, 11, 21 -> {
                    val x = readDoubleLE(shpEntry.value, recStart + 4)
                    val y = readDoubleLE(shpEntry.value, recStart + 12)
                    val latLon = shapefileCoordsToLatLon(x, y, projection)
                    val conv = CoordinateConverter.wgs84ToHtrs96Tm(latLon.lat, latLon.lon)
                    points += MarkedPoint(
                        id = "imp_shp_pt_${'$'}{System.nanoTime()}_${'$'}{points.size + 1}",
                        name = rawName.ifBlank { "${'$'}defaultNamePrefix točka ${'$'}{points.size + 1}" },
                        type = "import",
                        description = rawDesc.ifBlank { "SHP import" },
                        lat = latLon.lat,
                        lon = latLon.lon,
                        htrsX = conv.x,
                        htrsY = conv.y
                    )
                }
                8, 18, 28 -> {
                    val numPoints = readIntLE(shpEntry.value, recStart + 36).coerceAtLeast(0)
                    var pointOffset = recStart + 40
                    repeat(numPoints) { idx ->
                        if (pointOffset + 16 <= recEnd) {
                            val x = readDoubleLE(shpEntry.value, pointOffset)
                            val y = readDoubleLE(shpEntry.value, pointOffset + 8)
                            val latLon = shapefileCoordsToLatLon(x, y, projection)
                            val conv = CoordinateConverter.wgs84ToHtrs96Tm(latLon.lat, latLon.lon)
                            points += MarkedPoint(
                                id = "imp_shp_mpt_${'$'}{System.nanoTime()}_${'$'}{points.size + 1}",
                                name = rawName.ifBlank { "${'$'}defaultNamePrefix točka ${'$'}{points.size + 1}" } + if (numPoints > 1) " ${'$'}{idx + 1}" else "",
                                type = "import",
                                description = rawDesc.ifBlank { "SHP import" },
                                lat = latLon.lat,
                                lon = latLon.lon,
                                htrsX = conv.x,
                                htrsY = conv.y
                            )
                        }
                        pointOffset += 16
                    }
                }
                3, 5, 13, 15, 23, 25 -> {
                    val numParts = readIntLE(shpEntry.value, recStart + 36).coerceAtLeast(0)
                    val numPoints = readIntLE(shpEntry.value, recStart + 40).coerceAtLeast(0)
                    val partsOffset = recStart + 44
                    val pointsOffset = partsOffset + (numParts * 4)
                    if (pointsOffset <= recEnd) {
                        val parts = IntArray(numParts + 1)
                        for (i in 0 until numParts) parts[i] = readIntLE(shpEntry.value, partsOffset + (i * 4)).coerceAtLeast(0)
                        parts[numParts] = numPoints
                        for (partIndex in 0 until numParts) {
                            val start = parts[partIndex].coerceIn(0, numPoints)
                            val end = parts[partIndex + 1].coerceIn(start, numPoints)
                            val pts = ArrayList<TrackPoint>(end - start)
                            for (i in start until end) {
                                val ptOffset = pointsOffset + (i * 16)
                                if (ptOffset + 16 > recEnd) break
                                val x = readDoubleLE(shpEntry.value, ptOffset)
                                val y = readDoubleLE(shpEntry.value, ptOffset + 8)
                                val latLon = shapefileCoordsToLatLon(x, y, projection)
                                pts += TrackPoint(GeoPoint(latLon.lat, latLon.lon), null)
                            }
                            val simplified = simplifyTrackPoints(pts)
                            if (simplified.isNotEmpty()) {
                                tracks += SavedTrack(
                                    id = "imp_shp_track_${'$'}{System.nanoTime()}_${'$'}{tracks.size + 1}",
                                    name = rawName.ifBlank { "${'$'}defaultNamePrefix ${'$'}{tracks.size + 1}" } + if (numParts > 1) " / ${'$'}{partIndex + 1}" else "",
                                    description = rawDesc.ifBlank { "SHP import" },
                                    points = simplified
                                )
                            }
                        }
                    }
                }
            }
            processed += 1
            if (processed % 24 == 0) emitProgress(onProgress, "Obrada SHP", processed, points.size, tracks.size)
            offset = recEnd
        }

        emitProgress(onProgress, "SHP gotov", processed, points.size, tracks.size)
        return ImportedLayer(
            id = "",
            name = suggestedName.substringBeforeLast('.'),
            type = "SHP ZIP",
            points = points,
            tracks = tracks
        )
    }

    private fun parseKml(input: InputStream, suggestedName: String, onProgress: (Progress) -> Unit, isCancelled: () -> Boolean): ImportedLayer {
        val parser = Xml.newPullParser()
        parser.setInput(input, null)

        val points = ArrayList<MarkedPoint>(256)
        val tracks = ArrayList<SavedTrack>(32)

        var event = parser.eventType
        var insidePlacemark = false
        var currentName = ""
        var currentGeometry: String? = null
        var pointCoordinates: String? = null
        var lineCoordinates: String? = null
        var polygonCoordinates: String? = null
        var placemarkDescription = ""
        var pointIndex = 0
        var lineIndex = 0
        var placemarkCount = 0
        var folderName = ""

        emitProgress(onProgress, "Čitam KML", 0, 0, 0)

        while (event != XmlPullParser.END_DOCUMENT) {
            ensureNotCancelled(isCancelled)
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase(Locale.ROOT)) {
                    "placemark" -> {
                        insidePlacemark = true
                        currentName = ""
                        currentGeometry = null
                        pointCoordinates = null
                        lineCoordinates = null
                        polygonCoordinates = null
                        placemarkDescription = ""
                    }
                    "point" -> currentGeometry = "point"
                    "linestring" -> currentGeometry = "line"
                    "polygon", "outerboundaryis", "linearring" -> if (currentGeometry == null) currentGeometry = "polygon"
                    "folder" -> { /* entering folder — reset folderName on nested folder */ }
                    "name" -> when {
                        insidePlacemark && currentName.isBlank() ->
                            currentName = xmlUnescape(parser.nextText()).trim()
                        !insidePlacemark && folderName.isBlank() ->
                            folderName = xmlUnescape(parser.nextText()).trim()
                        else -> parser.nextText() // consume and discard
                    }
                    "description" -> if (insidePlacemark && placemarkDescription.isBlank()) {
                        placemarkDescription = normalizeKmlDescription(parser.nextText())
                    }
                    "coordinates" -> if (insidePlacemark) {
                        val coordsText = parser.nextText().trim()
                        if (coordsText.isNotBlank()) {
                            when (currentGeometry) {
                                "point" -> pointCoordinates = coordsText
                                "line" -> lineCoordinates = coordsText
                                "polygon" -> polygonCoordinates = coordsText
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> when (parser.name.lowercase(Locale.ROOT)) {
                    "point", "linestring", "polygon", "outerboundaryis", "linearring" -> currentGeometry = null
                    "placemark" -> {
                        pointCoordinates?.let { coordsText ->
                            val parts = coordsText.split(',')
                            val lon = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
                            val lat = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
                            val alt = parts.getOrNull(2)?.trim()?.toDoubleOrNull()
                            if (lat != null && lon != null) {
                                val conv = CoordinateConverter.wgs84ToHtrs96Tm(lat, lon)
                                pointIndex += 1
                                val baseDesc = placemarkDescription.ifBlank { "KML import" }
                                val desc = if (alt != null && alt != 0.0)
                                    "$baseDesc\nNadmorska visina: ${String.format(java.util.Locale.US, "%.1f", alt)} m"
                                else baseDesc
                                points += MarkedPoint(
                                    id = "imp_pt_${System.nanoTime()}_$pointIndex",
                                    name = currentName.ifBlank { "KML točka $pointIndex" },
                                    type = "import",
                                    description = desc,
                                    lat = lat,
                                    lon = lon,
                                    htrsX = conv.x,
                                    htrsY = conv.y
                                )
                            }
                        }
                        listOfNotNull(
                            lineCoordinates?.let { it to false },
                            polygonCoordinates?.let { it to true }
                        ).forEachIndexed { geomIdx, (coordsText, isPolygon) ->
                            val pts = parseCoordinateSequence(coordsText, isCancelled)
                            if (pts.isNotEmpty()) {
                                lineIndex += 1
                                val suffix = if (geomIdx > 0) " (${geomIdx + 1})" else ""
                                tracks += SavedTrack(
                                    id = "imp_track_${System.nanoTime()}_$lineIndex",
                                    name = currentName.ifBlank { if (isPolygon) "KML poligon $lineIndex" else "KML linija $lineIndex" } + suffix,
                                    description = placemarkDescription.ifBlank { "KML import" },
                                    points = pts
                                )
                            }
                        }
                        placemarkCount += 1
                        if (placemarkCount % 24 == 0) {
                            emitProgress(onProgress, "Obrada KML", placemarkCount, points.size, tracks.size)
                        }
                        insidePlacemark = false
                    }
                }
            }
            event = parser.next()
        }
        emitProgress(onProgress, "KML gotov", placemarkCount, points.size, tracks.size)

        return ImportedLayer(
            id = "",
            name = folderName.ifBlank { suggestedName.substringBeforeLast('.') },
            type = "KML",
            points = points,
            tracks = tracks
        )
    }

    private fun parseCoordinateSequence(raw: String, isCancelled: () -> Boolean = { false }): List<TrackPoint> {
        val tokenRegex = Regex("""\s+""")
        val points = ArrayList<TrackPoint>(512)
        var tokenCount = 0
        raw.lineSequence()
            .flatMap { tokenRegex.splitToSequence(it.trim()) }
            .forEach { token ->
                tokenCount += 1
                if (tokenCount % 256 == 0) ensureNotCancelled(isCancelled)
                val parts = token.split(',')
                val lon = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return@forEach
                val lat = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return@forEach
                val alt = parts.getOrNull(2)?.trim()?.toDoubleOrNull()
                points += TrackPoint(GeoPoint(lat, lon), alt)
            }
        return simplifyTrackPoints(points)
    }

    private fun parseGpx(input: InputStream, suggestedName: String, onProgress: (Progress) -> Unit, isCancelled: () -> Boolean): ImportedLayer {
        val parser = Xml.newPullParser()
        parser.setInput(input, null)
        val points = ArrayList<MarkedPoint>(128)
        val tracks = ArrayList<SavedTrack>(16)
        var event = parser.eventType
        var currentTrackName = ""
        var currentTrackPoints = mutableListOf<TrackPoint>()
        var insideTrack = false
        var insideTrackPoint = false
        var pendingTrackPointElevation: Double? = null
        var pendingWptLat: Double? = null
        var pendingWptLon: Double? = null
        var pendingWptName = ""
        var processed = 0
        emitProgress(onProgress, "Čitam GPX", 0, 0, 0)
        while (event != XmlPullParser.END_DOCUMENT) {
            ensureNotCancelled(isCancelled)
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase(Locale.ROOT)) {
                    "wpt" -> {
                        pendingWptLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        pendingWptLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        pendingWptName = ""
                    }
                    "trk" -> {
                        insideTrack = true
                        currentTrackName = ""
                        currentTrackPoints = mutableListOf()
                    }
                    "name" -> {
                        val t = parser.nextText()
                        if (insideTrack) currentTrackName = t else pendingWptName = t
                    }
                    "trkpt" -> {
                        insideTrackPoint = true
                        pendingTrackPointElevation = null
                        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            currentTrackPoints.add(TrackPoint(GeoPoint(lat, lon), null))
                            processed += 1
                            if (processed % 128 == 0) emitProgress(onProgress, "Obrada GPX tracka", processed, points.size, tracks.size)
                        }
                    }
                    "ele" -> if (insideTrackPoint) {
                        pendingTrackPointElevation = parser.nextText().toDoubleOrNull()
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name.lowercase(Locale.ROOT)) {
                    "trkpt" -> {
                        if (pendingTrackPointElevation != null && currentTrackPoints.isNotEmpty()) {
                            val last = currentTrackPoints.last()
                            currentTrackPoints[currentTrackPoints.lastIndex] = last.copy(altitudeM = pendingTrackPointElevation)
                        }
                        insideTrackPoint = false
                        pendingTrackPointElevation = null
                    }
                    "wpt" -> {
                        val lat = pendingWptLat
                        val lon = pendingWptLon
                        if (lat != null && lon != null) {
                            val conv = CoordinateConverter.wgs84ToHtrs96Tm(lat, lon)
                            processed += 1
                            if (processed % 32 == 0) emitProgress(onProgress, "Obrada GPX točaka", processed, points.size + 1, tracks.size)
                            points += MarkedPoint(
                                id = "imp_wpt_${System.nanoTime()}",
                                name = pendingWptName.ifBlank { "GPX točka" },
                                type = "import",
                                description = "GPX import",
                                lat = lat,
                                lon = lon,
                                htrsX = conv.x,
                                htrsY = conv.y
                            )
                        }
                        pendingWptLat = null
                        pendingWptLon = null
                        pendingWptName = ""
                    }
                    "trk" -> {
                        if (currentTrackPoints.isNotEmpty()) {
                            tracks += SavedTrack(
                                id = "imp_trk_${System.nanoTime()}",
                                name = currentTrackName.ifBlank { "GPX track" },
                                description = "GPX import",
                                points = simplifyTrackPoints(currentTrackPoints)
                            )
                        }
                        emitProgress(onProgress, "GPX track spremljen", processed, points.size, tracks.size + 1)
                        insideTrack = false
                    }
                }
            }
            event = parser.next()
        }
        emitProgress(onProgress, "GPX gotov", processed, points.size, tracks.size)
        return ImportedLayer(
            id = "",
            name = suggestedName.substringBeforeLast('.'),
            type = "GPX",
            points = points,
            tracks = tracks
        )
    }

    private fun parseGeoJson(input: InputStream, suggestedName: String, onProgress: (Progress) -> Unit, isCancelled: () -> Boolean): ImportedLayer {
        emitProgress(onProgress, "Čitam GeoJSON", 0, 0, 0)
        val text = input.bufferedReader().use { it.readText() }
        val root = JSONTokener(text).nextValue()
        val points = ArrayList<MarkedPoint>(256)
        val tracks = ArrayList<SavedTrack>(64)
        var processed = 0

        fun report(stage: String) {
            emitProgress(onProgress, stage, processed, points.size, tracks.size)
        }

        fun parseGeometry(geometry: JSONObject?, name: String, description: String) {
            ensureNotCancelled(isCancelled)
            if (geometry == null) return
            when (geometry.optString("type")) {
                "Point" -> {
                    val coords = geometry.optJSONArray("coordinates") ?: return
                    addPoint(coords, name, description, points)
                    processed += 1
                }
                "MultiPoint" -> {
                    val coords = geometry.optJSONArray("coordinates") ?: return
                    for (i in 0 until coords.length()) {
                        ensureNotCancelled(isCancelled)
                        addPoint(coords.optJSONArray(i), name.ifBlank { "GeoJSON točka ${points.size + 1}" }, description, points)
                        processed += 1
                    }
                }
                "LineString" -> {
                    val coords = geometry.optJSONArray("coordinates") ?: return
                    addTrack(coords, name, description, tracks)
                    processed += 1
                }
                "MultiLineString" -> {
                    val lines = geometry.optJSONArray("coordinates") ?: return
                    for (i in 0 until lines.length()) {
                        ensureNotCancelled(isCancelled)
                        addTrack(lines.optJSONArray(i), lineName(name, i), description, tracks)
                        processed += 1
                    }
                }
                "Polygon" -> {
                    val rings = geometry.optJSONArray("coordinates") ?: return
                    addTrack(rings.optJSONArray(0), name.ifBlank { "GeoJSON poligon ${tracks.size + 1}" }, description, tracks)
                    processed += 1
                }
                "MultiPolygon" -> {
                    val polygons = geometry.optJSONArray("coordinates") ?: return
                    for (i in 0 until polygons.length()) {
                        ensureNotCancelled(isCancelled)
                        val rings = polygons.optJSONArray(i)
                        addTrack(rings?.optJSONArray(0), lineName(name.ifBlank { "GeoJSON poligon" }, i), description, tracks)
                        processed += 1
                    }
                }
                "GeometryCollection" -> {
                    val geometries = geometry.optJSONArray("geometries") ?: return
                    for (i in 0 until geometries.length()) {
                        parseGeometry(geometries.optJSONObject(i), lineName(name, i), description)
                    }
                }
            }
            if (processed % 24 == 0) report("Obrada GeoJSON")
        }

        fun parseNode(node: Any?, fallbackName: String = "") {
            ensureNotCancelled(isCancelled)
            when (node) {
                is JSONObject -> {
                    when (node.optString("type")) {
                        "FeatureCollection" -> {
                            val features = node.optJSONArray("features") ?: return
                            for (i in 0 until features.length()) parseNode(features.optJSONObject(i), fallbackName)
                        }
                        "Feature" -> {
                            val properties = node.optJSONObject("properties")
                            val name = bestName(properties, node, fallbackName)
                            val description = bestDescription(properties)
                            parseGeometry(node.optJSONObject("geometry"), name, description)
                        }
                        else -> parseGeometry(node, fallbackName, "GeoJSON import")
                    }
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) parseNode(node.opt(i), fallbackName)
                }
            }
        }

        parseNode(root, suggestedName.substringBeforeLast('.'))
        report("GeoJSON gotov")
        return ImportedLayer(
            id = "",
            name = suggestedName.substringBeforeLast('.'),
            type = "GeoJSON",
            points = points,
            tracks = tracks
        )
    }

    private fun parseCsv(input: InputStream, suggestedName: String, onProgress: (Progress) -> Unit, isCancelled: () -> Boolean): ImportedLayer {
        emitProgress(onProgress, "Čitam CSV", 0, 0, 0)
        val lines = input.bufferedReader().readLines().filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "CSV je prazan" }
        val delimiter = detectDelimiter(lines.first())
        val header = parseDelimitedLine(lines.first(), delimiter)
        require(header.isNotEmpty()) { "CSV nema header" }
        val normalizedHeader = header.map { normalizeHeader(it) }
        var latIdx = normalizedHeader.indexOfFirst(::looksLikeLatHeader)
        var lonIdx = normalizedHeader.indexOfFirst(::looksLikeLonHeader)
        var xIdx = normalizedHeader.indexOfFirst(::looksLikeProjectedXHeader)
        var yIdx = normalizedHeader.indexOfFirst(::looksLikeProjectedYHeader)
        val genericXIdx = normalizedHeader.indexOfFirst { it == "x" }
        val genericYIdx = normalizedHeader.indexOfFirst { it == "y" }

        if ((latIdx < 0 || lonIdx < 0) && (xIdx < 0 || yIdx < 0) && genericXIdx >= 0 && genericYIdx >= 0) {
            when (inferGenericXyMode(lines.drop(1), delimiter, genericXIdx, genericYIdx)) {
                CoordinateMode.WGS84 -> {
                    lonIdx = genericXIdx
                    latIdx = genericYIdx
                }
                CoordinateMode.HTRS -> {
                    xIdx = genericXIdx
                    yIdx = genericYIdx
                }
                else -> Unit
            }
        }

        require((latIdx >= 0 && lonIdx >= 0) || (xIdx >= 0 && yIdx >= 0)) { "CSV mora imati lat/lon ili HTRS X/Y kolone" }
        val nameIdx = normalizedHeader.indexOfFirst(::looksLikeNameHeader)
        val descIdx = normalizedHeader.indexOfFirst(::looksLikeDescriptionHeader)

        val points = ArrayList<MarkedPoint>(lines.size)
        var processed = 0
        for (line in lines.drop(1)) {
            ensureNotCancelled(isCancelled)
            val row = parseDelimitedLine(line, delimiter)
            if (row.isEmpty()) continue
            val name = row.getOrNull(nameIdx)?.trim().orEmpty().ifBlank { "CSV točka ${points.size + 1}" }
            val desc = row.getOrNull(descIdx)?.trim().orEmpty().ifBlank { "CSV import" }

            val latLon = if (latIdx >= 0 && lonIdx >= 0) {
                val lat = row.getOrNull(latIdx)?.parseFlexibleDouble()
                val lon = row.getOrNull(lonIdx)?.parseFlexibleDouble()
                if (lat == null || lon == null) null else CoordinateConverter.LatLon(lat, lon)
            } else {
                val x = row.getOrNull(xIdx)?.parseFlexibleDouble()
                val y = row.getOrNull(yIdx)?.parseFlexibleDouble()
                if (x == null || y == null) null else CoordinateConverter.htrs96TmToWgs84(x, y)
            } ?: continue

            val conv = CoordinateConverter.wgs84ToHtrs96Tm(latLon.lat, latLon.lon)
            points += MarkedPoint(
                id = "imp_csv_${System.nanoTime()}_${points.size + 1}",
                name = name,
                type = "import",
                description = desc,
                lat = latLon.lat,
                lon = latLon.lon,
                htrsX = conv.x,
                htrsY = conv.y
            )
            processed += 1
            if (processed % 48 == 0) emitProgress(onProgress, "Obrada CSV", processed, points.size, 0)
        }
        emitProgress(onProgress, "CSV gotov", processed, points.size, 0)
        return ImportedLayer(
            id = "",
            name = suggestedName.substringBeforeLast('.'),
            type = "CSV",
            points = points,
            tracks = emptyList()
        )
    }

    private fun parseXlsx(input: InputStream, suggestedName: String, onProgress: (Progress) -> Unit, isCancelled: () -> Boolean): ImportedLayer {
        emitProgress(onProgress, "Čitam XLSX", 0, 0, 0)
        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                ensureNotCancelled(isCancelled)
                if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        require(entries.isNotEmpty()) { "XLSX je prazan" }

        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val workbookRels = parseWorkbookRelationships(entries["xl/_rels/workbook.xml.rels"])
        val firstSheetPath = parseFirstWorksheetPath(entries["xl/workbook.xml"], workbookRels)
            ?: entries.keys.firstOrNull { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }
            ?: throw IllegalArgumentException("XLSX nema worksheet")
        val rows = parseWorksheetRows(entries[firstSheetPath] ?: throw IllegalArgumentException("Ne mogu otvoriti prvi worksheet"), sharedStrings, isCancelled)
        require(rows.isNotEmpty()) { "XLSX nema podataka" }
        val header = rows.first().map { it.trim() }
        val dataRows = rows.drop(1).map { row ->
            if (row.size < header.size) row + List(header.size - row.size) { "" } else row
        }.filter { row -> row.any { it.isNotBlank() } }
        return parseTabularRows(header, dataRows, suggestedName.substringBeforeLast('.'), "XLSX", onProgress, isCancelled)
    }

    private fun parseGeoPackage(
        input: InputStream,
        suggestedName: String,
        onProgress: (Progress) -> Unit,
        isCancelled: () -> Boolean,
        tempDir: File?
    ): ImportedLayer {
        require(tempDir != null) { "GeoPackage traži privremeni storage" }
        emitProgress(onProgress, "Čitam GPKG", 0, 0, 0)
        val tempFile = File.createTempFile("sov_import_", ".gpkg", tempDir)
        try {
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            val db = SQLiteDatabase.openDatabase(tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            db.use { database ->
                val tables = queryGeometryTables(database)
                require(tables.isNotEmpty()) { "GPKG nema geometrijskih layera" }
                val points = ArrayList<MarkedPoint>()
                val tracks = ArrayList<SavedTrack>()
                var processed = 0
                tables.forEachIndexed { tableIndex, spec ->
                    ensureNotCancelled(isCancelled)
                    val columns = tableColumns(database, spec.tableName)
                    val nameColumn = columns.firstOrNull { looksLikeNameHeader(normalizeHeader(it)) }
                    val descColumn = columns.firstOrNull { looksLikeDescriptionHeader(normalizeHeader(it)) }
                    val sql = buildString {
                        append("SELECT ")
                        append(quoteIdentifier(spec.geometryColumn))
                        if (nameColumn != null) append(", ").append(quoteIdentifier(nameColumn))
                        if (descColumn != null && descColumn != nameColumn) append(", ").append(quoteIdentifier(descColumn))
                        append(" FROM ").append(quoteIdentifier(spec.tableName))
                    }
                    database.rawQuery(sql, null).use { cursor ->
                        val geomIdx = 0
                        val nameIdx = if (nameColumn != null) 1 else -1
                        val descIdx = when {
                            descColumn == null -> -1
                            nameColumn == null -> 1
                            descColumn == nameColumn -> 1
                            else -> 2
                        }
                        while (cursor.moveToNext()) {
                            ensureNotCancelled(isCancelled)
                            val blob = cursor.getBlob(geomIdx) ?: continue
                            val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                            val desc = if (descIdx >= 0) cursor.getString(descIdx).orEmpty() else ""
                            val geoms = parseGeoPackageGeometry(blob)
                            geoms.forEach { geom ->
                                when (geom) {
                                    is ParsedGeometry.PointGeom -> {
                                        val conv = CoordinateConverter.wgs84ToHtrs96Tm(geom.lat, geom.lon)
                                        points += MarkedPoint(
                                            id = "imp_gpkg_pt_${System.nanoTime()}_${points.size + 1}",
                                            name = name.ifBlank { "${spec.tableName} točka ${points.size + 1}" },
                                            type = "import",
                                            description = desc.ifBlank { "GPKG ${spec.tableName}" },
                                            lat = geom.lat,
                                            lon = geom.lon,
                                            htrsX = conv.x,
                                            htrsY = conv.y
                                        )
                                    }
                                    is ParsedGeometry.LineGeom -> {
                                        val pts = simplifyTrackPoints(geom.points.map { TrackPoint(GeoPoint(it.first, it.second), null) })
                                        if (pts.isNotEmpty()) {
                                            tracks += SavedTrack(
                                                id = "imp_gpkg_track_${System.nanoTime()}_${tracks.size + 1}",
                                                name = name.ifBlank { "${spec.tableName} ${tracks.size + 1}" },
                                                description = desc.ifBlank { "GPKG ${spec.tableName}" },
                                                points = pts
                                            )
                                        }
                                    }
                                }
                            }
                            processed += 1
                            if (processed % 32 == 0) emitProgress(onProgress, "Obrada GPKG ${tableIndex + 1}/${tables.size}", processed, points.size, tracks.size)
                        }
                    }
                }
                emitProgress(onProgress, "GPKG gotov", processed, points.size, tracks.size)
                return ImportedLayer(
                    id = "",
                    name = suggestedName.substringBeforeLast('.'),
                    type = "GPKG",
                    points = points,
                    tracks = tracks
                )
            }
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    private data class GeoPackageLayerSpec(val tableName: String, val geometryColumn: String)

    private sealed class ParsedGeometry {
        data class PointGeom(val lat: Double, val lon: Double) : ParsedGeometry()
        data class LineGeom(val points: List<Pair<Double, Double>>) : ParsedGeometry()
    }

    private fun parseTabularRows(
        header: List<String>,
        rows: List<List<String>>,
        baseName: String,
        layerType: String,
        onProgress: (Progress) -> Unit,
        isCancelled: () -> Boolean
    ): ImportedLayer {
        require(header.isNotEmpty()) { "Tablica nema header" }
        val normalizedHeader = header.map { normalizeHeader(it) }
        var latIdx = normalizedHeader.indexOfFirst(::looksLikeLatHeader)
        var lonIdx = normalizedHeader.indexOfFirst(::looksLikeLonHeader)
        var xIdx = normalizedHeader.indexOfFirst(::looksLikeProjectedXHeader)
        var yIdx = normalizedHeader.indexOfFirst(::looksLikeProjectedYHeader)
        val genericXIdx = normalizedHeader.indexOfFirst { it == "x" }
        val genericYIdx = normalizedHeader.indexOfFirst { it == "y" }
        if ((latIdx < 0 || lonIdx < 0) && (xIdx < 0 || yIdx < 0) && genericXIdx >= 0 && genericYIdx >= 0) {
            when (inferGenericXyModeFromValues(rows, genericXIdx, genericYIdx)) {
                CoordinateMode.WGS84 -> { lonIdx = genericXIdx; latIdx = genericYIdx }
                CoordinateMode.HTRS -> { xIdx = genericXIdx; yIdx = genericYIdx }
                else -> Unit
            }
        }
        require((latIdx >= 0 && lonIdx >= 0) || (xIdx >= 0 && yIdx >= 0)) { "$layerType mora imati lat/lon ili HTRS X/Y kolone" }
        val nameIdx = normalizedHeader.indexOfFirst(::looksLikeNameHeader)
        val descIdx = normalizedHeader.indexOfFirst(::looksLikeDescriptionHeader)
        val points = ArrayList<MarkedPoint>(rows.size)
        var processed = 0
        rows.forEach { row ->
            ensureNotCancelled(isCancelled)
            val name = row.getOrNull(nameIdx)?.trim().orEmpty().ifBlank { "$layerType točka ${points.size + 1}" }
            val desc = row.getOrNull(descIdx)?.trim().orEmpty().ifBlank { "$layerType import" }
            val latLon = if (latIdx >= 0 && lonIdx >= 0) {
                val lat = row.getOrNull(latIdx)?.parseFlexibleDouble()
                val lon = row.getOrNull(lonIdx)?.parseFlexibleDouble()
                if (lat == null || lon == null) null else CoordinateConverter.LatLon(lat, lon)
            } else {
                val x = row.getOrNull(xIdx)?.parseFlexibleDouble()
                val y = row.getOrNull(yIdx)?.parseFlexibleDouble()
                if (x == null || y == null) null else CoordinateConverter.htrs96TmToWgs84(x, y)
            } ?: return@forEach
            val conv = CoordinateConverter.wgs84ToHtrs96Tm(latLon.lat, latLon.lon)
            points += MarkedPoint(
                id = "imp_${layerType.lowercase(Locale.ROOT)}_${System.nanoTime()}_${points.size + 1}",
                name = name,
                type = "import",
                description = desc,
                lat = latLon.lat,
                lon = latLon.lon,
                htrsX = conv.x,
                htrsY = conv.y
            )
            processed += 1
            if (processed % 48 == 0) emitProgress(onProgress, "Obrada $layerType", processed, points.size, 0)
        }
        emitProgress(onProgress, "$layerType gotov", processed, points.size, 0)
        return ImportedLayer(id = "", name = baseName, type = layerType, points = points, tracks = emptyList())
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), null)
        val values = ArrayList<String>()
        var event = parser.eventType
        var insideSi = false
        val current = StringBuilder()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (parser.name == "si") {
                    insideSi = true
                    current.setLength(0)
                } else if (insideSi && parser.name == "t") {
                    current.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> if (parser.name == "si") {
                    values += current.toString()
                    insideSi = false
                }
            }
            event = parser.next()
        }
        return values
    }

    private fun parseWorkbookRelationships(bytes: ByteArray?): Map<String, String> {
        if (bytes == null) return emptyMap()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), null)
        val mapping = LinkedHashMap<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.endsWith("Relationship")) {
                val id = parser.getAttributeValue(null, "Id") ?: parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "Id")
                val target = parser.getAttributeValue(null, "Target") ?: parser.getAttributeValue("http://schemas.openxmlformats.org/package/2006/relationships", "Target")
                if (!id.isNullOrBlank() && !target.isNullOrBlank()) {
                    val normalized = if (target.startsWith("/")) target.removePrefix("/") else "xl/" + target.removePrefix("/")
                    mapping[id] = normalized
                }
            }
            event = parser.next()
        }
        return mapping
    }

    private fun parseFirstWorksheetPath(bytes: ByteArray?, rels: Map<String, String>): String? {
        if (bytes == null) return null
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), null)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.endsWith("sheet")) {
                val relId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                    ?: parser.getAttributeValue(null, "r:id")
                if (!relId.isNullOrBlank()) return rels[relId]
            }
            event = parser.next()
        }
        return null
    }

    private fun parseWorksheetRows(bytes: ByteArray, sharedStrings: List<String>, isCancelled: () -> Boolean): List<List<String>> {
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), null)
        val rows = ArrayList<List<String>>()
        var event = parser.eventType
        var currentRow: MutableMap<Int, String>? = null
        var currentCellRef: String? = null
        var currentCellType: String? = null
        var currentValue: String? = null
        while (event != XmlPullParser.END_DOCUMENT) {
            ensureNotCancelled(isCancelled)
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> currentRow = LinkedHashMap()
                    "c" -> {
                        currentCellRef = parser.getAttributeValue(null, "r")
                        currentCellType = parser.getAttributeValue(null, "t")
                        currentValue = ""
                    }
                    "v", "t" -> if (currentCellRef != null) currentValue = parser.nextText()
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "c" -> {
                        val row = currentRow
                        val ref = currentCellRef
                        if (row != null && ref != null) {
                            val col = excelColumnIndex(ref)
                            val raw = currentValue.orEmpty()
                            val value = when (currentCellType) {
                                "s" -> sharedStrings.getOrNull(raw.toIntOrNull() ?: -1).orEmpty()
                                "inlineStr" -> raw
                                else -> raw
                            }
                            row[col] = value
                        }
                        currentCellRef = null
                        currentCellType = null
                        currentValue = null
                    }
                    "row" -> {
                        val row = currentRow.orEmpty()
                        if (row.isNotEmpty()) {
                            val maxCol = row.keys.maxOrNull() ?: -1
                            rows.add((0..maxCol).map { idx -> row[idx].orEmpty() })
                        } else {
                            rows.add(emptyList())
                        }
                        currentRow = null
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun excelColumnIndex(cellRef: String): Int {
        var result = 0
        for (ch in cellRef) {
            if (!ch.isLetter()) break
            result = result * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return (result - 1).coerceAtLeast(0)
    }

    private fun looksLikeXlsx(sniffBytes: ByteArray, count: Int): Boolean {
        if (count < 2 || sniffBytes[0] != 'P'.code.toByte() || sniffBytes[1] != 'K'.code.toByte()) return false
        val sample = String(sniffBytes, 0, count.coerceAtMost(4096), Charsets.ISO_8859_1)
        return sample.contains("xl/") || sample.contains("[Content_Types].xml")
    }

    private fun inferGenericXyModeFromValues(rows: List<List<String>>, xIdx: Int, yIdx: Int): CoordinateMode {
        var wgsHits = 0
        var projectedHits = 0
        rows.take(12).forEach { row ->
            val x = row.getOrNull(xIdx)?.parseFlexibleDouble() ?: return@forEach
            val y = row.getOrNull(yIdx)?.parseFlexibleDouble() ?: return@forEach
            when {
                kotlin.math.abs(x) <= 180.0 && kotlin.math.abs(y) <= 90.0 -> wgsHits += 1
                kotlin.math.abs(x) > 1000.0 || kotlin.math.abs(y) > 1000.0 -> projectedHits += 1
            }
        }
        return when {
            projectedHits > 0 && projectedHits >= wgsHits -> CoordinateMode.HTRS
            wgsHits > 0 -> CoordinateMode.WGS84
            else -> CoordinateMode.UNKNOWN
        }
    }

    private fun quoteIdentifier(name: String): String = "\"" + name.replace("\"", "\"\"") + "\""

    private fun queryGeometryTables(db: SQLiteDatabase): List<GeoPackageLayerSpec> {
        val specs = ArrayList<GeoPackageLayerSpec>()
        db.rawQuery("SELECT table_name, column_name FROM gpkg_geometry_columns", null).use { cursor ->
            while (cursor.moveToNext()) {
                val table = cursor.getString(0).orEmpty()
                val geom = cursor.getString(1).orEmpty()
                if (table.isNotBlank() && geom.isNotBlank()) specs += GeoPackageLayerSpec(table, geom)
            }
        }
        return specs
    }

    private fun tableColumns(db: SQLiteDatabase, tableName: String): List<String> {
        val cols = ArrayList<String>()
        db.rawQuery("PRAGMA table_info(${quoteIdentifier(tableName)})", null).use { cursor ->
            while (cursor.moveToNext()) cols += cursor.getString(cursor.getColumnIndexOrThrow("name"))
        }
        return cols
    }

    private fun parseGeoPackageGeometry(blob: ByteArray): List<ParsedGeometry> {
        if (blob.size < 8 || blob[0] != 0x47.toByte() || blob[1] != 0x50.toByte()) return emptyList()
        val flags = blob[3].toInt() and 0xFF
        val envelopeIndicator = (flags shr 1) and 0x07
        val envelopeBytes = when (envelopeIndicator) {
            0 -> 0
            1 -> 32
            2, 3 -> 48
            4 -> 64
            else -> 0
        }
        val wkbOffset = 8 + envelopeBytes
        if (blob.size <= wkbOffset + 5) return emptyList()
        return parseWkbGeometry(blob, wkbOffset)
    }

    private fun parseWkbGeometry(bytes: ByteArray, offset: Int): List<ParsedGeometry> {
        var index = offset
        fun readByteOrder(): Boolean {
            val little = bytes[index].toInt() == 1
            index += 1
            return little
        }
        fun readInt(little: Boolean): Int {
            val b0 = bytes[index].toInt() and 0xFF
            val b1 = bytes[index + 1].toInt() and 0xFF
            val b2 = bytes[index + 2].toInt() and 0xFF
            val b3 = bytes[index + 3].toInt() and 0xFF
            index += 4
            return if (little) b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) else (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }
        fun readDouble(little: Boolean): Double {
            var value = 0L
            if (little) {
                for (i in 0 until 8) value = value or ((bytes[index + i].toLong() and 0xFF) shl (8 * i))
            } else {
                for (i in 0 until 8) value = (value shl 8) or (bytes[index + i].toLong() and 0xFF)
            }
            index += 8
            return java.lang.Double.longBitsToDouble(value)
        }
        fun parseOne(): List<ParsedGeometry> {
            val little = readByteOrder()
            val geometryTypeRaw = readInt(little)
            val geometryType = ((geometryTypeRaw % 1000) + 1000) % 1000
            return when (geometryType) {
                1 -> {
                    val x = readDouble(little)
                    val y = readDouble(little)
                    listOf(ParsedGeometry.PointGeom(lat = y, lon = x))
                }
                2 -> {
                    val count = readInt(little).coerceAtLeast(0)
                    val pts = ArrayList<Pair<Double, Double>>(count)
                    repeat(count) {
                        val x = readDouble(little)
                        val y = readDouble(little)
                        pts += (y to x)
                    }
                    listOf(ParsedGeometry.LineGeom(points = pts))
                }
                3 -> {
                    val rings = readInt(little).coerceAtLeast(0)
                    val out = ArrayList<ParsedGeometry>()
                    repeat(rings) { ringIndex ->
                        val count = readInt(little).coerceAtLeast(0)
                        val pts = ArrayList<Pair<Double, Double>>(count)
                        repeat(count) {
                            val x = readDouble(little)
                            val y = readDouble(little)
                            pts += (y to x)
                        }
                        if (ringIndex == 0 && pts.isNotEmpty()) out += ParsedGeometry.LineGeom(pts)
                    }
                    out
                }
                4 -> {
                    val count = readInt(little).coerceAtLeast(0)
                    val out = ArrayList<ParsedGeometry>()
                    repeat(count) { out += parseOne() }
                    out
                }
                5, 6, 7 -> {
                    val count = readInt(little).coerceAtLeast(0)
                    val out = ArrayList<ParsedGeometry>()
                    repeat(count) { out += parseOne() }
                    out
                }
                else -> emptyList()
            }
        }
        return try { parseOne() } catch (_: Throwable) { emptyList() }
    }

    private enum class GeoTiffCoordinateSystem { WGS84, HTRS }

    private data class GeoTiffInfo(
        val width: Int,
        val height: Int,
        val pixelScaleX: Double,
        val pixelScaleY: Double,
        val tiePointPixelX: Double,
        val tiePointPixelY: Double,
        val tiePointModelX: Double,
        val tiePointModelY: Double,
        val coordinateSystem: GeoTiffCoordinateSystem,
        val epsg: Int?
    )

    private data class GeoTiffBounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

    private fun looksLikeTiff(sniffBytes: ByteArray, count: Int): Boolean {
        if (count < 4) return false
        return (sniffBytes[0] == 'I'.code.toByte() && sniffBytes[1] == 'I'.code.toByte() && sniffBytes[2] == 42.toByte() && sniffBytes[3] == 0.toByte()) ||
            (sniffBytes[0] == 'M'.code.toByte() && sniffBytes[1] == 'M'.code.toByte() && sniffBytes[2] == 0.toByte() && sniffBytes[3] == 42.toByte())
    }

    private fun parseGeoTiffInfo(bytes: ByteArray): GeoTiffInfo? {
        if (bytes.size < 8 || !looksLikeTiff(bytes, bytes.size)) return null
        val little = bytes[0] == 'I'.code.toByte()
        val ifdOffset = readTiffInt(bytes, 4, little)
        if (ifdOffset <= 0 || ifdOffset + 2 > bytes.size) return null
        val entryCount = readTiffUnsignedShort(bytes, ifdOffset, little)
        var width: Int? = null
        var height: Int? = null
        var pixelScale: DoubleArray? = null
        var tiePoints: DoubleArray? = null
        var geoKeys: IntArray? = null
        for (i in 0 until entryCount) {
            val entryOffset = ifdOffset + 2 + (i * 12)
            if (entryOffset + 12 > bytes.size) break
            val tag = readTiffUnsignedShort(bytes, entryOffset, little)
            val type = readTiffUnsignedShort(bytes, entryOffset + 2, little)
            val count = readTiffInt(bytes, entryOffset + 4, little)
            val valueOffset = entryOffset + 8
            when (tag) {
                256 -> width = readTiffScalarInt(bytes, valueOffset, type, count, little)
                257 -> height = readTiffScalarInt(bytes, valueOffset, type, count, little)
                33550 -> pixelScale = readTiffDoubleArray(bytes, valueOffset, type, count, little)
                33922 -> tiePoints = readTiffDoubleArray(bytes, valueOffset, type, count, little)
                34735 -> geoKeys = readTiffShortArray(bytes, valueOffset, type, count, little)
            }
        }
        val w = width ?: return null
        val h = height ?: return null
        val scale = pixelScale?.takeIf { it.size >= 2 } ?: return null
        val tie = tiePoints?.takeIf { it.size >= 6 } ?: return null
        val epsg = parseGeoTiffEpsg(geoKeys)
        val coordinateSystem = when {
            epsg == 4326 -> GeoTiffCoordinateSystem.WGS84
            epsg == 3765 -> GeoTiffCoordinateSystem.HTRS
            kotlin.math.abs(tie[3]) <= 180.0 && kotlin.math.abs(tie[4]) <= 90.0 -> GeoTiffCoordinateSystem.WGS84
            else -> GeoTiffCoordinateSystem.HTRS
        }
        return GeoTiffInfo(
            width = w,
            height = h,
            pixelScaleX = kotlin.math.abs(scale[0]),
            pixelScaleY = kotlin.math.abs(scale[1]),
            tiePointPixelX = tie[0],
            tiePointPixelY = tie[1],
            tiePointModelX = tie[3],
            tiePointModelY = tie[4],
            coordinateSystem = coordinateSystem,
            epsg = epsg
        )
    }

    private fun parseGeoTiffEpsg(geoKeys: IntArray?): Int? {
        if (geoKeys == null || geoKeys.size < 4) return null
        val keyCount = geoKeys.getOrNull(3) ?: return null
        for (i in 0 until keyCount) {
            val base = 4 + (i * 4)
            if (base + 3 >= geoKeys.size) break
            val keyId = geoKeys[base]
            val tiffTagLocation = geoKeys[base + 1]
            val count = geoKeys[base + 2]
            val valueOffset = geoKeys[base + 3]
            if (count <= 0) continue
            when (keyId) {
                2048, 3072 -> if (tiffTagLocation == 0) return valueOffset
            }
        }
        return null
    }

    private fun geoTiffInfoToBounds(info: GeoTiffInfo): GeoTiffBounds {
        val originX = info.tiePointModelX - (info.tiePointPixelX * info.pixelScaleX)
        val originY = info.tiePointModelY + (info.tiePointPixelY * info.pixelScaleY)
        val maxX = originX + (info.width * info.pixelScaleX)
        val minY = originY - (info.height * info.pixelScaleY)
        return GeoTiffBounds(
            minX = minOf(originX, maxX),
            minY = minOf(minY, originY),
            maxX = maxOf(originX, maxX),
            maxY = maxOf(minY, originY)
        )
    }

    private fun readTiffUnsignedShort(bytes: ByteArray, offset: Int, little: Boolean): Int {
        val b0 = bytes.getOrElse(offset) { 0 }.toInt() and 0xFF
        val b1 = bytes.getOrElse(offset + 1) { 0 }.toInt() and 0xFF
        return if (little) b0 or (b1 shl 8) else (b0 shl 8) or b1
    }

    private fun readTiffInt(bytes: ByteArray, offset: Int, little: Boolean): Int {
        val b0 = bytes.getOrElse(offset) { 0 }.toInt() and 0xFF
        val b1 = bytes.getOrElse(offset + 1) { 0 }.toInt() and 0xFF
        val b2 = bytes.getOrElse(offset + 2) { 0 }.toInt() and 0xFF
        val b3 = bytes.getOrElse(offset + 3) { 0 }.toInt() and 0xFF
        return if (little) {
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        } else {
            (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }
    }

    private fun readTiffLong(bytes: ByteArray, offset: Int, little: Boolean): Long {
        var value = 0L
        return if (little) {
            for (i in 0 until 8) value = value or ((bytes.getOrElse(offset + i) { 0 }.toLong() and 0xFFL) shl (8 * i))
            value
        } else {
            for (i in 0 until 8) value = (value shl 8) or (bytes.getOrElse(offset + i) { 0 }.toLong() and 0xFFL)
            value
        }
    }

    private fun readTiffDouble(bytes: ByteArray, offset: Int, little: Boolean): Double = java.lang.Double.longBitsToDouble(readTiffLong(bytes, offset, little))

    private fun readTiffScalarInt(bytes: ByteArray, valueOffsetField: Int, type: Int, count: Int, little: Boolean): Int? {
        if (count <= 0) return null
        return when (type) {
            3 -> {
                if (count == 1) {
                    readTiffUnsignedShort(bytes, valueOffsetField, little)
                } else {
                    val dataOffset = readTiffInt(bytes, valueOffsetField, little)
                    readTiffUnsignedShort(bytes, dataOffset, little)
                }
            }
            4 -> {
                if (count == 1) {
                    readTiffInt(bytes, valueOffsetField, little)
                } else {
                    val dataOffset = readTiffInt(bytes, valueOffsetField, little)
                    readTiffInt(bytes, dataOffset, little)
                }
            }
            else -> null
        }
    }

    private fun readTiffDoubleArray(bytes: ByteArray, valueOffsetField: Int, type: Int, count: Int, little: Boolean): DoubleArray? {
        if (type != 12 || count <= 0) return null
        val dataOffset = readTiffInt(bytes, valueOffsetField, little)
        if (dataOffset < 0 || dataOffset + (count * 8) > bytes.size) return null
        return DoubleArray(count) { idx -> readTiffDouble(bytes, dataOffset + (idx * 8), little) }
    }

    private fun readTiffShortArray(bytes: ByteArray, valueOffsetField: Int, type: Int, count: Int, little: Boolean): IntArray? {
        if (type != 3 || count <= 0) return null
        val inlineFits = count * 2 <= 4
        val dataOffset = if (inlineFits) valueOffsetField else readTiffInt(bytes, valueOffsetField, little)
        if (dataOffset < 0 || dataOffset + (count * 2) > bytes.size) return null
        return IntArray(count) { idx -> readTiffUnsignedShort(bytes, dataOffset + (idx * 2), little) }
    }

    private fun addPoint(coords: JSONArray?, name: String, description: String, target: MutableList<MarkedPoint>) {
        val lon = coords?.optDouble(0)?.takeIf { !it.isNaN() } ?: return
        val lat = coords.optDouble(1).takeIf { !it.isNaN() } ?: return
        val conv = CoordinateConverter.wgs84ToHtrs96Tm(lat, lon)
        target += MarkedPoint(
            id = "imp_geo_pt_${System.nanoTime()}_${target.size + 1}",
            name = name.ifBlank { "GeoJSON točka ${target.size + 1}" },
            type = "import",
            description = description.ifBlank { "GeoJSON import" },
            lat = lat,
            lon = lon,
            htrsX = conv.x,
            htrsY = conv.y
        )
    }

    private fun addTrack(coords: JSONArray?, name: String, description: String, target: MutableList<SavedTrack>) {
        if (coords == null) return
        val pts = ArrayList<TrackPoint>(coords.length())
        for (i in 0 until coords.length()) {
            val pair = coords.optJSONArray(i) ?: continue
            val lon = pair.optDouble(0)
            val lat = pair.optDouble(1)
            if (!lon.isNaN() && !lat.isNaN()) {
                val alt = pair.optDouble(2).takeIf { !it.isNaN() }
                pts += TrackPoint(GeoPoint(lat, lon), alt)
            }
        }
        if (pts.isNotEmpty()) {
            target += SavedTrack(
                id = "imp_geo_track_${System.nanoTime()}_${target.size + 1}",
                name = name.ifBlank { "GeoJSON linija ${target.size + 1}" },
                description = description.ifBlank { "GeoJSON import" },
                points = simplifyTrackPoints(pts)
            )
        }
    }

    private fun bestName(properties: JSONObject?, fallbackNode: JSONObject? = null, fallback: String = ""): String {
        val candidates = listOf("name", "title", "label", "naziv", "ime", "id")
        for (key in candidates) {
            val value = properties?.optString(key).orEmpty().trim().ifBlank { fallbackNode?.optString(key).orEmpty().trim() }
            if (value.isNotBlank() && !value.equals("null", true)) return value
        }
        return fallback
    }

    private fun bestDescription(properties: JSONObject?): String {
        val candidates = listOf("description", "desc", "opis", "note", "notes", "comment", "komentar")
        for (key in candidates) {
            val value = properties?.optString(key).orEmpty().trim()
            if (value.isNotBlank() && !value.equals("null", true)) return value
        }
        return "GeoJSON import"
    }

    private fun lineName(base: String, index: Int): String = when {
        base.isBlank() -> "Linija ${index + 1}"
        else -> "$base ${index + 1}"
    }

    private fun looksLikeGeoJson(sniff: String): Boolean {
        val sample = sniff.trimStart()
        return sample.startsWith("{") && sample.contains("\"type\"") && (
            sample.contains("FeatureCollection") ||
                sample.contains("Feature") ||
                sample.contains("LineString") ||
                sample.contains("Point") ||
                sample.contains("Polygon")
            )
    }

    private fun looksLikeCsv(sniff: String): Boolean {
        val lines = sniff.lineSequence().filter { it.isNotBlank() }.take(3).toList()
        if (lines.size < 2) return false
        return listOf(',', ';', '\t').any { delimiter ->
            lines.all { parseDelimitedLine(it, delimiter).size >= 2 }
        }
    }

    private fun detectDelimiter(headerLine: String): Char {
        val delimiters = listOf(';', ',', '\t', '|')
        return delimiters.maxByOrNull { delim -> parseDelimitedLine(headerLine, delim).size } ?: ','
    }


    private data class DbfField(val name: String, val length: Int)
    private data class DbfTable(val fields: List<String>, val rows: List<List<String>>)
    private enum class ShapefileProjection { WGS84, HTRS }

    private fun findZipEntryByBase(entries: Map<String, ByteArray>, basePath: String, extension: String): ByteArray? {
        val target = (basePath + extension).lowercase(Locale.ROOT)
        return entries.entries.firstOrNull { it.key.lowercase(Locale.ROOT) == target }?.value
    }

    private fun parseDbfTable(bytes: ByteArray): DbfTable {
        if (bytes.size < 32) return DbfTable(emptyList(), emptyList())
        val recordCount = readIntLE(bytes, 4).coerceAtLeast(0)
        val headerLength = readUnsignedShortLE(bytes, 8).coerceAtLeast(32)
        val recordLength = readUnsignedShortLE(bytes, 10).coerceAtLeast(1)
        val charset = runCatching { charset("Windows-1250") }.getOrElse { Charsets.ISO_8859_1 }
        val fields = ArrayList<DbfField>()
        var pos = 32
        while (pos + 32 <= bytes.size && pos < headerLength) {
            val marker = bytes[pos].toInt() and 0xFF
            if (marker == 0x0D) break
            val rawName = bytes.copyOfRange(pos, pos + 11)
            val zeroIndex = rawName.indexOfFirst { it == 0.toByte() }.let { if (it < 0) rawName.size else it }
            val name = rawName.copyOfRange(0, zeroIndex).toString(charset).trim().trimEnd('\u0000')
            val length = bytes[pos + 16].toInt() and 0xFF
            if (name.isNotBlank() && length > 0) fields += DbfField(name, length)
            pos += 32
        }
        if (fields.isEmpty()) return DbfTable(emptyList(), emptyList())
        val rows = ArrayList<List<String>>()
        var recPos = headerLength
        var parsed = 0
        while (recPos + recordLength <= bytes.size && parsed < recordCount) {
            if ((bytes[recPos].toInt().toChar()) != '*') {
                var fieldPos = recPos + 1
                val row = ArrayList<String>(fields.size)
                fields.forEach { field ->
                    val end = (fieldPos + field.length).coerceAtMost(bytes.size)
                    row += bytes.copyOfRange(fieldPos, end).toString(charset).trim().trimEnd('\u0000')
                    fieldPos += field.length
                }
                rows += row
            }
            recPos += recordLength
            parsed += 1
        }
        return DbfTable(fields.map { it.name }, rows)
    }

    private fun inferShapefileProjection(prjText: String?, shpBytes: ByteArray): ShapefileProjection {
        val normalizedPrj = prjText.orEmpty().uppercase(Locale.ROOT)
        if (normalizedPrj.contains("3765") || normalizedPrj.contains("HTRS96") || normalizedPrj.contains("CROATIA_TM") || normalizedPrj.contains("CROATIA TM")) {
            return ShapefileProjection.HTRS
        }
        if (normalizedPrj.contains("4326") || normalizedPrj.contains("WGS84") || normalizedPrj.contains("WGS 84")) {
            return ShapefileProjection.WGS84
        }
        if (shpBytes.size >= 68) {
            val xMin = kotlin.math.abs(readDoubleLE(shpBytes, 36))
            val yMin = kotlin.math.abs(readDoubleLE(shpBytes, 44))
            val xMax = kotlin.math.abs(readDoubleLE(shpBytes, 52))
            val yMax = kotlin.math.abs(readDoubleLE(shpBytes, 60))
            if (listOf(xMin, yMin, xMax, yMax).any { it > 1000.0 }) return ShapefileProjection.HTRS
        }
        return ShapefileProjection.WGS84
    }

    private fun shapefileCoordsToLatLon(x: Double, y: Double, projection: ShapefileProjection): CoordinateConverter.LatLon {
        return when (projection) {
            ShapefileProjection.WGS84 -> CoordinateConverter.LatLon(lat = y, lon = x)
            ShapefileProjection.HTRS -> CoordinateConverter.htrs96TmToWgs84(x, y)
        }
    }

    private fun readIntBE(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes.getOrElse(offset) { 0 }.toInt() and 0xFF
        val b1 = bytes.getOrElse(offset + 1) { 0 }.toInt() and 0xFF
        val b2 = bytes.getOrElse(offset + 2) { 0 }.toInt() and 0xFF
        val b3 = bytes.getOrElse(offset + 3) { 0 }.toInt() and 0xFF
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    private fun readIntLE(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes.getOrElse(offset) { 0 }.toInt() and 0xFF
        val b1 = bytes.getOrElse(offset + 1) { 0 }.toInt() and 0xFF
        val b2 = bytes.getOrElse(offset + 2) { 0 }.toInt() and 0xFF
        val b3 = bytes.getOrElse(offset + 3) { 0 }.toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun readUnsignedShortLE(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes.getOrElse(offset) { 0 }.toInt() and 0xFF
        val b1 = bytes.getOrElse(offset + 1) { 0 }.toInt() and 0xFF
        return b0 or (b1 shl 8)
    }

    private fun readDoubleLE(bytes: ByteArray, offset: Int): Double {
        var bits = 0L
        for (i in 0 until 8) bits = bits or ((bytes.getOrElse(offset + i) { 0 }.toLong() and 0xFFL) shl (8 * i))
        return java.lang.Double.longBitsToDouble(bits)
    }

    private fun parseDelimitedLine(line: String, delimiter: Char): List<String> {
        val result = ArrayList<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i += 1
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> {
                    result += current.toString()
                    current.setLength(0)
                }
                else -> current.append(ch)
            }
            i += 1
        }
        result += current.toString()
        return result
    }

    private fun normalizeHeader(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace("č", "c")
        .replace("ć", "c")
        .replace("ž", "z")
        .replace("š", "s")
        .replace("đ", "d")
        .replace(Regex("[^a-z0-9]+"), "")

    private enum class CoordinateMode { UNKNOWN, WGS84, HTRS }

    private fun looksLikeLatHeader(header: String): Boolean {
        if (header in setOf("lat", "latitude", "gpslat", "wgs84lat", "geolat", "geographiclat", "geo_lat", "latdd", "latdeg", "latdecimal", "sirina", "geosirina")) return true
        return listOf("latitude", "gpslat", "wgs84lat", "geolat", "latdd", "sirina").any { header.contains(it) }
    }

    private fun looksLikeLonHeader(header: String): Boolean {
        if (header in setOf("lon", "lng", "longitude", "gpslon", "wgs84lon", "geolon", "geographiclon", "geo_lon", "londd", "londeg", "londecimal", "duzina", "geoduzina")) return true
        return listOf("longitude", "gpslon", "wgs84lon", "geolon", "londd", "duzina").any { header.contains(it) }
    }

    private fun looksLikeProjectedXHeader(header: String): Boolean {
        if (header in setOf("htrsx", "htrs96x", "htrsxm", "easting", "coordx", "coordinatex", "x3765", "tmx", "east", "istok", "e")) return true
        return listOf("htrsx", "easting", "coordx", "coordinatex", "x3765", "tmx").any { header.contains(it) }
    }

    private fun looksLikeProjectedYHeader(header: String): Boolean {
        if (header in setOf("htrsy", "htrs96y", "htrsym", "northing", "coordy", "coordinatey", "y3765", "tmy", "north", "sjever", "n")) return true
        return listOf("htrsy", "northing", "coordy", "coordinatey", "y3765", "tmy").any { header.contains(it) }
    }

    private fun looksLikeNameHeader(header: String): Boolean {
        if (header in setOf("name", "naziv", "title", "label", "ime", "objectname", "nazivobjekta", "lokacija")) return true
        return listOf("name", "naziv", "title", "label", "ime", "nazivobjekta").any { header.contains(it) }
    }

    private fun looksLikeDescriptionHeader(header: String): Boolean {
        if (header in setOf("description", "opis", "desc", "note", "notes", "comment", "komentar", "napomena", "remarks")) return true
        return listOf("description", "opis", "desc", "note", "comment", "komentar", "napomena").any { header.contains(it) }
    }

    private fun inferGenericXyMode(rows: List<String>, delimiter: Char, xIdx: Int, yIdx: Int): CoordinateMode {
        var wgsHits = 0
        var projectedHits = 0
        rows.take(12).forEach { line ->
            val row = parseDelimitedLine(line, delimiter)
            val x = row.getOrNull(xIdx)?.parseFlexibleDouble() ?: return@forEach
            val y = row.getOrNull(yIdx)?.parseFlexibleDouble() ?: return@forEach
            when {
                kotlin.math.abs(x) <= 180.0 && kotlin.math.abs(y) <= 90.0 -> wgsHits += 1
                kotlin.math.abs(x) > 1000.0 || kotlin.math.abs(y) > 1000.0 -> projectedHits += 1
            }
        }
        return when {
            projectedHits > 0 && projectedHits >= wgsHits -> CoordinateMode.HTRS
            wgsHits > 0 -> CoordinateMode.WGS84
            else -> CoordinateMode.UNKNOWN
        }
    }

    private fun String.parseFlexibleDouble(): Double? = trim().replace(',', '.').toDoubleOrNull()

    private fun emitProgress(onProgress: (Progress) -> Unit, stage: String, processed: Int, points: Int, tracks: Int) {
        onProgress(Progress(stage = stage, processed = processed, points = points, tracks = tracks))
    }

    private fun ensureNotCancelled(isCancelled: () -> Boolean) {
        if (isCancelled()) throw CancellationException("Import cancelled")
    }

    private fun simplifyTrackPoints(points: List<TrackPoint>): List<TrackPoint> {
        if (points.size < 800) return points
        val minSpacingM = when {
            points.size >= 15000 -> 20.0
            points.size >= 8000 -> 14.0
            points.size >= 3000 -> 9.0
            else -> 6.0
        }
        val simplified = ArrayList<TrackPoint>(points.size.coerceAtMost(6000))
        simplified += points.first()
        var anchor = points.first()
        for (index in 1 until points.lastIndex) {
            val candidate = points[index]
            if (distanceMeters(anchor.point, candidate.point) >= minSpacingM) {
                simplified += candidate
                anchor = candidate
            }
        }
        if (simplified.last() != points.last()) simplified += points.last()
        if (simplified.size <= 6000) return simplified
        val step = kotlin.math.ceil(simplified.size / 6000.0).toInt().coerceAtLeast(1)
        return simplified.filterIndexed { index, _ -> index == 0 || index == simplified.lastIndex || index % step == 0 }
    }

    private fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val result = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result.firstOrNull()?.toDouble() ?: 0.0
    }

    private fun normalizeKmlDescription(raw: String): String {
        val value = xmlUnescape(raw).trim()
        if (value.isBlank()) return ""

        val tableRows = Regex(
            pattern = """<tr[^>]*>\s*<th[^>]*>(.*?)</th>\s*<td[^>]*>(.*?)</td>\s*</tr>""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
            .findAll(value)
            .mapNotNull { match ->
                val key = stripHtml(match.groupValues[1]).trim().trimEnd(':')
                val cell = stripHtml(match.groupValues[2]).trim()
                if (key.isBlank() || cell.isBlank()) null else "$key: $cell"
            }
            .toList()

        if (tableRows.isNotEmpty()) return tableRows.joinToString("\n")

        val withBreaks = value
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</div>""", RegexOption.IGNORE_CASE), "\n")

        return stripHtml(withBreaks)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun stripHtml(value: String): String = value
        .replace(Regex("""<[^>]+>"""), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun xmlUnescape(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
}
