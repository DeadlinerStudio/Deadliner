package com.aritxonly.deadliner.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Keeps floating overlays on a raised surface instead of inheriting the window background. */
@Composable
fun deadlinerOverlayContainerColor(): Color = MiuixTheme.colorScheme.surfaceContainer

internal fun resolveOverlayContainerColor(miuixSurfaceContainer: Color): Color = miuixSurfaceContainer
