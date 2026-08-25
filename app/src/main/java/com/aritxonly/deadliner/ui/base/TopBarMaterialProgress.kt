package com.aritxonly.deadliner.ui.base

internal fun resolveTopBarButtonMaterialProgress(
    collapsedFraction: Float?,
    inherentlyCollapsed: Boolean,
    overrideProgress: Float?,
): Float {
    overrideProgress?.let { return it.coerceIn(0f, 1f) }
    if (collapsedFraction == null) return if (inherentlyCollapsed) 1f else 0f

    val normalized = ((collapsedFraction.coerceIn(0f, 1f) - 0.04f) / 0.28f)
        .coerceIn(0f, 1f)
    return normalized * normalized * (3f - 2f * normalized)
}
