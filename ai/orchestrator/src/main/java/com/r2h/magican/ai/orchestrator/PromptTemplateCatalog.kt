package com.r2h.magican.ai.orchestrator

import javax.inject.Inject
import javax.inject.Singleton

data class PromptTemplate(
    val id: String,
    val instruction: String
)

@Singleton
class PromptTemplateCatalog @Inject constructor() {

    fun select(feature: FeatureType): PromptTemplate {
        return when (feature) {
            FeatureType.Tarot -> PromptTemplate(
                id = "tarot_v1",
                instruction = "Interpret symbolic tarot meaning based on spread and cards."
            )
            FeatureType.Palm -> PromptTemplate(
                id = "palm_v1",
                instruction = "Analyze palm observations and provide concise interpretation."
            )
            FeatureType.VoiceAura -> PromptTemplate(
                id = "voiceaura_v1",
                instruction = "Infer emotional aura from transcript and tone hints."
            )
            FeatureType.Dreams -> PromptTemplate(
                id = "dreams_v1",
                instruction = "Interpret dream motifs and reflect practical next steps."
            )
            FeatureType.Horoscope -> PromptTemplate(
                id = "horoscope_v1",
                instruction = "Generate date-aware horoscope guidance for the provided sign."
            )
            FeatureType.Compatibility -> PromptTemplate(
                id = "compatibility_v1",
                instruction = "Assess relationship dynamics and communication fit."
            )
            FeatureType.BirthChart -> PromptTemplate(
                id = "birthchart_v1",
                instruction = "Summarize personality and timing themes from birth chart inputs."
            )
            FeatureType.Library -> PromptTemplate(
                id = "library_v1",
                instruction = "Retrieve educational style output with references-like structure."
            )
        }
    }

    fun buildPrompt(
        template: PromptTemplate,
        input: FeatureInput,
        safeFields: Map<String, String>
    ): String {
        val fieldsBlock = safeFields.entries.joinToString("\n") { (k, v) ->
            "- $k: $v"
        }

        return """
            You are Magican AI orchestrator worker.
            Feature: ${input.feature.name}
            Session: ${input.sessionId}
            Locale: ${input.locale}
            TemplateId: ${template.id}
            Task: ${template.instruction}

            UserInput:
            $fieldsBlock

            Output rules:
            1) Return only a valid JSON object.
            2) Keys must be: summary, insights, actions, disclaimer.
            3) summary is a short string.
            4) insights is an array of 2-5 short strings.
            5) actions is an array of 1-3 short strings.
            6) disclaimer is a short safety note.
            7) Do not include markdown.
        """.trimIndent()
    }
}
