package com.aritxonly.deadliner.widgets.glance.data

import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetItem
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.ceil

internal interface TaskWidgetStrings {
    fun overdue(): String
    fun dueInMinutes(minutes: Long): String
    fun dueInHours(hours: Long): String
    fun dueInDays(days: Long): String
    fun dueOn(month: Int, day: Int): String
}

internal class TaskWidgetRepository(
    private val source: TaskWidgetDataSource,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val nowProvider: () -> LocalDateTime = { LocalDateTime.now() },
) {
    fun load(strings: TaskWidgetStrings): TaskWidgetSnapshot {
        val now = nowProvider()
        val tasks = source.loadTasks()
            .asSequence()
            .filter { !it.isDeleted && it.isActive && !it.dueAt.isNullOrBlank() }
            .mapNotNull { record ->
                parseDateTime(record.dueAt.orEmpty())?.let { dueAt ->
                    ParsedTask(record = record, dueAt = dueAt)
                }
            }
            .sortedWith(
                compareByDescending<ParsedTask> { it.record.isStarred }
                    .thenBy { it.dueAt }
            )
            .toList()

        return TaskWidgetSnapshot(
            totalCount = tasks.size,
            items = tasks.take(MAX_VISIBLE_TASKS).map { parsed ->
                val remaining = Duration.between(now, parsed.dueAt)
                TaskWidgetItem(
                    title = parsed.record.title,
                    timeDescription = remaining.format(parsed.dueAt, strings),
                    isUrgent = remaining.isNegative || remaining <= URGENT_WINDOW,
                )
            },
        )
    }

    private fun parseDateTime(raw: String): LocalDateTime? =
        runCatching { LocalDateTime.parse(raw) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(zoneId).toLocalDateTime() }.getOrNull()
            ?: runCatching { Instant.parse(raw).atZone(zoneId).toLocalDateTime() }.getOrNull()

    private fun Duration.format(dueAt: LocalDateTime, strings: TaskWidgetStrings): String {
        if (isZero || isNegative) return strings.overdue()

        val minutes = toMinutes().coerceAtLeast(1)
        return when {
            minutes < 60 -> strings.dueInMinutes(minutes)
            minutes < MINUTES_PER_DAY -> strings.dueInHours(ceil(minutes / 60.0).toLong())
            minutes < MINUTES_PER_WEEK -> strings.dueInDays(ceil(minutes / MINUTES_PER_DAY.toDouble()).toLong())
            else -> strings.dueOn(dueAt.monthValue, dueAt.dayOfMonth)
        }
    }

    private data class ParsedTask(
        val record: TaskWidgetTaskRecord,
        val dueAt: LocalDateTime,
    )

    private companion object {
        const val MAX_VISIBLE_TASKS = 3
        const val MINUTES_PER_DAY = 24L * 60L
        const val MINUTES_PER_WEEK = 7L * MINUTES_PER_DAY
        val URGENT_WINDOW: Duration = Duration.ofHours(24)
    }
}
