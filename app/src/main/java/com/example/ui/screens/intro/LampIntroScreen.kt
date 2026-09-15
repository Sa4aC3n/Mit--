package com.example.ui.screens.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.lamp.LampGlow
import com.example.ui.components.lamp.RealisticLamp
import kotlinx.coroutines.delay

/**
 * Cinematic Interactive Lamp Intro Screen
 * Serves as a calm, elegant entrance: Dark screen -> Realistic Table Lamp -> Pull Cord -> Glow -> Quran Radio Home Screen.
 */
@Composable
fun LampIntroScreen(
    onIntroComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLampOn by remember { mutableStateOf(false) }
    var isInteractionCompleted by remember { mutableStateOf(false) }

    // Smooth background warmth interpolation
    val backgroundWarmth by animateFloatAsState(
        targetValue = if (isLampOn) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "bg_warmth"
    )

    // Handle cinematic pause and transition after lamp activation
    LaunchedEffect(isLampOn) {
        if (isLampOn && !isInteractionCompleted) {
            isInteractionCompleted = true
            // Cinematic dwell: allow the user to experience the warm light glow for 650ms
            delay(650)
            onIntroComplete()
        }
    }

    val darkBackgroundBrush = Brush.verticalGradient(
        colors = listOf(
            androidx.compose.ui.graphics.lerp(Color(0xFF0D0E12), Color(0xFF1E1711), backgroundWarmth),
            androidx.compose.ui.graphics.lerp(Color(0xFF08080B), Color(0xFF150F0B), backgroundWarmth),
            androidx.compose.ui.graphics.lerp(Color(0xFF040405), Color(0xFF0D0A08), backgroundWarmth)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(darkBackgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("lamp_intro_screen"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Glow Layer behind the lamp
        LampGlow(
            isLampOn = isLampOn,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Realistic Table Lamp centered
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            RealisticLamp(
                isLampOn = isLampOn,
                enabled = !isInteractionCompleted,
                onCordPulled = {
                    if (!isLampOn) {
                        isLampOn = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Subtle Elegant Arabic Hint at the bottom
        AnimatedVisibility(
            visible = !isLampOn,
            enter = fadeIn(tween(600)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "اسحب السلك للأسفل لتشغيل المصباح",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pull the cord down to turn on",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
