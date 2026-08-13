package com.aritxonly.deadliner.notification

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskState
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime

/** Android renderer for the live-update schedule owned by KMP NotificationService. */
class UpcomingLiveUpdateService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var taskUid: String? = null
    private var intervalMillis = 30_000L

    private val tick = object : Runnable {
        override fun run() {
            val uid = taskUid ?: return stopSelf()
            val task = KmpV6Runtime.database.tasks.find(uid)
            if (task == null || task.isDeleted || task.state != TaskState.ACTIVE) {
                stopLiveUpdate(uid)
                return
            }
            val remaining = remainingSeconds(task) ?: return stopLiveUpdate(uid)
            if (remaining <= 0L) {
                publish(task, remaining, ongoing = false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
                return
            }
            publish(task, remaining, ongoing = true)
            handler.postDelayed(this, intervalMillis)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uid = intent?.getStringExtra(EXTRA_TASK_UID) ?: return START_NOT_STICKY
        val task = KmpV6Runtime.database.tasks.find(uid) ?: return START_NOT_STICKY
        if (task.isDeleted || task.state != TaskState.ACTIVE) return START_NOT_STICKY
        val remaining = remainingSeconds(task) ?: return START_NOT_STICKY
        if (remaining <= 0L) return START_NOT_STICKY

        taskUid = uid
        intervalMillis = intent.getIntExtra(EXTRA_INTERVAL_SECONDS, 30).coerceAtLeast(1) * 1_000L
        startForeground(uid.hashCode(), buildNotification(task, remaining, ongoing = true))
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(tick, intervalMillis)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun publish(task: Task, remaining: Long, ongoing: Boolean) {
        NotificationManagerCompat.from(this).notify(
            task.uid.hashCode(),
            buildNotification(task, remaining, ongoing),
        )
    }

    private fun buildNotification(task: Task, remaining: Long, ongoing: Boolean): android.app.Notification {
        val minutes = (remaining.coerceAtLeast(0L) / 60L).toInt()
        val content = when {
            remaining <= 0L -> getString(R.string.notification_upcoming_task_overdue)
            remaining <= 60L -> getString(R.string.notification_upcoming_task_soon)
            else -> getString(R.string.notification_upcoming_task, minutes)
        }
        val open = PendingIntent.getActivity(
            this,
            task.uid.hashCode(),
            DeadlineDetailActivity.newIntent(this, task.uid),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_upcoming_task_title, task.title))
            .setContentText(content)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .build()
    }

    private fun remainingSeconds(task: Task): Long? {
        val due = task.dueAt?.let { raw ->
            runCatching { LocalDateTime.parse(raw) }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(raw).toLocalDateTime() }.getOrNull()
        } ?: return null
        return Duration.between(LocalDateTime.now(), due).seconds
    }

    private fun stopLiveUpdate(uid: String) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(uid.hashCode())
        stopSelf()
    }

    companion object {
        private const val EXTRA_TASK_UID = "task_uid"
        private const val EXTRA_INTERVAL_SECONDS = "interval_seconds"

        fun start(context: Context, taskUid: String, updateIntervalSeconds: Int) {
            val intent = Intent(context, UpcomingLiveUpdateService::class.java)
                .putExtra(EXTRA_TASK_UID, taskUid)
                .putExtra(EXTRA_INTERVAL_SECONDS, updateIntervalSeconds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
