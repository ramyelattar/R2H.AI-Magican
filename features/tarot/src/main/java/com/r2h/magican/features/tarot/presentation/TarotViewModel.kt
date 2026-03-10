package com.r2h.magican.features.tarot.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r2h.magican.ai.orchestrator.TarotInput
import com.r2h.magican.ai.orchestrator.domain.AiResponse
import com.r2h.magican.ai.orchestrator.usecase.TarotReadingUseCase
import com.r2h.magican.core.audio.Sfx
import com.r2h.magican.core.audio.SfxType
import com.r2h.magican.core.haptics.HapticPatternType
import com.r2h.magican.core.haptics.Haptics
import com.r2h.magican.core.sensors.ShakeConfig
import com.r2h.magican.core.sensors.ShakeDetector
import com.r2h.magican.features.tarot.domain.TarotDeckFactory
import com.r2h.magican.features.tarot.integration.BlowConfig
import com.r2h.magican.features.tarot.integration.BlowDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

@HiltViewModel
class TarotViewModel @Inject constructor(
    private val shakeDetector: ShakeDetector,
    private val blowDetector: BlowDetector,
    private val sfx: Sfx,
    private val haptics: Haptics,
    private val tarotReadingUseCase: TarotReadingUseCase
) : ViewModel() {

    private val deckMutex = Mutex()
    private var deck = TarotDeckFactory.majorArcana().shuffled(Random(System.currentTimeMillis())).toMutableList()
    private var lastShuffleAtMs = 0L
    private var isScreenActive = false
    private var shakeJob: Job? = null
    private var blowJob: Job? = null
    private var readingJob: Job? = null
    private var readingGeneration: Long = 0L
    private var isRequestingReading = false

    private val _uiState = MutableStateFlow(TarotUiState(deckCount = deck.size))
    val uiState = _uiState.asStateFlow()

    fun setScreenActive(active: Boolean) {
        isScreenActive = active
        if (active) {
            startShakeObservation()
            if (_uiState.value.hasMicPermission) {
                startBlowObservation()
            }
        } else {
            stopShakeObservation()
            stopBlowObservation()
        }
    }

    fun onMicPermissionChanged(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasMicPermission = granted,
                status = if (granted) {
                    if (it.status.startsWith("Mic permission")) {
                        "Mic ready. Blow to clear fog."
                    } else {
                        it.status
                    }
                } else {
                    "Mic permission is required for blow-to-clear fog."
                }
            )
        }
        if (granted && isScreenActive) {
            startBlowObservation()
        } else {
            stopBlowObservation()
        }
    }

    fun onQuestionChanged(value: String) {
        _uiState.update { it.copy(question = value.take(180)) }
    }

    fun onDeckTapDraw() {
        viewModelScope.launch {
            deckMutex.withLock {
                if (_uiState.value.spread.size >= 3) return@withLock
                if (deck.isEmpty()) refillDeck()

                val next = deck.removeFirst()
                _uiState.update {
                    it.copy(
                        deckCount = deck.size,
                        spread = it.spread + SpreadCardUi(card = next, isRevealed = false),
                        readingSummary = "",
                        insights = emptyList(),
                        actions = emptyList(),
                        disclaimer = ""
                    )
                }
            }
            sfx.play(SfxType.Tap)
            haptics.tick()
        }
    }

    fun onCardFlip(cardId: Int) {
        val shouldRequestReading = _uiState.value.let { state ->
            if (state.isLoadingAi) return
            if (state.spread.none { it.card.id == cardId }) return

            val target = state.spread.firstOrNull { it.card.id == cardId } ?: return
            if (target.isRevealed) return

            val spreadAfterFlip = state.spread.map { item ->
                if (item.card.id == cardId) item.copy(isRevealed = true) else item
            }
            if (spreadAfterFlip.size == 3 && spreadAfterFlip.all { it.isRevealed }) {
                _uiState.update { state.copy(spread = spreadAfterFlip) }
                return@let true
            }

            _uiState.update { state.copy(spread = spreadAfterFlip) }
            false
        }
            ?: return

        if (shouldRequestReading) {
            sfx.play(SfxType.Swipe)
            haptics.pattern(HapticPatternType.Selection)
            requestReading()
        } else {
            sfx.play(SfxType.Swipe)
            haptics.pattern(HapticPatternType.Selection)
        }
    }

    fun onSwipeShuffle() {
        viewModelScope.launch { shuffleDeck(reason = "Swipe shuffle fallback used.") }
    }

    fun onResetSpread() {
        readingJob?.cancel()
        readingJob = null
        readingGeneration += 1L
        isRequestingReading = false

        viewModelScope.launch {
            deckMutex.withLock {
                refillDeck()
                _uiState.update {
                    it.copy(
                        deckCount = deck.size,
                        spread = emptyList(),
                        readingSummary = "",
                        insights = emptyList(),
                        actions = emptyList(),
                        disclaimer = "",
                        fogAlpha = 0.82f,
                        isLoadingAi = false
                    )
                }
            }
        }
    }

    private fun startShakeObservation() {
        if (shakeJob?.isActive == true) return
        shakeJob = viewModelScope.launch {
            shakeDetector.events(
                ShakeConfig(
                    triggerGravity = 2.1f,
                    minShakesInWindow = 2,
                    windowMs = 750L,
                    minGapMs = 120L
                )
            ).catch { _uiState.update { it.copy(status = "Shake unavailable. Use swipe to shuffle.") } }
                .collect { shuffleDeck(reason = "Shake detected. Deck shuffled.") }
        }
    }

    private fun stopShakeObservation() {
        shakeJob?.cancel()
        shakeJob = null
    }

    private fun startBlowObservation() {
        if (!_uiState.value.hasMicPermission) return
        if (blowJob?.isActive == true) return
        blowJob = viewModelScope.launch {
            blowDetector.events(
                BlowConfig(
                    amplitudeThreshold = 0.18f,
                    minHoldMs = 100L,
                    cooldownMs = 500L
                )
            ).catch {
                _uiState.update { it.copy(status = "Mic unavailable. Fog remains until permission is granted.") }
            }.collect { event ->
                _uiState.update { state ->
                    val next = (state.fogAlpha - (event.intensity * 0.55f)).coerceAtLeast(0f)
                    state.copy(
                        fogAlpha = next,
                        status = if (next == 0f) "Fog cleared." else "Blow detected: clearing fog..."
                    )
                }
                if (_uiState.value.fogAlpha == 0f) {
                    sfx.play(SfxType.MysticChime)
                    haptics.confirm()
                }
            }
        }
    }

    private fun stopBlowObservation() {
        blowJob?.cancel()
        blowJob = null
    }

    private suspend fun shuffleDeck(reason: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastShuffleAtMs < 650L) return
        lastShuffleAtMs = now

        _uiState.update { it.copy(isShuffling = true, status = reason) }

        deckMutex.withLock {
            val inSpread = _uiState.value.spread.map { it.card }
            deck.addAll(inSpread)
            deck.shuffle()
            _uiState.update {
                it.copy(
                    spread = emptyList(),
                    deckCount = deck.size,
                    readingSummary = "",
                    insights = emptyList(),
                    actions = emptyList(),
                    disclaimer = "",
                    shuffleTick = it.shuffleTick + 1
                )
            }
        }

        sfx.play(SfxType.Swipe)
        haptics.pattern(HapticPatternType.MysticPulse)
        delay(420L)
        _uiState.update { it.copy(isShuffling = false) }
    }

    private fun requestReading() {
        if (readingJob?.isActive == true) return
        if (isRequestingReading) return
        if (_uiState.value.isLoadingAi) return
        val generation = readingGeneration + 1L
        readingGeneration = generation
        readingJob = viewModelScope.launch {
            isRequestingReading = true

            _uiState.update { it.copy(isLoadingAi = true, status = "Consulting the cards...") }

            val state = _uiState.value
            val cards = state.spread.joinToString(", ") { it.card.name }
            val input = TarotInput(
                sessionId = state.sessionId,
                locale = "en",
                question = state.question.ifBlank { "General guidance" },
                spread = "3-card",
                cardsDrawn = cards
            )

            try {
                val response = tarotReadingUseCase.execute(input)
                if (generation != readingGeneration) return@launch

                when (response) {
                    is AiResponse.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoadingAi = false,
                                readingSummary = response.content.summary,
                                insights = response.content.insights,
                                actions = response.content.actions,
                                disclaimer = response.content.disclaimer,
                                status = "Reading complete."
                            )
                        }
                        sfx.play(SfxType.Success)
                        haptics.confirm()
                    }
                    is AiResponse.Blocked -> {
                        _uiState.update {
                            it.copy(
                                isLoadingAi = false,
                                status = "Request blocked.",
                                readingSummary = response.reason
                            )
                        }
                    }
                    is AiResponse.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingAi = false,
                                status = "Reading failed: ${response.message}",
                                readingSummary = response.message
                            )
                        }
                    }
                }
            } finally {
                if (generation == readingGeneration) {
                    isRequestingReading = false
                    readingJob = null
                    _uiState.update { state ->
                        if (!state.isLoadingAi) state else state.copy(isLoadingAi = false)
                    }
                }
            }
        }
    }

    private fun refillDeck() {
        deck = TarotDeckFactory.majorArcana()
            .shuffled(Random(System.nanoTime()))
            .toMutableList()
    }

    override fun onCleared() {
        readingJob?.cancel()
        stopShakeObservation()
        stopBlowObservation()
        super.onCleared()
    }
}
