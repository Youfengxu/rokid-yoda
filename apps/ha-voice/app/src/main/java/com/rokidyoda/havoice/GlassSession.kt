package com.rokidyoda.havoice

import android.content.Context
import android.util.Log
import com.rokid.cxr.link.CXRLink
import com.rokid.cxr.link.callbacks.IAudioStreamCbk
import com.rokid.cxr.link.callbacks.ICXRLinkCbk
import com.rokid.cxr.link.callbacks.ICustomViewCbk
import com.rokid.cxr.link.utils.CxrDefs
import com.rokid.cxr.link.utils.GlassInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns a single CXR-L CustomView session and the HUD on the glasses.
 *
 * Lifecycle: [connect] (after auth token) → wait for [ready] → [showHud] opens the
 * view → [updateHud] pushes text → [release] closes + disconnects.
 * API mirrors Rokid's official CXR-L sample (vendor-sdk/CXRLSample).
 */
class GlassSession(private val onEvent: (String) -> Unit) {

    private val tag = "GlassSession"
    private var link: CXRLink? = null

    private var cxrl = false
    private var bt = false

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _hudOpen = MutableStateFlow(false)
    val hudOpen: StateFlow<Boolean> = _hudOpen.asStateFlow()

    private val linkCbk = object : ICXRLinkCbk {
        override fun onCXRLConnected(connected: Boolean) { cxrl = connected; syncReady() }
        override fun onGlassBtConnected(connected: Boolean) { bt = connected; syncReady() }
        override fun onGlassAiAssistStart() {}
        override fun onGlassAiAssistStop() {}
        override fun onGlassAiInterrupt(interruptWake: Boolean) {}
        override fun onGlassDeviceInfo(deviceInfo: GlassInfo) {
            Log.d(tag, "deviceInfo brightness=${deviceInfo.brightness} sound=${deviceInfo.sound}")
        }
        override fun onGlassWearingStatus(wearing: Boolean) {}
    }

    private val viewCbk = object : ICustomViewCbk {
        override fun onCustomViewOpened() { _hudOpen.value = true; onEvent("HUD open") }
        override fun onCustomViewUpdated() {}
        override fun onCustomViewClosed() { _hudOpen.value = false }
        override fun onCustomViewIconsSent() {}
        override fun onCustomViewError(code: Int, msg: String?) {
            _hudOpen.value = false
            onEvent("HUD error $code: ${msg ?: ""}")
        }
    }

    /** Configure a CustomView session and connect with the auth token. */
    fun connect(context: Context, token: String) {
        release()
        val l = CXRLink(context.applicationContext).apply {
            configCXRSession(CxrDefs.CXRSession(CxrDefs.CXRSessionType.CUSTOMVIEW))
            setCXRLinkCbk(linkCbk)
            setCXRCustomViewCbk(viewCbk)
        }
        link = l
        onEvent("Connecting…")
        l.connect(token)
    }

    /** Open the HUD with initial [text]. Safe to call once [ready] is true. */
    fun showHud(text: String) {
        val l = link ?: return
        if (!_ready.value) return
        l.customViewOpen(Hud.openLayout(text))
    }

    /** Replace the HUD text in place. */
    fun updateHud(text: String) {
        val l = link ?: return
        if (_hudOpen.value) l.customViewUpdate(Hud.updateText(text))
        else if (_ready.value) l.customViewOpen(Hud.openLayout(text))
    }

    /**
     * Start streaming the glasses microphone (16 kHz mono 16-bit PCM) to [cbk].
     * Requires an open CustomView (scene built) and MICROPHONE glass permission.
     * codecType 1 = PCM. Returns false if the link isn't ready.
     */
    fun startAudioStream(cbk: IAudioStreamCbk): Boolean {
        val l = link ?: return false
        if (!_ready.value) return false
        l.setCXRAudioCbk(cbk)
        l.startAudioStream(1)
        return true
    }

    fun stopAudioStream() {
        runCatching { link?.stopAudioStream() }
    }

    fun release() {
        link?.let { l ->
            runCatching { l.stopAudioStream() }
            runCatching { if (_hudOpen.value) l.customViewClose() }
            runCatching { l.disconnect() }
        }
        link = null
        cxrl = false; bt = false
        _ready.value = false
        _hudOpen.value = false
    }

    private fun syncReady() {
        val r = cxrl && bt
        val was = _ready.value
        _ready.value = r
        if (r && !was) onEvent("Glasses connected")
        if (!r && was) onEvent("Glasses disconnected")
    }
}
