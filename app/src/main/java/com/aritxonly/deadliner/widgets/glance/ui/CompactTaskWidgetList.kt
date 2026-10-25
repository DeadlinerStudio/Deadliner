package com.aritxonly.deadliner.widgets.glance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.aritxonly.deadliner.widgets.glance.collection.TaskWidgetCollectionRemoteViews
import com.aritxonly.deadliner.widgets.glance.interaction.TaskWidgetActions
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetBackgroundRenderer
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
    snapshot: TaskWidgetSnapshot,
    colors: TaskWidgetColors,
    appWidgetId: Int,
) {
    val context = LocalContext.current
    val openAppAction = remember(appWidgetId) {
        TaskWidgetActions.openAppAction(context, appWidgetId)
    }
    // Share the three status bitmaps across all rows to keep the widget payload small.
    val backgrounds = remember(colors) {
        TaskWidgetStatus.entries.associateWith { status ->
            TaskWidgetBackgroundRenderer.renderRow(colors, status)
        }
    }
    // LazyColumn inlines every row into one update, so bound the rows to stay under the
    // Binder limit. The header still shows the full task count.
    val visibleItems = snapshot.items.take(
        TaskWidgetLayoutMetrics.scrollableVisibleTaskCount(snapshot.items.size),
    )
    // Glance must attach the adapter to its outer widget RemoteViews. An adapter inside
    // AndroidRemoteViews can be rejected on host reapply, leaving a nonempty list blank.
    LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
        items(visibleItems, itemId = { it.stableId }) { item ->
            val row = remember(item, colors, backgrounds) {
                TaskWidgetCollectionRemoteViews.createRow(
                    context = context,
                    item = item,
                    colors = colors,
                    background = backgrounds.getValue(item.status),
                )
            }
            AndroidRemoteViews(
                remoteViews = row,
                modifier = GlanceModifier.fillMaxWidth().clickable(openAppAction),
            )
        }
    }
}
