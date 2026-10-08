package com.aritxonly.deadliner.notification

import android.content.Context
import com.aritxonly.deadliner.R
import com.deadliner.domain.task.Task
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object TaskIslandPresentation {
    const val BLUE = "#FF8ABEFF"

    fun deadline(context: Context, task: Task, now: Instant = Instant.now()): String =
        deadline(task.dueAt, now, ZoneId.systemDefault()) { time ->
            context.getString(R.string.notification_island_tomorrow, time)
        }

    fun deadline(dueAt: String?, now: Instant, zone: ZoneId, tomorrow: (String) -> String): String {
        val due = requireNotNull(NotificationTiming.dueInstant(dueAt)).atZone(zone)
        val today = now.atZone(zone).toLocalDate()
        val time = due.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))
        return when (due.toLocalDate()) {
            today -> time
            today.plusDays(1) -> tomorrow(time)
            else -> due.format(DateTimeFormatter.ofPattern(
                if (due.year == today.year) "MM-dd HH:mm" else "yyyy-MM-dd HH:mm", Locale.ROOT))
        }
    }

    data class Snapshot(val uid: String, val title: String, val version: String,
                        val deadline: String, val foreground: Boolean, val mode: IslandBypassMode)

    fun snapshot(context: Context, tasks: List<Task>, now: Instant): List<Snapshot> =
        tasks.mapIndexed { index, task -> Snapshot(task.uid, task.title, task.updatedAt,
            deadline(context, task, now), index == 0, IslandWhitelistBypass.mode(context)) }
}
