package com.aritxonly.deadliner.ui.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingNavigationShadowTest {
    @Test
    fun lightModeUsesSoftSymmetricSpread() {
        val light = floatingNavigationShadowTokens(isDark = false)

        assertEquals(8.dp, light.outerRadius)
        assertEquals((-0.75).dp, light.outerSpread)
        assertTrue(light.outerAlpha in 0.06f..0.07f)
        assertEquals(3.5.dp, light.edgeRadius)
        assertTrue(light.edgeAlpha in 0.01f..0.02f)
    }

    @Test
    fun darkModeUsesMoreRestrainedSymmetricSpread() {
        val dark = floatingNavigationShadowTokens(isDark = true)

        assertEquals(7.dp, dark.outerRadius)
        assertEquals((-0.75).dp, dark.outerSpread)
        assertEquals(0.052f, dark.outerAlpha, 0.0001f)
        assertEquals(3.dp, dark.edgeRadius)
        assertEquals(0.010f, dark.edgeAlpha, 0.0001f)
    }
}
