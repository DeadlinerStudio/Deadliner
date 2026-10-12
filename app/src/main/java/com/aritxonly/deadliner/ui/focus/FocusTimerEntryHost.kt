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

val LocalHabitFocusTimerEntry = staticCompositionLocalOf<(String, String) -> Unit> { { _, _ -> } }

val LocalFocusTimerEntry = staticCompositionLocalOf<(String, String) -> Unit> { { _, _ -> } }

/** Owns the secondary setup sheet; the running Activity owns no setup state. */
@Composable
fun FocusTimerEntryHost(onOpenTimer: () -> Unit, content: @Composable () -> Unit) {
    val viewModel = KmpV6Runtime.focusTimerViewModel
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val habitState by KmpV6Runtime.habitListViewModel.uiState.collectAsStateWithLifecycle()
    var habitTarget by rememberSaveable { mutableStateOf(false) }
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

    val openEntry: (Boolean, String, String) -> Unit = { isHabit, uid, title ->
        if (state.timer != null) {
            onOpenTimer()
        } else {
            habitTarget = isHabit
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
            KmpV6Runtime.habitListViewModel.initialize()
            val loaded = viewModel.loadNow()
            preparing = false
            if (loaded.timer != null) {
                openAfterDismiss = true
                show = false
            }
        }
    }
    CompositionLocalProvider(LocalFocusTimerEntry provides { uid, title -> openEntry(false, uid, title) },
        LocalHabitFocusTimerEntry provides { uid, title -> openEntry(true, uid, title) }) { content() }
    FocusTimerSetupSheet(
        show = show,
        selectedTaskUid = selectedUid,
        selectedTaskTitle = selectedTitle,
        tasks = taskState.tasks.filter { !it.isDeleted && it.state == TaskState.ACTIVE },
        busy = preparing || starting || state.isBusy || state.isLoading,
        error = state.error,
        startFailed = failed,
        onSelectTask = { task -> habitTarget = false; selectedUid = task.uid; selectedTitle = task.title },
        selectedIsHabit = habitTarget,
        habits = habitState.habits.filter { !it.isDeleted && it.status == com.deadliner.domain.habit.HabitStatus.ACTIVE },
        onSelectHabit = { habit -> habitTarget = true; selectedUid = habit.uid; selectedTitle = habit.name },
        onStart = { mode, minutes ->
            if (!starting && !preparing && !state.isBusy && selectedUid.isNotBlank()) {
                starting = true
                failed = false
                scope.launch {
                    val planned = if (mode == FocusTimerMode.COUNTDOWN) minutes * 60L else null
                    val command = if (habitTarget) FocusTimerCommand.StartHabit(selectedUid, mode, planned)
                        else FocusTimerCommand.Start(selectedUid, mode, planned)
                    val result = viewModel.executeNow(command)
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
