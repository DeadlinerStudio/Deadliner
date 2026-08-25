package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.contentColorFor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HyperOsWindowBackgroundTest {
    @Test
    fun lightAndDarkWindowBackgroundsMatchHyperOs() {
        assertEquals(0xFFF3F3F3.toInt(), hyperOsWindowBackground(darkTheme = false).toArgb())
        assertEquals(0xFF000000.toInt(), hyperOsWindowBackground(darkTheme = true).toArgb())
    }

    @Test
    fun textColorsAreOpaqueAndStableAcrossContainers() {
        val light = hyperOsTextColors(darkTheme = false)
        val dark = hyperOsTextColors(darkTheme = true)

        assertEquals(0xFF191919.toInt(), light.primary.toArgb())
        assertEquals(0xFF6F6F6F.toInt(), light.secondary.toArgb())
        assertEquals(0xFFF2F2F2.toInt(), dark.primary.toArgb())
        assertEquals(0xFFA6A6A6.toInt(), dark.secondary.toArgb())
    }

    @Test
    fun surfaceContainerResolvesToPrimaryContentColor() {
        listOf(false, true).forEach { darkTheme ->
            val baseScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            val tokens = DeadlinerColorTokenFactory.createFromMaterialScheme(
                baseScheme = baseScheme,
                miuixNeutralPalette = DeadlinerColorTokenFactory.createHyperOsNeutralPalette(
                    darkTheme,
                ),
            )
            val scheme = DeadlinerMaterial3ColorSchemeFactory.create(tokens, darkTheme)

            assertNotEquals(scheme.surfaceVariant, scheme.surfaceContainer)
            assertEquals(scheme.onSurface, scheme.contentColorFor(scheme.surfaceContainer))
        }
    }
}
