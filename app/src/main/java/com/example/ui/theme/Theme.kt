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
        cool = CyanElectric,
        optimal = EmeraldTelemetry,
        warm = AmberThermal,
        critical = CoralAlert,
        accentCyan = CyanElectric,
        gridLine = ObsidianOutlineVariant,
        cardBorder = ObsidianOutline
    )
}

private val NeovDarkColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = Color(0xFF00242B),
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = CyanOnContainerDark,
    secondary = EmeraldTelemetry,
    onSecondary = Color(0xFF00291D),
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = EmeraldOnContainerDark,
    tertiary = AmberThermal,
    onTertiary = Color(0xFF2B1600),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberOnContainerDark,
    error = CoralAlert,
    onError = Color(0xFF2B0000),
    errorContainer = CoralContainerDark,
    onErrorContainer = CoralOnContainerDark,
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
    primary = TealPrecision,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = TealOnContainerLight,
    secondary = EmeraldPrecision,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = EmeraldOnContainerLight,
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
            cool = CyanElectric,
            optimal = EmeraldTelemetry,
            warm = AmberThermal,
            critical = CoralAlert,
            accentCyan = CyanElectric,
            gridLine = ObsidianOutlineVariant,
            cardBorder = ObsidianOutline
        )
    } else {
        TelemetryColors(
            cool = TealPrecision,
            optimal = EmeraldPrecision,
            warm = AmberPrecision,
            critical = CoralPrecision,
            accentCyan = TealPrecision,
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
