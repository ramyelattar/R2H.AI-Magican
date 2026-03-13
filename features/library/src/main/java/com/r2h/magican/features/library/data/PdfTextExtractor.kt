package com.r2h.magican.features.library.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PdfTextExtractor {
    suspend fun extract(uri: Uri, maxChars: Int = 40_000): String
}

/**
 * Heuristic PDF text extractor that reads raw bytes and filters ASCII printable characters.
 *
 * **Known Limitations:**
 * - Does NOT handle compressed PDF streams (Flate/LZW deflate) — common in modern PDFs.
 * - Does NOT handle embedded fonts, CID font dictionaries, or ToUnicode CMaps.
 * - Does NOT decrypt password-protected PDFs.
 * - Best results on legacy, uncompressed PDFs with ASCII text streams.
 *
 * **TODO:** Replace with a proper PDF parsing library (e.g., PdfBox Android, Apache PDFBox)
 * for production-quality text extraction across modern PDF variants.
 */
@Singleton
class HeuristicPdfTextExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) : PdfTextExtractor {

    override suspend fun extract(uri: Uri, maxChars: Int): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(MAX_READ_BYTES) }
            ?: return@withContext ""

        val printable = StringBuilder(bytes.size)
        bytes.forEach { b ->
            val c = (b.toInt() and 0xFF).toChar()
            if (c in ' '..'~' || c == '\n' || c == '\r' || c == '\t') {
                printable.append(c)
            } else {
                printable.append(' ')
            }
        }

        val stopWords = setOf(
            "obj", "endobj", "stream", "endstream", "xref", "trailer",
            "catalog", "pages", "font", "encoding", "filter", "length"
        )

        val cleaned = printable
            .toString()
            .replace(Regex("\\s+"), " ")
            .split(' ')
            .asSequence()
            .map { it.trim('(', ')', '[', ']', '<', '>', '{', '}', '/', '\\', ',', ';', ':', '"') }
            .filter { token -> token.length >= 3 }
            .filter { token -> token.any { it.isLetter() } }
            .filter { token -> token.lowercase() !in stopWords }
            .take(10_000)
            .joinToString(" ")
            .take(maxChars)

        cleaned
    }

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            out.write(buffer, 0, read)
            total += read
            if (total >= limit) break
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_READ_BYTES = 4_000_000
    }
}
