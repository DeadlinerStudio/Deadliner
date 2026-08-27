package com.aritxonly.deadliner.ui.main.modern

import org.junit.Assert.assertEquals
import org.junit.Test

class ModernListTopBarScrollCoordinatorTest {
    @Test
    fun downwardScrollExpandsTitleBeforeSearchOrRefresh() {
        val title = resolveTitleExpansionChange(
            heightOffset = -40f,
            heightOffsetLimit = -80f,
            availableY = 64f,
        )
        val search = resolveSearchCollapseChange(
            collapsedPx = 45f,
            maximumCollapsePx = 45f,
            availableY = 64f - title.consumedY,
        )

        assertEquals(0f, title.heightOffset, 0.001f)
        assertEquals(40f, title.consumedY, 0.001f)
        assertEquals(21f, search.collapsedPx, 0.001f)
        assertEquals(24f, search.consumedY, 0.001f)
    }

    @Test
    fun upwardScrollCollapsesSearchBeforePassingRemainder() {
        val partial = resolveSearchCollapseChange(
            collapsedPx = 0f,
            maximumCollapsePx = 60f,
            availableY = -24f,
        )
        val completed = resolveSearchCollapseChange(
            collapsedPx = partial.collapsedPx,
            maximumCollapsePx = 60f,
            availableY = -50f,
        )

        assertEquals(24f, partial.collapsedPx, 0.001f)
        assertEquals(-24f, partial.consumedY, 0.001f)
        assertEquals(60f, completed.collapsedPx, 0.001f)
        assertEquals(-36f, completed.consumedY, 0.001f)
    }

    @Test
    fun downwardScrollOnlyConsumesRemainingSearchDistance() {
        val change = resolveSearchCollapseChange(
            collapsedPx = 60f,
            maximumCollapsePx = 60f,
            availableY = 72f,
        )

        assertEquals(0f, change.collapsedPx, 0.001f)
        assertEquals(60f, change.consumedY, 0.001f)
    }
}
