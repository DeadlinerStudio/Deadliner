package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/** Keep only the thin luminous rim; body contrast is handled separately. */
internal fun Modifier.glassBoundary(
    shape: Shape,
    spec: GlassEdgeOpticsSpec,
    isDark: Boolean,
    optical: Boolean,
): Modifier {
    val width = spec.edgeStrokeWidth(isDark)
    if (width.value <= 0f) return this
    return border(width, spec.edgeBrush(isDark, optical), shape)
}
