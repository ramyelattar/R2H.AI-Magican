package com.r2h.magican.features.horoscope.presentation

import java.time.LocalDate
import java.util.UUID

data class HoroscopeUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val sign: String = "Aries",
    val dateIso: String = LocalDate.now().toString(),
    val focusArea: String = "",
    val isLoading: Boolean = false,
    val summary: String = "",
    val insights: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val disclaimer: String = "",
    val status: String = "Choose sign and focus area for daily guidance.",
    val error: String? = null
)
