package com.rokidyoda.havoiceglass

/**
 * Custom-command protocol between this on-glasses app (CXR-S) and the ha-voice phone
 * app (CXR-L). Both sides must agree on these strings — the phone app duplicates them
 * in its own Config.kt. See docs/HA-VOICE.md.
 *
 * Direction & transport:
 *  - Glasses → phone: CXRServiceBridge.sendMessage(GLASS_TO_PHONE_KEY, caps)
 *                     → phone ICustomCmdCbk.onCustomCmdResult(key = GLASS_TO_PHONE_KEY, payload)
 *  - Phone → glasses: phone CXRLink.sendCustomCmd(PHONE_TO_GLASS_KEY, caps)
 *                     → glasses CXRServiceBridge.subscribe(PHONE_TO_GLASS_KEY).onReceive(...)
 */
object Protocol {
    const val GLASS_TO_PHONE_KEY = "hv_glass"
    const val PHONE_TO_GLASS_KEY = "hv_phone"

    /** Glasses → phone events: caps.write(event). */
    const val EVENT_PTT_START = "ptt_start"

    /** Phone → glasses commands: caps.write(cmd); caps.write(text). */
    const val CMD_STATUS = "status"   // transient state, e.g. "Listening…", "Thinking…"
    const val CMD_REPLY = "reply"     // final answer to display
}
