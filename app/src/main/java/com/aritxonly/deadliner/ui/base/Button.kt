package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.R
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem

// 为官方和 MIUIX 的组件起别名，防止重名冲突
import androidx.compose.material3.Button as Material3Button
import androidx.compose.material3.TextButton as Material3TextButton
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

/**
 * Deadliner 基础 Button 组件 (实心按钮)
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    when (LocalAppDesignSystem.current) {
        AppDesignSystem.MATERIAL3 -> {
            Material3Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                elevation = elevation,
                border = border,
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content
            )
        }
        AppDesignSystem.MIUIX -> {
            MiuixButton(
                onClick = onClick,
                modifier = modifier.padding(vertical = 8.dp),
                enabled = enabled,
                insideMargin = PaddingValues(12.dp),
                colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColors(
                    color = colors.containerColor,
                ),
                content = content
            )
        }
    }
}

/**
 * Deadliner 基础 TextButton 组件 (文本按钮)
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },

    // MIUIX 专属辅助参数
    miuixText: String = "",

    content: @Composable RowScope.() -> Unit
) {
    when (LocalAppDesignSystem.current) {
        AppDesignSystem.MATERIAL3 -> {
            Material3TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                elevation = elevation,
                border = border,
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content
            )
        }
        AppDesignSystem.MIUIX -> {
            MiuixTextButton(
                // 核心差异：MIUIX 直接要 String
                text = miuixText,
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                insideMargin = contentPadding,

                // 同样放权，保持默认透明背景
            )
        }
    }
}