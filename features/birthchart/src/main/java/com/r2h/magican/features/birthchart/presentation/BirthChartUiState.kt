package com.r2h.magican.features.birthchart.presentation

import java.util.UUID

data class BirthChartUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val birthDateIso: String = "",
    val birthTime24h: String = "",
    val birthPlace: String = "",
    val focusArea: String = "",
    val isLoading: Boolean = false,
    val summary: String = "",
    val insights: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val disclaimer: String = "",
    val status: String = "Enter birth details to generate chart insights.",
    val error: String? = null
)
