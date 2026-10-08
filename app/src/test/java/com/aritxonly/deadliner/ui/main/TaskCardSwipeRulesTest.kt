package com.aritxonly.deadliner.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskCardSwipeRulesTest {
    private val width = 400f
    private val revealThreshold = 84f
    private val closeDistance = 32f

    @Test
    fun shortDragReturnsToClosedPosition() {
        assertEquals(
            TaskCardSwipeSettleResult.CLOSED,
            settle(offset = 60f),
        )
    }

    @Test
    fun dragPastRevealThresholdHoversOnLeadingActions() {
        assertEquals(
            TaskCardSwipeSettleResult.LEADING_REVEALED,
            settle(offset = 90f),
        )
    }

    @Test
    fun dragPastRevealThresholdHoversOnTrailingActions() {
        assertEquals(
            TaskCardSwipeSettleResult.TRAILING_REVEALED,
            settle(offset = -90f),
        )
    }

    @Test
    fun fullSwipeRunsOuterAction() {
        assertEquals(
            TaskCardSwipeSettleResult.RUN_PRIMARY_ACTION,
            settle(offset = 260f),
        )
        assertEquals(
            TaskCardSwipeSettleResult.RUN_DELETE_ACTION,
            settle(offset = -260f),
        )
    }

    @Test
    fun closingDragFromRevealedPositionClosesAfterThirtyTwoPixels() {
        assertEquals(
            TaskCardSwipeSettleResult.CLOSED,
            settle(offset = 70f, dragStartOffset = 112f, gestureOffset = -42f),
        )
        assertEquals(
            TaskCardSwipeSettleResult.TRAILING_REVEALED,
            settle(offset = -82f, dragStartOffset = -112f, gestureOffset = 30f),
        )
    }

    @Test
    fun trailingDirectionIsDisabledWithoutTrailingActions() {
        assertEquals(
            TaskCardSwipeSettleResult.CLOSED,
            settle(offset = -100f, hasTrailingActions = false),
        )
    }

    @Test
    fun focusMenuOnCompactCardDoesNotRunCompleteWhenRevealedOrClosing() {
        assertEquals(TaskCardSwipeSettleResult.LEADING_REVEALED,
            settle(offset = 168f, cardWidth = 260f, leadingRevealWidth = 168f))
        assertEquals(TaskCardSwipeSettleResult.CLOSED,
            settle(offset = 136f, dragStartOffset = 168f, gestureOffset = -32f,
                cardWidth = 260f, leadingRevealWidth = 168f))
    }

    @Test
    fun focusMenuStillAllowsDeliberateFullSwipeToComplete() {
        assertEquals(TaskCardSwipeSettleResult.RUN_PRIMARY_ACTION,
            settle(offset = 205f, cardWidth = 260f, leadingRevealWidth = 168f))
        assertEquals(TaskCardSwipeSettleResult.RUN_DELETE_ACTION,
            settle(offset = -165f, cardWidth = 260f, leadingRevealWidth = 168f))
    }

    private fun settle(
        offset: Float,
        dragStartOffset: Float = 0f,
        gestureOffset: Float = offset - dragStartOffset,
        hasTrailingActions: Boolean = true,
        cardWidth: Float = width,
        leadingRevealWidth: Float = 0f,
    ): TaskCardSwipeSettleResult = resolveTaskCardSwipeSettle(
        offset = offset,
        dragStartOffset = dragStartOffset,
        gestureOffset = gestureOffset,
        cardWidth = cardWidth,
        revealThreshold = revealThreshold,
        closeDistance = closeDistance,
        hasTrailingActions = hasTrailingActions,
        leadingRevealWidth = leadingRevealWidth,
    )
}
