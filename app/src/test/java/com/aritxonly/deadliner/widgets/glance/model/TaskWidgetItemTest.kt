package com.aritxonly.deadliner.widgets.glance.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskWidgetItemTest {
    @Test
    fun `category color overrides semantic indicator color`() {
        val item = item(categoryColor = 0xFF10B981.toInt())

        assertEquals(0xFF10B981.toInt(), item.indicatorColorArgb(0xFFEF4444.toInt()))
    }

    @Test
    fun `uncategorized item uses semantic indicator color`() {
        assertEquals(0xFFF59E0B.toInt(), item(null).indicatorColorArgb(0xFFF59E0B.toInt()))
    }

    private fun item(categoryColor: Int?) = TaskWidgetItem(
        stableId = 1L,
        title = "Task",
        timeDescription = "1h",
        status = TaskWidgetStatus.NEAR,
        categoryColorArgb = categoryColor,
    )
}
