package com.aritxonly.deadliner.ui.base

internal fun resolveTopBarButtonMaterialProgress(
    collapsedFraction: Float?,
    scrollProgress: Float?,
    overrideProgress: Float?,
): Float {
    overrideProgress?.let { return it.coerceIn(0f, 1f) }
    if (collapsedFraction == null) return scrollProgress?.coerceIn(0f, 1f) ?: 0f

    val normalized = ((collapsedFraction.coerceIn(0f, 1f) - 0.04f) / 0.28f)
        .coerceIn(0f, 1f)
    return normalized * normalized * (3f - 2f * normalized)
}
