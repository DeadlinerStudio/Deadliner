package com.aritxonly.deadliner.widgets.glance.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskWidgetPaddingPresetTest {
    @Test
    fun `presets merge reference vertical padding into top only`() {
        assertEquals(0, TaskWidgetPaddingPreset.Standard.extraTopPaddingDp)
        assertEquals(26, TaskWidgetPaddingPreset.Grid4x6.extraTopPaddingDp)
        assertEquals(18, TaskWidgetPaddingPreset.Grid4x7.extraTopPaddingDp)
    }

    @Test
    fun `unknown stored values fall back to standard`() {
        assertEquals(
            TaskWidgetPaddingPreset.Standard,
            TaskWidgetPaddingPreset.fromStoredValue(Int.MAX_VALUE),
        )
    }
}
