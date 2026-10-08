package com.aritxonly.deadliner.notification

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskState
import com.deadliner.domain.task.TaskAction
import java.time.Duration
import java.time.Instant

/** One foreground lifecycle, independent live cards for every task inside its current window. */
class UpcomingLiveUpdateService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val active = linkedSetOf<String>()
    private val displayed = mutableSetOf<String>()
    private var foregroundStarted = false
    private var lastIslandPublication: List<TaskIslandPresentation.Snapshot>? = null
    private var intervalMillis = 30_000L
    private val tick = Runnable {
        try { refresh(); scheduleTick() }
        catch (error: Exception) { NotificationDiagnostics.failure("live.tick_failed", error); stopSelf() }
    }

    override fun onCreate() {
        super.onCreate()
        running = this
        NotificationDiagnostics.event("live.service.created")
        NotificationUtil.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationDiagnostics.event("live.service.start", "uid" to intent?.getStringExtra(EXTRA_TASK_UID), "startId" to startId)
        NotificationDiagnostics.snapshot(this)
        try {
            // Promote immediately even if a task was removed between alarm delivery and service start.
            if (!foregroundStarted) {
                startForeground(FOREGROUND_ID, NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_focus_notification)
                .setContentTitle(getString(R.string.app_name))
                .setOnlyAlertOnce(true).setOngoing(true).build())
                foregroundStarted = true
            }
            intent?.getStringExtra(EXTRA_TASK_UID)?.let(active::add)
            intervalMillis = (intent?.getIntExtra(EXTRA_INTERVAL_SECONDS, 30) ?: 30).coerceAtLeast(1) * 1_000L
            refresh()
            scheduleTick()
        } catch (error: Exception) {
            NotificationDiagnostics.failure("live.service.failed", error, "uid" to intent?.getStringExtra(EXTRA_TASK_UID))
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun scheduleTick() {
        handler.removeCallbacks(tick)
        if (active.isNotEmpty()) handler.postDelayed(tick, intervalMillis)
    }

    private fun liveTasks(now: Instant): List<Task> {
        val enabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        return active.mapNotNull { uid ->
            val task = KmpV6Runtime.database.tasks.find(uid)
            val reason = when {
                !enabled -> "app_notifications_disabled"
                !GlobalUtils.deadlineNotification -> "reminders_disabled"
                task == null -> "not_found"
                task.isDeleted || task.state != TaskState.ACTIVE -> "inactive"
                !NotificationTiming.inLiveWindow(task.dueAt, GlobalUtils.liveUpdatesInAdvance, now) -> "outside_window_or_invalid_due"
                else -> null
            }
            if (reason != null) NotificationDiagnostics.event("live.filtered", "uid" to uid, "reason" to reason, "dueAt" to task?.dueAt)
            task?.takeIf { reason == null }
        }.sortedBy { NotificationTiming.dueInstant(it.dueAt) }
    }

    private fun refresh() {
        val now = Instant.now()
        val tasks = liveTasks(now)
        NotificationDiagnostics.event("live.refresh", "requestedCount" to active.size, "validCount" to tasks.size)
        val valid = tasks.map { it.uid }.toSet()
        active.retainAll(valid)
        val manager = NotificationManagerCompat.from(this)
        // The earliest task uses the service's fixed foreground slot; others use collision-free tags.
        val extraCards = tasks.drop(1).map { it.uid }.toSet()
        (displayed - extraCards).forEach { manager.cancel(liveTag(it), LIVE_CARD_ID) }
        displayed.clear()
        displayed.addAll(extraCards)
        if (tasks.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val usesIsland = LiveUpdateNotificationAdapter.usesIsland(this)
        if (usesIsland && lastIslandPublication == TaskIslandPresentation.snapshot(this, tasks, now)) return
        if (!usesIsland) lastIslandPublication = null
        IslandWhitelistBypass.publish(this, "deadlines", onPublication = { accepted ->
            // A fallback notification must not suppress the next authorization retry.
            if (!accepted) lastIslandPublication = null
        }) {
            if (running === this) {
                val currentTime = Instant.now()
                val publishedTasks = liveTasks(currentTime)
                publishedTasks.forEachIndexed { index, task ->
                    val seconds = Duration.between(currentTime, NotificationTiming.dueInstant(task.dueAt)).seconds
                    val notification = buildNotification(task, seconds)
                    val id = if (index == 0) FOREGROUND_ID else LIVE_CARD_ID
                    NotificationDiagnostics.built(this, notification, task.uid, id)
                    NotificationDiagnostics.event("live.publish", "uid" to task.uid, "remainingSeconds" to seconds,
                        "foreground" to (index == 0), "dispatch" to "notify_inside_window")
                    if (index == 0) manager.notify(FOREGROUND_ID, notification)
                    else manager.notify(liveTag(task.uid), LIVE_CARD_ID, notification)
                    NotificationDiagnostics.observe(this, task.uid, id, if (index == 0) null else liveTag(task.uid))
                }
                if (LiveUpdateNotificationAdapter.usesIsland(this)) {
                    lastIslandPublication = TaskIslandPresentation.snapshot(this, publishedTasks, currentTime)
                }
            }
        }
    }

    private fun buildNotification(task: Task, remaining: Long): android.app.Notification {
        val timing = TaskIslandPresentation.deadline(this, task)
        val phase = if (remaining <= 0L) com.deadliner.platform.NotificationPhase.OVERDUE else com.deadliner.platform.NotificationPhase.UPCOMING
        val content = "${RealtimeNotificationRenderer.status(this, phase)} · ${getString(R.string.notification_island_due, timing)}"
        val open = PendingIntent.getActivity(
            this, 0,
            DeadlineDetailActivity.newIntent(this, task.uid)
                .setData(Uri.Builder().scheme("deadliner").authority("task").appendPath(task.uid).build()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = getString(R.string.notification_upcoming_task_title, task.title)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_focus_notification)
            .setContentTitle(title)
            .setContentText(content).setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true).setOngoing(true)
            .setWhen(0L).setShowWhen(false).setUsesChronometer(false).setChronometerCountDown(false)
            .setColor(android.graphics.Color.parseColor(RealtimeNotificationRenderer.color(phase)))
            .addAction(R.drawable.msr_close_wght400, getString(R.string.task_swipe_give_up),
                TaskNotificationActions.pendingIntent(this, task, TaskAction.MARK_GIVE_UP))
            .addAction(R.drawable.msr_task_alt_wght400, getString(R.string.mark_complete),
                TaskNotificationActions.pendingIntent(this, task, TaskAction.MARK_COMPLETE))
        return LiveUpdateNotificationAdapter.apply(this, builder, title, content, timing, R.drawable.ic_upcoming_island, task = task).build()
    }

    override fun onDestroy() {
        NotificationDiagnostics.event("live.service.destroyed", "activeCount" to active.size)
        if (running === this) running = null
        handler.removeCallbacksAndMessages(null)
        displayed.forEach { NotificationManagerCompat.from(this).cancel(liveTag(it), LIVE_CARD_ID) }
        displayed.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val EXTRA_TASK_UID = "task_uid"
        private const val EXTRA_INTERVAL_SECONDS = "interval_seconds"
        private const val FOREGROUND_ID = 520_001
        private const val LIVE_CARD_ID = 1
        @Volatile private var running: UpcomingLiveUpdateService? = null
        private fun liveTag(uid: String) = "live:$uid"

        fun refreshRunning(uid: String) {
            running?.let { service -> service.handler.post { service.active.add(uid); service.refresh(); service.scheduleTick() } }
        }
        fun removeRunning(uid: String) {
            running?.let { service -> service.handler.post { service.active.remove(uid); service.refresh(); service.scheduleTick() } }
        }
        fun removeAllRunning() {
            running?.let { service -> service.handler.post { service.active.clear(); service.refresh(); service.scheduleTick() } }
        }
        fun start(context: Context, taskUid: String, updateIntervalSeconds: Int) {
            NotificationDiagnostics.event("live.start.requested", "uid" to taskUid, "intervalSeconds" to updateIntervalSeconds)
            val task = KmpV6Runtime.database.tasks.find(taskUid) ?: run {
                NotificationDiagnostics.event("live.start.skipped", "uid" to taskUid, "reason" to "not_found"); return
            }
            if (!GlobalUtils.deadlineNotification || task.isDeleted || task.state != TaskState.ACTIVE ||
                !NotificationTiming.inLiveWindow(task.dueAt, GlobalUtils.liveUpdatesInAdvance)) {
                NotificationDiagnostics.event("live.start.skipped", "uid" to taskUid, "state" to task.state,
                    "dueAt" to task.dueAt, "enabled" to GlobalUtils.deadlineNotification, "reason" to "inactive_or_outside_window")
                return
            }
            if (running != null) { refreshRunning(taskUid); return }
            val intent = Intent(context, UpcomingLiveUpdateService::class.java)
                .putExtra(EXTRA_TASK_UID, taskUid).putExtra(EXTRA_INTERVAL_SECONDS, updateIntervalSeconds)
            try {
                context.startForegroundService(intent)
            } catch (error: IllegalStateException) {
                NotificationDiagnostics.failure("live.start.failed", error, "uid" to taskUid)
            } catch (error: SecurityException) {
                NotificationDiagnostics.failure("live.start.failed", error, "uid" to taskUid)
            }
        }
    }
}
