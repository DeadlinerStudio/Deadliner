package com.aritxonly.deadliner.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aritxonly.deadliner.notification.FocusTimerForegroundService
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.focus.FocusTimerCommand
import com.deadliner.domain.focus.FocusTimerMode
import com.deadliner.domain.task.TaskState
import kotlinx.coroutines.launch

val LocalFocusTimerEntry = staticCompositionLocalOf<(String, String) -> Unit> { { _, _ -> } }

/** Owns the secondary setup sheet; the running Activity owns no setup state. */
@Composable
fun FocusTimerEntryHost(onOpenTimer: () -> Unit, content: @Composable () -> Unit) {
    val viewModel = KmpV6Runtime.focusTimerViewModel
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val taskState by KmpV6Runtime.taskListViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var show by rememberSaveable { mutableStateOf(false) }
    var selectedUid by rememberSaveable { mutableStateOf("") }
    var selectedTitle by rememberSaveable { mutableStateOf("") }
    var preparing by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var failed by rememberSaveable { mutableStateOf(false) }
    var openAfterDismiss by rememberSaveable { mutableStateOf(false) }

    val openEntry: (String, String) -> Unit = { uid, title ->
        if (state.timer != null) {
            onOpenTimer()
        } else {
            selectedUid = uid
            selectedTitle = title
            failed = false
            show = true
        }
    }
    LaunchedEffect(show) {
        if (show) {
            preparing = true
            KmpV6Runtime.taskListViewModel.initialize()
            val loaded = viewModel.loadNow()
            preparing = false
            if (loaded.timer != null) {
                openAfterDismiss = true
                show = false
            }
        }
    }
    CompositionLocalProvider(LocalFocusTimerEntry provides openEntry) { content() }
    FocusTimerSetupSheet(
        show = show,
        selectedTaskUid = selectedUid,
        selectedTaskTitle = selectedTitle,
        tasks = taskState.tasks.filter { !it.isDeleted && it.state == TaskState.ACTIVE },
        busy = preparing || starting || state.isBusy || state.isLoading,
        error = state.error,
        startFailed = failed,
        onSelectTask = { task -> selectedUid = task.uid; selectedTitle = task.title },
        onStart = { mode, minutes ->
            if (!starting && !preparing && !state.isBusy && selectedUid.isNotBlank()) {
                starting = true
                failed = false
                scope.launch {
                    val result = viewModel.executeNow(FocusTimerCommand.Start(
                        taskUid = selectedUid,
                        mode = mode,
                        plannedDurationSeconds = if (mode == FocusTimerMode.COUNTDOWN) minutes * 60L else null,
                    ))
                    starting = false
                    if (result.timer != null) {
                        FocusTimerForegroundService.start(context)
                        openAfterDismiss = true
                        show = false
                    } else {
                        failed = true
                    }
                }
            }
        },
        onDismissRequest = { if (!starting && !preparing) show = false },
        onDismissFinished = {
            if (openAfterDismiss) {
                openAfterDismiss = false
                onOpenTimer()
            }
        },
    )
}
