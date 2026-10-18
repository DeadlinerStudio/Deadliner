package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.deadliner.domain.focus.*
import com.deadliner.domain.focus.management.*
import kotlinx.coroutines.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun FocusManagementHistorySheet(data: FocusManagementSnapshot?, target: FocusTargetRef?, onDismiss: () -> Unit, onSelectRecord: (String) -> Unit) {
    var limit by rememberSaveable(target?.kind?.name, target?.uid) { mutableIntStateOf(50) }
    val page = remember(data, target, limit) { if (target != null) data?.historyPage(limit, target) else null }
    MiuixBottomSheet(show = target != null, title = data?.allTime?.targets?.firstOrNull { it.target == target }?.title,
        onDismissRequest = onDismiss) { sheetPadding ->
        val sheetListState = rememberLazyListState()
        LazyColumn(state = sheetListState, modifier = Modifier.syncTopBarMaterialScroll(sheetListState).fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = sheetPadding.calculateTopPadding() + 20.dp, bottom = sheetPadding.calculateBottomPadding() + 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (page?.totalCount == 0) item { Text(stringResource(R.string.focus_no_sessions)) }
            page?.days?.forEach { day ->
                item("day-${day.date}") { FocusManagementDayHeader(day) }
                items(day.records, key = { it.session.uid }) { record -> FocusManagementRecordRow(record) { onSelectRecord(record.session.uid) } }
            }
            if (page?.hasMore == true) item { TextButton(onClick = { limit += 50 }) { Text(stringResource(R.string.focus_hub_more)) } }
        }
    }
}

@Composable
internal fun FocusManagementRecordSheet(record: FocusManagementRecord?, show: Boolean, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable(record?.session?.uid) { mutableStateOf(false) }
    var deleting by rememberSaveable(record?.session?.uid) { mutableStateOf(false) }
    var minutes by rememberSaveable(record?.session?.uid) { mutableStateOf("") }
    var seconds by rememberSaveable(record?.session?.uid) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<FocusTimerOutcome?>(null) }
    fun mutate(duration: Long?) {
        val uid = record?.session?.uid ?: return
        if (busy) return
        busy = true; failed = false
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val vm = KmpV6Runtime.focusTimerViewModel
                    if (duration == null) vm.deleteSessionNow(uid) else vm.adjustSessionDurationNow(uid, duration)
                }
                outcome = result.lastOutcome
                if (result.error == null && result.lastOutcome in listOf(FocusTimerOutcome.APPLIED, FocusTimerOutcome.NO_OP)) {
                    editing = false; deleting = false; onChanged()
                    if (duration == null) onDismiss()
                } else {
                    failed = true; onChanged()
                    if (result.lastOutcome == FocusTimerOutcome.TIMER_NOT_FOUND) { editing = false; deleting = false }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                failed = true
            } finally { busy = false }
        }
    }
    MiuixBottomSheet(show = show, title = stringResource(R.string.focus_hub_record_detail), allowDismiss = !busy,
        onDismissRequest = { if (!busy) onDismiss() }) { sheetPadding ->
        val sheetScrollState = rememberScrollState()
        Column(Modifier.fillMaxWidth().syncTopBarMaterialScroll(sheetScrollState)
                .verticalScroll(sheetScrollState).padding(sheetPadding).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (record == null) Text(stringResource(R.string.focus_hub_unavailable))
            else {
                val session = record.session
                Text(record.title, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.focus_hub_exact_duration, session.durationSeconds / 3600, session.durationSeconds % 3600 / 60, session.durationSeconds % 60), style = MaterialTheme.typography.headlineLarge)
                DetailField(stringResource(R.string.focus_hub_target_state), focusManagedTargetStatus(record.targetStatus))
                if (record.title != session.targetTitleSnapshot) DetailField(stringResource(R.string.focus_hub_original_title), session.targetTitleSnapshot)
                val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                DetailField(stringResource(R.string.focus_hub_started), Instant.parse(session.startedAt).atZone(ZoneId.systemDefault()).format(format))
                DetailField(stringResource(R.string.focus_hub_ended), Instant.parse(session.endedAt).atZone(ZoneId.systemDefault()).format(format))
                DetailField(stringResource(R.string.focus_hub_mode), stringResource(if (session.mode == FocusTimerMode.STOPWATCH) R.string.focus_stopwatch_mode else R.string.focus_countdown_mode))
                session.habitOccurredOn?.let { DetailField(stringResource(R.string.focus_hub_habit_date), it) }
                session.habitRecordedAmount?.let { DetailField(stringResource(R.string.focus_hub_habit_amount), it.toString()) }
                DetailField(stringResource(R.string.focus_hub_end_reason), stringResource(when (session.completionKind) {
                    FocusCompletionKind.COUNTDOWN_ELAPSED -> R.string.focus_hub_reason_countdown
                    FocusCompletionKind.FOCUS_AND_TASK_COMPLETED -> R.string.focus_hub_reason_completed
                    FocusCompletionKind.FOCUS_AND_HABIT_RECORDED -> R.string.focus_hub_reason_recorded
                    FocusCompletionKind.FOCUS_AND_TASK_CHANGED, FocusCompletionKind.TASK_BECAME_INACTIVE, FocusCompletionKind.HABIT_BECAME_INACTIVE -> R.string.focus_hub_reason_changed
                    else -> R.string.focus_hub_reason_saved
                }))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { failed = false; minutes = (session.durationSeconds / 60).toString(); seconds = (session.durationSeconds % 60).toString(); editing = true }, enabled = !busy) { Text(stringResource(R.string.focus_adjust)) }
                    TextButton(onClick = { failed = false; deleting = true }, enabled = !busy) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    val duration = focusManagementInputSeconds(minutes, seconds, record?.maximumAdjustmentSeconds ?: 0)
    AlertDialog(show = editing, onDismissRequest = { if (!busy) editing = false }, miuixTitle = stringResource(R.string.focus_adjust_session_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(minutes, { minutes = it }, Modifier.weight(1f), enabled = !busy, miuixLabel = stringResource(R.string.focus_hub_minutes), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(seconds, { seconds = it }, Modifier.weight(1f), enabled = !busy, miuixLabel = stringResource(R.string.focus_hub_seconds), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Text(stringResource(R.string.dashboard_focus_adjust_range, record?.maximumAdjustmentSeconds ?: 0), style = MaterialTheme.typography.bodySmall)
                if (failed) Text(stringResource(if (outcome == FocusTimerOutcome.INVALID_DURATION) R.string.focus_hub_changed_range else R.string.dashboard_focus_mutation_failed), color = MaterialTheme.colorScheme.error)
            }
        }, renderTextContentInMiuix = true,
        confirmButton = { Button(onClick = { duration?.let(::mutate) }, modifier = Modifier.testTag("focus-management-save"), enabled = duration != null && !busy) { Text(stringResource(R.string.focus_adjust)) } },
        dismissButton = { TextButton(onClick = { editing = false }, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
    AlertDialog(show = deleting, onDismissRequest = { if (!busy) deleting = false }, miuixTitle = stringResource(R.string.focus_delete_session_title),
        miuixSummary = stringResource(R.string.focus_hub_delete_summary, record?.title.orEmpty(), record?.localStartedDate.orEmpty(), record?.let { focusDuration(it.session.durationSeconds) }.orEmpty()),
        text = { if (failed) Text(stringResource(R.string.dashboard_focus_mutation_failed), color = MaterialTheme.colorScheme.error) }, renderTextContentInMiuix = true,
        confirmButton = { Button(onClick = { mutate(null) }, enabled = !busy) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deleting = false }, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
}

internal fun focusManagementInputSeconds(minutes: String, seconds: String, maximum: Long): Long? {
    if (minutes.isEmpty() || seconds.isEmpty() || minutes.any { !it.isDigit() } || seconds.any { !it.isDigit() }) return null
    val m = minutes.toLongOrNull() ?: return null
    val s = seconds.toLongOrNull() ?: return null
    if (s !in 0..59 || m > maximum / 60) return null
    return (m * 60 + s).takeIf { it in 1..maximum }
}

@Composable
private fun DetailField(title: String, value: String) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
}
