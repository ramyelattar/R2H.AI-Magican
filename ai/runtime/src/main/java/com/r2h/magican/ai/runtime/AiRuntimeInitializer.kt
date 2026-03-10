package com.r2h.magican.ai.runtime

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class AiRuntimeInitializer @Inject constructor(
    private val aiRuntime: AiRuntime
) {
    @Volatile
    private var lastReadiness: RuntimeReadinessReport = aiRuntime.capabilityState.value.toRuntimeReadinessReport(
        reason = "Not initialized"
    )

    suspend fun initializeDefaultModelIfConfigured(): RuntimeReadinessReport {
        val assetPath = BuildConfig.DEFAULT_MODEL_ASSET_PATH.trim()
        val modelSha256 = BuildConfig.DEFAULT_MODEL_SHA256.trim().lowercase()
        val modelVersion = BuildConfig.DEFAULT_MODEL_VERSION.trim().ifBlank { "unknown" }

        if (assetPath.isBlank()) {
            val report = if (BuildConfig.AI_RUNTIME_REQUIRED) {
                RuntimeReadinessReport(
                    availability = RuntimeAvailability.Unavailable,
                    capabilityState = AiCapabilityState.Failed(
                        errorId = "MISSING_MODEL_CONFIG",
                        message = "Missing model asset path"
                    ),
                    reason = "AI runtime requires a configured model asset path in release"
                )
            } else {
                RuntimeReadinessReport(
                    availability = RuntimeAvailability.Degraded,
                    capabilityState = AiCapabilityState.Degraded(
                        reason = "No model asset configured",
                        recoveryStrategy = "Enable optional model path in runtime build config"
                    ),
                    reason = "AI model not configured; runtime starts in optional mode"
                )
            }
            lastReadiness = report
            return report
        }

        if (BuildConfig.AI_RUNTIME_REQUIRED && modelSha256.isBlank()) {
            val report = RuntimeReadinessReport(
                availability = RuntimeAvailability.Unavailable,
                capabilityState = AiCapabilityState.Failed(
                    errorId = "MISSING_MODEL_HASH",
                    message = "Missing model SHA-256"
                ),
                reason = "AI runtime requires model SHA-256 in release"
            )
            lastReadiness = report
            return report
        }

        return try {
            aiRuntime.loadModel(
                QuantizedModelSpec(
                    assetPath = assetPath,
                    copiedFileName = assetPath.substringAfterLast('/'),
                    version = modelVersion,
                    expectedSha256 = modelSha256.ifBlank { null }
                )
            )
            val report = currentReadiness()
            lastReadiness = report
            if (BuildConfig.AI_RUNTIME_REQUIRED && !report.isReadyForInference) {
                throw IllegalStateException(report.reason)
            }
            report
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Throwable) {
            val report = RuntimeReadinessReport(
                availability = RuntimeAvailability.Unavailable,
                capabilityState = aiRuntime.capabilityState.value,
                reason = "AI runtime failed to initialize: ${error.message ?: "Unknown error"}"
            )
            lastReadiness = report
            if (BuildConfig.AI_RUNTIME_REQUIRED) throw error
            report
        }
    }

    fun currentReadiness(): RuntimeReadinessReport =
        aiRuntime.capabilityState.value.toRuntimeReadinessReport()

    fun latestReadiness(): RuntimeReadinessReport = lastReadiness
}
