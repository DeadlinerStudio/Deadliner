package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.focus.FocusTimerEntryHost
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.detail.DeadlineDetailScreen
import com.aritxonly.deadliner.ui.category.rememberActiveCategories
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.deadliner.viewmodel.task.SubtaskCreateConfig
import com.deadliner.domain.task.TaskAction
import com.deadliner.domain.task.TaskActionOutcome

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class DeadlineDetailActivity : DeadlinerAppCompatActivity() {
    companion object {
        private const val EXTRA_TASK_UID = "task_uid"

        fun newIntent(context: Context, taskUid: String): Intent =
            Intent(context, DeadlineDetailActivity::class.java).putExtra(EXTRA_TASK_UID, taskUid)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()
        super.onCreate(savedInstanceState)
        val taskUid = intent.getStringExtra(EXTRA_TASK_UID)
            ?: throw IllegalArgumentException("Missing task UID")
        val viewModel = KmpV6Runtime.createTaskDetailViewModel(taskUid)
        viewModel.initialize()
        val focusLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) finishAfterTransition() else viewModel.initialize()
        }

        fun showActionResult(action: TaskAction) {
            val result = KmpV6Runtime.applyTaskAction(taskUid, action)
            when (result.outcome) {
                TaskActionOutcome.APPLIED -> {
                    val message = when (action) {
                        TaskAction.MARK_COMPLETE -> R.string.toast_finished
                        TaskAction.MARK_ARCHIVE -> R.string.toast_archived
                        TaskAction.MARK_GIVE_UP -> R.string.toast_give_up
                        TaskAction.RESTORE_ACTIVE -> R.string.toast_restored_active
                        TaskAction.UNARCHIVE -> R.string.toast_unarchived
                    }
                    val text = if (action == TaskAction.MARK_ARCHIVE) getString(message, 1) else getString(message)
                    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
                    finishAfterTransition()
                }

                TaskActionOutcome.TIMER_REQUIRES_RESOLUTION -> {
                    Toast.makeText(this, R.string.focus_resolve_before_task_change, Toast.LENGTH_LONG).show()
                    focusLauncher.launch(FocusTimerActivity.newIntent(this, taskUid, result.task?.title.orEmpty()))
                }

                else -> Toast.makeText(this, R.string.focus_task_action_failed, Toast.LENGTH_SHORT).show()
            }
        }

        fun deleteTask() {
            val result = KmpV6Runtime.deleteTask(taskUid)
            when (result.outcome) {
                TaskActionOutcome.APPLIED -> {
                    Toast.makeText(this, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                    finishAfterTransition()
                }

                TaskActionOutcome.TIMER_REQUIRES_RESOLUTION -> {
                    Toast.makeText(this, R.string.focus_resolve_before_task_change, Toast.LENGTH_LONG).show()
                    focusLauncher.launch(FocusTimerActivity.newIntent(this, taskUid, result.task?.title.orEmpty()))
                }

                else -> Toast.makeText(this, R.string.focus_task_action_failed, Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            DeadlinerTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val task = state.task
                val categories = rememberActiveCategories(state)
                FocusTimerEntryHost(onOpenTimer = {
                    focusLauncher.launch(FocusTimerActivity.newIntent(this@DeadlineDetailActivity, "", ""))
                }) {
                    val focusEntry = com.aritxonly.deadliner.ui.focus.LocalFocusTimerEntry.current
                    ProvideAdvancedMaterialDialogBlurHost {
                        Box(Modifier.fillMaxSize()) {
                            if (task != null) {
                                DeadlineDetailScreen(
                                    applicationContext = applicationContext,
                                    activityContext = this@DeadlineDetailActivity,
                                    deadline = task,
                                    onClose = { finish() },
                                    onToggleStar = viewModel::setStarred,
                                    categories = categories,
                                    onSetCategory = { categoryUid ->
                                        KmpV6Runtime.updateTaskCategory(task, categoryUid)
                                        viewModel.initialize()
                                    },
                                    onManageCategories = {
                                        startActivity(CategoryManagementActivity.newIntent(this@DeadlineDetailActivity))
                                    },
                                    onOpenFocusTimer = {
                                        focusEntry(task.uid, task.title)
                                    },
                                    onApplyTaskAction = ::showActionResult,
                                    onDelete = ::deleteTask,
                                    onAddSubtask = { viewModel.addSubtask(SubtaskCreateConfig(it)) },
                                    onToggleSubtask = viewModel::toggleSubtask,
                                    onDeleteSubtask = viewModel::deleteSubtask,
                                    onUpdateSubtask = viewModel::updateSubtaskContent,
                                    onCalendarEventCreated = viewModel::setCalendarEventId,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdgeForAllDevices()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        enableEdgeToEdgeForAllDevices()
    }
}
