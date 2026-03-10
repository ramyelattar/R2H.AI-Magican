package com.r2h.magican.ai.orchestrator

import javax.inject.Inject
import javax.inject.Singleton

enum class ViolationCode {
    EmptyField,
    FieldTooLong,
    PromptInjection,
    DisallowedContent
}

data class SafetyViolation(
    val code: ViolationCode,
    val field: String,
    val message: String
)

data class SafetyAssessment(
    val allowed: Boolean,
    val violations: List<SafetyViolation>,
    val safeFields: Map<String, String>
)

@Singleton
class SafetyGuardrails @Inject constructor(
    private val safetyPolicy: SafetyPolicy
) {

    fun assess(input: FeatureInput): SafetyAssessment {
        val policyResult = safetyPolicy.evaluate(input)
        val violations = mutableListOf<SafetyViolation>()
        policyResult.classifications.forEach { classification ->
            val code = when (classification.level) {
                RiskLevel.Critical, RiskLevel.High -> ViolationCode.PromptInjection
                RiskLevel.Medium, RiskLevel.Low -> ViolationCode.DisallowedContent
                RiskLevel.None -> return@forEach
            }
            violations += SafetyViolation(
                code = code,
                field = "policy",
                message = classification.reason
            )
        }

        return SafetyAssessment(
            allowed = policyResult.allowed,
            violations = violations,
            safeFields = policyResult.sanitizedInput
        )
    }
}
