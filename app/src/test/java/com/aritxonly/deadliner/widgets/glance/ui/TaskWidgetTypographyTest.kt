package com.aritxonly.deadliner.widgets.glance.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskWidgetTypographyTest {
    @Test
    fun `primary widget text uses an explicit medium system family`() {
        assertEquals("sans-serif-medium", TaskWidgetTypography.EMPHASIS_FONT_FAMILY.family)
    }

    @Test
    fun `supporting text stays subordinate to primary text`() {
        assertEquals("sans-serif", TaskWidgetTypography.SUPPORTING_FONT_FAMILY.family)
        assertTrue(
            TaskWidgetTypography.CAPSULE_SUMMARY_SIZE_SP >
                TaskWidgetTypography.CAPSULE_SUPPORTING_SIZE_SP,
        )
    }
}
