package com.aritxonly.deadliner.notification

import android.content.Context
import com.deadliner.platform.NotificationInteractionState
import com.deadliner.platform.TaskNotificationResult
import com.google.gson.Gson

internal object NotificationInteractionStore {
    private val gson = Gson()
    private fun prefs(context: Context) = context.getSharedPreferences("notification_interactions", Context.MODE_PRIVATE)
    @Synchronized fun read(context: Context): NotificationInteractionState = runCatching {
        gson.fromJson(prefs(context).getString("state", null), NotificationInteractionState::class.java)
    }.getOrNull() ?: NotificationInteractionState()
    @Synchronized fun result(context: Context, result: TaskNotificationResult) {
        val old = read(context)
        save(context, old.copy(taskResults = (old.taskResults.filterNot { it.taskUid == result.taskUid } + result).takeLast(256)))
    }
    @Synchronized fun clearResult(context: Context, taskUid: String) {
        val old = read(context)
        save(context, old.copy(taskResults = old.taskResults.filterNot { it.taskUid == taskUid }))
    }
    @Synchronized fun dismiss(context: Context, key: String) {
        val old = read(context)
        save(context, old.copy(dismissedHabitOccurrences = (old.dismissedHabitOccurrences + key).toList().takeLast(2048).toSet()))
    }
    private fun save(context: Context, state: NotificationInteractionState) {
        check(prefs(context).edit().putString("state", gson.toJson(state)).commit())
    }
}
