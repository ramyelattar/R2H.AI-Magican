package com.r2h.magican.features.library.domain

import java.util.UUID

data class PdfMetadata(
    val title: String?,
    val author: String?,
    val subject: String?,
    val keywords: String?,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val importedAtEpochMs: Long
)

data class LibraryBookmark(
    val id: String = UUID.randomUUID().toString(),
    val page: Int,
    val label: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class LibraryDocument(
    val id: String,
    val displayName: String,
    val vaultFileName: String,
    val sha256: String,
    val metadata: PdfMetadata,
    val extractedText: String,
    val summary: String?,
    val bookmarks: List<LibraryBookmark>
) {
    fun searchableBlob(): String {
        return buildString {
            append(displayName).append(' ')
            append(metadata.title.orEmpty()).append(' ')
            append(metadata.author.orEmpty()).append(' ')
            append(metadata.subject.orEmpty()).append(' ')
            append(metadata.keywords.orEmpty()).append(' ')
            append(summary.orEmpty()).append(' ')
            bookmarks.forEach { append(it.label).append(' ') }
            append(extractedText)
        }.lowercase()
    }
}
