package com.aritxonly.deadliner.ui.overview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.deadliner.viewmodel.overview.OverviewMonthUiState
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun OverviewTaskResultCard(month: YearMonth,
    current: DashboardLoad<OverviewMonthUiState>, previous: DashboardLoad<OverviewMonthUiState>,
    onOpen: () -> Unit, onRetry: () -> Unit) {
    val comparison = current.data?.let { data -> previous.data?.let { prior ->
        compareOverviewPeriod(month, LocalDate.parse(data.asOfDate),
            data.daily.associate { LocalDate.parse(it.date) to it.completedCount },
            prior.daily.associate { LocalDate.parse(it.date) to it.completedCount })
    } }
    val locale = LocalConfiguration.current.locales[0]
    val palette = rememberOverviewIndicatorPalette()
    DashboardDataCard(stringResource(R.string.dashboard_task_statistics), "",
        "", onOpen,
        Modifier.testTag("dashboard-statistics"), current.failed || previous.failed, onRetry,
        compact = true, supportingContent = { current.data?.let { OverviewTaskStatusLegend(it.statusDistribution) } }, valueContent = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(current.data?.let { stringResource(R.string.dashboard_count, it.completedCount) }
                        ?: stringResource(if (current.loading) R.string.dashboard_loading else R.string.dashboard_unavailable),
                        style = MaterialTheme.typography.titleLarge)
                    if (comparison != null) {
                        val difference = comparison.difference
                        val change = when {
                            difference == 0 -> stringResource(R.string.dashboard_same_period_unchanged)
                            comparison.percent != null -> stringResource(
                                if (difference > 0) R.string.dashboard_same_period_up else R.string.dashboard_same_period_down,
                                String.format(locale, "%.1f%%", kotlin.math.abs(comparison.percent!!)))
                            else -> stringResource(R.string.dashboard_same_period_new, difference)
                        }
                        Text(change, style = MaterialTheme.typography.bodySmall, color = when {
                            difference > 0 -> palette.completed
                            difference < 0 -> palette.overdue
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        })

                    } else Text(stringResource(if (previous.failed) R.string.dashboard_comparison_unavailable else R.string.dashboard_loading),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                current.data?.let { data ->
                    OverviewTaskStatusPie(data.statusDistribution, Modifier.size(88.dp))
                }
            }
        })
}
