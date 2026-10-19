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
    detailAnchor: com.aritxonly.deadliner.ui.detail.TaskDetailAnchor? = null,
    completedSubtasks: Int = 0,
    totalSubtasks: Int = 0,
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    SwipeableItemCard(swipeKey = swipeKey, activeSwipeKey = activeSwipeKey,
        onActiveSwipeChange = onActiveSwipeChange, primaryActionLabel = primaryActionLabel,
        secondaryActionLabel = secondaryActionLabel, modifier = modifier.height(76.dp),
        onPrimaryAction = onPrimaryAction, onSecondaryAction = onSecondaryAction,
        onEditAction = onEditAction, onDeleteAction = onDeleteAction,
        primaryActionIcon = primaryActionIcon, primaryActionColor = primaryActionColor,
        secondaryActionIcon = secondaryActionIcon, secondaryActionColor = secondaryActionColor,
        selectionMode = selectionMode, onFocusAction = onFocusAction, accessibilityLabel = title,
    ) { closeOrRun ->
        Box(Modifier.fillMaxSize().combinedClickable(onClick = {
            closeOrRun { if (selectionMode) onToggleSelect?.invoke() else onClick?.invoke() }
        }, onLongClick = { onLongPressSelect?.invoke() })) {
            DDLItemCardSimplified(title = title, remainingTimeAlt = remainingTimeAlt, note = note,
                progress = progress, isStarred = isStarred, modifier = Modifier.fillMaxSize(),
                onClick = null, status = status, useDisabledCompletedStyle = useDisabledCompletedStyle,
                categoryContent = categoryContent, detailAnchor = detailAnchor,
                completedSubtasks = completedSubtasks, totalSubtasks = totalSubtasks)
            if (selectionMode && selected) SelectionOverlay(shape)
        }
    }
}
