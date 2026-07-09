package com.rokidyoda.havoice

/** App-wide constants. Edit ORCHESTRATOR_URL to match how your phone reaches k11-services. */
object Config {
    /**
     * Your existing orchestrator's /run endpoint (has the ha_control tool).
     * - Home Wi-Fi (LAN):     http://192.168.100.21:8100/run
     * - On tailnet (anywhere): http://k11-services.border-balance.ts.net:8100/run
     * Both hosts are allow-listed for cleartext in network_security_config.xml.
     */
    const val ORCHESTRATOR_URL = "http://192.168.100.21:8100/run"

    /** Prepended to every task so replies fit the 480x640 HUD (same trick as the G2 agent). */
    const val HUD_PREFIX = "Answer concisely for a small display: "

    /** Max user+assistant turns kept locally and sent back as `history` (orchestrator trims to 8). */
    const val MAX_HISTORY_TURNS = 6

    const val REQUEST_CODE_AUTH = 1001

    /** Where push-to-talk audio comes from. */
    enum class MicSource { PHONE, GLASSES }

    /**
     * PHONE  = phone mic via SpeechRecognizer (works out of the box).
     * GLASSES = glasses mic via CXR-L PCM stream + on-device Vosk STT (talk with phone
     *           pocketed). Requires the Vosk model in assets — see [VOSK_MODEL_ASSET].
     */
    val MIC_SOURCE = MicSource.GLASSES

    /**
     * Folder name under app/src/main/assets/ holding an unpacked Vosk model
     * (e.g. vosk-model-small-en-us-0.15 renamed to this). Gitignored — download it
     * yourself; see apps/ha-voice/README.md. Glasses PCM is 16 kHz mono 16-bit.
     */
    const val VOSK_MODEL_ASSET = "vosk-model-en"

    /** Read the reply aloud. Plays through the glasses when they're the active BT audio device. */
    const val TTS_ENABLED = true
}
