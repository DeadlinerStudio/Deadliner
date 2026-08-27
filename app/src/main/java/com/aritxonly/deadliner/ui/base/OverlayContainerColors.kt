package com.aritxonly.deadliner.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Keeps floating overlays on a raised surface instead of inheriting the window background. */
@Composable
fun deadlinerOverlayContainerColor(): Color = resolveOverlayContainerColor(
    designSystem = LocalAppDesignSystem.current,
    materialSurfaceContainer = MaterialTheme.colorScheme.surfaceContainer,
    miuixSurfaceContainer = MiuixTheme.colorScheme.surfaceContainer,
)

internal fun resolveOverlayContainerColor(
    designSystem: AppDesignSystem,
    materialSurfaceContainer: Color,
    miuixSurfaceContainer: Color,
): Color = when (designSystem) {
    AppDesignSystem.MATERIAL3 -> materialSurfaceContainer
    AppDesignSystem.MIUIX -> miuixSurfaceContainer
}
