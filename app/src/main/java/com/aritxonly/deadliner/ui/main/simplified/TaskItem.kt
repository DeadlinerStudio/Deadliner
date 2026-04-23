package com.aritxonly.deadliner.ui.main.simplified

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.GlobalUtils.refreshCount
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.TaskStateAction
import com.aritxonly.deadliner.model.updateNoteWithDate
import com.aritxonly.deadliner.ui.main.DDLItemCardSwipeable
import com.aritxonly.deadliner.ui.main.HabitItemCardSimplified
import com.aritxonly.deadliner.ui.main.shared.computeProgress
import com.aritxonly.deadliner.ui.iconResource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max

@Composable
fun AnimatedItem(
    item: DDLItem,
    index: Int,
    content:  @Composable () -> Unit
) {
    var visible by rememberSaveable(item.id) { mutableStateOf(false) }

    LaunchedEffect(item.id) {
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
    item: DDLItem,
    activity: MainActivity,
    applyTaskAction: (TaskStateAction, Boolean) -> Unit,
    celebrate: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPressSelect: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null,
    onAbandonDialogVisibilityChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current

    val startTime = GlobalUtils.parseDateTime(item.startTime)
    val endTime = GlobalUtils.parseDateTime(item.endTime)
    val now = LocalDateTime.now()

    val remainingTimeText =
        if (item.state == DDLState.ABANDONED)
            stringResource(R.string.abandoned)
        else if (!item.state.isCompletedFamily())
            GlobalUtils.buildRemainingTime(
                context,
                startTime,
                endTime,
                true,
                now
            )
        else stringResource(R.string.completed)

    val progress = computeProgress(startTime, endTime, now)
    val status = if (item.state.isCompletedFamily() || item.state.isAbandonedFamily()) {
        DDLStatus.COMPLETED
    } else {
        DDLStatus.calculateStatus(startTime, endTime, now, false)
    }

    val primaryAction = when (item.state) {
        DDLState.ACTIVE -> TaskStateAction.MARK_COMPLETE
        DDLState.COMPLETED, DDLState.ABANDONED -> TaskStateAction.RESTORE_ACTIVE
        else -> null
    }
    val secondaryAction = when (item.state) {
        DDLState.ACTIVE -> TaskStateAction.MARK_GIVE_UP
        DDLState.COMPLETED, DDLState.ABANDONED -> TaskStateAction.MARK_ARCHIVE
        else -> null
    }
    val primaryIcon = when (primaryAction) {
        TaskStateAction.RESTORE_ACTIVE -> iconResource(R.drawable.ic_back)
        else -> iconResource(R.drawable.ic_done)
    }
    val secondaryIcon = when (secondaryAction) {
        TaskStateAction.MARK_ARCHIVE -> iconResource(R.drawable.ic_archiving)
        TaskStateAction.MARK_GIVE_UP -> iconResource(R.drawable.ic_flag)
        else -> iconResource(R.drawable.ic_delete)
    }

    DDLItemCardSwipeable(
        title = item.name,
        remainingTimeAlt = remainingTimeText,
        note = item.note,
        progress = progress,
        isStarred = item.isStared,
        status = status,
        useDisabledCompletedStyle = item.state.isAbandonedFamily(),
        onClick = {
            val intent = DeadlineDetailActivity.newIntent(context, item)
            activity.startActivity(intent)
        },
        onComplete = {
            GlobalUtils.triggerVibration(activity, 100)
            val action = primaryAction ?: return@DDLItemCardSwipeable
            applyTaskAction(action, true)
            if (action == TaskStateAction.MARK_COMPLETE) {
                celebrate()
                Toast.makeText(activity, R.string.toast_finished, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, R.string.toast_restored_active, Toast.LENGTH_SHORT).show()
            }
        },
        onDelete = {
            GlobalUtils.triggerVibration(activity, 200)
            val action = secondaryAction ?: return@DDLItemCardSwipeable
            if (action == TaskStateAction.MARK_GIVE_UP) {
                onAbandonDialogVisibilityChange(true)
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.confirm_give_up_title)
                    .setMessage(R.string.confirm_give_up_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.accept) { _, _ ->
                        applyTaskAction(action, true)
                        Toast.makeText(activity, R.string.toast_give_up, Toast.LENGTH_SHORT).show()
                    }
                    .setOnDismissListener {
                        onAbandonDialogVisibilityChange(false)
                    }
                    .show()
            } else {
                applyTaskAction(action, true)
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
}

@Composable
fun HabitItem(
    item: DDLItem,
    onRefresh: () -> Unit,
    updateDDL: (DDLItem) -> Unit,
    onCheckInFailed: () -> Unit = {},
    onCheckInSuccess: (DDLItem, HabitMetaData) -> Unit = { i, m -> },
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPressSelect: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null
) {
    val now = LocalDateTime.now()
    val habitMeta = remember(item.note) { GlobalUtils.parseHabitMetaData(item.note) }

    LaunchedEffect(item.id, habitMeta.refreshDate) {
        refreshCount(item, habitMeta) {
            onRefresh()
        }
    }

    val startTime = GlobalUtils.safeParseDateTime(item.startTime)
    val endTime = GlobalUtils.safeParseDateTime(item.endTime)

    // —— 频率/总计描述 —— //
    val freqAndTotalText = when (habitMeta.frequencyType) {
        DeadlineFrequency.DAILY ->
            if (habitMeta.total == 0)
                stringResource(R.string.daily_frequency, habitMeta.frequency)
            else
                stringResource(
                    R.string.daily_frequency_with_total,
                    habitMeta.frequency,
                    habitMeta.total
                )

        DeadlineFrequency.WEEKLY ->
            if (habitMeta.total == 0)
                stringResource(R.string.weekly_frequency, habitMeta.frequency)
            else
                stringResource(
                    R.string.weekly_frequency_with_total,
                    habitMeta.frequency,
                    habitMeta.total
                )

        DeadlineFrequency.MONTHLY ->
            if (habitMeta.total == 0)
                stringResource(R.string.monthly_frequency, habitMeta.frequency)
            else
                stringResource(
                    R.string.monthly_frequency_with_total,
                    habitMeta.frequency,
                    habitMeta.total
                )

        DeadlineFrequency.TOTAL ->
            if (habitMeta.total == 0)
                stringResource(R.string.total_frequency_persistent)
            else
                stringResource(R.string.total_frequency_count, habitMeta.total)
    }

    val remainingText = if (endTime != GlobalUtils.timeNull) {
        val duration = Duration.between(now, endTime)
        val days = duration.toDays()
        if (days < 0)
            stringResource(R.string.ddl_overdue_short)
        else
            stringResource(R.string.remaining_days_arg, days)
    } else ""

    val (count, total) =
        if (habitMeta.total > 0) {
            item.habitTotalCount to habitMeta.total
        } else {
            val denom = max(1, habitMeta.frequency)
            item.habitCount to denom
        }

    val status = remember(startTime, endTime, now, item.isCompleted) {
        DDLStatus.calculateStatus(
            startTime = startTime,
            endTime = if (endTime == GlobalUtils.timeNull) null else endTime,
            now = now,
            isCompleted = item.isCompleted || // 已完成或累计达到总次数都算完成
                    (habitMeta.total != 0 && item.habitTotalCount >= habitMeta.total)
        )
    }

    val progress = computeProgress(startTime, endTime, now)

    HabitItemCardSimplified(
        title = item.name,
        habitCount = count,
        habitTotalCount = total,
        freqAndTotalText = freqAndTotalText,
        remainingText = remainingText,
        isStarred = item.isStared,
        status = status,
        progressTime = progress,
        onCheckIn = {
            val completedDates: Set<LocalDate> =
                habitMeta.completedDates.map { LocalDate.parse(it) }.toSet()

            val canCheckIn = (habitMeta.total != 0 && (
                    if (habitMeta.frequencyType != DeadlineFrequency.TOTAL) {
                        (item.habitCount < habitMeta.frequency) && (completedDates.size < habitMeta.total)
                    } else true
                    ) && (item.habitTotalCount < habitMeta.total)) || (habitMeta.total == 0)

            val alreadyChecked = when (habitMeta.frequencyType) {
                DeadlineFrequency.TOTAL -> false
                else -> habitMeta.frequency <= item.habitCount
            }
            val canPerformClick = canCheckIn && !alreadyChecked

            if (!canPerformClick) {
                onCheckInFailed()
                return@HabitItemCardSimplified
            }

            val today = LocalDate.now()
            val updatedNote = updateNoteWithDate(item, today)

            val updatedHabit = item.copy(
                note = updatedNote,
                habitCount = item.habitCount + 1,
                habitTotalCount = item.habitTotalCount + 1
            )

            onCheckInSuccess(item, habitMeta)

            updateDDL(updatedHabit)
        },
        selectionMode = selectionMode,
        selected = selected,
        onLongPressSelect = onLongPressSelect,
        onToggleSelect = onToggleSelect,
    )
}
