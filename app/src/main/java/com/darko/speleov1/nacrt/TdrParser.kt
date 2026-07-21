package com.darko.speleov1.nacrt

import java.io.DataInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * TopoDroid .tdr binary file parser.
 *
 * Format: big-endian DataOutputStream, tagged records:
 * V (version), S (scrap), I (bbox), N (scrap index), L (line), A (area),
 * P (point), U (user station), X (fixed station), D (display),
 * T (text), F (end scrap), E (end file).
 */
object TdrParser {

    data class TdrPoint(val x: Float, val y: Float, val cp: ControlPoints? = null)
    data class ControlPoints(val cx1: Float, val cy1: Float, val cx2: Float, val cy2: Float)

    data class TdrLine(
        val type: String,
        val group: String,
        val closed: Boolean,
        val color: Int,
        val level: Int,
        val pts: List<TdrPoint>
    )

    data class TdrStation(val name: String, val x: Float, val y: Float, val isUser: Boolean)

    data class TdrPointSymbol(
        val type: String,
        val group: String,
        val x: Float,
        val y: Float,
        val orientation: Float,
        val scale: Int,
        val level: Int
    )

    data class TdrResult(
        val version: Int,
        val scrapName: String,
        val plotType: Int, // 1=plan, 2=profile
        val lines: List<TdrLine>,
        val areas: List<TdrLine>,
        val points: List<TdrPointSymbol>,
        val stations: List<TdrStation>,
        val bbox: FloatArray? // xmin, ymin, xmax, ymax
    )

    fun parse(inputStream: InputStream): TdrResult {
        val dis = DataInputStream(inputStream)

        // V tag
        val vTag = dis.readByte().toInt().toChar()
        require(vTag == 'V') { "Expected V tag, got $vTag" }
        val version = dis.readInt()

        // S tag
        val sTag = dis.readByte().toInt().toChar()
        require(sTag == 'S') { "Expected S tag, got $sTag" }
        val scrapName = dis.readUTF()
        val plotType = dis.readInt()
        dis.readUTF(); dis.readUTF(); dis.readUTF() // palettes

        val lines = mutableListOf<TdrLine>()
        val areas = mutableListOf<TdrLine>()
        val points = mutableListOf<TdrPointSymbol>()
        val stations = mutableListOf<TdrStation>()
        var bbox: FloatArray? = null

        var running = true
        while (running && dis.available() > 0) {
            val tag = dis.readByte().toInt().toChar()

            when (tag) {
                'E' -> running = false

                'I' -> {
                    val xmin = dis.readFloat(); val ymin = dis.readFloat()
                    val xmax = dis.readFloat(); val ymax = dis.readFloat()
                    dis.readInt() // extra
                    bbox = floatArrayOf(xmin, ymin, xmax, ymax)
                }

                'N' -> dis.readInt()
                'F' -> { /* end scrap */ }

                'D' -> {
                    repeat(5) { dis.readFloat() }
                    dis.readUTF()
                    dis.skipBytes(6)
                }

                'L', 'A' -> {
                    val thType = dis.readUTF()
                    val group = dis.readUTF()
                    val closed = dis.readInt() != 0
                    dis.readByte(); dis.readByte()
                    val color = dis.readInt()
                    val level = dis.readInt()
                    dis.readUTF(); dis.readUTF(); dis.readUTF()
                    val nPts = dis.readInt()
                    val pts = mutableListOf<TdrPoint>()
                    repeat(nPts) {
                        val x = dis.readFloat(); val y = dis.readFloat()
                        val hasCp = dis.readByte().toInt()
                        val cp = if (hasCp == 1) {
                            ControlPoints(dis.readFloat(), dis.readFloat(), dis.readFloat(), dis.readFloat())
                        } else null
                        pts.add(TdrPoint(x, y, cp))
                    }
                    if (tag == 'A') dis.readFloat()

                    val line = TdrLine(thType, group, closed, color, level, pts)
                    if (tag == 'A') areas.add(line) else lines.add(line)
                }

                'P' -> {
                    val thType = dis.readUTF()
                    val group = dis.readUTF()
                    val x = dis.readFloat(); val y = dis.readFloat()
                    val orientation = dis.readFloat()
                    val scale = dis.readInt()
                    val level = dis.readInt()
                    dis.readUTF(); dis.readUTF(); dis.readUTF()
                    points.add(TdrPointSymbol(thType, group, x, y, orientation, scale, level))
                }

                'U' -> {
                    val name = dis.readUTF()
                    val x = dis.readFloat(); val y = dis.readFloat()
                    stations.add(TdrStation(name, x, y, isUser = true))
                }

                'X' -> {
                    val x = dis.readFloat(); val y = dis.readFloat()
                    val name = dis.readUTF()
                    dis.readInt(); dis.readInt(); dis.readInt()
                    stations.add(TdrStation(name, x, y, isUser = false))
                }

                'T' -> {
                    dis.readUTF()
                    dis.readFloat(); dis.readFloat()
                }

                else -> {
                    android.util.Log.w("TdrParser", "Unknown tag '$tag'")
                    running = false
                }
            }
        }

        return TdrResult(version, scrapName, plotType, lines, areas, points, stations, bbox)
    }
}
