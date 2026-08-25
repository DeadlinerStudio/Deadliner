package com.aritxonly.deadliner.widgets.glance

import android.content.Context
import android.content.SharedPreferences
import androidx.glance.appwidget.updateAll
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.widgets.glance.ui.CompactTaskGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

object TaskWidgetRefreshCoordinator {
    private const val PREFERENCES_NAME = "app_settings"
    private const val REFRESH_DEBOUNCE_MS = 150L

    private val appearanceKeys = setOf(
        "appearance_design_mode",
        "seed_color",
        "dynamic_palette_style",
        "modern_use_device_palette_strategy",
        "modern_color_palette",
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
            if (key in appearanceKeys) requestRefresh(context)
        }.also { preferences?.registerOnSharedPreferenceChangeListener(it) }

        scope.launch {
            KmpV6Runtime.taskListViewModel.uiState
                .map { state -> state.tasks }
                .distinctUntilChanged()
                .drop(1)
                .collect { requestRefresh(context) }
        }
    }

    @Synchronized
    fun requestRefresh(context: Context) {
        val appContext = context.applicationContext
        refreshJob?.cancel()
        refreshJob = scope.launch {
            delay(REFRESH_DEBOUNCE_MS)
            runCatching { CompactTaskGlanceWidget().updateAll(appContext) }
        }
    }
}
