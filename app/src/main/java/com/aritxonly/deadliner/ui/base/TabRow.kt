package com.aritxonly.deadliner.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.aritxonly.deadliner.ui.navigation.ImmersiveTextTabRow

/**
 * Deadliner 基础 TabRow 组件
 * 采用数据驱动的 API 设计，抹平 M3 插槽和 MIUIX 列表的差异
 */
@Composable
fun TabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // 保留以兼容已有调用；MIUIX 文本 Tab 不展示图标。
    tabIcons: List<Painter>? = null,
    // 保留以兼容已有调用；分割线由 MIUIX 容器管理。
    divider: @Composable () -> Unit = {}
) {
    ImmersiveTextTabRow(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}
