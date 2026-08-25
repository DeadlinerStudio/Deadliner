package com.aritxonly.deadliner.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AdvancedMaterialFineTuningTest {
    @Test
    fun defaultsRemainStableAfterNormalization() {
        assertEquals(
            AdvancedMaterialFineTuning.Default,
            AdvancedMaterialFineTuning.Default.normalized(),
        )
        assertEquals(1.05f, AdvancedMaterialFineTuning.Default.saturationMultiplier, 0.0001f)
        assertEquals(0.095f, AdvancedMaterialFineTuning.Default.noiseCoefficient, 0.0001f)
        assertEquals(0.25f, AdvancedMaterialFineTuning.Default.glassBlurRadiusMultiplier, 0.0001f)
        assertEquals(0.60f, AdvancedMaterialFineTuning.Default.refractionDepthEffect, 0.0001f)
        assertEquals(0.95f, AdvancedMaterialFineTuning.Default.edgeHighlightAlpha, 0.0001f)
        assertEquals(0.20f, AdvancedMaterialFineTuning.Default.darkHighlightMultiplier, 0.0001f)
        assertEquals(0.46f, AdvancedMaterialFineTuning.Default.fallbackLightAlpha, 0.0001f)
        assertEquals(0.08f, AdvancedMaterialFineTuning.Default.fallbackDarkAlpha, 0.0001f)
    }

    @Test
    fun normalizationClampsEveryNumericFamily() {
        val normalized = AdvancedMaterialFineTuning(
            saturationMultiplier = Float.NaN,
            noiseCoefficient = 2f,
            brightness = -2f,
            contrast = 4f,
            glassBlurRadiusMultiplier = 0f,
            glassTintLightGray = 2f,
            glassTintDarkGray = -1f,
            refractionEnabled = false,
            refractionHeightDp = 100f,
            chromaticAberrationDp = -1f,
            edgeHighlightAlpha = 2f,
            edgeHighlightGray = Float.NaN,
            darkHighlightMultiplier = -1f,
            fallbackEdgeWidthDp = 5f,
            postRefractionBlurRadiusDp = Float.POSITIVE_INFINITY,
        ).normalized()

        assertEquals(1.05f, normalized.saturationMultiplier, 0.0001f)
        assertEquals(0.20f, normalized.noiseCoefficient, 0.0001f)
        assertEquals(-0.20f, normalized.brightness, 0.0001f)
        assertEquals(1.25f, normalized.contrast, 0.0001f)
        assertEquals(0.10f, normalized.glassBlurRadiusMultiplier, 0.0001f)
        assertEquals(1f, normalized.glassTintLightGray, 0.0001f)
        assertEquals(0f, normalized.glassTintDarkGray, 0.0001f)
        assertFalse(normalized.refractionEnabled)
        assertEquals(32f, normalized.refractionHeightDp, 0.0001f)
        assertEquals(0f, normalized.chromaticAberrationDp, 0.0001f)
        assertEquals(1f, normalized.edgeHighlightAlpha, 0.0001f)
        assertEquals(1f, normalized.edgeHighlightGray, 0.0001f)
        assertEquals(0.10f, normalized.darkHighlightMultiplier, 0.0001f)
        assertEquals(2f, normalized.fallbackEdgeWidthDp, 0.0001f)
        assertEquals(0.5f, normalized.postRefractionBlurRadiusDp, 0.0001f)
    }
}
