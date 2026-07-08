package com.rokidyoda.hellohud

import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Minimal on-glasses HUD app.
 *
 * The Rokid Glasses display is a single green monochrome micro-LED at 480x640
 * (right eye only). So: black background, one big bright line of text, nothing
 * else. This is the "hello world" that proves your build/install/launch loop
 * works — see ../../../../../../../README.md and docs/DEVICE-SETUP.md.
 *
 * Next step: add the CXR-S bridge (docs/SDK-REFERENCE.md) to receive text from a
 * phone companion app and render it here.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the HUD awake while the app is foregrounded.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        val hud = findViewById<TextView>(R.id.hud_text)
        hud.text = getString(R.string.hud_greeting)
    }
}
