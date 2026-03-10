package com.r2h.magican.features.palm.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r2h.magican.ai.orchestrator.PalmInput
import com.r2h.magican.ai.orchestrator.domain.AiResponse
import com.r2h.magican.ai.orchestrator.usecase.PalmReadingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PalmViewModel @Inject constructor(
    private val palmReadingUseCase: PalmReadingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PalmUiState())
    val uiState: StateFlow<PalmUiState> = _uiState.asStateFlow()

    fun onHandChanged(hand: String) {
        _uiState.update { it.copy(hand = hand.lowercase(), error = null) }
    }

    fun onObservationsChanged(value: String) {
        _uiState.update { it.copy(observations = value.take(2000), error = null) }
    }

    fun analyze() {
        val current = _uiState.value
        if (current.observations.isBlank()) {
            _uiState.update { it.copy(error = "Please add palm observations first.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = "Analyzing palm...", error = null) }
            
            val response = palmReadingUseCase.execute(
                PalmInput(
                    sessionId = current.sessionId,
                    locale = "en",
                    hand = current.hand,
                    observations = current.observations
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
                            status = "Palm reading ready."
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
                            status = "Palm analysis failed.",
                            error = response.message
                        )
                    }
                }
            }
        }
    }
}
