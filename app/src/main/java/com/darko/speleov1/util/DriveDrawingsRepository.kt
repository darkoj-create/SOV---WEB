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

    // Nacrti ugrađeni u APK (assets/nacrti_bundled/). Index generira skripta convert_nacrti.py. [offline-nacrti v1]
    private const val BUNDLED_ASSET_DIR = "nacrti_bundled"
    private const val BUNDLED_INDEX_ASSET = "$BUNDLED_ASSET_DIR/index.json"
    private const val BUNDLED_FILE_ID_PREFIX = "asset:"

    @Volatile
    private var bundledDrawingsCache: List<DriveDrawing>? = null

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

    fun isBundledDrawing(drawing: DriveDrawing): Boolean = drawing.fileId.startsWith(BUNDLED_FILE_ID_PREFIX)

    private val bundledMatchCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /**
     * Brza provjera (s keširanjem) ima li objekt nacrt u ugrađenoj bazi.
     * Koristi se za indikator na karticama u pretrazi; pozivati s IO dispatchera.
     */
    fun hasBundledDrawingFor(context: Context, record: SpeleoRecord): Boolean {
        val key = record.id.ifBlank { record.name }
        if (key.isBlank()) return false
        return bundledMatchCache.getOrPut(key) {
            val bundled = loadBundledDrawings(context)
            bundled.isNotEmpty() && findMatches(record, bundled, limit = 1).isNotEmpty()
        }
    }

    /** Nacrti ugrađeni u APK. Učitava se jednom po procesu; prazna lista ako assets ne postoje. */
    fun loadBundledDrawings(context: Context): List<DriveDrawing> {
        bundledDrawingsCache?.let { return it }
        val loaded = runCatching {
            context.assets.open(BUNDLED_INDEX_ASSET).bufferedReader().use { it.readText() }
        }.mapCatching { raw ->
            Gson().fromJson(raw, DriveDrawingIndexResponse::class.java)?.drawings.orEmpty()
        }.getOrDefault(emptyList())
            .filter { it.fileName.isNotBlank() }
            .map { drawing ->
                if (drawing.fileId.startsWith(BUNDLED_FILE_ID_PREFIX)) drawing
                else drawing.copy(fileId = BUNDLED_FILE_ID_PREFIX + drawing.fileName)
            }
        bundledDrawingsCache = loaded
        return loaded
    }

    private fun bundledAssetPath(drawing: DriveDrawing): String =
        BUNDLED_ASSET_DIR + "/" + drawing.fileId.removePrefix(BUNDLED_FILE_ID_PREFIX).ifBlank { drawing.fileName }

    private val bundledExtractLock = Any()

    /**
     * Kopira ugrađeni nacrt iz assets u filesDir kako bi ga FileProvider mogao poslužiti.
     * Zaključano + preko temp datoteke: dva istovremena poziva (thumbnail + viewer)
     * ne smiju ostaviti napola zapisan file.
     */
    private fun extractBundledDrawing(context: Context, drawing: DriveDrawing): File {
        val target = localFileFor(context, drawing)
        if (target.exists() && target.length() > 0) return target
        synchronized(bundledExtractLock) {
            if (target.exists() && target.length() > 0) return target
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, target.name + ".tmp")
            context.assets.open(bundledAssetPath(drawing)).use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            }
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        }
        return target
    }

    fun loadCachedIndex(context: Context): DriveDrawingIndexResponse? = runCatching {
        val file = cacheFile(context)
        if (!file.exists()) return null
        Gson().fromJson(file.readText(), DriveDrawingIndexResponse::class.java)
    }.getOrNull()

    fun saveCachedIndex(context: Context, rawJson: String) {
        runCatching { cacheFile(context).writeText(rawJson) }
    }

    suspend fun fetchMatchesForRecord(context: Context, record: SpeleoRecord, forceNetwork: Boolean = false): DriveDrawingLookupResult {
        // 1) Ugrađeni nacrti (offline, u APK-u) — uvijek prvi izvor.
        val bundled = loadBundledDrawings(context)
        val bundledMatches = if (bundled.isEmpty()) emptyList() else findMatches(record, bundled, limit = 10)
        if (bundledMatches.isNotEmpty() && !forceNetwork) {
            return DriveDrawingLookupResult(ok = true, matches = bundledMatches, totalCount = bundled.size, error = null)
        }

        val webAppUrl = loadWebAppUrl(context)
        if (webAppUrl.isBlank()) {
            return when {
                bundledMatches.isNotEmpty() -> DriveDrawingLookupResult(ok = true, matches = bundledMatches, totalCount = bundled.size, error = null)
                bundled.isNotEmpty() -> DriveDrawingLookupResult(ok = true, matches = emptyList(), totalCount = bundled.size, error = null)
                else -> DriveDrawingLookupResult(ok = false, error = "Nije postavljen Apps Script /exec URL za nacrte.")
            }
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
                when {
                    bundledMatches.isNotEmpty() -> DriveDrawingLookupResult(ok = true, matches = bundledMatches, totalCount = bundled.size, error = null)
                    bundled.isNotEmpty() -> DriveDrawingLookupResult(ok = true, matches = emptyList(), totalCount = bundled.size, error = null)
                    else -> DriveDrawingLookupResult(ok = false, error = parsed.error ?: "Index nacrta nije dostupan.")
                }
            } else {
                // Spoji ugrađene i Drive nacrte; ugrađeni imaju prednost kod istog imena datoteke.
                val bundledNames = bundled.map { normalizeForMatch(it.displayName) }.toSet()
                val remoteOnly = parsed.drawings.filter { normalizeForMatch(it.displayName) !in bundledNames }
                val matches = findMatches(record, bundled + remoteOnly, limit = 10)
                DriveDrawingLookupResult(
                    ok = true,
                    matches = matches,
                    totalCount = parsed.totalCount ?: parsed.count,
                    error = null
                )
            }
        } catch (err: Exception) {
            when {
                bundledMatches.isNotEmpty() -> DriveDrawingLookupResult(ok = true, matches = bundledMatches, totalCount = bundled.size, error = null)
                // Baza je ugrađena ali ovaj objekt nema nacrt, a mreža nije dostupna:
                // vrati uredan prazan rezultat umjesto sirove mrežne greške.
                bundled.isNotEmpty() -> DriveDrawingLookupResult(ok = true, matches = emptyList(), totalCount = bundled.size, error = null)
                else -> DriveDrawingLookupResult(ok = false, error = err.message ?: "Ne mogu dohvatiti nacrte za objekt.")
            }
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

    // ===================== Matching engine v2 =====================
    // Tri razine:
    //  1) Broj pločice / katastarski broj = identifikator (05-0017 ≡ 05-17 ≡ 05017) → verified
    //  2) Kanonsko ime ili sinonim objekta (identično nakon normalizacije) → verified
    //  3) IDF-ponderirana sličnost: težina riječi ovisi o rijetkosti u korpusu nacrta,
    //     pa "jama"/"pod"/"velika" ne znače ništa, a rijetko ime znači sve.
    //  Brojevi u imenu (II, III, 13) su modifikatori: različiti broj obara match (II ≠ III).

    private val DOC_WORDS = setOf(
        "nacrt", "nacrti", "skica", "plan", "tlocrt", "profil", "presjek", "topodroid", "survey",
        "sken", "skenirani", "sredeni", "uredeni", "radni", "master", "korigirano", "fin", "final",
        "novo", "staro", "strana", "str", "pdf", "digitalizirani", "milimetarski", "papir",
        "cro", "speleo", "br", "broj"
    )
    private val YEAR_TOKEN_REGEX = Regex("(19|20)\\d{2}")
    private val SHORT_NUMBER_REGEX = Regex("\\d{1,3}[a-z]?")
    private val ROMAN_REGEX = Regex("[ivxl]{1,5}")
    private val ID_PAIR_REGEX = Regex("\\b(\\d{1,3}) (\\d{1,4})([a-z])?\\b")
    private val ID_HR_REGEX = Regex("\\bhr ?0*(\\d{1,6})\\b")
    private val ID_COMPACT_REGEX = Regex("\\b0(\\d{4,5})\\b")

    private data class MatchKey(val tokens: List<String>, val numerals: Set<String>, val canonical: String)

    private fun romanToInt(t: String): Int? {
        val map = mapOf('i' to 1, 'v' to 5, 'x' to 10, 'l' to 50)
        var total = 0
        var prev = 0
        for (c in t.reversed()) {
            val v = map[c] ?: return null
            if (v < prev) total -= v else { total += v; prev = v }
        }
        return if (total in 1..99) total else null
    }

    private fun numeralValue(t: String): String? {
        if (t.matches(SHORT_NUMBER_REGEX)) {
            val digits = t.takeWhile { it.isDigit() }
            val suffix = t.drop(digits.length)
            return digits.trimStart('0').ifEmpty { "0" } + suffix
        }
        if (t.matches(ROMAN_REGEX)) return romanToInt(t)?.toString()
        return null
    }

    /** Izvuče normalizirane identifikatore (pločica/katastar) iz teksta: "05-0017" → "5:17". */
    private fun extractIds(vararg texts: String?): Set<String> {
        val out = mutableSetOf<String>()
        for (raw in texts) {
            if (raw.isNullOrBlank()) continue
            val t = normalizeForMatch(raw)
            for (m in ID_PAIR_REGEX.findAll(t)) {
                if (m.groupValues[2].matches(YEAR_TOKEN_REGEX)) continue
                out.add(
                    m.groupValues[1].trimStart('0').ifEmpty { "0" } + ":" +
                        m.groupValues[2].trimStart('0').ifEmpty { "0" } + m.groupValues[3]
                )
            }
            for (m in ID_HR_REGEX.findAll(t)) out.add("hr:" + m.groupValues[1])
            for (m in ID_COMPACT_REGEX.findAll(t)) {
                val d = m.groupValues[1]
                out.add(d.take(1) + ":" + d.drop(1).trimStart('0').ifEmpty { "0" })
            }
        }
        return out
    }

    private fun matchKey(value: String, stripIds: Boolean = false): MatchKey {
        var norm = normalizeForMatch(value)
        if (stripIds) {
            norm = norm.replace(ID_PAIR_REGEX, " ").replace(ID_HR_REGEX, " ").replace(ID_COMPACT_REGEX, " ")
        }
        val kept = LinkedHashSet<String>()
        val nums = LinkedHashSet<String>()
        var prev = ""
        for (t in norm.split(' ')) {
            if (t.isBlank()) continue
            if (t in DOC_WORDS || t.matches(YEAR_TOKEN_REGEX)) { prev = t; continue }
            val n = numeralValue(t)
            if (n != null) {
                // broj stranice ("strana 7") nije dio imena objekta
                if (prev != "strana" && prev != "str") nums.add(n)
                prev = t
                continue
            }
            if (t.length >= 2) kept.add(t)
            prev = t
        }
        return MatchKey(kept.toList(), nums, kept.joinToString(" "))
    }

    private fun applyNumerals(base: Double, rec: MatchKey, drw: MatchKey): Double = base * when {
        rec.numerals.isEmpty() && drw.numerals.isEmpty() -> 1.0
        rec.numerals == drw.numerals -> 1.0
        rec.numerals.intersect(drw.numerals).isNotEmpty() -> 0.95
        rec.numerals.isEmpty() || drw.numerals.isEmpty() -> 0.85
        else -> 0.35
    }

    private fun weightedScore(rec: MatchKey, drw: MatchKey, idf: Map<String, Double>, defaultIdf: Double): Double {
        if (rec.tokens.isEmpty() || drw.tokens.isEmpty()) {
            val numeralOnly = rec.tokens.isEmpty() && drw.tokens.isEmpty() &&
                rec.numerals.isNotEmpty() && rec.numerals == drw.numerals
            return if (numeralOnly) 0.9 else 0.0
        }
        if (rec.canonical == drw.canonical) return applyNumerals(1.0, rec, drw)
        var got = 0.0
        var total = 0.0
        for (t in rec.tokens) {
            val w = idf[t] ?: defaultIdf
            total += w
            var best = 0.0
            for (d in drw.tokens) {
                val c = tokenCredit(t, d)
                if (c > best) { best = c; if (best >= 1.0) break }
            }
            got += w * best
        }
        if (total <= 0.0) return 0.0
        var score = got / total
        // koliko je imena nacrta pokriveno — kažnjava match na maleni dio dugog imena
        var dGot = 0.0
        var dTotal = 0.0
        for (t in drw.tokens) {
            val w = idf[t] ?: defaultIdf
            dTotal += w
            var best = 0.0
            for (r in rec.tokens) {
                val c = tokenCredit(t, r)
                if (c > best) { best = c; if (best >= 1.0) break }
            }
            dGot += w * best
        }
        val reverse = if (dTotal > 0.0) dGot / dTotal else 0.0
        score *= 0.72 + 0.28 * reverse
        return applyNumerals(score, rec, drw)
    }

    fun findMatches(record: SpeleoRecord, drawings: List<DriveDrawing>, limit: Int = 10): List<DriveDrawingMatch> {
        if (drawings.isEmpty()) return emptyList()
        // Ime objekta + sinonimi (radna imena) — uzima se najbolji rezultat.
        val recKeys = buildList {
            add(matchKey(record.name))
            val synonyms = listOfNotNull(record.content.synonyms, record.content.other_synonyms)
                .flatMap { it.split(';', ',', '/') }
                .map { it.trim() }
                .filter { it.length >= 3 }
                .take(6)
            for (s in synonyms) add(matchKey(s))
        }.filter { it.tokens.isNotEmpty() || it.numerals.isNotEmpty() }
        val recIds = extractIds(record.condition.plate_number, record.cadastre.cadastral_number)
        val locationTokens = recordLocationTokens(record)
        val recordId = record.id.trim()

        val drwKeys = drawings.map { d ->
            matchKey(listOfNotNull(d.objectName, d.detectedObjectName, d.displayName).joinToString(" "), stripIds = true)
        }
        // IDF iz korpusa nacrta: česte riječi (jama, pod, velika...) automatski gube težinu.
        val df = HashMap<String, Int>()
        for (k in drwKeys) for (t in k.tokens.toSet()) df[t] = (df[t] ?: 0) + 1
        val n = drawings.size.coerceAtLeast(1)
        val idf = df.mapValues { kotlin.math.ln((n + 1.0) / (it.value + 0.5)) }
        val defaultIdf = kotlin.math.ln((n + 1.0) / 0.5)

        val candidates = drawings.mapIndexedNotNull { i, drawing ->
            val status = drawing.matchStatus.orEmpty().lowercase(Locale.ROOT)
            if (status == "rejected") return@mapIndexedNotNull null
            val drawingRecordId = drawing.recordId.orEmpty().trim()
            if (drawingRecordId.isNotBlank()) {
                // Nacrt je offline sparen s točno određenim objektom:
                // prikazuje se SAMO kod njega, nikad kao fuzzy kandidat drugdje.
                return@mapIndexedNotNull if (drawingRecordId.equals(recordId, ignoreCase = true)) {
                    DriveDrawingMatch(drawing, 1.0, "verified", "spareno s objektom")
                } else {
                    null
                }
            }
            val drwIds = extractIds(
                drawing.fileName, drawing.detectedTile, drawing.detectedKatastarNumber,
                drawing.katastarId, drawing.detectedCadastralNumber
            )
            val idMatch = recIds.isNotEmpty() && recIds.intersect(drwIds).isNotEmpty()
            var bestName = 0.0
            var bestIdx = 0
            recKeys.forEachIndexed { ki, k ->
                val s = weightedScore(k, drwKeys[i], idf, defaultIdf)
                if (s > bestName) { bestName = s; bestIdx = ki }
            }
            val locationBonus = if (hasLocationOverlap(locationTokens, drawing.detectedLocation)) 0.04 else 0.0
            val verifiedBoost = if (status == "verified") 0.06 else 0.0
            when {
                idMatch -> DriveDrawingMatch(
                    drawing, (0.90 + 0.10 * bestName).coerceAtMost(1.0), "verified",
                    if (bestName >= 0.5) "broj pločice + ime" else "broj pločice"
                )
                bestName >= 0.86 -> DriveDrawingMatch(
                    drawing, (bestName + locationBonus + verifiedBoost).coerceAtMost(1.0), "verified",
                    if (bestIdx > 0) "sinonim" else "identično ime"
                )
                bestName >= 0.72 -> DriveDrawingMatch(
                    drawing, (bestName + locationBonus + verifiedBoost).coerceAtMost(0.85), "possible",
                    if (bestIdx > 0) "sličan sinonim" else "slično ime"
                )
                else -> null
            }
        }.sortedWith(compareByDescending<DriveDrawingMatch> { it.confidence }.thenBy { it.drawing.displayName.lowercase(Locale.ROOT) })
        return candidates.take(limit)
    }

    fun localFileFor(context: Context, drawing: DriveDrawing): File {
        if (drawing.fileId.startsWith("local:") && drawing.downloadUrl?.startsWith("file:") == true) {
            return File(Uri.parse(drawing.downloadUrl).path.orEmpty())
        }
        if (isBundledDrawing(drawing)) {
            val safe = sanitizeBasicFilename(drawing.fileName).ifBlank { "nacrt_bundled.${drawing.extension.ifBlank { "webp" }}" }
            return File(drawingsDir(context), "bundled/$safe")
        }
        val baseName = drawing.displayName.ifBlank { drawing.fileId.ifBlank { "nacrt" } }
        val safe = sanitizeFilename(baseName, drawing).ifBlank { "nacrt_${drawing.fileId}.${drawing.extension.ifBlank { "bin" }}" }
        return File(drawingsDir(context), safe)
    }

    fun hasLocalFile(context: Context, drawing: DriveDrawing): Boolean =
        isBundledDrawing(drawing) || localFileFor(context, drawing).exists()

    /**
     * Vraća lokalnu datoteku spremnu za čitanje: za ugrađene (asset:) nacrte
     * po potrebi prvo kopira iz APK assets u filesDir. Za ostale vraća localFileFor.
     */
    fun ensureLocalCopy(context: Context, drawing: DriveDrawing): File =
        if (isBundledDrawing(drawing)) {
            runCatching { extractBundledDrawing(context, drawing) }.getOrElse { localFileFor(context, drawing) }
        } else {
            localFileFor(context, drawing)
        }

    suspend fun downloadDrawing(context: Context, drawing: DriveDrawing): File {
        if (isBundledDrawing(drawing)) return extractBundledDrawing(context, drawing)
        val target = localFileFor(context, drawing)
        target.parentFile?.mkdirs()
        val url = drawing.downloadUrl?.takeIf { it.isNotBlank() }
            ?: "https://drive.google.com/uc?export=download&id=${drawing.fileId}"
        val connection = SovNetworkSecurity.openHttpConnection(url, "Drive nacrti").apply {
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
        val file = if (isBundledDrawing(drawing)) {
            runCatching { extractBundledDrawing(context, drawing) }.getOrNull() ?: return false
        } else {
            localFileFor(context, drawing)
        }
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
        if (drawing.fileId.startsWith("local:") || isBundledDrawing(drawing)) return openLocalDrawing(context, drawing)
        val url = drawing.webViewUrl?.takeIf { it.isNotBlank() }
            ?: drawing.fileId.takeIf { it.isNotBlank() }?.let { "https://drive.google.com/file/d/$it/view" }
            ?: return false
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    private fun httpGet(url: String): String {
        val securedUrl = SovAppsScriptAuth.withKeyQuery(url)
        val connection = SovNetworkSecurity.openHttpConnection(securedUrl, "Drive nacrti").apply {
            connectTimeout = DEFAULT_TIMEOUT_MS
            readTimeout = DEFAULT_TIMEOUT_MS
            requestMethod = "GET"
            SovAppsScriptAuth.applyTo(this)
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
            recordTokens.size <= 1 -> 0.45
            recordTokens.size == 2 -> 0.38
            else -> 0.34
        }
        // Broj pločice / katastarski broj je gotovo jedinstven identifikator:
        // ako se poklapa, ime smije biti i vrlo slabo (npr. skraćeno/radno ime).
        val hasIdMatch = cadastreMatch || tileMatch
        val hasStrongMetadata = hasIdMatch || locationMatch
        if (nameScore < minimumNameScore && !(hasIdMatch && nameScore >= 0.10) && !(locationMatch && nameScore >= 0.28)) return null

        val metadataBoost = when {
            cadastreMatch && tileMatch -> 0.30
            cadastreMatch -> 0.24
            tileMatch -> 0.22
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
            hasIdMatch && nameScore >= 0.30 -> "verified"
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
            .filter { it.length >= 2 && !it.matches(YEAR_REGEX) }
        val meaningful = rawTokens.filter { it !in STOP_WORDS && !it.matches(NUMERAL_REGEX) }.toSet()
        return if (meaningful.isNotEmpty()) meaningful else rawTokens.toSet()
    }

    /**
     * Fuzzy kredit tokena: tolerira tipfelere u DUŽIM riječima (Veternica/Vjeternica),
     * ali kratke riječi moraju biti identične (Kriki≠Kviki, Medina≠Jedina).
     * Fuzzy pogodak vrijedi manje od egzaktnog da sam po sebi teže prijeđe prag.
     */
    private fun tokenCredit(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.length < 9 || b.length < 9) return 0.0
        if (kotlin.math.abs(a.length - b.length) > 2) return 0.0
        val d = levenshtein(a, b)
        return when {
            d <= 1 -> 0.85
            d <= 2 && a.length >= 11 -> 0.6
            else -> 0.0
        }
    }

    private fun fuzzyOverlap(from: Set<String>, to: Set<String>): Double {
        if (from.isEmpty()) return 0.0
        var sum = 0.0
        for (t in from) sum += to.maxOfOrNull { other -> tokenCredit(t, other) } ?: 0.0
        return sum / from.size
    }
    private fun nameSimilarityScore(
        recordNameNormalized: String,
        recordTokens: Set<String>,
        drawingNormalized: String,
        fileTokens: Set<String>
    ): Double {
        val tokenOverlap = fuzzyOverlap(recordTokens, fileTokens)
        val reverseOverlap = fuzzyOverlap(fileTokens, recordTokens)
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
        // vrsta objekta / dokumenta
        "nacrt", "nacrti", "plan", "skica", "profil", "tlocrt", "topodroid", "survey",
        "jama", "jame", "jami", "spilja", "spilje", "spilji", "pecina", "pecine", "ponor", "kaverna",
        "cave", "pit", "hr", "sov", "baza", "objekt", "objekta", "pdf", "final", "novo", "staro",
        // sken/izvedba nacrta (šum u imenima datoteka)
        "strana", "sken", "skenirani", "sredeni", "uredeni", "radni", "master", "korigirano", "fin",
        // prijedlozi i veznici — ne smiju nositi match ("Jama pod X" vs "Jama pod Y")
        "pod", "kod", "na", "u", "za", "iz", "od", "do", "uz", "niz", "nad", "pred",
        "kraj", "pored", "iznad", "ispod", "izmedu", "izmedju", "blizu", "prema", "sa", "s",
        // generički pridjevi
        "velika", "veliki", "veliko", "mala", "mali", "malo",
        "gornja", "gornji", "gornje", "donja", "donji", "donje",
        "nova", "novi", "stara", "stari", "lijeva", "lijevi", "desna", "desni",
        // oznake broja — ne smiju same nositi match ("Jama br. 13" vs "br. 3")
        "br", "broj"
    )

    // Rimski brojevi i čiste znamenke razlikuju objekte (Zakičnica II vs III),
    // ali ne smiju sami stvoriti match — tretiraju se kao stop tokeni.
    private val NUMERAL_REGEX = Regex("[ivxl]{2,4}|\\d{1,4}")

    private val YEAR_REGEX = Regex("(19|20)\\d{2}")
}
