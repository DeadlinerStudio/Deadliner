package com.aritxonly.deadliner.widgets.glance.data

import android.content.Context
import com.aritxonly.deadliner.R

internal class AndroidTaskWidgetStrings(
    private val context: Context,
) : TaskWidgetStrings {
    override fun overdue(): String = context.getString(R.string.glance_task_widget_overdue)

    override fun dueInMinutes(minutes: Long): String =
        context.getString(R.string.glance_task_widget_due_minutes, minutes)

    override fun dueInHours(hours: Long): String =
        context.getString(R.string.glance_task_widget_due_hours, hours)

    override fun dueInDays(days: Long): String =
        context.getString(R.string.glance_task_widget_due_days, days)

    override fun unscheduled(): String =
        context.getString(R.string.glance_task_widget_unscheduled)

    override fun emptySummary(): String =
        context.getString(R.string.glance_task_widget_summary_empty)

    override fun overdueSummary(): String =
        context.getString(R.string.glance_task_widget_summary_overdue)

    override fun nearSummary(): String =
        context.getString(R.string.glance_task_widget_summary_near)

    override fun pendingSummary(): String =
        context.getString(R.string.glance_task_widget_summary_pending)
}
