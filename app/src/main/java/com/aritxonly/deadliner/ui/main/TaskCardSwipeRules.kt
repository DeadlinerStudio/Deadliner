package com.aritxonly.deadliner.ui.main

internal enum class TaskCardSwipeSettleResult {
    CLOSED,
    LEADING_REVEALED,
    TRAILING_REVEALED,
    RUN_PRIMARY_ACTION,
    RUN_DELETE_ACTION,
}

internal object TaskCardSwipeDefaults {
    const val MAX_OFFSET_FRACTION = 0.82f
    const val FULL_SWIPE_THRESHOLD_FRACTION = 0.62f
    const val REVEAL_WIDTH_DP = 112f
    const val REVEAL_TRIGGER_DISTANCE_DP = 84f
    const val CLOSE_DISTANCE_DP = 32f
}

internal fun resolveTaskCardSwipeSettle(
    offset: Float,
    dragStartOffset: Float,
    gestureOffset: Float,
    cardWidth: Float,
    revealThreshold: Float,
    closeDistance: Float,
    hasTrailingActions: Boolean,
): TaskCardSwipeSettleResult {
    val fullSwipeThreshold = cardWidth * TaskCardSwipeDefaults.FULL_SWIPE_THRESHOLD_FRACTION

    if (offset > fullSwipeThreshold) {
        return TaskCardSwipeSettleResult.RUN_PRIMARY_ACTION
    }
    if (offset < -fullSwipeThreshold && hasTrailingActions) {
        return TaskCardSwipeSettleResult.RUN_DELETE_ACTION
    }

    if (dragStartOffset <= -revealThreshold && gestureOffset > 0f) {
        return when {
            offset >= revealThreshold -> TaskCardSwipeSettleResult.LEADING_REVEALED
            gestureOffset >= closeDistance -> TaskCardSwipeSettleResult.CLOSED
            else -> TaskCardSwipeSettleResult.TRAILING_REVEALED
        }
    }

    if (dragStartOffset >= revealThreshold && gestureOffset < 0f) {
        return when {
            offset <= -revealThreshold && hasTrailingActions -> {
                TaskCardSwipeSettleResult.TRAILING_REVEALED
            }
            -gestureOffset >= closeDistance -> TaskCardSwipeSettleResult.CLOSED
            else -> TaskCardSwipeSettleResult.LEADING_REVEALED
        }
    }

    return when {
        offset >= revealThreshold -> TaskCardSwipeSettleResult.LEADING_REVEALED
        offset <= -revealThreshold && hasTrailingActions -> {
            TaskCardSwipeSettleResult.TRAILING_REVEALED
        }
        else -> TaskCardSwipeSettleResult.CLOSED
    }
}
