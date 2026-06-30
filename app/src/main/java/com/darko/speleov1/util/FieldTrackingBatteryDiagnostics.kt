package com.darko.speleov1.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.roundToInt

internal data class FieldTrackingBatteryDiagnostics(
    val batteryPct: Int = -1,
    val isCharging: Boolean = false,
    val temperatureC: Double? = null,
    val voltageMv: Int? = null,
    val trackingActive: Boolean = false,
    val trackingMode: String = "lite",
    val tripTitle: String = "",
    val startedAtMillis: Long = 0L,
    val elapsedMillis: Long = 0L,
    val startBatteryPct: Int = -1,
    val stopBatteryPct: Int = -1,
    val queuedPoints: Int = 0,
    val syncedPoints: Int = 0,
    val lastPointAtMillis: Long = 0L,
    val lastSyncAtMillis: Long = 0L,
    val consumptionPctPerHour: Double? = null,
    val hoursTo15Pct: Double? = null,
    val recommendation: String = ""
)

internal object FieldTrackingBatteryDiagnosticsStore {
    fun read(context: Context): FieldTrackingBatteryDiagnostics {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val currentPct = readBatteryPct(context, batteryIntent)
        val isCharging = batteryIntent?.let { intent ->
            when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING,
                BatteryManager.BATTERY_STATUS_FULL -> true
                else -> false
            }
        } ?: false
        val temperatureC = batteryIntent
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?.takeIf { it != Int.MIN_VALUE && it > -500 && it < 1000 }
            ?.let { it / 10.0 }
        val voltageMv = batteryIntent
            ?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Int.MIN_VALUE)
            ?.takeIf { it != Int.MIN_VALUE && it > 0 }

        val state = FieldTrackingLitePrefs.load(context)
        val now = System.currentTimeMillis()
        val started = state.trackingStartedAtMillis.takeIf { it > 0L }
        val elapsed = started?.let { (now - it).coerceAtLeast(0L) } ?: 0L
        val consumed = when {
            state.trackingStartBatteryPct in 0..100 && currentPct in 0..100 && elapsed >= 5L * 60L * 1000L ->
                (state.trackingStartBatteryPct - currentPct).coerceAtLeast(0)
            !state.active && state.trackingStartBatteryPct in 0..100 && state.trackingStopBatteryPct in 0..100 && state.trackingStoppedAtMillis > state.trackingStartedAtMillis ->
                (state.trackingStartBatteryPct - state.trackingStopBatteryPct).coerceAtLeast(0)
            else -> null
        }
        val perHour = if (consumed != null && consumed > 0 && elapsed > 0L) consumed / (elapsed / 3_600_000.0) else null
        val to15 = if (perHour != null && perHour > 0.05 && currentPct > 15) (currentPct - 15) / perHour else null

        val recommendation = when {
            !state.active -> "Tracking nije aktivan. Procjena se računa kad pokreneš teren."
            currentPct in 1..14 -> "Baterija je kritična. Prebaci na Lite, ugasi ekran i razmisli o powerbanku."
            currentPct in 15..29 && state.trackingMode == "route" -> "Baterija je niska. Za duži teren prebaci na Lite auto ping."
            currentPct in 15..29 -> "Low battery mode je aktivan: app razrjeđuje pingove radi uštede."
            state.trackingMode == "route" && (perHour ?: 0.0) >= 8.0 -> "Ruta/GPX troši dosta baterije. Lite je bolji ako samo želiš znati gdje je tko."
            state.trackingMode == "route" -> "Ruta/GPX daje bolji trag, ali troši više od Lite moda."
            else -> "Lite auto ping je dobar za dug teren i minimalnu potrošnju."
        }

        return FieldTrackingBatteryDiagnostics(
            batteryPct = currentPct,
            isCharging = isCharging,
            temperatureC = temperatureC,
            voltageMv = voltageMv,
            trackingActive = state.active,
            trackingMode = state.trackingMode,
            tripTitle = state.tripTitle,
            startedAtMillis = state.trackingStartedAtMillis,
            elapsedMillis = elapsed,
            startBatteryPct = state.trackingStartBatteryPct,
            stopBatteryPct = state.trackingStopBatteryPct,
            queuedPoints = state.queuedPoints,
            syncedPoints = state.syncedPoints,
            lastPointAtMillis = state.lastPointAtMillis,
            lastSyncAtMillis = state.lastSyncAtMillis,
            consumptionPctPerHour = perHour,
            hoursTo15Pct = to15,
            recommendation = recommendation
        )
    }

    fun resetBaseline(context: Context) {
        FieldTrackingLitePrefs.resetBatteryBaseline(context)
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "—"
        val totalMinutes = (ms / 60000L).coerceAtLeast(0L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours <= 0L -> "${minutes} min"
            minutes == 0L -> "${hours} h"
            else -> "${hours} h ${minutes} min"
        }
    }

    fun formatHours(hours: Double?): String {
        if (hours == null || hours.isNaN() || hours.isInfinite()) return "—"
        val totalMinutes = (hours * 60.0).roundToInt().coerceAtLeast(0)
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h <= 0 -> "${m} min"
            m == 0 -> "${h} h"
            else -> "${h} h ${m} min"
        }
    }

    private fun readBatteryPct(context: Context, intent: Intent?): Int {
        val direct = FieldTrackingLitePrefs.batteryPct(context)
        if (direct in 0..100) return direct
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else -1
    }
}
