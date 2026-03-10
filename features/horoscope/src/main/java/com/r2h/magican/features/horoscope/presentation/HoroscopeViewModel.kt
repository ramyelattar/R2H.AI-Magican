package com.r2h.magican.features.horoscope.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r2h.magican.ai.orchestrator.HoroscopeInput
import com.r2h.magican.ai.orchestrator.domain.AiResponse
import com.r2h.magican.ai.orchestrator.usecase.HoroscopeGenerationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HoroscopeViewModel @Inject constructor(
    private val horoscopeGenerationUseCase: HoroscopeGenerationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HoroscopeUiState())
    val uiState: StateFlow<HoroscopeUiState> = _uiState.asStateFlow()

    fun onSignChanged(value: String) {
        _uiState.update { it.copy(sign = value.take(40), error = null) }
    }

    fun onDateChanged(value: String) {
        _uiState.update { it.copy(dateIso = value.take(20), error = null) }
    }

    fun onFocusChanged(value: String) {
        _uiState.update { it.copy(focusArea = value.take(120), error = null) }
    }

    fun generate() {
        val current = _uiState.value
        if (current.sign.isBlank()) {
            _uiState.update { it.copy(error = "Please provide a zodiac sign.") }
            return
        }
        if (current.dateIso.isBlank()) {
            _uiState.update { it.copy(error = "Please provide a date in ISO format.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = "Generating horoscope...", error = null) }
            
            val response = horoscopeGenerationUseCase.execute(
                HoroscopeInput(
                    sessionId = current.sessionId,
                    locale = "en",
                    sign = current.sign,
                    dateIso = current.dateIso,
                    focusArea = current.focusArea
                )
            )

            when (response) {
                is AiResponse.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            summary = response.content.summary,
                            insights = response.content.insights,
                            actions = response.content.actions,
                            disclaimer = response.content.disclaimer,
                            status = "Horoscope ready."
                        )
                    }
                }
                is AiResponse.Blocked -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = "Request blocked.",
                            error = response.reason
                        )
                    }
                }
                is AiResponse.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = "Horoscope generation failed.",
                            error = response.message
                        )
                    }
                }
            }
        }
    }
}
