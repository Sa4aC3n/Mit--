package com.example.ui.components.lamp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Renders the multi-layered realistic warm light emission and ambient room glow
 * when the lamp is switched ON.
 */
@Composable
fun LampGlow(
    isLampOn: Boolean,
    modifier: Modifier = Modifier
) {
    val glowIntensity by animateFloatAsState(
        targetValue = if (isLampOn) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "lamp_glow_intensity"
    )

    if (glowIntensity > 0.01f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val lampCenter = Offset(width * 0.5f, height * 0.44f)
            val baseCenter = Offset(width * 0.5f, height * 0.62f)

            // 1. Wide ambient room warmth with subtle vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x40FFA726).copy(alpha = 0.28f * glowIntensity),
                        Color(0x20FF8F00).copy(alpha = 0.15f * glowIntensity),
                        Color(0x0A2B1800).copy(alpha = 0.05f * glowIntensity),
                        Color.Transparent
                    ),
                    center = lampCenter,
                    radius = width.coerceAtLeast(height) * 0.9f
                ),
                size = size
            )

            // 2. Medium warm aura surrounding the lamp
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x99FFE082).copy(alpha = 0.60f * glowIntensity),
                        Color(0x55FFB74D).copy(alpha = 0.40f * glowIntensity),
                        Color(0x22FFA000).copy(alpha = 0.18f * glowIntensity),
                        Color.Transparent
                    ),
                    center = lampCenter,
                    radius = width * 0.55f
                ),
                radius = width * 0.55f,
                center = lampCenter
            )

            // 3. Bright concentrated core glow right beneath and around the lampshade
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF9C4).copy(alpha = 0.85f * glowIntensity),
                        Color(0xCCFFE082).copy(alpha = 0.70f * glowIntensity),
                        Color(0x66FFCA28).copy(alpha = 0.35f * glowIntensity),
                        Color.Transparent
                    ),
                    center = lampCenter,
                    radius = width * 0.28f
                ),
                radius = width * 0.28f,
                center = lampCenter
            )

            // 4. Soft warm light pool / ellipse on the table surface under the base
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x88FFE082).copy(alpha = 0.45f * glowIntensity),
                        Color(0x33FFB74D).copy(alpha = 0.20f * glowIntensity),
                        Color.Transparent
                    ),
                    center = baseCenter,
                    radius = width * 0.35f
                ),
                topLeft = Offset(width * 0.15f, height * 0.58f),
                size = Size(width * 0.70f, height * 0.10f)
            )
        }
    }
}
