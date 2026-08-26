package com.aritxonly.deadliner.ui.theme

import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.model.AppearanceColorSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MiuixPresetColorSchemesTest {
    @Test
    fun presetUsesOfficialMiuixPrimaryColors() {
        val light = MiuixPresetColorSchemes.create(darkTheme = false)
        val dark = MiuixPresetColorSchemes.create(darkTheme = true)

        assertEquals(Color(0xFF3482FF), light.primary)
        assertEquals(Color(0xFF277AF7), dark.primary)
        assertEquals(Color.White, light.onPrimary)
        assertEquals(Color.White, dark.onPrimary)
    }

    @Test
    fun presetAddsIndependentMaterialAccentRoles() {
        val light = MiuixPresetColorSchemes.create(darkTheme = false)

        assertNotEquals(light.primary, light.secondary)
        assertNotEquals(light.primary, light.tertiary)
        assertNotEquals(light.secondary, light.tertiary)
    }

    @Test
    fun darkPresetKeepsMaterialAndMiuixControlForegroundsWhite() {
        val base = MiuixPresetColorSchemes.create(darkTheme = true)
        val tokens = DeadlinerColorTokenFactory.createFromMaterialScheme(base)
        val material = DeadlinerMaterial3ColorSchemeFactory.create(tokens, darkTheme = true)
        val miuix = DeadlinerMiuixColorSchemeFactory.create(tokens, darkTheme = true)

        assertEquals(Color.White, material.onPrimary)
        assertEquals(Color.White, material.onPrimaryContainer)
        assertEquals(Color.White, miuix.onPrimary)
        assertEquals(Color.White, miuix.onPrimaryContainer)
    }

    @Test
    fun materialAccentsDoNotTintMiuixNeutralControlTracks() {
        val lightBase = MiuixPresetColorSchemes.create(darkTheme = false)
        val darkBase = MiuixPresetColorSchemes.create(darkTheme = true)
        val lightMiuix = DeadlinerMiuixColorSchemeFactory.create(
            DeadlinerColorTokenFactory.createFromMaterialScheme(lightBase),
            darkTheme = false,
        )
        val darkMiuix = DeadlinerMiuixColorSchemeFactory.create(
            DeadlinerColorTokenFactory.createFromMaterialScheme(darkBase),
            darkTheme = true,
        )

        assertEquals(Color(0xFFE6E6E6), lightMiuix.secondary)
        assertEquals(Color(0xFF505050), darkMiuix.secondary)
        assertNotEquals(lightBase.secondary, lightMiuix.secondary)
        assertNotEquals(darkBase.secondary, darkMiuix.secondary)
    }

    @Test
    fun presetUsesHarmonyCoralFabOnlyForMiuixPreset() {
        val fallbackContainer = Color(0xFF123456)
        val fallbackContent = Color(0xFFFAFAFA)
        val preset = homeFabColors(
            designSystem = AppDesignSystem.MIUIX,
            colorSource = AppearanceColorSource.Preset,
            fallbackContainer = fallbackContainer,
            fallbackContent = fallbackContent,
        )
        val dynamic = homeFabColors(
            designSystem = AppDesignSystem.MIUIX,
            colorSource = AppearanceColorSource.SystemDynamic,
            fallbackContainer = fallbackContainer,
            fallbackContent = fallbackContent,
        )

        assertEquals(Color(0xFFF08A7E), preset.container)
        assertEquals(Color(0xFF2B100C), preset.content)
        assertEquals(fallbackContainer, dynamic.container)
        assertEquals(fallbackContent, dynamic.content)
    }
}
