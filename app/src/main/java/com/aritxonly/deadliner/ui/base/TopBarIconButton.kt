package com.aritxonly.deadliner.ui.base

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.aritxonly.deadliner.ui.material.glass.LocalProgressiveTopBarMaterialHosted
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarButtonMaterialProgress
import com.aritxonly.deadliner.ui.material.glass.SoftGlassIconButton

internal val LocalTopBarButtonMaterialHosted = staticCompositionLocalOf { false }

/** Uses a circular soft-glass host inside a progressive top bar. */
@Composable
fun TopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (LocalProgressiveTopBarMaterialHosted.current && !LocalTopBarButtonMaterialHosted.current) {
        SoftGlassIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            materialAlpha = LocalTopBarButtonMaterialProgress.current,
            content = content,
        )
    } else {
        IconButton(
            onClick = onClick,
            modifier = (modifier).miuixPressFeedback(enabled = enabled, shape = androidx.compose.material3.IconButtonDefaults.standardShape),
            enabled = enabled,
            content = content,
        )
    }
}
