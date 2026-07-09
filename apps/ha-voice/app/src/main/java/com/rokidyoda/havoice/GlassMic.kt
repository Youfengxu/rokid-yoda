package com.rokidyoda.havoice

import android.content.Context
import android.util.Log
import com.rokid.cxr.link.callbacks.IAudioStreamCbk
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService

/**
 * Push-to-talk speech capture from the GLASSES microphone.
 *
 * The glasses stream 16 kHz mono 16-bit PCM over CXR-L ([GlassSession.startAudioStream]).
 * We feed that PCM into an on-device Vosk recognizer — fully offline, so nothing leaves
 * the phone (keeps the "no self-hosted STT / private" property while letting you talk with
 * the phone pocketed). The model is loaded from assets; see README (Config.VOSK_MODEL_ASSET).
 */
class GlassMic(
    private val context: Context,
    private val session: GlassSession,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val tag = "GlassMic"
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private val segments = StringBuilder()

    val isModelReady: Boolean get() = model != null

    /** Unpack + load the Vosk model from assets (async). Call once at startup. */
    fun loadModel() {
        if (model != null) return
        StorageService.unpack(
            context,
            Config.VOSK_MODEL_ASSET,
            "vosk-model",
            { m -> model = m; Log.i(tag, "Vosk model ready") },
            { e -> Log.e(tag, "Vosk model load failed", e); onError("STT model missing (${Config.VOSK_MODEL_ASSET})") },
        )
    }

    fun start(): Boolean {
        val m = model ?: run { onError("STT model still loading"); return false }
        segments.setLength(0)
        recognizer = Recognizer(m, 16_000.0f)
        val ok = session.startAudioStream(audioCbk)
        if (!ok) { onError("Glasses not ready for audio"); recognizer?.close(); recognizer = null }
        return ok
    }

    /** Stop streaming and deliver the final transcript. */
    fun stop() {
        session.stopAudioStream()
        val rec = recognizer ?: return
        runCatching {
            appendText(JSONObject(rec.finalResult).optString("text"))
        }
        recognizer = null
        rec.close()
        val text = segments.toString().trim()
        if (text.isEmpty()) onError("Didn't catch that") else onResult(text)
    }

    fun destroy() {
        runCatching { recognizer?.close() }
        recognizer = null
        model?.close()
        model = null
    }

    private fun appendText(part: String?) {
        if (!part.isNullOrBlank()) {
            if (segments.isNotEmpty()) segments.append(' ')
            segments.append(part.trim())
        }
    }

    private val audioCbk = object : IAudioStreamCbk {
        override fun onAudioReceived(data: ByteArray?, offset: Int, length: Int) {
            val rec = recognizer ?: return
            if (data == null || length <= 0) return
            // Vosk's acceptWaveForm reads from index 0, so slice the SDK's [offset,length).
            val safeOffset = if (offset in 0 until data.size) offset else 0
            val safeLen = minOf(length, data.size - safeOffset).coerceAtLeast(0)
            if (safeLen <= 0) return
            val chunk = if (safeOffset == 0 && safeLen == data.size) data
                        else data.copyOfRange(safeOffset, safeOffset + safeLen)
            runCatching {
                if (rec.acceptWaveForm(chunk, safeLen)) {
                    appendText(JSONObject(rec.result).optString("text"))
                } else {
                    JSONObject(rec.partialResult).optString("partial")
                        .takeIf { it.isNotBlank() }?.let(onPartial)
                }
            }.onFailure { Log.e(tag, "vosk feed failed", it) }
        }

        override fun onAudioError(errorCode: Int, errorInfo: String?) {
            onError("Glasses audio error $errorCode: ${errorInfo ?: ""}")
        }

        override fun onAudioStreamStateChanged(started: Boolean) {
            Log.d(tag, "glass audio stream started=$started")
        }
    }
}
