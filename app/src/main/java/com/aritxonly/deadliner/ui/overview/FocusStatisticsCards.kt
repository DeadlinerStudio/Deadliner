package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.deadliner.domain.statistics.FocusDurationPoint
import com.deadliner.domain.statistics.FocusStatisticsSnapshot
import com.deadliner.domain.statistics.FocusStatisticsSummary

/** Inline content only: surfaces and navigation remain owned by the existing Overview cards. */
@Composable
internal fun FocusTodaySummary(state: FocusOverviewState) {
    val summary = state.data?.today
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.focus_stats_today_duration), Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(summary?.let { focusDuration(it.totalDurationSeconds) } ?: "—",
            style = MaterialTheme.typography.bodyMedium, color = rememberOverviewIndicatorPalette().total)
        summary?.let { Text(stringResource(R.string.focus_stats_session_count_short, it.sessionCount),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    FocusTargetTimeSummary(summary, "focus-today-types")
    FocusInlineStatus(state)
}

@Composable
internal fun FocusHistorySummary(state: FocusOverviewState) {
    val data = state.data
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.focus_stats_all_time_duration), Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(data?.let { focusDuration(it.allTime.totalDurationSeconds) } ?: "—",
            style = MaterialTheme.typography.bodyMedium, color = rememberOverviewIndicatorPalette().total)
    }
    data?.let {
        Text(stringResource(R.string.focus_stats_period_brief, focusDuration(it.thisWeek.totalDurationSeconds),
            focusDuration(it.thisMonth.summary.totalDurationSeconds)), Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    FocusTargetTimeSummary(data?.allTime, "focus-history-types")
    FocusInlineStatus(state)
}

@Composable
internal fun FocusInlineStatus(state: FocusOverviewState) {
    if (state.hasError || state.data == null) {
        Text(stringResource(if (state.hasError) R.string.focus_stats_error else R.string.focus_stats_loading),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun FocusTrendSection(
    state: FocusOverviewState,
    points: List<FocusDurationPoint>,
    kind: FocusChartKind,
    tag: String,
    summary: FocusStatisticsSummary? = null,
) {
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.focus_stats_plot_title), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (state.data == null) "—" else focusDuration(summary?.totalDurationSeconds ?: points.sumOf { it.durationSeconds }),
            style = MaterialTheme.typography.bodyMedium, color = rememberOverviewIndicatorPalette().total)
    }
    FocusInlineStatus(state)
    FocusTargetTimeSummary(summary, "$tag-types")
    if (state.data != null) FocusDurationChart(points, kind, tag = tag)
}

@Composable
internal fun FocusDistributionCard(snapshot: FocusStatisticsSnapshot, horizontalPadding: Dp = 16.dp, title: String? = null, limit: Int = 3) {
    // This is a ranked list, not a hand-drawn chart. Keep both primary distributions discoverable.
    if (snapshot.summary.sessionCount == 0) return
    Card(
        modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title ?: stringResource(R.string.focus_stats_time_allocation), style = MaterialTheme.typography.titleMedium)
            snapshot.targets.take(limit).forEach {
                val title = if (it.target.kind == com.deadliner.domain.focus.FocusTargetKind.HABIT)
                    it.title + " · " + stringResource(R.string.habit_focus_target) else it.title
                FocusDistributionRow(title, it.durationSeconds)
            }
            if (snapshot.categories.any { it.categoryUid != null }) {
                Text(stringResource(R.string.focus_stats_categories_minimal), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                snapshot.categories.take(limit).forEach {
                    FocusDistributionRow(it.categoryTitle ?: stringResource(R.string.focus_stats_uncategorized), it.durationSeconds)
                }
            }
        }
    }
}

@Composable
private fun FocusDistributionRow(title: String, seconds: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(focusDuration(seconds), style = MaterialTheme.typography.bodyMedium,
            color = rememberOverviewIndicatorPalette().total)
    }
}
