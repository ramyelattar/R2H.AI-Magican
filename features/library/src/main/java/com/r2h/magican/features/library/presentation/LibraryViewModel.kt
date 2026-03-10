package com.r2h.magican.features.library.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r2h.magican.features.library.data.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.initialize()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        status = "Library initialization failed.",
                        errorMessage = t.message ?: "Unknown initialization error"
                    )
                }
            }
        }

        viewModelScope.launch {
            // Debounce search queries by 300ms to avoid excessive computation
            val debouncedQuery = query.debounce(300L)
            
            combine(repository.documents, debouncedQuery) { docs, q ->
                val filtered = if (q.isBlank()) docs else repository.search(q)
                q to filtered
            }.collect { (q, docs) ->
                _uiState.update { it.copy(query = q, documents = docs) }
            }
        }
    }

    fun onQueryChanged(value: String) {
        query.value = value
    }

    fun importPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busyAction = LibraryBusyAction.Importing,
                    errorMessage = null,
                    status = "Importing PDF..."
                )
            }
            try {
                val imported = repository.importPdf(uri)
                _uiState.update { state ->
                    state.copy(
                        busyAction = null,
                        status = "Imported ${imported.displayName} into encrypted vault."
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        busyAction = null,
                        status = "Import failed.",
                        errorMessage = t.message ?: "Unknown import error"
                    )
                }
            }
        }
    }

    fun summarize(documentId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busyAction = LibraryBusyAction.Summarizing(documentId),
                    errorMessage = null,
                    status = "Running local summarization..."
                )
            }
            try {
                repository.summarize(documentId)
                _uiState.update { it.copy(busyAction = null, status = "Summary updated.") }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        busyAction = null,
                        status = "Summarization failed.",
                        errorMessage = t.message ?: "Unknown summarization error"
                    )
                }
            }
        }
    }

    fun toggleBookmark(documentId: String, page: Int = 1, label: String = "Page 1") {
        viewModelScope.launch {
            try {
                repository.toggleBookmark(documentId, page, label)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(errorMessage = t.message ?: "Bookmark action failed")
                }
            }
        }
    }

    fun delete(documentId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busyAction = LibraryBusyAction.Deleting(documentId),
                    errorMessage = null,
                    status = "Deleting document..."
                )
            }
            try {
                repository.delete(documentId)
                _uiState.update { it.copy(busyAction = null, status = "Document deleted.") }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        busyAction = null,
                        status = "Delete failed.",
                        errorMessage = t.message ?: "Unknown delete error"
                    )
                }
            }
        }
    }
}
