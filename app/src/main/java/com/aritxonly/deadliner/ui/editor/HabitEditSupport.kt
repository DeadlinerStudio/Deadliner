package com.aritxonly.deadliner.ui.editor

import com.deadliner.domain.habit.Habit
import java.time.LocalTime

/** Legacy editor presentation; the shared editor retains schedule/tracking fields not shown here. */
internal fun habitDraftForEditing(habit: Habit) = HabitEditorDraft(
    name = habit.name, period = habit.period, weekdaysMask = habit.schedule.weekdaysMask ?: 0b1111111,
    goalType = habit.goalType, timesPerPeriod = habit.goal.targetCount.toString(),
    totalTarget = habit.totalTarget?.toString().orEmpty(), reminderEnabled = habit.reminder != null,
    reminderTime = habit.reminder?.localTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.of(20, 0),
    categoryUid = habit.categoryUid,
)
