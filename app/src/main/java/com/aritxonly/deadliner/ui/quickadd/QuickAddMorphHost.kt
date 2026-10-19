package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.layout.positionInWindow

internal data class QuickAddMorphTransition(val progress: State<Float>, val active: Boolean)

@Composable
internal fun quickAddMorphTransition(visible: Boolean): QuickAddMorphTransition {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = visible
    val transition = updateTransition(visibility, label = "quickAddLens")
    val progress = transition.animateFloat(transitionSpec = {
        spring(dampingRatio = 0.78f, stiffness = 380f)
    }, label = "boundsProgress") { if (it) 1f else 0f }
    val active by remember(transition, visibility) {
        derivedStateOf { visibility.currentState || visibility.targetState || transition.isRunning }
    }
    // Pass the clock, not its current value: the overlay should only recompose on mount/unmount.
    return QuickAddMorphTransition(progress, active)
}

@Composable
internal fun QuickAddMorphHost(state: QuickAddState, progress: State<Float>, source: Rect,
                               placement: QuickAddPlacement, onSaved: () -> Unit, onExpand: () -> Unit) {
    val panelBottom = animateFloatAsState(placement.panelBottomInset, spring(stiffness = 500f), label = "lensKeyboardInset")
    val autofocus by remember(state, progress) { derivedStateOf { state.visible && progress.value >= 0.98f } }
    val coordinates = remember { QuickAddPanelCoordinates() }
    val expand = {
        // Layer-only motion need not dispatch onGloballyPositioned. Resolve the current screen
        // bounds at the tap, so the next Sheet starts at the visible card even mid-animation.
        coordinates.value?.takeIf { it.isAttached }?.let {
            state.panelBounds = it.boundsInWindow().translate(it.positionOnScreen() - it.positionInWindow())
        }
        onExpand()
    }
    QuickAddMorphLayout(progress, source, placement.rightInset, panelBottom,
        surface = { localSource -> QuickAddMorphSurface(progress, localSource) }) {
        QuickAddContent(state, onSaved, showClose = false, onExpand = expand,
            modifier = Modifier.onGloballyPositioned {
                coordinates.value = it
                state.panelBounds = it.boundsInWindow().translate(it.positionOnScreen() - it.positionInWindow())
            }, autofocus = autofocus)
    }
}

private class QuickAddPanelCoordinates {
    var value: LayoutCoordinates? = null
}
