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
                collapsedFraction = 0.04f,
                inherentlyCollapsed = false,
                overrideProgress = null,
            ),
            0.0001f,
        )
    }

    @Test
    fun slightCollapseProgressivelyRevealsMaterial() {
        val progress = resolveTopBarButtonMaterialProgress(
            collapsedFraction = 0.18f,
            inherentlyCollapsed = false,
            overrideProgress = null,
        )

        assertTrue(progress > 0f)
        assertTrue(progress < 1f)
        assertEquals(
            1f,
            resolveTopBarButtonMaterialProgress(
                collapsedFraction = 0.32f,
                inherentlyCollapsed = false,
                overrideProgress = null,
            ),
            0.0001f,
        )
    }

    @Test
    fun smallBarAndExplicitOverrideUseResolvedState() {
        assertEquals(
            1f,
            resolveTopBarButtonMaterialProgress(
                collapsedFraction = null,
                inherentlyCollapsed = true,
                overrideProgress = null,
            ),
            0.0001f,
        )
        assertEquals(
            0.25f,
            resolveTopBarButtonMaterialProgress(
                collapsedFraction = null,
                inherentlyCollapsed = true,
                overrideProgress = 0.25f,
            ),
            0.0001f,
        )
    }
}
