package com.aritxonly.deadliner.widgets.glance.ui

import org.junit.Assert.assertEquals
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
    }
}
