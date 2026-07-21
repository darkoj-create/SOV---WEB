@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.darko.speleov1

import android.content.Context
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darko.speleov1.util.ElevationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.util.Locale
import kotlin.math.roundToInt

private enum class ElevationProfileSource { GPS, DEM }

private data class ElevationProfilePoint(
    val distanceM: Double,
    val elevationM: Double
)

private data class ElevationProfileData(
    val points: List<ElevationProfilePoint>,
    val minElevationM: Double,
    val maxElevationM: Double,
    val ascentM: Double,
    val distanceM: Double,
    val validCount: Int,
    val source: ElevationProfileSource
)

/**
 * Jednostavna obrada visinskog profila bez vanjskih chart biblioteka.
 * DEM se koristi samo kao podatkovni izvor, nikad kao vizualni sloj karte.
 */
private object TrackElevationProfiler {
    private const val MAX_SAMPLES = 200

    suspend fun build(context: Context, track: SavedTrack, source: ElevationProfileSource): ElevationProfileData? = withContext(Dispatchers.IO) {
        val sampled = sampleTrack(track.points, MAX_SAMPLES)
        if (sampled.size < 2) return@withContext null
        val distances = cumulativeDistances(sampled.map { it.point })
        val elevations = when (source) {
            ElevationProfileSource.GPS -> sampled.map { it.altitudeM }
            ElevationProfileSource.DEM -> {
                val hasCachedDem = sampled.any { it.demAltitudeM != null }
                if (hasCachedDem) {
                    // Use pre-recorded DEM altitudes, fall back to live lookup for missing points
                    val repo = ElevationRepository(context)
                    sampled.map { pt ->
                        pt.demAltitudeM ?: repo.elevationAtBlocking(pt.point.latitude, pt.point.longitude)
                    }
                } else {
                    ElevationRepository(context).elevationsFor(sampled.map { it.point })
                }
            }
        }
        val profilePoints = sampled.indices.mapNotNull { index ->
            val h = elevations.getOrNull(index) ?: return@mapNotNull null
            ElevationProfilePoint(distances.getOrElse(index) { 0.0 }, h)
        }
        if (profilePoints.size < 2) return@withContext null
        val heights = profilePoints.map { it.elevationM }
        var ascent = 0.0
        profilePoints.zipWithNext().forEach { (a, b) ->
            val delta = b.elevationM - a.elevationM
            // Mali šum ignoriramo da ukupni uspon ne divlja.
            if (delta >= 2.0) ascent += delta
        }
        ElevationProfileData(
            points = profilePoints,
            minElevationM = heights.minOrNull() ?: 0.0,
            maxElevationM = heights.maxOrNull() ?: 0.0,
            ascentM = ascent,
            distanceM = distances.lastOrNull() ?: 0.0,
            validCount = profilePoints.size,
            source = source
        )
    }

    private fun sampleTrack(points: List<TrackPoint>, maxSamples: Int): List<TrackPoint> {
        if (points.size <= maxSamples) return points
        val last = points.lastIndex
        val out = ArrayList<TrackPoint>(maxSamples)
        for (i in 0 until maxSamples) {
            val index = ((i.toDouble() / (maxSamples - 1).toDouble()) * last).roundToInt().coerceIn(0, last)
            val value = points[index]
            if (out.lastOrNull()?.point != value.point) out += value
        }
        return out
    }

    private fun cumulativeDistances(points: List<GeoPoint>): List<Double> {
        if (points.isEmpty()) return emptyList()
        val out = ArrayList<Double>(points.size)
        val results = FloatArray(1)
        var total = 0.0
        out += 0.0
        points.zipWithNext().forEach { (a, b) ->
            Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
            total += results.firstOrNull()?.toDouble() ?: 0.0
            out += total
        }
        return out
    }
}

@Composable
fun TrackElevationProfileDialog(
    track: SavedTrack,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val hasGpsElevations = remember(track.id, track.points) { track.points.count { it.altitudeM != null } >= 2 }
    var source by remember(track.id) { mutableStateOf(if (hasGpsElevations) ElevationProfileSource.GPS else ElevationProfileSource.DEM) }
    var loading by remember(track.id, source) { mutableStateOf(true) }
    var profile by remember(track.id, source) { mutableStateOf<ElevationProfileData?>(null) }

    LaunchedEffect(track.id, source) {
        loading = true
        profile = TrackElevationProfiler.build(context, track, source)
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Terrain, contentDescription = null, tint = Color(0xFFE6C36A))
                Text(language.pick("Visinski profil", "Elevation profile"))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(track.name.ifBlank { language.pick("Track", "Track") }, fontWeight = FontWeight.SemiBold)
                if (hasGpsElevations) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = source == ElevationProfileSource.GPS,
                            onClick = { source = ElevationProfileSource.GPS },
                            label = { Text(language.pick("GPS visine", "GPS elevations")) }
                        )
                        FilterChip(
                            selected = source == ElevationProfileSource.DEM,
                            onClick = { source = ElevationProfileSource.DEM },
                            label = { Text(language.pick("DEM visine", "DEM elevations")) }
                        )
                    }
                    Text(
                        language.pick(
                            "GPS visine znaju šumiti; DEM je stabilniji za profil terena.",
                            "GPS elevations can be noisy; DEM is steadier for terrain profile."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AssistChip(onClick = {}, label = { Text(language.pick("DEM visine", "DEM elevations")) })
                }

                when {
                    loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Text(language.pick("Učitavam visine…", "Loading elevations…"))
                        }
                    }
                    profile == null -> {
                        Text(
                            language.pick(
                                "Visinski profil nije dostupan. Za DEM treba mreža ili offline DEM paket.",
                                "Elevation profile is not available. DEM needs network or an offline DEM package."
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        val data = profile!!
                        ElevationProfileCanvas(data)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ProfileMetric(language.pick("Min", "Min"), "${data.minElevationM.roundToInt()} m", Modifier.weight(1f))
                            ProfileMetric(language.pick("Max", "Max"), "${data.maxElevationM.roundToInt()} m", Modifier.weight(1f))
                            ProfileMetric(language.pick("Uspon", "Ascent"), "${data.ascentM.roundToInt()} m", Modifier.weight(1f))
                        }
                        Text(
                            language.pick(
                                "Uzoraka: ${data.validCount} • duljina ${formatProfileDistance(data.distanceM)}",
                                "Samples: ${data.validCount} • distance ${formatProfileDistance(data.distanceM)}"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(language.pick("Zatvori", "Close")) }
        }
    )
}

@Composable
private fun ElevationProfileCanvas(data: ElevationProfileData) {
    val lineColor = if (data.source == ElevationProfileSource.DEM) Color(0xFFE6C36A) else Color(0xFF86C5FF)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val points = data.points
                if (points.size < 2) return@Canvas
                val minH = data.minElevationM
                val maxH = data.maxElevationM
                val distance = data.distanceM.coerceAtLeast(1.0)
                val hRange = (maxH - minH).coerceAtLeast(1.0)
                val padLeft = 10f
                val padTop = 10f
                val padRight = 10f
                val padBottom = 16f
                val chartW = size.width - padLeft - padRight
                val chartH = size.height - padTop - padBottom
                repeat(4) { i ->
                    val y = padTop + chartH * (i / 3f)
                    drawLine(gridColor, Offset(padLeft, y), Offset(padLeft + chartW, y), strokeWidth = 1f)
                }
                var previous: Offset? = null
                points.forEach { p ->
                    val x = padLeft + ((p.distanceM / distance).toFloat().coerceIn(0f, 1f) * chartW)
                    val y = padTop + ((1f - ((p.elevationM - minH) / hRange).toFloat().coerceIn(0f, 1f)) * chartH)
                    val current = Offset(x, y)
                    previous?.let { drawLine(lineColor, it, current, strokeWidth = 4f, cap = StrokeCap.Round) }
                    previous = current
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${data.minElevationM.roundToInt()} m", style = MaterialTheme.typography.labelSmall, color = textColor)
                Text(formatProfileDistance(data.distanceM), style = MaterialTheme.typography.labelSmall, color = textColor)
                Text("${data.maxElevationM.roundToInt()} m", style = MaterialTheme.typography.labelSmall, color = textColor)
            }
        }
    }
}

@Composable
private fun ProfileMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label.uppercase(Locale.ROOT), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatProfileDistance(distanceM: Double): String = if (distanceM >= 1000.0) {
    String.format(Locale.US, "%.1f km", distanceM / 1000.0)
} else {
    "${distanceM.roundToInt()} m"
}
