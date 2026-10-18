// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
package com.aritxonly.deadliner.ui.base.sheet

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.DialogLayout
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** OverlayBottomSheet's 0.9.4-rc01 popup adapter, with the Scaffold owning the whole sheet header. */
@Composable
internal fun ScaffoldSheetOverlay(
    show: Boolean,
    modifier: Modifier,
    backgroundColor: Color,
    topInset: Dp,
    enterFrom: Rect? = null,
    allowDismiss: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    BottomSheetContentLayout(
        show = show,
        showDefaultHeader = false,
        topInset = topInset,
        enterFrom = enterFrom,
        modifier = modifier,
        backgroundColor = backgroundColor,
        cornerRadius = 36.dp,
        sheetMaxWidth = 560.dp,
        outsideMargin = DpSize(0.dp, 0.dp),
        insideMargin = DpSize(0.dp, 0.dp),
        dragHandleColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        allowDismiss = allowDismiss,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        popupHost = { visible, hostContent ->
            val visibleState = remember { mutableStateOf(false) }
            visibleState.value = visible
            DialogLayout(
                visible = visibleState,
                enableWindowDim = false,
                enterTransition = EnterTransition.None,
                exitTransition = ExitTransition.None,
                enableAutoLargeScreen = false,
                renderInRootScaffold = true,
            ) { hostContent() }
        },
        content = content,
    )
}
