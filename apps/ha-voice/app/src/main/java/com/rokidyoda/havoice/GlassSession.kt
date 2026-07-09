package com.rokidyoda.havoice

import android.content.Context
import android.util.Log
import com.rokid.cxr.Caps
import com.rokid.cxr.link.CXRLink
import com.rokid.cxr.link.callbacks.IAudioStreamCbk
import com.rokid.cxr.link.callbacks.ICXRLinkCbk
import com.rokid.cxr.link.callbacks.ICustomCmdCbk
import com.rokid.cxr.link.callbacks.IGlassAppCbk
import com.rokid.cxr.link.utils.CxrDefs
import com.rokid.cxr.link.utils.GlassInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

/**
 * Owns a CXR-L **CustomApp** session: installs + starts the on-glasses companion
 * (apps/havoice-glass), exchanges custom commands with it, and streams the glasses mic.
 *
 * Flow: [connect] (after auth token) → link ready → install/start the glass app →
 * glass app sends "ptt_start" (two-finger tap) → [onPtt] → phone captures audio →
 * [sendReply]/[sendStatus] push text back to the glass HUD. API mirrors Rokid's official
 * CXR-L sample (vendor-sdk/CXRLSample). See docs/HA-VOICE.md.
 */
class GlassSession(
    private val onEvent: (String) -> Unit,
    private val onPtt: () -> Unit,
) {
    private val tag = "GlassSession"
    private var link: CXRLink? = null
    private var appContext: Context? = null

    private var cxrl = false
    private var bt = false
    private var installTried = false

    private val _ready = MutableStateFlow(false)          // link (cxr+bt) up
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _appReady = MutableStateFlow(false)       // glass app installed + opened
    val appReady: StateFlow<Boolean> = _appReady.asStateFlow()

    private val linkCbk = object : ICXRLinkCbk {
        override fun onCXRLConnected(connected: Boolean) { cxrl = connected; syncReady() }
        override fun onGlassBtConnected(connected: Boolean) { bt = connected; syncReady() }
        override fun onGlassAiAssistStart() {}
        override fun onGlassAiAssistStop() {}
        override fun onGlassAiInterrupt(interruptWake: Boolean) {}
        override fun onGlassDeviceInfo(deviceInfo: GlassInfo) {}
        override fun onGlassWearingStatus(wearing: Boolean) {}
    }

    private val cmdCbk = object : ICustomCmdCbk {
        override fun onCustomCmdResult(key: String?, payload: ByteArray?) {
            if (key != Config.GLASS_TO_PHONE_KEY || payload == null) return
            val caps = runCatching { Caps.fromBytes(payload) }.getOrNull() ?: return
            if (caps.size() < 1) return
            when (caps.at(0).string) {
                Config.EVENT_PTT_START -> { Log.d(tag, "glass → ptt_start"); onPtt() }
            }
        }
    }

    private val appCbk = object : IGlassAppCbk {
        override fun onQueryAppResult(installed: Boolean) {
            Log.i(tag, "app installed=$installed")
            if (installed) startGlassApp() else installGlassApp()
        }
        override fun onInstallAppResult(success: Boolean) {
            Log.i(tag, "install success=$success")
            if (success) startGlassApp() else onEvent("Glass app install failed")
        }
        override fun onOpenAppResult(success: Boolean) {
            _appReady.value = success
            onEvent(if (success) "Glasses ready" else "Glass app start failed")
        }
        override fun onGlassAppResume(resumed: Boolean) { _appReady.value = resumed }
        override fun onStopAppResult(success: Boolean) { if (success) _appReady.value = false }
        override fun onUnInstallAppResult(success: Boolean) {}
    }

    fun connect(context: Context, token: String) {
        release()
        appContext = context.applicationContext
        installTried = false
        val l = CXRLink(context.applicationContext).apply {
            configCXRSession(CxrDefs.CXRSession(CxrDefs.CXRSessionType.CUSTOMAPP, Config.GLASS_PACKAGE))
            setCXRLinkCbk(linkCbk)
            setCXRCustomCmdCbk(cmdCbk)
        }
        link = l
        onEvent("Connecting…")
        l.connect(token)
    }

    /** Begin streaming the glasses mic (16 kHz mono PCM) to [cbk]. codecType 1 = PCM. */
    fun startAudioStream(cbk: IAudioStreamCbk): Boolean {
        val l = link ?: return false
        if (!_ready.value) return false
        l.setCXRAudioCbk(cbk)
        l.startAudioStream(1)
        return true
    }

    fun stopAudioStream() { runCatching { link?.stopAudioStream() } }

    fun sendStatus(text: String) = sendToGlass(Config.CMD_STATUS, text)
    fun sendReply(text: String) = sendToGlass(Config.CMD_REPLY, text)

    private fun sendToGlass(cmd: String, text: String) {
        val l = link ?: return
        runCatching {
            l.sendCustomCmd(Config.PHONE_TO_GLASS_KEY, Caps().apply { write(cmd); write(text) })
        }.onFailure { Log.e(tag, "sendToGlass failed", it) }
    }

    fun release() {
        link?.let { l ->
            runCatching { l.stopAudioStream() }
            runCatching { if (_appReady.value) l.appStop(appCbk) }
            runCatching { l.disconnect() }
        }
        link = null
        cxrl = false; bt = false
        _ready.value = false
        _appReady.value = false
    }

    private fun syncReady() {
        val r = cxrl && bt
        val was = _ready.value
        _ready.value = r
        if (r && !was) { onEvent("Link up — preparing glasses app"); ensureGlassApp() }
        if (!r && was) { onEvent("Glasses disconnected"); _appReady.value = false }
    }

    private fun ensureGlassApp() {
        val l = link ?: return
        runCatching { l.appIsInstalled(appCbk) }
            .onFailure { Log.e(tag, "appIsInstalled failed", it); installGlassApp() }
    }

    private fun startGlassApp() {
        val l = link ?: return
        runCatching { l.appStart(Config.GLASS_ENTRY, appCbk) }
            .onFailure { Log.e(tag, "appStart failed", it); onEvent("Glass app start failed") }
    }

    private fun installGlassApp() {
        if (installTried) { onEvent("Glass app not installed"); return }
        installTried = true
        val l = link ?: return
        val apk = stageGlassApk() ?: run { onEvent("Bundled glass.apk missing"); return }
        onEvent("Installing glasses app…")
        runCatching { l.appUploadAndInstall(apk, appCbk) }
            .onFailure { Log.e(tag, "appUploadAndInstall failed", it); onEvent("Glass app install failed") }
    }

    /** Copy the bundled glass APK from assets to a readable file; returns its path or null. */
    private fun stageGlassApk(): String? {
        val ctx = appContext ?: return null
        val out = File(ctx.filesDir, "havoice-glass.apk")
        return runCatching {
            ctx.assets.open(Config.GLASS_APK_ASSET).use { input ->
                FileOutputStream(out).use { input.copyTo(it) }
            }
            out.absolutePath
        }.getOrNull()
    }
}
