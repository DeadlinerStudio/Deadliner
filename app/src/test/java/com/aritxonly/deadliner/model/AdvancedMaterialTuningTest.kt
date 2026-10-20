package com.aritxonly.deadliner.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AdvancedMaterialTuningTest {
    @Test
    fun saturationPassesThroughLegacyPresetNodes() {
        AdvancedMaterialLevel.entries.forEach { level ->
            assertEquals(
                level.blurSaturation,
                AdvancedMaterialTuning.saturationForBlurRadius(level.blurRadius),
                0.0001f,
            )
        }
    }

    @Test
    fun defaultRadiusMatchesPinnedPreset() {
        assertEquals(84f, AdvancedMaterialTuning.DefaultBlurRadius, 0.0001f)
        assertEquals(
            0.962f,
            AdvancedMaterialTuning.saturationForBlurRadius(AdvancedMaterialTuning.DefaultBlurRadius),
            0.0001f,
        )
    }

    @Test
    fun saturationInterpolatesBetweenPresetNodes() {
        assertEquals(1.01f, AdvancedMaterialTuning.saturationForBlurRadius(43f), 0.0001f)
        assertEquals(0.99f, AdvancedMaterialTuning.saturationForBlurRadius(62f), 0.0001f)
    }

    @Test
    fun extendedRangeDoesNotJumpBackToClearGlassSaturation() {
        for (radius in listOf(92f, 93f, 150f, 168f)) {
            assertEquals(0.95f, AdvancedMaterialTuning.saturationForBlurRadius(radius), 0.0001f)
        }
    }

    @Test
    fun radiusIsNormalizedBeforeInterpolation() {
        assertEquals(16f, AdvancedMaterialTuning.normalizeBlurRadius(-10f), 0.0001f)
        assertEquals(168f, AdvancedMaterialTuning.normalizeBlurRadius(400f), 0.0001f)
        assertEquals(84f, AdvancedMaterialTuning.normalizeBlurRadius(Float.NaN), 0.0001f)
    }
}
