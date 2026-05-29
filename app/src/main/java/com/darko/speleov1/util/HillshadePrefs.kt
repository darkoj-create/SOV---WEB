package com.darko.speleov1.util

import android.content.Context

object HillshadePrefs {
    private const val PREFS = "hillshade_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_OPACITY = "opacity_percent"

    const val DEFAULT_OPACITY_TK25 = 40
    const val DEFAULT_OPACITY_DOF = 26
    const val MIN_ZOOM = 1
    const val MAX_ZOOM = 13

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getOpacityPercent(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_OPACITY, DEFAULT_OPACITY_TK25)
            .coerceIn(0, 75)

    fun setOpacityPercent(context: Context, opacityPercent: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_OPACITY, opacityPercent.coerceIn(0, 75))
            .apply()
    }

    fun recommendedOpacityFor(wmsConfig: WmsConfig, nightMode: Boolean = false): Int {
        val layer = wmsConfig.layers.uppercase()
        val base = when {
            layer.contains("DOF") -> DEFAULT_OPACITY_DOF
            else -> DEFAULT_OPACITY_TK25
        }
        return if (nightMode) (base * 0.70f).toInt().coerceAtLeast(14) else base
    }
}
