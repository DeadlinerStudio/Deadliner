package com.aritxonly.deadliner.ui.semantic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.model.semantic.SemanticTone

internal const val SemanticAtmosphereHeightDp = 224f

internal fun resolveSemanticAtmosphereAlpha(materialProgress: Float): Float =
    1f - materialProgress.coerceIn(0f, 1f)

@Composable
fun SemanticTopAtmosphere(
    tone: SemanticTone,
    module: SemanticAtmosphereModule? = null,
    materialProgress: Float,
    modifier: Modifier = Modifier,
) {
    val targetPalette = rememberSemanticAtmospherePalette(tone, module)
    val restingAlpha = if (isSystemInDarkTheme()) 0.82f else 1f
    val mainColor by animateColorAsState(
        targetValue = targetPalette.mainColor,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "semantic-atmosphere-main",
    )
    val bloomColor by animateColorAsState(
        targetValue = targetPalette.bloomColor,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "semantic-atmosphere-bloom",
    )
    val atmosphereAlpha by animateFloatAsState(
        targetValue = restingAlpha * resolveSemanticAtmosphereAlpha(materialProgress),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "semantic-atmosphere-alpha",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(SemanticAtmosphereHeightDp.dp)
            .graphicsLayer { alpha = atmosphereAlpha },
    ) {
        drawSemanticAtmosphere(
            mainColor = mainColor,
            bloomColor = bloomColor,
            orbColor = targetPalette.orbColor,
        )
    }
}

private fun DrawScope.drawSemanticAtmosphere(
    mainColor: Color,
    bloomColor: Color,
    orbColor: Color,
) {
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to mainColor.copy(alpha = 0.16f),
                0.48f to mainColor.copy(alpha = 0.055f),
                1f to Color.Transparent,
            ),
        ),
    )

    // Match iOS's composition: a cool off-canvas orb at top-leading and a smaller semantic
    // bloom at top-trailing. Both fade inside the fixed canvas instead of forming a color sheet.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                orbColor.copy(alpha = 0.07f),
                orbColor.copy(alpha = 0.018f),
                Color.Transparent,
            ),
            center = Offset(size.width * -0.08f, size.height * -0.08f),
            radius = size.height * 0.88f,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                bloomColor.copy(alpha = 0.13f),
                bloomColor.copy(alpha = 0.04f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.94f, size.height * 0.04f),
            radius = size.height * 0.64f,
        ),
    )
}
