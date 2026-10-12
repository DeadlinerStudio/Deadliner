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
    fun `new square widgets use 22dp corners and static list`() {
        val value = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE)

        assertEquals(22, value.cornerRadiusDp)
        assertEquals(false, value.isScrollable)
    }

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
    }

    @Test
    fun `capsule rejects desktop preset and shares square adjustment limits`() {
        val value = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.CAPSULE).copy(
            cornerRadiusDp = 1,
            marginPreset = TaskWidgetPaddingPreset.Grid4x6,
            customTopMarginDp = 28,
            customBottomMarginDp = 28,
            isScrollable = true,
        ).sanitized(TaskWidgetKind.CAPSULE)

        assertEquals(8, value.cornerRadiusDp)
        assertEquals(TaskWidgetPaddingPreset.Custom, value.marginPreset)
        assertEquals(28, value.topMarginDp())
        assertEquals(12, value.bottomMarginDp())
        assertEquals(false, value.isScrollable)
    }

    @Test
    fun `store keeps widget instances independent and deletes one instance`() {
        val preferences = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("widget-test", 0)
        val store = TaskWidgetConfigurationStore.createForTest(preferences)
        val first = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE).copy(
            colorScheme = TaskWidgetColorScheme.PURPLE,
            isScrollable = true,
        )
        val second = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE).copy(
            colorScheme = TaskWidgetColorScheme.GREEN,
            isScrollable = true,
        )

        store.write(10, TaskWidgetKind.SQUARE, first)
        store.write(20, TaskWidgetKind.SQUARE, second)
        store.delete(intArrayOf(10))

        assertEquals(TaskWidgetColorScheme.FOLLOW_APP, store.read(10, TaskWidgetKind.SQUARE).colorScheme)
        assertEquals(TaskWidgetColorScheme.GREEN, store.read(20, TaskWidgetKind.SQUARE).colorScheme)
        assertEquals(false, store.read(10, TaskWidgetKind.SQUARE).isScrollable)
        assertEquals(true, store.read(20, TaskWidgetKind.SQUARE).isScrollable)
    }

    @Test
    fun `legacy and new widgets retain their original background opacity`() {
        val preferences = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("widget-transparency-migration-test", 0)
        val store = TaskWidgetConfigurationStore.createForTest(preferences)
        for (kind in TaskWidgetKind.entries) {
            val id = 100 + kind.ordinal
            val expected = if (kind == TaskWidgetKind.SQUARE) 8 else 0
            assertEquals(expected, store.read(id, kind).backgroundTransparencyPercent)
            preferences.edit().putBoolean("$id:configured", true).commit()
            assertEquals(expected, store.read(id, kind).backgroundTransparencyPercent)
            assertEquals((100 - expected) / 100f, store.read(id, kind).backgroundAlpha(), 0.001f)
        }
    }

    @Test
    fun `transparency persists per instance clamps and is removed on deletion`() {
        val preferences = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("widget-transparency-test", 0)
        val store = TaskWidgetConfigurationStore.createForTest(preferences)
        val square = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE)
        val capsule = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.CAPSULE)
        store.write(10, TaskWidgetKind.SQUARE, square.copy(backgroundTransparencyPercent = 40))
        store.write(20, TaskWidgetKind.CAPSULE, capsule.copy(backgroundTransparencyPercent = 100))
        val reopened = TaskWidgetConfigurationStore.createForTest(preferences)
        assertEquals(40, reopened.read(10, TaskWidgetKind.SQUARE).backgroundTransparencyPercent)
        assertEquals(0.6f, reopened.read(10, TaskWidgetKind.SQUARE).backgroundAlpha(), 0.001f)
        assertEquals(0f, reopened.read(20, TaskWidgetKind.CAPSULE).backgroundAlpha(), 0.001f)

        store.delete(intArrayOf(10))
        assertEquals(false, preferences.contains("10:background_transparency"))
        assertEquals(8, reopened.read(10, TaskWidgetKind.SQUARE).backgroundTransparencyPercent)
        assertEquals(100, reopened.read(20, TaskWidgetKind.CAPSULE).backgroundTransparencyPercent)

        store.write(10, TaskWidgetKind.SQUARE, square.copy(backgroundTransparencyPercent = -20))
        assertEquals(0, reopened.read(10, TaskWidgetKind.SQUARE).backgroundTransparencyPercent)
        assertEquals(1f, reopened.read(10, TaskWidgetKind.SQUARE).backgroundAlpha(), 0.001f)
        preferences.edit().putInt("10:background_transparency", 150).commit()
        assertEquals(100, reopened.read(10, TaskWidgetKind.SQUARE).backgroundTransparencyPercent)
    }

    @Test
    fun `theme mode persists per widget and old or unknown values follow app`() {
        val preferences = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("widget-theme-mode-test", 0)
        val store = TaskWidgetConfigurationStore.createForTest(preferences)
        val square = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.SQUARE)
        val capsule = TaskWidgetConfiguration.defaultFor(TaskWidgetKind.CAPSULE)
        assertEquals(TaskWidgetThemeMode.FOLLOW_APP, store.read(10, TaskWidgetKind.SQUARE).themeMode)
        preferences.edit().putBoolean("10:configured", true).commit()
        assertEquals(TaskWidgetThemeMode.FOLLOW_APP, store.read(10, TaskWidgetKind.SQUARE).themeMode)
        preferences.edit().putString("10:theme_mode", "unknown").commit()
        assertEquals(TaskWidgetThemeMode.FOLLOW_APP, store.read(10, TaskWidgetKind.SQUARE).themeMode)

        store.write(10, TaskWidgetKind.SQUARE, square.copy(themeMode = TaskWidgetThemeMode.DARK))
        store.write(20, TaskWidgetKind.CAPSULE, capsule.copy(themeMode = TaskWidgetThemeMode.LIGHT))
        val reopened = TaskWidgetConfigurationStore.createForTest(preferences)
        assertEquals(TaskWidgetThemeMode.DARK, reopened.read(10, TaskWidgetKind.SQUARE).themeMode)
        assertEquals(TaskWidgetThemeMode.LIGHT, reopened.read(20, TaskWidgetKind.CAPSULE).themeMode)
        store.delete(intArrayOf(10))
        assertEquals(false, preferences.contains("10:theme_mode"))
        assertEquals(TaskWidgetThemeMode.FOLLOW_APP, reopened.read(10, TaskWidgetKind.SQUARE).themeMode)
        assertEquals(TaskWidgetThemeMode.LIGHT, reopened.read(20, TaskWidgetKind.CAPSULE).themeMode)
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
