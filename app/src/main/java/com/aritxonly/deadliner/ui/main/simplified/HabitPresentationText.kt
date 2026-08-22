package com.aritxonly.deadliner.ui.main.simplified

import android.content.Context
import com.aritxonly.deadliner.R
import com.deadliner.domain.habit.Habit
import com.deadliner.domain.habit.HabitGoalWindow
import com.deadliner.domain.habit.HabitScheduleKind
import com.deadliner.domain.habit.HabitWeekdays
import com.deadliner.viewmodel.habit.HabitDayStatus
import com.deadliner.viewmodel.habit.HabitScheduleHintKind
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Android-only wording for shared Habit schedule/progress projections. */
internal fun HabitDayStatus.progressText(): String {
    val progress = "$doneCount/$targetCount"
    return habit.tracking.unit?.takeIf(String::isNotBlank)?.let { "$progress $it" } ?: progress
}

internal fun HabitDayStatus.scheduleHintText(context: Context): String? = when (scheduleState.hintKind) {
    HabitScheduleHintKind.STARTS_IN -> scheduleState.daysUntil?.let {
        context.getString(R.string.habit_ebbinghaus_starts_in_days, it)
    }
    HabitScheduleHintKind.NEXT_REVIEW_IN -> scheduleState.daysUntil?.let {
        context.getString(R.string.habit_ebbinghaus_review_in_days, it)
    }
    HabitScheduleHintKind.COMPLETED_CYCLE -> context.getString(R.string.habit_ebbinghaus_cycle_completed)
    null -> null
}

internal fun Habit.presentationLabel(context: Context): String {
    val scheduleText = scheduleLabel(context)
    val goalText = when (goal.window) {
        HabitGoalWindow.SCHEDULED_DAY -> null
        HabitGoalWindow.WEEK -> context.getString(R.string.frequency_weekly)
        HabitGoalWindow.MONTH -> context.getString(R.string.frequency_monthly)
        HabitGoalWindow.LIFETIME -> context.getString(R.string.habit_goal_total_with_target, goal.targetCount)
    }
    return listOfNotNull(scheduleText, goalText).distinct().joinToString(" · ")
}

private fun Habit.scheduleLabel(context: Context): String = when (schedule.kind) {
    HabitScheduleKind.ONCE -> context.getString(R.string.frequency_once)
    HabitScheduleKind.ALGORITHM -> context.getString(R.string.editor_habit_period_ebbinghaus)
    HabitScheduleKind.WEEKLY_DAYS -> {
        val mask = schedule.weekdaysMask ?: HabitWeekdays.ALL
        when (mask) {
            HabitWeekdays.ALL -> context.getString(R.string.editor_habit_schedule_daily)
            HabitWeekdays.WEEKDAYS -> context.getString(R.string.editor_habit_schedule_workdays)
            HabitWeekdays.WEEKENDS -> context.getString(R.string.editor_habit_schedule_weekend)
            else -> DayOfWeek.entries
                    .filterIndexed { index, _ -> mask and (1 shl index) != 0 }
                    .joinToString("/") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
        }
    }
}
