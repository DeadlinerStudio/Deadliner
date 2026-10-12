package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class QuickAddMorphGeometryTest {
    private val source = Rect(320f, 700f, 380f, 760f)
    private val target = Rect(40f, 360f, 380f, 680f)

    @Test fun lensStartsAtTheButtonAndEndsAtThePanel() {
        assertEquals(source, quickAddMorphRect(source, target, 0f))
        assertEquals(target, quickAddMorphRect(source, target, 1f))
    }

    @Test fun middleFrameGrowsAndTravelsInsteadOfOnlyFading() {
        val halfway = quickAddMorphRect(source, target, 0.5f)
        assertEquals(200f, halfway.width, 0.001f)
        assertEquals(190f, halfway.height, 0.001f)
        assertEquals(530f, halfway.top, 0.001f)
        assertEquals(380f, halfway.right, 0.001f)
    }
    @Test fun anchorKeepsLayoutDiameterAroundTheTransformedCenter() {
        val anchor = quickAddAnchorRect(Offset(350f, 730f), Size(60f, 60f))
        assertEquals(source, anchor)
        assertEquals(60f, anchor.width, 0f)
    }

    @Test fun handoffHasNoSurfaceOrContentAtTheSourceAndIsReversible() {
        assertEquals(0f, quickAddHandoff(0f, 0f, 0.12f), 0f)
        assertEquals(1f, quickAddHandoff(0.12f, 0f, 0.12f), 0f)
        assertEquals(0f, quickAddHandoff(0.12f, 0.2f, 0.7f), 0f)
        assertEquals(0.5f, quickAddHandoff(0.45f, 0.2f, 0.7f), 0.001f)
        assertEquals(1f, quickAddHandoff(1f, 0.2f, 0.7f), 0f)
    }
    @Test fun compactPanelGuttersAreEqualEvenWhenTheFabIsInsetFurther() {
        val (left, right) = quickAddPanelHorizontalBounds(400f, 26f, 12f, 420f, 600f)
        assertEquals(12f, left, 0f)
        assertEquals(12f, 400f - right, 0f)
    }

    @Test fun tabletPanelKeepsItsMaximumWidthAndFollowsTheFab() {
        val (left, right) = quickAddPanelHorizontalBounds(1200f, 16f, 12f, 420f, 600f)
        assertEquals(420f, right - left, 0f)
        assertEquals(16f, 1200f - right, 0f)
        val (edgeLeft, edgeRight) = quickAddPanelHorizontalBounds(1200f, 1000f, 12f, 420f, 600f)
        assertEquals(12f, edgeLeft, 0f)
        assertEquals(420f, edgeRight - edgeLeft, 0f)
    }
    @Test fun largerPhoneStillHasEqualGutters() {
        val (left, right) = quickAddPanelHorizontalBounds(500f, 26f, 12f, 420f, 600f)
        assertEquals(12f, left, 0f)
        assertEquals(12f, 500f - right, 0f)
    }
}
