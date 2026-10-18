package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** Keep bright-theme rim definition without carrying a white outline into dark surfaces. */
internal fun GlassEdgeOpticsSpec.highlightAlpha(isDark: Boolean): Float =
    (highlightAlpha * if (isDark) darkHighlightMultiplier else 1f).coerceIn(0f, 1f)

internal fun GlassEdgeOpticsSpec.fallbackAlpha(isDark: Boolean): Float =
    (if (isDark) fallbackDarkAlpha * 0.45f else fallbackLightAlpha * 1.6f).coerceIn(0f, 1f)


/** Keep the luminous line narrow; its contrast is independent of the refraction band. */
internal fun GlassEdgeOpticsSpec.edgeStrokeWidth(isDark: Boolean): Dp =
    fallbackWidth * if (isDark) 0.75f else 1.5f

internal fun GlassEdgeOpticsSpec.edgeBrush(isDark: Boolean, optical: Boolean): Brush {
    val alpha = if (optical) highlightAlpha(isDark) else fallbackAlpha(isDark)
    val gray = highlightGray.coerceIn(0f, 1f)
    val light = Color(gray, gray, gray)
    if (isDark) return Brush.verticalGradient(listOf(
        light.copy(alpha = alpha),
        light.copy(alpha = alpha * if (optical) 0.72f else 0.66f),
        light.copy(alpha = alpha * if (optical) 0.46f else 0.42f),
    ))
    // A continuous white rim carries separation, so elevation can remain low-density.
    // Draw after the tint; a dark lower edge would combine with the shadow into a heavy halo.
    return Brush.verticalGradient(
        0f to light.copy(alpha = alpha),
        0.38f to light.copy(alpha = alpha * 0.90f),
        0.78f to light.copy(alpha = alpha * 0.72f),
        1f to light.copy(alpha = alpha * 0.82f),
    )
}
