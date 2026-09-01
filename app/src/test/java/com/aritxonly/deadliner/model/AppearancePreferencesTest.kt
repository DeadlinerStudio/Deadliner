package com.aritxonly.deadliner.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun semanticGlowDefaultsOnAndCanBeDisabled() {
        assertTrue(AppearancePreferences().semanticGlowEnabled)
        assertFalse(AppearancePreferences().copy(semanticGlowEnabled = false).semanticGlowEnabled)
    }

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
        assertEquals(AdvancedMaterialLevel.Light, AppearancePreferences().advancedMaterialLevel)
        assertEquals(92f, AppearancePreferences().advancedMaterialBlurRadius, 0.0001f)
    }

    @Test
    fun presetColorSourceIsScopedToModernDesign() {
        val modern = AppearancePreferences(
            designMode = AppearanceDesignMode.Modern,
            colorSource = AppearanceColorSource.Preset,
        )
        val vivid = modern.copy(designMode = AppearanceDesignMode.Vivid)

        assertEquals(AppearanceColorSource.Preset, modern.effectiveColorSource)
        assertEquals(AppearanceColorSource.SystemDynamic, vivid.effectiveColorSource)
    }

    @Test
    fun contrastingHomeFabDefaultsOnAndOnlyAppearsForModernPreset() {
        val modernPreset = AppearancePreferences(
            designMode = AppearanceDesignMode.Modern,
            colorSource = AppearanceColorSource.Preset,
        )

        assertTrue(modernPreset.useContrastingHomeFab)
        assertTrue(modernPreset.showsContrastingHomeFabOption)
        assertFalse(
            modernPreset.copy(colorSource = AppearanceColorSource.SeedColor)
                .showsContrastingHomeFabOption,
        )
        assertFalse(
            modernPreset.copy(designMode = AppearanceDesignMode.Vivid)
                .showsContrastingHomeFabOption,
        )
    }

    @Test
    fun colorSourceKeysRoundTripAndRejectUnknownValues() {
        AppearanceColorSource.entries.forEach { source ->
            assertEquals(source, AppearanceColorSource.fromKey(source.key))
        }
        assertNull(AppearanceColorSource.fromKey("unknown"))
    }

    @Test
    fun legacyColorSourceFallsBackToSeedPresence() {
        assertEquals(
            AppearanceColorSource.SystemDynamic,
            AppearanceColorSource.resolvePersisted(value = null, seedColorHex = null),
        )
        assertEquals(
            AppearanceColorSource.SeedColor,
            AppearanceColorSource.resolvePersisted(value = null, seedColorHex = "#3482FF"),
        )
        assertEquals(
            AppearanceColorSource.Preset,
            AppearanceColorSource.resolvePersisted(value = "preset", seedColorHex = "#3482FF"),
        )
    }
}
