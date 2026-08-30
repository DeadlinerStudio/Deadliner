package com.aritxonly.deadliner.ui.main.shared

import android.widget.Toast
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.EditDDLFragment
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.data.HabitViewModel
import com.aritxonly.deadliner.data.MainViewModel
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.TaskStateAction
import java.time.LocalDate

@Stable
class MainSelectionActionController(
    private val activity: MainActivity,
    private val vm: MainViewModel,
    private val habitVm: HabitViewModel,
    private val hostState: MainHostState,
) {
    var showDeleteDialog by mutableStateOf(false)
    var habitReminderTargetId by mutableStateOf<Long?>(null)

    private fun refresh() {
        vm.loadData(hostState.selectedPage)
        habitVm.refresh()
    }

    fun clearHabitReminderTarget() {
        habitReminderTargetId = null
    }

    fun onDoneClick(ddlList: List<DDLItem>, onCelebrate: () -> Unit = {}) {
        if (hostState.selectedIds.isEmpty()) {
            toast(R.string.please_select_done_first)
            return
        }
        GlobalUtils.triggerVibration(activity, 100)

        if (hostState.selectedPage == DeadlineType.HABIT) {
            val selectedDate = habitVm.selectedDate.value
            if (selectedDate.isAfter(LocalDate.now())) {
                toast(R.string.cannot_check_future)
                return
            }

            val habitRepo = HabitRepository()
            val idsToUpdate = hostState.selectedIds.toList()
            idsToUpdate.forEach { id ->
                val item = ddlList.find { it.id == id } ?: return@forEach
                if (item.type != DeadlineType.HABIT) return@forEach
                val habit = habitRepo.getHabitByDdlId(item.id) ?: return@forEach
                habitRepo.toggleRecord(habit.id, selectedDate)
            }
            refresh()
            hostState.clearSelection()
            onCelebrate()
            toast(R.string.toast_finished)
            return
        }

        val idsToUpdate = hostState.selectedIds.toList()
        idsToUpdate.forEach { id ->
            val item = ddlList.find { it.id == id } ?: return@forEach
            val action = when (item.state) {
                DDLState.ACTIVE -> TaskStateAction.MARK_COMPLETE
                DDLState.COMPLETED, DDLState.ABANDONED -> TaskStateAction.RESTORE_ACTIVE
                else -> null
            }
            if (action != null) {
                DDLRepository().applyTaskAction(item.id, action, confirmed = true)
            }
        }
        refresh()
        hostState.clearSelection()
        toast(R.string.toast_finished)
    }

    fun onArchiveClick(ddlList: List<DDLItem>) {
        if (hostState.selectedIds.isEmpty()) {
            toast(R.string.please_select_done_first)
            return
        }

        var count = 0
        val idsToUpdate = hostState.selectedIds.toList()
        idsToUpdate.forEach { id ->
            val item = ddlList.firstOrNull { it.id == id } ?: return@forEach
            if (item.state.canManualArchive()) {
                DDLRepository().applyTaskAction(item.id, TaskStateAction.MARK_ARCHIVE, confirmed = true)
                count++
            }
        }

        refresh()
        hostState.clearSelection()
        Toast.makeText(activity, activity.getString(R.string.toast_archived, count), Toast.LENGTH_SHORT).show()
    }

    fun onReminderClick(ddlList: List<DDLItem>) {
        if (hostState.selectedIds.isEmpty()) {
            toast(R.string.please_select_edit_first)
            return
        }
        val firstId = hostState.selectedIds.first()
        val item = ddlList.firstOrNull { it.id == firstId }
        if (item == null) {
            toast(R.string.please_select_edit_first)
            return
        }
        habitReminderTargetId = item.id
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
        val idsToDelete = hostState.selectedIds.toList()
        idsToDelete.forEach { id ->
            DDLRepository().deleteDDL(id)
            HabitRepository().deleteHabitByDdlId(id)
            DeadlineAlarmScheduler.cancelAlarm(activity.applicationContext, id)
        }

        refresh()
        hostState.clearSelection()
        showDeleteDialog = false
        toast(R.string.toast_deletion)
    }

    fun onEditClick(ddlList: List<DDLItem>) {
        if (hostState.selectedIds.isEmpty()) {
            toast(R.string.please_select_edit_first)
            return
        }

        val firstId = hostState.selectedIds.first()
        val clickedItem = ddlList.firstOrNull { it.id == firstId }
        if (clickedItem == null) {
            toast(R.string.please_select_edit_first)
            return
        }

        val editDialog = EditDDLFragment(clickedItem) { updatedDDL ->
            DDLRepository().updateDDL(updatedDDL)
            refresh()
            hostState.clearSelection()
        }
        editDialog.show(activity.supportFragmentManager, "EditDDLFragment")
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(activity, activity.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}

@androidx.compose.runtime.Composable
fun rememberMainSelectionActionController(
    activity: MainActivity,
    vm: MainViewModel,
    habitVm: HabitViewModel,
    hostState: MainHostState,
): MainSelectionActionController = remember(activity, vm, habitVm, hostState) {
    MainSelectionActionController(
        activity = activity,
        vm = vm,
        habitVm = habitVm,
        hostState = hostState,
    )
}
