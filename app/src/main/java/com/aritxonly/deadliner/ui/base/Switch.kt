package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem

// 为官方和 MIUIX 的组件起别名，防止重名冲突
import androidx.compose.material3.Switch as Material3Switch
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch

/**
 * Deadliner 基础 Switch 组件
 * 签名完全对齐 Material 3，实现双框架无缝路由
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    when (LocalAppDesignSystem.current) {
        AppDesignSystem.MATERIAL3 -> {
            // M3 分支：参数 1:1 完美透传
            Material3Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                thumbContent = thumbContent,
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource
            )
        }
        AppDesignSystem.MIUIX -> {
            // MIUIX 分支：降级处理
            MiuixSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier.padding(vertical = 2.dp),
                enabled = enabled

                // 1. thumbContent 被丢弃，因为 MIUIX 规范中 Switch 内部不放 Icon
                // 2. interactionSource 被丢弃
                // 3. colors 不传，直接让 MIUIX 使用它自己的主题默认色 (MiuixTheme.colorScheme.primary)
            )
        }
    }
}