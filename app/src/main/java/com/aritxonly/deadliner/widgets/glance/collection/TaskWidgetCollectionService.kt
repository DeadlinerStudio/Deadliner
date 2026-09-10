package com.aritxonly.deadliner.widgets.glance.collection

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.widgets.glance.data.TaskWidgetSnapshotLoader
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetSnapshot
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfigurationStore
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetBackgroundRenderer
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColors
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetThemeResolver
import kotlinx.coroutines.runBlocking

class TaskWidgetCollectionService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TaskWidgetCollectionRemoteViewsFactory(
            context = applicationContext,
            appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ),
        )
}

internal class TaskWidgetCollectionRemoteViewsFactory(
    context: Context,
    private val appWidgetId: Int,
    private val snapshotLoader: suspend (Context) -> TaskWidgetSnapshot = TaskWidgetSnapshotLoader::load,
    private val colorsResolver: (Context, Int, TaskWidgetSnapshot) -> TaskWidgetColors =
        { appContext, widgetId, snapshot ->
            val configuration = TaskWidgetConfigurationStore.from(appContext).read(
                appWidgetId = widgetId,
                kind = TaskWidgetKind.SQUARE,
            )
            TaskWidgetThemeResolver.resolve(
                context = appContext,
                visualState = snapshot.visualState,
                widgetColorScheme = configuration.colorScheme,
            )
        },
) : RemoteViewsService.RemoteViewsFactory {
    private val context = context.applicationContext
    private var snapshot = EMPTY_SNAPSHOT
    private var colors: TaskWidgetColors? = null
    private var rowBackgrounds = emptyMap<TaskWidgetStatus, android.graphics.Bitmap>()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        runCatching {
            runBlocking { snapshotLoader(context) }
        }.onSuccess { latest ->
            recycleRowBackgrounds()
            snapshot = latest
            val latestColors = colorsResolver(context, appWidgetId, latest)
            colors = latestColors
            rowBackgrounds = TaskWidgetStatus.entries.associateWith { status ->
                TaskWidgetBackgroundRenderer.renderRow(colors = latestColors, status = status)
            }
        }.onFailure { error ->
            Log.w(TAG, "Unable to load service-backed widget collection", error)
        }
    }

    override fun onDestroy() {
        recycleRowBackgrounds()
    }

    override fun getCount(): Int = if (colors == null) 0 else snapshot.items.size

    override fun getViewAt(position: Int): RemoteViews? {
        val currentColors = colors ?: return null
        val item = snapshot.items.getOrNull(position) ?: return null
        val rowColors = currentColors.rowColors(item.status)
        return RemoteViews(context.packageName, R.layout.widget_task_collection_row).apply {
            setImageViewBitmap(
                R.id.widget_task_collection_row_background,
                rowBackgrounds.getValue(item.status),
            )
            setInt(
                R.id.widget_task_collection_row_indicator,
                "setColorFilter",
                item.indicatorColorArgb(rowColors.indicator),
            )
            setTextViewText(R.id.widget_task_collection_row_title, item.title)
            setTextColor(R.id.widget_task_collection_row_title, currentColors.textPrimary)
            setTextViewText(R.id.widget_task_collection_row_time, item.timeDescription)
            setTextColor(R.id.widget_task_collection_row_time, rowColors.timeText)
            setOnClickFillInIntent(
                R.id.widget_task_collection_row,
                Intent().putExtra(TaskWidgetCollectionRemoteViews.EXTRA_TASK_STABLE_ID, item.stableId),
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        snapshot.items.getOrNull(position)?.stableId ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun recycleRowBackgrounds() {
        rowBackgrounds.values.forEach { bitmap ->
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        rowBackgrounds = emptyMap()
    }

    private companion object {
        const val TAG = "TaskWidgetCollection"
        val EMPTY_SNAPSHOT = TaskWidgetSnapshot(
            totalCount = 0,
            nearCount = 0,
            overdueCount = 0,
            summary = "",
            items = emptyList(),
        )
    }
}
