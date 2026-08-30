package com.aritxonly.deadliner.ui.base

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopBarMaterialScrollObserverTest {
    @Test
    fun progressStaysHiddenUntilRevealThreshold() {
        assertEquals(4.dp, TopBarMaterialRevealStartDistance)
        assertEquals(28.dp, TopBarMaterialRevealEndDistance)
        assertEquals(0f, resolveTopBarScrollProgress(4f, 4f, 28f), 0.0001f)
    }

    @Test
    fun progressSmoothlyRevealsAcrossScrollRange() {
        val progress = resolveTopBarScrollProgress(16f, 4f, 28f)

        assertTrue(progress > 0f)
        assertTrue(progress < 1f)
        assertEquals(1f, resolveTopBarScrollProgress(28f, 4f, 28f), 0.0001f)
    }

    @Test
    fun invalidRangeFallsBackToDiscreteThreshold() {
        assertEquals(0f, resolveTopBarScrollProgress(7f, 8f, 8f), 0.0001f)
        assertEquals(1f, resolveTopBarScrollProgress(8f, 8f, 8f), 0.0001f)
    }

    @Test
    fun progressIsStoredSeparatelyForEachScrollKey() {
        val activeKey = mutableStateOf<Any?>("overview")
        val observer = TopBarMaterialScrollObserver(
            revealStartPx = 4f,
            revealEndPx = 28f,
            activeScrollKey = activeKey,
        )

        observer.updateAbsoluteScrollDistance(28f)
        assertEquals(1f, observer.progress, 0.0001f)

        activeKey.value = "trend"
        assertEquals(0f, observer.progress, 0.0001f)

        observer.updateAbsoluteScrollDistance(16f)
        assertTrue(observer.progress > 0f)
        assertTrue(observer.progress < 1f)

        activeKey.value = "overview"
        assertEquals(1f, observer.progress, 0.0001f)
    }

    @Test
    fun absoluteListPositionIsNotClearedByAReverseScrollGesture() {
        val activeKey = mutableStateOf<Any?>("overview")
        val observer = TopBarMaterialScrollObserver(
            revealStartPx = 4f,
            revealEndPx = 28f,
            activeScrollKey = activeKey,
        )

        observer.updateAbsoluteScrollDistance(Float.MAX_VALUE)
        observer.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, 28f),
            available = Offset.Zero,
            source = NestedScrollSource.Drag,
        )

        assertEquals(1f, observer.progress, 0.0001f)

        observer.updateAbsoluteScrollDistance(0f)
        assertEquals(0f, observer.progress, 0.0001f)
    }
}
