package com.aritxonly.deadliner.widgets.glance.theme

import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetVisualState

internal data class TaskWidgetColors(
    val isDark: Boolean,
    val backgroundBase: Int,
    val backgroundTint: Int,
    val backgroundGlow: Int,
    val backgroundHighlight: Int,
    val decoration: Int,
    val badgeBackground: Int,
    val brand: Int,
    val brandSoft: Int,
    val near: Int,
    val overdue: Int,
    val completed: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val normalRow: TaskWidgetRowColors,
    val nearRow: TaskWidgetRowColors,
    val overdueRow: TaskWidgetRowColors,
) {
    fun badgeContentColor(state: TaskWidgetVisualState): Int = when (state) {
        TaskWidgetVisualState.OVERDUE -> overdue
        TaskWidgetVisualState.NEAR -> near
        TaskWidgetVisualState.EMPTY -> completed
        TaskWidgetVisualState.NORMAL -> brand
    }

    fun rowColors(status: TaskWidgetStatus): TaskWidgetRowColors = when (status) {
        TaskWidgetStatus.NORMAL -> normalRow
        TaskWidgetStatus.NEAR -> nearRow
        TaskWidgetStatus.OVERDUE -> overdueRow
    }

    /** Lightweight row background that does not duplicate bitmaps in RemoteCollectionItems. */
    fun rowBackgroundColor(status: TaskWidgetStatus): Int =
        (rowColors(status).base and 0x00FFFFFF) or (ROW_BACKGROUND_ALPHA shl 24)

    private companion object {
        const val ROW_BACKGROUND_ALPHA = 0xD9
    }
}

internal data class TaskWidgetRowColors(
    val base: Int,
    val tint: Int,
    val highlight: Int,
    val indicator: Int,
    val timeText: Int,
)
