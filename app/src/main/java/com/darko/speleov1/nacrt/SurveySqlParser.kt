package com.darko.speleov1.nacrt

import kotlin.math.*

/**
 * Parser for TopoDroid survey.sql export files.
 * Extracts survey metadata, classifies shots (legs/splays/deleted),
 * reduces station coordinates, and computes statistics.
 */
object SurveySqlParser {

    data class SurveyMeta(
        val id: String,
        val name: String,
        val date: String,
        val team: String,
        val declination: Double,
        val initStation: String
    )

    data class Shot(
        val surveyId: String,
        val id: String,
        val fStation: String,
        val tStation: String,
        val distance: Double,
        val bearing: Double,
        val clino: Double,
        val extend: Int,
        val flag: Int,
        val leg: Int,
        val status: Int
    )

    data class StationCoord(val e: Double, val n: Double, val z: Double)

    data class SurveyStats(
        val duljina: Double,      // total 3D length of legs
        val horizontalna: Double, // total horizontal projection
        val dubina: Double        // vertical extent (max Z - min Z)
    )

    data class ParseResult(
        val survey: SurveyMeta?,
        val legs: List<Shot>,
        val splays: List<Shot>,
        val deleted: List<Shot>,
        val declination: Double,
        val stationCoords: Map<String, StationCoord>,
        val stats: SurveyStats
    )

    fun parse(sqlText: String): ParseResult {
        var survey: SurveyMeta? = null
        val shots = mutableListOf<Shot>()
        val insertRegex = Regex("""^INSERT into (\w+) values\(\s*(.*?)\s*\);?\s*$""")

        for (line in sqlText.lines()) {
            val m = insertRegex.matchEntire(line) ?: continue
            val table = m.groupValues[1]
            val vals = tokenize(m.groupValues[2])

            when (table) {
                "surveys" -> {
                    survey = SurveyMeta(
                        id = vals[0], name = vals[1], date = vals[2],
                        team = vals[3], declination = vals[4].toDoubleOrNull() ?: 0.0,
                        initStation = vals.getOrElse(6) { "0" }
                    )
                }
                "shots" -> {
                    shots.add(Shot(
                        surveyId = vals[0], id = vals[1],
                        fStation = vals[2], tStation = vals[3],
                        distance = vals[4].toDoubleOrNull() ?: 0.0,
                        bearing = vals[5].toDoubleOrNull() ?: 0.0,
                        clino = vals[6].toDoubleOrNull() ?: 0.0,
                        extend = vals.getOrElse(11) { "0" }.toIntOrNull() ?: 0,
                        flag = vals.getOrElse(12) { "0" }.toIntOrNull() ?: 0,
                        leg = vals.getOrElse(13) { "0" }.toIntOrNull() ?: 0,
                        status = vals.getOrElse(14) { "0" }.toIntOrNull() ?: 0
                    ))
                }
            }
        }

        // Classify
        val legs = shots.filter { it.fStation.isNotEmpty() && it.tStation.isNotEmpty() && it.status == 0 && it.leg == 0 }
        val splays = shots.filter { it.fStation.isNotEmpty() && it.tStation.isEmpty() && it.status == 0 }
        val deleted = shots.filter { it.status != 0 }

        // Declination: 1080 = TopoDroid sentinel for "not set"
        val decl = if (survey != null && survey.declination == 1080.0) 0.0 else (survey?.declination ?: 0.0)

        // Station coordinate reduction
        val coords = mutableMapOf<String, StationCoord>()
        val initSt = survey?.initStation ?: "0"
        coords[initSt] = StationCoord(0.0, 0.0, 0.0)

        var changed = true
        while (changed) {
            changed = false
            for (leg in legs) {
                val from = coords[leg.fStation]
                val to = coords[leg.tStation]
                if (from != null && to == null) {
                    coords[leg.tStation] = projectForward(from, leg, decl)
                    changed = true
                } else if (to != null && from == null) {
                    coords[leg.fStation] = projectBackward(to, leg, decl)
                    changed = true
                }
            }
        }

        // Statistics
        val duljina = legs.sumOf { it.distance }
        val horizontalna = legs.sumOf { it.distance * cos(Math.toRadians(it.clino)) }
        val zValues = coords.values.map { it.z }
        val dubina = if (zValues.size >= 2) (zValues.max() - zValues.min()) else 0.0

        return ParseResult(
            survey = survey,
            legs = legs,
            splays = splays,
            deleted = deleted,
            declination = decl,
            stationCoords = coords,
            stats = SurveyStats(
                duljina = (duljina * 10).roundToInt() / 10.0,
                horizontalna = (horizontalna * 10).roundToInt() / 10.0,
                dubina = (dubina * 10).roundToInt() / 10.0
            )
        )
    }

    private fun projectForward(from: StationCoord, leg: Shot, decl: Double): StationCoord {
        val d = leg.distance
        val b = Math.toRadians(leg.bearing + decl)
        val c = Math.toRadians(leg.clino)
        return StationCoord(
            e = from.e + d * cos(c) * sin(b),
            n = from.n + d * cos(c) * cos(b),
            z = from.z + d * sin(c)
        )
    }

    private fun projectBackward(to: StationCoord, leg: Shot, decl: Double): StationCoord {
        val d = leg.distance
        val b = Math.toRadians(leg.bearing + decl)
        val c = Math.toRadians(leg.clino)
        return StationCoord(
            e = to.e - d * cos(c) * sin(b),
            n = to.n - d * cos(c) * cos(b),
            z = to.z - d * sin(c)
        )
    }

    private fun tokenize(valStr: String): List<String> {
        val tokens = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuote = false
        for (ch in valStr) {
            when {
                ch == '"' -> inQuote = !inQuote
                ch == ',' && !inQuote -> {
                    tokens.add(cur.toString().trim())
                    cur.clear()
                }
                else -> cur.append(ch)
            }
        }
        tokens.add(cur.toString().trim())
        return tokens
    }
}
