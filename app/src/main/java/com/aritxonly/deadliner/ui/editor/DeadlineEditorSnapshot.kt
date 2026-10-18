package com.aritxonly.deadliner.ui.editor

import com.google.gson.Gson
import androidx.annotation.Keep
import java.time.LocalDateTime
import java.time.LocalTime
import com.deadliner.domain.habit.HabitGoalType
import com.deadliner.domain.habit.HabitPeriod

private val snapshotJson = Gson()

/** Serializable UI draft only. Restoring it never automatically repeats a save/calendar write. */
@Keep
internal data class DeadlineEditorSnapshot(
    val page: Int,
    val name: String,
    val note: String,
    val start: String,
    val end: String,
    val starred: Boolean,
    val taskCategory: String?,
    val habitName: String,
    val period: String,
    val weekdays: Int,
    val goal: String,
    val frequency: String,
    val total: String,
    val reminder: Boolean,
    val reminderTime: String,
    val habitCategory: String?,
    val calendarEventId: Long?,
    val aiText: String,
    val resumeAI: Boolean,
    val pendingCalendarAction: String?,
    val pendingSaveAction: String?,
) {
    fun taskDraft() = TaskEditorDraft(name, note, LocalDateTime.parse(start), LocalDateTime.parse(end), starred, taskCategory)
    fun habitDraft() = HabitEditorDraft(habitName, HabitPeriod.valueOf(period), weekdays,
        HabitGoalType.valueOf(goal), frequency, total, reminder, LocalTime.parse(reminderTime), habitCategory)
    fun encode(): String = snapshotJson.toJson(this)
    companion object {
        fun decode(value: String): DeadlineEditorSnapshot = snapshotJson.fromJson(value, DeadlineEditorSnapshot::class.java)
    }
}

internal fun DeadlineEditorState.saveSnapshot(): String = DeadlineEditorSnapshot(
    selectedPage, taskDraft.name, taskDraft.note, taskDraft.startTime.toString(), taskDraft.endTime.toString(),
    taskDraft.isStarred, taskDraft.categoryUid, habitDraft.name, habitDraft.period.name, habitDraft.weekdaysMask,
    habitDraft.goalType.name, habitDraft.timesPerPeriod, habitDraft.totalTarget, habitDraft.reminderEnabled,
    habitDraft.reminderTime.toString(), habitDraft.categoryUid, calendarEventId, aiInputText,
    (autoRunAiOnAppear && !aiAutoTriggered) || isAiLoading, pendingCalendarAction?.name, pendingSaveAction?.name,
).encode()

internal fun DeadlineEditorState.restoreSnapshot(raw: String) {
    val value = DeadlineEditorSnapshot.decode(raw)
    selectedPage = when {
        editingTaskUid != null -> 0
        editingHabitUid != null -> 1
        else -> value.page.coerceIn(0, 1)
    }
    taskDraft = value.taskDraft()
    habitDraft = value.habitDraft()
    calendarEventId = value.calendarEventId
    aiInputText = value.aiText
    autoRunAiOnAppear = value.resumeAI
    aiAutoTriggered = false
    pendingCalendarAction = value.pendingCalendarAction?.let(DeadlineEditorState.PendingCalendarAction::valueOf)
    pendingSaveAction = value.pendingSaveAction?.let(DeadlineEditorState.SaveAction::valueOf)
}
