package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassTintCompositeTest {
    @Test
    fun neutralTintKeepsSingleSurfaceWash() {
        val tint = Color(0x99FF6D6D)
        val composite = resolveGlassTintComposite(
            tint = tint,
            style = GlassTintStyle.Neutral,
            isDark = true,
        )

        assertEquals(tint, composite.baseTint)
        assertEquals(Color.Transparent, composite.colorizeTint)
        assertEquals(Color.Transparent, composite.liftTint)
    }

    @Test
    fun chromaticTintUsesTransparentColorAndLiftLayers() {
        val tint = Color(0xFFFF6D6D)
        val composite = resolveGlassTintComposite(
            tint = tint,
            style = GlassTintStyle.Chromatic,
            isDark = true,
        )

        assertTrue(composite.baseTint.alpha < 0.5f)
        assertTrue(composite.colorizeTint.alpha > composite.baseTint.alpha)
        assertTrue(composite.liftTint.alpha > 0f)
        assertTrue(composite.colorizeTint.red >= tint.red)
        assertTrue(composite.colorizeTint.red > composite.colorizeTint.blue)
    }

    @Test
    fun darkChromaticGlassGetsMoreLiftThanLightGlass() {
        val tint = Color(0xFFFF6D6D)
        val dark = resolveGlassTintComposite(tint, GlassTintStyle.Chromatic, isDark = true)
        val light = resolveGlassTintComposite(tint, GlassTintStyle.Chromatic, isDark = false)

        assertTrue(dark.liftTint.alpha > light.liftTint.alpha)
        assertTrue(dark.fallbackTint.alpha > light.fallbackTint.alpha)
    }

    @Test
    fun seedChromaticGlassKeepsSeedRgbAtAboutSeventyFivePercentEffectiveCoverage() {
        val tint = Color(0xFF3382FF)
        val balanced = resolveGlassTintComposite(tint, GlassTintStyle.Chromatic, isDark = false)
        val lightSeed = resolveGlassTintComposite(tint, GlassTintStyle.SeedChromatic, isDark = false)
        val darkSeed = resolveGlassTintComposite(tint, GlassTintStyle.SeedChromatic, isDark = true)

        listOf(lightSeed, darkSeed).forEach { seed ->
            assertEquals(tint.red, seed.baseTint.red, 0.0001f)
            assertEquals(tint.green, seed.baseTint.green, 0.0001f)
            assertEquals(tint.blue, seed.baseTint.blue, 0.0001f)
            assertEquals(0.835f, seed.baseTint.alpha, 0.002f)
            assertEquals(0.86f, seed.colorizeTint.alpha, 0.002f)
            assertEquals(0.96f, seed.fallbackTint.alpha, 0.002f)
            assertEquals(0.7515f, seed.baseTint.alpha * 0.90f, 0.002f)
            assertTrue(seed.baseTint.alpha > balanced.baseTint.alpha)
            assertTrue(seed.baseTint.alpha < 1f)
            assertTrue(seed.colorizeTint.alpha > seed.baseTint.alpha)
            assertEquals(0f, seed.liftTint.alpha, 0.0001f)
            assertTrue(seed.fallbackTint.alpha < 1f)
        }
    }
}
