package com.aritxonly.deadliner.notification

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.aritxonly.deadliner.FocusTimerActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.platform.NotificationMetadataEntry
import com.deadliner.platform.NotificationAction
import com.deadliner.domain.task.TaskAction
import com.deadliner.domain.task.TaskActionOutcome
import com.deadliner.domain.habit.HabitActionOutcome
import kotlinx.datetime.Clock
import java.util.UUID

internal object NotificationMetadataActions {
    private fun prefs(context: Context) = context.getSharedPreferences("notification_metadata_actions", Context.MODE_PRIVATE)
    @Synchronized private fun capability(context: Context): String = prefs(context).getString("token", null)
        ?: UUID.randomUUID().toString().also { check(prefs(context).edit().putString("token", it).commit()) }

    fun intent(context: Context, entry: NotificationMetadataEntry, action: NotificationAction): Intent =
        Intent(context, NotificationMetadataActionActivity::class.java).setAction(action.name)
            .setData(Uri.Builder().scheme("deadliner-live-action").authority(context.packageName)
                .appendPath(entry.key).appendPath(action.name).appendQueryParameter("version", entry.version).build())
            .putExtra("key", entry.key).putExtra("version", entry.version).putExtra("capability", capability(context))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

    fun pending(context: Context, entry: NotificationMetadataEntry, action: NotificationAction): PendingIntent =
        PendingIntent.getActivity(context, 0, intent(context, entry, action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun taskAction(action: NotificationAction): TaskAction = when (action) {
        NotificationAction.COMPLETE -> TaskAction.MARK_COMPLETE
        NotificationAction.GIVE_UP -> TaskAction.MARK_GIVE_UP
        NotificationAction.RESTORE_ACTIVE -> TaskAction.RESTORE_ACTIVE
        else -> error("Not a task notification action: $action")
    }

    fun validate(context: Context, intent: Intent, entries: List<NotificationMetadataEntry>): Pair<NotificationMetadataEntry, NotificationAction>? {
        val token = intent.getStringExtra("capability") ?: return null
        if (token != prefs(context).getString("token", null)) return null
        val action = runCatching { NotificationAction.valueOf(intent.action.orEmpty()) }.getOrNull() ?: return null
        val entry = entries.firstOrNull { it.key == intent.getStringExtra("key") && it.version == intent.getStringExtra("version") && action in it.actions } ?: return null
        return entry to action
    }
}

class NotificationMetadataActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            KmpV6Runtime.initialize(this)
            val validated = NotificationMetadataActions.validate(this, intent, KmpV6Runtime.notificationMetadata().realtime)
            if (validated == null) {
                NotificationDiagnostics.event("metadata.action.rejected", "reason" to "stale_or_invalid_capability")
                return
            }
            val (entry, action) = validated
            val now = Clock.System.now().toString()
            when (action) {
                NotificationAction.OPEN_FOCUS -> {
                    entry.focusSessionUid?.let { startActivity(FocusTimerActivity.newResultIntent(this, it)) }
                }
                NotificationAction.OPEN_TASK -> {
                    startActivity(com.aritxonly.deadliner.DeadlineDetailActivity.newIntent(this, entry.ownerUid))
                    NotificationDiagnostics.event("metadata.action.open_task", "key" to entry.key, "task" to entry.ownerUid)
                }
                NotificationAction.COMPLETE, NotificationAction.GIVE_UP, NotificationAction.RESTORE_ACTIVE -> {
                    val result = KmpV6Runtime.applyTaskAction(entry.ownerUid, NotificationMetadataActions.taskAction(action))
                    if (result.outcome == TaskActionOutcome.TIMER_REQUIRES_RESOLUTION) {
                        Toast.makeText(this, R.string.focus_resolve_before_task_change, Toast.LENGTH_LONG).show()
                        result.task?.let { startActivity(FocusTimerActivity.newIntent(this, it.uid, it.title)) }
                    }
                    NotificationDiagnostics.event("metadata.action.task", "key" to entry.key, "action" to action, "outcome" to result.outcome)
                }
                NotificationAction.CHECK_IN -> {
                    val result = KmpV6Runtime.database.habits.addRecordAmount(entry.ownerUid, requireNotNull(entry.occurrenceOn), 1, now)
                    NotificationDiagnostics.event("metadata.action.habit", "key" to entry.key, "outcome" to result.outcome)
                    KmpV6Runtime.triggerNotificationInfoUpdate()
                }
                NotificationAction.IGNORE -> {
                    NotificationInteractionStore.dismiss(this, entry.key)
                    NotificationDiagnostics.event("metadata.action.ignored", "key" to entry.key)
                    KmpV6Runtime.triggerNotificationInfoUpdate()
                }
            }
        } catch (error: Exception) { NotificationDiagnostics.failure("metadata.action.failed", error) }
        finally { finish() }
    }
}
