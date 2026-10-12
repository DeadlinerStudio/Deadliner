package com.aritxonly.deadliner.ui.picker

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class DateTimePickerRulesTest {
    private val bounds = PickerDateBounds(LocalDate.of(1900, 1, 1), LocalDate.of(2100, 12, 31))

    @Test fun monthAndYearChangesClampInvalidDays() {
        assertEquals(LocalDate.of(2024, 2, 29), bounds.clamp(2024, 2, 31))
        assertEquals(LocalDate.of(2025, 2, 28), bounds.clamp(2025, 2, 29))
        assertEquals(LocalDate.of(2025, 4, 30), bounds.clamp(2025, 4, 31))
        assertEquals(LocalDate.of(2100, 2, 28), bounds.clamp(2100, 2, 29))
    }

    @Test fun dateBoundsRestrictEveryColumnAtBothEnds() {
        val range = PickerDateBounds(LocalDate.of(2026, 10, 9), LocalDate.of(2027, 2, 3))
        assertEquals(10..12, range.months(2026))
        assertEquals(1..2, range.months(2027))
        assertEquals(9..31, range.days(2026, 10))
        assertEquals(1..3, range.days(2027, 2))
        assertEquals(range.minimum, range.clamp(2025, 1, 1))
        assertEquals(range.maximum, range.clamp(2028, 12, 31))
    }

    @Test fun singletonDateRangeRemainsValid() {
        val date = LocalDate.of(2026, 10, 9)
        val range = PickerDateBounds(date, date)
        assertEquals(2026..2026, range.years)
        assertEquals(10..10, range.months(2026))
        assertEquals(9..9, range.days(2026, 10))
    }

    @Test fun minimumRoundsUpToTheNextSelectableMinute() {
        val exact = LocalDateTime.of(2026, 10, 9, 10, 30)
        assertEquals(exact, ceilToPickerMinute(exact))
        assertEquals(exact.plusMinutes(1), ceilToPickerMinute(exact.plusSeconds(1)))
        assertEquals(exact.plusMinutes(1), ceilToPickerMinute(exact.plusNanos(1)))
        assertEquals(LocalDateTime.of(2026, 10, 10, 0, 0),
            ceilToPickerMinute(LocalDateTime.of(2026, 10, 9, 23, 59, 59)))
    }

    @Test fun minimumTimeAppliesOnlyToTheBoundaryDate() {
        val minimum = LocalDateTime.of(2026, 10, 9, 15, 42)
        assertEquals(LocalTime.of(15, 42), pickerMinimumTime(minimum.toLocalDate(), minimum))
        assertNull(pickerMinimumTime(minimum.toLocalDate().plusDays(1), minimum))
        assertEquals(42..59, pickerMinuteRange(15, minimum.toLocalTime()))
        assertEquals(0..59, pickerMinuteRange(16, minimum.toLocalTime()))
    }

    @Test fun materialDatesRemainTheSameInPositiveAndNegativeTimeZones() {
        val original = TimeZone.getDefault()
        try {
            val date = LocalDate.of(2026, 10, 9)
            for (zone in listOf("Asia/Shanghai", "America/Los_Angeles", "Pacific/Kiritimati")) {
                TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone)))
                assertEquals(date, utcMillisToDate(dateToUtcMillis(date)))
            }
        } finally { TimeZone.setDefault(original) }
    }

    @Test fun dayWheelCarriesAcrossMonthsYearsAndLeapDaysInBothDirections() {
        for (date in listOf(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 12, 31),
            LocalDate.of(2024, 2, 28), LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28))) {
            val next = LocalDate.ofEpochDay(date.toEpochDay() + 1)
            assertEquals(date.plusDays(1), next)
            assertEquals(date, LocalDate.ofEpochDay(next.toEpochDay() - 1))
        }
    }

    @Test fun monthWheelCarriesYearsAndClampsMonthLength() {
        val december = LocalDate.of(2026, 12, 31)
        assertEquals(LocalDate.of(2027, 1, 31), dateAtPickerMonth(pickerMonthIndex(december) + 1, december, bounds))
        assertEquals(LocalDate.of(2026, 2, 28), dateAtPickerMonth(pickerMonthIndex(december) - 10, december, bounds))
        assertEquals(LocalDate.of(2026, 12, 31), dateAtPickerMonth(pickerMonthIndex(december) + 1 - 1, december, bounds))
    }

    @Test fun minuteWheelRetainsMultipleTurnsAndCarriesAcrossMidnight() {
        val value = LocalDateTime.of(2026, 10, 31, 23, 59)
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), dateTimeAtPickerMinute(pickerMinuteIndex(value) + 1))
        assertEquals(value.plusMinutes(121), dateTimeAtPickerMinute(pickerMinuteIndex(value) + 121))
        assertEquals(value.minusMinutes(121), dateTimeAtPickerMinute(pickerMinuteIndex(value) - 121))
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 59),
            dateTimeAtPickerHour(Math.floorDiv(pickerMinuteIndex(value), 60) + 1, value.minute))
    }

    @Test fun chronologicalTimeIndicesAlsoWorkBeforeTheEpoch() {
        val value = LocalDateTime.of(1900, 1, 1, 0, 1)
        assertEquals(value, dateTimeAtPickerMinute(pickerMinuteIndex(value)))
        assertEquals(value.minusMinutes(2), dateTimeAtPickerMinute(pickerMinuteIndex(value) - 2))
    }

    @Test fun liveGestureValuesCarryWhileMotionContinuesWithoutRebasing() {
        val start = LocalDate.of(2026, 10, 30).toEpochDay().toInt()
        val range = bounds.minimum.toEpochDay().toInt()..bounds.maximum.toEpochDay().toInt()
        val frames = listOf(0.2f, 0.8f, 1.2f, 1.8f, 33.2f).map {
            LocalDate.ofEpochDay(pickerScrolledValue(start, it, range, false).toLong())
        }
        assertEquals(listOf(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31),
            LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 2)), frames)
        assertEquals(start, pickerScrolledValue(start, 0f, range, false))
    }

    @Test fun liveOffsetsRespectLimitsAndWrapTimeInBothDirections() {
        assertEquals(0, pickerScrolledValue(1439, 1f, 0..1439, true))
        assertEquals(1439, pickerScrolledValue(0, -1f, 0..1439, true))
        assertEquals(3, pickerScrolledValue(1439, 1444f, 0..1439, true))
        assertEquals(10, pickerScrolledValue(12, -100f, 10..20, false))
        assertEquals(20, pickerScrolledValue(12, 100f, 10..20, false))
    }
}
