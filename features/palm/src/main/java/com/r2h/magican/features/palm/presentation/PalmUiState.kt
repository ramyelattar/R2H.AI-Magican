package com.r2h.magican.features.palm.presentation

import java.util.UUID

data class PalmUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val hand: String = "left",
    val observations: String = "",
    val isLoading: Boolean = false,
    val summary: String = "",
    val insights: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val disclaimer: String = "",
    val status: String = "Describe key palm lines and shape.",
    val error: String? = null
)
