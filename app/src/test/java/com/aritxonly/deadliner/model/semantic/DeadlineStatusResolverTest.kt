package com.aritxonly.deadliner.model.semantic

import com.aritxonly.deadliner.model.DDLStatus
import com.deadliner.domain.task.TaskState
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DeadlineStatusResolverTest {
    private val now = LocalDateTime.of(2026, 9, 1, 12, 0)

    @Test
    fun abandonedAndCompletedStatesWinOverPastDeadline() {
        val past = now.minusDays(2)

        assertEquals(
            DeadlineSemanticStatus.ABANDONED,
            DeadlineStatusResolver.resolve(TaskState.ABANDONED, past, now),
        )
        assertEquals(
            DeadlineSemanticStatus.ABANDONED,
            DeadlineStatusResolver.resolve(TaskState.ABANDONED_ARCHIVED, past, now),
        )
        assertEquals(
            DeadlineSemanticStatus.COMPLETED,
            DeadlineStatusResolver.resolve(TaskState.COMPLETED, past, now),
        )
        assertEquals(
            DeadlineSemanticStatus.COMPLETED,
            DeadlineStatusResolver.resolve(TaskState.ARCHIVED, past, now),
        )
    }

    @Test
    fun activeDeadlineUsesTwentyFourHourBoundary() {
        assertEquals(
            DeadlineSemanticStatus.OVERDUE,
            DeadlineStatusResolver.resolve(TaskState.ACTIVE, now.minusNanos(1), now),
        )
        assertEquals(
            DeadlineSemanticStatus.NEAR_DUE,
            DeadlineStatusResolver.resolve(TaskState.ACTIVE, now, now),
        )
        assertEquals(
            DeadlineSemanticStatus.NEAR_DUE,
            DeadlineStatusResolver.resolve(TaskState.ACTIVE, now.plusHours(24), now),
        )
        assertEquals(
            DeadlineSemanticStatus.ACTIVE,
            DeadlineStatusResolver.resolve(TaskState.ACTIVE, now.plusHours(24).plusNanos(1), now),
        )
    }

    @Test
    fun missingDeadlineIsActiveAndLegacyStatusUsesSameResolver() {
        assertEquals(
            DeadlineSemanticStatus.ACTIVE,
            DeadlineStatusResolver.resolve(TaskState.ACTIVE, null, now),
        )
        assertEquals(
            DDLStatus.NEAR,
            DDLStatus.calculateStatus(null, now.plusHours(20), now, isCompleted = false),
        )
    }
}
