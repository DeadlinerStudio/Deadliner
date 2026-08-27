package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingNavigationDock
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingNavigationAction
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingTabBarDefaults
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingTabItem
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val FloatingCompactOverlayHostPadding = 92.dp
private val FloatingNavigationBottomSpacing = 4.dp
private val FloatingNavigationEdgeMaskExtraHeight = 32.dp
private val FloatingFabNavigationGap = 20.dp

@Composable
internal fun ImmersiveMiuixCompactNavigationScaffold(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    detachedItemKey: String?,
    detachedAction: MiuixFloatingNavigationAction?,
    backdrop: LayerBackdrop?,
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    legacyTopBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val navigationPartition = partitionImmersiveNavigationItems(items, detachedItemKey)
    val primarySelectedKey = resolveImmersivePrimarySelectedKey(
        items = navigationPartition.primaryItems,
        selectedKey = selectedKey,
    )
    val primarySelectionVisible = isImmersivePrimarySelectionVisible(
        items = navigationPartition.primaryItems,
        selectedKey = selectedKey,
    )
    Scaffold(
        modifier = modifier,
        topBar = {
            if (backdrop != null) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    legacyTopBar()
                }
            } else {
                legacyTopBar()
            }
        },
        snackbarHost = {
            Box(modifier = Modifier.padding(bottom = FloatingCompactOverlayHostPadding)) {
                snackbarHost()
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .let { contentModifier ->
                        if (backdrop != null) {
                            contentModifier.layerBackdrop(backdrop)
                        } else {
                            contentModifier
                        }
                    },
            ) {
                content(PaddingValues())
            }

            if (backdrop != null) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    Box(modifier = Modifier.align(Alignment.TopCenter)) {
                        topBar()
                    }
                }
            } else {
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    topBar()
                }
            }

            FloatingNavigationEdgeMask(
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            val floatingFab: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            bottom = MiuixFloatingTabBarDefaults.Height +
                                FloatingNavigationBottomSpacing +
                                FloatingFabNavigationGap,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    floatingActionButton()
                }
            }
            if (backdrop != null) {
                CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                    floatingFab()
                }
            } else {
                floatingFab()
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = 16.dp,
                        top = 12.dp,
                        end = 16.dp,
                        bottom = FloatingNavigationBottomSpacing,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                MiuixFloatingNavigationDock(
                    items = navigationPartition.primaryItems.map { it.toFloatingTabItem() },
                    detachedItem = navigationPartition.detachedItem?.toFloatingTabItem(),
                    detachedAction = detachedAction,
                    primarySelectionVisible = primarySelectionVisible,
                    selectedKey = if (navigationPartition.detachedItem?.key == selectedKey) {
                        selectedKey
                    } else {
                        primarySelectedKey
                    },
                    onItemSelected = { selectedItem ->
                        items.firstOrNull { it.key == selectedItem.key }?.let(onItemSelected)
                    },
                    backdrop = backdrop,
                )
            }
        }
    }
}

private fun AdaptiveNavItem.toFloatingTabItem() = MiuixFloatingTabItem(
    key = key,
    label = label,
    selectedIcon = resolveImmersiveNavigationIcon(this),
    unselectedIcon = resolveImmersiveNavigationIcon(this),
    enabled = enabled,
    iconScale = immersiveIconScale,
)

@Composable
private fun FloatingNavigationEdgeMask(
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val navigationBarHeight = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val coverageHeight = MiuixFloatingTabBarDefaults.Height +
        FloatingNavigationBottomSpacing +
        FloatingNavigationEdgeMaskExtraHeight +
        navigationBarHeight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(coverageHeight)
            .edgeFade(top = coverageHeight, bottom = 0.dp)
            .background(MiuixTheme.colorScheme.surface),
    )
}
