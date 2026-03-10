package com.r2h.magican.features.voiceaura.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r2h.magican.ai.orchestrator.VoiceAuraInput
import com.r2h.magican.ai.orchestrator.domain.AiResponse
import com.r2h.magican.ai.orchestrator.usecase.VoiceAuraAnalysisUseCase
import com.r2h.magican.features.voiceaura.analysis.PitchEnergyAnalyzer
import com.r2h.magican.features.voiceaura.analysis.VoiceSessionAggregator
import com.r2h.magican.features.voiceaura.audio.VoiceSessionEngine
import com.r2h.magican.features.voiceaura.domain.SessionAnalytics
import com.r2h.magican.features.voiceaura.domain.VoiceSessionMode
import com.r2h.magican.features.voiceaura.integration.VoiceAuraAiInterpreter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class VoiceAuraViewModel @Inject constructor(
    private val sessionEngine: VoiceSessionEngine,
    private val analyzer: PitchEnergyAnalyzer,
    private val voiceAuraAnalysisUseCase: VoiceAuraAnalysisUseCase,
    private val aiInterpreter: VoiceAuraAiInterpreter
) : ViewModel() {

    private val aggregator = VoiceSessionAggregator()
    private var startMs: Long = 0L
    private var sessionJob: Job? = null
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow(VoiceAuraUiState())
    val uiState = _uiState.asStateFlow()

    fun onMicPermissionChanged(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasMicPermission = granted,
                status = if (granted) {
                    if (it.status.startsWith("Mic permission")) "Microphone ready." else it.status
                } else {
                    "Mic permission is required to record a session."
                }
            )
        }
    }

    fun selectMode(mode: VoiceSessionMode) {
        if (_uiState.value.isRecording) return
        _uiState.update {
            it.copy(
                mode = mode,
                status = if (mode == VoiceSessionMode.Hum) {
                    "Hum a steady tone near the microphone."
                } else {
                    "Take slow inhale/exhale cycles."
                }
            )
        }
    }

    fun toggleSession() {
        if (_uiState.value.isRecording) stopSession() else startSession()
    }

    private fun startSession() {
        if (_uiState.value.isRecording) return
        if (!_uiState.value.hasMicPermission) {
            _uiState.update {
                it.copy(
                    status = "Mic permission is required to start recording.",
                    errorMessage = "Please allow microphone access."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isSessionTransitioning = true,
                sessionTransitionLabel = "Starting…"
            )
        }

        aggregator.reset()
        startMs = SystemClock.elapsedRealtime()

        _uiState.update {
            it.copy(
                isRecording = true,
                isSessionTransitioning = false,
                sessionTransitionLabel = null,
                elapsedMs = 0L,
                waveform = List(96) { 0f },
                interpretationSummary = "",
                insights = emptyList(),
                actions = emptyList(),
                disclaimer = "",
                status = "Recording...",
                errorMessage = null
            )
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                _uiState.update { state ->
                    state.copy(elapsedMs = SystemClock.elapsedRealtime() - startMs)
                }
                delay(100L)
            }
        }

        sessionJob = viewModelScope.launch {
            sessionEngine.frames(_uiState.value.mode)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            isSessionTransitioning = false,
                            sessionTransitionLabel = null,
                            status = "Session failed.",
                            errorMessage = error.message ?: "Unknown audio error"
                        )
                    }
                }
                .collect { frame ->
                    val features = analyzer.analyze(frame)
                    aggregator.ingest(features, frame.timestampMs, _uiState.value.mode)

                    _uiState.update { state ->
                        val nextWave = (state.waveform + features.energy.coerceIn(0f, 1f)).takeLast(96)
                        state.copy(
                            waveform = nextWave,
                            currentEnergy = features.energy,
                            currentPitchHz = features.pitchHz
                        )
                    }
                }
        }
    }

    private fun stopSession() {
        if (!_uiState.value.isRecording) return

        _uiState.update {
            it.copy(
                isSessionTransitioning = true,
                sessionTransitionLabel = "Stopping…"
            )
        }

        sessionJob?.cancel()
        timerJob?.cancel()
        sessionJob = null
        timerJob = null

        val duration = (SystemClock.elapsedRealtime() - startMs).coerceAtLeast(0L)
        val analytics = aggregator.snapshot(_uiState.value.mode, duration)

        _uiState.update {
            it.copy(
                isRecording = false,
                isSessionTransitioning = false,
                sessionTransitionLabel = null,
                elapsedMs = duration,
                avgEnergy = analytics.avgEnergy,
                avgPitchHz = analytics.avgPitchHz,
                pitchStability = analytics.pitchStability,
                breathsPerMinute = analytics.breathsPerMinute,
                status = "Analyzing session..."
            )
        }

        interpretSession(analytics)
    }

    private fun interpretSession(analytics: SessionAnalytics) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInterpretation = true) }

            val transcript = aiInterpreter.buildTranscript(analytics)
            val toneHint = aiInterpreter.toneHint(analytics)

            val response = voiceAuraAnalysisUseCase.execute(
                VoiceAuraInput(
                    sessionId = _uiState.value.sessionId,
                    locale = "en",
                    transcript = transcript,
                    toneHint = toneHint
                )
            )

            when (response) {
                is AiResponse.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingInterpretation = false,
                            interpretationSummary = response.content.summary,
                            insights = response.content.insights,
                            actions = response.content.actions,
                            disclaimer = response.content.disclaimer,
                            status = "Interpretation ready."
                        )
                    }
                }
                is AiResponse.Blocked -> {
                    _uiState.update {
                        it.copy(
                            isLoadingInterpretation = false,
                            status = "Request blocked.",
                            errorMessage = response.reason
                        )
                    }
                }
                is AiResponse.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingInterpretation = false,
                            status = "Interpretation failed.",
                            errorMessage = response.message
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        sessionJob?.cancel()
        timerJob?.cancel()
        super.onCleared()
    }
}
