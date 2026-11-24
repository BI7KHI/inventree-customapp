package xyz.bi7khi.inventree.mobile.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max

/**
 * Extra semantic colours that Material 3 does not expose.
 *
 * Screens should read these instead of hard-coding yellow/cyan so that the
 * signal colour remains user-selectable while the industrial neutrals stay
 * stable.
 */
@Immutable
data class FieldPalette(
    val signal: Color,
    val onSignal: Color,
    val signalText: Color,
    val cyan: Color,
    val cyanText: Color,
    val rust: Color,
    val paper: Color,
    val panel: Color,
    val panelAlt: Color,
    val ink: Color,
    val line: Color,
    val lineStrong: Color,
    val muted: Color,
    val danger: Color,
    val warning: Color,
    val success: Color,
    val isDark: Boolean,
)

private fun lightFieldPalette(seed: Color): FieldPalette {
    val hsv = seed.hsv()
    val hue = if (hsv[1] < 0.08f) 60f else hsv[0]
    val saturation = max(0.72f, hsv[1]).coerceAtMost(1f)
    val fill = color(hue, saturation, 0.94f)
    return FieldPalette(
        signal = fill,
        onSignal = SignalInk,
        signalText = color(hue, saturation, 0.31f),
        cyan = IndustrialCyan,
        cyanText = IndustrialCyanDim,
        rust = EmberRedDim,
        paper = FieldPaper,
        panel = FieldPaperLight,
        panelAlt = FieldPaperDeep,
        ink = FieldInk,
        line = FieldLine,
        lineStrong = LightOutline,
        muted = Color(0xFF4E5149),
        danger = LightError,
        warning = Warning,
        success = Success,
        isDark = false,
    )
}

private fun darkFieldPalette(seed: Color): FieldPalette {
    val hsv = seed.hsv()
    val hue = if (hsv[1] < 0.08f) 60f else hsv[0]
    val saturation = max(0.70f, hsv[1]).coerceAtMost(1f)
    val fill = color(hue, saturation, 0.96f)
    return FieldPalette(
        signal = fill,
        onSignal = SignalInk,
        signalText = fill,
        cyan = IndustrialCyan,
        cyanText = IndustrialCyan,
        rust = Color(0xFFFF8067),
        paper = FieldCarbon,
        panel = FieldCarbonRaised,
        panelAlt = Color(0xFF1D221C),
        ink = Color(0xFFF1EFE4),
        line = FieldLineDark,
        lineStrong = DarkOutline,
        muted = Color(0xFFA8ADA2),
        danger = DarkError,
        warning = Color(0xFFFFB84D),
        success = Color(0xFF5BD58B),
        isDark = true,
    )
}

val LocalFieldPalette = staticCompositionLocalOf { lightFieldPalette(SignalYellow) }

/**
 * Build the whole Material 3 palette from one signal colour.
 *
 * Neutral surfaces deliberately do not follow the seed. The field style keeps
 * a constant paper/carbon ground and lets the accent act as a signal.
 */
@Composable
fun InvenTreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seedColor: Color = SignalYellow,
    content: @Composable () -> Unit,
) {
    val field = remember(darkTheme, seedColor) {
        if (darkTheme) darkFieldPalette(seedColor) else lightFieldPalette(seedColor)
    }
    val colorScheme = remember(field) {
        if (darkTheme) darkScheme(field) else lightScheme(field)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalFieldPalette provides field) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = InvenTreeTypography,
            shapes = InvenTreeShapes,
            content = content,
        )
    }
}

private fun lightScheme(f: FieldPalette) = lightColorScheme(
    primary = f.signalText,
    onPrimary = Color.White,
    primaryContainer = f.signal,
    onPrimaryContainer = f.onSignal,
    secondary = f.cyanText,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = Color(0xFF003737),
    tertiary = f.rust,
    onTertiary = Color.White,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = Color(0xFF3B0A02),
    background = f.paper,
    onBackground = f.ink,
    surface = f.panel,
    onSurface = f.ink,
    surfaceVariant = f.panelAlt,
    onSurfaceVariant = f.muted,
    surfaceContainerLowest = f.panel,
    surfaceContainerLow = f.panel,
    surfaceContainer = f.panel,
    surfaceContainerHigh = f.panel,
    surfaceContainerHighest = f.panelAlt,
    surfaceDim = f.paper,
    surfaceBright = f.panel,
    outline = f.lineStrong,
    outlineVariant = f.line,
    error = f.danger,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410002),
    surfaceTint = f.signalText,
    inverseSurface = FieldInk,
    inverseOnSurface = FieldPaperLight,
    inversePrimary = f.signal,
    scrim = Color.Black,
)

private fun darkScheme(f: FieldPalette) = darkColorScheme(
    primary = f.signal,
    onPrimary = f.onSignal,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = f.cyan,
    onSecondary = Color(0xFF003737),
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = Color(0xFF9FF2EC),
    tertiary = f.rust,
    onTertiary = Color(0xFF3B0A02),
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = Color(0xFFFFDAD1),
    background = f.paper,
    onBackground = f.ink,
    surface = f.panel,
    onSurface = f.ink,
    surfaceVariant = f.panelAlt,
    onSurfaceVariant = f.muted,
    surfaceContainerLowest = f.paper,
    surfaceContainerLow = f.panel,
    surfaceContainer = f.panel,
    surfaceContainerHigh = f.panel,
    surfaceContainerHighest = f.panelAlt,
    surfaceDim = f.paper,
    surfaceBright = f.panelAlt,
    outline = f.lineStrong,
    outlineVariant = f.line,
    error = f.danger,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceTint = f.signal,
    inverseSurface = FieldPaperLight,
    inverseOnSurface = FieldInk,
    inversePrimary = f.signalText,
    scrim = Color.Black,
)

private fun Color.hsv(): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    return hsv
}

private fun color(hue: Float, saturation: Float, value: Float): Color =
    Color(
        AndroidColor.HSVToColor(
            floatArrayOf(
                ((hue % 360f) + 360f) % 360f,
                saturation.coerceIn(0f, 1f),
                value.coerceIn(0f, 1f),
            ),
        ),
    )

fun Color.contrastOn(): Color = if (luminance() > 0.5f) Color.Black else Color.White

fun Color.blendWith(other: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = red * (1 - f) + other.red * f,
        green = green * (1 - f) + other.green * f,
        blue = blue * (1 - f) + other.blue * f,
        alpha = 1f,
    )
}
