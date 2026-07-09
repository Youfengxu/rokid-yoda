package com.rokidyoda.havoice

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Reads the orchestrator's reply aloud.
 *
 * CXR-L has no "play audio on the glasses" API (its audio capability is mic capture only),
 * so we use the phone's [TextToSpeech]. When the Rokid glasses are the phone's active
 * Bluetooth audio output, the speech comes out of the glasses. We tag the audio as
 * ASSISTANT/SPEECH so the system routes it like a voice assistant.
 */
class Tts(context: Context) {
    private val tag = "Tts"
    private var ready = false

    // Explicit type: the onInit lambda references `tts`, which would otherwise make
    // type inference recursive.
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val preferred = Locale.getDefault()
            val available = tts.isLanguageAvailable(preferred) >= TextToSpeech.LANG_AVAILABLE
            tts.language = if (available) preferred else Locale.US
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            ready = true
        } else {
            Log.w(tag, "TTS init failed: $status")
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ha-reply")
    }

    fun stop() {
        runCatching { tts.stop() }
    }

    fun shutdown() {
        runCatching { tts.stop(); tts.shutdown() }
    }
}
