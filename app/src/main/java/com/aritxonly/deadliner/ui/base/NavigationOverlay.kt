package com.aritxonly.deadliner.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop

val LocalNavigationOverlayContentPadding = staticCompositionLocalOf<Dp> { 0.dp }

/** Chrome rendered outside the page recorder, sharing the navigation backdrop. */
val LocalNavigationOverlay = staticCompositionLocalOf<@Composable (Modifier) -> Unit> { {} }

@Composable
internal fun NavigationOverlay(backdrop: LayerBackdrop?, modifier: Modifier) {
    CompositionLocalProvider(LocalAdvancedMaterialBackdrop provides backdrop) {
        LocalNavigationOverlay.current(modifier)
    }
}
