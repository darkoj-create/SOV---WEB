package com.darko.speleov1.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.Keep
import androidx.core.content.FileProvider
import com.darko.speleov1.model.SpeleoRecord
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Keep
data class DriveDrawingIndexResponse(
    @SerializedName("ok") val ok: Boolean = true,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("folderId") val folderId: String? = null,
    @SerializedName("drawings") val drawings: List<DriveDrawing> = emptyList(),
    @SerializedName("count") val count: Int? = null,
    @SerializedName("totalCount") val totalCount: Int? = null,
    @SerializedName("indexBuiltAt") val indexBuiltAt: String? = null,
    @SerializedName("error") val error: String? = null
)

@Keep
data class DriveDrawing(
    @SerializedName("fileId") val fileId: String = "",
    @SerializedName("fileName") val fileName: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null,
    @SerializedName("sizeBytes") val sizeBytes: Long? = null,
    @SerializedName("modifiedTime") val modifiedTime: String? = null,
    @SerializedName("webViewUrl") val webViewUrl: String? = null,
    @SerializedName("downloadUrl") val downloadUrl: String? = null,
    @SerializedName("recordId") val recordId: String? = null,
    @SerializedName("katastarId") val katastarId: String? = null,
    @SerializedName("objectName") val objectName: String? = null,
    @SerializedName("detectedObjectName") val detectedObjectName: String? = null,
    @SerializedName("detectedKatastarNumber") val detectedKatastarNumber: String? = null,
    @SerializedName("detectedCadastralNumber") val detectedCadastralNumber: String? = null,
    @SerializedName("detectedTile") val detectedTile: String? = null,
    @SerializedName("detectedLocation") val detectedLocation: String? = null,
    @SerializedName("extractionStatus") val extractionStatus: String? = null,
    @SerializedName("extractedTextPreview") val extractedTextPreview: String? = null,
    @SerializedName("matchStatus") val matchStatus: String? = null,
    @SerializedName("notes") val notes: String? = null
) {
    val displayName: String get() = fileName.ifBlank { name.orEmpty() }
    val extension: String
        get() = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val isPdf: Boolean
        get() = extension == "pdf" || mimeType.equals("application/pdf", ignoreCase = true)
    val isImage: Boolean
        get() = mimeType?.startsWith("image/", ignoreCase = true) == true || extension in IMAGE_EXTENSIONS
    val resolvedMimeType: String
        get() = when (extension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "tif", "tiff" -> "image/tiff"
            "webp" -> "image/webp"
            else -> mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        }
    val drawingKindLabel: String
        get() = when {
            isPdf -> "PDF nacrt"
            extension in setOf("tif", "tiff") -> "TIFF nacrt"
            isImage -> "Slika nacrta"
            else -> "Nacrt"
        }

    companion object {
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "tif", "tiff", "webp")
    }
}

data class DriveDrawingMatch(
    val drawing: DriveDrawing,
    val confidence: Double,
    val status: String,
    val reason: String
)

data class DriveDrawingLookupResult(
    val ok: Boolean = false,
    val matches: List<DriveDrawingMatch> = emptyList(),
    val totalCount: Int? = null,
    val error: String? = null
)

object DriveDrawingsRepository {
    private const val PREFS = "sov_drive_drawings_prefs"
    private const val KEY_WEBAPP_URL = "drawings_webapp_url"
    private const val INDEX_CACHE_FILE = "drive_drawings_index_cache.json"
    private const val DEFAULT_TIMEOUT_MS = 20_000

    const val DEFAULT_FOLDER_ID: String = "1vCPsPaznDOgwRMU_XVDiI4aUhu0o8yFB"
    const val DEFAULT_WEBAPP_URL: String = "https://script.google.com/macros/s/AKfycbx1Hg_s6mAdWgB7p559USC8dAMIhteJQ3RFhFgp8rkqzYEVqMfwZm-lrl2v7UmW8gvSyg/exec"

    fun loadWebAppUrl(context: Context): String {
        // The drawings index endpoint is now fixed/built-in so old saved manual URLs
        // cannot override the current deployment.
        return DEFAULT_WEBAPP_URL
    }

    fun saveWebAppUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_WEBAPP_URL, url.trim())
            .apply()
    }

    fun clearWebAppUrl(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_WEBAPP_URL).apply()
    }

    fun cacheFile(context: Context): File = File(context.filesDir, INDEX_CACHE_FILE)

    fun drawingsDir(context: Context): File = File(context.filesDir, "Offline/nacrti").apply { mkdirs() }

    private fun userDrawingsDir(context: Context, record: SpeleoRecord): File {
        val safeRecord = sanitizeBasicFilename(record.name.ifBlank { record.id }.ifBlank { "objekt" }).ifBlank { "objekt" }
        return File(drawingsDir(context), "moji_nacrti/$safeRecord").apply { mkdirs() }
    }

    fun loadUserDrawings(context: Context, record: SpeleoRecord): List<DriveDrawing> {
        val dir = userDrawingsDir(context, record)
        return dir.listFiles()
            ?.filter { it.isFile && isSupportedDrawingExtension(it.extension) }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) }
            ?.map { file ->
                DriveDrawing(
                    fileId = "local:${record.id}:${file.name}",
                    fileName = file.name,
                    name = file.name,
                    mimeType = mimeForExtension(file.extension),
                    sizeBytes = file.length(),
                    modifiedTime = java.util.Date(file.lastModified()).toInstant().toString(),
                    webViewUrl = null,
                    downloadUrl = file.toURI().toString(),
                    recordId = record.id,
                    objectName = record.name,
                    detectedObjectName = record.name,
                    matchStatus = "verified",
                    notes = "Korisnički dodan nacrt"
                )
            }
            .orEmpty()
    }

    fun importUserDrawing(context: Context, record: SpeleoRecord, uri: Uri): DriveDrawing {
        val displayName = queryDisplayName(context, uri).ifBlank { "nacrt_${System.currentTimeMillis()}" }
        val extension = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (!isSupportedDrawingExtension(extension)) {
            throw IllegalArgumentException("Format nije podržan. Podržani su JPG, PNG, TIFF, WEBP i PDF nacrti.")
        }
        val safeName = uniqueFilename(userDrawingsDir(context, record), sanitizeBasicFilename(displayName).ifBlank { "nacrt.$extension" })
        val target = File(userDrawingsDir(context, record), safeName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Ne mogu pročitati odabrani nacrt.")
        return DriveDrawing(
            fileId = "local:${record.id}:${target.name}",
            fileName = target.name,
            name = target.name,
            mimeType = mimeForExtension(target.extension),
            sizeBytes = target.length(),
            modifiedTime = java.util.Date(target.lastModified()).toInstant().toString(),
            webViewUrl = null,
            downloadUrl = target.toURI().toString(),
            recordId = record.id,
            objectName = record.name,
            detectedObjectName = record.name,
            matchStatus = "verified",
            notes = "Korisnički dodan nacrt"
        )
    }

    fun isSupportedDrawingExtension(extension: String): Boolean = extension.lowercase(Locale.ROOT) in setOf("jpg", "jpeg", "png", "tif", "tiff", "webp", "pdf")

    fun mimeForExtension(extension: String): String = when (extension.lowercase(Locale.ROOT)) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "tif", "tiff" -> "image/tiff"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }

    private fun queryDisplayName(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx).orEmpty() else ""
            } else ""
        }.orEmpty()
    }.getOrDefault("")

    private fun uniqueFilename(dir: File, preferred: String): String {
        val base = preferred.substringBeforeLast('.', preferred)
        val ext = preferred.substringAfterLast('.', "")
        var candidate = preferred
        var counter = 2
        while (File(dir, candidate).exists()) {
            candidate = if (ext.isBlank()) "${base}_$counter" else "${base}_$counter.$ext"
            counter++
        }
        return candidate
    }

    private fun sanitizeBasicFilename(value: String): String = value
        .replace(Regex("[\\/:*?\"<>|]+"), "_")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(140)

    fun loadCachedIndex(context: Context): DriveDrawingIndexResponse? = runCatching {
        val file = cacheFile(context)
        if (!file.exists()) return null
        Gson().fromJson(file.readText(), DriveDrawingIndexResponse::class.java)
    }.getOrNull()

    fun saveCachedIndex(context: Context, rawJson: String) {
        runCatching { cacheFile(context).writeText(rawJson) }
    }

    suspend fun fetchMatchesForRecord(context: Context, record: SpeleoRecord, forceNetwork: Boolean = false): DriveDrawingLookupResult {
        val webAppUrl = loadWebAppUrl(context)
        if (webAppUrl.isBlank()) {
            return DriveDrawingLookupResult(ok = false, error = "Nije postavljen Apps Script /exec URL za nacrte.")
        }
        return try {
            val separator = if (webAppUrl.contains("?")) "&" else "?"
            val query = URLEncoder.encode(record.name.ifBlank { record.id }, "UTF-8")
            val plate = URLEncoder.encode(record.condition.plate_number.orEmpty(), "UTF-8")
            val recordId = URLEncoder.encode(record.id, "UTF-8")
            val forceParams = if (forceNetwork) "&force=1&_=${System.currentTimeMillis()}" else ""
            val url = webAppUrl.trim() + separator + "action=searchDrawings&objectName=" + query + "&recordId=" + recordId + "&plate=" + plate + "&limit=30" + forceParams
            val raw = httpGet(url)
            val parsed = Gson().fromJson(raw, DriveDrawingIndexResponse::class.java)
                ?: DriveDrawingIndexResponse(ok = false, error = "Ne mogu pročitati odgovor nacrta.")
            if (!parsed.ok) {
                DriveDrawingLookupResult(ok = false, error = parsed.error ?: "Index nacrta nije dostupan.")
            } else {
                val matches = findMatches(record, parsed.drawings, limit = 10)
                DriveDrawingLookupResult(
                    ok = true,
                    matches = matches,
                    totalCount = parsed.totalCount ?: parsed.count,
                    error = null
                )
            }
        } catch (err: Exception) {
            DriveDrawingLookupResult(ok = false, error = err.message ?: "Ne mogu dohvatiti nacrte za objekt.")
        }
    }

    suspend fun fetchDrawingStats(context: Context): DriveDrawingIndexResponse? {
        val webAppUrl = loadWebAppUrl(context)
        if (webAppUrl.isBlank()) return null
        return runCatching {
            val separator = if (webAppUrl.contains("?")) "&" else "?"
            val raw = httpGet(webAppUrl.trim() + separator + "action=stats")
            Gson().fromJson(raw, DriveDrawingIndexResponse::class.java)
        }.getOrNull()
    }

    suspend fun fetchIndex(context: Context, forceNetwork: Boolean = false): DriveDrawingIndexResponse {
        val webAppUrl = loadWebAppUrl(context)
        if (webAppUrl.isBlank()) {
            return loadCachedIndex(context) ?: DriveDrawingIndexResponse(ok = false, error = "Nije postavljen Apps Script /exec URL za nacrte.")
        }
        if (!forceNetwork) {
            loadCachedIndex(context)?.let { cached ->
                if (cached.drawings.isNotEmpty()) return cached
            }
        }
        val separator = if (webAppUrl.contains("?")) "&" else "?"
        val forceParams = if (forceNetwork) "&force=1&_=${System.currentTimeMillis()}" else ""
        val url = webAppUrl.trim() + separator + "action=listDrawings&extractText=0&fast=1&formats=images,pdf" + forceParams
        val raw = httpGet(url)
        saveCachedIndex(context, raw)
        return Gson().fromJson(raw, DriveDrawingIndexResponse::class.java)
            ?: DriveDrawingIndexResponse(ok = false, error = "Ne mogu pročitati index nacrta.")
    }

    fun findMatches(record: SpeleoRecord, drawings: List<DriveDrawing>, limit: Int = 10): List<DriveDrawingMatch> {
        val recordTokens = normalizedTokens(record.name)
        val recordNameNormalized = normalizeForMatch(record.name)
        val cadastre = record.cadastre.cadastral_number.orEmpty().trim()
        val plate = record.condition.plate_number.orEmpty().trim()
        val locationTokens = recordLocationTokens(record)
        val recordId = record.id.trim()
        val candidates = drawings.mapNotNull { drawing ->
            scoreDrawing(record, recordId, cadastre, plate, recordNameNormalized, recordTokens, locationTokens, drawing)
        }.sortedWith(compareByDescending<DriveDrawingMatch> { it.confidence }.thenBy { it.drawing.displayName.lowercase(Locale.ROOT) })
        return candidates.take(limit)
    }

    fun localFileFor(context: Context, drawing: DriveDrawing): File {
        if (drawing.fileId.startsWith("local:") && drawing.downloadUrl?.startsWith("file:") == true) {
            return File(Uri.parse(drawing.downloadUrl).path.orEmpty())
        }
        val baseName = drawing.displayName.ifBlank { drawing.fileId.ifBlank { "nacrt" } }
        val safe = sanitizeFilename(baseName, drawing).ifBlank { "nacrt_${drawing.fileId}.${drawing.extension.ifBlank { "bin" }}" }
        return File(drawingsDir(context), safe)
    }

    fun hasLocalFile(context: Context, drawing: DriveDrawing): Boolean = localFileFor(context, drawing).exists()

    suspend fun downloadDrawing(context: Context, drawing: DriveDrawing): File {
        val target = localFileFor(context, drawing)
        target.parentFile?.mkdirs()
        val url = drawing.downloadUrl?.takeIf { it.isNotBlank() }
            ?: "https://drive.google.com/uc?export=download&id=${drawing.fileId}"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = DEFAULT_TIMEOUT_MS
            readTimeout = DEFAULT_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Download nije uspio: HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            return target
        } finally {
            connection.disconnect()
        }
    }

    fun openLocalDrawing(context: Context, drawing: DriveDrawing): Boolean {
        val file = localFileFor(context, drawing)
        if (!file.exists()) return false
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, drawing.resolvedMimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Otvori nacrt"))
            true
        }.getOrDefault(false)
    }

    fun openDrive(context: Context, drawing: DriveDrawing): Boolean {
        if (drawing.fileId.startsWith("local:")) return openLocalDrawing(context, drawing)
        val url = drawing.webViewUrl?.takeIf { it.isNotBlank() }
            ?: drawing.fileId.takeIf { it.isNotBlank() }?.let { "https://drive.google.com/file/d/$it/view" }
            ?: return false
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = DEFAULT_TIMEOUT_MS
            readTimeout = DEFAULT_TIMEOUT_MS
            requestMethod = "GET"
        }
        try {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${connection.responseCode}: $body")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun scoreDrawing(
        record: SpeleoRecord,
        recordId: String,
        cadastre: String,
        plate: String,
        recordNameNormalized: String,
        recordTokens: Set<String>,
        locationTokens: Set<String>,
        drawing: DriveDrawing
    ): DriveDrawingMatch? {
        val status = drawing.matchStatus.orEmpty().lowercase(Locale.ROOT)
        val drawingRecordId = drawing.recordId.orEmpty().trim()
        if (drawingRecordId.isNotBlank() && drawingRecordId.equals(recordId, ignoreCase = true)) {
            return DriveDrawingMatch(drawing, 1.0, "verified", "recordId")
        }
        if (status == "rejected") return null

        val drawingNames = listOfNotNull(
            drawing.objectName?.takeIf { it.isNotBlank() },
            drawing.detectedObjectName?.takeIf { it.isNotBlank() },
            drawing.displayName.takeIf { it.isNotBlank() }
        ).distinct()
        val drawingNameForDisplay = drawingNames.joinToString(" ")
        val drawingNormalized = normalizeForMatch(drawingNameForDisplay)
        val fileTokens = normalizedTokens(drawingNameForDisplay)
        if (recordNameNormalized.isBlank() || drawingNormalized.isBlank() || recordTokens.isEmpty() || fileTokens.isEmpty()) return null

        // Ime je glavni signal. Metadata iz PDF-a smije samo pojačati ili objasniti match,
        // ali ne smije sakriti nacrt koji ima slično ime.
        val nameScore = nameSimilarityScore(recordNameNormalized, recordTokens, drawingNormalized, fileTokens)
        val drawingKatastar = firstNonBlank(
            drawing.katastarId,
            drawing.detectedKatastarNumber,
            drawing.detectedCadastralNumber
        )
        val cadastreMatch = idsEquivalent(cadastre, drawingKatastar)
        val tileMatch = idsEquivalent(plate, drawing.detectedTile)
        val locationMatch = hasLocationOverlap(locationTokens, drawing.detectedLocation)

        val minimumNameScore = when {
            recordTokens.size <= 1 -> 0.42
            recordTokens.size == 2 -> 0.36
            else -> 0.32
        }
        val hasStrongMetadata = cadastreMatch || tileMatch || locationMatch
        if (nameScore < minimumNameScore && !(hasStrongMetadata && nameScore >= 0.24)) return null

        val metadataBoost = when {
            cadastreMatch -> 0.20
            tileMatch && locationMatch -> 0.12
            tileMatch -> 0.09
            locationMatch -> 0.06
            else -> 0.0
        }
        val statusBoost = when (status) {
            "verified" -> 0.10
            "possible" -> 0.04
            else -> 0.0
        }
        val confidence = (nameScore + metadataBoost + statusBoost).coerceIn(0.0, 1.0)

        val outStatus = when {
            status == "verified" && nameScore >= minimumNameScore -> "verified"
            nameScore >= 0.90 -> "verified"
            nameScore >= 0.68 && hasStrongMetadata -> "verified"
            confidence >= 0.78 -> "possible"
            else -> "possible"
        }
        val metadataReason = when {
            cadastreMatch -> " + evidencijski broj"
            tileMatch && locationMatch -> " + pločica/lokacija"
            tileMatch -> " + pločica"
            locationMatch -> " + lokacija"
            else -> ""
        }
        val baseReason = when {
            drawing.detectedObjectName?.isNotBlank() == true -> "slično ime iz metapodataka"
            else -> "slično ime datoteke"
        }
        return DriveDrawingMatch(drawing, confidence, outStatus, baseReason + metadataReason)
    }

    private fun normalizedTokens(value: String): Set<String> {
        val rawTokens = normalizeForMatch(value)
            .split(' ')
            .map { it.trim() }
            .filter { it.length >= 2 }
        val meaningful = rawTokens.filter { it !in STOP_WORDS }.toSet()
        return if (meaningful.isNotEmpty()) meaningful else rawTokens.toSet()
    }
    private fun nameSimilarityScore(
        recordNameNormalized: String,
        recordTokens: Set<String>,
        drawingNormalized: String,
        fileTokens: Set<String>
    ): Double {
        val tokenOverlap = recordTokens.intersect(fileTokens).size.toDouble() / max(recordTokens.size, 1)
        val reverseOverlap = fileTokens.intersect(recordTokens).size.toDouble() / max(fileTokens.size, 1)
        val containsBoost = when {
            drawingNormalized == recordNameNormalized -> 1.0
            drawingNormalized.contains(recordNameNormalized) || recordNameNormalized.contains(drawingNormalized) -> 0.94
            else -> 0.0
        }
        val distanceScore = normalizedSimilarity(recordNameNormalized, drawingNormalized)
        return maxOf(containsBoost, tokenOverlap * 0.72 + reverseOverlap * 0.18 + distanceScore * 0.10)
    }

    private fun recordLocationTokens(record: SpeleoRecord): Set<String> = listOfNotNull(
        record.location.locality,
        record.location.nearest_place,
        record.location.municipality,
        record.location.county,
        record.location.island
    ).flatMap { normalizedTokens(it) }.toSet()

    private fun hasLocationOverlap(recordLocationTokens: Set<String>, detectedLocation: String?): Boolean {
        if (recordLocationTokens.isEmpty() || detectedLocation.isNullOrBlank()) return false
        val detectedTokens = normalizedTokens(detectedLocation)
        if (detectedTokens.isEmpty()) return false
        return recordLocationTokens.intersect(detectedTokens).isNotEmpty()
    }

    private fun firstNonBlank(vararg values: String?): String = values.firstOrNull { !it.isNullOrBlank() }.orEmpty().trim()

    private fun idsEquivalent(left: String?, right: String?): Boolean {
        val a = normalizeIdentifier(left)
        val b = normalizeIdentifier(right)
        if (a.isBlank() || b.isBlank()) return false
        if (a == b) return true
        val shorter = if (a.length <= b.length) a else b
        val longer = if (a.length > b.length) a else b
        return shorter.length >= 4 && longer.endsWith(shorter)
    }

    private fun normalizeIdentifier(value: String?): String = normalizeForMatch(value.orEmpty()).replace(Regex("[^a-z0-9]+"), "")


    private fun normalizeForMatch(value: String): String {
        val noExtension = value.replace(Regex("\\.[A-Za-z0-9]{1,5}$"), " ")
        val ascii = Normalizer.normalize(noExtension, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("đ", "dj")
            .replace("Đ", "dj")
        return ascii.lowercase(Locale.ROOT)
            .replace(Regex("[_\\-–—/.,;:()\\[\\]{}]+"), " ")
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizedSimilarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        val maxLen = max(a.length, b.length)
        if (maxLen == 0) return 1.0
        return 1.0 - (levenshtein(a, b).toDouble() / maxLen.toDouble())
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in a.indices) {
            cur[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                cur[j + 1] = min(min(cur[j] + 1, prev[j + 1] + 1), prev[j] + cost)
            }
            val tmp = prev
            prev = cur
            cur = tmp
        }
        return prev[b.length]
    }

    private fun sanitizeFilename(value: String, drawing: DriveDrawing): String {
        val cleaned = value.replace(Regex("[\\/:*?\"<>|]+"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
        if (cleaned.isBlank()) return ""
        val hasExtension = cleaned.contains('.') && cleaned.substringAfterLast('.', "").length in 2..5
        if (hasExtension) return cleaned
        val ext = drawing.extension.ifBlank {
            when (drawing.resolvedMimeType) {
                "application/pdf" -> "pdf"
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/tiff" -> "tif"
                "image/webp" -> "webp"
                else -> "bin"
            }
        }
        return "$cleaned.$ext"
    }


    private val STOP_WORDS = setOf(
        "nacrt", "nacrti", "plan", "skica", "profil", "tlocrt", "topodroid", "survey",
        "jama", "jame", "spilja", "spilje", "spilji", "pecina", "pecine", "cave", "pit",
        "hr", "sov", "baza", "objekt", "objekta", "pdf", "final", "novo", "staro"
    )
}
