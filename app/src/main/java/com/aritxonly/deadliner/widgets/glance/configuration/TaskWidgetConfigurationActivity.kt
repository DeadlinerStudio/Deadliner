package com.aritxonly.deadliner.widgets.glance.configuration

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.glance.appwidget.AppWidgetId
import androidx.lifecycle.lifecycleScope
import com.aritxonly.deadliner.DeadlinerComponentActivity
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfiguration
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfigurationStore
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import com.aritxonly.deadliner.widgets.glance.ui.CapsuleTaskGlanceWidget
import com.aritxonly.deadliner.widgets.glance.ui.CapsuleTaskGlanceWidgetReceiver
import com.aritxonly.deadliner.widgets.glance.ui.CompactTaskGlanceWidget
import com.aritxonly.deadliner.widgets.glance.ui.CompactTaskGlanceWidgetReceiver
import kotlinx.coroutines.launch

class TaskWidgetConfigurationActivity : DeadlinerComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()
        super.onCreate(savedInstanceState)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
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
                    onCancel = ::finish,
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
        store.write(appWidgetId, kind, configuration)
        lifecycleScope.launch {
            val glanceId = AppWidgetId(appWidgetId)
            runCatching {
                when (kind) {
                    TaskWidgetKind.SQUARE -> CompactTaskGlanceWidget().update(this@TaskWidgetConfigurationActivity, glanceId)
                    TaskWidgetKind.CAPSULE -> CapsuleTaskGlanceWidget().update(this@TaskWidgetConfigurationActivity, glanceId)
                }
            }
            setResult(Activity.RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent(): Intent = Intent().putExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        appWidgetId,
    )
}
