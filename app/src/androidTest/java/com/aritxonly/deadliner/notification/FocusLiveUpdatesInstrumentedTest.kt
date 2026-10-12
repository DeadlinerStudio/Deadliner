package com.aritxonly.deadliner.notification

import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.aritxonly.deadliner.FocusTimerActivity
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.focus.FocusTimerCommand
import com.deadliner.domain.focus.FocusTimerMode
import com.deadliner.domain.focus.FocusTimerState
import com.deadliner.domain.task.Task
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 36)
class FocusLiveUpdatesInstrumentedTest {
    @Test fun promotedStandardCardCarriesWorkingPauseResumeAndFinishButtons() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val uid = "focus-live-test-${System.nanoTime()}"
        instrumentation.runOnMainSync {
            KmpV6Runtime.initialize(context)
            check(KmpV6Runtime.database.focusTimerNotificationInfo().snapshot == null) { "Requires an empty test timer" }
            KmpV6Runtime.database.tasks.create(Task(uid, "Focus LiveUpdates test", createdAt = Clock.System.now().toString(), updatedAt = Clock.System.now().toString()))
            runBlocking { KmpV6Runtime.focusTimerViewModel.executeNow(FocusTimerCommand.Start(uid, FocusTimerMode.STOPWATCH)) }
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        var scenario: ActivityScenario<FocusTimerActivity>? = null
        fun waitFor(predicate: () -> Boolean) {
            val end = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < end) {
                if (predicate()) return
                Thread.sleep(100)
            }
            fail("Timed out waiting for focus notification transition")
        }
        fun card() = manager.activeNotifications.firstOrNull { it.id == FocusRealtimeNotificationRenderer.NOTIFICATION_ID }?.notification
        try {
            scenario = ActivityScenario.launch(FocusTimerActivity.newIntent(context, uid, "Focus LiveUpdates test"))
            waitFor { card()?.actions?.size == 3 }
            val running = requireNotNull(card())
            assertTrue(running.hasPromotableCharacteristics())
            assertTrue(running.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
            assertEquals(3, running.actions.size)
            android.util.Log.i("FocusLiveUpdatesTest", "eligible=${running.hasPromotableCharacteristics()} promoted=${running.flags and Notification.FLAG_PROMOTED_ONGOING != 0} canPost=${manager.canPostPromotedNotifications()}")
            scenario.onActivity { running.actions[0].actionIntent.send() }
            waitFor { KmpV6Runtime.focusTimerViewModel.uiState.value.timer?.state == FocusTimerState.PAUSED &&
                card()?.extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == false }
            val paused = requireNotNull(card())
            assertEquals(3, paused.actions.size)
            assertTrue(paused.hasPromotableCharacteristics())
            assertEquals(0L, paused.`when`)
            scenario.onActivity { paused.actions[0].actionIntent.send() }
            waitFor { KmpV6Runtime.focusTimerViewModel.uiState.value.timer?.state == FocusTimerState.RUNNING &&
                card()?.extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true }
            val resumed = requireNotNull(card())
            scenario.onActivity { resumed.actions[1].actionIntent.send() }
            waitFor { KmpV6Runtime.focusTimerViewModel.uiState.value.timer == null && card() == null }
            assertEquals(1, KmpV6Runtime.database.focusSessions.recentForTask(uid, 10).size)
            fun reward() = manager.activeNotifications.firstOrNull { it.id == RealtimeNotificationService.FOREGROUND_ID }?.notification
            waitFor { reward()?.actions?.size == 1 }
            val finished = requireNotNull(reward())
            assertTrue(finished.hasPromotableCharacteristics())
            assertEquals(0L, finished.`when`)
            assertFalse(finished.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
            assertEquals(context.getString(com.aritxonly.deadliner.R.string.notification_island_encouragement), finished.shortCriticalText)
            assertEquals(context.getString(com.aritxonly.deadliner.R.string.notification_live_open_task), finished.actions.single().title)
        } finally {
            instrumentation.runOnMainSync {
                runBlocking { KmpV6Runtime.focusTimerViewModel.executeNow(FocusTimerCommand.Discard) }
                FocusTimerForegroundService.stop(context)
                KmpV6Runtime.database.tasks.delete(uid, Clock.System.now().toString())
            }
            scenario?.close()
        }
    }
}
