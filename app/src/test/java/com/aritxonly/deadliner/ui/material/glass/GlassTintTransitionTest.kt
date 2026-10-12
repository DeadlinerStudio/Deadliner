package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class GlassTintTransitionTest {
    @Test fun neutralTransitionPreservesBothOpticalAndFallbackEndpoints() {
        val seed = resolveGlassTintComposite(Color.Red, GlassTintStyle.SeedChromatic, true)
        val neutral = resolveGlassTintComposite(Color.Gray.copy(alpha = 0.6f), GlassTintStyle.Neutral, true)
        assertEquals(seed, blendGlassTintComposites(seed, neutral, 0f))
        assertEquals(neutral, blendGlassTintComposites(seed, neutral, 1f))
        val middle = blendGlassTintComposites(seed, neutral, 0.5f)
        assertTrue(middle.colorizeTint.alpha < seed.colorizeTint.alpha)
        assertTrue(middle.colorizeTint.alpha > 0f)
        assertNotEquals(seed.fallbackTint, middle.fallbackTint)
        assertNotEquals(neutral.fallbackTint, middle.fallbackTint)
    }
}
