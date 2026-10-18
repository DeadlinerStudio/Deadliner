package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.model.AppearanceColorSource
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.scheme.DynamicScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MiuixPresetColorSchemesTest {
    @Test fun sliderKeyPointsContrastWithBothPartsOfTheTrack() {
        for (dark in listOf(false, true)) {
            val scheme = MiuixPresetColorSchemes.create(dark)
            val colors = DeadlinerMiuixColorSchemeFactory.create(
                DeadlinerColorTokenFactory.createFromMaterialScheme(scheme), dark)
            assertNotEquals(colors.primary, colors.sliderKeyPointForeground)
            assertEquals(colors.onPrimary.copy(alpha = 0.85f), colors.sliderKeyPointForeground)
            assertNotEquals(colors.sliderBackground, colors.sliderKeyPoint)
        }
    }

    @Test
    fun presetUsesOfficialMiuixPrimaryColors() {
        val light = MiuixPresetColorSchemes.create(darkTheme = false)
        val dark = MiuixPresetColorSchemes.create(darkTheme = true)

        assertEquals(Color(0xFF3382FF), light.primary)
        assertEquals(Color(0xFF277AF7), dark.primary)
        assertEquals(Color.White, light.onPrimary)
        assertEquals(Color.White, dark.onPrimary)
    }

    @Test
    fun presetKeepsEveryGeneratedMaterialRoleExceptPrimaryAndOnPrimary() {
        listOf(false, true).forEach { darkTheme ->
            val seed = MiuixPresetColorSchemes.seed(darkTheme)
            val preset = MiuixPresetColorSchemes.create(darkTheme)
            val generated = generatedPresetScheme(seed, darkTheme)

            assertEquals(
                generated.rolesExceptPrimaryAndOnPrimary(),
                preset.rolesExceptPrimaryAndOnPrimary(),
            )
            assertNotEquals(preset.primary, preset.primaryContainer)
            assertNotEquals(preset.primary, preset.secondary)
            assertNotEquals(preset.primary, preset.tertiary)
        }
    }

    @Test
    fun onPrimaryStaysWhiteWithoutChangingContainerRoles() {
        val base = MiuixPresetColorSchemes.create(darkTheme = true)
        val generated = generatedPresetScheme(MiuixPresetColorSchemes.DarkSeed, darkTheme = true)
        val tokens = DeadlinerColorTokenFactory.createFromMaterialScheme(base)
        val material = DeadlinerMaterial3ColorSchemeFactory.create(tokens, darkTheme = true)
        val miuix = DeadlinerMiuixColorSchemeFactory.create(tokens, darkTheme = true)

        assertEquals(Color.White, material.onPrimary)
        assertEquals(generated.onPrimaryContainer, material.onPrimaryContainer)
        assertEquals(Color.White, miuix.onPrimary)
        assertEquals(generated.onPrimaryContainer, miuix.onPrimaryContainer)
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
    fun homeFabUsesContrastingPresetByDefaultAndAlwaysUsesWhiteContent() {
        val fallbackContainer = Color(0xFF123456)
        val lightPreset = homeFabColors(
            colorSource = AppearanceColorSource.Preset,
            seedColorHex = null,
            darkTheme = false,
            fallbackContainer = fallbackContainer,
            useContrastingPresetFab = true,
        )
        val darkPreset = homeFabColors(
            colorSource = AppearanceColorSource.Preset,
            seedColorHex = null,
            darkTheme = true,
            fallbackContainer = fallbackContainer,
            useContrastingPresetFab = true,
        )
        val lightPresetWithoutContrast = homeFabColors(
            colorSource = AppearanceColorSource.Preset,
            seedColorHex = null,
            darkTheme = false,
            fallbackContainer = fallbackContainer,
            useContrastingPresetFab = false,
        )
        val darkPresetWithoutContrast = homeFabColors(
            colorSource = AppearanceColorSource.Preset,
            seedColorHex = null,
            darkTheme = true,
            fallbackContainer = fallbackContainer,
            useContrastingPresetFab = false,
        )
        val customSeed = homeFabColors(
            colorSource = AppearanceColorSource.SeedColor,
            seedColorHex = "#A35BC7",
            darkTheme = false,
            fallbackContainer = fallbackContainer,
            useContrastingPresetFab = true,
        )
        val dynamic = homeFabColors(
            colorSource = AppearanceColorSource.SystemDynamic,
            seedColorHex = "#A35BC7",
            darkTheme = false,
            fallbackContainer = fallbackContainer,
            useContrastingPresetFab = true,
        )

        assertEquals(Color(0xFFFF6D6D), lightPreset.container)
        assertEquals(Color(0xFFFF6D6D), darkPreset.container)
        assertEquals(Color(0xFF3382FF), lightPresetWithoutContrast.container)
        assertEquals(Color(0xFF277AF7), darkPresetWithoutContrast.container)
        assertEquals(Color(0xFFA35BC7), customSeed.container)
        assertEquals(fallbackContainer, dynamic.container)
        listOf(
            lightPreset,
            darkPreset,
            lightPresetWithoutContrast,
            darkPresetWithoutContrast,
            customSeed,
            dynamic,
        ).forEach { colors ->
            assertEquals(Color.White, colors.content)
        }
    }

    @Test
    fun invalidCustomSeedFallsBackToMaterialPrimary() {
        val fallbackContainer = Color(0xFF123456)

        val colors = homeFabColors(
            colorSource = AppearanceColorSource.SeedColor,
            seedColorHex = "not-a-color",
            darkTheme = false,
            fallbackContainer = fallbackContainer,
            useContrastingPresetFab = true,
        )

        assertEquals(fallbackContainer, colors.container)
        assertEquals(Color.White, colors.content)
    }

    private fun generatedPresetScheme(seed: Color, darkTheme: Boolean) = dynamicColorScheme(
        primary = seed,
        isDark = darkTheme,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2021,
        platform = DynamicScheme.Platform.PHONE,
    )

    private fun ColorScheme.rolesExceptPrimaryAndOnPrimary(): List<Color> = listOf(
        primaryContainer,
        onPrimaryContainer,
        inversePrimary,
        secondary,
        onSecondary,
        secondaryContainer,
        onSecondaryContainer,
        tertiary,
        onTertiary,
        tertiaryContainer,
        onTertiaryContainer,
        background,
        onBackground,
        surface,
        onSurface,
        surfaceVariant,
        onSurfaceVariant,
        surfaceTint,
        inverseSurface,
        inverseOnSurface,
        error,
        onError,
        errorContainer,
        onErrorContainer,
        outline,
        outlineVariant,
        scrim,
        surfaceBright,
        surfaceDim,
        surfaceContainer,
        surfaceContainerHigh,
        surfaceContainerHighest,
        surfaceContainerLow,
        surfaceContainerLowest,
        primaryFixed,
        primaryFixedDim,
        onPrimaryFixed,
        onPrimaryFixedVariant,
        secondaryFixed,
        secondaryFixedDim,
        onSecondaryFixed,
        onSecondaryFixedVariant,
        tertiaryFixed,
        tertiaryFixedDim,
        onTertiaryFixed,
        onTertiaryFixedVariant,
    )
}
