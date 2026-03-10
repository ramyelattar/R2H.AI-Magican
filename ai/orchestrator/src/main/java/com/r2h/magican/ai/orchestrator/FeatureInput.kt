package com.r2h.magican.ai.orchestrator

enum class FeatureType {
    Tarot,
    Palm,
    VoiceAura,
    Dreams,
    Horoscope,
    Compatibility,
    BirthChart,
    Library
}

interface FeatureInput {
    val feature: FeatureType
    val sessionId: String
    val locale: String
    val fields: Map<String, String>
}

data class TarotInput(
    override val sessionId: String,
    override val locale: String = "en",
    val question: String,
    val spread: String,
    val cardsDrawn: String = ""
) : FeatureInput {
    override val feature: FeatureType = FeatureType.Tarot
    override val fields: Map<String, String>
        get() = mapOf(
            "question" to question,
            "spread" to spread,
            "cards_drawn" to cardsDrawn
        )
}

data class PalmInput(
    override val sessionId: String,
    override val locale: String = "en",
    val hand: String,
    val observations: String
) : FeatureInput {
    override val feature: FeatureType = FeatureType.Palm
    override val fields: Map<String, String>
        get() = mapOf(
            "hand" to hand,
            "observations" to observations
        )
}

data class VoiceAuraInput(
    override val sessionId: String,
    override val locale: String = "en",
    val transcript: String,
    val toneHint: String = ""
) : FeatureInput {
    override val feature: FeatureType = FeatureType.VoiceAura
    override val fields: Map<String, String>
        get() = mapOf(
            "transcript" to transcript,
            "tone_hint" to toneHint
        )
}

data class DreamsInput(
    override val sessionId: String,
    override val locale: String = "en",
    val dreamText: String,
    val mood: String = ""
) : FeatureInput {
    override val feature: FeatureType = FeatureType.Dreams
    override val fields: Map<String, String>
        get() = mapOf(
            "dream_text" to dreamText,
            "mood" to mood
        )
}

data class HoroscopeInput(
    override val sessionId: String,
    override val locale: String = "en",
    val sign: String,
    val dateIso: String,
    val focusArea: String = ""
) : FeatureInput {
    override val feature: FeatureType = FeatureType.Horoscope
    override val fields: Map<String, String>
        get() = mapOf(
            "sign" to sign,
            "date_iso" to dateIso,
            "focus_area" to focusArea
        )
}

data class CompatibilityInput(
    override val sessionId: String,
    override val locale: String = "en",
    val personA: String,
    val personB: String,
    val context: String = ""
) : FeatureInput {
    override val feature: FeatureType = FeatureType.Compatibility
    override val fields: Map<String, String>
        get() = mapOf(
            "person_a" to personA,
            "person_b" to personB,
            "context" to context
        )
}

data class BirthChartInput(
    override val sessionId: String,
    override val locale: String = "en",
    val birthDateIso: String,
    val birthTime24h: String,
    val birthPlace: String,
    val focusArea: String = ""
) : FeatureInput {
    override val feature: FeatureType = FeatureType.BirthChart
    override val fields: Map<String, String>
        get() = mapOf(
            "birth_date_iso" to birthDateIso,
            "birth_time_24h" to birthTime24h,
            "birth_place" to birthPlace,
            "focus_area" to focusArea
        )
}

data class LibraryInput(
    override val sessionId: String,
    override val locale: String = "en",
    val topic: String,
    val userNotes: String = ""
) : FeatureInput {
    override val feature: FeatureType = FeatureType.Library
    override val fields: Map<String, String>
        get() = mapOf(
            "topic" to topic,
            "user_notes" to userNotes
        )
}
