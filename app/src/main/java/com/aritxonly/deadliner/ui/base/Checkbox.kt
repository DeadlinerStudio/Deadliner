package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults as Material3CheckboxDefaults
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 为官方和 MIUIX 的组件起别名
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox

/**
 * Deadliner 基础 RadioButton 组件
 * 使用 MIUIX 的勾选控件承载单选语义。
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val toggleState = if (selected) ToggleableState.On else ToggleableState.Off
    val scheme = MiuixTheme.colorScheme
    MiuixCheckbox(
        state = toggleState,
        onClick = onClick,
        modifier = modifier.padding(vertical = 4.dp),
        enabled = enabled,
        colors = CheckboxDefaults.checkboxColors(
            uncheckedForegroundColor = scheme.secondary,
            uncheckedBackgroundColor = scheme.secondary,
        ),
    )
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = Material3CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val toggleState = if (checked) ToggleableState.On else ToggleableState.Off
    val scheme = MiuixTheme.colorScheme
    MiuixCheckbox(
        state = toggleState,
        onClick = onCheckedChange?.let { callback -> { callback(!checked) } },
        modifier = modifier.padding(4.dp),
        enabled = enabled,
        colors = CheckboxDefaults.checkboxColors(
            uncheckedForegroundColor = scheme.secondary,
            uncheckedBackgroundColor = scheme.secondary,
            checkedBackgroundColor = colors.checkedBoxColor,
            checkedForegroundColor = colors.checkedCheckmarkColor,
        ),
    )
}
