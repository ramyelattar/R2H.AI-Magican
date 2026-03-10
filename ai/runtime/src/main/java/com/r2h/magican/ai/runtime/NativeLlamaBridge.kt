package com.r2h.magican.ai.runtime

import javax.inject.Inject
import javax.inject.Singleton

interface NativeTokenListener {
    fun onToken(token: String)
    fun onError(message: String)
}

@Singleton
class NativeLlamaBridge @Inject constructor() {
    private val libraryLoadResult: Result<Unit> = runCatching {
        System.loadLibrary("ai_runtime_jni")
    }

    val isLibraryAvailable: Boolean
        get() = libraryLoadResult.isSuccess

    fun libraryLoadError(): String? = libraryLoadResult.exceptionOrNull()?.message

    private fun ensureLibraryLoaded() {
        if (!isLibraryAvailable) {
            error("Native library ai_runtime_jni unavailable: ${libraryLoadError() ?: "Unknown error"}")
        }
    }

    private external fun nativeCreate(
        modelPath: String,
        nCtx: Int,
        nThreads: Int,
        seed: Int
    ): Long

    private external fun nativeDestroy(handle: Long)

    private external fun nativeRuntimeMode(): Int

    private external fun nativeGenerate(
        handle: Long,
        requestId: String,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        stopTokens: Array<String>,
        listener: NativeTokenListener
    ): Int

    private external fun nativeCancel(handle: Long, requestId: String)

    fun create(modelPath: String, nCtx: Int, nThreads: Int, seed: Int): Long {
        ensureLibraryLoaded()
        return nativeCreate(modelPath, nCtx, nThreads, seed)
    }

    fun destroy(handle: Long) {
        if (!isLibraryAvailable) return
        nativeDestroy(handle)
    }

    fun runtimeMode(): Int {
        ensureLibraryLoaded()
        return nativeRuntimeMode()
    }

    fun generate(
        handle: Long,
        requestId: String,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        stopTokens: Array<String>,
        listener: NativeTokenListener
    ): Int {
        ensureLibraryLoaded()
        return nativeGenerate(
            handle = handle,
            requestId = requestId,
            prompt = prompt,
            maxTokens = maxTokens,
            temperature = temperature,
            topP = topP,
            stopTokens = stopTokens,
            listener = listener
        )
    }

    fun cancel(handle: Long, requestId: String) {
        if (!isLibraryAvailable) return
        nativeCancel(handle, requestId)
    }
}
