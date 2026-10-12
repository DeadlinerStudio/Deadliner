package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.AnimatedItem
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.main.shared.mainListContainerClip

@Composable
fun TrendAnalysisScreen(
    snapshot: OverviewSnapshot,
    modifier: Modifier = Modifier,
    topContentPadding: Dp = 0.dp,
    listState: LazyListState,
    focusState: FocusOverviewState = FocusOverviewState(),
) {
    val trendItems = listOf<@Composable () -> Unit>(
        { ContributionHeatmapCard(snapshot.contributionStats) },
        { DailyCompletedCard(snapshot.dailyStats, focusState) },
        { MonthlyTrendCard(snapshot.monthlyStats, focusState) },
        { PrevWeeksCard(snapshot.weeklyStats, focusState) },
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .testTag("overview-focus-trend-list")
            .syncTopBarMaterialScroll(listState)
            .mainListContainerClip(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = topContentPadding),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(trendItems) { index, itemComposable ->
            AnimatedItem(delayMillis = index * 100L) {
                itemComposable()
            }
        }

        item {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.overview_bottom_safe_area)))
        }
    }
}

@Composable
private fun ContributionHeatmapCard(
    contributionStats: List<ContributionDay>,
) {
    OverviewSurfaceCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.contribution_heatmap),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.contribution_heatmap_days, contributionStats.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ResponsiveContributionHeatmapGrid(contributionStats = contributionStats)

            Spacer(modifier = Modifier.height(12.dp))

            HeatLegend(
                lowLabel = stringResource(R.string.heatmap_less),
                highLabel = stringResource(R.string.heatmap_more),
                levels = List(5) { level -> colorForContribution(level) },
            )
        }
    }
}

@Composable
private fun ResponsiveContributionHeatmapGrid(
    contributionStats: List<ContributionDay>,
) {
    val columns = contributionStats.chunked(7)
    val horizontalSpacing = 3.dp
    val verticalSpacing = 3.dp
    val cellMaxSize = dimensionResource(R.dimen.overview_contribution_heatmap_cell_max_size)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnCount = columns.size.coerceAtLeast(1)
        val cellSize = ((maxWidth - horizontalSpacing * (columnCount - 1)) / columnCount)
            .coerceAtMost(cellMaxSize)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            columns.forEachIndexed { columnIndex, week ->
                Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
                    repeat(7) { rowIndex ->
                        val day = week.getOrNull(rowIndex)
                        if (day != null) {
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(dimensionResource(R.dimen.overview_corner_radius_xs)))
                                    .background(colorForContribution(day.count))
                            )
                        } else {
                            Spacer(modifier = Modifier.size(cellSize))
                        }
                    }
                }
                if (columnIndex < columns.lastIndex) {
                    Spacer(modifier = Modifier.width(horizontalSpacing))
                }
            }
        }
    }
}

@Composable
private fun DailyCompletedCard(
    dailyStats: List<DailyStat>,
    focusState: FocusOverviewState,
) {
    val indicatorPalette = rememberOverviewIndicatorPalette()
    val points = focusState.data?.daily.orEmpty()
    OverviewSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            TrendCardHeader(stringResource(R.string.focus_stats_recent_days), dailyStats.sumOf { it.completedCount }, "daily")
            DailyBarChart(
                data = dailyStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp).testTag("task-trend-daily"),
                barColor = indicatorPalette.completed,
                overdueColor = indicatorPalette.overdue,
            )
            FocusTrendSection(focusState, points, FocusChartKind.DAILY, "focus-trend-daily", focusState.data?.recentDaysSummary)
        }
    }
}

@Composable
private fun MonthlyTrendCard(
    monthlyStats: List<MonthlyStat>,
    focusState: FocusOverviewState,
) {
    val indicatorPalette = rememberOverviewIndicatorPalette()
    val points = focusState.data?.monthly.orEmpty()
    OverviewSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            TrendCardHeader(stringResource(R.string.monthly_trend), monthlyStats.sumOf { it.completedCount }, "monthly")
            MonthlyTrendChart(
                data = monthlyStats,
                dateLabels = points.takeIf { it.size == monthlyStats.size }?.map { it.startDate.substring(5, 7).toInt().toString() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp).testTag("task-trend-monthly"),
                totalColor = indicatorPalette.total,
                completedColor = indicatorPalette.completed,
                overdueColor = indicatorPalette.pending,
            )
            FocusTrendSection(focusState, points, FocusChartKind.MONTHLY, "focus-trend-monthly", focusState.data?.recentMonthsSummary)
        }
    }
}

@Composable
private fun PrevWeeksCard(
    weeklyStats: List<WeeklyStat>,
    focusState: FocusOverviewState,
) {
    val indicatorPalette = rememberOverviewIndicatorPalette()
    val points = focusState.data?.rollingWeekly.orEmpty()
    OverviewSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            TrendCardHeader(stringResource(R.string.prev4weeks), weeklyStats.sumOf { it.completedCount }, "weekly")
            WeeklyBarChart(
                data = weeklyStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp).testTag("task-trend-weekly"),
                barColor = indicatorPalette.pending,
            )
            FocusTrendSection(focusState, points, FocusChartKind.WEEKLY, "focus-trend-weekly", focusState.data?.recentWeeksSummary)
        }
    }
}

@Composable
private fun TrendCardHeader(title: String, completed: Int, period: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.focus_stats_task_count, completed), Modifier.testTag("task-total-$period"),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun OverviewSurfaceCard(
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .padding(16.dp, 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun HeatLegend(
    lowLabel: String,
    highLabel: String,
    levels: List<Color>,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = lowLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
        levels.forEach { color ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.overview_corner_radius_xs)))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
        Text(
            text = highLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun colorForContribution(count: Int): Color {
    return rememberOverviewHeatmapColor(
        role = OverviewIndicatorRole.Completed,
        count = count,
    )
}
