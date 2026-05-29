package com.darko.speleov1

import android.content.Context
import com.darko.speleov1.util.SOV_SUPABASE_ANON_KEY
import com.darko.speleov1.util.SOV_SUPABASE_URL
import com.darko.speleov1.util.SovPermissionsStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

internal data class FieldPackageSheetPayload(
    val packageId: String,
    val date: String,
    val leader: String,
    val location: String,
    val description: String,
    val goal: String,
    val objectCount: Int,
    val pointCount: Int,
    val trackCount: Int,
    val createdAtMillis: Long,
    val centerLat: Double? = null,
    val centerLon: Double? = null,
    val minLat: Double? = null,
    val maxLat: Double? = null,
    val minLon: Double? = null,
    val maxLon: Double? = null,
    val weatherCity: String = "",
    val rasporedUrl: String = "",
    val includeTracks: Boolean? = null,
    val selectedTrackIds: List<String> = emptyList(),
    val optionalMapName: String = ""
)

internal data class FieldPackageSheetTrip(
    val rowNumber: Int = 0,
    val date: String = "",
    val leader: String = "",
    val location: String = "",
    val description: String = "",
    val goal: String = "",
    val participants: String = "",
    val drivers: String = "",
    val rasporedUrl: String = "",
    val weatherCity: String = "",
    val centerLat: Double? = null,
    val centerLon: Double? = null,
    val minLat: Double? = null,
    val maxLat: Double? = null,
    val minLon: Double? = null,
    val maxLon: Double? = null,
    val cloudId: String = "",
    val status: String = "planned"
)

internal data class FieldPackageSheetFetchResult(
    val ok: Boolean = false,
    val trips: List<FieldPackageSheetTrip> = emptyList(),
    val error: String? = null,
    val sheetName: String? = null
)

internal data class FieldPackageTripSignup(
    val id: String = "",
    val tripId: String = "",
    val memberName: String = "",
    val attendanceStatus: String = "confirmed",
    val transportMode: String = "needs_ride",
    val seatsAvailable: Int = 0,
    val departurePlace: String = "",
    val note: String = ""
)

private data class RasporedTabResponse(
    val ok: Boolean = false,
    val gid: Long = 0,
    val url: String = ""
)

internal object FieldPackageSheetSyncClient {
    private const val PREFS = "field_package_sheet_sync_v3_supabase"
    private const val KEY_PENDING = "pending_payloads_json"
    private const val KEY_CACHED_TRIPS = "cached_cloud_trips_json"

    // Legacy cars schedule script is deliberately kept as optional helper only.
    // It is no longer the trip backend/source of truth.
    private const val RASPORED_WEBAPP_URL = "https://script.google.com/macros/s/AKfycbxtn2jOfQjOVBlYJrl8sNKDkBfP2rpONy-lZSaySYHx0sAf72BEdPNybqc4yNvhJDsJ/exec"
    private val gson = Gson()

    fun pendingCount(context: Context): Int = loadPending(context).size

    fun loadCachedTrips(context: Context): List<FieldPackageSheetTrip> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CACHED_TRIPS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<FieldPackageSheetTrip>>() {}.type
            gson.fromJson<List<FieldPackageSheetTrip>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun saveCachedTrips(context: Context, trips: List<FieldPackageSheetTrip>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CACHED_TRIPS, gson.toJson(trips))
            .apply()
    }

    suspend fun fetchTrips(): List<FieldPackageSheetTrip> = fetchTripsWithStatus().trips

    suspend fun fetchTripsWithStatus(context: Context? = null): FieldPackageSheetFetchResult = withContext(Dispatchers.IO) {
        val session = context?.let { SovPermissionsStore.loadSession(it) }
        val cached = context?.let { loadCachedTrips(it) }.orEmpty()
        if (session != null && !session.isLoggedIn) {
            return@withContext FieldPackageSheetFetchResult(
                ok = cached.isNotEmpty(),
                trips = cached,
                error = if (cached.isEmpty()) "Prijavi se u SOV Cloud za sync izleta." else "Prikazujem spremljeni cache — nisi prijavljen.",
                sheetName = "SOV Cloud cache"
            )
        }
        return@withContext try {
            val text = requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/sov_trips_mobile_feed?select=*&order=start_date.asc&limit=1500",
                method = "GET",
                accessToken = session?.accessToken,
                body = null,
                prefer = null
            )
            val arr = JSONArray(text)
            val trips = buildList {
                for (i in 0 until arr.length()) add(arr.getJSONObject(i).toSheetTrip(i))
            }
            if (context != null) saveCachedTrips(context, trips)
            FieldPackageSheetFetchResult(ok = true, trips = trips, sheetName = "SOV Cloud")
        } catch (err: Exception) {
            FieldPackageSheetFetchResult(
                ok = cached.isNotEmpty(),
                trips = cached,
                error = err.message ?: "Greška pri syncu izleta iz SOV Clouda.",
                sheetName = if (cached.isNotEmpty()) "SOV Cloud cache" else null
            )
        }
    }

    suspend fun deleteTrip(trip: FieldPackageSheetTrip): Boolean = withContext(Dispatchers.IO) {
        val context = AppContextHolder.context ?: return@withContext false
        val session = SovPermissionsStore.loadSession(context)
        val id = trip.cloudId.trim()
        if (!session.isLoggedIn || id.isBlank()) return@withContext false
        return@withContext runCatching {
            requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/sov_trips?id=eq.${urlEncode(id)}",
                method = "DELETE",
                accessToken = session.accessToken,
                body = null,
                prefer = null
            )
            true
        }.getOrDefault(false)
    }

    suspend fun signupForTrip(
        trip: FieldPackageSheetTrip,
        name: String,
        attendanceStatus: String,
        transportMode: String,
        seatsAvailable: Int,
        departurePlace: String,
        note: String
    ): Boolean = withContext(Dispatchers.IO) {
        val context = AppContextHolder.context ?: return@withContext false
        val session = SovPermissionsStore.loadSession(context)
        val cleanName = name.trim()
        val id = trip.cloudId.trim()
        if (!session.isLoggedIn || id.isBlank() || cleanName.isBlank()) return@withContext false
        val body = JSONObject()
            .put("p_trip_id", id)
            .put("p_attendance_status", attendanceStatus.trim().ifBlank { "confirmed" })
            .put("p_transport_mode", transportMode.trim().ifBlank { "needs_ride" })
            .put("p_seats_available", seatsAvailable.coerceAtLeast(0))
            .put("p_departure_place", departurePlace.trim())
            .put("p_note", note.trim())
            .put("p_member_name", cleanName)
            .toString()
        return@withContext runCatching {
            requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/rpc/sov_trip_signup",
                method = "POST",
                accessToken = session.accessToken,
                body = body,
                prefer = null
            )
            true
        }.getOrDefault(false)
    }

    suspend fun fetchTripSignups(trip: FieldPackageSheetTrip): List<FieldPackageTripSignup> = withContext(Dispatchers.IO) {
        val context = AppContextHolder.context ?: return@withContext emptyList()
        val session = SovPermissionsStore.loadSession(context)
        val id = trip.cloudId.trim()
        if (!session.isLoggedIn || id.isBlank()) return@withContext emptyList()
        return@withContext runCatching {
            val text = requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/sov_trip_members_transport_view?select=*&trip_id=eq.${urlEncode(id)}&order=created_at.asc&limit=500",
                method = "GET",
                accessToken = session.accessToken,
                body = null,
                prefer = null
            )
            val arr = JSONArray(text)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        FieldPackageTripSignup(
                            id = o.optString("id", ""),
                            tripId = o.optString("trip_id", ""),
                            memberName = o.optString("member_name", "Član"),
                            attendanceStatus = o.optString("attendance_status", "confirmed"),
                            transportMode = o.optString("transport_mode", "needs_ride"),
                            seatsAvailable = o.optInt("seats_available", 0),
                            departurePlace = o.optString("departure_place", ""),
                            note = o.optString("note", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun updateRasporedUrlOnSheet(rowNumber: Int, rasporedUrl: String): Boolean = withContext(Dispatchers.IO) {
        // v1.4.7: trips are no longer backed by Google Sheets. Cars-sheet URL is optional legacy metadata.
        false
    }

    suspend fun submitOrQueue(context: Context, summary: FieldPackageSummary): Boolean = withContext(Dispatchers.IO) {
        val payload = buildPayload(summary)
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) {
            queuePayload(context, payload)
            return@withContext false
        }
        val ok = postPayload(session.accessToken, payload)
        if (!ok) queuePayload(context, payload)
        ok
    }

    suspend fun kreirajRasporedTab(summary: FieldPackageSummary): String? = withContext(Dispatchers.IO) {
        postRasporedTab(
            naziv = summary.locationName.orEmpty().trim().ifBlank { summary.name.trim() },
            datum = summary.tripDateText.orEmpty().trim(),
            voditelj = summary.organizer.trim(),
            packageId = summary.id
        )
    }

    suspend fun kreirajRasporedTab(trip: FieldPackageSheetTrip): String? = withContext(Dispatchers.IO) {
        postRasporedTab(
            naziv = trip.location.trim().ifBlank { "Izlet ${trip.rowNumber}" },
            datum = trip.date.trim(),
            voditelj = trip.leader.trim(),
            packageId = trip.cloudId.ifBlank { "cloud_${trip.rowNumber}" }
        )
    }

    private fun postRasporedTab(naziv: String, datum: String, voditelj: String, packageId: String): String? {
        return try {
            val fields = linkedMapOf(
                "action" to "kreirajTab",
                "naziv" to naziv,
                "datum" to datum,
                "voditelj" to voditelj,
                "packageId" to packageId
            )
            val body = fields.entries.joinToString("&") { (key, value) ->
                URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
            }
            val conn = (URL(RASPORED_WEBAPP_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 15000
                doOutput = true
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val responseBody = if (code in 200..299) conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } else ""
            conn.disconnect()
            if (responseBody.isBlank()) return null
            val parsed = gson.fromJson(responseBody, RasporedTabResponse::class.java)
            if (parsed.ok && parsed.url.isNotBlank()) parsed.url else null
        } catch (_: Exception) { null }
    }

    suspend fun flushPending(context: Context): Int = withContext(Dispatchers.IO) {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) return@withContext 0
        val pending = loadPending(context)
        if (pending.isEmpty()) return@withContext 0
        var sent = 0
        val failed = mutableListOf<FieldPackageSheetPayload>()
        pending.forEach { payload -> if (postPayload(session.accessToken, payload)) sent++ else failed += payload }
        savePending(context, failed)
        sent
    }

    private fun buildPayload(summary: FieldPackageSummary): FieldPackageSheetPayload {
        val dateText = summary.tripDateText.orEmpty().trim().ifBlank { formatDateForSheet(summary.createdAtMillis) }
        return FieldPackageSheetPayload(
            packageId = summary.id,
            date = dateText,
            leader = summary.organizer.trim(),
            location = summary.locationName.orEmpty().trim().ifBlank { summary.name.trim() },
            description = summary.description.trim(),
            goal = summary.goal.orEmpty().trim().ifBlank { "Izletiranje" },
            objectCount = summary.objectCount,
            pointCount = summary.pointCount,
            trackCount = summary.trackCount,
            createdAtMillis = summary.createdAtMillis,
            centerLat = summary.centerLat,
            centerLon = summary.centerLon,
            minLat = summary.minLat,
            maxLat = summary.maxLat,
            minLon = summary.minLon,
            maxLon = summary.maxLon,
            weatherCity = summary.weatherCity.orEmpty().trim(),
            rasporedUrl = summary.rasporedUrl.orEmpty(),
            includeTracks = summary.includeTracks,
            selectedTrackIds = summary.selectedTrackIds.orEmpty(),
            optionalMapName = summary.offlineMapName.orEmpty()
        )
    }

    private fun queuePayload(context: Context, payload: FieldPackageSheetPayload) {
        val current = loadPending(context).filterNot { it.packageId == payload.packageId }
        savePending(context, current + payload)
    }

    private fun loadPending(context: Context): List<FieldPackageSheetPayload> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PENDING, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<FieldPackageSheetPayload>>() {}.type
            gson.fromJson<List<FieldPackageSheetPayload>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun savePending(context: Context, items: List<FieldPackageSheetPayload>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PENDING, gson.toJson(items)).apply()
    }

    private fun postPayload(accessToken: String, payload: FieldPackageSheetPayload): Boolean {
        return try {
            val (start, end) = dateRangeIso(payload.date, payload.createdAtMillis)
            val body = JSONObject()
                .put("start_date", start)
                .put("end_date", end)
                .put("leader_name", payload.leader)
                .put("location_name", payload.location)
                .put("objective", payload.goal)
                .put("description", payload.description)
                .put("status", "planned")
                .put("visibility", "club")
                .put("min_lat", JSONObject.NULL)
                .put("max_lat", JSONObject.NULL)
                .put("min_lon", JSONObject.NULL)
                .put("max_lon", JSONObject.NULL)
                .put("center_lat", JSONObject.NULL)
                .put("center_lon", JSONObject.NULL)
                .put("source", "android")
                .put("legacy_external_id", payload.packageId)
                .put("meta", JSONObject()
                    .put("weatherCity", payload.weatherCity)
                    .put("rasporedUrl", payload.rasporedUrl)
                    .put("objectCount", payload.objectCount)
                    .put("pointCount", payload.pointCount)
                    .put("trackCount", payload.trackCount)
                    .put("includeTracks", payload.includeTracks ?: false)
                    .put("selectedTrackIds", JSONArray(payload.selectedTrackIds))
                    .put("optionalMapName", payload.optionalMapName)
                    .put("optionalAttachments", JSONObject()
                        .put("hasMap", payload.optionalMapName.isNotBlank())
                        .put("trackCount", payload.trackCount)
                        .put("selectedTrackCount", payload.selectedTrackIds.size)
                        .put("source", "apk_1_4_10_trip_description")
                    )
                    .put("createdAtMillis", payload.createdAtMillis)
                    .put("source", "apk_1_4_10_trip_description")
                )
                .toString()
            requestText(
                url = "$SOV_SUPABASE_URL/rest/v1/sov_trips",
                method = "POST",
                accessToken = accessToken,
                body = body,
                prefer = "return=minimal"
            )
            true
        } catch (_: Exception) { false }
    }

    private fun JSONObject.toSheetTrip(index: Int): FieldPackageSheetTrip {
        val meta = optJSONObject("meta") ?: JSONObject()
        val start = optString("start_date", optString("startDate", ""))
        val end = optString("end_date", optString("endDate", start))
        return FieldPackageSheetTrip(
            rowNumber = index + 2,
            date = dateLabel(start, end),
            leader = optString("leader_name", optString("leaderName", "")),
            location = optString("location_name", optString("title", "")),
            description = optString("description", ""),
            goal = optString("objective", ""),
            participants = optInt("member_count", 0).takeIf { it > 0 }?.let { "$it prijavljenih" }.orEmpty(),
            drivers = meta.optString("drivers", ""),
            rasporedUrl = meta.optString("rasporedUrl", meta.optString("raspored_url", "")),
            weatherCity = meta.optString("weatherCity", optString("location_name", "")),
            centerLat = optNullableDouble("center_lat"),
            centerLon = optNullableDouble("center_lon"),
            minLat = optNullableDouble("min_lat"),
            maxLat = optNullableDouble("max_lat"),
            minLon = optNullableDouble("min_lon"),
            maxLon = optNullableDouble("max_lon"),
            cloudId = optString("id", ""),
            status = optString("status", "planned")
        )
    }

    private fun JSONObject.optNullableDouble(name: String): Double? = if (has(name) && !isNull(name)) optDouble(name).takeIf { !it.isNaN() } else null

    private fun dateLabel(start: String, end: String): String {
        val s = hrDate(start)
        val e = hrDate(end.ifBlank { start })
        return if (s.isNotBlank() && e.isNotBlank() && s != e) "$s - $e" else s.ifBlank { e }
    }

    private fun dateRangeIso(dateText: String, fallbackMillis: Long): Pair<String, String> {
        val parts = dateText.split(Regex("\\s+-\\s+"))
        val start = parseDateIso(parts.firstOrNull().orEmpty()) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(fallbackMillis))
        val end = parseDateIso(parts.getOrNull(1).orEmpty()) ?: start
        return start to end
    }

    private fun parseDateIso(text: String): String? {
        val t = text.trim()
        if (t.isBlank()) return null
        Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(t)?.let { m ->
            return "${m.groupValues[1]}-${m.groupValues[2].padStart(2,'0')}-${m.groupValues[3].padStart(2,'0')}"
        }
        Regex("(\\d{1,2})[.\\/ -](\\d{1,2})[.\\/ -](\\d{4})").find(t)?.let { m ->
            return "${m.groupValues[3]}-${m.groupValues[2].padStart(2,'0')}-${m.groupValues[1].padStart(2,'0')}"
        }
        return null
    }

    private fun hrDate(iso: String): String {
        val clean = iso.take(10)
        val m = Regex("(\\d{4})-(\\d{2})-(\\d{2})").matchEntire(clean) ?: return ""
        return "${m.groupValues[3]}.${m.groupValues[2]}.${m.groupValues[1]}."
    }

    private fun requestText(url: String, method: String, accessToken: String?, body: String?, prefer: String?): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 25000
            setRequestProperty("apikey", SOV_SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${accessToken ?: SOV_SUPABASE_ANON_KEY}")
            setRequestProperty("Accept", "application/json")
            if (prefer != null) setRequestProperty("Prefer", prefer)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        if (body != null) OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        conn.disconnect()
        if (code !in 200..299) error("Supabase HTTP $code: ${text.ifBlank { "bez detalja" }}")
        return text
    }

    private fun urlEncode(v: String): String = URLEncoder.encode(v, "UTF-8")

    private fun formatDateForSheet(millis: Long): String = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault()).format(Date(millis))
}

/**
 * Tiny context holder for legacy no-context calls such as delete/signup.
 * FieldPackageFeature refresh path initializes it before user actions.
 */
internal object AppContextHolder {
    var context: Context? = null
}
