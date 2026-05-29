package com.darko.speleov1.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.darko.speleov1.model.SpeleoRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos

object TopoDroidBridgeStore {
    private const val PREFS = "topodroid_bridge_lite"
    private const val KEY_ATTACHMENTS = "attachments"
    private val gson = Gson()
    private const val MAX_PREFS_BYTES = 1_500_000 // ~1.5 MB safe limit per SharedPreferences key
    private const val MAX_TRACK_POINTS_PER_TRACK = 3000 // trim per-track before serializing

    data class Attachment(
        val id: String,
        val objectId: String,
        val objectName: String,
        val plateNumber: String?,
        val cadastralNumber: String?,
        val uri: String,
        val originalFilename: String,
        val fileType: String,
        val attachedAtMillis: Long,
        val source: String,
        val surveyName: String? = null,
        val surveyDate: String? = null,
        val surveyTeam: String? = null,
        val shotCount: Int? = null,
        val centerlineShotCount: Int? = null,
        val splayShotCount: Int? = null,
        val stationCount: Int? = null,
        val totalLengthM: Double? = null,
        val verticalRangeM: Double? = null,
        val hasSurveySql: Boolean? = null,
        val hasPlanDrawing: Boolean? = null,
        val hasProfileDrawing: Boolean? = null,
        val hasImageOrPdfPreview: Boolean? = null,
        val qcWarnings: List<String>? = emptyList()
    )

    data class SurveyStationView(
        val name: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val chain: Double
    )

    data class SurveySegmentView(
        val from: String,
        val to: String,
        val x1: Double,
        val y1: Double,
        val z1: Double,
        val chain1: Double,
        val x2: Double,
        val y2: Double,
        val z2: Double,
        val chain2: Double,
        val length: Double,
        val bearing: Double,
        val clino: Double,
        val type: String
    )

    data class SurveyViewerModel(
        val surveyName: String?,
        val surveyDate: String?,
        val surveyTeam: String?,
        val stations: List<SurveyStationView>,
        val centerline: List<SurveySegmentView>,
        val splays: List<SurveySegmentView>,
        val totalLengthM: Double,
        val verticalRangeM: Double,
        val qcWarnings: List<String>
    )

    data class ScanResult(
        val scanned: Int,
        val matched: Int,
        val skippedUnsupported: Int
    )

    private data class Shot(
        val from: String,
        val to: String,
        val length: Double,
        val bearing: Double,
        val clino: Double,
        val type: String
    )

    private data class TopoDroidAnalysis(
        val surveyName: String? = null,
        val surveyDate: String? = null,
        val surveyTeam: String? = null,
        val shotCount: Int = 0,
        val centerlineShotCount: Int = 0,
        val splayShotCount: Int = 0,
        val stationCount: Int = 0,
        val totalLengthM: Double = 0.0,
        val verticalRangeM: Double = 0.0,
        val hasSurveySql: Boolean = false,
        val hasPlanDrawing: Boolean = false,
        val hasProfileDrawing: Boolean = false,
        val hasImageOrPdfPreview: Boolean = false,
        val qcWarnings: List<String> = emptyList()
    )

    private val supportedExtensions = setOf(
        "zip", "tdr", "td", "th", "th2", "png", "jpg", "jpeg", "pdf", "svg", "dxf", "kml", "kmz", "csv", "gpx"
    )

    fun loadAll(context: Context): List<Attachment> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ATTACHMENTS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<Attachment>>() {}.type
            gson.fromJson<List<Attachment>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun loadForRecord(context: Context, recordId: String): List<Attachment> =
        loadAll(context).filter { it.objectId == recordId }.sortedByDescending { it.attachedAtMillis }

    fun saveAll(context: Context, items: List<Attachment>) {
        val json = gson.toJson(items)
        if (json.toByteArray(Charsets.UTF_8).size > 1_500_000) {
            android.util.Log.w("TopoDroidBridgeStore", "saveAll: payload too large (${json.length} chars), skipping write to protect existing data")
            return
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ATTACHMENTS, json).apply()
    }

    fun attachUri(context: Context, record: SpeleoRecord, uri: Uri, source: String): Attachment? {
        val filename = displayName(context, uri).ifBlank { uri.lastPathSegment.orEmpty().substringAfterLast('/') }
        if (!isSupported(filename)) return null
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val item = buildAttachment(
            context = context,
            objectId = record.id,
            objectName = record.name,
            plateNumber = record.condition.plate_number,
            cadastralNumber = record.cadastre.cadastral_number,
            uri = uri,
            originalFilename = filename.ifBlank { "TopoDroid export" },
            source = source
        )
        val current = loadAll(context).toMutableList()
        if (current.none { it.objectId == record.id && it.uri == item.uri }) {
            current.add(item)
            saveAll(context, current)
        }
        return item
    }

    fun attachImportedFile(
        context: Context,
        objectId: String,
        objectName: String,
        plateNumber: String?,
        cadastralNumber: String?,
        file: File,
        originalFilename: String,
        source: String
    ): Attachment? {
        if (!isSupported(originalFilename)) return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val item = buildAttachment(
            context = context,
            objectId = objectId,
            objectName = objectName,
            plateNumber = plateNumber,
            cadastralNumber = cadastralNumber,
            uri = uri,
            originalFilename = originalFilename,
            source = source
        )
        val current = loadAll(context).toMutableList()
        if (current.none { it.objectId == objectId && it.originalFilename == originalFilename && it.source == source }) {
            current.add(item)
            saveAll(context, current)
        }
        return item
    }

    private fun buildAttachment(
        context: Context,
        objectId: String,
        objectName: String,
        plateNumber: String?,
        cadastralNumber: String?,
        uri: Uri,
        originalFilename: String,
        source: String
    ): Attachment {
        val ext = extensionFromName(originalFilename)
        val analysis = analyzeUri(context, uri, originalFilename)
        return Attachment(
            id = "td_${System.currentTimeMillis()}_${abs(uri.toString().hashCode())}",
            objectId = objectId,
            objectName = objectName,
            plateNumber = plateNumber,
            cadastralNumber = cadastralNumber,
            uri = uri.toString(),
            originalFilename = originalFilename.ifBlank { "TopoDroid export" },
            fileType = ext.uppercase(Locale.ROOT),
            attachedAtMillis = System.currentTimeMillis(),
            source = source,
            surveyName = analysis.surveyName,
            surveyDate = analysis.surveyDate,
            surveyTeam = analysis.surveyTeam,
            shotCount = analysis.shotCount.takeIf { it > 0 },
            centerlineShotCount = analysis.centerlineShotCount.takeIf { it > 0 },
            splayShotCount = analysis.splayShotCount.takeIf { it > 0 },
            stationCount = analysis.stationCount.takeIf { it > 0 },
            totalLengthM = analysis.totalLengthM.takeIf { it > 0.0 },
            verticalRangeM = analysis.verticalRangeM.takeIf { it > 0.0 },
            hasSurveySql = analysis.hasSurveySql,
            hasPlanDrawing = analysis.hasPlanDrawing,
            hasProfileDrawing = analysis.hasProfileDrawing,
            hasImageOrPdfPreview = analysis.hasImageOrPdfPreview,
            qcWarnings = analysis.qcWarnings
        )
    }

    fun detach(context: Context, attachmentId: String) {
        saveAll(context, loadAll(context).filterNot { it.id == attachmentId })
    }

    fun scanTreeForRecord(context: Context, record: SpeleoRecord, treeUri: Uri): ScanResult {
    runCatching {
        context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    var scanned = 0
    var matched = 0
    var unsupported = 0

    fun scanDir(parentDocId: String, depth: Int) {
        if (depth > 3) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeIndex).orEmpty()
                val childDocId = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex).orEmpty()
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    scanDir(childDocId, depth + 1)
                    continue
                }
                scanned++
                if (!isSupported(name)) {
                    unsupported++
                    continue
                }
                if (filenameMatchesRecord(name, record)) {
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                    if (attachUri(context, record, childUri, "Folder scan") != null) matched++
                }
            }
        }
    }

    val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
    scanDir(rootDocId, 0)
    return ScanResult(scanned, matched, unsupported)
}

    fun displayName(context: Context, uri: Uri): String {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx).orEmpty()
            }
        }
        return uri.lastPathSegment.orEmpty().substringAfterLast('/')
    }

    fun isSupported(filename: String): Boolean = extensionFromName(filename) in supportedExtensions

    fun isMapCandidate(filename: String): Boolean = extensionFromName(filename) in setOf("kml", "kmz", "gpx")

    fun isTopoDroidPackage(filename: String): Boolean = extensionFromName(filename) in setOf("zip", "tdr", "td", "th", "th2")

    fun extensionFromName(filename: String): String = filename.substringAfterLast('.', "").lowercase(Locale.ROOT)

    fun filenameMatchesRecord(filename: String, record: SpeleoRecord): Boolean {
        val base = normalizeKey(filename.substringBeforeLast('.'))
        val candidates = listOfNotNull(
            record.condition.plate_number,
            record.cadastre.cadastral_number,
            record.name
        ).map(::normalizeKey).filter { it.length >= 2 }
        return candidates.any { candidate ->
            base == candidate || base.contains(candidate) || candidate.contains(base)
        }
    }

    fun normalizedMatchPreview(record: SpeleoRecord): String {
        val candidates = listOfNotNull(record.condition.plate_number, record.cadastre.cadastral_number, record.name)
            .map(::normalizeKey)
            .filter { it.isNotBlank() }
            .distinct()
        return candidates.joinToString(" / ")
    }

    fun buildAttachmentSummary(attachment: Attachment): String {
        val parts = mutableListOf<String>()
        attachment.surveyName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        attachment.surveyDate?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        attachment.totalLengthM?.let { parts.add("${formatMeters(it)} m") }
        attachment.verticalRangeM?.let { parts.add("VR ${formatMeters(it)} m") }
        attachment.stationCount?.let { parts.add("$it stanica") }
        attachment.shotCount?.let { parts.add("$it mjerenja") }
        return parts.joinToString(" • ").ifBlank { "${attachment.fileType} • ${attachment.source}" }
    }

    fun buildQcText(attachment: Attachment): String {
        val warnings = attachment.qcWarnings.orEmpty().filter { it.isNotBlank() }
        return if (warnings.isEmpty()) "✓ Osnovna provjera bez upozorenja" else warnings.joinToString("\n") { "⚠ $it" }
    }

    fun createObjectArchive(context: Context, record: SpeleoRecord, attachments: List<Attachment>): File {
        val outDir = File(context.cacheDir, "topodroid_object_archives").apply { mkdirs() }
        val outFile = File(outDir, sanitizeFileName("${record.name}_nacrti_topodroid") + ".zip")
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            zip.putNextEntry(ZipEntry("README.txt"))
            zip.write(buildObjectArchiveReadme(record, attachments).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("attachments.csv"))
            zip.write(buildAttachmentsCsv(attachments).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            attachments.forEachIndexed { index, attachment ->
                val baseName = sanitizeFileName("${index + 1}_${attachment.originalFilename}")
                runCatching {
                    openInputStream(context, attachment)?.use { input ->
                        zip.putNextEntry(ZipEntry("original/$baseName"))
                        input.copyTo(zip)
                        zip.closeEntry()
                    }
                }
                val measurementCsv = buildMeasurementsCsv(context, attachment)
                if (measurementCsv.isNotBlank()) {
                    zip.putNextEntry(ZipEntry("measurements/${baseName.substringBeforeLast('.')}_measurements.csv"))
                    zip.write(measurementCsv.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        }
        return outFile
    }

    fun buildMeasurementsCsv(context: Context, attachment: Attachment): String {
        val filename = attachment.originalFilename
        val uri = Uri.parse(attachment.uri)
        val ext = extensionFromName(filename)
        val sql = when (ext) {
            "zip" -> openInputStream(context, attachment)?.use { findSurveySqlInZip(it) }
            "sql" -> openInputStream(context, attachment)?.bufferedReader()?.use { it.readText() }
            else -> null
        } ?: return ""
        val shots = parseShots(sql)
        if (shots.isEmpty()) return ""
        return buildString {
            appendLine("from,to,length_m,bearing_deg,clino_deg,type")
            shots.forEach { shot ->
                appendLine(listOf(shot.from, shot.to, formatMeters(shot.length), formatMeters(shot.bearing), formatMeters(shot.clino), shot.type).joinToString(",") { csvEscape(it) })
            }
        }
    }

    fun buildViewerModel(context: Context, attachment: Attachment): SurveyViewerModel? {
        val filename = attachment.originalFilename
        val ext = extensionFromName(filename)
        val sql = when (ext) {
            "zip" -> openInputStream(context, attachment)?.use { findSurveySqlInZip(it) }
            "sql" -> openInputStream(context, attachment)?.bufferedReader()?.use { it.readText() }
            else -> null
        } ?: return null
        return buildViewerModelFromSql(sql)
    }

    fun georeferenceToLayer(
        context: Context,
        attachment: Attachment,
        entranceLat: Double,
        entranceLon: Double
    ): com.darko.speleov1.ImportedLayer? {
        val viewerModel = buildViewerModel(context, attachment) ?: return null
        if (viewerModel.centerline.isEmpty()) return null

        // SOV XY: X = East (positive right in plan), Y = North (positive up in plan)
        // Conversion: 1 degree lat ≈ 111_320 m, 1 degree lon ≈ 111_320 * cos(lat) m
        val cosLat = kotlin.math.cos(Math.toRadians(entranceLat))
        val metersPerDegreeLat = 111_320.0
        val metersPerDegreeLon = 111_320.0 * cosLat

        // First station = origin (0,0) = entrance
        val originStation = viewerModel.stations.firstOrNull() ?: return null

        fun stationToGeoPoint(x: Double, y: Double): org.osmdroid.util.GeoPoint {
            val dxM = x - originStation.x
            val dyM = y - originStation.y
            val lat = entranceLat + dyM / metersPerDegreeLat
            val lon = entranceLon + dxM / metersPerDegreeLon
            return org.osmdroid.util.GeoPoint(lat, lon)
        }

        val stationByName = viewerModel.stations.associateBy { it.name }
        val segmentTracks = viewerModel.centerline.mapIndexedNotNull { index, segment ->
            val fromStation = stationByName[segment.from]
            val toStation = stationByName[segment.to]
            if (fromStation == null || toStation == null) {
                null
            } else {
                com.darko.speleov1.SavedTrack(
                    id = "topo_${attachment.id}_seg_$index",
                    name = "${attachment.objectName} — topo centerline",
                    description = "Orijentacijski TopoDroid centerline segment. Ulaz je sidren na koordinate objekta.",
                    createdAtMillis = System.currentTimeMillis(),
                    points = listOf(
                        com.darko.speleov1.TrackPoint(
                            point = stationToGeoPoint(fromStation.x, fromStation.y),
                            altitudeM = fromStation.z.takeIf { it != 0.0 }
                        ),
                        com.darko.speleov1.TrackPoint(
                            point = stationToGeoPoint(toStation.x, toStation.y),
                            altitudeM = toStation.z.takeIf { it != 0.0 }
                        )
                    ),
                    visible = true
                )
            }
        }

        if (segmentTracks.isEmpty()) return null

        val stationPoints = viewerModel.stations.take(180).mapIndexed { index, station ->
            val geo = stationToGeoPoint(station.x, station.y)
            com.darko.speleov1.MarkedPoint(
                id = "topo_${attachment.id}_station_$index",
                name = station.name.ifBlank { "S${index + 1}" },
                type = "topodroid_station",
                description = "TopoDroid survey station • Z ${String.format(java.util.Locale.US, "%.1f", station.z)} m • chain ${String.format(java.util.Locale.US, "%.1f", station.chain)} m",
                lat = geo.latitude,
                lon = geo.longitude,
                htrsX = 0.0,
                htrsY = 0.0,
                visible = true
            )
        }

        return com.darko.speleov1.ImportedLayer(
            id = "topo_layer_${attachment.id}",
            name = "${attachment.objectName} — topo centerline",
            type = "topodroid",
            visible = true,
            createdAtMillis = System.currentTimeMillis(),
            points = stationPoints,
            tracks = segmentTracks
        )
    }

    private fun buildViewerModelFromSql(sql: String): SurveyViewerModel? {
        val analysis = analyzeSurveySql(sql)
        val shots = parseShots(sql)
        val centerlineShots = shots.filter { it.from.isNotBlank() && it.to.isNotBlank() && it.length > 0.0 }

        if (centerlineShots.isEmpty()) {
            return SurveyViewerModel(
                surveyName = analysis.surveyName,
                surveyDate = analysis.surveyDate,
                surveyTeam = analysis.surveyTeam,
                stations = emptyList(),
                centerline = emptyList(),
                splays = emptyList(),
                totalLengthM = analysis.totalLengthM,
                verticalRangeM = analysis.verticalRangeM,
                qcWarnings = analysis.qcWarnings + "Nema dovoljno centerline mjerenja za prikaz plana/profila"
            )
        }

        data class WorkStation(val x: Double, val y: Double, val z: Double, val chain: Double)

        val stations = linkedMapOf<String, WorkStation>()
        centerlineShots.firstOrNull()?.from?.takeIf { it.isNotBlank() }?.let {
            stations[it] = WorkStation(0.0, 0.0, 0.0, 0.0)
        }

        repeat(centerlineShots.size + 2) {
            centerlineShots.forEach { shot ->
                val from = stations[shot.from]
                val to = stations[shot.to]
                val horizontal = shot.length * cos(Math.toRadians(shot.clino))
                val dx = horizontal * sin(Math.toRadians(shot.bearing))
                val dy = -horizontal * cos(Math.toRadians(shot.bearing))
                val dz = shot.length * sin(Math.toRadians(shot.clino))
                when {
                    from != null && to == null -> {
                        stations[shot.to] = WorkStation(
                            x = from.x + dx,
                            y = from.y + dy,
                            z = from.z + dz,
                            chain = from.chain + shot.length
                        )
                    }
                    from == null && to != null -> {
                        stations[shot.from] = WorkStation(
                            x = to.x - dx,
                            y = to.y - dy,
                            z = to.z - dz,
                            chain = (to.chain - shot.length).coerceAtLeast(0.0)
                        )
                    }
                }
            }
        }

        val stationViews = stations.map { (name, point) ->
            SurveyStationView(name = name, x = point.x, y = point.y, z = point.z, chain = point.chain)
        }

        val centerSegments = centerlineShots.mapNotNull { shot ->
            val from = stations[shot.from]
            val to = stations[shot.to]
            if (from == null || to == null) {
                null
            } else {
                SurveySegmentView(
                    from = shot.from,
                    to = shot.to,
                    x1 = from.x,
                    y1 = from.y,
                    z1 = from.z,
                    chain1 = from.chain,
                    x2 = to.x,
                    y2 = to.y,
                    z2 = to.z,
                    chain2 = to.chain,
                    length = shot.length,
                    bearing = shot.bearing,
                    clino = shot.clino,
                    type = "centerline"
                )
            }
        }

        val splaySegments = shots
            .filter { it.type == "splay" && it.from.isNotBlank() && it.length > 0.0 }
            .mapNotNull { shot ->
                val from = stations[shot.from] ?: return@mapNotNull null
                val horizontal = shot.length * cos(Math.toRadians(shot.clino))
                val dx = horizontal * sin(Math.toRadians(shot.bearing))
                val dy = -horizontal * cos(Math.toRadians(shot.bearing))
                val dz = shot.length * sin(Math.toRadians(shot.clino))
                SurveySegmentView(
                    from = shot.from,
                    to = shot.to.ifBlank { "splay" },
                    x1 = from.x,
                    y1 = from.y,
                    z1 = from.z,
                    chain1 = from.chain,
                    x2 = from.x + dx,
                    y2 = from.y + dy,
                    z2 = from.z + dz,
                    chain2 = from.chain + shot.length,
                    length = shot.length,
                    bearing = shot.bearing,
                    clino = shot.clino,
                    type = "splay"
                )
            }

        return SurveyViewerModel(
            surveyName = analysis.surveyName,
            surveyDate = analysis.surveyDate,
            surveyTeam = analysis.surveyTeam,
            stations = stationViews,
            centerline = centerSegments,
            splays = splaySegments,
            totalLengthM = analysis.totalLengthM,
            verticalRangeM = analysis.verticalRangeM,
            qcWarnings = analysis.qcWarnings
        )
    }

    fun openInputStream(context: Context, attachment: Attachment): InputStream? =
        runCatching { context.contentResolver.openInputStream(Uri.parse(attachment.uri)) }.getOrNull()

    private fun analyzeUri(context: Context, uri: Uri, filename: String): TopoDroidAnalysis {
        val ext = extensionFromName(filename)
        return runCatching {
            when (ext) {
                "zip" -> context.contentResolver.openInputStream(uri)?.use { analyzeZip(it) } ?: TopoDroidAnalysis(qcWarnings = listOf("Ne mogu pročitati ZIP"))
                "tdr" -> TopoDroidAnalysis(hasPlanDrawing = filename.lowercase(Locale.ROOT).contains("p.tdr") || filename.lowercase(Locale.ROOT).contains("-p"), hasProfileDrawing = filename.lowercase(Locale.ROOT).contains("s.tdr") || filename.lowercase(Locale.ROOT).contains("-s"), qcWarnings = listOf("TDR crtež je spremljen, puni render ovisi o TopoDroid exportu"))
                "png", "jpg", "jpeg", "pdf", "svg" -> TopoDroidAnalysis(hasImageOrPdfPreview = true)
                else -> TopoDroidAnalysis()
            }
        }.getOrElse { TopoDroidAnalysis(qcWarnings = listOf("Ne mogu analizirati datoteku: ${it.message.orEmpty()}")) }
    }

    private fun analyzeZip(input: InputStream): TopoDroidAnalysis {
        var surveySql: String? = null
        var hasPlan = false
        var hasProfile = false
        var hasPreview = false
        val entryNames = mutableListOf<String>()
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                val lower = name.lowercase(Locale.ROOT)
                entryNames.add(entry.name)
                if (!entry.isDirectory) {
                    if (lower == "survey.sql" || lower.endsWith("/survey.sql")) {
                        surveySql = zip.readBytes().toString(Charsets.UTF_8)
                    } else if (lower.endsWith(".tdr")) {
                        if (lower.contains("-1p") || lower.contains("_p") || lower.contains("plan") || lower.contains("p.tdr")) hasPlan = true
                        if (lower.contains("-1s") || lower.contains("_s") || lower.contains("section") || lower.contains("profil") || lower.contains("s.tdr")) hasProfile = true
                    } else if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".pdf") || lower.endsWith(".svg")) {
                        hasPreview = true
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val sql = surveySql
        val base = if (sql != null) analyzeSurveySql(sql) else TopoDroidAnalysis(qcWarnings = listOf("ZIP nema survey.sql — spremljen je samo kao originalni paket"))
        val warnings = base.qcWarnings.toMutableList()
        if (!hasPreview && (hasPlan || hasProfile)) warnings.add("Paket ima TDR crtež, ali nema PNG/PDF/SVG preview za brzi prikaz")
        if (!hasPlan && entryNames.any { it.lowercase(Locale.ROOT).endsWith(".tdr") }) warnings.add("Nije sigurno prepoznat plan crtež")
        return base.copy(
            hasSurveySql = sql != null,
            hasPlanDrawing = hasPlan,
            hasProfileDrawing = hasProfile,
            hasImageOrPdfPreview = hasPreview,
            qcWarnings = warnings.distinct()
        )
    }

    private fun findSurveySqlInZip(input: InputStream): String? {
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val lower = entry.name.lowercase(Locale.ROOT)
                if (!entry.isDirectory && (lower == "survey.sql" || lower.endsWith("/survey.sql"))) {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun analyzeSurveySql(sql: String): TopoDroidAnalysis {
        val surveyValues = Regex("INSERT\\s+into\\s+surveys\\s+values\\s*\\((.*?)\\)\\s*;", RegexOption.IGNORE_CASE)
            .find(sql)?.groupValues?.getOrNull(1)?.let(::splitSqlValues)
        val surveyName = surveyValues?.getOrNull(1)?.trimSql()
        val surveyDate = surveyValues?.getOrNull(2)?.trimSql()
        val surveyTeam = surveyValues?.getOrNull(3)?.trimSql()
        val shots = parseShots(sql)
        val centerline = shots.filter { it.from.isNotBlank() && it.to.isNotBlank() }
        val stationSet = centerline.flatMap { listOf(it.from, it.to) }.filter { it.isNotBlank() }.toSet()
        val totalLength = centerline.sumOf { it.length }
        val stationZ = mutableMapOf<String, Double>()
        centerline.firstOrNull()?.from?.let { stationZ[it] = 0.0 }
        centerline.forEach { shot ->
            val fromZ = stationZ[shot.from]
            if (fromZ != null && shot.to.isNotBlank() && stationZ[shot.to] == null) {
                stationZ[shot.to] = fromZ + shot.length * sin(Math.toRadians(shot.clino))
            }
        }
        val verticalRange = if (stationZ.isNotEmpty()) stationZ.values.maxOrNull()!! - stationZ.values.minOrNull()!! else 0.0
        val duplicatePairs = centerline.groupBy { "${it.from}→${it.to}" }.filter { it.value.size > 1 }.keys
        val warnings = mutableListOf<String>()
        if (shots.isEmpty()) warnings.add("Nema mjerenja u survey.sql")
        if (centerline.isEmpty() && shots.isNotEmpty()) warnings.add("Nema jasnih centerline mjerenja — većina su splay/pomoćna mjerenja")
        if (duplicatePairs.isNotEmpty()) warnings.add("Ponovljena centerline mjerenja: ${duplicatePairs.take(3).joinToString()}")
        if (stationSet.size > 80 || (centerline.size > 0 && centerline.size.toDouble() / stationSet.size.coerceAtLeast(1) > 3.0)) warnings.add("Složena topologija — plan može biti netočan za granate jame s petljama")
        return TopoDroidAnalysis(
            surveyName = surveyName,
            surveyDate = surveyDate,
            surveyTeam = surveyTeam,
            shotCount = shots.size,
            centerlineShotCount = centerline.size,
            splayShotCount = shots.size - centerline.size,
            stationCount = stationSet.size,
            totalLengthM = totalLength,
            verticalRangeM = abs(verticalRange),
            hasSurveySql = true,
            qcWarnings = warnings.distinct()
        )
    }

    private fun parseShots(sql: String): List<Shot> {
        val rows = Regex("INSERT\\s+into\\s+shots\\s+values\\s*\\((.*?)\\)\\s*;", RegexOption.IGNORE_CASE)
            .findAll(sql)
            .mapNotNull { match ->
                val values = splitSqlValues(match.groupValues[1])
                val from = values.getOrNull(2)?.trimSql().orEmpty()
                val to = values.getOrNull(3)?.trimSql().orEmpty()
                val length = values.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                val bearing = values.getOrNull(5)?.toDoubleOrNull() ?: 0.0
                val clino = values.getOrNull(6)?.toDoubleOrNull() ?: 0.0
                if (length <= 0.0) null else Shot(from, to, length, bearing, clino, if (from.isNotBlank() && to.isNotBlank()) "centerline" else "splay")
            }
            .toList()
        return rows
    }

    private fun splitSqlValues(raw: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var inDoubleQuote = false
        var inSingleQuote = false
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            when {
                c == '"' && !inSingleQuote -> {
                    inDoubleQuote = !inDoubleQuote
                    current.append(c)
                }
                c == '\'' && !inDoubleQuote -> {
                    inSingleQuote = !inSingleQuote
                    current.append(c)
                }
                c == ',' && !inDoubleQuote && !inSingleQuote -> {
                    out.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        out.add(current.toString().trim())
        return out
    }

    private fun String.trimSql(): String {
        val t = trim()
        return when {
            t.startsWith("\"") && t.endsWith("\"") -> t.removeSurrounding("\"").trim()
            t.startsWith("'") && t.endsWith("'") -> t.removeSurrounding("'").trim()
            else -> t
        }
    }

    private fun buildObjectArchiveReadme(record: SpeleoRecord, attachments: List<Attachment>): String = buildString {
        appendLine("SOV arhiva nacrta / TopoDroid")
        appendLine("Objekt: ${record.name}")
        record.condition.plate_number?.takeIf { it.isNotBlank() }?.let { appendLine("Pločica: $it") }
        appendLine("Broj povezanih fileova: ${attachments.size}")
        appendLine()
        attachments.forEachIndexed { index, attachment ->
            appendLine("${index + 1}. ${attachment.originalFilename}")
            appendLine("   ${buildAttachmentSummary(attachment)}")
            appendLine("   QC: ${buildQcText(attachment).replace("\n", " | ")}")
        }
    }

    private fun buildAttachmentsCsv(attachments: List<Attachment>): String = buildString {
        appendLine("filename,type,source,survey_name,survey_date,team,shots,centerline,splays,stations,total_length_m,vertical_range_m,qc")
        attachments.forEach { a ->
            val row = listOf(
                a.originalFilename,
                a.fileType,
                a.source,
                a.surveyName.orEmpty(),
                a.surveyDate.orEmpty(),
                a.surveyTeam.orEmpty(),
                a.shotCount?.toString().orEmpty(),
                a.centerlineShotCount?.toString().orEmpty(),
                a.splayShotCount?.toString().orEmpty(),
                a.stationCount?.toString().orEmpty(),
                a.totalLengthM?.let(::formatMeters).orEmpty(),
                a.verticalRangeM?.let(::formatMeters).orEmpty(),
                a.qcWarnings.orEmpty().joinToString(" | ")
            )
            appendLine(row.joinToString(",") { csvEscape(it) })
        }
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(',') || escaped.contains('\n') || escaped.contains('"')) "\"$escaped\"" else escaped
    }

    private fun sanitizeFileName(raw: String): String = raw.trim()
        .replace(Regex("[\\/:*?\"<>|]"), "_")
        .replace(Regex("\\s+"), "_")
        .ifBlank { "topodroid_export" }

    private fun formatMeters(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun normalizeKey(value: String): String {
        val noAccents = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return noAccents
            .replace("đ", "d")
            .replace("[^a-z0-9]+".toRegex(), "")
            .trim()
    }
}
