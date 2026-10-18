package com.aritxonly.deadliner.ui.base.sheet

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

internal val LocalSheetExpansionProgress = staticCompositionLocalOf<State<Float>> { mutableStateOf(1f) }

internal object SheetExpansionMotion {
    const val Duration = 420
    val Easing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)
    fun bounds(source: Rect, size: Size, progress: Float): Rect =
        lerp(source, Rect(0f, 0f, size.width, size.height), progress.coerceIn(0f, 1f))
    fun contentAlpha(progress: Float): Float {
        val p = ((progress - 0.18f) / 0.62f).coerceIn(0f, 1f)
        return p * p * (3f - 2f * p)
    }
}

/** The outer reveal owns motion; the glass keeps its complete, fixed optical geometry. */
internal class SheetExpansionShape(private val source: Rect, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = progress.coerceIn(0f, 1f)
        val bounds = SheetExpansionMotion.bounds(source, size, p)
        val radius = with(density) { (28.dp + 8.dp * p).toPx() }
            .coerceAtMost(minOf(bounds.width, bounds.height).coerceAtLeast(0f) / 2f)
        return Outline.Rounded(RoundRect(bounds,
            topLeft = CornerRadius(radius), topRight = CornerRadius(radius),
            bottomRight = CornerRadius(radius * (1f - p)), bottomLeft = CornerRadius(radius * (1f - p))))
    }
}
