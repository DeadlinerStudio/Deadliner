package com.aritxonly.deadliner.widgets.glance.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.toArgb
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.theme.DeadlinerColorTokenFactory
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetVisualState
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetColorScheme

internal object TaskWidgetThemeResolver {
    fun resolve(
        context: Context,
        visualState: TaskWidgetVisualState,
        widgetColorScheme: TaskWidgetColorScheme = TaskWidgetColorScheme.FOLLOW_APP,
    ): TaskWidgetColors {
        val isDark = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val appearance = GlobalUtils.appearancePreferences
        val systemAccent = context.getColor(
            if (isDark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600
        )
        val appAccent = DeadlinerColorTokenFactory.createBaseMaterialColorScheme(
            context = context,
            seedColorHex = appearance.seedColorHex,
            colorSource = appearance.effectiveColorSource,
            dynamicPaletteStyle = appearance.dynamicPaletteStyle,
            darkTheme = isDark,
            dynamicColor = true,
        ).primary.toArgb()
        return TaskWidgetPaletteFactory.create(
            appearance = appearance,
            isDark = isDark,
            systemAccent = systemAccent,
            devicePalette = GlobalUtils.getCurrentDeviceColorPalette(),
            visualState = visualState,
            appAccent = appAccent,
            widgetColorScheme = widgetColorScheme,
        )
    }
}
