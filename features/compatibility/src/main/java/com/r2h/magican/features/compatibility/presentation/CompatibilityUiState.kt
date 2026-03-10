package com.r2h.magican.features.compatibility.presentation

import java.util.UUID

data class CompatibilityUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val personA: String = "",
    val personB: String = "",
    val context: String = "",
    val isLoading: Boolean = false,
    val summary: String = "",
    val insights: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val disclaimer: String = "",
    val status: String = "Enter two profiles to assess compatibility.",
    val error: String? = null
)
