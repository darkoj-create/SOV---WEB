package com.darko.speleov1.util

import android.content.Context
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Central Supabase REST client for the SOV Android app.
 *
 * Handles:
 * - proactive refresh when JWT expires in less than 60 seconds
 * - legacy sessions without expiresAtMillis (0) as expired
 * - reactive refresh on HTTP 401, then retries once
 * - synchronized refresh so parallel requests cannot consume the same refresh token
 * - synchronous persistence of refreshed sessions through SovPermissionsStore.saveSession()
 */
internal object SovHttpClient {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 25_000
    private const val REFRESH_WINDOW_MS = 60_000L
    private const val EXPIRED_LOGIN_MESSAGE =
        "Prijava je istekla. Otvori Moj SOV Cloud i prijavi se ponovo."

    private val refreshLock = Any()

    fun activeToken(context: Context): String? {
        val session = SovPermissionsStore.loadSession(context)
        if (!session.isLoggedIn) return null

        val shouldRefresh = session.expiresAtMillis == 0L ||
            session.expiresAtMillis <= System.currentTimeMillis() + REFRESH_WINDOW_MS
        if (!shouldRefresh) return session.accessToken

        return runCatching {
            refreshSession(context, forceRefresh = false, expectedAccessToken = session.accessToken).accessToken
        }.getOrElse { session.accessToken }
    }

    /**
     * The single refresh entry point for other app components that need a full session.
     * All token rotation is serialized here to prevent refresh-token reuse races.
     */
    fun refreshSession(
        context: Context,
        forceRefresh: Boolean = false,
        expectedAccessToken: String? = null
    ): SovAuthSession = synchronized(refreshLock) {
        val current = SovPermissionsStore.loadSession(context)
        if (!current.isLoggedIn || current.refreshToken.isBlank()) error(EXPIRED_LOGIN_MESSAGE)

        val tokenChangedWhileWaiting = expectedAccessToken != null &&
            current.accessToken.isNotBlank() &&
            current.accessToken != expectedAccessToken

        val currentNeedsRefresh = current.expiresAtMillis == 0L ||
            current.expiresAtMillis <= System.currentTimeMillis() + REFRESH_WINDOW_MS

        // Another parallel request already refreshed and committed a usable session.
        if (tokenChangedWhileWaiting && !currentNeedsRefresh) return@synchronized current
        if (!forceRefresh && !currentNeedsRefresh) return@synchronized current

        val refreshed = SovSupabaseRoleClient.refreshSession(current.refreshToken)
        val merged = refreshed.copy(email = refreshed.email.ifBlank { current.email })
        SovPermissionsStore.saveSession(context, merged)
        merged
    }

    fun get(context: Context, url: String, prefer: String? = null): String =
        request(context, url, "GET", null, prefer)

    fun post(context: Context, url: String, body: String?, prefer: String? = null): String =
        request(context, url, "POST", body, prefer)

    fun patch(context: Context, url: String, body: String?, prefer: String? = null): String =
        request(context, url, "PATCH", body, prefer)

    fun delete(context: Context, url: String, prefer: String? = null): String =
        request(context, url, "DELETE", null, prefer)

    fun request(
        context: Context,
        url: String,
        method: String,
        body: String?,
        prefer: String? = null,
        contentType: String = "application/json"
    ): String {
        val firstToken = activeToken(context)
        val first = rawRequest(url, method, firstToken, body, prefer, contentType)
        if (first.code != 401) {
            if (first.code !in 200..299) logHttpFailure(context, url, method, first.code, first.text)
            return first.textOrThrow()
        }

        val refreshedToken = runCatching {
            refreshSession(context, forceRefresh = true, expectedAccessToken = firstToken).accessToken
        }.getOrElse {
            SovClientLogger.logHandledError(
                context = context,
                screen = inferScreen(url),
                action = "$method auth-refresh",
                message = EXPIRED_LOGIN_MESSAGE,
                severity = "warning"
            )
            error(EXPIRED_LOGIN_MESSAGE)
        }

        val second = rawRequest(url, method, refreshedToken, body, prefer, contentType)
        if (second.code == 401) {
            SovClientLogger.logHandledError(
                context = context,
                screen = inferScreen(url),
                action = "$method auth-refresh-retry",
                message = EXPIRED_LOGIN_MESSAGE,
                severity = "warning"
            )
            error(EXPIRED_LOGIN_MESSAGE)
        }
        if (second.code !in 200..299) logHttpFailure(context, url, method, second.code, second.text)
        return second.textOrThrow()
    }

    private fun logHttpFailure(context: Context, url: String, method: String, code: Int, text: String) {
        if (url.contains("/rpc/sov_log_client_error")) return
        SovClientLogger.logHandledError(
            context = context,
            screen = inferScreen(url),
            action = "$method ${url.substringAfter("/rest/v1/").take(120)}",
            message = "HTTP $code: ${text.ifBlank { "bez detalja" }.take(700)}",
            severity = if (code >= 500) "error" else "warning",
            details = org.json.JSONObject()
                .put("url", url.take(500))
                .put("method", method)
                .put("code", code)
                .put("response", text.take(1500))
        )
    }

    private fun inferScreen(url: String): String = when {
        url.contains("sov_trips") || url.contains("trip") -> "Izleti"
        url.contains("oruz") || url.contains("equipment") || url.contains("inventory") -> "Oružarstvo"
        url.contains("speleo_object_submissions") || url.contains("submission") -> "Arhivar"
        url.contains("tracking") -> "Tracking"
        url.contains("calendar") -> "Kalendar"
        else -> "SOV APK"
    }

    fun rawStorageUpload(
        url: String,
        method: String,
        token: String?,
        bodyWriter: (HttpURLConnection) -> Unit
    ): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS * 3
            setRequestProperty("apikey", SOV_SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${token ?: SOV_SUPABASE_ANON_KEY}")
            setRequestProperty("Accept", "application/json")
        }
        bodyWriter(conn)
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        conn.disconnect()
        if (code !in 200..299) error("Supabase HTTP $code: ${text.ifBlank { "bez detalja" }}")
        return text
    }

    fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun rawRequest(
        url: String,
        method: String,
        accessToken: String?,
        body: String?,
        prefer: String?,
        contentType: String
    ): ResponseText {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("apikey", SOV_SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${accessToken ?: SOV_SUPABASE_ANON_KEY}")
            setRequestProperty("Accept", "application/json")
            prefer?.let { setRequestProperty("Prefer", it) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
            }
        }
        if (body != null) OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        conn.disconnect()
        return ResponseText(code, text)
    }

    private data class ResponseText(val code: Int, val text: String) {
        fun textOrThrow(): String {
            if (code !in 200..299) error("Supabase HTTP $code: ${text.ifBlank { "bez detalja" }}")
            return text
        }
    }
}
