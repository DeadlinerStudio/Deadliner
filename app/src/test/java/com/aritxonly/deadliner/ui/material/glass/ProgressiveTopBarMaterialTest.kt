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
    fun overlayRemainsSubtleAndFadesMonotonically() {
        assertTrue(ProgressiveTopBarOverlayTopAlpha < 0.2f)
        assertTrue(ProgressiveTopBarOverlayTopAlpha > ProgressiveTopBarOverlayMiddleAlpha)
        assertTrue(ProgressiveTopBarOverlayMiddleAlpha > ProgressiveTopBarOverlayTailAlpha)
        assertTrue(ProgressiveTopBarOverlayTailAlpha > 0f)
        assertTrue(ProgressiveTopBarOverlayMiddleFraction < ProgressiveTopBarOverlayTailFraction)
    }
}
