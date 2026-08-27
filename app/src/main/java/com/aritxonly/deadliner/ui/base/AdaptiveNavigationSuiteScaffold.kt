package com.aritxonly.deadliner.ui.base

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarScrollProgress
import com.aritxonly.deadliner.ui.material.glass.ProgressiveTopBarMaterial
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingNavigationAction
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class AdaptiveNavItem(
    val key: String,
    val label: String,
    val selectedIcon: Painter,
    val unselectedIcon: Painter,
    val enabled: Boolean = true,
    val immersiveIcon: Painter? = null,
    val immersiveIconScale: Float = 1f,
)

//private val CompactNavigationShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val CompactNavigationShape = RoundedCornerShape(0.dp)
private val RailNavigationShape = RoundedCornerShape(12.dp)
private val CompactOverlayHostPadding = 92.dp
private val RailSlotWidth = 92.dp
private val NavigationIconFrameSize = 24.dp

@Composable
fun AdaptiveNavigationSuiteScaffold(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    useImmersiveExperience: Boolean = false,
    immersiveDetachedItemKey: String? = null,
    immersiveDetachedAction: MiuixFloatingNavigationAction? = null,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    legacyTopBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val designSystem = LocalAppDesignSystem.current
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
    val isShortBottomNavigation = navigationSuiteType == NavigationSuiteType.ShortNavigationBarCompact ||
        navigationSuiteType == NavigationSuiteType.ShortNavigationBarMedium
    val isCompactWidth = !adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )
    val shouldUseImmersiveExperience = useImmersiveExperience &&
        designSystem == AppDesignSystem.MIUIX &&
        isCompactWidth &&
        navigationSuiteType == NavigationSuiteType.ShortNavigationBarCompact
    val observeTopBarScroll = useImmersiveExperience && designSystem == AppDesignSystem.MIUIX
    val topBarScrollObserver = rememberTopBarMaterialScrollObserver(resetKey = selectedKey)
    val observedModifier = if (observeTopBarScroll) {
        modifier.nestedScroll(topBarScrollObserver.nestedScrollConnection)
    } else {
        modifier
    }
    val topBarScrollProgress = if (observeTopBarScroll) topBarScrollObserver.progress else null
    val hostedTopBar: @Composable () -> Unit = {
        ProgressiveTopBarMaterial(
            enabled = observeTopBarScroll,
            modifier = Modifier.fillMaxWidth(),
        ) {
            topBar()
        }
    }

    if (shouldUseImmersiveExperience) {
        val backdrop = if (advancedMaterial.enabled) rememberLayerBackdrop() else null
        CompositionLocalProvider(LocalTopBarScrollProgress provides topBarScrollProgress) {
            ImmersiveMiuixCompactNavigationScaffold(
                items = items,
                selectedKey = selectedKey,
                onItemSelected = onItemSelected,
                detachedItemKey = immersiveDetachedItemKey,
                detachedAction = immersiveDetachedAction,
                backdrop = backdrop,
                modifier = observedModifier,
                topBar = hostedTopBar,
                legacyTopBar = legacyTopBar,
                snackbarHost = snackbarHost,
                floatingActionButton = floatingActionButton,
                content = content,
            )
        }
        return
    }

    if (!isShortBottomNavigation) {
        val backdrop = if (
            useImmersiveExperience &&
            designSystem == AppDesignSystem.MIUIX &&
            advancedMaterial.enabled
        ) {
            rememberLayerBackdrop()
        } else {
            null
        }
        CompositionLocalProvider(LocalTopBarScrollProgress provides topBarScrollProgress) {
            LegacyWideNavigationScaffold(
                items = items,
                selectedKey = selectedKey,
                onItemSelected = onItemSelected,
                designSystem = designSystem,
                backdrop = backdrop,
                modifier = observedModifier,
                topBar = hostedTopBar,
                legacyTopBar = legacyTopBar,
                snackbarHost = snackbarHost,
                floatingActionButton = floatingActionButton,
                content = content,
            )
        }
        return
    }

    if (!advancedMaterial.enabled) {
        CompositionLocalProvider(LocalTopBarScrollProgress provides topBarScrollProgress) {
            LegacyCompactNavigationScaffold(
                items = items,
                selectedKey = selectedKey,
                onItemSelected = onItemSelected,
                designSystem = designSystem,
                useHorizontalItems = navigationSuiteType == NavigationSuiteType.ShortNavigationBarMedium,
                modifier = observedModifier,
                topBar = hostedTopBar,
                legacyTopBar = legacyTopBar,
                snackbarHost = snackbarHost,
                floatingActionButton = floatingActionButton,
                content = content,
            )
        }
        return
    }

    val backdrop = rememberLayerBackdrop()
    CompositionLocalProvider(LocalTopBarScrollProgress provides topBarScrollProgress) {
        AdvancedCompactNavigationScaffold(
            items = items,
            selectedKey = selectedKey,
            onItemSelected = onItemSelected,
            designSystem = designSystem,
            advancedMaterial = advancedMaterial,
            useHorizontalItems = navigationSuiteType == NavigationSuiteType.ShortNavigationBarMedium,
            backdrop = backdrop,
            modifier = observedModifier,
            topBar = hostedTopBar,
            legacyTopBar = legacyTopBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            content = content,
        )
    }
}

@Composable
private fun LegacyCompactNavigationScaffold(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    designSystem: AppDesignSystem,
    useHorizontalItems: Boolean,
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    legacyTopBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = legacyTopBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        bottomBar = {
            when (designSystem) {
                AppDesignSystem.MATERIAL3 -> Material3CompactBarContent(
                    items = items,
                    selectedKey = selectedKey,
                    onItemSelected = onItemSelected,
                    useHorizontalItems = useHorizontalItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                )

                AppDesignSystem.MIUIX -> MiuixNavigationBar(
                    color = MiuixTheme.colorScheme.surface,
                    showDivider = false,
                ) {
                    items.forEach { item ->
                        val selected = selectedKey == item.key
                        NavigationBarItem(
                            selected = selected,
                            enabled = item.enabled,
                            onClick = { onItemSelected(item) },
                            icon = {
                                AnimatedNavIcon(selected, item.selectedIcon, item.unselectedIcon, item.label)
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content(PaddingValues())
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                topBar()
            }
        }
    }
}

@Composable
private fun LegacyWideNavigationScaffold(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    designSystem: AppDesignSystem,
    backdrop: LayerBackdrop?,
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    legacyTopBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(top = 48.dp),
        ) {
            when (designSystem) {
                AppDesignSystem.MATERIAL3 -> NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    items.forEach { item ->
                        val selected = selectedKey == item.key
                        NavigationRailItem(
                            selected = selected,
                            enabled = item.enabled,
                            onClick = { onItemSelected(item) },
                            icon = {
                                AnimatedNavIcon(
                                    selected = selected,
                                    selectedIcon = item.selectedIcon,
                                    unselectedIcon = item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            colors = material3RailItemColors(),
                        )
                    }
                }

                AppDesignSystem.MIUIX -> MiuixNavigationRail(
                    color = MiuixTheme.colorScheme.surface,
                    showDivider = false,
                    mode = NavigationRailDisplayMode.IconAndText,
                ) {
                    items.forEach { item ->
                        val selected = selectedKey == item.key
                        NavigationRailItem(
                            selected = selected,
                            enabled = item.enabled,
                            onClick = { onItemSelected(item) },
                            icon = {
                                AnimatedNavIcon(selected, item.selectedIcon, item.unselectedIcon, item.label)
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            topBar = {
                if (backdrop != null) {
                    CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                        legacyTopBar()
                    }
                } else {
                    legacyTopBar()
                }
            },
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
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
            }
        }
    }
}

@Composable
private fun AdvancedCompactNavigationScaffold(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    designSystem: AppDesignSystem,
    advancedMaterial: AdvancedMaterialSpec,
    useHorizontalItems: Boolean,
    backdrop: LayerBackdrop,
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    legacyTopBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                legacyTopBar()
            }
        },
        snackbarHost = {
            Box(modifier = Modifier.padding(bottom = CompactOverlayHostPadding)) {
                snackbarHost()
            }
        },
        floatingActionButton = {
            Box(modifier = Modifier.padding(bottom = CompactOverlayHostPadding)) {
                floatingActionButton()
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
                    .layerBackdrop(backdrop),
            ) {
                content(PaddingValues())
            }
            CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    topBar()
                }
            }
            when (designSystem) {
                AppDesignSystem.MATERIAL3 -> Material3BottomNavigationBar(
                    items = items,
                    selectedKey = selectedKey,
                    onItemSelected = onItemSelected,
                    advancedMaterial = advancedMaterial,
                    useHorizontalItems = useHorizontalItems,
                    backdrop = backdrop,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                AppDesignSystem.MIUIX -> MiuixBottomNavigationBar(
                    items = items,
                    selectedKey = selectedKey,
                    onItemSelected = onItemSelected,
                    advancedMaterial = advancedMaterial,
                    backdrop = backdrop,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun Material3BottomNavigationBar(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    advancedMaterial: AdvancedMaterialSpec,
    useHorizontalItems: Boolean,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = advancedMaterial.navigationTintAlpha)
    val blurColors = advancedMaterial.rememberBlurColors(listOf(BlendColorEntry(tint)))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CompactNavigationShape)
            .advancedTextureBlur(
                advancedMaterial = advancedMaterial,
                backdrop = backdrop,
                shape = CompactNavigationShape,
                colors = blurColors,
            ),
    ) {
        Material3CompactBarContent(
            items = items,
            selectedKey = selectedKey,
            onItemSelected = onItemSelected,
            useHorizontalItems = useHorizontalItems,
            containerColor = Color.Transparent,
        )
    }
}

@Composable
private fun MiuixBottomNavigationBar(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    advancedMaterial: AdvancedMaterialSpec,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
) {
    val tint = MiuixTheme.colorScheme.surface.copy(alpha = advancedMaterial.navigationTintAlpha)
    val blurColors = advancedMaterial.rememberBlurColors(listOf(BlendColorEntry(tint)))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CompactNavigationShape)
            .advancedTextureBlur(
                advancedMaterial = advancedMaterial,
                backdrop = backdrop,
                shape = CompactNavigationShape,
                colors = blurColors,
            ),
    ) {
        MiuixNavigationBar(
            color = Color.Transparent,
            showDivider = false,
        ) {
            items.forEach { item ->
                val selected = selectedKey == item.key
                NavigationBarItem(
                    selected = selected,
                    enabled = item.enabled,
                    onClick = { onItemSelected(item) },
                    icon = {
                        AnimatedNavIcon(selected, item.selectedIcon, item.unselectedIcon, item.label)
                    },
                    label = { Text(item.label) },
                )
            }
        }
    }
}

@Composable
private fun Material3NavigationRailContainer(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    floatingActionButton: @Composable () -> Unit,
    advancedMaterial: AdvancedMaterialSpec,
    backdrop: LayerBackdrop,
) {
    val tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = advancedMaterial.navigationTintAlpha)
    val blurColors = advancedMaterial.rememberBlurColors(listOf(BlendColorEntry(tint)))

    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .width(RailSlotWidth)
            .fillMaxHeight()
            .clip(RailNavigationShape)
            .advancedTextureBlur(
                advancedMaterial = advancedMaterial,
                backdrop = backdrop,
                shape = RailNavigationShape,
                colors = blurColors,
            ),
    ) {
        NavigationRail(
            modifier = Modifier.fillMaxSize(),
            header = { floatingActionButton() },
            containerColor = Color.Transparent,
            windowInsets = NavigationRailDefaults.windowInsets,
        ) {
            items.forEach { item ->
                val selected = selectedKey == item.key
                NavigationRailItem(
                    selected = selected,
                    enabled = item.enabled,
                    onClick = { onItemSelected(item) },
                    icon = {
                        AnimatedNavIcon(
                            selected = selected,
                            selectedIcon = item.selectedIcon,
                            unselectedIcon = item.unselectedIcon,
                            contentDescription = item.label,
                        )
                    },
                    label = { Text(item.label) },
                    colors = material3RailItemColors(),
                )
            }
        }
    }
}

@Composable
private fun MiuixNavigationRailContainer(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    floatingActionButton: @Composable () -> Unit,
    advancedMaterial: AdvancedMaterialSpec,
    backdrop: LayerBackdrop,
) {
    val tint = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = advancedMaterial.navigationTintAlpha)
    val blurColors = advancedMaterial.rememberBlurColors(listOf(BlendColorEntry(tint)))

    Box(
        modifier = Modifier
            .padding(12.dp)
            .width(RailSlotWidth)
            .fillMaxHeight()
            .clip(RailNavigationShape)
            .advancedTextureBlur(
                advancedMaterial = advancedMaterial,
                backdrop = backdrop,
                shape = RailNavigationShape,
                colors = blurColors,
            ),
    ) {
        MiuixNavigationRail(
            modifier = Modifier.fillMaxSize(),
            header = { floatingActionButton() },
            color = Color.Transparent,
            showDivider = false,
            mode = NavigationRailDisplayMode.IconAndText,
        ) {
            items.forEach { item ->
                val selected = selectedKey == item.key
                NavigationRailItem(
                    selected = selected,
                    enabled = item.enabled,
                    onClick = { onItemSelected(item) },
                    icon = {
                        AnimatedNavIcon(selected, item.selectedIcon, item.unselectedIcon, item.label)
                    },
                    label = { Text(item.label) },
                )
            }
        }
    }
}

@Composable
private fun Material3CompactBarContent(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    useHorizontalItems: Boolean,
    containerColor: Color,
) {
    val itemColors = ShortNavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.secondary,
        selectedTextColor = MaterialTheme.colorScheme.secondary,
        selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    ShortNavigationBar(
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = ShortNavigationBarDefaults.windowInsets,
        arrangement = if (useHorizontalItems) {
            ShortNavigationBarArrangement.Centered
        } else {
            ShortNavigationBarArrangement.EqualWeight
        },
    ) {
        items.forEach { item ->
            val selected = selectedKey == item.key
            ShortNavigationBarItem(
                selected = selected,
                enabled = item.enabled,
                onClick = { onItemSelected(item) },
                icon = {
                    AnimatedNavIcon(
                        selected = selected,
                        selectedIcon = item.selectedIcon,
                        unselectedIcon = item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
                iconPosition = if (useHorizontalItems) {
                    NavigationItemIconPosition.Start
                } else {
                    NavigationItemIconPosition.Top
                },
                colors = itemColors,
            )
        }
    }
}

@Composable
private fun AnimatedNavIcon(
    selected: Boolean,
    selectedIcon: Painter,
    unselectedIcon: Painter,
    contentDescription: String,
) {
    AnimatedContent(
        targetState = selected,
        transitionSpec = {
            (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.88f, animationSpec = tween(220)))
                .togetherWith(
                    fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 1.04f, animationSpec = tween(120)),
                )
        },
        label = "nav_icon_switch",
    ) { isSelected ->
        Icon(
            painter = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = contentDescription,
            modifier = Modifier.size(NavigationIconFrameSize),
        )
    }
}

@Composable
private fun material3RailItemColors() = NavigationRailItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.secondary,
    selectedTextColor = MaterialTheme.colorScheme.secondary,
    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
