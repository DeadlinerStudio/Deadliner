package com.aritxonly.deadliner.ui.quickadd

import android.app.Application
import com.aritxonly.deadliner.AddDDLActivity
import com.deadliner.domain.habit.HabitPeriod
import com.deadliner.domain.quickadd.QuickAddProposal
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class QuickAddEditorEntryTest {
    @Test fun manuallyCompletedTaskCanExpandWithoutRecognition() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val source = QuickAddState(scope)
            source.open("TASK")
            source.edit("123")
            val due = LocalDateTime.now().plusDays(1)
            source.selectDueAt(due)
            assertTrue(source.canSubmit)
            val intent = QuickAddEditorEntry.intent(RuntimeEnvironment.getApplication(), source)
            val form = QuickAddEditorEntry.initialData(intent)!!
            assertEquals("123", form.task.name)
            assertEquals(due, form.task.endTime)
            assertEquals(source.effectiveStartAt, form.task.startTime)
            val target = QuickAddState(scope)
            assertTrue(QuickAddEditorEntry.restoreDraft(target, intent))
            assertTrue(target.canSubmit)
            assertEquals(source.effectiveProposal, target.effectiveProposal)
        } finally { scope.cancel() }
    }

    @Test fun endTimeChosenBeforeTypingSurvivesRecognitionAndExpansion() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val source = QuickAddState(scope)
            source.open("TASK")
            val chosen = LocalDateTime.now().plusDays(3).withHour(18).withMinute(30).withSecond(0).withNano(0)
            source.dueAt = chosen
            source.edit("明天交报告")
            assertEquals(chosen.toString(), source.effectiveProposal.dueAt)
            val intent = QuickAddEditorEntry.intent(RuntimeEnvironment.getApplication(), source)
            assertEquals(chosen, QuickAddEditorEntry.initialData(intent)!!.task.endTime)
            val target = QuickAddState(scope)
            assertTrue(QuickAddEditorEntry.restoreDraft(target, intent))
            assertEquals(chosen.toString(), target.effectiveProposal.dueAt)
        } finally { scope.cancel() }
    }

    @Test fun expansionCarriesTheVisibleCardBoundsAndSuppressesItsCollapseToTheFab() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.open("TASK")
            state.panelBounds = androidx.compose.ui.geometry.Rect(24f, 320f, 390f, 720f)
            val intent = QuickAddEditorEntry.intent(RuntimeEnvironment.getApplication(), state)
            assertEquals(state.panelBounds, QuickAddEditorEntry.sourceBounds(intent))
            state.expandToEditor()
            assertTrue(state.expandingToEditor)
            assertFalse(state.visible)
            state.open("TASK")
            assertFalse(state.expandingToEditor)
            assertNull(QuickAddEditorEntry.sourceBounds(android.content.Intent()))
        } finally { scope.cancel() }
    }

    @Test fun fullEditorAndInlineCardReceiveTheSameCorrectedTaskWithoutSaving() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val source = QuickAddState(scope)
            val start = LocalDateTime.parse("2026-10-17T09:15")
            source.restoreDraft(QuickAddProposal(title = "已手动修正", note = "保留备注",
                dueAt = "2026-10-18T15:30", ready = true), "明天下午交报告", start, "work", 42L)
            val intent = QuickAddEditorEntry.intent(RuntimeEnvironment.getApplication(), source)
            val full = QuickAddEditorEntry.initialData(intent)!!
            assertEquals(QuickAddEditorEntry.formData(source), full)
            assertEquals(start, full.task.startTime)
            assertEquals("work", full.task.categoryUid)
            assertEquals(42L, full.calendarEventId)
            assertFalse(full.autoRunAI)
            val target = QuickAddState(scope)
            assertTrue(QuickAddEditorEntry.restoreDraft(target, intent))
            assertEquals(source.text, target.text)
            assertEquals(source.effectiveProposal, target.effectiveProposal)
            assertEquals(full, QuickAddEditorEntry.formData(target))
        } finally { scope.cancel() }
    }

    @Test fun habitScheduleReminderAndCategorySurviveExpansion() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.restoreDraft(QuickAddProposal(kind = "HABIT", title = "阅读", ready = true,
                period = "WEEKLY", targetCount = 3, weekdaysMask = 0b0010101, reminderTime = "21:45"),
                "每周读书三次", null, "study", null)
            val intent = QuickAddEditorEntry.intent(RuntimeEnvironment.getApplication(), state)
            assertEquals(1, intent.getIntExtra("EXTRA_CURRENT_TYPE", -1))
            val habit = QuickAddEditorEntry.initialData(intent)!!.habit
            assertEquals(HabitPeriod.WEEKLY, habit.period)
            assertEquals(0b0010101, habit.weekdaysMask)
            assertEquals("3", habit.timesPerPeriod)
            assertEquals(LocalTime.of(21, 45), habit.reminderTime)
            assertTrue(habit.reminderEnabled)
            assertEquals("study", habit.categoryUid)
        } finally { scope.cancel() }
    }

    @Test fun expandingDuringRecognitionRequestsContinuationAndCancelsTheOldRequest() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var cancelled = false
            val source = QuickAddState(scope) { _, _ ->
                try { awaitCancellation() } finally { cancelled = true }
            }
            source.open("TASK")
            source.edit("明天下午交报告")
            source.recognizeWithAI()
            val intent = QuickAddEditorEntry.intent(RuntimeEnvironment.getApplication(), source)
            assertTrue(intent.getBooleanExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, false))
            assertTrue(QuickAddEditorEntry.initialData(intent)!!.autoRunAI)
            source.dismiss()
            assertTrue(cancelled)
            var continued = 0
            val target = QuickAddState(scope) { _, _ ->
                continued++
                QuickAddProposal(title = "交报告", ready = true, source = "AI")
            }
            assertTrue(QuickAddEditorEntry.restoreDraft(target, intent))
            target.recognizeWithAI()
            assertEquals(1, continued)
            assertEquals("AI", target.proposal.source)
            assertTrue(target.canSubmit)
        } finally { scope.cancel() }
    }

    @Test fun emptyCardCanExpandWithoutInventingATitleOrStartingAI() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.open("TASK")
            val data = QuickAddEditorEntry.initialData(
                QuickAddEditorEntry.intent(RuntimeEnvironment.getApplication(), state))!!
            assertEquals("", data.task.name)
            assertEquals("", data.aiText)
            assertFalse(data.autoRunAI)
        } finally { scope.cancel() }
    }
}
