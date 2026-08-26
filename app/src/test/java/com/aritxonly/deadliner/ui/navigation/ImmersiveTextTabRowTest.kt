package com.aritxonly.deadliner.ui.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
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
}
