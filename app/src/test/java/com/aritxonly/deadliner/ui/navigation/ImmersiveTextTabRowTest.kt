package com.aritxonly.deadliner.ui.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImmersiveTextTabRowTest {
    @Test
    fun selectedIndexIsClampedToAvailableTabs() {
        assertEquals(0, resolveImmersiveTextTabIndex(-1, 3))
        assertEquals(1, resolveImmersiveTextTabIndex(1, 3))
        assertEquals(2, resolveImmersiveTextTabIndex(8, 3))
        assertEquals(0, resolveImmersiveTextTabIndex(2, 0))
    }

    @Test
    fun immersiveTextTabsKeepTheFrozenHeight() {
        assertEquals(42.dp, ImmersiveTextTabRowDefaults.Height)
        assertEquals(3.dp, ImmersiveTextTabRowDefaults.ContentPadding)
    }

    @Test
    fun materialProgressRequiresImmersiveModeAndPrefersExplicitProgress() {
        assertEquals(
            0f,
            resolveImmersiveTextTabMaterialProgress(
                immersiveEnabled = false,
                explicitProgress = 0.8f,
                topBarProgress = 0.7f,
                scrollProgress = 0.6f,
            ),
            0.0001f,
        )
        assertEquals(
            0.8f,
            resolveImmersiveTextTabMaterialProgress(
                immersiveEnabled = true,
                explicitProgress = 0.8f,
                topBarProgress = 0.7f,
                scrollProgress = 0.6f,
            ),
            0.0001f,
        )
        assertEquals(
            0.7f,
            resolveImmersiveTextTabMaterialProgress(
                immersiveEnabled = true,
                explicitProgress = null,
                topBarProgress = 0.7f,
                scrollProgress = 0.6f,
            ),
            0.0001f,
        )
    }

    @Test
    fun fallbackContainerRemainsVisibleWhileGlassAppears() {
        val resting = resolveImmersiveTextTabContainerAlpha(isDark = false, materialProgress = 0f)
        val glass = resolveImmersiveTextTabContainerAlpha(isDark = false, materialProgress = 1f)

        assertEquals(0.82f, resting, 0.0001f)
        assertTrue(glass > 0f)
        assertTrue(glass < resting)
    }
}
