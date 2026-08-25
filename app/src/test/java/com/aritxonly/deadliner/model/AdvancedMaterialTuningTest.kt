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
        assertEquals(92f, AdvancedMaterialTuning.DefaultBlurRadius, 0.0001f)
        assertEquals(
            1.0105883f,
            AdvancedMaterialTuning.saturationForBlurRadius(AdvancedMaterialTuning.DefaultBlurRadius),
            0.0001f,
        )
    }

    @Test
    fun saturationInterpolatesBetweenPresetNodes() {
        assertEquals(1.01f, AdvancedMaterialTuning.saturationForBlurRadius(94f), 0.0001f)
        assertEquals(0.99f, AdvancedMaterialTuning.saturationForBlurRadius(162f), 0.0001f)
    }

    @Test
    fun radiusIsNormalizedBeforeInterpolation() {
        assertEquals(28f, AdvancedMaterialTuning.normalizeBlurRadius(-10f), 0.0001f)
        assertEquals(256f, AdvancedMaterialTuning.normalizeBlurRadius(400f), 0.0001f)
        assertEquals(92f, AdvancedMaterialTuning.normalizeBlurRadius(Float.NaN), 0.0001f)
    }
}
