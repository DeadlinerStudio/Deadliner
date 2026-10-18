package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Density
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/** A whole Compose window recorder, sampled only by surfaces in a different window. */
@Stable
internal class WindowGlassBackdrop(val source: LayerBackdrop) : Backdrop {
    var consumers by mutableIntStateOf(0)
    var origin by mutableStateOf(Offset.Zero)
    override val isCoordinatesDependent = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        if (coordinates == null || !coordinates.isAttached) return
        // LayerBackdrop's cross-owner fallback uses window-local coordinates. An anchored
        // popup can have a different window origin, so resolve both in screen coordinates.
        // This consumer has no backdrop layerBlock; its entrance transform is on its parent.
        val offset = coordinates.positionOnScreen() - origin
        val inverse = 1f / downscaleFactor.coerceAtLeast(1)
        withTransform({
            translate(-offset.x * inverse, -offset.y * inverse)
            scale(inverse, inverse, Offset.Zero)
        }) { drawLayer(source.graphicsLayer) }
    }
}

internal val LocalWindowGlassBackdrop = staticCompositionLocalOf<WindowGlassBackdrop?> { null }

@Composable
internal fun WindowGlassBackdropHost(content: @Composable () -> Unit) {
    if (LocalWindowGlassBackdrop.current != null) {
        content()
        return
    }
    val layer = rememberLayerBackdrop()
    val backdrop = remember(layer) { WindowGlassBackdrop(layer) }
    CompositionLocalProvider(LocalWindowGlassBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()
            .onGloballyPositioned { backdrop.origin = it.positionOnScreen() }
            .then(if (backdrop.consumers > 0) Modifier.layerBackdrop(layer) else Modifier)) {
            content()
        }
    }
}
