package com.aritxonly.deadliner.ui.base

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** The window, material and menu keep their size; only draw/layer observers follow the clock. */
@Composable
internal fun Modifier.dropdownMorphDrawing(
    source: Rect,
    progress: Animatable<Float, AnimationVector1D>,
    dark: Boolean,
): Modifier {
    val graphics = LocalGraphicsContext.current
    return drawWithCache {
        val shape = DropdownMorphShape(source, progress.value)
        val painter = graphics.shadowContext.createDropShadowPainter(shape,
            Shadow(radius = 20.dp, offset = DpOffset(0.dp, 4.dp), color = Color.Black,
                alpha = if (dark) 0.28f else 0.12f))
        onDrawWithContent {
            with(painter) { draw(size, alpha = progress.value.coerceIn(0f, 1f)) }
            drawContent()
        }
    }.graphicsLayer {
        // A new immutable outline per layer update is intentional: a mutable Shape can leave
        // OutlineResolver's cache stale at the start/end of the transition.
        shape = DropdownMorphShape(source, progress.value)
        clip = true
    }
}
