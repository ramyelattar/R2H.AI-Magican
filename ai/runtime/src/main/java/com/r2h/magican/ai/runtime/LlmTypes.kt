package com.r2h.magican.ai.runtime

import java.util.UUID

data class QuantizedModelSpec(
    val assetPath: String? = null,
    val absolutePath: String? = null,
    val copiedFileName: String = "model.gguf",
    val version: String = "unknown",
    val expectedSha256: String? = null,
    val expectedSizeBytes: Long? = null,
    val contextSize: Int = 4096,
    val threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 8),
    val seed: Int = 0
) {
    init {
        require(assetPath != null || absolutePath != null) {
            "Either assetPath or absolutePath must be provided."
        }
    }
}

data class ResolvedModel(
    val absolutePath: String,
    val version: String,
    val sha256: String,
    val sizeBytes: Long
)

data class GenerationConfig(
    val maxTokens: Int = 256,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val stopTokens: List<String> = emptyList(),
    val timeoutMs: Long = 30_000L
)

data class LlmRequest(
    val prompt: String,
    val config: GenerationConfig = GenerationConfig(),
    val requestId: String = UUID.randomUUID().toString()
)

enum class FinishReason {
    Completed,
    Cancelled,
    Timeout,
    Error
}

sealed interface LlmEvent {
    val requestId: String

    data class Token(override val requestId: String, val value: String) : LlmEvent
    data class Error(override val requestId: String, val message: String) : LlmEvent
    data class Finished(override val requestId: String, val reason: FinishReason) : LlmEvent
}
