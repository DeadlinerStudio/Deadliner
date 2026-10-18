package com.aritxonly.deadliner.ui.main.simplified

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.FocusTimerActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.semantic.DeadlineSemanticStatus
import com.aritxonly.deadliner.model.semantic.DeadlineStatusResolver
import com.aritxonly.deadliner.model.semantic.toDDLStatus
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.main.DDLItemCardSwipeable
import com.aritxonly.deadliner.ui.main.shared.computeProgress
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.theme.rememberTaskSwipeActionColors
import com.aritxonly.deadliner.ui.category.CategoryBadge
import com.aritxonly.deadliner.ui.category.CategoryPresentation
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.floor
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskAction
import com.deadliner.domain.task.TaskActionOutcome
import com.deadliner.domain.task.TaskState

@Composable
fun AnimatedItem(
    item: Task,
    index: Int,
    content:  @Composable () -> Unit
) {
    var visible by rememberSaveable(item.uid) { mutableStateOf(false) }

    LaunchedEffect(item.uid) {
        delay(index * 70L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(380)) +
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(380)
                ),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        content()
    }
}

@Composable
fun TaskItem(
    item: Task,
    activity: MainActivity,
    activeSwipeTaskId: String?,
    onActiveSwipeTaskChange: (String?) -> Unit,
    applyTaskAction: (TaskAction) -> TaskActionOutcome,
    celebrate: () -> Unit,
    deleteTask: () -> TaskActionOutcome,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPressSelect: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null,
    onAbandonDialogVisibilityChange: (Boolean) -> Unit = {},
    category: CategoryPresentation? = null,
) {
    val detailAnchor = com.aritxonly.deadliner.ui.detail.rememberTaskDetailAnchor()
    val focusEntry = com.aritxonly.deadliner.ui.focus.LocalFocusTimerEntry.current
    val context = LocalContext.current
    val editorEntry = com.aritxonly.deadliner.ui.editor.LocalDeadlineEditorEntry.current
    val swipeActionColors = rememberTaskSwipeActionColors()
    var showGiveUpDialog by rememberSaveable(item.uid) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(item.uid) { mutableStateOf(false) }

    LaunchedEffect(showGiveUpDialog, showDeleteDialog) {
        onAbandonDialogVisibilityChange(showGiveUpDialog || showDeleteDialog)
    }

    val startTime = item.startAt?.let { runCatching { GlobalUtils.parseDateTime(it) }.getOrNull() }
    val endTime = item.dueAt?.let { runCatching { GlobalUtils.parseDateTime(it) }.getOrNull() }
    val now = LocalDateTime.now()

    val remainingTimeText = formatTaskRemainingTime(
        context = context,
        item = item,
        startTime = startTime,
        endTime = endTime,
        now = now,
    )

    val progress = computeProgress(startTime, endTime, now)
    val semanticStatus = DeadlineStatusResolver.resolve(item.state, endTime, now)
    val status = semanticStatus.toDDLStatus()

    val primaryAction = when (item.state) {
        TaskState.ACTIVE -> TaskAction.MARK_COMPLETE
        TaskState.COMPLETED, TaskState.ABANDONED -> TaskAction.RESTORE_ACTIVE
        else -> null
    }
    val secondaryAction = when (item.state) {
        TaskState.ACTIVE -> TaskAction.MARK_GIVE_UP
        TaskState.COMPLETED, TaskState.ABANDONED -> TaskAction.MARK_ARCHIVE
        else -> null
    }
    val primaryIcon = when (primaryAction) {
        TaskAction.RESTORE_ACTIVE -> iconResource(R.drawable.msr_undo_wght400)
        else -> iconResource(R.drawable.msr_task_alt_wght400)
    }
    val secondaryIcon = when (secondaryAction) {
        TaskAction.MARK_ARCHIVE -> iconResource(R.drawable.msr_move_to_inbox_wght400)
        TaskAction.MARK_GIVE_UP -> iconResource(R.drawable.msr_flag_wght400)
        else -> iconResource(R.drawable.msr_delete_wght400)
    }
    val primaryLabel = when (item.state) {
        TaskState.ACTIVE -> stringResource(R.string.complete)
        TaskState.COMPLETED -> stringResource(R.string.undo)
        TaskState.ABANDONED -> stringResource(R.string.task_swipe_restore)
        else -> ""
    }
    val secondaryLabel = when (item.state) {
        TaskState.ACTIVE -> stringResource(R.string.task_swipe_give_up)
        TaskState.COMPLETED, TaskState.ABANDONED -> stringResource(R.string.archive)
        else -> ""
    }
    val openDetails = {
        detailAnchor.open(activity, item.uid)
    }
    val openEditor = {
        editorEntry.open(AddDDLActivity.newEditIntent(context, item.uid))
    }

    DDLItemCardSwipeable(
        modifier = detailAnchor.modifier,
        detailAnchor = detailAnchor,
        title = item.title,
        remainingTimeAlt = remainingTimeText,
        note = item.note,
        progress = progress,
        isStarred = item.isStarred,
        status = status,
        useDisabledCompletedStyle = semanticStatus == DeadlineSemanticStatus.ABANDONED,
        swipeKey = "task:${item.uid}",
        activeSwipeKey = activeSwipeTaskId,
        onActiveSwipeChange = onActiveSwipeTaskChange,
        primaryActionLabel = primaryLabel,
        secondaryActionLabel = secondaryLabel,
        onClick = openDetails,
        onPrimaryAction = {
            GlobalUtils.triggerVibration(activity, 100)
            val action = primaryAction ?: return@DDLItemCardSwipeable
            val outcome = applyTaskAction(action)
            if (outcome == TaskActionOutcome.TIMER_REQUIRES_RESOLUTION) {
                Toast.makeText(activity, R.string.focus_resolve_before_task_change, Toast.LENGTH_LONG).show()
                activity.startActivity(FocusTimerActivity.newResolutionIntent(activity, item.uid, item.title, action))
            } else if (outcome == TaskActionOutcome.APPLIED && action == TaskAction.MARK_COMPLETE) {
                celebrate()
                Toast.makeText(activity, R.string.toast_finished, Toast.LENGTH_SHORT).show()
            } else if (outcome == TaskActionOutcome.APPLIED) {
                Toast.makeText(activity, R.string.toast_restored_active, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, R.string.focus_task_action_failed, Toast.LENGTH_SHORT).show()
            }
        },
        onSecondaryAction = {
            GlobalUtils.triggerVibration(activity, 200)
            val action = secondaryAction ?: return@DDLItemCardSwipeable
            if (action == TaskAction.MARK_GIVE_UP) {
                showGiveUpDialog = true
            } else {
                if (applyTaskAction(action) == TaskActionOutcome.APPLIED) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.toast_archived, 1),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(activity, R.string.focus_task_action_failed, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onFocusAction = if (item.state == TaskState.ACTIVE && !item.isDeleted) {
            { focusEntry(item.uid, item.title) }
        } else null,
        onEditAction = openEditor,
        onDeleteAction = {
            GlobalUtils.triggerVibration(activity, 200)
            showDeleteDialog = true
        },
        primaryActionIcon = primaryIcon,
        primaryActionColor = if (item.state == TaskState.ABANDONED) {
            swipeActionColors.giveUp
        } else {
            swipeActionColors.complete
        },
        secondaryActionIcon = secondaryIcon,
        secondaryActionColor = if (item.state == TaskState.ACTIVE) {
            swipeActionColors.giveUp
        } else {
            swipeActionColors.archive
        },
        selectionMode = selectionMode,
        selected = selected,
        onLongPressSelect = onLongPressSelect,
        onToggleSelect = onToggleSelect,
        categoryContent = category?.let { selectedCategory ->
            { CategoryBadge(category = selectedCategory, compact = true) }
        },
    )

    if (showGiveUpDialog) {
        AlertDialog(
            show = true,
            onDismissRequest = { showGiveUpDialog = false },
            title = { Text(stringResource(R.string.confirm_give_up_title)) },
            text = { Text(stringResource(R.string.confirm_give_up_message)) },
            miuixTitle = context.getString(R.string.confirm_give_up_title),
            miuixSummary = context.getString(R.string.confirm_give_up_message),
            confirmButton = {
                TextButton(
                    onClick = {
                        showGiveUpDialog = false
                        val outcome = applyTaskAction(TaskAction.MARK_GIVE_UP)
                        if (outcome == TaskActionOutcome.TIMER_REQUIRES_RESOLUTION) {
                            Toast.makeText(
                                activity,
                                R.string.focus_resolve_before_task_change,
                                Toast.LENGTH_LONG,
                            ).show()
                            activity.startActivity(FocusTimerActivity.newResolutionIntent(activity, item.uid, item.title, TaskAction.MARK_GIVE_UP))
                        } else if (outcome == TaskActionOutcome.APPLIED) {
                            Toast.makeText(activity, R.string.toast_give_up, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, R.string.focus_task_action_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGiveUpDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            show = true,
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.alert_delete_title)) },
            text = { Text(stringResource(R.string.alert_delete_message)) },
            miuixTitle = context.getString(R.string.alert_delete_title),
            miuixSummary = context.getString(R.string.alert_delete_message),
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val outcome = deleteTask()
                        if (outcome == TaskActionOutcome.TIMER_REQUIRES_RESOLUTION) {
                            Toast.makeText(
                                activity,
                                R.string.focus_resolve_before_task_change,
                                Toast.LENGTH_LONG,
                            ).show()
                            activity.startActivity(FocusTimerActivity.newResolutionIntent(activity, item.uid, item.title, delete = true))
                        } else if (outcome == TaskActionOutcome.APPLIED) {
                            Toast.makeText(activity, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, R.string.focus_task_action_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun formatTaskRemainingTime(
    context: Context,
    item: Task,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    now: LocalDateTime,
): String {
    if (item.state == TaskState.ABANDONED) {
        return context.getString(R.string.abandoned)
    }
    if (item.state == TaskState.COMPLETED) {
        return context.getString(R.string.completed)
    }

    val beforeStart = startTime?.isAfter(now) == true
    val targetTime = when {
        beforeStart -> startTime ?: return item.dueAt.orEmpty().ifBlank { item.startAt.orEmpty() }
        endTime != null -> endTime
        else -> return item.dueAt.orEmpty().ifBlank { item.startAt.orEmpty() }
    }

    val diffSeconds = Duration.between(now, targetTime).seconds.toDouble()
    val diffHours = floor(diffSeconds / 3600.0).toInt()

    if (!beforeStart && diffSeconds < 0) {
        return context.getString(R.string.task_remaining_overdue_hours, abs(diffHours))
    }

    val positiveHours = diffHours.coerceAtLeast(0)
    val days = positiveHours / 24
    val hours = positiveHours % 24

    return when {
        beforeStart && days > 0 ->
            context.getString(R.string.task_remaining_starts_in_days_hours, days, hours)
        beforeStart && positiveHours > 0 ->
            context.getString(R.string.task_remaining_starts_in_hours, positiveHours)
        beforeStart ->
            context.getString(R.string.task_remaining_starts_in_less_than_one_hour)
        days > 0 ->
            context.getString(R.string.task_remaining_days_hours, days, hours)
        positiveHours > 0 ->
            context.getString(R.string.task_remaining_hours, positiveHours)
        else ->
            context.getString(R.string.task_remaining_less_than_one_hour)
    }
}
