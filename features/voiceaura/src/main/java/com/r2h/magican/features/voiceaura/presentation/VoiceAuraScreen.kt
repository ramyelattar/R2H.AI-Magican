package com.r2h.magican.features.voiceaura.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.r2h.magican.core.design.components.GlassCard
import com.r2h.magican.core.design.components.MysticScaffold
import com.r2h.magican.core.design.components.NeonButton
import com.r2h.magican.features.voiceaura.domain.VoiceSessionMode

@Composable
fun VoiceAuraScreen(
    viewModel: VoiceAuraViewModel = hiltViewModel()
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
    val sessionLoading = state.isSessionTransitioning || state.isLoadingInterpretation
    val sessionLoadingText = when {
        state.isSessionTransitioning -> state.sessionTransitionLabel ?: "Working…"
        state.isLoadingInterpretation -> "Interpreting…"
        else -> "Working…"
    }

    LaunchedEffect(hasMicPermission) {
        viewModel.onMicPermissionChanged(hasMicPermission)
    }

    MysticScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard {
                Text(
                    "Voice Aura",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    state.status,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(
                        text = "Hum",
                        onClick = { viewModel.selectMode(VoiceSessionMode.Hum) },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = "Breath",
                        onClick = { viewModel.selectMode(VoiceSessionMode.Breath) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = if (state.mode == VoiceSessionMode.Hum) {
                        "Hum session: hold one stable tone."
                    } else {
                        "Breath session: slow inhale, slow exhale."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            GlassCard {
                WaveformView(amplitudes = state.waveform)
                Text(
                    text = "Energy: ${state.currentEnergy.asPct()}  |  Pitch: ${state.currentPitchHz.asHz()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Elapsed: ${state.elapsedMs.asClock()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                NeonButton(
                    text = if (state.isRecording) "Stop Session" else "Start Session",
                    onClick = {
                        if (!state.hasMicPermission && !state.isRecording) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleSession()
                        }
                    },
                    isLoading = sessionLoading,
                    loadingText = sessionLoadingText,
                    enabled = !sessionLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!state.hasMicPermission) {
                    NeonButton(
                        text = "Enable Microphone",
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (state.avgEnergy != null) {
                GlassCard {
                    Text("Session Metrics", style = MaterialTheme.typography.titleMedium)
                    Text("Avg energy: ${state.avgEnergy.asPct()}", style = MaterialTheme.typography.bodyMedium)
                    Text("Avg pitch: ${state.avgPitchHz.asHz()}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Pitch stability: ${state.pitchStability.asPct()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Breaths/min: ${state.breathsPerMinute?.let { "%.1f".format(it) } ?: "n/a"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (state.interpretationSummary.isNotBlank()) {
                GlassCard {
                    Text(state.interpretationSummary, style = MaterialTheme.typography.titleMedium)
                    state.insights.forEach { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
                    state.actions.forEach { Text("Action: $it", style = MaterialTheme.typography.bodyMedium) }
                    Text(state.disclaimer, style = MaterialTheme.typography.bodyMedium)
                }
            }

            state.errorMessage?.let { error ->
                GlassCard {
                    Text(error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun Float?.asHz(): String = this?.let { "${"%.1f".format(it)} Hz" } ?: "n/a"

private fun Float?.asPct(): String = this?.let { "${"%.0f".format(it * 100f)}%" } ?: "n/a"

private fun Long.asClock(): String {
    val sec = this / 1000L
    val min = sec / 60L
    val rem = sec % 60L
    return "%02d:%02d".format(min, rem)
}
