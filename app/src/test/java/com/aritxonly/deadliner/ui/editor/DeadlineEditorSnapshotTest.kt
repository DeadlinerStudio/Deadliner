package com.aritxonly.deadliner.ui.editor

import com.deadliner.domain.habit.HabitGoalType
import com.deadliner.domain.habit.HabitPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class DeadlineEditorSnapshotTest {
    private val snapshot = DeadlineEditorSnapshot(
        page = 1, name = "任务 ✨", note = "多行\n备注", start = "2026-10-15T09:30", end = "2026-10-16T18:00",
        starred = true, taskCategory = "work", habitName = "阅读", period = "WEEKLY", weekdays = 0b0010101,
        goal = "TOTAL", frequency = "", total = "12", reminder = true, reminderTime = "21:45",
        habitCategory = "study", calendarEventId = 123L, aiText = "今晚阅读", resumeAI = true,
        pendingCalendarAction = "SAVE_TO_CALENDAR", pendingSaveAction = "SAVE_TO_CALENDAR",
    )

    @Test fun roundTripKeepsIndependentTaskHabitDraftsAndPendingPermission() {
        val restored = DeadlineEditorSnapshot.decode(snapshot.encode())
        assertEquals(snapshot, restored)
        assertEquals(TaskEditorDraft("任务 ✨", "多行\n备注", LocalDateTime.parse("2026-10-15T09:30"),
            LocalDateTime.parse("2026-10-16T18:00"), true, "work"), restored.taskDraft())
        assertEquals(HabitEditorDraft("阅读", HabitPeriod.WEEKLY, 0b0010101, HabitGoalType.TOTAL,
            "", "12", true, LocalTime.of(21, 45), "study"), restored.habitDraft())
        assertEquals("SAVE_TO_CALENDAR", restored.pendingCalendarAction)
        assertEquals(123L, restored.calendarEventId)
    }

    @Test fun emptyPartialDraftAndNoCategoryStayEditableAfterRestore() {
        val restored = DeadlineEditorSnapshot.decode(snapshot.copy(name = "", habitName = "", taskCategory = null,
            habitCategory = null, total = "", calendarEventId = null, pendingCalendarAction = null,
            pendingSaveAction = null, resumeAI = false).encode())
        assertEquals("", restored.taskDraft().name)
        assertEquals("", restored.habitDraft().timesPerPeriod)
        assertEquals("", restored.habitDraft().totalTarget)
        assertEquals(null, restored.taskDraft().categoryUid)
        assertEquals(null, restored.habitDraft().categoryUid)
    }
}
