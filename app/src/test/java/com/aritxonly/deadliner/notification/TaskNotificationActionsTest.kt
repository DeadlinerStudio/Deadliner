package com.aritxonly.deadliner.notification

import android.app.Application
import android.app.AlarmManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import com.deadliner.db.repository.TaskActionResult
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskAction
import com.deadliner.domain.task.TaskActionOutcome
import com.deadliner.domain.task.TaskState
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskNotificationActionsTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val now = Instant.now().toString()
    private fun task(uid: String = "action-task") = Task(uid, "Submit report",
        dueAt = Instant.now().plusSeconds(7200).toString(), createdAt = now, updatedAt = now)

    @Test fun serializedButtonsCompleteOrAbandonThroughCoreAndCancelAlarms() {
        GlobalUtils.init(context)
        GlobalUtils.deadlineNotification = true
        GlobalUtils.liveUpdatesInAdvance = 30
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        try {
            db.configureNotificationUpdates(AndroidNotificationGateway(context))
            listOf(TaskAction.MARK_COMPLETE to TaskState.COMPLETED, TaskAction.MARK_GIVE_UP to TaskState.ABANDONED)
                .forEachIndexed { index, (action, state) ->
                    val task = task("task-$index")
                    db.tasks.create(task)
                    val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
                    assertTrue(alarms.scheduledAlarms.isNotEmpty())
                    val intent = Intent.parseUri(TaskNotificationActions.intent(context, task, action)
                        .toUri(Intent.URI_INTENT_SCHEME), Intent.URI_INTENT_SCHEME)
                    val result = TaskNotificationActions.perform(context, intent, db.tasks::find) { uid, a ->
                        db.tasks.applyAction(uid, a, Instant.now().toString())
                    }
                    assertEquals(TaskActionOutcome.APPLIED, result!!.outcome)
                    assertEquals(state, db.tasks.find(task.uid)!!.state)
                    assertTrue(alarms.scheduledAlarms.isEmpty())
                    assertNull(TaskNotificationActions.perform(context, intent, db.tasks::find) { _, _ ->
                        fail("Repeated click must not restore ACTIVE"); error("unreachable")
                    })
                }
        } finally { db.close() }
    }

    @Test fun forgedAndStaleActionsCannotMutateTask() {
        val task = task()
        val valid = TaskNotificationActions.intent(context, task, TaskAction.MARK_COMPLETE)
        val forged = Intent(valid).removeCapability()
        val apply: (String, TaskAction) -> TaskActionResult = { _, _ -> fail("Rejected action"); error("unreachable") }
        assertNull(TaskNotificationActions.perform(context, forged, { task }, apply))
        assertNull(TaskNotificationActions.perform(context, valid, { task.copy(updatedAt = "new-version") }, apply))
        assertNull(TaskNotificationActions.perform(context, valid, { task.copy(isDeleted = true) }, apply))
        assertNull(TaskNotificationActions.perform(context, Intent(valid).putExtra("task_action", "RESTORE_ACTIVE"), { task }, apply))
    }

    @Test fun focusedTaskReturnsResolutionAndActionsHaveDistinctIdentities() {
        val task = task()
        val complete = TaskNotificationActions.intent(context, task, TaskAction.MARK_COMPLETE)
        val abandon = TaskNotificationActions.intent(context, task, TaskAction.MARK_GIVE_UP)
        assertFalse(complete.filterEquals(abandon))
        assertFalse(complete.filterEquals(TaskNotificationActions.intent(context, task("another-task"), TaskAction.MARK_COMPLETE)))
        assertFalse(complete.filterEquals(TaskNotificationActions.intent(context, task.copy(updatedAt = "new"), TaskAction.MARK_COMPLETE)))
        val result = TaskNotificationActions.perform(context, complete, { task }) { _, _ ->
            TaskActionResult(task, TaskActionOutcome.TIMER_REQUIRES_RESOLUTION)
        }
        assertEquals(TaskActionOutcome.TIMER_REQUIRES_RESOLUTION, result!!.outcome)
        assertTrue(context.packageManager.getActivityInfo(complete.component!!, 0).exported)
    }

    private fun Intent.removeCapability(): Intent = apply { removeExtra("capability") }
}
