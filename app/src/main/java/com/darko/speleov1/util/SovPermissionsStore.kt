package com.darko.speleov1.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal const val SOV_SUPABASE_URL = "https://ncomefzkuixyfixisrhi.supabase.co"
internal const val SOV_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5jb21lZnprdWl4eWZpeGlzcmhpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk1ODQwOTYsImV4cCI6MjA5NTE2MDA5Nn0.WFSiENYXv48Npaz7vFcY-ksYvg_Ja40iNGsEqb1nUDk"

internal data class SovAppPermissions(
    val role: String = "offline-admin",
    val label: String = "Offline admin",
    val email: String = "",
    val fullName: String = "",
    val status: String = "offline",
    val canViewSovBase: Boolean = true,
    val canViewKatastar: Boolean = true,
    val canEditObjects: Boolean = true,
    val canUploadDrawings: Boolean = true,
    val canVerifyDrawings: Boolean = true,
    val canManageTrips: Boolean = true,
    val canManageEquipment: Boolean = true,
    val canEditNews: Boolean = true,
    val canUseSqlTools: Boolean = true,
    val canManageUsers: Boolean = true,
    val fetchedAtMillis: Long = 0L,
    val expiresAtMillis: Long = 0L,
    val lastSyncOk: Boolean = false,
    val lastSyncError: String = "",
    val source: String = "local-default"
) {
    val isApproved: Boolean get() = status.equals("approved", ignoreCase = true) || status.equals("offline", ignoreCase = true)
    val roleLabel: String get() = label.ifBlank { role }

    fun toJson(): JSONObject = JSONObject()
        .put("role", role)
        .put("label", label)
        .put("email", email)
        .put("full_name", fullName)
        .put("status", status)
        .put("can_view_sov_base", canViewSovBase)
        .put("can_view_katastar", canViewKatastar)
        .put("can_edit_objects", canEditObjects)
        .put("can_upload_drawings", canUploadDrawings)
        .put("can_verify_drawings", canVerifyDrawings)
        .put("can_manage_trips", canManageTrips)
        .put("can_manage_equipment", canManageEquipment)
        .put("can_edit_news", canEditNews)
        .put("can_use_sql_tools", canUseSqlTools)
        .put("can_manage_users", canManageUsers)
        .put("fetched_at", fetchedAtMillis)
        .put("expires_at", expiresAtMillis)
        .put("last_sync_ok", lastSyncOk)
        .put("last_sync_error", lastSyncError)
        .put("source", source)

    companion object {
        fun fromJson(obj: JSONObject): SovAppPermissions = SovAppPermissions(
            role = obj.optString("role", "user"),
            label = obj.optString("label", obj.optString("role", "user")),
            email = obj.optString("email", ""),
            fullName = obj.optString("full_name", obj.optString("fullName", "")),
            status = obj.optString("status", "pending"),
            canViewSovBase = obj.optBoolean("can_view_sov_base", true),
            canViewKatastar = obj.optBoolean("can_view_katastar", false),
            canEditObjects = obj.optBoolean("can_edit_objects", false),
            canUploadDrawings = obj.optBoolean("can_upload_drawings", false),
            canVerifyDrawings = obj.optBoolean("can_verify_drawings", false),
            canManageTrips = obj.optBoolean("can_manage_trips", false),
            canManageEquipment = obj.optBoolean("can_manage_equipment", false),
            canEditNews = obj.optBoolean("can_edit_news", false),
            canUseSqlTools = obj.optBoolean("can_use_sql_tools", false),
            canManageUsers = obj.optBoolean("can_manage_users", false),
            fetchedAtMillis = obj.optLong("fetched_at", System.currentTimeMillis()),
            expiresAtMillis = obj.optLong("expires_at", 0L),
            lastSyncOk = obj.optBoolean("last_sync_ok", false),
            lastSyncError = obj.optString("last_sync_error", ""),
            source = obj.optString("source", "supabase")
        )
    }
}

internal data class SovAuthSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val email: String = "",
    val expiresAtMillis: Long = 0L
) {
    val isLoggedIn: Boolean get() = accessToken.isNotBlank()

    fun toJson(): JSONObject = JSONObject()
        .put("access_token", accessToken)
        .put("refresh_token", refreshToken)
        .put("email", email)
        .put("expires_at", expiresAtMillis)

    companion object {
        fun fromJson(obj: JSONObject): SovAuthSession = SovAuthSession(
            accessToken = obj.optString("access_token", ""),
            refreshToken = obj.optString("refresh_token", ""),
            email = obj.optString("email", ""),
            expiresAtMillis = obj.optLong("expires_at", 0L)
        )
    }
}

internal data class SovRoleSyncState(
    val session: SovAuthSession = SovAuthSession(),
    val permissions: SovAppPermissions = SovAppPermissions(),
    val isOnline: Boolean = false,
    val usedCachedPermissions: Boolean = true,
    val message: String = ""
) {
    val displayStatus: String
        get() = when {
            permissions.source == "local-default" -> "offline admin fallback"
            isOnline && permissions.lastSyncOk -> "online synced"
            usedCachedPermissions -> "cached offline"
            else -> "limited"
        }
}
internal data class SovCloudIdentity(
    val email: String = "",
    val fullName: String = "",
    val role: String = "offline-admin",
    val status: String = "offline",
    val source: String = "local-default"
) {
    val displayName: String get() = fullName.ifBlank { email.ifBlank { "Offline admin" } }
    val isCloudUser: Boolean get() = source == "supabase" && email.isNotBlank()
}

internal fun SovAppPermissions.toCloudIdentity(): SovCloudIdentity = SovCloudIdentity(
    email = email,
    fullName = fullName,
    role = role,
    status = status,
    source = source
)

internal fun SovAppPermissions.permissionSummaryHr(): String {
    val flags = listOfNotNull(
        "SOV baza".takeIf { canViewSovBase },
        "Katastar".takeIf { canViewKatastar },
        "Objekti edit".takeIf { canEditObjects },
        "Izleti".takeIf { canManageTrips },
        "Nacrti".takeIf { canUploadDrawings || canVerifyDrawings },
        "Oružarstvo".takeIf { canManageEquipment },
        "Vijesti".takeIf { canEditNews },
        "Admin".takeIf { canManageUsers || canUseSqlTools }
    )
    return if (flags.isEmpty()) "Samo osnovni pristup" else flags.joinToString(" · ")
}

internal object SovPermissionsStore {
    private const val PREFS = "sov_ecosystem_permissions_v1"
    private const val KEY_PERMISSIONS = "permissions"
    private const val KEY_SESSION = "session"

    fun loadPermissions(context: Context): SovAppPermissions {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PERMISSIONS, null)
        return runCatching { raw?.let { SovAppPermissions.fromJson(JSONObject(it)) } }.getOrNull()
            ?: SovAppPermissions()
    }

    fun savePermissions(context: Context, permissions: SovAppPermissions) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PERMISSIONS, permissions.toJson().toString())
            .apply()
    }

    fun loadSession(context: Context): SovAuthSession {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SESSION, null)
        return runCatching { raw?.let { SovAuthSession.fromJson(JSONObject(it)) } }.getOrNull() ?: SovAuthSession()
    }

    fun saveSession(context: Context, session: SovAuthSession) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSION, session.toJson().toString())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun lastSyncLabel(context: Context): String {
        val permissions = loadPermissions(context)
        if (permissions.fetchedAtMillis <= 0L) return "nikad"
        return runCatching {
            SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(Date(permissions.fetchedAtMillis))
        }.getOrDefault("spremljeno")
    }

    fun isSessionLikelyExpired(session: SovAuthSession): Boolean =
        session.isLoggedIn && session.expiresAtMillis > 0L && session.expiresAtMillis <= System.currentTimeMillis() + 60_000L
}


internal object SovRoleSyncManager {
    fun syncNow(context: Context, forceNetwork: Boolean = false): SovRoleSyncState {
        val cachedSession = SovPermissionsStore.loadSession(context)
        val cachedPermissions = SovPermissionsStore.loadPermissions(context)
        val online = SovSupabaseRoleClient.ping()
        if (!online) {
            return SovRoleSyncState(
                session = cachedSession,
                permissions = cachedPermissions.copy(lastSyncOk = false, lastSyncError = "Offline: koristim spremljene permissione."),
                isOnline = false,
                usedCachedPermissions = true,
                message = "Nema mreže: koristim lokalni role cache."
            )
        }
        if (!cachedSession.isLoggedIn) {
            return SovRoleSyncState(
                session = cachedSession,
                permissions = cachedPermissions,
                isOnline = true,
                usedCachedPermissions = true,
                message = "Online si, ali nisi prijavljen u SOV Cloud."
            )
        }
        return runCatching {
            val activeSession = if (forceNetwork || SovPermissionsStore.isSessionLikelyExpired(cachedSession)) {
                SovSupabaseRoleClient.refreshSession(cachedSession.refreshToken).also { SovPermissionsStore.saveSession(context, it) }
            } else cachedSession
            val permissions = SovSupabaseRoleClient.fetchCurrentPermissions(activeSession.accessToken)
                .copy(expiresAtMillis = activeSession.expiresAtMillis)
            SovPermissionsStore.savePermissions(context, permissions)
            SovRoleSyncState(
                session = activeSession,
                permissions = permissions,
                isOnline = true,
                usedCachedPermissions = false,
                message = "SOV Cloud sync OK: ${permissions.roleLabel}"
            )
        }.getOrElse { throwable ->
            val cached = cachedPermissions.copy(
                lastSyncOk = false,
                lastSyncError = throwable.message ?: "Sync nije uspio.",
                source = if (cachedPermissions.source.isBlank()) "cache" else cachedPermissions.source
            )
            SovPermissionsStore.savePermissions(context, cached)
            SovRoleSyncState(
                session = cachedSession,
                permissions = cached,
                isOnline = true,
                usedCachedPermissions = true,
                message = "Cloud sync nije uspio: koristim spremljeni cache."
            )
        }
    }
}

internal object SovSupabaseRoleClient {
    fun signInWithPassword(email: String, password: String): SovAuthSession {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .toString()
        val response = requestJson(
            url = "$SOV_SUPABASE_URL/auth/v1/token?grant_type=password",
            method = "POST",
            accessToken = null,
            body = body
        )
        val expiresInSec = response.optLong("expires_in", 3600L)
        return SovAuthSession(
            accessToken = response.optString("access_token"),
            refreshToken = response.optString("refresh_token"),
            email = response.optJSONObject("user")?.optString("email") ?: email.trim(),
            expiresAtMillis = System.currentTimeMillis() + expiresInSec * 1000L
        )
    }

    fun ping(): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress("ncomefzkuixyfixisrhi.supabase.co", 443), 2500)
        }
        true
    }.getOrDefault(false)

    fun refreshSession(refreshToken: String): SovAuthSession {
        if (refreshToken.isBlank()) error("Nema refresh tokena. Prijavi se ponovno.")
        val body = JSONObject().put("refresh_token", refreshToken).toString()
        val response = requestJson(
            url = "$SOV_SUPABASE_URL/auth/v1/token?grant_type=refresh_token",
            method = "POST",
            accessToken = null,
            body = body
        )
        val expiresInSec = response.optLong("expires_in", 3600L)
        return SovAuthSession(
            accessToken = response.optString("access_token"),
            refreshToken = response.optString("refresh_token", refreshToken),
            email = response.optJSONObject("user")?.optString("email") ?: "",
            expiresAtMillis = System.currentTimeMillis() + expiresInSec * 1000L
        )
    }

    fun fetchCurrentPermissions(accessToken: String): SovAppPermissions {
        val response = requestText(
            url = "$SOV_SUPABASE_URL/rest/v1/sov_current_user_permissions?select=*",
            method = "GET",
            accessToken = accessToken,
            body = null
        )
        val arr = JSONArray(response)
        if (arr.length() == 0) error("Supabase nije vratio profil/permissione. Provjeri da je korisnik approved i da postoji u profiles.")
        val obj = arr.getJSONObject(0)
        return SovAppPermissions.fromJson(obj).copy(
            fetchedAtMillis = System.currentTimeMillis(),
            lastSyncOk = true,
            lastSyncError = "",
            source = "supabase"
        )
    }

    fun fetchRoleManifest(accessToken: String? = null): String = requestText(
        url = "$SOV_SUPABASE_URL/rest/v1/sov_role_manifest?select=*",
        method = "GET",
        accessToken = accessToken,
        body = null
    )

    private fun requestJson(url: String, method: String, accessToken: String?, body: String?): JSONObject = JSONObject(requestText(url, method, accessToken, body))

    private fun requestText(url: String, method: String, accessToken: String?, body: String?): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("apikey", SOV_SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${accessToken ?: SOV_SUPABASE_ANON_KEY}")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        if (body != null) {
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        if (code !in 200..299) {
            val clean = text.ifBlank { "HTTP $code" }
            error("Supabase HTTP $code: $clean")
        }
        return text
    }
}
