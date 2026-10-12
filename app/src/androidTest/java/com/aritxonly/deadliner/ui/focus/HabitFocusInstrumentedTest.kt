package com.aritxonly.deadliner.ui.focus

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aritxonly.deadliner.FocusTimerActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.main.simplified.HabitDisplayLayout
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.deadliner.domain.focus.*
import com.deadliner.domain.habit.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File
import java.io.FileOutputStream

/** Uses unique fixtures only; no database reset or mutation of existing user targets. */
@RunWith(AndroidJUnit4::class)
class HabitFocusInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<FocusTimerActivity>()
    private val core get() = KmpV6Runtime.database
    private fun text(id: Int) = ui.activity.getString(id)
    private fun habit(uid: String, quantity: Boolean = true) = Habit(uid, "Read 20 pages",
        schedule = HabitSchedule(HabitScheduleKind.WEEKLY_DAYS, "2026-01-01", weekdaysMask = HabitWeekdays.ALL),
        goal = HabitGoal(targetCount = if (quantity) 20 else 1),
        tracking = HabitTracking(if (quantity) HabitTrackingMode.QUANTITY else HabitTrackingMode.CHECK_OFF, "pages"),
        createdAt = java.time.Instant.now().toString())

    @Test fun quantityCancelAndConfirmUseActualAmountAndPersistOnce() = runBlocking {
        val uid = "habit-focus-quantity-" + System.nanoTime()
        val timer = core.createFocusTimerService()
        var sessionUid: String? = null
        check(timer.current().activeTimer == null) { "Requires idle test timer" }
        try {
            core.habits.create(habit(uid))
            val started = timer.execute(FocusTimerCommand.StartHabit(uid, FocusTimerMode.STOPWATCH),
                Instant.parse(java.time.Instant.now().minusSeconds(30).toString()))
            sessionUid = started.snapshot!!.sessionUid
            KmpV6Runtime.focusTimerViewModel.loadNow()
            waitFor("Read 20 pages")
            ui.onNodeWithContentDescription(text(R.string.habit_focus_finish_record)).performClick()
            ui.onNodeWithTag("habit-focus-amount").assertIsDisplayed()
            capture("habit-focus-quantity-dialog")
            ui.onNodeWithText(text(R.string.cancel)).performClick()
            ui.waitForIdle()
            assertNotNull(timer.current().snapshot)
            assertTrue(core.habits.records(uid).isEmpty())
            ui.onNodeWithContentDescription(text(R.string.habit_focus_finish_record)).performClick()
            ui.onNodeWithTag("habit-focus-amount").performTextInput("7")
            ui.onNodeWithText(text(R.string.habit_focus_finish_record)).performClick()
            ui.waitUntil(10_000) { KmpV6Runtime.focusTimerViewModel.uiState.value.timer == null }
            assertEquals(7, core.habits.records(uid).single().count)
            assertEquals(7, core.focusSessions.find(sessionUid!!)?.habitRecordedAmount)
            timer.execute(FocusTimerCommand.FinishAndRecordHabit(sessionUid!!, 7))
            assertEquals(7, core.habits.records(uid).single().count)
            capture("habit-focus-quantity-finished")
        } finally { cleanup(uid, sessionUid) }
    }

    @Test fun countdownFinishesWhileQuantityInputRemainsAndCanRecordResult() = runBlocking {
        val uid = "habit-focus-countdown-" + System.nanoTime()
        val timer = core.createFocusTimerService()
        var sessionUid: String? = null
        check(timer.current().activeTimer == null) { "Requires idle test timer" }
        try {
            core.habits.create(habit(uid))
            val start = java.time.Instant.now().minusSeconds(54)
            sessionUid = timer.execute(FocusTimerCommand.StartHabit(uid, FocusTimerMode.COUNTDOWN, 60),
                Instant.parse(start.toString())).snapshot!!.sessionUid
            KmpV6Runtime.focusTimerViewModel.loadNow()
            waitFor("Read 20 pages")
            ui.onNodeWithContentDescription(text(R.string.habit_focus_finish_record)).performClick()
            ui.onNodeWithTag("habit-focus-amount").performTextInput("4")
            ui.waitUntil(15_000) { KmpV6Runtime.focusTimerViewModel.uiState.value.timer == null }
            ui.onNodeWithTag("habit-focus-amount").assertIsDisplayed()
            assertTrue(core.habits.records(uid).isEmpty())
            capture("habit-focus-countdown-input")
            ui.onNodeWithTag("habit-focus-record-confirm").performClick()
            ui.waitUntil(10_000) { core.focusSessions.find(sessionUid!!)?.habitRecordedAmount == 4 }
            assertEquals(60L, core.focusSessions.find(sessionUid!!)?.durationSeconds)
            assertEquals(4, core.habits.records(uid).single().count)
        } finally { cleanup(uid, sessionUid) }
    }

    @Test fun bothHabitLayoutsReuseLeadingArchiveFocusAndTrailingEditDelete() = runBlocking {
        val uid = "habit-focus-swipe-" + System.nanoTime()
        check(core.createFocusTimerService().current().activeTimer == null)
        try {
            core.habits.create(habit(uid, false))
            val vm = KmpV6Runtime.habitListViewModel
            val state = vm.loadNow()
            val target = state.visibleHabits.single { it.habit.uid == uid }
            for ((classic, direction) in listOf(false to LayoutDirection.Ltr, true to LayoutDirection.Ltr,
                false to LayoutDirection.Rtl, true to LayoutDirection.Rtl)) {
                var focused: String? = null
                ui.activity.runOnUiThread {
                    ui.activity.setContent { DeadlinerTheme {
                        CompositionLocalProvider(LocalLayoutDirection provides direction,
                            LocalHabitFocusTimerEntry provides { id, _ -> focused = id }) {
                            AdaptiveMaterialScaffold { HabitDisplayLayout(state.weekOverview, state.selectedDate,
                                listOf(target), vm, isClassic = classic) }
                        }
                    } }
                }
                waitFor("Read 20 pages")
                val row = ui.onNodeWithTag("habit-card-$uid")
                assertEquals(listOf(text(R.string.archive), text(R.string.focus_swipe),
                    text(R.string.edit), text(R.string.delete)),
                    row.fetchSemanticsNode().config[SemanticsActions.CustomActions].map { it.label })
                row.performTouchInput { swipe(center, center + Offset(width * 0.34f, 0f), 400) }
                ui.onNodeWithContentDescription(text(R.string.archive)).assertIsDisplayed()
                ui.onNodeWithContentDescription(text(R.string.focus_swipe)).performClick()
                assertEquals(uid, focused)
                capture("habit-focus-${if (classic) "classic" else "modern"}-${direction.name.lowercase()}")
                row.performTouchInput { swipe(center, center - Offset(width * 0.34f, 0f), 400) }
                ui.onNodeWithContentDescription(text(R.string.edit)).assertIsDisplayed()
                ui.onNodeWithContentDescription(text(R.string.delete)).performClick()
                ui.onNodeWithText(text(R.string.habit_focus_change_confirm)).assertIsDisplayed()
                ui.onNodeWithText(text(R.string.cancel)).performClick()
                assertFalse(core.habits.find(uid)!!.isDeleted)
            }
        } finally { core.habits.delete(uid, java.time.Instant.now().toString()) }
    }

    private suspend fun cleanup(uid: String, sessionUid: String?) {
        val timer = core.createFocusTimerService()
        if (timer.current().activeTimer?.boundTarget == FocusTargetRef(FocusTargetKind.HABIT, uid)) timer.execute(FocusTimerCommand.Discard)
        val now = java.time.Instant.now().toString()
        sessionUid?.let { core.focusSessions.delete(it, now) }
        core.habits.delete(uid, now)
        KmpV6Runtime.focusTimerViewModel.loadNow()
    }
    private fun waitFor(value: String) {
        ui.waitUntil(10_000) { ui.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
        ui.waitForIdle()
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        FileOutputStream(File(instrumentation.targetContext.cacheDir, "$name.png")).use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
    }
}
