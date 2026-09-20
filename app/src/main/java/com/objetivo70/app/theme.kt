package com.objetivo70.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF2E7D32)
private val GreenLight = Color(0xFF66BB6A)
private val Dark = Color(0xFF151A16)

private val LightColors = lightColorScheme(
    primary = Green,
    secondary = GreenLight,
    background = Color(0xFFF8FAF8),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Dark,
    onSurface = Dark
)
private val DarkColors = darkColorScheme(
    primary = GreenLight,
    secondary = Green,
    background = Color(0xFF0F1310),
    surface = Color(0xFF181D19),
    onPrimary = Color(0xFF0C2510),
    onBackground = Color(0xFFE7EDE7),
    onSurface = Color(0xFFE7EDE7)
)

@Composable
fun Objetivo70Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
