package com.aritxonly.deadliner.ui.overview

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.deadliner.domain.statistics.FocusDurationBreakdown
import com.deadliner.domain.statistics.FocusStatisticsSummary

/** One inline source breakdown; never infer it from legacy totals or habit check-ins. */
internal fun focusTargetTimeBrief(context: Context, breakdown: FocusDurationBreakdown?): String? =
    breakdown?.takeIf { it.habitDurationSeconds > 0 }?.let {
        context.getString(R.string.focus_stats_target_time_brief,
            focusDuration(context, it.taskDurationSeconds), focusDuration(context, it.habitDurationSeconds))
    }

@Composable
internal fun FocusTargetTimeSummary(summary: FocusStatisticsSummary?, tag: String) {
    focusTargetTimeBrief(LocalContext.current, summary?.durationByTarget)?.let { brief ->
        Text(brief, Modifier.padding(top = 4.dp).testTag(tag),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
