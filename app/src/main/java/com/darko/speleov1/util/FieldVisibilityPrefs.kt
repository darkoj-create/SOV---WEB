package com.darko.speleov1.util

import android.content.Context

object FieldVisibilityPrefs {
    private const val PREFS = "sov_field_visibility_prefs"
    private const val KEY_ENABLED = "field_visibility_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
