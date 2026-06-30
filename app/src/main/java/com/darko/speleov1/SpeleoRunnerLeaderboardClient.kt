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
    private const val SHEET_ID = "1NjtgeSth1lVW3ZuIbBYIAletPkLTncz0U_L4UGRuYmk"

    // Legacy Google Sheet remains read-only fallback/import source so old scores are not lost.
    private const val CSV_URL = "https://docs.google.com/spreadsheets/d/$SHEET_ID/gviz/tq?tqx=out:csv&gid=0"

    // Legacy Apps Script remains emergency write fallback if Supabase submit is temporarily unavailable.
    private const val SUBMIT_URL = "https://script.google.com/macros/s/AKfycbyl7eZZzClNVGGYTBH0mZiiwBE4btIl8-WpapOj05kLy_CIKjizhaZVAqcZx_yTmax-/exec"

    suspend fun refresh(context: Context): List<SpeleoRunnerLeaderboardEntry> = withContext(Dispatchers.IO) {
        flushPending(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val supabaseEntries = runCatching { fetchSupabaseLeaderboard(context) }.getOrDefault(emptyList())
        if (supabaseEntries.isNotEmpty()) {
            prefs.edit().putString(KEY_CACHE, entriesToCsv(supabaseEntries)).apply()
            return@withContext supabaseEntries
        }

        // If SQL is empty or temporarily unavailable, keep old results visible from the published Sheet.
        val legacyEntries = runCatching {
            val csv = httpGet(CSV_URL)
            prefs.edit().putString(KEY_CACHE, csv).apply()
            parseCsv(csv)
        }.getOrElse {
            parseCsv(prefs.getString(KEY_CACHE, null).orEmpty())
        }

        if (legacyEntries.isNotEmpty()) {
            importLegacyEntriesToSupabase(context, legacyEntries)
        }
        legacyEntries
    }

    suspend fun submitOrQueue(context: Context, name: String, score: Int, bats: Int): Boolean = withContext(Dispatchers.IO) {
        if (name.isBlank() || score <= 0) return@withContext false
        val safeName = name.trim()
        val safeBats = bats.coerceAtLeast(0)
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val clientKey = scoreClientKey("apk", safeName, score, safeBats, date)

        val ok = trySubmitSupabase(context, safeName, score, safeBats, date, clientKey, "apk") ||
            trySubmitLegacyAppsScript(safeName, score, safeBats, date)
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
        val ok = trySubmitSupabase(context, name, score, bats.coerceAtLeast(0), date, scoreClientKey("apk_pending", name, score, bats, date), "apk_pending") ||
            trySubmitLegacyAppsScript(name, score, bats.coerceAtLeast(0), date)
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

    private fun importLegacyEntriesToSupabase(context: Context, entries: List<SpeleoRunnerLeaderboardEntry>) {
        entries
            .filter { it.score > 0 && it.name.isNotBlank() }
            .take(500)
            .forEach { entry ->
                val key = scoreClientKey("legacy_sheet", entry.name, entry.score, entry.bats, entry.date)
                trySubmitSupabase(context, entry.name, entry.score, entry.bats, entry.date, key, "legacy_sheet")
            }
    }

    private fun trySubmitLegacyAppsScript(name: String, score: Int, bats: Int, date: String): Boolean {
        if (SUBMIT_URL.isBlank()) return false
        return try {
            val body = buildString {
                append("name=").append(URLEncoder.encode(name, "UTF-8"))
                append("&score=").append(URLEncoder.encode(score.toString(), "UTF-8"))
                append("&bats=").append(URLEncoder.encode(bats.coerceAtLeast(0).toString(), "UTF-8"))
                append("&date=").append(URLEncoder.encode(date, "UTF-8"))
            }
            val conn = (URL(SUBMIT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 7000
                readTimeout = 7000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
        }
        return try {
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } finally {
            conn.disconnect()
        }
    }

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
