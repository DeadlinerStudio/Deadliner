package com.aritxonly.deadliner.widgets.glance.theme

import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskWidgetColorsTest {
    @Test
    fun `row background preserves base rgb and uses lightweight transparency`() {
        val colors = colors(normalBase = 0xFF123456.toInt())

        val background = colors.rowBackgroundColor(TaskWidgetStatus.NORMAL)

        assertEquals(0xD9, background ushr 24)
        assertEquals(0x00123456, background and 0x00FFFFFF)
    }

    private fun colors(normalBase: Int): TaskWidgetColors {
        val normal = row(base = normalBase)
        return TaskWidgetColors(
            isDark = false,
            backgroundBase = 0,
            backgroundTint = 0,
            backgroundGlow = 0,
            backgroundHighlight = 0,
            decoration = 0,
            badgeBackground = 0,
            brand = 0,
            brandSoft = 0,
            near = 0,
            overdue = 0,
            completed = 0,
            textPrimary = 0,
            textSecondary = 0,
            normalRow = normal,
            nearRow = row(base = 0),
            overdueRow = row(base = 0),
        )
    }

    private fun row(base: Int) = TaskWidgetRowColors(
        base = base,
        tint = 0,
        highlight = 0,
        indicator = 0,
        timeText = 0,
    )
}
