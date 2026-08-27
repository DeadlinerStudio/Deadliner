package com.aritxonly.deadliner.ui.base

internal const val TopBarMaterialRevealStartFraction = 0.015f
internal const val TopBarMaterialRevealRangeFraction = 0.18f

internal fun resolveTopBarButtonMaterialProgress(
    collapsedFraction: Float?,
    scrollProgress: Float?,
    overrideProgress: Float?,
): Float {
    overrideProgress?.let { return it.coerceIn(0f, 1f) }
    if (collapsedFraction == null) return scrollProgress?.coerceIn(0f, 1f) ?: 0f

    val normalized = (
        (collapsedFraction.coerceIn(0f, 1f) - TopBarMaterialRevealStartFraction) /
            TopBarMaterialRevealRangeFraction
    )
        .coerceIn(0f, 1f)
    return normalized * normalized * (3f - 2f * normalized)
}
