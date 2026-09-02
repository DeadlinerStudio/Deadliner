package com.aritxonly.deadliner.widgets.glance.preferences

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskWidgetConfigurationTest {
    @Test
    fun `square configuration clamps corner and combined custom margins`() {
        val value = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE).copy(
            cornerRadiusDp = 99,
            marginPreset = TaskWidgetPaddingPreset.Custom,
            customTopMarginDp = 28,
            customBottomMarginDp = 28,
        ).sanitized(TaskWidgetKind.SQUARE)

        assertEquals(32, value.cornerRadiusDp)
        assertEquals(28, value.topMarginDp())
        assertEquals(12, value.bottomMarginDp())
        assertEquals(1, value.maxVisibleTasks())
    }

    @Test
    fun `capsule rejects desktop preset and keeps a safe total margin`() {
        val value = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.CAPSULE).copy(
            cornerRadiusDp = 1,
            marginPreset = TaskWidgetPaddingPreset.Grid4x6,
            customTopMarginDp = 8,
            customBottomMarginDp = 8,
        ).sanitized(TaskWidgetKind.CAPSULE)

        assertEquals(12, value.cornerRadiusDp)
        assertEquals(TaskWidgetPaddingPreset.Custom, value.marginPreset)
        assertEquals(8, value.topMarginDp())
        assertEquals(4, value.bottomMarginDp())
    }

    @Test
    fun `store keeps widget instances independent and deletes one instance`() {
        val preferences = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("widget-test", 0)
        val store = TaskWidgetConfigurationStore.createForTest(preferences)
        val first = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE).copy(
            colorScheme = TaskWidgetColorScheme.PURPLE,
        )
        val second = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE).copy(
            colorScheme = TaskWidgetColorScheme.GREEN,
        )

        store.write(10, TaskWidgetKind.SQUARE, first)
        store.write(20, TaskWidgetKind.SQUARE, second)
        store.delete(intArrayOf(10))

        assertEquals(TaskWidgetColorScheme.FOLLOW_APP, store.read(10, TaskWidgetKind.SQUARE).colorScheme)
        assertEquals(TaskWidgetColorScheme.GREEN, store.read(20, TaskWidgetKind.SQUARE).colorScheme)
    }

    @Test
    fun `unconfigured square inherits the legacy desktop preset`() {
        val preferences = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("widget-migration-test", 0)
        val store = TaskWidgetConfigurationStore.createForTest(
            preferences = preferences,
            legacySquarePreset = { TaskWidgetPaddingPreset.Grid4x7 },
        )

        val value = store.read(30, TaskWidgetKind.SQUARE)

        assertEquals(TaskWidgetPaddingPreset.Grid4x7, value.marginPreset)
        assertEquals(12, value.topMarginDp())
    }
}
