package com.aritxonly.deadliner.widgets.glance.preferences

import android.content.Context

enum class TaskWidgetPaddingPreset(
    val storedValue: Int,
    val cardTopMarginDp: Int,
    val cardBottomMarginDp: Int,
    val maxVisibleTasks: Int,
) {
    Standard(storedValue = 0, cardTopMarginDp = 0, cardBottomMarginDp = 0, maxVisibleTasks = 3),
    Grid4x6(storedValue = 1, cardTopMarginDp = 18, cardBottomMarginDp = 0, maxVisibleTasks = 2),
    Grid4x7(storedValue = 2, cardTopMarginDp = 12, cardBottomMarginDp = 0, maxVisibleTasks = 2),
    Custom(storedValue = 3, cardTopMarginDp = 0, cardBottomMarginDp = 0, maxVisibleTasks = 2);

    companion object {
        fun fromStoredValue(value: Int): TaskWidgetPaddingPreset =
            entries.firstOrNull { it.storedValue == value } ?: Standard
    }
}

object TaskWidgetPreferences {
    const val PADDING_PRESET_KEY = "task_widget_padding_preset"

    private const val PREFERENCES_NAME = "app_settings"

    fun readPaddingPreset(context: Context): TaskWidgetPaddingPreset {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return TaskWidgetPaddingPreset.fromStoredValue(
            preferences.getInt(PADDING_PRESET_KEY, TaskWidgetPaddingPreset.Standard.storedValue)
        ).takeUnless { it == TaskWidgetPaddingPreset.Custom } ?: TaskWidgetPaddingPreset.Standard
    }

    fun writePaddingPreset(context: Context, preset: TaskWidgetPaddingPreset) {
        require(preset != TaskWidgetPaddingPreset.Custom) {
            "The global default only supports named desktop presets."
        }
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(PADDING_PRESET_KEY, preset.storedValue)
            .apply()
    }
}
