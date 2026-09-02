package com.aritxonly.deadliner.widgets.glance.data

import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetVisualState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TaskWidgetRepositoryTest {
    private val now = LocalDateTime.parse("2026-08-16T12:00:00")
    private val strings = FakeStrings()

    @Test
    fun `filters active records and sorts missing deadlines last`() {
        val snapshot = repository(
            record("No deadline", null),
            record("Later", "2026-08-19T12:00:00"),
            record("Soon", "2026-08-16T14:00:00"),
            record("Deleted", "2026-08-16T13:00:00", isDeleted = true),
            record("Completed", "2026-08-16T13:00:00", isActive = false),
        ).load(strings)

        assertEquals(3, snapshot.totalCount)
        assertEquals(listOf("Soon", "Later", "No deadline"), snapshot.items.map { it.title })
        assertEquals("unscheduled", snapshot.items.last().timeDescription)
    }

    @Test
    fun `limits content to three rows while preserving total count`() {
        val snapshot = repository(
            record("One", "2026-08-17T12:00:00"),
            record("Two", "2026-08-18T12:00:00"),
            record("Three", "2026-08-19T12:00:00"),
            record("Four", "2026-08-20T12:00:00"),
        ).load(strings)

        assertEquals(4, snapshot.totalCount)
        assertEquals(listOf("One", "Two", "Three"), snapshot.items.map { it.title })
    }

    @Test
    fun `calculates overdue near and normal states from one day window`() {
        val snapshot = repository(
            record("Overdue", "2026-08-16T11:59:00"),
            record("Near", "2026-08-17T12:00:00"),
            record("Normal", "2026-08-17T12:01:00"),
        ).load(strings)

        assertEquals(1, snapshot.overdueCount)
        assertEquals(1, snapshot.nearCount)
        assertEquals(TaskWidgetVisualState.OVERDUE, snapshot.visualState)
        assertEquals(
            listOf(TaskWidgetStatus.OVERDUE, TaskWidgetStatus.NEAR, TaskWidgetStatus.NORMAL),
            snapshot.items.map { it.status },
        )
        assertEquals("overdue-summary", snapshot.summary)
    }

    @Test
    fun `formats relative time with reference floor semantics`() {
        val firstPage = repository(
            record("Minutes", "2026-08-16T12:45:00"),
            record("Hours", "2026-08-16T13:59:00"),
            record("Days", "2026-08-18T23:59:00"),
        ).load(strings)

        assertEquals(listOf("45m", "1h", "2d"), firstPage.items.map { it.timeDescription })
    }

    @Test
    fun `summary precedence is empty then overdue then near then pending`() {
        assertEquals("empty-summary", repository().load(strings).summary)
        assertEquals(
            "near-summary",
            repository(record("Near", "2026-08-16T13:00:00")).load(strings).summary,
        )
        assertEquals(
            "pending-summary",
            repository(record("Later", "2026-08-20T12:00:00")).load(strings).summary,
        )
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
        isDeleted: Boolean = false,
        isActive: Boolean = true,
    ) = TaskWidgetTaskRecord(
        title = title,
        dueAt = dueAt,
        isDeleted = isDeleted,
        isActive = isActive,
    )

    private class FakeStrings : TaskWidgetStrings {
        override fun overdue(): String = "overdue"
        override fun dueInMinutes(minutes: Long): String = "${minutes}m"
        override fun dueInHours(hours: Long): String = "${hours}h"
        override fun dueInDays(days: Long): String = "${days}d"
        override fun unscheduled(): String = "unscheduled"
        override fun emptySummary(): String = "empty-summary"
        override fun overdueSummary(): String = "overdue-summary"
        override fun nearSummary(): String = "near-summary"
        override fun pendingSummary(): String = "pending-summary"
    }
}
