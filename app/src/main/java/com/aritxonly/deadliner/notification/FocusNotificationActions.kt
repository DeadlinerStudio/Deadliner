package com.aritxonly.deadliner.notification

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.focus.FocusTimerCommand
import com.deadliner.domain.focus.FocusTimerOutcome
import com.deadliner.domain.focus.FocusTimerSnapshot
import com.deadliner.domain.task.TaskState
import com.deadliner.platform.NotificationKind
import com.deadliner.platform.NotificationPhase
import com.deadliner.platform.TaskNotificationResult
import com.deadliner.service.focus.FocusTimerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.UUID

internal enum class FocusNotificationAction { PAUSE, RESUME, FINISH, FINISH_AND_COMPLETE }

internal object FocusNotificationActions {
    private fun prefs(context: Context) = context.getSharedPreferences("focus_notification_actions", Context.MODE_PRIVATE)
    @Synchronized private fun capability(context: Context): String = prefs(context).getString("token", null)
        ?: UUID.randomUUID().toString().also { check(prefs(context).edit().putString("token", it).commit()) }

    fun available(timer: FocusTimerSnapshot): List<FocusNotificationAction> = buildList {
        if (timer.canPause) add(FocusNotificationAction.PAUSE)
        else if (timer.canResume) add(FocusNotificationAction.RESUME)
        if (timer.canFinish) add(FocusNotificationAction.FINISH)
        if (timer.canFinishAndComplete) add(FocusNotificationAction.FINISH_AND_COMPLETE)
    }
    fun label(action: FocusNotificationAction): Int = when (action) {
        FocusNotificationAction.PAUSE -> R.string.focus_pause
        FocusNotificationAction.RESUME -> R.string.focus_resume
        FocusNotificationAction.FINISH -> R.string.focus_finish_short
        FocusNotificationAction.FINISH_AND_COMPLETE -> R.string.focus_finish_and_complete
    }
    fun icon(action: FocusNotificationAction): Int = when (action) {
        FocusNotificationAction.PAUSE -> R.drawable.ic_focus_pause
        FocusNotificationAction.RESUME -> R.drawable.ic_focus_play
        FocusNotificationAction.FINISH -> R.drawable.ic_focus_stop
        FocusNotificationAction.FINISH_AND_COMPLETE -> R.drawable.ic_focus_complete
    }
    fun command(action: FocusNotificationAction): FocusTimerCommand = when (action) {
        FocusNotificationAction.PAUSE -> FocusTimerCommand.Pause
        FocusNotificationAction.RESUME -> FocusTimerCommand.Resume
        FocusNotificationAction.FINISH -> FocusTimerCommand.Finish
        FocusNotificationAction.FINISH_AND_COMPLETE -> FocusTimerCommand.FinishAndCompleteTask
    }
    fun intent(context: Context, info: FocusTimerResult, action: FocusNotificationAction): Intent {
        val timer = requireNotNull(info.snapshot)
        val version = requireNotNull(info.activeTimer).updatedAt
        return Intent(context, FocusNotificationActionActivity::class.java).setAction(action.name)
            .setData(Uri.Builder().scheme("deadliner-focus-action").authority(context.packageName)
                .appendPath(timer.sessionUid).appendPath(action.name).appendQueryParameter("version", version).build())
            .putExtra("session", timer.sessionUid).putExtra("version", version).putExtra("capability", capability(context))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
    fun pending(context: Context, info: FocusTimerResult, action: FocusNotificationAction): PendingIntent =
        PendingIntent.getActivity(context, 0, intent(context, info, action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun validate(context: Context, intent: Intent, info: FocusTimerResult): FocusNotificationAction? {
        val token = intent.getStringExtra("capability") ?: return null
        if (token != prefs(context).getString("token", null)) return null
        val timer = info.snapshot ?: return null
        if (timer.sessionUid != intent.getStringExtra("session") || info.activeTimer?.updatedAt != intent.getStringExtra("version")) return null
        return runCatching { FocusNotificationAction.valueOf(intent.action.orEmpty()) }.getOrNull()?.takeIf { it in available(timer) }
    }
}

class FocusNotificationActionActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope.launch {
            try {
                KmpV6Runtime.initialize(this@FocusNotificationActionActivity)
                val info = KmpV6Runtime.database.focusTimerNotificationInfo()
                val action = FocusNotificationActions.validate(this@FocusNotificationActionActivity, intent, info)
                if (action == null) {
                    NotificationDiagnostics.event("focus.notification.action.rejected", "reason" to "stale_session_version_or_capability")
                    return@launch
                }
                val timer = requireNotNull(info.snapshot)
                val hadRealtime = KmpV6Runtime.notificationMetadata().realtime.any {
                    it.ownerUid == timer.taskUid && it.kind == NotificationKind.TASK_REALTIME
                }
                val state = KmpV6Runtime.focusTimerViewModel.executeNow(FocusNotificationActions.command(action))
                if (state.lastOutcome == FocusTimerOutcome.APPLIED && action == FocusNotificationAction.FINISH_AND_COMPLETE && hadRealtime) {
                    KmpV6Runtime.database.tasks.find(timer.taskUid)?.takeIf { it.state == TaskState.COMPLETED }?.let {
                        NotificationInteractionStore.result(this@FocusNotificationActionActivity,
                            TaskNotificationResult(it.uid, it.updatedAt, NotificationPhase.COMPLETED, Clock.System.now().toString()))
                    }
                }
                KmpV6Runtime.triggerNotificationInfoUpdate()
                if (state.timer == null) FocusTimerForegroundService.stop(this@FocusNotificationActionActivity)
                else FocusTimerForegroundService.start(this@FocusNotificationActionActivity)
                NotificationDiagnostics.event("focus.notification.action", "session" to timer.sessionUid,
                    "action" to action, "outcome" to state.lastOutcome, "error" to state.error)
            } catch (error: Exception) { NotificationDiagnostics.failure("focus.notification.action.failed", error) }
            finally { finish() }
        }
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
