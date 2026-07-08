package com.rokidyoda.havoice

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds the CXR-L CustomView JSON for the glasses HUD.
 *
 * Layout: a full-screen black LinearLayout with a single green TextView (id "textView").
 * Matches the 480x640 green-monochrome display. Props/format mirror Rokid's official
 * CXR-L sample (vendor-sdk/CXRLSample). See docs/HA-VOICE.md.
 */
object Hud {

    const val TEXT_ID = "textView"

    /** JSON for customViewOpen — the initial HUD layout showing [text]. */
    fun openLayout(text: String): String {
        val textProps = JSONObject()
            .put("id", TEXT_ID)
            .put("layout_width", "match_parent")
            .put("layout_height", "wrap_content")
            .put("text", text)
            .put("textColor", "#00FF00")
            .put("textSize", "22sp")
            .put("textStyle", "bold")
            .put("gravity", "center")
            .put("paddingStart", "24dp")
            .put("paddingEnd", "24dp")

        val textView = JSONObject().put("type", "TextView").put("props", textProps)

        val rootProps = JSONObject()
            .put("id", "root")
            .put("layout_width", "match_parent")
            .put("layout_height", "match_parent")
            .put("backgroundColor", "#FF000000")
            .put("orientation", "vertical")
            .put("gravity", "center")
            .put("marginTop", "120dp")
            .put("marginBottom", "80dp")

        return JSONObject()
            .put("type", "LinearLayout")
            .put("props", rootProps)
            .put("children", JSONArray().put(textView))
            .toString()
    }

    /** JSON for customViewUpdate — replaces the HUD text in place. */
    fun updateText(text: String): String {
        val update = JSONObject()
            .put("action", "update")
            .put("id", TEXT_ID)
            .put("props", JSONObject().put("text", text))
        return JSONArray().put(update).toString()
    }
}
