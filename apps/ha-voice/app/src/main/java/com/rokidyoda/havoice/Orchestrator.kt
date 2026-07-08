package com.rokidyoda.havoice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One turn of conversation kept locally and replayed to the stateless orchestrator. */
data class Turn(val role: String, val content: String)

/**
 * Thin client for the existing homelab orchestrator (`POST /run`), which owns the
 * ha_control tool. Reused unchanged — see docs/HA-VOICE.md.
 */
object Orchestrator {

    /**
     * Sends [utterance] as a task, with recent [history] for follow-ups.
     * Returns the orchestrator's reply text (best-effort extracted from the JSON).
     * Runs on IO; call from a coroutine.
     */
    suspend fun run(utterance: String, history: List<Turn>): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("task", Config.HUD_PREFIX + utterance)
            if (history.isNotEmpty()) {
                put("history", JSONArray().apply {
                    history.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
                })
            }
        }.toString()

        val conn = (URL(Config.ORCHESTRATOR_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 60_000            // agent + tool execution can take a while
            setRequestProperty("Content-Type", "application/json")
        }

        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) return@withContext "Error $code: ${text.take(140)}"
            extractReply(text)
        } catch (e: Exception) {
            "Network error: ${e.message}"
        } finally {
            conn.disconnect()
        }
    }

    /**
     * The orchestrator returns `{"result": "..."}` (verified against the live :8100/run on
     * 2026-07-08). We check `result` first, then a few fallbacks in case the shape changes.
     */
    private fun extractReply(raw: String): String {
        return try {
            val json = JSONObject(raw)
            for (key in listOf("result", "reply", "response", "answer", "output", "text", "message")) {
                if (json.has(key)) {
                    val v = json.get(key)
                    return if (v is JSONObject) v.optString("content", v.toString()) else v.toString()
                }
            }
            raw
        } catch (_: Exception) {
            raw
        }.trim()
    }
}
