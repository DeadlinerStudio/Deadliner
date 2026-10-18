package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.*
import org.junit.Test

class GlassDropdownMotionTest {
    private val source = Rect(70f, 102f, 230f, 128f)
    private val size = Size(240f, 136f)

    @Test fun openingStartsAtTheExactValueBoundsAndClosingReturnsThere() {
        assertEquals(source, GlassDropdownMotion.bounds(source, size, 0f))
        assertEquals(Rect(0f, 0f, 240f, 136f), GlassDropdownMotion.bounds(source, size, 1f))
        var previous = source
        for (step in 1..100) {
            val next = GlassDropdownMotion.bounds(source, size, step / 100f)
            assertTrue(next.left <= previous.left)
            assertTrue(next.top <= previous.top)
            assertTrue(next.right >= previous.right)
            assertTrue(next.bottom >= previous.bottom)
            previous = next
        }
    }

    @Test fun sourceAndMenuTextNeverOverlapDuringOpeningOrReversedClosing() {
        assertEquals(1f, GlassDropdownMotion.sourceAlpha(0f))
        assertEquals(0f, GlassDropdownMotion.contentAlpha(0f))
        assertEquals(0f, GlassDropdownMotion.sourceAlpha(1f))
        assertEquals(1f, GlassDropdownMotion.contentAlpha(1f))
        assertTrue(GlassDropdownMotion.sourceAlpha(0.2f) > 0f)
        assertEquals(0f, GlassDropdownMotion.contentAlpha(0.2f))
        for (step in 0..1000) {
            val fraction = step / 1000f
            assertEquals(0f, GlassDropdownMotion.sourceAlpha(fraction) *
                GlassDropdownMotion.contentAlpha(fraction))
        }
    }

    @Test fun collapsedWindowIsTransparentAndEndpointOpacityChangesSmoothly() {
        assertEquals(0f, GlassDropdownMotion.surfaceAlpha(0f))
        assertEquals(1f, GlassDropdownMotion.surfaceAlpha(0.25f))
        assertEquals(1f, GlassDropdownMotion.surfaceAlpha(1f))
        for (step in 0..1000) {
            val fraction = step / 1000f
            assertEquals(1f, GlassDropdownMotion.surfaceAlpha(fraction) +
                GlassDropdownMotion.sourceAlpha(fraction), 0.00001f)
        }
        // No opacity step when mounting/unmounting the popup at progress zero.
        assertTrue(GlassDropdownMotion.surfaceAlpha(0.001f) < 0.0001f)
        assertTrue(GlassDropdownMotion.sourceAlpha(0.001f) > 0.9999f)
    }

    @Test fun finalShapeHas24DpCornersAtDifferentDensities() {
        for (scale in listOf(1f, 2f, 3f)) {
            val density = Density(scale)
            val outline = DropdownMorphShape(source, 1f)
                .createOutline(Size(300f * scale, 400f * scale), LayoutDirection.Ltr, density) as Outline.Rounded
            assertEquals(24f * scale, outline.roundRect.topLeftCornerRadius.x, 0.001f)
        }
    }

    @Test fun sourceShapeMatchesTheOriginalValueCapsuleRatherThanTheFinalPanel() {
        val outline = DropdownMorphShape(source, 0f)
            .createOutline(size, LayoutDirection.Ltr, Density(1f)) as Outline.Rounded
        assertEquals(source.left, outline.roundRect.left)
        assertEquals(source.top, outline.roundRect.top)
        assertEquals(source.width, outline.roundRect.width)
        assertEquals(source.height / 2f, outline.roundRect.topLeftCornerRadius.x)
    }
}
