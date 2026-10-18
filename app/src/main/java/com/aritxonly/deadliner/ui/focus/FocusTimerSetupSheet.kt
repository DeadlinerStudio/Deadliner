package com.aritxonly.deadliner.ui.focus

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.Slider
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.settings.SettingsSection
import com.deadliner.domain.focus.FocusTimerMode
import com.deadliner.domain.task.Task
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerSetupSheet(
    show: Boolean,
    selectedTaskUid: String,
    selectedTaskTitle: String,
    tasks: List<Task>,
    busy: Boolean,
    error: String?,
    startFailed: Boolean,
    onSelectTask: (Task) -> Unit,
    onStart: (FocusTimerMode, Int) -> Unit,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
    selectedIsHabit: Boolean = false,
    habits: List<com.deadliner.domain.habit.Habit> = emptyList(),
    onSelectHabit: (com.deadliner.domain.habit.Habit) -> Unit = {},
    managementTargets: List<com.deadliner.domain.focus.management.FocusStartTargetOption>? = null,
    onSelectManagementTarget: (com.deadliner.domain.focus.management.FocusStartTargetOption) -> Unit = {},
    onRetryTargets: () -> Unit = {},
) {
    var countdown by rememberSaveable { mutableStateOf(false) }
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var selectingTask by rememberSaveable { mutableStateOf(false) }
    var customDuration by rememberSaveable { mutableStateOf(false) }
    var customMinutes by rememberSaveable { mutableStateOf("25") }
    var managementKind by rememberSaveable { mutableStateOf(com.deadliner.domain.focus.FocusTargetKind.TASK.name) }
    var managementSearch by rememberSaveable { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(show) {
        if (show) {
            countdown = false; minutes = 25; selectingTask = false; customDuration = false
            managementKind = if (selectedIsHabit) com.deadliner.domain.focus.FocusTargetKind.HABIT.name else com.deadliner.domain.focus.FocusTargetKind.TASK.name
            managementSearch = ""
        }
    }
    // Both entry points render the same candidate picker. Only their initial selection differs.
    val targets = remember(managementTargets, tasks, habits) {
        managementTargets ?: tasks.map { task -> com.deadliner.domain.focus.management.FocusStartTargetOption(
            com.deadliner.domain.focus.FocusTargetRef(com.deadliner.domain.focus.FocusTargetKind.TASK, task.uid), task.title)
        } + habits.map { habit -> com.deadliner.domain.focus.management.FocusStartTargetOption(
            com.deadliner.domain.focus.FocusTargetRef(com.deadliner.domain.focus.FocusTargetKind.HABIT, habit.uid), habit.name)
        }
    }
    fun selectTarget(option: com.deadliner.domain.focus.management.FocusStartTargetOption) {
        if (managementTargets != null) onSelectManagementTarget(option)
        else if (option.target.kind == com.deadliner.domain.focus.FocusTargetKind.TASK) {
            tasks.firstOrNull { it.uid == option.target.uid }?.let(onSelectTask)
        } else habits.firstOrNull { it.uid == option.target.uid }?.let(onSelectHabit)
        selectingTask = false
    }
    MiuixBottomSheet(
        show = show, title = stringResource(R.string.focus_setup_title), allowDismiss = !busy,
        onDismissRequest = onDismissRequest, onDismissFinished = onDismissFinished,
        sampleContentForBottomBar = true,
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(selectedTaskUid.isNotBlank() && !selectingTask,
                enter = androidx.compose.animation.fadeIn(FocusVisuals.motion()), exit = androidx.compose.animation.fadeOut(FocusVisuals.motion())) {
                val backdrop = com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop.current
                androidx.compose.foundation.layout.Box {
                    FocusBottomChrome(Modifier.matchParentSize())
                    FocusRoundAction(icon = R.drawable.msr_play_arrow_wght400,
                        label = stringResource(if (countdown) R.string.focus_start_countdown else R.string.focus_start_stopwatch),
                        enabled = !busy, primary = true, size = 64.dp, backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 12.dp),
                        onClick = { onStart(if (countdown) FocusTimerMode.COUNTDOWN else FocusTimerMode.STOPWATCH, minutes) })
                }
            }
        },
    ) { sheetPadding ->
        val sheetListState = rememberLazyListState()
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val pickerColumns = focusLayout((maxWidth - 40.dp).value, androidx.compose.ui.platform.LocalDensity.current.fontScale).summaryColumns
            LazyColumn(state = sheetListState, modifier = Modifier.syncTopBarMaterialScroll(sheetListState).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = sheetPadding.calculateTopPadding() + 8.dp,
                    bottom = sheetPadding.calculateBottomPadding() + 16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                if (selectedTaskUid.isNotBlank()) item("focus-setup") {
                    FocusPanel(modifier = Modifier.padding(bottom = 12.dp)) {
                        FocusTargetHeading(
                            title = selectedTaskTitle.ifBlank { stringResource(R.string.focus_hub_select_target) },
                            supporting = if (selectedTaskUid.isBlank()) stringResource(R.string.focus_hub_binding_hint) else stringResource(if (selectedIsHabit) R.string.habit else R.string.task),
                            modifier = Modifier.clickable(enabled = !busy, role = Role.Button) { selectingTask = !selectingTask },
                            onClick = { selectingTask = !selectingTask })
                        androidx.compose.animation.AnimatedVisibility(selectedTaskUid.isNotBlank() && !selectingTask,
                            enter = androidx.compose.animation.expandVertically(FocusVisuals.motion()) + androidx.compose.animation.fadeIn(FocusVisuals.motion()),
                            exit = androidx.compose.animation.shrinkVertically(FocusVisuals.motion()) + androidx.compose.animation.fadeOut(FocusVisuals.motion())) {
                            FocusSetupBody(countdown, minutes, busy, { countdown = it }, { minutes = it },
                                { customMinutes = minutes.toString(); customDuration = true })
                        }
                    }
                }
                if (selectingTask || selectedTaskUid.isBlank()) {
                    managementTargetPicker(targets, managementKind, managementSearch, busy,
                        { managementKind = it }, { managementSearch = it }, ::selectTarget,
                        selectedUid = selectedTaskUid, selectedIsHabit = selectedIsHabit, columns = pickerColumns)
                    if (managementTargets != null && error != null) item {
                        com.aritxonly.deadliner.ui.base.TextButton(onClick = onRetryTargets) { Text(stringResource(R.string.dashboard_retry)) }
                    }
                }
                if (error != null || startFailed) item { Text(error ?: stringResource(R.string.focus_start_failed),
                    color = MiuixTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            }
        }
    }
    val customValid = customMinutes.toIntOrNull()?.takeIf { it in 1..180 }
    com.aritxonly.deadliner.ui.base.AlertDialog(
        show = customDuration && show, onDismissRequest = { if (!busy) customDuration = false },
        miuixTitle = stringResource(R.string.focus_hub_custom),
        text = { androidx.compose.foundation.layout.Column {
            com.aritxonly.deadliner.ui.base.OutlinedTextField(customMinutes, { customMinutes = it },
                enabled = !busy, singleLine = true, miuixLabel = stringResource(R.string.focus_hub_minutes),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            Text(stringResource(R.string.focus_hub_countdown_range), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        } }, renderTextContentInMiuix = true,
        confirmButton = { com.aritxonly.deadliner.ui.base.Button(onClick = {
            customValid?.let { minutes = it; customDuration = false }
        }, enabled = customValid != null && !busy) { Text(stringResource(R.string.accept)) } },
        dismissButton = { com.aritxonly.deadliner.ui.base.TextButton(onClick = { customDuration = false }, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun FocusSetupBody(countdown: Boolean, minutes: Int, busy: Boolean, onMode: (Boolean) -> Unit,
    onMinutes: (Int) -> Unit, onCustom: () -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        val wide = focusLayout(maxWidth.value, androidx.compose.ui.platform.LocalDensity.current.fontScale).setupWide
        val clock: @Composable () -> Unit = {
            androidx.compose.animation.Crossfade(countdown, animationSpec = FocusVisuals.motion(), label = "focus-setup-face") { mode ->
                FocusClockFace(clock = if (mode) formatFocusClock(minutes * 60L) else "00:00",
                    label = stringResource(if (mode) R.string.focus_countdown_mode else R.string.focus_stopwatch_mode),
                    status = stringResource(R.string.focus_hub_ready), maximumDiameter = 220.dp)
            }
        }
        val controls: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusChoiceRow(listOf(stringResource(R.string.focus_stopwatch_mode), stringResource(R.string.focus_countdown_mode)),
                    if (countdown) 1 else 0, !busy, { onMode(it == 1) }, segmented = true)
                androidx.compose.animation.AnimatedVisibility(countdown,
                    enter = androidx.compose.animation.expandVertically(FocusVisuals.motion()) + androidx.compose.animation.fadeIn(FocusVisuals.motion()),
                    exit = androidx.compose.animation.shrinkVertically(FocusVisuals.motion()) + androidx.compose.animation.fadeOut(FocusVisuals.motion())) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val presets = listOf(15, 25, 45, 60)
                        FocusChoiceRow(presets.map { stringResource(R.string.focus_minutes_short, it) }, presets.indexOf(minutes), !busy, { onMinutes(presets[it]) })
                        Row(Modifier.fillMaxWidth().clickable(enabled = !busy, role = Role.Button, onClick = onCustom)
                            .heightIn(min = 48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.focus_custom_minutes, minutes), Modifier.weight(1f), style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            Icon(iconResource(R.drawable.msr_keyboard_arrow_down_wght400), null, Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
                        }
                    }
                }
                Text(stringResource(if (countdown) R.string.focus_hub_countdown_hint else R.string.focus_hub_stopwatch_hint),
                    style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(Modifier.weight(0.9f)) { clock() }
            androidx.compose.foundation.layout.Box(Modifier.weight(1.1f)) { controls() }
        } else Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { clock(); controls() }
    }
}
