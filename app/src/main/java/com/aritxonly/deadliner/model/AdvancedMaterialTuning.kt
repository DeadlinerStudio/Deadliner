package com.aritxonly.deadliner.model

import kotlin.math.abs

object AdvancedMaterialTuning {
    const val MinBlurRadius = 16f
    const val DefaultBlurRadius = 52f
    const val MaxBlurRadius = 92f
    fun normalizeBlurRadius(value: Float): Float = if (value.isFinite()) {
        value.coerceIn(MinBlurRadius, MaxBlurRadius)
    } else {
        DefaultBlurRadius
    }

    fun saturationForBlurRadius(value: Float): Float {
        val radius = normalizeBlurRadius(value)
        val levels = AdvancedMaterialLevel.entries
        val upperIndex = levels.indexOfFirst { radius <= it.blurRadius }
        if (upperIndex <= 0) return levels.first().blurSaturation

        val upper = levels[upperIndex]
        val lower = levels[upperIndex - 1]
        val fraction = (radius - lower.blurRadius) / (upper.blurRadius - lower.blurRadius)
        return lower.blurSaturation + (upper.blurSaturation - lower.blurSaturation) * fraction
    }

    fun nearestLevel(value: Float): AdvancedMaterialLevel {
        val radius = normalizeBlurRadius(value)
        return AdvancedMaterialLevel.entries.minBy { level -> abs(level.blurRadius - radius) }
    }

    fun withLevel(
        appearance: AppearancePreferences,
        level: AdvancedMaterialLevel,
    ): AppearancePreferences = appearance.copy(
        advancedMaterialLevel = level,
        advancedMaterialBlurRadius = level.blurRadius,
    )

    fun withBlurRadius(
        appearance: AppearancePreferences,
        value: Float,
    ): AppearancePreferences {
        val radius = normalizeBlurRadius(value)
        return appearance.copy(
            advancedMaterialLevel = nearestLevel(radius),
            advancedMaterialBlurRadius = radius,
        )
    }
}
