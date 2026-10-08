package com.aritxonly.deadliner.notification

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class NotificationTimingTest {
    @Test fun offsetPreservesTheInstant() {
        assertEquals(Instant.parse("2026-10-07T02:00:00Z"), NotificationTiming.dueInstant("2026-10-07T10:00:00+08:00"))
    }
    @Test fun windowIncludesStartButExcludesDeadlineAndMovedFuture() {
        val due = "2026-10-07T10:00:00Z"
        assertTrue(NotificationTiming.inLiveWindow(due, 10, Instant.parse("2026-10-07T09:50:00Z")))
        assertFalse(NotificationTiming.inLiveWindow(due, 10, Instant.parse("2026-10-07T09:49:59Z")))
        assertFalse(NotificationTiming.inLiveWindow(due, 10, Instant.parse("2026-10-07T10:00:00Z")))
        assertFalse(NotificationTiming.inLiveWindow(null, 10))
        assertFalse(NotificationTiming.inLiveWindow("invalid", 10))
    }
}
