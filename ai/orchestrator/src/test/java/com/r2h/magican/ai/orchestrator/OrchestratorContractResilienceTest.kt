package com.r2h.magican.ai.orchestrator

import com.r2h.magican.ai.orchestrator.domain.AiResponse
import com.r2h.magican.ai.orchestrator.domain.ResponseMapper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrchestratorContractResilienceTest {

    private val mapper = ResponseMapper()
    private val encoder = JsonStyleEncoder()

    @Test
    fun `encoder parses nested response wrapped with non-json text`() {
        val raw = """
            model output:
            {"response":{"summary":"A short summary","insights":"i1;i2","actions":"a1\na2","disclaimer":"d"}}
        """.trimIndent()

        val normalized = JSONObject(encoder.requireValidResponseObject(raw))
        assertEquals("A short summary", normalized.getString("summary"))
        assertEquals("d", normalized.getString("disclaimer"))
        assertEquals(2, normalized.getJSONArray("insights").length())
        assertEquals(2, normalized.getJSONArray("actions").length())
    }

    @Test(expected = IllegalStateException::class)
    fun `encoder rejects non-json response`() {
        encoder.requireValidResponseObject("not-json-anywhere")
    }

    @Test
    fun `mapper normalizes success status`() {
        val raw = """
            {
              "request_id":"r1",
              "feature":"Tarot",
              "status":"success",
              "response":{"summary":"ok","insights":[],"actions":[],"disclaimer":"d"},
              "safety":{"allowed":true,"risk_level":"None"}
            }
        """.trimIndent()

        val result = mapper.map<Nothing>(raw)
        assertTrue(result is AiResponse.Success)
        val success = result as AiResponse.Success
        assertEquals("ok", success.metadata.status)
    }

    @Test
    fun `mapper infers blocked status when safety disallows`() {
        val raw = """
            {
              "request_id":"r2",
              "feature":"Tarot",
              "response":{"summary":"blocked by policy"},
              "safety":{"allowed":false,"risk_level":"High"}
            }
        """.trimIndent()

        val result = mapper.map<Nothing>(raw)
        assertTrue(result is AiResponse.Blocked)
        val blocked = result as AiResponse.Blocked
        assertEquals("blocked", blocked.metadata.status)
        assertEquals("High", blocked.riskLevel)
    }

    @Test
    fun `mapper marks invalid request as non-recoverable`() {
        val raw = """
            {
              "request_id":"r3",
              "feature":"Tarot",
              "status":"error",
              "error":"Invalid request payload"
            }
        """.trimIndent()

        val result = mapper.map<Nothing>(raw)
        assertTrue(result is AiResponse.Error)
        val error = result as AiResponse.Error
        assertFalse(error.isRecoverable)
    }

    @Test
    fun `mapper returns parse error envelope for malformed json`() {
        val result = mapper.map<Nothing>("{oops")
        assertTrue(result is AiResponse.Error)
        val error = result as AiResponse.Error
        assertEquals("parse_error", error.requestId)
        assertEquals("parse_error", error.metadata.status)
        assertTrue(error.isRecoverable)
    }
}
