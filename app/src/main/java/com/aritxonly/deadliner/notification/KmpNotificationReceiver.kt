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
import com.deadliner.platform.NotificationService

/** Renders notifications for schedules created by the KMP NotificationOrchestrator. */
class KmpNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

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
        val open = PendingIntent.getActivity(
            context,
            uid.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notify(context, uid, habit.name.ifBlank { fallbackTitle }, context.getString(R.string.habit_notification), open)
        KmpV6Runtime.onHabitNotificationDelivered(habit)
    }

    private fun notify(context: Context, uid: String, title: String, content: String, open: PendingIntent) {
        NotificationUtil.createNotificationChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(uid.hashCode(), notification)
    }
}
