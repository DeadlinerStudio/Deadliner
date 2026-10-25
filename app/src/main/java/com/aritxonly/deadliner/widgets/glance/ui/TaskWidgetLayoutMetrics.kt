package com.aritxonly.deadliner.widgets.glance.ui

internal object TaskWidgetLayoutMetrics {
    const val CARD_CONTENT_PADDING_DP = 12
    const val HEADER_HEIGHT_DP = 19
    const val HEADER_GAP_DP = 5
    const val ROW_GAP_DP = 4
    const val STATIC_TASK_LIMIT = 3

    // Scrollable rows are inlined into one RemoteViews update, which must fit the shared
    // 1 MiB Binder buffer. These bounds keep the worst case a small fraction of it.
    const val SCROLLABLE_TASK_LIMIT = 50
    const val ROW_TITLE_CHAR_LIMIT = 80

    fun staticVisibleTaskCount(totalCount: Int): Int = totalCount.coerceIn(0, STATIC_TASK_LIMIT)

    fun scrollableVisibleTaskCount(totalCount: Int): Int =
        totalCount.coerceIn(0, SCROLLABLE_TASK_LIMIT)

    /** Rows are single-line and ellipsized, so characters beyond the limit are never visible. */
    fun boundedRowTitle(title: String): String {
        if (title.length <= ROW_TITLE_CHAR_LIMIT) return title
        var end = ROW_TITLE_CHAR_LIMIT
        if (Character.isHighSurrogate(title[end - 1])) end -= 1
        return title.substring(0, end) + ELLIPSIS
    }

    private const val ELLIPSIS = "…"
}
