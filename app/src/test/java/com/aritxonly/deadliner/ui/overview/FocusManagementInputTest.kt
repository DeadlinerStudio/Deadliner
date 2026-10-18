package com.aritxonly.deadliner.ui.overview

import org.junit.Assert.*
import org.junit.Test

class FocusManagementInputTest {
    @Test fun respectsSecondsAndAlreadyAdjustedMaximum() {
        assertEquals(120L, focusManagementInputSeconds("2", "0", 120))
        assertNull(focusManagementInputSeconds("2", "1", 120))
        assertNull(focusManagementInputSeconds("1", "60", 120))
        assertEquals(1L, focusManagementInputSeconds("0", "1", 120))
        assertNull(focusManagementInputSeconds("0", "0", 120))
    }
    @Test fun rejectsFractionsNegativesAndOverflow() {
        listOf("-1", "1.5", "", "9223372036854775807", "9223372036854775808").forEach {
            assertNull(focusManagementInputSeconds(it, "0", 86400))
        }
    }
}
