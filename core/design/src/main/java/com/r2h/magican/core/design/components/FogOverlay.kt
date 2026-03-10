package com.r2h.magican.core.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.r2h.magican.core.design.theme.mysticGlassTokens

@Composable
fun FogOverlay(
    modifier: Modifier = Modifier,
    intensity: Float = 0.32f,
    animated: Boolean = true,
    tint: Color = Color.Unspecified
) {
    val glass = mysticGlassTokens()
    val fog = if (tint == Color.Unspecified) glass.fogTint else tint

    val phase = if (animated) {
        val transition = rememberInfiniteTransition(label = "fog")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 9000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "fogPhase"
        ).value
    } else {
        0.5f
    }

    Canvas(modifier = modifier) {
        val leftCenter = Offset(size.width * (0.15f + phase * 0.35f), size.height * 0.25f)
        val rightCenter = Offset(size.width * (0.85f - phase * 0.3f), size.height * 0.78f)

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    fog.copy(alpha = 0.40f * intensity),
                    Color.Transparent
                ),
                center = leftCenter,
                radius = size.minDimension * 0.72f
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    fog.copy(alpha = 0.32f * intensity),
                    Color.Transparent
                ),
                center = rightCenter,
                radius = size.minDimension * 0.64f
            )
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    fog.copy(alpha = 0.18f * intensity),
                    Color.Transparent
                )
            )
        )
    }
}
