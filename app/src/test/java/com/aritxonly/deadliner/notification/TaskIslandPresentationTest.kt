package com.aritxonly.deadliner.notification

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskIslandPresentationTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private fun label(due: String, now: String) = TaskIslandPresentation.deadline(due,
        Instant.parse(now), zone) { "明天$it" }

    @Test fun sameDayShowsAbsoluteLocalDeadline() {
        assertEquals("22:30", label("2026-10-07T14:30:00Z", "2026-10-07T06:42:00Z"))
    }
    @Test fun tomorrowUsesCalendarDayAcrossMonthAndYearBoundaries() {
        assertEquals("明天00:05", label("2027-01-01T00:05:00+08:00", "2026-12-31T15:59:00Z"))
        assertEquals("明天09:00", label("2026-11-01T09:00:00", "2026-10-31T06:42:00Z"))
    }
    @Test fun distantDeadlineHasDateAndMidnightReformatsTomorrow() {
        assertEquals("10-09 09:00", label("2026-10-09T09:00:00+08:00", "2026-10-07T06:42:00Z"))
        assertEquals("00:05", label("2026-10-08T00:05:00+08:00", "2026-10-07T16:00:00Z"))
    }
}
