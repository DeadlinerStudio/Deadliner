package com.aritxonly.deadliner.ui.material.glass

/** Three Gaussian sigmas plus the lens displacement and final softening kernel, in pixels. */
internal fun glassEffectPadding(
    radiusX: Float,
    radiusY: Float,
    postRadius: Float,
    displacement: Float,
    dispersion: Float,
): Float = maxOf(radiusX, radiusY, 0f) * 3f +
    displacement.coerceAtLeast(0f) + dispersion.coerceAtLeast(0f) +
    postRadius.coerceAtLeast(0f) * 3f
