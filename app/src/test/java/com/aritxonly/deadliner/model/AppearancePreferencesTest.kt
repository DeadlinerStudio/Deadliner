package com.aritxonly.deadliner.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun modernModeMapsToMiuixThemeAndModernDivider() {
        val appearance = AppearancePreferences(
            designMode = AppearanceDesignMode.Modern,
            modernHideDivider = true,
            vividHideDivider = false,
        )

        assertTrue(appearance.usesMiuixTheme)
        assertEquals(true, appearance.activeHideDivider)
    }

    @Test
    fun vividModeMapsToMaterialThemeAndVividDivider() {
        val appearance = AppearancePreferences(
            designMode = AppearanceDesignMode.Vivid,
            modernHideDivider = true,
            vividHideDivider = false,
        )

        assertEquals(AppThemeStyle.Material3, appearance.effectiveThemeStyle)
        assertEquals(false, appearance.activeHideDivider)
    }

    @Test
    fun advancedMaterialSoftLevelMatchesDefaultSpecValues() {
        assertEquals(128f, AdvancedMaterialLevel.Soft.blurRadius)
        assertEquals(1.00f, AdvancedMaterialLevel.Soft.blurSaturation)
    }
}
