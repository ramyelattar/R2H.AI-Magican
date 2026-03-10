package com.r2h.magican.features.voiceaura.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun WaveformView(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    colorStart: Color = Color(0xFF00F5D4),
    colorEnd: Color = Color(0xFF43B0FF)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics {
                contentDescription = "Live audio waveform"
            }
    ) {
        if (amplitudes.isEmpty()) return@Canvas

        val midY = size.height / 2f
        val stepX = size.width / max(1, amplitudes.size - 1)
        val barWidth = (stepX * 0.65f).coerceAtLeast(2f)

        val brush = Brush.verticalGradient(
            colors = listOf(colorStart, colorEnd)
        )

        for (i in amplitudes.indices) {
            val x = i * stepX
            val amp = amplitudes[i].coerceIn(0f, 1f)
            val halfBar = (amp * (size.height * 0.42f)).coerceAtLeast(2f)

            drawLine(
                brush = brush,
                start = Offset(x, midY - halfBar),
                end = Offset(x, midY + halfBar),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }

        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.5f
        )
    }
}
