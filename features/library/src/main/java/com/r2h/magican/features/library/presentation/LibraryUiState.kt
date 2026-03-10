package com.r2h.magican.features.library.presentation

import com.r2h.magican.features.library.domain.LibraryDocument

sealed interface LibraryBusyAction {
    data object Importing : LibraryBusyAction
    data class Summarizing(val documentId: String) : LibraryBusyAction
    data class Deleting(val documentId: String) : LibraryBusyAction
}

data class LibraryUiState(
    val query: String = "",
    val documents: List<LibraryDocument> = emptyList(),
    val selectedDocumentId: String? = null,
    val busyAction: LibraryBusyAction? = null,
    val status: String = "Import PDFs to build your encrypted vault.",
    val errorMessage: String? = null
) {
    val isBusy: Boolean get() = busyAction != null
}
