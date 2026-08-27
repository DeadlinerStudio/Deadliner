package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopBarMaterialScrollObserverTest {
    @Test
    fun progressStaysHiddenUntilRevealThreshold() {
        assertEquals(4.dp, TopBarMaterialRevealStartDistance)
        assertEquals(28.dp, TopBarMaterialRevealEndDistance)
        assertEquals(0f, resolveTopBarScrollProgress(4f, 4f, 28f), 0.0001f)
    }

    @Test
    fun progressSmoothlyRevealsAcrossScrollRange() {
        val progress = resolveTopBarScrollProgress(16f, 4f, 28f)

        assertTrue(progress > 0f)
        assertTrue(progress < 1f)
        assertEquals(1f, resolveTopBarScrollProgress(28f, 4f, 28f), 0.0001f)
    }

    @Test
    fun invalidRangeFallsBackToDiscreteThreshold() {
        assertEquals(0f, resolveTopBarScrollProgress(7f, 8f, 8f), 0.0001f)
        assertEquals(1f, resolveTopBarScrollProgress(8f, 8f, 8f), 0.0001f)
    }
}
