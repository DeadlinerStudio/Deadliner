package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.model.MiuixModalKind
import com.aritxonly.deadliner.model.miuixModalMaterial
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.GlassMaterialRecipe
import com.aritxonly.deadliner.ui.material.glass.GlassTintStyle
import com.aritxonly.deadliner.ui.material.glass.deadlinerGlassMaterial
import com.aritxonly.deadliner.ui.material.glass.softGlassTintColor
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class MiuixModalSurface(
    val modifier: Modifier,
    val backgroundColor: Color,
)

/**
 * Resolves one material policy for every Miuix modal surface.
 *
 * Advanced material off uses an opaque container; enabled material uses shared soft-glass optics.
 */
@Composable
fun rememberMiuixModalSurface(
    kind: MiuixModalKind,
    shape: Shape,
    recipe: GlassMaterialRecipe,
): MiuixModalSurface {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val backdrop = LocalAdvancedMaterialBackdrop.current
        ?: LocalAdvancedMaterialDialogBlurHostState.current?.modalBackdrop
    val container = MiuixTheme.colorScheme.surfaceContainer
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val modalMaterial = advancedMaterial.fineTuning.miuixModalMaterial(kind)

    if (!advancedMaterial.enabled) {
        return MiuixModalSurface(Modifier, container)
    }

    if (backdrop == null) return MiuixModalSurface(Modifier, container)
    val tint = if (kind == MiuixModalKind.Dialog) container
        else advancedMaterial.fineTuning.softGlassTintColor(isDark)
    return MiuixModalSurface(
        modifier = Modifier.deadlinerGlassMaterial(
            advancedMaterial = advancedMaterial, backdrop = backdrop, shape = shape,
            tint = tint, tintStyle = GlassTintStyle.Neutral, isDark = isDark, recipe = recipe,
            blurRadiusOverride = modalMaterial.blurRadiusDp,
            tintAlphaOverride = modalMaterial.backgroundAlpha,
        ),
        backgroundColor = Color.Transparent,
    )
}

/** Page-scoped Miuix dialog with Deadliner's shared modal material policy. */
@Composable
fun MiuixDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String? = null,
    summary: String? = null,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 420.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(DeadlinerMiuixDefaults.DialogCornerRadius)
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val surface = rememberMiuixModalSurface(
        kind = MiuixModalKind.Dialog,
        shape = shape,
        recipe = DeadlinerGlassRecipes.immersiveDialogAdvancedHighlight(
            advancedMaterial.fineTuning,
        ),
    )

    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
        summary = summary,
        modifier = modifier.then(surface.modifier),
        backgroundColor = surface.backgroundColor,
        cornerRadius = DeadlinerMiuixDefaults.DialogCornerRadius,
        maxWidth = maxWidth,
        renderInRootScaffold = true,
        content = content,
    )
}
