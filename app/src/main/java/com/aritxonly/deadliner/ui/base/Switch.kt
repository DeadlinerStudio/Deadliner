package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 为官方和 MIUIX 的组件起别名，防止重名冲突
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch

/**
 * Deadliner 基础 Switch 组件
 * 兼容现有调用参数并统一使用 MIUIX 控件。
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
    val scheme = MiuixTheme.colorScheme
    MiuixSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.padding(vertical = 2.dp),
        enabled = enabled,
        colors = top.yukonga.miuix.kmp.basic.SwitchDefaults.switchColors(
            uncheckedThumbColor = scheme.onSecondary,
            uncheckedTrackColor = scheme.secondary,
        ),
    )
}
