package com.aritxonly.deadliner.widgets.glance.data

import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetItem
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

internal interface TaskWidgetStrings {
    fun overdue(): String
    fun dueInMinutes(minutes: Long): String
    fun dueInHours(hours: Long): String
    fun dueInDays(days: Long): String
    fun unscheduled(): String
    fun emptySummary(): String
    fun overdueSummary(): String
    fun nearSummary(): String
    fun pendingSummary(): String
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
            .filter { !it.isDeleted && it.isActive }
            .map { record ->
                ParsedTask(
                    record = record,
                    dueAt = record.dueAt
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::parseDateTime),
                )
            }
            .sortedBy { it.dueAt ?: LocalDateTime.MAX }
            .toList()

        val statuses = tasks.associateWith { parsed -> parsed.statusAt(now) }
        val nearCount = statuses.values.count { it == TaskWidgetStatus.NEAR }
        val overdueCount = statuses.values.count { it == TaskWidgetStatus.OVERDUE }
        val summary = when {
            tasks.isEmpty() -> strings.emptySummary()
            overdueCount > 0 -> strings.overdueSummary()
            nearCount > 0 -> strings.nearSummary()
            else -> strings.pendingSummary()
        }

        return TaskWidgetSnapshot(
            totalCount = tasks.size,
            nearCount = nearCount,
            overdueCount = overdueCount,
            summary = summary,
            items = tasks.take(MAX_VISIBLE_TASKS).map { parsed ->
                val remaining = parsed.dueAt?.let { Duration.between(now, it) }
                TaskWidgetItem(
                    title = parsed.record.title,
                    timeDescription = remaining?.format(strings) ?: strings.unscheduled(),
                    status = statuses.getValue(parsed),
                )
            },
        )
    }

    private fun parseDateTime(raw: String): LocalDateTime? =
        runCatching { LocalDateTime.parse(raw) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(zoneId).toLocalDateTime() }.getOrNull()
            ?: runCatching { Instant.parse(raw).atZone(zoneId).toLocalDateTime() }.getOrNull()

    private fun Duration.format(strings: TaskWidgetStrings): String {
        if (isZero || isNegative) return strings.overdue()

        val minutes = toMinutes().coerceAtLeast(1)
        return when {
            minutes < 60 -> strings.dueInMinutes(minutes)
            minutes < MINUTES_PER_DAY -> strings.dueInHours(minutes / 60L)
            else -> strings.dueInDays(minutes / MINUTES_PER_DAY)
        }
    }

    private fun ParsedTask.statusAt(now: LocalDateTime): TaskWidgetStatus {
        val deadline = dueAt ?: return TaskWidgetStatus.NORMAL
        val remaining = Duration.between(now, deadline)
        return when {
            remaining.isZero || remaining.isNegative -> TaskWidgetStatus.OVERDUE
            remaining <= NEAR_WINDOW -> TaskWidgetStatus.NEAR
            else -> TaskWidgetStatus.NORMAL
        }
    }

    private data class ParsedTask(
        val record: TaskWidgetTaskRecord,
        val dueAt: LocalDateTime?,
    )

    private companion object {
        const val MAX_VISIBLE_TASKS = 3
        const val MINUTES_PER_DAY = 24L * 60L
        val NEAR_WINDOW: Duration = Duration.ofHours(24)
    }
}
