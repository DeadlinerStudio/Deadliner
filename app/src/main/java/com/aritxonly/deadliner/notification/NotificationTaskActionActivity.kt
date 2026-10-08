package com.aritxonly.deadliner.notification

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.aritxonly.deadliner.FocusTimerActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.task.TaskActionOutcome
import com.deadliner.domain.task.TaskState

/** User-clicked Activity actions also work with OEM serialized Intent URIs. */
class NotificationTaskActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            KmpV6Runtime.initialize(this)
            val result = TaskNotificationActions.perform(this, intent,
                KmpV6Runtime.database.tasks::find, KmpV6Runtime::applyTaskAction)
            when (result?.outcome) {
                TaskActionOutcome.APPLIED -> {
                    result.task?.let { UpcomingLiveUpdateService.removeRunning(it.uid) }
                    Toast.makeText(this, if (result.task?.state == TaskState.COMPLETED)
                        R.string.toast_finished else R.string.toast_give_up, Toast.LENGTH_SHORT).show()
                }
                TaskActionOutcome.TIMER_REQUIRES_RESOLUTION -> {
                    Toast.makeText(this, R.string.focus_resolve_before_task_change, Toast.LENGTH_LONG).show()
                    result.task?.let { startActivity(FocusTimerActivity.newIntent(this, it.uid, it.title)) }
                }
                else -> Unit
            }
        } catch (error: Exception) {
            NotificationDiagnostics.failure("task_action.failed", error)
        } finally {
            finish()
        }
    }
}
