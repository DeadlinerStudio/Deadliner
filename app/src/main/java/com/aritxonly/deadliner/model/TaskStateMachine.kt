package com.aritxonly.deadliner.model

import java.time.LocalDateTime

enum class TaskStateAction {
    MARK_COMPLETE,
    MARK_ARCHIVE,
    MARK_GIVE_UP,
    RESTORE_ACTIVE,
    UNARCHIVE;

    fun requiresConfirmation(): Boolean = this == MARK_GIVE_UP
}

class InvalidTaskStateTransitionException(
    val from: DDLState,
    val action: TaskStateAction
) : IllegalStateException("Illegal task transition: $from --$action--> ?")

class TaskActionConfirmationRequiredException(
    val action: TaskStateAction
) : IllegalStateException("Confirmation required for action=$action")

object TaskStateMachine {
    fun isNoOp(state: DDLState, action: TaskStateAction): Boolean {
        return when (action) {
            TaskStateAction.MARK_COMPLETE -> state == DDLState.COMPLETED
            TaskStateAction.MARK_GIVE_UP -> state == DDLState.ABANDONED
            TaskStateAction.RESTORE_ACTIVE -> state == DDLState.ACTIVE
            TaskStateAction.MARK_ARCHIVE -> state.isArchivedFamily()
            TaskStateAction.UNARCHIVE -> state == DDLState.COMPLETED || state == DDLState.ABANDONED
        }
    }

    fun nextState(from: DDLState, action: TaskStateAction): DDLState {
        return when (from) {
            DDLState.ACTIVE -> when (action) {
                TaskStateAction.MARK_COMPLETE -> DDLState.COMPLETED
                TaskStateAction.MARK_GIVE_UP -> DDLState.ABANDONED
                else -> throw InvalidTaskStateTransitionException(from, action)
            }
            DDLState.COMPLETED -> when (action) {
                TaskStateAction.MARK_ARCHIVE -> DDLState.ARCHIVED
                TaskStateAction.RESTORE_ACTIVE -> DDLState.ACTIVE
                else -> throw InvalidTaskStateTransitionException(from, action)
            }
            DDLState.ARCHIVED -> when (action) {
                TaskStateAction.UNARCHIVE -> DDLState.COMPLETED
                else -> throw InvalidTaskStateTransitionException(from, action)
            }
            DDLState.ABANDONED -> when (action) {
                TaskStateAction.MARK_ARCHIVE -> DDLState.ABANDONED_ARCHIVED
                TaskStateAction.RESTORE_ACTIVE -> DDLState.ACTIVE
                else -> throw InvalidTaskStateTransitionException(from, action)
            }
            DDLState.ABANDONED_ARCHIVED -> when (action) {
                TaskStateAction.UNARCHIVE -> DDLState.ABANDONED
                else -> throw InvalidTaskStateTransitionException(from, action)
            }
        }
    }
}

fun DDLItem.transition(
    using: TaskStateAction,
    confirmed: Boolean = true,
    now: LocalDateTime = LocalDateTime.now()
): DDLItem {
    if (using.requiresConfirmation() && !confirmed) {
        throw TaskActionConfirmationRequiredException(using)
    }

    val nextState = TaskStateMachine.nextState(state, using)
    val nextCompleteTime = when (using) {
        TaskStateAction.MARK_COMPLETE,
        TaskStateAction.MARK_GIVE_UP -> now.toString()
        TaskStateAction.RESTORE_ACTIVE -> ""
        TaskStateAction.MARK_ARCHIVE,
        TaskStateAction.UNARCHIVE -> completeTime
    }

    return copy(
        state = nextState,
        completeTime = nextCompleteTime,
        isCompleted = nextState.isCompletedFamily(),
        isArchived = nextState.isArchivedFamily()
    )
}
