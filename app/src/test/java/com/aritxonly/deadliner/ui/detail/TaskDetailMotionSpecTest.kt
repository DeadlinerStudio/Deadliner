package com.aritxonly.deadliner.ui.detail

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class TaskDetailMotionSpecTest {
    private val window = Rect(50f, 100f, 450f, 900f)
    private val card = Rect(20f, 160f, 380f, 236f)
    private val timing = TaskDetailMotionSpec.timing(card.height, 800f)

    @Test fun returningContentTravelsWithItsContainerInsteadOfRemainingAtTheTopOfTheScreen() {
        // Follow a point on the detail progress card as the surface returns to a low list row.
        val source = Rect(16f, 480f, 384f, 556f)
        val nodes = TaskDetailMotionSpec.timing(source.height, 800f)
        var previousY = 0f
        for (step in 100 downTo 0) {
            val clock = TaskDetailMotionSpec.ContainerExpandEnd * step / 100f
            val bounds = TaskDetailMotionSpec.bounds(source, 400f, 800f, clock, nodes)
            val placement = TaskDetailMotionSpec.contentPlacement(bounds, 400f, 0f)
            val pointY = placement.offset.y + 80f * placement.scale
            assertTrue(pointY >= previousY)
            assertEquals(bounds.top, placement.offset.y, 0.001f)
            assertEquals(bounds.left, placement.offset.x, 0.001f)
            // A natural-width page stays aligned to both sides of its animated surface.
            assertEquals(bounds.right, placement.offset.x + 400f * placement.scale, 0.001f)
            previousY = pointY
        }
        val settled = TaskDetailMotionSpec.contentPlacement(Rect(0f, 0f, 400f, 800f), 400f, 0f)
        assertEquals(1f, settled.scale)
        assertEquals(0f, settled.offset.y)
        val before = TaskDetailMotionSpec.bounds(source, 400f, 800f, TaskDetailMotionSpec.ContainerExpandEnd, nodes)
        val after = TaskDetailMotionSpec.bounds(source, 400f, 800f, TaskDetailMotionSpec.ContainerExpandEnd - 0.02f, nodes)
        assertTrue(after.height < before.height)
    }

    @Test fun sourceCoordinatesRespectAnOffsetWindowAndReachTheWholePage() {
        val source = TaskDetailMotionSpec.sourceRect(floatArrayOf(70f, 260f, 430f, 336f), window)!!
        assertEquals(card, source)
        assertEquals(source, TaskDetailMotionSpec.bounds(source, 400f, 800f, 0f, timing))
        assertEquals(Rect(0f, 0f, 400f, 800f), TaskDetailMotionSpec.bounds(source, 400f, 800f, 1f, timing))
        for (step in 0..100) {
            val frame = TaskDetailMotionSpec.bounds(source, 400f, 800f, step / 100f, timing)
            assertTrue(frame.width in 360f..400f)
            assertTrue(frame.height in 76f..800f)
        }
    }

    @Test fun invalidOrEntirelyOffscreenCardsUseTheFadeFallback() {
        for (values in listOf(null, floatArrayOf(1f), floatArrayOf(50f, 100f, Float.NaN, 200f),
            floatArrayOf(50f, 100f, 40f, 200f), floatArrayOf(50f, 100f, 450f, 100f),
            floatArrayOf(900f, 200f, 1000f, 276f),
            floatArrayOf(-Float.MAX_VALUE, 100f, Float.MAX_VALUE, 200f))) {
            assertNull(TaskDetailMotionSpec.sourceRect(values, window))
        }
        assertEquals(Rect(0f, 0f, 400f, 800f), TaskDetailMotionSpec.bounds(null, 400f, 800f, 0f, timing))
    }

    @Test fun partiallyVisibleCardsKeepTheirUnclippedContentCoordinates() {
        val source = TaskDetailMotionSpec.sourceRect(floatArrayOf(70f, 80f, 430f, 156f), window)!!
        assertEquals(-20f, source.top)
        assertEquals(76f, source.height)
    }

    @Test fun cachedNodesMatchQuarterAndHalfOfTheActualWindowHeightAcrossLayouts() {
        for ((sourceHeight, targetHeight) in listOf(76f to 800f, 114f to 1200f, 76f to 400f, 48f to 900f)) {
            val source = Rect(20f, 80f, 380f, 80f + sourceHeight)
            val nodes = TaskDetailMotionSpec.timing(sourceHeight, targetHeight)
            val quarter = TaskDetailMotionSpec.bounds(source, 400f, targetHeight, nodes.quarterHeight, nodes)
            val half = TaskDetailMotionSpec.bounds(source, 400f, targetHeight, nodes.halfHeight, nodes)
            assertEquals(targetHeight * 0.25f, quarter.height, 0.05f)
            assertEquals(targetHeight * 0.50f, half.height, 0.05f)
            assertEquals(source.width, half.width, 0.001f)
        }
        assertNotEquals(timing.quarterHeight, TaskDetailMotionSpec.timing(76f, 1200f).quarterHeight)
    }

    @Test fun progressFadesWithTextAndBackgroundOnlyStartsChangingAtHalfHeight() {
        val quarter = timing.quarterHeight
        val half = timing.halfHeight
        assertEquals(1f, TaskDetailMotionSpec.progressAlpha(0f, timing))
        assertTrue(TaskDetailMotionSpec.progressAlpha(quarter, timing) < 1f)
        assertEquals(0f, TaskDetailMotionSpec.progressAlpha(half, timing))
        assertEquals(0f, TaskDetailMotionSpec.backgroundBlend(half - 0.001f, timing))
        assertEquals(0f, TaskDetailMotionSpec.backgroundBlend(half, timing))
        assertTrue(TaskDetailMotionSpec.backgroundBlend(half + 0.01f, timing) > 0f)
        for (step in 0..1000) {
            val p = step / 1000f
            assertEquals(TaskDetailMotionSpec.foregroundAlpha(p, timing),
                TaskDetailMotionSpec.progressAlpha(p, timing))
            if (TaskDetailMotionSpec.progressAlpha(p, timing) > 0f) {
                assertEquals(0f, TaskDetailMotionSpec.backgroundBlend(p, timing))
                assertEquals(0f, TaskDetailMotionSpec.detailAlpha(p, true, timing))
            }
        }
    }

    @Test fun alreadyTallSourceCardsDoNotWaitForThresholdsTheyHavePassed() {
        val tall = TaskDetailMotionSpec.timing(250f, 400f)
        assertEquals(0f, tall.quarterHeight)
        assertEquals(0f, tall.halfHeight)
        assertEquals(0f, TaskDetailMotionSpec.progressAlpha(0f, tall))
        for (height in listOf(0f, 400f, 500f)) {
            val nodes = TaskDetailMotionSpec.timing(height, 400f)
            assertTrue(TaskDetailMotionSpec.backgroundBlend(0f, nodes).isFinite())
            assertTrue(TaskDetailMotionSpec.foregroundAlpha(0f, nodes).isFinite())
        }
    }

    @Test fun thePageIsFullyExpandedBeforeTheContentFinishesHalfItsLift() {
        val p = TaskDetailMotionSpec.ContainerExpandEnd
        assertEquals(Rect(0f, 0f, 400f, 800f), TaskDetailMotionSpec.bounds(card, 400f, 800f, p, timing))
        assertTrue(TaskDetailMotionSpec.contentFloat(p, true, timing) in 0f..0.5f)
        assertEquals(1f, TaskDetailMotionSpec.detailAlpha(p, true, timing))
        assertEquals(0f, TaskDetailMotionSpec.detailAlpha(timing.detailStart, true, timing))
        assertTrue(TaskDetailMotionSpec.backgroundBlend(timing.detailStart, timing) < 1f)
        assertEquals(1f, TaskDetailMotionSpec.contentFloat(1f, true, timing))
        assertEquals(1f, TaskDetailMotionSpec.detailAlpha(1f, true, timing))
    }

    @Test fun detailContentIsVisibleBeforeTheCardBackgroundHasFinishedDisappearing() {
        for ((sourceHeight, targetHeight) in listOf(76f to 800f, 76f to 400f, 114f to 1200f)) {
            val nodes = TaskDetailMotionSpec.timing(sourceHeight, targetHeight)
            assertTrue(nodes.detailStart >= nodes.halfHeight)
            assertTrue(nodes.detailStart < nodes.backgroundEnd)
            // The video exposed an empty black panel here. Keep incoming content readable instead.
            assertTrue(TaskDetailMotionSpec.detailAlpha(nodes.backgroundEnd, true, nodes) > 0.35f)
            var hasOverlap = false
            for (step in 0..1000) {
                val clock = step / 1000f
                val background = TaskDetailMotionSpec.backgroundBlend(clock, nodes)
                val content = TaskDetailMotionSpec.detailAlpha(clock, true, nodes)
                if (background in 0.1f..0.9f && content > 0.1f) hasOverlap = true
            }
            assertTrue(hasOverlap)
        }
    }

    @Test fun opacityRampsAreBoundedAndMonotonicForTheWholeReversibleTimeline() {
        var detail = 0f
        var foreground = 1f
        var indicator = 1f
        var background = 0f
        for (step in 0..100) {
            val p = step / 100f
            val nextDetail = TaskDetailMotionSpec.detailAlpha(p, true, timing)
            val nextForeground = TaskDetailMotionSpec.foregroundAlpha(p, timing)
            val nextIndicator = TaskDetailMotionSpec.progressAlpha(p, timing)
            val nextBackground = TaskDetailMotionSpec.backgroundBlend(p, timing)
            assertTrue(nextDetail in detail..1f)
            assertTrue(nextForeground in 0f..foreground)
            assertTrue(nextIndicator in 0f..indicator)
            assertTrue(nextBackground in background..1f)
            detail = nextDetail
            foreground = nextForeground
            indicator = nextIndicator
            background = nextBackground
        }
        assertEquals(0f, TaskDetailMotionSpec.detailAlpha(0f, false, timing))
        assertEquals(1f, TaskDetailMotionSpec.detailAlpha(1f, false, timing))
    }
}
