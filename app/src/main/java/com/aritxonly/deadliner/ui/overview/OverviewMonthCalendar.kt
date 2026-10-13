package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.TextButton
import com.deadliner.viewmodel.overview.OverviewMonthUiState
import com.deadliner.domain.statistics.FocusStatisticsSnapshot
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun OverviewMonthCalendar(month: YearMonth, task: DashboardLoad<OverviewMonthUiState>,
    focus: DashboardLoad<FocusStatisticsSnapshot>, onRetry: () -> Unit) {
    var selected by remember(month) { mutableStateOf<LocalDate?>(null) }
    val completed = task.data?.daily?.associateBy { it.date }.orEmpty()
    val durations = focus.data?.daily?.associateBy { it.startDate }.orEmpty()
    val today = LocalDate.now()
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    DashboardSurface(contentPadding = 16.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.dashboard_month_activity), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(if (task.loading || focus.loading) R.string.dashboard_loading else R.string.dashboard_calendar_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            java.time.DayOfWeek.entries.forEach { day ->
                Text(day.getDisplayName(TextStyle.NARROW, locale), Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        val offset = month.atDay(1).dayOfWeek.value - 1
        val weeks = (offset + month.lengthOfMonth() + 6) / 7
        repeat(weeks) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { column ->
                    val number = week * 7 + column - offset + 1
                    if (number !in 1..month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f).heightIn(min = 32.dp))
                    } else {
                        val date = month.atDay(number)
                        val count = completed[date.toString()]?.completedCount
                        val seconds = durations[date.toString()]?.durationSeconds
                        val available = date <= today
                        val background = rememberOverviewHeatmapColor(OverviewIndicatorRole.Completed,
                            if (available) count ?: 0 else 0)
                        val description = stringResource(R.string.dashboard_day_accessibility, date.toString(),
                            count?.toString() ?: "—", seconds?.let { focusDuration(it) } ?: "—")
                        Column(Modifier.weight(1f).heightIn(min = 32.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (available) background else MaterialTheme.colorScheme.surfaceContainer)
                            .semantics { contentDescription = description }
                            .clickable(enabled = available, role = Role.Button, onClick = { selected = date })
                            .padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(number.toString(), style = MaterialTheme.typography.labelMedium,
                                color = if (!available) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f)
                                else if ((count ?: 0) > 0) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(Modifier.size(4.dp).clip(RoundedCornerShape(2.dp))
                                .background(if (available && (seconds ?: 0) > 0) rememberOverviewIndicatorPalette().total
                                    else androidx.compose.ui.graphics.Color.Transparent))
                        }
                    }
                }
            }
            if (week < weeks - 1) Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.dashboard_heatmap_tasks), style = MaterialTheme.typography.labelSmall)
            listOf(0, 1, 2, 4, 6).forEach { level -> Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp))
                .background(rememberOverviewHeatmapColor(OverviewIndicatorRole.Completed, level))) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(rememberOverviewIndicatorPalette().total))
            Text(stringResource(R.string.dashboard_has_focus), style = MaterialTheme.typography.labelSmall)
        }
        if (task.failed || focus.failed) {
            Text(stringResource(R.string.dashboard_load_error), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.dashboard_retry)) }
        }
    }
    selected?.let { date ->
        val count = completed[date.toString()]?.completedCount
        val seconds = durations[date.toString()]?.durationSeconds
        AlertDialog(show = true, onDismissRequest = { selected = null },
            title = { Text(date.toString()) }, miuixTitle = date.toString(),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.dashboard_day_tasks, count?.toString() ?: "—"))
                    Text(stringResource(R.string.dashboard_day_focus, seconds?.let { focusDuration(it) } ?: "—"))
                }
            }, confirmButton = { TextButton(onClick = { selected = null }) { Text(stringResource(R.string.close)) } })
    }
}
