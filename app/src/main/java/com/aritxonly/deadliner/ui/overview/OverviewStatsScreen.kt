package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.AnimatedItem
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.main.shared.mainListContainerClip
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun OverviewStatsScreen(
    activeStats: Map<String, Int>,
    historyStats: Map<String, Int>,
    completionTimeStats: List<Pair<String, Int>>,
    modifier: Modifier,
    topContentPadding: Dp = 0.dp,
    listState: LazyListState,
    focusState: FocusOverviewState = FocusOverviewState(),
    includeFocus: Boolean = true,
    monthData: com.deadliner.viewmodel.overview.OverviewMonthUiState? = null,
    bottomContentPadding: Dp = 0.dp,
) {
    val overviewItems = listOf<@Composable () -> Unit>(
        { ActiveStatsCard(activeStats, focusState, includeFocus) },
        { CompletionTimeCard(completionTimeStats) },
        { HistoryStatsCard(historyStats, focusState, includeFocus) }
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .testTag("overview-focus-overview-list")
            .syncTopBarMaterialScroll(listState)
            .mainListContainerClip(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = topContentPadding, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        monthData?.let { month ->
            item("selected-month-tasks") {
                Card(modifier = Modifier.padding(16.dp, 8.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.dashboard_selected_task_month) + " · " + month.monthStart.take(7), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.dashboard_count, month.completedCount), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.dashboard_completion_date_scope), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        DailyBarChart(month.daily.map { DailyStat(java.time.LocalDate.parse(it.date), it.date, it.completedCount, it.overdueCount) },
                            Modifier.fillMaxWidth().height(140.dp), showOverdue = false, compact = true)
                    }
                }
            }
        }
        itemsIndexed(overviewItems) { index, itemContent ->
            AnimatedItem(delayMillis = index * 100L) {
                itemContent()
            }
        }
        if (includeFocus) focusState.data?.thisMonth?.let { focus ->
            item(key = "focus-distribution") { FocusDistributionCard(focus) }
        }

        item {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.overview_bottom_safe_area)))
        }
    }
}

@Composable
fun HistoryStatsCard(
    historyStats: Map<String, Int>,
    focusState: FocusOverviewState = FocusOverviewState(),
    includeFocus: Boolean = true,
) {
    val indicatorPalette = rememberOverviewIndicatorPalette()
    val indicatorRoles = listOf(
        OverviewIndicatorRole.Completed,
        OverviewIndicatorRole.Pending,
        OverviewIndicatorRole.Abandoned,
        OverviewIndicatorRole.Overdue,
    )
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .padding(16.dp, 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.task_status_summary),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                historyStats.entries.forEachIndexed { index, (key, value) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = indicatorPalette.colorFor(
                                indicatorRoles.getOrElse(index) { OverviewIndicatorRole.Total }
                            ),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(key, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (includeFocus) FocusHistorySummary(focusState)
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                NewPieChart(statistics = historyStats)
            }
        }
    }
}

@Composable
fun CompletionTimeCard(
    completionTimeStats: List<Pair<String, Int>>,
) {
    val indicatorPalette = rememberOverviewIndicatorPalette()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .padding(16.dp, 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius))),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.task_complete_time_summary),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 使用条形图展示完成时间段统计
            NewBarChartCompletionTimeStats(
                data = completionTimeStats,
                barColor = indicatorPalette.total,
                textColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ActiveStatsCard(
    activeStats: Map<String, Int>,
    focusState: FocusOverviewState = FocusOverviewState(),
    includeFocus: Boolean = true,
) {
    val indicatorPalette = rememberOverviewIndicatorPalette()
    val indicatorRoles = listOf(
        OverviewIndicatorRole.Completed,
        OverviewIndicatorRole.Pending,
        OverviewIndicatorRole.Overdue,
        OverviewIndicatorRole.Abandoned,
    )
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .padding(16.dp, 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.item_corner_radius))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.focus_stats_today_overview),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (includeFocus) FocusTodaySummary(focusState)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                activeStats.entries.forEachIndexed { index, (key, value) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = indicatorPalette.colorFor(
                                indicatorRoles.getOrElse(index) { OverviewIndicatorRole.Total }
                            ),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(key, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
