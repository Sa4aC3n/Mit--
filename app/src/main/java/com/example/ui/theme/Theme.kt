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
    primary = SkyBlueLight,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = SkyBlueDark,
    onPrimaryContainer = Color.White,
    secondary = WatermelonRedLight,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = WatermelonRedDark,
    onSecondaryContainer = Color.White,
    tertiary = LettuceGreenLight,
    onTertiary = Color(0xFF0F172A),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF475569),
    error = WatermelonRedDark
)

private val LightColorScheme = lightColorScheme(
    primary = SkyBluePrimary,
    onPrimary = Color.White,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = SkyBlueDeep,
    secondary = WatermelonRed,
    onSecondary = Color.White,
    secondaryContainer = WatermelonRedBg,
    onSecondaryContainer = WatermelonRedDark,
    tertiary = LettuceGreen,
    onTertiary = Color.White,
    background = SurfaceLight,
    surface = SurfaceCard,
    surfaceVariant = SkyBlueSoftBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    error = WatermelonRedDark
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


