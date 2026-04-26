package com.r2h.magican.core.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.r2h.magican.core.design.theme.mysticNeonTokens

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = "Working…",
    shape: Shape = RoundedCornerShape(16.dp),
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val effectiveEnabled = enabled && !isLoading
    val neon = mysticNeonTokens()
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            neon.primary.copy(alpha = if (effectiveEnabled) 1f else 0.45f),
            neon.secondary.copy(alpha = if (effectiveEnabled) 1f else 0.45f)
        )
    )

    Button(
        onClick = onClick,
        enabled = effectiveEnabled,
        shape = shape,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = neon.onAccent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = neon.onAccent.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .heightIn(min = 50.dp)
            .defaultMinSize(minWidth = 120.dp)
            .shadow(
                elevation = if (effectiveEnabled) 20.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = neon.glow,
                spotColor = neon.glow
            )
            .background(gradient, shape)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = neon.tertiary.copy(alpha = if (effectiveEnabled) 0.85f else 0.35f)
                ),
                shape = shape
            )
            .semantics {
                contentDescription = if (isLoading) loadingText else text
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = neon.onAccent
                )
                Text(text = loadingText, style = MaterialTheme.typography.labelLarge)
            } else {
                leading?.invoke()
                Text(text = text, style = MaterialTheme.typography.labelLarge)
                trailing?.invoke()
            }
        }
    }
}
