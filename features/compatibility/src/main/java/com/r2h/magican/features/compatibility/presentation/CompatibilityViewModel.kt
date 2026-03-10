package com.r2h.magican.features.compatibility.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r2h.magican.ai.orchestrator.CompatibilityInput
import com.r2h.magican.ai.orchestrator.domain.AiResponse
import com.r2h.magican.ai.orchestrator.usecase.CompatibilityAnalysisUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CompatibilityViewModel @Inject constructor(
    private val compatibilityAnalysisUseCase: CompatibilityAnalysisUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompatibilityUiState())
    val uiState: StateFlow<CompatibilityUiState> = _uiState.asStateFlow()

    fun onPersonAChanged(value: String) {
        _uiState.update { it.copy(personA = value.take(120), error = null) }
    }

    fun onPersonBChanged(value: String) {
        _uiState.update { it.copy(personB = value.take(120), error = null) }
    }

    fun onContextChanged(value: String) {
        _uiState.update { it.copy(context = value.take(500), error = null) }
    }

    fun analyze() {
        val current = _uiState.value
        if (current.personA.isBlank() || current.personB.isBlank()) {
            _uiState.update { it.copy(error = "Please fill both profiles.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = "Analyzing compatibility...", error = null) }
            
            val response = compatibilityAnalysisUseCase.execute(
                CompatibilityInput(
                    sessionId = current.sessionId,
                    locale = "en",
                    personA = current.personA,
                    personB = current.personB,
                    context = current.context
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
                            status = "Compatibility reading ready."
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
                            status = "Compatibility analysis failed.",
                            error = response.message
                        )
                    }
                }
            }
        }
    }
}
