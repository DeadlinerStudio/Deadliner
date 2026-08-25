package com.aritxonly.deadliner.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTapUnlockCounterTest {
    @Test
    fun unlocksOnlyOnTenthVersionTap() {
        val counter = VersionTapUnlockCounter()

        repeat(9) {
            assertFalse(counter.registerTap())
        }
        assertTrue(counter.registerTap())
        assertTrue(counter.registerTap())
    }
}
