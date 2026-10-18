package com.aritxonly.deadliner.ui.detail

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import kotlin.math.max

/** Normalized clock positions, resolved once from source/target heights rather than guessed. */
internal data class TaskDetailMotionTiming(
    val quarterHeight: Float,
    val halfHeight: Float,
    val backgroundEnd: Float,
    val contentStart: Float,
    val detailStart: Float,
)

internal data class TaskDetailContentPlacement(val scale: Float, val offset: Offset)

internal object TaskDetailMotionSpec {
    const val EnterDuration = 480
    const val ExitDuration = 440
    const val FallbackDuration = 220
    const val ContainerExpandEnd = 0.64f
    const val ExpandDuration = EnterDuration * 64 / 100
    // A gentler vertical curve gives the quarter-to-half-height fade room to breathe.
    private val VerticalEasing = CubicBezierEasing(0.3f, 0f, 0.7f, 1f)
    private val HorizontalEasing = CubicBezierEasing(0.3f, 0f, 0.3f, 1f)
    private val FloatEasing = CubicBezierEasing(0.3f, 0f, 0.7f, 1f)
    private val RevealEasing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)

    fun timing(sourceHeight: Float?, targetHeight: Float): TaskDetailMotionTiming {
        val start = if (targetHeight.isFinite() && targetHeight > 0f) {
            (sourceHeight ?: targetHeight) / targetHeight
        } else 1f
        val quarter = timeAtHeight(start, 0.25f)
        val half = timeAtHeight(start, 0.50f)
        val backgroundEnd = timeAtHeight(start, 0.72f)
        // Start handing off while the background changes, instead of exposing an empty panel.
        val detailStart = half + 0.01f
        // Keep the page settled before half of the content lift, including unusually tall cards.
        val contentStart = max(detailStart, 2f * ContainerExpandEnd - 1f + 0.02f)
        return TaskDetailMotionTiming(quarter, half, backgroundEnd, contentStart, detailStart)
    }

    /** Invert the known vertical curve only when geometry changes, never during a draw. */
    private fun timeAtHeight(start: Float, heightFraction: Float): Float {
        if (!start.isFinite() || start >= heightFraction || start >= 1f) return 0f
        val expansion = ((heightFraction - start) / (1f - start)).coerceIn(0f, 1f)
        var low = 0f
        var high = 1f
        repeat(24) {
            val mid = (low + high) / 2f
            if (VerticalEasing.transform(mid) < expansion) low = mid else high = mid
        }
        return (low + high) / 2f * ContainerExpandEnd
    }

    fun sourceRect(values: FloatArray?, viewport: Rect): Rect? {
        if (values == null || values.size != 4 || values.any { !it.isFinite() }) return null
        val rect = Rect(values[0], values[1], values[2], values[3])
        if (!rect.width.isFinite() || !rect.height.isFinite() || rect.width <= 0f || rect.height <= 0f ||
            rect.width > viewport.width * 2f || rect.height > viewport.height * 2f ||
            !rect.overlaps(viewport)) return null
        return rect.translate(-viewport.left, -viewport.top)
    }

    fun bounds(source: Rect?, width: Float, height: Float, progress: Float, timing: TaskDetailMotionTiming): Rect {
        if (source == null) return Rect(0f, 0f, width, height)
        val vertical = VerticalEasing.transform(phase(progress, 0f, ContainerExpandEnd))
        val horizontal = cornerProgress(progress, timing)
        return Rect(
            source.left * (1f - horizontal),
            source.top * (1f - vertical),
            source.right + (width - source.right) * horizontal,
            source.bottom + (height - source.bottom) * vertical,
        )
    }

    fun cornerProgress(progress: Float, timing: TaskDetailMotionTiming): Float =
        HorizontalEasing.transform(phase(progress, timing.halfHeight, ContainerExpandEnd))

    fun contentPlacement(bounds: Rect, viewportWidth: Float, lift: Float): TaskDetailContentPlacement =
        TaskDetailContentPlacement(
            scale = if (viewportWidth > 0f) bounds.width / viewportWidth else 1f,
            offset = bounds.topLeft + Offset(0f, lift),
        )

    fun foregroundAlpha(progress: Float, timing: TaskDetailMotionTiming): Float =
        1f - fade(progress, timing.quarterHeight * 0.12f, timing.halfHeight)

    fun progressAlpha(progress: Float, timing: TaskDetailMotionTiming): Float =
        foregroundAlpha(progress, timing)

    fun backgroundBlend(progress: Float, timing: TaskDetailMotionTiming): Float =
        fade(progress, timing.halfHeight, timing.backgroundEnd)

    fun detailAlpha(progress: Float, hasSource: Boolean, timing: TaskDetailMotionTiming): Float =
        if (hasSource) RevealEasing.transform(phase(progress, timing.detailStart, ContainerExpandEnd))
        else fade(progress, 0f, 1f)

    fun contentFloat(progress: Float, hasSource: Boolean, timing: TaskDetailMotionTiming): Float =
        FloatEasing.transform(phase(progress, if (hasSource) timing.contentStart else 0f, 1f))

    private fun phase(progress: Float, start: Float, end: Float): Float =
        if (end <= start) { if (progress < end) 0f else 1f }
        else ((progress - start) / (end - start)).coerceIn(0f, 1f)

    private fun fade(progress: Float, start: Float, end: Float): Float {
        val p = phase(progress, start, end)
        return p * p * (3f - 2f * p)
    }
}
