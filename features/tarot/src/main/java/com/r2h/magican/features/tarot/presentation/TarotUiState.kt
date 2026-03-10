package com.r2h.magican.features.tarot.presentation

import com.r2h.magican.features.tarot.domain.TarotCard
import java.util.UUID

data class SpreadCardUi(
    val card: TarotCard,
    val isRevealed: Boolean = false
)

data class TarotUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val question: String = "",
    val deckCount: Int = 0,
    val spread: List<SpreadCardUi> = emptyList(),
    val isShuffling: Boolean = false,
    val shuffleTick: Int = 0,
    val fogAlpha: Float = 0.82f,
    val readingSummary: String = "",
    val insights: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val disclaimer: String = "",
    val isLoadingAi: Boolean = false,
    val hasMicPermission: Boolean = false,
    val status: String = "Shake to shuffle, blow to clear fog. Swipe deck if shake is unavailable."
)
