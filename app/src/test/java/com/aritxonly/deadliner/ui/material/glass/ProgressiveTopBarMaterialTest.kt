package com.aritxonly.deadliner.ui.material.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressiveTopBarMaterialTest {
    @Test
    fun overlayKeepsDefaultSurfaceContrastAndBlurRadiusIsDeepened() {
        assertEquals(8f, ProgressiveTopBarBlurRadiusDp, 0.0001f)
        assertEquals(0.94f, ProgressiveTopBarOverlayTopAlpha, 0.0001f)
        assertEquals(0.80f, ProgressiveTopBarOverlayEndFraction, 0.0001f)
        assertEquals(0.75f, ProgressiveTopBarContentEndFraction, 0.0001f)
    }

    @Test
    fun measuredTopBarHeightIncludesTabsAndReservesFadeTail() {
        val metrics = resolveProgressiveTopBarHeightMetrics(
            measuredTopBarHeightPx = 130,
            statusBarHeightPx = 24,
            minimumContentHeightPx = 56,
            materialHeightTrimPx = 30,
        )

        assertEquals(130, metrics.occupiedHeightPx)
        assertEquals(144, metrics.materialHeightPx)
        assertTrue(metrics.materialHeightPx > metrics.occupiedHeightPx)
    }

    @Test
    fun shortTopBarUsesStatusBarPlusMinimumContentHeight() {
        val metrics = resolveProgressiveTopBarHeightMetrics(
            measuredTopBarHeightPx = 64,
            statusBarHeightPx = 24,
            minimumContentHeightPx = 56,
            materialHeightTrimPx = 30,
        )

        assertEquals(80, metrics.occupiedHeightPx)
        assertEquals(80, metrics.materialHeightPx)
    }

    @Test
    fun progressiveMaterialKeepsOriginalHeightTrim() {
        assertEquals(6f, ProgressiveTopBarMaterialHeightTrimDp, 0.0001f)
        assertEquals(12f, ProgressiveTopBarBottomClearanceDp, 0.0001f)
    }

    @Test
    fun progressiveMaterialOnlyRevealsForAnObservedScrollPosition() {
        assertEquals(0f, resolveProgressiveTopBarMaterialProgress(null), 0.0001f)
        assertEquals(0f, resolveProgressiveTopBarMaterialProgress(0f), 0.0001f)
        assertEquals(0.5f, resolveProgressiveTopBarMaterialProgress(0.5f), 0.0001f)
        assertEquals(1f, resolveProgressiveTopBarMaterialProgress(2f), 0.0001f)
    }

    @Test
    fun emptyTopBarDoesNotCreateMaterial() {
        val metrics = resolveProgressiveTopBarHeightMetrics(
            measuredTopBarHeightPx = 0,
            statusBarHeightPx = 24,
            minimumContentHeightPx = 56,
        )

        assertEquals(0, metrics.occupiedHeightPx)
        assertEquals(0, metrics.materialHeightPx)
    }
}
