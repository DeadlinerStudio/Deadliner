package com.aritxonly.deadliner.ui.main.shared

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aritxonly.deadliner.FocusTimerActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DeadlineType
import com.deadliner.domain.task.TaskAction
import com.deadliner.domain.task.TaskActionOutcome
import com.deadliner.domain.task.TaskState
import com.deadliner.viewmodel.habit.HabitListViewModel
import com.deadliner.viewmodel.task.TaskListViewModel
import java.time.Instant
import java.time.LocalDate

@Stable
class MainSelectionActionController(
    private val activity: MainActivity,
    private val taskViewModel: TaskListViewModel,
    private val habitViewModel: HabitListViewModel,
    private val hostState: MainHostState,
) {
    var showDeleteDialog by mutableStateOf(false)

    fun onDoneClick(onCelebrate: () -> Unit = {}) {
        if (hostState.selectedIds.isEmpty()) {
            toast(R.string.please_select_done_first)
            return
        }
        GlobalUtils.triggerVibration(activity, 100)

        if (hostState.selectedPage == DeadlineType.HABIT) {
            val selectedDate = habitViewModel.uiState.value.selectedDate
            if (LocalDate.parse(selectedDate).isAfter(LocalDate.now())) {
                toast(R.string.cannot_check_future)
                return
            }
            hostState.selectedIds.forEach(habitViewModel::toggleHabitOnSelectedDate)
            hostState.clearSelection()
            onCelebrate()
            toast(R.string.toast_finished)
            return
        }

        val tasks = taskViewModel.uiState.value.tasks.associateBy { it.uid }
        var appliedCount = 0
        var blockedTaskUid: String? = null
        var blockedTaskTitle: String? = null
        hostState.selectedIds.forEach { uid ->
            val action = when (tasks[uid]?.state) {
                TaskState.ACTIVE -> TaskAction.MARK_COMPLETE
                TaskState.COMPLETED, TaskState.ABANDONED -> TaskAction.RESTORE_ACTIVE
                else -> null
            }
            if (action != null) {
                when (
                    taskViewModel.applyTaskActionNow(uid, action, Instant.now().toString())
                        .lastActionOutcome
                ) {
                    TaskActionOutcome.APPLIED -> appliedCount++
                    TaskActionOutcome.TIMER_REQUIRES_RESOLUTION -> if (blockedTaskUid == null) {
                        blockedTaskUid = uid
                        blockedTaskTitle = tasks[uid]?.title
                    }
                    else -> Unit
                }
            }
        }
        hostState.clearSelection()
        if (appliedCount > 0) {
            onCelebrate()
            toast(R.string.toast_finished)
        }
        openFocusTimerIfBlocked(blockedTaskUid, blockedTaskTitle)
    }

    fun onArchiveClick() {
        if (hostState.selectedIds.isEmpty()) {
            toast(R.string.please_select_done_first)
            return
        }
        var count = 0
        val tasks = taskViewModel.uiState.value.tasks.associateBy { it.uid }
        hostState.selectedIds.forEach { uid ->
            if (tasks[uid]?.state in setOf(TaskState.COMPLETED, TaskState.ABANDONED)) {
                val outcome = taskViewModel
                    .applyTaskActionNow(uid, TaskAction.MARK_ARCHIVE, Instant.now().toString())
                    .lastActionOutcome
                if (outcome == TaskActionOutcome.APPLIED) count++
            }
        }
        hostState.clearSelection()
        Toast.makeText(activity, activity.getString(R.string.toast_archived, count), Toast.LENGTH_SHORT).show()
    }

    fun onDeleteClick() {
        if (hostState.selectedIds.isEmpty()) {
            toast(R.string.please_select_delete_first)
            return
        }
        GlobalUtils.triggerVibration(activity, 200)
        showDeleteDialog = true
    }

    fun dismissDeleteDialog() {
        showDeleteDialog = false
    }

    fun confirmDeleteSelected() {
        val now = Instant.now().toString()
        when (hostState.selectedPage) {
            DeadlineType.TASK -> {
                val tasks = taskViewModel.uiState.value.tasks.associateBy { it.uid }
                var appliedCount = 0
                var blockedTaskUid: String? = null
                var blockedTaskTitle: String? = null
                hostState.selectedIds.forEach { uid ->
                    when (taskViewModel.deleteTaskNow(uid, now).lastActionOutcome) {
                        TaskActionOutcome.APPLIED -> appliedCount++
                        TaskActionOutcome.TIMER_REQUIRES_RESOLUTION -> if (blockedTaskUid == null) {
                            blockedTaskUid = uid
                            blockedTaskTitle = tasks[uid]?.title
                        }
                        else -> Unit
                    }
                }
                if (appliedCount > 0) toast(R.string.toast_deletion)
                openFocusTimerIfBlocked(blockedTaskUid, blockedTaskTitle)
            }
            DeadlineType.HABIT -> {
                hostState.selectedIds.forEach { habitViewModel.deleteHabitNow(it, now) }
                toast(R.string.toast_deletion)
            }
        }
        hostState.clearSelection()
        showDeleteDialog = false
    }

    private fun openFocusTimerIfBlocked(taskUid: String?, taskTitle: String?) {
        if (taskUid == null) return
        Toast.makeText(
            activity,
            activity.getString(R.string.focus_resolve_before_task_change),
            Toast.LENGTH_LONG,
        ).show()
        activity.startActivity(FocusTimerActivity.newIntent(activity, taskUid, taskTitle.orEmpty()))
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(activity, activity.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun rememberMainSelectionActionController(
    activity: MainActivity,
    taskViewModel: TaskListViewModel,
    habitViewModel: HabitListViewModel,
    hostState: MainHostState,
): MainSelectionActionController = remember(activity, taskViewModel, habitViewModel, hostState) {
    MainSelectionActionController(activity, taskViewModel, habitViewModel, hostState)
}
