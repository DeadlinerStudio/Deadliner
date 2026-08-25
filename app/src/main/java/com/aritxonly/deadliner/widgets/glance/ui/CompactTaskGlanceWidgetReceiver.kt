package com.aritxonly.deadliner.widgets.glance.ui

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.aritxonly.deadliner.widgets.glance.TaskWidgetRefreshCoordinator

class CompactTaskGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactTaskGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in refreshActions) {
            TaskWidgetRefreshCoordinator.requestRefresh(context)
        }
    }

    private companion object {
        val refreshActions = setOf(
            Intent.ACTION_CONFIGURATION_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_BOOT_COMPLETED,
        )
    }
}
