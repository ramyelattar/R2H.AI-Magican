package com.r2h.magican.core.design.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RevealContainer(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    durationMillis: Int = 650,
    initialOffsetY: Dp = 22.dp,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val offsetPx = with(LocalDensity.current) { initialOffsetY.toPx() }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        label = "revealProgress"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * offsetPx
                scaleX = 0.96f + (0.04f * progress)
                scaleY = 0.96f + (0.04f * progress)
            },
        content = content
    )
}
