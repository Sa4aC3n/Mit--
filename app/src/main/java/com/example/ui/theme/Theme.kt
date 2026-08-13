package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = MetGhamrGoldLight,
    onPrimary = MetGhamrNavy,
    primaryContainer = MetGhamrBlue,
    onPrimaryContainer = Color.White,
    secondary = MetGhamrGold,
    onSecondary = MetGhamrNavy,
    tertiary = MetGhamrTeal,
    background = MetGhamrNavy,
    surface = MetGhamrNavyLight,
    surfaceVariant = MetGhamrNavyLight,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = TextMuted,
    outline = BorderLight.copy(alpha = 0.2f),
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = MetGhamrNavy,
    onPrimary = Color.White,
    primaryContainer = MetGhamrSubtle,
    onPrimaryContainer = MetGhamrNavy,
    secondary = MetGhamrGold,
    onSecondary = Color.White,
    tertiary = MetGhamrTeal,
    background = SurfaceLight,
    surface = SurfaceCard,
    surfaceVariant = SurfaceSubtle,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    error = ErrorRed
)

@Composable
fun MetGhamrDirectoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

