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
}
