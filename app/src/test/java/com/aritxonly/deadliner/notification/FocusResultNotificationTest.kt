package com.aritxonly.deadliner.notification

import android.app.Application
import android.app.Notification
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.R
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import com.deadliner.domain.focus.*
import com.deadliner.domain.task.Task
import com.deadliner.platform.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class FocusResultNotificationTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val start = Instant.parse("2026-10-09T12:00:00Z")
    private val end = Instant.parse("2026-10-09T12:01:00Z")
    private fun createTask(db: DeadlinerDatabase, uid: String = "focus-result") = Task(uid, "Report",
        createdAt = start.toString(), updatedAt = start.toString()).also(db.tasks::create)

    @Test fun persistedFinishRendersStaticRewardAndTaskNavigationOnBothSurfaces() = runBlocking {
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        try {
            val task = createTask(db)
            val service = db.createFocusTimerService()
            service.execute(FocusTimerCommand.Start(task.uid, FocusTimerMode.STOPWATCH), start)
            service.execute(FocusTimerCommand.Finish, end)
            val entry = db.focusResultNotificationInfo(end)!!
            val note = RealtimeNotificationRenderer.build(context, entry)
            assertEquals(FocusRealtimeNotificationRenderer.CHANNEL_ID, note.channelId)
            assertEquals(R.drawable.ic_island_miuix_topdownloads, note.smallIcon.resId)
            assertEquals(0L, note.`when`)
            assertFalse(note.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
            assertTrue(note.extras.getString(Notification.EXTRA_TEXT)!!.contains("01:00"))
            assertEquals(1, note.actions.size)
            assertEquals(context.getString(R.string.notification_live_open_task), note.actions.single().title)
            val open = shadowOf(note.contentIntent).savedIntent
            assertEquals(DeadlineDetailActivity::class.java.name, open.component!!.className)
            assertEquals(task.uid, open.getStringExtra("task_uid"))
            val action = shadowOf(note.actions.single().actionIntent).savedIntent
            assertEquals(NotificationAction.OPEN_TASK, NotificationMetadataActions.validate(context, action, listOf(entry))!!.second)
            val extras = XiaomiIslandExtras.createRealtime(context, entry, "01:00")
            val params = JSONObject(extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
            val island = params.getJSONObject("param_island")
            assertEquals("#FFA8DFC0", island.getString("highlightColor"))
            assertEquals(context.getString(R.string.notification_island_encouragement), island.getJSONObject("bigIslandArea")
                .getJSONObject("imageTextInfoRight").getJSONObject("textInfo").getString("title"))
            assertFalse(params.toString().contains("timerInfo"))
            val button = params.getJSONArray("textButton").getJSONObject(0)
            val parsed = Intent.parseUri(button.getString("actionIntent"), Intent.URI_INTENT_SCHEME)
            assertEquals(entry.ownerUid, NotificationMetadataActions.validate(context, parsed, listOf(entry))!!.first.ownerUid)
            assertNull(db.focusResultNotificationInfo(Instant.parse("2026-10-09T12:04:00Z")))
            assertNull(NotificationMetadataActions.validate(context, action, emptyList()))
        } finally { db.close() }
    }

    @Test fun committedFinishPublishesSharedMetadataAndReconfigurationRestoresOriginalExpiry() = runBlocking {
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        var metadata: NotificationMetadata? = null
        val gateway = object : NotificationGateway by AndroidNotificationGateway(context) {
            override fun cancelAll() = Unit
            override fun applyNotificationMetadata(value: NotificationMetadata) { metadata = value }
        }
        try {
            val task = createTask(db)
            db.configureNotificationUpdates(gateway, { NotificationPolicy() })
            val service = db.createFocusTimerService()
            val finished = java.time.Instant.now()
            service.execute(FocusTimerCommand.Start(task.uid, FocusTimerMode.STOPWATCH), Instant.parse(finished.minusSeconds(60).toString()))
            service.execute(FocusTimerCommand.Finish, Instant.parse(finished.toString()))
            val entry = metadata!!.realtime.single()
            assertEquals(NotificationKind.FOCUS_RESULT, entry.kind)
            assertEquals(finished.plusSeconds(180).toString(), entry.endsAt)
            assertEquals(entry.endsAt, metadata!!.nextRefreshAt)
            db.configureNotificationUpdates(gateway, { NotificationPolicy() })
            assertEquals(entry, metadata!!.realtime.single())
            db.focusSessions.adjustDuration(entry.focusSessionUid!!, 30, finished.plusMillis(1).toString())
            val changed = metadata!!.realtime.single()
            assertEquals(30L, changed.focusDurationSeconds)
            assertNotEquals(entry.version, changed.version)
            assertEquals(entry.endsAt, changed.endsAt)
            db.focusSessions.delete(entry.focusSessionUid!!, finished.plusMillis(2).toString())
            assertTrue(metadata!!.realtime.isEmpty())
        } finally { db.close() }
    }

    @Test fun countdownFinishProducesRewardAndDiscardDoesNotSaveCompletion() = runBlocking {
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        try {
            val task = createTask(db)
            val service = db.createFocusTimerService()
            service.execute(FocusTimerCommand.Start(task.uid, FocusTimerMode.STOPWATCH), start)
            service.execute(FocusTimerCommand.Discard, end)
            assertNull(db.focusResultNotificationInfo(end))
            assertTrue(db.focusSessions.recent(10).isEmpty())
            service.execute(FocusTimerCommand.Start(task.uid, FocusTimerMode.COUNTDOWN, 60), start)
            service.execute(FocusTimerCommand.Reconcile, end)
            val result = db.focusResultNotificationInfo(end)!!
            assertEquals(60L, result.focusDurationSeconds)
            assertEquals(FocusCompletionKind.COUNTDOWN_ELAPSED, db.focusSessions.recent(10).single().completionKind)
            service.execute(FocusTimerCommand.Start(task.uid, FocusTimerMode.STOPWATCH), end)
            assertNull(db.focusResultNotificationInfo(end))
        } finally { db.close() }
    }

    @Test fun latestDeletedSessionDoesNotExposeAnEarlierReward() = runBlocking {
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        try {
            val task = createTask(db)
            val service = db.createFocusTimerService()
            service.execute(FocusTimerCommand.Start(task.uid, FocusTimerMode.STOPWATCH), start)
            service.execute(FocusTimerCommand.Finish, end)
            service.execute(FocusTimerCommand.Start(task.uid, FocusTimerMode.STOPWATCH), end)
            val later = Instant.parse("2026-10-09T12:02:00Z")
            service.execute(FocusTimerCommand.FinishAndCompleteTask, later)
            val latest = db.focusResultNotificationInfo(later)!!
            assertEquals(2, db.focusSessions.recent(10).size)
            db.focusSessions.delete(latest.focusSessionUid!!, later.toString())
            assertNull(db.focusResultNotificationInfo(later))
        } finally { db.close() }
    }

}
