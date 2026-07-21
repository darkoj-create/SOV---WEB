package com.darko.speleov1

import android.content.Context
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID
import com.darko.speleov1.util.SovNetworkSecurity

internal data class SovFieldHubSettings(
    val baseUrl: String = "",
    val pin: String = ""
) {
    val normalizedBaseUrl: String
        get() = normalizeHubBaseUrl(baseUrl)
}

internal data class SovFieldHubRoster(
    val trips: List<SovFieldHubRosterTrip> = emptyList(),
    val fetchedAtMillis: Long = System.currentTimeMillis()
)

internal data class SovFieldHubRosterTrip(
    val id: String = "",
    val name: String = "",
    val date: String = "",
    val location: String = "",
    val teams: List<SovFieldHubRosterTeam> = emptyList()
)

internal data class SovFieldHubRosterTeam(
    val id: String = "",
    val name: String = "Ekipa",
    val leaderName: String = "",
    val membersText: String = "",
    val note: String = ""
)

internal data class SovFieldHubUploadResult(
    val ok: Boolean,
    val message: String,
    val sha256: String = ""
)

internal object SovFieldHubClient {
    private const val PREFS = "sov_field_hub_v1"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_PIN = "pin"
    private const val KEY_ROSTER = "cached_roster_json"
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 20_000
    private val gson = Gson()

    fun loadSettings(context: Context): SovFieldHubSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return SovFieldHubSettings(
            baseUrl = prefs.getString(KEY_BASE_URL, "").orEmpty(),
            pin = prefs.getString(KEY_PIN, "").orEmpty()
        )
    }

    fun saveSettings(context: Context, settings: SovFieldHubSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, settings.normalizedBaseUrl)
            .putString(KEY_PIN, settings.pin.trim())
            .apply()
    }

    fun loadCachedRoster(context: Context): SovFieldHubRoster {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ROSTER, null)
            ?: return SovFieldHubRoster()
        return runCatching {
            gson.fromJson(raw, SovFieldHubRoster::class.java) ?: SovFieldHubRoster()
        }.getOrDefault(SovFieldHubRoster())
    }

    private fun saveCachedRoster(context: Context, roster: SovFieldHubRoster) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ROSTER, gson.toJson(roster))
            .apply()
    }

    fun ping(settings: SovFieldHubSettings): String {
        val base = requireBaseUrl(settings)
        val text = getJson("$base/ping", settings.pin)
        if (text.isBlank()) return "Laptop hub je dostupan."
        val obj = runCatching { JSONObject(text) }.getOrNull()
        return obj?.optString("message").orEmpty()
            .ifBlank { if (obj?.optBoolean("ok", true) != false) "Laptop hub je dostupan." else "Hub se javio, ali nije OK." }
    }

    fun fetchRoster(context: Context, settings: SovFieldHubSettings): SovFieldHubRoster {
        val base = requireBaseUrl(settings)
        val text = getJson("$base/roster", settings.pin)
        val roster = parseRoster(text)
        saveCachedRoster(context, roster)
        return roster
    }

    fun uploadPackage(settings: SovFieldHubSettings, file: File): SovFieldHubUploadResult {
        val base = requireBaseUrl(settings)
        if (!file.exists() || !file.isFile) error("Paket nije pronađen.")
        if (!file.name.endsWith(".sovpkg", ignoreCase = true)) error("Hub prima samo .sovpkg pakete.")
        val boundary = "SOV-${UUID.randomUUID()}"
        val conn = SovNetworkSecurity.openHttpConnection("$base/upload", "SOV Field Hub").apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS * 6
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-SOV-PIN", settings.pin.trim())
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        BufferedOutputStream(conn.outputStream).use { out ->
            fun write(text: String) = out.write(text.toByteArray(Charsets.UTF_8))
            write("--$boundary\r\n")
            write("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name.replace("\"", "_")}\"\r\n")
            write("Content-Type: application/vnd.sov.field-package\r\n\r\n")
            file.inputStream().use { input -> input.copyTo(out) }
            write("\r\n--$boundary--\r\n")
        }
        val code = conn.responseCode
        val response = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        conn.disconnect()
        if (code !in 200..299) error("Hub HTTP $code: ${response.ifBlank { "bez detalja" }}")
        val obj = runCatching { JSONObject(response.ifBlank { "{}" }) }.getOrNull()
        val ok = obj?.optBoolean("ok", true) ?: true
        val sha = obj?.optString("sha256", "").orEmpty()
        val message = obj?.optString("message").orEmpty()
            .ifBlank { if (ok) "Paket poslan na laptop hub." else "Hub nije prihvatio paket." }
        return SovFieldHubUploadResult(ok = ok, message = message, sha256 = sha)
    }

    fun cachedTeamsForTrip(context: Context, trip: FieldPackageSheetTrip): List<SovFieldHubRosterTeam> {
        val roster = loadCachedRoster(context)
        val tripKeys = listOf(
            trip.cloudId.trim(),
            trip.location.trim(),
            trip.date.trim(),
            fieldPackageSharedTripKey(trip.date, trip.location)
        ).filter { it.isNotBlank() }.map { it.normalizedHubKey() }
        return roster.trips.firstOrNull { candidate ->
            val candidateKeys = listOf(candidate.id, candidate.name, candidate.location, candidate.date)
                .filter { it.isNotBlank() }
                .map { it.normalizedHubKey() }
            candidateKeys.any { it in tripKeys } ||
                (candidate.location.normalizedHubKey() == trip.location.normalizedHubKey() &&
                    candidate.date.normalizedHubKey() == trip.date.normalizedHubKey())
        }?.teams.orEmpty()
    }

    private fun getJson(url: String, pin: String): String {
        val conn = SovNetworkSecurity.openHttpConnection(url, "SOV network").apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            if (pin.isNotBlank()) setRequestProperty("X-SOV-PIN", pin.trim())
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        conn.disconnect()
        if (code !in 200..299) error("Hub HTTP $code: ${text.ifBlank { "bez detalja" }}")
        return text
    }

    private fun parseRoster(text: String): SovFieldHubRoster {
        val parsed = JSONTokener(text.ifBlank { "{}" }).nextValue()
        val sourceTrips = when (parsed) {
            is JSONArray -> parsed
            is JSONObject -> when {
                parsed.has("trips") -> parsed.optJSONArray("trips")
                parsed.has("roster") -> parsed.optJSONArray("roster")
                parsed.has("items") -> parsed.optJSONArray("items")
                else -> JSONArray()
            } ?: JSONArray()
            else -> JSONArray()
        }
        val trips = buildList {
            for (i in 0 until sourceTrips.length()) {
                val obj = sourceTrips.optJSONObject(i) ?: continue
                add(obj.toHubTrip())
            }
        }
        return SovFieldHubRoster(trips = trips)
    }

    private fun JSONObject.toHubTrip(): SovFieldHubRosterTrip {
        val teamsArray = optJSONArray("teams") ?: optJSONArray("ekipe") ?: JSONArray()
        val teams = buildList {
            for (i in 0 until teamsArray.length()) {
                val team = teamsArray.optJSONObject(i) ?: continue
                add(team.toHubTeam(i))
            }
        }
        return SovFieldHubRosterTrip(
            id = optString("id", optString("trip_id", optString("cloud_id", ""))),
            name = optString("name", optString("title", optString("naziv", ""))),
            date = optString("date", optString("trip_date", optString("datum", ""))),
            location = optString("location", optString("location_name", optString("lokacija", ""))),
            teams = teams
        )
    }

    private fun JSONObject.toHubTeam(index: Int): SovFieldHubRosterTeam {
        val membersFromArray = optJSONArray("members")?.let { arr ->
            buildList {
                for (i in 0 until arr.length()) add(arr.optString(i, ""))
            }.filter { it.isNotBlank() }.joinToString("\n")
        }.orEmpty()
        return SovFieldHubRosterTeam(
            id = optString("id", optString("team_id", "hub_team_$index")),
            name = optString("name", optString("team_name", optString("naziv", "Ekipa ${index + 1}"))),
            leaderName = optString("leader_name", optString("leader", optString("voditelj", ""))),
            membersText = optString("members_text", optString("membersText", membersFromArray)),
            note = optString("note", optString("napomena", ""))
        )
    }

    private fun requireBaseUrl(settings: SovFieldHubSettings): String {
        val url = settings.normalizedBaseUrl
        if (url.isBlank()) error("Upiši adresu laptop huba.")
        SovNetworkSecurity.requireCleartextAllowed(url, "SOV Field Hub")
        return url
    }
}

private fun normalizeHubBaseUrl(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    if (trimmed.isBlank()) return ""
    return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        trimmed
    } else {
        "http://$trimmed"
    }
}

private fun String.normalizedHubKey(): String =
    trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
