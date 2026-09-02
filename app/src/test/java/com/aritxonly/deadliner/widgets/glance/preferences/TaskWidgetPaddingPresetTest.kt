package com.aritxonly.deadliner.widgets.glance.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskWidgetPaddingPresetTest {
    @Test
    fun `presets expose whole card top margin`() {
        assertEquals(0, TaskWidgetPaddingPreset.Standard.cardTopMarginDp)
        assertEquals(18, TaskWidgetPaddingPreset.Grid4x6.cardTopMarginDp)
        assertEquals(12, TaskWidgetPaddingPreset.Grid4x7.cardTopMarginDp)
        assertEquals(0, TaskWidgetPaddingPreset.Grid4x6.cardBottomMarginDp)
        assertEquals(0, TaskWidgetPaddingPreset.Grid4x7.cardBottomMarginDp)
    }

    @Test
    fun `unknown stored values fall back to standard`() {
        assertEquals(
            TaskWidgetPaddingPreset.Standard,
            TaskWidgetPaddingPreset.fromStoredValue(Int.MAX_VALUE),
        )
    }
}
