package com.aritxonly.deadliner.widgets.glance

import android.content.ComponentCallbacks
import android.content.res.Configuration

/** Application callbacks also receive configuration changes while activities are stopped. */
internal class TaskWidgetSystemThemeObserver(
    initialConfiguration: Configuration,
    private val onThemeChanged: () -> Unit,
) : ComponentCallbacks {
    private var nightMode = initialConfiguration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    override fun onConfigurationChanged(newConfig: Configuration) {
        val nextNightMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nextNightMode == nightMode) return
        nightMode = nextNightMode
        onThemeChanged()
    }

    override fun onLowMemory() = Unit
}
