package com.aritxonly.deadliner.ui.focus

import org.junit.Assert.*
import org.junit.Test

class FocusResponsiveLayoutTest {
    @Test fun narrowAndLargeTextWindowsKeepReadableMinimumWidths() {
        assertEquals(1, focusLayout(288f, 1f).summaryColumns)
        assertEquals(2, focusLayout(328f, 1f).summaryColumns)
        assertEquals(1, focusLayout(328f, 1.5f).summaryColumns)
        for (width in listOf(240f, 288f, 328f, 540f, 728f, 920f, 1200f)) {
            for (font in listOf(1f, 1.3f, 1.6f, 2f)) {
                val layout = focusLayout(width, font)
                if (layout.summaryColumns == 2) assertTrue((width - 12) / 2 >= 150 * font)
                if (layout.historyColumns == 2) assertTrue((width - 12) / 2 >= 330 * font)
            }
        }
    }
    @Test fun wideLayoutsReflowTheirOwnResponsibilities() {
        assertFalse(focusLayout(728f, 1f).dashboardWide)
        assertTrue(focusLayout(920f, 1f).dashboardWide)
        assertEquals(2, focusLayout(728f, 1f).historyColumns)
        assertTrue(focusLayout(540f, 1f).setupWide)
        assertFalse(focusLayout(540f, 1f).sessionWide)
        assertTrue(focusLayout(640f, 1f).sessionWide)
        assertFalse(focusLayout(640f, 1.5f).sessionWide)
    }
}
