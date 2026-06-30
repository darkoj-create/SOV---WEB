package com.darko.speleov1.util

import android.content.Context
import android.os.Build
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * SOV Observability v1: lightweight handled-error logger for Android.
 * Sends non-crash errors to Supabase through rpc/sov_log_client_error.
 * Does not use SovHttpClient to avoid recursive logging loops.
 */
internal object SovClientLogger {
    private const val APP_VERSION = "1.4.27-wms-nav-runner"
    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS = 8_000
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
        val cleanMessage = message.take(1800).ifBlank { "Greška bez poruke" }
        val key = listOf(screen, action, severity, cleanMessage).joinToString("|")
        val now = System.currentTimeMillis()
        if (key == lastKey && now - lastAt < 15_000L) return
        lastKey = key
        lastAt = now

        // SOV Observability v2: Crashlytics gets the same context as Supabase logs.
        logToCrashlytics(context.applicationContext, screen, action, severity, cleanMessage, details, tripId, teamId)

        Thread {
            runCatching {
                send(context.applicationContext, screen, action, severity, cleanMessage, details, tripId, teamId, handled = true)
            }
        }.start()
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
                    .put("email", session.email)
                )
                .put("p_user_role", permissions.role)
                .put("p_trip_id", tripId)
                .put("p_team_id", teamId)
                .put("p_handled", handled)

            val token = session.accessToken.ifBlank { SOV_SUPABASE_ANON_KEY }
            val conn = (URL("$SOV_SUPABASE_URL/rest/v1/rpc/sov_log_client_error").openConnection() as HttpURLConnection).apply {
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
