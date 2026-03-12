package com.aritxonly.deadliner.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.settings.ThemeColorPicker
import com.aritxonly.deadliner.ui.settings.UiModeSelectionRow
// 如果你封装了跨框架的 Switch，建议用 ui.base.Switch；这里用 Material3 的做演示
import androidx.compose.material3.Switch

@Composable
fun UiModeScreen() {
    val darkTheme = isSystemInDarkTheme()

    // 1. 状态定义
    var currentStyle by remember { mutableStateOf(GlobalUtils.style) }
    var miuixModeEnabled by remember { mutableStateOf(GlobalUtils.miuixMode) }
    var selectedColorState by remember { mutableStateOf(GlobalUtils.seedColor) }

    // 2. 状态回调
    val onStyleChange: (String) -> Unit = {
        GlobalUtils.style = it
        currentStyle = it
    }

    val onMiuixModeChange: (Boolean) -> Unit = { enabled ->
        GlobalUtils.miuixMode = enabled
        miuixModeEnabled = enabled
        // 🌟 核心联动：如果关掉了 MIUIX 引擎，且当前刚好是 MIUIX 布局，强制回退到极简布局
        if (!enabled && currentStyle == "miuix") {
            onStyleChange("simplified")
        }
    }

    val onThemeChange: (String?) -> Unit = {
        GlobalUtils.seedColor = it
        selectedColorState = it
    }

    // 3. 颜色反转滤镜 (深色模式适配图)
    val invertColorFilter = remember(darkTheme) {
        if (!darkTheme) null
        else ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f,   0f
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp) // 加大模块之间的间距
    ) {

        // ==========================================
        // 模块 1：底层渲染引擎开关 (MIUIX Mode)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_miuix_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_support_miuix_mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(end = 16.dp)
                )
                Switch(
                    checked = miuixModeEnabled,
                    onCheckedChange = onMiuixModeChange
                )
            }
        }

        // ==========================================
        // 模块 2：布局风格选择
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.intro_theme_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.intro_theme_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            UiModeSelectionRow(
                currentStyle = currentStyle,
                onStyleChange = onStyleChange,
                invertColorFilter = invertColorFilter,
                isMiuixModeEnabled = miuixModeEnabled, // 动态传递 MIUIX 引擎状态
                inIntroPage = true,
            )
        }

        // ==========================================
        // 模块 3：强调色选择 (Theme Color Picker)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.theme),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 为了保持引导页的美观，给色盘加一个底色卡片包裹
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(vertical = 12.dp)
            ) {
                ThemeColorPicker(
                    currentSeed = selectedColorState,
                    onColorSelected = onThemeChange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 底部留白
    }
}