package com.r2h.magican.features.library.data

import com.r2h.magican.ai.orchestrator.RiskClassifier
import com.r2h.magican.ai.orchestrator.RiskLevel
import com.r2h.magican.ai.orchestrator.SafetyTokenizer
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
    private val aiRuntime: AiRuntime,
    private val tokenizer: SafetyTokenizer,
    private val classifier: RiskClassifier
) : LocalPdfSummarizer {

    /**
     * Strips tokens classified as Critical or High risk from externally-sourced text
     * before embedding it in an AI prompt. Prevents PDF-based prompt injection.
     */
    private fun sanitize(input: String): String {
        val tokens = tokenizer.tokenize(input)
        return tokens.filter { token ->
            val classification = classifier.classify(token)
            classification.level != RiskLevel.Critical && classification.level != RiskLevel.High
        }.joinToString(" ")
    }

    override suspend fun summarize(document: LibraryDocument): String {
        val rawContext = document.extractedText.take(6_000).ifBlank {
            listOfNotNull(document.metadata.title, document.metadata.subject, document.metadata.keywords)
                .joinToString(" ")
        }

        // Sanitize externally-sourced content to prevent PDF-based prompt injection
        val safeContext = sanitize(rawContext)
        val safeName = sanitize(document.displayName)

        val prompt = """
            Summarize this PDF locally.
            Return plain text only, 5 bullet points max.
            Document: $safeName
            Context:
            $safeContext
        """.trimIndent()

        return runCatching {
            withTimeout(20_000L) {
                aiRuntime.infer(prompt).trim()
            }
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            fallbackSummary(rawContext)  // fallback uses raw text since it won't be sent to AI
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
