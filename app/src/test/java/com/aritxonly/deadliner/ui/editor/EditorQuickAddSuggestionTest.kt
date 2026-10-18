package com.aritxonly.deadliner.ui.editor

import com.aritxonly.deadliner.ui.quickadd.QuickAddState
import com.deadliner.domain.habit.HabitPeriod
import com.deadliner.domain.quickadd.QuickAddProposal
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class EditorQuickAddSuggestionTest {
    private val start = LocalDateTime.parse("2026-10-17T09:00")
    private val current = TaskEditorDraft("旧名称", "手工备注", start, start.plusHours(2), true, "work")
    private fun suggestion(p: QuickAddProposal = QuickAddProposal(title = "交报告", ready = true),
                           startAt: LocalDateTime? = null, dueAt: LocalDateTime? = null) =
        EditorQuickAddSuggestion("明天交报告", p, startAt, dueAt)

    @Test fun automaticNameRecognitionPreservesManualFieldsAndDoesNotInventADueDate() {
        val result = suggestion().mergeTask(current)
        assertEquals(current.copy(name = "交报告"), result)
    }
    @Test fun explicitTimesAndNotesUpdateWhileCategoryAndStarredStayInTheForm() {
        val due = start.plusDays(1)
        val result = suggestion(QuickAddProposal(title = "新任务", note = "识别备注", ready = true),
            start.plusHours(1), due).mergeTask(current)
        assertEquals(due, result.endTime)
        assertEquals(start.plusHours(1), result.startTime)
        assertEquals("识别备注", result.note)
        assertTrue(result.isStarred)
        assertEquals("work", result.categoryUid)
    }
    @Test fun aDueOnlySuggestionInThePastKeepsTheIntervalValid() {
        val due = start.minusDays(1)
        val result = suggestion(dueAt = due).mergeTask(current)
        assertEquals(due, result.endTime)
        assertEquals(due.minusHours(1), result.startTime)
    }
    @Test fun invalidExplicitRangeCannotReplaceTheValidManualRange() {
        val result = suggestion(startAt = start.plusDays(1), dueAt = start).mergeTask(current)
        assertEquals(current.startTime, result.startTime)
        assertEquals(current.endTime, result.endTime)
    }
    @Test fun recognitionMetadataDoesNotReapplyTheSameResultOverManualEdits() {
        val first = suggestion()
        val confirmed = first.copy(proposal = first.proposal.copy(source = "AI", message = "AI 已确认草案"))
        assertEquals(first.key(), confirmed.key())
        assertNotEquals(first.key(), first.copy(text = "后天交报告").key())
    }
    @Test fun habitRecognitionPreservesManualReminderAndCategoryWhenNotSpecified() {
        val old = HabitEditorDraft(name = "旧习惯", reminderEnabled = true,
            reminderTime = LocalTime.of(21, 45), categoryUid = "study")
        val result = suggestion(QuickAddProposal(title = "读书", kind = "HABIT", ready = true,
            period = "WEEKLY", targetCount = 3)).mergeHabit(old)
        assertEquals(HabitPeriod.WEEKLY, result.period)
        assertEquals("3", result.timesPerPeriod)
        assertEquals(old.reminderTime, result.reminderTime)
        assertTrue(result.reminderEnabled)
        assertEquals("study", result.categoryUid)
    }
    @Test fun emptyAndRejectedInputDoNotGenerateAnAutomaticFormUpdate() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val state = QuickAddState(scope)
            state.open("TASK")
            assertNull(state.editorSuggestion())
            state.edit("123")
            assertNull(state.editorSuggestion())
            state.edit("交报告")
            assertNotNull(state.editorSuggestion())
            assertNull(state.editorSuggestion()!!.due)
        } finally { scope.cancel() }
    }
}
