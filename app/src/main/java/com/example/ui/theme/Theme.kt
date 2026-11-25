package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class TelemetryColors(
    val cool: Color,
    val optimal: Color,
    val warm: Color,
    val critical: Color,
    val accentCyan: Color,
    val gridLine: Color,
    val cardBorder: Color
)

val LocalTelemetryColors = staticCompositionLocalOf {
    TelemetryColors(
        cool = TextPrimaryDark,
        optimal = ScarletSecondary,
        warm = AmberThermal,
        critical = CrimsonAlert,
        accentCyan = CrimsonPrimary,
        gridLine = ObsidianOutlineVariant,
        cardBorder = ObsidianOutline
    )
}

private val NeovDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonContainerDark,
    onPrimaryContainer = CrimsonOnContainerDark,
    secondary = ScarletSecondary,
    onSecondary = Color(0xFF2B0006),
    secondaryContainer = ScarletContainerDark,
    onSecondaryContainer = ScarletOnContainerDark,
    tertiary = AmberThermal,
    onTertiary = Color(0xFF2B1600),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberOnContainerDark,
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = CrimsonAlertContainerDark,
    onErrorContainer = CrimsonAlertOnContainerDark,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurfaceLow,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = ObsidianOutline,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant
)

private val NeovLightColorScheme = lightColorScheme(
    primary = CrimsonPrecision,
    onPrimary = Color.White,
    primaryContainer = CrimsonContainerLight,
    onPrimaryContainer = CrimsonOnContainerLight,
    secondary = JetBlackSecondary,
    onSecondary = Color.White,
    secondaryContainer = JetBlackContainerLight,
    onSecondaryContainer = JetBlackOnContainerLight,
    tertiary = AmberPrecision,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = AmberOnContainerLight,
    error = CoralPrecision,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = CoralOnContainerLight,
    background = AlabasterBg,
    onBackground = TextPrimaryLight,
    surface = AlabasterSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AlabasterSurfaceContainer,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = AlabasterSurface,
    surfaceContainerLow = AlabasterSurfaceLow,
    surfaceContainer = AlabasterSurfaceContainer,
    surfaceContainerHigh = AlabasterSurfaceHigh,
    surfaceContainerHighest = AlabasterOutline,
    outline = AlabasterOutline,
    outlineVariant = AlabasterOutlineVariant
)

val NeovShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NeovCoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NeovDarkColorScheme else NeovLightColorScheme
    val telemetryColors = if (darkTheme) {
        TelemetryColors(
            cool = Color(0xFFE4E4E7),
            optimal = ScarletSecondary,
            warm = AmberThermal,
            critical = CrimsonAlert,
            accentCyan = CrimsonPrimary,
            gridLine = ObsidianOutlineVariant,
            cardBorder = ObsidianOutline
        )
    } else {
        TelemetryColors(
            cool = JetBlackSecondary,
            optimal = CrimsonPrecision,
            warm = AmberPrecision,
            critical = CoralPrecision,
            accentCyan = CrimsonPrecision,
            gridLine = AlabasterOutlineVariant,
            cardBorder = AlabasterOutline
        )
    }

    CompositionLocalProvider(LocalTelemetryColors provides telemetryColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = NeovShapes,
            content = content
        )
    }
}
