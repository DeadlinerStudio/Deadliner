package com.aritxonly.deadliner.ui.material.glass

internal object SoftGlassPressFeedbackDefaults {
    const val PressedScale = 0.94f
    const val RefractionScaleBoost = 0.06f
    const val HighlightScaleBoost = 0.12f
    const val LightHighlightAlpha = 0.07f
    const val DarkHighlightAlpha = 0.11f
}

internal fun softGlassPressScale(progress: Float): Float {
    val fraction = progress.coerceIn(0f, 1f)
    return 1f + (SoftGlassPressFeedbackDefaults.PressedScale - 1f) * fraction
}

internal fun softGlassPressHighlightAlpha(
    progress: Float,
    isDark: Boolean,
): Float = progress.coerceIn(0f, 1f) * if (isDark) {
    SoftGlassPressFeedbackDefaults.DarkHighlightAlpha
} else {
    SoftGlassPressFeedbackDefaults.LightHighlightAlpha
}

internal fun softGlassPressMaterialInteraction(progress: Float): GlassMaterialInteraction {
    val fraction = progress.coerceIn(0f, 1f)
    return GlassMaterialInteraction(
        refractionScale = 1f + SoftGlassPressFeedbackDefaults.RefractionScaleBoost * fraction,
        highlightScale = 1f + SoftGlassPressFeedbackDefaults.HighlightScaleBoost * fraction,
    )
}
