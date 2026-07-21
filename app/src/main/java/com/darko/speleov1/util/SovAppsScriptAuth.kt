package com.darko.speleov1.util

import com.darko.speleov1.BuildConfig
import java.net.HttpURLConnection
import java.net.URLEncoder

/**
 * Zajednička zaštita za stare Google Apps Script webapp endpointove.
 *
 * Ključ se učitava iz BuildConfig-a, a BuildConfig ga dobiva iz local.properties
 * ili environment varijable SOV_APPS_SCRIPT_KEY. Vrijednost se namjerno ne smije
 * hardkodirati u source.
 *
 * Apps Script webappovi u doGet/doPost pouzdano vide query/form parametre, dok
 * custom headere ne exposeaju u svakom runtimeu. Zato šaljemo oboje:
 * - HTTP header: X-SOV-KEY
 * - query/form parametar: X-SOV-KEY
 */
internal object SovAppsScriptAuth {
    const val KEY_NAME: String = "X-SOV-KEY"

    private val key: String
        get() = BuildConfig.SOV_APPS_SCRIPT_KEY.trim()

    fun hasKey(): Boolean = key.isNotBlank()

    fun applyTo(connection: HttpURLConnection) {
        val value = key
        if (value.isNotBlank()) {
            connection.setRequestProperty(KEY_NAME, value)
        }
    }

    fun withKeyQuery(url: String): String {
        val value = key
        if (value.isBlank()) return url
        val separator = if (url.contains("?")) "&" else "?"
        return url + separator + URLEncoder.encode(KEY_NAME, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
    }

    fun addToForm(fields: MutableMap<String, String>) {
        val value = key
        if (value.isNotBlank()) {
            fields[KEY_NAME] = value
        }
    }
}
