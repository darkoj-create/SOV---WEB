package com.darko.speleov1.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.darko.speleov1.model.Cadastre
import com.darko.speleov1.model.Classification
import com.darko.speleov1.model.Condition
import com.darko.speleov1.model.Content
import com.darko.speleov1.model.Location
import com.darko.speleov1.model.Metrics
import com.darko.speleov1.model.Research
import com.darko.speleov1.model.SpeleoRecord
import java.io.File
import java.text.Normalizer
import java.util.Locale

object MyBaseRepository {
    private const val SOURCE = "my_base"
    private const val LABEL = "my_base"

    data class ImportResult(
        val importedPoints: Int,
        val fileName: String,
        val totalPoints: Int
    )

    fun rootDir(context: Context): File = File(context.filesDir, "mybase")
    fun importsDir(context: Context): File = File(rootDir(context), "imports")

    fun loadRecords(context: Context): List<SpeleoRecord> {
        val dir = importsDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file -> file.isFile && isSupportedImportFile(file) }
            .orEmpty()
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .flatMap { file -> parseImportFile(file) }
    }

    fun importKml(context: Context, uri: Uri): ImportResult = importFile(context, uri)

    fun importFile(context: Context, uri: Uri): ImportResult {
        val originalName = resolveDisplayName(context, uri).takeIf { it.isNotBlank() } ?: "mybase_${System.currentTimeMillis()}.kml"
        val ext = resolveSupportedExtension(originalName)
            ?: resolveSupportedExtension(uri.lastPathSegment.orEmpty())
            ?: resolveSupportedMime(context.contentResolver.getType(uri).orEmpty())
            ?: error("Podržani su samo KML i CSV za Moju bazu.")
        val wantedName = if (originalName.endsWith(".$ext", ignoreCase = true)) originalName else "$originalName.$ext"
        val safeName = sanitizeFileName(wantedName, ext)
        val dir = importsDir(context).apply { mkdirs() }
        val target = File(dir, uniqueFileName(dir, safeName))
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Datoteka se ne može otvoriti.")
        val imported = parseImportFile(target).size
        if (imported == 0) {
            target.delete()
            error(if (ext == "csv") "CSV nema prepoznatljive točke. Treba imati stupce name/naziv i latitude/longitude ili lat/lon." else "KML nema prepoznatljive Point točke.")
        }
        val total = loadRecords(context).size
        return ImportResult(importedPoints = imported, fileName = target.name, totalPoints = total)
    }

    fun clear(context: Context): Int {
        val dir = importsDir(context)
        val files = dir.listFiles().orEmpty()
        files.forEach { it.deleteRecursively() }
        return files.size
    }


    fun exportCsv(context: Context): File {
        val records = loadRecords(context)
        require(records.isNotEmpty()) { "Moja baza je prazna." }
        val dir = File(rootDir(context), "exports").apply { mkdirs() }
        val file = File(dir, "moja_baza_${System.currentTimeMillis()}.csv")
        val header = listOf("name", "latitude", "longitude", "altitude_m", "description", "note", "source_file")
        val rows = records.map { record ->
            listOf(
                record.name,
                record.location.lat?.toString().orEmpty(),
                record.location.lon?.toString().orEmpty(),
                record.location.altitude_m?.toString().orEmpty(),
                record.content.technical_description.orEmpty(),
                record.content.note.orEmpty(),
                record.raw?.get("source_file")?.toString().orEmpty()
            )
        }
        file.writeText((listOf(header) + rows).joinToString("\n") { row -> row.joinToString(",") { csvEscape(it) } }, Charsets.UTF_8)
        return file
    }

    fun exportKml(context: Context): File {
        val records = loadRecords(context)
        require(records.isNotEmpty()) { "Moja baza je prazna." }
        val dir = File(rootDir(context), "exports").apply { mkdirs() }
        val file = File(dir, "moja_baza_${System.currentTimeMillis()}.kml")
        val placemarks = records.joinToString("\n") { record ->
            val lat = record.location.lat ?: 0.0
            val lon = record.location.lon ?: 0.0
            val alt = record.location.altitude_m ?: 0.0
            val description = listOfNotNull(
                record.content.technical_description,
                record.content.note
            ).filter { it.isNotBlank() }.joinToString("\n\n")
            """
    <Placemark>
      <name>${xmlEscape(record.name)}</name>
      <description>${xmlEscape(description)}</description>
      <Point>
        <coordinates>$lon,$lat,$alt</coordinates>
      </Point>
    </Placemark>""".trimEnd()
        }
        file.writeText("""<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>Moja baza</name>
$placemarks
  </Document>
</kml>
""", Charsets.UTF_8)
        return file
    }

    fun summary(context: Context): String {
        val files = importsDir(context).listFiles { file -> file.isFile && isSupportedImportFile(file) }.orEmpty()
        val count = loadRecords(context).size
        if (count == 0) return "Nema učitanih KML/CSV točaka"
        val kmlCount = files.count { it.extension.equals("kml", ignoreCase = true) }
        val csvCount = files.count { it.extension.equals("csv", ignoreCase = true) }
        val parts = listOfNotNull(
            kmlCount.takeIf { it > 0 }?.let { "$it KML" },
            csvCount.takeIf { it > 0 }?.let { "$it CSV" }
        ).joinToString(" + ")
        return "$count točaka iz $parts datoteka"
    }

    fun cacheFingerprint(context: Context): String {
        val files = importsDir(context).listFiles { file -> file.isFile && isSupportedImportFile(file) }.orEmpty()
        if (files.isEmpty()) return "mybase:empty"
        return files.sortedBy { it.name }.joinToString("|") { file ->
            "${file.name}:${file.length()}:${file.lastModified()}"
        }
    }

    private fun isSupportedImportFile(file: File): Boolean = resolveSupportedExtension(file.name) != null

    private fun resolveSupportedExtension(name: String): String? {
        val lower = name.substringAfterLast('/', name).substringAfterLast('\\', name).lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".kml") -> "kml"
            lower.endsWith(".csv") -> "csv"
            else -> null
        }
    }

    private fun resolveSupportedMime(mime: String): String? {
        val lower = mime.lowercase(Locale.ROOT)
        return when {
            "kml" in lower || "google-earth" in lower -> "kml"
            "csv" in lower || "comma-separated" in lower -> "csv"
            else -> null
        }
    }

    private fun parseImportFile(file: File): List<SpeleoRecord> {
        val text = file.readText(Charsets.UTF_8)
        return when (resolveSupportedExtension(file.name)) {
            "kml" -> parseKml(text, file.nameWithoutExtension)
            "csv" -> parseCsv(text, file.nameWithoutExtension)
            else -> emptyList()
        }
    }

    private fun parseKml(kml: String, sourceFileName: String): List<SpeleoRecord> {
        val placemarkRegex = Regex("<Placemark\\b[^>]*>(.*?)</Placemark>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        return placemarkRegex.findAll(kml).mapIndexedNotNull { index, match ->
            val block = match.groupValues[1]
            val coordinatesText = Regex("<Point\\b[^>]*>.*?<coordinates\\b[^>]*>(.*?)</coordinates>.*?</Point>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(block)?.groupValues?.getOrNull(1)
                ?: Regex("<coordinates\\b[^>]*>(.*?)</coordinates>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .find(block)?.groupValues?.getOrNull(1)
                ?: return@mapIndexedNotNull null
            val firstCoordinate = coordinatesText.trim().split(Regex("\\s+")).firstOrNull { it.contains(',') } ?: return@mapIndexedNotNull null
            val parts = firstCoordinate.split(',')
            val lon = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return@mapIndexedNotNull null
            val lat = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return@mapIndexedNotNull null
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return@mapIndexedNotNull null

            val rawName = extractTag(block, "name").ifBlank { "Moja točka ${index + 1}" }
            val description = extractTag(block, "description")
            val extended = extractExtendedData(block)
            val idBase = normalizeId("$sourceFileName-$index-$rawName-$lat-$lon")

            SpeleoRecord(
                id = "mybase_$idBase",
                source = SOURCE,
                source_labels = listOf(LABEL),
                name = rawName,
                location = Location(
                    lat = lat,
                    lon = lon,
                    county = null,
                    municipality = null,
                    nearest_place = null,
                    locality = null,
                    island = null,
                    altitude_m = parts.getOrNull(2)?.trim()?.toDoubleOrNull(),
                    protected_area = null
                ),
                cadastre = Cadastre(
                    status = "moja_baza",
                    cadastral_number = null,
                    in_cadastre = null,
                    not_in_cadastre_candidate = null
                ),
                classification = Classification(
                    object_type = "Moja baza",
                    object_type_source = "KML",
                    record_status = "moja_baza",
                    field_tasks = emptyList(),
                    priority = null,
                    kml_export_candidate = true
                ),
                metrics = Metrics(
                    depth_m = null,
                    length_m = null,
                    vertical_range_m = null,
                    entrance_count = null
                ),
                condition = Condition(
                    plate_number = null,
                    main_entrance_status = null,
                    hazards = null,
                    pollution = null
                ),
                research = Research(
                    last_research_year = null,
                    last_research_date = null,
                    clubs = null,
                    team_members = null,
                    survey_author = null,
                    survey_in_digital_base = null,
                    bibliography_record = null,
                    georef_record = null,
                    further_research_possible = null,
                    further_research_note = null
                ),
                content = Content(
                    access_description = null,
                    technical_description = description.ifBlank { null },
                    note = listOf("Moja baza: $sourceFileName", extended).filter { it.isNotBlank() }.joinToString("\n").ifBlank { null },
                    literature = null,
                    name_origin = null,
                    synonyms = null,
                    other_synonyms = null,
                    clean_cave_report = null,
                    geological_or_anthropogenic_activities = null
                ),
                raw = mapOf(
                    "source_file" to sourceFileName,
                    "kml_description" to description,
                    "extended_data" to extended
                ),
                search_text = listOf(rawName, description, extended, sourceFileName, "moja baza", "my base").joinToString(" ")
            )
        }.toList()
    }

    private fun parseCsv(csv: String, sourceFileName: String): List<SpeleoRecord> {
        val rows = parseCsvRows(csv)
            .filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return emptyList()

        val first = rows.first().map { normalizeHeader(it) }
        val hasHeader = first.any { it in setOf("name", "naziv", "lat", "latitude", "lon", "lng", "longitude", "opis", "description") }
        val headers = if (hasHeader) first else emptyList()
        val dataRows = if (hasHeader) rows.drop(1) else rows

        fun headerIndex(vararg names: String): Int? {
            val normalized = names.map(::normalizeHeader).toSet()
            return headers.indexOfFirst { it in normalized }.takeIf { it >= 0 }
        }

        val nameIndex = headerIndex("name", "naziv", "ime", "object", "objekt", "title")
        val latIndex = headerIndex("lat", "latitude", "sirina", "sjever", "y", "wgs84_lat")
        val lonIndex = headerIndex("lon", "lng", "longitude", "duzina", "istok", "x", "wgs84_lon")
        val altIndex = headerIndex("alt", "altitude", "altitude_m", "visina", "nadmorska_visina")
        val descriptionIndex = headerIndex("description", "opis", "technical_description", "napomena", "note", "komentar")

        return dataRows.mapIndexedNotNull { index, row ->
            fun cell(i: Int?): String = i?.let { row.getOrNull(it) }.orEmpty().trim()
            val lat = parseCoordinate(cell(latIndex)) ?: guessLatLon(row)?.first ?: return@mapIndexedNotNull null
            val lon = parseCoordinate(cell(lonIndex)) ?: guessLatLon(row)?.second ?: return@mapIndexedNotNull null
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return@mapIndexedNotNull null
            val rawName = cell(nameIndex).ifBlank {
                if (!hasHeader && row.isNotEmpty() && parseCoordinate(row.first()) == null) row.first().trim() else "Moja točka ${index + 1}"
            }
            val description = cell(descriptionIndex)
            val altitude = cell(altIndex).replace(',', '.').toDoubleOrNull()
            val extra = buildCsvExtra(headers, row, listOfNotNull(nameIndex, latIndex, lonIndex, altIndex, descriptionIndex).toSet())
            val idBase = normalizeId("$sourceFileName-$index-$rawName-$lat-$lon")

            SpeleoRecord(
                id = "mybase_$idBase",
                source = SOURCE,
                source_labels = listOf(LABEL),
                name = rawName,
                location = Location(
                    lat = lat,
                    lon = lon,
                    county = null,
                    municipality = null,
                    nearest_place = null,
                    locality = null,
                    island = null,
                    altitude_m = altitude,
                    protected_area = null
                ),
                cadastre = Cadastre(
                    status = "moja_baza",
                    cadastral_number = null,
                    in_cadastre = null,
                    not_in_cadastre_candidate = null
                ),
                classification = Classification(
                    object_type = "Moja baza",
                    object_type_source = "CSV",
                    record_status = "moja_baza",
                    field_tasks = emptyList(),
                    priority = null,
                    kml_export_candidate = true
                ),
                metrics = Metrics(
                    depth_m = null,
                    length_m = null,
                    vertical_range_m = null,
                    entrance_count = null
                ),
                condition = Condition(
                    plate_number = null,
                    main_entrance_status = null,
                    hazards = null,
                    pollution = null
                ),
                research = Research(
                    last_research_year = null,
                    last_research_date = null,
                    clubs = null,
                    team_members = null,
                    survey_author = null,
                    survey_in_digital_base = null,
                    bibliography_record = null,
                    georef_record = null,
                    further_research_possible = null,
                    further_research_note = null
                ),
                content = Content(
                    access_description = null,
                    technical_description = description.ifBlank { null },
                    note = listOf("Moja baza: $sourceFileName", extra).filter { it.isNotBlank() }.joinToString("\n").ifBlank { null },
                    literature = null,
                    name_origin = null,
                    synonyms = null,
                    other_synonyms = null,
                    clean_cave_report = null,
                    geological_or_anthropogenic_activities = null
                ),
                raw = mapOf(
                    "source_file" to sourceFileName,
                    "csv_description" to description,
                    "csv_extra" to extra
                ),
                search_text = listOf(rawName, description, extra, sourceFileName, "moja baza", "my base", "csv").joinToString(" ")
            )
        }
    }

    private fun parseCsvRows(csv: String): List<List<String>> {
        val cleaned = csv.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        val delimiter = detectCsvDelimiter(cleaned.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty())
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < cleaned.length) {
            val ch = cleaned[i]
            when {
                ch == '"' && inQuotes && i + 1 < cleaned.length && cleaned[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    row.add(cell.toString())
                    cell.clear()
                }
                ch == '\n' && !inQuotes -> {
                    row.add(cell.toString())
                    rows.add(row.toList())
                    row.clear()
                    cell.clear()
                }
                else -> cell.append(ch)
            }
            i++
        }
        row.add(cell.toString())
        if (row.any { it.isNotBlank() }) rows.add(row.toList())
        return rows
    }

    private fun detectCsvDelimiter(headerLine: String): Char {
        val candidates = listOf(',', ';', '\t')
        return candidates.maxByOrNull { candidate -> headerLine.count { it == candidate } } ?: ','
    }

    private fun parseCoordinate(value: String): Double? = value.trim()
        .replace(',', '.')
        .toDoubleOrNull()

    private fun guessLatLon(row: List<String>): Pair<Double, Double>? {
        val numbers = row.mapNotNull { parseCoordinate(it) }
        for (i in 0 until numbers.lastIndex) {
            val a = numbers[i]
            val b = numbers[i + 1]
            if (a in -90.0..90.0 && b in -180.0..180.0) return a to b
            if (b in -90.0..90.0 && a in -180.0..180.0) return b to a
        }
        return null
    }

    private fun buildCsvExtra(headers: List<String>, row: List<String>, usedIndexes: Set<Int>): String {
        if (headers.isEmpty()) return ""
        return row.mapIndexedNotNull { index, value ->
            val key = headers.getOrNull(index).orEmpty()
            val cleanValue = value.trim()
            if (index in usedIndexes || key.isBlank() || cleanValue.isBlank()) null else "$key: $cleanValue"
        }.joinToString("\n")
    }

    private fun normalizeHeader(value: String): String {
        val withoutDiacritics = Normalizer.normalize(value.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return withoutDiacritics.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    private fun extractTag(block: String, tag: String): String {
        return Regex("<$tag\\b[^>]*>(.*?)</$tag>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(block)?.groupValues?.getOrNull(1)
            ?.let(::cleanXmlText)
            .orEmpty()
    }

    private fun extractExtendedData(block: String): String {
        val dataRegex = Regex("<Data\\b[^>]*name=[\"']([^\"']+)[\"'][^>]*>.*?<value\\b[^>]*>(.*?)</value>.*?</Data>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        return dataRegex.findAll(block).mapNotNull { match ->
            val key = cleanXmlText(match.groupValues[1])
            val value = cleanXmlText(match.groupValues[2])
            if (key.isBlank() || value.isBlank()) null else "$key: $value"
        }.joinToString("\n")
    }


    private fun csvEscape(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace("\r", "\n")
        return if (normalized.any { it == ',' || it == '"' || it == '\n' }) {
            "\"${normalized.replace("\"", "\"\"")}\""
        } else {
            normalized
        }
    }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun cleanXmlText(value: String): String = value
        .replace("<![CDATA[", "")
        .replace("]]>", "")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index).orEmpty()
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: ""
    }

    private fun sanitizeFileName(value: String, fallbackExt: String = "kml"): String = value
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { "mybase_${System.currentTimeMillis()}.$fallbackExt" }

    private fun uniqueFileName(dir: File, wanted: String): String {
        val base = wanted.substringBeforeLast('.', wanted)
        val ext = wanted.substringAfterLast('.', "kml")
        var candidate = "$base.$ext"
        var n = 2
        while (File(dir, candidate).exists()) {
            candidate = "${base}_$n.$ext"
            n++
        }
        return candidate
    }

    private fun normalizeId(value: String): String {
        val withoutDiacritics = Normalizer.normalize(value.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return withoutDiacritics.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(90)
            .ifBlank { System.currentTimeMillis().toString() }
    }
}
