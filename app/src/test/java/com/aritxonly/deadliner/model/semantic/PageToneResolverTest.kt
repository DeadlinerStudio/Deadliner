package com.aritxonly.deadliner.model.semantic

import com.deadliner.domain.task.TaskState
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PageToneResolverTest {
    private val now = LocalDateTime.of(2026, 9, 1, 12, 0)

    @Test
    fun taskToneUsesHighestVisibleRisk() {
        val inputs = listOf(
            task("active", TaskState.ACTIVE, now.plusDays(5)),
            task("near", TaskState.ACTIVE, now.plusHours(4)),
            task("overdue", TaskState.ACTIVE, now.minusMinutes(1)),
            task("completed", TaskState.COMPLETED, now.minusDays(1)),
        )

        val snapshot = PageToneResolver.resolveTasks(inputs, now)

        assertEquals(SemanticTone.DANGER, snapshot.tone)
        assertEquals(1, snapshot.overdueCount)
        assertEquals(1, snapshot.nearDueCount)
        assertEquals(3, snapshot.openTaskCount)
        assertEquals(1, snapshot.completedTaskCount)
        assertEquals(DeadlineSemanticStatus.OVERDUE, snapshot.statusForTask("overdue"))
    }

    @Test
    fun tasksWithoutOpenItemsUseSuccess() {
        val snapshot = PageToneResolver.resolveTasks(
            listOf(
                task("done", TaskState.COMPLETED, now.minusDays(1)),
                task("abandoned", TaskState.ABANDONED, now.minusDays(1)),
            ),
            now,
        )

        assertEquals(SemanticTone.SUCCESS, snapshot.tone)
        assertEquals(0, snapshot.openTaskCount)
        assertEquals(1, snapshot.abandonedTaskCount)
    }

    @Test
    fun habitsUseSuccessOnlyWhenVisibleSetIsNonEmptyAndComplete() {
        assertEquals(
            SemanticTone.ACCENT,
            PageToneResolver.resolveHabits(emptyList()).tone,
        )
        assertEquals(
            SemanticTone.ACCENT,
            PageToneResolver.resolveHabits(
                listOf(HabitSemanticInput("one", true), HabitSemanticInput("two", false)),
            ).tone,
        )
        assertEquals(
            SemanticTone.SUCCESS,
            PageToneResolver.resolveHabits(
                listOf(HabitSemanticInput("one", true), HabitSemanticInput("two", true)),
            ).tone,
        )
    }

    private fun task(
        id: String,
        state: TaskState,
        dueAt: LocalDateTime?,
    ) = DeadlineSemanticInput(id = id, state = state, dueAt = dueAt)
}
