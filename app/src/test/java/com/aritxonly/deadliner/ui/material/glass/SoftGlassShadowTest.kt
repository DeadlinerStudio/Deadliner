package com.aritxonly.deadliner.ui.material.glass

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class SoftGlassShadowTest {
    @Test
    fun lightGlassUsesStrongerOuterAndContactShadows() {
        val light = softGlassShadowTokens(isDark = false)
        val dark = softGlassShadowTokens(isDark = true)

        assertEquals(0f, light.offset.x.value, 0.0001f)
        assertEquals(0f, light.offset.y.value, 0.0001f)
        assertEquals(0f, dark.offset.x.value, 0.0001f)
        assertEquals(0f, dark.offset.y.value, 0.0001f)
        assertTrue(light.outerRadius > dark.outerRadius)
        assertTrue(light.outerAlpha > dark.outerAlpha)
        assertTrue(light.edgeRadius > dark.edgeRadius)
        assertTrue(light.edgeAlpha > dark.edgeAlpha)
    }
}
