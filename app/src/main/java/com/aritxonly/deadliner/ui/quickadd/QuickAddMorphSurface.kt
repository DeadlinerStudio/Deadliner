package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.GlassGeometrySpec
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import com.aritxonly.deadliner.ui.material.glass.softGlassShadow
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import kotlin.math.roundToInt

/** Resize the actual sampled lens, never a scaled texture or the input subtree. */
@Composable
internal fun QuickAddMorphSurface(progress: State<Float>, source: Rect) {
    val dark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val fineTuning = LocalAdvancedMaterialSpec.current.fineTuning
    val navigation = remember(fineTuning) { DeadlinerGlassRecipes.floatingNavigation(fineTuning) }
    val target = remember(fineTuning) { DeadlinerGlassRecipes.quickAdd(fineTuning) }
    SubcomposeLayout { constraints ->
        val p = progress.value
        val bounds = quickAddMorphRect(source,
            Rect(0f, 0f, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()), p)
        val corner = quickAddMorphCorner(source, 28.dp.toPx(), p).toDp()
        val materialAlpha = quickAddHandoff(p, 0f, 0.12f)
        val recipe = target.copy(
            blur = target.blur.copy(radiusMultiplier = navigation.blur.radiusMultiplier +
                (target.blur.radiusMultiplier - navigation.blur.radiusMultiplier) * p.coerceIn(0f, 1f)),
            geometry = GlassGeometrySpec(corner))
        val lens = subcompose("lens") {
            val shape = remember(corner) { RoundedCornerShape(corner) }
            SoftGlassSurface(Modifier.softGlassShadow(shape, dark, alpha = materialAlpha)
                .pointerInput(Unit) { detectTapGestures { } }, shape = shape,
                materialAlpha = materialAlpha, recipe = recipe) {}
        }.single().measure(Constraints.fixed(bounds.width.roundToInt().coerceAtLeast(1),
            bounds.height.roundToInt().coerceAtLeast(1)))
        layout(constraints.maxWidth, constraints.maxHeight) {
            lens.place(bounds.left.roundToInt(), bounds.top.roundToInt())
        }
    }
}
