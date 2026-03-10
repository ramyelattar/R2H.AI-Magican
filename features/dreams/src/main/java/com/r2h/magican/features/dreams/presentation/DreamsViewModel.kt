package com.r2h.magican.features.dreams.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r2h.magican.ai.orchestrator.DreamsInput
import com.r2h.magican.ai.orchestrator.domain.AiResponse
import com.r2h.magican.ai.orchestrator.usecase.DreamInterpretationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DreamsViewModel @Inject constructor(
    private val dreamInterpretationUseCase: DreamInterpretationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DreamsUiState())
    val uiState: StateFlow<DreamsUiState> = _uiState.asStateFlow()

    fun onDreamTextChanged(value: String) {
        _uiState.update { it.copy(dreamText = value.take(4000), error = null) }
    }

    fun onMoodChanged(value: String) {
        _uiState.update { it.copy(mood = value.take(120), error = null) }
    }

    fun interpret() {
        val current = _uiState.value
        if (current.dreamText.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your dream text.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = "Interpreting dream...", error = null) }
            
            val response = dreamInterpretationUseCase.execute(
                DreamsInput(
                    sessionId = current.sessionId,
                    locale = "en",
                    dreamText = current.dreamText,
                    mood = current.mood
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
                            status = "Dream interpretation ready."
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
                            status = "Interpretation failed.",
                            error = response.message
                        )
                    }
                }
            }
        }
    }
}
