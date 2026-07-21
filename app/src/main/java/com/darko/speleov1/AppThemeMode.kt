package com.darko.speleov1

/**
 * Korisnički odabir teme. Default ostaje DARK da postojeći korisnici
 * nakon nadogradnje dobiju isti vizual kao i prije.
 */
enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    fun label(language: AppLanguage): String = when (this) {
        SYSTEM -> language.pick("Sustav", "System")
        LIGHT -> language.pick("Svijetla", "Light")
        DARK -> language.pick("Tamna", "Dark")
    }

    fun description(language: AppLanguage): String = when (this) {
        SYSTEM -> language.pick("Prati postavku uređaja.", "Follow device setting.")
        LIGHT -> language.pick("Svijetla tema za dan.", "Light theme for daytime.")
        DARK -> language.pick("Klasična SOV tamna tema.", "Classic SOV dark theme.")
    }

    companion object {
        fun fromStored(value: String?): AppThemeMode = runCatching {
            if (value.isNullOrBlank()) DARK else valueOf(value)
        }.getOrDefault(DARK)
    }
}
