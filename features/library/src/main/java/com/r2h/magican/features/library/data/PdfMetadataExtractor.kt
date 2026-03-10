package com.r2h.magican.features.library.data

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.r2h.magican.features.library.domain.PdfMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PdfMetadataExtractor {
    suspend fun extract(uri: Uri): Pair<String, PdfMetadata>
}

@Singleton
class AndroidPdfMetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) : PdfMetadataExtractor {

    override suspend fun extract(uri: Uri): Pair<String, PdfMetadata> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val nameAndSize = runCatching { queryNameAndSize(uri) }.recoverOrDefault(null to 0L)
        val pageCount = runCatching { queryPageCount(resolver, uri) }.recoverOrDefault(0)
        val info = runCatching { parsePdfInfo(resolver, uri) }.recoverOrDefault(emptyMap())

        val displayName = nameAndSize.first ?: "document.pdf"
        val size = nameAndSize.second

        displayName to PdfMetadata(
            title = info["Title"],
            author = info["Author"],
            subject = info["Subject"],
            keywords = info["Keywords"],
            pageCount = pageCount,
            fileSizeBytes = size,
            importedAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun queryNameAndSize(uri: Uri): Pair<String?, Long> {
        val resolver = context.contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                    return name to size
                }
            }
        return null to 0L
    }

    private fun queryPageCount(resolver: android.content.ContentResolver, uri: Uri): Int {
        return runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer -> renderer.pageCount }
            } ?: 0
        }.getOrDefault(0)
    }

    private fun parsePdfInfo(
        resolver: android.content.ContentResolver,
        uri: Uri
    ): Map<String, String> {
        val raw = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                if (total >= MAX_READ_BYTES) break
                output.write(buffer, 0, read)
                total += read
            }
            output.toByteArray().toString(Charsets.ISO_8859_1)
        }.orEmpty()

        if (raw.isBlank()) return emptyMap()

        val keys = listOf("Title", "Author", "Subject", "Keywords")
        return keys.mapNotNull { key ->
            val regex = Regex("/$key\\s*\\((.*?)\\)", RegexOption.DOT_MATCHES_ALL)
            val value = regex.find(raw)?.groupValues?.getOrNull(1)
                ?.replace("\\(", "(")
                ?.replace("\\)", ")")
                ?.replace("\\n", " ")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            if (value != null) key to value else null
        }.toMap()
    }

    private companion object {
        const val MAX_READ_BYTES = 1_200_000
    }
}

private fun <T> Result<T>.recoverOrDefault(defaultValue: T): T {
    val failure = exceptionOrNull()
    if (failure is CancellationException) throw failure
    return getOrDefault(defaultValue)
}
