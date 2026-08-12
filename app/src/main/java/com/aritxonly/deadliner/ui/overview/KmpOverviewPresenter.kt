package com.aritxonly.deadliner.ui.overview

import android.content.Context
import com.aritxonly.deadliner.R
import com.deadliner.viewmodel.overview.OverviewCompletionPeriod
import com.deadliner.viewmodel.overview.OverviewMonthlyMetrics
import com.deadliner.viewmodel.overview.OverviewUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/** Presentation-only localization for the shared Overview aggregation. */
internal object KmpOverviewPresenter {
    fun present(context: Context, state: OverviewUiState): OverviewSnapshot {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val monthStart = state.lastMonth.monthStart?.let(LocalDate::parse)
        val lastMonthName = monthStart?.month?.getDisplayName(TextStyle.FULL, locale).orEmpty()
        val metrics = presentMetrics(context, state.lastMonth.metrics)

        return OverviewSnapshot(
            activeStats = linkedMapOf(
                context.getString(R.string.today_completed) to state.summary.completedToday,
                context.getString(R.string.pending_tasks) to state.summary.pendingDueFromNow,
                context.getString(R.string.today_overdue) to state.summary.overdueToday,
                context.getString(R.string.abandoned) to state.summary.activeAbandoned,
            ),
            historyStats = linkedMapOf(
                context.getString(R.string.cumulative_completed) to state.history.completed,
                context.getString(R.string.current_pending) to state.history.pending,
                context.getString(R.string.cumulative_abandoned) to state.history.abandoned,
                context.getString(R.string.cumulative_overdue) to state.history.overdue,
            ),
            completionTimeStats = state.completionBuckets.map { bucket ->
                context.getString(bucket.period.labelRes()) to bucket.completedCount
            },
            dailyStats = state.daily.map { stat ->
                val date = LocalDate.parse(stat.date)
                DailyStat(date, date.format(DateTimeFormatter.ofPattern("MM-dd")), stat.completedCount, stat.overdueCount)
            },
            monthlyStats = state.monthly.map { stat ->
                val date = LocalDate.parse(stat.monthStart)
                MonthlyStat(
                    month = date.month.getDisplayName(TextStyle.SHORT, locale),
                    totalCount = stat.totalCount,
                    completedCount = stat.completedCount,
                    overdueCompletedCount = stat.overdueCompletedCount,
                )
            },
            weeklyStats = state.weekly.map { stat ->
                val weekStart = LocalDate.parse(stat.weekStart)
                WeeklyStat(
                    weekLabel = weekStart.format(DateTimeFormatter.ofPattern("MM-dd")),
                    completedCount = stat.completedCount,
                )
            },
            contributionStats = state.contribution.map { stat ->
                ContributionDay(LocalDate.parse(stat.date), stat.completedCount)
            },
            lastMonthDailyStats = state.lastMonth.daily.map { stat ->
                val date = LocalDate.parse(stat.date)
                DailyStat(date, date.dayOfMonth.toString(), stat.completedCount, stat.overdueCount)
            },
            metrics = metrics,
            lastMonthName = lastMonthName,
            lastMonthKey = monthStart?.let(YearMonth::from)?.toString().orEmpty(),
            completedTaskNames = state.lastMonth.completedTaskTitles,
            metricsSummary = metrics.joinToString("\n") { metric ->
                buildString {
                    append(metric.label).append(": ").append(metric.value)
                    metric.change?.takeIf(String::isNotBlank)?.let { append(" (").append(it).append(')') }
                }
            },
        )
    }

    private fun presentMetrics(context: Context, metrics: OverviewMonthlyMetrics): List<Metric> = buildList {
        add(metric(context, R.string.metric_last_month_total, metrics.totalTasks.toDouble(), metrics.previousTotalTasks.toDouble(), metrics.totalTasks.toString()))
        add(metric(context, R.string.metric_last_month_completed, metrics.completedTasks.toDouble(), metrics.previousCompletedTasks.toDouble(), metrics.completedTasks.toString()))
        add(metric(context, R.string.metric_last_month_completion_rate, metrics.completionRate, metrics.previousCompletionRate, String.format(Locale.US, "%.1f%%", metrics.completionRate)))
        add(metric(context, R.string.metric_last_month_overdue, metrics.overdueTasks.toDouble(), metrics.previousOverdueTasks.toDouble(), metrics.overdueTasks.toString()))
        metrics.mostActivePeriod?.let { period ->
            add(Metric(
                label = context.getString(R.string.metric_most_active_period),
                value = context.getString(period.labelRes()),
                change = context.getString(R.string.metric_bucket_completed_count, metrics.mostActivePeriodCompletedCount),
            ))
        }
        metrics.averageCompletionMinutes?.let { minutes ->
            add(Metric(
                label = context.getString(R.string.metric_average_duration),
                value = when {
                    minutes < 60 -> context.getString(R.string.duration_minutes, minutes.coerceAtLeast(1))
                    minutes < 24 * 60 -> context.getString(R.string.duration_hours, minutes / 60)
                    else -> context.getString(R.string.duration_days_hours, minutes / (24 * 60), (minutes % (24 * 60)) / 60)
                },
            ))
        }
    }

    private fun metric(context: Context, label: Int, current: Double, previous: Double, value: String): Metric {
        val diff = current - previous
        return Metric(
            label = context.getString(label),
            value = value,
            change = previous.takeIf { it > 0.0 }?.let { String.format(Locale.US, "%.1f%%", abs(diff) / it * 100.0) },
            isDown = previous.takeIf { it > 0.0 }?.let { diff < 0 },
        )
    }

    private fun OverviewCompletionPeriod.labelRes(): Int = when (this) {
        OverviewCompletionPeriod.OVERNIGHT -> R.string.time_bucket_late_night
        OverviewCompletionPeriod.MORNING -> R.string.time_bucket_morning
        OverviewCompletionPeriod.AFTERNOON -> R.string.time_bucket_afternoon
        OverviewCompletionPeriod.EVENING -> R.string.time_bucket_evening
    }
}
