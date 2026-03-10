package com.r2h.magican.ai.orchestrator

import com.r2h.magican.ai.orchestrator.domain.AiResponse
import com.r2h.magican.ai.orchestrator.domain.ResponseMapper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OrchestratorSchemaFuzzTest {

    private val encoder = JsonStyleEncoder()
    private val mapper = ResponseMapper()

    @Test
    fun `encoder fuzz normalizes mixed model payloads`() {
        val random = Random(20260310)

        repeat(250) {
            val raw = fuzzModelPayload(random)
            val normalizedRaw = encoder.requireValidResponseObject(raw)
            val normalized = JSONObject(normalizedRaw)

            assertTrue(normalized.has("summary"))
            assertTrue(normalized.has("disclaimer"))
            assertTrue(normalized.has("insights"))
            assertTrue(normalized.has("actions"))

            assertTrue(normalized.optString("summary").isNotBlank())
            assertTrue(normalized.optString("disclaimer").isNotBlank())

            val insights = normalized.getJSONArray("insights")
            val actions = normalized.getJSONArray("actions")
            assertTrue(insights.length() <= 5)
            assertTrue(actions.length() <= 3)
        }
    }

    @Test
    fun `mapper fuzz returns stable response object for malformed envelopes`() {
        val random = Random(424242)

        repeat(350) {
            val raw = fuzzEnvelope(random)
            val result = mapper.map<Nothing>(raw)

            when (result) {
                is AiResponse.Success -> {
                    assertTrue(result.content.summary.isNotBlank())
                    assertTrue(result.content.disclaimer.isNotBlank())
                    assertTrue(result.content.insights.size <= 5)
                    assertTrue(result.content.actions.size <= 3)
                    assertTrue(result.metadata.status.isNotBlank())
                }

                is AiResponse.Blocked -> {
                    assertTrue(result.reason.isNotBlank())
                    assertTrue(result.riskLevel.isNotBlank())
                    assertTrue(result.metadata.status == "blocked")
                }

                is AiResponse.Error -> {
                    assertTrue(result.message.isNotBlank())
                    assertTrue(result.metadata.status == "error" || result.metadata.status == "parse_error")
                }
            }
        }
    }

    private fun fuzzModelPayload(random: Random): String {
        val payload = JSONObject().apply {
            put("summary", randomStringOrBlank(random, maxWords = 8))
            put("disclaimer", randomStringOrBlank(random, maxWords = 6))
            put("insights", fuzzListLikeValue(random, maxItems = 8))
            put("actions", fuzzListLikeValue(random, maxItems = 5))
        }

        val root = if (random.nextBoolean()) {
            JSONObject().put("response", payload)
        } else {
            payload
        }

        val raw = root.toString()
        return when (random.nextInt(4)) {
            0 -> raw
            1 -> "model_output:\n$raw"
            2 -> "```json\n$raw\n```"
            else -> "prefix $raw suffix"
        }
    }

    private fun fuzzEnvelope(random: Random): String {
        if (random.nextInt(10) == 0) return "{invalid-json-${random.nextInt()}"

        val root = JSONObject()

        if (random.nextBoolean()) root.put("request_id", "req-${random.nextInt(1_000_000)}")
        if (random.nextBoolean()) root.put("feature", listOf("Tarot", "Palm", "Dreams", "Library").random(random))
        if (random.nextBoolean()) root.put("template_id", "tpl-${random.nextInt(100)}")
        if (random.nextBoolean()) root.put("timestamp_utc", "2026-03-10T12:${random.nextInt(60).toString().padStart(2, '0')}:00Z")

        if (random.nextBoolean()) {
            root.put(
                "status",
                listOf("ok", "success", "blocked", "denied", "error", "failed", "parse_error", "", "unknown")
                    .random(random)
            )
        }

        if (random.nextBoolean()) {
            root.put(
                "safety",
                JSONObject().apply {
                    if (random.nextBoolean()) put("allowed", random.nextBoolean())
                    if (random.nextBoolean()) put("risk_level", listOf("None", "Low", "Medium", "High", "Critical").random(random))
                }
            )
        }

        when (random.nextInt(5)) {
            0 -> root.put("response", JSONObject().apply {
                put("summary", randomStringOrBlank(random, maxWords = 10))
                put("disclaimer", randomStringOrBlank(random, maxWords = 8))
                put("insights", fuzzListLikeValue(random, maxItems = 8))
                put("actions", fuzzListLikeValue(random, maxItems = 5))
            })
            1 -> root.put("response", JSONArray().put("not-an-object"))
            2 -> root.put("response", "string-response")
            3 -> root.put("response", JSONObject.NULL)
        }

        if (random.nextBoolean()) {
            root.put("error", listOf("timeout while generating", "Invalid request payload", "network issue", "").random(random))
        }

        val raw = root.toString()
        return if (random.nextInt(8) == 0) "noise-$raw-tail" else raw
    }

    private fun fuzzListLikeValue(random: Random, maxItems: Int): Any {
        return when (random.nextInt(5)) {
            0 -> JSONArray().apply {
                repeat(random.nextInt(maxItems + 1)) {
                    put(if (random.nextInt(4) == 0) "" else "item-${random.nextInt(100)}")
                }
            }
            1 -> (1..random.nextInt(1, maxItems.coerceAtLeast(2))).joinToString(";") { "item-${random.nextInt(100)}" }
            2 -> (1..random.nextInt(1, maxItems.coerceAtLeast(2))).joinToString("\n") { "item-${random.nextInt(100)}" }
            3 -> random.nextInt(1000)
            else -> JSONObject.NULL
        }
    }

    private fun randomStringOrBlank(random: Random, maxWords: Int): String {
        if (random.nextInt(6) == 0) return ""
        val words = random.nextInt(1, maxWords.coerceAtLeast(2))
        val content = (1..words).joinToString(" ") { "w${random.nextInt(1000)}" }
        return if (random.nextBoolean()) content else " $content "
    }
}
