package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem

// 别名防止冲突
import androidx.compose.material3.AlertDialog as Material3AlertDialog
import top.yukonga.miuix.kmp.extra.WindowDialog as MiuixWindowDialog

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

    // ==========================================
    // MIUIX 专属辅助参数
    // ==========================================
    miuixTitle: String? = null,
    miuixSummary: String? = null
) {
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
            // 1. 状态桥接：把外层传进来的 Boolean 包装成 MIUIX 需要的 MutableState
            val miuixShowState = remember { mutableStateOf(show) }

            // 2. 同步向下：当外层的 show 变化时，同步给 MIUIX
            LaunchedEffect(show) {
                miuixShowState.value = show
            }

            // 3. 同步向上：如果 MIUIX 内部（比如手势返回、点击外部）把状态改为了 false，我们要通知外层业务
            LaunchedEffect(miuixShowState.value) {
                if (!miuixShowState.value && show) {
                    onDismissRequest()
                }
            }

            // MIUIX 分支：传入伪装好的 MutableState
            MiuixWindowDialog(
                show = miuixShowState, // 传入包装好的 MutableState
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                title = miuixTitle,
                summary = miuixSummary,
            ) {
                // 按钮排列逻辑保持不变
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
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