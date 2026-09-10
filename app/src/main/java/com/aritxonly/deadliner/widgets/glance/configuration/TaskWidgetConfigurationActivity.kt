package com.aritxonly.deadliner.widgets.glance.configuration

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.aritxonly.deadliner.DeadlinerComponentActivity
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.aritxonly.deadliner.widgets.glance.TaskWidgetRefreshCoordinator
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfiguration
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfigurationStore
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import com.aritxonly.deadliner.widgets.glance.ui.CapsuleTaskGlanceWidgetReceiver
import com.aritxonly.deadliner.widgets.glance.ui.CompactTaskGlanceWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskWidgetConfigurationActivity : DeadlinerComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var editorSource: TaskWidgetEditorSource = TaskWidgetEditorSource.HOST

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()
        super.onCreate(savedInstanceState)

        appWidgetId = TaskWidgetHostEditor.resolveAppWidgetId(intent)
        editorSource = TaskWidgetHostEditor.resolveEditorSource(intent)
        val kind = resolveKind(appWidgetId)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || kind == null) {
            finish()
            return
        }

        setResult(Activity.RESULT_CANCELED, resultIntent())
        val store = TaskWidgetConfigurationStore.from(this)
        val initial = store.read(appWidgetId, kind)
        setContent {
            DeadlinerTheme {
                TaskWidgetConfigurationScreen(
                    kind = kind,
                    initialConfiguration = initial,
                    onCancel = { finishEditor() },
                    onSave = { configuration -> save(kind, configuration, store) },
                )
            }
        }
    }

    private fun resolveKind(id: Int): TaskWidgetKind? {
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return null
        val providerClassName = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(id)
            ?.provider
            ?.className
            ?: return null
        return when (providerClassName) {
            CompactTaskGlanceWidgetReceiver::class.java.name -> TaskWidgetKind.SQUARE
            CapsuleTaskGlanceWidgetReceiver::class.java.name -> TaskWidgetKind.CAPSULE
            else -> null
        }
    }

    private fun save(
        kind: TaskWidgetKind,
        configuration: TaskWidgetConfiguration,
        store: TaskWidgetConfigurationStore,
    ) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                store.write(appWidgetId, kind, configuration)
            }
            TaskWidgetHostEditor.registerMiuiEditUri(
                this@TaskWidgetConfigurationActivity,
                intArrayOf(appWidgetId),
            )
            TaskWidgetRefreshCoordinator.requestRefreshAfterConfiguration(
                context = this@TaskWidgetConfigurationActivity,
                appWidgetId = appWidgetId,
                kind = kind,
            )
            setResult(Activity.RESULT_OK, resultIntent())
            finishEditor()
        }
    }

    private fun finishEditor() {
        if (TaskWidgetHostEditor.shouldRemoveEditorTask(editorSource, isTaskRoot)) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private fun resultIntent(): Intent = Intent().putExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        appWidgetId,
    )
}
