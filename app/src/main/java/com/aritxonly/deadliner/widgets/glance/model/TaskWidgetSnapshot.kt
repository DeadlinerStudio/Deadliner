package com.aritxonly.deadliner.widgets.glance.model

internal data class TaskWidgetSnapshot(
    val totalCount: Int,
    val items: List<TaskWidgetItem>,
)

internal data class TaskWidgetItem(
    val title: String,
    val timeDescription: String,
    val isUrgent: Boolean,
)
