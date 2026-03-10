package com.r2h.magican.ai.orchestrator

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class JsonStyleEncoder @Inject constructor() {

    fun success(
        requestId: String,
        feature: FeatureType,
        templateId: String,
        policyResult: SafetyPolicyResult,
        responseObjectJson: String
    ): String {
        return buildString {
            append("{")
            append("\"request_id\":").append(quote(requestId)).append(",")
            append("\"feature\":").append(quote(feature.name)).append(",")
            append("\"status\":\"ok\",")
            append("\"template_id\":").append(quote(templateId)).append(",")
            append("\"timestamp_utc\":").append(quote(Instant.now().toString())).append(",")
            append("\"safety\":").append(policyJson(policyResult)).append(",")
            append("\"response\":").append(responseObjectJson)
            append("}")
        }
    }

    fun blocked(
        requestId: String,
        feature: FeatureType,
        policyResult: SafetyPolicyResult
    ): String {
        return buildString {
            append("{")
            append("\"request_id\":").append(quote(requestId)).append(",")
            append("\"feature\":").append(quote(feature.name)).append(",")
            append("\"status\":\"blocked\",")
            append("\"timestamp_utc\":").append(quote(Instant.now().toString())).append(",")
            append("\"safety\":").append(policyJson(policyResult)).append(",")
            append("\"response\":{")
            append("\"summary\":").append(quote(policyResult.blockReason ?: "Request blocked by safety policy.")).append(",")
            append("\"insights\":[],")
            append("\"actions\":[],")
            append("\"disclaimer\":\"Please revise the request and try again.\"")
            append("}")
            append("}")
        }
    }

    fun error(
        requestId: String,
        feature: FeatureType,
        templateId: String?,
        policyResult: SafetyPolicyResult,
        message: String
    ): String {
        return buildString {
            append("{")
            append("\"request_id\":").append(quote(requestId)).append(",")
            append("\"feature\":").append(quote(feature.name)).append(",")
            append("\"status\":\"error\",")
            if (templateId != null) {
                append("\"template_id\":").append(quote(templateId)).append(",")
            }
            append("\"timestamp_utc\":").append(quote(Instant.now().toString())).append(",")
            append("\"safety\":").append(policyJson(policyResult)).append(",")
            append("\"error\":").append(quote(message)).append(",")
            append("\"response\":{")
            append("\"summary\":\"Runtime generation error.\",")
            append("\"insights\":[],")
            append("\"actions\":[],")
            append("\"disclaimer\":\"Please retry.\"")
            append("}")
            append("}")
        }
    }

    fun requireValidResponseObject(raw: String): String {
        val rawObj = parseResponseObject(raw)
        val payload = rawObj.optJSONObject("response") ?: rawObj

        val summary = sanitizeText(payload.optString("summary"))
        val disclaimer = sanitizeText(
            payload.optString("disclaimer"),
            default = "Please review this output for self-reflection only."
        )
        val insights = sanitizeStringArray(payload.opt("insights"), maxItems = 5)
        val actions = sanitizeStringArray(payload.opt("actions"), maxItems = 3)

        return JSONObject().apply {
            put("summary", summary)
            put("disclaimer", disclaimer)
            put("insights", JSONArray().apply { insights.forEach { put(it) } })
            put("actions", JSONArray().apply { actions.forEach { put(it) } })
        }.toString()
    }

    private fun sanitizeText(value: String, default: String = "No summary generated."): String {
        return value.trim().ifBlank { default }
    }

    private fun parseResponseObject(raw: String): JSONObject {
        val trimmed = raw.trim()
        val direct = runCatching { JSONObject(trimmed) }.getOrNull()
        if (direct != null) return direct

        val candidate = extractLikelyJsonObject(trimmed)
            ?: error("Model response is not valid JSON object")
        return runCatching { JSONObject(candidate) }
            .getOrElse { error("Model response is not valid JSON object") }
    }

    private fun extractLikelyJsonObject(text: String): String? {
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1)
        }
        return null
    }

    private fun sanitizeStringArray(input: Any?, maxItems: Int): List<String> {
        if (input == null || input == JSONObject.NULL) return emptyList()

        if (input is String) {
            return input
                .split(Regex("[\\r\\n;]+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(maxItems)
        }

        if (input !is JSONArray || input.length() == 0) {
            val single = input.toString().trim()
            return if (single.isBlank()) emptyList() else listOf(single.take(500))
        }

        val out = ArrayList<String>(input.length().coerceAtMost(maxItems))
        for (i in 0 until input.length()) {
            if (out.size >= maxItems) break
            val item = input.opt(i)
            val text = item.toString().trim()
            if (text.isNotBlank()) out += text
        }
        return out
    }

    private fun policyJson(policyResult: SafetyPolicyResult): String {
        val classifications = policyResult.classifications
            .filter { it.level != RiskLevel.None }
            .joinToString(separator = ",") { c ->
                buildString {
                    append("{")
                    append("\"category\":").append(quote(c.category.name)).append(",")
                    append("\"level\":").append(quote(c.level.name)).append(",")
                    append("\"reason\":").append(quote(c.reason))
                    append("}")
                }
            }

        return buildString {
            append("{")
            append("\"allowed\":").append(if (policyResult.allowed) "true" else "false").append(",")
            append("\"risk_level\":").append(quote(policyResult.riskLevel.name)).append(",")
            append("\"classifications\":[").append(classifications).append("]")
            append("}")
        }
    }

    fun quote(value: String): String {
        val escaped = buildString {
            value.forEach { c ->
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(c)
                }
            }
        }
        return "\"$escaped\""
    }
}
