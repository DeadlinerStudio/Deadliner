package com.aritxonly.deadliner.notification

import android.app.Application
import android.app.Notification
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import com.deadliner.domain.focus.*
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskState
import com.deadliner.platform.FocusAlarmPort
import com.deadliner.platform.FocusAlarmResult
import com.deadliner.service.focus.FocusTimerResult
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class FocusRealtimeNotificationTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val now = Instant.parse("2026-10-07T12:00:00Z")
    private val nowMillis = now.toEpochMilliseconds()
    private fun info(mode: FocusTimerMode = FocusTimerMode.STOPWATCH, state: FocusTimerState = FocusTimerState.RUNNING): FocusTimerResult {
        val active = ActiveFocusTimer("session", "task", "Report", mode, if (mode == FocusTimerMode.COUNTDOWN) 300 else null,
            state, "2026-10-07T11:59:00Z", if (state == FocusTimerState.RUNNING) "2026-10-07T11:59:30Z" else null,
            30, "2026-10-07T11:59:30Z")
        return FocusTimerResult(FocusTimerOutcome.NO_OP, activeTimer = active,
            snapshot = FocusTimerEngine.snapshot(active, now, "Report", true))
    }
    private fun parameters(info: FocusTimerResult) = JSONObject(FocusIslandTemplate.create(context, info, nowMillis)
        .getString("miui.focus.param")!!).getJSONObject("param_v2")

    @Test fun recordingTemplateUsesAnchoredTimerAndTwoSystemRecordingControls() {
        val info = info()
        val extras = FocusIslandTemplate.create(context, info, nowMillis)
        val params = JSONObject(extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
        assertFalse(params.has("baseInfo")); assertFalse(params.has("picInfo")); assertFalse(params.has("textButton"))
        val highlight = params.getJSONObject("highlightInfo")
        assertFalse(highlight.has("title"))
        assertFalse(highlight.has("content"))
        assertEquals(1, highlight.getInt("type"))
        assertEquals("#FFFFFFFF", highlight.getString("colorTitle"))
        assertEquals(TaskIslandPresentation.BLUE, highlight.getString("colorSubContent"))
        assertEquals(1, highlight.getJSONObject("timerInfo").getInt("timerType"))
        assertEquals(nowMillis - 60_000, highlight.getJSONObject("timerInfo").getLong("timerWhen"))
        val actions = params.getJSONArray("actions")
        assertEquals(2, actions.length())
        assertEquals("action_pause_record", actions.getJSONObject(0).getString("actionIcon"))
        assertEquals("action_record_end", actions.getJSONObject(1).getString("actionIcon"))
        repeat(actions.length()) { index ->
            val action = actions.getJSONObject(index)
            assertEquals(0, action.getInt("type")); assertEquals(1, action.getInt("actionIntentType"))
            assertFalse(action.getBoolean("clickWithCollapse"))
            assertEquals(action.getString("actionIcon"), action.getString("actionIconDark"))
            // No bitmap may mask the OEM preset; the plugin resolves its circular drawable.
            assertFalse(extras.getBundle("miui.focus.pics")!!.containsKey(action.getString("actionIcon")))
            assertFalse(action.has("actionBgColor"))
            val intent = Intent.parseUri(action.getString("actionIntent"), Intent.URI_INTENT_SCHEME)
            assertEquals(FocusNotificationActions.available(info.snapshot!!)[index], FocusNotificationActions.validate(context, intent, info))
        }
        assertFalse(params.getJSONObject("param_island").getJSONObject("bigIslandArea").getJSONObject("sameWidthDigitInfo").has("digit"))
        assertEquals(highlight.getJSONObject("timerInfo").toString(), params.getJSONObject("param_island")
            .getJSONObject("bigIslandArea").getJSONObject("sameWidthDigitInfo").getJSONObject("timerInfo").toString())
    }

    @Test fun circularControlIconHasPaddingAndCacheDoesNotReuseFullCanvasVariant() {
        val icon = com.aritxonly.deadliner.R.drawable.ic_focus_pause
        val padded = XiaomiIslandExtras.bitmapIcon(context, icon, 88, "#FF183C61", contentSize = 44)
        val full = XiaomiIslandExtras.bitmapIcon(context, icon, 88, "#FF183C61")
        assertNotSame(full, padded)
        assertSame(padded, XiaomiIslandExtras.bitmapIcon(context, icon, 88, "#FF183C61", contentSize = 44))
        val drawable = padded.loadDrawable(context)!!
        val bitmap = android.graphics.Bitmap.createBitmap(88, 88, android.graphics.Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 88, 88)
        drawable.draw(android.graphics.Canvas(bitmap))
        for (y in 0 until 88) for (x in 0 until 88) {
            if (x < 22 || x >= 66 || y < 22 || y >= 66) assertEquals(0, android.graphics.Color.alpha(bitmap.getPixel(x, y)))
        }
    }

    @Test fun countdownAndPauseUseCorrectTimerDirectionAndFrozenClock() {
        val running = info(FocusTimerMode.COUNTDOWN)
        val native = FocusRealtimeNotificationRenderer.build(context, running, nowMillis)
        assertEquals(nowMillis + 240_000, native.`when`)
        assertTrue(native.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(native.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(-1, parameters(running).getJSONObject("highlightInfo").getJSONObject("timerInfo").getInt("timerType"))
        val paused = info(FocusTimerMode.COUNTDOWN, FocusTimerState.PAUSED)
        val params = parameters(paused)
        assertEquals(2, params.getJSONArray("actions").length())
        assertEquals("action_restart_record", params.getJSONArray("actions").getJSONObject(0).getString("actionIcon"))
        assertFalse(params.getJSONObject("highlightInfo").has("timerInfo"))
        assertEquals("04:30", params.getJSONObject("highlightInfo").getString("title"))
        val pausedDigits = params.getJSONObject("param_island").getJSONObject("bigIslandArea").getJSONObject("sameWidthDigitInfo")
        assertFalse(pausedDigits.has("timerInfo"))
        assertEquals("04:30", pausedDigits.getString("digit"))
        val frozen = FocusRealtimeNotificationRenderer.build(context, paused, nowMillis)
        assertEquals(0L, frozen.`when`)
        assertFalse(frozen.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertFalse(frozen.extras.getBoolean(Notification.EXTRA_SHOW_WHEN))
        assertEquals(listOf(FocusNotificationAction.RESUME, FocusNotificationAction.FINISH, FocusNotificationAction.FINISH_AND_COMPLETE),
            FocusNotificationActions.available(paused.snapshot!!))
    }

    @Test fun standardNativeNotificationCarriesButtonsAndSessionNavigationWithoutCustomViews() {
        val info = info()
        val notification = FocusRealtimeNotificationRenderer.build(context, info, nowMillis)
        assertEquals(nowMillis - 60_000, notification.`when`)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertFalse(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(3, notification.actions.size)
        assertEquals(0, notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX))
        assertNull(notification.contentView); assertNull(notification.bigContentView)
        assertEquals("Report", notification.extras.getString(Notification.EXTRA_TITLE))
        val open = org.robolectric.Shadows.shadowOf(notification.contentIntent).savedIntent
        assertTrue(open.data.toString().endsWith("/session"))
    }

    @Test fun forgedWrongSessionWrongVersionAndUnavailableActionsAreRejected() {
        val info = info()
        val intent = FocusNotificationActions.intent(context, info, FocusNotificationAction.PAUSE)
        assertEquals(FocusNotificationAction.PAUSE, FocusNotificationActions.validate(context, intent, info))
        assertNull(FocusNotificationActions.validate(context, Intent(intent).putExtra("capability", "forged"), info))
        assertNull(FocusNotificationActions.validate(context, Intent(intent).putExtra("version", "old"), info))
        assertNull(FocusNotificationActions.validate(context, Intent(intent).putExtra("session", "other"), info))
        assertNull(FocusNotificationActions.validate(context, intent, info(state = FocusTimerState.PAUSED)))
        val emptyDuration = info.copy(snapshot = info.snapshot!!.copy(canFinish = false, canFinishAndComplete = false))
        assertEquals(listOf(FocusNotificationAction.PAUSE), FocusNotificationActions.available(emptyDuration.snapshot!!))
        assertNull(FocusNotificationActions.validate(context,
            FocusNotificationActions.intent(context, emptyDuration, FocusNotificationAction.FINISH), emptyDuration))
        assertNull(FocusNotificationActions.validate(context, intent, FocusTimerResult(FocusTimerOutcome.TIMER_NOT_FOUND)))
    }

    @Test fun normalTicksKeepPublicationSignatureButStateAndVersionChangesInvalidateIt() {
        val info = info()
        val signature = FocusRealtimeNotificationRenderer.signature(context, info)
        assertEquals(signature, FocusRealtimeNotificationRenderer.signature(context,
            info.copy(snapshot = info.snapshot!!.copy(elapsedSeconds = 61))))
        assertNotEquals(signature, FocusRealtimeNotificationRenderer.signature(context, info(state = FocusTimerState.PAUSED)))
        assertNotEquals(signature, FocusRealtimeNotificationRenderer.signature(context,
            info.copy(activeTimer = info.activeTimer!!.copy(updatedAt = now.toString()))))
    }

    @Test fun validatedPauseResumeAndFinishUseSharedCommandsAndSaveExactlyOneSession() = runBlocking {
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        try {
            db.tasks.create(Task("flow", "Flow", createdAt = now.toString(), updatedAt = now.toString()))
            val service = db.createFocusTimerService()
            service.execute(FocusTimerCommand.Start("flow", FocusTimerMode.STOPWATCH), now)
            val running = db.focusTimerNotificationInfo(Instant.fromEpochMilliseconds(nowMillis + 30_000))
            val pause = FocusNotificationActions.intent(context, running, FocusNotificationAction.PAUSE)
            val action = FocusNotificationActions.validate(context, pause, running)!!
            service.execute(FocusNotificationActions.command(action), Instant.fromEpochMilliseconds(nowMillis + 30_000))
            val paused = db.focusTimerNotificationInfo(Instant.fromEpochMilliseconds(nowMillis + 50_000))
            assertEquals(30L, paused.snapshot!!.elapsedSeconds)
            assertNull(FocusNotificationActions.validate(context, pause, paused))
            val resume = FocusNotificationActions.intent(context, paused, FocusNotificationAction.RESUME)
            service.execute(FocusNotificationActions.command(FocusNotificationActions.validate(context, resume, paused)!!),
                Instant.fromEpochMilliseconds(nowMillis + 60_000))
            val resumed = db.focusTimerNotificationInfo(Instant.fromEpochMilliseconds(nowMillis + 75_000))
            assertNull(FocusNotificationActions.validate(context, pause, resumed))
            assertNull(FocusNotificationActions.validate(context, resume, resumed))
            assertEquals(45L, resumed.snapshot!!.elapsedSeconds)
            val finish = FocusNotificationActions.intent(context, resumed, FocusNotificationAction.FINISH)
            service.execute(FocusNotificationActions.command(FocusNotificationActions.validate(context, finish, resumed)!!),
                Instant.fromEpochMilliseconds(nowMillis + 75_000))
            assertNull(db.focusTimerNotificationInfo(Instant.fromEpochMilliseconds(nowMillis + 76_000)).snapshot)
            assertEquals(45L, db.focusSessions.recent(10).single().durationSeconds)
            assertEquals(TaskState.ACTIVE, db.tasks.find("flow")!!.state)
            assertNull(FocusNotificationActions.validate(context, finish, db.focusTimerNotificationInfo()))
        } finally { db.close() }
    }

    @Test fun finishAndCompleteSavesFocusAndUpdatesTaskThroughCore() = runBlocking {
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        try {
            db.tasks.create(Task("complete-flow", "Flow", createdAt = now.toString(), updatedAt = now.toString()))
            val service = db.createFocusTimerService()
            service.execute(FocusTimerCommand.Start("complete-flow", FocusTimerMode.STOPWATCH), now)
            val info = db.focusTimerNotificationInfo(Instant.fromEpochMilliseconds(nowMillis + 60_000))
            val intent = FocusNotificationActions.intent(context, info, FocusNotificationAction.FINISH_AND_COMPLETE)
            service.execute(FocusNotificationActions.command(FocusNotificationActions.validate(context, intent, info)!!),
                Instant.fromEpochMilliseconds(nowMillis + 60_000))
            assertEquals(TaskState.COMPLETED, db.tasks.find("complete-flow")!!.state)
            assertEquals(FocusCompletionKind.FOCUS_AND_TASK_COMPLETED, db.focusSessions.recent(10).single().completionKind)
            assertNull(db.focusTimerNotificationInfo().snapshot)
        } finally { db.close() }
    }

    @Test fun countdownExpiryHidesReadOnlyCardAndReconcileSavesOneSession() = runBlocking {
        val db = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        var scheduled = 0
        var cancelled = 0
        val port = object : FocusAlarmPort {
            override fun scheduleCountdown(sessionUid: String, fireAt: String, taskTitle: String): FocusAlarmResult { scheduled++; return FocusAlarmResult(true) }
            override fun cancelCountdown(sessionUid: String): FocusAlarmResult { cancelled++; return FocusAlarmResult(true) }
        }
        try {
            db.tasks.create(Task("countdown", "Flow", createdAt = now.toString(), updatedAt = now.toString()))
            val service = db.createFocusTimerService(port)
            service.execute(FocusTimerCommand.Start("countdown", FocusTimerMode.COUNTDOWN, 60), now)
            assertEquals(1, scheduled)
            val expiry = Instant.fromEpochMilliseconds(nowMillis + 60_000)
            assertNull(db.focusTimerNotificationInfo(expiry).snapshot)
            assertTrue(db.focusSessions.recent(10).isEmpty())
            service.execute(FocusTimerCommand.Reconcile, expiry)
            service.execute(FocusTimerCommand.Reconcile, expiry)
            assertEquals(1, db.focusSessions.recent(10).size)
            assertEquals(FocusCompletionKind.COUNTDOWN_ELAPSED, db.focusSessions.recent(10).single().completionKind)
            assertTrue(cancelled > 0)
        } finally { db.close() }
    }
}
