package com.aritxonly.deadliner.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class HomeFabColors(
    val container: Color,
    val content: Color,
)

val LocalHomeFabColors = staticCompositionLocalOf {
    HomeFabColors(
        container = Color.Unspecified,
        content = Color.Unspecified,
    )
}

internal fun homeFabColors(
    designSystem: AppDesignSystem,
    colorSource: com.aritxonly.deadliner.model.AppearanceColorSource,
    fallbackContainer: Color,
    fallbackContent: Color,
): HomeFabColors = if (
    designSystem == AppDesignSystem.MIUIX &&
    colorSource == com.aritxonly.deadliner.model.AppearanceColorSource.Preset
) {
    HomeFabColors(
        container = MiuixPresetColorSchemes.HomeFab,
        content = Color(0xFF2B100C),
    )
} else {
    HomeFabColors(
        container = fallbackContainer,
        content = fallbackContent,
    )
}
