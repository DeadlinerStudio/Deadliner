package com.aritxonly.deadliner.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Stable
class AdvancedMaterialDialogBlurHostState {
    private val registeredBackdrops = mutableListOf<LayerBackdrop>()

    var modalBackdrop by mutableStateOf<LayerBackdrop?>(null)
        private set

    fun registerBackdrop(backdrop: LayerBackdrop) {
        registeredBackdrops += backdrop
        modalBackdrop = backdrop
    }

    fun unregisterBackdrop(backdrop: LayerBackdrop) {
        val index = registeredBackdrops.indexOfLast { it === backdrop }
        if (index >= 0) {
            registeredBackdrops.removeAt(index)
        }
        modalBackdrop = registeredBackdrops.lastOrNull()
    }
}

val LocalAdvancedMaterialDialogBlurHostState =
    compositionLocalOf<AdvancedMaterialDialogBlurHostState?> { null }

@Composable
fun rememberAdvancedMaterialDialogBlurHostState(): AdvancedMaterialDialogBlurHostState {
    return remember { AdvancedMaterialDialogBlurHostState() }
}

@Composable
fun ProvideAdvancedMaterialDialogBlurHost(
    content: @Composable () -> Unit
) {
    val inheritedState = LocalAdvancedMaterialDialogBlurHostState.current
    val ownedState = rememberAdvancedMaterialDialogBlurHostState()
    val state = inheritedState ?: ownedState
    CompositionLocalProvider(LocalAdvancedMaterialDialogBlurHostState provides state) {
        content()
    }
}

/** Makes an already captured page backdrop available to sibling dialogs and sheets. */
@Composable
fun RegisterAdvancedMaterialModalBackdrop(backdrop: LayerBackdrop?) {
    val state = LocalAdvancedMaterialDialogBlurHostState.current
    DisposableEffect(state, backdrop) {
        if (state != null && backdrop != null) {
            state.registerBackdrop(backdrop)
            onDispose { state.unregisterBackdrop(backdrop) }
        } else {
            onDispose { }
        }
    }
}
