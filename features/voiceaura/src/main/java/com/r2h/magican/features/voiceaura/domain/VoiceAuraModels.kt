package com.r2h.magican.features.voiceaura.domain

enum class VoiceSessionMode {
    Hum,
    Breath
}

data class VoiceFrame(
    val samples: ShortArray,
    val sampleRate: Int,
    val timestampMs: Long
)

data class VoiceFeatures(
    val energy: Float,
    val pitchHz: Float?,
    val voicedProbability: Float
)

data class SessionAnalytics(
    val mode: VoiceSessionMode,
    val durationMs: Long,
    val avgEnergy: Float,
    val peakEnergy: Float,
    val avgPitchHz: Float?,
    val pitchStability: Float?,
    val breathsPerMinute: Float?,
    val voicePresence: Float
)

data class VoiceAuraInterpretation(
    val summary: String,
    val insights: List<String>,
    val actions: List<String>,
    val disclaimer: String,
    val raw: String
)
