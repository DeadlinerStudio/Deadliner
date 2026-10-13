package com.aritxonly.deadliner

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.notification.FocusTimerForegroundService
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.focus.FocusTimerScreen
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.deadliner.domain.focus.FocusCompletionKind
import com.deadliner.domain.focus.FocusTimerOutcome
import com.deadliner.domain.task.TaskAction
import kotlinx.coroutines.launch
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class FocusTimerActivity : DeadlinerAppCompatActivity() {
    companion object {
        private const val EXTRA_SESSION_UID = "focus_session_uid"
        private const val EXTRA_TASK_UID = "task_uid"
        private const val EXTRA_TASK_TITLE = "task_title"
        private const val EXTRA_RESOLVE_ACTION = "focus_resolve_action"
        private const val EXTRA_RESOLVE_DELETE = "focus_resolve_delete"
        private const val EXTRA_EXPECTED_SESSION = "focus_expected_session"

        fun newResolutionIntent(context: Context, taskUid: String, taskTitle: String,
            action: TaskAction? = null, delete: Boolean = false): Intent = newIntent(context, taskUid, taskTitle)
            .putExtra(EXTRA_RESOLVE_ACTION, action?.name)
            .putExtra(EXTRA_RESOLVE_DELETE, delete)
            .putExtra(EXTRA_EXPECTED_SESSION, KmpV6Runtime.focusTimerViewModel.uiState.value.timer?.sessionUid)

        fun newResultIntent(context: Context, sessionUid: String): Intent =
            Intent(context, FocusTimerActivity::class.java).putExtra(EXTRA_SESSION_UID, sessionUid)

        fun newIntent(context: Context, taskUid: String, taskTitle: String): Intent =
            Intent(context, FocusTimerActivity::class.java)
                .putExtra(EXTRA_TASK_UID, taskUid)
                .putExtra(EXTRA_TASK_TITLE, taskTitle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()
        super.onCreate(savedInstanceState)
        val requestedTaskUid = intent.getStringExtra(EXTRA_TASK_UID).orEmpty()
        val viewModel = KmpV6Runtime.focusTimerViewModel

        setContent {
            DeadlinerTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                val resolutionAction = intent.getStringExtra(EXTRA_RESOLVE_ACTION)?.let { name -> TaskAction.entries.firstOrNull { it.name == name } }
                val resolutionDelete = intent.getBooleanExtra(EXTRA_RESOLVE_DELETE, false)
                var resolutionVisible by rememberSaveable { mutableStateOf(resolutionDelete || resolutionAction != null) }
                var expectedSessionUid by rememberSaveable { mutableStateOf(intent.getStringExtra(EXTRA_EXPECTED_SESSION)) }
                var loaded by remember { mutableStateOf(false) }
                var finishAndCompleteRequested by remember { mutableStateOf(false) }
                val timer = state.timer

                LaunchedEffect(requestedTaskUid) {
                    viewModel.loadNow()
                    if (expectedSessionUid == null) expectedSessionUid = viewModel.uiState.value.timer?.sessionUid
                    loaded = true
                    intent.getStringExtra(EXTRA_SESSION_UID)?.let(viewModel::selectSession)
                    if (requestedTaskUid.isNotBlank()) viewModel.selectTask(requestedTaskUid)
                }
                LaunchedEffect(timer?.sessionUid, timer?.state) {
                    if (timer == null) FocusTimerForegroundService.stop(this@FocusTimerActivity)
                    else FocusTimerForegroundService.start(this@FocusTimerActivity)
                }
                LaunchedEffect(timer?.sessionUid) {
                    if (timer == null) return@LaunchedEffect
                    while (isActive) {
                        delay(1_000)
                        viewModel.tick()
                    }
                }
                LaunchedEffect(state.lastCompletedSession?.uid, state.lastCompletedSession?.habitRecordedAmount) {
                    if (state.lastCompletedSession?.habitRecordedAmount != null) {
                        KmpV6Runtime.habitListViewModel.loadNow()
                        KmpV6Runtime.refreshNotifications()
                    }
                    if (finishAndCompleteRequested &&
                        state.lastCompletedSession?.completionKind == FocusCompletionKind.FOCUS_AND_TASK_COMPLETED
                    ) {
                        KmpV6Runtime.refreshNotifications()
                        setResult(RESULT_OK)
                        finishAfterTransition()
                    }
                }
                LaunchedEffect(state.lastCompletedTaskState) {
                    KmpV6Runtime.taskListViewModel.loadNow()
                    KmpV6Runtime.refreshNotifications()
                }

                ProvideAdvancedMaterialDialogBlurHost {
                    Box(Modifier.fillMaxSize()) {
                        FocusTimerScreen(
                            state = state,
                            onClose = { finishAfterTransition() },
                            onPause = viewModel::pause,
                            onResume = viewModel::resume,
                            onFinish = viewModel::finish,
                            onFinishAndComplete = {
                                finishAndCompleteRequested = true
                                viewModel.finishAndCompleteTask()
                            },
                            onFinishAndRecordHabit = viewModel::finishAndRecordHabit,
                            onDiscard = viewModel::discard,
                            onAdjustSession = viewModel::adjustSessionDuration,
                            onDeleteSession = viewModel::deleteSession,
                            onCompleteSavedTask = viewModel::completeSavedTask,
                            resolutionVisible = resolutionVisible && loaded,
                            resolutionAction = resolutionAction,
                            resolutionDelete = resolutionDelete,
                            onCancelResolution = { resolutionVisible = false; finishAfterTransition() },
                            onConfirmResolution = {
                                scope.launch {
                                    if (viewModel.uiState.value.isBusy) return@launch
                                    val expected = expectedSessionUid
                                    if (expected == null) {
                                        Toast.makeText(this@FocusTimerActivity, R.string.focus_confirmation_expired, Toast.LENGTH_LONG).show()
                                        resolutionVisible = false
                                        return@launch
                                    }
                                    val next = if (resolutionDelete) viewModel.resolveTaskDeletionNow(requestedTaskUid, expected)
                                        else viewModel.resolveTaskActionNow(requestedTaskUid, requireNotNull(resolutionAction), expected)
                                    if (next.lastOutcome == FocusTimerOutcome.APPLIED || next.lastOutcome == FocusTimerOutcome.NO_OP) {
                                        KmpV6Runtime.taskListViewModel.loadNow()
                                        KmpV6Runtime.refreshNotifications()
                                        resolutionVisible = false
                                        setResult(RESULT_OK)
                                        finishAfterTransition()
                                    } else Toast.makeText(this@FocusTimerActivity,
                                        if (next.lastOutcome == FocusTimerOutcome.CLOCK_ANOMALY) R.string.focus_clock_warning else R.string.focus_confirmation_expired,
                                        Toast.LENGTH_LONG).show()
                                }
                            },
                        )
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
