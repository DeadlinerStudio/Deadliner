package com.aritxonly.deadliner.ui.base

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.model.MiuixModalKind
import com.aritxonly.deadliner.model.miuixModalMaterial
import com.aritxonly.deadliner.ui.material.glass.GlassMaterialRecipe
import com.aritxonly.deadliner.ui.material.glass.GlassTintStyle
import com.aritxonly.deadliner.ui.material.glass.deadlinerGlassMaterial
import com.aritxonly.deadliner.ui.material.glass.softGlassTintColor
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.Backdrop
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
    backdrop: Backdrop? = null,
): MiuixModalSurface {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val resolvedBackdrop = backdrop ?: LocalAdvancedMaterialBackdrop.current
        ?: LocalAdvancedMaterialDialogBlurHostState.current?.modalBackdrop
    val container = MiuixTheme.colorScheme.surfaceContainer
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val modalMaterial = advancedMaterial.fineTuning.miuixModalMaterial(kind)

    if (!advancedMaterial.textureBlurEnabled) {
        return MiuixModalSurface(Modifier, container)
    }

    if (resolvedBackdrop == null) return MiuixModalSurface(Modifier, container)
    val tint = if (kind == MiuixModalKind.Dialog) container
        else advancedMaterial.fineTuning.softGlassTintColor(isDark)
    return MiuixModalSurface(
        modifier = Modifier.deadlinerGlassMaterial(
            advancedMaterial = advancedMaterial, backdrop = resolvedBackdrop, shape = shape,
            tint = tint, tintStyle = GlassTintStyle.Neutral, isDark = isDark, recipe = recipe,
            blurRadiusOverride = modalMaterial.blurRadiusDp,
            tintAlphaOverride = modalMaterial.backgroundAlpha,
        ),
        backgroundColor = Color.Transparent,
    )
}

/** Root-hosted Miuix dialog; conditional callers may disappear before its exit finishes. */
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
    val host = LocalAdvancedMaterialDialogBlurHostState.current
    val request = MiuixDialogRequest(title, summary, modifier, maxWidth,
        currentCompositionLocalContext, onDismissRequest, content)
    if (host == null) {
        MiuixDialogSurface(show, request, onDismissFinished = {})
        return
    }
    val id = remember { Any() }
    SideEffect { if (show) host.present(id, request) else host.hide(id) }
    DisposableEffect(host, id) { onDispose { host.hide(id) } }
}
