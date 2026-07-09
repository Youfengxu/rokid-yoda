package com.rokidyoda.havoiceglass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Rokid glasses button / touchpad input via YodaOS ordered broadcasts. We register at
 * high priority and consume the event with abortBroadcast(). Action strings are from
 * Rokid's official CXR-S sample. This app uses the two-finger touchpad tap as PTT.
 *
 * Reserved gestures (cannot be intercepted): touchpad long-press (Rokid AI app),
 * top-button tap/long-press (photo/video), button double-tap (Back).
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
        if (isOrderedBroadcast) abortBroadcast()
    }
}
