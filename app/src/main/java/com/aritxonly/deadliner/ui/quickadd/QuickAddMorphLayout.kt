package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Content is measured against the final panel. Only IME/host/content changes remeasure it. */
@Composable
internal fun QuickAddMorphLayout(
    progress: State<Float>, source: Rect, rightInset: Float, panelBottom: State<Float>,
    modifier: Modifier = Modifier,
    surface: @Composable (Rect) -> Unit,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier.fillMaxSize().semantics { paneTitle = "快速添加" }) { constraints ->
        val (left, right) = quickAddPanelHorizontalBounds(constraints.maxWidth.toFloat(), rightInset,
            12.dp.toPx(), 420.dp.toPx(), 600.dp.toPx())
        val width = (right - left).roundToInt().coerceAtLeast(1)
        val availableHeight = (constraints.maxHeight - panelBottom.value - 24.dp.toPx())
            .roundToInt().coerceAtLeast(1)
        val body = subcompose("content", content).single()
            .measure(Constraints(minWidth = width, maxWidth = width, maxHeight = availableHeight))
        val bottom = constraints.maxHeight - panelBottom.value
        val target = Rect(right - width, bottom - body.height, right, bottom)
        val localSource = source.translate(-target.topLeft)
        // The surface's child layout owns its changing physical size. Its outer constraints
        // remain fixed, so it cannot propagate clock-driven remeasurement into the text field.
        val lens = subcompose("surface") { surface(localSource) }.single()
            .measure(Constraints.fixed(width, body.height))
        val targetCorner = 28.dp.toPx()
        layout(constraints.maxWidth, constraints.maxHeight) {
            lens.place(target.left.roundToInt(), target.top.roundToInt())
            body.placeWithLayer(target.left.roundToInt(), target.top.roundToInt()) {
                val p = progress.value
                val bounds = quickAddMorphRect(source, target, p)
                translationX = bounds.right - target.right
                translationY = bounds.bottom - target.bottom
                alpha = quickAddHandoff(p, 0.2f, 0.7f)
                clip = true
                shape = LensRevealShape(bounds.width, bounds.height,
                    quickAddMorphCorner(source, targetCorner, p))
            }
        }
    }
}

internal fun quickAddMorphCorner(source: Rect, target: Float, progress: Float): Float =
    (source.width / 2f + (target - source.width / 2f) * progress.coerceIn(0f, 1f)).coerceAtLeast(0f)

/** Reveal the full-size content from its bottom-end corner without scaling its typography. */
private class LensRevealShape(private val width: Float, private val height: Float, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(size.width - width, size.height - height, size.width, size.height,
            CornerRadius(radius)))
}
