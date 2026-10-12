package com.aritxonly.deadliner.ui.quickadd

import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.editor.HabitEditorDraft
import com.aritxonly.deadliner.ui.editor.TaskEditorDraft
import com.deadliner.domain.habit.HabitGoalType
import com.deadliner.domain.habit.HabitPeriod
import com.deadliner.domain.habit.HabitReminder
import com.deadliner.domain.quickadd.QuickAddProposal
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime

object QuickAddPersistence {
    fun taskDraft(p: QuickAddProposal, raw: String): TaskEditorDraft {
        val now = LocalDateTime.now()
        val due = p.dueAt?.let { runCatching { LocalDateTime.parse(it.replace(' ', 'T')) }.getOrNull() }
            ?: now.plusHours(1)
        return TaskEditorDraft(name = p.title.ifBlank { raw.trim() }, note = p.note,
            startTime = if (due.isBefore(now)) due else now, endTime = due)
    }
    fun habitDraft(p: QuickAddProposal, raw: String): HabitEditorDraft = HabitEditorDraft(
        name = p.title.ifBlank { raw.trim() }, period = period(p), weekdaysMask = p.weekdaysMask,
        goalType = if (p.period == "TOTAL") HabitGoalType.TOTAL else HabitGoalType.PER_PERIOD,
        timesPerPeriod = p.targetCount.toString(), totalTarget = if (p.period == "TOTAL") p.targetCount.toString() else "",
        reminderEnabled = p.reminderTime != null,
        reminderTime = p.reminderTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.of(20, 0))
    private fun period(p: QuickAddProposal): HabitPeriod = when(p.period) {
        "WEEKLY" -> HabitPeriod.WEEKLY; "MONTHLY" -> HabitPeriod.MONTHLY
        "EBBINGHAUS" -> HabitPeriod.EBBINGHAUS; "ONCE" -> HabitPeriod.ONCE; else -> HabitPeriod.DAILY
    }
    suspend fun save(p: QuickAddProposal, startAt: LocalDateTime? = null, categoryUid: String? = null,
                     calendarEventId: Long? = null) {
        check(p.ready)
        if (p.kind == "TASK") {
            val editor = KmpV6Runtime.createTaskEditorViewModel()
            editor.updateTitle(p.title); editor.updateNote(p.note); editor.updateDueAt(p.dueAt?.replace(' ', 'T'))
            editor.updateStartAt(startAt?.toString())
            editor.updateCalendarEventId(calendarEventId)
            editor.updateCategory(categoryUid)
            editor.save()
            val state = editor.uiState.first { it.savedTask != null || it.error != null }
            check(state.savedTask != null) { state.error ?: "保存失败" }
            KmpV6Runtime.taskListViewModel.loadNow()
        } else {
            val editor = KmpV6Runtime.createHabitEditorViewModel()
            editor.updateName(p.title); editor.updatePeriod(period(p)); editor.updateWeekdaysMask(p.weekdaysMask)
            editor.updateTimesPerPeriod(p.targetCount)
            editor.updateGoalType(if(p.period == "TOTAL") HabitGoalType.TOTAL else HabitGoalType.PER_PERIOD)
            editor.updateTotalTarget(if(p.period == "TOTAL") p.targetCount else null)
            editor.updateReminder(p.reminderTime?.let { HabitReminder(it) })
            editor.updateCategory(categoryUid)
            editor.save()
            val state = editor.uiState.first { it.savedHabit != null || it.error != null }
            check(state.savedHabit != null) { state.error ?: "保存失败" }
            KmpV6Runtime.habitListViewModel.loadNow()
        }
    }
}
