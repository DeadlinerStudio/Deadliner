package com.aritxonly.deadliner.notification

import android.content.Context
import android.app.NotificationManager
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.deadliner.platform.DailySummaryConfig
import com.deadliner.platform.HabitNotificationConfig
import com.deadliner.platform.HabitNotificationInfo
import com.deadliner.platform.NotificationGateway
import com.deadliner.platform.NotificationService
import com.deadliner.platform.TaskNotificationConfig
import com.deadliner.platform.TaskNotificationInfo
import com.deadliner.platform.UpcomingLiveUpdateConfig

/** Applies Android-owned preferences at the platform notification boundary. */
internal class AndroidNotificationGateway(context: Context) : NotificationGateway {
    private val appContext = context.applicationContext
    private val metadataScheduler = AndroidNotificationMetadataScheduler(appContext)
    override fun applyNotificationMetadata(metadata: com.deadliner.platform.NotificationMetadata) = metadataScheduler.apply(metadata)
    private val service = NotificationService(context.applicationContext)

    override fun setTaskNotification(config: TaskNotificationConfig) {
        NotificationDiagnostics.event("gateway.task.upsert", "uid" to config.taskUid, "dueAt" to config.dueAt, "enabled" to GlobalUtils.deadlineNotification, "reminderHours" to GlobalUtils.deadlineNotificationBefore)
        appContext.getSystemService(NotificationManager::class.java).cancel("reminder:${config.taskUid}", config.taskUid.hashCode())
        if (GlobalUtils.deadlineNotification) {
            service.setTaskNotification(
                config.copy(
                    reminderMinutes = (GlobalUtils.deadlineNotificationBefore * 60L)
                        .coerceIn(0L, Int.MAX_VALUE.toLong())
                        .toInt(),
                ),
            )
        } else {
            service.cancelTaskNotification(config.taskUid)
            cancelUpcomingLiveUpdate(config.taskUid)
        }
    }

    override fun cancelTaskNotification(taskUid: String) {
        NotificationDiagnostics.event("gateway.task.cancel", "uid" to taskUid)
        service.cancelTaskNotification(taskUid)
        appContext.getSystemService(NotificationManager::class.java).cancel("reminder:$taskUid", taskUid.hashCode())
        cancelUpcomingLiveUpdate(taskUid)
    }
    override fun cancelAllTaskNotifications() {
        service.cancelAllTaskNotifications()
        // cancelAllTaskNotifications in the native bridge only covers reminder alarms.
        appContext.getSharedPreferences("kmp_notification_schedule", Context.MODE_PRIVATE)
            .getStringSet("live", emptySet()).orEmpty().toList().forEach(::cancelUpcomingLiveUpdate)
    }
    override fun setHabitNotification(config: HabitNotificationConfig) {
        NotificationDiagnostics.event("gateway.habit.upsert", "uid" to config.habitUid, "occurrences" to config.scheduledLocalDateTimes.size, "authoritative" to config.isScheduleAuthoritative)
        appContext.getSystemService(NotificationManager::class.java).cancel("reminder:${config.habitUid}", config.habitUid.hashCode())
        service.setHabitNotification(config)
    }
    override fun cancelHabitNotification(habitUid: String) {
        NotificationDiagnostics.event("gateway.habit.cancel", "uid" to habitUid)
        service.cancelHabitNotification(habitUid)
        appContext.getSystemService(NotificationManager::class.java).cancel("reminder:$habitUid", habitUid.hashCode())
    }
    override fun cancelAllHabitNotifications() = service.cancelAllHabitNotifications()
    override fun setDailySummaryNotification(config: DailySummaryConfig) {
        NotificationDiagnostics.event("gateway.daily.upsert", "time" to config.time, "enabled" to GlobalUtils.dailyStatsNotification)
        if (GlobalUtils.dailyStatsNotification) service.setDailySummaryNotification(config)
        else service.cancelDailySummaryNotification()
    }
    override fun cancelDailySummaryNotification() = service.cancelDailySummaryNotification()
    override fun setUpcomingLiveUpdate(config: UpcomingLiveUpdateConfig) {
        NotificationDiagnostics.event("gateway.live.upsert", "uid" to config.taskUid, "dueAt" to config.dueAt, "advanceMinutes" to GlobalUtils.liveUpdatesInAdvance, "enabled" to GlobalUtils.deadlineNotification)
        if (GlobalUtils.deadlineNotification) {
            service.setUpcomingLiveUpdate(config.copy(advanceMinutes = GlobalUtils.liveUpdatesInAdvance))
            if (NotificationTiming.inLiveWindow(config.dueAt, GlobalUtils.liveUpdatesInAdvance)) {
                UpcomingLiveUpdateService.start(appContext, config.taskUid, config.updateIntervalSeconds)
            } else UpcomingLiveUpdateService.removeRunning(config.taskUid)
        } else cancelUpcomingLiveUpdate(config.taskUid)
    }
    override fun cancelUpcomingLiveUpdate(taskUid: String) {
        NotificationDiagnostics.event("gateway.live.cancel", "uid" to taskUid)
        service.cancelUpcomingLiveUpdate(taskUid)
        UpcomingLiveUpdateService.removeRunning(taskUid)
    }
    override fun cancelAll() {
        NotificationDiagnostics.event("gateway.cancel_all")
        service.cancelAll()
        metadataScheduler.cancelAll()
        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager.activeNotifications.filter { it.tag?.startsWith("live:") == true || it.id == 520_001 }
            .forEach { manager.cancel(it.tag, it.id) }
        UpcomingLiveUpdateService.removeAllRunning()
    }
    override fun refreshAll(tasks: List<TaskNotificationInfo>, habits: List<HabitNotificationInfo>) {
        cancelAll()
        tasks.filter { it.state.equals("active", true) }.forEach { task ->
            task.dueAt?.let { due ->
                setTaskNotification(TaskNotificationConfig(task.uid, task.title, due))
                setUpcomingLiveUpdate(UpcomingLiveUpdateConfig(task.uid, task.title, due))
            }
        }
        habits.filter { it.status.equals("active", true) }.forEach { habit ->
            habit.alarmTime?.let { setHabitNotification(HabitNotificationConfig(habit.uid, habit.name, it)) }
        }
    }
}
