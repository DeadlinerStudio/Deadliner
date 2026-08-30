package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

internal val TopBarMaterialRevealStartDistance = 4.dp
internal val TopBarMaterialRevealEndDistance = 28.dp
internal val LocalTopBarMaterialScrollObserver =
    staticCompositionLocalOf<TopBarMaterialScrollObserver?> { null }

@Stable
internal class TopBarMaterialScrollObserver(
    private val revealStartPx: Float,
    private val revealEndPx: Float,
    private val activeScrollKey: State<Any?>,
) {
    /** Keeps each tab's glass progress paired with that tab's scroll position. */
    private val accumulatedDistances = mutableStateMapOf<Any?, Float>()

    val progress: Float
        get() = resolveTopBarScrollProgress(
            distancePx = accumulatedDistances[activeScrollKey.value] ?: 0f,
            revealStartPx = revealStartPx,
            revealEndPx = revealEndPx,
        )

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            return Offset.Zero
        }
    }

    internal fun updateAbsoluteScrollDistance(distancePx: Float) {
        val key = activeScrollKey.value
        accumulatedDistances[key] = distancePx.coerceIn(0f, revealEndPx)
    }
}

@Composable
internal fun rememberTopBarMaterialScrollObserver(
    scrollKey: Any? = Unit,
): TopBarMaterialScrollObserver {
    val density = LocalDensity.current
    val revealStartPx = with(density) { TopBarMaterialRevealStartDistance.toPx() }
    val revealEndPx = with(density) { TopBarMaterialRevealEndDistance.toPx() }
    val activeScrollKey = rememberUpdatedState(scrollKey)
    return remember(revealStartPx, revealEndPx) {
        TopBarMaterialScrollObserver(
            revealStartPx = revealStartPx,
            revealEndPx = revealEndPx,
            activeScrollKey = activeScrollKey,
        )
    }
}

/**
 * Makes a non-collapsing top bar follow the actual list position instead of a short-lived scroll
 * gesture. This keeps its material visible until the list itself returns to the top.
 */
@Composable
internal fun Modifier.syncTopBarMaterialScroll(listState: LazyListState): Modifier {
    val observer = LocalTopBarMaterialScrollObserver.current ?: return this
    LaunchedEffect(observer, listState) {
        snapshotFlow {
            if (listState.firstVisibleItemIndex > 0) {
                Float.MAX_VALUE
            } else {
                listState.firstVisibleItemScrollOffset.toFloat()
            }
        }.collect(observer::updateAbsoluteScrollDistance)
    }
    return this
}

@Composable
internal fun Modifier.syncTopBarMaterialScroll(scrollState: ScrollState): Modifier {
    val observer = LocalTopBarMaterialScrollObserver.current ?: return this
    LaunchedEffect(observer, scrollState) {
        snapshotFlow { scrollState.value.toFloat() }
            .collect(observer::updateAbsoluteScrollDistance)
    }
    return this
}

internal fun resolveTopBarScrollProgress(
    distancePx: Float,
    revealStartPx: Float,
    revealEndPx: Float,
): Float {
    if (revealEndPx <= revealStartPx) {
        return if (distancePx >= revealEndPx) 1f else 0f
    }
    val normalized = ((distancePx - revealStartPx) / (revealEndPx - revealStartPx))
        .coerceIn(0f, 1f)
    return normalized * normalized * (3f - 2f * normalized)
}
