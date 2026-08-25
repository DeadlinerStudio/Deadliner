package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.shapes.SmoothCapsuleShape

object SoftGlassDefaults {
    val Shape: Shape = SmoothCapsuleShape()
    val IconButtonSize: Dp = 44.dp
}

/**
 * Layout-neutral high-level entry point for Deadliner's soft-glass material.
 *
 * The caller owns size and padding. This layer only resolves the active material, backdrop,
 * neutral tint and optical recipe before delegating to [deadlinerGlassMaterial].
 */
@Composable
fun SoftGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = SoftGlassDefaults.Shape,
    recipe: GlassMaterialRecipe? = null,
    tint: Color? = null,
    interaction: GlassMaterialInteraction = GlassMaterialInteraction(),
    backdrop: LayerBackdrop? = LocalAdvancedMaterialBackdrop.current,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val resolvedRecipe = recipe ?: DeadlinerGlassRecipes.floatingNavigation(
        advancedMaterial.fineTuning,
    )
    val resolvedTint = tint ?: advancedMaterial.fineTuning
        .softGlassTintColor(isDark)
        .copy(
            alpha = if (advancedMaterial.enabled) {
                advancedMaterial.navigationTintAlpha
            } else {
                0.92f
            },
        )

    Box(
        modifier = modifier
            .clip(shape)
            .deadlinerGlassMaterial(
                advancedMaterial = advancedMaterial,
                backdrop = backdrop,
                shape = shape,
                tint = resolvedTint,
                isDark = isDark,
                recipe = resolvedRecipe,
                interaction = interaction,
            ),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** A circular soft-glass host for icon-only top-bar actions. */
@Composable
fun SoftGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = SoftGlassDefaults.IconButtonSize,
    recipe: GlassMaterialRecipe? = null,
    backdrop: LayerBackdrop? = LocalAdvancedMaterialBackdrop.current,
    content: @Composable () -> Unit,
) {
    SoftGlassSurface(
        modifier = modifier.size(size),
        shape = SoftGlassDefaults.Shape,
        recipe = recipe,
        backdrop = backdrop,
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            content = content,
        )
    }
}
