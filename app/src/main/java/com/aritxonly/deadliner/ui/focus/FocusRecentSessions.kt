package com.aritxonly.deadliner.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.materialSymbolPainter
import com.aritxonly.deadliner.ui.overview.focusDuration
import com.deadliner.domain.focus.FocusSession
import com.deadliner.domain.focus.FocusTimerMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** The row opens details; destructive/edit actions belong inside that sheet. */
@Composable
internal fun FocusRecentSessions(sessions: List<FocusSession>, onSelect: (FocusSession) -> Unit) {
    Column(Modifier.widthIn(max = FocusVisuals.MaximumWidth).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FocusSectionHeading(stringResource(R.string.focus_recent_sessions), modifier = Modifier.padding(horizontal = 4.dp))
        FocusPanel(padding = 0.dp) {
            if (sessions.isEmpty()) Text(stringResource(R.string.focus_no_sessions), Modifier.padding(20.dp),
                style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            sessions.forEachIndexed { index, session ->
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelect(session) }
                    .heightIn(min = 76.dp).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(focusSessionDateTime(session.startedAt, "MM-dd HH:mm"), style = MiuixTheme.textStyles.body2)
                        Text(stringResource(if (session.mode == FocusTimerMode.STOPWATCH) R.string.focus_stopwatch_mode else R.string.focus_countdown_mode),
                            style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    Text(focusDuration(session.durationSeconds), style = MiuixTheme.textStyles.body2)
                    Icon(materialSymbolPainter("chevron_right"), null, Modifier.size(com.aritxonly.deadliner.ui.overviewChevronSize(18.dp)), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
                }
                if (index < sessions.lastIndex) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp)
                    .background(MiuixTheme.colorScheme.dividerLine))
            }
        }
    }
}

@Composable
internal fun FocusRecentSessionDetail(session: FocusSession?, onDismiss: () -> Unit,
    onAdjust: (FocusSession) -> Unit, onDelete: (FocusSession) -> Unit) {
    MiuixBottomSheet(show = session != null, title = stringResource(R.string.focus_hub_record_detail),
        onDismissRequest = onDismiss) { padding ->
        val scroll = rememberScrollState()
        Column(Modifier.fillMaxWidth().syncTopBarMaterialScroll(scroll).verticalScroll(scroll)
            .padding(padding).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (session != null) {
                Text(session.targetTitleSnapshot, style = MiuixTheme.textStyles.title3)
                Text(stringResource(R.string.focus_hub_exact_duration, session.durationSeconds / 3600,
                    session.durationSeconds % 3600 / 60, session.durationSeconds % 60), style = MiuixTheme.textStyles.title1)
                SessionDetailField(stringResource(R.string.focus_hub_started), focusSessionDateTime(session.startedAt, "yyyy-MM-dd HH:mm:ss"))
                SessionDetailField(stringResource(R.string.focus_hub_ended), focusSessionDateTime(session.endedAt, "yyyy-MM-dd HH:mm:ss"))
                SessionDetailField(stringResource(R.string.focus_hub_mode), stringResource(
                    if (session.mode == FocusTimerMode.STOPWATCH) R.string.focus_stopwatch_mode else R.string.focus_countdown_mode))
                session.habitOccurredOn?.let { SessionDetailField(stringResource(R.string.focus_hub_habit_date), it) }
                session.habitRecordedAmount?.let { SessionDetailField(stringResource(R.string.focus_hub_habit_amount), it.toString()) }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = { onAdjust(session) }) { Text(stringResource(R.string.focus_adjust)) }
                    TextButton(onClick = { onDelete(session) }) { Text(stringResource(R.string.delete), color = MiuixTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun SessionDetailField(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Text(value, style = MiuixTheme.textStyles.body2)
    }
}

private fun focusSessionDateTime(value: String, pattern: String): String = runCatching {
    Instant.parse(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern))
}.getOrDefault(value)
