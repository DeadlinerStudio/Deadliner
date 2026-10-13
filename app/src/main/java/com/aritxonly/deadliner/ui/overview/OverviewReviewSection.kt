package com.aritxonly.deadliner.ui.overview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R

@Composable
internal fun OverviewReviewSection(snapshot: OverviewSnapshot?, failed: Boolean, period: String,
    onOpen: () -> Unit, onReload: () -> Unit) {
    var retry by remember { mutableIntStateOf(0) }
    val insight by rememberMonthlyInsight(snapshot, retry)
    DashboardDataCard(
        title = stringResource(R.string.dashboard_month_review),
        value = "",
        supporting = period,
        onClick = onOpen,
        modifier = Modifier.testTag("dashboard-monthly-review"),
        compact = true,
        failed = failed || (snapshot != null && !insight.loading && insight.analysis == null),
        onRetry = { if (failed) onReload() else retry++ },
        supportingContent = {
            Text(when {
                failed -> stringResource(R.string.dashboard_load_error)
                snapshot == null -> stringResource(R.string.dashboard_loading)
                insight.loading -> stringResource(R.string.dashboard_ai_analyzing)
                else -> insight.analysis?.summary ?: stringResource(R.string.monthly_ai_unavailable)
            }, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            insight.analysis?.keywords?.takeIf { it.isNotEmpty() }?.let { keywords ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keywords.forEach { keyword ->
                        Surface(shape = RoundedCornerShape(dimensionResource(R.dimen.overview_corner_radius_pill)),
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(keyword, Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            Text(stringResource(R.string.dashboard_review_entry), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
}
