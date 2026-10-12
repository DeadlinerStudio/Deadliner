package com.aritxonly.deadliner.ui.overview

import com.deadliner.domain.statistics.FocusDurationPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FocusStatisticsPresenterTest {
    @Test fun alignedDateLabelsAndSparseMonthDaysKeepDistinctUnits() {
        val points = listOf(
            FocusDurationPoint("2026-09-01", "2026-09-02", 30),
            FocusDurationPoint("2026-09-05", "2026-09-06", 120),
            FocusDurationPoint("2026-09-30", "2026-10-01", 300),
        )
        assertEquals(listOf("1", "5", "30"), focusChartLabels(points, FocusChartKind.MONTH_DAILY, Locale.US))
        assertEquals(listOf("09-01", "09-05", "09-30"), focusChartLabels(points, FocusChartKind.WEEKLY, Locale.US))
        assertEquals(listOf("9", "9", "9"), focusChartLabels(points, FocusChartKind.MONTHLY, Locale.US))
    }
}
