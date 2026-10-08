package com.aritxonly.deadliner.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.FocusTimerActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.platform.FocusAlarmPort
import com.deadliner.platform.FocusAlarmResult
import java.time.Instant

internal const val ACTION_FOCUS_COUNTDOWN = "com.aritxonly.deadliner.action.FOCUS_COUNTDOWN"
internal const val CHANNEL_FOCUS_ID = "deadliner_focus_timer"

/** Android exact-alarm adapter for the shared countdown engine. */
internal class AndroidFocusAlarmPort(context: Context) : FocusAlarmPort {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun scheduleCountdown(
        sessionUid: String,
        fireAt: String,
        taskTitle: String,
    ): FocusAlarmResult = runCatching {
        val triggerAtMillis = Instant.parse(fireAt).toEpochMilli()
        val pendingIntent = countdownPendingIntent(appContext, sessionUid, taskTitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            FocusAlarmResult(
                succeeded = false,
                failureMessage = "Exact alarm permission is unavailable; a fallback alarm was scheduled",
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            FocusAlarmResult(succeeded = true)
        }
    }.getOrElse { error ->
        FocusAlarmResult(succeeded = false, failureMessage = error.message)
    }

    override fun cancelCountdown(sessionUid: String): FocusAlarmResult = runCatching {
        alarmManager.cancel(countdownPendingIntent(appContext, sessionUid, taskTitle = ""))
        FocusAlarmResult(succeeded = true)
    }.getOrElse { error ->
        FocusAlarmResult(succeeded = false, failureMessage = error.message)
    }
}

class FocusCountdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FOCUS_COUNTDOWN) return
        val sessionUid = intent.getStringExtra(EXTRA_SESSION_UID) ?: return
        val taskUid = intent.getStringExtra(EXTRA_TASK_UID).orEmpty()
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()

        KmpV6Runtime.focusTimerViewModel.initialize()
        if (!canPostNotifications(context)) return

        createFocusChannel(context)
        val openIntent = FocusTimerActivity.newIntent(context, taskUid, taskTitle)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            sessionUid.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS_ID)
            .setSmallIcon(R.drawable.ic_focus_notification)
            .setContentTitle(taskTitle.ifBlank { context.getString(R.string.focus_timer_title) })
            .setContentText(context.getString(R.string.focus_countdown_finished))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(sessionUid.hashCode(), notification)
    }

    private fun createFocusChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_FOCUS_ID,
                context.getString(R.string.focus_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.focus_notification_channel_description)
                enableVibration(true)
            },
        )
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}

private fun countdownPendingIntent(
    context: Context,
    sessionUid: String,
    taskTitle: String,
): PendingIntent {
    val intent = Intent(context, FocusCountdownReceiver::class.java)
        .setAction(ACTION_FOCUS_COUNTDOWN)
        .putExtra(EXTRA_SESSION_UID, sessionUid)
        .putExtra(EXTRA_TASK_UID, "")
        .putExtra(EXTRA_TASK_TITLE, taskTitle)
    return PendingIntent.getBroadcast(
        context,
        sessionUid.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private const val EXTRA_SESSION_UID = "focus_session_uid"
private const val EXTRA_TASK_UID = "focus_task_uid"
private const val EXTRA_TASK_TITLE = "focus_task_title"
