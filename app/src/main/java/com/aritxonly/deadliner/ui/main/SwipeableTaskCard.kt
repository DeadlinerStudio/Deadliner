package com.aritxonly.deadliner.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.ui.theme.actionContentColor
import com.aritxonly.deadliner.ui.theme.rememberTaskSwipeActionColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val ACTION_BUTTON_SIZE_DP = 48f
private const val ACTION_CONTENT_HEIGHT_DP = 60f
private const val ACTION_BASE_GAP_DP = 8f
private const val ACTION_MAX_GAP_DP = 24f

@Composable
fun DDLItemCardSwipeable(
    title: String,
    remainingTimeAlt: String,
    note: String,
    progress: Float,
    isStarred: Boolean,
    status: DDLStatus,
    swipeKey: String,
    activeSwipeKey: String?,
    onActiveSwipeChange: (String?) -> Unit,
    primaryActionLabel: String,
    secondaryActionLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    onEditAction: (() -> Unit)? = null,
    onDeleteAction: (() -> Unit)? = null,
    useDisabledCompletedStyle: Boolean = false,
    primaryActionIcon: Painter? = null,
    primaryActionColor: Color? = null,
    secondaryActionIcon: Painter? = null,
    secondaryActionColor: Color? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPressSelect: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null,
    categoryContent: (@Composable () -> Unit)? = null,
    onFocusAction: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    val leadingRevealWidthPx = with(density) {
        (if (onFocusAction != null) TaskCardSwipeDefaults.FOCUS_REVEAL_WIDTH_DP
        else TaskCardSwipeDefaults.REVEAL_WIDTH_DP).dp.toPx()
    }
    val trailingRevealWidthPx = with(density) { TaskCardSwipeDefaults.REVEAL_WIDTH_DP.dp.toPx() }
    val revealThresholdPx = with(density) {
        TaskCardSwipeDefaults.REVEAL_TRIGGER_DISTANCE_DP.dp.toPx()
    }
    val closeDistancePx = with(density) { TaskCardSwipeDefaults.CLOSE_DISTANCE_DP.dp.toPx() }
    val actionColors = rememberTaskSwipeActionColors()

    val resolvedPrimaryActionIcon = primaryActionIcon
        ?: com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_task_alt_wght400)
    val resolvedPrimaryActionColor = primaryActionColor ?: actionColors.complete
    val resolvedSecondaryActionIcon = secondaryActionIcon
        ?: com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_flag_wght400)
    val resolvedSecondaryActionColor = secondaryActionColor ?: actionColors.giveUp
    val editActionIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_edit_wght400)
    val deleteActionIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_delete_wght400)
    val editActionColor = actionColors.edit
    val deleteActionColor = actionColors.delete
    val editActionLabel = androidx.compose.ui.res.stringResource(R.string.edit)
    val deleteActionLabel = androidx.compose.ui.res.stringResource(R.string.delete)
    val hasTrailingActions = onEditAction != null && onDeleteAction != null

    val latestOnPrimaryAction by rememberUpdatedState(onPrimaryAction)
    val latestOnSecondaryAction by rememberUpdatedState(onSecondaryAction)
    val latestOnFocusAction by rememberUpdatedState(onFocusAction)
    val focusActionIcon = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_schedule_wght400)
    val focusActionLabel = androidx.compose.ui.res.stringResource(R.string.focus_swipe)
    val focusActionColor = MaterialTheme.colorScheme.primary
    val latestOnEditAction by rememberUpdatedState(onEditAction)
    val latestOnDeleteAction by rememberUpdatedState(onDeleteAction)
    val latestOnActiveSwipeChange by rememberUpdatedState(onActiveSwipeChange)
    val latestActiveSwipeKey by rememberUpdatedState(activeSwipeKey)

    var cardWidthPx by remember(swipeKey) { mutableIntStateOf(1) }
    var offsetPx by remember(swipeKey) { mutableFloatStateOf(0f) }
    var dragStartOffsetPx by remember(swipeKey) { mutableFloatStateOf(0f) }
    var gestureOffsetPx by remember(swipeKey) { mutableFloatStateOf(0f) }
    var animationJob by remember(swipeKey) { mutableStateOf<Job?>(null) }

    fun animateTo(target: Float, clearActiveWhenClosed: Boolean = true) {
        animationJob?.cancel()
        animationJob = scope.launch {
            Animatable(offsetPx).animateTo(
                targetValue = target,
                animationSpec = if (target == 0f) {
                    spring(dampingRatio = 0.72f, stiffness = 275f)
                } else {
                    spring(dampingRatio = 0.78f, stiffness = 385f)
                },
            ) {
                offsetPx = value
            }
            offsetPx = target
            if (
                target == 0f &&
                clearActiveWhenClosed &&
                latestActiveSwipeKey == swipeKey
            ) {
                latestOnActiveSwipeChange(null)
            }
        }
    }

    fun runAction(action: () -> Unit) {
        animateTo(0f)
        latestOnActiveSwipeChange(null)
        action()
    }

    LaunchedEffect(selectionMode, activeSwipeKey, swipeKey) {
        if ((selectionMode || activeSwipeKey != swipeKey) && abs(offsetPx) > 0.5f) {
            animateTo(0f, clearActiveWhenClosed = false)
        }
    }

    val draggableState = rememberDraggableState { delta ->
        animationJob?.cancel()
        val maximumOffset = cardWidthPx * TaskCardSwipeDefaults.MAX_OFFSET_FRACTION
        val minimumOffset = if (hasTrailingActions) -maximumOffset else 0f
        offsetPx = (offsetPx + delta).coerceIn(minimumOffset, maximumOffset)
        gestureOffsetPx += delta
    }

    fun settleAfterDrag() {
        when (
            resolveTaskCardSwipeSettle(
                offset = offsetPx,
                dragStartOffset = dragStartOffsetPx,
                gestureOffset = gestureOffsetPx,
                cardWidth = cardWidthPx.toFloat(),
                revealThreshold = revealThresholdPx,
                closeDistance = closeDistancePx,
                hasTrailingActions = hasTrailingActions,
                leadingRevealWidth = leadingRevealWidthPx,
            )
        ) {
            TaskCardSwipeSettleResult.CLOSED -> animateTo(0f)
            TaskCardSwipeSettleResult.LEADING_REVEALED -> {
                animateTo(leadingRevealWidthPx, clearActiveWhenClosed = false)
            }
            TaskCardSwipeSettleResult.TRAILING_REVEALED -> {
                animateTo(-trailingRevealWidthPx, clearActiveWhenClosed = false)
            }
            TaskCardSwipeSettleResult.RUN_PRIMARY_ACTION -> runAction(latestOnPrimaryAction)
            TaskCardSwipeSettleResult.RUN_DELETE_ACTION -> {
                latestOnDeleteAction?.let(::runAction) ?: animateTo(0f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .onSizeChanged { cardWidthPx = it.width },
    ) {
        if (!selectionMode && abs(offsetPx) > 0.5f) {
            SwipeActionBackground(
                offsetPx = offsetPx,
                density = density,
                primaryAction = SwipeActionUi(
                    label = primaryActionLabel,
                    icon = resolvedPrimaryActionIcon,
                    color = resolvedPrimaryActionColor,
                    onClick = { runAction(latestOnPrimaryAction) },
                ),
                secondaryAction = SwipeActionUi(
                    label = secondaryActionLabel,
                    icon = resolvedSecondaryActionIcon,
                    color = resolvedSecondaryActionColor,
                    onClick = { runAction(latestOnSecondaryAction) },
                ),
                editAction = SwipeActionUi(
                    label = editActionLabel,
                    icon = editActionIcon,
                    color = editActionColor,
                    onClick = { latestOnEditAction?.let(::runAction) },
                ),
                deleteAction = SwipeActionUi(
                    label = deleteActionLabel,
                    icon = deleteActionIcon,
                    color = deleteActionColor,
                    onClick = { latestOnDeleteAction?.let(::runAction) },
                ),
                showTrailingActions = hasTrailingActions,
                focusAction = if (onFocusAction != null) SwipeActionUi(
                    label = focusActionLabel,
                    icon = focusActionIcon,
                    color = focusActionColor,
                    onClick = { latestOnFocusAction?.let(::runAction) },
                ) else null,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .clip(shape)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = !selectionMode,
                    onDragStarted = {
                        animationJob?.cancel()
                        latestOnActiveSwipeChange(swipeKey)
                        dragStartOffsetPx = offsetPx
                        gestureOffsetPx = 0f
                    },
                    onDragStopped = { settleAfterDrag() },
                )
                .combinedClickable(
                    onClick = {
                        if (abs(offsetPx) > 0.5f) {
                            animateTo(0f)
                        } else if (selectionMode) {
                            onToggleSelect?.invoke()
                        } else {
                            onClick?.invoke()
                        }
                    },
                    onLongClick = { onLongPressSelect?.invoke() },
                ),
        ) {
            DDLItemCardSimplified(
                title = title,
                remainingTimeAlt = remainingTimeAlt,
                note = note,
                progress = progress,
                isStarred = isStarred,
                modifier = Modifier.fillMaxSize(),
                onClick = null,
                status = status,
                useDisabledCompletedStyle = useDisabledCompletedStyle,
                categoryContent = categoryContent,
            )

            if (selectionMode && selected) {
                SelectionOverlay(shape)
            }
        }
    }
}

private data class SwipeActionUi(
    val label: String,
    val icon: Painter,
    val color: Color,
    val onClick: () -> Unit,
)

@Composable
private fun SwipeActionBackground(
    offsetPx: Float,
    density: androidx.compose.ui.unit.Density,
    primaryAction: SwipeActionUi,
    secondaryAction: SwipeActionUi,
    editAction: SwipeActionUi,
    deleteAction: SwipeActionUi,
    showTrailingActions: Boolean,
    focusAction: SwipeActionUi?,
) {
    val revealedDistancePx = abs(offsetPx)
    val revealWidthPx = with(density) {
        (if (offsetPx > 0f && focusAction != null) TaskCardSwipeDefaults.FOCUS_REVEAL_WIDTH_DP
        else TaskCardSwipeDefaults.REVEAL_WIDTH_DP).dp.toPx()
    }
    val baseGapPx = with(density) { ACTION_BASE_GAP_DP.dp.toPx() }
    val maxGapPx = with(density) { ACTION_MAX_GAP_DP.dp.toPx() }
    val buttonSizePx = with(density) { ACTION_BUTTON_SIZE_DP.dp.toPx() }
    val groupWidthPx = max(revealWidthPx, revealedDistancePx)
    val gapCount = if (offsetPx > 0f && focusAction != null) 2 else 1
    val gapPx = baseGapPx + ((revealedDistancePx - revealWidthPx) / gapCount)
        .coerceIn(0f, maxGapPx - baseGapPx)
    val stretchPx = (revealedDistancePx - revealWidthPx - (gapPx - baseGapPx) * gapCount)
        .coerceAtLeast(0f)
    val fullActionWidthPx = buttonSizePx + stretchPx

    val groupWidth = with(density) { groupWidthPx.toDp() }
    val gap = with(density) { gapPx.toDp() }
    val fullActionWidth = with(density) { fullActionWidthPx.toDp() }

    Box(Modifier.fillMaxSize()) {
        if (offsetPx > 0f) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(groupWidth),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SwipeActionButton(
                    action = primaryAction,
                    modifier = Modifier.width(fullActionWidth),
                    alignment = Alignment.CenterStart,
                )
                focusAction?.let { SwipeActionButton(it, Modifier.width(ACTION_BUTTON_SIZE_DP.dp)) }
                SwipeActionButton(secondaryAction, Modifier.width(ACTION_BUTTON_SIZE_DP.dp))
            }
        } else if (showTrailingActions) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(groupWidth),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SwipeActionButton(editAction, Modifier.width(ACTION_BUTTON_SIZE_DP.dp))
                SwipeActionButton(
                    action = deleteAction,
                    modifier = Modifier.width(fullActionWidth),
                    alignment = Alignment.CenterEnd,
                )
            }
        }
    }
}

@Composable
private fun SwipeActionButton(
    action: SwipeActionUi,
    modifier: Modifier,
    alignment: Alignment = Alignment.Center,
) {
    val contentColor = actionContentColor(action.color)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier
                .width(ACTION_BUTTON_SIZE_DP.dp)
                .height(ACTION_CONTENT_HEIGHT_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                onClick = action.onClick,
                modifier = Modifier.size(ACTION_BUTTON_SIZE_DP.dp),
                shape = CircleShape,
                color = action.color,
                contentColor = contentColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = action.icon,
                        contentDescription = action.label,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Text(
                text = action.label,
                modifier = Modifier.offset(y = 2.dp),
                fontSize = 10.sp,
                lineHeight = 10.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
