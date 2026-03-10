package com.r2h.magican.features.birthchart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r2h.magican.ai.orchestrator.BirthChartInput
import com.r2h.magican.ai.orchestrator.domain.AiResponse
import com.r2h.magican.ai.orchestrator.usecase.BirthChartInterpretationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BirthChartViewModel @Inject constructor(
    private val birthChartInterpretationUseCase: BirthChartInterpretationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BirthChartUiState())
    val uiState: StateFlow<BirthChartUiState> = _uiState.asStateFlow()

    fun onBirthDateChanged(value: String) {
        _uiState.update { it.copy(birthDateIso = value.take(20), error = null) }
    }

    fun onBirthTimeChanged(value: String) {
        _uiState.update { it.copy(birthTime24h = value.take(10), error = null) }
    }

    fun onBirthPlaceChanged(value: String) {
        _uiState.update { it.copy(birthPlace = value.take(120), error = null) }
    }

    fun onFocusChanged(value: String) {
        _uiState.update { it.copy(focusArea = value.take(120), error = null) }
    }

    fun generate() {
        val current = _uiState.value
        if (current.birthDateIso.isBlank() || current.birthTime24h.isBlank() || current.birthPlace.isBlank()) {
            _uiState.update { it.copy(error = "Date, time, and place are required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = "Generating birth chart reading...", error = null) }
            
            val response = birthChartInterpretationUseCase.execute(
                BirthChartInput(
                    sessionId = current.sessionId,
                    locale = "en",
                    birthDateIso = current.birthDateIso,
                    birthTime24h = current.birthTime24h,
                    birthPlace = current.birthPlace,
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
                            status = "Birth chart reading ready."
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
                            status = "Birth chart generation failed.",
                            error = response.message
                        )
                    }
                }
            }
        }
    }
}
