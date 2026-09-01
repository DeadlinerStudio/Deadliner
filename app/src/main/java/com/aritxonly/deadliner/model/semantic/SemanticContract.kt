package com.aritxonly.deadliner.model.semantic

import com.deadliner.domain.task.TaskState
import java.time.LocalDateTime

enum class DeadlineSemanticStatus {
    ACTIVE,
    NEAR_DUE,
    OVERDUE,
    COMPLETED,
    ABANDONED,
}

enum class SemanticTone {
    ACCENT,
    SUCCESS,
    WARNING,
    DANGER,
    CALM,
    NEUTRAL,
}

enum class SemanticPageKind {
    TASKS,
    HABITS,
}

data class DeadlineSemanticInput(
    val id: String,
    val state: TaskState,
    val dueAt: LocalDateTime?,
)

data class HabitSemanticInput(
    val id: String,
    val isCompleted: Boolean,
)

data class SemanticSnapshot(
    val kind: SemanticPageKind,
    val tone: SemanticTone,
    val taskStatusById: Map<String, DeadlineSemanticStatus> = emptyMap(),
    val overdueCount: Int = 0,
    val nearDueCount: Int = 0,
    val openTaskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val abandonedTaskCount: Int = 0,
    val visibleTaskCount: Int = 0,
    val completedHabitCount: Int = 0,
    val visibleHabitCount: Int = 0,
) {
    fun statusForTask(id: String): DeadlineSemanticStatus =
        taskStatusById[id] ?: DeadlineSemanticStatus.ACTIVE

    companion object {
        fun empty(kind: SemanticPageKind): SemanticSnapshot = SemanticSnapshot(
            kind = kind,
            tone = if (kind == SemanticPageKind.TASKS) SemanticTone.SUCCESS else SemanticTone.ACCENT,
        )
    }
}
