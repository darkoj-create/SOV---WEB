package com.darko.speleov1.util

import android.content.Context

object BatteryOptimizationPrefs {
    private const val PREFS = "battery_optimization_prefs"
    private const val KEY_AUTO_PROMPT_SHOWN = "auto_prompt_shown"

    fun shouldShowAutoPrompt(context: Context): Boolean {
        return !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_PROMPT_SHOWN, false)
    }

    fun markAutoPromptShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_PROMPT_SHOWN, true)
            .apply()
    }
}
