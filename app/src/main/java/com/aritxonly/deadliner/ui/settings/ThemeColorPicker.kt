package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.AppearanceColorSource
import com.aritxonly.deadliner.ui.iconResource

val M3BaselineColors = listOf(
    "#365AB0",

    // ✨ M3 官方基准色 (经典、沉稳)
    "#6750A4", // Baseline Purple (基准紫)
    "#0061A4", // Baseline Blue (基准蓝)
    "#006874", // Baseline Cyan (深海青)
    "#386A20", // Baseline Green (护眼绿)

    // 🌿 自然与平静 (适合缓解 DDL 焦虑)
    "#2E7D32", // Forest Green (森林绿)
    "#556B2F", // Olive Drab (橄榄绿)
    "#00796B", // Teal (水鸭青)
    "#5D4037", // Brown (大地棕)

    // 🔥 活力与紧迫感 (适合专注冲刺)
    "#C62828", // Deep Red (警示红)
    "#E65100", // Deep Orange (落日橙)
    "#F57F17", // Amber (琥珀黄)
    "#880E4F"  // Deep Pink (晚樱粉)
)

@Composable
fun ThemeColorPicker(
    currentSource: AppearanceColorSource,
    currentSeed: String?,
    showPreset: Boolean,
    onColorSelected: (AppearanceColorSource, String?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. 动态壁纸取色按钮 (传 null 代表跟随系统)
        item {
            ColorCircle(
                option = ThemeColorOption.SystemDynamic,
                isSelected = currentSource == AppearanceColorSource.SystemDynamic,
                onClick = { onColorSelected(AppearanceColorSource.SystemDynamic, null) }
            )
        }

        if (showPreset) {
            item {
                ColorCircle(
                    option = ThemeColorOption.Preset,
                    isSelected = currentSource == AppearanceColorSource.Preset,
                    onClick = { onColorSelected(AppearanceColorSource.Preset, null) },
                )
            }
        }

        // 2. 官方预设色卡
        items(M3BaselineColors) { hex ->
            ColorCircle(
                option = ThemeColorOption.Seed(hex),
                isSelected = currentSource == AppearanceColorSource.SeedColor && currentSeed == hex,
                onClick = { onColorSelected(AppearanceColorSource.SeedColor, hex) }
            )
        }
    }
}

@Composable
private fun ColorCircle(
    option: ThemeColorOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (option) {
        ThemeColorOption.SystemDynamic -> MaterialTheme.colorScheme.surfaceVariant
        ThemeColorOption.Preset -> Color.Transparent
        is ThemeColorOption.Seed -> Color(option.hex.toColorInt())
    }
    val description = when (option) {
        ThemeColorOption.SystemDynamic -> stringResource(R.string.settings_color_source_dynamic)
        ThemeColorOption.Preset -> stringResource(R.string.settings_color_source_preset)
        is ThemeColorOption.Seed -> stringResource(R.string.settings_color_source_seed)
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (option == ThemeColorOption.Preset) {
                    Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3482FF),
                                Color(0xFFF08A7E),
                                Color(0xFF53DBC8),
                            )
                        )
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .then(
                // 选中时加一个边框反馈
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // 选中时打个勾
            Icon(
                imageVector = iconResource(R.drawable.ic_on),
                contentDescription = description,
                tint = if (option == ThemeColorOption.SystemDynamic) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    Color.White
                }
            )
        } else if (option == ThemeColorOption.SystemDynamic) {
            // 动态颜色的图标提示
            Icon(
                imageVector = iconResource(R.drawable.ic_wallpaper),
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private sealed interface ThemeColorOption {
    data object SystemDynamic : ThemeColorOption
    data object Preset : ThemeColorOption
    data class Seed(val hex: String) : ThemeColorOption
}
