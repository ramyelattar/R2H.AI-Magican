package com.r2h.magican.features.library.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.r2h.magican.core.design.components.GlassCard
import com.r2h.magican.core.design.components.NeonButton
import com.r2h.magican.features.library.domain.LibraryDocument

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isImporting = state.busyAction is LibraryBusyAction.Importing

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importPdf(uri)
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            Text(
                "Library Vault",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                state.status,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search in metadata, notes, extracted text") },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton(
                    text = "Import PDF",
                    onClick = { importLauncher.launch(arrayOf("application/pdf")) },
                    isLoading = isImporting,
                    loadingText = "Importing…",
                    enabled = !isImporting,
                    modifier = Modifier.weight(1f)
                )
            }

            state.errorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(state.documents, key = { it.id }) { doc ->
                val isSummarizing =
                    (state.busyAction as? LibraryBusyAction.Summarizing)?.documentId == doc.id
                val isDeleting =
                    (state.busyAction as? LibraryBusyAction.Deleting)?.documentId == doc.id

                DocumentCard(
                    document = doc,
                    isSummarizing = isSummarizing,
                    isDeleting = isDeleting,
                    onBookmark = { viewModel.toggleBookmark(doc.id, page = 1, label = "Page 1") },
                    onSummarize = { viewModel.summarize(doc.id) },
                    onDelete = { viewModel.delete(doc.id) }
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: LibraryDocument,
    isSummarizing: Boolean,
    isDeleting: Boolean,
    onBookmark: () -> Unit,
    onSummarize: () -> Unit,
    onDelete: () -> Unit
) {
    val isDocumentBusy = isSummarizing || isDeleting

    val metadataLine = remember(document.metadata.pageCount, document.metadata.fileSizeBytes) {
        "Pages: ${document.metadata.pageCount}  |  Size: ${document.metadata.fileSizeBytes} bytes"
    }
    val bookmarksLine = remember(document.bookmarks) {
        if (document.bookmarks.isEmpty()) null else "Bookmarks: " + document.bookmarks.joinToString { "p${it.page}" }
    }

    GlassCard {
        Text(document.displayName, style = MaterialTheme.typography.titleMedium)
        Text(text = metadataLine, style = MaterialTheme.typography.bodyMedium)
        if (!document.metadata.author.isNullOrBlank()) {
            Text("Author: ${document.metadata.author}", style = MaterialTheme.typography.bodyMedium)
        }
        if (!document.metadata.title.isNullOrBlank()) {
            Text("Title: ${document.metadata.title}", style = MaterialTheme.typography.bodyMedium)
        }

        Text(
            text = document.summary ?: "No summary yet.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (bookmarksLine != null) {
            Text(
                text = bookmarksLine,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonButton(
                text = "Bookmark",
                onClick = onBookmark,
                enabled = !isDocumentBusy,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Bookmark ${document.displayName}" }
            )
            NeonButton(
                text = "Summarize",
                onClick = onSummarize,
                isLoading = isSummarizing,
                loadingText = "Summarizing…",
                enabled = !isDocumentBusy,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Summarize ${document.displayName}" }
            )
            NeonButton(
                text = "Delete",
                onClick = onDelete,
                isLoading = isDeleting,
                loadingText = "Deleting…",
                enabled = !isDocumentBusy,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Delete ${document.displayName}" }
            )
        }
    }
}
