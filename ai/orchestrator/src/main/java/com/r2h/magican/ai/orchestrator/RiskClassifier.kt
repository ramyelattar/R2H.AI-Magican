package com.r2h.magican.ai.orchestrator

import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classifies tokens into risk categories with severity levels.
 * Uses pattern-based rules + semantic analysis.
 */
@Singleton
class RiskClassifier @Inject constructor() {

    private val invisibleCharsRegex = Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2060\\uFEFF]")

    private val criticalInjectionPatterns = setOf(
        "ignore all previous",
        "ignore previous instructions",
        "ignore all instructions",
        "system prompt",
        "developer message",
        "you are now",
        "your new role",
        "override your instructions",
        "bypass all filters",
        "disable safety",
        "jailbreak mode",
        "تجاهل جميع التعليمات",
        "تجاهل التعليمات",
        "تجاهل جميع التعليمات السابقة",
        "تجاهل التعليمات السابقة",
        "تجاوز جميع القيود",
        "عطل الامان",
        "عطّل الأمان"
    )

    private val highRiskSystemPatterns = setOf(
        "admin mode",
        "root access",
        "system command",
        "developer mode",
        "debug mode activated",
        "unrestricted mode",
        "وضع المطور",
        "وضع المسؤول",
        "صلاحية الروت"
    )

    private val highRiskHarmPatterns = setOf(
        "build a bomb",
        "build bomb",
        "make explosives",
        "suicide methods",
        "self harm instructions",
        "child sexual",
        "minor sexual",
        "illegal drugs synthesis",
        "صنع قنبلة",
        "كيفية صنع قنبلة",
        "طرق الانتحار",
        "ايذاء النفس",
        "إيذاء النفس"
    )

    private val mediumRiskLeakPatterns = setOf(
        "reveal your prompt",
        "show system instructions",
        "what are your rules",
        "bypass restrictions",
        "ignore guidelines",
        "اكشف تعليمات النظام",
        "اعرض تعليمات النظام",
        "واعرض تعليمات النظام",
        "ما هي قواعدك",
        "ما هي تعليماتك"
    )

    private val injectionPrefixes = setOf(
        "ignore", "skip", "bypass", "override", "disable", "forget",
        "تجاهل", "تجاوز", "عطل", "عطّل", "انس"
    )

    private val highRiskMaliciousPatterns = setOf(
        "<script",
        "</script",
        "script alert",
        "scriptalert",
        "alert xss",
        "onerror",
        "svg onload",
        "javascript:",
        "xss"
    )

    fun classify(token: String): TokenClassification {
        val normalized = normalizeForAnalysis(token).trim()
        val canonical = normalized
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Critical: Direct prompt injection attempts
        if (criticalInjectionPatterns.any { it in canonical }) {
            return TokenClassification(
                token = token,
                category = RiskCategory.PromptInjection,
                level = RiskLevel.Critical,
                reason = "Direct instruction override pattern detected"
            )
        }

        // Critical: System-level jailbreak attempts
        if (highRiskSystemPatterns.any { it in canonical }) {
            return TokenClassification(
                token = token,
                category = RiskCategory.JailbreakAttempt,
                level = RiskLevel.Critical,
                reason = "System privilege escalation attempt"
            )
        }

        // High: Harmful content requests
        if (highRiskHarmPatterns.any { it in canonical }) {
            return TokenClassification(
                token = token,
                category = RiskCategory.HarmfulContent,
                level = RiskLevel.High,
                reason = "Request for harmful or illegal content"
            )
        }

        // High: Script/XSS or malicious payload markers
        if (highRiskMaliciousPatterns.any { it in normalized || it in canonical }) {
            return TokenClassification(
                token = token,
                category = RiskCategory.Malicious,
                level = RiskLevel.High,
                reason = "Script or XSS payload marker detected"
            )
        }

        // Medium: Information leakage attempts
        if (mediumRiskLeakPatterns.any { it in canonical }) {
            return TokenClassification(
                token = token,
                category = RiskCategory.PersonalDataLeak,
                level = RiskLevel.Medium,
                reason = "Attempt to reveal system internals"
            )
        }

        // Low: Suspicious command-like prefix patterns
        val words = canonical.split(Regex("\\s+"))
        if (words.isNotEmpty() && words[0] in injectionPrefixes && words.size >= 2) {
            return TokenClassification(
                token = token,
                category = RiskCategory.PromptInjection,
                level = RiskLevel.Low,
                reason = "Potentially adversarial command structure"
            )
        }

        // Additional heuristic: direct HTML tag injection patterns
        if (normalized.contains("<script") || normalized.contains("<iframe")) {
            return TokenClassification(
                token = token,
                category = RiskCategory.Malicious,
                level = RiskLevel.High,
                reason = "HTML/JavaScript injection attempt"
            )
        }

        return TokenClassification(
            token = token,
            category = RiskCategory.Benign,
            level = RiskLevel.None,
            reason = "No risk detected"
        )
    }

    private fun normalizeForAnalysis(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFKC)
            .replace(invisibleCharsRegex, "")
            .replace("ـ", "")
            .lowercase()
    }
}
