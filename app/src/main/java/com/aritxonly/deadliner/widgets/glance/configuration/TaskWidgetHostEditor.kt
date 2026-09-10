package com.aritxonly.deadliner.widgets.glance.configuration

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import com.aritxonly.deadliner.widgets.glance.ui.CapsuleTaskGlanceWidgetReceiver
import com.aritxonly.deadliner.widgets.glance.ui.CompactTaskGlanceWidgetReceiver

internal enum class TaskWidgetEditorSource {
    HOST,
    SETTINGS,
}

internal data class TaskWidgetInstance(
    val appWidgetId: Int,
    val kind: TaskWidgetKind,
)

/** Bridges the standard AppWidget configuration flow and Xiaomi's launcher editor entry. */
internal object TaskWidgetHostEditor {
    private const val MIUI_EDIT_URI_OPTION = "miuiEditUri"
    private const val MIUI_WIDGET_ID_PARAMETER = "miuiWidgetId"
    private const val EXTRA_EDITOR_SOURCE =
        "com.aritxonly.deadliner.widgets.extra.EDITOR_SOURCE"
    private const val EDIT_URI =
        "deadliner://com.aritxonly.deadliner/widget/TaskWidgetConfigurationActivity"

    fun settingsEditIntent(context: Context, appWidgetId: Int): Intent =
        Intent(context, TaskWidgetConfigurationActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(EXTRA_EDITOR_SOURCE, TaskWidgetEditorSource.SETTINGS.name)

    fun resolveEditorSource(intent: Intent): TaskWidgetEditorSource =
        intent.getStringExtra(EXTRA_EDITOR_SOURCE)
            ?.let { value -> TaskWidgetEditorSource.entries.firstOrNull { it.name == value } }
            ?: TaskWidgetEditorSource.HOST

    fun shouldRemoveEditorTask(source: TaskWidgetEditorSource, isTaskRoot: Boolean): Boolean =
        source == TaskWidgetEditorSource.HOST && isTaskRoot

    fun installedWidgets(context: Context): List<TaskWidgetInstance> {
        val manager = AppWidgetManager.getInstance(context)
        val squareIds = manager.getAppWidgetIds(
            ComponentName(context, CompactTaskGlanceWidgetReceiver::class.java),
        )
        val capsuleIds = manager.getAppWidgetIds(
            ComponentName(context, CapsuleTaskGlanceWidgetReceiver::class.java),
        )
        return mergeInstalledWidgets(squareIds, capsuleIds)
    }

    fun registerMiuiEditUri(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetIds
            .filter { it != AppWidgetManager.INVALID_APPWIDGET_ID }
            .forEach { appWidgetId ->
                val editUri = "$EDIT_URI?$MIUI_WIDGET_ID_PARAMETER=$appWidgetId"
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                if (options.getString(MIUI_EDIT_URI_OPTION) == editUri) return@forEach
                options.putString(MIUI_EDIT_URI_OPTION, editUri)
                appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
            }
    }

    fun resolveAppWidgetId(intent: Intent): Int {
        val standardId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (standardId != AppWidgetManager.INVALID_APPWIDGET_ID) return standardId

        return intent.data
            ?.getQueryParameter(MIUI_WIDGET_ID_PARAMETER)
            ?.toIntOrNull()
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    internal fun mergeInstalledWidgets(
        squareIds: IntArray,
        capsuleIds: IntArray,
    ): List<TaskWidgetInstance> = buildList {
        squareIds.forEach { add(TaskWidgetInstance(it, TaskWidgetKind.SQUARE)) }
        capsuleIds.forEach { add(TaskWidgetInstance(it, TaskWidgetKind.CAPSULE)) }
    }
        .filter { it.appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID }
        .distinctBy(TaskWidgetInstance::appWidgetId)
        .sortedWith(compareBy<TaskWidgetInstance> { it.kind.ordinal }.thenBy { it.appWidgetId })
}
