package com.example.ui.components.lamp

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Realistic Table Lamp component with interactive pull-cord physics,
 * smooth dome lampshade, metallic stand and base, and realistic lighting transitions.
 */
@Composable
fun RealisticLamp(
    isLampOn: Boolean,
    onCordPulled: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    // Cord pull animation & drag states
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val recoilAnim = remember { Animatable(0f) }

    // Thresholds in pixels for downward pull (gentler threshold for effortless pull)
    val maxPullPx = with(density) { 110.dp.toPx() }
    val activationThresholdPx = with(density) { 32.dp.toPx() }

    // Subtle breathing pulse for downward pull affordance when lamp is off
    val infiniteTransition = rememberInfiniteTransition(label = "cord_hint")
    val hintPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_pulse"
    )

    // Smooth transition factor for lamp ON/OFF aesthetics
    val transitionFactor by animateFloatAsState(
        targetValue = if (isLampOn) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "lamp_transition"
    )

    val lampWidthDp = 280.dp
    val lampHeightDp = 320.dp

    val lampWidthPx = with(density) { lampWidthDp.toPx() }
    val lampHeightPx = with(density) { lampHeightDp.toPx() }

    // Proportional geometry anchors
    val shadeWidthPx = lampWidthPx * 0.76f
    val shadeHeightPx = lampHeightPx * 0.36f
    val shadeTopPx = lampHeightPx * 0.12f
    val shadeBottomPx = shadeTopPx + shadeHeightPx

    val stemWidthPx = with(density) { 8.dp.toPx() }
    val baseWidthPx = lampWidthPx * 0.44f
    val baseHeightPx = with(density) { 14.dp.toPx() }
    val baseBottomPx = lampHeightPx * 0.88f
    val baseTopPx = baseBottomPx - baseHeightPx

    // Pull cord attachment coordinates (on the right side of the lampshade)
    val cordOriginX = (lampWidthPx / 2f) + (shadeWidthPx * 0.28f)
    val cordOriginY = shadeBottomPx - with(density) { 2.dp.toPx() }
    val restingCordLengthPx = with(density) { 50.dp.toPx() }

    // Current dynamic cord extension (either active finger drag or spring recoil)
    val currentExtensionPx = if (isDragging) dragOffsetY else recoilAnim.value
    val currentCordLengthPx = restingCordLengthPx + currentExtensionPx
    val cordEndX = cordOriginX
    val cordEndY = cordOriginY + currentCordLengthPx

    BoxWithConstraints(
        modifier = modifier
            .testTag("realistic_lamp_container"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = if (maxWidth.isSpecified && maxWidth > 0.dp && maxWidth != Dp.Infinity) maxWidth else lampWidthDp

        // 1. Draw Base, Stand, Lampshade, Cord & Hint on Canvas (Centered)
        Box(
            modifier = Modifier
                .size(lampWidthDp, lampHeightDp)
                .align(Alignment.Center)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
            val centerX = size.width / 2f

            // A. Contact Shadow under Base
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x77000000),
                        Color(0x33000000),
                        Color.Transparent
                    ),
                    center = Offset(centerX, baseBottomPx + with(density) { 3.dp.toPx() }),
                    radius = baseWidthPx * 0.65f
                ),
                topLeft = Offset(centerX - baseWidthPx * 0.65f, baseBottomPx - with(density) { 2.dp.toPx() }),
                size = Size(baseWidthPx * 1.3f, with(density) { 12.dp.toPx() })
            )

            // B. Stand / Stem
            drawLampStand(
                centerX = centerX,
                startY = shadeBottomPx - with(density) { 6.dp.toPx() },
                endY = baseTopPx + with(density) { 2.dp.toPx() },
                stemWidth = stemWidthPx,
                isLampOnFactor = transitionFactor
            )

            // C. Base
            drawLampBase(
                centerX = centerX,
                baseTopY = baseTopPx,
                baseBottomY = baseBottomPx,
                baseWidth = baseWidthPx,
                isLampOnFactor = transitionFactor
            )

            // D. Pull Cord line & handle (draws realistically with ball beads and metallic bell handle)
            drawPullCord(
                startX = cordOriginX,
                startY = cordOriginY,
                endX = cordEndX,
                endY = cordEndY,
                density = density.density,
                isLampOnFactor = transitionFactor
            )

            // D2. Animated downward chevron hint under the handle when lamp is off
            if (!isLampOn && !isDragging && recoilAnim.value < 2f) {
                val arrowTopY = cordEndY + with(density) { 14.dp.toPx() } + hintPulse
                val arrowWidth = with(density) { 5.dp.toPx() }
                val arrowHeight = with(density) { 4.dp.toPx() }
                val arrowColor = Color(0xFFFFD54F).copy(alpha = 0.75f)

                drawLine(
                    color = arrowColor,
                    start = Offset(cordEndX - arrowWidth, arrowTopY),
                    end = Offset(cordEndX, arrowTopY + arrowHeight),
                    strokeWidth = 1.8f * density.density,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = arrowColor,
                    start = Offset(cordEndX, arrowTopY + arrowHeight),
                    end = Offset(cordEndX + arrowWidth, arrowTopY),
                    strokeWidth = 1.8f * density.density,
                    cap = StrokeCap.Round
                )
            }

            // E. Lampshade (Drawn in front of stand, with depth and highlight)
            drawLampShade(
                centerX = centerX,
                topY = shadeTopPx,
                bottomY = shadeBottomPx,
                width = shadeWidthPx,
                isLampOnFactor = transitionFactor
            )
        }
    }

        // 2. Interactive Touch Target for the Pull Cord on the Right Side
        // Expanded to generously cover from the lamp center (covering cord and handle)
        // all the way to the far right screen edge!
        val touchWidthDp = if (containerWidth > lampWidthDp) {
            (containerWidth / 2) + 24.dp
        } else {
            180.dp
        }
        val touchHeightDp = 420.dp

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(touchWidthDp)
                .height(touchHeightDp)
                .semantics {
                    contentDescription = "اسحب سلسلة المصباح للأسفل لتشغيل الإضاءة والبدء"
                }
                .testTag("lamp_pull_cord_handle")
                .pointerInput(enabled, isLampOn) {
                    if (!enabled || isLampOn) return@pointerInput

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var currentPull = 0f
                        val startY = down.position.y
                        var hasTriggeredTick = false
                        isDragging = true

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                break
                            }
                            change.consume()
                            val deltaY = change.position.y - startY
                            if (deltaY > 0f) {
                                currentPull = deltaY.coerceIn(0f, maxPullPx)
                                dragOffsetY = currentPull

                                if (currentPull >= activationThresholdPx && !hasTriggeredTick) {
                                    hasTriggeredTick = true
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                } else if (currentPull < activationThresholdPx) {
                                    hasTriggeredTick = false
                                }
                            } else {
                                currentPull = 0f
                                dragOffsetY = 0f
                            }
                        }

                        // Finger released
                        isDragging = false
                        if (currentPull >= activationThresholdPx) {
                            // User pulled the cord past activation threshold: recoil with spring and light up!
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            coroutineScope.launch {
                                recoilAnim.snapTo(currentPull)
                                recoilAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessHigh
                                    )
                                )
                            }
                            coroutineScope.launch {
                                delay(120)
                                onCordPulled()
                            }
                        } else {
                            // User released without pulling far enough (or simple tap):
                            // Spring back to rest WITHOUT turning on the lamp!
                            coroutineScope.launch {
                                recoilAnim.snapTo(currentPull)
                                recoilAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    }
                }
        )
    }
}

/**
 * Draws the realistic metallic Stand (stem) connecting shade and base
 */
private fun DrawScope.drawLampStand(
    centerX: Float,
    startY: Float,
    endY: Float,
    stemWidth: Float,
    isLampOnFactor: Float
) {
    val left = centerX - stemWidth / 2f
    val right = centerX + stemWidth / 2f

    // OFF colors vs ON warm metallic colors
    val darkMetallic = listOf(
        Color(0xFF1E1F22),
        Color(0xFF383C42),
        Color(0xFF1B1C1F)
    )
    val warmMetallic = listOf(
        Color(0xFF362B21),
        Color(0xFF6B5844),
        Color(0xFF2C2219)
    )

    val currentColors = darkMetallic.zip(warmMetallic).map { (offColor, onColor) ->
        androidx.compose.ui.graphics.lerp(offColor, onColor, isLampOnFactor)
    }

    drawRect(
        brush = Brush.horizontalGradient(
            colors = currentColors,
            startX = left,
            endX = right
        ),
        topLeft = Offset(left, startY),
        size = Size(stemWidth, endY - startY)
    )

    // Subtle edge highlight on the stem
    drawLine(
        color = androidx.compose.ui.graphics.lerp(
            Color(0x44FFFFFF),
            Color(0x88FFE082),
            isLampOnFactor
        ),
        start = Offset(left + stemWidth * 0.35f, startY),
        end = Offset(left + stemWidth * 0.35f, endY),
        strokeWidth = 1.2f
    )
}

/**
 * Draws the horizontal disc Lamp Base
 */
private fun DrawScope.drawLampBase(
    centerX: Float,
    baseTopY: Float,
    baseBottomY: Float,
    baseWidth: Float,
    isLampOnFactor: Float
) {
    val baseHeight = baseBottomY - baseTopY
    val left = centerX - baseWidth / 2f
    val cornerRadius = baseHeight * 0.45f

    // Base body gradient
    val baseBrush = Brush.verticalGradient(
        colors = listOf(
            androidx.compose.ui.graphics.lerp(Color(0xFF3B3E45), Color(0xFF635242), isLampOnFactor),
            androidx.compose.ui.graphics.lerp(Color(0xFF232529), Color(0xFF3E3125), isLampOnFactor),
            androidx.compose.ui.graphics.lerp(Color(0xFF17181B), Color(0xFF241C15), isLampOnFactor)
        ),
        startY = baseTopY,
        endY = baseBottomY
    )

    drawRoundRect(
        brush = baseBrush,
        topLeft = Offset(left, baseTopY),
        size = Size(baseWidth, baseHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
    )

    // Top rim bevel highlight
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                androidx.compose.ui.graphics.lerp(Color(0x55FFFFFF), Color(0x99FFE082), isLampOnFactor),
                Color.Transparent
            ),
            startX = left,
            endX = left + baseWidth
        ),
        topLeft = Offset(left + 2f, baseTopY + 1f),
        size = Size(baseWidth - 4f, 2f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f)
    )
}

/**
 * Draws the pull cord string with ball beads and the metallic handle/bead at the bottom
 */
private fun DrawScope.drawPullCord(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    density: Float,
    isLampOnFactor: Float
) {
    // 1. Cord Line (slender, with bright metallic contrast)
    val cordColor = androidx.compose.ui.graphics.lerp(
        Color(0xFFCFD8DC),
        Color(0xFFFFD54F),
        isLampOnFactor
    )

    val cordStrokeWidth = 2.4f * density
    drawLine(
        color = cordColor,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = cordStrokeWidth,
        cap = StrokeCap.Round
    )

    // Ball-chain beads along the pull cord
    val totalCordLength = endY - startY
    val beadInterval = 9f * density
    val beadCount = (totalCordLength / beadInterval).toInt().coerceAtLeast(1)
    val beadRadius = 2.0f * density

    val beadBrush = Brush.radialGradient(
        colors = listOf(
            androidx.compose.ui.graphics.lerp(Color(0xFFFFFFFF), Color(0xFFFFF9C4), isLampOnFactor),
            androidx.compose.ui.graphics.lerp(Color(0xFFB0BEC5), Color(0xFFFFC107), isLampOnFactor),
            androidx.compose.ui.graphics.lerp(Color(0xFF546E7A), Color(0xFF8D6E63), isLampOnFactor)
        ),
        radius = beadRadius * 1.5f
    )

    for (i in 1..beadCount) {
        val beadY = startY + (i * totalCordLength / (beadCount + 1))
        drawCircle(
            brush = beadBrush,
            radius = beadRadius,
            center = Offset(startX, beadY)
        )
    }

    // 2. Beaded Bell Handle at the bottom of the cord (prominent, tactile and clearly visible)
    val handleRadiusX = 6.0f * density
    val handleRadiusY = 10.5f * density

    // Handle soft shadow
    drawOval(
        color = Color(0x55000000),
        topLeft = Offset(endX - handleRadiusX + 2f * density, endY - handleRadiusY + 2f * density),
        size = Size(handleRadiusX * 2f, handleRadiusY * 2f)
    )

    // Handle body with rich metallic gradient
    val handleBrush = Brush.radialGradient(
        colors = listOf(
            androidx.compose.ui.graphics.lerp(Color(0xFFFFFFFF), Color(0xFFFFF9C4), isLampOnFactor),
            androidx.compose.ui.graphics.lerp(Color(0xFFCFD8DC), Color(0xFFFFCA28), isLampOnFactor),
            androidx.compose.ui.graphics.lerp(Color(0xFF607D8B), Color(0xFFE65100), isLampOnFactor)
        ),
        center = Offset(endX - 1.5f * density, endY - 2.5f * density),
        radius = handleRadiusY * 1.6f
    )

    drawOval(
        brush = handleBrush,
        topLeft = Offset(endX - handleRadiusX, endY - handleRadiusY),
        size = Size(handleRadiusX * 2f, handleRadiusY * 2f)
    )

    // Handle metallic decorative rim ring
    drawLine(
        color = androidx.compose.ui.graphics.lerp(Color(0xBBFFFFFF), Color(0xFFFFD54F), isLampOnFactor),
        start = Offset(endX - handleRadiusX * 0.75f, endY),
        end = Offset(endX + handleRadiusX * 0.75f, endY),
        strokeWidth = 1.4f * density,
        cap = StrokeCap.Round
    )

    // Handle highlight reflection pip
    drawCircle(
        color = Color.White,
        radius = 1.6f * density,
        center = Offset(endX - 1.8f * density, endY - 3.5f * density)
    )
}

/**
 * Draws the realistic dome-shaped Lampshade with custom Path,
 * layered gradients, soft upper highlight, subtle bottom depth, and warm inner glow.
 */
private fun DrawScope.drawLampShade(
    centerX: Float,
    topY: Float,
    bottomY: Float,
    width: Float,
    isLampOnFactor: Float
) {
    val height = bottomY - topY
    val left = centerX - width / 2f
    val right = centerX + width / 2f

    // Construct smooth dome path
    val domePath = Path().apply {
        moveTo(left, bottomY)

        // Left curve ascending to top apex
        cubicTo(
            x1 = left - width * 0.02f,
            y1 = topY + height * 0.35f,
            x2 = centerX - width * 0.38f,
            y2 = topY,
            x3 = centerX,
            y3 = topY
        )

        // Right curve descending from apex to bottom-right
        cubicTo(
            x1 = centerX + width * 0.38f,
            y1 = topY,
            x2 = right + width * 0.02f,
            y2 = topY + height * 0.35f,
            x3 = right,
            y3 = bottomY
        )

        // Bottom curved lip connecting right to left
        quadraticTo(
            x1 = centerX,
            y1 = bottomY + height * 0.06f,
            x2 = left,
            y2 = bottomY
        )
        close()
    }

    // Colors for OFF state:
    // Main shade: #F2EBDD, Highlight: #FFF9EE, Shadow: #CFC7B8
    val offMain = Color(0xFFF2EBDD)
    val offHighlight = Color(0xFFFFF9EE)
    val offShadow = Color(0xFFCFC7B8)

    // Colors for ON state:
    // Main shade: #FFF8E8, Highlight: #FFFFFF, Inner glow: #FFF4C7
    val onMain = Color(0xFFFFF8E8)
    val onHighlight = Color(0xFFFFFFFF)
    val onShadow = Color(0xFFFFE0B2)

    val currentMain = androidx.compose.ui.graphics.lerp(offMain, onMain, isLampOnFactor)
    val currentHighlight = androidx.compose.ui.graphics.lerp(offHighlight, onHighlight, isLampOnFactor)
    val currentShadow = androidx.compose.ui.graphics.lerp(offShadow, onShadow, isLampOnFactor)

    // 1. Base Lampshade Body
    val shadeBrush = Brush.radialGradient(
        colors = listOf(
            currentHighlight,
            currentMain,
            currentShadow
        ),
        center = Offset(centerX - width * 0.12f, topY + height * 0.32f),
        radius = width * 0.65f
    )

    drawPath(
        path = domePath,
        brush = shadeBrush,
        style = Fill
    )

    // 2. Inner warm glow accent when lamp is ON
    if (isLampOnFactor > 0.01f) {
        val innerGlowBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF9C4).copy(alpha = 0.70f * isLampOnFactor),
                Color(0xFFFFE082).copy(alpha = 0.40f * isLampOnFactor),
                Color.Transparent
            ),
            center = Offset(centerX, bottomY - height * 0.15f),
            radius = width * 0.48f
        )
        drawPath(
            path = domePath,
            brush = innerGlowBrush,
            style = Fill
        )
    }

    // 3. Soft Upper Dome Highlight arc
    val topHighlightPath = Path().apply {
        moveTo(centerX - width * 0.32f, topY + height * 0.22f)
        cubicTo(
            x1 = centerX - width * 0.18f,
            y1 = topY + height * 0.04f,
            x2 = centerX + width * 0.18f,
            y2 = topY + height * 0.04f,
            x3 = centerX + width * 0.32f,
            y3 = topY + height * 0.22f
        )
    }

    drawPath(
        path = topHighlightPath,
        color = Color.White.copy(alpha = 0.35f + 0.35f * isLampOnFactor),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )

    // 4. Subtle Bottom Rim Shading and depth
    val bottomRimPath = Path().apply {
        moveTo(left, bottomY)
        quadraticTo(
            x1 = centerX,
            y1 = bottomY + height * 0.06f,
            x2 = right,
            y2 = bottomY
        )
    }

    drawPath(
        path = bottomRimPath,
        color = androidx.compose.ui.graphics.lerp(
            Color(0x33000000),
            Color(0x33FFA000),
            isLampOnFactor
        ),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )
}
