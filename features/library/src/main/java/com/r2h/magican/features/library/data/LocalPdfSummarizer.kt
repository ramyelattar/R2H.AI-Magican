package com.r2h.magican.features.library.data

import com.r2h.magican.ai.runtime.AiRuntime
import com.r2h.magican.features.library.domain.LibraryDocument
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

interface LocalPdfSummarizer {
    suspend fun summarize(document: LibraryDocument): String
}

@Singleton
class AiRuntimePdfSummarizer @Inject constructor(
    private val aiRuntime: AiRuntime
) : LocalPdfSummarizer {

    override suspend fun summarize(document: LibraryDocument): String {
        val context = document.extractedText.take(6_000).ifBlank {
            listOfNotNull(
                document.metadata.title,
                document.metadata.subject,
                document.metadata.keywords
            ).joinToString(" ")
        }

        val prompt = """
            Summarize this PDF locally.
            Return plain text only, 5 bullet points max.
            Document: ${document.displayName}
            Context:
            $context
        """.trimIndent()

        return runCatching {
            withTimeout(20_000L) {
                aiRuntime.infer(prompt).trim()
            }
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            fallbackSummary(context)
        }
    }

    private fun fallbackSummary(text: String): String {
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length in 25..240 }
            .distinct()
            .take(5)

        if (sentences.isEmpty()) return "No extractable text found in this PDF."
        return sentences.joinToString(separator = "\n") { "- $it" }
    }
}
