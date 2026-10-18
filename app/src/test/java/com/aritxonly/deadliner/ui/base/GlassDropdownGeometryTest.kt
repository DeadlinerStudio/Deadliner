package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.*
import org.junit.Test

class GlassDropdownGeometryTest {
    private val safe = Rect(8f, 32f, 392f, 768f)
    private val anchor = Rect(24f, 120f, 376f, 176f)
    private val menu = IntSize(240, 180)


    @Test fun coversTheSourceAndAlignsItsTrailingContentInset() {
        val p = GlassDropdownGeometry.position(anchor, safe, menu, false, 8f)
        assertEquals(144, p.x)
        assertEquals(32, p.y)
        val rtl = GlassDropdownGeometry.position(anchor, safe, menu, true, 8f)
        assertEquals(16, rtl.x)
        assertEquals(p.y, rtl.y)
    }

    @Test fun aLowSourceExpandsUpwardsWhileRemainingInsideThePanel() {
        val p = GlassDropdownGeometry.position(Rect(24f, 650f, 376f, 706f), safe, menu, false, 8f)
        assertEquals(534, p.y)
        assertTrue(678f >= p.y && 678f <= p.y + menu.height)
    }

    @Test fun keepsMenusInTheSafeAreaWhenNeitherSideHasRoom() {
        val p = GlassDropdownGeometry.position(anchor, safe, IntSize(384, 736), false, 8f)
        assertEquals(8, p.x)
        assertEquals(32, p.y)
    }

    @Test fun translatesScreenAnchorsIntoThePopupWindowWithoutChangingTheirAlignment() {
        val popupOrigin = Offset(50f, 100f)
        val screenAnchor = anchor.translate(popupOrigin)
        assertEquals(GlassDropdownGeometry.position(anchor, safe, menu, false, 8f),
            GlassDropdownGeometry.position(screenAnchor.translate(-popupOrigin), safe, menu, false, 8f))
    }

}
