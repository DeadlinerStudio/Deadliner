package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImmersiveNavigationItemsTest {
    private val testIcon = ColorPainter(Color.Black)
    private val immersiveTestIcon = ColorPainter(Color.White)

    private fun item(key: String, enabled: Boolean = true) = AdaptiveNavItem(
        key = key,
        label = key,
        selectedIcon = testIcon,
        unselectedIcon = testIcon,
        enabled = enabled,
    )

    @Test
    fun detachedSearchIsRemovedFromPrimaryItems() {
        val partition = partitionImmersiveNavigationItems(
            items = listOf(item("list"), item("lifi", enabled = false), item("search")),
            detachedItemKey = "search",
        )

        assertEquals("search", partition.detachedItem?.key)
        assertEquals(listOf("list", "lifi"), partition.primaryItems.map { it.key })
        assertFalse(partition.primaryItems.single { it.key == "lifi" }.enabled)
    }

    @Test
    fun detachedSelectionKeepsPrimaryIndicatorOnFirstEnabledItem() {
        val primary = listOf(item("list"), item("lifi", enabled = false))

        assertEquals("list", resolveImmersivePrimarySelectedKey(primary, "search"))
        assertEquals("list", resolveImmersivePrimarySelectedKey(primary, "lifi"))
        assertFalse(isImmersivePrimarySelectionVisible(primary, "search"))
        assertFalse(isImmersivePrimarySelectionVisible(primary, "lifi"))
        assertTrue(isImmersivePrimarySelectionVisible(primary, "list"))
    }

    @Test
    fun immersiveIconOverridesSelectionVariantsAndFallsBackToOutline() {
        val immersiveItem = item("list").copy(immersiveIcon = immersiveTestIcon)
        val fallbackItem = item("capture")

        assertEquals(immersiveTestIcon, resolveImmersiveNavigationIcon(immersiveItem))
        assertEquals(testIcon, resolveImmersiveNavigationIcon(fallbackItem))
    }
}
