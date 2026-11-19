package com.nebula.browser.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC5A5FF), onPrimary = Color(0xFF241038),
    primaryContainer = Color(0xFF38205C), onPrimaryContainer = Color(0xFFF0E5FF),
    secondary = Color(0xFF69E6DC), tertiary = Color(0xFFFF8BD1),
    surface = Color(0xFF15101F), surfaceVariant = Color(0xFF21182F),
    background = Color(0xFF05030B), onBackground = Color(0xFFF4EEFF),
    outline = Color(0xFF78668E),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF7040C8), primaryContainer = Color(0xFFE9DCFF),
    secondary = Color(0xFF006A6A), surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF0E8FA), background = Color(0xFFF8F4FF),
)

@Composable
fun NebulaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography(), content = content)
}
