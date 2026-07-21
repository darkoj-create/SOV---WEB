package com.darko.speleov1.nacrt

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders a NacrtSurvey (parsed TopoDroid ZIP) to a Bitmap.
 * Output is A4-proportioned, suitable for export as PNG or PDF.
 *
 * Scene coordinates: 20 TDR units = 1 m, Y grows DOWN.
 */
object NacrtRenderer {

    data class RenderOptions(
        val title: String = "",
        val date: String = "",
        val team: String = "",
        val club: String = "SO Velebit",
        val cadastreNum: String = "",
        val widthPx: Int = 1190,   // A4 at 144 dpi
        val heightPx: Int = 1684,
        val margin: Int = 50
    )

    private val LINE_COLORS = mapOf(
        "wall" to 0xFF333333.toInt(),
        "pit" to 0xFF333333.toInt(),
        "chimney" to 0xFF555555.toInt(),
        "slope" to 0xFF666666.toInt(),
        "border" to 0xFF333333.toInt(),
        "rock-border" to 0xFF555555.toInt(),
        "contour" to 0xFF888888.toInt(),
        "floor-step" to 0xFF555555.toInt(),
        "ceiling-step" to 0xFF555555.toInt()
    )

    private val LINE_WIDTHS = mapOf(
        "wall" to 4.4f, "pit" to 3f, "chimney" to 2.4f,
        "slope" to 2f, "border" to 3f, "rock-border" to 2.4f,
        "contour" to 1.6f, "floor-step" to 2.4f, "ceiling-step" to 2.4f
    )

    fun render(survey: NacrtZipParser.NacrtSurvey, opts: RenderOptions = RenderOptions()): Bitmap {
        val W = opts.widthPx
        val H = opts.heightPx
        val M = opts.margin

        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFFFFFFFF.toInt())

        val headerH = 180
        val footerH = 60
        val drawTop = M + headerH + 20
        val drawBottom = H - M - footerH
        val drawH = drawBottom - drawTop
        val drawW = W - 2 * M

        // Border
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE; color = 0xFF333333.toInt(); strokeWidth = 3f; isAntiAlias = true
        }
        canvas.drawRect(M.toFloat(), M.toFloat(), (W - M).toFloat(), (H - M).toFloat(), borderPaint)

        // Header
        drawHeader(canvas, M, M, drawW, headerH, survey, opts)

        // Compute extents
        val planExt = survey.plan?.let { getExtent(it) }
        val profileExt = survey.profile?.let { getExtent(it) }
        val hasPlan = planExt != null
        val hasProfile = profileExt != null

        // Layout areas
        val planArea: DrawArea?
        val profileArea: DrawArea?

        if (hasPlan && hasProfile) {
            val splitX = M + (drawW * 0.52f).toInt()
            planArea = DrawArea(M, drawTop, splitX - M - 10, drawH)
            profileArea = DrawArea(splitX + 10, drawTop, W - M - splitX - 10, drawH)

            // Separator
            val sepX = (planArea.left + planArea.width + profileArea.left) / 2f
            val sepPaint = Paint().apply { color = 0xFFCCCCCC.toInt(); strokeWidth = 1f }
            canvas.drawLine(sepX, drawTop.toFloat(), sepX, drawBottom.toFloat(), sepPaint)
        } else if (hasPlan) {
            planArea = DrawArea(M, drawTop, drawW, drawH)
            profileArea = null
        } else if (hasProfile) {
            planArea = null
            profileArea = DrawArea(M, drawTop, drawW, drawH)
        } else {
            planArea = null; profileArea = null
        }

        // Labels
        val labelPaint = Paint().apply {
            color = 0xFF444444.toInt(); textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true; textAlign = Paint.Align.CENTER
        }

        if (planArea != null && survey.plan != null && planExt != null) {
            canvas.drawText("TLOCRT", planArea.left + planArea.width / 2f, drawTop - 4f, labelPaint)
            val tx = makeTransform(planExt, planArea)
            renderDrawing(canvas, survey.plan, survey.sql, tx)
            drawScaleBar(canvas, planArea.left + 30f, drawBottom - 20f, tx.pixPerM)
            drawNorthArrow(canvas, (planArea.left + planArea.width - 40).toFloat(), (drawTop + 60).toFloat())
        }

        if (profileArea != null && survey.profile != null && profileExt != null) {
            canvas.drawText("PROFIL", profileArea.left + profileArea.width / 2f, drawTop - 4f, labelPaint)
            val tx = makeTransform(profileExt, profileArea)
            renderDrawing(canvas, survey.profile, survey.sql, tx)
            drawScaleBar(canvas, profileArea.left + 30f, drawBottom - 20f, tx.pixPerM)
        }

        // Footer
        val footPaint = Paint().apply { color = 0xFF999999.toInt(); textSize = 16f; isAntiAlias = true }
        canvas.drawText("SOV Nacrt Generator", M + 16f, H - M - 16f, footPaint)
        footPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("TopoDroid → PNG", W - M - 16f, H - M - 16f, footPaint)

        return bitmap
    }

    private data class DrawArea(val left: Int, val top: Int, val width: Int, val height: Int)
    private data class Extent(val xmin: Float, val xmax: Float, val ymin: Float, val ymax: Float)

    private data class Transform(
        val offsetX: Float, val offsetY: Float, val scale: Float, val pixPerM: Float,
        val extXmin: Float, val extYmin: Float
    ) {
        fun apply(x: Float, y: Float): Pair<Float, Float> {
            val px = offsetX + (x - extXmin) * scale
            val py = offsetY + (y - extYmin) * scale
            return px to py
        }
    }

    private fun getExtent(tdr: TdrParser.TdrResult): Extent {
        var xmin = Float.MAX_VALUE; var xmax = -Float.MAX_VALUE
        var ymin = Float.MAX_VALUE; var ymax = -Float.MAX_VALUE
        for (line in tdr.lines) for (pt in line.pts) {
            xmin = min(xmin, pt.x); xmax = max(xmax, pt.x)
            ymin = min(ymin, pt.y); ymax = max(ymax, pt.y)
        }
        for (st in tdr.stations) {
            xmin = min(xmin, st.x); xmax = max(xmax, st.x)
            ymin = min(ymin, st.y); ymax = max(ymax, st.y)
        }
        val pad = 20f
        return Extent(xmin - pad, xmax + pad, ymin - pad, ymax + pad)
    }

    private fun makeTransform(ext: Extent, area: DrawArea): Transform {
        val sceneW = ext.xmax - ext.xmin
        val sceneH = ext.ymax - ext.ymin
        val scaleX = area.width / sceneW
        val scaleY = area.height / sceneH
        val scale = min(scaleX, scaleY)
        val offsetX = area.left + (area.width - sceneW * scale) / 2
        val offsetY = area.top + (area.height - sceneH * scale) / 2
        return Transform(offsetX, offsetY, scale, scale * 20f, ext.xmin, ext.ymin)
    }

    private fun renderDrawing(canvas: Canvas, tdr: TdrParser.TdrResult, sql: SurveySqlParser.ParseResult, tx: Transform) {
        // Lines
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE; isAntiAlias = true
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        for (line in tdr.lines) {
            linePaint.color = LINE_COLORS[line.type] ?: 0xFF666666.toInt()
            linePaint.strokeWidth = LINE_WIDTHS[line.type] ?: 2f

            val path = buildPath(line.pts, tx)
            if (line.closed) path.close()
            canvas.drawPath(path, linePaint)
        }

        // Points
        val ptPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        for (pt in tdr.points) {
            val (px, py) = tx.apply(pt.x, pt.y)
            ptPaint.color = 0xFF777777.toInt()
            canvas.drawCircle(px, py, 5f, ptPaint)
        }

        // Stations
        val stPaint = Paint().apply { isAntiAlias = true; color = 0xFFCC0000.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
        val stFill = Paint().apply { isAntiAlias = true; color = 0xFFCC0000.toInt(); style = Paint.Style.FILL }
        val stText = Paint().apply { isAntiAlias = true; color = 0xFFCC0000.toInt(); textSize = 18f }
        for (st in tdr.stations) {
            val (px, py) = tx.apply(st.x, st.y)
            canvas.drawCircle(px, py, 6f, stPaint)
            canvas.drawCircle(px, py, 2f, stFill)
            canvas.drawText(st.name, px + 12, py - 8, stText)
        }

        // Centerline legs
        val clPaint = Paint().apply {
            isAntiAlias = true; color = 0xFFCC0000.toInt(); strokeWidth = 2.4f
            style = Paint.Style.STROKE; pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 4f), 0f)
        }
        val initStation = sql.survey?.initStation ?: return
        val tdrSt0 = tdr.stations.find { it.name == initStation } ?: return
        val sceneScale = 20f

        for (leg in sql.legs) {
            val from = sql.stationCoords[leg.fStation] ?: continue
            val to = sql.stationCoords[leg.tStation] ?: continue

            val (fx, fy) = if (tdr.plotType == 1) {
                (tdrSt0.x + from.e.toFloat() * sceneScale) to (tdrSt0.y - from.n.toFloat() * sceneScale)
            } else {
                (tdrSt0.x + from.e.toFloat() * sceneScale) to (tdrSt0.y - from.z.toFloat() * sceneScale)
            }
            val (toX, toY) = if (tdr.plotType == 1) {
                (tdrSt0.x + to.e.toFloat() * sceneScale) to (tdrSt0.y - to.n.toFloat() * sceneScale)
            } else {
                (tdrSt0.x + to.e.toFloat() * sceneScale) to (tdrSt0.y - to.z.toFloat() * sceneScale)
            }

            val (p1x, p1y) = tx.apply(fx, fy)
            val (p2x, p2y) = tx.apply(toX, toY)
            canvas.drawLine(p1x, p1y, p2x, p2y, clPaint)
        }
    }

    private fun buildPath(pts: List<TdrParser.TdrPoint>, tx: Transform): Path {
        val path = Path()
        for (i in pts.indices) {
            val (px, py) = tx.apply(pts[i].x, pts[i].y)
            if (i == 0) {
                path.moveTo(px, py)
            } else if (pts[i].cp != null) {
                val cp = pts[i].cp!!
                val (c1x, c1y) = tx.apply(cp.cx1, cp.cy1)
                val (c2x, c2y) = tx.apply(cp.cx2, cp.cy2)
                path.cubicTo(c1x, c1y, c2x, c2y, px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        return path
    }

    private fun drawHeader(
        canvas: Canvas, x: Int, y: Int, w: Int, h: Int,
        survey: NacrtZipParser.NacrtSurvey, opts: RenderOptions
    ) {
        val rect = RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat())
        val borderP = Paint().apply { style = Paint.Style.STROKE; color = 0xFF333333.toInt(); strokeWidth = 2f; isAntiAlias = true }
        canvas.drawRect(rect, borderP)

        // Title row
        val lineP = Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 1.5f }
        val titleY = y + 60f
        canvas.drawLine(x.toFloat(), titleY, (x + w).toFloat(), titleY, lineP)

        val titleP = Paint().apply {
            color = 0xFF222222.toInt(); textSize = 32f; isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER
        }
        canvas.drawText(opts.title.ifBlank { survey.name }, x + w / 2f, y + 42f, titleP)

        // Info
        val labelP = Paint().apply { color = 0xFF555555.toInt(); textSize = 18f; isAntiAlias = true }
        val valueP = Paint().apply { color = 0xFF222222.toInt(); textSize = 20f; isAntiAlias = true }

        val col2x = x + (w * 0.45f)
        canvas.drawLine(col2x, titleY, col2x, (y + h).toFloat(), lineP)

        // Left
        val ly = titleY + 28f
        canvas.drawText("Datum:", x + 16f, ly, labelP)
        canvas.drawText(opts.date.ifBlank { survey.date }, x + 100f, ly, valueP)
        canvas.drawText("Mjerili:", x + 16f, ly + 30f, labelP)
        canvas.drawText(opts.team.ifBlank { survey.team }, x + 100f, ly + 30f, valueP)
        canvas.drawText("Klub:", x + 16f, ly + 60f, labelP)
        canvas.drawText(opts.club, x + 100f, ly + 60f, valueP)

        // Right — stats
        val stats = survey.sql.stats
        val fmt = { v: Double -> v.toString().replace('.', ',') }
        canvas.drawText("Duljina:", col2x + 16f, ly, labelP)
        canvas.drawText("${fmt(stats.duljina)} m", col2x + 130f, ly, valueP)
        canvas.drawText("Horizontalna:", col2x + 16f, ly + 30f, labelP)
        canvas.drawText("${fmt(stats.horizontalna)} m", col2x + 160f, ly + 30f, valueP)
        canvas.drawText("Dubina:", col2x + 16f, ly + 60f, labelP)
        canvas.drawText("${fmt(stats.dubina)} m", col2x + 130f, ly + 60f, valueP)
    }

    private fun drawScaleBar(canvas: Canvas, x: Float, y: Float, pixPerM: Float) {
        val candidates = intArrayOf(1, 2, 5, 10, 20, 50)
        var barM = 5
        for (c in candidates) {
            if (c * pixPerM in 60f..300f) { barM = c; break }
        }
        val barPx = barM * pixPerM
        val paint = Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 4f; isAntiAlias = true; strokeCap = Paint.Cap.BUTT }
        canvas.drawLine(x, y, x + barPx, y, paint)
        paint.strokeWidth = 3f
        canvas.drawLine(x, y - 8, x, y + 8, paint)
        canvas.drawLine(x + barPx, y - 8, x + barPx, y + 8, paint)
        val textP = Paint().apply { color = 0xFF333333.toInt(); textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("$barM m", x + barPx / 2, y + 28, textP)
    }

    private fun drawNorthArrow(canvas: Canvas, x: Float, y: Float) {
        val paint = Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 3f; isAntiAlias = true }
        canvas.drawLine(x, y + 40, x, y - 40, paint)
        val arrow = Path().apply {
            moveTo(x, y - 40f); lineTo(x - 10f, y - 24f); lineTo(x + 10f, y - 24f); close()
        }
        paint.style = Paint.Style.FILL
        canvas.drawPath(arrow, paint)
        val textP = Paint().apply {
            color = 0xFF333333.toInt(); textSize = 22f; isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER
        }
        canvas.drawText("S", x, y - 48f, textP)
    }
}
