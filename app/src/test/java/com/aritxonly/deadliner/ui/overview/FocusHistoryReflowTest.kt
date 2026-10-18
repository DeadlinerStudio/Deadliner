package com.aritxonly.deadliner.ui.overview

import com.deadliner.domain.focus.*
import com.deadliner.domain.focus.management.*
import org.junit.Assert.*
import org.junit.Test

class FocusHistoryReflowTest {
    private fun day(date: String, uids: List<String>) = FocusManagementDay(date, uids.size * 60L, uids.size, uids.map { uid ->
        val stamp = date + "T00:00:00Z"
        FocusManagementRecord(FocusSession(uid, "target", "Target", FocusTimerMode.STOPWATCH,
            startedAt = stamp, endedAt = date + "T00:01:00Z", durationSeconds = 60,
            completionKind = FocusCompletionKind.FOCUS_ONLY, createdAt = stamp),
            "Target", FocusManagedTargetStatus.ACTIVE, date, 60)
    })
    @Test fun anOddRecordRemainsVisibleWhenHistoryChangesFromOneColumnToTwo() {
        val days = listOf(day("2026-10-15", listOf("a", "b", "c", "d", "e")), day("2026-10-14", listOf("f", "g", "h")))
        assertEquals(7, focusHistoryAnchorIndex(days, 1, "d", 3))
        assertEquals(5, focusHistoryAnchorIndex(days, 2, "d", 3))
        assertEquals(8, focusHistoryAnchorIndex(days, 2, "g", 3))
        assertEquals(11, focusHistoryAnchorIndex(days, 1, "g", 3))
        assertNull(focusHistoryAnchorIndex(days, 2, "deleted", 3))
    }
}
