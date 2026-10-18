package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.GlassGeometrySpec
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import com.aritxonly.deadliner.ui.material.glass.softGlassShadow
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import kotlin.math.roundToInt

internal data class QuickAddMorphTransition(val progress: Float, val active: Boolean)

@Composable
internal fun quickAddMorphTransition(visible: Boolean): QuickAddMorphTransition {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = visible
    val transition = updateTransition(visibility, label = "quickAddLens")
    val progress by transition.animateFloat(transitionSpec = {
        spring(dampingRatio = 0.78f, stiffness = 380f)
    }, label = "boundsProgress") { if (it) 1f else 0f }
    return QuickAddMorphTransition(progress, visibility.currentState || visibility.targetState || transition.isRunning)
}

@Composable
internal fun QuickAddMorphHost(state: QuickAddState, progress: Float, source: Rect,
                               placement: QuickAddPlacement, onSaved: () -> Unit, onExpand: () -> Unit) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val fineTuning = LocalAdvancedMaterialSpec.current.fineTuning
    val navigationRecipe = DeadlinerGlassRecipes.floatingNavigation(fineTuning)
    val targetRecipe = DeadlinerGlassRecipes.quickAdd(fineTuning)
    val p = progress.coerceIn(0f, 1f)
    val recipe = targetRecipe.copy(blur = targetRecipe.blur.copy(
        radiusMultiplier = navigationRecipe.blur.radiusMultiplier +
            (targetRecipe.blur.radiusMultiplier - navigationRecipe.blur.radiusMultiplier) * p))
    val materialAlpha = quickAddHandoff(progress, 0f, 0.12f)
    val panelBottom by animateFloatAsState(placement.panelBottomInset, spring(stiffness = 500f), label = "lensKeyboardInset")
    SubcomposeLayout(Modifier.fillMaxSize().semantics { paneTitle = "快速添加" }) { constraints ->
        val (left, right) = quickAddPanelHorizontalBounds(constraints.maxWidth.toFloat(), placement.rightInset,
            with(density) { 12.dp.toPx() }, with(density) { 420.dp.toPx() }, with(density) { 600.dp.toPx() })
        val width = (right - left).roundToInt().coerceAtLeast(1)
        val availableHeight = (constraints.maxHeight - panelBottom - with(density) { 24.dp.toPx() })
            .roundToInt().coerceAtLeast(1)
        val content = subcompose("content") {
            QuickAddContent(state, onSaved, showClose = false, onExpand = onExpand,
                modifier = Modifier.onGloballyPositioned {
                    state.panelBounds = it.boundsInWindow().translate(it.positionOnScreen() - it.positionInWindow())
                },
                autofocus = state.visible && progress >= 0.98f)
        }.single().measure(Constraints(minWidth = width, maxWidth = width, maxHeight = availableHeight))
        val bottom = constraints.maxHeight - panelBottom
        val target = Rect(right - width, bottom - content.height, right, bottom)
        val bounds = quickAddMorphRect(source, target, progress)
        val corner = (source.width / 2f + (with(density) { 28.dp.toPx() } - source.width / 2f) * progress.coerceIn(0f, 1f))
            .coerceAtLeast(0f)
        val lens = subcompose("surface") {
            val shape = RoundedCornerShape(with(density) { corner.toDp() })
            SoftGlassSurface(Modifier.softGlassShadow(shape, colors.onSurface.luminance() > 0.5f, alpha = materialAlpha)
                .pointerInput(Unit) { detectTapGestures { } }, shape = shape,
                materialAlpha = materialAlpha,
                recipe = recipe.copy(geometry = GlassGeometrySpec(with(density) { corner.toDp() }))) {}
        }.single().measure(Constraints.fixed(bounds.width.roundToInt().coerceAtLeast(1), bounds.height.roundToInt().coerceAtLeast(1)))
        layout(constraints.maxWidth, constraints.maxHeight) {
            lens.place(bounds.left.roundToInt(), bounds.top.roundToInt())
            content.placeWithLayer((bounds.right - width).roundToInt(), (bounds.bottom - content.height).roundToInt()) {
                alpha = quickAddHandoff(progress, 0.2f, 0.7f)
                clip = true
                shape = LensRevealShape(bounds.width, bounds.height, corner)
            }
        }
    }
}

/** Reveal the full-size content from its bottom-end corner without scaling its typography. */
private class LensRevealShape(private val width: Float, private val height: Float, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(size.width - width, size.height - height, size.width, size.height,
            CornerRadius(radius)))
}
