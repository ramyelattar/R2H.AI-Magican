package com.r2h.magican.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.r2h.magican.core.design.theme.mysticNeonTokens

@Composable
fun MysticScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    fogEnabled: Boolean = true,
    fogAnimated: Boolean = false,
    fogIntensity: Float = 0.28f,
    content: @Composable (PaddingValues) -> Unit
) {
    val neon = mysticNeonTokens()
    val backdrop = Brush.verticalGradient(
        colors = listOf(neon.backdropTop, neon.backdropBottom)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backdrop)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton
        ) { innerPadding ->
            Box(Modifier.fillMaxSize()) {
                content(innerPadding)
                if (fogEnabled) {
                    FogOverlay(
                        modifier = Modifier.fillMaxSize(),
                        animated = fogAnimated,
                        intensity = fogIntensity
                    )
                }
            }
        }
    }
}
