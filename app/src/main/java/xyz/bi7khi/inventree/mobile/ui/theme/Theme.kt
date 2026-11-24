package xyz.bi7khi.inventree.mobile.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max

/**
 * Build the whole Material 3 palette from one seed colour.
 *
 * Material's dynamic colour is only available on Android 12+, and the app
 * needs a consistent branded palette on Android 8+. This compact tonal-palette
 * generator keeps the same roles while making every screen follow the seed.
 */
@Composable
fun InvenTreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seedColor: Color = Color(0xFF146B54),
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(darkTheme, seedColor) {
        if (darkTheme) darkScheme(seedColor) else lightScheme(seedColor)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = InvenTreeTypography,
        shapes = InvenTreeShapes,
        content = content,
    )
}

private fun lightScheme(seed: Color) = run {
    val hsv = seed.hsv()
    val hue = hsv[0]
    val saturation = max(0.28f, hsv[1])
    val primary = color(hue, saturation.coerceAtMost(0.85f), 0.36f)
    val secondary = color(hue + 22f, 0.32f, 0.42f)
    val tertiary = color(hue + 55f, 0.40f, 0.40f)
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = color(hue, 0.42f, 0.90f),
        onPrimaryContainer = color(hue, 0.70f, 0.20f),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = color(hue + 22f, 0.20f, 0.92f),
        onSecondaryContainer = color(hue + 22f, 0.50f, 0.24f),
        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = color(hue + 55f, 0.30f, 0.90f),
        onTertiaryContainer = color(hue + 55f, 0.55f, 0.22f),
        background = color(hue, 0.05f, 0.985f),
        onBackground = color(hue, 0.25f, 0.12f),
        surface = color(hue, 0.025f, 0.995f),
        onSurface = color(hue, 0.25f, 0.12f),
        surfaceVariant = color(hue, 0.08f, 0.91f),
        onSurfaceVariant = color(hue, 0.18f, 0.34f),
        outline = color(hue, 0.08f, 0.53f),
        error = Color(0xFFBA1A1A),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        surfaceTint = primary,
    )
}

private fun darkScheme(seed: Color) = run {
    val hsv = seed.hsv()
    val hue = hsv[0]
    val saturation = max(0.22f, hsv[1])
    val primary = color(hue, saturation.coerceAtMost(0.65f), 0.72f)
    val secondary = color(hue + 22f, 0.26f, 0.78f)
    val tertiary = color(hue + 55f, 0.32f, 0.74f)
    darkColorScheme(
        primary = primary,
        onPrimary = color(hue, 0.85f, 0.16f),
        primaryContainer = color(hue, 0.60f, 0.26f),
        onPrimaryContainer = color(hue, 0.35f, 0.92f),
        secondary = secondary,
        onSecondary = color(hue + 22f, 0.40f, 0.16f),
        secondaryContainer = color(hue + 22f, 0.25f, 0.29f),
        onSecondaryContainer = color(hue + 22f, 0.20f, 0.90f),
        tertiary = tertiary,
        onTertiary = color(hue + 55f, 0.50f, 0.15f),
        tertiaryContainer = color(hue + 55f, 0.30f, 0.28f),
        onTertiaryContainer = color(hue + 55f, 0.25f, 0.90f),
        background = color(hue, 0.14f, 0.075f),
        onBackground = color(hue, 0.08f, 0.90f),
        surface = color(hue, 0.12f, 0.10f),
        onSurface = color(hue, 0.08f, 0.90f),
        surfaceVariant = color(hue, 0.10f, 0.24f),
        onSurfaceVariant = color(hue, 0.08f, 0.78f),
        outline = color(hue, 0.08f, 0.62f),
        error = Color(0xFFFFB4AB),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        surfaceTint = primary,
    )
}

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
