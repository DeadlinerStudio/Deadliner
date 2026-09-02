package com.aritxonly.deadliner.widgets.glance.theme

import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetColorScheme

internal data class TaskWidgetColorPair(
    val background: Int,
    val accent: Int,
)

internal object TaskWidgetColorSchemePalette {
    fun resolve(scheme: TaskWidgetColorScheme, isDark: Boolean): TaskWidgetColorPair? = when (scheme) {
        TaskWidgetColorScheme.FOLLOW_APP -> null
        TaskWidgetColorScheme.BLUE -> if (isDark) {
            TaskWidgetColorPair(background = 0xFF161C24.toInt(), accent = 0xFF277AF7.toInt())
        } else {
            // Miuix 0.9.4-rc01 default primary pair.
            TaskWidgetColorPair(background = 0xFFF4F7FC.toInt(), accent = 0xFF3482FF.toInt())
        }

        TaskWidgetColorScheme.PURPLE -> if (isDark) {
            TaskWidgetColorPair(background = 0xFF1D1924.toInt(), accent = 0xFFA78BFA.toInt())
        } else {
            TaskWidgetColorPair(background = 0xFFF8F5FC.toInt(), accent = 0xFF7C4DDA.toInt())
        }

        TaskWidgetColorScheme.GREEN -> if (isDark) {
            TaskWidgetColorPair(background = 0xFF14201A.toInt(), accent = 0xFF42C47A.toInt())
        } else {
            TaskWidgetColorPair(background = 0xFFF4FAF7.toInt(), accent = 0xFF2E9B63.toInt())
        }

        TaskWidgetColorScheme.ORANGE -> if (isDark) {
            TaskWidgetColorPair(background = 0xFF241A12.toInt(), accent = 0xFFFF9F0A.toInt())
        } else {
            TaskWidgetColorPair(background = 0xFFFFF8F1.toInt(), accent = 0xFFF57C00.toInt())
        }

        TaskWidgetColorScheme.GRAPHITE -> if (isDark) {
            TaskWidgetColorPair(background = 0xFF171717.toInt(), accent = 0xFFAEB4BD.toInt())
        } else {
            TaskWidgetColorPair(background = 0xFFF2F3F5.toInt(), accent = 0xFF5B6472.toInt())
        }
    }
}
