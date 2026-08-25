package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.aritxonly.deadliner.model.AdvancedMaterialTuning
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.BlurDefaults.blurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

@Immutable
data class AdvancedMaterialSpec(
    val enabled: Boolean = false,
    val blurEnabled: Boolean = true,
    val blurRadius: Float = AdvancedMaterialTuning.DefaultBlurRadius,
    val blurRadiusX: Float? = null,
    val blurRadiusY: Float? = null,
    val noiseCoefficient: Float = AdvancedMaterialFineTuning.Default.noiseCoefficient,
    val blurBlendColors: List<BlendColorEntry> = emptyList(),
    val blurBrightness: Float = AdvancedMaterialFineTuning.Default.brightness,
    val blurContrast: Float = AdvancedMaterialFineTuning.Default.contrast,
    val blurSaturation: Float = AdvancedMaterialTuning.saturationForBlurRadius(
        AdvancedMaterialTuning.DefaultBlurRadius,
    ) * AdvancedMaterialFineTuning.Default.saturationMultiplier,
    val blurContentBlendMode: BlendMode = BlendMode.SrcOver,
    val navigationTintAlpha: Float = AdvancedMaterialFineTuning.Default.navigationTintAlpha,
    val topBarTintAlpha: Float = AdvancedMaterialFineTuning.Default.topBarTintAlpha,
    val fineTuning: AdvancedMaterialFineTuning = AdvancedMaterialFineTuning.Default,
) {
    val resolvedBlurRadius: Float
        get() = blurRadius.coerceIn(0f, BlurDefaults.MaxBlurRadius)

    val resolvedBlurRadiusX: Float
        get() = (blurRadiusX ?: blurRadius).coerceIn(0f, BlurDefaults.MaxBlurRadius)

    val resolvedBlurRadiusY: Float
        get() = (blurRadiusY ?: blurRadius).coerceIn(0f, BlurDefaults.MaxBlurRadius)

    val usesIndependentBlurRadii: Boolean
        get() = blurRadiusX != null || blurRadiusY != null

    val textureBlurEnabled: Boolean
        get() = enabled && blurEnabled
}

fun createAdvancedMaterialSpec(
    enabled: Boolean,
    blurRadius: Float,
    fineTuning: AdvancedMaterialFineTuning,
): AdvancedMaterialSpec {
    val tuning = fineTuning.normalized()
    return AdvancedMaterialSpec(
        enabled = enabled,
        blurRadius = AdvancedMaterialTuning.normalizeBlurRadius(blurRadius),
        noiseCoefficient = tuning.noiseCoefficient,
        blurBrightness = tuning.brightness,
        blurContrast = tuning.contrast,
        blurSaturation = AdvancedMaterialTuning.saturationForBlurRadius(blurRadius) *
            tuning.saturationMultiplier,
        navigationTintAlpha = tuning.navigationTintAlpha,
        topBarTintAlpha = tuning.topBarTintAlpha,
        fineTuning = tuning,
    )
}

@Composable
fun AdvancedMaterialSpec.rememberBlurColors(
    additionalBlendColors: List<BlendColorEntry> = emptyList(),
): BlurColors = blurColors(
    blendColors = additionalBlendColors + blurBlendColors,
    brightness = blurBrightness,
    contrast = blurContrast,
    saturation = blurSaturation,
)

fun Modifier.advancedTextureBlur(
    advancedMaterial: AdvancedMaterialSpec,
    backdrop: Backdrop,
    shape: Shape,
    colors: BlurColors,
): Modifier = if (advancedMaterial.usesIndependentBlurRadii) {
    textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadiusX = advancedMaterial.resolvedBlurRadiusX,
        blurRadiusY = advancedMaterial.resolvedBlurRadiusY,
        noiseCoefficient = advancedMaterial.noiseCoefficient,
        colors = colors,
        contentBlendMode = advancedMaterial.blurContentBlendMode,
        enabled = advancedMaterial.textureBlurEnabled,
    )
} else {
    textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = advancedMaterial.resolvedBlurRadius,
        noiseCoefficient = advancedMaterial.noiseCoefficient,
        colors = colors,
        contentBlendMode = advancedMaterial.blurContentBlendMode,
        enabled = advancedMaterial.textureBlurEnabled,
    )
}

val LocalAdvancedMaterialSpec = staticCompositionLocalOf { AdvancedMaterialSpec() }

val LocalAdvancedMaterialBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
