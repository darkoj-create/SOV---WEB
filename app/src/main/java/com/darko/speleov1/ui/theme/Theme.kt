package com.darko.speleov1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF4257B2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E0FF),
    onPrimaryContainer = Color(0xFF101B46),
    secondary = Color(0xFF4F6075),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE7F2),
    onSecondaryContainer = Color(0xFF132131),
    background = Color(0xFFEEF2F6),
    onBackground = Color(0xFF141A23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D26),
    surfaceVariant = Color(0xFFDDE4EC),
    onSurfaceVariant = Color(0xFF4C5869),
    outline = Color(0xFF7B8798)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCFBCFF),
    onPrimary = Color(0xFF37216A),
    primaryContainer = Color(0xFF4E3784),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Color(0xFFB9C3DC),
    onSecondary = Color(0xFF23304A),
    background = Color(0xFF121016),
    onBackground = Color(0xFFECE7F2),
    surface = Color(0xFF17141C),
    onSurface = Color(0xFFECE7F2),
    surfaceVariant = Color(0xFF241F2B),
    onSurfaceVariant = Color(0xFFC9C0D3),
    outline = Color(0xFF938AA1)
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
fun SpeleoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
