package com.aritxonly.deadliner

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import com.aritxonly.deadliner.logging.AppLog
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aritxonly.deadliner.localutils.DynamicColorsExtension
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.UiStyle
import com.aritxonly.deadliner.ui.main.modern.ModernHost
import com.aritxonly.deadliner.ui.main.simplified.SimplifiedHost
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.aritxonly.deadliner.widgets.HabitMiniWidget
import com.aritxonly.deadliner.widgets.LargeDeadlineWidget
import com.aritxonly.deadliner.widgets.MultiDeadlineWidget
import com.aritxonly.deadliner.widgets.glance.TaskWidgetRefreshCoordinator
import com.deadliner.platform.PlatformLogSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : DeadlinerAppCompatActivity() {
    private val _showSearch = MutableStateFlow(false)
    val showSearch: StateFlow<Boolean> = _showSearch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.initialize(applicationContext)
        PlatformLogSink.configure(filesDir.absolutePath, android.os.Process.myPid().toLong())

        enableEdgeToEdgeForAllDevices()

        DynamicColorsExtension.apply(this, GlobalUtils.seedColor)

        _showSearch.value = intent.getBooleanExtra("EXTRA_SHOW_SEARCH", false)

        setContent {
            val searchActive by showSearch.collectAsState()
            val style by GlobalUtils.styleFlow.collectAsState()

            DeadlinerTheme {
                when (style) {
                    UiStyle.Simplified  -> {
                        SimplifiedHost(
                            searchActive = searchActive,
                            onSearchActiveChange = { _showSearch.value = it },
                            activity = this
                        )
                    }
                    UiStyle.Miuix -> {
                        ModernHost(
                            searchActive = searchActive,
                            onSearchActiveChange = { _showSearch.value = it },
                            activity = this
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateWidget()
    }

    override fun onStop() {
        super.onStop()
        updateWidget()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        _showSearch.value = intent.getBooleanExtra("EXTRA_SHOW_SEARCH", false)

        AppLog.d("Search", _showSearch.value.toString())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
    }

    fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, MultiDeadlineWidget::class.java))
        for (appWidgetId in appWidgetIds) {
            MultiDeadlineWidget.updateWidget(this, appWidgetManager, appWidgetId)
        }

        val largeWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, LargeDeadlineWidget::class.java))
        for (largeWidgetId in largeWidgetIds) {
            LargeDeadlineWidget.updateWidget(this, appWidgetManager, largeWidgetId)
        }

        val habitMiniWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this,
            HabitMiniWidget::class.java))
        for (habitMiniWidgetId in habitMiniWidgetIds) {
            HabitMiniWidget.updateWidget(this, appWidgetManager, habitMiniWidgetId)
        }

        TaskWidgetRefreshCoordinator.requestRefresh(this)
    }
}
