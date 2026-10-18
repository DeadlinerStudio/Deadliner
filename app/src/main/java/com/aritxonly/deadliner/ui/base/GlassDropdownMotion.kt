package com.aritxonly.deadliner.ui.base

import androidx.compose.animation.core.CubicBezierEasing
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
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes

internal object GlassDropdownMotion {
    val CornerRadius = DeadlinerGlassRecipes.DropdownCornerRadius
    val ItemInset = 8.dp
    val HoverCornerRadius = CornerRadius - ItemInset
    const val HoverAlpha = 0.06f
    const val EnterDuration = 320
    const val ExitDuration = 220
    val Easing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)

    fun bounds(source: Rect, size: Size, fraction: Float): Rect =
        lerp(source, Rect(0f, 0f, size.width, size.height), fraction.coerceIn(0f, 1f))

    // The original row owns source glyphs throughout; menu glyphs enter after they fade.
    fun contentAlpha(fraction: Float): Float = smooth((fraction - 0.25f) / 0.57f)
    fun sourceAlpha(fraction: Float): Float = 1f - smooth(fraction / 0.25f)
    // A collapsed popup must be transparent before its native window is removed.
    fun surfaceAlpha(fraction: Float): Float = smooth(fraction / 0.25f)

    private fun smooth(value: Float): Float {
        val p = value.coerceIn(0f, 1f)
        return p * p * (3f - 2f * p)
    }
}

internal class DropdownMorphShape(private val source: Rect, private val fraction: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val bounds = GlassDropdownMotion.bounds(source, size, fraction)
        val targetRadius = with(density) { GlassDropdownMotion.CornerRadius.toPx() }
        val radius = source.height.coerceAtLeast(0f) / 2f * (1f - fraction) + targetRadius * fraction
        return Outline.Rounded(RoundRect(bounds, CornerRadius(radius)))
    }
}
