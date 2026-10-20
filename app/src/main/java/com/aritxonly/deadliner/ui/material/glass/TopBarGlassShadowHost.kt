package com.aritxonly.deadliner.ui.material.glass

import com.aritxonly.deadliner.model.GlassShadowPreset
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

internal data class TopBarGlassShadow(
    val bounds: Rect,
    val shape: Shape,
    val isDark: Boolean,
    val alpha: Float,
    val official: Boolean,
    val profile: SoftGlassShadowProfile,
    val preset: GlassShadowPreset,
)

@Stable
internal class TopBarGlassShadowState(val owner: View) {
    var origin by mutableStateOf<Offset?>(null)
    val shadows = mutableStateMapOf<Any, TopBarGlassShadow>()
}

internal val LocalTopBarGlassShadowHost = staticCompositionLocalOf<TopBarGlassShadowState?> { null }

/** Own elevation at the outermost header foreground, above the progressive backdrop. */
@Composable
internal fun TopBarGlassShadowHost(content: @Composable () -> Unit) {
    val owner = LocalView.current
    // A nested common TopAppBar must not pull shadows back inside AnimatedContent/native clips.
    if (LocalTopBarGlassShadowHost.current?.owner === owner) {
        content()
        return
    }
    val state = remember(owner) { TopBarGlassShadowState(owner) }
    val density = LocalDensity.current
    Box(Modifier.onGloballyPositioned { state.origin = it.positionInRoot() }) {
        // These noninteractive siblings do not affect bar measurement or touch targets.
        Box(Modifier.matchParentSize()) {
            val origin = state.origin
            if (origin != null) for ((id, shadow) in state.shadows.toMap()) key(id) {
                if (shadow.alpha > 0f && shadow.bounds.width > 0f && shadow.bounds.height > 0f) {
                    val offset = shadow.bounds.topLeft - origin
                    Box(Modifier
                        .absoluteOffset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                        .size(with(density) { shadow.bounds.width.toDp() }, with(density) { shadow.bounds.height.toDp() })
                        .drawSoftGlassShadow(shadow.shape, shadow.isDark, shadow.alpha, shadow.official, shadow.profile, shadow.preset))
                }
            }
        }
        CompositionLocalProvider(LocalTopBarGlassShadowHost provides state, content = content)
    }
}

@Composable
internal fun Modifier.registerTopBarGlassShadow(
    host: TopBarGlassShadowState,
    shape: Shape,
    isDark: Boolean,
    alpha: Float,
    official: Boolean,
    profile: SoftGlassShadowProfile,
    preset: GlassShadowPreset,
): Modifier {
    val id = remember(host) { Any() }
    var bounds by remember(host) { mutableStateOf<Rect?>(null) }
    val measuredBounds = bounds
    SideEffect {
        measuredBounds?.let { host.shadows[id] = TopBarGlassShadow(it, shape, isDark, alpha.coerceIn(0f, 1f), official, profile, preset) }
    }
    DisposableEffect(host, id) { onDispose { host.shadows.remove(id) } }
    return onGloballyPositioned { coordinates ->
        // Unlike boundsInRoot(), this keeps the complete lens when an ancestor clips it.
        val topLeft = coordinates.positionInRoot()
        bounds = Rect(topLeft.x, topLeft.y,
            topLeft.x + coordinates.size.width, topLeft.y + coordinates.size.height)
    }
}
