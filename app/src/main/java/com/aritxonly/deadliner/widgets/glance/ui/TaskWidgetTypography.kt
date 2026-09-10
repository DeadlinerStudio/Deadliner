package com.aritxonly.deadliner.widgets.glance.ui

import androidx.glance.text.FontFamily

/** Compact type roles shared by the 2 x 2 and 2 x 1 widgets. */
internal object TaskWidgetTypography {
    // Glance maps Medium to a bold text style on some hosts. Use the medium family itself with
    // Normal weight for primary text so Xiaomi does not compound family and style weights.
    val EMPHASIS_FONT_FAMILY = FontFamily("sans-serif-medium")
    val SUPPORTING_FONT_FAMILY = FontFamily("sans-serif")

    const val BRAND_SIZE_SP = 14

    const val BADGE_SIZE_SP = 10

    const val TASK_TITLE_SIZE_SP = 12

    const val TASK_TIME_SIZE_SP = 10

    const val EMPTY_SIZE_SP = 12

    const val CAPSULE_SUMMARY_SIZE_SP = 15

    const val CAPSULE_SUPPORTING_SIZE_SP = 11

    const val CAPSULE_COUNT_SIZE_SP = 40
}
