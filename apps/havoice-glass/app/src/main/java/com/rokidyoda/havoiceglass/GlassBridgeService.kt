package com.rokidyoda.havoiceglass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rokid.cxr.CXRServiceBridge
import com.rokid.cxr.Caps

/**
 * Foreground service that owns the CXR bridge + two-finger-tap receiver, so HA-Voice keeps
 * responding even when this app isn't the visible/foreground app on the glasses. The HUD
 * is drawn as a system overlay (transparent bg, green text) so replies show over whatever
 * is on screen. Needs "draw over other apps" permission (requested by MainActivity).
 */
class GlassBridgeService : Service() {

    private val tag = "HaVoiceGlass"
    private val bridge = CXRServiceBridge()
    private var overlay: TextView? = null
    private var taps = 0

    private val keyReceiver = RokidKeyReceiver { key ->
        if (key == RokidKey.TWO_FINGER_SINGLE_TAP) startPtt()
    }

    private val statusListener = object : CXRServiceBridge.StatusListener {
        override fun onConnected(addr: String?, type: Int) = showHud("Ready — two-finger tap to talk")
        override fun onDisconnected() = showHud("Disconnected")
        override fun onARTCStatus(quality: Float, ok: Boolean) {}
    }

    private val phoneMsg = object : CXRServiceBridge.MsgCallback {
        override fun onReceive(name: String?, args: Caps?, bytes: ByteArray?) {
            if (args == null || args.size() < 2) return
            Log.i(tag, "from phone: cmd=${args.at(0).string} text=${args.at(1).string}")
            showHud(args.at(1).string)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        bridge.setStatusListener(statusListener)
        bridge.subscribe(Protocol.PHONE_TO_GLASS_KEY, phoneMsg)
        ContextCompat.registerReceiver(
            this, keyReceiver, RokidKey.intentFilter(), ContextCompat.RECEIVER_EXPORTED
        )
        addOverlay()
        showHud("Ready — two-finger tap to talk")
        Log.i(tag, "GlassBridgeService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { unregisterReceiver(keyReceiver) }
        removeOverlay()
        super.onDestroy()
    }

    private fun startPtt() {
        taps++
        val rc = bridge.sendMessage(Protocol.GLASS_TO_PHONE_KEY, Caps().apply { write(Protocol.EVENT_PTT_START) })
        Log.i(tag, "two-finger tap #$taps → ptt_start rc=$rc")
        showHud(if (rc == 0) "Listening…" else "Phone not linked")
    }

    // ---- Overlay HUD (shows over any app) ----

    private fun addOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(tag, "no overlay permission — HUD will only show when the app is foreground")
            return
        }
        val tv = TextView(this).apply {
            setTextColor(Color.parseColor("#FF00FF66"))
            textSize = 30f
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }
        runCatching {
            windowManager().addView(tv, lp)
            overlay = tv
        }.onFailure { Log.e(tag, "addView overlay failed", it) }
    }

    private fun removeOverlay() {
        overlay?.let { tv -> runCatching { windowManager().removeView(tv) } }
        overlay = null
    }

    private fun showHud(text: String) {
        val tv = overlay
        if (tv != null) tv.post { tv.text = text } else Log.i(tag, "HUD (no overlay): $text")
    }

    private fun windowManager() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // ---- Foreground notification ----

    private fun buildNotification(): Notification {
        val chId = "havoice_glass"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(chId, "HA Voice", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, chId)
            .setContentTitle("HA Voice")
            .setContentText("Two-finger tap to talk")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 1
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, GlassBridgeService::class.java))
        }
    }
}
