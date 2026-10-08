package com.aritxonly.deadliner.ui.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingNavigationShadowTest {
    @Test
    fun lightModeUsesSoftSymmetricSpread() {
        val light = floatingNavigationShadowTokens(isDark = false)

        assertEquals(24.dp, light.ambientRadius)
        assertEquals((-1).dp, light.ambientSpread)
        assertEquals(0.025f, light.ambientAlpha, 0.0001f)
        assertEquals(12.dp, light.outerRadius)
        assertEquals((-0.5).dp, light.outerSpread)
        assertEquals(0.050f, light.outerAlpha, 0.0001f)
        assertEquals(4.5.dp, light.edgeRadius)
        assertTrue(light.edgeAlpha in 0.01f..0.02f)
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
