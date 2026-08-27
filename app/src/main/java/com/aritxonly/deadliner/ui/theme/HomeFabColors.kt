package com.aritxonly.deadliner.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.model.AppearanceColorSource

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
    colorSource: AppearanceColorSource,
    seedColorHex: String?,
    darkTheme: Boolean,
    fallbackContainer: Color,
    useContrastingPresetFab: Boolean,
): HomeFabColors = HomeFabColors(
    container = when (colorSource) {
        AppearanceColorSource.Preset -> if (useContrastingPresetFab) {
            MiuixPresetColorSchemes.HomeFab
        } else {
            MiuixPresetColorSchemes.seed(darkTheme)
        }
        AppearanceColorSource.SeedColor -> parseSeedColor(seedColorHex) ?: fallbackContainer
        AppearanceColorSource.SystemDynamic -> fallbackContainer
    },
    content = Color.White,
)

internal fun parseSeedColor(raw: String?): Color? {
    val normalized = raw
        ?.trim()
        ?.removePrefix("#")
        ?.takeIf { it.length == 6 || it.length == 8 }
        ?: return null
    val argb = runCatching {
        val opaqueHex = if (normalized.length == 6) "FF$normalized" else normalized
        opaqueHex.toLong(radix = 16).toInt()
    }.getOrNull() ?: return null
    return Color(argb).copy(alpha = 1f)
}
