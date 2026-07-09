package com.rokidyoda.havoiceglass

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Thin launcher for the on-glasses companion. Started by the phone (CXR-L appStart).
 * Its only jobs: make sure "draw over other apps" is granted, then start
 * [GlassBridgeService], which owns the CXR bridge, the two-finger-tap receiver, and the
 * overlay HUD — so the app keeps responding even when it's not the foreground app.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var hud: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        hud = findViewById(R.id.hud_text)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            hud.text = getString(R.string.hud_need_overlay)
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"),
                    )
                )
            }
            return
        }
        GlassBridgeService.start(this)
        hud.text = getString(R.string.hud_running)
    }
}
