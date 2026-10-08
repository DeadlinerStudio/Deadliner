package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.aritxonly.deadliner.ui.navigation.*
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun TabletNavigationScaffold(
    items: List<AdaptiveNavItem>, selectedKey: String, onItemSelected: (AdaptiveNavItem) -> Unit,
    backdrop: LayerBackdrop?, modifier: Modifier,
    topBar: @Composable () -> Unit, legacyTopBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit, floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.screenHeightDp > configuration.screenWidthDp
    var expanded by rememberSaveable { mutableStateOf(!isPortrait) }
    val progress = rememberTabletNavExpandProgress(expanded)
    val expandedWidth = configuration.screenWidthDp.dp * TabletNavSideWidthFraction - TabletNavSideInset
    val contentInset = Modifier.layout { measurable, constraints ->
        val inset = (TabletNavSideInset + if (isPortrait) TabletNavIconRailWidth
            else lerp(TabletNavIconRailWidth, expandedWidth, progress.value)).roundToPx()
            .coerceAtMost(constraints.maxWidth)
        val child = measurable.measure(constraints.offset(horizontal = -inset))
        layout(child.width + inset, child.height) { child.placeRelative(inset, 0) }
    }
    RegisterAdvancedMaterialModalBackdrop(backdrop)
    CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
        // Capture content across the entire window; sidebar and top material are sibling consumers.
        Box(modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
                Box(Modifier.fillMaxSize().then(contentInset)) {
                    Scaffold(topBar = legacyTopBar, snackbarHost = snackbarHost,
                        floatingActionButton = floatingActionButton, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)
                            .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                            .background(MiuixTheme.colorScheme.surface)) {
                            content(PaddingValues())
                        }
                    }
                }
            }
            Box(Modifier.fillMaxSize().then(contentInset)) {
                Box(Modifier.align(Alignment.TopCenter)) { topBar() }
                NavigationOverlay(backdrop, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp))
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
