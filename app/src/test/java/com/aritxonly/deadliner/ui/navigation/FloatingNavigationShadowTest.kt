package com.aritxonly.deadliner.ui.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingNavigationShadowTest {
    @Test
    fun lightModeUsesSoftSymmetricSpread() {
        val light = floatingNavigationShadowTokens(isDark = false)

        assertEquals(20.dp, light.ambientRadius)
        assertEquals((-2).dp, light.ambientSpread)
        assertEquals(0.018f, light.ambientAlpha, 0.0001f)
        assertEquals(10.dp, light.outerRadius)
        assertEquals((-1).dp, light.outerSpread)
        assertEquals(0.040f, light.outerAlpha, 0.0001f)
        assertEquals(3.dp, light.edgeRadius)
        assertTrue(light.edgeAlpha in 0.004f..0.008f)
    }

    @Test
    fun darkModeUsesMoreRestrainedSymmetricSpread() {
        val dark = floatingNavigationShadowTokens(isDark = true)

        assertEquals(22.dp, dark.ambientRadius)
        assertEquals((-1).dp, dark.ambientSpread)
        assertEquals(0.020f, dark.ambientAlpha, 0.0001f)
        assertEquals(11.dp, dark.outerRadius)
        assertEquals((-0.5).dp, dark.outerSpread)
        assertEquals(0.038f, dark.outerAlpha, 0.0001f)
        assertEquals(4.dp, dark.edgeRadius)
        assertEquals(0.008f, dark.edgeAlpha, 0.0001f)
    }
}
