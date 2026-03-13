package com.r2h.magican.features.library.data

import android.net.Uri
import com.r2h.magican.features.library.domain.LibraryBookmark
import com.r2h.magican.features.library.domain.LibraryDocument
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class LibraryRepository @Inject constructor(
    private val vault: EncryptedVault,
    private val indexStore: LibraryIndexStore,
    private val metadataExtractor: PdfMetadataExtractor,
    private val textExtractor: PdfTextExtractor,
    private val summarizer: LocalPdfSummarizer,
    private val searchIndex: LibrarySearchIndex
) {
    private val mutex = Mutex()
    private val _documents = MutableStateFlow<List<LibraryDocument>>(emptyList())
    val documents: StateFlow<List<LibraryDocument>> = _documents.asStateFlow()

    suspend fun initialize() {
        mutex.withLock {
            val docs = indexStore.load().sortedByDescending { it.metadata.importedAtEpochMs }
            _documents.value = docs
            searchIndex.rebuild(docs)
        }
    }

    suspend fun importPdf(uri: Uri): LibraryDocument {
        val id = UUID.randomUUID().toString()
        val (displayName, metadataBase) = metadataExtractor.extract(uri)
        val extracted = textExtractor.extract(uri)
        val receipt = vault.importPdf(id, uri)
        val document = LibraryDocument(
            id = id,
            displayName = displayName,
            vaultFileName = receipt.vaultFileName,
            sha256 = receipt.sha256,
            metadata = metadataBase.copy(fileSizeBytes = maxOf(metadataBase.fileSizeBytes, receipt.bytesCopied)),
            extractedText = extracted,
            summary = null,
            bookmarks = emptyList()
        )

        return mutex.withLock {
            val duplicate = _documents.value.firstOrNull { it.sha256 == document.sha256 }
            if (duplicate != null) {
                vault.delete(document.vaultFileName)
                return@withLock duplicate
            }

            val updated = (_documents.value + document).sortedByDescending { it.metadata.importedAtEpochMs }
            _documents.value = updated
            indexStore.save(updated)
            searchIndex.rebuild(updated)
            document
        }
    }

    suspend fun delete(documentId: String) = mutex.withLock {
        val current = _documents.value
        val target = current.firstOrNull { it.id == documentId } ?: return@withLock
        vault.delete(target.vaultFileName)
        val updated = current.filterNot { it.id == documentId }
        _documents.value = updated
        searchIndex.rebuild(updated)
        indexStore.save(updated)
    }

    suspend fun toggleBookmark(documentId: String, page: Int, label: String = "Page $page") = mutex.withLock {
        val updated = _documents.value.map { doc ->
            if (doc.id != documentId) return@map doc
            if (page <= 0 || (doc.metadata.pageCount > 0 && page > doc.metadata.pageCount)) return@map doc

            val existing = doc.bookmarks.firstOrNull { it.page == page }
            if (existing != null) {
                doc.copy(bookmarks = doc.bookmarks.filterNot { it.id == existing.id })
            } else {
                doc.copy(bookmarks = (doc.bookmarks + LibraryBookmark(page = page, label = label)).sortedBy { it.page })
            }
        }
        _documents.value = updated
        searchIndex.rebuild(updated)
        indexStore.save(updated)
    }

    suspend fun summarize(documentId: String): String {
        val target = mutex.withLock {
            _documents.value.firstOrNull { it.id == documentId } ?: error("Document not found")
        }
        val summary = summarizer.summarize(target)

        return mutex.withLock {
            val exists = _documents.value.any { it.id == documentId }
            if (!exists) error("Document not found")

            val updated = _documents.value.map { doc ->
                if (doc.id == documentId) doc.copy(summary = summary) else doc
            }
            _documents.value = updated
            indexStore.save(updated)
            searchIndex.rebuild(updated)
            summary
        }
    }

    fun search(query: String): List<LibraryDocument> {
        val q = query.trim()
        // Capture a single consistent snapshot to avoid two diverging reads
        // under concurrent mutation (delete/import hold the mutex while updating _documents).
        val snapshot = _documents.value
        if (q.isBlank()) return snapshot

        val matchingIds = searchIndex.search(q, maxResults = 100)
        val docMap = snapshot.associateBy { it.id }

        return matchingIds.mapNotNull { docMap[it] }
    }
}
