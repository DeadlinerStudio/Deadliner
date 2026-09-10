package com.aritxonly.deadliner.widgets.glance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.aritxonly.deadliner.widgets.glance.collection.TaskWidgetCollectionRemoteViews
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColors

@Composable
internal fun ColumnScope.StaticCompactTaskList(
    snapshot: TaskWidgetSnapshot,
    colors: TaskWidgetColors,
) {
    val visibleItems = snapshot.items.take(
        TaskWidgetLayoutMetrics.staticVisibleTaskCount(snapshot.items.size),
    )
    repeat(TaskWidgetLayoutMetrics.STATIC_TASK_LIMIT) { index ->
        val item = visibleItems.getOrNull(index)
        if (item == null) {
            Spacer(modifier = GlanceModifier.defaultWeight())
        } else {
            TaskWidgetItemRow(
                item = item,
                colors = colors,
                openAppAction = null,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
        if (index != TaskWidgetLayoutMetrics.STATIC_TASK_LIMIT - 1) {
            Spacer(modifier = GlanceModifier.height(TaskWidgetLayoutMetrics.ROW_GAP_DP.dp))
        }
    }
}

@Composable
internal fun ColumnScope.ScrollableCompactTaskList(
    colors: TaskWidgetColors,
    appWidgetId: Int,
) {
    val context = LocalContext.current
    val remoteViews = remember(appWidgetId, colors.textSecondary) {
        TaskWidgetCollectionRemoteViews.create(
            context = context,
            appWidgetId = appWidgetId,
            emptyTextColor = colors.textSecondary,
        )
    }
    AndroidRemoteViews(
        remoteViews = remoteViews,
        modifier = GlanceModifier
            .fillMaxWidth()
            .defaultWeight(),
    )
}
