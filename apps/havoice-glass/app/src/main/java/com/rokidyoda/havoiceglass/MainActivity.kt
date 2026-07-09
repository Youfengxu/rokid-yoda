package com.rokidyoda.havoiceglass

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.rokid.cxr.CXRServiceBridge
import com.rokid.cxr.Caps

/**
 * On-glasses companion for HA-Voice (CustomApp mode).
 *
 * - Renders the HUD (black bg / green text) on the glasses display.
 * - Two-finger touchpad tap → tells the phone to start listening (PTT trigger).
 * - Shows status / reply text pushed from the phone over the CXR link.
 *
 * The phone (ha-voice, CXR-L) installs and starts this app, does the STT + orchestrator
 * call, and sends the reply back. See docs/HA-VOICE.md and Protocol.kt.
 */
class MainActivity : AppCompatActivity() {

    private val tag = "HaVoiceGlass"
    private lateinit var hud: TextView
    private val bridge = CXRServiceBridge()
    private var taps = 0

    private val keyReceiver = RokidKeyReceiver { key ->
        if (key == RokidKey.TWO_FINGER_SINGLE_TAP) startPtt()
    }

    private val statusListener = object : CXRServiceBridge.StatusListener {
        override fun onConnected(addr: String?, type: Int) = setHud("Ready — two-finger tap to talk")
        override fun onDisconnected() = setHud("Disconnected")
        override fun onARTCStatus(quality: Float, ok: Boolean) {}
    }

    private val phoneMsg = object : CXRServiceBridge.MsgCallback {
        override fun onReceive(name: String?, args: Caps?, bytes: ByteArray?) {
            if (args == null || args.size() < 2) return
            val cmd = args.at(0).string
            val text = args.at(1).string
            Log.i(tag, "from phone: cmd=$cmd text=$text")
            setHud(text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        hud = findViewById(R.id.hud_text)

        bridge.setStatusListener(statusListener)
        bridge.subscribe(Protocol.PHONE_TO_GLASS_KEY, phoneMsg)
        setHud(getString(R.string.hud_boot))
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            this, keyReceiver, RokidKey.intentFilter(), ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(keyReceiver) }
    }

    /** Tell the phone to start listening (or, in the phone's DEBUG_ECHO mode, echo a pong). */
    private fun startPtt() {
        taps++
        val caps = Caps().apply { write(Protocol.EVENT_PTT_START) }
        val rc = bridge.sendMessage(Protocol.GLASS_TO_PHONE_KEY, caps)
        Log.i(tag, "two-finger tap #$taps → sendMessage ptt_start rc=$rc")
        setHud(if (rc == 0) "Tap #$taps sent…" else "Phone not linked")
    }

    private fun setHud(text: String) = runOnUiThread { hud.text = text }
}
