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
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.main.DDLItemCardSwipeable
import com.aritxonly.deadliner.ui.main.shared.computeProgress
import com.aritxonly.deadliner.ui.iconResource
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.floor
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskAction
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
    applyTaskAction: (TaskAction) -> Unit,
    celebrate: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPressSelect: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null,
    onAbandonDialogVisibilityChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var showGiveUpDialog by rememberSaveable(item.uid) { mutableStateOf(false) }

    LaunchedEffect(showGiveUpDialog) {
        onAbandonDialogVisibilityChange(showGiveUpDialog)
    }

    val startTime = item.startAt?.let(GlobalUtils::parseDateTime)
    val endTime = item.dueAt?.let(GlobalUtils::parseDateTime)
    val now = LocalDateTime.now()

    val remainingTimeText = formatTaskRemainingTime(
        context = context,
        item = item,
        startTime = startTime,
        endTime = endTime,
        now = now,
    )

    val progress = computeProgress(startTime, endTime, now)
    val status = if (item.state != TaskState.ACTIVE) {
        DDLStatus.COMPLETED
    } else {
        DDLStatus.calculateStatus(startTime, endTime, now, false)
    }

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
        TaskAction.RESTORE_ACTIVE -> iconResource(R.drawable.ic_back)
        else -> iconResource(R.drawable.ic_done)
    }
    val secondaryIcon = when (secondaryAction) {
        TaskAction.MARK_ARCHIVE -> iconResource(R.drawable.ic_archiving)
        TaskAction.MARK_GIVE_UP -> iconResource(R.drawable.ic_flag)
        else -> iconResource(R.drawable.ic_delete)
    }

    DDLItemCardSwipeable(
        title = item.title,
        remainingTimeAlt = remainingTimeText,
        note = item.note,
        progress = progress,
        isStarred = item.isStarred,
        status = status,
        useDisabledCompletedStyle = item.state == TaskState.ABANDONED,
        onClick = {
            val intent = DeadlineDetailActivity.newIntent(context, item.uid)
            activity.startActivity(intent)
        },
        onComplete = {
            GlobalUtils.triggerVibration(activity, 100)
            val action = primaryAction ?: return@DDLItemCardSwipeable
            applyTaskAction(action)
            if (action == TaskAction.MARK_COMPLETE) {
                celebrate()
                Toast.makeText(activity, R.string.toast_finished, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, R.string.toast_restored_active, Toast.LENGTH_SHORT).show()
            }
        },
        onDelete = {
            GlobalUtils.triggerVibration(activity, 200)
            val action = secondaryAction ?: return@DDLItemCardSwipeable
            if (action == TaskAction.MARK_GIVE_UP) {
                showGiveUpDialog = true
            } else {
                applyTaskAction(action)
                Toast.makeText(
                    activity,
                    activity.getString(R.string.toast_archived, 1),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        primaryActionIcon = primaryIcon,
        secondaryActionIcon = secondaryIcon,
        selectionMode = selectionMode,
        selected = selected,
        onLongPressSelect = onLongPressSelect,
        onToggleSelect = onToggleSelect,
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
                androidx.compose.material3.TextButton(
                    onClick = {
                        showGiveUpDialog = false
                        applyTaskAction(TaskAction.MARK_GIVE_UP)
                        Toast.makeText(activity, R.string.toast_give_up, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showGiveUpDialog = false }) {
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
