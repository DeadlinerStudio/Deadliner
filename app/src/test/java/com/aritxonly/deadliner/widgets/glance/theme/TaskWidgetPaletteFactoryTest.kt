package com.aritxonly.deadliner.widgets.glance.theme

import android.graphics.Color
import android.app.Application
import com.aritxonly.deadliner.model.AppearanceDesignMode
import com.aritxonly.deadliner.model.AppearancePreferences
import com.aritxonly.deadliner.model.ModernColorPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskWidgetPaletteFactoryTest {
    @Test
    fun `modern mode uses selected neutral palette and current accent`() {
        val colors = TaskWidgetPaletteFactory.create(
            appearance = AppearancePreferences(
                designMode = AppearanceDesignMode.Modern,
                modernColorPalette = ModernColorPalette.Honor,
                seedColorHex = "#3366FF",
            ),
            isDark = false,
            systemAccent = Color.RED,
            devicePalette = ModernColorPalette.Oppo,
        )

        assertEquals(Color.parseColor("#F2F3F7"), colors.backgroundStart)
        assertEquals(Color.parseColor("#3366FF"), colors.brand)
    }

    @Test
    fun `device palette strategy overrides stored modern palette`() {
        val colors = TaskWidgetPaletteFactory.create(
            appearance = AppearancePreferences(
                designMode = AppearanceDesignMode.Modern,
                modernColorPalette = ModernColorPalette.Honor,
                modernUseDevicePaletteStrategy = true,
            ),
            isDark = false,
            systemAccent = Color.BLUE,
            devicePalette = ModernColorPalette.Oppo,
        )

        assertEquals(Color.parseColor("#EEF2F3"), colors.backgroundStart)
    }

    @Test
    fun `vivid mode and night mode produce distinct theme surfaces`() {
        val appearance = AppearancePreferences(
            designMode = AppearanceDesignMode.Vivid,
            seedColorHex = "6750A4",
        )

        val light = TaskWidgetPaletteFactory.create(
            appearance = appearance,
            isDark = false,
            systemAccent = Color.GREEN,
            devicePalette = ModernColorPalette.HyperOs,
        )
        val dark = TaskWidgetPaletteFactory.create(
            appearance = appearance,
            isDark = true,
            systemAccent = Color.GREEN,
            devicePalette = ModernColorPalette.HyperOs,
        )

        assertNotEquals(light.backgroundStart, dark.backgroundStart)
        assertNotEquals(light.textPrimary, dark.textPrimary)
        assertEquals(Color.parseColor("#6750A4"), light.brand)
    }
}
