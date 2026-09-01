package com.aritxonly.deadliner.model

import com.aritxonly.deadliner.model.semantic.DeadlineStatusResolver
import com.aritxonly.deadliner.model.semantic.toDDLStatus
import com.deadliner.domain.task.TaskState
import java.time.LocalDateTime

enum class DDLStatus {
    COMPLETED,
    UNDERGO,
    NEAR,
    PASSED;

    companion object {
        fun calculateStatus(
            startTime: LocalDateTime?,
            endTime: LocalDateTime?,
            now: LocalDateTime = LocalDateTime.now(),
            isCompleted: Boolean
        ): DDLStatus = DeadlineStatusResolver.resolve(
            state = if (isCompleted) TaskState.COMPLETED else TaskState.ACTIVE,
            dueAt = endTime,
            now = now,
        ).toDDLStatus()
    }
}
