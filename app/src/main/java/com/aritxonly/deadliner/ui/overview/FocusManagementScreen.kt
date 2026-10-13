package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.base.*
import com.deadliner.domain.focus.FocusSession
import com.deadliner.domain.focus.FocusTargetKind
import com.deadliner.domain.focus.FocusTimerOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun FocusManagementContent(modifier: Modifier, top: Dp, bottom: Dp) {
    var retry by remember { mutableIntStateOf(0) }
    val loaded by rememberFocusManagement(retry)
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<FocusSession?>(null) }
    var deleting by remember { mutableStateOf<FocusSession?>(null) }
    var seconds by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var mutationFailed by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    fun mutate(session: FocusSession, duration: Long?) {
        if (busy) return
        busy = true
        mutationFailed = false
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val viewModel = KmpV6Runtime.focusTimerViewModel
                    if (duration == null) viewModel.deleteSessionNow(session.uid)
                    else viewModel.adjustSessionDurationNow(session.uid, duration)
                }
                val succeeded = result.error == null && result.lastOutcome in listOf(FocusTimerOutcome.APPLIED, FocusTimerOutcome.NO_OP)
                mutationFailed = !succeeded
                if (succeeded) { editing = null; deleting = null }
                retry++
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutationFailed = true
            } finally { busy = false }
        }
    }
    LazyColumn(modifier.fillMaxSize().syncTopBarMaterialScroll(listState), state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 12.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item("today") { DashboardSurface {
            Text(stringResource(R.string.focus_stats_today_overview), style = MaterialTheme.typography.titleMedium)
            Text(loaded.data?.let { focusDuration(it.todaySeconds) } ?: stringResource(if (loaded.failed) R.string.dashboard_unavailable else R.string.dashboard_loading),
                style = MaterialTheme.typography.headlineLarge)
            loaded.data?.let { Text(stringResource(R.string.focus_stats_session_count_short, it.todayCount)) }
            Text(stringResource(R.string.dashboard_focus_management_scope), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        if (loaded.failed) item("error") { DashboardSurface {
            Text(stringResource(R.string.dashboard_load_error))
            TextButton({ retry++ }) { Text(stringResource(R.string.dashboard_retry)) }
        } }
        loaded.data?.let { data ->
            item("summary") { DashboardSurface {
                Text(stringResource(R.string.dashboard_focus_history_statistics), style = MaterialTheme.typography.titleMedium)
                Text(focusDuration(data.totalSeconds), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.focus_stats_session_count_short, data.sessions.size))
                Spacer(Modifier.height(12.dp))
                listOf(FocusTargetKind.TASK, FocusTargetKind.HABIT).forEach { kind ->
                    val matching = data.sessions.filter { it.boundTarget.kind == kind }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(if (kind == FocusTargetKind.TASK) R.string.dashboard_focus_task_records else R.string.dashboard_focus_habit_records))
                        Text(focusDuration(matching.sumOf { it.durationSeconds }))
                    }
                }
            } }
            data.distribution?.let { distribution -> item("allocation") {
                FocusDistributionCard(distribution, horizontalPadding = 0.dp, limit = Int.MAX_VALUE)
            } }
            item("records-title") { Text(stringResource(R.string.dashboard_focus_records), style = MaterialTheme.typography.titleMedium) }
            if (data.sessions.isEmpty()) item("empty") { DashboardSurface { Text(stringResource(R.string.focus_no_sessions)) } }
            items(data.sessions, key = { it.uid }) { session ->
                DashboardSurface {
                    Text(session.taskTitleSnapshot, style = MaterialTheme.typography.titleMedium)
                    Text(Instant.parse(session.startedAt).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(focusDuration(session.durationSeconds), style = MaterialTheme.typography.titleLarge)
                    session.habitOccurredOn?.let { Text(stringResource(R.string.habit_focus_record_date, it),
                        style = MaterialTheme.typography.bodySmall) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = {
                            mutationFailed = false; seconds = session.durationSeconds.toString(); editing = session
                        }, enabled = !busy) { Text(stringResource(R.string.focus_adjust)) }
                        TextButton(onClick = { mutationFailed = false; deleting = session }, enabled = !busy) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
    val edit = editing
    val valid = edit != null && seconds.toLongOrNull()?.let { it in 1..maximumFocusAdjustment(edit) } == true
    AlertDialog(show = edit != null, onDismissRequest = { if (!busy) editing = null },
        miuixTitle = stringResource(R.string.focus_adjust_session_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = seconds, onValueChange = { seconds = it.filter(Char::isDigit) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), miuixLabel = stringResource(R.string.focus_duration_seconds))
                edit?.let { Text(stringResource(R.string.dashboard_focus_adjust_range, maximumFocusAdjustment(it)),
                    style = MaterialTheme.typography.bodySmall) }
                if (mutationFailed) Text(stringResource(R.string.dashboard_focus_mutation_failed), color = MaterialTheme.colorScheme.error)
            }
        }, renderTextContentInMiuix = true,
        confirmButton = { Button(onClick = {
            val amount = seconds.toLongOrNull()
            if (edit != null && amount != null && amount in 1..maximumFocusAdjustment(edit)) mutate(edit, amount)
        }, enabled = valid && !busy) {
            Text(stringResource(R.string.focus_adjust))
        } }, dismissButton = { TextButton(onClick = { editing = null }, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
    val deletion = deleting
    AlertDialog(show = deletion != null, onDismissRequest = { if (!busy) deleting = null },
        miuixTitle = stringResource(R.string.focus_delete_session_title),
        miuixSummary = stringResource(R.string.focus_delete_session_message),
        text = { if (mutationFailed) Text(stringResource(R.string.dashboard_focus_mutation_failed), color = MaterialTheme.colorScheme.error) },
        renderTextContentInMiuix = true,
        confirmButton = { Button(onClick = { deletion?.let { mutate(it, null) } }, enabled = !busy) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deleting = null }, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
}
