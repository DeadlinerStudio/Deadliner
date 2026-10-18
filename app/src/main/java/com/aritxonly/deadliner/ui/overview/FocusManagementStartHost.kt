package com.aritxonly.deadliner.ui.overview

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aritxonly.deadliner.R
import androidx.compose.ui.res.stringResource
import com.aritxonly.deadliner.FocusTimerActivity
import com.aritxonly.deadliner.notification.FocusTimerForegroundService
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.focus.FocusTimerSetupSheet
import com.deadliner.domain.focus.*
import com.deadliner.domain.focus.management.FocusStartTargetOption
import kotlinx.coroutines.*

/** Reuses the existing singleton timer, setup sheet and running Activity; no second timer owner. */
@Composable
internal fun FocusManagementStartHost(
    targets: List<FocusStartTargetOption>, loading: Boolean, loadFailed: Boolean, retry: () -> Unit,
    content: @Composable (FocusTimerSnapshot?, Boolean, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val vm = KmpV6Runtime.focusTimerViewModel
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var show by rememberSaveable { mutableStateOf(false) }
    var selectedUid by rememberSaveable { mutableStateOf("") }
    var selectedKind by rememberSaveable { mutableStateOf(FocusTargetKind.TASK.name) }
    var starting by remember { mutableStateOf(false) }
    var failed by rememberSaveable { mutableStateOf(false) }
    var openAfterDismiss by remember { mutableStateOf(false) }
    fun openTimer() { context.startActivity(FocusTimerActivity.newIntent(context, "", "")) }
    val selected = targets.firstOrNull { it.target.uid == selectedUid && it.target.kind.name == selectedKind }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.IO) { vm.loadNow() }
            while (isActive) { delay(1_000); if (vm.uiState.value.timer != null) vm.tick() }
        }
    }
    // Another entry can start while the setup sheet is visible. Preserve that session and open it.
    LaunchedEffect(state.timer?.sessionUid, show) {
        if (show && state.timer != null && !starting) { openAfterDismiss = true; show = false }
    }
    content(state.timer, starting || state.isBusy || state.isLoading || loading) {
        if (state.timer != null) openTimer() else {
            selectedUid = ""; failed = false; show = true
            if (loadFailed) retry()
        }
    }
    FocusTimerSetupSheet(
        show = show, selectedTaskUid = selected?.target?.uid.orEmpty(), selectedTaskTitle = selected?.title.orEmpty(),
        tasks = emptyList(), busy = loading || starting || state.isBusy,
        error = if (loadFailed) stringResource(R.string.focus_hub_load_failed) else state.error,
        startFailed = failed,
        onSelectTask = {}, selectedIsHabit = selectedKind == FocusTargetKind.HABIT.name,
        onStart = { mode, minutes ->
            val option = selected
            if (!starting && option != null) {
                starting = true; failed = false
                scope.launch {
                    try {
                        val planned = if (mode == FocusTimerMode.COUNTDOWN) minutes * 60L else null
                        val command = if (option.target.kind == FocusTargetKind.HABIT) FocusTimerCommand.StartHabit(option.target.uid, mode, planned)
                            else FocusTimerCommand.Start(option.target.uid, mode, planned)
                        val result = withContext(Dispatchers.IO) { vm.executeNow(command) }
                        if (result.timer != null) {
                            FocusTimerForegroundService.start(context)
                            openAfterDismiss = true; show = false
                        } else failed = true
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        failed = true
                    } finally { starting = false }
                }
            }
        },
        onDismissRequest = { if (!starting) show = false },
        onDismissFinished = { if (openAfterDismiss) { openAfterDismiss = false; openTimer() } },
        managementTargets = targets,
        onSelectManagementTarget = { option -> selectedUid = option.target.uid; selectedKind = option.target.kind.name },
        onRetryTargets = retry,
    )
}
