package com.aritxonly.deadliner.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.roundToInt

internal object FloatingTabMotionDefaults {
    const val PressedScale = 52f / 56f
    const val MaximumStretch = 0.18f
    const val FullStretchTabsPerSecond = 8f
    const val MaximumPanelOffsetDp = 4f
    const val SelectedIconPressedScale = 0.90f
}

internal fun floatingTabSelectedIconScale(pressProgress: Float): Float {
    val progress = pressProgress.coerceIn(0f, 1f)
    return 1f + (FloatingTabMotionDefaults.SelectedIconPressedScale - 1f) * progress
}

internal fun floatingTabPanelDragFraction(
    dragOffsetPx: Float,
    panelWidthPx: Float,
): Float {
    if (panelWidthPx <= 0f) return 0f
    return (dragOffsetPx / panelWidthPx).coerceIn(-1f, 1f)
}

internal fun resolveFloatingTabTarget(
    position: Float,
    tabCount: Int,
    enabledIndices: List<Int>,
): Int {
    if (tabCount <= 0) return 0
    val roundedTarget = position.roundToInt().coerceIn(0, tabCount - 1)
    return enabledIndices
        .asSequence()
        .filter { it in 0 until tabCount }
        .minWithOrNull(
            compareBy<Int> { abs(it - roundedTarget) }
                .thenBy { abs(it - position) },
        )
        ?: roundedTarget
}

internal fun floatingTabStretch(
    velocityPxPerSecond: Float,
    tabWidthPx: Float,
): Float {
    if (tabWidthPx <= 0f) return 0f
    val tabsPerSecond = abs(velocityPxPerSecond) / tabWidthPx
    return (
        tabsPerSecond /
            FloatingTabMotionDefaults.FullStretchTabsPerSecond *
            FloatingTabMotionDefaults.MaximumStretch
        ).coerceIn(0f, FloatingTabMotionDefaults.MaximumStretch)
}

internal fun floatingTabTransformOriginX(
    velocityPxPerSecond: Float,
    stretch: Float,
): Float {
    if (stretch <= 0f || velocityPxPerSecond == 0f) return 0.5f
    val direction = if (velocityPxPerSecond > 0f) 1f else -1f
    val normalizedStretch = (stretch / FloatingTabMotionDefaults.MaximumStretch).coerceIn(0f, 1f)
    return (0.5f + direction * normalizedStretch * 0.35f).coerceIn(0.15f, 0.85f)
}

internal class FloatingTabSelectionSync {
    private var pendingCommittedIndex: Int? = null

    fun markCommitted(index: Int) {
        pendingCommittedIndex = index
    }

    fun shouldAnimateExternalSelection(index: Int): Boolean {
        if (pendingCommittedIndex == index) {
            pendingCommittedIndex = null
            return false
        }
        pendingCommittedIndex = null
        return true
    }

    fun clear() {
        pendingCommittedIndex = null
    }
}

@Stable
internal class FloatingTabMotionState(
    initialIndex: Int,
) {
    val position = Animatable(initialIndex.toFloat(), visibilityThreshold = 0.001f)
    private var directPosition: Float? by mutableStateOf(null)

    val visualPosition: Float
        get() = directPosition ?: position.value

    var isPressed by mutableStateOf(false)
        private set
    var isDragging by mutableStateOf(false)
        private set
    var velocityPxPerSecond by mutableFloatStateOf(0f)
        private set

    private val selectionSync = FloatingTabSelectionSync()

    fun press() {
        isPressed = true
    }

    fun startDrag() {
        directPosition = position.value
        isDragging = true
    }

    fun dragTo(
        positionInTabs: Float,
        tabCount: Int,
        velocityPxPerSecond: Float,
    ) {
        if (tabCount <= 0) return
        directPosition = positionInTabs.coerceIn(0f, (tabCount - 1).toFloat())
        this.velocityPxPerSecond = velocityPxPerSecond
    }

    fun finishGesture(targetIndex: Int) {
        selectionSync.markCommitted(targetIndex)
        isPressed = false
        isDragging = false
        velocityPxPerSecond = 0f
    }

    fun cancelGesture() {
        isPressed = false
        isDragging = false
        velocityPxPerSecond = 0f
        selectionSync.clear()
    }

    suspend fun settleTo(index: Int) {
        directPosition?.let { draggedPosition ->
            position.snapTo(draggedPosition)
            directPosition = null
        }
        animateTo(index)
    }

    private suspend fun animateTo(index: Int) {
        position.animateTo(
            targetValue = index.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    suspend fun syncExternalSelection(index: Int) {
        if (isDragging) return
        if (!selectionSync.shouldAnimateExternalSelection(index)) {
            return
        }
        if (abs(position.value - index) > 0.001f) {
            settleTo(index)
        }
    }
}
