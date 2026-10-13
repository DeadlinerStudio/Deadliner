package com.aritxonly.deadliner.ui.overview

import com.deadliner.domain.focus.FocusSession
import com.deadliner.domain.focus.FocusTimerMode
import com.deadliner.domain.focus.FocusCompletionKind
import com.deadliner.domain.focus.FocusTargetKind
import com.deadliner.domain.focus.FocusTargetRef
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusManagementStateTest {
    private fun session(uid: String, start: String, seconds: Long = 300) = FocusSession(
        uid = uid, taskUid = "task-1", taskTitleSnapshot = "Task", mode = FocusTimerMode.STOPWATCH,
        startedAt = start, endedAt = Instant.parse(start).plusSeconds(600).toString(),
        durationSeconds = seconds, completionKind = FocusCompletionKind.FOCUS_ONLY, createdAt = start)

    @Test fun todayUsesLocalStartDateAcrossMidnight() {
        val previous = session("previous", "2026-10-11T15:59:00Z")
        val today = session("today", "2026-10-11T16:00:00Z")
        val data = focusManagementSnapshot(listOf(previous, today), LocalDate.parse("2026-10-12"), ZoneId.of("Asia/Shanghai"))
        assertEquals(300L, data.todaySeconds)
        assertEquals(1, data.todayCount)
        assertEquals(600L, data.totalSeconds)
    }

    @Test fun deletedRecordsAreExcludedAndAdjustedTaskAndHabitRecordsRemain() {
        val task = session("task", "2026-10-12T01:00:00Z", 120)
        val habit = session("habit", "2026-10-12T02:00:00Z", 240).copy(taskUid = "",
            target = FocusTargetRef(FocusTargetKind.HABIT, "habit-1"), habitOccurredOn = "2026-10-12")
        val deleted = session("deleted", "2026-10-12T03:00:00Z").copy(isDeleted = true)
        val data = focusManagementSnapshot(listOf(task, deleted, habit), LocalDate.parse("2026-10-12"), ZoneId.of("Asia/Shanghai"))
        assertEquals(listOf("habit", "task"), data.sessions.map { it.uid })
        assertEquals(360L, data.todaySeconds)
        assertEquals(360L, data.totalSeconds)
        assertEquals(2, data.todayCount)
    }

    @Test fun correctionCannotReinflateAlreadyShortenedDuration() {
        assertEquals(120L, maximumFocusAdjustment(session("adjusted", "2026-10-12T01:00:00Z", 120)))
    }
}
