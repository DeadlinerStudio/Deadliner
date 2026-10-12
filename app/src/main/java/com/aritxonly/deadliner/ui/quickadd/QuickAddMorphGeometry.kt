package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.ui.geometry.Rect

/** Interpolate physical bounds. No scale transform is applied to the sampled backdrop. */
internal fun quickAddMorphRect(source: Rect, target: Rect, progress: Float): Rect {
    val p = progress.coerceIn(0f, 1.06f)
    fun mix(from: Float, to: Float) = from + (to - from) * p
    return Rect(mix(source.left, target.left), mix(source.top, target.top),
        mix(source.right, target.right), mix(source.bottom, target.bottom))
}

/** A short material handoff at the source; content waits until the lens has room. */
internal fun quickAddHandoff(progress: Float, start: Float, end: Float): Float {
    val t = ((progress - start) / (end - start)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Compact panels have equal gutters; bounded wide panels stay attached to the end action. */
internal fun quickAddPanelHorizontalBounds(hostWidth: Float, actionRightInset: Float,
                                         edgeInset: Float, maxWidth: Float, wideMinWidth: Float): Pair<Float, Float> {
    val gutter = edgeInset.coerceIn(0f, (hostWidth - 1f).coerceAtLeast(0f) / 2f)
    val available = (hostWidth - 2f * gutter).coerceAtLeast(1f)
    val compact = hostWidth < wideMinWidth
    val width = if (compact) available else minOf(available, maxWidth)
    val rightInset = if (compact || available <= maxWidth) gutter else
        actionRightInset.coerceIn(gutter, hostWidth - width - gutter)
    val right = hostWidth - rightInset
    return (right - width) to right
}
