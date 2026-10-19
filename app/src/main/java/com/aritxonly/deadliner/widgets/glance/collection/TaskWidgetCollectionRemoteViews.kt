package com.aritxonly.deadliner.widgets.glance.collection

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.RemoteViews
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetItem
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColors
import com.aritxonly.deadliner.widgets.glance.interaction.TaskWidgetActions

internal object TaskWidgetCollectionRemoteViews {
    const val EXTRA_TASK_STABLE_ID = "task_widget_stable_id"

    @Suppress("DEPRECATION")
    fun create(
        context: Context,
        appWidgetId: Int,
        emptyTextColor: Int,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_task_collection).apply {
        setRemoteAdapter(
            R.id.widget_task_collection_list,
            adapterIntent(context, appWidgetId),
        )
        setPendingIntentTemplate(
            R.id.widget_task_collection_list,
            TaskWidgetActions.openCollectionPendingIntent(context, appWidgetId),
        )
        setEmptyView(
            R.id.widget_task_collection_list,
            R.id.widget_task_collection_empty,
        )
        setTextColor(R.id.widget_task_collection_empty, emptyTextColor)
    }

    fun createRow(
        context: Context,
        item: TaskWidgetItem,
        colors: TaskWidgetColors,
        background: Bitmap,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_task_collection_row).apply {
        val rowColors = colors.rowColors(item.status)
        setImageViewBitmap(R.id.widget_task_collection_row_background, background)
        setInt(
            R.id.widget_task_collection_row_indicator,
            "setColorFilter",
            item.indicatorColorArgb(rowColors.indicator),
        )
        setTextViewText(R.id.widget_task_collection_row_title, item.title)
        setTextColor(R.id.widget_task_collection_row_title, colors.textPrimary)
        setTextViewText(R.id.widget_task_collection_row_time, item.timeDescription)
        setTextColor(R.id.widget_task_collection_row_time, rowColors.timeText)
    }

    fun adapterIntent(context: Context, appWidgetId: Int): Intent =
        Intent(context, TaskWidgetCollectionService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.Builder()
                .scheme("deadliner")
                .authority("widget-list")
                .appendPath(appWidgetId.toString())
                .build()
        }

    fun notifyDataChanged(context: Context, appWidgetId: Int) {
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
            appWidgetId,
            R.id.widget_task_collection_list,
        )
    }
}
