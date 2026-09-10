package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.detail.DeadlineDetailScreen
import com.aritxonly.deadliner.ui.category.rememberActiveCategories
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.deadliner.viewmodel.task.SubtaskCreateConfig

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

        setContent {
            DeadlinerTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val task = state.task
                val categories = rememberActiveCategories(state)
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
                                onApplyTaskAction = { action ->
                                    when (action) {
                                        com.deadliner.domain.task.TaskAction.MARK_COMPLETE -> viewModel.markComplete()
                                        com.deadliner.domain.task.TaskAction.MARK_ARCHIVE -> viewModel.markArchive()
                                        com.deadliner.domain.task.TaskAction.MARK_GIVE_UP -> viewModel.markGiveUp()
                                        com.deadliner.domain.task.TaskAction.RESTORE_ACTIVE -> viewModel.restoreActive()
                                        com.deadliner.domain.task.TaskAction.UNARCHIVE -> viewModel.unarchive()
                                    }
                                },
                                onDelete = viewModel::deleteTask,
                                onAddSubtask = { viewModel.addSubtask(SubtaskCreateConfig(it)) },
                                onToggleSubtask = viewModel::toggleSubtask,
                                onDeleteSubtask = viewModel::deleteSubtask,
                                onUpdateSubtask = viewModel::updateSubtaskContent,
                                onCalendarEventCreated = viewModel::setCalendarEventId,
                                finishActivity = { finishAfterTransition() },
                            )
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
