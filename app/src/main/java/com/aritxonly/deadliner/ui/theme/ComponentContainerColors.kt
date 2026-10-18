package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.model.AppearanceColorSource
import com.aritxonly.deadliner.model.BackgroundColorStrategy
import com.materialkolor.ktx.harmonize
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/** Component surfaces only: this never modifies either global theme's palette. */
internal fun componentContainerColor(
    material: ColorScheme,
    strategy: BackgroundColorStrategy,
    colorSource: AppearanceColorSource,
    darkTheme: Boolean,
): Color {
    if (strategy == BackgroundColorStrategy.MaterialMonet) return material.secondaryContainer
    val neutral = (if (darkTheme) darkColorScheme() else lightColorScheme()).secondaryContainer
    return if (colorSource == AppearanceColorSource.Preset) neutral
        else neutral.harmonize(material.secondary)
}

internal fun textFieldContainerColors(
    material: ColorScheme,
    strategy: BackgroundColorStrategy,
    colorSource: AppearanceColorSource,
    darkTheme: Boolean,
): TextFieldContainerColors {
    val base = componentContainerColor(material, strategy, colorSource, darkTheme)
    return TextFieldContainerColors(base, base.copy(alpha = 0.65f))
}
