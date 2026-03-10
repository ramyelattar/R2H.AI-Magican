package com.r2h.magican.features.tarot.integration

import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

data class TarotReading(
    val summary: String,
    val insights: List<String>,
    val actions: List<String>,
    val disclaimer: String,
    val raw: String
)

@Singleton
class TarotAiInterpreter @Inject constructor() {

    fun parse(orchestratorJson: String): TarotReading {
        return runCatching {
            val root = JSONObject(orchestratorJson)
            val response = root.optJSONObject("response") ?: JSONObject()

            TarotReading(
                summary = response.optString("summary", "No summary generated."),
                insights = response.optJSONArray("insights").toStringList(),
                actions = response.optJSONArray("actions").toStringList(),
                disclaimer = response.optString("disclaimer", "For reflection and entertainment only."),
                raw = orchestratorJson
            )
        }.getOrElse {
            TarotReading(
                summary = "Unable to parse AI response.",
                insights = emptyList(),
                actions = emptyList(),
                disclaimer = "Please try again.",
                raw = orchestratorJson
            )
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(length())
        for (i in 0 until length()) {
            out += optString(i)
        }
        return out
    }
}
