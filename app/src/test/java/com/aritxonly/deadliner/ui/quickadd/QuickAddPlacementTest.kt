package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class QuickAddPlacementTest {
    @Test
    fun panelClearsEntireDockInsteadOfCoveringTheAnchor() {
        val p = quickAddPlacement(Size(400f, 800f), Offset.Zero,
            Rect(320f, 700f, 374f, 754f), 0f, 12f, 58f, 64f)
        assertFalse(p.liftAction)
        // The 64px dock starts at 695px; the input stops 12px above it.
        assertEquals(683f, 800f - p.panelBottomInset, 0.01f)
    }

    @Test
    fun windowCoordinatesAreConvertedToTheHostsOrigin() {
        val p = quickAddPlacement(Size(400f, 800f), Offset(0f, 30f),
            Rect(320f, 730f, 374f, 784f), 0f, 12f, 58f, 64f)
        assertEquals(683f, 800f - p.panelBottomInset, 0.01f)
    }

    @Test
    fun keyboardMovesOnlyThePanelAndKeepsCloseAtTheOriginalAnchor() {
        val p = quickAddPlacement(Size(400f, 800f), Offset.Zero,
            Rect(320f, 700f, 374f, 754f), 300f, 12f, 58f, 64f)
        assertFalse(p.liftAction)
        assertEquals(54f, p.actionDiameter, 0.01f)
        assertEquals(46f, p.actionBottomInset, 0.01f)
        assertEquals(312f, p.panelBottomInset, 0.01f)
    }
}
