package com.aritxonly.deadliner.ui.main.modern

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import top.yukonga.miuix.kmp.basic.ScrollBehavior

internal data class SearchCollapseChange(
    val collapsedPx: Float,
    val consumedY: Float,
)

internal data class TitleExpansionChange(
    val heightOffset: Float,
    val consumedY: Float,
)

internal fun resolveTitleExpansionChange(
    heightOffset: Float,
    heightOffsetLimit: Float,
    availableY: Float,
): TitleExpansionChange {
    val safeLimit = heightOffsetLimit.coerceAtMost(0f)
    val current = heightOffset.coerceIn(safeLimit, 0f)
    val updated = (current + availableY.coerceAtLeast(0f)).coerceIn(safeLimit, 0f)
    return TitleExpansionChange(
        heightOffset = updated,
        consumedY = updated - current,
    )
}

internal fun resolveSearchCollapseChange(
    collapsedPx: Float,
    maximumCollapsePx: Float,
    availableY: Float,
): SearchCollapseChange {
    val safeMaximum = maximumCollapsePx.coerceAtLeast(0f)
    val current = collapsedPx.coerceIn(0f, safeMaximum)
    val updated = (current - availableY).coerceIn(0f, safeMaximum)
    return SearchCollapseChange(
        collapsedPx = updated,
        consumedY = -(updated - current),
    )
}

@Stable
internal class ModernListSearchCollapseState(initialMaximumCollapsePx: Float) {
    var maximumCollapsePx by mutableFloatStateOf(initialMaximumCollapsePx.coerceAtLeast(0f))
        private set
    var collapsedPx by mutableFloatStateOf(initialMaximumCollapsePx.coerceAtLeast(0f))
        private set

    val expandedFraction: Float
        get() = if (maximumCollapsePx <= 0f) {
            1f
        } else {
            1f - collapsedPx / maximumCollapsePx
        }

    val isExpanded: Boolean get() = collapsedPx <= 0.5f
    val isCollapsed: Boolean get() = collapsedPx >= maximumCollapsePx - 0.5f

    fun updateMaximumCollapsePx(value: Float) {
        val collapsedFraction = if (maximumCollapsePx <= 0f) 1f else collapsedPx / maximumCollapsePx
        maximumCollapsePx = value.coerceAtLeast(0f)
        collapsedPx = maximumCollapsePx * collapsedFraction.coerceIn(0f, 1f)
    }

    fun dispatchScroll(availableY: Float): Float {
        val change = resolveSearchCollapseChange(
            collapsedPx = collapsedPx,
            maximumCollapsePx = maximumCollapsePx,
            availableY = availableY,
        )
        collapsedPx = change.collapsedPx
        return change.consumedY
    }

    fun snapToCollapsed() {
        collapsedPx = maximumCollapsePx
    }

    fun snapTo(value: Float) {
        collapsedPx = value.coerceIn(0f, maximumCollapsePx)
    }
}

@Composable
internal fun rememberModernListSearchCollapseState(
    maximumCollapsePx: Float,
): ModernListSearchCollapseState {
    val state = remember { ModernListSearchCollapseState(maximumCollapsePx) }
    state.updateMaximumCollapsePx(maximumCollapsePx)
    return state
}

/**
 * Allocates vertical scroll in two stages: the search slot first on upward scroll, then the
 * MIUIX title. Search expands only from user-driven leftover pull after the list reaches its top.
 */
@Composable
internal fun rememberModernListChromeScrollConnection(
    searchState: ModernListSearchCollapseState,
    topBarScrollBehavior: ScrollBehavior,
    chromeEnabled: Boolean,
    searchEnabled: Boolean,
): NestedScrollConnection {
    val currentChromeEnabled by rememberUpdatedState(chromeEnabled)
    val currentSearchEnabled by rememberUpdatedState(searchEnabled)
    return remember(searchState, topBarScrollBehavior) {
        object : NestedScrollConnection {
            private val topBarConnection = topBarScrollBehavior.nestedScrollConnection

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!currentChromeEnabled) {
                    return topBarConnection.onPreScroll(available, source)
                }
                if (available.y > 0f) {
                    val titleChange = resolveTitleExpansionChange(
                        heightOffset = topBarScrollBehavior.state.heightOffset,
                        heightOffsetLimit = topBarScrollBehavior.state.heightOffsetLimit,
                        availableY = available.y,
                    )
                    topBarScrollBehavior.state.heightOffset = titleChange.heightOffset
                    if (
                        topBarScrollBehavior.state.heightOffset >= -0.5f &&
                        (!currentSearchEnabled || searchState.isExpanded)
                    ) {
                        topBarScrollBehavior.state.contentOffset = 0f
                    }
                    return Offset(0f, titleChange.consumedY)
                }
                if (!currentSearchEnabled || available.y >= 0f || searchState.isCollapsed) {
                    return topBarConnection.onPreScroll(available, source)
                }
                val searchConsumedY = searchState.dispatchScroll(available.y)
                val remaining = available.copy(y = available.y - searchConsumedY)
                val topBarConsumed = if (remaining.y < 0f) {
                    topBarConnection.onPreScroll(remaining, source)
                } else {
                    Offset.Zero
                }
                return Offset(0f, searchConsumedY + topBarConsumed.y)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val topBarConsumed = topBarConnection.onPostScroll(consumed, available, source)
                if (!currentSearchEnabled || source != NestedScrollSource.UserInput || available.y <= 0f || searchState.isExpanded) {
                    return topBarConsumed
                }
                val remainingY = available.y - topBarConsumed.y
                val titleExpanded = topBarScrollBehavior.state.heightOffset >= -0.5f
                if (!titleExpanded || remainingY <= 0f) return topBarConsumed

                val searchConsumedY = searchState.dispatchScroll(remainingY)
                return Offset(0f, topBarConsumed.y + searchConsumedY)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (currentSearchEnabled && !searchState.isExpanded && !searchState.isCollapsed) {
                    val target = if (
                        available.y < 0f || searchState.expandedFraction < 0.5f
                    ) {
                        searchState.maximumCollapsePx
                    } else {
                        0f
                    }
                    Animatable(searchState.collapsedPx).animateTo(
                        targetValue = target,
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                    ) {
                        searchState.snapTo(value)
                    }
                    return available.copy(x = 0f)
                }
                return topBarConnection.onPostFling(consumed, available)
            }
        }
    }
}
