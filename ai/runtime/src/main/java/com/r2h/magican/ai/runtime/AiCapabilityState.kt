package com.r2h.magican.ai.runtime

sealed interface AiCapabilityState {
    data object Unloaded : AiCapabilityState

    data class Loading(
        val startedAtEpochMs: Long = System.currentTimeMillis()
    ) : AiCapabilityState

    data class Ready(
        val modelVersion: String,
        val modelSha256: String,
        val modelSizeBytes: Long,
        val runtimeMode: AiRuntimeMode
    ) : AiCapabilityState

    data class Degraded(
        val reason: String,
        val recoveryStrategy: String
    ) : AiCapabilityState

    data class Failed(
        val errorId: String,
        val message: String
    ) : AiCapabilityState
}

enum class AiRuntimeMode {
    Native,
    Stub
}

fun AiCapabilityState.describe(): String {
    return when (this) {
        AiCapabilityState.Unloaded -> "unloaded"
        is AiCapabilityState.Loading -> "loading"
        is AiCapabilityState.Ready -> "ready:${runtimeMode.name.lowercase()}"
        is AiCapabilityState.Degraded -> "degraded:$reason"
        is AiCapabilityState.Failed -> "failed:$errorId"
    }
}
