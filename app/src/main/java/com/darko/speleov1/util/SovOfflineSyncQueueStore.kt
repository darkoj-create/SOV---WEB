package com.darko.speleov1.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

internal enum class SovSyncEntityType { TRIP, WAYPOINT, TRACK, NEWS, DRAWING, OBJECT_NOTE, EQUIPMENT }
internal enum class SovSyncOperation { CREATE, UPDATE, DELETE, UPSERT }

internal data class SovOfflineSyncItem(
    val id: String = UUID.randomUUID().toString(),
    val entityType: SovSyncEntityType,
    val operation: SovSyncOperation,
    val localRef: String = "",
    val remoteRef: String = "",
    val payloadJson: String = "{}",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastAttemptAtMillis: Long = 0L,
    val attemptCount: Int = 0,
    val lastError: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("entity_type", entityType.name)
        .put("operation", operation.name)
        .put("local_ref", localRef)
        .put("remote_ref", remoteRef)
        .put("payload", JSONObject(payloadJson.ifBlank { "{}" }))
        .put("created_at", createdAtMillis)
        .put("last_attempt_at", lastAttemptAtMillis)
        .put("attempt_count", attemptCount)
        .put("last_error", lastError)

    companion object {
        fun fromJson(obj: JSONObject): SovOfflineSyncItem = SovOfflineSyncItem(
            id = obj.optString("id", UUID.randomUUID().toString()),
            entityType = runCatching { SovSyncEntityType.valueOf(obj.optString("entity_type")) }.getOrDefault(SovSyncEntityType.OBJECT_NOTE),
            operation = runCatching { SovSyncOperation.valueOf(obj.optString("operation")) }.getOrDefault(SovSyncOperation.UPSERT),
            localRef = obj.optString("local_ref", ""),
            remoteRef = obj.optString("remote_ref", ""),
            payloadJson = obj.optJSONObject("payload")?.toString() ?: "{}",
            createdAtMillis = obj.optLong("created_at", System.currentTimeMillis()),
            lastAttemptAtMillis = obj.optLong("last_attempt_at", 0L),
            attemptCount = obj.optInt("attempt_count", 0),
            lastError = obj.optString("last_error", "")
        )
    }
}

internal data class SovSyncQueueSnapshot(
    val pendingCount: Int,
    val failedCount: Int,
    val oldestLabel: String,
    val items: List<SovOfflineSyncItem>
)

internal object SovOfflineSyncQueueStore {
    private const val PREFS = "sov_offline_sync_queue_v1"
    private const val KEY_QUEUE = "queue_json"
    private const val MAX_ITEMS = 250

    fun enqueue(context: Context, item: SovOfflineSyncItem) {
        val updated = (load(context).filterNot { it.id == item.id } + item).takeLast(MAX_ITEMS)
        save(context, updated)
    }

    fun load(context: Context): List<SovOfflineSyncItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_QUEUE, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) add(SovOfflineSyncItem.fromJson(arr.getJSONObject(i)))
            }
        }.getOrDefault(emptyList())
    }

    fun remove(context: Context, id: String) = save(context, load(context).filterNot { it.id == id })

    fun markFailed(context: Context, id: String, error: String) {
        save(context, load(context).map {
            if (it.id == id) it.copy(
                lastAttemptAtMillis = System.currentTimeMillis(),
                attemptCount = it.attemptCount + 1,
                lastError = error.take(280)
            ) else it
        })
    }

    fun clear(context: Context) = save(context, emptyList())

    fun snapshot(context: Context): SovSyncQueueSnapshot {
        val items = load(context)
        val failed = items.count { it.lastError.isNotBlank() }
        val oldest = items.minOfOrNull { it.createdAtMillis } ?: 0L
        val label = if (oldest <= 0L) "nema čekanja" else runCatching {
            SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(Date(oldest))
        }.getOrDefault("spremljeno")
        return SovSyncQueueSnapshot(
            pendingCount = items.size,
            failedCount = failed,
            oldestLabel = label,
            items = items
        )
    }

    private fun save(context: Context, items: List<SovOfflineSyncItem>) {
        val arr = JSONArray()
        items.takeLast(MAX_ITEMS).forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_QUEUE, arr.toString())
            .apply()
    }
}
