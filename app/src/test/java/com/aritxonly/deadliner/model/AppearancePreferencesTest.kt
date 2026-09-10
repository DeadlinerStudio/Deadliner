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
    fun miuixIsTheOnlyComponentThemeAndUsesOneDividerPreference() {
        val appearance = AppearancePreferences(
            modernHideDivider = true,
        )

        assertTrue(appearance.usesMiuixTheme)
        assertEquals(true, appearance.activeHideDivider)
    }

    @Test
    fun advancedMaterialSoftLevelMatchesDefaultSpecValues() {
        assertEquals(128f, AdvancedMaterialLevel.Soft.blurRadius)
        assertEquals(1.00f, AdvancedMaterialLevel.Soft.blurSaturation)
        assertEquals(AdvancedMaterialLevel.Light, AppearancePreferences().advancedMaterialLevel)
        assertEquals(92f, AppearancePreferences().advancedMaterialBlurRadius, 0.0001f)
    }

    @Test
    fun presetColorSourceWorksWithBothBackgroundStrategies() {
        val miuix = AppearancePreferences(
            colorSource = AppearanceColorSource.Preset,
        )
        val monet = miuix.copy(backgroundColorStrategy = BackgroundColorStrategy.MaterialMonet)

        assertEquals(AppearanceColorSource.Preset, miuix.effectiveColorSource)
        assertEquals(AppearanceColorSource.Preset, monet.effectiveColorSource)
    }

    @Test
    fun contrastingHomeFabDefaultsOnAndAppearsForPreset() {
        val preset = AppearancePreferences(
            colorSource = AppearanceColorSource.Preset,
        )

        assertTrue(preset.useContrastingHomeFab)
        assertTrue(preset.showsContrastingHomeFabOption)
        assertFalse(
            preset.copy(colorSource = AppearanceColorSource.SeedColor)
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

    @Test
    fun legacyMaterialThemeMigratesToMaterialMonetBackground() {
        assertEquals(
            BackgroundColorStrategy.MaterialMonet,
            BackgroundColorStrategy.resolvePersisted(value = null, legacyUsedMaterialTheme = true),
        )
        assertEquals(
            BackgroundColorStrategy.MiuixHyperOs,
            BackgroundColorStrategy.resolvePersisted(value = null, legacyUsedMaterialTheme = false),
        )
    }
}
