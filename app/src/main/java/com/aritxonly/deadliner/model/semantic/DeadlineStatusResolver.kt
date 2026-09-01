package com.aritxonly.deadliner.model.semantic

import com.aritxonly.deadliner.model.DDLStatus
import com.deadliner.domain.task.TaskState
import java.time.Duration
import java.time.LocalDateTime

object DeadlineStatusResolver {
    const val NearDueWindowHours: Long = 24L

    fun resolve(
        state: TaskState,
        dueAt: LocalDateTime?,
        now: LocalDateTime = LocalDateTime.now(),
    ): DeadlineSemanticStatus {
        if (state == TaskState.ABANDONED || state == TaskState.ABANDONED_ARCHIVED) {
            return DeadlineSemanticStatus.ABANDONED
        }
        if (state == TaskState.COMPLETED || state == TaskState.ARCHIVED) {
            return DeadlineSemanticStatus.COMPLETED
        }
        if (dueAt == null) return DeadlineSemanticStatus.ACTIVE
        if (dueAt.isBefore(now)) return DeadlineSemanticStatus.OVERDUE

        return if (Duration.between(now, dueAt) <= Duration.ofHours(NearDueWindowHours)) {
            DeadlineSemanticStatus.NEAR_DUE
        } else {
            DeadlineSemanticStatus.ACTIVE
        }
    }
}

fun DeadlineSemanticStatus.toDDLStatus(): DDLStatus = when (this) {
    DeadlineSemanticStatus.ACTIVE -> DDLStatus.UNDERGO
    DeadlineSemanticStatus.NEAR_DUE -> DDLStatus.NEAR
    DeadlineSemanticStatus.OVERDUE -> DDLStatus.PASSED
    DeadlineSemanticStatus.COMPLETED -> DDLStatus.COMPLETED
    DeadlineSemanticStatus.ABANDONED -> DDLStatus.COMPLETED
}
