package com.r2h.magican.ai.orchestrator

import java.time.Instant
import java.util.UUID

/**
 * Structured telemetry for AI orchestration events.
 * Enables audit trails, debugging, and security monitoring.
 */

enum class EventSeverity {
    Debug,
    Info,
    Warning,
    Error,
    Critical
}

sealed class TelemetryEvent {
    abstract val eventId: String
    abstract val timestamp: Instant
    abstract val severity: EventSeverity
    abstract fun describe(): String

    data class SafetyBlocked(
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val severity: EventSeverity = EventSeverity.Warning,
        val feature: String,
        val riskLevel: RiskLevel,
        val blockReason: String,
        val classifications: List<TokenClassification>
    ) : TelemetryEvent() {
        override fun describe() = "SafetyBlocked: $feature - $blockReason (risk=$riskLevel)"
    }

    data class RuntimeUnavailable(
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val severity: EventSeverity = EventSeverity.Error,
        val feature: String,
        val capabilityState: String
    ) : TelemetryEvent() {
        override fun describe() = "RuntimeUnavailable: $feature - state=$capabilityState"
    }

    data class InferenceFailure(
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val severity: EventSeverity = EventSeverity.Error,
        val feature: String,
        val templateId: String?,
        val errorMessage: String,
        val errorClass: String?
    ) : TelemetryEvent() {
        override fun describe() = "InferenceFailure: $feature - $errorMessage"
    }

    data class SchemaValidationFailure(
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val severity: EventSeverity = EventSeverity.Error,
        val feature: String,
        val templateId: String?,
        val errorMessage: String,
        val responseLength: Int
    ) : TelemetryEvent() {
        override fun describe() = "SchemaValidationFailure: $feature - $errorMessage"
    }

    data class SuccessfulInference(
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val severity: EventSeverity = EventSeverity.Info,
        val feature: String,
        val templateId: String,
        val durationMs: Long,
        val tokenCount: Int?
    ) : TelemetryEvent() {
        override fun describe() = "SuccessfulInference: $feature (${durationMs}ms)"
    }

    data class StartupSelfTestFailed(
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val severity: EventSeverity = EventSeverity.Critical,
        val errorMessage: String,
        val expectedOutput: String,
        val actualOutput: String?
    ) : TelemetryEvent() {
        override fun describe() = "StartupSelfTestFailed: $errorMessage"
    }

    data class StartupSelfTestPassed(
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val severity: EventSeverity = EventSeverity.Info,
        val durationMs: Long
    ) : TelemetryEvent() {
        override fun describe() = "StartupSelfTestPassed: ${durationMs}ms"
    }

    data class StartupSelfTestSkipped(
        override val eventId: String = UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val severity: EventSeverity = EventSeverity.Warning,
        val reason: String
    ) : TelemetryEvent() {
        override fun describe() = "StartupSelfTestSkipped: $reason"
    }
}
