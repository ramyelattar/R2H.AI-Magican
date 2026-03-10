package com.r2h.magican.features.dreams.presentation

import java.util.UUID

data class DreamsUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val dreamText: String = "",
    val mood: String = "",
    val isLoading: Boolean = false,
    val summary: String = "",
    val insights: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val disclaimer: String = "",
    val status: String = "Describe your dream and current mood.",
    val error: String? = null
)
