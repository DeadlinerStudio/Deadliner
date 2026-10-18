package com.aritxonly.deadliner.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Cross-Activity shared container: move content with the surface without distorting its aspect ratio. */
@Composable
internal fun TaskDetailMotion(
    source: FloatArray?,
    sourceSnapshot: TaskDetailSnapshot?,
    sourceCornerRadiusPx: Float,
    closing: Boolean,
    ready: Boolean,
    animateEntrance: Boolean,
    onClose: () -> Unit,
    onClosed: () -> Unit,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(if (animateEntrance) 0f else 1f) }
    // Geometry stops early on entry, but must start moving immediately on return.
    // Separate clocks also preserve both positions if Back interrupts the entrance.
    val geometry = remember {
        Animatable(if (animateEntrance) 0f else TaskDetailMotionSpec.ContainerExpandEnd)
    }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    val initialSource = remember { source != null }
    val rect = remember(source, viewport) { TaskDetailMotionSpec.sourceRect(source, viewport) }
    val timing = remember(rect, viewport.height) {
        TaskDetailMotionSpec.timing(rect?.height, viewport.height)
    }
    val hasSource = rect != null
    val surface = MaterialTheme.colorScheme.surface

    LaunchedEffect(closing, ready, viewport, hasSource) {
        if (viewport.width <= 0 || viewport.height <= 0 || (!ready && !closing)) return@LaunchedEffect
        // A rotation or split resize invalidates the old source, without replaying the entrance.
        if (initialSource && source == null && !closing) {
            progress.snapTo(1f)
            geometry.snapTo(TaskDetailMotionSpec.ContainerExpandEnd)
        }
        val duration = when {
            !hasSource -> TaskDetailMotionSpec.FallbackDuration
            closing -> TaskDetailMotionSpec.ExitDuration
            else -> TaskDetailMotionSpec.EnterDuration
        }
        coroutineScope {
            launch { progress.animateTo(if (closing) 0f else 1f, tween(duration, easing = LinearEasing)) }
            launch {
                geometry.animateTo(
                    if (closing) 0f else TaskDetailMotionSpec.ContainerExpandEnd,
                    tween(if (closing) duration else TaskDetailMotionSpec.ExpandDuration, easing = LinearEasing),
                )
            }
        }
        if (closing) onClosed()
    }
    BackHandler(onBack = onClose)
    val interactive = !closing && progress.value == 1f && ready
    Box(Modifier.fillMaxSize().onGloballyPositioned { viewport = it.detailBoundsOnScreen() }) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.12f * progress.value))
        }
        Box(Modifier.fillMaxSize().graphicsLayer {
            val p = geometry.value
            val bounds = TaskDetailMotionSpec.bounds(rect, size.width, size.height, p, timing)
            val radius = sourceCornerRadiusPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
            shape = DetailContainerClip(bounds, radius * (1f - TaskDetailMotionSpec.cornerProgress(p, timing)))
            clip = true
            // Keep a valid source covered until the final handoff. Fading the whole surface
            // exposed the original row beneath the moving snapshot, creating double text.
            alpha = if (hasSource) 1f else progress.value
        }) {
            Canvas(Modifier.fillMaxSize()) {
                val p = geometry.value
                val appearance = sourceSnapshot?.appearance.takeIf { hasSource }
                drawRect(if (appearance == null) surface else
                    lerp(appearance.background, surface, TaskDetailMotionSpec.backgroundBlend(p, timing)))
                if (appearance != null && appearance.progress > 0f) {
                    val bounds = TaskDetailMotionSpec.bounds(rect, size.width, size.height, p, timing)
                    val color = appearance.indicator
                    // Stretch the progress with the card body, separately from its foreground.
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = color.alpha * 0.45f), color),
                            startX = bounds.left,
                            endX = bounds.left + bounds.width * appearance.progress,
                        ),
                        topLeft = bounds.topLeft,
                        size = Size(bounds.width * appearance.progress, bounds.height),
                        alpha = TaskDetailMotionSpec.progressAlpha(p, timing),
                    )
                }
            }
            Box(Modifier.fillMaxSize()
                .then(if (interactive) Modifier else Modifier.clearAndSetSemantics {})
                .graphicsLayer {
                    val p = geometry.value
                    val bounds = TaskDetailMotionSpec.bounds(rect, size.width, size.height, p, timing)
                    alpha = TaskDetailMotionSpec.detailAlpha(if (hasSource) p else progress.value, hasSource, timing)
                    // Content belongs to the moving surface, rather than the final screen
                    // behind a moving stencil. Scale uniformly by width, never by height.
                    val lift = (if (hasSource) 24f else 12f) * density *
                        (1f - TaskDetailMotionSpec.contentFloat(progress.value, hasSource, timing))
                    val placement = TaskDetailMotionSpec.contentPlacement(bounds, size.width, lift)
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = placement.scale
                    scaleY = placement.scale
                    translationX = placement.offset.x
                    // The page is already open before this independent lift reaches its midpoint.
                    translationY = placement.offset.y
                }) { content() }
            val foreground = sourceSnapshot?.foreground
            if (foreground != null && rect != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val p = geometry.value
                    val bounds = TaskDetailMotionSpec.bounds(rect, size.width, size.height, p, timing)
                    // Preserve the snapshot's natural pixel size; only its container expands.
                    drawImage(foreground, topLeft = bounds.topLeft,
                        alpha = TaskDetailMotionSpec.foregroundAlpha(p, timing))
                }
            }
        }
        if (!interactive) {
            Box(Modifier.fillMaxSize().clearAndSetSemantics {}.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            })
        }
    }
}

private class DetailContainerClip(private val bounds: Rect, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(bounds, CornerRadius(radius)))
}
