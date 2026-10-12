package com.aritxonly.deadliner.ui.overview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.deadliner.R
import com.deadliner.domain.statistics.FocusDurationPoint
import com.deadliner.domain.statistics.FocusMonthlyReview
import java.time.LocalDate
import java.util.Locale

internal enum class FocusChartKind { DAILY, WEEKLY, MONTHLY, MONTH_DAILY }

@Composable
internal fun focusDuration(seconds: Long): String = focusDuration(LocalContext.current, seconds)

internal fun focusDuration(context: Context, seconds: Long): String = when {
    seconds < 60 -> context.getString(R.string.focus_stats_seconds, seconds)
    seconds < 3_600 -> context.getString(R.string.focus_stats_minutes, seconds / 60)
    else -> context.getString(R.string.focus_stats_hours_minutes, seconds / 3_600, seconds % 3_600 / 60)
}

internal fun focusChartLabels(points: List<FocusDurationPoint>, kind: FocusChartKind, locale: Locale): List<String> =
    points.mapIndexed { index, point ->
        val date = LocalDate.parse(point.startDate)
        when (kind) {
            FocusChartKind.MONTHLY -> date.monthValue.toString()
            FocusChartKind.WEEKLY -> point.startDate.substring(5)
            FocusChartKind.DAILY -> date.dayOfMonth.toString()
            FocusChartKind.MONTH_DAILY -> if (index == 0 || date.dayOfMonth % 5 == 0 || index == points.lastIndex)
                date.dayOfMonth.toString() else ""
        }
    }

internal fun focusMonthlyMetrics(context: Context, review: FocusMonthlyReview): List<Metric> {
    val summary = review.current.summary
    val percent = review.durationChangePercent
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val comparison = when {
        percent != null -> String.format(locale, "%+.1f%%", percent)
        summary.totalDurationSeconds > 0 -> context.getString(R.string.focus_stats_first_month_short)
        else -> context.getString(R.string.focus_stats_both_months_empty)
    }
    return listOf(
        // More time is not automatically better. No positive/negative status coloring for focus metrics.
        Metric(context.getString(R.string.focus_stats_duration), focusDuration(context, summary.totalDurationSeconds),
            comparison, neutralChange = true, supportingText = focusTargetTimeBrief(context, summary.durationByTarget)),
        Metric(context.getString(R.string.focus_stats_days), summary.activeDayCount.toString(),
            supportingText = context.getString(R.string.focus_stats_session_brief,
                summary.sessionCount, focusDuration(context, summary.averageSessionDurationSeconds))),
    )
}
