package com.r2h.magican.ai.orchestrator

import com.r2h.magican.ai.runtime.AiRuntime
import com.r2h.magican.ai.runtime.AiCapabilityState
import com.r2h.magican.ai.runtime.describe
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class MagicanOrchestrator @Inject constructor(
    private val aiRuntime: AiRuntime,
    private val templates: PromptTemplateCatalog,
    private val safetyPolicy: SafetyPolicy,
    private val encoder: JsonStyleEncoder,
    private val telemetry: TelemetrySink
) {

    suspend fun orchestrate(input: FeatureInput): String {
        val requestId = UUID.randomUUID().toString()
        val startNs = System.nanoTime()

        // Phase 1: Safety assessment with Guardrails 2.0
        val policyResult = safetyPolicy.evaluate(input)

        if (!policyResult.allowed) {
            telemetry.emit(
                TelemetryEvent.SafetyBlocked(
                    feature = input.feature.name,
                    riskLevel = policyResult.riskLevel,
                    blockReason = policyResult.blockReason ?: "Unknown",
                    classifications = policyResult.classifications
                )
            )
            return encoder.blocked(
                requestId = requestId,
                feature = input.feature,
                policyResult = policyResult
            )
        }

        // Phase 2: Runtime capability check
        val capability = aiRuntime.capabilityState.value
        if (capability !is AiCapabilityState.Ready) {
            telemetry.emit(
                TelemetryEvent.RuntimeUnavailable(
                    feature = input.feature.name,
                    capabilityState = capability.describe()
                )
            )
            return encoder.error(
                requestId = requestId,
                feature = input.feature,
                templateId = null,
                policyResult = policyResult,
                message = "AI runtime unavailable: ${capability.describe()}"
            )
        }

        // Phase 3: Prompt assembly and inference
        val template = templates.select(input.feature)
        val prompt = templates.buildPrompt(template, input, policyResult.sanitizedInput)

        return try {
            val raw = aiRuntime.infer(prompt)
            val responseJson = try {
                encoder.requireValidResponseObject(raw)
            } catch (e: Exception) {
                telemetry.emit(
                    TelemetryEvent.SchemaValidationFailure(
                        feature = input.feature.name,
                        templateId = template.id,
                        errorMessage = e.message ?: "Schema mismatch",
                        responseLength = raw.length
                    )
                )
                throw e
            }

            val durationMs = (System.nanoTime() - startNs) / 1_000_000L
            telemetry.emit(
                TelemetryEvent.SuccessfulInference(
                    feature = input.feature.name,
                    templateId = template.id,
                    durationMs = durationMs,
                    tokenCount = estimateTokenCount(responseJson)
                )
            )

            encoder.success(
                requestId = requestId,
                feature = input.feature,
                templateId = template.id,
                policyResult = policyResult,
                responseObjectJson = responseJson
            )
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Throwable) {
            telemetry.emit(
                TelemetryEvent.InferenceFailure(
                    feature = input.feature.name,
                    templateId = template.id,
                    errorMessage = error.message ?: "Unknown error",
                    errorClass = error::class.java.simpleName
                )
            )
            encoder.error(
                requestId = requestId,
                feature = input.feature,
                templateId = template.id,
                policyResult = policyResult,
                message = error.message ?: "Unknown runtime error"
            )
        }
    }

    private fun estimateTokenCount(text: String): Int {
        val compact = text.trim()
        if (compact.isEmpty()) return 0
        return compact.split(Regex("\\s+")).size
    }
}
