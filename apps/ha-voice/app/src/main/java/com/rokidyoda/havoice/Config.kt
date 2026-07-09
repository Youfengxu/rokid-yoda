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

    // ---- CustomApp: the on-glasses companion (apps/havoice-glass) ----
    /** Must match havoice-glass applicationId. The phone installs + starts this on the glasses. */
    const val GLASS_PACKAGE = "com.rokidyoda.havoiceglass"
    const val GLASS_ENTRY = "com.rokidyoda.havoiceglass.MainActivity"
    /** Built havoice-glass APK, shipped in this app's assets. See README for how to place it. */
    const val GLASS_APK_ASSET = "glass.apk"

    // ---- Custom-command protocol (must match havoice-glass Protocol.kt) ----
    const val GLASS_TO_PHONE_KEY = "hv_glass"
    const val PHONE_TO_GLASS_KEY = "hv_phone"
    const val EVENT_PTT_START = "ptt_start"
    const val CMD_STATUS = "status"
    const val CMD_REPLY = "reply"

    /**
     * Debug channel smoke-test. When true, a two-finger tap makes the phone echo a
     * "pong" straight back to the glasses HUD instead of capturing audio — proving the
     * glasses↔phone custom-command channel end-to-end WITHOUT STT/orchestrator/model.
     * Also enables a "Ping glasses" button. Set false for the real voice flow.
     */
    const val DEBUG_ECHO = true

    // ---- Voice capture ----
    enum class MicSource { PHONE, GLASSES }

    /** GLASSES = glasses mic via CXR-L PCM + on-device Vosk STT. PHONE = phone mic. */
    val MIC_SOURCE = MicSource.GLASSES

    /** Vosk model folder under assets/ (gitignored — download it; see README). */
    const val VOSK_MODEL_ASSET = "vosk-model-en"

    /** Read the reply aloud (through the glasses when they're the active BT audio device). */
    const val TTS_ENABLED = true

    // ---- Silence / endpointing for glasses-mic capture (tap-to-talk, auto-stop) ----
    /** Finalize after this much silence once we have some transcript. */
    const val SILENCE_MS = 1500L
    /** Cancel if no speech at all within this window. */
    const val NO_SPEECH_MS = 6000L
    /** Hard cap on a single utterance. */
    const val MAX_UTTERANCE_MS = 12000L
}
