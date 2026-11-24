package xyz.bi7khi.inventree.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF0A1F16),
    tertiary = LightTertiary,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF001F29),
    background = LightBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF171D1A),
    surface = LightSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF171D1A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF404944),
    outline = LightOutline,
    error = LightError,
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410002),
)

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF1E352B),
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFCFE9DB),
    tertiary = DarkTertiary,
    onTertiary = androidx.compose.ui.graphics.Color(0xFF063541),
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFC1E8FA),
    background = DarkBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFFDEE4DF),
    surface = DarkSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFFDEE4DF),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFBEC9C2),
    outline = DarkOutline,
    error = DarkError,
    errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
)

@Composable
fun InvenTreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = InvenTreeTypography,
        shapes = InvenTreeShapes,
        content = content,
    )
}
