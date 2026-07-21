package com.darko.speleov1.nacrt

import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Parses a complete TopoDroid export ZIP into a NacrtSurvey model.
 *
 * ZIP structure:
 *   manifest          — version line + filename→type pairs
 *   survey.sql        — shots, surveys tables
 *   *-1p.tdr          — plan drawing
 *   *-1s.tdr          — profile drawing
 */
object NacrtZipParser {

    data class NacrtSurvey(
        val name: String,
        val date: String,
        val team: String,
        val sql: SurveySqlParser.ParseResult,
        val plan: TdrParser.TdrResult?,
        val profile: TdrParser.TdrResult?
    )

    fun parse(inputStream: InputStream): NacrtSurvey {
        var sqlText: String? = null
        val tdrBuffers = mutableMapOf<String, ByteArray>()
        var manifestText: String? = null

        // Read all entries
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                when {
                    name.equals("manifest", ignoreCase = true) -> {
                        manifestText = zis.readBytes().toString(Charsets.UTF_8)
                    }
                    name.equals("survey.sql", ignoreCase = true) -> {
                        sqlText = zis.readBytes().toString(Charsets.UTF_8)
                    }
                    name.endsWith(".tdr", ignoreCase = true) -> {
                        tdrBuffers[name] = zis.readBytes()
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        requireNotNull(sqlText) { "survey.sql not found in ZIP" }
        val sqlData = SurveySqlParser.parse(sqlText!!)

        // Parse TDR files
        var plan: TdrParser.TdrResult? = null
        var profile: TdrParser.TdrResult? = null

        for ((_, bytes) in tdrBuffers) {
            val result = runCatching {
                TdrParser.parse(bytes.inputStream())
            }.getOrNull() ?: continue

            when (result.plotType) {
                1 -> if (plan == null) plan = result
                2 -> if (profile == null) profile = result
            }
        }

        val surveyName = sqlData.survey?.name ?: manifestText?.lines()?.firstOrNull()?.trim() ?: "Nepoznato"
        val date = sqlData.survey?.date ?: ""
        val team = sqlData.survey?.team ?: ""

        return NacrtSurvey(
            name = surveyName,
            date = date,
            team = team,
            sql = sqlData,
            plan = plan,
            profile = profile
        )
    }
}
