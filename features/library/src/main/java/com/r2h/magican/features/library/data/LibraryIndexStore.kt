package com.r2h.magican.features.library.data

import com.r2h.magican.features.library.domain.LibraryBookmark
import com.r2h.magican.features.library.domain.LibraryDocument
import com.r2h.magican.features.library.domain.PdfMetadata
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class LibraryIndexStore @Inject constructor(
    private val vault: EncryptedVault
) {
    suspend fun load(): List<LibraryDocument> {
        val bytes = vault.readEncryptedIndex(INDEX_FILE_NAME) ?: return emptyList()
        if (bytes.isEmpty()) return emptyList()

        val root = runCatching { JSONObject(bytes.toString(Charsets.UTF_8)) }
            .getOrElse { return emptyList() }

        val docs = root.optJSONArray("documents") ?: JSONArray()
        val out = ArrayList<LibraryDocument>(docs.length())
        for (i in 0 until docs.length()) {
            val item = docs.optJSONObject(i) ?: continue
            item.toDocumentOrNull()?.let(out::add)
        }
        return out
    }

    suspend fun save(documents: List<LibraryDocument>) {
        val root = JSONObject().apply {
            put("version", 1)
            put("documents", JSONArray().apply {
                documents.forEach { put(it.toJson()) }
            })
        }
        vault.writeEncryptedIndex(INDEX_FILE_NAME, root.toString().toByteArray(Charsets.UTF_8))
    }

    private fun JSONObject.toDocumentOrNull(): LibraryDocument? {
        val metadataJson = optJSONObject("metadata") ?: JSONObject()
        val bookmarksJson = optJSONArray("bookmarks") ?: JSONArray()

        val bookmarks = ArrayList<LibraryBookmark>(bookmarksJson.length())
        for (j in 0 until bookmarksJson.length()) {
            val b = bookmarksJson.optJSONObject(j) ?: continue
            bookmarks += LibraryBookmark(
                id = b.optString("id"),
                page = b.optInt("page"),
                label = b.optString("label"),
                createdAtEpochMs = b.optLong("created_at")
            )
        }

        val id = optString("id")
        val vaultFileName = optString("vault_file_name")
        val sha256 = optString("sha256")
        if (id.isBlank() || vaultFileName.isBlank() || sha256.isBlank()) {
            return null
        }

        return LibraryDocument(
            id = id,
            displayName = optString("display_name").ifBlank { "document.pdf" },
            vaultFileName = vaultFileName,
            sha256 = sha256,
            metadata = PdfMetadata(
                title = metadataJson.optString("title").ifBlank { null },
                author = metadataJson.optString("author").ifBlank { null },
                subject = metadataJson.optString("subject").ifBlank { null },
                keywords = metadataJson.optString("keywords").ifBlank { null },
                pageCount = metadataJson.optInt("page_count"),
                fileSizeBytes = metadataJson.optLong("file_size_bytes"),
                importedAtEpochMs = metadataJson.optLong("imported_at")
            ),
            extractedText = optString("extracted_text"),
            summary = optString("summary").ifBlank { null },
            bookmarks = bookmarks.sortedBy { it.page }
        )
    }

    private fun LibraryDocument.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("display_name", displayName)
            put("vault_file_name", vaultFileName)
            put("sha256", sha256)
            put("summary", summary ?: "")
            put("extracted_text", extractedText)
            put("metadata", JSONObject().apply {
                put("title", metadata.title ?: "")
                put("author", metadata.author ?: "")
                put("subject", metadata.subject ?: "")
                put("keywords", metadata.keywords ?: "")
                put("page_count", metadata.pageCount)
                put("file_size_bytes", metadata.fileSizeBytes)
                put("imported_at", metadata.importedAtEpochMs)
            })
            put("bookmarks", JSONArray().apply {
                bookmarks.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("page", b.page)
                        put("label", b.label)
                        put("created_at", b.createdAtEpochMs)
                    })
                }
            })
        }
    }

    private companion object {
        const val INDEX_FILE_NAME = "library_index.enc"
    }
}
