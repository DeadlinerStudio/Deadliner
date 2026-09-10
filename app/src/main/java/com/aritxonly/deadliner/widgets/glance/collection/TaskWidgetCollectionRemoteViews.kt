package com.aritxonly.deadliner.widgets.glance.collection

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.aritxonly.deadliner.R
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
