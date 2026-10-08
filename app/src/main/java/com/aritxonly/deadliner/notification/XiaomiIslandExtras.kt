package com.aritxonly.deadliner.notification

import android.content.Context
import android.graphics.drawable.Icon
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import com.deadliner.platform.NotificationMetadataEntry
import com.deadliner.platform.NotificationKind
import com.deadliner.platform.NotificationPhase
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.annotation.DrawableRes
import org.json.JSONObject
import org.json.JSONArray
import android.content.Intent
import com.aritxonly.deadliner.R
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskAction
import java.time.Instant

/** Focus V3 wire format verified against HyperNotification 1.4, as used by Capsulyric. */
internal object XiaomiIslandExtras {
    private const val PICTURE = "miui.focus.pic_deadliner"
    private const val SMALL_PICTURE = "miui.land.pic_deadliner"
    private data class IconKey(val resource: Int, val size: Int, val tint: String?, val contentSize: Int)
    private val icons = mutableMapOf<IconKey, Icon>()
    internal fun bitmapIcon(context: Context, resource: Int, size: Int, tint: String? = null, contentSize: Int = size): Icon = synchronized(icons) {
        icons.getOrPut(IconKey(resource, size, tint, contentSize)) {
            val drawable = requireNotNull(ContextCompat.getDrawable(context, resource)).mutate()
            if (tint != null) drawable.setColorFilter(Color.parseColor(tint), PorterDuff.Mode.SRC_IN)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val content = contentSize.coerceIn(1, size)
            val inset = (size - content) / 2
            drawable.setBounds(inset, inset, inset + content, inset + content)
            drawable.draw(Canvas(bitmap))
            Icon.createWithBitmap(bitmap)
        }
    }
    fun createRealtime(context: Context, entry: NotificationMetadataEntry, content: String): Bundle {
        val status = RealtimeNotificationRenderer.status(context, entry.phase)
        val color = RealtimeNotificationRenderer.color(entry.phase)
        val deadline = RealtimeNotificationRenderer.deadline(context, entry)
        val habit = entry.kind == NotificationKind.HABIT_REALTIME
        val checked = entry.phase == NotificationPhase.HABIT_CHECKED_IN
        val summaryStatus = if (habit && !checked) HabitIslandPresentation.period(context, entry) else status
        val stateIcon = IslandStateIcons.realtime(entry.phase)
        val extras = create(context, entry.title, content, status, stateIcon)
        val root = JSONObject(extras.getString("miui.focus.param")!!)
        val params = root.getJSONObject("param_v2")
        val island = params.getJSONObject("param_island").put("highlightColor", color)
        val statePicture = "miui.land.pic_deadliner_state"
        val pictures = extras.getBundle("miui.focus.pics")!!
        // The template's app identity picture stays separate from its business clock icon.
        val clockPicture = "miui.focus.pic_deadliner_clock"
        pictures.putParcelable(statePicture, bitmapIcon(context, stateIcon, 88, color))
        pictures.putParcelable(clockPicture, bitmapIcon(context, stateIcon, 120, color))
        // Also replace the fallback small picture; every business surface shares the phase.
        pictures.putParcelable(SMALL_PICTURE, bitmapIcon(context, stateIcon, 88, color))
        val summaryPicture = JSONObject().put("type", 1).put("pic", statePicture)
        island.getJSONObject("bigIslandArea").put("imageTextInfoLeft", JSONObject().put("type", 1)
            .put("picInfo", summaryPicture).put("textInfo", JSONObject().put("title", summaryStatus)
                .put("narrowFont", false).put("showHighlightColor", true)))
        // Put the date prefix in small text and keep the main numerals at HH:mm.
        val time = deadline.takeLast(5)
        val prefix = deadline.dropLast(5).trim()
        // Use matching A/B image-text components, as in Capsulyric's two-column layout.
        // A standalone textInfo selects a different OEM layout; omit unused slots entirely.
        val right = if (habit) {
            if (checked) HabitIslandPresentation.progress(entry) else HabitIslandPresentation.name(entry.title)
        } else time
        val timeInfo = JSONObject().put("title", right).put("narrowFont", false).put("showHighlightColor", false)
        if (!habit && prefix.isNotEmpty()) timeInfo.put("frontTitle", prefix)
        val bigIsland = island.getJSONObject("bigIslandArea")
        bigIsland.remove("textInfo")
        bigIsland.put("imageTextInfoRight", JSONObject().put("type", 2).put("textInfo", timeInfo))
        island.getJSONObject("smallIslandArea").put("combinePicInfo", JSONObject().put("picInfo", summaryPicture))
        params.remove("baseInfo")
        params.put("business", "deadliner_realtime").put("ticker", "$summaryStatus $right")
            .put("iconTextInfo", JSONObject().put("animIconInfo", JSONObject().put("type", 0)
                .put("src", clockPicture).put("srcDark", clockPicture))
                .put("title", entry.title).put("content", content)
                .put("subContent", status).put("colorSubContent", color).put("colorSubContentDark", color))
        val buttons = JSONArray()
        entry.actions.forEach { action ->
            val key = "miui.focus.pic_action_" + action.name.lowercase(java.util.Locale.ROOT)
            val resource = when (action) {
                com.deadliner.platform.NotificationAction.IGNORE, com.deadliner.platform.NotificationAction.GIVE_UP -> R.drawable.msr_close_wght400
                com.deadliner.platform.NotificationAction.RESTORE_ACTIVE -> R.drawable.msr_undo_wght400
                else -> R.drawable.msr_task_alt_wght400
            }
            pictures.putParcelable(key, bitmapIcon(context, resource, 88))
            buttons.put(JSONObject().put("type", 0).put("actionTitle", RealtimeNotificationRenderer.label(context, action))
                .put("actionIcon", key).put("actionIntentType", 1)
                .put("actionIntent", NotificationMetadataActions.intent(context, entry, action).toUri(Intent.URI_INTENT_SCHEME)))
        }
        if (buttons.length() > 0) params.put("textButton", buttons)
        extras.putString("miui.focus.param", root.toString())
        return extras
    }

    fun createUpcoming(context: Context, task: Task, now: Instant = Instant.now()): Bundle {
        val deadline = TaskIslandPresentation.deadline(context, task, now)
        val extras = create(context, task.title, context.getString(R.string.notification_island_due, deadline),
            deadline, R.drawable.ic_upcoming_island)
        val root = JSONObject(extras.getString("miui.focus.param")!!)
        val parameters = root.getJSONObject("param_v2")
        val island = parameters.getJSONObject("param_island")
        island.put("highlightColor", TaskIslandPresentation.BLUE)
        val left = island.getJSONObject("bigIslandArea").getJSONObject("imageTextInfoLeft")
        left.put("type", 5).put("textInfo", JSONObject()
            .put("title", context.getString(R.string.notification_island_near))
            .put("showHighlightColor", true).put("narrowFont", false))
        island.getJSONObject("bigIslandArea").getJSONObject("textInfo").put("showHighlightColor", false)
        parameters.remove("baseInfo")
        parameters.put("business", "deadliner_upcoming")
            .put("ticker", context.getString(R.string.notification_island_near))
            .put("iconTextInfo", JSONObject().put("animIconInfo", JSONObject().put("type", 0)
                .put("src", PICTURE).put("srcDark", PICTURE))
                .put("title", task.title).put("content", context.getString(R.string.notification_island_due, deadline)))
        val buttons = JSONArray()
        val pictures = extras.getBundle("miui.focus.pics")!!
        listOf(
            Triple(TaskAction.MARK_GIVE_UP, R.string.task_swipe_give_up, R.drawable.msr_close_wght400),
            Triple(TaskAction.MARK_COMPLETE, R.string.mark_complete, R.drawable.msr_task_alt_wght400),
        ).forEach { (action, label, icon) ->
            val key = "miui.focus.pic_" + action.name.lowercase(java.util.Locale.ROOT)
            pictures.putParcelable(key, bitmapIcon(context, icon, 88))
            buttons.put(JSONObject().put("type", 0).put("actionTitle", context.getString(label))
                .put("actionIcon", key).put("actionIntentType", 1)
                .put("actionIntent", TaskNotificationActions.intent(context, task, action).toUri(Intent.URI_INTENT_SCHEME)))
        }
        parameters.put("textButton", buttons)
        extras.putString("miui.focus.param", root.toString())
        return extras
    }

    fun create(context: Context, title: String, content: String, status: String, @DrawableRes icon: Int): Bundle {
        val picture = JSONObject().put("type", 1).put("pic", PICTURE)
        val island = JSONObject().put("islandProperty", 1)
            .put("bigIslandArea", JSONObject()
                .put("imageTextInfoLeft", JSONObject().put("type", 1).put("picInfo", picture)
                    .put("textInfo", JSONObject().put("title", title).put("narrowFont", false)))
                .put("textInfo", JSONObject().put("title", status).put("narrowFont", false)))
            .put("smallIslandArea", JSONObject().put("combinePicInfo", JSONObject().put("picInfo",
                JSONObject().put("type", 1).put("pic", SMALL_PICTURE))))
        val parameters = JSONObject().put("business", "deadliner_timer")
            .put("isShowNotification", true).put("enableFloat", false).put("updatable", true)
            .put("islandFirstFloat", false).put("filterWhenNoPermission", false)
            .put("ticker", status).put("tickerPic", PICTURE).put("aodTitle", status)
            .put("baseInfo", JSONObject().put("type", 2).put("title", title).put("content", content))
            .put("picInfo", picture)
            .put("param_island", island)
        return Bundle().apply {
            putString("miui.focus.param", JSONObject().put("param_v2", parameters).toString())
            putBundle("miui.focus.pics", Bundle().apply {
                putParcelable(PICTURE, bitmapIcon(context, R.mipmap.ic_launcher, 120))
                putParcelable(SMALL_PICTURE, bitmapIcon(context, icon, 88))
            })
        }
    }
}
