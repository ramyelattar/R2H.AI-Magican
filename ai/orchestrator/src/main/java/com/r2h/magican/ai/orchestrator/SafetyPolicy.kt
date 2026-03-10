package com.r2h.magican.ai.orchestrator

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guardrails 2.0: Token-based safety policy with category classification.
 * Replaces naive substring matching with structured risk assessment.
 */

enum class RiskCategory {
    PromptInjection,
    JailbreakAttempt,
    HarmfulContent,
    PersonalDataLeak,
    Malicious,
    Benign
}

enum class RiskLevel {
    Critical,  // Block immediately
    High,      // Block + log for review
    Medium,    // Warn + continue with sanitization
    Low,       // Allow + track
    None       // Clean input
}

data class TokenClassification(
    val token: String,
    val category: RiskCategory,
    val level: RiskLevel,
    val reason: String
)

data class SafetyPolicyResult(
    val allowed: Boolean,
    val riskLevel: RiskLevel,
    val classifications: List<TokenClassification>,
    val sanitizedInput: Map<String, String>,
    val blockReason: String? = null
)

@Singleton
class SafetyPolicy @Inject constructor(
    private val tokenizer: SafetyTokenizer,
    private val classifier: RiskClassifier
) {

    private val maxFieldLength = 2_000
    private val riskPriority = mapOf(
        RiskLevel.None to 0,
        RiskLevel.Low to 1,
        RiskLevel.Medium to 2,
        RiskLevel.High to 3,
        RiskLevel.Critical to 4
    )

    fun evaluate(input: FeatureInput): SafetyPolicyResult {
        val classifications = mutableListOf<TokenClassification>()
        val sanitizedInput = LinkedHashMap<String, String>()
        if (input.fields.values.none { it.isNotBlank() }) {
            return SafetyPolicyResult(
                allowed = true,
                riskLevel = RiskLevel.None,
                classifications = emptyList(),
                sanitizedInput = input.fields.entries.associate { it.key to "" },
                blockReason = null
            )
        }

        for ((field, rawValue) in input.fields) {
            val value = rawValue.trim().take(maxFieldLength)
            sanitizedInput[field] = value

            if (value.isEmpty()) continue

            // Tokenize and classify each significant token
            val tokens = tokenizer.tokenize(value)
            for (token in tokens) {
                val classification = classifier.classify(token)
                if (classification.level != RiskLevel.None) {
                    classifications += classification
                }
            }
        }

        // Aggregate risk assessment
        val maxRiskLevel = classifications
            .maxByOrNull { riskPriority[it.level] ?: 0 }
            ?.level
            ?: RiskLevel.None
        val criticalViolations = classifications.filter { it.level == RiskLevel.Critical }
        val highViolations = classifications.filter { it.level == RiskLevel.High }

        val allowed = criticalViolations.isEmpty() && highViolations.isEmpty()
        val blockReason = when {
            criticalViolations.isNotEmpty() -> "Critical security violation: ${criticalViolations.first().reason}"
            highViolations.isNotEmpty() -> "High-risk content detected: ${highViolations.first().reason}"
            else -> null
        }

        return SafetyPolicyResult(
            allowed = allowed,
            riskLevel = maxRiskLevel,
            classifications = classifications,
            sanitizedInput = sanitizedInput,
            blockReason = blockReason
        )
    }
}
