package com.darko.speleov1

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    // Public/read-only CSV endpoint. Works when the sheet is shared/published enough for anonymous read.
    private const val CSV_URL = "https://docs.google.com/spreadsheets/d/$SHEET_ID/gviz/tq?tqx=out:csv&gid=0"

    // Apps Script Web App /exec endpoint for writing Speleo Runner scores.
    private const val SUBMIT_URL = "https://script.google.com/macros/s/AKfycbyl7eZZzClNVGGYTBH0mZiiwBE4btIl8-WpapOj05kLy_CIKjizhaZVAqcZx_yTmax-/exec"

    suspend fun refresh(context: Context): List<SpeleoRunnerLeaderboardEntry> = withContext(Dispatchers.IO) {
        flushPending(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        try {
            val csv = httpGet(CSV_URL)
            prefs.edit().putString(KEY_CACHE, csv).apply()
            parseCsv(csv)
        } catch (_: Exception) {
            parseCsv(prefs.getString(KEY_CACHE, null).orEmpty())
        }
    }

    suspend fun submitOrQueue(context: Context, name: String, score: Int, bats: Int): Boolean = withContext(Dispatchers.IO) {
        if (name.isBlank() || score <= 0) return@withContext false
        val safeBats = bats.coerceAtLeast(0)
        val ok = trySubmit(name.trim(), score, safeBats)
        if (!ok) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_PENDING_NAME, name.trim())
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
        if (trySubmit(name, score, bats.coerceAtLeast(0))) {
            prefs.edit()
                .remove(KEY_PENDING_NAME)
                .remove(KEY_PENDING_SCORE)
                .remove(KEY_PENDING_BATS)
                .apply()
        }
    }

    private fun trySubmit(name: String, score: Int, bats: Int): Boolean {
        if (SUBMIT_URL.isBlank()) return false
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
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
