package com.aritxonly.deadliner.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.deadliner.db.repository.TaskActionResult
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskAction
import com.deadliner.domain.task.TaskState
import java.util.UUID

/** Explicit OEM action URIs carry a private capability, scoped to the displayed task version. */
internal object TaskNotificationActions {
    private const val TOKEN = "capability"
    private const val UID = "task_uid"
    private const val VERSION = "task_version"
    private const val ACTION = "task_action"
    private const val PREFIX = "com.aritxonly.deadliner.notification.TASK_"

    private fun token(context: Context): String = synchronized(this) {
        val prefs = context.getSharedPreferences("notification_actions", Context.MODE_PRIVATE)
        prefs.getString(TOKEN, null) ?: UUID.randomUUID().toString().also {
            check(prefs.edit().putString(TOKEN, it).commit())
        }
    }

    fun intent(context: Context, task: Task, action: TaskAction): Intent =
        Intent(context, NotificationTaskActionActivity::class.java)
            .setAction(PREFIX + action.name)
            .setData(Uri.Builder().scheme("deadliner-action").authority("task")
                .appendPath(task.uid).appendPath(action.name).appendQueryParameter("version", task.updatedAt).build())
            .putExtra(UID, task.uid).putExtra(VERSION, task.updatedAt)
            .putExtra(ACTION, action.name).putExtra(TOKEN, token(context))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

    fun pendingIntent(context: Context, task: Task, action: TaskAction): PendingIntent =
        PendingIntent.getActivity(context, 0, intent(context, task, action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun perform(context: Context, intent: Intent, find: (String) -> Task?,
                apply: (String, TaskAction) -> TaskActionResult): TaskActionResult? {
        val suppliedToken = intent.getStringExtra(TOKEN)
        val expectedToken = context.getSharedPreferences("notification_actions", Context.MODE_PRIVATE).getString(TOKEN, null)
        val action = runCatching { TaskAction.valueOf(intent.getStringExtra(ACTION).orEmpty()) }.getOrNull()
        val uid = intent.getStringExtra(UID)
        val task = if (suppliedToken != null && suppliedToken == expectedToken && uid != null) find(uid) else null
        val reason = when {
            suppliedToken == null || suppliedToken != expectedToken -> "invalid_capability"
            action !in setOf(TaskAction.MARK_GIVE_UP, TaskAction.MARK_COMPLETE) || intent.action != PREFIX + action?.name -> "invalid_action"
            task == null -> "not_found"
            task.isDeleted || task.state != TaskState.ACTIVE -> "inactive"
            task.updatedAt != intent.getStringExtra(VERSION) -> "stale_version"
            else -> null
        }
        if (reason != null) {
            NotificationDiagnostics.event("task_action.rejected", "uid" to uid, "reason" to reason)
            return null
        }
        return apply(requireNotNull(uid), requireNotNull(action)).also {
            NotificationDiagnostics.event("task_action.result", "uid" to uid, "action" to action, "outcome" to it.outcome)
        }
    }
}
