package com.aritxonly.deadliner.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.deadliner.service.HabitReminderPlanner
import kotlinx.datetime.LocalDate
import java.time.Instant
import java.time.ZoneId
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.habit.HabitStatus
import com.deadliner.domain.task.TaskState
import java.time.LocalDateTime
import com.deadliner.platform.NotificationService

/** Renders notifications for schedules created by the KMP NotificationOrchestrator. */
class KmpNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationDiagnostics.event("alarm.received", "action" to intent.action,
            "uid" to intent.getStringExtra(NotificationService.EXTRA_UID),
            "scheduledAt" to intent.getLongExtra(NotificationService.EXTRA_TRIGGER_MILLIS, -1L),
            "receivedAt" to System.currentTimeMillis())
        KmpV6Runtime.initialize(context)
        if (KmpV6Runtime.usesNotificationMetadata()) {
            NotificationDiagnostics.event("alarm.legacy_ignored", "action" to intent.action)
            KmpV6Runtime.triggerNotificationInfoUpdate()
            return
        }
        if (intent.action == NotificationService.ACTION_LIVE) {
            if (!canPostNotifications(context)) { skip("live", null, "post_permission_denied"); return }
            UpcomingLiveUpdateService.start(
                context = context,
                taskUid = intent.getStringExtra(NotificationService.EXTRA_UID) ?: return,
                updateIntervalSeconds = intent.getIntExtra(NotificationService.EXTRA_INTERVAL_SECONDS, 30),
            )
            return
        }
        if (intent.action == NotificationService.ACTION_DAILY) {
            KmpV6Runtime.scheduleDailySummary()
            showDailySummary(context)
            return
        }
        val uid = intent.getStringExtra(NotificationService.EXTRA_UID) ?: return
        val title = intent.getStringExtra(NotificationService.EXTRA_TITLE).orEmpty()
        when (intent.action) {
            NotificationService.ACTION_TASK -> showTask(context, uid, title, intent)
            NotificationService.ACTION_HABIT -> showHabit(context, uid, title, intent)
        }
    }

    private fun showTask(context: Context, uid: String, fallbackTitle: String, intent: Intent) {
        val task = KmpV6Runtime.database.tasks.find(uid) ?: run { skip("task", uid, "not_found"); return }
        if (!GlobalUtils.deadlineNotification || task.isDeleted || task.state != TaskState.ACTIVE) { skip("task", uid, "disabled_or_inactive"); return }
        val due = NotificationTiming.dueInstant(task.dueAt) ?: run { skip("task", uid, "invalid_due"); return }
        val expected = due.toEpochMilli() - GlobalUtils.deadlineNotificationBefore * 3_600_000L
        if (intent.hasExtra(NotificationService.EXTRA_TRIGGER_MILLIS) &&
            intent.getLongExtra(NotificationService.EXTRA_TRIGGER_MILLIS, 0L) != expected) { skip("task", uid, "stale_alarm"); return }
        if (!canPostNotifications(context)) { skip("reminder", uid, "post_permission_denied"); return }
        val open = PendingIntent.getActivity(
            context,
            uid.hashCode(),
            DeadlineDetailActivity.newIntent(context, uid)
                .setData(Uri.Builder().scheme("deadliner").authority("task").appendPath(uid).build()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notify(context, uid, task.title.ifBlank { fallbackTitle }, context.getString(R.string.remaining_due_soon), open)
    }

    private fun showHabit(context: Context, uid: String, fallbackTitle: String, intent: Intent) {
        val habit = KmpV6Runtime.database.habits.find(uid) ?: run { skip("habit", uid, "not_found"); return }
        if (habit.isDeleted || habit.status != HabitStatus.ACTIVE || habit.reminder?.isEnabled != true) { skip("habit", uid, "disabled_or_inactive"); return }
        KmpV6Runtime.onHabitNotificationDelivered(habit)
        val scheduledAt = intent.getLongExtra(NotificationService.EXTRA_TRIGGER_MILLIS, System.currentTimeMillis())
        val occurrence = Instant.ofEpochMilli(scheduledAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val planned = HabitReminderPlanner.plan(
            habit, KmpV6Runtime.database.habits.records(uid), LocalDate.parse(occurrence.toLocalDate().toString()), 0,
        )
        if (planned.none { runCatching { LocalDateTime.parse(it) }.getOrNull() == occurrence }) { skip("habit", uid, "completed_or_stale_occurrence"); return }
        if (!canPostNotifications(context)) { skip("reminder", uid, "post_permission_denied"); return }
        val open = PendingIntent.getActivity(
            context,
            uid.hashCode(),
            Intent(context, MainActivity::class.java)
                .setData(Uri.Builder().scheme("deadliner").authority("habit").appendPath(uid).build()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notify(context, uid, habit.name.ifBlank { fallbackTitle }, context.getString(R.string.habit_notification), open)
    }

    private fun showDailySummary(context: Context) {
        if (!GlobalUtils.dailyStatsNotification || !canPostNotifications(context)) { skip("daily", null, "disabled_or_permission_denied"); return }
        val now = Instant.now()
        val actionable = KmpV6Runtime.database.tasks.list().filter {
            !it.isDeleted && it.state == TaskState.ACTIVE && it.dueAt != null
        }
        val dueTimes = actionable.mapNotNull { NotificationTiming.dueInstant(it.dueAt) }
        val overdue = dueTimes.count { it.isBefore(now) }
        val dueToday = dueTimes.count {
            !it.isBefore(now) && it.atZone(ZoneId.systemDefault()).toLocalDate() == now.atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val inProgress = dueTimes.size - overdue - dueToday
        val title = if (dueToday == 0) context.getString(R.string.title_today_overview)
            else context.getString(R.string.title_today_overview_with_due, dueToday)
        val content = context.getString(R.string.summary_status, overdue, inProgress)
        val open = PendingIntent.getActivity(
            context,
            DAILY_NOTIFICATION_ID,
            com.aritxonly.deadliner.OverviewActivity.newIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notify(context, DAILY_NOTIFICATION_ID.toString(), title, content, open, CHANNEL_DAILY_ID)
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun notify(
        context: Context,
        uid: String,
        title: String,
        content: String,
        open: PendingIntent,
        channelId: String = CHANNEL_ID,
    ) {
        NotificationUtil.createNotificationChannels(context)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            context.getSystemService(NotificationManager::class.java).notify("reminder:$uid", uid.hashCode(), notification)
            NotificationDiagnostics.event("reminder.published", "uid" to uid, "channel" to channelId, "id" to uid.hashCode())
            NotificationDiagnostics.observe(context, uid, uid.hashCode(), "reminder:$uid")
        } catch (error: Exception) { NotificationDiagnostics.failure("reminder.publish_failed", error, "uid" to uid) }
    }

    private fun skip(kind: String, uid: String?, reason: String) =
        NotificationDiagnostics.event("receiver.skipped", "kind" to kind, "uid" to uid, "reason" to reason)

    private companion object {
        const val DAILY_NOTIFICATION_ID = 114514
    }
}
