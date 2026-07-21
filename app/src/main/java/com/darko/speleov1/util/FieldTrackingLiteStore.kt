package com.darko.speleov1.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

internal data class FieldTrackingLiteState(
    val active: Boolean = false,
    val tripId: String = "",
    val tripTitle: String = "",
    val sessionId: String = "",
    val queuedPoints: Int = 0,
    val syncedPoints: Int = 0,
    val lastSyncOk: Boolean = false,
    val lastSyncError: String = "",
    val lastPointAtMillis: Long = 0L,
    val lastSyncAtMillis: Long = 0L,
    val lowBatteryMode: Boolean = false,
    val trackingMode: String = "lite",
    val pingIntervalSec: Int = 60,
    val trackingStartedAtMillis: Long = 0L,
    val trackingStartBatteryPct: Int = -1,
    val trackingStoppedAtMillis: Long = 0L,
    val trackingStopBatteryPct: Int = -1
)


internal data class FieldTrackingFieldEvent(
    val id: String,
    val title: String,
    val locationText: String = "",
    val status: String = "",
    val joinCode: String = "",
    val defaultTrackingMode: String = "lite",
    val role: String = "",
    val memberCount: Int = 0,
    val activeSessions: Int = 0,
    val lastSeenAtMillis: Long = 0L
)

data class FieldTrackingLatestPosition(
    val tripId: String,
    val sessionId: String,
    val userId: String,
    val deviceId: String,
    val displayName: String,
    val lat: Double,
    val lng: Double,
    val accuracyM: Double?,
    val altitudeM: Double?,
    val speedMps: Double?,
    val headingDeg: Double?,
    val batteryPct: Int?,
    val liveStatus: String,
    val clientStatus: String,
    val recordedAtMillis: Long
) {
    val personKey: String get() = userId.ifBlank { deviceId.ifBlank { sessionId } }
}

data class FieldTrackingRemotePoint(
    val sessionId: String,
    val userId: String,
    val deviceId: String,
    val displayName: String,
    val lat: Double,
    val lng: Double,
    val recordedAtMillis: Long,
    val accuracyM: Double?,
    val batteryPct: Int?,
    val clientStatus: String,
    val trackingMode: String
) {
    val personKey: String get() = userId.ifBlank { deviceId.ifBlank { sessionId } }
}

internal object FieldTrackingLitePrefs {
    private const val PREFS = "sov_field_tracking_lite_v1"
    private const val KEY_ACTIVE = "active"
    private const val KEY_TRIP_ID = "trip_id"
    private const val KEY_TRIP_TITLE = "trip_title"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_SYNCED_POINTS = "synced_points"
    private const val KEY_LAST_SYNC_OK = "last_sync_ok"
    private const val KEY_LAST_SYNC_ERROR = "last_sync_error"
    private const val KEY_LAST_POINT_AT = "last_point_at"
    private const val KEY_LAST_SYNC_AT = "last_sync_at"
    private const val KEY_TRACKING_MODE = "tracking_mode"
    private const val KEY_PING_INTERVAL_SEC = "ping_interval_sec"
    private const val KEY_TRACKING_STARTED_AT = "tracking_started_at"
    private const val KEY_TRACKING_START_BATTERY = "tracking_start_battery"
    private const val KEY_TRACKING_STOPPED_AT = "tracking_stopped_at"
    private const val KEY_TRACKING_STOP_BATTERY = "tracking_stop_battery"

    fun load(context: Context): FieldTrackingLiteState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return FieldTrackingLiteState(
            active = p.getBoolean(KEY_ACTIVE, false),
            tripId = p.getString(KEY_TRIP_ID, "").orEmpty(),
            tripTitle = p.getString(KEY_TRIP_TITLE, "").orEmpty(),
            sessionId = p.getString(KEY_SESSION_ID, "").orEmpty(),
            syncedPoints = p.getInt(KEY_SYNCED_POINTS, 0),
            lastSyncOk = p.getBoolean(KEY_LAST_SYNC_OK, false),
            lastSyncError = p.getString(KEY_LAST_SYNC_ERROR, "").orEmpty(),
            lastPointAtMillis = p.getLong(KEY_LAST_POINT_AT, 0L),
            lastSyncAtMillis = p.getLong(KEY_LAST_SYNC_AT, 0L),
            lowBatteryMode = batteryPct(context).let { it in 1..29 },
            trackingMode = p.getString(KEY_TRACKING_MODE, "lite").orEmpty().ifBlank { "lite" },
            pingIntervalSec = p.getInt(KEY_PING_INTERVAL_SEC, 60).coerceIn(15, 180),
            trackingStartedAtMillis = p.getLong(KEY_TRACKING_STARTED_AT, 0L),
            trackingStartBatteryPct = p.getInt(KEY_TRACKING_START_BATTERY, -1),
            trackingStoppedAtMillis = p.getLong(KEY_TRACKING_STOPPED_AT, 0L),
            trackingStopBatteryPct = p.getInt(KEY_TRACKING_STOP_BATTERY, -1),
            queuedPoints = FieldTrackingLiteStore.pendingCount(context)
        )
    }

    fun markActive(context: Context, tripId: String, tripTitle: String, sessionId: String, trackingMode: String = "lite", pingIntervalSec: Int = 60) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wasActive = p.getBoolean(KEY_ACTIVE, false)
        val currentSessionId = p.getString(KEY_SESSION_ID, "").orEmpty()
        val shouldResetBatteryBaseline = !wasActive || currentSessionId != sessionId || p.getLong(KEY_TRACKING_STARTED_AT, 0L) <= 0L
        val editor = p.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_TRIP_ID, tripId)
            .putString(KEY_TRIP_TITLE, tripTitle)
            .putString(KEY_SESSION_ID, sessionId)
            .putString(KEY_TRACKING_MODE, if (trackingMode == "route") "route" else "lite")
            .putInt(KEY_PING_INTERVAL_SEC, pingIntervalSec.coerceIn(15, 180))
            .putString(KEY_LAST_SYNC_ERROR, "")
        if (shouldResetBatteryBaseline) {
            editor
                .putLong(KEY_TRACKING_STARTED_AT, System.currentTimeMillis())
                .putInt(KEY_TRACKING_START_BATTERY, batteryPct(context))
                .putLong(KEY_TRACKING_STOPPED_AT, 0L)
                .putInt(KEY_TRACKING_STOP_BATTERY, -1)
        }
        editor.apply()
    }

    fun markStopped(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVE, false)
            .putLong(KEY_TRACKING_STOPPED_AT, System.currentTimeMillis())
            .putInt(KEY_TRACKING_STOP_BATTERY, batteryPct(context))
            .apply()
    }

    fun resetBatteryBaseline(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_TRACKING_STARTED_AT, System.currentTimeMillis())
            .putInt(KEY_TRACKING_START_BATTERY, batteryPct(context))
            .putLong(KEY_TRACKING_STOPPED_AT, 0L)
            .putInt(KEY_TRACKING_STOP_BATTERY, -1)
            .apply()
    }

    fun savePointAt(context: Context, millis: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_POINT_AT, millis).apply()
    }

    fun saveSyncResult(context: Context, ok: Boolean, error: String = "", inserted: Int = 0) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit()
            .putBoolean(KEY_LAST_SYNC_OK, ok)
            .putString(KEY_LAST_SYNC_ERROR, error)
            .putLong(KEY_LAST_SYNC_AT, System.currentTimeMillis())
            .putInt(KEY_SYNCED_POINTS, p.getInt(KEY_SYNCED_POINTS, 0) + inserted)
            .apply()
    }

    fun batteryPct(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return -1
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun deviceId(context: Context): String = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "android-${UUID.randomUUID()}"
}

internal object FieldTrackingLiteStore {
    private const val DB_NAME = "sov_field_tracking_lite.db"
    private const val DB_VERSION = 3
    private const val TABLE = "tracking_queue"

    private class Helper(context: Context) : SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                create table if not exists $TABLE(
                  client_point_id text primary key,
                  session_id text,
                  trip_id text not null,
                  user_id text,
                  device_id text not null,
                  lat real not null,
                  lng real not null,
                  recorded_at integer not null,
                  accuracy_m real,
                  altitude_m real,
                  altitude_dem_m real,
                  speed_mps real,
                  heading_deg real,
                  battery_pct integer,
                  tracking_mode text,
                  ping_interval_sec integer,
                  sync_status text not null default 'pending',
                  attempts integer not null default 0,
                  created_at integer not null,
                  synced_at integer
                )
                """.trimIndent()
            )
            db.execSQL("create index if not exists idx_tracking_queue_status on $TABLE(sync_status, recorded_at)")
            db.execSQL("create index if not exists idx_tracking_queue_trip on $TABLE(trip_id, recorded_at)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onCreate(db)
            if (oldVersion < 2) {
                runCatching { db.execSQL("alter table $TABLE add column tracking_mode text") }
                runCatching { db.execSQL("alter table $TABLE add column ping_interval_sec integer") }
            }
            if (oldVersion < 3) {
                runCatching { db.execSQL("alter table $TABLE add column altitude_dem_m real") }
            }
        }
    }

    fun enqueue(context: Context, location: Location, sessionId: String, tripId: String, userId: String? = null, demAltitudeM: Double? = null) {
        val now = if (location.time > 0L) location.time else System.currentTimeMillis()
        val db = Helper(context).writableDatabase
        val sql = """
            insert or ignore into $TABLE(
              client_point_id, session_id, trip_id, user_id, device_id, lat, lng, recorded_at,
              accuracy_m, altitude_m, altitude_dem_m, speed_mps, heading_deg, battery_pct, tracking_mode, ping_interval_sec, sync_status, attempts, created_at
            ) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'pending',0,?)
        """.trimIndent()
        val st = db.compileStatement(sql)
        try {
            st.bindString(1, UUID.randomUUID().toString())
            st.bindString(2, sessionId)
            st.bindString(3, tripId)
            if (userId.isNullOrBlank()) st.bindNull(4) else st.bindString(4, userId)
            st.bindString(5, FieldTrackingLitePrefs.deviceId(context))
            st.bindDouble(6, location.latitude)
            st.bindDouble(7, location.longitude)
            st.bindLong(8, now)
            if (location.hasAccuracy()) st.bindDouble(9, location.accuracy.toDouble()) else st.bindNull(9)
            if (location.hasAltitude()) st.bindDouble(10, location.altitude) else st.bindNull(10)
            if (demAltitudeM != null) st.bindDouble(11, demAltitudeM) else st.bindNull(11)
            if (location.hasSpeed()) st.bindDouble(12, location.speed.toDouble()) else st.bindNull(12)
            if (location.hasBearing()) st.bindDouble(13, location.bearing.toDouble()) else st.bindNull(13)
            val batt = FieldTrackingLitePrefs.batteryPct(context)
            val state = FieldTrackingLitePrefs.load(context)
            if (batt >= 0) st.bindLong(14, batt.toLong()) else st.bindNull(14)
            st.bindString(15, state.trackingMode)
            st.bindLong(16, state.pingIntervalSec.toLong())
            st.bindLong(17, System.currentTimeMillis())
            st.executeInsert()
        } finally {
            st.close()
        }
        FieldTrackingLitePrefs.savePointAt(context, now)
    }

    fun pendingCount(context: Context): Int = runCatching {
        Helper(context).readableDatabase.rawQuery("select count(*) from $TABLE where sync_status='pending'", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }.getOrDefault(0)

    fun loadPending(context: Context, limit: Int = 120): JSONArray {
        val arr = JSONArray()
        Helper(context).readableDatabase.rawQuery(
            "select client_point_id,lat,lng,recorded_at,accuracy_m,altitude_m,speed_mps,heading_deg,battery_pct,tracking_mode,ping_interval_sec,altitude_dem_m from $TABLE where sync_status='pending' order by recorded_at asc limit ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val recorded = c.getLong(3)
                arr.put(JSONObject()
                    .put("client_point_id", c.getString(0))
                    .put("lat", c.getDouble(1))
                    .put("lng", c.getDouble(2))
                    .put("recorded_at", iso(recorded))
                    .put("accuracy_m", if (c.isNull(4)) JSONObject.NULL else c.getDouble(4))
                    .put("altitude_m", if (c.isNull(5)) JSONObject.NULL else c.getDouble(5))
                    .put("speed_mps", if (c.isNull(6)) JSONObject.NULL else c.getDouble(6))
                    .put("heading_deg", if (c.isNull(7)) JSONObject.NULL else c.getDouble(7))
                    .put("battery_pct", if (c.isNull(8)) JSONObject.NULL else c.getInt(8))
                    .put("tracking_mode", if (c.isNull(9)) FieldTrackingLitePrefs.load(context).trackingMode else c.getString(9))
                    .put("ping_interval_sec", if (c.isNull(10)) FieldTrackingLitePrefs.load(context).pingIntervalSec else c.getInt(10))
                    .put("altitude_dem_m", if (c.isNull(11)) JSONObject.NULL else c.getDouble(11))
                    .put("network_state", "android-batch")
                    .put("client_status", "queued")
                )
            }
        }
        return arr
    }

    fun markSynced(context: Context, ids: List<String>) {
        if (ids.isEmpty()) return
        val db = Helper(context).writableDatabase
        db.beginTransaction()
        try {
            val st = db.compileStatement("update $TABLE set sync_status='synced', synced_at=? where client_point_id=?")
            ids.forEach { id -> st.bindLong(1, System.currentTimeMillis()); st.bindString(2, id); st.executeUpdateDelete(); st.clearBindings() }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun clientIds(points: JSONArray): List<String> = (0 until points.length()).mapNotNull { i -> points.optJSONObject(i)?.optString("client_point_id")?.takeIf { it.isNotBlank() } }

    private fun iso(millis: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date(millis))
}

internal object FieldTrackingLiteApi {
    @Synchronized
    private fun activeSession(context: Context, forceRefresh: Boolean = false): SovAuthSession {
        val cached = SovPermissionsStore.loadSession(context)
        if (!cached.isLoggedIn) error("Prvo se prijavi u SOV Cloud.")
        val shouldRefresh = forceRefresh || SovPermissionsStore.isSessionLikelyExpired(cached)
        if (!shouldRefresh || cached.refreshToken.isBlank()) return cached
        return runCatching {
            SovHttpClient.refreshSession(
                context = context,
                forceRefresh = forceRefresh,
                expectedAccessToken = cached.accessToken
            )
        }.getOrElse { throwable ->
            if (forceRefresh) error("Prijava je istekla.")
            cached
        }
    }

    private fun isAuthExpired(throwable: Throwable): Boolean {
        val msg = throwable.message.orEmpty()
        return msg.contains("JWT expired", ignoreCase = true) ||
            msg.contains("PGRST303", ignoreCase = true) ||
            msg.contains("HTTP 401", ignoreCase = true)
    }

    fun friendlyError(throwable: Throwable): String {
        val msg = throwable.message.orEmpty()
        return when {
            isAuthExpired(throwable) -> "Prijava je istekla."
            msg.contains("Failed to connect", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) -> "Nema veze. Spremam lokalno."
            msg.contains("Prvo se prijavi", ignoreCase = true) -> "Prvo se prijavi u SOV Cloud."
            else -> msg.ifBlank { "Tracking sync nije uspio." }
        }
    }

    private fun requestJsonAuth(context: Context, url: String, body: String): JSONObject =
        JSONObject(SovHttpClient.post(context, url, body))

    private fun requestJsonArrayAuth(context: Context, url: String, body: String): JSONArray =
        JSONArray(SovHttpClient.post(context, url, body))

    fun startSession(context: Context, tripId: String, tripTitle: String, appVersion: String, trackingMode: String = "lite"): String {
        val session = activeSession(context)
        val permissions = SovPermissionsStore.loadPermissions(context)
        val cleanMode = if (trackingMode == "route") "route" else "lite"
        val interval = if (cleanMode == "route") 20 else 60
        val payloadV2 = JSONObject()
            .put("p_trip_id", tripId)
            .put("p_device_id", FieldTrackingLitePrefs.deviceId(context))
            .put("p_display_name", permissions.fullName.ifBlank { permissions.email.ifBlank { "SOV član" } })
            .put("p_app_version", appVersion)
            .put("p_device_model", Build.MANUFACTURER + " " + Build.MODEL)
            .put("p_tracking_mode", cleanMode)
            .put("p_ping_interval_sec", interval)
            .put("p_route_name", if (cleanMode == "route") "SOV ruta / GPX" else "SOV lite ping")
        val response = runCatching {
            requestJsonAuth(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_tracking_start_session_v2", payloadV2.toString())
        }.getOrElse {
            val payload = JSONObject()
                .put("p_trip_id", tripId)
                .put("p_device_id", FieldTrackingLitePrefs.deviceId(context))
                .put("p_display_name", permissions.fullName.ifBlank { permissions.email.ifBlank { "SOV član" } })
                .put("p_app_version", appVersion)
                .put("p_device_model", Build.MANUFACTURER + " " + Build.MODEL)
            requestJsonAuth(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_tracking_start_session", payload.toString())
        }
        val id = response.optString("session_id")
        if (id.isBlank()) error("Cloud nije vratio ID.")
        FieldTrackingLitePrefs.markActive(
            context = context,
            tripId = tripId,
            tripTitle = tripTitle,
            sessionId = id,
            trackingMode = response.optString("tracking_mode", cleanMode),
            pingIntervalSec = response.optInt("tracking_interval_sec", interval)
        )
        return id
    }

    fun stopSession(context: Context) {
        val state = FieldTrackingLitePrefs.load(context)
        if (state.sessionId.isNotBlank()) {
            runCatching {
                requestJsonAuth(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_tracking_stop_session", JSONObject().put("p_session_id", state.sessionId).toString())
            }
        }
        FieldTrackingLitePrefs.markStopped(context)
    }

    fun syncPending(context: Context, limit: Int = 120): Int {
        val state = FieldTrackingLitePrefs.load(context)
        if (!state.active || state.sessionId.isBlank()) return 0
        val points = FieldTrackingLiteStore.loadPending(context, limit)
        if (points.length() == 0) return 0
        val payload = JSONObject().put("p_session_id", state.sessionId).put("p_points", points)
        return runCatching {
            val response = requestJsonAuth(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_tracking_ingest_batch", payload.toString())
            val inserted = response.optInt("inserted", 0)
            FieldTrackingLiteStore.markSynced(context, FieldTrackingLiteStore.clientIds(points))
            FieldTrackingLitePrefs.saveSyncResult(context, true, "", inserted)
            inserted
        }.getOrElse { throwable ->
            val clean = friendlyError(throwable)
            FieldTrackingLitePrefs.saveSyncResult(context, false, clean, 0)
            throw IllegalStateException(clean, throwable)
        }
    }


    fun getMyFieldEvents(context: Context): List<FieldTrackingFieldEvent> {
        val arr = requestJsonArrayAuth(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_tracking_get_my_field_events", "{}")
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            FieldTrackingFieldEvent(
                id = o.optString("id"),
                title = o.optString("title", "Teren"),
                locationText = o.optString("location_text", ""),
                status = o.optString("status", ""),
                joinCode = o.optString("join_code", ""),
                defaultTrackingMode = o.optString("default_tracking_mode", "lite"),
                role = o.optString("role", ""),
                memberCount = o.optInt("member_count", 0),
                activeSessions = o.optInt("active_sessions", 0),
                lastSeenAtMillis = parseIsoMillis(o.optString("last_seen_at", ""))
            )
        }.filter { it.id.isNotBlank() }
    }

    fun getLatestPositions(context: Context, tripId: String): List<FieldTrackingLatestPosition> {
        val payload = JSONObject().put("p_trip_id", tripId)
        val arr = requestJsonArrayAuth(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_tracking_get_latest_positions", payload.toString())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val lat = o.optDouble("lat", Double.NaN)
            val lng = o.optDouble("lng", Double.NaN)
            if (lat.isNaN() || lng.isNaN()) return@mapNotNull null
            FieldTrackingLatestPosition(
                tripId = o.optString("trip_id", tripId),
                sessionId = o.optString("session_id", ""),
                userId = o.optString("user_id", ""),
                deviceId = o.optString("device_id", ""),
                displayName = o.optString("display_name", "").ifBlank { "SOV član" },
                lat = lat,
                lng = lng,
                accuracyM = optNullableDouble(o, "accuracy_m"),
                altitudeM = optNullableDouble(o, "altitude_m"),
                speedMps = optNullableDouble(o, "speed_mps"),
                headingDeg = optNullableDouble(o, "heading_deg"),
                batteryPct = if (o.isNull("battery_pct")) null else o.optInt("battery_pct"),
                liveStatus = o.optString("live_status", "offline"),
                clientStatus = o.optString("client_status", "synced"),
                recordedAtMillis = parseIsoMillis(o.optString("recorded_at", ""))
            )
        }
    }

    fun getTripTrackPoints(context: Context, tripId: String, hours: Int = 6): List<FieldTrackingRemotePoint> {
        val payloadV2 = JSONObject().put("p_trip_id", tripId).put("p_hours", hours).put("p_user_id", JSONObject.NULL)
        val arr = runCatching {
            requestJsonArrayAuth(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_tracking_get_trip_points_v2", payloadV2.toString())
        }.getOrElse {
            val payload = JSONObject().put("p_trip_id", tripId).put("p_hours", hours).put("p_user_id", JSONObject.NULL)
            requestJsonArrayAuth(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_tracking_get_trip_points", payload.toString())
        }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val lat = o.optDouble("lat", Double.NaN)
            val lng = o.optDouble("lng", Double.NaN)
            if (lat.isNaN() || lng.isNaN()) return@mapNotNull null
            FieldTrackingRemotePoint(
                sessionId = o.optString("session_id", ""),
                userId = o.optString("user_id", ""),
                deviceId = o.optString("device_id", ""),
                displayName = o.optString("display_name", "").ifBlank { "SOV član" },
                lat = lat,
                lng = lng,
                recordedAtMillis = parseIsoMillis(o.optString("recorded_at", "")),
                accuracyM = optNullableDouble(o, "accuracy_m"),
                batteryPct = if (o.isNull("battery_pct")) null else o.optInt("battery_pct"),
                clientStatus = o.optString("client_status", "synced"),
                trackingMode = o.optString("tracking_mode", "lite")
            )
        }
    }

    private fun optNullableDouble(o: JSONObject, key: String): Double? = if (o.isNull(key)) null else o.optDouble(key).takeUnless { it.isNaN() }

    private fun parseIsoMillis(raw: String): Long {
        if (raw.isBlank() || raw == "null") return 0L
        val clean = raw.trim()
        runCatching { return OffsetDateTime.parse(clean, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() }
        runCatching { return java.time.Instant.parse(clean).toEpochMilli() }
        val normalized = clean.replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ"
        )
        patterns.forEach { pattern ->
            runCatching {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return sdf.parse(normalized)?.time ?: 0L
            }
        }
        return 0L
    }

    fun createFieldEvent(context: Context, sourceTripId: String, title: String, location: String = "", trackingMode: String = "lite"): FieldTrackingFieldEvent {
        val cleanMode = if (trackingMode == "route") "route" else "lite"
        val payload = JSONObject()
            .put("p_source_trip_id", sourceTripId.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("p_title", title.ifBlank { JSONObject.NULL })
            .put("p_location", location.ifBlank { JSONObject.NULL })
            .put("p_start_at", JSONObject.NULL)
            .put("p_default_tracking_mode", cleanMode)
        val response = requestJsonAuth(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_tracking_create_field_event_v2", payload.toString())
        val id = response.optString("field_event_id", response.optString("trip_id"))
        if (id.isBlank()) error("Cloud nije vratio ID ekipe.")
        return FieldTrackingFieldEvent(
            id = id,
            title = response.optString("title", title.ifBlank { "SOV teren" }),
            locationText = location,
            status = "active",
            joinCode = response.optString("join_code", ""),
            defaultTrackingMode = response.optString("tracking_mode", cleanMode),
            role = "leader",
            memberCount = 1,
            activeSessions = 0,
            lastSeenAtMillis = 0L
        )
    }

    fun joinFieldEvent(context: Context, joinCode: String): FieldTrackingFieldEvent {
        val code = joinCode.trim().uppercase(Locale.getDefault())
        if (code.isBlank()) error("Upiši kod ekipe.")
        val response = requestJsonAuth(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_tracking_join_field_event", JSONObject().put("p_join_code", code).toString())
        val id = response.optString("field_event_id", response.optString("trip_id"))
        if (id.isBlank()) error("Cloud nije vratio ID ekipe.")
        return FieldTrackingFieldEvent(
            id = id,
            title = response.optString("title", "SOV teren"),
            locationText = "",
            status = "active",
            joinCode = response.optString("join_code", code),
            defaultTrackingMode = response.optString("tracking_mode", "lite"),
            role = "participant",
            memberCount = 0,
            activeSessions = 0,
            lastSeenAtMillis = 0L
        )
    }
}
