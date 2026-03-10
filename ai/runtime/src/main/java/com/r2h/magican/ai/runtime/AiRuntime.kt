package com.r2h.magican.ai.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AiRuntime {
    val capabilityState: StateFlow<AiCapabilityState>
    suspend fun loadModel(spec: QuantizedModelSpec)
    fun stream(request: LlmRequest): Flow<LlmEvent>
    fun cancel(requestId: String)
    suspend fun generate(request: LlmRequest): String
    suspend fun infer(prompt: String): String
}
