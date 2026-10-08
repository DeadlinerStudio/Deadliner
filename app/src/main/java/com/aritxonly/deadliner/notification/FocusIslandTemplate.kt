package com.aritxonly.deadliner.notification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.aritxonly.deadliner.R
import com.deadliner.domain.focus.FocusTimerMode
import com.deadliner.service.focus.FocusTimerResult
import org.json.JSONArray
import org.json.JSONObject

/** Template 13: recording highlight + circular controls, without any recording permissions. */
internal object FocusIslandTemplate {
    fun create(context: Context, info: FocusTimerResult, nowMillis: Long = System.currentTimeMillis()): Bundle {
        val timer = requireNotNull(info.snapshot)
        val running = FocusRealtimeNotificationRenderer.running(timer)
        val clock = FocusRealtimeNotificationRenderer.clock(timer)
        val status = FocusRealtimeNotificationRenderer.status(context, timer)
        val color = if (running) TaskIslandPresentation.BLUE else "#FFBFC5CE"
        val stateIcon = IslandStateIcons.focus(info)
        val extras = XiaomiIslandExtras.create(context, timer.taskTitle, clock, status, stateIcon)
        val params = JSONObject(extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
        val pictures = extras.getBundle("miui.focus.pics")!!
        val clockPicture = "miui.focus.pic_focus_clock"
        val smallClock = "miui.land.pic_focus_clock"
        pictures.putParcelable(clockPicture, XiaomiIslandExtras.bitmapIcon(context, stateIcon, 120, color))
        pictures.putParcelable(smallClock, XiaomiIslandExtras.bitmapIcon(context, stateIcon, 88, color))
        val timerInfo = JSONObject().put("timerType", if (timer.mode == FocusTimerMode.COUNTDOWN) -1 else 1)
            .put("timerWhen", FocusRealtimeNotificationRenderer.anchor(timer, nowMillis)).put("timerSystemCurrent", nowMillis)
        val highlight = JSONObject().put("type", 1).put("subContent", status)
            .put("picFunction", clockPicture).put("picFunctionDark", clockPicture)
            .put("colorTitle", "#FFFFFFFF").put("colorTitleDark", "#FFFFFFFF")
            .put("colorSubContent", color).put("colorSubContentDark", color)
        if (running) highlight.put("timerInfo", timerInfo) else highlight.put("title", clock)
        params.remove("baseInfo")
        params.remove("picInfo")
        params.put("business", "deadliner_focus_timer").put("ticker", status).put("tickerPic", clockPicture)
            .put("highlightInfo", highlight)
        val actions = JSONArray()
        FocusNotificationActions.available(timer).filter { it != FocusNotificationAction.FINISH_AND_COMPLETE }.forEach { action ->
            // DecoLand's icon branch uses built-in recording drawables (including their circles).
            // actionBgColor is only read by its text branch, so custom glyphs lose the background.
            val key = when (action) {
                FocusNotificationAction.PAUSE -> "action_pause_record"
                FocusNotificationAction.RESUME -> "action_restart_record"
                FocusNotificationAction.FINISH -> "action_record_end"
                FocusNotificationAction.FINISH_AND_COMPLETE -> error("Not an island control")
            }
            actions.put(JSONObject().put("type", 0).put("actionIcon", key).put("actionIconDark", key)
                .put("actionTitle", context.getString(FocusNotificationActions.label(action))).put("actionIntentType", 1)
                .put("actionIntent", FocusNotificationActions.intent(context, info, action).toUri(Intent.URI_INTENT_SCHEME))
                .put("clickWithCollapse", false))
        }
        params.put("actions", actions)
        // OEM prioritizes digit over timerInfo and hides the chronometer when both are sent.
        val digits = JSONObject().put("showHighlightColor", false)
        if (running) digits.put("timerInfo", timerInfo) else digits.put("digit", clock)
        params.put("param_island", JSONObject().put("islandProperty", 1).put("highlightColor", color)
            .put("bigIslandArea", JSONObject()
                .put("imageTextInfoLeft", JSONObject().put("type", 1)
                    .put("picInfo", JSONObject().put("type", 1).put("pic", smallClock))
                    .put("textInfo", JSONObject().put("title", status).put("narrowFont", false).put("showHighlightColor", true)))
                .put("sameWidthDigitInfo", digits))
            .put("smallIslandArea", JSONObject().put("combinePicInfo", JSONObject()
                .put("picInfo", JSONObject().put("type", 1).put("pic", smallClock)))))
        extras.putString("miui.focus.param", JSONObject().put("param_v2", params).toString())
        return extras
    }
}
