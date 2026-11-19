package com.nebula.browser.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCFBDFF),
    onPrimary = Color(0xFF37265D),
    primaryContainer = Color(0xFF4F3D75),
    secondary = Color(0xFF69E6DC),
    surface = Color(0xFF121118),
    surfaceVariant = Color(0xFF211F28),
    background = Color(0xFF0D0C12),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF006A6A),
    surface = Color(0xFFFFFBFF),
    background = Color(0xFFF8F5FC),
)

@Composable
fun NebulaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
