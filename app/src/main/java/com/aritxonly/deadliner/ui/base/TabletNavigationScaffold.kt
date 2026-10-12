package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.aritxonly.deadliner.ui.material.glass.ProgressiveTopBarMaterial
import com.aritxonly.deadliner.ui.navigation.*
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
internal fun TabletNavigationScaffold(
    items: List<AdaptiveNavItem>, selectedKey: String, onItemSelected: (AdaptiveNavItem) -> Unit,
    backdrop: LayerBackdrop?, modifier: Modifier,
    background: @Composable () -> Unit,
    topBar: @Composable () -> Unit, legacyTopBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit, floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.screenHeightDp > configuration.screenWidthDp
    var expanded by rememberSaveable { mutableStateOf(!isPortrait) }
    val progress = rememberTabletNavExpandProgress(expanded)
    val expandedWidth = configuration.screenWidthDp.dp * TabletNavSideWidthFraction - TabletNavSideInset
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1f else -1f
    val targetInset = TabletNavSideInset + if (isPortrait || !expanded) TabletNavIconRailWidth else expandedWidth
    // Match upstream: measure the content once per toggle, animate only its translation.
    val contentInset = Modifier.layout { measurable, constraints ->
        val inset = targetInset.roundToPx().coerceAtMost(constraints.maxWidth)
        val child = measurable.measure(constraints.offset(horizontal = -inset))
        layout(child.width + inset, child.height) { child.placeRelative(inset, 0) }
    }.graphicsLayer {
        val currentInset = TabletNavSideInset + if (isPortrait) TabletNavIconRailWidth
            else lerp(TabletNavIconRailWidth, expandedWidth, progress.value)
        translationX = (currentInset - targetInset).toPx() * direction
    }
    CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
        Scaffold(modifier = modifier, topBar = legacyTopBar, snackbarHost = snackbarHost,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                // Full-window atmosphere and page content only. All sampled chrome is a sibling.
                Box(Modifier.fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)) {
                    background()
                    Box(Modifier.fillMaxSize().then(contentInset)) { content(PaddingValues()) }
                }
                // The blur covers the full window; only the foreground avoids the sidebar.
                Box(Modifier.align(Alignment.TopCenter)) {
                    ProgressiveTopBarMaterial(modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().then(contentInset)) { topBar() }
                    }
                }
                Box(Modifier.fillMaxSize().then(contentInset)) {
                    NavigationOverlay(backdrop, Modifier.align(Alignment.BottomCenter)
                        .navigationBarsPadding().padding(bottom = 12.dp))
                }
                // Share the existing quick-add glass toggle; keep it out of the recorded page.
                // Navigation bars only: opening the IME moves the lens, not the close action.
                Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp)) {
                    floatingActionButton()
                }
                TabletNavSideBar(
                    items = items.map { it.toFloatingTabItem() }, selectedKey = selectedKey,
                    onItemSelected = { selected -> items.firstOrNull { it.key == selected.key }?.let(onItemSelected) },
                    expanded = expanded, onExpandedChange = { expanded = it }, expandProgress = progress,
                    expandedWidth = expandedWidth, isPortrait = isPortrait, backdrop = backdrop,
                )
            }
        }
    }
}
