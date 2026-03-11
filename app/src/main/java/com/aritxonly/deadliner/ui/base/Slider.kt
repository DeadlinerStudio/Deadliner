package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem

// 为官方和 MIUIX 的组件起别名，防止重名冲突
import androidx.compose.material3.Slider as Material3Slider
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider

/**
 * Deadliner 基础 Slider 组件
 * 签名覆盖了你在业务层用到的所有 Material 3 参数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (SliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = colors,
            enabled = enabled
        )
    }
) {
    when (LocalAppDesignSystem.current) {
        AppDesignSystem.MATERIAL3 -> {
            // M3 分支：完全透传，保留你的气泡动画和 M3 颜色
            Material3Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = onValueChangeFinished,
                colors = colors,
                interactionSource = interactionSource,
                thumb = thumb
            )
        }
        AppDesignSystem.MIUIX -> {
            // MIUIX 分支：降级处理与特性替换
            MiuixSlider(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = onValueChangeFinished

                // 丢弃 M3 的 thumb 参数：MIUIX 使用粗条形设计，无独立滑块
                // 丢弃 M3 的 interactionSource 和 colors
                // 默认享受 MIUIX 自带的 hapticEffect = SliderHapticEffect.Edge 震动反馈
            )
        }
    }
}