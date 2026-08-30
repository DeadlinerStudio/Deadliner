package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem

// 为官方和 MIUIX 的组件起别名，防止重名冲突
import androidx.compose.material3.Button as Material3Button
import androidx.compose.material3.LocalContentColor as Material3LocalContentColor
import androidx.compose.material3.TextButton as Material3TextButton
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonColors as MiuixButtonColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors as MiuixTextButtonColors
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class MiuixDialogActionStyle {
    Default,
    Secondary,
    Primary,
}

internal val LocalMiuixDialogActionStyle = staticCompositionLocalOf {
    MiuixDialogActionStyle.Default
}

@Composable
private fun resolveMiuixButtonColors(
    style: MiuixDialogActionStyle,
    fallback: ButtonColors,
): MiuixButtonColors = when (style) {
    MiuixDialogActionStyle.Default -> MiuixButtonDefaults.buttonColors(
        color = fallback.containerColor,
        contentColor = fallback.contentColor,
        disabledColor = fallback.disabledContainerColor,
        disabledContentColor = fallback.disabledContentColor,
    )

    MiuixDialogActionStyle.Secondary -> MiuixButtonDefaults.buttonColors(
        contentColor = MiuixTheme.colorScheme.onSurface,
        disabledContentColor = MiuixTheme.colorScheme.disabledOnSurface,
    )

    MiuixDialogActionStyle.Primary -> MiuixButtonDefaults.buttonColorsPrimary()
}

@Composable
private fun resolveMiuixTextButtonColors(
    style: MiuixDialogActionStyle,
): MiuixTextButtonColors = when (style) {
    MiuixDialogActionStyle.Default -> MiuixButtonDefaults.textButtonColors()
    MiuixDialogActionStyle.Secondary -> MiuixButtonDefaults.textButtonColors(
        textColor = MiuixTheme.colorScheme.onSurface,
        disabledTextColor = MiuixTheme.colorScheme.disabledOnSurface,
    )

    MiuixDialogActionStyle.Primary -> MiuixButtonDefaults.textButtonColorsPrimary()
}

private fun Modifier.miuixDialogActionWidth(style: MiuixDialogActionStyle): Modifier =
    if (style == MiuixDialogActionStyle.Default) this else fillMaxWidth()

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
    forceMaterial3: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val designSystem = if (forceMaterial3) AppDesignSystem.MATERIAL3 else LocalAppDesignSystem.current
    when (designSystem) {
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
            val dialogActionStyle = LocalMiuixDialogActionStyle.current
            val miuixColors = resolveMiuixButtonColors(dialogActionStyle, colors)
            val resolvedContentColor =
                if (enabled) miuixColors.contentColor else miuixColors.disabledContentColor
            MiuixButton(
                onClick = onClick,
                modifier = modifier.miuixDialogActionWidth(dialogActionStyle),
                enabled = enabled,
                cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                colors = miuixColors,
            ) {
                CompositionLocalProvider(
                    Material3LocalContentColor provides resolvedContentColor,
                    MiuixLocalContentColor provides resolvedContentColor,
                ) {
                    content()
                }
            }
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
    forceMaterial3: Boolean = false,

    // MIUIX 专属辅助参数
    miuixText: String = "",

    content: @Composable RowScope.() -> Unit
) {
    val designSystem = if (forceMaterial3) AppDesignSystem.MATERIAL3 else LocalAppDesignSystem.current
    when (designSystem) {
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
            val dialogActionStyle = LocalMiuixDialogActionStyle.current
            val miuixModifier = modifier.miuixDialogActionWidth(dialogActionStyle)
            if (miuixText.isNotBlank()) {
                MiuixTextButton(
                    text = miuixText,
                    onClick = onClick,
                    modifier = miuixModifier,
                    enabled = enabled,
                    cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                    colors = resolveMiuixTextButtonColors(dialogActionStyle),
                )
            } else {
                val miuixColors = resolveMiuixButtonColors(dialogActionStyle, colors)
                val resolvedContentColor =
                    if (enabled) miuixColors.contentColor else miuixColors.disabledContentColor
                MiuixButton(
                    onClick = onClick,
                    modifier = miuixModifier,
                    enabled = enabled,
                    cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                    colors = miuixColors,
                ) {
                    CompositionLocalProvider(
                        Material3LocalContentColor provides resolvedContentColor,
                        MiuixLocalContentColor provides resolvedContentColor,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
