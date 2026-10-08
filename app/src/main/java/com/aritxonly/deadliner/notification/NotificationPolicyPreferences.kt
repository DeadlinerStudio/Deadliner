package com.aritxonly.deadliner.notification

import android.content.Context
import com.deadliner.platform.HabitNotificationMode
import com.deadliner.platform.NotificationPolicy

internal object NotificationPolicyPreferences {
    private fun prefs(context: Context) = context.getSharedPreferences("notification_policy_v2", Context.MODE_PRIVATE)
    fun read(context: Context): NotificationPolicy {
        val prefs = prefs(context)
        // The former task switch is the migration default; preserve an explicit opt-out.
        val enabled = com.aritxonly.deadliner.localutils.GlobalUtils.deadlineNotification
        return NotificationPolicy(prefs.getBoolean("task_realtime", enabled),
            prefs.getBoolean("task_nearby", enabled), prefs.getBoolean("task_due", enabled),
            runCatching { HabitNotificationMode.valueOf(prefs.getString("habit_mode", "DUE")!!) }.getOrDefault(HabitNotificationMode.DUE))
    }
    fun save(context: Context, policy: NotificationPolicy) {
        prefs(context).edit().putBoolean("task_realtime", policy.taskRealtime)
            .putBoolean("task_nearby", policy.taskNearby).putBoolean("task_due", policy.taskDue)
            .putString("habit_mode", policy.habitMode.name).apply()
    }
}
