package com.r2h.magican.ai.orchestrator.domain

import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized mapper for AI orchestration JSON responses.
 * Converts raw JSON strings into typed AiResponse domain models.
 */
@Singleton
class ResponseMapper @Inject constructor() {

    /**
     * Maps orchestrator JSON envelope to typed AiResponse.
     * Handles success, blocked, and error states uniformly.
     */
    fun <T> map(
        raw: String,
        contentTransformer: ((ResponseContent) -> T)? = null
    ): AiResponse<T> {
        return runCatching {
            val root = JSONObject(raw)
            val requestId = root.optString("request_id", "unknown")
            val status = normalizeStatus(root)
            val metadata = extractMetadata(root, status)

            when (status) {
                "ok" -> mapSuccess(root, requestId, metadata, contentTransformer)
                "blocked" -> mapBlocked(root, requestId, metadata)
                "error" -> mapError(root, requestId, metadata)
                "parse_error" -> mapParseError(root, requestId, metadata)
                else -> AiResponse.Error(
                    requestId = requestId,
                    message = "Unknown status: $status",
                    isRecoverable = true,
                    metadata = metadata
                )
            }
        }.getOrElse { exception ->
            // JSON parsing failure
            AiResponse.Error(
                requestId = "parse_error",
                message = "Failed to parse response: ${exception.message}",
                isRecoverable = true,
                metadata = ResponseMetadata(
                    feature = "unknown",
                    templateId = null,
                    timestampUtc = "",
                    status = "parse_error",
                    safetyAllowed = false,
                    safetyRiskLevel = null
                )
            )
        }
    }

    private fun <T> mapSuccess(
        root: JSONObject,
        requestId: String,
        metadata: ResponseMetadata,
        contentTransformer: ((ResponseContent) -> T)?
    ): AiResponse<T> {
        val responseObj = root.optJSONObject("response") ?: JSONObject()
        val content = ResponseContent(
            summary = sanitizeText(responseObj.optString("summary"), default = "No summary generated."),
            insights = sanitizeContentList(responseObj.optJSONArray("insights"), maxItems = 5),
            actions = sanitizeContentList(responseObj.optJSONArray("actions"), maxItems = 3),
            disclaimer = sanitizeText(
                responseObj.optString("disclaimer"),
                default = "For self-reflection only."
            )
        )

        val typedData = contentTransformer?.invoke(content)

        return AiResponse.Success(
            requestId = requestId,
            content = content,
            metadata = metadata,
            typedData = typedData
        )
    }

    private fun mapBlocked(
        root: JSONObject,
        requestId: String,
        metadata: ResponseMetadata
    ): AiResponse<Nothing> {
        val safetyObj = root.optJSONObject("safety") ?: JSONObject()
        val riskLevel = sanitizeText(safetyObj.optString("risk_level"), default = "Unknown")
        val responseObj = root.optJSONObject("response") ?: JSONObject()
        val reason = sanitizeText(
            responseObj.optString("summary"),
            default = "Request blocked by safety policy."
        )

        return AiResponse.Blocked(
            requestId = requestId,
            reason = reason,
            riskLevel = riskLevel,
            metadata = metadata
        )
    }

    private fun mapError(
        root: JSONObject,
        requestId: String,
        metadata: ResponseMetadata
    ): AiResponse<Nothing> {
        val errorMessage = sanitizeText(
            root.optString("error"),
            default = "Unknown error occurred."
        )
        
        // Determine if error is recoverable based on message content
        val isRecoverable = when {
            "unavailable" in errorMessage.lowercase() -> true
            "timeout" in errorMessage.lowercase() -> true
            "network" in errorMessage.lowercase() -> true
            "parse_error" in errorMessage.lowercase() -> true
            "invalid request" in errorMessage.lowercase() -> false
            else -> true
        }

        return AiResponse.Error(
            requestId = requestId,
            message = errorMessage,
            isRecoverable = isRecoverable,
            metadata = metadata
        )
    }

    private fun mapParseError(
        root: JSONObject,
        requestId: String,
        metadata: ResponseMetadata
    ): AiResponse<Nothing> {
        return AiResponse.Error(
            requestId = requestId,
            message = sanitizeText(
                root.optString("error"),
                default = "Runtime produced an invalid response."
            ),
            isRecoverable = true,
            metadata = metadata
        )
    }

    private fun extractMetadata(root: JSONObject, normalizedStatus: String): ResponseMetadata {
        val safetyObj = root.optJSONObject("safety") ?: JSONObject()

        return ResponseMetadata(
            feature = root.optString("feature", "unknown"),
            templateId = root.optNullableString("template_id"),
            timestampUtc = root.optString("timestamp_utc", ""),
            status = normalizedStatus,
            safetyAllowed = safetyObj.optBoolean("allowed", false),
            safetyRiskLevel = safetyObj.optNullableString("risk_level")
        )
    }

    private fun normalizeStatus(root: JSONObject): String {
        val raw = root.optString("status", "").trim().lowercase()
        if (raw == "ok" || raw == "success") return "ok"
        if (raw == "blocked" || raw == "denied") return "blocked"
        if (raw == "error" || raw == "failed") return "error"
        if (raw == "parse_error") return "parse_error"

        if (root.has("error")) return "error"
        if (root.optJSONObject("safety")?.optBoolean("allowed") == false) return "blocked"
        if (root.has("response")) return "ok"
        return "error"
    }

    private fun sanitizeText(value: String, default: String): String {
        return value.trim().ifBlank { default }
    }

    private fun sanitizeContentList(input: JSONArray?, maxItems: Int): List<String> {
        if (input == null || input.length() == 0) return emptyList()

        val out = ArrayList<String>(input.length().coerceAtMost(maxItems))
        for (i in 0 until input.length()) {
            if (out.size >= maxItems) break
            val item = input.opt(i)?.toString()?.trim().orEmpty()
            if (item.isNotBlank()) out += item
        }
        return out
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().ifBlank { null }
    }
}
