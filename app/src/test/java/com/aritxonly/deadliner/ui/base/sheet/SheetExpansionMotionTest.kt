package com.aritxonly.deadliner.ui.base.sheet

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.app.Application

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SheetExpansionMotionTest {
    private val source = Rect(80f, 380f, 380f, 680f)
    private val target = Size(400f, 850f)
    @Test fun revealStartsAtTheActualCardAndExpandsContinuouslyToTheWholeSheet() {
        assertEquals(source, SheetExpansionMotion.bounds(source, target, 0f))
        assertEquals(Rect(0f, 0f, 400f, 850f), SheetExpansionMotion.bounds(source, target, 1f))
        var last = source
        repeat(100) {
            val next = SheetExpansionMotion.bounds(source, target, (it + 1) / 100f)
            assertTrue(next.left <= last.left && next.top <= last.top)
            assertTrue(next.right >= last.right && next.bottom >= last.bottom)
            last = next
        }
    }
    @Test fun formWaitsForTheRevealAndFinishesBeforeTheBoundarySettles() {
        assertEquals(0f, SheetExpansionMotion.contentAlpha(0f))
        assertEquals(0f, SheetExpansionMotion.contentAlpha(0.18f))
        assertTrue(SheetExpansionMotion.contentAlpha(0.5f) in 0.1f..0.9f)
        assertEquals(1f, SheetExpansionMotion.contentAlpha(0.8f))
    }
    @Test fun finalCornersMatchTheSheetRatherThanTheQuickAddCard() {
        val outline = SheetExpansionShape(source, 1f)
            .createOutline(target, LayoutDirection.Ltr, Density(1f)) as Outline.Rounded
        assertEquals(36f, outline.roundRect.topLeftCornerRadius.x)
        assertEquals(0f, outline.roundRect.bottomLeftCornerRadius.x)
    }
}
