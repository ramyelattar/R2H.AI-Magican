package com.r2h.magican.ai.orchestrator

import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tokenizes user input into semantic units for risk classification.
 * Extracts n-grams, special patterns, and meaningful phrases.
 */
@Singleton
class SafetyTokenizer @Inject constructor() {

    private val invisibleCharsRegex = Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2060\\uFEFF]")

    private val specialPatternRegex = Regex(
        """(?:(?:ignore|skip|bypass|override|disable)\s+(?:all|previous|prior|above|system)|
            |(?:system|developer|admin|root)\s+(?:prompt|instruction|command|mode)|
            |(?:تجاهل|تجاوز|عط(?:ل|ل))\s+(?:كل|جميع|التعليمات|القيود|الأمان)|
            |(?:تعليمات|موجه)\s+(?:النظام|المطور)|
            |<(?:script|iframe|embed|object)|
            |(?:===|---|###)\s*(?:SYSTEM|ADMIN|DEV)|
            |jailbreak|role\s*:\s*(?:system|admin))""".trimMargin(),
        RegexOption.IGNORE_CASE
    )

    fun tokenize(input: String): List<String> {
        val tokens = mutableSetOf<String>()
        val canonicalInput = normalizeForAnalysis(input)
            .replace(Regex("[^\\p{L}\\p{N}<:/#=_-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // 1. Extract special attack patterns
        specialPatternRegex.findAll(canonicalInput).forEach { match ->
            tokens += match.value.lowercase().trim()
        }

        // 2. Word-level tokens (meaningful content)
        val words = canonicalInput
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 3 }

        tokens += words

        // 3. Bigrams (two-word combinations)
        if (words.size >= 2) {
            words.windowed(2).forEach { (w1, w2) ->
                tokens += "$w1 $w2"
            }
        }

        // 4. Trigrams (three-word combinations) for complex attacks
        if (words.size >= 3) {
            words.windowed(3).forEach { (w1, w2, w3) ->
                tokens += "$w1 $w2 $w3"
            }
        }

        return tokens.toList()
    }

    private fun normalizeForAnalysis(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFKC)
            .replace(invisibleCharsRegex, "")
            .replace("ـ", "")
            .lowercase()
    }
}
