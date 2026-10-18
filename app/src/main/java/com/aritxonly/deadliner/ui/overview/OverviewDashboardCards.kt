package com.aritxonly.deadliner.ui.overview

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R

@Composable
internal fun DashboardSurface(modifier: Modifier = Modifier, contentPadding: androidx.compose.ui.unit.Dp = 20.dp, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(contentPadding), content = content)
    }
}

@Composable
internal fun DashboardDataCard(title: String, value: String, supporting: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, failed: Boolean = false, onRetry: () -> Unit = {},
    supportingContent: (@Composable () -> Unit)? = null, compact: Boolean = false,
    valueContent: (@Composable () -> Unit)? = null) {
    Card(onClick = onClick, modifier = (modifier.fillMaxWidth()).miuixPressFeedback(enabled = true, shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))),
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(if (compact) 16.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_chevron_right_wght400),
                    contentDescription = null, modifier = Modifier.size(com.aritxonly.deadliner.ui.overviewChevronSize()),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (valueContent != null) valueContent() else if (value.isNotBlank()) Text(value, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            if (supporting.isNotBlank()) Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            supportingContent?.invoke()
            if (failed) com.aritxonly.deadliner.ui.base.TextButton(onClick = onRetry) {
                Text(stringResource(R.string.dashboard_retry))
            }
        }
    }
}
