package com.r2h.magican.ai.orchestrator.domain

/**
 * Standard AI response content matching the JSON schema.
 * All AI features return this structured format.
 */
data class ResponseContent(
    val summary: String,
    val insights: List<String>,
    val actions: List<String>,
    val disclaimer: String
) {
    companion object {
        val Empty = ResponseContent(
            summary = "",
            insights = emptyList(),
            actions = emptyList(),
            disclaimer = ""
        )

        fun error(message: String) = ResponseContent(
            summary = message,
            insights = emptyList(),
            actions = emptyList(),
            disclaimer = "Please try again."
        )
    }
}

/**
 * Sealed hierarchy representing AI orchestration outcomes.
 * Replaces raw JSON string handling in presentation layer.
 */
sealed class AiResponse<out T> {
    /**
     * Successful AI generation with parsed content.
     */
    data class Success<T>(
        val requestId: String,
        val content: ResponseContent,
        val metadata: ResponseMetadata,
        val typedData: T? = null
    ) : AiResponse<T>()

    /**
     * Request blocked by safety policy.
     */
    data class Blocked(
        val requestId: String,
        val reason: String,
        val riskLevel: String,
        val metadata: ResponseMetadata
    ) : AiResponse<Nothing>()

    /**
     * Error during orchestration (runtime unavailable, inference failure, etc.)
     */
    data class Error(
        val requestId: String,
        val message: String,
        val isRecoverable: Boolean,
        val metadata: ResponseMetadata
    ) : AiResponse<Nothing>()
}

/**
 * Response metadata extracted from orchestration envelope.
 */
data class ResponseMetadata(
    val feature: String,
    val templateId: String?,
    val timestampUtc: String,
    val status: String,  // "ok", "blocked", "error"
    val safetyAllowed: Boolean,
    val safetyRiskLevel: String?
)
