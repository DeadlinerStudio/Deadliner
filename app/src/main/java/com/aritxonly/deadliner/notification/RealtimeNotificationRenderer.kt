package com.aritxonly.deadliner.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.deadliner.platform.NotificationMetadataEntry
import com.deadliner.platform.NotificationKind
import com.deadliner.platform.NotificationPhase
import com.deadliner.platform.NotificationAction
import java.time.Instant
import java.time.ZoneId

internal object RealtimeNotificationRenderer {
    fun status(context: Context, phase: NotificationPhase?): String = context.getString(when (phase) {
        NotificationPhase.OVERDUE -> R.string.notification_live_overdue
        NotificationPhase.COMPLETED -> R.string.notification_live_completed
        NotificationPhase.ABANDONED -> R.string.notification_live_abandoned
        NotificationPhase.HABIT_DUE -> R.string.notification_live_habit_ready
        NotificationPhase.HABIT_CHECKED_IN -> R.string.notification_live_habit_checked
        NotificationPhase.FOCUS_FINISHED -> R.string.notification_live_focus_finished
        else -> R.string.notification_island_near
    })
    fun color(phase: NotificationPhase?): String = when (phase) {
        NotificationPhase.OVERDUE -> "#FFFF7659"
        NotificationPhase.COMPLETED, NotificationPhase.HABIT_CHECKED_IN, NotificationPhase.FOCUS_FINISHED -> "#FFA8DFC0"
        NotificationPhase.ABANDONED -> "#FFBFC5CE"
        else -> TaskIslandPresentation.BLUE
    }
    fun label(context: Context, action: NotificationAction): String = context.getString(when (action) {
        NotificationAction.COMPLETE -> R.string.mark_complete
        NotificationAction.GIVE_UP -> R.string.task_swipe_give_up
        NotificationAction.CHECK_IN -> R.string.notification_live_check_in
        NotificationAction.IGNORE -> R.string.notification_live_ignore
        NotificationAction.RESTORE_ACTIVE -> R.string.notification_live_undo
        NotificationAction.OPEN_FOCUS -> R.string.focus_timer_title
        NotificationAction.OPEN_TASK -> R.string.notification_live_open_task
    })
    fun deadline(context: Context, entry: NotificationMetadataEntry, now: Instant = Instant.now()): String =
        if (entry.kind == NotificationKind.FOCUS_RESULT) ""
        else TaskIslandPresentation.deadline(entry.dueAt, now, ZoneId.systemDefault()) { context.getString(R.string.notification_island_tomorrow, it) }

    fun build(context: Context, entry: NotificationMetadataEntry): Notification {
        val status = status(context, entry.phase)
        val timing = deadline(context, entry)
        val habit = entry.kind == NotificationKind.HABIT_REALTIME
        val focus = entry.kind == NotificationKind.FOCUS_RESULT
        val content = when {
            focus -> context.getString(R.string.notification_focus_result_content,
                com.aritxonly.deadliner.ui.focus.formatFocusClock(entry.focusDurationSeconds ?: 0))
            habit -> "${HabitIslandPresentation.period(context, entry)} · ${HabitIslandPresentation.progress(entry)}"
            else -> context.getString(R.string.notification_island_due, timing)
        }
        val open = if (focus && NotificationAction.OPEN_FOCUS in entry.actions && entry.focusSessionUid != null)
            com.aritxonly.deadliner.FocusTimerActivity.newResultIntent(context, requireNotNull(entry.focusSessionUid))
        else if (entry.kind == NotificationKind.HABIT_REALTIME) Intent(context, MainActivity::class.java)
            else DeadlineDetailActivity.newIntent(context, entry.ownerUid)
        open.data = Uri.Builder().scheme("deadliner").authority("realtime").appendPath(entry.key).build()
        val native = LiveUpdateNotificationAdapter.shouldRequestPromotion(context)
        val builder = NotificationCompat.Builder(context, if (focus) FocusRealtimeNotificationRenderer.CHANNEL_ID else CHANNEL_ID)
            .setSmallIcon(if (focus) R.drawable.ic_island_miuix_topdownloads else if (native) R.drawable.ic_upcoming_island else R.mipmap.ic_launcher)
            .setContentTitle(entry.title).setContentText("$status · $content")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$status · $content"))
            .setContentIntent(PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true).setOnlyAlertOnce(true).setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(android.graphics.Color.parseColor(color(entry.phase)))
            // A future when time makes Android's promoted chip count down by itself.
            .setWhen(0L).setShowWhen(false).setUsesChronometer(false).setChronometerCountDown(false)
        entry.actions.forEach { action -> builder.addAction(0, label(context, action), NotificationMetadataActions.pending(context, entry, action)) }
        if (native) builder.setRequestPromotedOngoing(true).setShortCriticalText(
            if (focus) context.getString(R.string.notification_island_encouragement)
            else if (habit) HabitIslandPresentation.progress(entry)
            else if (entry.phase in listOf(NotificationPhase.COMPLETED, NotificationPhase.ABANDONED)) status else timing)
        if (LiveUpdateNotificationAdapter.usesIsland(context)) builder.addExtras(XiaomiIslandExtras.createRealtime(context, entry, content))
        return builder.build()
    }
}
