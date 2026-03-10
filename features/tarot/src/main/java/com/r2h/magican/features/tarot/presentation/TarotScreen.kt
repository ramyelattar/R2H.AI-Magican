package com.r2h.magican.features.tarot.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.r2h.magican.core.design.components.FogOverlay
import com.r2h.magican.core.design.components.GlassCard
import com.r2h.magican.core.design.components.MysticScaffold
import com.r2h.magican.core.design.components.NeonButton
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TarotScreen(
    viewModel: TarotViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        viewModel.onMicPermissionChanged(granted)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(hasMicPermission) {
        viewModel.onMicPermissionChanged(hasMicPermission)
    }

    DisposableEffect(Unit) {
        viewModel.setScreenActive(true)
        onDispose { viewModel.setScreenActive(false) }
    }

    TarotScreen(
        state = state,
        onQuestionChanged = viewModel::onQuestionChanged,
        onDeckTapDraw = viewModel::onDeckTapDraw,
        onCardFlip = viewModel::onCardFlip,
        onSwipeShuffle = viewModel::onSwipeShuffle,
        onResetSpread = viewModel::onResetSpread,
        onRequestMicPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    )
}

@Composable
private fun TarotScreen(
    state: TarotUiState,
    onQuestionChanged: (String) -> Unit,
    onDeckTapDraw: () -> Unit,
    onCardFlip: (Int) -> Unit,
    onSwipeShuffle: () -> Unit,
    onResetSpread: () -> Unit,
    onRequestMicPermission: () -> Unit
) {
    MysticScaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard {
                Text(
                    "Tarot Reading",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                OutlinedTextField(
                    value = state.question,
                    onValueChange = onQuestionChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Question (optional)") },
                    singleLine = true
                )
                if (!state.hasMicPermission) {
                    NeonButton(
                        text = "Enable Mic for Blow Gesture",
                        onClick = onRequestMicPermission,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            DeckArea(
                state = state,
                onDeckTapDraw = onDeckTapDraw,
                onSwipeShuffle = onSwipeShuffle
            )

            if (state.spread.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(state.spread, key = { it.card.id }) { item ->
                        FlipTarotCard(
                            title = item.card.name,
                            subtitle = if (item.isRevealed) item.card.upright else "Tap to reveal",
                            isRevealed = item.isRevealed,
                            onClick = { onCardFlip(item.card.id) }
                        )
                    }
                }
            }

            if (state.readingSummary.isNotBlank()) {
                GlassCard {
                    Text(
                        text = state.readingSummary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    state.insights.forEach {
                        Text("- $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    state.actions.forEach {
                        Text("Action: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.disclaimer.isNotBlank()) {
                        Text(
                            state.disclaimer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            NeonButton(
                text = "Reset Spread",
                onClick = onResetSpread,
                isLoading = state.isLoadingAi,
                loadingText = "Generating…",
                enabled = !state.isLoadingAi,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DeckArea(
    state: TarotUiState,
    onDeckTapDraw: () -> Unit,
    onSwipeShuffle: () -> Unit
) {
    var dragY by remember { mutableFloatStateOf(0f) }

    val pulse by animateFloatAsState(
        targetValue = if (state.isShuffling) 1f else 0f,
        label = "deckPulse"
    )

    val jitter = rememberInfiniteTransition(label = "jitter").animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 420
                -8f at 0
                8f at 210
                -8f at 420
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "jitterValue"
    )

    val xOffset = if (state.isShuffling) jitter.value else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0x33112244), Color(0x55151F3B))
                )
            )
            .pointerInput(state.shuffleTick) {
                detectVerticalDragGestures(
                    onDragStart = { dragY = 0f },
                    onVerticalDrag = { _, amount -> dragY += amount },
                    onDragEnd = {
                        if (abs(dragY) > 120f) onSwipeShuffle()
                    }
                )
            }
            .padding(16.dp)
            .semantics {
                contentDescription = "Tarot deck area. Swipe vertically to shuffle, or activate draw card button."
            }
    ) {
        repeat(5) { i ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset((xOffset * (i + 1) * 0.2f).roundToInt(), (i * 2)) }
                    .graphicsLayer {
                        rotationZ = ((i - 2) * 2.2f) + (pulse * (if (i % 2 == 0) 4f else -4f))
                    }
                    .size(width = 122.dp, height = 176.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF223A72), Color(0xFF0D1936))
                        )
                    )
            )
        }

        NeonButton(
            text = "Draw Card (${state.deckCount})",
            onClick = onDeckTapDraw,
            isLoading = state.isLoadingAi,
            loadingText = "Generating…",
            enabled = !state.isLoadingAi,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )

        Text(
            text = "Shake to shuffle - Swipe as fallback",
            modifier = Modifier.align(Alignment.TopCenter),
            style = MaterialTheme.typography.bodyMedium
        )

        if (state.fogAlpha > 0f) {
            FogOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(state.fogAlpha),
                intensity = state.fogAlpha
            )
        }
    }
}

@Composable
private fun FlipTarotCard(
    title: String,
    subtitle: String,
    isRevealed: Boolean,
    onClick: () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isRevealed) {
        rotation.animateTo(if (isRevealed) 180f else 0f)
    }

    Box(
        modifier = Modifier
            .size(width = 140.dp, height = 190.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (isRevealed) {
                    "Tarot card $title, revealed. Double tap to flip back."
                } else {
                    "Tarot card face down. Double tap to reveal."
                }
                onClick(label = "Flip tarot card") {
                    onClick()
                    true
                }
            }
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12 * density
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x332A3D66))
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            }
            .padding(1.dp)
    ) {
        if (rotation.value <= 90f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF284E8B), Color(0xFF172844))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("*", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            }
        } else {
            GlassCard(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                contentPadding = PaddingValues(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
