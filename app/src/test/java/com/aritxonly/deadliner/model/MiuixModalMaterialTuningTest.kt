package com.aritxonly.deadliner.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MiuixModalMaterialTuningTest {
    @Test
    fun resolvesIndependentDialogAndSheetValues() {
        val tuning = AdvancedMaterialFineTuning.Default.copy(
            dialogBackgroundAlpha = 0.51f,
            dialogBlurRadiusDp = 40f,
            bottomSheetBackgroundAlpha = 0.59f,
            bottomSheetBlurRadiusDp = 57f,
        )

        assertEquals(
            MiuixModalMaterialTuning(0.51f, 40f),
            tuning.miuixModalMaterial(MiuixModalKind.Dialog),
        )
        assertEquals(
            MiuixModalMaterialTuning(0.59f, 57f),
            tuning.miuixModalMaterial(MiuixModalKind.BottomSheet),
        )
    }

    @Test
    fun resolverUsesKindSpecificFallbacksAfterNormalization() {
        val tuning = AdvancedMaterialFineTuning.Default.copy(
            dialogBackgroundAlpha = Float.NaN,
            dialogBlurRadiusDp = Float.NEGATIVE_INFINITY,
            bottomSheetBackgroundAlpha = Float.POSITIVE_INFINITY,
            bottomSheetBlurRadiusDp = Float.NaN,
        )

        assertEquals(
            MiuixModalMaterialTuning(0.65f, 80f),
            tuning.miuixModalMaterial(MiuixModalKind.Dialog),
        )
        assertEquals(
            MiuixModalMaterialTuning(0.70f, 80f),
            tuning.miuixModalMaterial(MiuixModalKind.BottomSheet),
        )
    }
}
