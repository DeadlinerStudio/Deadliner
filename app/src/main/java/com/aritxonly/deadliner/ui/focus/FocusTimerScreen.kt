package com.aritxonly.deadliner.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material3.Icon
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import top.yukonga.miuix.kmp.basic.TextButton
import com.aritxonly.deadliner.ui.base.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.iconResource
import com.deadliner.domain.focus.FocusSession
import com.deadliner.domain.focus.FocusTimerSnapshot
import com.deadliner.domain.focus.FocusTimerState
import com.deadliner.viewmodel.focus.FocusTimerUiState
import com.aritxonly.deadliner.ui.settings.SettingsSection
import androidx.compose.foundation.layout.ColumnScope
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    state: FocusTimerUiState,
    onClose: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onFinishAndComplete: () -> Unit,
    onDiscard: () -> Unit,
    onAdjustSession: (String, Long) -> Unit,
    onDeleteSession: (String) -> Unit,
    onFinishAndRecordHabit: (String, Int?, String?) -> Unit = { _, _, _ -> },
    onCompleteSavedTask: (String) -> Unit = {},
    resolutionVisible: Boolean = false,
    resolutionAction: com.deadliner.domain.task.TaskAction? = null,
    resolutionDelete: Boolean = false,
    onCancelResolution: () -> Unit = {},
    onConfirmResolution: () -> Unit = {},
) {
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<FocusSession?>(null) }
    var deletingSession by remember { mutableStateOf<FocusSession?>(null) }
    var adjustedSeconds by rememberSaveable { mutableStateOf("") }
    var recordSessionUid by rememberSaveable { mutableStateOf<String?>(null) }
    var recordExpectedVersion by rememberSaveable { mutableStateOf<String?>(null) }
    val recordContext = state.habitRecordContext
    fun recordHabit(uid: String) {
        val context = recordContext ?: return
        if (context.trackingMode == com.deadliner.domain.habit.HabitTrackingMode.QUANTITY) {
            recordExpectedVersion = context.habitUpdatedAt
            recordSessionUid = uid
        } else onFinishAndRecordHabit(uid, null, context.habitUpdatedAt)
    }
    val timer = state.timer
    val listState = rememberLazyListState()

    HabitFocusRecordDialog(state = state, sessionUid = recordSessionUid, expectedVersion = recordExpectedVersion,
        onDismiss = { recordSessionUid = null }, onConfirm = onFinishAndRecordHabit)

    AdaptiveMaterialScaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MiuixTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = stringResource(R.string.focus_timer_title),
                navigationIcon = {
                    TopBarIconButton(onClick = onClose) {
                        Icon(
                            painter = iconResource(R.drawable.msr_close_wght400),
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                actions = {
                    if (timer != null) TopBarIconButton(
                        onClick = { showDiscardDialog = true }, enabled = !state.isBusy) {
                        Icon(iconResource(R.drawable.msr_delete_wght400),
                            contentDescription = stringResource(R.string.focus_discard))
                    }
                },
                mode = TopAppBarStyle.SMALL,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().syncTopBarMaterialScroll(listState),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            state.error?.let { message ->
                item { InlineMessage(message, isError = true) }
            }
            if (state.selectedTarget?.kind == com.deadliner.domain.focus.FocusTargetKind.HABIT && state.lastOutcome in listOf(com.deadliner.domain.focus.FocusTimerOutcome.HABIT_NOT_FOUND,
                com.deadliner.domain.focus.FocusTimerOutcome.HABIT_NOT_ACTIVE, com.deadliner.domain.focus.FocusTimerOutcome.HABIT_NOT_DUE,
                com.deadliner.domain.focus.FocusTimerOutcome.HABIT_INVALID_AMOUNT, com.deadliner.domain.focus.FocusTimerOutcome.INVALID_STATE)) {
                item { InlineMessage(stringResource(R.string.habit_focus_record_failed), isError = true) }
            }
            state.warning?.let { warning ->
                item {
                    InlineMessage(
                        stringResource(focusWarningResource(warning)),
                        isError = false,
                    )
                }
            }

            item {
                if (timer != null) {
                    ActiveTimerContent(
                        timer = timer,
                        busy = state.isBusy,
                        onPause = onPause,
                        onResume = onResume,
                        onFinish = onFinish,
                        onFinishAndComplete = {
                            if (timer.target?.kind == com.deadliner.domain.focus.FocusTargetKind.HABIT) recordHabit(timer.sessionUid)
                            else onFinishAndComplete()
                        },
                    )
                } else {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(if (state.isLoading) R.string.focus_loading_task else R.string.focus_session_finished),
                            style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.Medium))
                        state.lastCompletedSession?.let {
                            Text(formatDuration(it.durationSeconds),
                                style = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.SemiBold))
                        }
                        val saved = state.lastCompletedSession
                        if (saved != null && state.canCompleteLastTask) {
                            TextButton(text = stringResource(R.string.focus_complete_saved_task), enabled = !state.isBusy,
                                onClick = { onCompleteSavedTask(saved.uid) })
                        } else if (state.lastCompletedTaskState == com.deadliner.domain.task.TaskState.COMPLETED ||
                            state.lastCompletedTaskState == com.deadliner.domain.task.TaskState.ARCHIVED) {
                            Text(stringResource(R.string.focus_saved_task_completed), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                        if (saved?.boundTarget?.kind == com.deadliner.domain.focus.FocusTargetKind.HABIT &&
                            saved.habitRecordedAmount == null && recordSessionUid == null && recordContext?.canRecord == true) {
                            TextButton(text = stringResource(R.string.habit_focus_record), enabled = !state.isBusy,
                                onClick = { recordHabit(saved.uid) })
                        }
                        FocusRoundAction(icon = R.drawable.msr_close_wght400,
                            label = stringResource(R.string.close), enabled = true, onClick = onClose)
                    }
                }
            }

            if (state.selectedTarget != null || state.selectedTaskUid != null) {
                item {
                    FocusSummaryCard(
                        taskTitle = state.selectedTaskTitle.orEmpty(),
                        todaySeconds = state.selectedTaskTodayDurationSeconds,
                        totalSeconds = state.selectedTaskTotalDurationSeconds,
                    )
                }
            }

            item {
                SettingsSection(topLabel = stringResource(R.string.focus_recent_sessions)) {
                    val sessions = state.selectedTaskRecentSessions.take(5)
                    if (sessions.isEmpty()) {
                        Text(stringResource(R.string.focus_no_sessions), modifier = Modifier.padding(18.dp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    } else {
                        sessions.forEach { session ->
                            SessionRow(
                                session = session,
                                onAdjust = {
                                    adjustedSeconds = session.durationSeconds.toString()
                                    editingSession = session
                                },
                                onDelete = { deletingSession = session },
                            )
                        }
                    }
                }
            }
        }
    }

    val resolutionLabel = stringResource(when {
        resolutionDelete -> R.string.focus_resolve_delete
        resolutionAction == com.deadliner.domain.task.TaskAction.MARK_ARCHIVE -> R.string.focus_resolve_archive
        resolutionAction == com.deadliner.domain.task.TaskAction.MARK_GIVE_UP -> R.string.focus_resolve_abandon
        else -> R.string.focus_resolve_complete
    })
    AlertDialog(show = resolutionVisible, onDismissRequest = onCancelResolution,
        miuixTitle = resolutionLabel, miuixSummary = stringResource(R.string.focus_resolve_joint_message),
        confirmButton = { Button(onClick = onConfirmResolution, enabled = !state.isBusy) { Text(resolutionLabel) } },
        dismissButton = { TextButton(text = stringResource(R.string.cancel), enabled = !state.isBusy, onClick = onCancelResolution) })

    AlertDialog(
        show = showDiscardDialog,
        onDismissRequest = { showDiscardDialog = false },
        miuixTitle = stringResource(R.string.focus_discard_confirm_title),
        miuixSummary = stringResource(R.string.focus_discard_confirm_message),
        confirmButton = {
            Button(onClick = {
                showDiscardDialog = false
                onDiscard()
            }) { Text(stringResource(R.string.accept)) }
        },
        dismissButton = {
            Button(onClick = { showDiscardDialog = false }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )

    val sessionToEdit = editingSession
    AlertDialog(
        show = sessionToEdit != null,
        onDismissRequest = { editingSession = null },
        miuixTitle = stringResource(R.string.focus_adjust_session_title),
        text = {
            OutlinedTextField(
                value = adjustedSeconds,
                onValueChange = { adjustedSeconds = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                miuixLabel = stringResource(R.string.focus_duration_seconds),
            )
        },
        renderTextContentInMiuix = true,
        confirmButton = {
            Button(
                onClick = {
                    val seconds = adjustedSeconds.toLongOrNull()
                    if (sessionToEdit != null && seconds != null) {
                        onAdjustSession(sessionToEdit.uid, seconds)
                        editingSession = null
                    }
                },
            ) { Text(stringResource(R.string.focus_adjust)) }
        },
        dismissButton = {
            Button(onClick = { editingSession = null }) { Text(stringResource(R.string.cancel)) }
        },
    )

    val sessionToDelete = deletingSession
    AlertDialog(
        show = sessionToDelete != null,
        onDismissRequest = { deletingSession = null },
        miuixTitle = stringResource(R.string.focus_delete_session_title),
        miuixSummary = stringResource(R.string.focus_delete_session_message),
        confirmButton = {
            Button(onClick = {
                sessionToDelete?.let { onDeleteSession(it.uid) }
                deletingSession = null
            }) { Text(stringResource(R.string.accept)) }
        },
        dismissButton = {
            Button(onClick = { deletingSession = null }) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun FocusSummaryCard(taskTitle: String, todaySeconds: Long, totalSeconds: Long) {
    FocusSection(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(18.dp),
    ) {
        Text(
            text = taskTitle.ifBlank { stringResource(R.string.focus_loading_task) },
            style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(stringResource(R.string.focus_today), todaySeconds)
            Metric(stringResource(R.string.focus_total), totalSeconds)
        }
    }
}

@Composable
private fun Metric(label: String, seconds: Long) {
    Column {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Normal),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(text = formatDuration(seconds),
            style = MiuixTheme.textStyles.title4.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun ActiveTimerContent(
    timer: FocusTimerSnapshot,
    busy: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onFinishAndComplete: () -> Unit,
) {
    val running = timer.state == FocusTimerState.RUNNING
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(timer.taskTitle, style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center)
        timer.habitOccurredOn?.let { Text(stringResource(R.string.habit_focus_record_date, it),
            style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
        FocusTimerDial(timer)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            FocusRoundAction(icon = R.drawable.msr_stop_wght400,
                label = stringResource(R.string.focus_finish_short),
                contentDescription = stringResource(R.string.focus_finish),
                enabled = !busy && timer.canFinish, onClick = onFinish,
                modifier = Modifier.weight(1f))
            FocusRoundAction(
                icon = if (running) R.drawable.msr_pause_wght400 else R.drawable.msr_play_arrow_wght400,
                label = stringResource(if (running) R.string.focus_pause else R.string.focus_resume),
                enabled = !busy && if (running) timer.canPause else timer.canResume,
                onClick = if (running) onPause else onResume,
                primary = true, size = 80.dp, modifier = Modifier.weight(1f))
            FocusRoundAction(icon = R.drawable.msr_check_wght400,
                label = stringResource(if (timer.target?.kind == com.deadliner.domain.focus.FocusTargetKind.HABIT) R.string.habit_focus_record else R.string.focus_complete_task_short),
                contentDescription = stringResource(if (timer.target?.kind == com.deadliner.domain.focus.FocusTargetKind.HABIT) R.string.habit_focus_finish_record else R.string.focus_finish_and_complete),
                enabled = !busy && (timer.canFinishAndComplete || timer.canFinishAndRecord), onClick = onFinishAndComplete,
                modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SessionRow(session: FocusSession, onAdjust: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = session.startedAt.take(10),
                style = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.Normal),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(text = formatDuration(session.durationSeconds),
                style = MiuixTheme.textStyles.main.copy(fontWeight = FontWeight.Medium))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(text = stringResource(R.string.focus_adjust), onClick = onAdjust)
            TextButton(text = stringResource(R.string.delete), onClick = onDelete)
        }
    }
}

@Composable
private fun InlineMessage(message: String, isError: Boolean) {
    FocusSection(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = if (isError) MiuixTheme.colorScheme.error
            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

internal fun formatFocusClock(rawSeconds: Long): String {
    val seconds = rawSeconds.coerceAtLeast(0)
    val hours = seconds / 3_600
    val minutes = seconds % 3_600 / 60
    val remainder = seconds % 60
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, remainder)
    else "%02d:%02d".format(minutes, remainder)
}

private fun formatDuration(rawSeconds: Long): String {
    val minutes = rawSeconds.coerceAtLeast(0) / 60
    return if (rawSeconds in 1..59) "${rawSeconds}s"
    else if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
}

@Composable
private fun FocusSection(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsSection(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(insideMargin), content = content)
    }
}
