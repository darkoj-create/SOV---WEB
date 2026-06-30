package com.darko.speleov1

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun premiumIconTint(key: String, active: Boolean = false): Color {
    val resolved = when {
        key.containsAny("search", "pretra") -> Color(0xFFFFC857)
        key.containsAny("map", "karta", "maps", "location", "gps", "gmaps", "nav") -> Color(0xFF67B7FF)
        key.containsAny("offline", "overlay", "layer", "layers", "storage", "mbtiles", "tile", "baza") -> Color(0xFF76D7C4)
        key.containsAny("tool", "tools", "calc", "kalk", "ruler", "measure", "track", "tracking") -> Color(0xFFF4B66A)
        key.containsAny("photo", "camera", "gallery", "slika", "foto") -> Color(0xFFFF8FAB)
        key.containsAny("import", "folder", "open", "otvori", "google", "link") -> Color(0xFF9B8CFF)
        key.containsAny("export", "download", "save", "share", "png", "gpx", "kml", "spremi") -> Color(0xFF6FD08C)
        key.containsAny("delete", "remove", "obri", "trash") -> Color(0xFFFF8A80)
        key.containsAny("favorite", "star") -> Color(0xFFFFD166)
        key.containsAny("warning", "battery", "hazard", "danger") -> Color(0xFFFFB86C)
        key.containsAny("settings", "theme", "light", "dark", "about", "info") -> Color(0xFFA7B4FF)
        else -> MaterialTheme.colorScheme.primary
    }
    return if (active) resolved.copy(alpha = 0.98f) else resolved
}

@Composable
fun premiumIconContainer(key: String, active: Boolean = false): Color {
    val alpha = if (active) 0.26f else 0.16f
    return premiumIconTint(key, active = active).copy(alpha = alpha)
}

private fun String.containsAny(vararg parts: String): Boolean {
    val lower = lowercase()
    return parts.any { lower.contains(it) }
}
