package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.annotation.DrawableRes
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.window.core.layout.WindowSizeClass
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarScrollProgress
import com.aritxonly.deadliner.ui.material.glass.ProgressiveTopBarMaterial
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingNavigationAction
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

data class AdaptiveNavItem(
    val key: String,
    val label: String,
    val selectedIcon: Painter,
    val unselectedIcon: Painter,
    val enabled: Boolean = true,
    val immersiveIcon: Painter? = null,
    @DrawableRes val immersiveIconResource: Int? = null,
    val immersiveIconScale: Float = 1f,
)

@Composable
fun AdaptiveNavigationSuiteScaffold(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
    onItemSelected: (AdaptiveNavItem) -> Unit,
    topBarMaterialScrollKey: Any? = Unit,
    immersiveDetachedItemKey: String? = null,
    immersiveDetachedAction: MiuixFloatingNavigationAction? = null,
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    legacyTopBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    overlayContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompactWidth = !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isWideWidth = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val topBarScrollObserver = rememberTopBarMaterialScrollObserver(
        scrollKey = listOf(selectedKey, topBarMaterialScrollKey),
    )
    val observedModifier = modifier.nestedScroll(topBarScrollObserver.nestedScrollConnection)
    val hostedTopBar: @Composable () -> Unit = {
        ProgressiveTopBarMaterial(modifier = Modifier.fillMaxWidth()) { topBar() }
    }
    val backdrop = if (advancedMaterial.enabled) rememberLayerBackdrop() else null
    RegisterAdvancedMaterialModalBackdrop(backdrop)
    CompositionLocalProvider(
        LocalTopBarScrollProgress provides topBarScrollObserver.progress,
        LocalTopBarMaterialScrollObserver provides topBarScrollObserver,
    ) {
        Box {
            if (isWideWidth) {
                TabletNavigationScaffold(
                    items = items, selectedKey = selectedKey, onItemSelected = onItemSelected,
                    backdrop = backdrop, modifier = observedModifier, topBar = topBar,
                    background = background,
                    legacyTopBar = legacyTopBar, snackbarHost = snackbarHost,
                    floatingActionButton = floatingActionButton, content = content,
                )
            } else {
                ImmersiveMiuixCompactNavigationScaffold(
                    items = items, selectedKey = selectedKey, onItemSelected = onItemSelected,
                    detachedItemKey = immersiveDetachedItemKey, detachedAction = immersiveDetachedAction,
                    backdrop = backdrop, horizontalItems = !isCompactWidth,
                    modifier = observedModifier, topBar = hostedTopBar,
                    background = background,
                    legacyTopBar = legacyTopBar, snackbarHost = snackbarHost,
                    floatingActionButton = floatingActionButton, content = content,
                )
            }
            CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) { overlayContent() }
        }
    }
}
