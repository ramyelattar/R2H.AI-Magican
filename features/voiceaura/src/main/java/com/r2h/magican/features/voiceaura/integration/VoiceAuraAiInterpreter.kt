package com.r2h.magican.features.voiceaura.integration

import com.r2h.magican.features.voiceaura.domain.SessionAnalytics
import com.r2h.magican.features.voiceaura.domain.VoiceAuraInterpretation
import com.r2h.magican.features.voiceaura.domain.VoiceSessionMode
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class VoiceAuraAiInterpreter @Inject constructor() {

    fun buildTranscript(analytics: SessionAnalytics): String {
        return buildString {
            appendLine("mode=${analytics.mode.name.lowercase()}")
            appendLine("duration_sec=${"%.1f".format(analytics.durationMs / 1000f)}")
            appendLine("avg_energy=${"%.3f".format(analytics.avgEnergy)}")
            appendLine("peak_energy=${"%.3f".format(analytics.peakEnergy)}")
            appendLine("voice_presence=${"%.2f".format(analytics.voicePresence)}")
            appendLine("avg_pitch_hz=${analytics.avgPitchHz?.let { "%.1f".format(it) } ?: "n/a"}")
            appendLine("pitch_stability=${analytics.pitchStability?.let { "%.2f".format(it) } ?: "n/a"}")
            append("breaths_per_min=${analytics.breathsPerMinute?.let { "%.1f".format(it) } ?: "n/a"}")
        }
    }

    fun toneHint(analytics: SessionAnalytics): String {
        return when (analytics.mode) {
            VoiceSessionMode.Hum -> {
                when {
                    analytics.pitchStability != null && analytics.pitchStability > 0.72f -> "steady and centered"
                    analytics.avgEnergy > 0.20f -> "expressive and activated"
                    else -> "soft and introspective"
                }
            }
            VoiceSessionMode.Breath -> {
                val bpm = analytics.breathsPerMinute ?: return "calm breathing focus"
                when {
                    bpm < 6f -> "deep restorative breathing"
                    bpm <= 12f -> "balanced grounded breathing"
                    else -> "fast or elevated breathing"
                }
            }
        }
    }

    fun parse(orchestratorJson: String): VoiceAuraInterpretation {
        return runCatching {
            val root = JSONObject(orchestratorJson)
            val response = root.optJSONObject("response") ?: JSONObject()
            VoiceAuraInterpretation(
                summary = response.optString("summary", "No summary generated."),
                insights = response.optJSONArray("insights").toStringList(),
                actions = response.optJSONArray("actions").toStringList(),
                disclaimer = response.optString("disclaimer", "For self-reflection only."),
                raw = orchestratorJson
            )
        }.getOrElse {
            VoiceAuraInterpretation(
                summary = "Unable to parse AI interpretation.",
                insights = emptyList(),
                actions = emptyList(),
                disclaimer = "Try again with a new recording.",
                raw = orchestratorJson
            )
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(length())
        for (i in 0 until length()) out += optString(i)
        return out
    }
}
