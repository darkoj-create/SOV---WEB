package com.darko.speleov1

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.pow
import kotlin.random.Random

private enum class RunnerStatus { READY, RUNNING, GAME_OVER }

private val SPELE_SUIT_DARK   = Color(0xFFB71C1C)
private val SPELE_SUIT_MID    = Color(0xFFE53935)
private val SPELE_SUIT_HI     = Color(0xFFFF8A80)
private val SPELE_HELMET_DARK = Color(0xFFF9A825)
private val SPELE_HELMET_MID  = Color(0xFFFFD600)
private val SPELE_HELMET_HI   = Color(0xFFFFF9C4)
private val SPELE_SKIN_DARK   = Color(0xFFD4A574)
private val SPELE_SKIN_MID    = Color(0xFFE8C49A)
private val SPELE_SKIN_HI     = Color(0xFFFFF0DC)
private val SPELE_BOOT_DARK   = Color(0xFFF57F17)
private val SPELE_BOOT_MID    = Color(0xFFFFCA28)
private val SPELE_BOOT_HI     = Color(0xFFFFF59D)
private val SPELE_PACK_DARK   = Color(0xFFF9A825)
private val SPELE_PACK_MID    = Color(0xFFFFD600)
private val SPELE_PACK_HI     = Color(0xFFFFF9C4)
private val SPELE_INK         = Color(0xFF1A0A00)

private enum class RunnerPhase { HORIZONTAL, TRANSITION, VERTICAL, EXIT_VERTICAL, LAKE }
private enum class RunnerObstacleType { PIT, BLIND_PIT, LOW_CEILING, ICE_PATCH, STALAGMITE }

private data class RunnerObstacle(
    val id: Int,
    val type: RunnerObstacleType,
    val x: Float,
    val width: Float
)

private data class RunnerToken(
    val id: Int,
    val x: Float,
    val yFactor: Float,
    val collected: Boolean = false
)

private data class RunnerCollectFlash(
    val x: Float,
    val y: Float,
    val timestamp: Float
)


// Performance caches: reuse heavy drawing/text objects across frames.
private val _pathA = Path()
private val _pathB = Path()
private val _pathC = Path()
private val _pathD = Path()
private val _pathE = Path()
private val _wobblyPath = Path()
private val _newObstaclesBuffer = ArrayList<RunnerObstacle>(32)
private val _activeObstaclesBuffer = ArrayList<RunnerObstacle>(32)
private val _newTokensBuffer = ArrayList<RunnerToken>(32)
private val _activeTokensBuffer = ArrayList<RunnerToken>(32)
private var _smoothedRunnerDt = 0.016f

private val _textPaintCache = HashMap<Long, android.graphics.Paint>()
private val _shadowPaintCache = HashMap<Long, android.graphics.Paint>()

private fun cachedTextPaint(
    color: Int,
    textSize: Float,
    align: android.graphics.Paint.Align
): android.graphics.Paint {
    val key = (color.toLong() shl 32) xor textSize.toBits().toLong() xor align.ordinal.toLong()
    return _textPaintCache.getOrPut(key) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = color
            this.textSize = textSize
            textAlign = align
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
        }
    }
}

private fun cachedShadowPaint(
    textSize: Float,
    align: android.graphics.Paint.Align
): android.graphics.Paint {
    val color = android.graphics.Color.argb(230, 26, 10, 0)
    val key = (color.toLong() shl 32) xor textSize.toBits().toLong() xor align.ordinal.toLong()
    return _shadowPaintCache.getOrPut(key) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = color
            this.textSize = textSize
            textAlign = align
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
        }
    }
}

private val _indieRunnerShadowPaint = android.graphics.Paint().apply {
    isAntiAlias = true
}
private val _indieRunnerTextPaint = android.graphics.Paint().apply {
    isAntiAlias = true
}

private fun wobblyPath(path: Path, px: Float): Path {
    _wobblyPath.reset()
    _wobblyPath.addPath(path, Offset(1.8f * px, -1.05f * px))
    return _wobblyPath
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWobblyPathOutline(path: Path, px: Float) {
    drawPath(
        wobblyPath(path, px),
        Color(0xFF1A1008),
        style = Stroke(width = 4.0f * px)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWobblyRoundRectOutline(
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    px: Float
) {
    drawRoundRect(
        color = Color(0xFF1A1008),
        topLeft = topLeft + Offset(1.8f * px, -1.05f * px),
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 4.0f * px)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIndieRunnerText(
    text: String,
    x: Float,
    y: Float,
    textSize: Float,
    textColor: Int,
    align: android.graphics.Paint.Align = android.graphics.Paint.Align.CENTER
) {
    val shadow = cachedShadowPaint(textSize, align)
    val paint = cachedTextPaint(textColor, textSize, align)
    drawContext.canvas.nativeCanvas.drawText(text, x + 2.2f, y + 2.2f, shadow)
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private fun Float.powInt(n: Int): Float = this.toDouble().pow(n.toDouble()).toFloat()

private fun blendColor(from: Color, to: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * k,
        green = from.green + (to.green - from.green) * k,
        blue = from.blue + (to.blue - from.blue) * k,
        alpha = from.alpha + (to.alpha - from.alpha) * k
    )
}

private fun runnerBiomeName(biomeIndex: Int): String = when (biomeIndex % 10) {
    0 -> "🌿 Ulaz u špilju"
    1 -> "💧 Mokra špilja"
    2 -> "💎 Kristalna dvorana"
    3 -> "🔥 Lava tunel"
    4 -> "❄️ Ledena špilja"
    5 -> "🦴 Fosilna dvorana"
    6 -> "🌱 Aragonitna špilja"
    7 -> "🦇 Guano dvorana"
    8 -> "🌋 Vulkanski tunel"
    else -> "⚪ Gipsana kristalna dvorana"
}

private data class SierraVgaPalette(
    val darkest: Color,
    val dark: Color,
    val mid: Color,
    val midLight: Color,
    val light: Color,
    val highlight: Color,
    val outline: Color,
    val floorEdge: Color,
    val ceilingEdge: Color
)

// Compatibility aliases for older runner draw code; all biome colors still come from SierraVgaPalette.
private val SierraVgaPalette.midDark: Color get() = dark
private val SierraVgaPalette.accent: Color get() = floorEdge

private fun runnerSierraPalette(biomeIndex: Int): SierraVgaPalette = when (biomeIndex % 10) {
    0 -> SierraVgaPalette(
        darkest = Color(0xFF1A0D08), dark = Color(0xFF2E1A10), mid = Color(0xFF5D3A28),
        midLight = Color(0xFF8D6040), light = Color(0xFFC49A6C), highlight = Color(0xFFF5DEB3),
        outline = Color(0xFF000000), floorEdge = Color(0xFFFFD6A0), ceilingEdge = Color(0xFFC49A6C)
    )
    1 -> SierraVgaPalette(
        darkest = Color(0xFF041E26), dark = Color(0xFF062A30), mid = Color(0xFF0D5C6A),
        midLight = Color(0xFF1A8C9A), light = Color(0xFF4FC3D7), highlight = Color(0xFFB2EBF2),
        outline = Color(0xFF000000), floorEdge = Color(0xFF80DEEA), ceilingEdge = Color(0xFF4DD0E1)
    )
    2 -> SierraVgaPalette(
        darkest = Color(0xFF150720), dark = Color(0xFF240047), mid = Color(0xFF5E35B1),
        midLight = Color(0xFF7E57C2), light = Color(0xFFB39DDB), highlight = Color(0xFFEDE7F6),
        outline = Color(0xFF000000), floorEdge = Color(0xFFCE93D8), ceilingEdge = Color(0xFF9575CD)
    )
    3 -> SierraVgaPalette(
        darkest = Color(0xFF1A0000), dark = Color(0xFF3D0000), mid = Color(0xFF8B1A00),
        midLight = Color(0xFFC62800), light = Color(0xFFFF5722), highlight = Color(0xFFFFCCBC),
        outline = Color(0xFF000000), floorEdge = Color(0xFFFF6D00), ceilingEdge = Color(0xFFFF3D00)
    )
    4 -> SierraVgaPalette(
        darkest = Color(0xFF061828), dark = Color(0xFF0D2B4A), mid = Color(0xFF1565C0),
        midLight = Color(0xFF1976D2), light = Color(0xFF64B5F6), highlight = Color(0xFFE3F2FD),
        outline = Color(0xFF000000), floorEdge = Color(0xFF82B1FF), ceilingEdge = Color(0xFF448AFF)
    )
    5 -> SierraVgaPalette(
        darkest = Color(0xFF1A1508), dark = Color(0xFF2E2510), mid = Color(0xFF6B5A3E),
        midLight = Color(0xFF9C8560), light = Color(0xFFD4B896), highlight = Color(0xFFFFF8E7),
        outline = Color(0xFF000000), floorEdge = Color(0xFFFFF8E7), ceilingEdge = Color(0xFFD4B896)
    )
    6 -> SierraVgaPalette(
        darkest = Color(0xFF081A10), dark = Color(0xFF0D2B18), mid = Color(0xFF1B5E35),
        midLight = Color(0xFF2E8B57), light = Color(0xFF66BB6A), highlight = Color(0xFFE8F5E9),
        outline = Color(0xFF000000), floorEdge = Color(0xFFE8F5E9), ceilingEdge = Color(0xFF66BB6A)
    )
    7 -> SierraVgaPalette(
        darkest = Color(0xFF0D0A00), dark = Color(0xFF1A1400), mid = Color(0xFF4A3800),
        midLight = Color(0xFF7A5C00), light = Color(0xFFB8860B), highlight = Color(0xFFFFE066),
        outline = Color(0xFF000000), floorEdge = Color(0xFFFFE066), ceilingEdge = Color(0xFFB8860B)
    )
    8 -> SierraVgaPalette(
        darkest = Color(0xFF0A0000), dark = Color(0xFF1A0500), mid = Color(0xFF7F1500),
        midLight = Color(0xFFBF3600), light = Color(0xFFFF6E00), highlight = Color(0xFFFFD180),
        outline = Color(0xFF000000), floorEdge = Color(0xFFFF6E00), ceilingEdge = Color(0xFFFFD180)
    )
    else -> SierraVgaPalette(
        darkest = Color(0xFF0D0D12), dark = Color(0xFF1A1A2E), mid = Color(0xFF4A4A6A),
        midLight = Color(0xFF7A7A9A), light = Color(0xFFC0C0D8), highlight = Color(0xFFFFFFFF),
        outline = Color(0xFF000000), floorEdge = Color(0xFFFFFFFF), ceilingEdge = Color(0xFFC0C0D8)
    )
}

private fun runnerBiomeBack(biomeIndex: Int): Color = runnerSierraPalette(biomeIndex).dark
private fun runnerBiomeMid(biomeIndex: Int): Color = runnerSierraPalette(biomeIndex).mid
private fun runnerBiomeLight(biomeIndex: Int): Color = runnerSierraPalette(biomeIndex).light
private fun runnerBiomeAccent(biomeIndex: Int): Color = runnerSierraPalette(biomeIndex).floorEdge

private fun runnerBiomePitInner(biomeIndex: Int, blind: Boolean): Color {
    val p = runnerSierraPalette(biomeIndex)
    return if (blind) p.outline.copy(alpha = 0.98f) else p.darkest.copy(alpha = 0.96f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSierraShape(
    path: Path,
    baseFill: Color,
    midFill: Color,
    highlightFill: Color,
    px: Float,
    alpha: Float = 1f,
    detail: Boolean = true
) {
    val a = alpha.coerceIn(0f, 1f)
    if (a <= 0.01f) return

    // Sierra/VGA pass order: chunky outside ink, flat shadow/base, flat mid fill,
    // then a small top-left highlight pass. No gradients inside solid shapes.
    drawPath(
        Path().apply { addPath(path, Offset(2.2f * px, 2.4f * px)) },
        Color.Black.copy(alpha = 0.64f),
        style = Stroke(width = 6.5f * px)
    )
    drawPath(path, baseFill.copy(alpha = baseFill.alpha * a))
    drawPath(Path().apply { addPath(path, Offset(-0.75f * px, -0.65f * px)) }, midFill.copy(alpha = midFill.alpha * a))
    drawPath(
        path,
        Color.Black.copy(alpha = 1.0f),
        style = Stroke(width = 4.8f * px)
    )
    drawPath(
        Path().apply { addPath(path, Offset(-1.5f * px, -1.5f * px)) },
        highlightFill.copy(alpha = 0.55f),
        style = Stroke(width = 2.0f * px)
    )
    if (detail) {
        drawPath(Path().apply { addPath(path, Offset(0.9f * px, 0.9f * px)) }, Color(0xFF2B1608).copy(alpha = 0.55f * a), style = Stroke(width = 1.15f * px))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSierraCircle(
    center: Offset,
    radius: Float,
    baseFill: Color,
    midFill: Color,
    highlightFill: Color,
    px: Float,
    alpha: Float = 1f
) {
    val a = alpha.coerceIn(0f, 1f)
    drawCircle(Color.Black.copy(alpha = 0.46f * a), radius = radius + 2.5f * px, center = center + Offset(1.5f * px, 1.7f * px))
    drawCircle(baseFill.copy(alpha = baseFill.alpha * a), radius = radius, center = center)
    drawCircle(midFill.copy(alpha = midFill.alpha * a), radius = radius * 0.91f, center = center + Offset(-0.7f * px, -0.55f * px))
    drawCircle(Color.Black.copy(alpha = 0.98f * a), radius = radius, center = center, style = Stroke(width = 3.3f * px))
    drawCircle(highlightFill.copy(alpha = highlightFill.alpha * 0.42f * a), radius = radius * 0.34f, center = center + Offset(-radius * 0.32f, -radius * 0.36f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSierraRoundRect(
    topLeft: Offset,
    size: Size,
    radius: Float,
    baseFill: Color,
    midFill: Color,
    highlightFill: Color,
    px: Float,
    alpha: Float = 1f
) {
    val a = alpha.coerceIn(0f, 1f)
    drawRoundRect(Color.Black.copy(alpha = 0.48f * a), topLeft + Offset(1.6f * px, 1.8f * px), size, CornerRadius(radius), style = Stroke(width = 4.6f * px))
    drawRoundRect(baseFill.copy(alpha = baseFill.alpha * a), topLeft, size, CornerRadius(radius))
    drawRoundRect(midFill.copy(alpha = midFill.alpha * a), topLeft + Offset(-0.8f * px, -0.7f * px), Size(size.width, size.height), CornerRadius(radius))
    drawRoundRect(Color.Black.copy(alpha = 0.96f * a), topLeft, size, CornerRadius(radius), style = Stroke(width = 3.5f * px))
    drawLine(highlightFill.copy(alpha = highlightFill.alpha * 0.45f * a), topLeft + Offset(radius * 0.75f, 2f * px), topLeft + Offset(size.width - radius * 0.8f, 2f * px), strokeWidth = 1.3f * px)
    drawLine(highlightFill.copy(alpha = highlightFill.alpha * 0.34f * a), topLeft + Offset(2f * px, radius * 0.75f), topLeft + Offset(2f * px, size.height - radius * 0.8f), strokeWidth = 1.15f * px)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSierraLimb(
    a: Offset,
    joint: Offset,
    b: Offset,
    width: Float,
    baseFill: Color,
    midFill: Color,
    highlightFill: Color,
    px: Float,
    alpha: Float = 1f
) {
    // Limbs stay geometric and readable: thick black contour, flat suit color, small top-left highlight.
    val aa = alpha.coerceIn(0f, 1f)
    drawLine(Color.Black.copy(alpha = 0.96f * aa), a, joint, strokeWidth = width + 3.2f * px, cap = StrokeCap.Round)
    drawLine(Color.Black.copy(alpha = 0.96f * aa), joint, b, strokeWidth = width + 3.2f * px, cap = StrokeCap.Round)
    drawLine(baseFill.copy(alpha = baseFill.alpha * aa), a, joint, strokeWidth = width, cap = StrokeCap.Round)
    drawLine(baseFill.copy(alpha = baseFill.alpha * aa), joint, b, strokeWidth = width, cap = StrokeCap.Round)
    drawLine(midFill.copy(alpha = midFill.alpha * aa), a + Offset(-0.5f * px, -0.5f * px), joint + Offset(-0.5f * px, -0.5f * px), strokeWidth = width * 0.68f, cap = StrokeCap.Round)
    drawLine(midFill.copy(alpha = midFill.alpha * aa), joint + Offset(-0.5f * px, -0.5f * px), b + Offset(-0.5f * px, -0.5f * px), strokeWidth = width * 0.68f, cap = StrokeCap.Round)
    drawLine(highlightFill.copy(alpha = 0.35f * aa), a + Offset(-1f * px, -1f * px), joint + Offset(-1f * px, -1f * px), strokeWidth = 1.2f * px, cap = StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInkPath(path: Path, fill: Color, px: Float, highlight: Boolean = true) {
    val mid = if (highlight) blendColor(fill, Color.White, 0.18f) else fill
    val hi = if (highlight) blendColor(fill, Color.White, 0.38f) else fill
    drawSierraShape(path, blendColor(fill, Color.Black, 0.34f), mid, hi, px, alpha = fill.alpha)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPaperGrain(w: Float, h: Float, px: Float, phaseTime: Float) {
    // VGA scanlines — horizontalne linije svakih 3px, blaga tamnost
    val lineSpacing = 3f * px
    var scanY = 0f
    while (scanY < h) {
        drawLine(
            Color.Black.copy(alpha = 0.055f),
            Offset(0f, scanY),
            Offset(w, scanY),
            strokeWidth = px
        )
        scanY += lineSpacing
    }
    // Subtle pixel shimmer — mali bijeli pikseli na rubovima
    val tick = (phaseTime * 6f).toInt()
    repeat(28) { i ->
        val x = ((i * 153.7f + tick * 31f) * px) % w
        val y = ((i * 89.3f + tick * 17f) * px) % h
        drawRect(
            color = Color.White.copy(alpha = 0.018f),
            topLeft = Offset(x, y),
            size = Size(px, px)
        )
    }
}



private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRunnerBiomeCard(
    w: Float,
    h: Float,
    px: Float,
    phaseTime: Float,
    status: RunnerStatus,
    biomeIndex: Int,
    level: Int = 1
) {
    if (status != RunnerStatus.RUNNING || phaseTime >= 3.5f) return
    val intro = (phaseTime / 0.55f).coerceIn(0f, 1f)
    val fadeOut = (1f - ((phaseTime - 2.75f).coerceAtLeast(0f) / 0.75f)).coerceIn(0f, 1f)
    val alpha = (intro * fadeOut).coerceIn(0f, 1f)
    val slideX = (1f - intro) * 120f * px
    val cardW = w * 0.76f
    val cardH = 62f * px
    val topLeft = Offset(w * 0.12f + slideX, h * 0.38f)
    // VGA-style chunky border — dvostruki outline behind the card
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.90f * alpha),
        topLeft = topLeft + Offset(-2f * px, -2f * px),
        size = Size(cardW + 4f * px, cardH + 4f * px),
        cornerRadius = CornerRadius(18f * px)
    )
    drawRoundRect(
        color = runnerBiomeAccent(biomeIndex).copy(alpha = 0.55f * alpha),
        topLeft = topLeft + Offset(-1f * px, -1f * px),
        size = Size(cardW + 2f * px, cardH + 2f * px),
        cornerRadius = CornerRadius(17f * px)
    )
    drawRoundRect(Color(0xFF0D0804).copy(alpha = alpha), topLeft, Size(cardW, cardH), CornerRadius(16f * px))
    drawRoundRect(Color.Black.copy(alpha = 0.95f * alpha), topLeft, Size(cardW, cardH), CornerRadius(16f * px), style = Stroke(width = 3.5f * px))
    drawLine(runnerBiomeAccent(biomeIndex).copy(alpha = 0.75f * alpha), Offset(w * 0.18f + slideX, h * 0.38f + 3f * px), Offset(w * 0.82f + slideX, h * 0.38f + 3f * px), strokeWidth = 1.5f * px)
    drawIndieRunnerText(
        text = runnerBiomeName(biomeIndex),
        x = topLeft.x + cardW * 0.50f,
        y = topLeft.y + 27f * px,
        textSize = 16f * px,
        textColor = android.graphics.Color.argb((255 * alpha).toInt().coerceIn(0, 255), 255, 248, 225)
    )
    drawIndieRunnerText(
        text = "Level $level",
        x = topLeft.x + cardW * 0.50f,
        y = topLeft.y + 48f * px,
        textSize = 11f * px,
        textColor = android.graphics.Color.argb((230 * alpha).toInt().coerceIn(0, 255), 255, 213, 79)
    )
}


@Composable
internal fun SpeleoRunnerScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember { context.getSharedPreferences("speleo_runner", Context.MODE_PRIVATE) }

    var status by remember { mutableStateOf(RunnerStatus.READY) }
    var phase by remember { mutableStateOf(RunnerPhase.HORIZONTAL) }
    var restartNonce by remember { mutableIntStateOf(0) }
    var phaseTime by remember { mutableFloatStateOf(0f) }
    var transitionTime by remember { mutableFloatStateOf(0f) }
    var walkMeters by remember { mutableFloatStateOf(0f) }
    var depthMeters by remember { mutableFloatStateOf(0f) }
    var segmentDepth by remember { mutableFloatStateOf(0f) }
    var highScore by remember { mutableIntStateOf(prefs.getInt("high_score_total_m", 0)) }
    var highScoreBats by remember { mutableIntStateOf(prefs.getInt("high_score_bats", 0)) }
    var gearCount by remember { mutableIntStateOf(0) }
    var playerLift by remember { mutableFloatStateOf(0f) }
    var playerVelocity by remember { mutableFloatStateOf(0f) }
    var crawlLeft by remember { mutableFloatStateOf(0f) }
    var crawlPressed by remember { mutableStateOf(false) }
    var spawnCooldown by remember { mutableFloatStateOf(1.35f) }
    var tokenCooldown by remember { mutableFloatStateOf(0.95f) }
    var nextId by remember { mutableIntStateOf(1) }
    var obstacles by remember { mutableStateOf(emptyList<RunnerObstacle>()) }
    var tokens by remember { mutableStateOf(emptyList<RunnerToken>()) }
    var gameWidth by remember { mutableIntStateOf(1) }
    var gameHeight by remember { mutableIntStateOf(1) }
    var playerName by remember { mutableStateOf(prefs.getString("player_name", "") ?: "") }
    var playerNameDraft by remember { mutableStateOf(playerName) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var leaderboardTab by remember { mutableIntStateOf(0) }
    var leaderboard by remember { mutableStateOf<List<SpeleoRunnerLeaderboardEntry>>(emptyList()) }
    var leaderboardStatus by remember { mutableStateOf("Učitavam high scoreove…") }
    var pendingSubmitScore by remember { mutableIntStateOf(0) }
    var pendingSubmitBats by remember { mutableIntStateOf(0) }
    var playerLane by remember { mutableIntStateOf(1) }
    var hazardLane by remember { mutableIntStateOf(0) }
    var batLane by remember { mutableIntStateOf(2) }
    var nextHazardDepth by remember { mutableFloatStateOf(4.8f) }
    var nextVerticalBatDepth by remember { mutableFloatStateOf(2.4f) }
    var nextFallingRockDepth by remember { mutableFloatStateOf(5.8f) }
    var fallingRockLane by remember { mutableIntStateOf(0) }
    var nextBatSwarmDepth by remember { mutableFloatStateOf(6.8f) }
    var batSwarmLane by remember { mutableIntStateOf(1) }
    var batSwarmTimer by remember { mutableFloatStateOf(0f) }
    var batSwarmDirection by remember { mutableIntStateOf(1) }
    var cobwebLane by remember { mutableIntStateOf(0) }
    var cobwebTimer by remember { mutableFloatStateOf(0f) }
    var nextCobwebDepth by remember { mutableFloatStateOf(8.5f) }
    var icyRopeLane by remember { mutableIntStateOf(1) }
    var icyRopeTimer by remember { mutableFloatStateOf(0f) }
    var nextIcyRopeDepth by remember { mutableFloatStateOf(6.5f) }
    var slowdownTimer by remember { mutableFloatStateOf(0f) }
    var speedBoostTimer by remember { mutableFloatStateOf(0f) }
    var verticalBackgroundIndex by remember { mutableIntStateOf(0) }
    var horizontalBackgroundIndex by remember { mutableIntStateOf(0) }
    var horizontalCycles by remember { mutableIntStateOf(0) }
    var phaseSequenceLevel by remember { mutableIntStateOf(0) }
    var verticalExitTime by remember { mutableFloatStateOf(0f) }
    var anchorWindowTime by remember { mutableFloatStateOf(0f) }
    var anchorPressed by remember { mutableStateOf(false) }
    var anchorFallTime by remember { mutableFloatStateOf(0f) }
    var anchorFailing by remember { mutableStateOf(false) }
    var horizontalPhaseDuration by remember { mutableFloatStateOf(20f) }
    var caveCeilingOffset by remember { mutableFloatStateOf(0f) }
    var caveCeilingTarget by remember { mutableFloatStateOf(0f) }
    var caveCeilingChangeTimer by remember { mutableFloatStateOf(0f) }
    var lakeDuration by remember { mutableFloatStateOf(10f) }
    var lakeBoatLift by remember { mutableFloatStateOf(0f) }
    var lakeBoatVelocity by remember { mutableFloatStateOf(0f) }
    var lakePaddleTimer by remember { mutableFloatStateOf(0f) }
    var lakeShellCooldown by remember { mutableFloatStateOf(1.4f) }
    var lakeSharkActive by remember { mutableStateOf(false) }
    var lakeSharkX by remember { mutableFloatStateOf(-200f) }
    var lakeSharkTimer by remember { mutableFloatStateOf(5f) }
    var lakePaddleBoost by remember { mutableFloatStateOf(0f) }
    var lakeRockObstacles by remember { mutableStateOf(emptyList<RunnerObstacle>()) }
    var lakeRockCooldown by remember { mutableFloatStateOf(4.5f) }
    var lakeJumpVelocity by remember { mutableFloatStateOf(0f) }
    val lakeGravity = 520f
    var lakeSharkEscaped by remember { mutableStateOf(false) }
    var lakePaddleTapCount by remember { mutableIntStateOf(0) }
    var fallingObstacle by remember { mutableStateOf<RunnerObstacle?>(null) }
    var fallAnimationTime by remember { mutableFloatStateOf(0f) }
    var shakeOffset by remember { mutableStateOf(Offset.Zero) }
    val collectFlash = remember { mutableStateListOf<RunnerCollectFlash>() }
    var runnerPaused by remember { mutableStateOf(false) }
    var soundMuted by remember { mutableStateOf(prefs.getBoolean("runner_sound_muted", false)) }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    fun playRunnerTone(toneType: Int, durationMs: Int = 70) {
        if (!soundMuted) runCatching { toneGenerator.startTone(toneType, durationMs) }
    }

    val totalMeters = (walkMeters + depthMeters).toInt()
    val hudScoreMeters = (walkMeters + depthMeters).roundToInt().coerceAtLeast(0)
    val hudWalkMeters = walkMeters.roundToInt().coerceAtLeast(0)
    val hudDepthMeters = depthMeters.roundToInt().coerceAtLeast(0)
    val hudLevel = (hudScoreMeters / 144) + 1

    LaunchedEffect(Unit) {
        val resetKey = "high_score_reset_v142_done"
        if (!prefs.getBoolean(resetKey, false)) {
            prefs.edit()
                .putInt("high_score_seconds", 0)
                .putInt("high_score", 0)
                .putInt("high_score_total_m", 0)
                .putInt("high_score_bats", 0)
                .putBoolean(resetKey, true)
                .apply()
            highScore = 0
            highScoreBats = 0
        }
        try {
            leaderboard = SpeleoRunnerLeaderboardClient.refresh(context)
            leaderboardStatus = if (leaderboard.isEmpty()) "Nema spremljenih scoreova ili nema interneta." else "Zadnji dostupni high scoreovi"
        } catch (_: Throwable) {
            leaderboardStatus = "Leaderboard trenutno nije dostupan; igra radi lokalno."
            leaderboard = emptyList()
        }
    }

    LaunchedEffect(pendingSubmitScore, pendingSubmitBats) {
        val scoreToSubmit = pendingSubmitScore
        val batsToSubmit = pendingSubmitBats
        if (scoreToSubmit > 0 && playerName.isNotBlank()) {
            try {
                SpeleoRunnerLeaderboardClient.submitOrQueue(context, playerName, scoreToSubmit, batsToSubmit)
                leaderboard = SpeleoRunnerLeaderboardClient.refresh(context)
                leaderboardStatus = if (leaderboard.isEmpty()) "Score spremljen lokalno; leaderboard će se osvježiti kad bude dostupan." else "Zadnji dostupni high scoreovi"
            } catch (_: Throwable) {
                leaderboardStatus = "Score je ostao lokalno; leaderboard trenutno nije dostupan."
            }
            pendingSubmitScore = 0
            pendingSubmitBats = 0
        }
    }

    val jumpVelocity = with(density) { 390.dp.toPx() }
    val gravity = with(density) { 640.dp.toPx() }
    val playerWidth = with(density) { 36.dp.toPx() }
    val standingHeight = with(density) { 66.dp.toPx() }
    val crawlHeight = with(density) { 38.dp.toPx() }
    val minGap = with(density) { 34.dp.toPx() }
    val maxGap = with(density) { 46.dp.toPx() }
    val lowWidth = with(density) { 62.dp.toPx() }
    val rockWidth = with(density) { 30.dp.toPx() }

    fun totalScore(): Int = (walkMeters + depthMeters).roundToInt().coerceAtLeast(0)

    fun resetGame(start: Boolean) {
        status = if (start) RunnerStatus.RUNNING else RunnerStatus.READY
        runnerPaused = false
        phase = RunnerPhase.HORIZONTAL
        phaseTime = 0f
        transitionTime = 0f
        walkMeters = 0f
        depthMeters = 0f
        segmentDepth = 0f
        gearCount = 0
        playerLift = 0f
        playerVelocity = 0f
        crawlLeft = 0f
        crawlPressed = false
        spawnCooldown = 4.85f
        tokenCooldown = 4.95f
        nextId = 1
        obstacles = emptyList()
        tokens = emptyList()
        playerLane = 1
        hazardLane = Random.nextInt(0, 3)
        batLane = Random.nextInt(0, 3)
        nextHazardDepth = 4.8f
        nextVerticalBatDepth = 2.4f
        nextFallingRockDepth = Random.nextDouble(7.5, 11.5).toFloat()
        fallingRockLane = Random.nextInt(0, 3)
        nextBatSwarmDepth = Random.nextDouble(6.0, 9.0).toFloat()
        batSwarmLane = Random.nextInt(0, 3)
        batSwarmTimer = 0f
        batSwarmDirection = if (Random.nextBoolean()) 1 else -1
        cobwebLane = Random.nextInt(0, 3)
        cobwebTimer = 0f
        nextCobwebDepth = Random.nextDouble(8.0, 13.0).toFloat()
        icyRopeLane = Random.nextInt(0, 3)
        icyRopeTimer = 0f
        nextIcyRopeDepth = Random.nextDouble(6.8, 11.5).toFloat()
        slowdownTimer = 0f
        speedBoostTimer = 0f
        horizontalBackgroundIndex = 0
        verticalBackgroundIndex = 1
        horizontalCycles = 0
        phaseSequenceLevel = 0
        verticalExitTime = 0f
        anchorWindowTime = 0f
        anchorPressed = false
        anchorFallTime = 0f
        anchorFailing = false
        horizontalPhaseDuration = Random.nextDouble(20.0, 35.0).toFloat()
        caveCeilingOffset = 0f
        caveCeilingTarget = 0f
        caveCeilingChangeTimer = Random.nextDouble(8.0, 18.0).toFloat()
        lakeDuration = 10f
        lakeBoatLift = 0f
        lakeBoatVelocity = 0f
        lakePaddleTimer = 0f
        lakeShellCooldown = 1.4f
        lakeSharkActive = true
        lakeSharkX = -90f * with(density) { 1.dp.toPx() }
        lakeSharkTimer = 99f
        lakePaddleBoost = 0f
        lakeRockObstacles = emptyList()
        lakeRockCooldown = 999f
        lakeJumpVelocity = 0f
        lakeSharkEscaped = false
        lakePaddleTapCount = 0
        fallingObstacle = null
        fallAnimationTime = 0f
        shakeOffset = Offset.Zero
        collectFlash.clear()
        restartNonce++
    }

    fun requestStart() {
        if (playerName.isBlank()) {
            playerNameDraft = ""
            showNameDialog = true
        } else {
            resetGame(start = true)
        }
    }

    fun startTransitionPhase() {
        playRunnerTone(ToneGenerator.TONE_PROP_BEEP, 85)
        phaseSequenceLevel += 1
        // TODO SOUND: play "cave_wind_ambient" u TRANSITION fazi
        verticalBackgroundIndex = (horizontalBackgroundIndex + 1) % 10
        phase = RunnerPhase.TRANSITION
        phaseTime = 0f
        transitionTime = 0f
        anchorWindowTime = 0f
        anchorPressed = false
        anchorFallTime = 0f
        anchorFailing = false
        obstacles = emptyList()
        tokens = emptyList()
        playerLift = 0f
        playerVelocity = 0f
        crawlLeft = 0f
        crawlPressed = false
        slowdownTimer = 0f
        speedBoostTimer = 0f
    }

    fun startVerticalPhase() {
        playRunnerTone(ToneGenerator.TONE_PROP_ACK, 90)
        // TODO SOUND: play "rope_clip_click" kad player prijeđe u VERTICAL
        // TODO SOUND: play "water_drip_echo" pri ulasku u Biome 1
        phase = RunnerPhase.VERTICAL
        phaseTime = 0f
        transitionTime = 0f
        anchorWindowTime = 0f
        anchorPressed = false
        anchorFallTime = 0f
        anchorFailing = false
        segmentDepth = 0f
        obstacles = emptyList()
        tokens = emptyList()
        playerLift = 0f
        playerVelocity = 0f
        crawlLeft = 0f
        crawlPressed = false
        playerLane = 1
        hazardLane = Random.nextInt(0, 3)
        batLane = Random.nextInt(0, 3)
        nextHazardDepth = 4.8f
        nextVerticalBatDepth = 2.2f
        nextFallingRockDepth = Random.nextDouble(7.5, 11.5).toFloat()
        fallingRockLane = Random.nextInt(0, 3)
        nextBatSwarmDepth = Random.nextDouble(6.0, 9.0).toFloat()
        batSwarmLane = Random.nextInt(0, 3)
        batSwarmTimer = 0f
        batSwarmDirection = if (Random.nextBoolean()) 1 else -1
        cobwebLane = Random.nextInt(0, 3)
        cobwebTimer = 0f
        nextCobwebDepth = Random.nextDouble(8.0, 13.0).toFloat()
        icyRopeLane = Random.nextInt(0, 3)
        icyRopeTimer = 0f
        nextIcyRopeDepth = Random.nextDouble(6.8, 11.5).toFloat()
        slowdownTimer = 0f
        speedBoostTimer = 0f
    }

    fun startHorizontalPhase() {
        phaseSequenceLevel += 1
        phase = RunnerPhase.HORIZONTAL
        phaseTime = 0f
        transitionTime = 0f
        verticalExitTime = 0f
        anchorWindowTime = 0f
        anchorPressed = false
        anchorFallTime = 0f
        anchorFailing = false
        segmentDepth = 0f
        horizontalCycles += 1
        horizontalBackgroundIndex = (verticalBackgroundIndex + 1) % 10
        horizontalPhaseDuration = Random.nextDouble(20.0, 35.0).toFloat()
        caveCeilingOffset = 0f
        caveCeilingTarget = 0f
        caveCeilingChangeTimer = Random.nextDouble(8.0, 18.0).toFloat()
        spawnCooldown = 1.45f
        tokenCooldown = 0.85f
        obstacles = emptyList()
        tokens = emptyList()
        playerLift = 0f
        playerVelocity = 0f
        crawlLeft = 0f
        crawlPressed = false
        nextFallingRockDepth = Random.nextDouble(7.5, 11.5).toFloat()
        fallingRockLane = Random.nextInt(0, 3)
        nextBatSwarmDepth = Random.nextDouble(6.0, 9.0).toFloat()
        batSwarmLane = Random.nextInt(0, 3)
        batSwarmTimer = 0f
        batSwarmDirection = if (Random.nextBoolean()) 1 else -1
        cobwebLane = Random.nextInt(0, 3)
        cobwebTimer = 0f
        nextCobwebDepth = Random.nextDouble(8.0, 13.0).toFloat()
        icyRopeLane = Random.nextInt(0, 3)
        icyRopeTimer = 0f
        nextIcyRopeDepth = Random.nextDouble(6.8, 11.5).toFloat()
        slowdownTimer = 0f
        speedBoostTimer = 0f
        lakeBoatLift = 0f
        lakeBoatVelocity = 0f
        lakePaddleTimer = 0f
        lakeShellCooldown = 1.4f
        lakeSharkActive = true
        lakeSharkX = -90f * with(density) { 1.dp.toPx() }
        lakeSharkTimer = 99f
        lakePaddleBoost = 0f
        lakeRockObstacles = emptyList()
        lakeRockCooldown = 999f
        lakeJumpVelocity = 0f
        lakeSharkEscaped = false
        lakePaddleTapCount = 0
    }

    fun startLakePhase() {
        playRunnerTone(ToneGenerator.TONE_PROP_BEEP2, 110)
        phaseSequenceLevel += 1
        phase = RunnerPhase.LAKE
        phaseTime = 0f
        lakeDuration = 10f
        lakeBoatLift = 0f
        lakeBoatVelocity = 0f
        lakePaddleTimer = 0f
        lakeShellCooldown = 1.0f
        lakeSharkActive = true
        lakeSharkX = -90f * with(density) { 1.dp.toPx() }
        lakeSharkTimer = 99f
        lakePaddleBoost = 0f
        lakeRockObstacles = emptyList()
        lakeRockCooldown = 999f
        lakeJumpVelocity = 0f
        lakeSharkEscaped = false
        lakePaddleTapCount = 0
        spawnCooldown = 1.25f
        tokenCooldown = 1.15f
        obstacles = emptyList()
        tokens = emptyList()
        playerLift = 0f
        playerVelocity = 0f
        crawlLeft = 0f
        crawlPressed = false
    }

    fun jump() {
        if (status == RunnerStatus.READY || status == RunnerStatus.GAME_OVER) {
            requestStart()
            return
        }
        if (runnerPaused) return
        if (phase == RunnerPhase.TRANSITION) {
            if (!anchorFailing) {
                anchorPressed = true
                transitionTime = max(transitionTime, 2.95f)
            }
            return
        }
        if (phase == RunnerPhase.LAKE) {
            // LAKE action = PADDLE only. No jump in lake mode.
            lakePaddleBoost = (lakePaddleBoost + 46f).coerceAtMost(210f)
            lakePaddleTapCount++
            lakePaddleTimer = 0.32f
            playRunnerTone(ToneGenerator.TONE_PROP_BEEP, 35)
            return
        }
        if (phase == RunnerPhase.EXIT_VERTICAL) return
        if (phase == RunnerPhase.VERTICAL) {
            playerLane = (playerLane - 1).coerceAtLeast(0)
            return
        }
        if (playerLift <= 1.5f) {
            playerVelocity = jumpVelocity
            crawlLeft = 0f
            crawlPressed = false
        }
    }

    fun crawlOrRight() {
        if (status == RunnerStatus.READY || status == RunnerStatus.GAME_OVER) {
            requestStart()
            return
        }
        if (runnerPaused) return
        if (phase == RunnerPhase.TRANSITION) {
            if (!anchorFailing) {
                anchorPressed = true
                        transitionTime = max(transitionTime, 2.95f)
            }
            return
        }
        if (phase == RunnerPhase.EXIT_VERTICAL) return
        if (phase == RunnerPhase.VERTICAL) {
            playerLane = (playerLane + 1).coerceAtMost(2)
            return
        }
        if (playerLift <= 10f) crawlLeft = max(crawlLeft, 0.18f)
    }

    fun gameOver(hitObstacle: RunnerObstacle? = null) {
        playRunnerTone(ToneGenerator.TONE_PROP_NACK, 140)
        runnerPaused = false
        fallingObstacle = hitObstacle
        fallAnimationTime = 0f
        shakeOffset = Offset.Zero
        status = RunnerStatus.GAME_OVER
        val finalScore = totalScore()
        if (finalScore > highScore) {
            highScore = finalScore
            prefs.edit()
                .putInt("high_score_total_m", finalScore)
                .apply()
        }
        if (gearCount > highScoreBats) {
            highScoreBats = gearCount
            prefs.edit()
                .putInt("high_score_bats", gearCount)
                .apply()
        }
        if (playerName.isNotBlank() && finalScore > 0) {
            pendingSubmitScore = finalScore
            pendingSubmitBats = gearCount
        }
    }


    LaunchedEffect(status, fallingObstacle, restartNonce) {
        if (status == RunnerStatus.GAME_OVER && fallingObstacle != null) {
            var lastFrame = 0L
            val px = with(density) { 1.dp.toPx() }
            while (fallAnimationTime < 1.15f) {
                val now = withFrameNanos { it }
                if (lastFrame == 0L) {
                    lastFrame = now
                    continue
                }
                val dt = ((now - lastFrame) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                lastFrame = now
                fallAnimationTime = (fallAnimationTime + dt).coerceAtMost(1.15f)
                shakeOffset = if (fallAnimationTime < 0.45f) {
                    Offset(
                        sin(fallAnimationTime * 80f) * 6f * px,
                        cos(fallAnimationTime * 60f) * 3f * px
                    )
                } else {
                    Offset.Zero
                }
            }
            shakeOffset = Offset.Zero
        } else {
            shakeOffset = Offset.Zero
        }
    }

    LaunchedEffect(status, restartNonce, gameWidth, gameHeight) {
        if (status != RunnerStatus.RUNNING || gameWidth <= 1 || gameHeight <= 1) return@LaunchedEffect
        var lastFrame = 0L
        while (status == RunnerStatus.RUNNING) {
            val now = withFrameNanos { it }
            if (lastFrame == 0L) {
                lastFrame = now
                continue
            }
            val rawDt = ((now - lastFrame) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            _smoothedRunnerDt = _smoothedRunnerDt * 0.85f + rawDt * 0.15f
            val dt = _smoothedRunnerDt.coerceIn(0.008f, 0.028f)
            lastFrame = now

            if (runnerPaused) {
                continue
            }

            phaseTime += dt
            val difficultyLevel = ((walkMeters + depthMeters) / 144f).toInt().coerceAtMost(10)
            val cycleSpeedMultiplier = 1.06f.powInt(horizontalCycles).coerceAtMost(1.68f)
            val horizontalSpeed = (81.6f + difficultyLevel * 4.875f) * cycleSpeedMultiplier
            val verticalSpeed = 1.57f + difficultyLevel * 0.1125f
            slowdownTimer = max(0f, slowdownTimer - dt)
            speedBoostTimer = max(0f, speedBoostTimer - dt)
            batSwarmTimer = max(0f, batSwarmTimer - dt)
            cobwebTimer = max(0f, cobwebTimer - dt)
            icyRopeTimer = max(0f, icyRopeTimer - dt)

            if (phase == RunnerPhase.HORIZONTAL) {
                val width = gameWidth.toFloat()
                val height = gameHeight.toFloat()
                val groundY = height * 0.76f
                val playerX = width * 0.18f
                val horizontalSpeedMultiplier = when {
                    speedBoostTimer > 0f -> 1.35f
                    else -> 1f
                }
                val pxSpeed = with(density) { (horizontalSpeed * horizontalSpeedMultiplier).dp.toPx() }

                walkMeters += dt * 2.0f * horizontalSpeedMultiplier * cycleSpeedMultiplier

                // Cave morphology: keep the floor stable and only let the ceiling breathe subtly.
                caveCeilingChangeTimer -= dt
                if (caveCeilingChangeTimer <= 0f) {
                    caveCeilingTarget = Random.nextDouble((-height * 0.08f).toDouble(), (height * 0.15f).toDouble()).toFloat()
                    caveCeilingChangeTimer = Random.nextDouble(8.0, 18.0).toFloat()
                }
                val ceilingDiff = caveCeilingTarget - caveCeilingOffset
                val ceilingStep = ceilingDiff.coerceIn(-height * 0.006f, height * 0.006f)
                caveCeilingOffset += ceilingStep

                if (phaseTime >= horizontalPhaseDuration) {
                    // Deterministic phase order: HORIZONTAL(0) -> VERTICAL(1) -> LAKE(2), repeat.
                    // The lake phase is entered after the vertical exit when the next sequence slot is 2 mod 3.
                    startTransitionPhase()
                    continue
                }

                playerVelocity -= gravity * dt
                playerLift = max(0f, playerLift + playerVelocity * dt)
                if (playerLift <= 0f && playerVelocity < 0f) playerVelocity = 0f
                if (crawlPressed && playerLift <= 10f) {
                    // True hold-to-crawl: the speleologist stays down while the player keeps CRAWL pressed.
                    crawlLeft = 0.20f
                } else {
                    crawlLeft = max(0f, crawlLeft - dt)
                }

                spawnCooldown -= dt
                tokenCooldown -= dt

                _activeObstaclesBuffer.clear()
                obstacles.forEach { obstacle ->
                    val moved = obstacle.copy(x = obstacle.x - pxSpeed * dt)
                    if (moved.x + moved.width > -40f) _activeObstaclesBuffer.add(moved)
                }
                _activeTokensBuffer.clear()
                tokens.forEach { token ->
                    val moved = token.copy(x = token.x - pxSpeed * dt)
                    if (moved.x > -40f && !moved.collected) _activeTokensBuffer.add(moved)
                }
                val newObstacles = _activeObstaclesBuffer
                val newTokens = _activeTokensBuffer

                if (spawnCooldown <= 0f) {
                    // Big gameplay patch: keep jump obstacles away from crawl passages.
                    // This prevents unfair layouts where pits/stalagmites spawn under or right next to a long crawl ceiling.
                    val crawlSafetyBuffer = with(density) { 235.dp.toPx() }
                    val anyObstacleBuffer = with(density) { 104.dp.toPx() }
                    val lastObstacle = newObstacles.maxByOrNull { it.x + it.width }
                    val lastRight = lastObstacle?.let { it.x + it.width } ?: -9999f
                    val tooCloseToPrevious = lastRight > width - anyObstacleBuffer
                    val recentCrawlNearby = newObstacles.any { it.type == RunnerObstacleType.LOW_CEILING && it.x + it.width > width - crawlSafetyBuffer }
                    val recentJumpNearby = newObstacles.any { (it.type == RunnerObstacleType.PIT || it.type == RunnerObstacleType.BLIND_PIT || it.type == RunnerObstacleType.STALAGMITE) && it.x + it.width > width - crawlSafetyBuffer }

                    if (tooCloseToPrevious) {
                        spawnCooldown = 0.14f
                    } else {
                        val biome = horizontalBackgroundIndex % 5
                        val obstacleRoll = Random.nextInt(100)
                        val candidateType = when (biome) {
                            // Wet cave: more crawl passages, fewer ice patches, no rockfall/drip/water clutter.
                            1 -> when (obstacleRoll) {
                                in 0..24 -> RunnerObstacleType.PIT
                                in 25..44 -> RunnerObstacleType.BLIND_PIT
                                in 45..69 -> RunnerObstacleType.LOW_CEILING
                                in 70..84 -> RunnerObstacleType.STALAGMITE
                                else -> RunnerObstacleType.ICE_PATCH
                            }
                            // Lava cave: pits dominate, ice remains rare as a gameplay power-up.
                            3 -> when (obstacleRoll) {
                                in 0..29 -> RunnerObstacleType.PIT
                                in 30..49 -> RunnerObstacleType.BLIND_PIT
                                in 50..73 -> RunnerObstacleType.LOW_CEILING
                                in 74..90 -> RunnerObstacleType.STALAGMITE
                                else -> RunnerObstacleType.ICE_PATCH
                            }
                            // Ice cave: ice patch is still capped at 20%, with normal cave hazards preserved.
                            4 -> when (obstacleRoll) {
                                in 0..23 -> RunnerObstacleType.PIT
                                in 24..43 -> RunnerObstacleType.BLIND_PIT
                                in 44..67 -> RunnerObstacleType.LOW_CEILING
                                in 68..79 -> RunnerObstacleType.STALAGMITE
                                else -> RunnerObstacleType.ICE_PATCH
                            }
                            else -> when (obstacleRoll) {
                                in 0..27 -> RunnerObstacleType.PIT
                                in 28..47 -> RunnerObstacleType.BLIND_PIT
                                in 48..72 -> RunnerObstacleType.LOW_CEILING
                                in 73..88 -> RunnerObstacleType.STALAGMITE
                                else -> RunnerObstacleType.ICE_PATCH
                            }
                        }
                        val candidateIsJump = when (candidateType) {
                            RunnerObstacleType.PIT, RunnerObstacleType.BLIND_PIT, RunnerObstacleType.STALAGMITE -> true
                            else -> false
                        }
                        val candidateIsCrawl = when (candidateType) {
                            RunnerObstacleType.LOW_CEILING -> true
                            else -> false
                        }
                        val invalidNearCrawl = candidateIsJump && recentCrawlNearby
                        val invalidNearJump = candidateIsCrawl && recentJumpNearby

                        if (invalidNearCrawl || invalidNearJump) {
                            spawnCooldown = Random.nextDouble(0.28, 0.46).toFloat()
                        } else {
                            val obstacleWidth = when (candidateType) {
                                RunnerObstacleType.PIT -> Random.nextDouble((minGap * 1.15f).toDouble(), (maxGap * 1.45f).toDouble()).toFloat()
                                RunnerObstacleType.BLIND_PIT -> Random.nextDouble((minGap * 0.80f).toDouble(), (maxGap * 1.02f).toDouble()).toFloat()
                                RunnerObstacleType.ICE_PATCH -> if (horizontalBackgroundIndex % 5 == 4) {
                                    Random.nextDouble((lowWidth * 4.40f).toDouble(), (lowWidth * 7.20f).toDouble()).toFloat()
                                } else {
                                    Random.nextDouble((lowWidth * 2.85f).toDouble(), (lowWidth * 5.20f).toDouble()).toFloat()
                                }
                                RunnerObstacleType.LOW_CEILING -> Random.nextDouble((lowWidth * 3.80f).toDouble(), (lowWidth * 8.80f).toDouble()).toFloat()
                                RunnerObstacleType.STALAGMITE -> rockWidth
                            }
                            newObstacles.add(RunnerObstacle(nextId++, candidateType, width + 44f, obstacleWidth))
                            val spawnScale = 0.93f.powInt(difficultyLevel).coerceAtLeast(0.60f)
                            spawnCooldown = when (candidateType) {
                                RunnerObstacleType.LOW_CEILING -> (Random.nextDouble(2.15, 3.10) * spawnScale).toFloat()
                                RunnerObstacleType.PIT -> (Random.nextDouble(1.80, 2.65) * spawnScale).toFloat()
                                RunnerObstacleType.BLIND_PIT -> (Random.nextDouble(1.95, 2.80) * spawnScale).toFloat()
                                RunnerObstacleType.ICE_PATCH -> if (horizontalBackgroundIndex % 5 == 4) (Random.nextDouble(1.10, 2.05) * spawnScale).toFloat() else (Random.nextDouble(1.90, 3.10) * spawnScale).toFloat()
                                RunnerObstacleType.STALAGMITE -> (Random.nextDouble(1.40, 2.15) * spawnScale).toFloat()
                            }
                        }
                    }
                }

                if (tokenCooldown <= 0f) {
                    // Collectible bats must spawn inside the actual jump envelope.
                    // yFactor grows downward on screen, so the top reachable bat is groundY - maxJumpLift.
                    val maxJumpLift = (jumpVelocity * jumpVelocity) / (2f * gravity)
                    val reachableTopFactor = ((groundY - maxJumpLift + with(density) { 8.dp.toPx() }) / height)
                        .coerceIn(0.60f, 0.68f)
                    val reachableBottomFactor = ((groundY - standingHeight * 0.35f) / height)
                        .coerceIn(reachableTopFactor + 0.03f, 0.74f)
                    newTokens.add(
                        RunnerToken(
                            id = nextId++,
                            x = width + Random.nextInt(55, 185),
                            yFactor = Random.nextDouble(reachableTopFactor.toDouble(), reachableBottomFactor.toDouble()).toFloat()
                        )
                    )
                    tokenCooldown = Random.nextDouble(0.85, 1.35).toFloat()
                }

                val isCrawling = crawlLeft > 0f
                val playerHeight = if (isCrawling) crawlHeight else standingHeight
                val playerRect = Rect(
                    offset = Offset(playerX - playerWidth / 2f, groundY - playerHeight - playerLift),
                    size = Size(playerWidth, playerHeight)
                )

                var collectedNow = 0
                val tokenIterator = newTokens.iterator()
                while (tokenIterator.hasNext()) {
                    val token = tokenIterator.next()
                    val tokenX = token.x
                    val tokenY = height * token.yFactor
                    val hit = tokenX in (playerRect.left - 16f)..(playerRect.right + 16f) && tokenY in (playerRect.top - 24f)..(playerRect.bottom + 24f)
                    if (hit) {
                        collectedNow += 1
                        collectFlash += RunnerCollectFlash(tokenX, tokenY, phaseTime)
                        tokenIterator.remove()
                    }
                }
                if (collectedNow > 0) {
                    gearCount += collectedNow
                    playRunnerTone(ToneGenerator.TONE_PROP_ACK, 45)
                }
                collectFlash.removeAll { phaseTime - it.timestamp > 0.45f }

                val hitObstacle = newObstacles.firstOrNull { obstacle ->
                    when (obstacle.type) {
                        RunnerObstacleType.PIT, RunnerObstacleType.BLIND_PIT -> {
                            val dangerLeft = obstacle.x + obstacle.width * 0.20f
                            val dangerRight = obstacle.x + obstacle.width * 0.80f
                            val centerInPit = playerX in dangerLeft..dangerRight
                            centerInPit && playerLift < with(density) { 11.dp.toPx() }
                        }
                        RunnerObstacleType.LOW_CEILING -> {
                            val rockRect = Rect(
                                offset = Offset(obstacle.x + obstacle.width * 0.10f, groundY - standingHeight - with(density) { 14.dp.toPx() }),
                                size = Size(obstacle.width * 0.80f, with(density) { 26.dp.toPx() })
                            )
                            playerRect.overlaps(rockRect)
                        }
                        RunnerObstacleType.STALAGMITE -> {
                            // Fairer horizontal stalagmite hitbox: obstacle remains visible, but only the central spike is dangerous.
                            val rockRect = Rect(
                                offset = Offset(obstacle.x + obstacle.width * 0.34f, groundY - with(density) { 27.dp.toPx() }),
                                size = Size(obstacle.width * 0.32f, with(density) { 27.dp.toPx() })
                            )
                            playerRect.overlaps(rockRect)
                        }
                        RunnerObstacleType.ICE_PATCH -> {
                            val iceRect = Rect(
                                offset = Offset(obstacle.x, groundY - with(density) { 13.dp.toPx() }),
                                size = Size(obstacle.width, with(density) { 17.dp.toPx() })
                            )
                            if (playerRect.overlaps(iceRect) && speedBoostTimer <= 0.05f) {
                                speedBoostTimer = 0.85f
                            }
                            false
                        }
                    }
                }

                obstacles = newObstacles.toList()
                tokens = newTokens.toList()
                if (hitObstacle != null) gameOver(hitObstacle)
            } else if (phase == RunnerPhase.LAKE) {
                val width = gameWidth.toFloat()
                val height = gameHeight.toFloat()
                val px = with(density) { 1.dp.toPx() }
                val waterY = height * 0.60f
                val boatX = width * 0.50f
                val baseSpeed = 115f + difficultyLevel * 4f
                val boostSpeed = baseSpeed + lakePaddleBoost
                lakePaddleBoost = (lakePaddleBoost - dt * 95f).coerceAtLeast(0f)
                lakePaddleTimer = max(0f, lakePaddleTimer - dt)

                // Lake mode is shark-only: no boat jump and no rock obstacles.
                lakeBoatLift = 0f
                lakeJumpVelocity = 0f
                lakeRockObstacles = emptyList()
                lakeRockCooldown = 999f

                // Shark chase.
                if (!lakeSharkActive) {
                    lakeSharkTimer -= dt
                    if (lakeSharkTimer <= 0f) {
                        lakeSharkActive = true
                        lakeSharkX = -90f * px
                        lakeSharkEscaped = false
                        lakePaddleTapCount = 0
                    }
                } else {
                    val sharkBaseSpeed = baseSpeed * 0.70f
                    val sharkRamp = (phaseTime / lakeDuration).coerceIn(0f, 1f)
                    val sharkActualSpeed = (sharkBaseSpeed + difficultyLevel * 3.0f + sharkRamp * 46f).coerceAtMost(168f)
                    // Shark accelerates during the lake chase, but regular paddling can always keep the boat safe.
                    lakeSharkX += sharkActualSpeed * dt - lakePaddleBoost * dt
                    val sharkFin = lakeSharkX + 120f * px
                    if (sharkFin >= boatX - 22f * px) {
                        gameOver()
                        continue
                    }
                    val gap = boatX - (lakeSharkX + 120f * px)
                    if (sharkFin > 0f && gap > width * 0.62f) {
                        lakeSharkActive = false
                        lakeSharkEscaped = true
                        lakeSharkTimer = 99f
                        lakePaddleBoost = 0f
                    }
                }

                // Collectible golden shells. Keep them separate from rock/shark threat.
                tokenCooldown -= dt
                val pxSpeed = with(density) { boostSpeed.dp.toPx() }
                _newTokensBuffer.clear()
                tokens.forEach { token ->
                    val moved = token.copy(x = token.x - pxSpeed * dt)
                    if (moved.x > -40f && !moved.collected) _newTokensBuffer.add(moved)
                }
                val newTokens = _newTokensBuffer
                if (tokenCooldown <= 0f && !lakeSharkActive) {
                    newTokens.add(RunnerToken(nextId++, width + Random.nextInt(90, 220), Random.nextDouble(0.47, 0.55).toFloat()))
                    tokenCooldown = Random.nextDouble(1.35, 2.25).toFloat()
                }
                val boatRect = Rect(Offset(boatX - 26f * px, waterY + lakeBoatLift - 36f * px), Size(64f * px, 38f * px))
                var collectedLake = 0
                val lakeTokenIterator = newTokens.iterator()
                while (lakeTokenIterator.hasNext()) {
                    val token = lakeTokenIterator.next()
                    val tokenX = token.x
                    val tokenY = height * token.yFactor
                    if (tokenX in (boatRect.left - 16f)..(boatRect.right + 16f) && tokenY in (boatRect.top - 22f)..(boatRect.bottom + 22f)) {
                        collectedLake += 1
                        collectFlash += RunnerCollectFlash(tokenX, tokenY, phaseTime)
                        lakeTokenIterator.remove()
                    }
                }
                if (collectedLake > 0) {
                    gearCount += collectedLake
                    playRunnerTone(ToneGenerator.TONE_PROP_ACK, 45)
                }
                collectFlash.removeAll { phaseTime - it.timestamp > 0.55f }
                tokens = newTokens.toList()

                // Score — lake counts as forward progress like horizontal.
                walkMeters += baseSpeed * dt / 100f

                if (phaseTime >= lakeDuration) {
                    startHorizontalPhase()
                    continue
                }
            } else if (phase == RunnerPhase.TRANSITION) {
                if (anchorFailing) {
                    anchorFallTime += dt
                    transitionTime += dt * 0.45f
                    if (anchorFallTime >= 1.15f) {
                        gameOver()
                    }
                } else {
                    anchorWindowTime += dt
                    val transitionAdvance = if (anchorPressed) dt * 1.15f else dt * 0.34f
                    transitionTime += transitionAdvance
                    val anchorDeadline = (3.0f - difficultyLevel * 0.12f).coerceAtLeast(1.6f)
                    if (!anchorPressed && anchorWindowTime >= anchorDeadline) {
                        // Missed anchor: show a short fall into the shaft before the Game Over card.
                        anchorFailing = true
                        anchorFallTime = 0f
                    } else if (anchorPressed && transitionTime >= 4.15f) {
                        startVerticalPhase()
                    }
                }
            } else if (phase == RunnerPhase.VERTICAL) {

                val cobwebMultiplier = if (cobwebTimer > 0f && playerLane == cobwebLane) 0.60f else 1f
                val iceRopeMultiplier = if (icyRopeTimer > 0f && playerLane == icyRopeLane) 1.38f else 1f
                val beforeDepth = segmentDepth
                val gravityDepthBoost = (1f + (depthMeters / 180f)).coerceAtMost(2.25f)
                val addDepth = dt * 2.0f * cobwebMultiplier * iceRopeMultiplier * gravityDepthBoost
                segmentDepth += addDepth
                depthMeters += addDepth

                if (beforeDepth < nextVerticalBatDepth && segmentDepth >= nextVerticalBatDepth) {
                    if (playerLane == batLane) gearCount += 1
                    nextVerticalBatDepth += Random.nextDouble(3.0, 4.6).toFloat()
                    batLane = Random.nextInt(0, 3)
                }

                if (beforeDepth < nextHazardDepth && segmentDepth >= nextHazardDepth) {
                    if (playerLane == hazardLane) {
                        gameOver()
                        continue
                    } else {
                        nextHazardDepth += (Random.nextDouble(4.3, 6.2) * 0.93f.powInt(difficultyLevel).coerceAtLeast(0.65f)).toFloat()
                        hazardLane = Random.nextInt(0, 3)
                    }
                }

                if (beforeDepth < nextFallingRockDepth && segmentDepth >= nextFallingRockDepth) {
                    if (playerLane == fallingRockLane) {
                        gameOver()
                        continue
                    }
                    val minGapMeters = (8.2f - difficultyLevel * 0.38f).coerceAtLeast(4.2f)
                    val maxGapMeters = (11.8f - difficultyLevel * 0.52f).coerceAtLeast(5.6f)
                    nextFallingRockDepth += Random.nextDouble(minGapMeters.toDouble(), maxGapMeters.toDouble()).toFloat()
                    fallingRockLane = Random.nextInt(0, 3)
                }

                if (beforeDepth < nextBatSwarmDepth && segmentDepth >= nextBatSwarmDepth) {
                    batSwarmLane = Random.nextInt(0, 3)
                    batSwarmDirection = if (Random.nextBoolean()) 1 else -1
                    batSwarmTimer = 0.90f
                    if (playerLane == batSwarmLane) gearCount += 2
                    nextBatSwarmDepth += (Random.nextDouble(6.0, 9.0) * 0.93f.powInt(difficultyLevel).coerceAtLeast(0.70f)).toFloat()
                }

                if (cobwebTimer <= 0f && beforeDepth < nextCobwebDepth && segmentDepth >= nextCobwebDepth) {
                    cobwebLane = Random.nextInt(0, 3)
                    cobwebTimer = 3.5f
                    nextCobwebDepth += Random.nextDouble(8.0, 13.0).toFloat()
                }

                if (icyRopeTimer <= 0f && beforeDepth < nextIcyRopeDepth && segmentDepth >= nextIcyRopeDepth) {
                    icyRopeLane = Random.nextInt(0, 3)
                    icyRopeTimer = Random.nextDouble(5.0, 7.0).toFloat()
                    nextIcyRopeDepth += Random.nextDouble(7.0, 12.0).toFloat()
                }

                if (status == RunnerStatus.RUNNING && phaseTime >= 30f) {
                    phase = RunnerPhase.EXIT_VERTICAL
                    phaseTime = 0f
                    verticalExitTime = 0f
                }
            } else if (phase == RunnerPhase.EXIT_VERTICAL) {
                verticalExitTime += dt
                if (verticalExitTime >= 2.05f) {
                    val nextPhaseLevel = phaseSequenceLevel + 1
                    if (nextPhaseLevel % 3 == 2) {
                        startLakePhase()
                    } else {
                        startHorizontalPhase()
                    }
                }
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF101827), RoundedCornerShape(28.dp))
                    .graphicsLayer {
                        translationX = shakeOffset.x
                        translationY = shakeOffset.y
                    }
                    .onSizeChanged {
                        gameWidth = it.width.coerceAtLeast(1)
                        gameHeight = it.height.coerceAtLeast(1)
                    }
                    .pointerInput(status, restartNonce, phase) {
                        detectTapGestures(onTap = { offset ->
                            if (phase == RunnerPhase.VERTICAL && status == RunnerStatus.RUNNING) {
                                if (offset.x < gameWidth / 2f) {
                                    playerLane = (playerLane - 1).coerceAtLeast(0)
                                } else {
                                    playerLane = (playerLane + 1).coerceAtMost(2)
                                }
                            } else {
                                jump()
                            }
                        })
                    }
            ) {
                RunnerCanvas(
                    modifier = Modifier.fillMaxSize(),
                    phase = phase,
                    obstacles = obstacles,
                    tokens = tokens,
                    walkMeters = walkMeters,
                    depthMeters = depthMeters,
                    totalMeters = totalMeters,
                    phaseTime = phaseTime,
                    transitionTime = transitionTime,
                    segmentDepth = segmentDepth,
                    playerLift = playerLift,
                    isCrawling = crawlLeft > 0f,
                    status = status,
                    gearCount = gearCount,
                    playerLane = playerLane,
                    hazardLane = hazardLane,
                    batLane = batLane,
                    nextHazardDepth = nextHazardDepth,
                    nextVerticalBatDepth = nextVerticalBatDepth,
                    nextFallingRockDepth = nextFallingRockDepth,
                    fallingRockLane = fallingRockLane,
                    nextBatSwarmDepth = nextBatSwarmDepth,
                    batSwarmLane = batSwarmLane,
                    batSwarmTimer = batSwarmTimer,
                    batSwarmDirection = batSwarmDirection,
                    cobwebLane = cobwebLane,
                    cobwebTimer = cobwebTimer,
                    nextCobwebDepth = nextCobwebDepth,
                    icyRopeLane = icyRopeLane,
                    icyRopeTimer = icyRopeTimer,
                    nextIcyRopeDepth = nextIcyRopeDepth,
                    slowdownTimer = slowdownTimer,
                    speedBoostTimer = speedBoostTimer,
                    verticalBackgroundIndex = verticalBackgroundIndex,
                    horizontalBackgroundIndex = horizontalBackgroundIndex,
                    fallingObstacle = fallingObstacle,
                    fallAnimationTime = fallAnimationTime,
                    verticalExitTime = verticalExitTime,
                    anchorWindowTime = anchorWindowTime,
                    anchorPressed = anchorPressed,
                    anchorFallTime = anchorFallTime,
                    anchorFailing = anchorFailing,
                    caveFloorOffset = 0f,
                    caveCeilingOffset = caveCeilingOffset,
                    horizontalPhaseDuration = horizontalPhaseDuration,
                    lakeDuration = lakeDuration,
                    lakeBoatLift = lakeBoatLift,
                    lakePaddleTimer = lakePaddleTimer,
                    lakeRockObstacles = lakeRockObstacles,
                    lakeSharkActive = lakeSharkActive,
                    lakeSharkX = lakeSharkX,
                    lakePaddleBoost = lakePaddleBoost,
                    lakePaddleTapCount = lakePaddleTapCount,
                    lakeJumpVelocity = lakeJumpVelocity,
                    jumpVelocity = jumpVelocity,
                    gravity = gravity,
                    collectFlashes = collectFlash.toList(),
                    currentBats = gearCount,
                    highScore = highScore,
                    highScoreBats = highScoreBats,
                    playerName = playerName,
                    leaderboard = leaderboard
                )

                SpeleoRunnerHudBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    scoreMeters = hudScoreMeters,
                    walkMeters = hudWalkMeters,
                    depthMeters = hudDepthMeters,
                    bats = gearCount,
                    level = hudLevel,
                    phase = phase,
                    phaseSecondsLeft = max(0, ((if (phase == RunnerPhase.HORIZONTAL) horizontalPhaseDuration else if (phase == RunnerPhase.LAKE) lakeDuration else 30f) - phaseTime).toInt()),
                    horizontalPhaseDuration = horizontalPhaseDuration,
                    phaseProgress = when (phase) {
                        RunnerPhase.TRANSITION -> if (!anchorPressed) (anchorWindowTime / 3.0f).coerceIn(0f, 1f) else (transitionTime / 4.15f).coerceIn(0f, 1f)
                        RunnerPhase.EXIT_VERTICAL -> (verticalExitTime / 2.05f).coerceIn(0f, 1f)
                        RunnerPhase.HORIZONTAL -> (phaseTime / horizontalPhaseDuration).coerceIn(0f, 1f)
                        RunnerPhase.LAKE -> (phaseTime / lakeDuration).coerceIn(0f, 1f)
                        else -> (phaseTime / 30f).coerceIn(0f, 1f)
                    }
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 58.dp, end = 12.dp)
                        .size(38.dp)
                        .pointerInput(soundMuted) {
                            detectTapGestures(onTap = {
                                soundMuted = !soundMuted
                                prefs.edit().putBoolean("runner_sound_muted", soundMuted).apply()
                                if (!soundMuted) playRunnerTone(ToneGenerator.TONE_PROP_ACK, 50)
                            })
                        },
                    color = Color(0xAA07111F),
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (soundMuted) "🔇" else "🔊",
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (status == RunnerStatus.READY) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 126.dp)
                            .size(width = 230.dp, height = 64.dp)
                            .pointerInput(showLeaderboard) {
                                detectTapGestures(onTap = { showLeaderboard = true })
                            },
                        color = Color(0xE61B1208),
                        shape = RoundedCornerShape(22.dp),
                        border = androidx.compose.foundation.BorderStroke(1.4.dp, Color(0xFFFFB74D).copy(alpha = 0.78f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🏆", fontSize = 18.sp, color = Color(0xFFFFD54F))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Rezultati",
                                    color = Color(0xFFFFE0A0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Duljina: ${highScore} m  •  Šišmiši: ${highScoreBats}",
                                color = Color(0xFFFFF3D0),
                                fontSize = 11.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (false && status != RunnerStatus.RUNNING) {
                    Card(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                if (status == RunnerStatus.GAME_OVER) "Game over" else "Spreman za špilju i vertikalu?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                if (status == RunnerStatus.GAME_OVER) "Score ${hudScoreMeters} m  •  horizontalno ${hudWalkMeters} m  •  vertikala ${hudDepthMeters} m  •  šišmiši ${gearCount}  •  rekord: ${highScore} m" else "Start",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { requestStart() }) {
                                Text(if (status == RunnerStatus.GAME_OVER) "Igraj opet" else "Start")
                            }
                            TextButton(onClick = { showLeaderboard = true }) {
                                Text("High scoreovi")
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF4A3020), Color(0xFF2A1A0E))),
                            RoundedCornerShape(999.dp)
                        )
                        .border(1.5.dp, Color(0xFFAA8855).copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                        .pointerInput(status, phase, restartNonce) {
                            detectTapGestures(onTap = { jump() })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (phase == RunnerPhase.VERTICAL) "LEFT" else if (phase == RunnerPhase.LAKE) "PADDLE" else if (phase == RunnerPhase.TRANSITION) "⚓" else if (phase == RunnerPhase.EXIT_VERTICAL) "WAIT" else "JUMP",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE0A0),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF3A2518), Color(0xFF1E130A))),
                            RoundedCornerShape(999.dp)
                        )
                        .border(1.5.dp, Color(0xFFAA8855).copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                        .pointerInput(status, phase, restartNonce) {
                            detectTapGestures(
                                onPress = {
                                    when {
                                        status == RunnerStatus.READY || status == RunnerStatus.GAME_OVER -> requestStart()
                                        runnerPaused -> Unit
                                        phase == RunnerPhase.TRANSITION -> {
                                            if (!anchorFailing) {
                                                anchorPressed = true
                                                                                        transitionTime = max(transitionTime, 2.95f)
                                            }
                                        }
                                        phase == RunnerPhase.VERTICAL -> playerLane = (playerLane + 1).coerceAtMost(2)
                                        phase == RunnerPhase.LAKE -> {
                                            lakePaddleBoost = (lakePaddleBoost + 46f).coerceAtMost(210f)
                                            lakePaddleTapCount++
                                            lakePaddleTimer = 0.32f
                                            playRunnerTone(ToneGenerator.TONE_PROP_BEEP, 35)
                                        }
                                        phase == RunnerPhase.HORIZONTAL && playerLift <= 10f -> {
                                            crawlPressed = true
                                            crawlLeft = 0.20f
                                            try {
                                                awaitRelease()
                                            } finally {
                                                crawlPressed = false
                                            }
                                        }
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (phase == RunnerPhase.VERTICAL) "RIGHT" else if (phase == RunnerPhase.LAKE) "PADDLE" else if (phase == RunnerPhase.TRANSITION) { if (anchorPressed) "CLIPPED" else "ANCHOR" } else if (phase == RunnerPhase.EXIT_VERTICAL) "ANCHOR" else "HOLD CRAWL",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE0A0),
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (status == RunnerStatus.RUNNING) {
                            runnerPaused = !runnerPaused
                            crawlPressed = false
                            crawlLeft = 0f
                        } else {
                            requestStart()
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) { Text(if (status == RunnerStatus.RUNNING && runnerPaused) "Resume" else "Pause") }
            }
        }
    }


    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Ime igrača") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Upiši ime za Speleo Runner leaderboard.")
                    TextField(
                        value = playerNameDraft,
                        onValueChange = { playerNameDraft = it.take(32) },
                        singleLine = true,
                        label = { Text("Ime") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleanName = playerNameDraft.trim().ifBlank { "Anon" }
                    playerName = cleanName
                    prefs.edit().putString("player_name", cleanName).apply()
                    showNameDialog = false
                    resetGame(start = true)
                }) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Odustani") }
            }
        )
    }

    if (showLeaderboard) {
        val topMeters = leaderboard.sortedWith(
            compareByDescending<SpeleoRunnerLeaderboardEntry> { it.score }.thenByDescending { it.bats }
        ).take(5)
        val topBats = leaderboard.sortedWith(
            compareByDescending<SpeleoRunnerLeaderboardEntry> { it.bats }.thenByDescending { it.score }
        ).take(5)

        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.96f),
            onDismissRequest = { showLeaderboard = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🏆 Speleo Runner rezultati", fontWeight = FontWeight.Bold)
                    Text(
                        "Najbolji metri i najviše skupljenih šišmiša",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFB74D).copy(alpha = 0.10f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.28f))
                    ) {
                        Text(
                            leaderboardStatus,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (leaderboard.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "Još nema rezultata za prikaz.",
                                modifier = Modifier.padding(18.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { leaderboardTab = 0 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (leaderboardTab == 0) Color(0xFFFFB74D) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (leaderboardTab == 0) Color(0xFF241406) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text("Top metri") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { leaderboardTab = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (leaderboardTab == 1) Color(0xFFFFD54F) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (leaderboardTab == 1) Color(0xFF201800) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text("Top šišmiši") }
                        }

                        val rows = if (leaderboardTab == 0) topMeters else topBats
                        val title = if (leaderboardTab == 0) "Top 5 — metri" else "Top 5 — šišmiši"
                        val accent = if (leaderboardTab == 0) Color(0xFFFFB74D) else Color(0xFFFFD54F)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (leaderboardTab == 0) Color(0xFF23170B).copy(alpha = 0.92f) else Color(0xFF1D1A09).copy(alpha = 0.92f)
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text(title, fontWeight = FontWeight.Bold, color = accent, style = MaterialTheme.typography.titleMedium)
                                rows.forEachIndexed { index, entry ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (index == 0) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${index + 1}. ${entry.name}",
                                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.SemiBold,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Column(horizontalAlignment = Alignment.End) {
                                                if (leaderboardTab == 0) {
                                                    Text("${entry.score} m", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
                                                    Text("${entry.bats} šiš.", color = Color(0xFFFFD54F), style = MaterialTheme.typography.labelSmall)
                                                } else {
                                                    Text("${entry.bats} šiš.", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                                                    Text("${entry.score} m", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLeaderboard = false }) { Text("OK") }
            }
        )
    }

}


@Composable
private fun SpeleoRunnerHudBar(
    modifier: Modifier = Modifier,
    scoreMeters: Int,
    walkMeters: Int,
    depthMeters: Int,
    bats: Int,
    level: Int,
    phase: RunnerPhase,
    phaseSecondsLeft: Int,
    horizontalPhaseDuration: Float = 30f,
    phaseProgress: Float = 0f
) {
    val phaseText = when (phase) {
        RunnerPhase.VERTICAL -> "DESCENT"
        RunnerPhase.TRANSITION -> "ANCHOR"
        RunnerPhase.EXIT_VERTICAL -> "EXIT"
        RunnerPhase.LAKE -> "🚣 JEZERO  ${phaseSecondsLeft}s"
        RunnerPhase.HORIZONTAL -> "${phaseSecondsLeft}s / ${horizontalPhaseDuration.toInt()}s"
    }

    val animatedScoreMeters by animateIntAsState(
        targetValue = scoreMeters,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "runner_score_anim"
    )
    val animatedWalkMeters by animateIntAsState(
        targetValue = walkMeters,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "runner_walk_anim"
    )
    val animatedDepthMeters by animateIntAsState(
        targetValue = depthMeters,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "runner_depth_anim"
    )
    val animatedBats by animateIntAsState(
        targetValue = bats,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "runner_bats_anim"
    )
    val scorePulse by animateFloatAsState(
        targetValue = if (scoreMeters > 0 && scoreMeters % 5 == 0) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "runner_score_pulse"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(94.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xFF1A0F07), RoundedCornerShape(26.dp))
                .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(26.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RunnerHudMetric(
                        icon = "◆",
                        label = "SCORE",
                        value = "$animatedScoreMeters m",
                        accent = Color(0xFFFFE082),
                        valueFontSize = 22.sp,
                        modifier = Modifier
                            .weight(1.22f)
                            .background(Color(0xFFFFE082).copy(alpha = 0.13f + 0.08f * scorePulse), RoundedCornerShape(14.dp))
                            .border(
                                width = if (scoreMeters % 5 == 0 && scoreMeters > 0) 2.dp else 1.dp,
                                color = Color(0xFFFFE082).copy(alpha = if (scoreMeters % 5 == 0 && scoreMeters > 0) 1f else 0.30f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                    RunnerHudDivider()
                    RunnerHudMetric(
                        icon = "→",
                        label = "WALK",
                        value = "${animatedWalkMeters}m",
                        accent = Color(0xFF80DEEA),
                        valueFontSize = 13.sp,
                        modifier = Modifier.weight(0.72f)
                    )
                    RunnerHudDivider()
                    RunnerHudMetric(
                        icon = "↓",
                        label = "DEPTH",
                        value = "${animatedDepthMeters}m",
                        accent = Color(0xFF4FC3F7),
                        valueFontSize = 13.sp,
                        modifier = Modifier.weight(0.72f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFD54F).copy(alpha = 0.20f), RoundedCornerShape(999.dp))
                            .border(1.4.dp, Color(0xFFFFD54F).copy(alpha = 0.70f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🦇 BATS $animatedBats",
                            color = Color(0xFFFFF3C4),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                    Text(
                        "Metri: $animatedScoreMeters m",
                        color = Color(0xFFFFE082),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFD54F).copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                            .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.38f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "LVL $level • $phaseText",
                            color = Color(0xFFFFE0A0).copy(alpha = 0.96f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(phaseProgress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(Color(0xFFFFD54F).copy(alpha = 0.92f), RoundedCornerShape(999.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun RunnerHudMetric(
    icon: String,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    valueFontSize: TextUnit = 12.sp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(icon, fontSize = 16.sp, maxLines = 1)
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                label,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                value,
                color = Color.White,
                fontSize = valueFontSize,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RunnerHudDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
    )
}


@Composable
private fun RunnerCanvas(
    modifier: Modifier,
    phase: RunnerPhase,
    obstacles: List<RunnerObstacle>,
    tokens: List<RunnerToken>,
    walkMeters: Float,
    depthMeters: Float,
    totalMeters: Int,
    phaseTime: Float,
    transitionTime: Float,
    segmentDepth: Float,
    playerLift: Float,
    isCrawling: Boolean,
    status: RunnerStatus,
    gearCount: Int,
    playerLane: Int,
    hazardLane: Int,
    batLane: Int,
    nextHazardDepth: Float,
    nextVerticalBatDepth: Float,
    nextFallingRockDepth: Float,
    fallingRockLane: Int,
    nextBatSwarmDepth: Float,
    batSwarmLane: Int,
    batSwarmTimer: Float,
    batSwarmDirection: Int,
    cobwebLane: Int,
    cobwebTimer: Float,
    nextCobwebDepth: Float,
    icyRopeLane: Int,
    icyRopeTimer: Float,
    nextIcyRopeDepth: Float,
    slowdownTimer: Float,
    speedBoostTimer: Float,
    verticalBackgroundIndex: Int,
    horizontalBackgroundIndex: Int,
    fallingObstacle: RunnerObstacle?,
    fallAnimationTime: Float,
    verticalExitTime: Float = 0f,
    anchorWindowTime: Float = 0f,
    anchorPressed: Boolean = false,
    anchorFallTime: Float = 0f,
    anchorFailing: Boolean = false,
    caveFloorOffset: Float = 0f,
    caveCeilingOffset: Float = 0f,
    horizontalPhaseDuration: Float = 30f,
    lakeDuration: Float = 10f,
    lakeBoatLift: Float = 0f,
    lakePaddleTimer: Float = 0f,
    lakeRockObstacles: List<RunnerObstacle> = emptyList(),
    lakeSharkActive: Boolean = false,
    lakeSharkX: Float = -200f,
    lakePaddleBoost: Float = 0f,
    lakePaddleTapCount: Int = 0,
    lakeJumpVelocity: Float = 0f,
    jumpVelocity: Float = 0f,
    gravity: Float = 1f,
    collectFlashes: List<RunnerCollectFlash>,
    currentBats: Int,
    highScore: Int,
    highScoreBats: Int,
    playerName: String,
    leaderboard: List<SpeleoRunnerLeaderboardEntry>
) {
    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w < 4f || h < 4f) return@Canvas
        val groundY = h * 0.76f
        val drift = (totalMeters % 40) / 40f
        val px = with(density) { 1.dp.toPx() }
        val difficultyLevel = ((walkMeters + depthMeters) / 144f).toInt().coerceAtMost(10)

        when (phase) {
            RunnerPhase.VERTICAL -> {
                drawVerticalShaft(
                    w = w,
                    h = h,
                    px = px,
                    segmentDepth = segmentDepth,
                    playerLane = playerLane,
                    hazardLane = hazardLane,
                    batLane = batLane,
                    nextHazardDepth = nextHazardDepth,
                    nextBatDepth = nextVerticalBatDepth,
                    nextFallingRockDepth = nextFallingRockDepth,
                    fallingRockLane = fallingRockLane,
                    nextBatSwarmDepth = nextBatSwarmDepth,
                    batSwarmLane = batSwarmLane,
                    batSwarmTimer = batSwarmTimer,
                    batSwarmDirection = batSwarmDirection,
                    cobwebLane = cobwebLane,
                    cobwebTimer = cobwebTimer,
                    nextCobwebDepth = nextCobwebDepth,
                    icyRopeLane = icyRopeLane,
                    icyRopeTimer = icyRopeTimer,
                    nextIcyRopeDepth = nextIcyRopeDepth,
                    depthMeters = depthMeters,
                    phaseTime = phaseTime,
                    status = status,
                    biomeIndex = verticalBackgroundIndex,
                    difficultyLevel = difficultyLevel
                )
                drawRunnerBiomeCard(w, h, px, phaseTime, status, verticalBackgroundIndex, (totalMeters / 60) + 1)
            }
            RunnerPhase.EXIT_VERTICAL -> {
                drawVerticalShaft(
                    w = w, h = h, px = px, segmentDepth = segmentDepth,
                    playerLane = playerLane, hazardLane = hazardLane, batLane = batLane,
                    nextHazardDepth = nextHazardDepth, nextBatDepth = nextVerticalBatDepth,
                    nextFallingRockDepth = nextFallingRockDepth, fallingRockLane = fallingRockLane,
                    nextBatSwarmDepth = nextBatSwarmDepth, batSwarmLane = batSwarmLane,
                    batSwarmTimer = batSwarmTimer, batSwarmDirection = batSwarmDirection,
                    cobwebLane = cobwebLane, cobwebTimer = cobwebTimer, nextCobwebDepth = nextCobwebDepth,
                    icyRopeLane = icyRopeLane, icyRopeTimer = icyRopeTimer,
                    nextIcyRopeDepth = nextIcyRopeDepth, depthMeters = depthMeters,
                    phaseTime = phaseTime, status = status, biomeIndex = verticalBackgroundIndex,
                    difficultyLevel = difficultyLevel,
                    drawPlayer = verticalExitTime < 0.55f
                )
                drawVerticalExitToHorizontal(w, h, px, verticalExitTime, verticalBackgroundIndex, status)
            }
            RunnerPhase.TRANSITION -> {
                drawTransitionToVertical(
                    w = w,
                    h = h,
                    groundY = groundY,
                    px = px,
                    transitionTime = transitionTime,
                    status = status,
                    biomeIndex = horizontalBackgroundIndex,
                    anchorWindowTime = anchorWindowTime,
                    anchorPressed = anchorPressed,
                    anchorFallTime = anchorFallTime,
                    anchorFailing = anchorFailing
                )
            }
            RunnerPhase.HORIZONTAL -> {
                drawHorizontalCave(
                    w = w,
                    h = h,
                    groundY = groundY,
                    drift = drift,
                    px = px,
                    obstacles = obstacles,
                    tokens = tokens,
                    score = totalMeters,
                    depthMeters = depthMeters,
                    difficultyLevel = difficultyLevel,
                    playerLift = playerLift,
                    isCrawling = isCrawling,
                    status = status,
                    phaseTime = phaseTime,
                    slowdownTimer = slowdownTimer,
                    speedBoostTimer = speedBoostTimer,
                    fallingObstacle = fallingObstacle,
                    fallAnimationTime = fallAnimationTime,
                    collectFlashes = collectFlashes,
                    biomeIndex = horizontalBackgroundIndex,
                    caveFloorOffset = 0f,
                    caveCeilingOffset = caveCeilingOffset,
                    horizontalPhaseDuration = horizontalPhaseDuration,
                    jumpVelocity = jumpVelocity,
                    gravity = gravity,
                    phase = phase
                )
                drawRunnerBiomeCard(w, h, px, phaseTime, status, horizontalBackgroundIndex, (totalMeters / 60) + 1)
            }
            RunnerPhase.LAKE -> {
                drawLakeCave(
                    w = w,
                    h = h,
                    px = px,
                    phaseTime = phaseTime,
                    boatLift = lakeBoatLift,
                    rockObstacles = lakeRockObstacles,
                    sharkActive = lakeSharkActive,
                    sharkX = lakeSharkX,
                    paddleBoost = lakePaddleBoost,
                    paddleTapCount = lakePaddleTapCount,
                    jumpVelocity = lakeJumpVelocity,
                    biomeIndex = horizontalBackgroundIndex,
                    difficultyLevel = difficultyLevel,
                    status = status,
                    lakeDuration = lakeDuration,
                    collectFlashes = collectFlashes,
                    tokens = tokens
                )
                drawRunnerBiomeCard(w, h, px, phaseTime, status, 1, (totalMeters / 60) + 1)
            }
        }
        drawDepthVignette(w, h, px, depthMeters)
        drawPaperGrain(w, h, px, phaseTime)

        when (status) {
            RunnerStatus.READY -> drawRunnerReadyScreen(
                w = w,
                h = h,
                px = px,
                phaseTime = phaseTime,
                highScore = highScore,
                highScoreBats = highScoreBats,
                playerName = playerName
            )
            RunnerStatus.GAME_OVER -> drawRunnerGameOverScreen(
                w = w,
                h = h,
                px = px,
                phaseTime = phaseTime,
                scoreMeters = totalMeters,
                highScore = highScore,
                highScoreBats = highScoreBats,
                currentBats = currentBats,
                leaderboard = leaderboard
            )
            else -> Unit
        }
    }
}




private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRunnerBottomCaveSilhouette(
    w: Float,
    h: Float,
    px: Float,
    fill: Color
) {
    val silhouette = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h - h * 0.13f)
        lineTo(w * 0.08f, h - h * 0.21f)
        lineTo(w * 0.16f, h - h * 0.10f)
        lineTo(w * 0.26f, h - h * 0.18f)
        lineTo(w * 0.36f, h - h * 0.12f)
        lineTo(w * 0.48f, h - h * 0.22f)
        lineTo(w * 0.58f, h - h * 0.11f)
        lineTo(w * 0.70f, h - h * 0.19f)
        lineTo(w * 0.81f, h - h * 0.13f)
        lineTo(w * 0.91f, h - h * 0.20f)
        lineTo(w, h - h * 0.12f)
        lineTo(w, h)
        close()
    }
    drawPath(silhouette, fill)
    drawPath(silhouette, Color.Black, style = Stroke(width = 2f * px))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRunnerReadyScreen(
    w: Float,
    h: Float,
    px: Float,
    phaseTime: Float,
    highScore: Int,
    highScoreBats: Int,
    playerName: String
) {
    drawRect(Color(0xFF0D0D12), size = Size(w, h))
    repeat(12) { i ->
        val x = ((i * 67f + 23f) % 100f) / 100f * w
        val y = h * (0.08f + ((i * 29f) % 42f) / 100f)
        drawCircle(Color.White.copy(alpha = 0.34f), radius = (0.9f + (i % 2) * 0.45f) * px, center = Offset(x, y))
    }
    drawRunnerBottomCaveSilhouette(w, h, px, Color(0xFF1A0D08))

    drawIndieRunnerText(
        text = "SPELEO RUNNER",
        x = w * 0.5f,
        y = h * 0.28f,
        textSize = 22f * px,
        textColor = android.graphics.Color.rgb(255, 213, 79)
    )
    drawRect(
        color = Color(0xFF1A1A2E),
        topLeft = Offset(w * 0.14f, h * 0.315f),
        size = Size(w * 0.72f, 2f * px)
    )
    drawIndieRunnerText(
        text = "REKORD DULJINA: ${highScore} m",
        x = w * 0.5f,
        y = h * 0.365f,
        textSize = 11.5f * px,
        textColor = android.graphics.Color.rgb(176, 190, 197)
    )
    drawIndieRunnerText(
        text = "REKORD ŠIŠMIŠI: ${highScoreBats}",
        x = w * 0.5f,
        y = h * 0.405f,
        textSize = 11.5f * px,
        textColor = android.graphics.Color.rgb(255, 213, 79)
    )

    val blinkVisible = ((phaseTime / 1.2f).toInt() % 2) == 0
    val btnW = w * 0.55f
    val btnH = 44f * px
    val btnLeft = w * 0.5f - btnW / 2f
    val btnTop = h * 0.48f
    drawRect(Color.Black, topLeft = Offset(btnLeft - 3f * px, btnTop - 3f * px), size = Size(btnW + 6f * px, btnH + 6f * px))
    drawRect(if (blinkVisible) Color(0xFF1B5E20) else Color(0xFF103512), topLeft = Offset(btnLeft, btnTop), size = Size(btnW, btnH))
    drawLine(Color(0xFF4CAF50), Offset(btnLeft, btnTop + 2f * px), Offset(btnLeft + btnW, btnTop + 2f * px), strokeWidth = 2f * px)
    drawIndieRunnerText(
        text = "▶ START",
        x = w * 0.5f,
        y = btnTop + 29f * px,
        textSize = 15f * px,
        textColor = android.graphics.Color.WHITE
    )
    drawIndieRunnerText(
        text = "🏆 Rezultati su odmah ispod START",
        x = w * 0.5f,
        y = btnTop + btnH + 28f * px,
        textSize = 9.5f * px,
        textColor = android.graphics.Color.rgb(255, 183, 77)
    )
    if (playerName.isNotBlank()) {
        drawIndieRunnerText(
            text = "Speleolog: $playerName",
            x = w * 0.5f,
            y = btnTop + btnH + 44f * px,
            textSize = 10f * px,
            textColor = android.graphics.Color.rgb(120, 144, 156)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRunnerGameOverScreen(
    w: Float,
    h: Float,
    px: Float,
    phaseTime: Float,
    scoreMeters: Int,
    highScore: Int,
    highScoreBats: Int,
    currentBats: Int,
    leaderboard: List<SpeleoRunnerLeaderboardEntry>
) {
    drawRect(Color(0xFF0D0000), size = Size(w, h))
    repeat(10) { i ->
        val x = ((i * 73f + 11f) % 100f) / 100f * w
        val y = h * (0.09f + ((i * 31f) % 50f) / 100f)
        drawCircle(Color(0xFFFFCDD2).copy(alpha = 0.18f), radius = (0.9f + (i % 2) * 0.55f) * px, center = Offset(x, y))
    }
    drawRunnerBottomCaveSilhouette(w, h, px, Color(0xFF2D0000))

    drawIndieRunnerText(
        text = "GAME OVER",
        x = w * 0.5f,
        y = h * 0.20f,
        textSize = 20f * px,
        textColor = android.graphics.Color.rgb(255, 23, 68)
    )

    val isRecord = scoreMeters > 0 && scoreMeters >= highScore
    if (isRecord && ((phaseTime * 2.0f).toInt() % 2 == 0)) {
        drawIndieRunnerText(
            text = "★ NOVI REKORD",
            x = w * 0.5f,
            y = h * 0.255f,
            textSize = 11f * px,
            textColor = android.graphics.Color.rgb(255, 213, 79)
        )
    }
    drawIndieRunnerText(
        text = "${scoreMeters}m",
        x = w * 0.5f,
        y = h * 0.315f,
        textSize = 28f * px,
        textColor = android.graphics.Color.rgb(255, 213, 79)
    )
    drawIndieRunnerText(
        text = "BEST: ${highScore}m  •  ${highScoreBats} šiš.  |  sada: ${currentBats} šiš.",
        x = w * 0.5f,
        y = h * 0.355f,
        textSize = 10f * px,
        textColor = android.graphics.Color.rgb(176, 190, 197)
    )

    val topMetersRows = leaderboard.sortedWith(
        compareByDescending<SpeleoRunnerLeaderboardEntry> { it.score }.thenByDescending { it.bats }
    ).take(3)
    val topBatsRows = leaderboard.sortedWith(
        compareByDescending<SpeleoRunnerLeaderboardEntry> { it.bats }.thenByDescending { it.score }
    ).take(3)
    val listTop = h * 0.405f
    if (topMetersRows.isNotEmpty()) {
        drawIndieRunnerText(
            text = "TOP METRI",
            x = w * 0.5f,
            y = listTop - 8f * px,
            textSize = 9f * px,
            textColor = android.graphics.Color.rgb(255, 183, 77)
        )
        topMetersRows.forEachIndexed { index, entry ->
            val rowW = w * 0.78f
            val rowH = 18f * px
            val rowLeft = w * 0.5f - rowW / 2f
            val rowTop = listTop + index * 21f * px
            drawRect(Color(0xFF1A0808), topLeft = Offset(rowLeft, rowTop), size = Size(rowW, rowH))
            drawRect(Color(0xFF3D0000), topLeft = Offset(rowLeft, rowTop), size = Size(rowW, rowH), style = Stroke(width = 1.1f * px))
            val safeName = entry.name.take(10)
            drawIndieRunnerText(
                text = "#${index + 1} $safeName — ${entry.score}m / ${entry.bats} šiš.",
                x = w * 0.5f,
                y = rowTop + 13f * px,
                textSize = 8.6f * px,
                textColor = android.graphics.Color.rgb(236, 239, 241)
            )
        }
    }
    val batsTop = listTop + 76f * px
    if (topBatsRows.isNotEmpty()) {
        drawIndieRunnerText(
            text = "TOP ŠIŠMIŠI",
            x = w * 0.5f,
            y = batsTop - 8f * px,
            textSize = 9f * px,
            textColor = android.graphics.Color.rgb(255, 213, 79)
        )
        topBatsRows.forEachIndexed { index, entry ->
            val rowW = w * 0.78f
            val rowH = 18f * px
            val rowLeft = w * 0.5f - rowW / 2f
            val rowTop = batsTop + index * 21f * px
            drawRect(Color(0xFF1A0808), topLeft = Offset(rowLeft, rowTop), size = Size(rowW, rowH))
            drawRect(Color(0xFF3D0000), topLeft = Offset(rowLeft, rowTop), size = Size(rowW, rowH), style = Stroke(width = 1.1f * px))
            val safeName = entry.name.take(10)
            drawIndieRunnerText(
                text = "#${index + 1} $safeName — ${entry.bats} šiš. / ${entry.score}m",
                x = w * 0.5f,
                y = rowTop + 13f * px,
                textSize = 8.6f * px,
                textColor = android.graphics.Color.rgb(236, 239, 241)
            )
        }
    }

    val btnW = w * 0.64f
    val btnH = 44f * px
    val btnLeft = w * 0.5f - btnW / 2f
    val btnTop = h * 0.74f
    drawRect(Color.Black, topLeft = Offset(btnLeft - 3f * px, btnTop - 3f * px), size = Size(btnW + 6f * px, btnH + 6f * px))
    drawRect(Color(0xFF7F0000), topLeft = Offset(btnLeft, btnTop), size = Size(btnW, btnH))
    drawLine(Color(0xFFD32F2F), Offset(btnLeft, btnTop + 2f * px), Offset(btnLeft + btnW, btnTop + 2f * px), strokeWidth = 2f * px)
    drawIndieRunnerText(
        text = "↺ POKUŠAJ PONOVO",
        x = w * 0.5f,
        y = btnTop + 29f * px,
        textSize = 14f * px,
        textColor = android.graphics.Color.WHITE
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDepthVignette(
    w: Float,
    h: Float,
    px: Float,
    depthMeters: Float
) {
    val vignetteAlpha = (depthMeters / 180f).coerceIn(0f, 0.52f)
    if (vignetteAlpha <= 0.001f) return
    val edge = 24f * px
    val frameColor = Color.Black.copy(alpha = vignetteAlpha)
    // Flat Sierra VGA frame vignette — no modern radial gradient.
    drawRect(frameColor, topLeft = Offset(0f, 0f), size = Size(w, edge))
    drawRect(frameColor, topLeft = Offset(0f, h - edge), size = Size(w, edge))
    drawRect(frameColor, topLeft = Offset(0f, 0f), size = Size(edge, h))
    drawRect(frameColor, topLeft = Offset(w - edge, 0f), size = Size(edge, h))
}



private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStableHorizontalBackground(
    w: Float,
    h: Float,
    groundY: Float,
    walkMeters: Float,
    px: Float,
    difficultyLevel: Int = 0,
    biomeIndex: Int = 0
) {
    val palette = runnerSierraPalette(biomeIndex)
    val biomeSpeedMult = 1f + difficultyLevel * 0.09f

    // VGA flat bands — 4 zone umjesto 3 za bogatiji depth
    drawRect(palette.darkest.copy(alpha = 0.75f), size = Size(w, h * 0.22f))
    drawRect(palette.midDark.copy(alpha = 0.75f), topLeft = Offset(0f, h * 0.22f), size = Size(w, h * 0.25f))
    drawRect(palette.mid.copy(alpha = 0.67f), topLeft = Offset(0f, h * 0.47f), size = Size(w, h * 0.28f))
    drawRect(palette.midLight.copy(alpha = 0.18f), topLeft = Offset(0f, h * 0.75f), size = Size(w, h * 0.25f))
    // Chunky band separator lines — VGA karakteristična crta između zona
    drawLine(Color.Black.copy(alpha = 0.80f), Offset(0f, h * 0.22f), Offset(w, h * 0.22f), strokeWidth = 3.0f * px)
    drawLine(palette.highlight.copy(alpha = 0.22f), Offset(0f, h * 0.22f - px), Offset(w, h * 0.22f - px), strokeWidth = 1.2f * px)
    drawLine(Color.Black.copy(alpha = 0.60f), Offset(0f, h * 0.47f), Offset(w, h * 0.47f), strokeWidth = 2.2f * px)
    drawLine(palette.midLight.copy(alpha = 0.16f), Offset(0f, h * 0.47f - px), Offset(w, h * 0.47f - px), strokeWidth = 1.0f * px)

    val farDrift = (walkMeters * 0.4f * biomeSpeedMult) % (260f * px)
    repeat(6) { i ->
        val x = ((i * 235f * px - farDrift) % (w + 320f * px)) - 150f * px
        val y = h * (0.17f + (i % 3) * 0.035f)
        val chamber = Path().apply {
            moveTo(x - 120f * px, y)
            cubicTo(x - 60f * px, y - 70f * px, x + 70f * px, y + 58f * px, x + 185f * px, y - 12f * px)
            lineTo(x + 215f * px, groundY - 28f * px)
            cubicTo(x + 120f * px, groundY - 55f * px, x - 10f * px, groundY - 18f * px, x - 140f * px, groundY - 42f * px)
            close()
        }
        drawInkPath(chamber, palette.midDark.copy(alpha = 0.17f), px)
        drawPath(chamber, palette.midLight.copy(alpha = 0.12f), style = Stroke(width = 1.2f * px))
    }

    val midDrift = (walkMeters * 1.1f * biomeSpeedMult) % (210f * px)
    repeat(8) { i ->
        val x = ((i * 145f * px - midDrift) % (w + 240f * px)) - 80f * px
        val top = h * (0.10f + (i % 4) * 0.025f)
        val len = (26f + (i % 5) * 12f) * px
        val p = Path().apply {
            moveTo(x - (7f + i % 3) * px, top)
            quadraticBezierTo(x, top + len * 0.42f, x + sin(i.toFloat()) * 2f * px, top + len)
            quadraticBezierTo(x + (8f + i % 2) * px, top + len * 0.36f, x + (7f + i % 3) * px, top)
            close()
        }
        drawInkPath(p, palette.accent.copy(alpha = 0.24f), px, highlight = false)
    }

    // Flat biome atmosphere hints only; stronger per-biome props are drawn by drawBiomeAtmosphere().
    when (biomeIndex % 5) {
        1 -> {
            val waterTop = groundY - 34f * px
            drawRect(palette.mid.copy(alpha = 0.37f), topLeft = Offset(0f, waterTop), size = Size(w, h - waterTop))
            drawLine(palette.floorEdge, Offset(0f, waterTop), Offset(w, waterTop), strokeWidth = 2.0f * px)
        }
        3 -> {
            drawRect(palette.dark.copy(alpha = 0.49f), topLeft = Offset(0f, groundY + 18f * px), size = Size(w, h - groundY))
            drawLine(palette.ceilingEdge, Offset(0f, groundY + 18f * px), Offset(w, groundY + 18f * px), strokeWidth = 2.5f * px)
        }
        4 -> drawLine(palette.highlight.copy(alpha = 0.30f), Offset(0f, groundY - 5f * px), Offset(w, groundY - 6f * px), strokeWidth = 3f * px)
    }

    val nearDrift = (walkMeters * 2.2f * biomeSpeedMult) % (180f * px)
    val floorPath = Path().apply {
        moveTo(0f, groundY)
        cubicTo(w * 0.16f, groundY - h * 0.026f, w * 0.34f, groundY + h * 0.016f, w * 0.52f, groundY - h * 0.016f)
        cubicTo(w * 0.68f, groundY - h * 0.040f, w * 0.84f, groundY + h * 0.020f, w, groundY - h * 0.012f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawInkPath(floorPath, palette.dark, px)
    drawPath(floorPath, Color.Black.copy(alpha = 0.88f), style = Stroke(width = 4f * px))
    drawPath(floorPath, palette.midLight.copy(alpha = 0.45f), style = Stroke(width = 1.2f * px))

    // Small angular foreground stones, not soft ovals.
    repeat(8) { i ->
        val x = ((i * 97f * px - nearDrift) % (w + 90f * px)) - 35f * px
        val y = groundY + (10f + (i % 5) * 12f) * px
        val stone = Path().apply {
            moveTo(x, y)
            lineTo(x + (7f + i % 3) * px, y - (3f + i % 2) * px)
            lineTo(x + (13f + i % 4) * px, y + 2f * px)
            lineTo(x + 4f * px, y + 5f * px)
            close()
        }
        drawInkPath(stone, palette.midDark.copy(alpha = 0.35f), px)
    }

    // Extra Sierra VGA biome props for biomes 5-9.
    when (biomeIndex % 10) {
        5 -> repeat(6) { i ->
            val fx = w * (0.12f + (i % 3) * 0.32f) + sin(i.toFloat()) * 18f * px
            val fy = h * (0.26f + (i / 3) * 0.20f)
            val fw = (18f + i % 3 * 8f) * px
            val fh = (10f + i % 2 * 5f) * px
            drawOval(Color(0xFF9C8560), Offset(fx, fy), Size(fw, fh))
            drawOval(Color.Black, Offset(fx, fy), Size(fw, fh), style = Stroke(width = 1.4f * px))
            drawLine(Color(0xFFFFF8E7).copy(alpha = 0.45f), Offset(fx + 4f * px, fy + 3f * px), Offset(fx + fw - 5f * px, fy + fh - 3f * px), strokeWidth = 1f * px)
        }
        6 -> repeat(8) { i ->
            val x = w * ((i * 17 % 100) / 100f)
            val y = h * (0.10f + (i % 4) * 0.11f)
            val c = Path().apply { moveTo(x, y - 16f * px); lineTo(x + 6f * px, y + 15f * px); lineTo(x - 5f * px, y + 13f * px); close() }
            drawPath(c, Color(0xFF66BB6A)); drawPath(c, Color.Black, style = Stroke(width = 1.4f * px))
            drawLine(Color.White.copy(alpha = 0.55f), Offset(x - 2f * px, y - 10f * px), Offset(x - 3f * px, y + 8f * px), strokeWidth = 1f * px)
        }
        7 -> repeat(20) { i ->
            val bx = w * ((i * 37 % 100) / 100f)
            val by = h * (0.08f + (i % 5) * 0.035f)
            val wing = 5f * px
            drawLine(Color.Black.copy(alpha = 0.55f), Offset(bx - wing, by), Offset(bx, by + 2f * px), strokeWidth = 1.5f * px)
            drawLine(Color.Black.copy(alpha = 0.55f), Offset(bx, by + 2f * px), Offset(bx + wing, by), strokeWidth = 1.5f * px)
        }
        8 -> repeat(4) { i ->
            val x = w * (0.14f + i * 0.22f)
            val crack = Path().apply { moveTo(x, groundY + 8f * px); lineTo(x + 36f * px, groundY + 15f * px); lineTo(x + 30f * px, groundY + 22f * px); lineTo(x - 4f * px, groundY + 14f * px); close() }
            drawPath(crack, Color(0xFFFF6E00).copy(alpha = 0.45f)); drawPath(crack, Color.Black, style = Stroke(width = 1.2f * px))
        }
        9 -> repeat(10) { i ->
            val cx = w * ((i * 23 % 100) / 100f)
            val cy = h * (0.16f + (i % 5) * 0.11f)
            val r = (5f + i % 3 * 3f) * px
            val d = Path().apply { moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close() }
            drawPath(d, Color.White.copy(alpha = 0.35f)); drawPath(d, Color.Black, style = Stroke(width = 1f * px))
        }
    }
}


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGoldenShell(
    x: Float,
    y: Float,
    px: Float
) {
    val shell = Path().apply {
        moveTo(x, y - 12f * px)
        cubicTo(x - 18f * px, y - 6f * px, x - 20f * px, y + 10f * px, x, y + 16f * px)
        cubicTo(x + 20f * px, y + 10f * px, x + 18f * px, y - 6f * px, x, y - 12f * px)
        close()
    }
    drawPath(Path().apply { addPath(shell, Offset(2f * px, 2f * px)) }, Color.Black.copy(alpha = 0.35f))
    drawPath(shell, Color(0xFFFFC107))
    drawPath(shell, Color.Black, style = Stroke(width = 2.8f * px))
    repeat(5) { i ->
        val a = -0.85f + i * 0.42f
        drawLine(
            Color(0xFFFFF8E1).copy(alpha = 0.72f),
            Offset(x, y - 9f * px),
            Offset(x + sin(a) * 15f * px, y + 11f * px),
            strokeWidth = 1.2f * px,
            cap = StrokeCap.Round
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBoatSpeleologist(
    x: Float,
    y: Float,
    px: Float,
    paddleTimer: Float,
    phaseTime: Float
) {
    val body = Path().apply {
        moveTo(x - 8f * px, y - 30f * px)
        lineTo(x + 9f * px, y - 30f * px)
        lineTo(x + 12f * px, y - 10f * px)
        lineTo(x - 10f * px, y - 9f * px)
        close()
    }
    drawSierraShape(body, SPELE_SUIT_DARK, SPELE_SUIT_MID, SPELE_SUIT_HI, px)

    // Compact seated helmet/head for lake mode. Keep this local because drawProfileHelmet
    // is scoped inside drawSpeleologist() and cannot be called from this top-level helper.
    val headCenter = Offset(x + 4f * px, y - 42f * px)
    drawSierraCircle(headCenter, 10.5f * px, SPELE_SKIN_DARK, SPELE_SKIN_MID, SPELE_SKIN_HI, px)
    val helmet = Path().apply {
        moveTo(headCenter.x - 11.5f * px, headCenter.y - 1.5f * px)
        cubicTo(headCenter.x - 10f * px, headCenter.y - 12f * px, headCenter.x + 9f * px, headCenter.y - 13f * px, headCenter.x + 12f * px, headCenter.y - 1f * px)
        lineTo(headCenter.x + 13.5f * px, headCenter.y + 1.8f * px)
        cubicTo(headCenter.x + 5f * px, headCenter.y + 4f * px, headCenter.x - 8f * px, headCenter.y + 3f * px, headCenter.x - 11.5f * px, headCenter.y - 1.5f * px)
        close()
    }
    drawSierraShape(helmet, SPELE_HELMET_DARK, SPELE_HELMET_MID, SPELE_HELMET_HI, px)
    drawSierraRoundRect(
        Offset(headCenter.x + 7.5f * px, headCenter.y - 6.5f * px),
        Size(7f * px, 5f * px),
        2f * px,
        SPELE_HELMET_DARK, Color(0xFF455A64), Color(0xFFECEFF1), px
    )
    drawCircle(Color.Black, radius = 1.2f * px, center = Offset(headCenter.x + 4.0f * px, headCenter.y - 0.8f * px))

    val paddleDown = paddleTimer > 0.05f
    val angle = if (paddleDown) 0.78f else -0.32f + sin(phaseTime * 8f) * 0.10f
    val hand = Offset(x + 10f * px, y - 20f * px)
    val blade = Offset(hand.x + cos(angle) * 42f * px, hand.y + sin(angle) * 42f * px)
    drawLine(Color.Black, hand, blade, strokeWidth = 4.0f * px, cap = StrokeCap.Round)
    drawLine(Color(0xFF8D5524), hand, blade, strokeWidth = 2.4f * px, cap = StrokeCap.Round)
    drawOval(Color(0xFFD6A56F), Offset(blade.x - 5f * px, blade.y - 9f * px), Size(10f * px, 18f * px))
    drawOval(Color.Black, Offset(blade.x - 5f * px, blade.y - 9f * px), Size(10f * px, 18f * px), style = Stroke(width = 1.2f * px))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLakeCave(
    w: Float,
    h: Float,
    px: Float,
    phaseTime: Float,
    boatLift: Float,
    rockObstacles: List<RunnerObstacle>,
    sharkActive: Boolean,
    sharkX: Float,
    paddleBoost: Float,
    paddleTapCount: Int,
    jumpVelocity: Float,
    biomeIndex: Int,
    difficultyLevel: Int,
    status: RunnerStatus,
    lakeDuration: Float = 10f,
    collectFlashes: List<RunnerCollectFlash> = emptyList(),
    tokens: List<RunnerToken> = emptyList()
) {
    val palette = runnerSierraPalette(biomeIndex)

    // --- POZADINA ---
    drawRect(Color(0xFF060E14), size = Size(w, h * 0.38f))
    repeat(10) { i ->
        val sx = w * (0.05f + i * 0.10f)
        val sh = (32f + (i % 4) * 14f) * px
        val sw = (7f + i % 3 * 4f) * px
        val sp = Path().apply {
            moveTo(sx - sw, 0f)
            lineTo(sx + sw, 0f)
            lineTo(sx + sw * 0.4f, sh)
            lineTo(sx - sw * 0.4f, sh)
            close()
        }
        drawPath(sp, Color(0xFF0D1F2D))
        drawPath(sp, Color.Black, style = Stroke(width = 1.8f * px))
        drawLine(
            Color(0xFF1A4A6A).copy(alpha = 0.55f),
            Offset(sx - sw * 0.5f, 2f * px),
            Offset(sx - sw * 0.2f, sh - 4f * px),
            strokeWidth = 1.1f * px
        )
    }

    drawRect(Color(0xFF0A1A24), topLeft = Offset(0f, 0f), size = Size(18f * px, h))
    drawRect(Color(0xFF0A1A24), topLeft = Offset(w - 18f * px, 0f), size = Size(18f * px, h))

    val waterY = h * 0.60f
    drawRect(Color(0xFF041E30), topLeft = Offset(0f, waterY), size = Size(w, h - waterY))
    repeat(3) { wave ->
        val wy = waterY + wave * 3.5f * px
        val wAmp = (2.5f - wave * 0.6f) * px
        var x = 0f
        while (x < w) {
            val y1 = wy + sin(phaseTime * 2.2f + x / (w * 0.18f) + wave) * wAmp
            val y2 = wy + sin(phaseTime * 2.2f + (x + w * 0.05f) / (w * 0.18f) + wave) * wAmp
            drawLine(
                Color(0xFF1A6B8A).copy(alpha = 0.48f - wave * 0.10f),
                Offset(x, y1),
                Offset(x + w * 0.05f, y2),
                strokeWidth = 1.2f * px
            )
            x += w * 0.05f
        }
    }
    drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, waterY), Offset(w, waterY), strokeWidth = 1.0f * px)

    // --- KAMENCI ---
    rockObstacles.forEach { rock ->
        val rockH = 12f * px + rock.width * 0.30f
        val rockTop = waterY - rockH
        val rp = Path().apply {
            moveTo(rock.x, waterY)
            lineTo(rock.x + rock.width * 0.12f, rockTop + rockH * 0.3f)
            lineTo(rock.x + rock.width * 0.32f, rockTop)
            lineTo(rock.x + rock.width * 0.58f, rockTop + rockH * 0.15f)
            lineTo(rock.x + rock.width * 0.82f, rockTop + rockH * 0.08f)
            lineTo(rock.x + rock.width, waterY)
            close()
        }
        drawOval(Color.Black.copy(alpha = 0.28f), Offset(rock.x + 4f * px, waterY - 3f * px), Size(rock.width - 8f * px, 7f * px))
        drawPath(rp, Color(0xFF4A4A4A))
        drawPath(rp, Color.Black, style = Stroke(width = 2.2f * px))
        drawLine(
            Color(0xFF9E9E9E).copy(alpha = 0.65f),
            Offset(rock.x + rock.width * 0.12f, rockTop + rockH * 0.3f),
            Offset(rock.x + rock.width * 0.42f, rockTop + rockH * 0.05f),
            strokeWidth = 1.4f * px
        )
        drawLine(
            Color.Black.copy(alpha = 0.55f),
            Offset(rock.x + rock.width * 0.55f, rockTop + rockH * 0.25f),
            Offset(rock.x + rock.width * 0.68f, rockTop + rockH * 0.55f),
            strokeWidth = 1.0f * px
        )
    }

    tokens.forEach { drawGoldenShell(it.x, h * it.yFactor, px) }
    collectFlashes.forEach { flash ->
        val age = (phaseTime - flash.timestamp).coerceAtLeast(0f)
        if (age < 0.55f) {
            val alpha = (1f - age / 0.55f).coerceIn(0f, 1f)
            drawCircle(Color(0xFFFFE57F).copy(alpha = alpha * 0.42f), radius = (8f + age * 60f) * px, center = Offset(flash.x, flash.y))
        }
    }

    // --- ČAMAC I SPELEOLOG ---
    val boatX = w * 0.50f
    val boatY = waterY + boatLift
    val boatW = 84f * px
    val boatH = 19f * px
    val paddleAngle = if (paddleBoost > 0f) sin(phaseTime * 14f) * 38f else sin(phaseTime * 3.5f) * 12f

    val paddlePivotX = boatX - boatW * 0.28f
    val paddlePivotY = boatY - 8f * px
    val paddleLen = 28f * px
    val paddleAngleRad = Math.toRadians(paddleAngle.toDouble()).toFloat()
    val paddleEndX = paddlePivotX + cos(paddleAngleRad + 1.2f) * paddleLen
    val paddleEndY = paddlePivotY + sin(paddleAngleRad + 1.2f) * paddleLen
    drawLine(Color.Black, Offset(paddlePivotX, paddlePivotY), Offset(paddleEndX, paddleEndY), strokeWidth = 5.0f * px, cap = StrokeCap.Round)
    drawLine(Color(0xFF5D3A1A), Offset(paddlePivotX, paddlePivotY), Offset(paddleEndX, paddleEndY), strokeWidth = 3.5f * px, cap = StrokeCap.Round)
    drawOval(Color(0xFF4A2800), Offset(paddleEndX - 7f * px, paddleEndY - 4f * px), Size(14f * px, 8f * px))
    drawOval(Color.Black, Offset(paddleEndX - 7f * px, paddleEndY - 4f * px), Size(14f * px, 8f * px), style = Stroke(width = 1.5f * px))

    if (paddleBoost > 20f) {
        repeat(4) { i ->
            val sx = paddleEndX + (-8f + i * 5f) * px
            val sy = paddleEndY + sin(phaseTime * 18f + i) * 3f * px
            drawCircle(Color(0xFFB2EBF2).copy(alpha = 0.55f), radius = (1.2f + i % 2) * px, center = Offset(sx, sy))
        }
    }

    val hullPath = Path().apply {
        moveTo(boatX - boatW * 0.5f, boatY)
        lineTo(boatX - boatW * 0.42f, boatY - boatH)
        lineTo(boatX + boatW * 0.48f, boatY - boatH)
        lineTo(boatX + boatW * 0.58f, boatY)
        close()
    }
    drawPath(hullPath, Color(0xFF4A2800))
    drawPath(hullPath, Color.Black, style = Stroke(width = 2.5f * px))
    drawLine(Color(0xFF8D5524), Offset(boatX - boatW * 0.42f, boatY - boatH + px), Offset(boatX + boatW * 0.48f, boatY - boatH + px), strokeWidth = 2.2f * px)
    repeat(3) { plank ->
        val plankX = boatX - boatW * 0.28f + plank * boatW * 0.22f
        drawLine(Color(0xFF3A1F00).copy(alpha = 0.55f), Offset(plankX, boatY - boatH + 3f * px), Offset(plankX, boatY - 2f * px), strokeWidth = 1.0f * px)
    }
    repeat(4) { w2 ->
        val wx = boatX - boatW * 0.3f + w2 * boatW * 0.22f
        val wy2 = waterY + boatLift * 0.15f + sin(phaseTime * 4f + w2) * 2.0f * px
        drawLine(Color(0xFF4DD0E1).copy(alpha = 0.28f - w2 * 0.04f), Offset(wx - 8f * px, wy2), Offset(wx + 8f * px, wy2 + 1.5f * px), strokeWidth = 1.3f * px)
    }

    val spelX = boatX + 4f * px
    val spelY = boatY - boatH - 28f * px
    drawRect(Color.Black, Offset(spelX - 9f * px, spelY + 6f * px), Size(18f * px, 22f * px))
    drawRect(Color(0xFFE53935), Offset(spelX - 8f * px, spelY + 7f * px), Size(16f * px, 20f * px))
    drawRect(Color.Black, Offset(spelX - 8f * px, spelY + 7f * px), Size(16f * px, 20f * px), style = Stroke(width = 1.5f * px))
    drawLine(Color.Black, Offset(spelX - 7f * px, spelY + 26f * px), Offset(spelX + 12f * px, spelY + 26f * px), strokeWidth = 6f * px)
    drawLine(Color(0xFF1565C0), Offset(spelX - 6f * px, spelY + 26f * px), Offset(spelX + 11f * px, spelY + 26f * px), strokeWidth = 4f * px)
    drawOval(Color.Black, Offset(spelX - 11f * px, spelY - 12f * px), Size(22f * px, 20f * px))
    drawOval(Color(0xFFFFD54F), Offset(spelX - 10f * px, spelY - 11f * px), Size(20f * px, 18f * px))
    drawOval(Color.Black, Offset(spelX - 10f * px, spelY - 11f * px), Size(20f * px, 18f * px), style = Stroke(width = 1.5f * px))
    drawCircle(Color.Black, radius = 4.5f * px, center = Offset(spelX + 8f * px, spelY - 5f * px))
    drawCircle(Color(0xFFFFF176), radius = 3.0f * px, center = Offset(spelX + 8f * px, spelY - 5f * px))
    val beamPath = Path().apply {
        moveTo(spelX + 10f * px, spelY - 5f * px)
        lineTo(spelX + 55f * px, spelY - 18f * px)
        lineTo(spelX + 55f * px, spelY + 4f * px)
        close()
    }
    drawPath(beamPath, Color(0xFFFFF59D).copy(alpha = 0.20f))

    // --- SPELEO SHARK ---
    if (sharkActive && sharkX > -160f * px) {
        val sx = sharkX
        val sy = waterY - 8f * px
        val sharkBody = Path().apply {
            moveTo(sx, sy)
            lineTo(sx + 20f * px, sy - 18f * px)
            lineTo(sx + 90f * px, sy - 14f * px)
            lineTo(sx + 110f * px, sy - 4f * px)
            lineTo(sx + 120f * px, sy - 22f * px)
            lineTo(sx + 130f * px, sy - 2f * px)
            lineTo(sx + 120f * px, sy + 8f * px)
            lineTo(sx + 90f * px, sy + 10f * px)
            lineTo(sx + 20f * px, sy + 8f * px)
            close()
        }
        drawPath(sharkBody, Color(0xFF1A2A3A))
        drawPath(sharkBody, Color.Black, style = Stroke(width = 2.5f * px))
        val sharkBelly = Path().apply {
            moveTo(sx + 22f * px, sy + 5f * px)
            lineTo(sx + 88f * px, sy + 7f * px)
            lineTo(sx + 88f * px, sy + 4f * px)
            lineTo(sx + 22f * px, sy + 3f * px)
            close()
        }
        drawPath(sharkBelly, Color(0xFF3A5060))
        val fin = Path().apply {
            moveTo(sx + 55f * px, sy - 14f * px)
            lineTo(sx + 68f * px, sy - 42f * px)
            lineTo(sx + 82f * px, sy - 14f * px)
            close()
        }
        drawPath(fin, Color(0xFF152030))
        drawPath(fin, Color.Black, style = Stroke(width = 2.0f * px))
        drawLine(Color(0xFF2A4A5A).copy(alpha = 0.62f), Offset(sx + 58f * px, sy - 14f * px), Offset(sx + 68f * px, sy - 38f * px), strokeWidth = 1.2f * px)

        val antennaBaseX = sx + 112f * px
        val antennaBaseY = sy - 16f * px
        val antennaTipX = sx + 148f * px
        val antennaTipY = sy - 36f * px
        drawLine(Color.Black, Offset(antennaBaseX, antennaBaseY), Offset(antennaTipX, antennaTipY), strokeWidth = 2.8f * px)
        drawLine(Color(0xFF4A3A20), Offset(antennaBaseX, antennaBaseY), Offset(antennaTipX, antennaTipY), strokeWidth = 1.8f * px)
        drawCircle(Color(0xFFFFF176).copy(alpha = 0.35f + sin(phaseTime * 6f) * 0.15f), radius = 9f * px, center = Offset(antennaTipX, antennaTipY))
        drawCircle(Color(0xFFFFF176), radius = 3.5f * px, center = Offset(antennaTipX, antennaTipY))
        drawCircle(Color.White, radius = 1.5f * px, center = Offset(antennaTipX - px, antennaTipY - px))

        val eyeX = sx + 106f * px
        val eyeY = sy - 8f * px
        drawCircle(Color(0xFF00E5FF).copy(alpha = 0.40f + sin(phaseTime * 4f) * 0.12f), radius = 7f * px, center = Offset(eyeX, eyeY))
        drawCircle(Color(0xFF00BCD4), radius = 3.5f * px, center = Offset(eyeX, eyeY))
        drawCircle(Color.Black, radius = 1.8f * px, center = Offset(eyeX, eyeY))
        drawCircle(Color.White, radius = 0.9f * px, center = Offset(eyeX - px, eyeY - px))

        repeat(5) { t ->
            val tx = sx + 104f * px + t * 3.5f * px
            val ty = sy + 4f * px
            val tooth = Path().apply {
                moveTo(tx, ty)
                lineTo(tx + 1.5f * px, ty + 6f * px)
                lineTo(tx + 3f * px, ty)
                close()
            }
            drawPath(tooth, Color.White)
            drawPath(tooth, Color.Black, style = Stroke(width = 0.8f * px))
        }
        repeat(5) { sp ->
            val spX = sx + (35f + sp * 14f) * px
            val spY = waterY + sin(phaseTime * 8f + sp) * 2.5f * px
            drawCircle(Color(0xFF80DEEA).copy(alpha = 0.32f), radius = (2.5f + sp % 2) * px, center = Offset(spX, spY))
        }
        val gap = boatX - (sx + 120f * px)
        if (gap < w * 0.35f) {
            val dangerAlpha = (1f - gap / (w * 0.35f)).coerceIn(0f, 1f)
            drawRoundRect(Color(0xCC3D0000).copy(alpha = dangerAlpha * 0.85f), Offset(w * 0.5f - 64f * px, h * 0.14f), Size(128f * px, 26f * px), CornerRadius(999f))
            drawIndieRunnerText("⚠ PADDLAJ BRZO!", w * 0.5f, h * 0.14f + 18f * px, 11.5f * px, android.graphics.Color.rgb(255, 82, 82))
        }
    }

    drawRoundRect(Color(0xAA07111F), Offset(w * 0.5f - 70f * px, 14f * px), Size(140f * px, 28f * px), CornerRadius(999f))
    val lakeSecondsLeft = (lakeDuration - phaseTime).toInt().coerceAtLeast(0)
    drawIndieRunnerText("🚣 JEZERO  ${lakeSecondsLeft}s", w * 0.5f, 34f * px, 12.5f * px, android.graphics.Color.rgb(178, 235, 242))
    if (paddleBoost > 5f) {
        val boostFrac = (paddleBoost / 210f).coerceIn(0f, 1f)
        drawRoundRect(Color(0xFF0D2A3A), Offset(w * 0.5f - 55f * px, 46f * px), Size(110f * px, 7f * px), CornerRadius(999f))
        drawRoundRect(Color(0xFF00BCD4), Offset(w * 0.5f - 55f * px, 46f * px), Size(110f * px * boostFrac, 7f * px), CornerRadius(999f))
    }
}


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPetzlStop(
    cx: Float,
    cy: Float,
    px: Float,
    tint: Color = Color.White,
    scale: Float = 1.0f
) {
    val s = scale.coerceAtLeast(0.25f)
    val metalDark = Color(0xFF37474F)
    val metalMid = Color(0xFF607D8B)
    val metalHi = Color(0xFFB0BEC5)
    val metalShine = Color(0xFFECEFF1)
    val ink = Color.Black

    val frameW = 24f * px * s
    val frameH = 32f * px * s
    val frameLeft = cx - frameW * 0.52f
    val frameTop = cy - frameH * 0.50f

    val outerFrame = Path().apply {
        moveTo(frameLeft + frameW * 0.18f, frameTop)
        cubicTo(frameLeft + frameW * 0.62f, frameTop, frameLeft + frameW, frameTop + frameH * 0.22f, frameLeft + frameW, frameTop + frameH * 0.50f)
        cubicTo(frameLeft + frameW, frameTop + frameH * 0.78f, frameLeft + frameW * 0.62f, frameTop + frameH, frameLeft + frameW * 0.18f, frameTop + frameH)
        cubicTo(frameLeft, frameTop + frameH, frameLeft, frameTop + frameH * 0.80f, frameLeft, frameTop + frameH * 0.68f)
        lineTo(frameLeft, frameTop + frameH * 0.32f)
        cubicTo(frameLeft, frameTop + frameH * 0.18f, frameLeft, frameTop, frameLeft + frameW * 0.18f, frameTop)
        close()
    }

    drawPath(Path().apply { addPath(outerFrame, Offset(1.5f * px * s, 1.8f * px * s)) }, ink.copy(alpha = 0.55f))
    drawPath(outerFrame, metalDark)
    drawPath(Path().apply { addPath(outerFrame, Offset(-0.6f * px * s, -0.5f * px * s)) }, metalMid)
    drawPath(outerFrame, ink, style = Stroke(width = 2.8f * px * s))
    drawPath(Path().apply { addPath(outerFrame, Offset(-0.9f * px * s, -0.9f * px * s)) }, metalShine.copy(alpha = 0.42f), style = Stroke(width = 1.0f * px * s))

    val innerW = frameW * 0.52f
    val innerH = frameH * 0.56f
    val innerLeft = cx - innerW * 0.42f
    val innerTop = cy - innerH * 0.50f
    drawOval(Color(0xFF1A1A1A), Offset(innerLeft, innerTop), Size(innerW, innerH))
    drawOval(ink, Offset(innerLeft, innerTop), Size(innerW, innerH), style = Stroke(width = 1.4f * px * s))

    val bridgeY = cy + 2f * px * s
    val bridgeLeft = frameLeft + frameW * 0.08f
    val bridgeRight = frameLeft + frameW * 0.88f
    val bridgeH = 5f * px * s
    drawRoundRect(ink.copy(alpha = 0.60f), Offset(bridgeLeft + 1.2f * px * s, bridgeY + 1.5f * px * s), Size(bridgeRight - bridgeLeft, bridgeH), CornerRadius(bridgeH * 0.5f))
    drawRoundRect(metalDark, Offset(bridgeLeft, bridgeY), Size(bridgeRight - bridgeLeft, bridgeH), CornerRadius(bridgeH * 0.5f))
    drawRoundRect(metalMid, Offset(bridgeLeft - 0.5f * px * s, bridgeY - 0.5f * px * s), Size(bridgeRight - bridgeLeft, bridgeH), CornerRadius(bridgeH * 0.5f))
    drawRoundRect(ink, Offset(bridgeLeft, bridgeY), Size(bridgeRight - bridgeLeft, bridgeH), CornerRadius(bridgeH * 0.5f), style = Stroke(width = 1.4f * px * s))
    drawLine(metalShine.copy(alpha = 0.50f), Offset(bridgeLeft + 3f * px * s, bridgeY + 1.2f * px * s), Offset(bridgeRight - 3f * px * s, bridgeY + 1.2f * px * s), strokeWidth = 1.0f * px * s)

    val sheaveR = 5.5f * px * s
    val sheaveCX = cx + 1f * px * s
    val sheaveCY = cy + frameH * 0.22f
    drawCircle(ink.copy(alpha = 0.50f), radius = sheaveR + 1.2f * px * s, center = Offset(sheaveCX + 1.2f * px * s, sheaveCY + 1.4f * px * s))
    drawCircle(metalDark, radius = sheaveR, center = Offset(sheaveCX, sheaveCY))
    drawCircle(metalMid, radius = sheaveR * 0.80f, center = Offset(sheaveCX - 0.5f * px * s, sheaveCY - 0.5f * px * s))
    drawCircle(ink, radius = sheaveR, center = Offset(sheaveCX, sheaveCY), style = Stroke(width = 1.5f * px * s))
    drawCircle(ink, radius = 1.4f * px * s, center = Offset(sheaveCX, sheaveCY))
    drawCircle(metalShine.copy(alpha = 0.70f), radius = 0.7f * px * s, center = Offset(sheaveCX - 0.6f * px * s, sheaveCY - 0.6f * px * s))

    val leverBaseX = frameLeft + frameW * 0.80f
    val leverBaseY = cy - frameH * 0.12f
    val leverTipX = leverBaseX + 9f * px * s
    val leverTipY = leverBaseY - 18f * px * s
    drawLine(ink.copy(alpha = 0.55f), Offset(leverBaseX + 1.5f * px * s, leverBaseY + 1.5f * px * s), Offset(leverTipX + 1.5f * px * s, leverTipY + 1.5f * px * s), strokeWidth = 5.0f * px * s, cap = StrokeCap.Round)
    drawLine(ink, Offset(leverBaseX, leverBaseY), Offset(leverTipX, leverTipY), strokeWidth = 6.2f * px * s, cap = StrokeCap.Round)
    drawLine(metalDark, Offset(leverBaseX, leverBaseY), Offset(leverTipX, leverTipY), strokeWidth = 4.2f * px * s, cap = StrokeCap.Round)
    drawLine(metalMid, Offset(leverBaseX - 0.5f * px * s, leverBaseY - 0.5f * px * s), Offset(leverTipX - 0.5f * px * s, leverTipY - 0.5f * px * s), strokeWidth = 2.8f * px * s, cap = StrokeCap.Round)
    drawLine(metalShine.copy(alpha = 0.48f), Offset(leverBaseX - 1f * px * s, leverBaseY - 1f * px * s), Offset(leverTipX - 1f * px * s, leverTipY - 1f * px * s), strokeWidth = 1.1f * px * s, cap = StrokeCap.Round)
    drawSierraCircle(Offset(leverTipX, leverTipY), 3.5f * px * s, metalDark, metalMid, metalShine, px, alpha = 1f)

    // Flat metal shine — Sierra-style diagonal glint across the Stop body.
    drawLine(
        Color.White.copy(alpha = 0.62f),
        Offset(cx - 6.5f * px * s, cy - 10.0f * px * s),
        Offset(cx + 7.5f * px * s, cy + 9.5f * px * s),
        strokeWidth = 1.4f * px * s,
        cap = StrokeCap.Round
    )

    if (tint != Color.White) {
        drawPath(outerFrame, tint.copy(alpha = 0.28f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTransitionToVertical(
    w: Float,
    h: Float,
    groundY: Float,
    px: Float,
    transitionTime: Float,
    status: RunnerStatus,
    biomeIndex: Int = 0,
    anchorWindowTime: Float = 0f,
    anchorPressed: Boolean = false,
    anchorFallTime: Float = 0f,
    anchorFailing: Boolean = false
) {
    val stepForFade = (transitionTime / 4.2f).coerceIn(0f, 1f)
    // Sierra VGA transition backdrop: flat darkness with deterministic cave-star dust.
    drawRect(Color(0xFF0A0A0F), size = Size(w, h))
    repeat(12) { i ->
        val seed = biomeIndex * 7 + i
        val sx = (((seed * 37) % 100) / 100f) * w
        val sy = h * (0.08f + (((seed * 53) % 52) / 100f))
        drawCircle(
            Color.White.copy(alpha = 0.55f),
            radius = 1.2f * px,
            center = Offset(sx, sy)
        )
    }

    val ledgeY = groundY - 10f * px
    val holeLeft = w * 0.50f
    val holeTop = h * 0.30f
    val holeWidth = w * 0.42f
    val holeHeight = h * 0.63f
    val ropeX = w * 0.66f
    val ropeTop = h * 0.12f

    // Big vertical entrance: a readable abyss appears exactly at the 30-second transition.
    val caveLip = Path().apply {
        moveTo(0f, ledgeY)
        cubicTo(w * 0.14f, ledgeY - 18f * px, w * 0.28f, ledgeY + 8f * px, holeLeft - 18f * px, ledgeY - 3f * px)
        lineTo(holeLeft + 7f * px, ledgeY + 26f * px)
        cubicTo(w * 0.38f, ledgeY + 24f * px, w * 0.20f, ledgeY + 37f * px, 0f, ledgeY + 24f * px)
        close()
    }
    drawPath(caveLip, Color(0xFF392B23))
    drawPath(caveLip, Color(0xFFD6A56F).copy(alpha = 0.38f), style = Stroke(width = 2.5f * px))

    drawOval(
        brush = Brush.radialGradient(
            listOf(
                Color(0xFF02040A),
                Color(0xFF07111B),
                Color(0xFF19313C).copy(alpha = 0.72f),
                Color.Transparent
            ),
            center = Offset(holeLeft + holeWidth * 0.50f, holeTop + holeHeight * 0.34f),
            radius = holeWidth * 0.70f
        ),
        topLeft = Offset(holeLeft, holeTop),
        size = Size(holeWidth, holeHeight)
    )
    drawOval(
        color = Color(0xFF010307),
        topLeft = Offset(holeLeft - 6f * px, ledgeY - 22f * px),
        size = Size(holeWidth + 22f * px, h * 0.58f)
    )
    drawOval(
        brush = Brush.radialGradient(
            listOf(Color.Transparent, Color(0xFF000000).copy(alpha = 0.72f)),
            center = Offset(holeLeft + holeWidth * 0.50f, ledgeY + h * 0.16f),
            radius = holeWidth * 0.55f
        ),
        topLeft = Offset(holeLeft - 14f * px, ledgeY - 30f * px),
        size = Size(holeWidth + 28f * px, h * 0.62f)
    )
    drawOval(
        color = Color(0xFF0A1E2A).copy(alpha = 0.52f),
        topLeft = Offset(holeLeft + 38f * px, ledgeY + 18f * px),
        size = Size(holeWidth - 76f * px, h * 0.38f)
    )

    // Broken stone rim around the abyss, without hiding that it is a hole.
    repeat(9) { i ->
        val t = i / 8f
        val chipX = holeLeft + 15f * px + t * (holeWidth - 42f * px)
        val chipY = ledgeY + ((i % 3) - 1) * 5f * px
        val shard = Path().apply {
            moveTo(chipX - 10f * px, chipY)
            lineTo(chipX + 2f * px, chipY - (7f + i % 3 * 2f) * px)
            lineTo(chipX + 13f * px, chipY + 2f * px)
            lineTo(chipX + 4f * px, chipY + 9f * px)
            close()
        }
        drawPath(shard, Color(0xFF8D6E63).copy(alpha = 0.86f))
        drawPath(shard, Color(0xFFFFD7A3).copy(alpha = 0.20f), style = Stroke(width = 0.9f * px))
    }

    // Visual cue: this is the point where horizontal running ends and rope descent begins.
    drawRoundRect(
        Color(0xAA07111F),
        Offset(w * 0.50f - 96f * px, 16f * px),
        Size(192f * px, 30f * px),
        CornerRadius(999f)
    )
    drawIndieRunnerText(
        text = "Vertikala naprijed",
        x = w * 0.50f,
        y = 37f * px,
        textSize = 13f * px * 1.15f,
        textColor = android.graphics.Color.WHITE
    )

    // Rope anchor and rope hanging into the visible shaft.
    drawLine(Color(0xFFE0D6C8), Offset(ropeX, 0f), Offset(ropeX, h + 50f * px), strokeWidth = 5.0f * px, cap = StrokeCap.Round)
    repeat(3) { knot ->
        val ky = ropeTop - (22f - knot * 7f) * px
        drawLine(Color(0xFF8D6E63), Offset(ropeX - 8f * px, ky), Offset(ropeX + 8f * px, ky + 4f * px), strokeWidth = 2.0f * px, cap = StrokeCap.Round)
        drawLine(Color(0xFF8D6E63), Offset(ropeX + 8f * px, ky + 4f * px), Offset(ropeX - 6f * px, ky + 7f * px), strokeWidth = 1.6f * px, cap = StrokeCap.Round)
    }
    drawPetzlStop(ropeX, ropeTop, px)
    var ry = ropeTop + 18f * px
    while (ry < h + 45f * px) {
        drawLine(Color(0xFF8D6E63), Offset(ropeX - 7f * px, ry), Offset(ropeX + 7f * px, ry + 5f * px), strokeWidth = 1.8f * px)
        ry += 34f * px
    }

    val step = (transitionTime / 4.2f).coerceIn(0f, 1f)
    val approach = (step / 0.46f).coerceIn(0f, 1f)
    val clip = ((step - 0.36f) / 0.22f).coerceIn(0f, 1f)
    val swing = ((step - 0.50f) / 0.50f).coerceIn(0f, 1f)

    val edgeX = holeLeft - 32f * px
    val runX = w * (0.24f + 0.25f * approach)
    val runY = ledgeY - 48f * px
    val swingX = edgeX + (ropeX - edgeX) * swing + sin(swing * 3.14159f) * 34f * px
    val swingY = runY + 18f * px + swing * 88f * px
    val playerX = if (step < 0.50f) runX else swingX
    val playerY = if (step < 0.50f) runY else swingY

    // Headlamp beam leads the eye from the ledge into the abyss.
    drawLine(
        Color(0xFFFFF59D).copy(alpha = if (status == RunnerStatus.RUNNING) 0.20f else 0.10f),
        Offset(playerX + 14f * px, playerY - 15f * px),
        Offset(holeLeft + holeWidth * 0.70f, ledgeY + 88f * px),
        strokeWidth = 25f * px
    )

    // Transfer animation: run to edge, clip in, then swing onto the rope before descent starts.
    if (step < 0.56f) {
        drawSpeleologistAt(playerX, playerY, px, status)
    } else {
        val ropePath = Path().apply {
            moveTo(ropeX, ropeTop)
            quadraticBezierTo(ropeX - 42f * px, playerY - 42f * px, playerX, playerY + 18f * px)
        }
        drawPath(ropePath, Color(0xFFE0D6C8).copy(alpha = 0.72f), style = Stroke(width = 3.0f * px))
        drawRopeSpeleologist(playerX, playerY + 12f * px, px)
    }

    val clipStart = Offset(playerX + 7f * px, playerY + 18f * px)
    val clipEnd = Offset(ropeX, playerY + 3f * px)
    if (clip > 0f) {
        drawLine(Color(0xFFB0BEC5).copy(alpha = 0.35f + 0.45f * clip), clipStart, clipEnd, strokeWidth = 2.4f * px)
        drawCircle(Color(0xFFFF7043), radius = (3.5f + 1.5f * clip) * px, center = Offset(clipStart.x + (clipEnd.x - clipStart.x) * clip, clipStart.y + (clipEnd.y - clipStart.y) * clip))
    }

    val remaining = (3.0f - anchorWindowTime).coerceIn(0f, 3.0f)
    val blink = 0.50f + 0.50f * sin(anchorWindowTime * 9.5f)
    val promptColor = when {
        anchorPressed -> Color(0xFFB9F6CA)
        remaining < 1f -> Color(0xFFFF1744)
        else -> Color(0xFFFFD54F).copy(alpha = 0.42f + 0.58f * blink)
    }

    // Old top pill stays as secondary status only; the real CTA is the big center button below.
    drawRoundRect(
        color = Color(0xCC120A04),
        topLeft = Offset(w * 0.50f - 112f * px, 53f * px),
        size = Size(224f * px, 34f * px),
        cornerRadius = CornerRadius(999f)
    )
    drawRoundRect(
        color = if (remaining < 1f && !anchorPressed) Color(0xFFFF1744).copy(alpha = 0.72f) else Color(0x44FFD54F),
        topLeft = Offset(w * 0.50f - 112f * px, 53f * px),
        size = Size(224f * px, 34f * px),
        cornerRadius = CornerRadius(999f),
        style = Stroke(width = 1.4f * px)
    )
    drawIndieRunnerText(
        text = when {
            anchorFailing -> "Promašen anchor!"
            anchorPressed -> "Ukopčan — spuštanje"
            else -> "ANCHOR ${remaining.toInt() + 1}s"
        },
        x = w * 0.50f,
        y = 76f * px,
        textSize = 13f * px * 1.12f,
        textColor = promptColor.toArgb()
    )

    if (!anchorPressed && !anchorFailing) {
        val ctaVisible = (transitionTime * 2.5f).toInt() % 2 == 0
        drawCircle(promptColor.copy(alpha = 0.28f * blink), radius = (22f + 6f * blink) * px, center = Offset(ropeX, ropeTop))
        drawPetzlStop(ropeX, ropeTop, px, tint = promptColor, scale = 1.0f)

        val countdownNumber = remaining.toInt().coerceIn(0, 2) + 1
        val countdownColor = when (countdownNumber) {
            3 -> Color(0xFFFFD54F)
            2 -> Color(0xFFFF9800)
            else -> Color(0xFFFF1744)
        }
        val countdownText = countdownNumber.toString()
        val countdownX = w * 0.50f
        val countdownY = h * 0.40f
        listOf(
            Offset(-2f * px, 0f), Offset(2f * px, 0f), Offset(0f, -2f * px), Offset(0f, 2f * px),
            Offset(-1.6f * px, -1.6f * px), Offset(1.6f * px, -1.6f * px), Offset(-1.6f * px, 1.6f * px), Offset(1.6f * px, 1.6f * px)
        ).forEach { off ->
            drawIndieRunnerText(
                text = countdownText,
                x = countdownX + off.x,
                y = countdownY + off.y,
                textSize = 64f * px,
                textColor = android.graphics.Color.BLACK
            )
        }
        drawIndieRunnerText(
            text = countdownText,
            x = countdownX,
            y = countdownY,
            textSize = 64f * px,
            textColor = countdownColor.toArgb()
        )

        val center = Offset(w * 0.50f, h * 0.62f)
        val btnW = 154f * px
        val btnH = 52f * px
        val btnX = center.x - btnW / 2f
        val btnY = center.y - btnH / 2f
        val btnFill = if (ctaVisible) Color(0xFFD32F2F) else Color(0xFF7F0000)
        drawRect(
            color = btnFill,
            topLeft = Offset(btnX, btnY),
            size = Size(btnW, btnH)
        )
        drawRect(
            color = Color.Black,
            topLeft = Offset(btnX, btnY),
            size = Size(btnW, btnH),
            style = Stroke(width = 3f * px)
        )
        drawLine(
            color = Color(0xFFEF9A9A),
            start = Offset(btnX + 4f * px, btnY + 5f * px),
            end = Offset(btnX + btnW - 4f * px, btnY + 5f * px),
            strokeWidth = 2f * px
        )
        drawIndieRunnerText(
            text = "⚓ ANCHOR",
            x = center.x,
            y = center.y + 7f * px,
            textSize = 18f * px,
            textColor = android.graphics.Color.WHITE
        )
    }

    if (anchorPressed) {
        val successFade = (1f - ((transitionTime - 0.1f) / 0.8f).coerceIn(0f, 1f))
        if (successFade > 0.01f) {
            drawRect(color = Color(0xFF00E676).copy(alpha = 0.15f * successFade), size = Size(w, h))
        }
        drawRoundRect(
            color = Color(0xFF1B5E20).copy(alpha = 0.85f),
            topLeft = Offset(w * 0.50f - 90f * px, h * 0.60f),
            size = Size(180f * px, 36f * px),
            cornerRadius = CornerRadius(18f * px)
        )
        drawIndieRunnerText(
            text = "✓ Ukopčan — spuštanje",
            x = w * 0.50f,
            y = h * 0.60f + 24f * px,
            textSize = 13f * px,
            textColor = android.graphics.Color.rgb(185, 246, 202)
        )
    }

    if (anchorFailing) {
        val fall = anchorFallTime.coerceIn(0f, 1.15f) / 1.15f
        val fallX = edgeX + (ropeX - edgeX) * 0.35f + sin(fall * 18f) * 10f * px
        val fallY = ledgeY - 36f * px + fall * h * 0.72f
        drawLine(Color(0xFFFFF59D).copy(alpha = 0.18f), Offset(fallX + 10f * px, fallY - 12f * px), Offset(fallX + 62f * px, fallY + 18f * px), strokeWidth = 20f * px)
        drawRopeSpeleologist(fallX, fallY, px)
        drawOval(Color.Black.copy(alpha = 0.28f + 0.28f * fall), Offset(holeLeft - 20f * px, ledgeY - 18f * px), Size(holeWidth + 40f * px, h * 0.70f))
    }

}


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStableVerticalBackground(
    w: Float,
    h: Float,
    segmentDepth: Float,
    px: Float,
    alpha: Float = 0.88f,
    biomeIndex: Int = 0,
    difficultyLevel: Int = 0
) {
    val palette = runnerSierraPalette(biomeIndex)
    drawRect(palette.darkest.copy(alpha = alpha), topLeft = Offset(0f, 0f), size = Size(w * 0.14f, h))
    drawLine(palette.midLight.copy(alpha = 0.65f * alpha), Offset(w * 0.14f, 0f), Offset(w * 0.14f, h), strokeWidth = 2.5f * px)
    drawRect(palette.darkest.copy(alpha = alpha), topLeft = Offset(w * 0.86f, 0f), size = Size(w * 0.14f, h))
    drawLine(palette.midLight.copy(alpha = 0.65f * alpha), Offset(w * 0.86f, 0f), Offset(w * 0.86f, h), strokeWidth = 2.5f * px)
    drawRect(palette.dark.copy(alpha = alpha), topLeft = Offset(w * 0.14f, 0f), size = Size(w * 0.72f, h))

    val leftScroll = (segmentDepth * 14f * px) % (120f * px)
    val rightScroll = (segmentDepth * 18f * px) % (120f * px)
    repeat(9) { i ->
        val yL = i * h / 7f - leftScroll
        val yR = i * h / 7f - rightScroll
        val left = Path().apply {
            moveTo(0f, yL - 95f * px)
            cubicTo(w * 0.07f, yL - 42f * px, w * 0.15f, yL + 38f * px, w * 0.07f, yL + 118f * px)
            lineTo(0f, yL + 150f * px)
            close()
        }
        val right = Path().apply {
            moveTo(w, yR - 105f * px)
            cubicTo(w * 0.91f, yR - 42f * px, w * 0.85f, yR + 34f * px, w * 0.94f, yR + 124f * px)
            lineTo(w, yR + 155f * px)
            close()
        }
        drawInkPath(left, palette.mid.copy(alpha = 0.48f), px, highlight = i % 2 == 0)
        drawInkPath(right, palette.floorEdge.copy(alpha = 0.34f), px, highlight = i % 2 == 1)
    }

    when (biomeIndex % 5) {
        4 -> repeat(7) { i ->
            val y = ((i * 63f * px + segmentDepth * 12f * px) % h)
            val cx = if (i % 2 == 0) w * 0.12f else w * 0.88f
            val r = (10f + i % 3 * 4f) * px
            val facet = Path().apply { moveTo(cx, y - r); lineTo(cx + r * 0.45f, y); lineTo(cx, y + r); lineTo(cx - r * 0.45f, y); close() }
            drawSierraShape(facet, palette.dark, palette.mid, palette.highlight, px, alpha = 0.42f)
        }
    }
}



private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAiCaveVerticalBackground(
    verticalBackground: ImageBitmap,
    w: Float,
    h: Float,
    segmentDepth: Float,
    px: Float,
    alpha: Float = 0.88f
) {
    val sourceW = verticalBackground.width.coerceAtLeast(1)
    val sourceH = verticalBackground.height.coerceAtLeast(1)
    val scaleW = w / sourceW.toFloat()
    val scaleH = h / sourceH.toFloat()
    val scale = max(scaleW, scaleH)
    val drawW = sourceW * scale
    val drawH = sourceH * scale
    val cropX = (w - drawW) / 2f
    val scrollRange = max(1f, drawH - h)
    val scrollY = -((segmentDepth * 5.5f * px) % scrollRange)

    drawImage(
        image = verticalBackground,
        srcOffset = IntOffset(0, 0),
        srcSize = IntSize(sourceW, sourceH),
        dstOffset = IntOffset(cropX.toInt(), scrollY.toInt()),
        dstSize = IntSize(drawW.toInt() + 1, drawH.toInt() + 1),
        alpha = alpha
    )

    // Vertical readability layer: keeps left/right ropes, the rope end and the back-facing player visible.
    drawRect(Color(0xFF020713).copy(alpha = 0.30f), size = Size(w, h))
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                Color(0xFF000000).copy(alpha = 0.38f),
                Color.Transparent,
                Color.Transparent,
                Color(0xFF000000).copy(alpha = 0.38f)
            )
        ),
        size = Size(w, h)
    )
    drawRect(
        brush = Brush.radialGradient(
            listOf(Color(0xFFB3E5FC).copy(alpha = 0.10f), Color.Transparent),
            center = Offset(w * 0.50f, h * 0.45f),
            radius = w * 0.62f
        ),
        size = Size(w, h)
    )
}


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAiCaveHorizontalBackground(
    caveBackground: ImageBitmap,
    w: Float,
    h: Float,
    groundY: Float,
    walkMeters: Float,
    px: Float,
    difficultyLevel: Int = 0
) {
    val sourceW = caveBackground.width.coerceAtLeast(1)
    val sourceH = caveBackground.height.coerceAtLeast(1)
    val scale = h / sourceH.toFloat()
    val drawW = max(w, sourceW * scale)
    val drawH = h
    val speedFeedback = 1f + difficultyLevel * 0.09f
    val parallax = (walkMeters * 2.2f * speedFeedback) % drawW
    var x = -parallax
    while (x < w) {
        drawImage(
            image = caveBackground,
            srcOffset = IntOffset(0, 0),
            srcSize = IntSize(sourceW, sourceH),
            dstOffset = IntOffset(x.toInt(), 0),
            dstSize = IntSize(drawW.toInt() + 1, drawH.toInt() + 1),
            alpha = 0.98f
        )
        x += drawW
    }

    // Gameplay-safe readability layer: keeps the player/obstacles readable over the art asset.
    drawRect(Color(0xFF06101A).copy(alpha = 0.14f), size = Size(w, h))
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF000000).copy(alpha = 0.18f),
                Color.Transparent,
                Color.Transparent,
                Color(0xFF000000).copy(alpha = 0.36f)
            ),
            startY = 0f,
            endY = h
        ),
        size = Size(w, h)
    )

    // Clean horizontal gameplay lane. It prevents the background floor details from competing with pits/stalagmites.
    drawRoundRect(
        color = Color(0xFF2D211B).copy(alpha = 0.72f),
        topLeft = Offset(0f, groundY - 4f * px),
        size = Size(w, h - groundY + 8f * px),
        cornerRadius = CornerRadius(0f)
    )
    drawLine(
        color = Color(0xFFC18B61).copy(alpha = 0.54f),
        start = Offset(0f, groundY - 5f * px),
        end = Offset(w, groundY - 5f * px),
        strokeWidth = 2.4f * px
    )
}


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawForestCaveStartIntro(
    w: Float,
    h: Float,
    groundY: Float,
    px: Float,
    phaseTime: Float,
    status: RunnerStatus
) {
    val intro = (phaseTime / 4.9f).coerceIn(0f, 1f)
    val fadeOut = (1f - ((phaseTime - 4.15f) / 0.95f).coerceIn(0f, 1f)).coerceIn(0f, 1f)
    if (fadeOut <= 0.01f) return

    // Sierra VGA sky: painted flat bands instead of a smooth gradient.
    val skyDark = blendColor(Color(0xFF071323), Color(0xFF142840), intro)
    val skyMid = blendColor(Color(0xFF0A1A2C), Color(0xFF1A3050), intro)
    val horizon = blendColor(Color(0xFF102338), Color(0xFF2D405E), intro)
    drawRect(skyDark.copy(alpha = fadeOut), size = Size(w, h * 0.42f))
    drawRect(skyMid.copy(alpha = fadeOut), topLeft = Offset(0f, h * 0.42f), size = Size(w, h * 0.25f))
    drawRect(horizon.copy(alpha = fadeOut), topLeft = Offset(0f, h * 0.67f), size = Size(w, h * 0.33f))

    // Parallax 1: distant hills and sky. Slowest layer.
    val backShift = intro * 20f * px
    val moonAlpha = (1f - intro * 0.72f).coerceIn(0f, 1f) * fadeOut
    drawCircle(Color(0xFFFFF8D6).copy(alpha = 0.86f * moonAlpha), radius = 17f * px, center = Offset(w * 0.20f - backShift, h * 0.14f))
    repeat(14) { i ->
        val sx = ((i * 61f * px + 37f * px - backShift * 0.35f) % (w + 40f * px)) - 20f * px
        val sy = h * (0.07f + (i % 5) * 0.035f)
        drawCircle(Color.White.copy(alpha = (0.55f - intro * 0.42f).coerceAtLeast(0.05f) * fadeOut), radius = (0.8f + (i % 3) * 0.25f) * px, center = Offset(sx, sy))
    }
    val hills = Path().apply {
        moveTo(-30f * px - backShift, groundY - 180f * px)
        cubicTo(w * 0.20f - backShift, groundY - 250f * px, w * 0.42f - backShift, groundY - 135f * px, w * 0.68f - backShift, groundY - 205f * px)
        cubicTo(w * 0.86f - backShift, groundY - 245f * px, w + 70f * px - backShift, groundY - 150f * px, w + 120f * px - backShift, groundY - 190f * px)
        lineTo(w + 120f * px, h); lineTo(-40f * px, h); close()
    }
    drawPath(hills, Color(0xFF0E1A23).copy(alpha = 0.72f * fadeOut))

    // Parallax 2: distant forest with curved branches.
    val midShift = intro * 50f * px
    repeat(12) { i ->
        val x = ((i * 54f * px - midShift) % (w + 90f * px)) - 30f * px
        val trunkH = (78f + (i % 4) * 15f) * px
        val top = groundY - trunkH - 18f * px
        val trunk = Path().apply {
            moveTo(x - 4f * px, groundY)
            cubicTo(x - 7f * px, groundY - trunkH * 0.35f, x - 3f * px, top + trunkH * 0.15f, x + 1f * px, top)
            cubicTo(x + 6f * px, top + trunkH * 0.20f, x + 7f * px, groundY - trunkH * 0.33f, x + 5f * px, groundY)
            close()
        }
        drawPath(trunk, Color(0xFF10130E).copy(alpha = 0.82f * fadeOut))
        repeat(3) { b ->
            val by = top + (18f + b * 18f) * px
            val sign = if ((i + b) % 2 == 0) 1f else -1f
            val branch = Path().apply {
                moveTo(x, by)
                cubicTo(x + sign * 11f * px, by - 8f * px, x + sign * 26f * px, by - 14f * px, x + sign * 39f * px, by - 25f * px)
            }
            drawPath(branch, Color(0xFF16180F).copy(alpha = 0.78f * fadeOut), style = Stroke(width = 2f * px))
        }
    }

    // Cave entrance: destination point with moss, wet shine, drips and lamp response.
    val caveX = w * 0.72f
    val caveW = w * 0.42f
    val caveH = h * 0.54f
    val caveLeft = caveX - caveW * 0.48f
    val cavePath = Path().apply {
        moveTo(caveLeft, groundY)
        cubicTo(caveLeft + caveW * 0.04f, groundY - caveH * 0.62f, caveX - caveW * 0.18f, groundY - caveH, caveX, groundY - caveH * 0.98f)
        cubicTo(caveX + caveW * 0.26f, groundY - caveH * 0.92f, caveLeft + caveW * 0.96f, groundY - caveH * 0.52f, caveLeft + caveW, groundY)
        close()
    }
    drawPath(cavePath, Color(0xFF050607).copy(alpha = fadeOut))
    drawPath(cavePath, Color(0xFF244150).copy(alpha = 0.22f * fadeOut), style = Stroke(width = 7f * px))
    drawPath(cavePath, Color(0xFF8D6E43).copy(alpha = 0.75f * fadeOut), style = Stroke(width = 3f * px))
    drawPath(cavePath, Color(0xFF386B35).copy(alpha = 0.45f * fadeOut), style = Stroke(width = 1.8f * px))
    repeat(8) { d ->
        val dx = caveLeft + caveW * (0.18f + d * 0.08f)
        val dy = groundY - caveH * (0.72f - (d % 3) * 0.08f)
        drawLine(Color(0xFFB2EBF2).copy(alpha = 0.28f * fadeOut), Offset(dx, dy), Offset(dx + sin(phaseTime * 2f + d) * px, dy + (7f + d % 3 * 4f) * px), strokeWidth = 0.75f * px)
    }
    drawOval(Color(0xFF0B2330).copy(alpha = 0.38f * fadeOut), Offset(caveLeft + caveW * 0.20f, groundY - caveH * 0.72f), Size(caveW * 0.60f, caveH * 0.64f))

    // Parallax 3: foreground forest, roots, shrubs and grass. Fastest layer.
    val frontShift = intro * 100f * px
    drawRect(Color(0xFF1A110A).copy(alpha = fadeOut), topLeft = Offset(0f, groundY), size = Size(w, h - groundY))
    repeat(15) { i ->
        val x = ((i * 46f * px - frontShift) % (w + 120f * px)) - 50f * px
        val y = groundY + (8f + (i % 4) * 7f) * px
        drawOval(Color(0xFF20351B).copy(alpha = 0.70f * fadeOut), Offset(x, y), Size((22f + i % 4 * 7f) * px, (11f + i % 3 * 4f) * px))
        drawLine(Color(0xFF3A2513).copy(alpha = 0.72f * fadeOut), Offset(x + 8f * px, groundY + 4f * px), Offset(x + 36f * px, groundY + 16f * px), strokeWidth = 2.2f * px)
    }
    repeat(44) { i ->
        val x = ((i * 17f * px - frontShift * 1.2f) % (w + 40f * px)) - 10f * px
        val len = (5f + (i % 5) * 2f) * px
        drawLine(Color(0xFF314321).copy(alpha = 0.68f * fadeOut), Offset(x, groundY + 7f * px), Offset(x + sin(i.toFloat()) * 1.8f * px, groundY + 7f * px - len), strokeWidth = 0.9f * px)
    }

    // Campfire with a richer glow, left behind as the runner enters.
    val fireX = w * 0.28f - frontShift * 0.22f
    val fireY = groundY - 7f * px
    drawCircle(Color(0xFFFF6D00).copy(alpha = 0.22f * fadeOut), radius = 54f * px, center = Offset(fireX, fireY))
    repeat(4) { f ->
        val flame = Path().apply {
            moveTo(fireX, fireY - (30f + f * 2f) * px)
            cubicTo(fireX - (8f + f * 2f) * px, fireY - 12f * px, fireX - 5f * px, fireY - 4f * px, fireX, fireY)
            cubicTo(fireX + (8f + f * 2f) * px, fireY - 11f * px, fireX + 4f * px, fireY - 20f * px, fireX, fireY - (30f + f * 2f) * px)
            close()
        }
        drawPath(flame, (if (f % 2 == 0) Color(0xFFFFB300) else Color(0xFFFF3D00)).copy(alpha = (0.75f - f * 0.11f) * fadeOut))
    }
    drawLine(Color(0xFF5D4037).copy(alpha = fadeOut), Offset(fireX - 20f * px, fireY + 3f * px), Offset(fireX + 19f * px, fireY + 10f * px), strokeWidth = 5f * px)
    drawLine(Color(0xFF4E342E).copy(alpha = fadeOut), Offset(fireX + 18f * px, fireY + 3f * px), Offset(fireX - 18f * px, fireY + 10f * px), strokeWidth = 5f * px)

    // Camp group: two teammates stay by the fire so the moving red speleologist reads as "our" player.
    fun drawCampMate(cx: Float, cy: Float, suit: Color, facingRight: Boolean) {
        val dir = if (facingRight) 1f else -1f
        drawSierraCircle(Offset(cx, cy - 28f * px), 8.5f * px, Color(0xFFD49A28), Color(0xFFFFD54F), Color(0xFFFFF8E1), px, fadeOut)
        drawSierraRoundRect(Offset(cx - 10f * px, cy - 20f * px), Size(20f * px, 20f * px), 7f * px, blendColor(suit, Color.Black, 0.35f), suit, blendColor(suit, Color.White, 0.34f), px, fadeOut)
        drawRoundRect(Color(0xFF4E342E).copy(alpha = fadeOut), Offset(cx - dir * 13f * px, cy - 8f * px), Size(15f * px, 9f * px), CornerRadius(5f * px))
        drawLine(Color(0xFF263238).copy(alpha = 0.90f * fadeOut), Offset(cx - 5f * px, cy - 1f * px), Offset(cx - 19f * px, cy + 9f * px), strokeWidth = 3.5f * px)
        drawLine(Color(0xFF263238).copy(alpha = 0.90f * fadeOut), Offset(cx + 5f * px, cy - 1f * px), Offset(cx + 19f * px, cy + 8f * px), strokeWidth = 3.5f * px)
        drawCircle(Color.White.copy(alpha = 0.80f * fadeOut), radius = 2.2f * px, center = Offset(cx + dir * 7f * px, cy - 27f * px))
    }
    drawCampMate(fireX - 58f * px, fireY + 6f * px, Color(0xFF3949AB), true)
    drawCampMate(fireX + 56f * px, fireY + 9f * px, Color(0xFF2E7D32), false)
    drawOval(Color.Black.copy(alpha = 0.22f * fadeOut), Offset(fireX - 78f * px, fireY + 14f * px), Size(48f * px, 9f * px))
    drawOval(Color.Black.copy(alpha = 0.22f * fadeOut), Offset(fireX + 32f * px, fireY + 16f * px), Size(48f * px, 9f * px))

    // Our runner is the only moving speleologist: red suit, backpack, helmet lamp aimed at the cave.
    val playerX = w * (0.11f + 0.56f * intro)
    val playerY = groundY - 6f * px
    val lean = 7f * px * intro
    val lampX = playerX + 19f * px
    val lampY = playerY - 55f * px
    val lampAlpha = ((intro - 0.52f) / 0.35f).coerceIn(0f, 1f) * fadeOut
    val lampBeam = Path().apply {
        moveTo(lampX, lampY)
        lineTo(caveLeft + caveW * 0.38f, groundY - caveH * 0.70f)
        lineTo(caveLeft + caveW * 0.77f, groundY - caveH * 0.40f)
        close()
    }
    drawPath(lampBeam, Color(0xFFFFF59D).copy(alpha = 0.20f * lampAlpha))
    drawCircle(Color(0xFFFFF9C4).copy(alpha = 0.24f * lampAlpha), radius = 75f * px, center = Offset(caveX, groundY - caveH * 0.52f))
    drawLine(Color(0xFFFFF59D).copy(alpha = 0.82f * fadeOut), Offset(lampX - 3f * px, lampY), Offset(lampX + 4f * px, lampY), strokeWidth = 2f * px)
    drawSierraCircle(Offset(playerX + lean * 0.25f, playerY - 42f * px), 13f * px, Color(0xFFD49A28), Color(0xFFFFC107), Color(0xFFFFF8E1), px, fadeOut)
    drawSierraRoundRect(Offset(playerX + lean * 0.25f - 14f * px, playerY - 53f * px), Size(28f * px, 7f * px), 6f * px, Color(0xFFE6A800), Color(0xFFFFD54F), Color(0xFFFFF8E1), px, fadeOut)
    drawSierraRoundRect(Offset(playerX - 14f * px, playerY - 30f * px), Size(13f * px, 27f * px), 6f * px, Color(0xFF2B1608), Color(0xFF4E342E), Color(0xFFBCAAA4), px, fadeOut)
    drawSierraLimb(Offset(playerX, playerY - 30f * px), Offset(playerX + lean * 0.52f, playerY - 18f * px), Offset(playerX + lean, playerY - 8f * px), 6.4f * px, Color(0xFF7F0000), Color(0xFFE53935), Color(0xFFFF8A80), px, fadeOut)
    drawLine(Color(0xFF263238).copy(alpha = fadeOut), Offset(playerX + lean, playerY - 8f * px), Offset(playerX - 8f * px, playerY + 5f * px), strokeWidth = 4f * px)
    drawLine(Color(0xFF263238).copy(alpha = fadeOut), Offset(playerX + lean, playerY - 8f * px), Offset(playerX + 17f * px, playerY + 5f * px), strokeWidth = 4f * px)
    drawLine(Color(0xFFFFCC80).copy(alpha = fadeOut), Offset(playerX + 2f * px, playerY - 25f * px), Offset(playerX + 19f * px, playerY - 31f * px), strokeWidth = 3f * px)

    // Low fog ties the forest and cave together.
    drawRect(
        brush = Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.11f * fadeOut)), startY = groundY - 75f * px, endY = groundY + 30f * px),
        topLeft = Offset(0f, groundY - 80f * px),
        size = Size(w, 115f * px)
    )

    val titleAlpha = ((phaseTime - 0.45f) / 0.75f).coerceIn(0f, 1f) * fadeOut
    val subAlpha = ((phaseTime - 1.5f) / 0.75f).coerceIn(0f, 1f) * fadeOut
    val floatY = sin(phaseTime * 2.4f) * 3.5f * px
    drawIndieRunnerText("Ulaz iz šume", w * 0.50f, h * 0.18f + floatY, 23f * px, android.graphics.Color.argb((245 * titleAlpha).toInt(), 255, 239, 196))
    drawIndieRunnerText("▸ Uđi u špilju...", w * 0.50f, h * 0.23f + floatY, 14f * px, android.graphics.Color.argb((230 * subAlpha).toInt(), 255, 224, 160))
}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHorizontalVerticalApproachCue(
    w: Float,
    h: Float,
    groundY: Float,
    px: Float,
    phaseTime: Float,
    horizontalPhaseDuration: Float = 30f
) {
    val cueStart = (horizontalPhaseDuration - 8f).coerceAtLeast(horizontalPhaseDuration * 0.85f)
    val t = ((phaseTime - cueStart) / 8f).coerceIn(0f, 1f)
    val alpha = (sin(t * 3.14159f) * 0.90f).coerceIn(0f, 0.90f)
    if (alpha <= 0.02f) return
    val holeX = w * (0.82f - 0.18f * t)
    val holeY = groundY - 9f * px
    drawOval(Color(0xFF020304).copy(alpha = alpha), Offset(holeX - 52f * px, holeY - 9f * px), Size(112f * px, 64f * px))
    drawOval(Color(0xFF1A2D36).copy(alpha = 0.42f * alpha), Offset(holeX - 44f * px, holeY - 4f * px), Size(92f * px, 44f * px))
    drawCircle(Color(0xFFFFD54F).copy(alpha = 0.88f * alpha), radius = 8f * px, center = Offset(holeX + 35f * px, groundY - 122f * px))
    drawLine(Color(0xFFE0D6C8).copy(alpha = alpha), Offset(holeX + 35f * px, groundY - 122f * px), Offset(holeX + 35f * px, groundY + 36f * px), strokeWidth = 3.2f * px)
    drawRoundRect(Color(0xAA07111F).copy(alpha = alpha), Offset(w * 0.5f - 118f * px, h * 0.19f), Size(236f * px, 30f * px), CornerRadius(999f))
    drawIndieRunnerText(
        text = "Ukopčaj se",
        x = w * 0.5f,
        y = h * 0.19f + 21f * px,
        textSize = 12.5f * px * 1.15f,
        textColor = android.graphics.Color.rgb(255, 224, 130)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHorizontalCave(
    w: Float,
    h: Float,
    groundY: Float,
    drift: Float,
    px: Float,
    obstacles: List<RunnerObstacle>,
    tokens: List<RunnerToken>,
    score: Int,
    depthMeters: Float,
    difficultyLevel: Int,
    playerLift: Float,
    isCrawling: Boolean,
    status: RunnerStatus,
    phaseTime: Float,
    slowdownTimer: Float,
    speedBoostTimer: Float,
    fallingObstacle: RunnerObstacle?,
    fallAnimationTime: Float,
    collectFlashes: List<RunnerCollectFlash>,
    biomeIndex: Int = 0,
    caveFloorOffset: Float = 0f,
    caveCeilingOffset: Float = 0f,
    horizontalPhaseDuration: Float = 30f,
    lakeDuration: Float = 10f,
    lakeBoatLift: Float = 0f,
    lakePaddleTimer: Float = 0f,
    jumpVelocity: Float = 0f,
    gravity: Float = 1f,
    phase: RunnerPhase = RunnerPhase.HORIZONTAL
) {
    drawStableHorizontalBackground(
        w = w,
        h = h,
        groundY = groundY,
        walkMeters = score.toFloat(),
        px = px,
        difficultyLevel = difficultyLevel,
        biomeIndex = biomeIndex
    )
    drawBiomeAtmosphere(biomeIndex, w, h, px, score.toFloat(), phaseTime)

    if (score < 14 && phaseTime < 6.2f && biomeIndex % 5 == 0) {
        // Continuous opening run: the player starts outside, passes the campfire and enters the cave.
        // Return here so the intro is not just an overlay on top of the cave runner.
        drawForestCaveStartIntro(w, h, groundY, px, phaseTime, status)
        return
    }

    val baseCeilingH = 168f * px - caveCeilingOffset
    val tunnelCeilingY = (groundY - baseCeilingH + sin(drift * 5.7f) * 6f * px)
        .coerceIn(h * 0.28f, groundY - 110f * px)
    val ceilingSlopeLeft = tunnelCeilingY + sin(drift * 3.1f) * 8f * px
    val ceilingSlopeRight = tunnelCeilingY - sin(drift * 3.1f + 0.8f) * 8f * px
    val ceilingMass = Path().apply {
        moveTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(w, ceilingSlopeRight + 20f * px)
        cubicTo(
            w * 0.75f, ceilingSlopeRight + sin(drift * 4.2f) * 6f * px,
            w * 0.50f, tunnelCeilingY + sin(drift * 6.1f) * 6f * px,
            w * 0.25f, ceilingSlopeLeft + sin(drift * 3.8f) * 5f * px
        )
        lineTo(0f, ceilingSlopeLeft + 14f * px)
        close()
    }
    drawInkPath(ceilingMass, runnerBiomeBack(biomeIndex).copy(alpha = 0.94f), px, highlight = true)
    drawPath(
        ceilingMass,
        runnerBiomeMid(biomeIndex).copy(alpha = 0.24f),
        style = Stroke(width = 2.2f * px)
    )
    // VGA ceiling highlight stripe — svijetla linija na rubu stropa = dubina i solidnost
    drawLine(
        color = runnerSierraPalette(biomeIndex).ceilingEdge.copy(alpha = 0.62f),
        start = Offset(0f, tunnelCeilingY + 6f * px),
        end = Offset(w, tunnelCeilingY + 6f * px),
        strokeWidth = 2.5f * px
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.90f),
        start = Offset(0f, tunnelCeilingY + 9f * px),
        end = Offset(w, tunnelCeilingY + 9f * px),
        strokeWidth = 1.8f * px
    )

    // Cave breath: small cold puffs from ceiling cracks every few seconds.
    val breathTrigger = (phaseTime * 0.25f) % 1f
    if (breathTrigger < 0.26f) {
        val t = (phaseTime % 4f) / 4f
        val breathX = ((biomeIndex + 1) * 73f * px + w * 0.18f + sin(phaseTime * 0.7f + biomeIndex) * w * 0.20f).coerceIn(28f * px, w - 28f * px)
        val breathY = tunnelCeilingY + 12f * px
        repeat(3) { puff ->
            drawCircle(
                color = Color(0xFFECEFF1).copy(alpha = (1f - t).coerceIn(0f, 1f) * (0.28f - puff * 0.055f)),
                radius = (2f + t * 12f + puff * 3.2f) * px,
                center = Offset(breathX + (puff - 1) * 7f * px, breathY + puff * 2.4f * px)
            )
        }
    }

    repeat(14) { i ->
        val x = ((i * 91f - drift * 95f) % (w + 150f)) - 55f
        val anchorY = tunnelCeilingY + ((i % 4) * 5f - 8f) * px
        val len = (20f + (i % 6) * 9f) * px
        val half = (5f + (i % 4) * 2.0f) * px
        val p = Path().apply {
            moveTo(x - half, anchorY)
            quadraticBezierTo(x - half * 0.28f, anchorY + len * 0.44f, x + sin(i.toFloat()) * 1.4f * px, anchorY + len)
            quadraticBezierTo(x + half * 0.24f, anchorY + len * 0.46f, x + half, anchorY)
            close()
        }
        drawInkPath(
            p,
            runnerSierraPalette(biomeIndex).dark.copy(alpha = 0.41f),
            px,
            highlight = i % 2 == 0
        )
        // Sierra highlight drip — bijela linija s lijeve strane svakog stalaktita
        if (i % 3 != 2) {
            drawLine(
                runnerSierraPalette(biomeIndex).highlight.copy(alpha = 0.38f),
                Offset(x - half * 0.55f, anchorY + 2f * px),
                Offset(x - half * 0.35f, anchorY + len * 0.72f),
                strokeWidth = 1.2f * px,
                cap = StrokeCap.Round
            )
        }
    }

    if (biomeIndex % 5 == 1) {
        val waterfallX = w * 0.84f
        val waterfallTop = tunnelCeilingY + 8f * px
        val waterfallH = (groundY - waterfallTop - 36f * px).coerceAtLeast(58f * px)
        drawRoundRect(Color(0xFF8BD3F7).copy(alpha = 0.22f), Offset(waterfallX, waterfallTop), Size(20f * px, waterfallH), CornerRadius(14f * px))
        repeat(4) { i ->
            val lineX = waterfallX + (4f + i * 4.4f) * px
            drawLine(Color(0xFFCFF4FF).copy(alpha = 0.42f), Offset(lineX, waterfallTop + 3f * px), Offset(lineX, waterfallTop + waterfallH), strokeWidth = 1.3f * px)
        }
    }

    val floor = Path().apply {
        moveTo(0f, groundY)
        lineTo(w, groundY)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawInkPath(floor, runnerSierraPalette(biomeIndex).dark, px)
    // VGA floor edge highlight — tanki svijetli rub na vrhu poda
    drawLine(
        color = runnerSierraPalette(biomeIndex).floorEdge.copy(alpha = 0.45f),
        start = Offset(0f, groundY - 2f * px),
        end = Offset(w * 0.4f, groundY - 2f * px),
        strokeWidth = 2.0f * px
    )

    repeat(8) { i ->
        val x = ((i * 145f - drift * 105f) % (w + 200f)) - 50f
        val base = groundY - (3f + (i % 2) * 8f) * px
        val height = (20f + (i % 3) * 9f) * px
        val half = (6f + (i % 3) * 2f) * px
        val p = Path().apply {
            moveTo(x - half, base)
            quadraticBezierTo(x - half * 0.15f, base - height * 0.55f, x, base - height)
            quadraticBezierTo(x + half * 0.20f, base - height * 0.55f, x + half, base)
            close()
        }
        drawInkPath(p, runnerBiomeMid(biomeIndex).copy(alpha = 0.13f), px, highlight = i % 2 == 0)
        // Sierra top highlight — kratka svijetla cap linija na vrhu stalagmita
        drawLine(
            runnerSierraPalette(biomeIndex).highlight.copy(alpha = 0.42f),
            Offset(x - half * 0.30f, base - height + px),
            Offset(x + half * 0.15f, base - height + 4f * px),
            strokeWidth = 1.4f * px,
            cap = StrokeCap.Round
        )
    }

    // Soft decorative bats in the background. Kept small/dim so they do not read as obstacles.
    repeat(5) { i ->
        val batX = ((i * 164f - drift * 130f) % (w + 140f)) - 40f
        val batY = h * (0.22f + (i % 3) * 0.075f)
        val wing = (8f + (i % 2) * 3f) * px
        val body = 2.6f * px
        val bat = Path().apply {
            moveTo(batX, batY)
            quadraticBezierTo(batX - wing * 0.62f, batY - wing * 0.48f, batX - wing, batY)
            quadraticBezierTo(batX - wing * 0.46f, batY - wing * 0.05f, batX - body, batY + body)
            quadraticBezierTo(batX, batY + body * 1.8f, batX + body, batY + body)
            quadraticBezierTo(batX + wing * 0.46f, batY - wing * 0.05f, batX + wing, batY)
            quadraticBezierTo(batX + wing * 0.62f, batY - wing * 0.48f, batX, batY)
            close()
        }
        drawPath(bat, Color(0xFF0B1018).copy(alpha = 0.14f))
        drawPath(bat, Color(0xFFB0BEC5).copy(alpha = 0.03f), style = Stroke(width = 0.8f * px))
    }

    // Slow dust motes for cave depth. They are decorative only and do not interact with obstacles.
    repeat(8) { i ->
        val moteX = ((i * 113f * px - drift * 28f * px) % (w + 80f * px)) - 20f * px
        val moteY = h * (0.32f + (i % 5) * 0.08f)
        drawCircle(
            color = Color.White.copy(alpha = 0.10f + (i % 3) * 0.04f),
            radius = (0.8f + i % 2 * 0.5f) * px,
            center = Offset(moteX, moteY)
        )
    }

    if (tokens.isNotEmpty() && gravity > 0f && jumpVelocity > 0f) {
        val maxJumpLift = (jumpVelocity * jumpVelocity) / (2f * gravity)
        val reachLineY = groundY - maxJumpLift + playerLift
        repeat(8) { seg ->
            val segX = w * (0.05f + seg * 0.12f)
            drawLine(
                Color(0xFFFFC107).copy(alpha = 0.18f),
                Offset(segX, reachLineY),
                Offset(segX + w * 0.07f, reachLineY),
                strokeWidth = 1.0f * px
            )
        }
    }

    tokens.forEach { token ->
        val tokenY = h * token.yFactor
        val bob = if ((token.id + score) % 2 == 0) -2f * px else 2f * px
        drawGoldenBat(token.x, tokenY + bob, px)
    }

    collectFlashes.forEach { flash ->
        val age = (phaseTime - flash.timestamp).coerceAtLeast(0f)
        if (age < 0.55f) {
            val t = (age / 0.55f).coerceIn(0f, 1f)
            val alpha = (1f - t * t).coerceIn(0f, 1f)
            // Burst circle — raste i blijedi
            drawCircle(
                Color(0xFFFFE57F).copy(alpha = alpha * 0.55f),
                radius = (8f + t * 48f) * px,
                center = Offset(flash.x, flash.y)
            )
            drawCircle(
                Color.White.copy(alpha = alpha * 0.35f),
                radius = (4f + t * 28f) * px,
                center = Offset(flash.x, flash.y)
            )
            // 8 rays umjesto 6 — VGA star burst
            val length = (12f + t * 55f) * px
            repeat(8) { ray ->
                val angle = ray * 0.7854f // 360/8 = 45 stupnjeva
                val start = Offset(flash.x + cos(angle) * 5f * px, flash.y + sin(angle) * 5f * px)
                val end = Offset(flash.x + cos(angle) * length, flash.y + sin(angle) * length)
                drawLine(Color(0xFFFFD700).copy(alpha = alpha * 0.90f), start, end, strokeWidth = 2.8f * px)
                drawLine(
                    Color.White.copy(alpha = alpha * 0.40f),
                    start,
                    Offset(flash.x + cos(angle) * length * 0.5f, flash.y + sin(angle) * length * 0.5f),
                    strokeWidth = 1.4f * px
                )
            }
            // Central star dot
            if (t < 0.3f) {
                drawCircle(Color.White.copy(alpha = alpha), radius = (6f - t * 20f).coerceAtLeast(0f) * px, center = Offset(flash.x, flash.y))
            }
        }
    }

    val cueStart = (horizontalPhaseDuration - 8f).coerceAtLeast(horizontalPhaseDuration * 0.85f)
    if (status == RunnerStatus.RUNNING && phaseTime > cueStart && phaseTime < horizontalPhaseDuration) {
        drawHorizontalVerticalApproachCue(w, h, groundY, px, phaseTime, horizontalPhaseDuration)
    }

    if (score >= 100 && score % 100 < 12) {
        val level = score / 100 + 1
        drawRoundRect(Color(0xAA2B1A11), Offset(w * 0.50f - 86f * px, h * 0.18f), Size(172f * px, 36f * px), CornerRadius(18f * px))
        drawRoundRect(Color(0xFFD0A15D).copy(alpha = 0.50f), Offset(w * 0.50f - 80f * px, h * 0.18f + 5f * px), Size(160f * px, 26f * px), CornerRadius(14f * px))
        drawIndieRunnerText(
            text = "LEVEL $level  •  DEEPER",
            x = w * 0.50f,
            y = h * 0.18f + 24f * px,
            textSize = 13f * px * 1.15f,
            textColor = android.graphics.Color.rgb(255, 245, 210)
        )
    }

    obstacles.forEach { obstacle ->
        when (obstacle.type) {
            RunnerObstacleType.PIT, RunnerObstacleType.BLIND_PIT -> {
                val pitTop = groundY - 5.5f * px
                val pitDepth = 84f * px
                val pitBottom = pitTop + pitDepth
                val variant = obstacle.id % 5
                val left = obstacle.x
                val width = obstacle.width
                val isBlindPit = obstacle.type == RunnerObstacleType.BLIND_PIT
                val revealedBlindPit = !isBlindPit || obstacle.x - (w * 0.18f) <= 120f * px
                if (!revealedBlindPit) {
                    drawRoundRect(
                        color = Color(0xFF342A24),
                        topLeft = Offset(left - 10f * px, groundY - 10f * px),
                        size = Size(width + 20f * px, 20f * px),
                        cornerRadius = CornerRadius(8f * px)
                    )
                    drawLine(
                        color = Color(0xFFC18B61).copy(alpha = 0.44f),
                        start = Offset(left - 12f * px, groundY - 5f * px),
                        end = Offset(left + width + 12f * px, groundY - 5f * px),
                        strokeWidth = 2.0f * px
                    )
                    return@forEach
                }
                val pitOuterColor = if (isBlindPit) runnerSierraPalette(biomeIndex).darkest else runnerSierraPalette(biomeIndex).dark
                val pitOuterStroke = runnerSierraPalette(biomeIndex).ceilingEdge.copy(alpha = if (isBlindPit) 0.58f else 0.66f)
                val pitInnerColor = runnerBiomePitInner(biomeIndex, isBlindPit)
                val pitInnerStroke = runnerSierraPalette(biomeIndex).floorEdge.copy(alpha = if (isBlindPit) 0.88f else 0.80f)

                // Flat shadow under the pit opening for stronger ground contact.
                drawOval(
                    Color.Black.copy(alpha = 0.22f),
                    Offset(left + width * 0.075f, groundY - 1.5f * px),
                    Size(width * 0.85f, 4f * px)
                )

                // VGA danger stripe border on the lip of every pit / blind pit.
                val stripeY = groundY - 12f * px
                val stripeH = 8f * px
                val stripeW = 14f * px
                var stripeX = left - 10f * px
                var stripeIndex = 0
                while (stripeX < left + width + 10f * px) {
                    drawRect(
                        color = if (stripeIndex % 2 == 0) Color.Black else Color(0xFFFFD54F),
                        topLeft = Offset(stripeX, stripeY),
                        size = Size(stripeW, stripeH)
                    )
                    stripeX += stripeW
                    stripeIndex++
                }
                drawRect(
                    Color.Black,
                    topLeft = Offset(left - 10f * px, stripeY),
                    size = Size(width + 20f * px, stripeH),
                    style = Stroke(width = 1.4f * px)
                )
                drawLine(
                    color = Color(0xFFFFD54F),
                    start = Offset(left - 4f * px, groundY),
                    end = Offset(left + width + 4f * px, groundY),
                    strokeWidth = 3.5f * px
                )

                // Completely new trap-pit visual pass: readable Prince-of-Persia style broken floor, finite hollow, rich interior.
                val outer = Path().apply {
                    moveTo(left - 12f * px, groundY - 8f * px)
                    quadraticBezierTo(left + width * 0.18f, groundY - 18f * px, left + width * 0.35f, groundY - 8f * px)
                    quadraticBezierTo(left + width * 0.52f, groundY + 2f * px, left + width * 0.68f, groundY - 9f * px)
                    quadraticBezierTo(left + width * 0.84f, groundY - 21f * px, left + width + 12f * px, groundY - 8f * px)
                    lineTo(left + width + 7f * px, pitBottom - 8f * px)
                    quadraticBezierTo(left + width * 0.62f, pitBottom + 10f * px, left + width * 0.46f, pitBottom - 2f * px)
                    quadraticBezierTo(left + width * 0.20f, pitBottom + 6f * px, left - 7f * px, pitBottom - 8f * px)
                    close()
                }
                drawPath(outer, Color.Black, style = Stroke(width = 3.2f * px))
                drawPath(outer, pitOuterColor)
                drawWobblyPathOutline(outer, px)
                drawPath(outer, pitOuterStroke, style = Stroke(width = 2.2f * px))
                if (biomeIndex % 5 == 3) {
                    drawLine(Color(0xFFFF6D00).copy(alpha = 0.65f), Offset(left - 4f * px, groundY - 10f * px), Offset(left + width * 0.28f, groundY - 2f * px), strokeWidth = 1.4f * px)
                    drawLine(Color(0xFFFF6D00).copy(alpha = 0.55f), Offset(left + width * 0.72f, groundY - 2f * px), Offset(left + width + 5f * px, groundY - 9f * px), strokeWidth = 1.4f * px)
                }

                val inner = Path().apply {
                    moveTo(left + 6f * px, groundY - 3f * px)
                    quadraticBezierTo(left + width * 0.30f, groundY - 13f * px, left + width * 0.50f, groundY - 5f * px)
                    quadraticBezierTo(left + width * 0.72f, groundY + 2f * px, left + width - 5f * px, groundY - 5f * px)
                    lineTo(left + width - 11f * px, pitBottom - 12f * px)
                    quadraticBezierTo(left + width * 0.52f, pitBottom + 4f * px, left + 9f * px, pitBottom - 11f * px)
                    close()
                }
                drawPath(inner, pitInnerColor)
                drawPath(inner, pitInnerStroke, style = Stroke(width = 1.5f * px))

                repeat(6) { chip ->
                    val side = if (chip % 2 == 0) -1f else 1f
                    val chipX = if (side < 0f) left - (5f + chip * 2f) * px else left + width + (2f + chip * 1.4f) * px
                    val chipY = groundY - (5f + (chip % 3) * 3f) * px
                    val shard = Path().apply {
                        moveTo(chipX, chipY)
                        lineTo(chipX + side * (13f + chip) * px, chipY + (5f + chip % 2 * 4f) * px)
                        lineTo(chipX + side * (5f + chip) * px, chipY + (13f + chip % 3 * 2f) * px)
                        close()
                    }
                    drawPath(shard, Color(0xFF8D674D))
                    drawPath(shard, Color(0xFFFFD6A0).copy(alpha = 0.28f), style = Stroke(width = 1f * px))
                }
                repeat(4) { crack ->
                    val baseX = if (crack < 2) left - 6f * px else left + width + 5f * px
                    val dir = if (crack < 2) -1f else 1f
                    val baseY = groundY - (2f + crack * 3f) * px
                    val crackPath = Path().apply {
                        moveTo(baseX, baseY)
                        lineTo(baseX + dir * (12f + crack * 2f) * px, baseY + 6f * px)
                        lineTo(baseX + dir * (5f + crack * 3f) * px, baseY + 15f * px)
                    }
                    drawPath(crackPath, Color(0xFF120908).copy(alpha = 0.88f), style = Stroke(width = 1.65f * px))
                }

                fun drawWaterPool(withPiranha: Boolean) {
                    val waterTop = pitBottom - 34f * px
                    val water = Path().apply {
                        moveTo(left + 10f * px, waterTop + 8f * px)
                        cubicTo(left + width * 0.25f, waterTop - 4f * px, left + width * 0.42f, waterTop + 14f * px, left + width * 0.58f, waterTop + 5f * px)
                        cubicTo(left + width * 0.75f, waterTop - 6f * px, left + width - 11f * px, waterTop + 8f * px, left + width - 7f * px, waterTop + 18f * px)
                        lineTo(left + width - 12f * px, waterTop + 30f * px)
                        lineTo(left + 10f * px, waterTop + 30f * px)
                        close()
                    }
                    drawPath(water, Color(0xFF06324B))
                    drawPath(water, Color(0xFF18A7E0).copy(alpha = 0.92f))
                    drawPath(water, Color(0xFFB2EBF2).copy(alpha = 0.68f), style = Stroke(width = 1.5f * px))
                    drawOval(Color.White.copy(alpha = 0.52f), Offset(left + width * 0.17f, waterTop + 8f * px), Size(width * 0.28f, 4.5f * px))
                    drawOval(Color(0xFF80DEEA).copy(alpha = 0.48f), Offset(left + width * 0.56f, waterTop + 12f * px), Size(width * 0.24f, 3.6f * px))
                    if (withPiranha) {
                        val fishX = left + width * 0.57f
                        val fishY = waterTop + 14f * px
                        val tail = Path().apply { moveTo(fishX - 15f * px, fishY); lineTo(fishX - 29f * px, fishY - 10f * px); lineTo(fishX - 27f * px, fishY + 10f * px); close() }
                        drawPath(tail, Color(0xFF8B1A1A))
                        drawOval(Color(0xFFC62828), Offset(fishX - 16f * px, fishY - 8f * px), Size(31f * px, 17f * px))
                        drawOval(Color(0xFFFF7043).copy(alpha = 0.74f), Offset(fishX - 6f * px, fishY - 5f * px), Size(13f * px, 8f * px))
                        drawCircle(Color.White, radius = 2.4f * px, center = Offset(fishX + 10f * px, fishY - 2.6f * px))
                        drawCircle(Color.Black, radius = 1.0f * px, center = Offset(fishX + 10.5f * px, fishY - 2.5f * px))
                        drawArc(Color(0xFF3A0505), 310f, 100f, false, Offset(fishX + 4f * px, fishY - 3f * px), Size(9f * px, 9f * px), style = Stroke(width = 1.4f * px))
                        repeat(5) { t ->
                            val tx = fishX + (3f + t * 2.0f) * px
                            val tooth = Path().apply { moveTo(tx, fishY + 2.2f * px); lineTo(tx + 1.1f * px, fishY + 6.7f * px); lineTo(tx + 2.3f * px, fishY + 2.2f * px); close() }
                            drawPath(tooth, Color.White)
                        }
                        drawCircle(Color(0xFFFFF59D).copy(alpha = 0.46f), radius = 10f * px, center = Offset(fishX + 18f * px, fishY - 7f * px))
                        drawCircle(Color(0xFFFFF176), radius = 2.4f * px, center = Offset(fishX + 18f * px, fishY - 7f * px))
                    }
                }

                if (biomeIndex % 5 == 3) {
                    // Lava biome: pits read as dangerous molten holes, never as water / bones / animals.
                    val lavaTop = pitBottom - 40f * px
                    val lava = Path().apply {
                        moveTo(left + 9f * px, lavaTop + 14f * px)
                        cubicTo(left + width * 0.22f, lavaTop - 8f * px, left + width * 0.38f, lavaTop + 20f * px, left + width * 0.55f, lavaTop + 5f * px)
                        cubicTo(left + width * 0.72f, lavaTop - 11f * px, left + width - 10f * px, lavaTop + 10f * px, left + width - 7f * px, lavaTop + 24f * px)
                        lineTo(left + width - 12f * px, pitBottom - 4f * px)
                        lineTo(left + 11f * px, pitBottom - 5f * px)
                        close()
                    }
                    drawPath(lava, Color(0xFF3A0000))
                    drawPath(lava, Color(0xFFFF3D00).copy(alpha = 0.88f))
                    drawPath(lava, Color(0xFFFFC400).copy(alpha = 0.66f), style = Stroke(width = 1.6f * px))
                    repeat(5) { bubble ->
                        val bx = left + width * (0.14f + bubble * 0.17f)
                        val by = lavaTop + (10f + (bubble % 3) * 7f) * px + sin(phaseTime * 5f + bubble) * 3f * px
                        drawCircle(Color(0xFFFFAB00).copy(alpha = 0.58f), radius = (2.4f + bubble % 2) * px, center = Offset(bx, by))
                    }
                    return@forEach
                }

                when (variant) {
                    0 -> drawWaterPool(withPiranha = false)
                    1 -> {
                        val baseY = pitBottom - 12f * px
                        repeat(7) { i ->
                            val bx = left + width * (0.10f + i * 0.13f)
                            val by = baseY - (i % 4) * 4.2f * px
                            drawLine(Color(0xFFF3E4C8), Offset(bx - 10f * px, by + 3f * px), Offset(bx + 15f * px, by - 4f * px), strokeWidth = 3.6f * px)
                            drawCircle(Color(0xFFF3E4C8), radius = 3f * px, center = Offset(bx - 12f * px, by + 4f * px))
                            drawCircle(Color(0xFFF3E4C8), radius = 3f * px, center = Offset(bx + 17f * px, by - 5f * px))
                        }
                        val skullX = left + width * 0.58f
                        val skullY = pitBottom - 30f * px
                        drawOval(Color(0xFFFFE8C7), Offset(skullX - 13f * px, skullY - 10f * px), Size(26f * px, 20f * px))
                        drawCircle(Color(0xFF120A08), radius = 2.7f * px, center = Offset(skullX - 5f * px, skullY - 2f * px))
                        drawCircle(Color(0xFF120A08), radius = 2.7f * px, center = Offset(skullX + 5f * px, skullY - 2f * px))
                        drawPath(Path().apply { moveTo(skullX, skullY + 1f * px); lineTo(skullX - 2.5f * px, skullY + 5f * px); lineTo(skullX + 2.5f * px, skullY + 5f * px); close() }, Color(0xFF120A08))
                        drawRoundRect(Color(0xFFFFE8C7), Offset(skullX - 8f * px, skullY + 7f * px), Size(16f * px, 7f * px), CornerRadius(2f * px))
                    }
                    2 -> drawWaterPool(withPiranha = true)
                    3 -> {
                        val cx = left + width * 0.52f
                        val cy = pitTop + 27f * px
                        repeat(8) { i ->
                            val endX = left + width * (0.08f + i * 0.12f)
                            val endY = pitTop + (8f + (i % 2) * 12f) * px
                            drawLine(Color(0xFFECEFF1).copy(alpha = 0.76f), Offset(cx, cy), Offset(endX, endY), strokeWidth = 1.25f * px)
                        }
                        repeat(4) { r ->
                            val rw = (23 + r * 13) * px
                            val rh = (12 + r * 7) * px
                            drawArc(Color(0xFFECEFF1).copy(alpha = 0.72f), 190f, 160f, false, Offset(cx - rw / 2f, cy - rh / 2f), Size(rw, rh), style = Stroke(width = 1.15f * px))
                            drawArc(Color(0xFFECEFF1).copy(alpha = 0.55f), 20f, 135f, false, Offset(cx - rw / 2f, cy - rh / 2f + 4f * px), Size(rw, rh), style = Stroke(width = 1f * px))
                        }
                        val sx = left + width * 0.56f
                        val sy = pitBottom - 23f * px
                        drawLine(Color(0xFFECEFF1).copy(alpha = 0.62f), Offset(cx, cy + 8f * px), Offset(sx, sy - 10f * px), strokeWidth = 1.2f * px)
                        drawOval(Color(0xFF080304), Offset(sx - 13f * px, sy - 8f * px), Size(26f * px, 18f * px))
                        drawCircle(Color(0xFF261015), radius = 7f * px, center = Offset(sx + 12f * px, sy - 1f * px))
                        repeat(4) { leg ->
                            val yy = sy - 13f * px + leg * 8f * px
                            drawLine(Color(0xFF080304), Offset(sx - 4f * px, sy), Offset(sx - 27f * px, yy), strokeWidth = 2.1f * px)
                            drawLine(Color(0xFF080304), Offset(sx + 5f * px, sy), Offset(sx + 30f * px, yy), strokeWidth = 2.1f * px)
                        }
                        drawCircle(Color(0xFFFFD54F), radius = 2.0f * px, center = Offset(sx + 10f * px, sy - 3f * px))
                        drawCircle(Color(0xFFFFD54F), radius = 2.0f * px, center = Offset(sx + 15f * px, sy - 3f * px))
                    }
                    else -> {
                        val sx = left + width * 0.18f
                        val sy = pitBottom - 22f * px
                        val snake = Path().apply {
                            moveTo(sx, sy)
                            cubicTo(sx + 13f * px, sy - 14f * px, sx + 27f * px, sy + 10f * px, sx + 42f * px, sy - 4f * px)
                            cubicTo(sx + 54f * px, sy - 16f * px, sx + 62f * px, sy - 4f * px, sx + 72f * px, sy - 10f * px)
                        }
                        drawPath(snake, Color(0xFF1B5E20), style = Stroke(width = 7.4f * px))
                        drawPath(snake, Color(0xFF9CCC65), style = Stroke(width = 3.1f * px))
                        val headX = sx + 72f * px
                        val headY = sy - 10f * px
                        drawOval(Color(0xFF0B3D14), Offset(headX - 6f * px, headY - 7f * px), Size(16f * px, 12f * px))
                        drawCircle(Color(0xFFFFF176), radius = 1.5f * px, center = Offset(headX + 5f * px, headY - 2f * px))
                        drawLine(Color(0xFFE53935), Offset(headX + 9f * px, headY), Offset(headX + 17f * px, headY - 2f * px), strokeWidth = 1.5f * px)
                        drawLine(Color(0xFFE53935), Offset(headX + 17f * px, headY - 2f * px), Offset(headX + 21f * px, headY - 5f * px), strokeWidth = 1f * px)
                        drawLine(Color(0xFFE53935), Offset(headX + 17f * px, headY - 2f * px), Offset(headX + 21f * px, headY + 1f * px), strokeWidth = 1f * px)
                    }
                }
            }
            RunnerObstacleType.LOW_CEILING -> {
                val squeezeBottom = groundY - 48f * px
                val distToPlayer = obstacle.x - (w * 0.18f)
                val warningAlpha = if (distToPlayer < 200f * px && distToPlayer > -obstacle.width) {
                    ((200f * px - distToPlayer) / (200f * px)).coerceIn(0f, 0.85f)
                } else 0f

                if (warningAlpha > 0.05f) {
                    repeat(3) { a ->
                        val arrowX = obstacle.x + obstacle.width * (0.20f + a * 0.28f)
                        val arrowY = squeezeBottom + 12f * px
                        val arrowPath = Path().apply {
                            moveTo(arrowX - 8f * px, arrowY - 10f * px)
                            lineTo(arrowX + 8f * px, arrowY - 10f * px)
                            lineTo(arrowX, arrowY)
                            close()
                        }
                        drawPath(arrowPath, Color.Black.copy(alpha = 0.90f * warningAlpha))
                        drawPath(arrowPath, Color(0xFFFFD54F).copy(alpha = warningAlpha))
                        drawPath(arrowPath, Color.Black.copy(alpha = 0.90f * warningAlpha), style = Stroke(width = 1.5f * px))
                    }
                }

                val squeezeLeft = obstacle.x - 14f * px
                val squeezeRight = obstacle.x + obstacle.width + 14f * px
                val squeezeColor = Color(0xFF5A5A5A)
                val edgeColor = Color(0xFF9E9E9E)
                val squeezeInnerShadow = Color(0xFF212121)
                val squeezeStalactiteFill = Color(0xFF616161)
                val squeezeStalactiteHighlight = Color(0xFFBDBDBD).copy(alpha = 0.55f)
                val squeeze = Path().apply {
                    moveTo(squeezeLeft, 0f)
                    lineTo(squeezeRight, 0f)
                    lineTo(squeezeRight, squeezeBottom - 16f * px)
                    lineTo(obstacle.x + obstacle.width * 0.88f, squeezeBottom - 2f * px)
                    lineTo(obstacle.x + obstacle.width * 0.70f, squeezeBottom - 12f * px)
                    lineTo(obstacle.x + obstacle.width * 0.50f, squeezeBottom + 3f * px)
                    lineTo(obstacle.x + obstacle.width * 0.30f, squeezeBottom - 10f * px)
                    lineTo(obstacle.x + obstacle.width * 0.12f, squeezeBottom - 1f * px)
                    lineTo(squeezeLeft, squeezeBottom - 18f * px)
                    close()
                }
                drawPath(squeeze, Color.Black, style = Stroke(width = 3.2f * px))
                drawInkPath(squeeze, squeezeColor.copy(alpha = 0.96f), px, highlight = true)

                drawLine(
                    squeezeInnerShadow,
                    Offset(obstacle.x + 2f * px, squeezeBottom + 3f * px),
                    Offset(obstacle.x + obstacle.width - 2f * px, squeezeBottom - 2f * px),
                    strokeWidth = 2.5f * px
                )
                drawLine(
                    edgeColor.copy(alpha = 1.0f),
                    Offset(obstacle.x + 2f * px, squeezeBottom + 2f * px),
                    Offset(obstacle.x + obstacle.width - 2f * px, squeezeBottom - 3f * px),
                    strokeWidth = 3.5f * px
                )
                drawLine(
                    Color.White.copy(alpha = 0.30f),
                    Offset(obstacle.x + 4f * px, squeezeBottom - 1f * px),
                    Offset(obstacle.x + obstacle.width - 4f * px, squeezeBottom - 5f * px),
                    strokeWidth = 1.2f * px
                )

                repeat(5) { k ->
                    val sx = obstacle.x + obstacle.width * (0.12f + k * 0.18f)
                    val len = (12f + (k % 3) * 6f) * px
                    val p = Path().apply {
                        moveTo(sx - 5f * px, squeezeBottom - 8f * px)
                        lineTo(sx + sin(k.toFloat()) * 2f * px, squeezeBottom + len)
                        lineTo(sx + 5f * px, squeezeBottom - 7f * px)
                        close()
                    }
                    drawInkPath(p, squeezeStalactiteFill.copy(alpha = 0.96f), px)
                    drawPath(p, Color.Black, style = Stroke(width = 1.4f * px))
                    drawLine(squeezeStalactiteHighlight, Offset(sx - 2.0f * px, squeezeBottom - 5f * px), Offset(sx - 1.0f * px, squeezeBottom + len * 0.75f), strokeWidth = 1.1f * px)
                }

                // Extra flat Sierra stalactites on the squeeze edge so the crawl hazard reads instantly.
                repeat(5) { i ->
                    val sx = obstacle.x + obstacle.width * (0.14f + i * 0.18f)
                    val stalH = (8f + (i % 3) * 4f) * px
                    val halfW = (5f + (i % 2) * 3f) * px
                    val p = Path().apply {
                        moveTo(sx - halfW, squeezeBottom - 3f * px)
                        lineTo(sx, squeezeBottom + stalH)
                        lineTo(sx + halfW, squeezeBottom - 3f * px)
                        close()
                    }
                    drawPath(p, squeezeStalactiteFill)
                    drawPath(p, Color.Black, style = Stroke(width = 1.4f * px))
                    drawLine(
                        squeezeStalactiteHighlight,
                        Offset(sx - halfW * 0.38f, squeezeBottom - 1f * px),
                        Offset(sx - halfW * 0.10f, squeezeBottom + stalH * 0.70f),
                        strokeWidth = 1.0f * px,
                        cap = StrokeCap.Round
                    )
                }

                if (warningAlpha > 0.30f) {
                    val textX = obstacle.x + obstacle.width * 0.50f
                    val textY = squeezeBottom + 28f * px
                    drawRoundRect(
                        Color.Black.copy(alpha = 0.75f * warningAlpha),
                        Offset(textX - 36f * px, textY - 13f * px),
                        Size(72f * px, 20f * px),
                        CornerRadius(10f * px)
                    )
                    drawIndieRunnerText(
                        "▼ PUZAJ",
                        textX,
                        textY,
                        10.5f * px * 1.15f,
                        android.graphics.Color.argb((230 * warningAlpha).toInt().coerceIn(0, 230), 255, 213, 79)
                    )
                }

                if (biomeIndex % 5 == 1) repeat(7) { b ->
                    val bx = obstacle.x + obstacle.width * (0.10f + b * 0.12f)
                    val by = squeezeBottom + (8f + b % 3 * 7f) * px - ((phaseTime * 18f + b * 5f) % 24f) * px
                    drawCircle(Color(0xFFE0F7FA).copy(alpha = 0.26f), radius = (1.2f + b % 2) * px, center = Offset(bx, by))
                }
            }
            RunnerObstacleType.STALAGMITE -> {
                val base = groundY
                val left = obstacle.x
                val w0 = obstacle.width
                drawOval(
                    Color.Black.copy(alpha = 0.22f),
                    Offset(left + w0 * 0.075f, groundY - 2f * px),
                    Size(w0 * 0.85f, 4f * px)
                )
                val cluster = listOf(
                    Triple(left + w0 * 0.18f, w0 * 0.12f, 18f * px),
                    Triple(left + w0 * 0.42f, w0 * 0.18f, 31f * px),
                    Triple(left + w0 * 0.64f, w0 * 0.15f, 25f * px),
                    Triple(left + w0 * 0.82f, w0 * 0.10f, 20f * px)
                )
                cluster.forEachIndexed { idx, (cx, half, height) ->
                    val p = Path().apply {
                        moveTo(cx - half, base)
                        quadraticBezierTo(cx - half * 0.12f, base - height * 0.60f, cx, base - height)
                        quadraticBezierTo(cx + half * 0.12f, base - height * 0.56f, cx + half, base)
                        close()
                    }
                    drawPath(p, Color.Black, style = Stroke(width = 3.2f * px))
                    drawInkPath(p, (if (idx == 1) runnerSierraPalette(biomeIndex).midLight else runnerSierraPalette(biomeIndex).mid).copy(alpha = 0.94f), px)
                    val shadow = Path().apply {
                        moveTo(cx + half * 0.08f, base - height * 0.88f)
                        lineTo(cx + half, base)
                        lineTo(cx + half * 0.18f, base)
                        close()
                    }
                    drawPath(shadow, Color.Black.copy(alpha = 0.28f))
                    drawWobblyPathOutline(p, px)
                    drawPath(p, runnerBiomeLight(biomeIndex).copy(alpha = 0.62f), style = Stroke(width = 3.5f * px))
                    drawLine(
                        runnerSierraPalette(biomeIndex).highlight.copy(alpha = 0.75f),
                        Offset(cx - half * 0.42f, base - 2f * px),
                        Offset(cx - half * 0.10f, base - height + 3f * px),
                        strokeWidth = 1.4f * px,
                        cap = StrokeCap.Round
                    )
                }
            }
            RunnerObstacleType.ICE_PATCH -> {
                val iceTop = groundY - if (biomeIndex % 5 == 4) 25f * px else 18f * px
                val iceHeight = if (biomeIndex % 5 == 4) 31f * px else 22f * px
                val shimmer = (sin(phaseTime * 10.5f + obstacle.id) * 0.08f).coerceIn(-0.08f, 0.08f)
                val icePath = Path().apply {
                    moveTo(obstacle.x + 6f * px, iceTop + 7f * px)
                    lineTo(obstacle.x + obstacle.width * 0.18f, iceTop + 1f * px)
                    lineTo(obstacle.x + obstacle.width * 0.46f, iceTop + 4f * px)
                    lineTo(obstacle.x + obstacle.width * 0.73f, iceTop)
                    lineTo(obstacle.x + obstacle.width - 8f * px, iceTop + 6f * px)
                    lineTo(obstacle.x + obstacle.width - 2f * px, iceTop + iceHeight * 0.62f)
                    lineTo(obstacle.x + obstacle.width * 0.66f, iceTop + iceHeight)
                    lineTo(obstacle.x + obstacle.width * 0.34f, iceTop + iceHeight * 0.82f)
                    lineTo(obstacle.x + 2f * px, iceTop + iceHeight * 0.66f)
                    close()
                }
                drawPath(
                    path = icePath,
                    color = Color.White.copy(alpha = 0.22f),
                    style = Stroke(width = 6f * px)
                )
                drawPath(
                    path = icePath,
                    color = Color(0xFF8EE7FF).copy(alpha = (if (biomeIndex % 5 == 4) 0.62f else 0.44f) + shimmer)
                )
                drawWobblyPathOutline(icePath, px)
                drawPath(
                    path = icePath,
                    color = Color(0xFFE1F5FE).copy(alpha = if (biomeIndex % 5 == 4) 0.68f else 0.42f),
                    style = Stroke(width = if (biomeIndex % 5 == 4) 2.2f * px else 1.6f * px)
                )
                drawRoundRect(
                    color = Color(0xFFEAFBFF).copy(alpha = 0.34f),
                    topLeft = Offset(obstacle.x + obstacle.width * 0.08f, iceTop + 5f * px),
                    size = Size(obstacle.width * 0.62f, 5f * px),
                    cornerRadius = CornerRadius(999f)
                )
                drawRoundRect(
                    color = Color(0xFF4FC3F7).copy(alpha = 0.18f),
                    topLeft = Offset(obstacle.x + obstacle.width * 0.18f, iceTop + iceHeight - 5f * px),
                    size = Size(obstacle.width * 0.68f, 4f * px),
                    cornerRadius = CornerRadius(999f)
                )
                repeat(7) { i ->
                    val crackX = obstacle.x + obstacle.width * (0.10f + i * 0.12f)
                    val crackY = iceTop + (6f + (i % 4) * 3f) * px
                    drawLine(
                        color = Color(0xFF0277BD).copy(alpha = 0.30f),
                        start = Offset(crackX, crackY),
                        end = Offset(crackX + (18f + i * 2.5f) * px, crackY + (-4f + (i % 3) * 4f) * px),
                        strokeWidth = 0.9f * px
                    )
                }
                repeat(5) { i ->
                    val glintX = obstacle.x + obstacle.width * (0.16f + i * 0.17f)
                    val glintY = iceTop + (4f + (i % 3) * 5f) * px + sin(phaseTime * 5f + i) * 1.5f * px
                    drawLine(
                        color = Color(0xFFFFFFFF).copy(alpha = 0.32f + (i % 2) * 0.12f),
                        start = Offset(glintX - 5f * px, glintY),
                        end = Offset(glintX + 5f * px, glintY),
                        strokeWidth = 1.2f * px
                    )
                    drawLine(
                        color = Color(0xFFFFFFFF).copy(alpha = 0.22f),
                        start = Offset(glintX, glintY - 4f * px),
                        end = Offset(glintX, glintY + 4f * px),
                        strokeWidth = 0.8f * px
                    )
                }
                listOf(0.20f, 0.50f, 0.80f).forEachIndexed { idx, xFactor ->
                    val cx = obstacle.x + obstacle.width * xFactor
                    val cy = iceTop + iceHeight * (0.42f + (idx % 2) * 0.18f)
                    val r = 5f * px
                    repeat(6) { arm ->
                        val angle = arm * 1.0471976f
                        drawLine(
                            Color.White.copy(alpha = 0.55f),
                            Offset(cx - cos(angle) * r, cy - sin(angle) * r),
                            Offset(cx + cos(angle) * r, cy + sin(angle) * r),
                            strokeWidth = 1.0f * px,
                            cap = StrokeCap.Round
                        )
                    }
                }
                if (speedBoostTimer > 0f && obstacle.x < w * 0.36f && obstacle.x + obstacle.width > w * 0.08f) {
                    repeat(3) { i ->
                        val y = groundY - (10f + i * 4f) * px
                        drawLine(
                            color = Color(0xFFB3E5FC).copy(alpha = 0.20f + sin(phaseTime * 18f + i) * 0.05f),
                            start = Offset(w * 0.11f - i * 10f * px, y),
                            end = Offset(w * 0.27f + i * 7f * px, y - 2f * px),
                            strokeWidth = (1.3f + i * 0.4f) * px
                        )
                    }
                }
            }
        }
    }

    if (phase != RunnerPhase.TRANSITION) {
        drawSpeleologist(w, groundY, px, playerLift, isCrawling, status, phaseTime, fallingObstacle, fallAnimationTime, depthMeters, biomeIndex, speedBoostTimer)
    }

    if (biomeIndex % 5 == 1 && status == RunnerStatus.RUNNING) {
        // Foreground water pass: makes the explorer read as waist-deep in the wet cave.
        val waterTop = groundY - 35f * px
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF4DD0E1).copy(alpha = 0.30f), Color(0xFF052A35).copy(alpha = 0.64f)),
                startY = waterTop,
                endY = h
            ),
            topLeft = Offset(0f, waterTop),
            size = Size(w, h - waterTop)
        )
        repeat(4) { wave ->
            val y = waterTop + (4f + wave * 8f) * px + sin(phaseTime * 3.4f + wave) * 2f * px
            drawLine(
                Color(0xFFE0F7FA).copy(alpha = 0.24f - wave * 0.025f),
                Offset(0f, y),
                Offset(w, y + sin(phaseTime * 2f + wave) * 2f * px),
                strokeWidth = (1.2f + wave * 0.25f) * px
            )
        }
        val playerX = w * 0.18f
        if (isCrawling) {
            repeat(9) { bubble ->
                val bx = playerX + (10f + bubble * 6.5f) * px
                val by = waterTop + (18f + bubble % 4 * 7f) * px - ((phaseTime * 18f + bubble * 5f) % 26f) * px
                drawCircle(Color(0xFFE0F7FA).copy(alpha = 0.42f), radius = (1.3f + bubble % 3 * 0.5f) * px, center = Offset(bx, by))
            }
        } else {
            repeat(5) { splash ->
                val bx = playerX - 16f * px + splash * 9f * px
                val by = waterTop + 4f * px + sin(phaseTime * 8f + splash) * 3f * px
                drawCircle(Color(0xFFB2EBF2).copy(alpha = 0.25f), radius = (1.4f + splash % 2) * px, center = Offset(bx, by))
            }
        }
    }
}


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVerticalExitToHorizontal(
    w: Float,
    h: Float,
    px: Float,
    verticalExitTime: Float,
    biomeIndex: Int,
    status: RunnerStatus
) {
    val t = (verticalExitTime / 2.05f).coerceIn(0f, 1f)
    val floorY = h * 0.78f
    val open = (t * t * (3f - 2f * t)).coerceIn(0f, 1f)
    drawRect(Color.Black.copy(alpha = 0.25f * (1f - t)), size = Size(w, h))
    val tunnelH = 76f * px * open
    val tunnel = Path().apply {
        moveTo(w * 0.44f, floorY + 5f * px)
        cubicTo(w * 0.53f, floorY - tunnelH, w * 0.80f, floorY - tunnelH * 0.70f, w + 12f * px, floorY - tunnelH * 0.42f)
        lineTo(w + 12f * px, floorY + 52f * px)
        cubicTo(w * 0.78f, floorY + 32f * px, w * 0.55f, floorY + 28f * px, w * 0.44f, floorY + 5f * px)
        close()
    }
    drawPath(tunnel, Color(0xFF030506).copy(alpha = 0.94f * open))
    drawPath(tunnel, runnerBiomeLight((biomeIndex + 1) % 5).copy(alpha = 0.28f * open), style = Stroke(width = 2.2f * px))
    repeat(9) { i ->
        val rx = w * (0.45f + i * 0.045f) + sin(i.toFloat()) * 5f * px
        val ry = floorY - tunnelH * (0.14f + (i % 3) * 0.18f)
        drawCircle(Color(0xFF5D4037).copy(alpha = 0.62f * open), radius = (2f + i % 3) * px, center = Offset(rx, ry + (1f - open) * 25f * px))
    }
    if (verticalExitTime >= 0.55f) {
        val walkT = ((verticalExitTime - 0.55f) / 1.50f).coerceIn(0f, 1f)
        val playerX = w * (0.34f + 0.14f * walkT)
        drawSpeleologistAt(playerX, floorY - 8f * px, px, status)
    }
    drawIndieRunnerText("Kanal se otvara...", w * 0.5f, h * 0.31f, 16f * px, android.graphics.Color.argb((220 * (1f - abs(t - 0.5f))).toInt().coerceIn(0,255), 255, 224, 160))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBiomeAtmosphere(
    biomeIndex: Int,
    w: Float,
    h: Float,
    px: Float,
    drift: Float,
    phaseTime: Float
) {
    when (biomeIndex % 5) {
        0 -> {
            repeat(8) { i ->
                val y = h * (0.18f + i * 0.055f) + sin(drift * 0.04f + i) * 2f * px
                drawLine(Color(0xFFD7B27A).copy(alpha = 0.12f), Offset(0f, y), Offset(w, y + sin(i.toFloat()) * 9f * px), strokeWidth = (1.2f + i % 2) * px)
            }
            repeat(7) { i ->
                val x = ((i * 77f * px - drift * 1.8f) % (w + 70f * px)) - 20f * px
                drawLine(Color(0xFF3E2714).copy(alpha = 0.45f), Offset(x, 0f), Offset(x + sin(i.toFloat()) * 20f * px, h * 0.22f), strokeWidth = (1.4f + i % 3) * px)
            }
            if ((drift.toInt() / 35) % 2 == 0) {
                val crackX = w * 0.62f
                val beam = Path().apply { moveTo(crackX, h * 0.10f); lineTo(crackX + 62f * px, h * 0.56f); lineTo(crackX + 15f * px, h * 0.56f); close() }
                drawPath(beam, Color(0xFFFFF59D).copy(alpha = 0.075f))
            }
        }
        1 -> {
            repeat(14) { i ->
                val x = ((i * 59f * px - drift * 1.4f) % (w + 50f * px))
                val y0 = h * (0.12f + (i % 5) * 0.05f)
                drawLine(Color(0xFFB2EBF2).copy(alpha = 0.17f), Offset(x, y0), Offset(x + sin(i.toFloat()) * 4f * px, y0 + (55f + i % 4 * 12f) * px), strokeWidth = 0.8f * px)
                val dropY = y0 + ((phaseTime * 42f + i * 17f) % 85f) * px
                drawCircle(Color(0xFFB2EBF2).copy(alpha = 0.52f), radius = 1.3f * px, center = Offset(x, dropY))
            }
        }
        2 -> {
            repeat(16) { i ->
                val pulse = (0.35f + 0.30f * sin(phaseTime * 3.8f + i)).coerceIn(0.08f, 0.72f)
                val cx = ((i * 83f * px - drift * 0.7f) % (w + 70f * px)) - 15f * px
                val cy = h * (0.18f + (i % 6) * 0.09f)
                val r = (5f + (i % 4) * 3f) * px
                val crystal = Path().apply { moveTo(cx, cy - r); lineTo(cx + r * 0.6f, cy); lineTo(cx, cy + r); lineTo(cx - r * 0.55f, cy); close() }
                drawPath(crystal, Color(0xFFE1BEE7).copy(alpha = 0.14f + pulse * 0.22f))
                drawPath(crystal, Color(0xFFB3E5FC).copy(alpha = pulse), style = Stroke(width = 0.8f * px))
            }
        }
        3 -> {
            repeat(8) { i ->
                val x = ((i * 93f * px - drift * 1.1f) % (w + 80f * px)) - 30f * px
                val y = h * (0.30f + (i % 5) * 0.10f) + sin(phaseTime * 4f + i) * 3f * px
                drawLine(Color(0xFFFF3D00).copy(alpha = 0.16f), Offset(x, y), Offset(x + 65f * px, y + sin(i.toFloat()) * 12f * px), strokeWidth = 5f * px)
                drawLine(Color(0xFFFFA000).copy(alpha = 0.42f), Offset(x, y), Offset(x + 65f * px, y + sin(i.toFloat()) * 12f * px), strokeWidth = 1.2f * px)
            }
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFFF3D00).copy(alpha = 0.12f)), startY = h * 0.62f, endY = h), size = Size(w, h))
        }
        else -> {
            repeat(10) { i ->
                val x = ((i * 70f * px - drift * 0.9f) % (w + 70f * px))
                val y = h * (0.15f + (i % 6) * 0.09f)
                drawRoundRect(Color(0xFFE1F5FE).copy(alpha = 0.13f), Offset(x, y), Size((22f + i % 3 * 8f) * px, (5f + i % 2 * 3f) * px), CornerRadius(2f * px))
            }
            val breath = ((sin(phaseTime * 2.9f) + 1f) / 2f).coerceIn(0f, 1f)
            drawCircle(Color.White.copy(alpha = 0.15f * breath), radius = (10f + 13f * breath) * px, center = Offset(w * 0.26f + breath * 22f * px, h * 0.55f - breath * 10f * px))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVerticalShaft(
    w: Float,
    h: Float,
    px: Float,
    segmentDepth: Float,
    playerLane: Int,
    hazardLane: Int,
    batLane: Int,
    nextHazardDepth: Float,
    nextBatDepth: Float,
    nextFallingRockDepth: Float,
    fallingRockLane: Int,
    nextBatSwarmDepth: Float,
    batSwarmLane: Int,
    batSwarmTimer: Float,
    batSwarmDirection: Int,
    cobwebLane: Int,
    cobwebTimer: Float,
    nextCobwebDepth: Float,
    icyRopeLane: Int,
    icyRopeTimer: Float,
    nextIcyRopeDepth: Float,
    depthMeters: Float,
    phaseTime: Float,
    status: RunnerStatus,
    biomeIndex: Int = 0,
    difficultyLevel: Int = 0,
    drawPlayer: Boolean = true
) {
    drawStableVerticalBackground(
        w = w,
        h = h,
        segmentDepth = segmentDepth,
        px = px,
        alpha = 0.92f,
        biomeIndex = biomeIndex,
        difficultyLevel = difficultyLevel
    )
    drawBiomeAtmosphere(biomeIndex, w, h, px, segmentDepth * 16f, phaseTime)

    val laneXs = listOf(w * 0.28f, w * 0.50f, w * 0.72f)
    val playerY = h * 0.58f
    val ropeOffset = (segmentDepth * 34f * px) % (38f * px)
    val secondsLeft = max(0f, 30f - phaseTime)

    fun drawRopeColumn(lane: Int, active: Boolean, icyActive: Boolean) {
        val x = laneXs[lane]
        val alpha = if (active) 0.95f else 0.42f
        val stroke = if (active) 4.8f * px else 3.0f * px
        val ropeColor = if (icyActive) Color(0xFFB3E5FC) else Color(0xFFE0D6C8)
        val knotColor = if (icyActive) Color(0xFFE1F5FE) else Color(0xFF8D6E63)
        if (icyActive) {
            drawRoundRect(
                color = Color(0xFF4FC3F7).copy(alpha = if (lane == playerLane) 0.22f else 0.12f),
                topLeft = Offset(x - 24f * px, playerY - 120f * px),
                size = Size(48f * px, 245f * px),
                cornerRadius = CornerRadius(999f)
            )
            drawLine(Color(0xFFE1F5FE).copy(alpha = 0.80f), Offset(x - 8f * px, playerY - 116f * px), Offset(x + 8f * px, playerY + 118f * px), strokeWidth = 2.2f * px)
            drawLine(Color(0xFF81D4FA).copy(alpha = 0.62f), Offset(x + 9f * px, playerY - 96f * px), Offset(x - 5f * px, playerY + 104f * px), strokeWidth = 1.6f * px)
        }
        drawLine(ropeColor.copy(alpha = alpha), Offset(x, -30f * px), Offset(x, h + 35f * px), strokeWidth = stroke)
        var y = -ropeOffset
        while (y < h + 45f * px) {
            drawLine(
                knotColor.copy(alpha = alpha * if (icyActive) 0.92f else 0.82f),
                Offset(x - 7f * px, y),
                Offset(x + 7f * px, y + 5f * px),
                strokeWidth = if (icyActive) 2.3f * px else 1.8f * px
            )
            if (icyActive && ((y / (38f * px)).toInt() % 2 == 0)) {
                drawCircle(Color.White.copy(alpha = 0.62f), radius = 1.5f * px, center = Offset(x + 10f * px, y + 4f * px))
                drawLine(Color(0xFFE1F5FE).copy(alpha = 0.48f), Offset(x - 12f * px, y + 9f * px), Offset(x - 3f * px, y + 2f * px), strokeWidth = 0.9f * px)
            }
            y += 38f * px
        }
    }

    // Three readable descent lanes. Only the selected lane is bright; the others stay as guides.
    laneXs.forEachIndexed { lane, x ->
        drawRoundRect(
            Color(0xFF06121B).copy(alpha = if (lane == playerLane) 0.28f else 0.13f),
            Offset(x - 37f * px, 0f),
            Size(74f * px, h),
            CornerRadius(999f)
        )
        drawRopeColumn(lane, lane == playerLane, icyRopeTimer > 0f && lane == icyRopeLane)
    }

    // Vertical shaft droplets: fall near inactive ropes so they add atmosphere without confusing lane input.
    laneXs.forEachIndexed { lane, laneX ->
        if (lane != playerLane) {
            repeat(6) { i ->
                val dropX = laneX + (-3f + i % 3 * 3f) * px
                val dropY = ((segmentDepth * 80f + i * 37f) % (h + 20f)) - 10f
                drawCircle(
                    color = Color(0xFF81D4FA).copy(alpha = 0.55f),
                    radius = (1.2f + i % 2 * 0.6f) * px,
                    center = Offset(dropX, dropY)
                )
            }
        }
    }

    val icyRemaining = (nextIcyRopeDepth - segmentDepth).coerceIn(0f, 5.0f)
    if (icyRopeTimer > 0f) {
        val iceX = laneXs[icyRopeLane]
        val iceSegmentTop = h * 0.12f
        val iceSegmentBottom = h * 0.58f
        val iceAlpha = if (icyRopeLane == playerLane) 0.95f else 0.60f

        // Normal-rope connector above and below the fixed icy segment.
        drawLine(Color(0xFFE0D6C8), Offset(iceX, 0f), Offset(iceX, iceSegmentTop), strokeWidth = 4.8f * px, cap = StrokeCap.Round)
        drawLine(Color(0xFFE0D6C8), Offset(iceX, iceSegmentBottom), Offset(iceX, h), strokeWidth = 4.8f * px, cap = StrokeCap.Round)
        drawLine(Color.Black, Offset(iceX, iceSegmentTop), Offset(iceX, iceSegmentBottom), strokeWidth = 9.5f * px, cap = StrokeCap.Round)
        drawLine(Color(0xFFB3E5FC).copy(alpha = iceAlpha), Offset(iceX, iceSegmentTop), Offset(iceX, iceSegmentBottom), strokeWidth = 7.5f * px, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.35f * iceAlpha), Offset(iceX - 2.4f * px, iceSegmentTop + 4f * px), Offset(iceX - 2.4f * px, iceSegmentBottom - 4f * px), strokeWidth = 1.8f * px, cap = StrokeCap.Round)

        repeat(5) { i ->
            val y = iceSegmentTop + (i + 1f) * (iceSegmentBottom - iceSegmentTop) / 6f
            val r = 5.5f * px
            val c = Color.White.copy(alpha = 0.72f * iceAlpha)
            drawLine(c, Offset(iceX - r, y), Offset(iceX + r, y), strokeWidth = 1.2f * px)
            drawLine(c, Offset(iceX - r * 0.55f, y - r), Offset(iceX + r * 0.55f, y + r), strokeWidth = 1.2f * px)
            drawLine(c, Offset(iceX - r * 0.55f, y + r), Offset(iceX + r * 0.55f, y - r), strokeWidth = 1.2f * px)
        }
        if (icyRopeLane == playerLane) {
            drawRoundRect(Color(0xAA07111F), Offset(iceX - 54f * px, iceSegmentBottom + 12f * px), Size(108f * px, 24f * px), CornerRadius(999f))
            drawIndieRunnerText("Ledeno uže", iceX, iceSegmentBottom + 29f * px, 10.5f * px, android.graphics.Color.rgb(225, 245, 254))
        }
    } else if (icyRemaining < 3.2f) {
        val iceX = laneXs[icyRopeLane]
        val warningY = playerY + (icyRemaining / 3.2f) * h * 0.32f
        repeat(5) { i ->
            drawCircle(
                color = Color(0xFFB3E5FC).copy(alpha = 0.28f + i * 0.04f),
                radius = (2.0f + i % 2) * px,
                center = Offset(iceX + (-18f + i * 9f) * px, warningY + sin(phaseTime * 8f + i) * 5f * px)
            )
        }
    }

    // Hazard: a clearly broken rope section drops toward the player line. Avoid that lane.
    val hazardRemaining = (nextHazardDepth - segmentDepth).coerceIn(0f, 6.2f)
    val hazardY = playerY + (hazardRemaining / 6.2f) * h * 0.38f
    val hazardX = laneXs[hazardLane]
    val breakGlow = if (hazardRemaining < 2.1f) 0.42f else 0.28f
    drawCircle(Color(0xFFFF5252).copy(alpha = breakGlow), radius = 34f * px, center = Offset(hazardX, hazardY))
    drawRoundRect(
        Color(0xFF230B0B).copy(alpha = 0.82f),
        Offset(hazardX - 30f * px, hazardY - 24f * px),
        Size(60f * px, 50f * px),
        CornerRadius(18f * px)
    )
    // visible rope gap
    drawLine(Color(0xFFE0D6C8), Offset(hazardX, hazardY - 44f * px), Offset(hazardX, hazardY - 14f * px), strokeWidth = 5.2f * px)
    drawLine(Color(0xFFE0D6C8), Offset(hazardX, hazardY + 15f * px), Offset(hazardX, hazardY + 45f * px), strokeWidth = 5.2f * px)
    // frayed broken fibers
    drawLine(Color(0xFFFFCDD2), Offset(hazardX - 8f * px, hazardY - 13f * px), Offset(hazardX - 20f * px, hazardY - 1f * px), strokeWidth = 2.4f * px)
    drawLine(Color(0xFFFFCDD2), Offset(hazardX + 7f * px, hazardY - 13f * px), Offset(hazardX + 20f * px, hazardY - 2f * px), strokeWidth = 2.4f * px)
    drawLine(Color(0xFFFFCDD2), Offset(hazardX - 7f * px, hazardY + 14f * px), Offset(hazardX - 20f * px, hazardY + 3f * px), strokeWidth = 2.4f * px)
    drawLine(Color(0xFFFFCDD2), Offset(hazardX + 8f * px, hazardY + 14f * px), Offset(hazardX + 20f * px, hazardY + 4f * px), strokeWidth = 2.4f * px)
    // high-contrast warning cross over the break
    drawLine(Color(0xFFFF1744), Offset(hazardX - 22f * px, hazardY - 12f * px), Offset(hazardX + 22f * px, hazardY + 12f * px), strokeWidth = 4.6f * px)
    drawLine(Color(0xFFFF1744), Offset(hazardX - 22f * px, hazardY + 12f * px), Offset(hazardX + 22f * px, hazardY - 12f * px), strokeWidth = 4.0f * px)
    drawCircle(Color(0xFFFFEBEE).copy(alpha = 0.80f), radius = 3.8f * px, center = Offset(hazardX, hazardY))
    if (hazardRemaining < 3.2f) {
        repeat(4) { s ->
            val sx = hazardX + (-18f + s * 12f) * px
            val sy = hazardY - 38f * px + ((phaseTime * 22f + s * 17f) % 46f) * px
            drawCircle(Color(0xFFBCAAA4).copy(alpha = 0.62f), radius = (2.0f + s % 2) * px, center = Offset(sx, sy))
        }
    }

    // Collectible: golden bat drops on a lane. Move into that lane before it passes the player.
    val batRemaining = (nextBatDepth - segmentDepth).coerceIn(0f, 5.5f)
    val batY = playerY + (batRemaining / 5.5f) * h * 0.36f
    val batX = laneXs[batLane]
    drawGoldenBat(batX, batY, px)

    val fallingRemaining = (nextFallingRockDepth - segmentDepth).coerceIn(0f, 5.8f)
    if (fallingRemaining < 5.8f) {
        val progress = (1f - fallingRemaining / 5.8f).coerceIn(0f, 1f)
        val laneX = laneXs[fallingRockLane]
        val rockY = -32f * px + progress * (playerY + 36f * px)
        if (fallingRemaining < 3.5f) {
            drawOval(
                color = Color(0xFFFF1744).copy(alpha = (0.20f + (3.5f - fallingRemaining) * 0.07f).coerceIn(0.20f, 0.46f)),
                topLeft = Offset(laneX - 28f * px, playerY + 25f * px),
                size = Size(56f * px, 13f * px)
            )
        }
        val wobble = sin(phaseTime * 12f + fallingRockLane) * 4f * px
        val rock = Path().apply {
            moveTo(laneX - 22f * px + wobble, rockY - 19f * px)
            lineTo(laneX - 5f * px - wobble, rockY - 31f * px)
            lineTo(laneX + 20f * px + wobble * 0.4f, rockY - 17f * px)
            lineTo(laneX + 27f * px - wobble, rockY + 5f * px)
            lineTo(laneX + 7f * px + wobble, rockY + 27f * px)
            lineTo(laneX - 23f * px - wobble * 0.5f, rockY + 15f * px)
            close()
        }
        drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.12f), radius = 38f * px, center = Offset(laneX, rockY))
        drawInkPath(rock, runnerSierraPalette(biomeIndex).mid, px)
        drawWobblyPathOutline(rock, px)
        drawPath(rock, Color(0xFFECEFF1).copy(alpha = 0.72f), style = Stroke(width = 2.2f * px))
        drawLine(Color(0xFFFAFAFA).copy(alpha = 0.78f), Offset(laneX - 11f * px, rockY - 18f * px), Offset(laneX + 8f * px, rockY - 6f * px), strokeWidth = 1.8f * px)
    }

    if (batSwarmTimer > 0f) {
        val progress = (1f - batSwarmTimer / 0.90f).coerceIn(0f, 1f)
        val direction = if (batSwarmDirection >= 0) 1f else -1f
        val swarmY = playerY + (batSwarmLane - 1) * 31f * px
        repeat(8) { i ->
            val offset = i * 27f * px
            val startX = if (direction > 0f) -70f * px - offset else w + 70f * px + offset
            val x = startX + direction * progress * (w + 220f * px)
            val y = swarmY + sin(phaseTime * 10f + i) * 8f * px
            val wing = (7f + i % 3) * px
            val bat = Path().apply {
                moveTo(x, y)
                quadraticBezierTo(x - wing, y - wing * 0.65f, x - wing * 1.65f, y + 2f * px)
                quadraticBezierTo(x - wing * 0.55f, y + 1f * px, x - 2f * px, y + 5f * px)
                quadraticBezierTo(x, y + 8f * px, x + 2f * px, y + 5f * px)
                quadraticBezierTo(x + wing * 0.55f, y + 1f * px, x + wing * 1.65f, y + 2f * px)
                quadraticBezierTo(x + wing, y - wing * 0.65f, x, y)
                close()
            }
            drawInkPath(bat, runnerSierraPalette(biomeIndex).darkest.copy(alpha = 0.88f), px)
            drawPath(bat, Color(0xFFFFC107).copy(alpha = if (batSwarmLane == playerLane) 0.34f else 0.12f), style = Stroke(width = 0.9f * px))
        }
    }

    if (cobwebTimer > 0f) {
        val mudX = laneXs[cobwebLane]
        val mudSegmentTop = h * 0.15f
        val mudSegmentBottom = h * 0.55f
        val mudAlpha = if (cobwebLane == playerLane) 0.95f else 0.60f

        // Normal-rope connector above and below the fixed mud segment.
        drawLine(Color(0xFFE0D6C8), Offset(mudX, 0f), Offset(mudX, mudSegmentTop), strokeWidth = 4.8f * px, cap = StrokeCap.Round)
        drawLine(Color(0xFFE0D6C8), Offset(mudX, mudSegmentBottom), Offset(mudX, h), strokeWidth = 4.8f * px, cap = StrokeCap.Round)
        drawLine(Color.Black, Offset(mudX, mudSegmentTop), Offset(mudX, mudSegmentBottom), strokeWidth = 10.5f * px, cap = StrokeCap.Round)
        drawLine(Color(0xFF5D4037).copy(alpha = mudAlpha), Offset(mudX, mudSegmentTop), Offset(mudX, mudSegmentBottom), strokeWidth = 8.5f * px, cap = StrokeCap.Round)

        repeat(6) { i ->
            val my = mudSegmentTop + (i + 0.5f) * (mudSegmentBottom - mudSegmentTop) / 6f
            val mw = (7f + i % 3 * 3f) * px
            val mh = (4f + i % 2 * 3f) * px
            val side = if (i % 2 == 0) -1f else 1f
            val blobTopLeft = Offset(mudX - mw / 2f + side * 2.2f * px, my - mh / 2f)
            val blobSize = Size(mw, mh)
            drawOval(Color(0xFF4E342E).copy(alpha = mudAlpha * 0.88f), blobTopLeft, blobSize)
            drawOval(Color.Black, blobTopLeft, blobSize, style = Stroke(width = 1.1f * px))
            drawLine(Color(0xFF8D6E63).copy(alpha = mudAlpha * 0.55f), Offset(blobTopLeft.x + mw * 0.20f, blobTopLeft.y + mh * 0.25f), Offset(blobTopLeft.x + mw * 0.62f, blobTopLeft.y + mh * 0.42f), strokeWidth = 1.0f * px)
        }

        repeat(3) { i ->
            val dropCenter = Offset(mudX + (-4f + i * 4f) * px, mudSegmentBottom + (9f + i * 8f) * px)
            drawCircle(Color(0xFF5D4037).copy(alpha = mudAlpha * 0.80f), radius = 2.5f * px, center = dropCenter)
            drawCircle(Color.Black, radius = 3.0f * px, center = dropCenter, style = Stroke(width = 0.8f * px))
        }

        if (cobwebLane == playerLane) {
            drawRoundRect(Color(0xAA07111F), Offset(mudX - 38f * px, mudSegmentBottom + 12f * px), Size(76f * px, 24f * px), CornerRadius(999f))
            drawIndieRunnerText("Blato", mudX, mudSegmentBottom + 29f * px, 10.0f * px, android.graphics.Color.rgb(141, 110, 99))
        }
    }

    // Player light, subtle rope swing arc and back-facing rope sprite.
    val swing = sin(phaseTime * 5.4f) * 5.5f * px
    val playerX = laneXs[playerLane] + swing
    if (drawPlayer) {
        val swingPath = Path().apply {
            moveTo(laneXs[playerLane], playerY - 78f * px)
            quadraticBezierTo(laneXs[playerLane] + swing * 2.4f, playerY - 28f * px, playerX, playerY + 40f * px)
        }
        drawPath(swingPath, Color(0xFFE0D6C8).copy(alpha = 0.30f), style = Stroke(width = 2.2f * px))
        drawRopeSpeleologist(playerX, playerY, px, depthMeters, biomeIndex)
    }

    if (hazardRemaining < 2.0f) {
        drawCircle(Color(0xFFFF5252).copy(alpha = 0.34f), radius = 31f * px, center = Offset(hazardX, playerY + 34f * px))
        drawRoundRect(Color(0xFFFF5252).copy(alpha = 0.72f), Offset(hazardX - 18f * px, playerY + 23f * px), Size(36f * px, 8f * px), CornerRadius(999f))
    }

    drawRoundRect(
        Color(0xAA07111F),
        Offset(w * 0.5f - 68f * px, 15f * px),
        Size(136f * px, 28f * px),
        CornerRadius(999f)
    )
    drawIndieRunnerText(
        text = "Vertical ${secondsLeft.toInt()}s",
        x = w * 0.5f,
        y = 35f * px,
        textSize = 13f * px * 1.15f,
        textColor = android.graphics.Color.WHITE
    )
}


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGoldenBat(
    x: Float,
    y: Float,
    px: Float
) {
    // Outer glow ring
    drawCircle(Color(0xFFFFD700).copy(alpha = 0.18f), radius = 26f * px, center = Offset(x, y))
    drawCircle(Color(0xFFFFF176).copy(alpha = 0.10f), radius = 32f * px, center = Offset(x, y))

    val wing = 20f * px
    val bat = Path().apply {
        moveTo(x, y - 5f * px)
        cubicTo(x - wing * 0.40f, y - wing * 0.78f, x - wing, y - 9f * px, x - wing * 1.20f, y + 2f * px)
        quadraticBezierTo(x - wing * 0.75f, y - 1f * px, x - wing * 0.44f, y + 11f * px)
        quadraticBezierTo(x - wing * 0.20f, y + 4f * px, x - 5.5f * px, y + 6f * px)
        quadraticBezierTo(x, y + 14f * px, x + 5.5f * px, y + 6f * px)
        quadraticBezierTo(x + wing * 0.20f, y + 4f * px, x + wing * 0.44f, y + 11f * px)
        quadraticBezierTo(x + wing * 0.75f, y - 1f * px, x + wing * 1.20f, y + 2f * px)
        cubicTo(x + wing, y - 9f * px, x + wing * 0.40f, y - wing * 0.78f, x, y - 5f * px)
        close()
    }
    // Shadow
    drawPath(Path().apply { addPath(bat, Offset(2f * px, 2.5f * px)) }, Color.Black.copy(alpha = 0.38f))
    // Fill — gold gradient faked with two passes
    drawPath(bat, Color(0xFFFFB300))
    drawPath(Path().apply { addPath(bat, Offset(-1f * px, -1.2f * px)) }, Color(0xFFFFE57F))
    // Chunky ink outline
    drawPath(bat, Color.Black, style = Stroke(width = 3.0f * px))
    // Highlight stroke
    drawPath(Path().apply { addPath(bat, Offset(-1f * px, -1f * px)) }, Color(0xFFFFF9C4).copy(alpha = 0.65f), style = Stroke(width = 1.2f * px))
    // Body oval
    drawOval(Color(0xFFFF8F00), Offset(x - 5.5f * px, y - 7f * px), Size(11f * px, 20f * px))
    drawOval(Color.Black, Offset(x - 5.5f * px, y - 7f * px), Size(11f * px, 20f * px), style = Stroke(width = 2.0f * px))
    // Ears
    val ears = Path().apply {
        moveTo(x - 5f * px, y - 7f * px); lineTo(x - 10f * px, y - 16f * px); lineTo(x - 1f * px, y - 9f * px)
        moveTo(x + 5f * px, y - 7f * px); lineTo(x + 10f * px, y - 16f * px); lineTo(x + 1f * px, y - 9f * px)
    }
    drawPath(ears, Color.Black, style = Stroke(width = 2.2f * px, cap = StrokeCap.Round))
    // Eyes
    drawCircle(Color(0xFFFF3D00), radius = 1.8f * px, center = Offset(x - 2.2f * px, y - 1f * px))
    drawCircle(Color(0xFFFF3D00), radius = 1.8f * px, center = Offset(x + 2.2f * px, y - 1f * px))
    drawCircle(Color.Black, radius = 0.9f * px, center = Offset(x - 2.2f * px, y - 1f * px))
    drawCircle(Color.Black, radius = 0.9f * px, center = Offset(x + 2.2f * px, y - 1f * px))
    // Shine dot
    drawCircle(Color.White.copy(alpha = 0.90f), radius = 0.7f * px, center = Offset(x - 1.5f * px, y - 1.8f * px))
}




private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallingSpeleologist(
    playerX: Float,
    groundY: Float,
    px: Float,
    obstacle: RunnerObstacle,
    fallTime: Float
) {
    val t = fallTime.coerceIn(0f, 1.35f)
    val progress = (t / 1.35f).coerceIn(0f, 1f)
    if (obstacle.type == RunnerObstacleType.LOW_CEILING) {
        val squash = sin(progress * 3.14159f).coerceAtLeast(0f)
        val cx = playerX + 2f * px
        val baseY = groundY - 4f * px
        drawRoundRect(Color(0xFF4D2B23), Offset(cx - 48f * px, baseY - 72f * px), Size(98f * px, 24f * px), CornerRadius(13f * px))
        drawRoundRect(Color(0xFFFFCCBC).copy(alpha = 0.28f), Offset(cx - 35f * px, baseY - 66f * px), Size(48f * px, 5f * px), CornerRadius(4f * px))
        drawRoundRect(Color(0xFFE53935), Offset(cx - 28f * px, baseY - 38f * px + squash * 4f * px), Size(58f * px, 26f * px - squash * 6f * px), CornerRadius(15f * px))
        drawCircle(Color(0xFFFFC107), radius = 16f * px, center = Offset(cx + 22f * px, baseY - 48f * px + squash * 7f * px))
        drawRoundRect(Color(0xFF90A4AE), Offset(cx + 31f * px, baseY - 54f * px + squash * 7f * px), Size(9f * px, 8f * px), CornerRadius(3f * px))
        drawLine(Color(0xFFFFCC80), Offset(cx - 14f * px, baseY - 30f * px), Offset(cx - 44f * px, baseY - 50f * px), strokeWidth = 4.4f * px)
        drawLine(Color(0xFFFFCC80), Offset(cx + 14f * px, baseY - 30f * px), Offset(cx + 48f * px, baseY - 43f * px), strokeWidth = 4.4f * px)
        return
    }
    if (obstacle.type == RunnerObstacleType.STALAGMITE) {
        val cx = playerX + sin(progress * 9f) * 5f * px
        val baseY = groundY - 6f * px
        val star = Path().apply {
            moveTo(cx, baseY - 90f * px); lineTo(cx + 5f * px, baseY - 76f * px); lineTo(cx + 20f * px, baseY - 76f * px); lineTo(cx + 8f * px, baseY - 66f * px); lineTo(cx + 12f * px, baseY - 52f * px); lineTo(cx, baseY - 60f * px); lineTo(cx - 12f * px, baseY - 52f * px); lineTo(cx - 8f * px, baseY - 66f * px); lineTo(cx - 20f * px, baseY - 76f * px); lineTo(cx - 5f * px, baseY - 76f * px); close()
        }
        drawPath(star, Color(0xFFFFF176).copy(alpha = 0.72f))
        drawRoundRect(Color(0xFFE53935), Offset(cx - 28f * px, baseY - 41f * px + progress * 10f * px), Size(56f * px, 35f * px), CornerRadius(18f * px))
        drawCircle(Color(0xFFFFC107), radius = 16f * px, center = Offset(cx + 23f * px, baseY - 49f * px + progress * 4f * px))
        drawLine(Color(0xFFFFCC80), Offset(cx - 18f * px, baseY - 27f * px), Offset(cx - 46f * px, baseY - 15f * px), strokeWidth = 4.5f * px)
        drawLine(Color(0xFFFFCC80), Offset(cx + 16f * px, baseY - 27f * px), Offset(cx + 48f * px, baseY - 18f * px), strokeWidth = 4.5f * px)
        drawLine(Color(0xFFD32F2F), Offset(cx - 10f * px, baseY - 9f * px), Offset(cx - 32f * px, baseY + 5f * px), strokeWidth = 5.2f * px)
        drawLine(Color(0xFFD32F2F), Offset(cx + 10f * px, baseY - 9f * px), Offset(cx + 35f * px, baseY + 5f * px), strokeWidth = 5.2f * px)
        return
    }
    val sink = 12f * px + progress * 86f * px
    val tilt = sin(progress * 3.14159f) * 14f * px
    val shake = sin(t * 35f) * 2.0f * px
    val cx = playerX + tilt + shake
    val baseY = groundY - 42f * px + sink

    // Dust and small falling stones around the broken edge.
    drawCircle(Color(0xFFBCAAA4).copy(alpha = 0.22f * (1f - progress)), radius = (22f + 16f * progress) * px, center = Offset(playerX, groundY - 4f * px))
    drawCircle(Color(0xFF795548).copy(alpha = 0.38f * (1f - progress)), radius = 3.2f * px, center = Offset(playerX - 24f * px, groundY - 21f * px + sink * 0.20f))
    drawCircle(Color(0xFF5D4037).copy(alpha = 0.34f * (1f - progress)), radius = 2.4f * px, center = Offset(playerX + 25f * px, groundY - 28f * px + sink * 0.24f))
    drawCircle(Color(0xFF8D6E63).copy(alpha = 0.30f * (1f - progress)), radius = 2.8f * px, center = Offset(playerX + 5f * px, groundY - 36f * px + sink * 0.18f))

    // If he is already deep in the hole, show only helmet/lamp disappearing.
    val alpha = (1f - (progress - 0.68f).coerceIn(0f, 0.32f) / 0.32f).coerceIn(0f, 1f)
    if (alpha <= 0.02f) return

    // Headlamp cone, angled upward as he falls.
    val lampX = cx + 14f * px
    val lampY = baseY - 40f * px
    val cone = Path().apply {
        moveTo(lampX, lampY)
        lineTo(lampX + 78f * px, lampY - 52f * px)
        lineTo(lampX + 92f * px, lampY + 18f * px)
        close()
    }
    drawPath(cone, Color(0xFFFFF59D).copy(alpha = 0.18f * alpha))

    // Flailing arms.
    drawLine(Color(0xFFFFCC80).copy(alpha = alpha), Offset(cx - 12f * px, baseY - 17f * px), Offset(cx - 37f * px, baseY - 45f * px), strokeWidth = 4.5f * px)
    drawLine(Color(0xFFFFCC80).copy(alpha = alpha), Offset(cx + 11f * px, baseY - 17f * px), Offset(cx + 38f * px, baseY - 39f * px), strokeWidth = 4.5f * px)
    drawCircle(Color(0xFFFFE0B2).copy(alpha = alpha), radius = 3.8f * px, center = Offset(cx - 38f * px, baseY - 46f * px))
    drawCircle(Color(0xFFFFE0B2).copy(alpha = alpha), radius = 3.8f * px, center = Offset(cx + 39f * px, baseY - 40f * px))

    // Body in red/orange caving suit.
    drawRoundRect(Color.Black.copy(alpha = 0.18f * alpha), Offset(cx - 16f * px, baseY - 27f * px), Size(36f * px, 45f * px), CornerRadius(15f * px))
    drawRoundRect(Color(0xFFB71C1C).copy(alpha = alpha), Offset(cx - 18f * px, baseY - 30f * px), Size(36f * px, 44f * px), CornerRadius(15f * px))
    if (alpha > 0.15f) drawWobblyRoundRectOutline(Offset(cx - 18f * px, baseY - 30f * px), Size(36f * px, 44f * px), CornerRadius(15f * px), px)
    drawRoundRect(Color(0xFFE53935).copy(alpha = alpha), Offset(cx - 15f * px, baseY - 28f * px), Size(30f * px, 38f * px), CornerRadius(13f * px))
    drawLine(Color(0xFF263238).copy(alpha = alpha), Offset(cx - 14f * px, baseY - 18f * px), Offset(cx + 14f * px, baseY + 8f * px), strokeWidth = 2.2f * px)
    drawLine(Color(0xFF263238).copy(alpha = alpha), Offset(cx + 14f * px, baseY - 18f * px), Offset(cx - 14f * px, baseY + 8f * px), strokeWidth = 2.2f * px)
    drawRoundRect(Color(0xFF5D4037).copy(alpha = alpha), Offset(cx - 22f * px, baseY - 19f * px), Size(11f * px, 19f * px), CornerRadius(5f * px))

    // Legs kicking upward while falling.
    drawLine(Color(0xFFD32F2F).copy(alpha = alpha), Offset(cx - 8f * px, baseY + 7f * px), Offset(cx - 31f * px, baseY - 2f * px), strokeWidth = 5.2f * px)
    drawLine(Color(0xFFD32F2F).copy(alpha = alpha), Offset(cx + 8f * px, baseY + 7f * px), Offset(cx + 28f * px, baseY + 22f * px), strokeWidth = 5.2f * px)
    drawLine(Color(0xFF4E342E).copy(alpha = alpha), Offset(cx - 35f * px, baseY - 2f * px), Offset(cx - 24f * px, baseY - 2f * px), strokeWidth = 5.4f * px)
    drawLine(Color(0xFF4E342E).copy(alpha = alpha), Offset(cx + 25f * px, baseY + 24f * px), Offset(cx + 37f * px, baseY + 24f * px), strokeWidth = 5.4f * px)

    // Helmet and expressive face.
    val helmetCenter = Offset(cx + 2f * px, baseY - 48f * px)
    drawCircle(Color(0xFFFFC107).copy(alpha = alpha), radius = 17f * px, center = helmetCenter)
    drawCircle(Color(0xFFFFE082).copy(alpha = 0.58f * alpha), radius = 11.5f * px, center = Offset(helmetCenter.x - 3f * px, helmetCenter.y - 4f * px))
    drawRoundRect(Color(0xFFFFD54F).copy(alpha = alpha), Offset(helmetCenter.x - 18f * px, helmetCenter.y - 14f * px), Size(36f * px, 10f * px), CornerRadius(8f * px))
    drawOval(Color(0xFFFFCC80).copy(alpha = alpha), Offset(helmetCenter.x - 6f * px, helmetCenter.y + 4f * px), Size(17f * px, 15f * px))
    drawCircle(Color(0xFF263238).copy(alpha = alpha), radius = 1.8f * px, center = Offset(helmetCenter.x + 5f * px, helmetCenter.y + 8f * px))
    drawLine(Color(0xFF4E342E).copy(alpha = alpha), Offset(helmetCenter.x + 4f * px, helmetCenter.y + 14f * px), Offset(helmetCenter.x + 10f * px, helmetCenter.y + 13f * px), strokeWidth = 1.4f * px)
    drawRoundRect(Color(0xFF90A4AE).copy(alpha = alpha), Offset(helmetCenter.x + 8f * px, helmetCenter.y - 8f * px), Size(10f * px, 10f * px), CornerRadius(3f * px))
    drawCircle(Color.White.copy(alpha = 0.95f * alpha), radius = 4.2f * px, center = Offset(helmetCenter.x + 14f * px, helmetCenter.y - 5f * px))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpeleologist(
    w: Float,
    groundY: Float,
    px: Float,
    playerLift: Float,
    isCrawling: Boolean,
    status: RunnerStatus,
    stepTime: Float,
    fallingObstacle: RunnerObstacle?,
    fallAnimationTime: Float,
    depthMeters: Float = 0f,
    biomeIndex: Int = 0,
    speedBoostTimer: Float = 0f
) {
    if (status == RunnerStatus.GAME_OVER && fallingObstacle != null) {
        drawFallingSpeleologist(
            playerX = w * 0.18f,
            groundY = groundY,
            px = px,
            obstacle = fallingObstacle,
            fallTime = fallAnimationTime
        )
        return
    }

    val playerX = w * 0.18f
    val playerH = if (isCrawling) 40f * px else 76f * px
    val playerW = if (isCrawling) 60f * px else 48f * px
    val isAirborne = playerLift > 1.5f
    val feetY = groundY - playerLift
    val runCycle = if (status == RunnerStatus.RUNNING && !isCrawling && !isAirborne) sin(stepTime * 16.5f) else 0f
    val jumpPeak = if (isAirborne) sin((playerLift / (150f * px)).coerceIn(0f, 1f) * 3.14159f) else 0f
    val jumpTuck = jumpPeak * 16f * px
    val walkBob = if (status == RunnerStatus.RUNNING && !isCrawling && !isAirborne)
        sin(stepTime * 9.5f) * 3.8f * px
    else 0f
    val armSwing = when {
        isAirborne -> -16f * px * jumpPeak
        // Arms move opposite to the legs: when the left leg moves forward, the left arm moves back.
        else -> -runCycle * 8.5f * px
    }
    val legSwing = if (isAirborne) 0f else runCycle * 9.5f * px
    val top = feetY - playerH - walkBob
    val left = playerX - playerW / 2f

    fun drawProfileHelmet(cx: Float, cy: Float, compact: Boolean = false) {
        val r = if (compact) 13f * px else 15f * px
        val smileDepth = if (compact) 2.2f * px else 3.5f * px

        // GLAVA — beige krug s Sierra passom
        drawSierraCircle(Offset(cx, cy), r, SPELE_SKIN_DARK, SPELE_SKIN_MID, SPELE_SKIN_HI, px)

        // OKO — profil gleda desno
        val eyeX = cx + r * 0.38f
        val eyeY = cy - r * 0.10f
        drawCircle(Color.White, radius = 3.6f * px, center = Offset(eyeX, eyeY))
        drawCircle(SPELE_INK, radius = 2.0f * px, center = Offset(eyeX + 0.8f * px, eyeY))
        drawCircle(Color.White, radius = 0.8f * px, center = Offset(eyeX + 1.3f * px, eyeY - 1.0f * px))
        // Obrva
        drawLine(
            SPELE_INK,
            Offset(eyeX - 3.0f * px, eyeY - 4.2f * px),
            Offset(eyeX + 3.5f * px, eyeY - 4.8f * px),
            strokeWidth = 1.4f * px, cap = StrokeCap.Round
        )

        // OSMIJEH — luk prema dolje, profil desno
        val smileX = cx + r * 0.18f
        val smileY = cy + r * 0.44f
        val smilePath = Path().apply {
            moveTo(smileX - 4f * px, smileY - 1f * px)
            cubicTo(
                smileX - 1.5f * px, smileY + smileDepth,
                smileX + 2f * px,   smileY + smileDepth,
                smileX + 4.5f * px, smileY - 1f * px
            )
        }
        drawPath(smilePath, SPELE_INK, style = Stroke(width = 1.5f * px, cap = StrokeCap.Round))

        // KACIGA — žuta, sjedi na gornjoj 2/3 glave
        val helmetPath = Path().apply {
            moveTo(cx - r * 1.10f, cy + r * 0.06f)
            cubicTo(
                cx - r * 1.16f, cy - r * 0.58f,
                cx - r * 0.78f, cy - r * 1.40f,
                cx,             cy - r * 1.36f
            )
            cubicTo(
                cx + r * 0.68f,  cy - r * 1.33f,
                cx + r * 1.04f,  cy - r * 0.70f,
                cx + r * 1.06f,  cy - r * 0.04f
            )
            // Brim kacige
            lineTo(cx + r * 1.20f, cy + r * 0.12f)
            cubicTo(
                cx + r * 0.86f, cy + r * 0.30f,
                cx - r * 0.80f, cy + r * 0.30f,
                cx - r * 1.20f, cy + r * 0.14f
            )
            close()
        }
        drawSierraShape(helmetPath, SPELE_HELMET_DARK, SPELE_HELMET_MID, SPELE_HELMET_HI, px)

        // Mali SOV natpis na kacigi — samo na većoj glavnoj pozi, da ne zatrpa compact/crawl prikaz.
        if (!compact) {
            drawIndieRunnerText(
                text = "SOV",
                x = cx - r * 0.14f,
                y = cy - r * 0.64f,
                textSize = 5.2f * px,
                textColor = android.graphics.Color.rgb(72, 38, 0)
            )
        }

        // LAMPA — kutijica na desnoj strani kacige + bijeli sjaj
        val lampBx = cx + r * 0.80f
        val lampBy = cy - r * 0.46f
        drawSierraRoundRect(
            Offset(lampBx, lampBy), Size(11f * px, 7.5f * px), 2.5f * px,
            SPELE_HELMET_DARK, Color(0xFF455A64), Color(0xFF90A4AE), px
        )
        drawCircle(Color.White.copy(alpha = 0.96f), radius = 3.0f * px,
            center = Offset(lampBx + 9.5f * px, lampBy + 3.8f * px))
        drawCircle(Color.White.copy(alpha = 0.30f), radius = 5.0f * px,
            center = Offset(lampBx + 9.5f * px, lampBy + 3.8f * px))
    }

    fun drawBackpack(x: Float, y: Float, width: Float, height: Float) {
        drawSierraRoundRect(
            Offset(x, y), Size(width, height), 6f * px,
            SPELE_PACK_DARK, SPELE_PACK_MID, SPELE_PACK_HI, px
        )
        // Horizontalna traka
        drawLine(
            SPELE_PACK_DARK.copy(alpha = 0.55f),
            Offset(x + 2f * px, y + height * 0.52f),
            Offset(x + width - 2f * px, y + height * 0.52f),
            strokeWidth = 1.3f * px
        )
    }

    val liftFactor = (playerLift / (150f * px)).coerceIn(0f, 1f)
    val shadowW = playerW * 1.4f * (1f + liftFactor * 0.55f)
    drawOval(
        Color.Black.copy(alpha = 0.38f * (1f - liftFactor * 0.42f)),
        Offset(playerX - shadowW / 2f, groundY - 4f * px),
        Size(shadowW, 5f * px)
    )

    if (status == RunnerStatus.RUNNING && playerLift <= 1.5f) {
        val dustAlpha = if (isCrawling) 0.34f else 0.22f
        repeat(3) { d ->
            val dx = left - (10f + d * 11f) * px - abs(runCycle) * 3f * px
            val dy = groundY - (3f + d * 2.2f) * px
            drawOval(
                runnerSierraPalette(biomeIndex).highlight.copy(alpha = dustAlpha - d * 0.07f),
                Offset(dx, dy),
                Size((10f + d * 3f) * px, (4f + d) * px)
            )
        }
    }

    val isSlidingOnIce = status == RunnerStatus.RUNNING && speedBoostTimer > 0f && playerLift <= 1.5f
    if (isSlidingOnIce) {
        val slidePulse = sin(stepTime * 11.0f)
        val slideWobble = sin(stepTime * 6.5f) * 1.8f * px

        val feetY = groundY - playerLift

        // Compact butt-slide pivot: keep the whole pose close to normal runner width.
        val hipX = playerX - 4f * px + slideWobble * 0.3f
        val hipY = feetY - 14f * px
        val shoulderX = playerX - 18f * px + slideWobble
        val shoulderY = hipY - 26f * px
        val headX = shoulderX + 8f * px + slideWobble * 0.5f
        val headY = shoulderY - 14f * px + slidePulse * 1.2f * px

        // Shadow is now close to the standing character footprint, not a giant sled shadow.
        val shadowW = 54f * px
        drawOval(
            Color.Black.copy(alpha = 0.26f),
            Offset(playerX - shadowW * 0.45f, groundY - 4f * px),
            Size(shadowW, 7f * px)
        )

        val lampCone = Path().apply {
            moveTo(headX + 12f * px, headY - 3f * px)
            lineTo(headX + 140f * px, headY - 32f * px)
            lineTo(headX + 140f * px, headY + 28f * px)
            close()
        }
        val lampSoft = Path().apply {
            moveTo(headX + 12f * px, headY - 3f * px)
            lineTo(headX + 158f * px, headY - 56f * px)
            lineTo(headX + 158f * px, headY + 54f * px)
            close()
        }
        drawPath(lampSoft, Color(0xFFFFF59D).copy(alpha = 0.07f))
        drawPath(lampCone, Color(0xFFFFF59D).copy(alpha = 0.22f + sin(stepTime * 10f) * 0.04f))

        // Small spray behind the boots only, so the slide reads as motion without stretching the body.
        repeat(4) { sIdx ->
            val sprayX = playerX + (8f + sIdx * 9f) * px + abs(slidePulse) * 2.5f * px
            val sprayY = feetY - (1f + sIdx % 2 * 2f) * px
            drawLine(
                Color(0xFFB3E5FC).copy(alpha = 0.42f - sIdx * 0.09f),
                Offset(sprayX, sprayY),
                Offset(sprayX + (10f + sIdx * 3f) * px, sprayY - (1f + sIdx) * px),
                strokeWidth = (1.1f - sIdx * 0.18f) * px
            )
        }
        repeat(3) { sp ->
            val iceX = playerX + (4f + sp * 12f) * px
            val iceY = feetY - 2f * px
            drawLine(SPELE_INK, Offset(iceX - 3f * px, iceY + 2f * px), Offset(iceX + 4f * px, iceY - 5f * px), strokeWidth = 1.0f * px)
            drawLine(Color(0xFFE1F5FE).copy(alpha = 0.60f), Offset(iceX - 3f * px, iceY + 2f * px), Offset(iceX + 4f * px, iceY - 5f * px), strokeWidth = 0.7f * px)
        }

        val bKneeX = playerX + 18f * px
        val bKneeY = feetY - 6f * px - slidePulse * 1.0f * px
        val bFootX = playerX + 30f * px
        val bFootY = feetY + 1f * px
        drawSierraLimb(
            Offset(hipX + 2f * px, hipY + 2f * px),
            Offset(bKneeX, bKneeY),
            Offset(bFootX, bFootY),
            5.0f * px,
            SPELE_SUIT_DARK.copy(alpha = 0.72f),
            SPELE_SUIT_MID.copy(alpha = 0.72f),
            SPELE_SUIT_HI.copy(alpha = 0.72f),
            px
        )
        drawRoundRect(SPELE_BOOT_DARK.copy(alpha = 0.50f), Offset(bFootX - 3f * px, bFootY - 1f * px), Size(12f * px, 6f * px), CornerRadius(3f * px))
        drawRoundRect(SPELE_BOOT_MID.copy(alpha = 0.75f), Offset(bFootX - 4f * px, bFootY - 5f * px), Size(13f * px, 7f * px), CornerRadius(3.5f * px))
        drawRoundRect(SPELE_INK.copy(alpha = 0.70f), Offset(bFootX - 4f * px, bFootY - 5f * px), Size(13f * px, 7f * px), CornerRadius(3.5f * px), style = Stroke(width = 1.3f * px))

        val fKneeX = playerX + 22f * px
        val fKneeY = feetY - 10f * px + slidePulse * 1.5f * px
        val fFootX = playerX + 36f * px
        val fFootY = feetY
        drawSierraLimb(
            Offset(hipX, hipY),
            Offset(fKneeX, fKneeY),
            Offset(fFootX, fFootY),
            5.5f * px,
            SPELE_SUIT_DARK,
            SPELE_SUIT_MID,
            SPELE_SUIT_HI,
            px
        )
        drawRoundRect(SPELE_BOOT_DARK.copy(alpha = 0.55f), Offset(fFootX - 3f * px, fFootY - 1f * px), Size(14f * px, 7f * px), CornerRadius(3.5f * px))
        drawRoundRect(SPELE_BOOT_MID, Offset(fFootX - 4f * px, fFootY - 6f * px), Size(15f * px, 8f * px), CornerRadius(4f * px))
        drawRoundRect(SPELE_BOOT_HI.copy(alpha = 0.38f), Offset(fFootX - 3f * px, fFootY - 5.5f * px), Size(6f * px, 3.5f * px), CornerRadius(2.5f * px))
        drawRoundRect(SPELE_INK, Offset(fFootX - 4f * px, fFootY - 6f * px), Size(15f * px, 8f * px), CornerRadius(4f * px), style = Stroke(width = 1.5f * px))

        val torzoW = 22f * px
        val torzoH = 28f * px
        drawSierraRoundRect(
            Offset(shoulderX - torzoW * 0.4f, shoulderY),
            Size(torzoW, torzoH),
            10f * px,
            SPELE_SUIT_DARK, SPELE_SUIT_MID, SPELE_SUIT_HI,
            px
        )

        drawBackpack(shoulderX - torzoW * 0.4f - 10f * px, shoulderY + 4f * px, 11f * px, 16f * px)

        val brakeShoulderX = shoulderX + torzoW * 0.6f
        val brakeShoulderY = shoulderY + 8f * px
        val brakeElbowX = brakeShoulderX + 9f * px + slidePulse * 2f * px
        val brakeElbowY = brakeShoulderY + 9f * px
        val brakeHandX = playerX + 12f * px + slidePulse * 2.5f * px
        val brakeHandY = feetY - 2f * px
        drawSierraLimb(
            Offset(brakeShoulderX, brakeShoulderY),
            Offset(brakeElbowX, brakeElbowY),
            Offset(brakeHandX, brakeHandY),
            3.8f * px,
            SPELE_SKIN_DARK, SPELE_SKIN_MID, SPELE_SKIN_HI,
            px
        )
        drawSierraCircle(Offset(brakeHandX, brakeHandY), 3.0f * px, SPELE_SKIN_DARK, SPELE_SKIN_MID, SPELE_SKIN_HI, px)
        repeat(3) { scratch ->
            drawLine(
                Color(0xFFE1F5FE).copy(alpha = 0.48f - scratch * 0.13f),
                Offset(brakeHandX - 2f * px - scratch * 4f * px, brakeHandY),
                Offset(brakeHandX + 8f * px - scratch * 4f * px, brakeHandY - 1.5f * px),
                strokeWidth = 0.85f * px
            )
        }

        val balShoulderX = shoulderX + torzoW * 0.55f
        val balShoulderY = shoulderY + 6f * px
        val balElbowX = headX + 4f * px
        val balElbowY = headY + 5f * px + slidePulse * 2f * px
        val balHandX = headX + 16f * px
        val balHandY = headY - 4f * px + slidePulse * 3.5f * px
        drawSierraLimb(
            Offset(balShoulderX, balShoulderY),
            Offset(balElbowX, balElbowY),
            Offset(balHandX, balHandY),
            3.6f * px,
            SPELE_SKIN_DARK, SPELE_SKIN_MID, SPELE_SKIN_HI,
            px
        )
        drawSierraCircle(Offset(balHandX, balHandY), 2.8f * px, SPELE_SKIN_DARK, SPELE_SKIN_MID, SPELE_SKIN_HI, px)

        drawProfileHelmet(headX, headY, compact = false)

        return
    }

    val lampX = if (isCrawling) left + playerW * 0.92f else playerX + 18f * px
    val lampY = if (isCrawling) top + 9f * px else top + 8f * px
    // Sierra VGA flat headlamp cone — drawn before the body so the character stays readable.
    val light = Path().apply {
        moveTo(lampX, lampY)
        lineTo(lampX + 160f * px, lampY - 42f * px)
        lineTo(lampX + 160f * px, lampY + 66f * px)
        close()
    }
    drawPath(light, Color(0xFFFFF59D).copy(alpha = 0.28f))
    drawPath(light, Color.Black.copy(alpha = 0.42f), style = Stroke(width = 1.0f * px))

    if (isCrawling) {
        if ((stepTime % 0.3f) < 0.15f) {
            repeat(3) { i ->
                val r = (2f + i) * px
                val offset = (8f + i * 7f) * px
                drawCircle(
                    Color(0xFFBCAAA4).copy(alpha = 0.55f),
                    radius = r,
                    center = Offset(playerX - offset, groundY - 4f * px - i * 0.6f * px)
                )
                drawCircle(
                    Color(0xFFBCAAA4).copy(alpha = 0.55f),
                    radius = r,
                    center = Offset(playerX + offset, groundY - 4f * px - i * 0.6f * px)
                )
            }
        }
        // Human crawl pose: hips stay higher, shoulders/head are low and forward, diagonal limbs alternate.
        val crawlPhase = sin(stepTime * 9.8f)
        val pairA = crawlPhase
        val pairB = -crawlPhase
        val spineY = groundY - 20f * px + crawlPhase * 3.5f * px
        val bodyLeft = left + 1f * px
        val bodyW = playerW * 1.16f
        val hipY = spineY - 18f * px
        val shoulderY = spineY - 8f * px
        val helmetCenter = Offset(
            left + playerW * 0.95f,
            spineY - 6f * px + crawlPhase * 2f * px
        )

        val crawlBody = Path().apply {
            // Slanted torso: rear/hips higher, front/shoulders lower.
            moveTo(bodyLeft + 2f * px, hipY - 4f * px)
            cubicTo(bodyLeft + bodyW * 0.24f, hipY - 13f * px, bodyLeft + bodyW * 0.58f, shoulderY - 10f * px, bodyLeft + bodyW, shoulderY - 2f * px)
            cubicTo(bodyLeft + bodyW + 5f * px, shoulderY + 12f * px, bodyLeft + bodyW * 0.72f, spineY + 8f * px, bodyLeft + 14f * px, spineY + 7f * px)
            cubicTo(bodyLeft - 5f * px, spineY + 2f * px, bodyLeft - 6f * px, hipY + 3f * px, bodyLeft + 2f * px, hipY - 4f * px)
            close()
        }
        drawSierraShape(crawlBody, SPELE_SUIT_DARK, SPELE_SUIT_MID, SPELE_SUIT_HI, px)
        drawBackpack(bodyLeft + 3f * px, hipY - 8f * px, 15f * px, 18f * px)
        drawLine(Color(0xFF263238), Offset(bodyLeft + bodyW * 0.42f, hipY - 3f * px), Offset(bodyLeft + bodyW * 0.52f, spineY + 7f * px), strokeWidth = 2.0f * px)

        fun crawlLimb(shoulder: Offset, elbow: Offset, hand: Offset, back: Boolean) {
            val alpha = if (back) 0.76f else 1f
            drawSierraLimb(shoulder, elbow, hand, 3.8f * px, SPELE_SKIN_DARK.copy(alpha = alpha), SPELE_SKIN_MID.copy(alpha = alpha), SPELE_SKIN_HI.copy(alpha = alpha), px)
            drawSierraCircle(hand, 3.2f * px, SPELE_SKIN_DARK.copy(alpha = alpha), SPELE_SKIN_MID.copy(alpha = alpha), SPELE_SKIN_HI.copy(alpha = alpha), px)
        }
        fun crawlLeg(hip: Offset, knee: Offset, foot: Offset, back: Boolean) {
            val alpha = if (back) 0.76f else 1f
            drawSierraLimb(hip, knee, foot, 5.0f * px, SPELE_SUIT_DARK.copy(alpha = alpha), SPELE_SUIT_MID.copy(alpha = alpha), SPELE_SUIT_HI.copy(alpha = alpha), px)
            drawLine(SPELE_BOOT_MID.copy(alpha = alpha), Offset(foot.x - 7f * px, foot.y), Offset(foot.x + 10f * px, foot.y - 1.5f * px), strokeWidth = 5.8f * px, cap = StrokeCap.Round)
            drawLine(SPELE_INK.copy(alpha = 0.35f * alpha), Offset(foot.x - 7f * px, foot.y), Offset(foot.x + 10f * px, foot.y - 1.5f * px), strokeWidth = 5.8f * px, cap = StrokeCap.Round)
        }

        val lHandReach = pairA * 18f * px
        val rHandReach = pairB * 18f * px
        val rKneeForward = pairA * 14f * px
        val lKneeForward = pairB * 14f * px
        val rKneeY = groundY - (8f + abs(pairA) * 12f) * px
        val lKneeY = groundY - (8f + abs(pairB) * 12f) * px

        // Far pair first: right arm + left leg.
        val rElbow = Offset(bodyLeft + bodyW * 0.64f, shoulderY + 13f * px)
        val rHand = Offset(bodyLeft + bodyW * 0.52f + rHandReach, groundY - 2f * px)
        crawlLimb(Offset(bodyLeft + bodyW * 0.62f, shoulderY), rElbow, rHand, back = true)
        val lKnee = Offset(bodyLeft + bodyW * 0.24f + lKneeForward, lKneeY)
        val lFoot = Offset(bodyLeft + bodyW * 0.16f + lKneeForward, groundY)
        crawlLeg(Offset(bodyLeft + bodyW * 0.25f, hipY + 10f * px), lKnee, lFoot, back = true)

        // Near diagonal pair: left arm + right leg, drawn on top.
        val lElbow = Offset(bodyLeft + bodyW * 0.70f, shoulderY + 12f * px)
        val lHand = Offset(bodyLeft + bodyW * 0.90f + lHandReach, groundY - 2f * px)
        crawlLimb(Offset(bodyLeft + bodyW * 0.78f, shoulderY + 1f * px), lElbow, lHand, back = false)
        val rKnee = Offset(bodyLeft + bodyW * 0.55f + rKneeForward, rKneeY)
        val rFoot = Offset(bodyLeft + bodyW * 0.65f + rKneeForward, groundY)
        crawlLeg(Offset(bodyLeft + bodyW * 0.38f, hipY + 10f * px), rKnee, rFoot, back = false)

        drawProfileHelmet(helmetCenter.x, helmetCenter.y, compact = true)
        if (depthMeters > 60f && biomeIndex % 5 == 4) repeat(3) { f ->
            drawCircle(Color.White.copy(alpha = 0.55f), radius = 1.0f * px, center = Offset(helmetCenter.x + (-9f + f * 5f) * px, helmetCenter.y - (8f - f) * px))
        }
        repeat(3) { m ->
            val mx = left - (10f + m * 13f + abs(crawlPhase) * 6f) * px
            drawLine(Color(0xFFD7CCC8).copy(alpha = 0.20f - m * 0.04f), Offset(mx, groundY - (2f + m) * px), Offset(mx + 16f * px, groundY - (4f + m) * px), strokeWidth = 1.1f * px)
        }
        // Knee dust makes contact with the floor obvious during crawl.
        val kneeX = bodyLeft + bodyW * 0.40f
        drawOval(Color(0xFFD7CCC8).copy(alpha = 0.28f), Offset(kneeX - abs(crawlPhase) * 8f * px, groundY - 3f * px), Size(8f * px, 4f * px))
    } else {
        val bodyTop = top + playerH * 0.34f
        val bodyLeft = left + 5f * px
        val bodyW = playerW * 0.78f
        val bodyH = playerH * 0.56f
        drawSierraRoundRect(Offset(bodyLeft, bodyTop), Size(bodyW, bodyH), 16f * px, SPELE_SUIT_DARK, SPELE_SUIT_MID, SPELE_SUIT_HI, px)
        drawRoundRect(
            Color.Black,
            Offset(bodyLeft, bodyTop),
            Size(bodyW, bodyH),
            CornerRadius(16f * px),
            style = Stroke(width = 2.0f * px)
        )
        if (depthMeters > 60f && biomeIndex % 5 == 1) drawRoundRect(Color(0xFF00BCD4).copy(alpha = 0.12f), Offset(bodyLeft + 2f * px, bodyTop + 2f * px), Size(bodyW - 4f * px, bodyH - 6f * px), CornerRadius(15f * px))
        if (depthMeters > 60f && biomeIndex % 5 == 3) drawLine(Color(0xFFFF6D00).copy(alpha = 0.42f), Offset(bodyLeft + 6f * px, bodyTop + 6f * px), Offset(bodyLeft + bodyW - 6f * px, bodyTop + bodyH - 9f * px), strokeWidth = 1.0f * px)
        drawRoundRect(SPELE_SUIT_HI.copy(alpha = 0.45f), Offset(bodyLeft + 6f * px, bodyTop + 4f * px), Size(bodyW * 0.20f, bodyH * 0.64f), CornerRadius(8f * px))
        drawLine(Color(0xFF263238), Offset(bodyLeft + bodyW * 0.50f, bodyTop + 4f * px), Offset(bodyLeft + bodyW * 0.50f, bodyTop + bodyH - 5f * px), strokeWidth = 2.3f * px)
        drawLine(Color(0xFF263238), Offset(bodyLeft + 7f * px, bodyTop + 11f * px), Offset(bodyLeft + bodyW - 5f * px, bodyTop + bodyH - 6f * px), strokeWidth = 2.2f * px)
        drawLine(Color(0xFF263238), Offset(bodyLeft + bodyW - 7f * px, bodyTop + 11f * px), Offset(bodyLeft + 10f * px, bodyTop + bodyH - 5f * px), strokeWidth = 2.2f * px)
        drawRoundRect(Color(0xFF616161), Offset(playerX - 8f * px, bodyTop + bodyH - 4f * px), Size(18f * px, 6f * px), CornerRadius(4f * px))
        drawBackpack(bodyLeft - 4f * px, bodyTop + 9f * px, 13f * px, 23f * px)
        drawProfileHelmet(playerX, top + 17f * px)
        if (depthMeters > 60f && biomeIndex % 5 == 4) repeat(3) { f -> drawCircle(Color.White.copy(alpha = 0.55f), radius = 1.0f * px, center = Offset(playerX + (-7f + f * 5f) * px, top + (10f + f) * px)) }
        drawLine(SPELE_INK.copy(alpha = 0.72f), Offset(playerX - 4f * px, top + 25f * px), Offset(bodyLeft + 6f * px, bodyTop + 10f * px), strokeWidth = 2.7f * px)
        drawSierraLimb(Offset(bodyLeft + 4f * px, bodyTop + 16f * px), Offset(bodyLeft - 3f * px, bodyTop + 23f * px + armSwing * 0.5f), Offset(left - 12f * px, bodyTop + 30f * px + armSwing), 4.0f * px, SPELE_SKIN_DARK, SPELE_SKIN_MID, SPELE_SKIN_HI, px)
        drawSierraLimb(Offset(bodyLeft + bodyW - 6f * px, bodyTop + 16f * px), Offset(bodyLeft + bodyW + 4f * px, bodyTop + 23f * px - armSwing * 0.5f), Offset(left + playerW + 12f * px, bodyTop + 29f * px - armSwing), 4.0f * px, SPELE_SKIN_DARK, SPELE_SKIN_MID, SPELE_SKIN_HI, px)
        val leftFoot = if (isAirborne) {
            Offset(playerX - 20f * px, feetY - jumpTuck * 0.85f)
        } else {
            Offset(playerX - 14f * px - legSwing, feetY)
        }
        val rightFoot = if (isAirborne) {
            Offset(playerX + 24f * px, feetY - jumpTuck * 0.45f)
        } else {
            Offset(playerX + 15f * px + legSwing, feetY)
        }
        drawSierraLimb(Offset(playerX - 6f * px, bodyTop + bodyH - 1f * px), Offset((playerX - 6f * px + leftFoot.x) * 0.50f, (bodyTop + bodyH - 1f * px + leftFoot.y) * 0.50f), leftFoot, 4.8f * px, SPELE_SUIT_DARK, SPELE_SUIT_MID, SPELE_SUIT_HI, px)
        drawSierraLimb(Offset(playerX + 6f * px, bodyTop + bodyH - 1f * px), Offset((playerX + 6f * px + rightFoot.x) * 0.50f, (bodyTop + bodyH - 1f * px + rightFoot.y) * 0.50f), rightFoot, 4.8f * px, SPELE_SUIT_DARK, SPELE_SUIT_MID, SPELE_SUIT_HI, px)

        // Sierra VGA boots: chunky flat fills, top-left highlight, dark contact shadow.
        // LIJEVA ČIZMA
        drawRoundRect(
            SPELE_BOOT_DARK.copy(alpha = 0.55f),
            Offset(leftFoot.x - 4f * px, leftFoot.y - 1f * px), Size(17f * px, 7f * px), CornerRadius(4f * px)
        )
        drawRoundRect(
            SPELE_BOOT_MID,
            Offset(leftFoot.x - 5f * px, leftFoot.y - 6.5f * px), Size(17f * px, 8f * px), CornerRadius(4f * px)
        )
        drawRoundRect(
            SPELE_BOOT_HI.copy(alpha = 0.40f),
            Offset(leftFoot.x - 4f * px, leftFoot.y - 6f * px), Size(7f * px, 4f * px), CornerRadius(3f * px)
        )
        drawRoundRect(
            SPELE_INK,
            Offset(leftFoot.x - 5f * px, leftFoot.y - 6.5f * px), Size(17f * px, 8f * px), CornerRadius(4f * px),
            style = Stroke(width = 1.6f * px)
        )

        // DESNA ČIZMA — isti pattern
        drawRoundRect(
            SPELE_BOOT_DARK.copy(alpha = 0.55f),
            Offset(rightFoot.x - 5f * px, rightFoot.y - 1f * px), Size(17f * px, 7f * px), CornerRadius(4f * px)
        )
        drawRoundRect(
            SPELE_BOOT_MID,
            Offset(rightFoot.x - 7f * px, rightFoot.y - 6.5f * px), Size(17f * px, 8f * px), CornerRadius(4f * px)
        )
        drawRoundRect(
            SPELE_BOOT_HI.copy(alpha = 0.40f),
            Offset(rightFoot.x - 6f * px, rightFoot.y - 6f * px), Size(7f * px, 4f * px), CornerRadius(3f * px)
        )
        drawRoundRect(
            SPELE_INK,
            Offset(rightFoot.x - 7f * px, rightFoot.y - 6.5f * px), Size(17f * px, 8f * px), CornerRadius(4f * px),
            style = Stroke(width = 1.6f * px)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRopeSpeleologist(
    playerX: Float,
    playerY: Float,
    px: Float,
    depthMeters: Float = 0f,
    biomeIndex: Int = 0
) {
    val sierra = runnerSierraPalette(biomeIndex)
    drawLine(sierra.highlight, Offset(playerX, playerY - 58f * px), Offset(playerX, playerY + 80f * px), strokeWidth = 3.8f * px)

    val helmetCenter = Offset(playerX, playerY - 24f * px)
    drawSierraCircle(helmetCenter, 16.4f * px, SPELE_HELMET_DARK, SPELE_HELMET_MID, SPELE_HELMET_HI, px)
    drawSierraRoundRect(Offset(playerX - 18.5f * px, playerY - 35.5f * px), Size(37f * px, 10.5f * px), 8f * px, SPELE_HELMET_DARK, SPELE_HELMET_MID, SPELE_HELMET_HI, px)
    drawSierraRoundRect(Offset(playerX + 4.5f * px, playerY - 29f * px), Size(9.5f * px, 9f * px), 3f * px, SPELE_HELMET_DARK, Color(0xFF455A64), Color(0xFF90A4AE), px)
    drawCircle(Color.White.copy(alpha = 0.95f), radius = 3.5f * px, center = Offset(playerX + 11f * px, playerY - 25f * px))
    val bodyTop = playerY - 4f * px
    drawSierraRoundRect(Offset(playerX - 16f * px, bodyTop), Size(32f * px, 42f * px), 14f * px, SPELE_SUIT_DARK, SPELE_SUIT_MID, SPELE_SUIT_HI, px)
    if (depthMeters > 60f && biomeIndex % 5 == 1) drawRoundRect(Color(0xFF00BCD4).copy(alpha = 0.12f), Offset(playerX - 14f * px, bodyTop + 2f * px), Size(28f * px, 36f * px), CornerRadius(12f * px))
    if (depthMeters > 60f && biomeIndex % 5 == 3) drawLine(Color(0xFFFF6D00).copy(alpha = 0.42f), Offset(playerX - 10f * px, bodyTop + 7f * px), Offset(playerX + 10f * px, bodyTop + 30f * px), strokeWidth = 1.0f * px)
    if (depthMeters > 60f && biomeIndex % 5 == 4) repeat(3) { f -> drawCircle(Color.White.copy(alpha = 0.50f), radius = 0.9f * px, center = Offset(playerX + (-8f + f * 5f) * px, playerY - 32f * px + f * 2f * px)) }
    drawRoundRect(SPELE_SUIT_HI.copy(alpha = 0.42f), Offset(playerX - 10f * px, bodyTop + 4f * px), Size(7f * px, 22f * px), CornerRadius(5f * px))
    drawRoundRect(SPELE_PACK_DARK, Offset(playerX - 11f * px, bodyTop + 9f * px), Size(22f * px, 23f * px), CornerRadius(7f * px))
    drawRoundRect(SPELE_PACK_MID, Offset(playerX - 7f * px, bodyTop + 12f * px), Size(14f * px, 14f * px), CornerRadius(5f * px))
    drawLine(Color(0xFF263238), Offset(playerX - 15f * px, bodyTop + 18f * px), Offset(playerX + 15f * px, bodyTop + 18f * px), strokeWidth = 3f * px)
    drawLine(Color(0xFF263238), Offset(playerX - 12f * px, bodyTop + 8f * px), Offset(playerX + 10f * px, bodyTop + 30f * px), strokeWidth = 2.1f * px)
    drawLine(Color(0xFF263238), Offset(playerX + 12f * px, bodyTop + 8f * px), Offset(playerX - 10f * px, bodyTop + 30f * px), strokeWidth = 2.1f * px)
    drawRoundRect(Color(0xFF616161), Offset(playerX - 9f * px, bodyTop + 30f * px), Size(18f * px, 6f * px), CornerRadius(4f * px))

    drawSierraLimb(Offset(playerX - 11f * px, playerY + 5f * px), Offset(playerX - 7f * px, playerY - 4f * px), Offset(playerX - 3f * px, playerY - 15f * px), 4.0f * px, SPELE_SKIN_DARK, SPELE_SKIN_MID, SPELE_SKIN_HI, px)
    drawSierraLimb(Offset(playerX + 11f * px, playerY + 5f * px), Offset(playerX + 7f * px, playerY - 4f * px), Offset(playerX + 3f * px, playerY - 15f * px), 4.0f * px, SPELE_SKIN_DARK, SPELE_SKIN_MID, SPELE_SKIN_HI, px)
    drawCircle(SPELE_SKIN_HI, radius = 3.5f * px, center = Offset(playerX - 3f * px, playerY - 15f * px))
    drawCircle(SPELE_SKIN_HI, radius = 3.5f * px, center = Offset(playerX + 3f * px, playerY - 15f * px))

    drawSierraLimb(Offset(playerX - 7f * px, playerY + 34f * px), Offset(playerX - 12f * px, playerY + 45f * px), Offset(playerX - 17f * px, playerY + 58f * px), 4.7f * px, SPELE_SUIT_DARK, SPELE_SUIT_MID, SPELE_SUIT_HI, px)
    drawSierraLimb(Offset(playerX + 7f * px, playerY + 34f * px), Offset(playerX + 12f * px, playerY + 45f * px), Offset(playerX + 17f * px, playerY + 58f * px), 4.7f * px, SPELE_SUIT_DARK, SPELE_SUIT_MID, SPELE_SUIT_HI, px)
    drawLine(SPELE_BOOT_MID, Offset(playerX - 20f * px, playerY + 60f * px), Offset(playerX - 9f * px, playerY + 60f * px), strokeWidth = 5.8f * px)
    drawLine(SPELE_BOOT_MID, Offset(playerX + 9f * px, playerY + 60f * px), Offset(playerX + 20f * px, playerY + 60f * px), strokeWidth = 5.8f * px)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpeleologistAt(
    playerX: Float,
    playerY: Float,
    px: Float,
    status: RunnerStatus
) {
    val bounce = if (status == RunnerStatus.RUNNING) 1.6f * px else 0f
    val bodyTop = playerY + 1f * px - bounce
    val helmetCenter = Offset(playerX, playerY - 16f * px - bounce)

    drawRoundRect(Color.Black.copy(alpha = 0.18f), Offset(playerX - 13f * px, bodyTop + 3f * px), Size(30f * px, 38f * px), CornerRadius(13f * px))
    drawRoundRect(SPELE_SUIT_DARK, Offset(playerX - 15f * px, bodyTop), Size(30f * px, 38f * px), CornerRadius(13f * px))
    drawRoundRect(SPELE_SUIT_MID, Offset(playerX - 13f * px, bodyTop + 2f * px), Size(26f * px, 33f * px), CornerRadius(12f * px))
    drawRoundRect(SPELE_SUIT_HI.copy(alpha = 0.48f), Offset(playerX - 9f * px, bodyTop + 3f * px), Size(6f * px, 21f * px), CornerRadius(5f * px))
    drawRoundRect(SPELE_PACK_DARK, Offset(playerX - 19f * px, bodyTop + 9f * px), Size(11f * px, 18f * px), CornerRadius(5f * px))
    drawRoundRect(SPELE_PACK_MID, Offset(playerX - 16f * px, bodyTop + 12f * px), Size(7f * px, 9f * px), CornerRadius(3f * px))
    drawLine(Color(0xFF263238), Offset(playerX - 13f * px, bodyTop + 12f * px), Offset(playerX + 12f * px, bodyTop + 31f * px), strokeWidth = 2f * px)
    drawLine(Color(0xFF263238), Offset(playerX + 12f * px, bodyTop + 12f * px), Offset(playerX - 11f * px, bodyTop + 31f * px), strokeWidth = 2f * px)
    drawRoundRect(Color(0xFF616161), Offset(playerX - 8f * px, bodyTop + 28f * px), Size(16f * px, 5f * px), CornerRadius(4f * px))

    drawCircle(SPELE_HELMET_MID, radius = 15.5f * px, center = helmetCenter)
    drawCircle(SPELE_HELMET_HI.copy(alpha = 0.58f), radius = 11f * px, center = Offset(helmetCenter.x - 2.2f * px, helmetCenter.y - 3.5f * px))
    drawRoundRect(SPELE_HELMET_MID, Offset(helmetCenter.x - 17.5f * px, helmetCenter.y - 13f * px), Size(35f * px, 10f * px), CornerRadius(8f * px))
    drawRoundRect(Color(0xFF455A64), Offset(helmetCenter.x + 4f * px, helmetCenter.y - 7f * px), Size(9f * px, 9f * px), CornerRadius(3f * px))
    drawCircle(Color.White.copy(alpha = 0.94f), radius = 4.1f * px, center = Offset(helmetCenter.x + 10f * px, helmetCenter.y - 4.2f * px))
    drawOval(SPELE_SKIN_MID, Offset(helmetCenter.x - 5f * px, helmetCenter.y + 4f * px), Size(11f * px, 10f * px))
    drawCircle(Color(0xFF263238), radius = 1.6f * px, center = Offset(helmetCenter.x + 8.8f * px, helmetCenter.y - 2.2f * px))
    drawLine(Color(0xFFBF360C), Offset(helmetCenter.x + 5f * px, helmetCenter.y + 7.6f * px), Offset(helmetCenter.x + 11f * px, helmetCenter.y + 7f * px), strokeWidth = 1.3f * px)

    drawLine(SPELE_PACK_DARK, Offset(playerX - 10f * px, bodyTop + 14f * px), Offset(playerX - 26f * px, playerY + 31f * px), strokeWidth = 5.5f * px)
    drawLine(SPELE_SKIN_MID, Offset(playerX - 10f * px, bodyTop + 14f * px), Offset(playerX - 26f * px, playerY + 31f * px), strokeWidth = 4.0f * px)
    drawLine(SPELE_PACK_DARK, Offset(playerX + 10f * px, bodyTop + 14f * px), Offset(playerX + 26f * px, playerY + 29f * px), strokeWidth = 5.5f * px)
    drawLine(SPELE_SKIN_MID, Offset(playerX + 10f * px, bodyTop + 14f * px), Offset(playerX + 26f * px, playerY + 29f * px), strokeWidth = 4.0f * px)
    drawCircle(SPELE_SKIN_HI, radius = 3f * px, center = Offset(playerX - 26f * px, playerY + 31f * px))
    drawCircle(SPELE_SKIN_HI, radius = 3f * px, center = Offset(playerX + 26f * px, playerY + 29f * px))
    drawLine(SPELE_SUIT_DARK, Offset(playerX - 6f * px, bodyTop + 34f * px), Offset(playerX - 15f * px, playerY + 55f * px), strokeWidth = 6.0f * px)
    drawLine(SPELE_SUIT_MID, Offset(playerX - 6f * px, bodyTop + 34f * px), Offset(playerX - 15f * px, playerY + 55f * px), strokeWidth = 4.7f * px)
    drawLine(SPELE_SUIT_DARK, Offset(playerX + 6f * px, bodyTop + 34f * px), Offset(playerX + 15f * px, playerY + 55f * px), strokeWidth = 6.0f * px)
    drawLine(SPELE_SUIT_MID, Offset(playerX + 6f * px, bodyTop + 34f * px), Offset(playerX + 15f * px, playerY + 55f * px), strokeWidth = 4.7f * px)
    drawLine(SPELE_BOOT_MID, Offset(playerX - 18f * px, playerY + 57f * px), Offset(playerX - 7f * px, playerY + 57f * px), strokeWidth = 5.5f * px)
    drawLine(SPELE_BOOT_MID, Offset(playerX + 7f * px, playerY + 57f * px), Offset(playerX + 18f * px, playerY + 57f * px), strokeWidth = 5.5f * px)
}
