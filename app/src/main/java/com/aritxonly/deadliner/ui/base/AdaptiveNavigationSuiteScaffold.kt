package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold as Material3Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem

/**
 * 主导航项定义。
 */
data class AdaptiveNavItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * 主界面自适应导航脚手架：
 * - Material3 / 平板等场景：使用 NavigationSuiteScaffold（支持底栏/侧栏自适应）
 * - MIUIX + 紧凑窗口：使用 MIUIX NavigationBar
 */
@Composable
fun AdaptiveNavigationSuiteScaffold(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val designSystem = LocalAppDesignSystem.current
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)

    val isShortBottomNavigation = navigationSuiteType == NavigationSuiteType.ShortNavigationBarCompact ||
        navigationSuiteType == NavigationSuiteType.ShortNavigationBarMedium

    if (designSystem == AppDesignSystem.MIUIX) {
        // MIUIX 紧凑窗口：底部导航栏
        if (isShortBottomNavigation) {
            Scaffold(
                modifier = modifier,
                topBar = topBar,
                snackbarHost = snackbarHost,
                floatingActionButton = floatingActionButton,
                bottomBar = {
                    MiuixNavigationBar {
                        items.forEach { item ->
                            MiuixNavigationBarItem(
                                selected = selectedKey == item.key,
                                onClick = { onItemSelected(item) },
                                icon = item.icon,
                                label = item.label,
                            )
                        }
                    }
                },
                content = content,
            )
            return
        }

        // MIUIX 平板/宽屏：侧边 NavigationRail
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            snackbarHost = snackbarHost,
            floatingActionButton = {},
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                MiuixNavigationRail(
                    header = { floatingActionButton() },
                    mode = NavigationRailDisplayMode.IconAndText,
                ) {
                    items.forEach { item ->
                        MiuixNavigationRailItem(
                            selected = selectedKey == item.key,
                            onClick = { onItemSelected(item) },
                            icon = item.icon,
                            label = item.label,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    content(PaddingValues())
                }
            }
        }
        return
    }

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteType = navigationSuiteType,
        navigationItems = {
            items.forEach { item ->
                NavigationSuiteItem(
                    selected = selectedKey == item.key,
                    onClick = { onItemSelected(item) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                        )
                    },
                    label = {
                        Text(item.label)
                    },
                )
            }
        },
    ) {
        Material3Scaffold(
            topBar = topBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            containerColor = MaterialTheme.colorScheme.surface,
            content = content,
        )
    }
}
