package com.r2h.magican.ai.orchestrator

import com.r2h.magican.ai.runtime.AiRuntime
import com.r2h.magican.ai.runtime.RuntimeReadinessReport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

/**
 * Self-test runner that verifies AI runtime functionality on startup.
 * Ensures the model loads correctly and can perform basic inference.
 */
@Singleton
class StartupSelfTest @Inject constructor(
    private val aiRuntime: AiRuntime,
    private val telemetry: TelemetrySink
) {

    private val testPrompt = """
        You are a mystical advisor. Answer in one sentence only: What is the purpose of this test?
    """.trimIndent()

    private val expectedKeywords = setOf("test", "verify", "validation", "check", "purpose")
    private val minResponseLength = 16
    private val minResponseWords = 4

    suspend fun run(runtimeReadiness: RuntimeReadinessReport): StartupSelfTestResult {
        val startNs = System.nanoTime()

        if (!runtimeReadiness.isReadyForInference) {
            val reason = "Runtime unavailable for startup self-test: ${runtimeReadiness.availability} - ${runtimeReadiness.reason}"
            telemetry.emit(TelemetryEvent.StartupSelfTestSkipped(reason = reason))
            return StartupSelfTestResult.Skipped(reason)
        }

        return try {
            // 2. Perform test inference with timeout
            val response = withTimeout(15_000L) {
                aiRuntime.infer(testPrompt)
            }

            // 3. Validate response is meaningful
            val normalized = response.replace(Regex("\\s+"), " ").trim()
            val responseLower = normalized.lowercase()
            val hasKeyword = expectedKeywords.any { responseLower.contains(it) }
            val hasLength = normalized.length >= minResponseLength
            val hasLetters = normalized.any { it.isLetter() }
            val wordCount = normalized.split(' ').count { it.isNotBlank() }
            val valid = hasLetters && (hasKeyword || hasLength || wordCount >= minResponseWords)

            if (!valid) {
                val event = TelemetryEvent.StartupSelfTestFailed(
                    errorMessage = "Invalid response content",
                    expectedOutput = "Readable sentence (>= $minResponseWords words or keyword: ${expectedKeywords.joinToString()})",
                    actualOutput = normalized.take(100)
                )
                telemetry.emit(event)
                return StartupSelfTestResult.Failed(event.errorMessage)
            }

            // Success
            val durationMs = (System.nanoTime() - startNs) / 1_000_000L
            val event = TelemetryEvent.StartupSelfTestPassed(durationMs = durationMs)
            telemetry.emit(event)

            StartupSelfTestResult.Passed(
                durationMs = durationMs,
                response = response
            )

        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: Throwable) {
            val event = TelemetryEvent.StartupSelfTestFailed(
                errorMessage = e.message ?: "Unknown error",
                expectedOutput = "Successful inference",
                actualOutput = e::class.java.simpleName
            )
            telemetry.emit(event)
            StartupSelfTestResult.Failed(event.errorMessage)
        }
    }
}

sealed class StartupSelfTestResult {
    data class Passed(val durationMs: Long, val response: String) : StartupSelfTestResult()
    data class Failed(val reason: String) : StartupSelfTestResult()
    data class Skipped(val reason: String) : StartupSelfTestResult()
}
