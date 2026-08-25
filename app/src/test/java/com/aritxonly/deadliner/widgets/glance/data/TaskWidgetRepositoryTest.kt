package com.aritxonly.deadliner.widgets.glance.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TaskWidgetRepositoryTest {
    private val now = LocalDateTime.parse("2026-08-16T12:00:00")
    private val strings = FakeStrings()

    @Test
    fun `filters KMP records and prioritizes starred tasks before due time`() {
        val repository = repository(
            record("Soon", "2026-08-16T14:00:00"),
            record("Starred", "2026-08-19T12:00:00", isStarred = true),
            record("Deleted", "2026-08-16T13:00:00", isDeleted = true),
            record("Completed", "2026-08-16T13:00:00", isActive = false),
            record("No due time", null),
        )

        val snapshot = repository.load(strings)

        assertEquals(2, snapshot.totalCount)
        assertEquals(listOf("Starred", "Soon"), snapshot.items.map { it.title })
        assertFalse(snapshot.items[0].isUrgent)
        assertTrue(snapshot.items[1].isUrgent)
    }

    @Test
    fun `limits content to three rows while preserving total count`() {
        val repository = repository(
            record("One", "2026-08-17T12:00:00"),
            record("Two", "2026-08-18T12:00:00"),
            record("Three", "2026-08-19T12:00:00"),
            record("Four", "2026-08-20T12:00:00"),
        )

        val snapshot = repository.load(strings)

        assertEquals(4, snapshot.totalCount)
        assertEquals(listOf("One", "Two", "Three"), snapshot.items.map { it.title })
    }

    @Test
    fun `formats overdue minute hour day and date ranges`() {
        val repository = repository(
            record("Overdue", "2026-08-16T11:59:00", isStarred = true),
            record("Minutes", "2026-08-16T12:45:00"),
            record("Hours", "2026-08-16T13:01:00"),
            record("Days", "2026-08-18T12:01:00"),
            record("Date", "2026-08-24T12:00:00"),
        )

        val firstPage = repository.load(strings)
        assertEquals(listOf("overdue", "45m", "2h"), firstPage.items.map { it.timeDescription })

        val laterRepository = repository(
            record("Days", "2026-08-18T12:01:00"),
            record("Date", "2026-08-24T12:00:00"),
        )
        assertEquals(listOf("3d", "8/24"), laterRepository.load(strings).items.map { it.timeDescription })
    }

    @Test
    fun `parses offset KMP timestamps in the supplied zone`() {
        val source = TaskWidgetDataSource {
            listOf(record("UTC", "2026-08-16T05:00:00Z"))
        }
        val repository = TaskWidgetRepository(
            source = source,
            zoneId = ZoneId.of("Asia/Shanghai"),
            nowProvider = { now },
        )

        assertEquals("1h", repository.load(strings).items.single().timeDescription)
    }

    private fun repository(vararg records: TaskWidgetTaskRecord): TaskWidgetRepository =
        TaskWidgetRepository(
            source = TaskWidgetDataSource { records.toList() },
            zoneId = ZoneId.of("Asia/Shanghai"),
            nowProvider = { now },
        )

    private fun record(
        title: String,
        dueAt: String?,
        isStarred: Boolean = false,
        isDeleted: Boolean = false,
        isActive: Boolean = true,
    ) = TaskWidgetTaskRecord(
        title = title,
        dueAt = dueAt,
        isStarred = isStarred,
        isDeleted = isDeleted,
        isActive = isActive,
    )

    private class FakeStrings : TaskWidgetStrings {
        override fun overdue(): String = "overdue"
        override fun dueInMinutes(minutes: Long): String = "${minutes}m"
        override fun dueInHours(hours: Long): String = "${hours}h"
        override fun dueInDays(days: Long): String = "${days}d"
        override fun dueOn(month: Int, day: Int): String = "$month/$day"
    }
}
