package com.aritxonly.deadliner.ui.material.glass

import org.junit.Assert.assertTrue
import org.junit.Test

class GlassSamplingTest {
    @Test fun blurKernelAndLensStayInsideTheCaptureAtEverySupportedRadius() {
        for (radius in listOf(0f, 8f, 32f, 80f, 160f)) {
            val padding = glassEffectPadding(radius, radius / 2f, 0.5f, 18f, 1f)
            // Even the furthest refracted/colour-separated sample has a complete blur kernel.
            assertTrue(padding - 18f - 1f - 1.5f >= radius * 3f)
        }
    }

    @Test fun anisotropicBlurReservesTheLargerAxis() {
        assertTrue(glassEffectPadding(2f, 80f, 0f, 0f, 0f) >= 240f)
    }
}
