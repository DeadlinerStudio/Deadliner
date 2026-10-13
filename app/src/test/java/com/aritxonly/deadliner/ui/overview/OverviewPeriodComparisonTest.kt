package com.aritxonly.deadliner.ui.overview

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class OverviewPeriodComparisonTest {
    @Test fun currentMonthExcludesTheLaterDaysOfPreviousMonth() {
        val comparison = compareOverviewPeriod(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 12),
            mapOf(LocalDate.of(2026, 10, 12) to 4),
            mapOf(LocalDate.of(2026, 9, 12) to 2, LocalDate.of(2026, 9, 13) to 100))
        assertEquals(2, comparison.previous)
        assertEquals(100.0, comparison.percent!!, .001)
        assertEquals(12, comparison.throughDay)
    }
    @Test fun historicalMonthUsesMatchingDaysRatherThanWholePreviousMonth() {
        val comparison = compareOverviewPeriod(YearMonth.of(2026, 2), LocalDate.of(2026, 10, 12),
            mapOf(LocalDate.of(2026, 2, 28) to 4),
            mapOf(LocalDate.of(2026, 1, 28) to 8, LocalDate.of(2026, 1, 31) to 100))
        assertEquals(8, comparison.previous)
        assertEquals(-50.0, comparison.percent!!, .001)
    }
    @Test fun shorterPreviousMonthClampsToItsLastDayAndZeroHasNoPercentage() {
        val comparison = compareOverviewPeriod(YearMonth.of(2026, 3), LocalDate.of(2026, 3, 31),
            mapOf(LocalDate.of(2026, 3, 31) to 2), emptyMap())
        assertEquals(28, comparison.previousThroughDay)
        assertNull(comparison.percent)
        assertEquals(2, comparison.difference)
    }
    @Test fun reviewMonthIsTheLastCompletedMonthAcrossYearBoundary() {
        assertEquals(YearMonth.of(2025, 12), latestCompletedOverviewMonth(LocalDate.of(2026, 1, 1)))
    }
}
