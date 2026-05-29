package com.darko.speleov1.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets

internal data class ArchiveWorkItem(
    val objectId: String,
    val objectName: String,
    val plateNumber: String = "",
    val objectType: String = "",
    val nearestPlace: String = "",
    val lat: String = "",
    val lon: String = "",
    val county: String = "",
    val municipality: String = "",
    val cadastreStatus: String = "",
    val recordStatus: String = "",
    val fieldTasks: String = "",
    val workflowRaw: String = "",
    val digitalSurveyStatus: String = "",
    val bibliographyStatus: String = "",
    val gpsTracklog: String = "",
    val georefRecord: String = "",
    val hasCoordinates: Boolean = false,
    val hasDrawing: Boolean = false,
    val hasRecord: Boolean = false,
    val hasPlate: Boolean = false,
    val hasPhoto: Boolean = true,
    val needsRedraw: Boolean = false,
    val missingCoordinates: Boolean = true,
    val missingDrawing: Boolean = true,
    val missingRecord: Boolean = true,
    val missingPlate: Boolean = false,
    val missingCategoriesText: String = "",
    val sourceMissingText: String = "",
    val drawingCount: Int = 0,
    val reportCount: Int = 0,
    val readiness: String = "nepotpuno",
    val priorityScore: Int = 0,
    val note: String = "",
    val baseDetailsText: String = "",
    val reportDetailsText: String = "",
    val drawingDetailsText: String = "",
    val fullDetailsText: String = "",
    val rawText: String = ""
)

internal data class ArchiveDashboardStats(
    val total: Int = 0,
    val missingCoordinates: Int = 0,
    val missingDrawings: Int = 0,
    val missingRecords: Int = 0,
    val missingPlates: Int = 0,
    val ready: Int = 0
)

internal data class ArchiveSnapshot(
    val items: List<ArchiveWorkItem> = emptyList(),
    val stats: ArchiveDashboardStats = ArchiveDashboardStats(),
    val message: String = "",
    val fromCache: Boolean = false
)

private fun JSONObject.toArchiveWorkItem(): ArchiveWorkItem = ArchiveWorkItem(
    objectId = optString("object_id"),
    objectName = optString("object_name", "Bez naziva"),
    plateNumber = optString("plate_number"),
    objectType = optString("object_type"),
    nearestPlace = optString("nearest_place"),
    lat = optNullableString("lat"),
    lon = optNullableString("lon"),
    county = optString("county"),
    municipality = optString("municipality"),
    cadastreStatus = optString("cadastre_status"),
    recordStatus = optString("record_status"),
    fieldTasks = optString("field_tasks"),
    workflowRaw = optString("workflow_raw"),
    digitalSurveyStatus = optString("digital_survey_status"),
    bibliographyStatus = optString("bibliography_status"),
    gpsTracklog = optString("gps_tracklog"),
    georefRecord = optString("georef_record"),
    hasCoordinates = optBoolean("has_coordinates", false),
    hasDrawing = optBoolean("has_drawing", false),
    hasRecord = optBoolean("has_record", false),
    hasPlate = when {
        has("has_plate") && !isNull("has_plate") -> optBoolean("has_plate", false)
        optString("plate_number").isNotBlank() -> true
        has("missing_plate") && !isNull("missing_plate") -> !optBoolean("missing_plate", false)
        else -> false
    },
    hasPhoto = if (has("has_photo") && !isNull("has_photo")) optBoolean("has_photo", true) else true,
    needsRedraw = if (has("needs_redraw") && !isNull("needs_redraw")) optBoolean("needs_redraw", false) else false,
    missingCoordinates = optBoolean("missing_coordinates", true),
    missingDrawing = optBoolean("missing_drawing", true),
    missingRecord = optBoolean("missing_record", true),
    missingPlate = optBoolean("missing_plate", false),
    missingCategoriesText = optString("missing_categories_text"),
    sourceMissingText = optString("source_missing_text"),
    drawingCount = optInt("drawing_count", 0),
    reportCount = optInt("report_count", 0),
    readiness = optString("katastar_readiness", "nepotpuno"),
    priorityScore = optInt("priority_score", 0),
    note = optString("last_note"),
    baseDetailsText = optString("base_details_text"),
    reportDetailsText = optString("report_details_text"),
    drawingDetailsText = optString("drawing_details_text"),
    fullDetailsText = optString("full_details_text"),
    rawText = optString("raw_text")
)

private fun JSONObject.optNullableString(name: String): String {
    if (!has(name) || isNull(name)) return ""
    return opt(name)?.toString().orEmpty()
}

private fun ArchiveWorkItem.toJson(): JSONObject = JSONObject()
    .put("object_id", objectId)
    .put("object_name", objectName)
    .put("plate_number", plateNumber)
    .put("object_type", objectType)
    .put("nearest_place", nearestPlace)
    .put("lat", lat)
    .put("lon", lon)
    .put("county", county)
    .put("municipality", municipality)
    .put("cadastre_status", cadastreStatus)
    .put("record_status", recordStatus)
    .put("field_tasks", fieldTasks)
    .put("workflow_raw", workflowRaw)
    .put("digital_survey_status", digitalSurveyStatus)
    .put("bibliography_status", bibliographyStatus)
    .put("gps_tracklog", gpsTracklog)
    .put("georef_record", georefRecord)
    .put("has_coordinates", hasCoordinates)
    .put("has_drawing", hasDrawing)
    .put("has_record", hasRecord)
    .put("has_plate", hasPlate)
    .put("has_photo", hasPhoto)
    .put("needs_redraw", needsRedraw)
    .put("missing_coordinates", missingCoordinates)
    .put("missing_drawing", missingDrawing)
    .put("missing_record", missingRecord)
    .put("missing_plate", missingPlate)
    .put("missing_categories_text", missingCategoriesText)
    .put("source_missing_text", sourceMissingText)
    .put("drawing_count", drawingCount)
    .put("report_count", reportCount)
    .put("katastar_readiness", readiness)
    .put("priority_score", priorityScore)
    .put("last_note", note)
    .put("base_details_text", baseDetailsText)
    .put("report_details_text", reportDetailsText)
    .put("drawing_details_text", drawingDetailsText)
    .put("full_details_text", fullDetailsText)
    .put("raw_text", rawText)

private inline fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> = buildList {
    for (i in 0 until length()) add(mapper(getJSONObject(i)))
}

internal object ArchiveCloudCache {
    private const val PREFS = "sov_archive_work_cache_v5_workflow_checks"
    private const val KEY_ITEMS = "items"
    private const val KEY_SYNCED_AT = "synced_at"

    fun save(context: Context, items: List<ArchiveWorkItem>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ITEMS, JSONArray().also { arr -> items.forEach { arr.put(it.toJson()) } }.toString())
            .putLong(KEY_SYNCED_AT, System.currentTimeMillis())
            .apply()
    }

    fun load(context: Context): ArchiveSnapshot? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, null) ?: return null
        return runCatching {
            val items = JSONArray(raw).mapObjects { it.toArchiveWorkItem() }
            ArchiveSnapshot(items = items, stats = statsFromItems(items), message = "Prikazujem zadnji spremljeni arhivarski worklist. Falinke dolaze iz speleo baze / field_tasks.", fromCache = true)
        }.getOrNull()
    }

    private fun statsFromItems(items: List<ArchiveWorkItem>) = ArchiveDashboardStats(
        total = items.size,
        missingCoordinates = items.count { it.missingCoordinates },
        missingDrawings = items.count { it.missingDrawing },
        missingRecords = items.count { it.missingRecord },
        missingPlates = items.count { it.missingPlate },
        ready = items.count { !it.missingCoordinates && !it.missingDrawing && !it.missingRecord && !it.missingPlate }
    )
}

internal object ArchiveSupabaseRepository {
    fun loadCached(context: Context): ArchiveSnapshot? = ArchiveCloudCache.load(context)

    fun loadSnapshot(context: Context): ArchiveSnapshot {
        val session = SovPermissionsStore.loadSession(context)
        val cached = ArchiveCloudCache.load(context)
        if (!session.isLoggedIn) return cached ?: fallback("Nisi prijavljen u SOV Cloud.")
        return runCatching {
            val items = fetchWorklist(session.accessToken)
            val stats = fetchStats(session.accessToken) ?: statsFromItems(items)
            ArchiveCloudCache.save(context, items)
            ArchiveSnapshot(items, stats, "Arhiva sinkronizirana. Falinke dolaze iz speleo baze: field_tasks/workflow/statusi, ne iz upload arhive.", false)
        }.getOrElse { err ->
            cached?.copy(message = "Cloud nije dostupan: ${err.message.orEmpty().take(90)}", fromCache = true)
                ?: fallback("Cloud nije dostupan: ${err.message.orEmpty().take(90)}")
        }
    }

    fun loadObjectDetail(context: Context, base: ArchiveWorkItem): ArchiveWorkItem {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn || base.objectId.isBlank()) return base
        val body = JSONObject().put("p_object_id", base.objectId).toString()
        val txt = requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/rpc/sov_arhivar_get_object_detail",
            method = "POST",
            accessToken = session.accessToken,
            body = body
        )
        val detail = JSONObject(txt).toArchiveWorkItem()
        return base.copy(
            objectName = detail.objectName.ifBlank { base.objectName },
            plateNumber = detail.plateNumber.ifBlank { base.plateNumber },
            objectType = detail.objectType.ifBlank { base.objectType },
            nearestPlace = detail.nearestPlace.ifBlank { base.nearestPlace },
            lat = detail.lat.ifBlank { base.lat },
            lon = detail.lon.ifBlank { base.lon },
            county = detail.county.ifBlank { base.county },
            municipality = detail.municipality.ifBlank { base.municipality },
            cadastreStatus = detail.cadastreStatus.ifBlank { base.cadastreStatus },
            recordStatus = detail.recordStatus.ifBlank { base.recordStatus },
            fieldTasks = detail.fieldTasks.ifBlank { base.fieldTasks },
            workflowRaw = detail.workflowRaw.ifBlank { base.workflowRaw },
            digitalSurveyStatus = detail.digitalSurveyStatus.ifBlank { base.digitalSurveyStatus },
            bibliographyStatus = detail.bibliographyStatus.ifBlank { base.bibliographyStatus },
            gpsTracklog = detail.gpsTracklog.ifBlank { base.gpsTracklog },
            georefRecord = detail.georefRecord.ifBlank { base.georefRecord },
            hasPlate = detail.hasPlate || (detail.plateNumber.isBlank() && base.hasPlate),
            hasPhoto = detail.hasPhoto,
            needsRedraw = detail.needsRedraw,
            missingPlate = !(detail.hasPlate || (detail.plateNumber.isBlank() && base.hasPlate)),
            drawingCount = if (detail.drawingCount > 0) detail.drawingCount else base.drawingCount,
            reportCount = if (detail.reportCount > 0) detail.reportCount else base.reportCount,
            baseDetailsText = detail.baseDetailsText.ifBlank { base.baseDetailsText },
            reportDetailsText = detail.reportDetailsText.ifBlank { base.reportDetailsText },
            drawingDetailsText = detail.drawingDetailsText.ifBlank { base.drawingDetailsText },
            fullDetailsText = detail.fullDetailsText.ifBlank { base.fullDetailsText },
            rawText = detail.rawText.ifBlank { base.rawText }
        )
    }

    fun saveStatus(
        context: Context,
        item: ArchiveWorkItem,
        plateNumber: String,
        hasCoordinates: Boolean,
        hasDrawing: Boolean,
        hasRecord: Boolean,
        hasPlate: Boolean,
        hasPhoto: Boolean,
        needsRedraw: Boolean,
        priority: String,
        note: String
    ): String {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) error("Nisi prijavljen u SOV Cloud.")
        val cleanPlate = plateNumber.trim()
        val isReady = hasCoordinates && hasDrawing && hasRecord && hasPlate && hasPhoto && !needsRedraw
        val extraChecks = JSONObject()
            .put("ui", "android_admin")
            .put("version", "1.4.13.6")
            .put("has_plate", hasPlate)
            .put("has_photo", hasPhoto)
            .put("needs_redraw", needsRedraw)
        val body = JSONObject()
            .put("p_object_id", item.objectId)
            .put("p_object_name", item.objectName)
            .put("p_plate_number", cleanPlate.ifBlank { JSONObject.NULL })
            .put("p_has_coordinates", hasCoordinates)
            .put("p_has_drawing", hasDrawing)
            .put("p_has_record", hasRecord)
            .put("p_archive_status", if (isReady) "ready" else "needs_review")
            .put("p_priority", priority.ifBlank { "normal" })
            .put("p_note", note.ifBlank { JSONObject.NULL })
            .put("p_has_plate", hasPlate)
            .put("p_has_photo", hasPhoto)
            .put("p_needs_redraw", needsRedraw)
            .put("p_extra_checks", extraChecks)
        requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/rpc/sov_archive_update_object_status_v2",
            method = "POST",
            accessToken = session.accessToken,
            body = body.toString(),
            prefer = "return=representation"
        )
        return "Status arhive spremljen."
    }

    private fun fetchWorklist(accessToken: String): List<ArchiveWorkItem> {
        val txt = requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/sov_arhivar_worklist?select=*&order=priority_score.desc,object_name.asc&limit=800",
            method = "GET",
            accessToken = accessToken,
            body = null
        )
        return JSONArray(txt).mapObjects { it.toArchiveWorkItem() }
    }

    private fun fetchStats(accessToken: String): ArchiveDashboardStats? = runCatching {
        val arr = JSONArray(requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/sov_arhivar_dashboard?select=*&limit=1",
            method = "GET",
            accessToken = accessToken,
            body = null
        ))
        val o = arr.optJSONObject(0) ?: return@runCatching null
        ArchiveDashboardStats(
            total = o.optInt("total_objects", 0),
            missingCoordinates = o.optInt("missing_coordinates", 0),
            missingDrawings = o.optInt("missing_drawings", 0),
            missingRecords = o.optInt("missing_records", 0),
            missingPlates = o.optInt("missing_plates", 0),
            ready = o.optInt("ready_for_katastar", 0)
        )
    }.getOrNull()

    private fun statsFromItems(items: List<ArchiveWorkItem>) = ArchiveDashboardStats(
        total = items.size,
        missingCoordinates = items.count { it.missingCoordinates },
        missingDrawings = items.count { it.missingDrawing },
        missingRecords = items.count { it.missingRecord },
        missingPlates = items.count { it.missingPlate },
        ready = items.count { !it.missingCoordinates && !it.missingDrawing && !it.missingRecord && !it.missingPlate }
    )

    private fun fallback(message: String) = ArchiveSnapshot(message = message, fromCache = true)

    private fun requestText(url: String, method: String, accessToken: String, body: String?, prefer: String? = null): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 20000
            setRequestProperty("apikey", SOV_SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            prefer?.let { setRequestProperty("Prefer", it) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        if (body != null) OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("Supabase HTTP $code: ${text.ifBlank { "bez detalja" }}")
        return text
    }

    @Suppress("unused")
    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
