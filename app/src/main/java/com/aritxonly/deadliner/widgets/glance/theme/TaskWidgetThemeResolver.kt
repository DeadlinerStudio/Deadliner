package com.aritxonly.deadliner.widgets.glance.theme

import android.content.Context
import android.content.res.Configuration
import com.aritxonly.deadliner.localutils.GlobalUtils

internal object TaskWidgetThemeResolver {
    fun resolve(context: Context): TaskWidgetColors {
        val isDark = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val systemAccent = context.getColor(
            if (isDark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600
        )
        return TaskWidgetPaletteFactory.create(
            appearance = GlobalUtils.appearancePreferences,
            isDark = isDark,
            systemAccent = systemAccent,
            devicePalette = GlobalUtils.getCurrentDeviceColorPalette(),
        )
    }
}
