package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    backdrop: LayerBackdrop? = LocalAdvancedMaterialBackdrop.current,
    content: @Composable () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val glassTint = containerColor.copy(alpha = if (isDark) 0.82f else 0.86f)

    SoftGlassIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = SoftGlassFloatingActionButtonDefaults.Size,
        tint = glassTint,
        backdrop = backdrop,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
