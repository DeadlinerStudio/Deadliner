package com.aritxonly.deadliner.widgets.glance.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TaskWidgetLayoutMetricsTest {
    @Test
    fun `static widget renders no more than three tasks`() {
        assertEquals(0, TaskWidgetLayoutMetrics.staticVisibleTaskCount(0))
        assertEquals(2, TaskWidgetLayoutMetrics.staticVisibleTaskCount(2))
        assertEquals(3, TaskWidgetLayoutMetrics.staticVisibleTaskCount(3))
        assertEquals(3, TaskWidgetLayoutMetrics.staticVisibleTaskCount(12))
    }

    @Test
    fun `negative task count is safely treated as empty`() {
        assertEquals(0, TaskWidgetLayoutMetrics.staticVisibleTaskCount(-1))
        assertEquals(0, TaskWidgetLayoutMetrics.scrollableVisibleTaskCount(-1))
    }

    @Test
    fun `scrollable widget bounds rows in one update`() {
        assertEquals(0, TaskWidgetLayoutMetrics.scrollableVisibleTaskCount(0))
        assertEquals(12, TaskWidgetLayoutMetrics.scrollableVisibleTaskCount(12))
        assertEquals(
            TaskWidgetLayoutMetrics.SCROLLABLE_TASK_LIMIT,
            TaskWidgetLayoutMetrics.scrollableVisibleTaskCount(1_000),
        )
    }

    @Test
    fun `short row titles are unchanged`() {
        val title = "a".repeat(TaskWidgetLayoutMetrics.ROW_TITLE_CHAR_LIMIT)
        assertEquals(title, TaskWidgetLayoutMetrics.boundedRowTitle(title))
        assertEquals("", TaskWidgetLayoutMetrics.boundedRowTitle(""))
    }

    @Test
    fun `long row titles are truncated with an ellipsis`() {
        val limit = TaskWidgetLayoutMetrics.ROW_TITLE_CHAR_LIMIT
        val bounded = TaskWidgetLayoutMetrics.boundedRowTitle("b".repeat(10_000))
        assertEquals("b".repeat(limit) + "…", bounded)
    }

    @Test
    fun `row title truncation never splits a surrogate pair`() {
        val limit = TaskWidgetLayoutMetrics.ROW_TITLE_CHAR_LIMIT
        val title = "c".repeat(limit - 1) + "😀" + "tail"
        val bounded = TaskWidgetLayoutMetrics.boundedRowTitle(title)
        assertEquals("c".repeat(limit - 1) + "…", bounded)
        assertFalse(Character.isHighSurrogate(bounded[bounded.length - 2]))
    }
}
