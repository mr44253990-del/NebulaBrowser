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
    surface = Color(0xFF15101F), onSurface = Color(0xFFF8F4FF),
    surfaceVariant = Color(0xFF21182F), onSurfaceVariant = Color(0xFFD9CCEF),
    background = Color(0xFF05030B), onBackground = Color(0xFFF8F4FF),
    onSecondary = Color(0xFF062928), onTertiary = Color(0xFF3B0828),
    outline = Color(0xFFA794C2), error = Color(0xFFFFB4AB),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF7040C8), primaryContainer = Color(0xFFE9DCFF),
    secondary = Color(0xFF006A6A), surface = Color(0xFFFFFBFF), onSurface = Color(0xFF1D1527),
    surfaceVariant = Color(0xFFF0E8FA), onSurfaceVariant = Color(0xFF554A63),
    background = Color(0xFFF8F4FF), onBackground = Color(0xFF1D1527),
)

@Composable
fun NebulaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography(), content = content)
}
