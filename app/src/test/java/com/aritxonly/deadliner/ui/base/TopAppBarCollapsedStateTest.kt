package com.aritxonly.deadliner.ui.base

import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopAppBarCollapsedStateTest {
    @Test
    fun miuixRevealsMaterialWhenSmallTitleBecomesVisible() {
        assertFalse(
            isTopAppBarVisuallyCollapsed(
                AppDesignSystem.MIUIX,
                MiuixTopAppBarVisualCollapseFraction - 0.001f,
            )
        )
        assertTrue(
            isTopAppBarVisuallyCollapsed(
                AppDesignSystem.MIUIX,
                MiuixTopAppBarVisualCollapseFraction,
            )
        )
    }

    @Test
    fun material3RevealsMaterialWhenCollapsedTitleTakesOver() {
        assertFalse(
            isTopAppBarVisuallyCollapsed(
                AppDesignSystem.MATERIAL3,
                Material3TopAppBarVisualCollapseFraction - 0.001f,
            )
        )
        assertTrue(
            isTopAppBarVisuallyCollapsed(
                AppDesignSystem.MATERIAL3,
                Material3TopAppBarVisualCollapseFraction,
            )
        )
    }

    @Test
    fun missingScrollBehaviorKeepsMaterialHidden() {
        assertFalse(isTopAppBarVisuallyCollapsed(AppDesignSystem.MIUIX, null))
    }
}
