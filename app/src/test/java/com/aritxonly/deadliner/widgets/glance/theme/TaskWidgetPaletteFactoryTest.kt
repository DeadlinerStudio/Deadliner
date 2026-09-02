package com.aritxonly.deadliner.widgets.glance.theme

import android.app.Application
import android.graphics.Color
import com.aritxonly.deadliner.model.AppearanceDesignMode
import com.aritxonly.deadliner.model.AppearancePreferences
import com.aritxonly.deadliner.model.ModernColorPalette
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetVisualState
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetColorScheme
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
        val colors = createColors(
            appearance = AppearancePreferences(
                designMode = AppearanceDesignMode.Modern,
                modernColorPalette = ModernColorPalette.Honor,
                seedColorHex = "#3366FF",
            ),
        )

        assertEquals(Color.parseColor("#F2F3F7"), colors.backgroundBase)
        assertEquals(Color.parseColor("#3366FF"), colors.brand)
    }

    @Test
    fun `device palette strategy overrides stored modern palette`() {
        val colors = createColors(
            appearance = AppearancePreferences(
                designMode = AppearanceDesignMode.Modern,
                modernColorPalette = ModernColorPalette.Honor,
                modernUseDevicePaletteStrategy = true,
            ),
            devicePalette = ModernColorPalette.Oppo,
        )

        assertEquals(Color.parseColor("#EEF2F3"), colors.backgroundBase)
    }

    @Test
    fun `visual state changes tint and semantic badge color`() {
        val appearance = AppearancePreferences(
            designMode = AppearanceDesignMode.Modern,
            modernColorPalette = ModernColorPalette.HyperOs,
            seedColorHex = "#6750A4",
        )
        val normal = createColors(appearance, visualState = TaskWidgetVisualState.NORMAL)
        val near = createColors(appearance, visualState = TaskWidgetVisualState.NEAR)
        val overdue = createColors(appearance, visualState = TaskWidgetVisualState.OVERDUE)

        assertNotEquals(normal.backgroundTint, near.backgroundTint)
        assertNotEquals(near.backgroundTint, overdue.backgroundTint)
        assertEquals(Color.parseColor("#FF9500"), near.badgeContentColor(TaskWidgetVisualState.NEAR))
        assertEquals(Color.parseColor("#E94634"), overdue.badgeContentColor(TaskWidgetVisualState.OVERDUE))
    }

    @Test
    fun `miuix blue scheme uses verified light and dark primary values`() {
        val appearance = AppearancePreferences()
        val light = TaskWidgetPaletteFactory.create(
            appearance = appearance,
            isDark = false,
            systemAccent = Color.RED,
            devicePalette = ModernColorPalette.HyperOs,
            widgetColorScheme = TaskWidgetColorScheme.BLUE,
        )
        val dark = TaskWidgetPaletteFactory.create(
            appearance = appearance,
            isDark = true,
            systemAccent = Color.RED,
            devicePalette = ModernColorPalette.HyperOs,
            widgetColorScheme = TaskWidgetColorScheme.BLUE,
        )

        assertEquals(Color.parseColor("#3482FF"), light.brand)
        assertEquals(Color.parseColor("#277AF7"), dark.brand)
        assertEquals(Color.parseColor("#F4F7FC"), light.backgroundBase)
        assertEquals(Color.parseColor("#161C24"), dark.backgroundBase)
    }

    @Test
    fun `follow app prefers resolved app accent over raw seed fallback`() {
        val colors = TaskWidgetPaletteFactory.create(
            appearance = AppearancePreferences(seedColorHex = "#FF0000"),
            isDark = false,
            systemAccent = Color.BLUE,
            devicePalette = ModernColorPalette.HyperOs,
            appAccent = Color.GREEN,
        )

        assertEquals(Color.GREEN, colors.brand)
    }

    @Test
    fun `vivid mode and night mode produce distinct theme surfaces`() {
        val appearance = AppearancePreferences(
            designMode = AppearanceDesignMode.Vivid,
            seedColorHex = "6750A4",
        )

        val light = createColors(appearance, isDark = false)
        val dark = createColors(appearance, isDark = true)

        assertNotEquals(light.backgroundBase, dark.backgroundBase)
        assertNotEquals(light.textPrimary, dark.textPrimary)
        assertEquals(Color.parseColor("#6750A4"), light.brand)
    }

    private fun createColors(
        appearance: AppearancePreferences,
        isDark: Boolean = false,
        devicePalette: ModernColorPalette = ModernColorPalette.HyperOs,
        visualState: TaskWidgetVisualState = TaskWidgetVisualState.NORMAL,
    ): TaskWidgetColors = TaskWidgetPaletteFactory.create(
        appearance = appearance,
        isDark = isDark,
        systemAccent = Color.BLUE,
        devicePalette = devicePalette,
        visualState = visualState,
    )
}
