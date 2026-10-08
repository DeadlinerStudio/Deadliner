package com.aritxonly.deadliner.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.aritxonly.deadliner.FocusTimerActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.focus.formatFocusClock
import com.deadliner.domain.focus.FocusTimerMode
import com.deadliner.domain.focus.FocusTimerSnapshot
import com.deadliner.domain.focus.FocusTimerState
import com.deadliner.domain.focus.FocusTimerWarning
import com.deadliner.service.focus.FocusTimerResult

internal object FocusRealtimeNotificationRenderer {
    const val CHANNEL_ID = "deadliner_focus_running"
    const val NOTIFICATION_ID = 0xF0C05
    fun running(timer: FocusTimerSnapshot) = timer.state == FocusTimerState.RUNNING && timer.warning != FocusTimerWarning.CLOCK_ANOMALY
    fun clock(timer: FocusTimerSnapshot) = formatFocusClock(timer.remainingSeconds ?: timer.elapsedSeconds)
    fun status(context: Context, timer: FocusTimerSnapshot) = context.getString(
        if (running(timer)) R.string.focus_running else R.string.focus_paused)
    fun anchor(timer: FocusTimerSnapshot, nowMillis: Long): Long = if (timer.mode == FocusTimerMode.COUNTDOWN)
        nowMillis + requireNotNull(timer.remainingSeconds) * 1_000 else nowMillis - timer.elapsedSeconds * 1_000

    fun signature(context: Context, info: FocusTimerResult): String {
        val timer = requireNotNull(info.snapshot)
        return "${timer.sessionUid}:${info.activeTimer?.updatedAt}:${timer.taskTitle}:${timer.mode}:${timer.state}:${timer.warning}:" +
            "${FocusNotificationActions.available(timer)}:${if (running(timer)) "running" else clock(timer)}:${IslandWhitelistBypass.mode(context)}"
    }

    fun build(context: Context, info: FocusTimerResult, nowMillis: Long = System.currentTimeMillis()): Notification {
        val timer = requireNotNull(info.snapshot)
        val status = status(context, timer)
        val mode = context.getString(if (timer.mode == FocusTimerMode.COUNTDOWN) R.string.focus_countdown_mode else R.string.focus_stopwatch_mode)
        val content = if (running(timer)) "$status · $mode" else "$status · ${clock(timer)}"
        val open = FocusTimerActivity.newIntent(context, timer.taskUid, timer.taskTitle)
            .setData(Uri.Builder().scheme("deadliner-focus").authority(context.packageName).appendPath(timer.sessionUid).build())
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_focus_notification).setContentTitle(timer.taskTitle).setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(PendingIntent.getActivity(context, NOTIFICATION_ID, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setOnlyAlertOnce(true).setOngoing(true).setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setColor(android.graphics.Color.parseColor(if (running(timer)) TaskIslandPresentation.BLUE else "#FFBFC5CE"))
            .setWhen(if (running(timer)) anchor(timer, nowMillis) else 0L).setShowWhen(running(timer))
            .setUsesChronometer(running(timer)).setChronometerCountDown(timer.mode == FocusTimerMode.COUNTDOWN && running(timer))
        FocusNotificationActions.available(timer).forEach {
            builder.addAction(FocusNotificationActions.icon(it), context.getString(FocusNotificationActions.label(it)), FocusNotificationActions.pending(context, info, it))
        }
        if (LiveUpdateNotificationAdapter.shouldRequestPromotion(context)) {
            builder.setRequestPromotedOngoing(true)
            if (!running(timer)) builder.setShortCriticalText(status)
        }
        if (LiveUpdateNotificationAdapter.usesIsland(context)) builder.addExtras(FocusIslandTemplate.create(context, info, nowMillis))
        return builder.build()
    }
}
