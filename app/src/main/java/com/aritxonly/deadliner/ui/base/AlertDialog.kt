package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem

// 别名防止冲突
import androidx.compose.material3.AlertDialog as Material3AlertDialog
import top.yukonga.miuix.kmp.window.WindowDialog as MiuixWindowDialog

/**
 * Deadliner 基础 AlertDialog 组件
 * 融合了 M3 的插槽机制与 MIUIX 的 state-based 显示机制
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
    RegisterAdvancedMaterialDialogBlur(show = show)

    when (LocalAppDesignSystem.current) {

        AppDesignSystem.MATERIAL3 -> {
            // M3 分支：只有当 show 为 true 时，才将其挂载到 Compose 树上
            if (show) {
                Material3AlertDialog(
                    onDismissRequest = onDismissRequest,
                    confirmButton = confirmButton,
                    modifier = modifier,
                    dismissButton = dismissButton,
                    title = title,
                    text = text,
                    shape = shape,
                    containerColor = containerColor
                )
            }
        }

        AppDesignSystem.MIUIX -> {
            val showInlineTitle = title != null && miuixTitle == null
            val showInlineText = text != null && (renderTextContentInMiuix || miuixSummary == null)
            val showInlineContent = showInlineTitle || showInlineText

            // MIUIX 分支：传入伪装好的 MutableState
            MiuixWindowDialog(
                show = show, // 传入包装好的 MutableState
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                title = miuixTitle,
                summary = miuixSummary,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showInlineTitle) {
                        title?.invoke()
                    }
                    if (showInlineText) {
                        text?.invoke()
                    }

                    CompositionLocalProvider(LocalAppDesignSystem provides AppDesignSystem.MATERIAL3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (showInlineContent) 16.dp else 0.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            dismissButton?.invoke()
                            if (dismissButton != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
