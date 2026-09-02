package com.aritxonly.deadliner.widgets.glance.theme

import android.app.Application
import android.graphics.Color
import com.aritxonly.deadliner.model.AppearancePreferences
import com.aritxonly.deadliner.model.ModernColorPalette
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskWidgetBackgroundRendererTest {
    private val colors = TaskWidgetPaletteFactory.create(
        appearance = AppearancePreferences(seedColorHex = "#6750A4"),
        isDark = false,
        systemAccent = Color.BLUE,
        devicePalette = ModernColorPalette.HyperOs,
    )

    @Test
    fun `renders bounded widget backgrounds`() {
        val square = TaskWidgetBackgroundRenderer.renderSquare(colors)
        val capsule = TaskWidgetBackgroundRenderer.renderCapsule(colors)

        assertEquals(240, square.width)
        assertEquals(240, square.height)
        assertEquals(300, capsule.width)
        assertEquals(128, capsule.height)
        assertEquals(240 * 240 * 4, square.allocationByteCount)
        assertEquals(300 * 128 * 4, capsule.allocationByteCount)
    }

    @Test
    fun `renders row state background`() {
        val row = TaskWidgetBackgroundRenderer.renderRow(colors, TaskWidgetStatus.OVERDUE)

        assertEquals(180, row.width)
        assertEquals(40, row.height)
        assertEquals(180 * 40 * 4, row.allocationByteCount)
    }
}
