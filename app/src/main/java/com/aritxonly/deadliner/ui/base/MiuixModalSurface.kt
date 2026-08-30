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
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.GlassMaterialRecipe
import com.aritxonly.deadliner.ui.material.glass.GlassTintStyle
import com.aritxonly.deadliner.ui.material.glass.LocalImmersiveExperienceEnabled
import com.aritxonly.deadliner.ui.material.glass.deadlinerGlassMaterial
import com.aritxonly.deadliner.ui.material.glass.softGlassTintColor
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Immutable
data class MiuixModalSurface(
    val modifier: Modifier,
    val backgroundColor: Color,
)

/**
 * Resolves one material policy for every Miuix modal surface.
 *
 * Advanced material off intentionally uses an opaque container. When it is on, immersive mode
 * receives Deadliner's soft-glass optics; otherwise we retain the standard Miuix texture blur.
 */
@Composable
fun rememberMiuixModalSurface(
    shape: Shape,
    recipe: GlassMaterialRecipe,
): MiuixModalSurface {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val backdrop = LocalAdvancedMaterialBackdrop.current
        ?: LocalAdvancedMaterialDialogBlurHostState.current?.modalBackdrop
    val immersive = LocalImmersiveExperienceEnabled.current
    val container = MiuixTheme.colorScheme.surfaceContainer
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f

    if (!advancedMaterial.enabled) {
        return MiuixModalSurface(Modifier, container)
    }

    if (immersive) {
        if (backdrop == null) {
            return MiuixModalSurface(Modifier, container)
        }
        val tint = advancedMaterial.fineTuning.softGlassTintColor(isDark)
        return MiuixModalSurface(
            modifier = Modifier.deadlinerGlassMaterial(
                advancedMaterial = advancedMaterial,
                backdrop = backdrop,
                shape = shape,
                tint = tint,
                tintStyle = GlassTintStyle.Neutral,
                isDark = isDark,
                recipe = recipe,
            ),
            backgroundColor = Color.Transparent,
        )
    }

    if (backdrop != null) {
        val colors = BlurDefaults.blurColors(
            blendColors = listOf(BlendColorEntry(container.copy(alpha = 0.84f))),
        )
        return MiuixModalSurface(
            modifier = Modifier.textureBlur(
                backdrop = backdrop,
                shape = shape,
                colors = colors,
            ),
            backgroundColor = Color.Transparent,
        )
    }

    return MiuixModalSurface(Modifier, container)
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
    val shape = RoundedCornerShape(32.dp)
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val surface = rememberMiuixModalSurface(
        shape = shape,
        recipe = DeadlinerGlassRecipes.dialog(advancedMaterial.fineTuning),
    )

    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
        summary = summary,
        modifier = modifier.then(surface.modifier),
        backgroundColor = surface.backgroundColor,
        cornerRadius = 32.dp,
        maxWidth = maxWidth,
        renderInRootScaffold = true,
        content = content,
    )
}
