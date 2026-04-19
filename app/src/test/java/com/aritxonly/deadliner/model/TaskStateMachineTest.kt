package com.aritxonly.deadliner.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TaskStateMachineTest {
    private val now = LocalDateTime.parse("2026-03-31T12:00:00")

    @Test
    fun activeMarkCompleteToCompleted() {
        assertEquals(DDLState.COMPLETED, TaskStateMachine.nextState(DDLState.ACTIVE, TaskStateAction.MARK_COMPLETE))
    }

    @Test
    fun completedMarkArchiveToArchived() {
        assertEquals(DDLState.ARCHIVED, TaskStateMachine.nextState(DDLState.COMPLETED, TaskStateAction.MARK_ARCHIVE))
    }

    @Test
    fun activeMarkGiveUpToAbandoned() {
        val item = sampleItem(state = DDLState.ACTIVE).transition(TaskStateAction.MARK_GIVE_UP, confirmed = true, now = now)
        assertEquals(DDLState.ABANDONED, item.state)
        assertEquals(now.toString(), item.completeTime)
    }

    @Test
    fun abandonedMarkArchiveToAbandonedArchived() {
        assertEquals(DDLState.ABANDONED_ARCHIVED, TaskStateMachine.nextState(DDLState.ABANDONED, TaskStateAction.MARK_ARCHIVE))
    }

    @Test
    fun completedRestoreActiveToActive() {
        val item = sampleItem(state = DDLState.COMPLETED, completeTime = now.minusDays(1).toString())
            .transition(TaskStateAction.RESTORE_ACTIVE, confirmed = true, now = now)
        assertEquals(DDLState.ACTIVE, item.state)
        assertEquals("", item.completeTime)
    }

    @Test
    fun abandonedRestoreActiveToActive() {
        assertEquals(DDLState.ACTIVE, TaskStateMachine.nextState(DDLState.ABANDONED, TaskStateAction.RESTORE_ACTIVE))
    }

    @Test
    fun archivedUnarchiveToCompleted() {
        assertEquals(DDLState.COMPLETED, TaskStateMachine.nextState(DDLState.ARCHIVED, TaskStateAction.UNARCHIVE))
    }

    @Test
    fun abandonedArchivedUnarchiveToAbandoned() {
        assertEquals(DDLState.ABANDONED, TaskStateMachine.nextState(DDLState.ABANDONED_ARCHIVED, TaskStateAction.UNARCHIVE))
    }

    @Test(expected = InvalidTaskStateTransitionException::class)
    fun invalidTransitionFailsExplicitly() {
        TaskStateMachine.nextState(DDLState.ACTIVE, TaskStateAction.MARK_ARCHIVE)
    }

    @Test(expected = TaskActionConfirmationRequiredException::class)
    fun giveUpRequiresConfirmation() {
        sampleItem(state = DDLState.ACTIVE).transition(TaskStateAction.MARK_GIVE_UP, confirmed = false, now = now)
    }

    @Test
    fun repeatedMarkCompleteOnCompletedIsNoOp() {
        assertEquals(true, TaskStateMachine.isNoOp(DDLState.COMPLETED, TaskStateAction.MARK_COMPLETE))
    }

    @Test
    fun repeatedRestoreActiveOnActiveIsNoOp() {
        assertEquals(true, TaskStateMachine.isNoOp(DDLState.ACTIVE, TaskStateAction.RESTORE_ACTIVE))
    }

    private fun sampleItem(
        state: DDLState,
        completeTime: String = ""
    ): DDLItem {
        return DDLItem(
            id = 1,
            name = "Task",
            startTime = "2026-03-31T08:00:00",
            endTime = "2026-03-31T18:00:00",
            state = state,
            completeTime = completeTime,
            note = ""
        )
    }
}
