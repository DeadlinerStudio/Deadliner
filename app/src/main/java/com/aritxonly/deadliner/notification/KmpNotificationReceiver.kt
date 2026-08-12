package com.aritxonly.deadliner.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        if (intent.action == NotificationService.ACTION_DAILY) {
            KmpV6Runtime.scheduleDailySummary()
            showDailySummary(context)
            return
        }
        val uid = intent.getStringExtra(NotificationService.EXTRA_UID) ?: return
        val title = intent.getStringExtra(NotificationService.EXTRA_TITLE).orEmpty()
        when (intent.action) {
            NotificationService.ACTION_TASK -> showTask(context, uid, title)
            NotificationService.ACTION_HABIT -> showHabit(context, uid, title)
        }
    }

    private fun showTask(context: Context, uid: String, fallbackTitle: String) {
        val task = KmpV6Runtime.database.tasks.find(uid) ?: return
        if (task.isDeleted || task.state != TaskState.ACTIVE) return
        if (!canPostNotifications(context)) return
        val open = PendingIntent.getActivity(
            context,
            uid.hashCode(),
            DeadlineDetailActivity.newIntent(context, uid),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notify(context, uid, task.title.ifBlank { fallbackTitle }, context.getString(R.string.remaining_due_soon), open)
    }

    private fun showHabit(context: Context, uid: String, fallbackTitle: String) {
        val habit = KmpV6Runtime.database.habits.find(uid) ?: return
        if (habit.isDeleted || habit.status != HabitStatus.ACTIVE || habit.reminder?.isEnabled != true) return
        KmpV6Runtime.onHabitNotificationDelivered(habit)
        if (!canPostNotifications(context)) return
        val open = PendingIntent.getActivity(
            context,
            uid.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notify(context, uid, habit.name.ifBlank { fallbackTitle }, context.getString(R.string.habit_notification), open)
    }

    private fun showDailySummary(context: Context) {
        if (!canPostNotifications(context)) return
        val now = LocalDateTime.now()
        val actionable = KmpV6Runtime.database.tasks.list().filter {
            !it.isDeleted && it.state == TaskState.ACTIVE && it.dueAt != null
        }
        val dueTimes = actionable.mapNotNull { task ->
            task.dueAt?.let { raw ->
                runCatching { LocalDateTime.parse(raw) }.getOrNull()
                    ?: runCatching { java.time.OffsetDateTime.parse(raw).toLocalDateTime() }.getOrNull()
            }
        }
        val overdue = dueTimes.count { it.isBefore(now) }
        val dueToday = dueTimes.count { !it.isBefore(now) && it.toLocalDate() == now.toLocalDate() }
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
        context.getSystemService(NotificationManager::class.java).notify(uid.hashCode(), notification)
    }

    private companion object {
        const val DAILY_NOTIFICATION_ID = 114514
    }
}
