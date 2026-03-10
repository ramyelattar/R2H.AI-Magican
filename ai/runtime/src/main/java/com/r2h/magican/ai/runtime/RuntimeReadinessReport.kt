package com.r2h.magican.ai.runtime

enum class RuntimeAvailability {
    ReadyForInference,
    Degraded,
    Unavailable
}

data class RuntimeReadinessReport(
    val availability: RuntimeAvailability,
    val capabilityState: AiCapabilityState,
    val reason: String
) {
    val isReadyForInference: Boolean
        get() = availability == RuntimeAvailability.ReadyForInference
}

fun AiCapabilityState.toRuntimeReadinessReport(
    reason: String = describe()
): RuntimeReadinessReport = when (this) {
    AiCapabilityState.Unloaded,
    is AiCapabilityState.Loading,
    is AiCapabilityState.Failed -> RuntimeReadinessReport(
        availability = RuntimeAvailability.Unavailable,
        capabilityState = this,
        reason = reason
    )

    is AiCapabilityState.Degraded -> RuntimeReadinessReport(
        availability = RuntimeAvailability.Degraded,
        capabilityState = this,
        reason = reason
    )

    is AiCapabilityState.Ready -> RuntimeReadinessReport(
        availability = RuntimeAvailability.ReadyForInference,
        capabilityState = this,
        reason = reason
    )
}
