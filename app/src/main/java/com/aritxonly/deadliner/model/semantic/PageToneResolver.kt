package com.aritxonly.deadliner.model.semantic

import java.time.LocalDateTime

object PageToneResolver {
    fun resolveTasks(
        items: List<DeadlineSemanticInput>,
        now: LocalDateTime = LocalDateTime.now(),
    ): SemanticSnapshot {
        val statusById = LinkedHashMap<String, DeadlineSemanticStatus>(items.size)
        var overdueCount = 0
        var nearDueCount = 0
        var openTaskCount = 0
        var completedTaskCount = 0
        var abandonedTaskCount = 0

        items.forEach { item ->
            val status = DeadlineStatusResolver.resolve(item.state, item.dueAt, now)
            statusById[item.id] = status
            when (status) {
                DeadlineSemanticStatus.OVERDUE -> {
                    overdueCount++
                    openTaskCount++
                }

                DeadlineSemanticStatus.NEAR_DUE -> {
                    nearDueCount++
                    openTaskCount++
                }

                DeadlineSemanticStatus.ACTIVE -> openTaskCount++
                DeadlineSemanticStatus.COMPLETED -> completedTaskCount++
                DeadlineSemanticStatus.ABANDONED -> abandonedTaskCount++
            }
        }

        val tone = when {
            overdueCount > 0 -> SemanticTone.DANGER
            nearDueCount > 0 -> SemanticTone.WARNING
            openTaskCount > 0 -> SemanticTone.ACCENT
            else -> SemanticTone.SUCCESS
        }

        return SemanticSnapshot(
            kind = SemanticPageKind.TASKS,
            tone = tone,
            taskStatusById = statusById,
            overdueCount = overdueCount,
            nearDueCount = nearDueCount,
            openTaskCount = openTaskCount,
            completedTaskCount = completedTaskCount,
            abandonedTaskCount = abandonedTaskCount,
            visibleTaskCount = items.size,
        )
    }

    fun resolveHabits(items: List<HabitSemanticInput>): SemanticSnapshot {
        val completedHabitCount = items.count(HabitSemanticInput::isCompleted)
        val tone = if (items.isNotEmpty() && completedHabitCount == items.size) {
            SemanticTone.SUCCESS
        } else {
            SemanticTone.ACCENT
        }
        return SemanticSnapshot(
            kind = SemanticPageKind.HABITS,
            tone = tone,
            completedHabitCount = completedHabitCount,
            visibleHabitCount = items.size,
        )
    }
}
