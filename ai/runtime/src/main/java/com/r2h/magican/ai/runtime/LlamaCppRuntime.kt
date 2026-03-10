package com.r2h.magican.ai.runtime

import java.io.Closeable
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@Singleton
class LlamaCppRuntime @Inject constructor(
    private val bridge: NativeLlamaBridge,
    private val modelStore: ModelStore
) : AiRuntime, Closeable {

    private val handle = AtomicLong(0L)
    private val loadMutex = Mutex()
    private val _capabilityState = MutableStateFlow<AiCapabilityState>(AiCapabilityState.Unloaded)
    override val capabilityState: StateFlow<AiCapabilityState> = _capabilityState

    override suspend fun loadModel(spec: QuantizedModelSpec) {
        _capabilityState.value = AiCapabilityState.Loading()
        try {
            loadMutex.withLock {
                if (BuildConfig.AI_RUNTIME_REQUIRED && spec.expectedSha256.isNullOrBlank()) {
                    error("AI_RUNTIME_REQUIRED is enabled but expectedSha256 is missing")
                }

                val resolved = modelStore.resolveVerifiedModel(spec)
                val newHandle = withContext(Dispatchers.IO) {
                    bridge.create(
                        modelPath = resolved.absolutePath,
                        nCtx = spec.contextSize,
                        nThreads = spec.threads,
                        seed = spec.seed
                    )
                }
                require(newHandle != 0L) { "Failed to load model: ${resolved.absolutePath}" }

                val mode = when (bridge.runtimeMode()) {
                    0 -> AiRuntimeMode.Native
                    else -> AiRuntimeMode.Stub
                }

                if (BuildConfig.AI_RUNTIME_REQUIRED && mode == AiRuntimeMode.Stub) {
                    withContext(Dispatchers.IO) { bridge.destroy(newHandle) }
                    error("AI_RUNTIME_REQUIRED is enabled and stub runtime was detected")
                }

                val oldHandle = handle.getAndSet(newHandle)
                if (oldHandle != 0L) {
                    withContext(Dispatchers.IO) { bridge.destroy(oldHandle) }
                }

                _capabilityState.value = if (mode == AiRuntimeMode.Native) {
                    AiCapabilityState.Ready(
                        modelVersion = resolved.version,
                        modelSha256 = resolved.sha256,
                        modelSizeBytes = resolved.sizeBytes,
                        runtimeMode = mode
                    )
                } else {
                    AiCapabilityState.Degraded(
                        reason = "Stub runtime active",
                        recoveryStrategy = "Bundle llama.cpp and a verified GGUF model"
                    )
                }
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            _capabilityState.value = AiCapabilityState.Failed(
                errorId = UUID.randomUUID().toString(),
                message = t.message ?: "Model load failed"
            )
            throw t
        }
    }

    override fun stream(request: LlmRequest): Flow<LlmEvent> = callbackFlow {
        val capability = capabilityState.value
        if (capability !is AiCapabilityState.Ready) {
            trySend(LlmEvent.Error(request.requestId, "AI runtime not ready: ${capability.describe()}"))
            trySend(LlmEvent.Finished(request.requestId, FinishReason.Error))
            close()
            return@callbackFlow
        }

        val h = handle.get()
        if (h == 0L) {
            trySend(LlmEvent.Error(request.requestId, "Model not loaded"))
            trySend(LlmEvent.Finished(request.requestId, FinishReason.Error))
            close()
            return@callbackFlow
        }

        val listener = object : NativeTokenListener {
            override fun onToken(token: String) {
                trySend(LlmEvent.Token(request.requestId, token))
            }

            override fun onError(message: String) {
                trySend(LlmEvent.Error(request.requestId, message))
            }
        }

        val job = launch(Dispatchers.IO) {
            try {
                val result = withTimeout(request.config.timeoutMs) {
                    bridge.generate(
                        handle = h,
                        requestId = request.requestId,
                        prompt = request.prompt,
                        maxTokens = request.config.maxTokens,
                        temperature = request.config.temperature,
                        topP = request.config.topP,
                        stopTokens = request.config.stopTokens.toTypedArray(),
                        listener = listener
                    )
                }

                when (result) {
                    0 -> trySend(LlmEvent.Finished(request.requestId, FinishReason.Completed))
                    1 -> trySend(LlmEvent.Finished(request.requestId, FinishReason.Cancelled))
                    else -> {
                        trySend(LlmEvent.Error(request.requestId, "Native error code=$result"))
                        trySend(LlmEvent.Finished(request.requestId, FinishReason.Error))
                    }
                }
            } catch (_: TimeoutCancellationException) {
                bridge.cancel(h, request.requestId)
                trySend(LlmEvent.Error(request.requestId, "Timeout after ${request.config.timeoutMs}ms"))
                trySend(LlmEvent.Finished(request.requestId, FinishReason.Timeout))
            } catch (_: CancellationException) {
                bridge.cancel(h, request.requestId)
                trySend(LlmEvent.Finished(request.requestId, FinishReason.Cancelled))
            } catch (t: Throwable) {
                trySend(LlmEvent.Error(request.requestId, t.message ?: "Unknown failure"))
                trySend(LlmEvent.Finished(request.requestId, FinishReason.Error))
            } finally {
                close()
            }
        }

        awaitClose {
            bridge.cancel(h, request.requestId)
            job.cancel()
        }
    }

    override fun cancel(requestId: String) {
        val h = handle.get()
        if (h != 0L) bridge.cancel(h, requestId)
    }

    override suspend fun generate(request: LlmRequest): String {
        val capability = capabilityState.value
        require(capability is AiCapabilityState.Ready) {
            "AI runtime not ready: ${capability.describe()}"
        }

        val out = StringBuilder()
        stream(request).collect { event ->
            when (event) {
                is LlmEvent.Token -> out.append(event.value)
                is LlmEvent.Error -> throw IllegalStateException(event.message)
                is LlmEvent.Finished -> when (event.reason) {
                    FinishReason.Completed -> Unit
                    FinishReason.Cancelled -> throw CancellationException("Cancelled")
                    FinishReason.Timeout -> throw CancellationException("Timeout")
                    FinishReason.Error -> throw IllegalStateException("Generation failed")
                }
            }
        }
        return out.toString()
    }

    override suspend fun infer(prompt: String): String {
        return generate(LlmRequest(prompt = prompt))
    }

    override fun close() {
        val h = handle.getAndSet(0L)
        if (h != 0L) bridge.destroy(h)
        _capabilityState.value = AiCapabilityState.Unloaded
    }
}
