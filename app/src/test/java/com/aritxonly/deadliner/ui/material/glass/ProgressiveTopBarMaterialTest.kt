package com.aritxonly.deadliner.ui.material.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressiveTopBarMaterialTest {
    @Test
    fun backdropPathRequiresBackdropAndRuntimeShader() {
        assertEquals(
            ProgressiveTopBarRenderMode.Backdrop,
            resolveProgressiveTopBarRenderMode(
                backdropAvailable = true,
                runtimeShaderSupported = true,
            ),
        )
        assertEquals(
            ProgressiveTopBarRenderMode.GradientFallback,
            resolveProgressiveTopBarRenderMode(
                backdropAvailable = false,
                runtimeShaderSupported = true,
            ),
        )
        assertEquals(
            ProgressiveTopBarRenderMode.GradientFallback,
            resolveProgressiveTopBarRenderMode(
                backdropAvailable = true,
                runtimeShaderSupported = false,
            ),
        )
    }

    @Test
    fun blurRemainsOpaqueUntilItsFadeStart() {
        assertEquals(
            1f,
            progressiveTopBarFadeAlpha(
                verticalFraction = ProgressiveTopBarBlurFadeStart,
                fadeStartFraction = ProgressiveTopBarBlurFadeStart,
            ),
            0.0001f,
        )
    }

    @Test
    fun fadeUsesSmoothstepAndEndsTransparent() {
        val fadeMidpoint = ProgressiveTopBarBlurFadeStart +
            (1f - ProgressiveTopBarBlurFadeStart) / 2f

        assertEquals(
            0.5f,
            progressiveTopBarFadeAlpha(
                verticalFraction = fadeMidpoint,
                fadeStartFraction = ProgressiveTopBarBlurFadeStart,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            progressiveTopBarFadeAlpha(
                verticalFraction = 1f,
                fadeStartFraction = ProgressiveTopBarBlurFadeStart,
            ),
            0.0001f,
        )
    }

    @Test
    fun overlayUsesStrongerSurfaceContrastAndEndsBeforeThreeQuarters() {
        assertEquals(0.94f, ProgressiveTopBarOverlayTopAlpha, 0.0001f)
        assertEquals(0.80f, ProgressiveTopBarOverlayEndFraction, 0.0001f)
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
    fun progressiveMaterialAddsTwentyFourDpBottomClearanceToExistingTrim() {
        assertEquals(6f, ProgressiveTopBarMaterialHeightTrimDp, 0.0001f)
        assertEquals(12f, ProgressiveTopBarBottomClearanceDp, 0.0001f)
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
