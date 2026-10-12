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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class FocusTimerActivity : DeadlinerAppCompatActivity() {
    companion object {
        private const val EXTRA_SESSION_UID = "focus_session_uid"
        private const val EXTRA_TASK_UID = "task_uid"
        private const val EXTRA_TASK_TITLE = "task_title"

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
                var finishAndCompleteRequested by remember { mutableStateOf(false) }
                val timer = state.timer

                LaunchedEffect(requestedTaskUid) {
                    viewModel.loadNow()
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
