package com.darko.speleov1.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class ArchiveSubmissionFile(
    val id: String,
    val submissionId: String,
    val fileType: String,
    val fileName: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val storageBucket: String = "speleo-submissions",
    val storagePath: String = "",
    val publicUrl: String = ""
)

internal data class ArchiveSubmission(
    val id: String,
    val status: String,
    val objectName: String,
    val objectType: String = "",
    val county: String = "",
    val municipality: String = "",
    val nearestPlace: String = "",
    val lat: String = "",
    val lon: String = "",
    val depthM: String = "",
    val lengthM: String = "",
    val surveyDate: String = "",
    val team: String = "",
    val accessDescription: String = "",
    val technicalDescription: String = "",
    val researchHistory: String = "",
    val notes: String = "",
    val submitterEmail: String = "",
    val missingCategories: List<String> = emptyList(),
    val archivistNote: String = "",
    val approvedObjectId: String = "",
    val createdAt: String = "",
    val files: List<ArchiveSubmissionFile> = emptyList()
)

private fun JSONObject.optStringOrJson(name: String): String {
    if (!has(name) || isNull(name)) return ""
    return opt(name)?.toString().orEmpty()
}

private fun JSONObject.stringFromRecord(name: String, record: JSONObject): String =
    optStringOrJson(name).ifBlank { record.optStringOrJson(name) }

private fun JSONObject.toSubmissionFile(): ArchiveSubmissionFile = ArchiveSubmissionFile(
    id = optStringOrJson("id"),
    submissionId = optStringOrJson("submission_id"),
    fileType = optStringOrJson("file_type"),
    fileName = optStringOrJson("file_name"),
    mimeType = optStringOrJson("mime_type"),
    sizeBytes = optLong("size_bytes", 0L),
    storageBucket = optString("storage_bucket", "speleo-submissions"),
    storagePath = optStringOrJson("storage_path"),
    publicUrl = optStringOrJson("public_url")
)

private fun JSONObject.toSubmission(files: List<ArchiveSubmissionFile> = emptyList()): ArchiveSubmission {
    val rec = optJSONObject("record_json") ?: JSONObject()
    val missing = optJSONArray("missing_categories")?.let { arr ->
        buildList { for (i in 0 until arr.length()) add(arr.optString(i)) }.filter { it.isNotBlank() }
    }.orEmpty()
    return ArchiveSubmission(
        id = optStringOrJson("id"),
        status = optString("status", "submitted"),
        objectName = stringFromRecord("object_name", rec).ifBlank { "Bez naziva" },
        objectType = stringFromRecord("object_type", rec),
        county = stringFromRecord("county", rec),
        municipality = stringFromRecord("municipality", rec),
        nearestPlace = stringFromRecord("nearest_place", rec),
        lat = stringFromRecord("lat", rec),
        lon = stringFromRecord("lon", rec),
        depthM = stringFromRecord("depth_m", rec),
        lengthM = stringFromRecord("length_m", rec),
        surveyDate = stringFromRecord("survey_date", rec),
        team = stringFromRecord("team", rec),
        accessDescription = stringFromRecord("access_description", rec),
        technicalDescription = stringFromRecord("technical_description", rec),
        researchHistory = stringFromRecord("research_history", rec),
        notes = stringFromRecord("notes", rec),
        submitterEmail = optStringOrJson("submitter_email"),
        missingCategories = missing,
        archivistNote = optStringOrJson("archivist_note"),
        approvedObjectId = optStringOrJson("approved_object_id"),
        createdAt = optStringOrJson("created_at"),
        files = files
    )
}

private inline fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> = buildList {
    for (i in 0 until length()) add(mapper(getJSONObject(i)))
}

internal object ArchiveSubmissionsRepository {
    fun load(context: Context, status: String = "submitted"): List<ArchiveSubmission> {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) error("Nisi prijavljen u SOV Cloud.")
        val filter = if (status == "all") "" else "&status=eq.${SovHttpClient.urlEncode(status)}"
        val submissionsText = SovHttpClient.get(
            context = context,
            url = "$SOV_SUPABASE_URL/rest/v1/speleo_object_submissions?select=*&order=created_at.desc&limit=300$filter"
        )
        val rawRows = JSONArray(submissionsText).mapObjects { it }
        if (rawRows.isEmpty()) return emptyList()
        val ids = rawRows.joinToString(",") { it.optStringOrJson("id") }
        val filesText = SovHttpClient.get(
            context = context,
            url = "$SOV_SUPABASE_URL/rest/v1/speleo_object_submission_files?select=*&submission_id=in.(${ids})&order=created_at.asc"
        )
        val filesBy = JSONArray(filesText).mapObjects { it.toSubmissionFile() }.groupBy { it.submissionId }
        return rawRows.map { row -> row.toSubmission(filesBy[row.optStringOrJson("id")].orEmpty()) }
    }

    fun approve(context: Context, submissionId: String, note: String): String {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) error("Nisi prijavljen u SOV Cloud.")
        val body = JSONObject()
            .put("p_submission_id", submissionId)
            .put("p_archivist_note", note.ifBlank { JSONObject.NULL })
            .toString()
        SovHttpClient.post(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_approve_speleo_submission", body)
        return "Predaja odobrena i upisana u bazu."
    }

    fun markNeedsChanges(context: Context, submissionId: String, missing: List<String>, note: String): String {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) error("Nisi prijavljen u SOV Cloud.")
        val arr = JSONArray().also { a -> missing.forEach { a.put(it) } }
        val body = JSONObject()
            .put("p_submission_id", submissionId)
            .put("p_missing_categories", arr)
            .put("p_archivist_note", note.ifBlank { JSONObject.NULL })
            .toString()
        SovHttpClient.post(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_mark_speleo_submission_needs_changes", body)
        return "Predaja označena kao nepotpuna."
    }

    fun reject(context: Context, submissionId: String, note: String): String {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) error("Nisi prijavljen u SOV Cloud.")
        val body = JSONObject().put("status", "rejected").put("archivist_note", note).toString()
        SovHttpClient.patch(
            context = context,
            url = "$SOV_SUPABASE_URL/rest/v1/speleo_object_submissions?id=eq.${SovHttpClient.urlEncode(submissionId)}",
            body = body,
            prefer = "return=minimal"
        )
        return "Predaja odbijena."
    }
}
