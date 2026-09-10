package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Deadliner 基础 AlertDialog 组件
 * 使用 MIUIX 的 state-based 显示机制。
 */
@Composable
fun AlertDialog(
    // 💥 核心修改：强制要求传入 show 状态，用来兼容 MIUIX 的动画生命周期
    show: Boolean,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    renderTextContentInMiuix: Boolean = false,

    // ==========================================
    // MIUIX 专属辅助参数
    // ==========================================
    miuixTitle: String? = null,
    miuixSummary: String? = null
) {
    val showInlineTitle = title != null && miuixTitle == null
    val showInlineText = text != null && (renderTextContentInMiuix || miuixSummary == null)
    val showInlineContent = showInlineTitle || showInlineText
    val hasTextAboveActions = showInlineContent || miuixTitle != null || miuixSummary != null
    MiuixDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = miuixTitle,
        summary = miuixSummary,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showInlineTitle) title?.invoke()
            if (showInlineText) text?.invoke()
            MiuixDialogActions(
                modifier = Modifier.fillMaxWidth().padding(
                    top = if (hasTextAboveActions) DeadlinerMiuixDefaults.DialogActionTopSpacing else 0.dp,
                ),
                secondaryButton = dismissButton,
                primaryButton = confirmButton,
            )
        }
    }
}

/** Equal-width, filled action buttons used by Miuix dialog content. */
@Composable
internal fun MiuixDialogActions(
    primaryButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    secondaryButton: @Composable (() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        if (secondaryButton != null) {
            MiuixDialogActionSlot(
                style = MiuixDialogActionStyle.Secondary,
                content = secondaryButton,
            )
            Spacer(modifier = Modifier.width(DeadlinerMiuixDefaults.DialogActionSpacing))
        }
        MiuixDialogActionSlot(
            style = MiuixDialogActionStyle.Primary,
            content = primaryButton,
        )
    }
}

@Composable
private fun RowScope.MiuixDialogActionSlot(
    style: MiuixDialogActionStyle,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalMiuixDialogActionStyle provides style,
            content = content,
        )
    }
}
