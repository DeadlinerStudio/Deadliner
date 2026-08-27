package com.aritxonly.deadliner.ui.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopBarMaterialProgressTest {
    @Test
    fun expandedLargeBarKeepsButtonMaterialHidden() {
        assertEquals(
            0f,
            resolveTopBarButtonMaterialProgress(
                collapsedFraction = TopBarMaterialRevealStartFraction,
                scrollProgress = null,
                overrideProgress = null,
            ),
            0.0001f,
        )
    }

    @Test
    fun slightCollapseProgressivelyRevealsMaterial() {
        val progress = resolveTopBarButtonMaterialProgress(
            collapsedFraction = 0.09f,
            scrollProgress = null,
            overrideProgress = null,
        )

        assertTrue(progress > 0f)
        assertTrue(progress < 1f)
        assertEquals(
            1f,
            resolveTopBarButtonMaterialProgress(
                collapsedFraction = TopBarMaterialRevealStartFraction +
                    TopBarMaterialRevealRangeFraction,
                scrollProgress = null,
                overrideProgress = null,
            ),
            0.0001f,
        )
    }

    @Test
    fun smallBarStartsHiddenAndUsesObservedScrollProgress() {
        assertEquals(
            0f,
            resolveTopBarButtonMaterialProgress(
                collapsedFraction = null,
                scrollProgress = null,
                overrideProgress = null,
            ),
            0.0001f,
        )
        assertEquals(
            0.25f,
            resolveTopBarButtonMaterialProgress(
                collapsedFraction = null,
                scrollProgress = 0.25f,
                overrideProgress = null,
            ),
            0.0001f,
        )
    }

    @Test
    fun explicitOverrideTakesPriorityOverScrollState() {
        assertEquals(
            0.75f,
            resolveTopBarButtonMaterialProgress(
                collapsedFraction = 0.1f,
                scrollProgress = 0.25f,
                overrideProgress = 0.75f,
            ),
            0.0001f,
        )
    }
}
