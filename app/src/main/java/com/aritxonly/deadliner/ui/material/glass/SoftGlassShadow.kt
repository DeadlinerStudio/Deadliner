package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

internal data class SoftGlassShadowTokens(
    val outerRadius: Dp,
    val outerSpread: Dp,
    val outerAlpha: Float,
    val edgeRadius: Dp,
    val edgeSpread: Dp,
    val edgeAlpha: Float,
    val offset: DpOffset = DpOffset.Zero,
)

internal fun softGlassShadowTokens(
    isDark: Boolean,
): SoftGlassShadowTokens = if (isDark) {
    SoftGlassShadowTokens(
        outerRadius = 7.dp,
        outerSpread = (-0.75).dp,
        outerAlpha = 0.052f,
        edgeRadius = 3.dp,
        edgeSpread = (-0.75).dp,
        edgeAlpha = 0.010f,
    )
} else {
    SoftGlassShadowTokens(
        outerRadius = 8.dp,
        outerSpread = (-0.75).dp,
        outerAlpha = 0.068f,
        edgeRadius = 3.5.dp,
        edgeSpread = (-0.75).dp,
        edgeAlpha = 0.014f,
    )
}

internal fun Modifier.softGlassShadow(
    shape: Shape,
    isDark: Boolean,
    alpha: Float = 1f,
): Modifier {
    val tokens = softGlassShadowTokens(isDark)
    val resolvedAlpha = alpha.coerceIn(0f, 1f)
    return dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = tokens.outerRadius,
            spread = tokens.outerSpread,
            offset = tokens.offset,
            color = Color.Black,
            alpha = tokens.outerAlpha * resolvedAlpha,
        ),
    ).dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = tokens.edgeRadius,
            spread = tokens.edgeSpread,
            offset = tokens.offset,
            color = Color.Black,
            alpha = tokens.edgeAlpha * resolvedAlpha,
        ),
    )
}
