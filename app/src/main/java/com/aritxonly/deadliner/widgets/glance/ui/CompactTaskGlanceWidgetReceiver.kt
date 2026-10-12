package com.aritxonly.deadliner.widgets.glance.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.aritxonly.deadliner.widgets.glance.TaskWidgetRefreshCoordinator
import com.aritxonly.deadliner.widgets.glance.configuration.TaskWidgetHostEditor
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfigurationStore

class CompactTaskGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactTaskGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: intArrayOf()
            TaskWidgetHostEditor.registerMiuiEditUri(context, appWidgetIds)
        }
        if (intent.action in refreshActions) {
            TaskWidgetRefreshCoordinator.requestRefresh(context, force = true)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        TaskWidgetConfigurationStore.from(context).delete(appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }

    private companion object {
        val refreshActions = setOf(
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_BOOT_COMPLETED,
        )
    }
}
