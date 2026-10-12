// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.aritxonly.deadliner.ui.picker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

/**
 * Local fork of MIUIX 0.9.4-rc01 NumberPicker (4a6b750b).
 * Seeds row geometry before the first layout so all digits render on opening.
 * Date/time carry is supplied by the caller through chronological indices.
 *
 * A [DeadlinerNumberPicker] component with Miuix style.
 *
 * A vertical scroll picker that displays a range of numbers with the selected value centered.
 * Items fade out and scale down as they move away from the center.
 *
 * @param value The current selected value. If outside [range], it will be coerced into the range.
 * @param onValueChange The callback invoked when the selected value changes.
 * @param modifier The modifier to be applied to the [DeadlinerNumberPicker].
 * @param enabled Whether the [DeadlinerNumberPicker] is enabled for user interaction.
 * @param range The range of selectable values.
 * @param label A function that converts a value to its display string.
 * @param visibleItemCount The number of visible items. Must be odd and at least 3.
 * @param wrapAround Whether the picker wraps around from the last item to the first (infinite scrolling).
 * @param colors The [NumberPickerColors] for this [DeadlinerNumberPicker].
 * @param textStyle The [TextStyle] for the picker items.
 * @param itemHeight The height of each item in the picker.
 */
@Composable
internal fun DeadlinerNumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    range: IntRange = 0..10,
    label: (Int) -> String = { it.toString() },
    visibleItemCount: Int = 5,
    wrapAround: Boolean = false,
    colors: NumberPickerColors = NumberPickerDefaults.colors(),
    textStyle: TextStyle = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.Normal),
    itemHeight: Dp = NumberPickerDefaults.ItemHeight,
) {
    require(visibleItemCount % 2 == 1 && visibleItemCount >= 3) {
        "visibleItemCount must be odd and at least 3, but was $visibleItemCount"
    }
    require(range.first <= range.last) {
        "range must not be empty"
    }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val publisher = remember { PickerValuePublisher(value.coerceIn(range)) }
    val itemCount = range.last - range.first + 1
    val coercedValue = value.coerceIn(range)
    // Keep the gesture origin fixed while publishing live values to the other wheels.
    var anchorValue by remember { mutableIntStateOf(coercedValue) }
    val currentIndex = anchorValue.coerceIn(range) - range.first
    val latestRange by rememberUpdatedState(range)
    val latestWrapAround by rememberUpdatedState(wrapAround)
    val halfVisibleCount = visibleItemCount / 2
    val hapticFeedback = LocalHapticFeedback.current

    // Drag offset (updated synchronously, no coroutine needed)
    var dragOffset by remember { mutableFloatStateOf(0f) }
    // Fling/snap animated offset
    val flingAnimatable = remember { Animatable(0f) }
    var isUserScrolling by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var itemHeightPx by remember(density, itemHeight) { mutableIntStateOf(with(density) { itemHeight.roundToPx() }) }

    // Total offset combines drag + fling
    val totalOffset by remember {
        derivedStateOf { if (!isUserScrolling) 0f else if (isDragging) dragOffset else flingAnimatable.value }
    }

    // Parent echoes during drag/fling must not restart motion or move the gesture origin.
    LaunchedEffect(coercedValue, isUserScrolling) {
        if (!isUserScrolling) {
            anchorValue = coercedValue
            publisher.reset(coercedValue)
            dragOffset = 0f
            flingAnimatable.snapTo(0f)
        }
    }

    fun publishOffset(offset: Float) {
        val next = pickerScrolledValue(anchorValue, offset, latestRange, latestWrapAround)
        if (publisher.publish(next, currentOnValueChange)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val totalHeight = itemHeight * visibleItemCount

    val draggableState = rememberDraggableState { delta ->
        if (itemHeightPx > 0) {
            val newOffset = dragOffset - delta / itemHeightPx
            dragOffset = if (wrapAround) {
                newOffset
            } else {
                newOffset.coerceIn(
                    -(currentIndex.toFloat()),
                    (itemCount - 1 - currentIndex).toFloat(),
                )
            }
            publishOffset(dragOffset)
        }
    }

    val displayValue = label(coercedValue)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .clipToBounds()
            .semantics {
                contentDescription = "$displayValue, ${range.first} - ${range.last}"
            }
            .onSizeChanged { size ->
                itemHeightPx = size.height / visibleItemCount
            }
            .then(
                if (enabled) {
                    Modifier.draggable(
                        orientation = Orientation.Vertical,
                        state = draggableState,
                        onDragStarted = {
                            flingAnimatable.stop()
                            // Switch the offset source before resetting the inactive animator.
                            // A suspending snap must never publish a transient zero/double offset.
                            dragOffset = totalOffset
                            isDragging = true
                            isUserScrolling = true
                            flingAnimatable.snapTo(0f)
                        },
                        onDragStopped = { velocity ->
                            if (itemHeightPx > 0) {
                                // Transfer drag offset to fling animatable
                                val currentDragOffset = dragOffset
                                flingAnimatable.snapTo(currentDragOffset)
                                isDragging = false
                                dragOffset = 0f

                                val velocityInItems = -velocity / itemHeightPx
                                val decay = exponentialDecay<Float>(frictionMultiplier = 2f)
                                // Set bounds for non-wrap mode
                                if (!wrapAround) {
                                    val min = -(currentIndex.toFloat())
                                    val max = (itemCount - 1 - currentIndex).toFloat()
                                    flingAnimatable.updateBounds(min, max)
                                }
                                // Fling with real decay for natural inertia
                                flingAnimatable.animateDecay(velocityInItems, decay) { publishOffset(this.value) }
                                // Reset bounds
                                flingAnimatable.updateBounds(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
                                // Snap to nearest item
                                val snappedTarget = flingAnimatable.value.fastRoundToInt().toFloat()
                                flingAnimatable.animateTo(
                                    targetValue = snappedTarget,
                                    animationSpec = spring(dampingRatio = 1f, stiffness = 400f),
                                ) { publishOffset(this.value) }
                                val offsetInt = flingAnimatable.value.fastRoundToInt()
                                val newIndex = if (wrapAround) {
                                    ((currentIndex + offsetInt) % itemCount + itemCount) % itemCount
                                } else {
                                    (currentIndex + offsetInt).coerceIn(0, itemCount - 1)
                                }
                                val newValue = range.first + newIndex
                                publisher.publish(newValue, currentOnValueChange)
                                // Rebase only after settling; live parent values never double the offset.
                                anchorValue = newValue
                                isUserScrolling = false
                                flingAnimatable.snapTo(0f)
                            }
                        },
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (itemHeightPx > 0) {
            val currentTotalOffset = totalOffset
            val centerItemOffset = currentTotalOffset - currentTotalOffset.fastRoundToInt()
            val roundedOffset = currentTotalOffset.fastRoundToInt()
            val selectedColor = colors.selectedTextColor(enabled)
            val unselectedColor = colors.unselectedTextColor(enabled)
            val resolvedTextStyle = if (textStyle.fontWeight == null) textStyle.copy(fontWeight = FontWeight.SemiBold) else textStyle

            for (i in -halfVisibleCount - 1..halfVisibleCount + 1) {
                val rawItemIndex = currentIndex + i + roundedOffset
                val itemIndex = if (wrapAround) {
                    ((rawItemIndex % itemCount) + itemCount) % itemCount
                } else {
                    if (rawItemIndex !in 0..<itemCount) continue
                    rawItemIndex
                }

                val distanceFromCenter = i.toFloat() - centerItemOffset
                val normalizedDistance = (abs(distanceFromCenter) / (halfVisibleCount + 0.5f)).coerceIn(0f, 1f)

                val alpha = (1f - normalizedDistance) * (1f - normalizedDistance * 0.5f)
                val scale = 1f - 0.2f * normalizedDistance
                val yOffset = distanceFromCenter * itemHeightPx

                val textColor = Color(
                    red = lerp(selectedColor.red, unselectedColor.red, normalizedDistance),
                    green = lerp(selectedColor.green, unselectedColor.green, normalizedDistance),
                    blue = lerp(selectedColor.blue, unselectedColor.blue, normalizedDistance),
                    alpha = lerp(selectedColor.alpha, unselectedColor.alpha, normalizedDistance),
                )

                val displayItem = range.first + itemIndex
                val itemLabel = remember(displayItem, label) { label(displayItem) }
                Text(
                    text = itemLabel,
                    modifier = Modifier
                        .graphicsLayer {
                            this.alpha = alpha
                            scaleX = scale
                            scaleY = scale
                            translationY = yOffset
                        },
                    style = resolvedTextStyle,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Contains default values used by [DeadlinerNumberPicker].
 */
object NumberPickerDefaults {

    val ItemHeight = 45.dp

    /**
     * Creates the default [NumberPickerColors] for a [DeadlinerNumberPicker].
     *
     * @param selectedTextColor The color for the selected (center) item text.
     * @param unselectedTextColor The color for unselected item text.
     * @param disabledSelectedTextColor The color for the selected item text when disabled.
     * @param disabledUnselectedTextColor The color for unselected item text when disabled.
     */
    @Composable
    fun colors(
        selectedTextColor: Color = MiuixTheme.colorScheme.onSurface,
        unselectedTextColor: Color = MiuixTheme.colorScheme.onSurfaceSecondary,
        disabledSelectedTextColor: Color = MiuixTheme.colorScheme.disabledOnSecondary,
        disabledUnselectedTextColor: Color = MiuixTheme.colorScheme.disabledOnSecondary,
    ): NumberPickerColors = remember(
        selectedTextColor,
        unselectedTextColor,
        disabledSelectedTextColor,
        disabledUnselectedTextColor,
    ) {
        NumberPickerColors(
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            disabledSelectedTextColor = disabledSelectedTextColor,
            disabledUnselectedTextColor = disabledUnselectedTextColor,
        )
    }
}

/**
 * The colors for a [DeadlinerNumberPicker].
 *
 * @see NumberPickerDefaults.colors
 */
@Immutable
data class NumberPickerColors(
    private val selectedTextColor: Color,
    private val unselectedTextColor: Color,
    private val disabledSelectedTextColor: Color,
    private val disabledUnselectedTextColor: Color,
) {

    @Stable
    internal fun selectedTextColor(enabled: Boolean): Color = if (enabled) selectedTextColor else disabledSelectedTextColor

    @Stable
    internal fun unselectedTextColor(enabled: Boolean): Color = if (enabled) unselectedTextColor else disabledUnselectedTextColor
}
