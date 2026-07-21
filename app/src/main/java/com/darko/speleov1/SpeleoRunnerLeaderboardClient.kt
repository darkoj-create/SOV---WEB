package com.darko.speleov1

import android.content.Context
import com.darko.speleov1.util.SOV_SUPABASE_URL
import com.darko.speleov1.util.SovHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class SpeleoRunnerLeaderboardEntry(
    val name: String,
    val score: Int,
    val date: String,
    val bats: Int = 0
)

internal object SpeleoRunnerLeaderboardClient {
    private const val PREFS = "speleo_runner_leaderboard"
    private const val KEY_CACHE = "leaderboard_cache_csv"
    private const val KEY_PENDING_NAME = "pending_name"
    private const val KEY_PENDING_SCORE = "pending_score"
    private const val KEY_PENDING_BATS = "pending_bats"
    // v1.4.29c: Supabase is the source of truth.
    // Google Sheet / Apps Script write fallback is intentionally removed so scores only append to SQL.

    suspend fun refresh(context: Context): List<SpeleoRunnerLeaderboardEntry> = withContext(Dispatchers.IO) {
        flushPending(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Supabase is the only online source of truth. If the network is down, show only the last local cache.
        val supabaseEntries = runCatching { fetchSupabaseLeaderboard(context) }.getOrElse {
            parseCsv(prefs.getString(KEY_CACHE, null).orEmpty())
        }
        if (supabaseEntries.isNotEmpty()) {
            prefs.edit().putString(KEY_CACHE, entriesToCsv(supabaseEntries)).apply()
        }
        supabaseEntries
    }

    suspend fun submitOrQueue(context: Context, name: String, score: Int, bats: Int): Boolean = withContext(Dispatchers.IO) {
        if (name.isBlank() || score <= 0) return@withContext false
        val safeName = name.trim()
        val safeBats = bats.coerceAtLeast(0)
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val clientKey = scoreClientKey("apk", safeName, score, safeBats, date)

        val ok = trySubmitSupabase(context, safeName, score, safeBats, date, clientKey, "apk")
        if (!ok) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_PENDING_NAME, safeName)
                .putInt(KEY_PENDING_SCORE, score)
                .putInt(KEY_PENDING_BATS, safeBats)
                .apply()
        }
        ok
    }

    private fun flushPending(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_PENDING_NAME, null).orEmpty()
        val score = prefs.getInt(KEY_PENDING_SCORE, 0)
        val bats = prefs.getInt(KEY_PENDING_BATS, 0)
        if (name.isBlank() || score <= 0) return
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val ok = trySubmitSupabase(context, name, score, bats.coerceAtLeast(0), date, scoreClientKey("apk_pending", name, score, bats, date), "apk_pending")
        if (ok) {
            prefs.edit()
                .remove(KEY_PENDING_NAME)
                .remove(KEY_PENDING_SCORE)
                .remove(KEY_PENDING_BATS)
                .apply()
        }
    }

    private fun fetchSupabaseLeaderboard(context: Context): List<SpeleoRunnerLeaderboardEntry> {
        val body = JSONObject().put("p_limit", 200).toString()
        val text = SovHttpClient.post(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_list_runner_leaderboard", body)
        return JSONArray(text).toEntries()
    }

    private fun trySubmitSupabase(
        context: Context,
        name: String,
        score: Int,
        bats: Int,
        date: String,
        clientKey: String,
        source: String
    ): Boolean = runCatching {
        val body = JSONObject()
            .put("p_name", name)
            .put("p_score", score)
            .put("p_bats", bats.coerceAtLeast(0))
            .put("p_date_text", date)
            .put("p_client_key", clientKey)
            .put("p_source", source)
            .toString()
        SovHttpClient.post(context, "$SOV_SUPABASE_URL/rest/v1/rpc/sov_submit_runner_score", body)
        true
    }.getOrDefault(false)

    private fun parseCsv(csv: String): List<SpeleoRunnerLeaderboardEntry> {
        if (csv.isBlank()) return emptyList()
        return csv.lineSequence()
            .drop(1)
            .mapNotNull { line ->
                val cols = splitCsvLine(line)
                if (cols.size < 2) return@mapNotNull null
                val name = cols[0].trim().ifBlank { "Anon" }
                val score = cols[1].trim().toIntOrNull() ?: return@mapNotNull null
                val date = cols.getOrNull(2)?.trim().orEmpty()
                val bats = cols.getOrNull(3)?.trim()?.toIntOrNull() ?: 0
                SpeleoRunnerLeaderboardEntry(name, score, date, bats)
            }
            .toList()
    }

    private fun JSONArray.toEntries(): List<SpeleoRunnerLeaderboardEntry> = buildList {
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            val name = obj.optString("name").ifBlank { "Anon" }
            val score = obj.optInt("score", 0)
            if (score <= 0) continue
            add(SpeleoRunnerLeaderboardEntry(
                name = name,
                score = score,
                date = obj.optString("date"),
                bats = obj.optInt("bats", 0).coerceAtLeast(0)
            ))
        }
    }

    private fun entriesToCsv(entries: List<SpeleoRunnerLeaderboardEntry>): String = buildString {
        appendLine("name,score,date,bats")
        entries.forEach { entry ->
            append(csvEscape(entry.name)).append(',')
                .append(entry.score).append(',')
                .append(csvEscape(entry.date)).append(',')
                .append(entry.bats.coerceAtLeast(0))
                .appendLine()
        }
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun scoreClientKey(source: String, name: String, score: Int, bats: Int, date: String): String =
        listOf(source, name.trim().lowercase(Locale.ROOT), score.toString(), bats.coerceAtLeast(0).toString(), date.trim())
            .joinToString("|")
            .take(220)

    private fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    cur.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    out += cur.toString()
                    cur.clear()
                }
                else -> cur.append(c)
            }
            i++
        }
        out += cur.toString()
        return out
    }
}
