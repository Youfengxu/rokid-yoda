package com.rokidyoda.hellohud

import android.os.Bundle
import android.widget.TextView
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Minimal on-glasses HUD app.
 *
 * The Rokid Glasses display is a single green monochrome micro-LED at 480x640
 * (right eye only). So: black background, big bright text, nothing else. This is
 * the "hello world" that proves your build/install/launch loop works — see the
 * top-level README and docs/DEVICE-SETUP.md.
 *
 * It also registers a [RokidKeyReceiver] so you can see hardware button / touchpad
 * events on the HUD — tap the temple button and the label updates. Next step: add
 * the CXR-S bridge (docs/SDK-REFERENCE.md) to exchange messages with a phone app.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var hud: TextView

    private val keyReceiver = RokidKeyReceiver { key ->
        // DOUBLE_CLICK is the OS Back gesture and can't be swallowed; the rest can.
        hud.text = getString(R.string.hud_key, key.name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the HUD awake while the app is foregrounded.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        hud = findViewById(R.id.hud_text)
        hud.text = getString(R.string.hud_greeting)
    }

    override fun onResume() {
        super.onResume()
        // ContextCompat picks the right registerReceiver overload across API levels
        // (glasses are API 31/32; the flags overload alone would need API 33+).
        ContextCompat.registerReceiver(
            this, keyReceiver, RokidKey.intentFilter(), ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(keyReceiver)
    }
}
