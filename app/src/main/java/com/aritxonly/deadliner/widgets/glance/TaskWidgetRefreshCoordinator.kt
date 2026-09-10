package com.aritxonly.deadliner.widgets.glance

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.updateAll
import com.deadliner.platform.AndroidWidgetUpdateBridge
import com.aritxonly.deadliner.widgets.glance.collection.TaskWidgetCollectionRemoteViews
import com.aritxonly.deadliner.widgets.glance.data.TaskWidgetSnapshotLoader
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetRenderStateSource
import com.aritxonly.deadliner.widgets.glance.ui.CapsuleTaskGlanceWidget
import com.aritxonly.deadliner.widgets.glance.ui.CompactTaskGlanceWidget
import com.aritxonly.deadliner.widgets.glance.ui.CompactTaskGlanceWidgetReceiver
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetPreferences
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfigurationStore
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TaskWidgetRefreshCoordinator {
    private const val TAG = "TaskWidgetRefresh"
    private const val PREFERENCES_NAME = "app_settings"
    private const val REFRESH_DEBOUNCE_MS = 300L

    private val widgetRefreshKeys = setOf(
        "appearance_design_mode",
        "seed_color",
        "appearance_color_source",
        "dynamic_palette_style",
        "modern_use_device_palette_strategy",
        "modern_color_palette",
        TaskWidgetPreferences.PADDING_PRESET_KEY,
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var applicationContext: Context? = null
    private var preferences: SharedPreferences? = null
    private var preferenceListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var refreshJob: Job? = null

    @Synchronized
    fun start(context: Context) {
        if (applicationContext != null) return

        applicationContext = context.applicationContext
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in widgetRefreshKeys) requestRefresh(context, force = true)
        }.also { preferences?.registerOnSharedPreferenceChangeListener(it) }
        AndroidWidgetUpdateBridge.setHandler { requestRefresh(context) }
    }

    @Synchronized
    fun requestRefresh(context: Context, force: Boolean = false) {
        val appContext = context.applicationContext
        refreshJob?.cancel()
        refreshJob = scope.launch {
            delay(REFRESH_DEBOUNCE_MS)
            if (publishLatestSnapshot(appContext, force)) {
                updateAllWidgets(appContext)
                notifyScrollableCollections(appContext)
            }
        }
    }

    internal fun requestRefreshAfterConfiguration(
        context: Context,
        appWidgetId: Int,
        kind: TaskWidgetKind,
    ) {
        val appContext = context.applicationContext
        TaskWidgetRenderStateSource.store.invalidateConfiguration()
        scope.launch {
            val glanceId = AppWidgetId(appWidgetId)
            updateInstance(appContext, glanceId, kind)
            if (kind == TaskWidgetKind.SQUARE) {
                TaskWidgetCollectionRemoteViews.notifyDataChanged(appContext, appWidgetId)
            }
        }
    }

    private suspend fun publishLatestSnapshot(context: Context, force: Boolean): Boolean {
        val snapshot = runCatching { TaskWidgetSnapshotLoader.load(context) }
            .onFailure { error -> Log.w(TAG, "Unable to load widget snapshot", error) }
            .getOrNull()
            ?: return false
        return TaskWidgetRenderStateSource.store.publish(snapshot, forceRevision = force)
    }

    private suspend fun updateAllWidgets(context: Context) {
        runCatching { CompactTaskGlanceWidget().updateAll(context) }
            .onFailure { error -> Log.w(TAG, "Unable to refresh 2x2 widgets", error) }
        runCatching { CapsuleTaskGlanceWidget().updateAll(context) }
            .onFailure { error -> Log.w(TAG, "Unable to refresh 2x1 widgets", error) }
    }

    private suspend fun updateInstance(
        context: Context,
        glanceId: AppWidgetId,
        kind: TaskWidgetKind,
    ) {
        runCatching {
            when (kind) {
                TaskWidgetKind.SQUARE -> CompactTaskGlanceWidget().update(context, glanceId)
                TaskWidgetKind.CAPSULE -> CapsuleTaskGlanceWidget().update(context, glanceId)
            }
        }.onFailure { error -> Log.w(TAG, "Unable to refresh widget instance", error) }
    }

    private fun notifyScrollableCollections(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val store = TaskWidgetConfigurationStore.from(context)
        manager.getAppWidgetIds(ComponentName(context, CompactTaskGlanceWidgetReceiver::class.java))
            .filter { appWidgetId ->
                store.read(appWidgetId, TaskWidgetKind.SQUARE).isScrollable
            }
            .forEach { appWidgetId ->
                TaskWidgetCollectionRemoteViews.notifyDataChanged(context, appWidgetId)
            }
    }

}
