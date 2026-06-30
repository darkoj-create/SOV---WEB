package com.darko.speleov1

import android.content.Context
import com.darko.speleov1.util.SOV_SUPABASE_URL
import com.darko.speleov1.util.SovHttpClient
import com.darko.speleov1.util.SovPermissionsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal data class SovCalendarEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val eventType: String = "ostalo",
    val startAt: String = "",
    val endAt: String = "",
    val location: String = "",
    val visibility: String = "members",
    val createdBy: String = ""
)

internal object SovCalendarCloudRepository {
    suspend fun fetchEvents(context: Context): List<SovCalendarEvent> = withContext(Dispatchers.IO) {
        runCatching {
            val text = SovHttpClient.get(
                context,
                "$SOV_SUPABASE_URL/rest/v1/sov_calendar_events?select=*&order=start_at.asc&limit=1200"
            )
            val arr = JSONArray(text)
            buildList {
                for (i in 0 until arr.length()) add(arr.getJSONObject(i).toCalendarEvent())
            }
        }.getOrDefault(emptyList())
    }

    suspend fun createEvent(context: Context, event: SovCalendarEvent): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val session = SovPermissionsStore.loadSession(context)
            if (!session.isLoggedIn) return@withContext false
            val body = JSONObject()
                .put("title", event.title.trim())
                .put("description", event.description.trim())
                .put("event_type", event.eventType.ifBlank { "ostalo" })
                .put("start_at", event.startAt.trim())
                .put("end_at", event.endAt.trim().ifBlank { event.startAt.trim() })
                .put("location", event.location.trim())
                .put("visibility", event.visibility.ifBlank { "members" })
                .toString()
            SovHttpClient.post(
                context = context,
                url = "$SOV_SUPABASE_URL/rest/v1/sov_calendar_events",
                body = body,
                prefer = "return=minimal"
            )
            true
        }.getOrDefault(false)
    }

    private fun JSONObject.toCalendarEvent(): SovCalendarEvent = SovCalendarEvent(
        id = optString("id"),
        title = optString("title"),
        description = optString("description"),
        eventType = optString("event_type", "ostalo"),
        startAt = optString("start_at"),
        endAt = optString("end_at"),
        location = optString("location"),
        visibility = optString("visibility", "members"),
        createdBy = optString("created_by")
    )
}
