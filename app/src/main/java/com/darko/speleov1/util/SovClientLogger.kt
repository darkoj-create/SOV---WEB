package com.darko.speleov1.util

import android.content.Context
import android.os.Build
import com.darko.speleov1.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * SOV Observability v1: lightweight handled-error logger for Android.
 * Sends non-crash errors to Supabase through rpc/sov_log_client_error.
 * Does not use SovHttpClient to avoid recursive logging loops.
 */
internal object SovClientLogger {
    private val APP_VERSION: String
        get() = BuildConfig.VERSION_NAME
    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS = 8_000
    private val logExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SOV-ClientLogger").apply { isDaemon = true }
    }
    @Volatile private var isSending = false
    private var lastKey: String = ""
    private var lastAt: Long = 0L

    fun logHandledError(
        context: Context,
        screen: String,
        action: String,
        message: String,
        severity: String = "error",
        details: JSONObject = JSONObject(),
        tripId: String = "",
        teamId: String = ""
    ) {
        val cleanMessage = sanitizeLogText(message).take(1800).ifBlank { "Greška bez poruke" }
        val cleanDetails = sanitizeJsonObject(details)
        val key = listOf(screen, sanitizeLogText(action), severity, cleanMessage).joinToString("|")
        val now = System.currentTimeMillis()
        if (key == lastKey && now - lastAt < 15_000L) return
        lastKey = key
        lastAt = now

        // SOV Observability v2: Crashlytics gets the same context as Supabase logs.
        logToCrashlytics(context.applicationContext, screen, sanitizeLogText(action), severity, cleanMessage, cleanDetails, tripId, teamId)

        logExecutor.execute {
            runCatching {
                send(context.applicationContext, screen, sanitizeLogText(action), severity, cleanMessage, cleanDetails, tripId, teamId, handled = true)
            }
        }
    }

    fun logInfo(context: Context, screen: String, action: String, message: String, details: JSONObject = JSONObject()) {
        logHandledError(context, screen, action, message, severity = "info", details = details)
    }

    private fun logToCrashlytics(
        context: Context,
        screen: String,
        action: String,
        severity: String,
        message: String,
        details: JSONObject,
        tripId: String,
        teamId: String
    ) {
        runCatching {
            val session = SovPermissionsStore.loadSession(context)
            val permissions = SovPermissionsStore.loadPermissions(context)
            val crashlytics = FirebaseCrashlytics.getInstance()
            val emailHash = session.email.trim().lowercase().takeIf { it.isNotBlank() }?.let { sha256(it).take(16) }.orEmpty()

            crashlytics.setCustomKey("app_version", APP_VERSION)
            crashlytics.setCustomKey("user_role", permissions.role.ifBlank { "unknown" })
            crashlytics.setCustomKey("user_email_hash", emailHash.ifBlank { "anonymous" })
            crashlytics.setCustomKey("current_screen", screen.take(80))
            crashlytics.setCustomKey("last_action", action.take(120))
            crashlytics.setCustomKey("severity", severity.take(24))
            crashlytics.setCustomKey("trip_id", tripId.take(80))
            crashlytics.setCustomKey("team_id", teamId.take(80))
            crashlytics.setCustomKey("device_model", "${Build.MANUFACTURER} ${Build.MODEL}".take(80))
            crashlytics.setCustomKey("android_version", "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})".take(40))
            if (emailHash.isNotBlank()) crashlytics.setUserId(emailHash)
            crashlytics.log("[$severity] $screen / $action: ${message.take(500)}")
            if (severity == "error" || severity == "fatal") {
                crashlytics.recordException(RuntimeException("SOV handled error: $screen / $action — ${message.take(900)}"))
            }
            if (details.length() > 0) {
                crashlytics.log("details: ${details.toString().take(900)}")
            }
        }
    }

    private fun sanitizeLogText(value: String): String {
        var out = value
        out = out.replace(SOV_SUPABASE_ANON_KEY, "<supabase-anon-key>")
        out = out.replace(Regex("(?i)Bearer\\s+[A-Za-z0-9._\\-]+"), "Bearer <redacted>")
        out = out.replace(Regex("(?i)(access_token|refresh_token|apikey|authorization)([\\\"'\\s:=]+)([^\\\"'\\s,}]+)")) {
            "${it.groupValues[1]}${it.groupValues[2]}<redacted>"
        }
        out = out.replace(Regex("(?i)(email)([\\\"'\\s:=]+)([^\\\"'\\s,}]+@[^\\\"'\\s,}]+)")) {
            "${it.groupValues[1]}${it.groupValues[2]}<redacted-email>"
        }
        return out
    }

    private fun sanitizeJsonObject(input: JSONObject): JSONObject {
        val output = JSONObject()
        val keys = input.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = input.opt(key)
            val safeValue = when (value) {
                is JSONObject -> sanitizeJsonObject(value)
                is JSONArray -> sanitizeJsonArray(value)
                is String -> sanitizeLogText(value)
                else -> value
            }
            output.put(key, safeValue)
        }
        return output
    }

    private fun sanitizeJsonArray(input: JSONArray): JSONArray {
        val output = JSONArray()
        for (index in 0 until input.length()) {
            val value = input.opt(index)
            output.put(when (value) {
                is JSONObject -> sanitizeJsonObject(value)
                is JSONArray -> sanitizeJsonArray(value)
                is String -> sanitizeLogText(value)
                else -> value
            })
        }
        return output
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun send(
        context: Context,
        screen: String,
        action: String,
        severity: String,
        message: String,
        details: JSONObject,
        tripId: String,
        teamId: String,
        handled: Boolean
    ) {
        if (isSending) return
        isSending = true
        try {
            val session = SovPermissionsStore.loadSession(context)
            val permissions = SovPermissionsStore.loadPermissions(context)
            val payload = JSONObject()
                .put("p_platform", "android")
                .put("p_app_version", APP_VERSION)
                .put("p_screen", screen.take(160))
                .put("p_action", action.take(180))
                .put("p_severity", severity)
                .put("p_message", message.take(2000))
                .put("p_details", details)
                .put("p_device_info", JSONObject()
                    .put("manufacturer", Build.MANUFACTURER ?: "")
                    .put("model", Build.MODEL ?: "")
                    .put("android", Build.VERSION.RELEASE ?: "")
                    .put("sdk", Build.VERSION.SDK_INT)
                    .put("email_hash", session.email.trim().lowercase().takeIf { it.isNotBlank() }?.let { sha256(it).take(16) }.orEmpty())
                )
                .put("p_user_role", permissions.role)
                .put("p_trip_id", tripId)
                .put("p_team_id", teamId)
                .put("p_handled", handled)

            val token = session.accessToken.ifBlank { SOV_SUPABASE_ANON_KEY }
            val conn = SovNetworkSecurity.openHttpConnection("$SOV_SUPABASE_URL/rest/v1/rpc/sov_log_client_error", "Client error log").apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("apikey", SOV_SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.inputStream?.close()
            if (code !in 200..299) conn.errorStream?.close()
            conn.disconnect()
        } finally {
            isSending = false
        }
    }
}
