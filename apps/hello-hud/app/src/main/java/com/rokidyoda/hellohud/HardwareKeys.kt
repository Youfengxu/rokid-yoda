package com.rokidyoda.hellohud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Rokid Glasses hardware button / touchpad input.
 *
 * YodaOS-Sprite delivers input as ORDERED broadcasts. Register this receiver with a
 * high priority and call [BroadcastReceiver.abortBroadcast] to consume an event so the
 * system default doesn't also fire. Action strings are verbatim from Rokid's official
 * CXR-S sample (see vendor-sdk/CXRSSDKSamples).
 *
 * Some gestures are reserved by the OS and cannot be intercepted: long-press touchpad
 * (opens Rokid AI app), double-tap button (Back), top-button tap (photo) / long-press
 * (video). See docs/SDK-REFERENCE.md.
 */
enum class RokidKey(val action: String) {
    CLICK("com.android.action.ACTION_SPRITE_BUTTON_CLICK"),
    BUTTON_DOWN("com.android.action.ACTION_SPRITE_BUTTON_DOWN"),
    BUTTON_UP("com.android.action.ACTION_SPRITE_BUTTON_UP"),
    DOUBLE_CLICK("com.android.action.ACTION_SPRITE_BUTTON_DOUBLE_CLICK"),
    AI_START("com.android.action.ACTION_AI_START"),
    LONG_PRESS("com.android.action.ACTION_SPRITE_BUTTON_LONG_PRESS"),
    TWO_FINGER_SINGLE_TAP("com.android.action.ACTION_TWO_FINGER_SINGLE_TAP"),
    TWO_FINGER_DOUBLE_TAP("com.android.action.ACTION_TWO_FINGER_DOUBLE_TAP"),
    TWO_FINGER_SWIPE_FORWARD("com.android.action.ACTION_TWO_FINGER_SWIPE_FORWARD"),
    TWO_FINGER_SWIPE_BACK("com.android.action.ACTION_TWO_FINGER_SWIPE_BACK"),
    SETTINGS_KEY("com.android.action.ACTION_SETTINGS_KEY");

    companion object {
        private val byAction = entries.associateBy { it.action }
        fun fromAction(action: String?): RokidKey? = action?.let { byAction[it] }

        /** IntentFilter covering every Rokid key action, at high ordered-broadcast priority. */
        fun intentFilter(priority: Int = 100): IntentFilter =
            IntentFilter().apply {
                entries.forEach { addAction(it.action) }
                this.priority = priority
            }
    }
}

class RokidKeyReceiver(
    private val onKey: (RokidKey) -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val key = RokidKey.fromAction(intent?.action) ?: return
        onKey(key)
        // Consume the event so the OS default action doesn't also run.
        if (isOrderedBroadcast) abortBroadcast()
    }
}
