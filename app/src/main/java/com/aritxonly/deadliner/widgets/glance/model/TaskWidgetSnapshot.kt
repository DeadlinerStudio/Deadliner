package com.aritxonly.deadliner.widgets.glance.model

internal data class TaskWidgetSnapshot(
    val totalCount: Int,
    val nearCount: Int,
    val overdueCount: Int,
    val summary: String,
    val items: List<TaskWidgetItem>,
) {
    val visualState: TaskWidgetVisualState
        get() = when {
            totalCount == 0 -> TaskWidgetVisualState.EMPTY
            overdueCount > 0 -> TaskWidgetVisualState.OVERDUE
            nearCount > 0 -> TaskWidgetVisualState.NEAR
            else -> TaskWidgetVisualState.NORMAL
        }
}

internal data class TaskWidgetItem(
    val stableId: Long,
    val title: String,
    val timeDescription: String,
    val status: TaskWidgetStatus,
    val categoryColorArgb: Int? = null,
) {
    fun indicatorColorArgb(semanticColorArgb: Int): Int = categoryColorArgb ?: semanticColorArgb
}

internal enum class TaskWidgetStatus {
    NORMAL,
    NEAR,
    OVERDUE,
}

internal enum class TaskWidgetVisualState {
    EMPTY,
    NORMAL,
    NEAR,
    OVERDUE,
}
