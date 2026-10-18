package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class SoftGlassShadowTest {
    @Test
    fun lightGlassUsesLowDensityThreeStageDiffuseShadow() {
        val light = softGlassShadowTokens(isDark = false)
        val dark = softGlassShadowTokens(isDark = true)

        assertEquals(0f, light.offset.x.value, 0.0001f)
        assertEquals(0f, light.offset.y.value, 0.0001f)
        assertEquals(0f, dark.offset.x.value, 0.0001f)
        assertEquals(0f, dark.offset.y.value, 0.0001f)
        assertTrue(light.ambientRadius > light.outerRadius)
        assertTrue(dark.ambientRadius > dark.outerRadius)
        assertTrue(light.outerAlpha > dark.outerAlpha)
        assertTrue(light.edgeAlpha < light.outerAlpha * 0.25f)
        assertTrue(dark.edgeAlpha < dark.outerAlpha * 0.25f)
        assertTrue(light.outerSpread < 0.dp)
        assertTrue(dark.outerSpread < 0.dp)
        assertTrue(light.ambientSpread < light.outerSpread)
        assertTrue(dark.ambientSpread < dark.outerSpread)
    }

    @Test fun selfLightGlassUsesLessShadowWithoutChangingDarkOrOfficialGlass() {
        val self = softGlassShadowTokens(isDark = false)
        val official = softGlassShadowTokens(isDark = false, useOfficialMiuixGlass = true)
        assertTrue(self.outerAlpha < official.outerAlpha)
        assertTrue(self.ambientAlpha < official.ambientAlpha)
        assertTrue(self.edgeAlpha < self.outerAlpha * 0.25f)
        assertEquals(softGlassShadowTokens(true, false), softGlassShadowTokens(true, true))
        assertEquals(0.025f, official.ambientAlpha, 0.0001f)
        assertEquals(0.050f, official.outerAlpha, 0.0001f)
    }

    @Test fun topBarCompensationOnlyChangesSelfLightAmbientShadow() {
        val base = softGlassShadowTokens(false)
        val control = softGlassShadowTokens(false, profile = SoftGlassShadowProfile.TopBarControl)
        assertTrue(control.ambientRadius > base.ambientRadius)
        assertTrue(control.ambientAlpha > base.ambientAlpha)
        assertEquals(base, control.copy(ambientRadius = base.ambientRadius, ambientAlpha = base.ambientAlpha))
        assertEquals(softGlassShadowTokens(true), softGlassShadowTokens(true, profile = SoftGlassShadowProfile.TopBarControl))
        assertEquals(softGlassShadowTokens(false, true), softGlassShadowTokens(false, true, SoftGlassShadowProfile.TopBarControl))
    }

}
