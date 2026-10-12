package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalHomeFabColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop

object SoftGlassFloatingActionButtonDefaults {
    val Size = 58.dp
    val IconSize = 30.dp
}

/** A colored soft-glass FAB for immersive compact surfaces. */
@Composable
fun SoftGlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = LocalHomeFabColors.current.container,
    contentColor: Color = LocalHomeFabColors.current.content,
    size: Dp = SoftGlassFloatingActionButtonDefaults.Size,
    neutralTint: Boolean = false,
    backdrop: LayerBackdrop? = LocalAdvancedMaterialBackdrop.current,
    content: @Composable () -> Unit,
) {
    val tintProgress by animateFloatAsState(if (neutralTint) 1f else 0f,
        spring(dampingRatio = 0.78f, stiffness = 380f), label = "fabNeutralTint")
    val animatedContentColor by animateColorAsState(contentColor,
        spring(dampingRatio = 0.78f, stiffness = 380f), label = "fabContentTint")

    SoftGlassIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        tint = containerColor.copy(alpha = 1f),
        tintStyle = GlassTintStyle.SeedChromatic,
        neutralTintProgress = tintProgress,
        backdrop = backdrop,
    ) {
        CompositionLocalProvider(LocalContentColor provides animatedContentColor) {
            content()
        }
    }
}
