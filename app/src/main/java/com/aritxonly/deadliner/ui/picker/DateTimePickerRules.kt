package com.aritxonly.deadliner.ui.picker

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.Instant
import kotlin.math.roundToInt

/** Material encodes the chosen calendar day at UTC midnight, independently of the device zone. */
internal fun dateToUtcMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
internal fun utcMillisToDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/** A minute-only picker must round its minimum up, including a possible rollover to tomorrow. */
internal fun ceilToPickerMinute(value: LocalDateTime): LocalDateTime {
    val minute = value.withSecond(0).withNano(0)
    return if (value == minute) minute else minute.plusMinutes(1)
}

internal data class PickerDateBounds(val minimum: LocalDate, val maximum: LocalDate) {
    init { require(!maximum.isBefore(minimum)) }
    val years: IntRange get() = minimum.year..maximum.year
    fun months(year: Int): IntRange =
        (if (year == minimum.year) minimum.monthValue else 1)..
            (if (year == maximum.year) maximum.monthValue else 12)
    fun days(year: Int, month: Int): IntRange =
        (if (year == minimum.year && month == minimum.monthValue) minimum.dayOfMonth else 1)..
            (if (year == maximum.year && month == maximum.monthValue) maximum.dayOfMonth
                else LocalDate.of(year, month, 1).lengthOfMonth())
    fun clamp(year: Int, month: Int, day: Int): LocalDate {
        val y = year.coerceIn(years)
        val m = month.coerceIn(months(y))
        return LocalDate.of(y, m, day.coerceIn(days(y, m)))
    }
}

internal fun pickerMinimumTime(date: LocalDate, minimum: LocalDateTime?): LocalTime? =
    minimum?.takeIf { it.toLocalDate() == date }?.toLocalTime()

internal fun pickerMinuteRange(hour: Int, minimum: LocalTime?): IntRange =
    (if (minimum?.hour == hour) minimum.minute else 0)..59

/** Chronological indices retain direction and multiple turns, unlike wrapped display digits. */
internal fun pickerMonthIndex(date: LocalDate): Int = date.year * 12 + date.monthValue - 1

internal fun dateAtPickerMonth(index: Int, previous: LocalDate, bounds: PickerDateBounds): LocalDate =
    bounds.clamp(Math.floorDiv(index, 12), Math.floorMod(index, 12) + 1, previous.dayOfMonth)

internal fun pickerMinuteIndex(value: LocalDateTime): Int =
    (value.toLocalDate().toEpochDay() * 1440 + value.hour * 60 + value.minute).toInt()

internal fun dateTimeAtPickerMinute(index: Int): LocalDateTime =
    LocalDate.ofEpochDay(Math.floorDiv(index, 1440).toLong())
        .atTime(LocalTime.ofSecondOfDay(Math.floorMod(index, 1440) * 60L))

internal fun dateTimeAtPickerHour(index: Int, minute: Int): LocalDateTime =
    LocalDate.ofEpochDay(Math.floorDiv(index, 24).toLong()).atTime(Math.floorMod(index, 24), minute)

/** Offset stays relative to the gesture origin even when the parent echoes a live selection. */
internal fun pickerScrolledValue(anchor: Int, offset: Float, range: IntRange, wrapAround: Boolean): Int {
    val raw = anchor.toLong() + offset.roundToInt()
    return if (wrapAround) {
        (range.first + Math.floorMod(raw - range.first, range.last.toLong() - range.first + 1)).toInt()
    } else raw.coerceIn(range.first.toLong(), range.last.toLong()).toInt()
}
