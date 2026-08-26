package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class SoftGlassPressFeedbackTest {
    @Test
    fun floatingActionButtonUsesExpandedImmersiveSize() {
        assertEquals(58.dp, SoftGlassFloatingActionButtonDefaults.Size)
        assertEquals(30.dp, SoftGlassFloatingActionButtonDefaults.IconSize)
    }

    @Test
    fun pressFeedbackInterpolatesAcrossWholeMaterial() {
        assertEquals(1f, softGlassPressScale(0f), 0.0001f)
        assertEquals(0.97f, softGlassPressScale(0.5f), 0.0001f)
        assertEquals(0.94f, softGlassPressScale(1f), 0.0001f)
        assertEquals(0.07f, softGlassPressHighlightAlpha(1f, isDark = false), 0.0001f)
        assertEquals(0.11f, softGlassPressHighlightAlpha(1f, isDark = true), 0.0001f)
    }

    @Test
    fun pressFeedbackBoostsRefractionAndHighlight() {
        val interaction = softGlassPressMaterialInteraction(1f)

        assertEquals(1.06f, interaction.refractionScale, 0.0001f)
        assertEquals(1.12f, interaction.highlightScale, 0.0001f)
    }
}
