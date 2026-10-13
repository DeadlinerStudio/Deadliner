package com.aritxonly.deadliner.ui.overview

import java.time.LocalDate
import java.time.YearMonth

internal data class OverviewPeriodComparison(
    val current: Int,
    val previous: Int,
    val throughDay: Int,
    val previousThroughDay: Int,
) {
    val difference: Int get() = current - previous
    val percent: Double? get() = previous.takeIf { it > 0 }?.let { difference * 100.0 / it }
}

/** Compare matching calendar-day spans, never a partial month against a full prior month. */
internal fun compareOverviewPeriod(month: YearMonth, today: LocalDate,
    current: Map<LocalDate, Int>, previous: Map<LocalDate, Int>): OverviewPeriodComparison {
    val through = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
    val previousMonth = month.minusMonths(1)
    val previousThrough = minOf(through, previousMonth.lengthOfMonth())
    fun total(values: Map<LocalDate, Int>, target: YearMonth, lastDay: Int) =
        values.filterKeys { YearMonth.from(it) == target && it.dayOfMonth <= lastDay }.values.sum()
    return OverviewPeriodComparison(total(current, month, through), total(previous, previousMonth, previousThrough),
        through, previousThrough)
}

internal fun latestCompletedOverviewMonth(today: LocalDate): YearMonth = YearMonth.from(today).minusMonths(1)
