package com.aritxonly.deadliner.widgets.glance.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import com.aritxonly.deadliner.LauncherActivity
import com.aritxonly.deadliner.widgets.glance.data.AndroidTaskWidgetStrings
import com.aritxonly.deadliner.widgets.glance.data.KmpTaskWidgetDataSource
import com.aritxonly.deadliner.widgets.glance.data.TaskWidgetRepository
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfiguration
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfigurationStore
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetBackgroundRenderer
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColors
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetThemeResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CompactTaskGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(150.dp, 150.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) {
            TaskWidgetRepository(KmpTaskWidgetDataSource()).load(AndroidTaskWidgetStrings(context))
        }
        val appWidgetId = (id as AppWidgetId).appWidgetId
        val configuration = TaskWidgetConfigurationStore.from(context).read(
            appWidgetId = appWidgetId,
            kind = TaskWidgetKind.SQUARE,
        )
        val colors = TaskWidgetThemeResolver.resolve(
            context = context,
            visualState = snapshot.visualState,
            widgetColorScheme = configuration.colorScheme,
        )
        val rowBackgrounds = TaskWidgetRowBackgrounds(
            normal = ImageProvider(TaskWidgetBackgroundRenderer.renderRow(colors, TaskWidgetStatus.NORMAL)),
            near = ImageProvider(TaskWidgetBackgroundRenderer.renderRow(colors, TaskWidgetStatus.NEAR)),
            overdue = ImageProvider(TaskWidgetBackgroundRenderer.renderRow(colors, TaskWidgetStatus.OVERDUE)),
        )
        provideContent {
            CompactTaskWidgetContent(
                snapshot = snapshot,
                colors = colors,
                background = ImageProvider(TaskWidgetBackgroundRenderer.renderSquare(colors)),
                rowBackgrounds = rowBackgrounds,
                configuration = configuration,
            )
        }
    }
}

@Composable
private fun CompactTaskWidgetContent(
    snapshot: TaskWidgetSnapshot,
    colors: TaskWidgetColors,
    background: ImageProvider,
    rowBackgrounds: TaskWidgetRowBackgrounds,
    configuration: TaskWidgetConfiguration,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(
                top = configuration.topMarginDp().dp,
                bottom = configuration.bottomMarginDp().dp,
            ),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(imageProvider = background)
                .cornerRadius(configuration.cornerRadiusDp.dp)
                .appWidgetBackground()
                .clickable(actionStartActivity<LauncherActivity>()),
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(10.dp),
            ) {
                TaskWidgetHeader(snapshot = snapshot, colors = colors)
                Spacer(modifier = GlanceModifier.height(7.dp))

                if (snapshot.items.isEmpty()) {
                    TaskWidgetEmptyState(colors)
                } else {
                    val visibleItems = snapshot.items.take(configuration.maxVisibleTasks())
                    visibleItems.forEachIndexed { index, item ->
                        TaskWidgetItemRow(item, colors, rowBackgrounds.forStatus(item.status))
                        if (index != visibleItems.lastIndex) {
                            Spacer(modifier = GlanceModifier.height(5.dp))
                        }
                    }
                }
            }
        }
    }
}
