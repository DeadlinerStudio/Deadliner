package com.aritxonly.deadliner.ui.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingNavigationShadowTest {
    @Test
    fun lightModeUsesSoftSymmetricSpread() {
        val light = floatingNavigationShadowTokens(isDark = false)

        assertEquals(12.dp, light.outerRadius)
        assertEquals(0.dp, light.outerSpread)
        assertTrue(light.outerAlpha in 0.08f..0.14f)
        assertEquals(3.dp, light.edgeRadius)
        assertTrue(light.edgeAlpha in 0.03f..0.07f)
    }

    @Test
    fun darkModeUsesMoreRestrainedSymmetricSpread() {
        val dark = floatingNavigationShadowTokens(isDark = true)

        assertEquals(10.dp, dark.outerRadius)
        assertEquals(0.09f, dark.outerAlpha, 0.0001f)
        assertEquals(2.dp, dark.edgeRadius)
        assertEquals(0.04f, dark.edgeAlpha, 0.0001f)
    }
}
