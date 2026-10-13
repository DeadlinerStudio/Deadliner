package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.deadliner.viewmodel.overview.OverviewHistory

private data class TaskStatusSlice(val count: Int, val label: String, val color: Color)

@Composable
private fun taskStatusSlices(status: OverviewHistory): List<TaskStatusSlice> {
    val colors = rememberOverviewIndicatorPalette()
    return listOf(
        TaskStatusSlice(status.pending, stringResource(R.string.dashboard_status_pending), colors.pending),
        TaskStatusSlice(status.completed, stringResource(R.string.dashboard_status_completed), colors.completed),
        TaskStatusSlice(status.overdue, stringResource(R.string.dashboard_status_overdue), colors.overdue),
        TaskStatusSlice(status.abandoned, stringResource(R.string.dashboard_status_abandoned), colors.abandoned),
    )
}

@Composable
internal fun OverviewTaskStatusPie(status: OverviewHistory, modifier: Modifier = Modifier) {
    val slices = taskStatusSlices(status)
    val total = slices.sumOf { it.count }
    val label = if (total == 0) stringResource(R.string.dashboard_status_empty)
        else slices.joinToString { "${it.label} ${it.count}" }
    val neutral = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.semantics { contentDescription = label }) {
        if (total == 0) drawCircle(neutral) else {
            var start = -90f
            slices.filter { it.count > 0 }.forEach { slice ->
                val sweep = 360f * slice.count / total
                drawArc(slice.color, start, sweep, useCenter = true)
                start += sweep
            }
        }
    }
}

@Composable
internal fun OverviewTaskStatusLegend(status: OverviewHistory) {
    val slices = taskStatusSlices(status)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        slices.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { slice ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Canvas(Modifier.size(6.dp)) { drawCircle(slice.color) }
                        Text("${slice.label} ${slice.count}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
