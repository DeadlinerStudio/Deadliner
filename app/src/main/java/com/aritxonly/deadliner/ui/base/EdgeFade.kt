package com.aritxonly.deadliner.ui.base

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies an alpha fade to either edge of the modified content.
 *
 * The modifier owns its offscreen layer so callers can use a single edge independently.
 * Passing zero for an edge keeps that edge fully opaque.
 */
@Stable
fun Modifier.edgeFade(
    top: Dp = 16.dp,
    bottom: Dp = 16.dp,
): Modifier = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val heightPx = size.height.coerceAtLeast(1f)
        var topPx = top.toPx().coerceIn(0f, heightPx)
        var bottomPx = bottom.toPx().coerceIn(0f, heightPx)
        val combinedFadePx = topPx + bottomPx
        if (combinedFadePx > heightPx) {
            val scale = heightPx / combinedFadePx
            topPx *= scale
            bottomPx *= scale
        }

        val transparent = Color.Black.copy(alpha = 0f)
        val opaque = Color.Black
        val topFraction = topPx / heightPx
        val bottomFraction = 1f - bottomPx / heightPx
        val colorStops = when {
            topPx > 0f && bottomPx > 0f -> arrayOf(
                0f to transparent,
                topFraction to opaque,
                bottomFraction to opaque,
                1f to transparent,
            )

            topPx > 0f -> arrayOf(
                0f to transparent,
                topFraction to opaque,
            )

            bottomPx > 0f -> arrayOf(
                bottomFraction to opaque,
                1f to transparent,
            )

            else -> arrayOf(
                0f to opaque,
                1f to opaque,
            )
        }

        drawRect(
            brush = Brush.verticalGradient(colorStops = colorStops),
            blendMode = BlendMode.DstIn,
        )
    }
