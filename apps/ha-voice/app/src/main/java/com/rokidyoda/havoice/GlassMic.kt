package com.rokidyoda.havoice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.rokid.cxr.link.callbacks.IAudioStreamCbk
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService

/**
 * Tap-to-talk speech capture from the GLASSES microphone, with automatic stop on silence.
 *
 * The glasses stream 16 kHz mono 16-bit PCM over CXR-L ([GlassSession.startAudioStream]).
 * We feed it to an on-device Vosk recognizer (offline → private). Once started (by a
 * two-finger tap), it auto-finalizes when the speaker goes quiet — no release needed:
 *  - Vosk endpointing (silence) → finalize, and/or
 *  - a [Config.SILENCE_MS] timer since the last new words,
 *  - [Config.NO_SPEECH_MS] with nothing said → cancel,
 *  - [Config.MAX_UTTERANCE_MS] hard cap.
 */
class GlassMic(
    private val context: Context,
    private val session: GlassSession,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val tag = "GlassMic"
    private val main = Handler(Looper.getMainLooper())

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private val segments = StringBuilder()
    private var lastPartialLen = 0
    @Volatile private var finalizing = false

    val isModelReady: Boolean get() = model != null

    private val onSilence = Runnable { finalizeUtterance() }
    private val onNoSpeech = Runnable { finalizeUtterance() }
    private val onMaxDuration = Runnable { finalizeUtterance() }

    /** Unpack + load the Vosk model from assets (async). Call once at startup. */
    fun loadModel() {
        if (model != null) return
        StorageService.unpack(
            context, Config.VOSK_MODEL_ASSET, "vosk-model",
            { m -> model = m; Log.i(tag, "Vosk model ready") },
            { e -> Log.e(tag, "Vosk model load failed", e); onError("STT model missing (${Config.VOSK_MODEL_ASSET})") },
        )
    }

    fun start(): Boolean {
        val m = model ?: run { onError("STT model still loading"); return false }
        segments.setLength(0)
        lastPartialLen = 0
        finalizing = false
        recognizer = Recognizer(m, 16_000.0f)
        val ok = session.startAudioStream(audioCbk)
        if (!ok) { onError("Glasses not ready for audio"); recognizer?.close(); recognizer = null; return false }
        main.postDelayed(onNoSpeech, Config.NO_SPEECH_MS)
        main.postDelayed(onMaxDuration, Config.MAX_UTTERANCE_MS)
        return true
    }

    /** Cancel without delivering a result (e.g. on teardown). */
    fun cancel() {
        finalizing = true
        clearTimers()
        session.stopAudioStream()
        runCatching { recognizer?.close() }
        recognizer = null
    }

    fun destroy() {
        cancel()
        model?.close()
        model = null
    }

    private fun finalizeUtterance() {
        if (finalizing) return
        finalizing = true
        clearTimers()
        session.stopAudioStream()
        val rec = recognizer ?: return
        runCatching { appendText(JSONObject(rec.finalResult).optString("text")) }
        recognizer = null
        rec.close()
        val text = segments.toString().trim()
        if (text.isEmpty()) onError("Didn't catch that") else onResult(text)
    }

    private fun clearTimers() {
        main.removeCallbacks(onSilence)
        main.removeCallbacks(onNoSpeech)
        main.removeCallbacks(onMaxDuration)
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
            if (finalizing || data == null || length <= 0) return
            val safeOffset = if (offset in 0 until data.size) offset else 0
            val safeLen = minOf(length, data.size - safeOffset).coerceAtLeast(0)
            if (safeLen <= 0) return
            val chunk = if (safeOffset == 0 && safeLen == data.size) data
                        else data.copyOfRange(safeOffset, safeOffset + safeLen)
            runCatching {
                if (rec.acceptWaveForm(chunk, safeLen)) {
                    // Vosk detected an endpoint (silence): utterance segment complete.
                    appendText(JSONObject(rec.result).optString("text"))
                    if (segments.isNotEmpty()) main.post { finalizeUtterance() }
                } else {
                    val partial = JSONObject(rec.partialResult).optString("partial")
                    if (partial.length > lastPartialLen) {
                        lastPartialLen = partial.length
                        onPartial(partial)
                        // New words → not silent; (re)arm the silence timer, drop no-speech.
                        main.removeCallbacks(onNoSpeech)
                        main.removeCallbacks(onSilence)
                        main.postDelayed(onSilence, Config.SILENCE_MS)
                    }
                }
            }.onFailure { Log.e(tag, "vosk feed failed", it) }
        }

        override fun onAudioError(errorCode: Int, errorInfo: String?) {
            onError("Glasses audio error $errorCode: ${errorInfo ?: ""}")
            main.post { cancel() }
        }

        override fun onAudioStreamStateChanged(started: Boolean) {
            Log.d(tag, "glass audio stream started=$started")
        }
    }
}
