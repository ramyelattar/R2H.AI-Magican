package com.r2h.magican.features.voiceaura.presentation

import com.r2h.magican.features.voiceaura.domain.VoiceSessionMode
import java.util.UUID

data class VoiceAuraUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val mode: VoiceSessionMode = VoiceSessionMode.Hum,
    val hasMicPermission: Boolean = false,
    val isRecording: Boolean = false,
    val isSessionTransitioning: Boolean = false,
    val sessionTransitionLabel: String? = null,
    val elapsedMs: Long = 0L,
    val waveform: List<Float> = List(96) { 0f },
    val currentEnergy: Float = 0f,
    val currentPitchHz: Float? = null,
    val avgEnergy: Float? = null,
    val avgPitchHz: Float? = null,
    val pitchStability: Float? = null,
    val breathsPerMinute: Float? = null,
    val isLoadingInterpretation: Boolean = false,
    val interpretationSummary: String = "",
    val insights: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val disclaimer: String = "",
    val status: String = "Select mode and start session.",
    val errorMessage: String? = null
)
