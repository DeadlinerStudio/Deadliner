package com.aritxonly.deadliner.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val RevealStartDistance = 12.dp
private val RevealEndDistance = 48.dp

@Stable
internal class TopBarMaterialScrollObserver(
    private val revealStartPx: Float,
    private val revealEndPx: Float,
) {
    private val accumulatedDistanceState = mutableFloatStateOf(0f)

    val progress: Float
        get() = resolveTopBarScrollProgress(
            distancePx = accumulatedDistanceState.floatValue,
            revealStartPx = revealStartPx,
            revealEndPx = revealEndPx,
        )

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            updateDistance(consumed.y)
            return Offset.Zero
        }
    }

    private fun updateDistance(consumedY: Float) {
        accumulatedDistanceState.floatValue = (
            accumulatedDistanceState.floatValue - consumedY
        ).coerceIn(0f, revealEndPx)
    }
}

@Composable
internal fun rememberTopBarMaterialScrollObserver(
    resetKey: Any? = Unit,
): TopBarMaterialScrollObserver {
    val density = LocalDensity.current
    val revealStartPx = with(density) { RevealStartDistance.toPx() }
    val revealEndPx = with(density) { RevealEndDistance.toPx() }
    return remember(resetKey, revealStartPx, revealEndPx) {
        TopBarMaterialScrollObserver(
            revealStartPx = revealStartPx,
            revealEndPx = revealEndPx,
        )
    }
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
