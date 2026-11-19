package com.nebula.browser.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * One hand-tuned accent palette. Each accent supplies a dark and a light variant so the
 * app keeps readable contrast without pulling in a color-science dependency.
 */
data class AccentPalette(
    val id: String,
    val label: String,
    val swatch: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkSecondary: Color,
    val darkOnSecondary: Color,
    val darkTertiary: Color,
    val darkOnTertiary: Color,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val lightSecondary: Color,
    val lightOnSecondary: Color,
    val lightTertiary: Color,
    val lightOnTertiary: Color,
)

object NebulaAccents {
    val violet = AccentPalette(
        id = "violet", label = "Violet", swatch = Color(0xFFC5A5FF),
        darkPrimary = Color(0xFFC5A5FF), darkOnPrimary = Color(0xFF241038),
        darkPrimaryContainer = Color(0xFF38205C), darkOnPrimaryContainer = Color(0xFFF0E5FF),
        darkSecondary = Color(0xFF69E6DC), darkOnSecondary = Color(0xFF062928),
        darkTertiary = Color(0xFFFF8BD1), darkOnTertiary = Color(0xFF3B0828),
        lightPrimary = Color(0xFF7040C8), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFE9DCFF), lightOnPrimaryContainer = Color(0xFF23005C),
        lightSecondary = Color(0xFF006A6A), lightOnSecondary = Color(0xFFFFFFFF),
        lightTertiary = Color(0xFF8B1B7A), lightOnTertiary = Color(0xFFFFFFFF),
    )
    val cyan = AccentPalette(
        id = "cyan", label = "Cyan", swatch = Color(0xFF8DF0FF),
        darkPrimary = Color(0xFF8DF0FF), darkOnPrimary = Color(0xFF00252B),
        darkPrimaryContainer = Color(0xFF004E5A), darkOnPrimaryContainer = Color(0xFFB8F4FF),
        darkSecondary = Color(0xFFB0CCD2), darkOnSecondary = Color(0xFF1A3438),
        darkTertiary = Color(0xFFFFB3C8), darkOnTertiary = Color(0xFF3E0A1D),
        lightPrimary = Color(0xFF00687A), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFB8EAFF), lightOnPrimaryContainer = Color(0xFF001F26),
        lightSecondary = Color(0xFF4B6267), lightOnSecondary = Color(0xFFFFFFFF),
        lightTertiary = Color(0xFF9B4058), lightOnTertiary = Color(0xFFFFFFFF),
    )
    val magenta = AccentPalette(
        id = "magenta", label = "Magenta", swatch = Color(0xFFFFB0DA),
        darkPrimary = Color(0xFFFFB0DA), darkOnPrimary = Color(0xFF3A0B29),
        darkPrimaryContainer = Color(0xFF55163D), darkOnPrimaryContainer = Color(0xFFFFD9EA),
        darkSecondary = Color(0xFFB7E7FF), darkOnSecondary = Color(0xFF00354A),
        darkTertiary = Color(0xFFC8F06A), darkOnTertiary = Color(0xFF233600),
        lightPrimary = Color(0xFFA4306E), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFFFD9EA), lightOnPrimaryContainer = Color(0xFF3A0B29),
        lightSecondary = Color(0xFF2C6480), lightOnSecondary = Color(0xFFFFFFFF),
        lightTertiary = Color(0xFF4C6A00), lightOnTertiary = Color(0xFFFFFFFF),
    )
    val lime = AccentPalette(
        id = "lime", label = "Lime", swatch = Color(0xFFC8F06A),
        darkPrimary = Color(0xFFC8F06A), darkOnPrimary = Color(0xFF233600),
        darkPrimaryContainer = Color(0xFF3A4F00), darkOnPrimaryContainer = Color(0xFFDDF6A6),
        darkSecondary = Color(0xFF9BE8C0), darkOnSecondary = Color(0xFF003824),
        darkTertiary = Color(0xFFFFD48A), darkOnTertiary = Color(0xFF2B1700),
        lightPrimary = Color(0xFF4C6A00), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFDDF6A6), lightOnPrimaryContainer = Color(0xFF152000),
        lightSecondary = Color(0xFF3F6B52), lightOnSecondary = Color(0xFFFFFFFF),
        lightTertiary = Color(0xFF8B6A00), lightOnTertiary = Color(0xFFFFFFFF),
    )
    val amber = AccentPalette(
        id = "amber", label = "Amber", swatch = Color(0xFFFFD08A),
        darkPrimary = Color(0xFFFFD08A), darkOnPrimary = Color(0xFF2B1700),
        darkPrimaryContainer = Color(0xFF5C3A00), darkOnPrimaryContainer = Color(0xFFFFE3B8),
        darkSecondary = Color(0xFF7CE0D4), darkOnSecondary = Color(0xFF003731),
        darkTertiary = Color(0xFFFFB4AB), darkOnTertiary = Color(0xFF561F18),
        lightPrimary = Color(0xFF8A5300), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFFFDDB0), lightOnPrimaryContainer = Color(0xFF2B1700),
        lightSecondary = Color(0xFF00695F), lightOnSecondary = Color(0xFFFFFFFF),
        lightTertiary = Color(0xFF8B4B45), lightOnTertiary = Color(0xFFFFFFFF),
    )
    val cobalt = AccentPalette(
        id = "cobalt", label = "Cobalt", swatch = Color(0xFFAEC6FF),
        darkPrimary = Color(0xFFAEC6FF), darkOnPrimary = Color(0xFF002E6B),
        darkPrimaryContainer = Color(0xFF1B4590), darkOnPrimaryContainer = Color(0xFFD9E2FF),
        darkSecondary = Color(0xFF7CE0D4), darkOnSecondary = Color(0xFF003731),
        darkTertiary = Color(0xFFFFB3C8), darkOnTertiary = Color(0xFF3E0A1D),
        lightPrimary = Color(0xFF2C5DA8), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFD9E2FF), lightOnPrimaryContainer = Color(0xFF001A41),
        lightSecondary = Color(0xFF00695F), lightOnSecondary = Color(0xFFFFFFFF),
        lightTertiary = Color(0xFF8B4058), lightOnTertiary = Color(0xFFFFFFFF),
    )

    val all: List<AccentPalette> = listOf(violet, cyan, magenta, lime, amber, cobalt)

    fun byId(id: String): AccentPalette = all.firstOrNull { it.id == id } ?: violet
}

private val BaseDark = darkColorScheme(
    primary = Color(0xFFC5A5FF), onPrimary = Color(0xFF241038),
    primaryContainer = Color(0xFF38205C), onPrimaryContainer = Color(0xFFF0E5FF),
    secondary = Color(0xFF69E6DC), tertiary = Color(0xFFFF8BD1),
    surface = Color(0xFF15101F), onSurface = Color(0xFFF8F4FF),
    surfaceVariant = Color(0xFF21182F), onSurfaceVariant = Color(0xFFD9CCEF),
    background = Color(0xFF05030B), onBackground = Color(0xFFF8F4FF),
    onSecondary = Color(0xFF062928), onTertiary = Color(0xFF3B0828),
    outline = Color(0xFFA794C2), error = Color(0xFFFFB4AB),
)
private val BaseLight = lightColorScheme(
    primary = Color(0xFF7040C8), primaryContainer = Color(0xFFE9DCFF),
    secondary = Color(0xFF006A6A), surface = Color(0xFFFFFBFF), onSurface = Color(0xFF1D1527),
    surfaceVariant = Color(0xFFF0E8FA), onSurfaceVariant = Color(0xFF554A63),
    background = Color(0xFFF8F4FF), onBackground = Color(0xFF1D1527),
)

private fun darkSchemeFor(accent: AccentPalette) = BaseDark.copy(
    primary = accent.darkPrimary,
    onPrimary = accent.darkOnPrimary,
    primaryContainer = accent.darkPrimaryContainer,
    onPrimaryContainer = accent.darkOnPrimaryContainer,
    inversePrimary = accent.darkPrimary,
    secondary = accent.darkSecondary,
    onSecondary = accent.darkOnSecondary,
    secondaryContainer = accent.darkPrimaryContainer,
    tertiary = accent.darkTertiary,
    onTertiary = accent.darkOnTertiary,
)

private fun lightSchemeFor(accent: AccentPalette) = BaseLight.copy(
    primary = accent.lightPrimary,
    onPrimary = accent.lightOnPrimary,
    primaryContainer = accent.lightPrimaryContainer,
    onPrimaryContainer = accent.lightOnPrimaryContainer,
    inversePrimary = accent.lightPrimary,
    secondary = accent.lightSecondary,
    onSecondary = accent.lightOnSecondary,
    secondaryContainer = accent.lightPrimaryContainer,
    tertiary = accent.lightTertiary,
    onTertiary = accent.lightOnTertiary,
)

/** Slightly tighter, bolder display type so headings feel more modern. */
private val NebulaTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/**
 * @param dynamicColors use the wallpaper-derived Material You palette (Android 12+).
 * @param accentColor id from [NebulaAccents]; ignored when [dynamicColors] is active.
 */
@Composable
fun NebulaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColors: Boolean = false,
    accentColor: String = "violet",
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val accent = remember(accentColor) { NebulaAccents.byId(accentColor) }
    val colorScheme = when {
        dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkSchemeFor(accent)
        else -> lightSchemeFor(accent)
    }
    MaterialTheme(colorScheme = colorScheme, typography = NebulaTypography, content = content)
}
