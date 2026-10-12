package com.aritxonly.deadliner.widgets.glance.ui

internal object TaskWidgetLayoutMetrics {
    const val CARD_CONTENT_PADDING_DP = 12
    const val HEADER_HEIGHT_DP = 19
    const val HEADER_GAP_DP = 5
    const val ROW_GAP_DP = 4
    const val STATIC_TASK_LIMIT = 3

    fun staticVisibleTaskCount(totalCount: Int): Int = totalCount.coerceIn(0, STATIC_TASK_LIMIT)
}
