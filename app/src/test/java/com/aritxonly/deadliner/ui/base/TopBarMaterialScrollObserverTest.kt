package com.aritxonly.deadliner.ui.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopBarMaterialScrollObserverTest {
    @Test
    fun progressStaysHiddenUntilRevealThreshold() {
        assertEquals(0f, resolveTopBarScrollProgress(12f, 12f, 48f), 0.0001f)
    }

    @Test
    fun progressSmoothlyRevealsAcrossScrollRange() {
        val progress = resolveTopBarScrollProgress(30f, 12f, 48f)

        assertTrue(progress > 0f)
        assertTrue(progress < 1f)
        assertEquals(1f, resolveTopBarScrollProgress(48f, 12f, 48f), 0.0001f)
    }

    @Test
    fun invalidRangeFallsBackToDiscreteThreshold() {
        assertEquals(0f, resolveTopBarScrollProgress(7f, 8f, 8f), 0.0001f)
        assertEquals(1f, resolveTopBarScrollProgress(8f, 8f, 8f), 0.0001f)
    }
}
