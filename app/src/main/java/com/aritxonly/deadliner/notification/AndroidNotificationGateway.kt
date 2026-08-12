package com.aritxonly.deadliner.notification

import android.content.Context
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
    private val service = NotificationService(context.applicationContext)

    override fun setTaskNotification(config: TaskNotificationConfig) {
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
        }
    }

    override fun cancelTaskNotification(taskUid: String) = service.cancelTaskNotification(taskUid)
    override fun cancelAllTaskNotifications() = service.cancelAllTaskNotifications()
    override fun setHabitNotification(config: HabitNotificationConfig) = service.setHabitNotification(config)
    override fun cancelHabitNotification(habitUid: String) = service.cancelHabitNotification(habitUid)
    override fun cancelAllHabitNotifications() = service.cancelAllHabitNotifications()
    override fun setDailySummaryNotification(config: DailySummaryConfig) = service.setDailySummaryNotification(config)
    override fun cancelDailySummaryNotification() = service.cancelDailySummaryNotification()
    override fun setUpcomingLiveUpdate(config: UpcomingLiveUpdateConfig) = service.setUpcomingLiveUpdate(config)
    override fun cancelUpcomingLiveUpdate(taskUid: String) = service.cancelUpcomingLiveUpdate(taskUid)
    override fun cancelAll() = service.cancelAll()
    override fun refreshAll(tasks: List<TaskNotificationInfo>, habits: List<HabitNotificationInfo>) =
        service.refreshAll(tasks, habits)
}
