package com.aritxonly.deadliner.widgets.glance.preferences

import android.content.Context
import android.content.SharedPreferences

internal class TaskWidgetConfigurationStore private constructor(
    private val preferences: SharedPreferences,
    private val legacySquarePreset: () -> TaskWidgetPaddingPreset,
) {
    fun read(appWidgetId: Int, kind: TaskWidgetKind): TaskWidgetConfiguration {
        val defaults = TaskWidgetConfiguration.defaultFor(
            kind = kind,
            squarePreset = if (kind == TaskWidgetKind.SQUARE) {
                legacySquarePreset()
            } else {
                TaskWidgetPaddingPreset.Standard
            },
        )
        if (!preferences.getBoolean(key(appWidgetId, KEY_CONFIGURED), false)) {
            return defaults.sanitized(kind)
        }
        return TaskWidgetConfiguration(
            cornerRadiusDp = preferences.getInt(key(appWidgetId, KEY_CORNER), defaults.cornerRadiusDp),
            marginPreset = TaskWidgetPaddingPreset.fromStoredValue(
                preferences.getInt(key(appWidgetId, KEY_MARGIN_PRESET), defaults.marginPreset.storedValue)
            ),
            customTopMarginDp = preferences.getInt(
                key(appWidgetId, KEY_TOP_MARGIN),
                defaults.customTopMarginDp,
            ),
            customBottomMarginDp = preferences.getInt(
                key(appWidgetId, KEY_BOTTOM_MARGIN),
                defaults.customBottomMarginDp,
            ),
            colorScheme = TaskWidgetColorScheme.fromStoredValue(
                preferences.getString(key(appWidgetId, KEY_COLOR_SCHEME), null)
            ),
            themeMode = TaskWidgetThemeMode.fromStoredValue(
                preferences.getString(key(appWidgetId, KEY_THEME_MODE), null)
            ),
            backgroundTransparencyPercent = preferences.getInt(
                key(appWidgetId, KEY_BACKGROUND_TRANSPARENCY),
                defaults.backgroundTransparencyPercent,
            ),
            isScrollable = preferences.getBoolean(
                key(appWidgetId, KEY_SCROLLABLE),
                defaults.isScrollable,
            ),
        ).sanitized(kind)
    }

    fun write(appWidgetId: Int, kind: TaskWidgetKind, configuration: TaskWidgetConfiguration) {
        val value = configuration.sanitized(kind)
        preferences.edit()
            .putBoolean(key(appWidgetId, KEY_CONFIGURED), true)
            .putInt(key(appWidgetId, KEY_CORNER), value.cornerRadiusDp)
            .putInt(key(appWidgetId, KEY_MARGIN_PRESET), value.marginPreset.storedValue)
            .putInt(key(appWidgetId, KEY_TOP_MARGIN), value.customTopMarginDp)
            .putInt(key(appWidgetId, KEY_BOTTOM_MARGIN), value.customBottomMarginDp)
            .putString(key(appWidgetId, KEY_COLOR_SCHEME), value.colorScheme.storedValue)
            .putString(key(appWidgetId, KEY_THEME_MODE), value.themeMode.storedValue)
            .putBoolean(key(appWidgetId, KEY_SCROLLABLE), value.isScrollable)
            .putInt(key(appWidgetId, KEY_BACKGROUND_TRANSPARENCY), value.backgroundTransparencyPercent)
            .commit()
    }

    fun delete(appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        preferences.edit().also { editor ->
            appWidgetIds.forEach { id ->
                KEYS.forEach { suffix -> editor.remove(key(id, suffix)) }
            }
        }.apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "task_widget_instance_settings"
        private const val KEY_CONFIGURED = "configured"
        private const val KEY_CORNER = "corner"
        private const val KEY_MARGIN_PRESET = "margin_preset"
        private const val KEY_TOP_MARGIN = "top_margin"
        private const val KEY_BOTTOM_MARGIN = "bottom_margin"
        private const val KEY_COLOR_SCHEME = "color_scheme"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SCROLLABLE = "scrollable"
        private const val KEY_BACKGROUND_TRANSPARENCY = "background_transparency"
        private val KEYS = listOf(
            KEY_CONFIGURED,
            KEY_CORNER,
            KEY_MARGIN_PRESET,
            KEY_TOP_MARGIN,
            KEY_BOTTOM_MARGIN,
            KEY_COLOR_SCHEME,
            KEY_THEME_MODE,
            KEY_SCROLLABLE,
            KEY_BACKGROUND_TRANSPARENCY,
        )

        fun from(context: Context): TaskWidgetConfigurationStore = TaskWidgetConfigurationStore(
            preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
            legacySquarePreset = { TaskWidgetPreferences.readPaddingPreset(context) },
        )

        internal fun createForTest(
            preferences: SharedPreferences,
            legacySquarePreset: () -> TaskWidgetPaddingPreset = { TaskWidgetPaddingPreset.Standard },
        ): TaskWidgetConfigurationStore = TaskWidgetConfigurationStore(
            preferences = preferences,
            legacySquarePreset = legacySquarePreset,
        )

        private fun key(appWidgetId: Int, suffix: String): String = "$appWidgetId:$suffix"
    }
}
