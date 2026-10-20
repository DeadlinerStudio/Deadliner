package com.aritxonly.deadliner.widgets.glance.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import com.aritxonly.deadliner.widgets.glance.data.TaskWidgetSnapshotLoader
import com.aritxonly.deadliner.widgets.glance.interaction.TaskWidgetActions
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetRenderStateSource
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfigurationStore
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetBackgroundRenderer
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetThemeResolver

class CompactTaskGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = TaskWidgetSnapshotLoader.load(context)
        TaskWidgetRenderStateSource.store.publish(snapshot)
        val appWidgetId = (id as AppWidgetId).appWidgetId
        provideContent {
            CompactTaskWidgetContent(
                snapshot = snapshot,
                appWidgetId = appWidgetId,
            )
        }
    }
}

@Composable
private fun CompactTaskWidgetContent(
    snapshot: TaskWidgetSnapshot,
    appWidgetId: Int,
) {
    // update() does not restart provideGlance while its composition is active. Observe the
    // published render state so database and per-instance configuration changes are immediate.
    val renderState by TaskWidgetRenderStateSource.store.state.collectAsState()
    val currentSnapshot = renderState.snapshot ?: snapshot
    val context = LocalContext.current
    val configuration = remember(renderState.revision, appWidgetId) {
        TaskWidgetConfigurationStore.from(context).read(
            appWidgetId = appWidgetId,
            kind = TaskWidgetKind.SQUARE,
        )
    }
    val colors = TaskWidgetThemeResolver.resolve(
        context = context,
        visualState = currentSnapshot.visualState,
        widgetColorScheme = configuration.colorScheme,
        themeMode = configuration.themeMode,
    )
    val background = remember(colors) {
        ImageProvider(TaskWidgetBackgroundRenderer.renderSquare(colors))
    }
    val openAppAction = remember(appWidgetId) {
        TaskWidgetActions.openAppAction(context, appWidgetId)
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(
                top = configuration.topMarginDp().dp,
                bottom = configuration.bottomMarginDp().dp,
            ),
    ) {
        TaskWidgetLayeredCard(
            background = background,
            backgroundAlpha = configuration.backgroundAlpha(),
            cornerRadiusDp = configuration.cornerRadiusDp,
            openAppAction = openAppAction,
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(TaskWidgetLayoutMetrics.CARD_CONTENT_PADDING_DP.dp),
            ) {
                TaskWidgetHeader(snapshot = currentSnapshot, colors = colors)
                Spacer(modifier = GlanceModifier.height(TaskWidgetLayoutMetrics.HEADER_GAP_DP.dp))

                if (currentSnapshot.items.isEmpty()) {
                    TaskWidgetEmptyState(
                        colors = colors,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                } else if (configuration.isScrollable) {
                    ScrollableCompactTaskList(
                        snapshot = currentSnapshot,
                        colors = colors,
                        appWidgetId = appWidgetId,
                    )
                } else {
                    StaticCompactTaskList(
                        snapshot = currentSnapshot,
                        colors = colors,
                    )
                }
            }
        }
    }
}
