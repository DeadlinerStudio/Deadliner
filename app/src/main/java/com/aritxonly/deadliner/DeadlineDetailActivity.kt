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
import androidx.compose.runtime.setValue
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
    private var closing by androidx.compose.runtime.mutableStateOf(false)
    private var sourceBounds by androidx.compose.runtime.mutableStateOf<FloatArray?>(null)
    private var sourceSnapshot by androidx.compose.runtime.mutableStateOf<com.aritxonly.deadliner.ui.detail.TaskDetailSnapshot?>(null)
    private var sourceCornerRadiusPx = 0f
    private var animateEntrance = true
    private var motionMounted = false

    override fun finish() {
        if (motionMounted) closing = true else super.finish()
    }

    override fun finishAfterTransition() = finish()

    private fun invalidateSource() {
        sourceBounds = null
        sourceSnapshot = null
    }

    private fun completeClose() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val EXTRA_TASK_UID = "task_uid"

        fun newIntent(context: Context, taskUid: String): Intent =
            Intent(context, DeadlineDetailActivity::class.java).putExtra(EXTRA_TASK_UID, taskUid)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()
        super.onCreate(savedInstanceState)
        animateEntrance = savedInstanceState == null
        sourceBounds = if (animateEntrance) intent.getFloatArrayExtra(
            com.aritxonly.deadliner.ui.detail.TaskDetailSourceExtra) else null
        sourceSnapshot = com.aritxonly.deadliner.ui.detail.TaskDetailSnapshotStore.take(
            intent.getStringExtra(com.aritxonly.deadliner.ui.detail.TaskDetailSnapshotExtra),
        ).takeIf { animateEntrance && sourceBounds != null }
        sourceCornerRadiusPx = intent.getFloatExtra(
            com.aritxonly.deadliner.ui.detail.TaskDetailCornerExtra,
            resources.getDimension(R.dimen.item_corner_radius),
        )
        val taskUid = intent.getStringExtra(EXTRA_TASK_UID)
            ?: throw IllegalArgumentException("Missing task UID")
        val viewModel = KmpV6Runtime.createTaskDetailViewModel(taskUid)
        viewModel.initialize()
        val focusLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                invalidateSource()
                finishAfterTransition()
            } else viewModel.initialize()
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
                    // This action removes/reorders the source row; fade out rather than target it.
                    invalidateSource()
                    finishAfterTransition()
                }

                TaskActionOutcome.TIMER_REQUIRES_RESOLUTION -> {
                    Toast.makeText(this, R.string.focus_resolve_before_task_change, Toast.LENGTH_LONG).show()
                    focusLauncher.launch(FocusTimerActivity.newResolutionIntent(this, taskUid, result.task?.title.orEmpty(), action))
                }

                else -> Toast.makeText(this, R.string.focus_task_action_failed, Toast.LENGTH_SHORT).show()
            }
        }

        fun deleteTask() {
            val result = KmpV6Runtime.deleteTask(taskUid)
            when (result.outcome) {
                TaskActionOutcome.APPLIED -> {
                    Toast.makeText(this, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                    // This action removes/reorders the source row; fade out rather than target it.
                    invalidateSource()
                    finishAfterTransition()
                }

                TaskActionOutcome.TIMER_REQUIRES_RESOLUTION -> {
                    Toast.makeText(this, R.string.focus_resolve_before_task_change, Toast.LENGTH_LONG).show()
                    focusLauncher.launch(FocusTimerActivity.newResolutionIntent(this, taskUid, result.task?.title.orEmpty(), delete = true))
                }

                else -> Toast.makeText(this, R.string.focus_task_action_failed, Toast.LENGTH_SHORT).show()
            }
        }

        fun postpone(delay: com.deadliner.domain.task.TaskDeadlineDelay) {
            val result = runCatching { KmpV6Runtime.postponeTask(taskUid, delay) }.getOrNull()
            val message = when (result?.outcome) {
                com.deadliner.domain.task.TaskDeadlineOutcome.APPLIED -> {
                    invalidateSource()
                    viewModel.initialize()
                    R.string.detail_postponed
                }
                com.deadliner.domain.task.TaskDeadlineOutcome.INVALID_DEADLINE -> R.string.detail_postpone_invalid_deadline
                com.deadliner.domain.task.TaskDeadlineOutcome.INVALID_DELAY -> R.string.detail_postpone_limit
                else -> R.string.detail_postpone_failed
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        setContent {
            DeadlinerTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val task = state.task
                val categories = rememberActiveCategories(state)
                val editorEntry = com.aritxonly.deadliner.ui.editor.LocalDeadlineEditorEntry.current
                FocusTimerEntryHost(onOpenTimer = {
                    focusLauncher.launch(FocusTimerActivity.newIntent(this@DeadlineDetailActivity, "", ""))
                }) {
                    val focusEntry = com.aritxonly.deadliner.ui.focus.LocalFocusTimerEntry.current
                    ProvideAdvancedMaterialDialogBlurHost {
                        com.aritxonly.deadliner.ui.detail.TaskDetailMotion(
                            source = sourceBounds,
                            sourceSnapshot = sourceSnapshot,
                            sourceCornerRadiusPx = sourceCornerRadiusPx,
                            closing = closing,
                            ready = task != null,
                            animateEntrance = animateEntrance,
                            onClose = { finish() },
                            onClosed = ::completeClose,
                        ) {
                            if (task != null) {
                                DeadlineDetailScreen(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    applicationContext = applicationContext,
                                    activityContext = this@DeadlineDetailActivity,
                                    deadline = task,
                                    onClose = { finish() },
                                    onToggleStar = { starred ->
                                        invalidateSource()
                                        viewModel.setStarred(starred)
                                    },
                                    categories = categories,
                                    onEdit = {
                                        editorEntry.open(AddDDLActivity.newEditIntent(this@DeadlineDetailActivity, task.uid)) {
                                            invalidateSource()
                                            viewModel.initialize()
                                        }
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
                                    onPostpone = ::postpone,
                                )
                            }
                        }
                    }
                }
            }
        }
        motionMounted = true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        sourceBounds = null // The source card no longer has valid window coordinates after resizing.
        sourceSnapshot = null
        enableEdgeToEdgeForAllDevices()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        sourceBounds = null
        sourceSnapshot = null
        enableEdgeToEdgeForAllDevices()
    }
}
