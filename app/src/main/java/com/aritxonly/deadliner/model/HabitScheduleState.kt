package com.aritxonly.deadliner.model

import android.content.Context
import com.aritxonly.deadliner.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val EBBINGHAUS_REVIEW_DAYS = listOf(0L, 1L, 2L, 4L, 7L, 15L, 30L, 60L)

enum class HabitScheduleHintKind {
    STARTS_IN,
    NEXT_REVIEW_IN,
    COMPLETED_CYCLE,
}

data class HabitScheduleState(
    val isDue: Boolean,
    val hintKind: HabitScheduleHintKind? = null,
    val daysUntil: Int? = null,
)

fun Habit.scheduleStateOn(targetDate: LocalDate): HabitScheduleState? {
    if (period != HabitPeriod.EBBINGHAUS) return null

    val createdDate = createdAt.toLocalDate()
    val diffDays = ChronoUnit.DAYS.between(createdDate, targetDate)

    if (diffDays < 0) {
        return HabitScheduleState(
            isDue = false,
            hintKind = HabitScheduleHintKind.STARTS_IN,
            daysUntil = (-diffDays).toInt(),
        )
    }

    if (EBBINGHAUS_REVIEW_DAYS.contains(diffDays)) {
        return HabitScheduleState(isDue = true)
    }

    val nextReviewDay = EBBINGHAUS_REVIEW_DAYS.firstOrNull { it > diffDays }
    return if (nextReviewDay != null) {
        HabitScheduleState(
            isDue = false,
            hintKind = HabitScheduleHintKind.NEXT_REVIEW_IN,
            daysUntil = (nextReviewDay - diffDays).toInt(),
        )
    } else {
        HabitScheduleState(
            isDue = false,
            hintKind = HabitScheduleHintKind.COMPLETED_CYCLE,
        )
    }
}

fun Habit.isReviewDueOn(targetDate: LocalDate): Boolean {
    return scheduleStateOn(targetDate)?.isDue ?: true
}

fun HabitScheduleState.formatHint(context: Context): String? {
    if (isDue) return null

    return when (hintKind) {
        HabitScheduleHintKind.STARTS_IN -> context.getString(
            R.string.habit_ebbinghaus_starts_in_days,
            daysUntil ?: 0,
        )

        HabitScheduleHintKind.NEXT_REVIEW_IN -> context.getString(
            R.string.habit_ebbinghaus_review_in_days,
            daysUntil ?: 0,
        )

        HabitScheduleHintKind.COMPLETED_CYCLE -> context.getString(
            R.string.habit_ebbinghaus_cycle_completed,
        )

        null -> null
    }
}
