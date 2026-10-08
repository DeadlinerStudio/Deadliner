package com.aritxonly.deadliner.ui.focus

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
) {
    var countdown by rememberSaveable { mutableStateOf(false) }
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var selectingTask by rememberSaveable { mutableStateOf(false) }
    var customDuration by rememberSaveable { mutableStateOf(false) }
    // The theme's root popup host shares the registered page backdrop with other sheets.
    MiuixBottomSheet(
        show = show,
        title = stringResource(R.string.focus_setup_title),
        allowDismiss = !busy,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
    ) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
            item {
                SettingsSection {
                    FocusTaskChoice(
                        title = selectedTaskTitle.ifBlank { stringResource(R.string.focus_select_active_task) },
                        enabled = !busy, onClick = { selectingTask = !selectingTask })
                    if (selectedTaskUid.isNotBlank() && !selectingTask) {
                        Column(Modifier.fillMaxWidth().animateContentSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FocusChoiceRow(
                                choices = listOf(stringResource(R.string.focus_stopwatch_mode),
                                    stringResource(R.string.focus_countdown_mode)),
                                selectedIndex = if (countdown) 1 else 0, enabled = !busy,
                                onSelect = { countdown = it == 1 }, segmented = true)
                            Text(if (countdown) formatFocusClock(minutes * 60L) else "00:00",
                                style = MiuixTheme.textStyles.title1.copy(fontSize = 48.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"),
                                modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                            if (countdown) {
                                val presets = listOf(15, 25, 45, 60)
                                FocusChoiceRow(
                                    choices = presets.map { stringResource(R.string.focus_minutes_short, it) },
                                    selectedIndex = presets.indexOf(minutes), enabled = !busy,
                                    onSelect = { minutes = presets[it] })
                                Row(Modifier.fillMaxWidth().clickable(enabled = !busy, role = Role.Button,
                                    onClick = { customDuration = !customDuration })
                                    .heightIn(min = 48.dp).padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.focus_custom_minutes, minutes),
                                        style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Normal),
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        modifier = Modifier.weight(1f))
                                    Icon(iconResource(R.drawable.msr_keyboard_arrow_down_wght400),
                                        contentDescription = null, modifier = Modifier.size(20.dp),
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
                                }
                                if (customDuration) Slider(
                                    value = minutes.toFloat(),
                                    onValueChange = { minutes = it.toInt().coerceIn(1, 180) },
                                    enabled = !busy, valueRange = 1f..180f, steps = 178,
                                    modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
            if (selectingTask || selectedTaskUid.isBlank()) {
                if (tasks.isEmpty()) {
                    item { Text(stringResource(R.string.focus_no_active_tasks), modifier = Modifier.padding(16.dp)) }
                }
                items(tasks, key = Task::uid) { task ->
                    FocusTaskChoice(title = task.title, enabled = !busy,
                        selected = selectedTaskUid == task.uid,
                        onClick = { onSelectTask(task); selectingTask = false })
                }
            }
            if (error != null || startFailed) {
                item { Text(error ?: stringResource(R.string.focus_start_failed), color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)) }
            }
            if (selectedTaskUid.isNotBlank() && !selectingTask) {
                item {
                    FocusRoundAction(
                        icon = R.drawable.msr_play_arrow_wght400,
                        label = stringResource(if (countdown) R.string.focus_start_countdown else R.string.focus_start_stopwatch),
                        enabled = !busy, primary = true, size = 76.dp, backdrop = null,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        onClick = { onStart(if (countdown) FocusTimerMode.COUNTDOWN else FocusTimerMode.STOPWATCH, minutes) })
                }
            }
        }
    }
}

@Composable
private fun FocusTaskChoice(title: String, enabled: Boolean, onClick: () -> Unit, selected: Boolean = false) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .heightIn(min = 56.dp).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(iconResource(R.drawable.msr_schedule_wght400), contentDescription = null,
            tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(title, style = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f), maxLines = 2,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f))
        Icon(iconResource(if (selected) R.drawable.msr_check_wght400 else R.drawable.msr_keyboard_arrow_down_wght400),
            contentDescription = null, tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            modifier = Modifier.size(20.dp))
    }
}
