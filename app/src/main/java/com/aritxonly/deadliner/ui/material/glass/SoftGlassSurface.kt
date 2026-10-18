package com.aritxonly.deadliner.ui.material.glass

import com.aritxonly.deadliner.ui.base.feedback.miuixControlFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.LayerBackdrop

object SoftGlassDefaults {
    val Shape: Shape = Capsule()
    val IconButtonSize: Dp = 44.dp
}

/**
 * Layout-neutral high-level entry point for Deadliner's soft-glass material.
 *
 * The caller owns size and padding. This layer only resolves the active material, backdrop,
 * tint strategy and optical recipe before delegating to [deadlinerGlassMaterial].
 */
@Composable
fun SoftGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = SoftGlassDefaults.Shape,
    recipe: GlassMaterialRecipe? = null,
    tint: Color? = null,
    tintStyle: GlassTintStyle = GlassTintStyle.Neutral,
    interaction: GlassMaterialInteraction = GlassMaterialInteraction(),
    materialAlpha: Float = 1f,
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
        modifier = modifier.clip(shape),
        contentAlignment = contentAlignment,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = materialAlpha.coerceIn(0f, 1f)
                }
                .deadlinerGlassMaterial(
                    advancedMaterial = advancedMaterial,
                    backdrop = backdrop,
                    shape = shape,
                    tint = resolvedTint,
                    tintStyle = tintStyle,
                    isDark = isDark,
                    recipe = resolvedRecipe,
                    interaction = interaction,
                ),
        )
        content()
    }
}

/** A circular soft-glass host for icon-only top-bar actions. */
@Composable
fun SoftGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = SoftGlassDefaults.IconButtonSize,
    recipe: GlassMaterialRecipe? = null,
    tint: Color? = null,
    tintStyle: GlassTintStyle = GlassTintStyle.Neutral,
    backdrop: LayerBackdrop? = LocalAdvancedMaterialBackdrop.current,
    materialAlpha: Float = 1f,
    neutralTintProgress: Float = 0f,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f

    // Match floating navigation: elevation belongs to the outer frame, not the glass clip.
    Box(
        modifier = modifier
            .size(size)
            .softGlassShadow(
                shape = SoftGlassDefaults.Shape,
                isDark = isDark,
                alpha = materialAlpha,
                profile = if (LocalProgressiveTopBarMaterialHosted.current) SoftGlassShadowProfile.TopBarControl
                    else SoftGlassShadowProfile.Standard,
            )
            .miuixControlFeedback(enabled = enabled, shape = SoftGlassDefaults.Shape),
    ) {
        SoftGlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = SoftGlassDefaults.Shape,
            recipe = (recipe ?: DeadlinerGlassRecipes.floatingNavigation(
                LocalAdvancedMaterialSpec.current.fineTuning,
            )).copy(surfaceRole = GlassSurfaceRole.Control),
            tint = tint,
            tintStyle = tintStyle,
            backdrop = backdrop,
            materialAlpha = materialAlpha,
            interaction = GlassMaterialInteraction(neutralTintProgress = neutralTintProgress),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
