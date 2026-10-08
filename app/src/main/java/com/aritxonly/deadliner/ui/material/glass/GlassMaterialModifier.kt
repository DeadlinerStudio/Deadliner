package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.effect
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect

@Composable
fun Modifier.deadlinerGlassMaterial(
    advancedMaterial: AdvancedMaterialSpec,
    backdrop: LayerBackdrop?,
    shape: Shape,
    tint: Color,
    tintStyle: GlassTintStyle = GlassTintStyle.Neutral,
    isDark: Boolean,
    recipe: GlassMaterialRecipe,
    interaction: GlassMaterialInteraction = GlassMaterialInteraction(),
    blurRadiusOverride: Float? = null,
    tintAlphaOverride: Float? = null,
): Modifier {
    val blurSpec = recipe.blur
    val resolvedBlurRadius = blurRadiusOverride
        ?.takeIf { it.isFinite() }
        ?.coerceIn(0f, blurSpec.maxRadius)
    val resolvedMaterial = advancedMaterial.copy(
        blurRadius = resolvedBlurRadius ?: advancedMaterial.blurRadius * blurSpec.radiusMultiplier,
        blurRadiusX = if (resolvedBlurRadius != null) {
            null
        } else {
            advancedMaterial.blurRadiusX?.times(blurSpec.radiusMultiplier)
        },
        blurRadiusY = if (resolvedBlurRadius != null) {
            null
        } else {
            advancedMaterial.blurRadiusY?.times(blurSpec.radiusMultiplier)
        },
        noiseCoefficient = (advancedMaterial.noiseCoefficient * blurSpec.noiseMultiplier)
            .coerceAtLeast(0f),
    )
    val resolvedTint = tint.copy(
        alpha = tintAlphaOverride
            ?.takeIf { it.isFinite() }
            ?.coerceIn(0f, 1f)
            ?: (tint.alpha * blurSpec.tintAlphaMultiplier).coerceIn(0f, 1f),
    )
    val tintComposite = resolveGlassTintComposite(
        tint = resolvedTint,
        style = tintStyle,
        isDark = isDark,
    )
    val canDrawBackdrop = resolvedMaterial.textureBlurEnabled && backdrop != null
    val canDrawOpticalStack = canDrawBackdrop &&
        recipe.refraction.enabled &&
        isRuntimeShaderSupported()
    val material = when {
        canDrawOpticalStack -> Modifier.glassOpticalBackdrop(
            backdrop = requireNotNull(backdrop),
            shape = shape,
            material = resolvedMaterial,
            tintComposite = tintComposite,
            recipe = recipe,
            interaction = interaction,
            isDark = isDark,
        ).opticalGlassEdge(shape, recipe.edgeOptics, isDark)

        canDrawBackdrop -> {
            val blurColors = resolvedMaterial.rememberBlurColors(
                additionalBlendColors = listOf(BlendColorEntry(tintComposite.fallbackTint)),
            )
            Modifier
                .advancedTextureBlur(
                    advancedMaterial = resolvedMaterial,
                    backdrop = requireNotNull(backdrop),
                    shape = shape,
                    colors = blurColors,
                )
                .fallbackGlassEdge(shape, recipe.edgeOptics, isDark)
        }

        else -> Modifier
            .background(color = tintComposite.fallbackTint, shape = shape)
            .fallbackGlassEdge(shape, recipe.edgeOptics, isDark)
    }

    return this.then(material)
}

internal data class GlassTintComposite(
    val baseTint: Color,
    val colorizeTint: Color,
    val liftTint: Color,
    val fallbackTint: Color,
)

/**
 * Chromatic glass keeps the sampled luminance instead of covering it with one nearly opaque wash.
 * The seed profile uses the supplied RGB directly in both color modes, with enough transparent base
 * coverage to remain recognizable without sacrificing backdrop blur and refraction.
 */
internal fun resolveGlassTintComposite(
    tint: Color,
    style: GlassTintStyle,
    isDark: Boolean,
): GlassTintComposite {
    if (style == GlassTintStyle.Neutral) {
        return GlassTintComposite(
            baseTint = tint,
            colorizeTint = Color.Transparent,
            liftTint = Color.Transparent,
            fallbackTint = tint,
        )
    }

    val seedFaithful = style == GlassTintStyle.SeedChromatic
    val average = (tint.red + tint.green + tint.blue) / 3f
    val saturationScale = when {
        seedFaithful -> 1f
        isDark -> 1.24f
        else -> 1.16f
    }
    val brightnessLift = when {
        seedFaithful -> 0f
        isDark -> 0.08f
        else -> 0.035f
    }
    fun resolveChannel(channel: Float): Float {
        val saturated = average + (channel - average) * saturationScale
        return (saturated + (1f - saturated) * brightnessLift).coerceIn(0f, 1f)
    }

    val vividTint = Color(
        red = resolveChannel(tint.red),
        green = resolveChannel(tint.green),
        blue = resolveChannel(tint.blue),
        alpha = 1f,
    )
    val baseAlpha = tint.alpha * when {
        seedFaithful -> 0.835f
        isDark -> 0.42f
        else -> 0.34f
    }
    val colorizeAlpha = tint.alpha * when {
        seedFaithful -> 0.86f
        isDark -> 0.58f
        else -> 0.50f
    }
    val liftAlpha = tint.alpha * when {
        seedFaithful -> 0f
        isDark -> 0.28f
        else -> 0.10f
    }
    val fallbackAlpha = tint.alpha * when {
        seedFaithful -> 0.96f
        isDark -> 0.68f
        else -> 0.62f
    }
    return GlassTintComposite(
        baseTint = vividTint.copy(alpha = baseAlpha.coerceIn(0f, 1f)),
        colorizeTint = vividTint.copy(alpha = colorizeAlpha.coerceIn(0f, 1f)),
        liftTint = vividTint.copy(alpha = liftAlpha.coerceIn(0f, 1f)),
        fallbackTint = vividTint.copy(alpha = fallbackAlpha.coerceIn(0f, 1f)),
    )
}

/**
 * A single backdrop effect chain keeps the Gaussian blur inside the refractive lens:
 * color treatment -> full material blur -> rounded-rectangle lens -> gentle softening.
 */
private fun Modifier.glassOpticalBackdrop(
    backdrop: LayerBackdrop,
    shape: Shape,
    material: AdvancedMaterialSpec,
    tintComposite: GlassTintComposite,
    recipe: GlassMaterialRecipe,
    interaction: GlassMaterialInteraction,
    isDark: Boolean,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        val maxBlurRadius = recipe.blur.maxRadius.coerceAtLeast(0f)
        val blurRadiusX = (material.blurRadiusX ?: material.blurRadius)
            .coerceIn(0f, maxBlurRadius)
        val blurRadiusY = (material.blurRadiusY ?: material.blurRadius)
            .coerceIn(0f, maxBlurRadius)
        val refraction = recipe.refraction
        val postBlurRadiusPx = if (recipe.layering.postRefractionBlurEnabled) {
            recipe.layering.postRefractionBlurRadius.toPx().coerceAtLeast(0f)
        } else {
            0f
        }
        val effectPaddingPx = postBlurRadiusPx * 2f
        padding = effectPaddingPx

        colorControls(
            brightness = material.blurBrightness,
            contrast = material.blurContrast,
            saturation = material.blurSaturation,
        )
        if (blurRadiusX > 0f || blurRadiusY > 0f) {
            effect(
                BlurEffect(
                    radiusX = blurRadiusX,
                    radiusY = blurRadiusY,
                    edgeTreatment = TileMode.Clamp,
                ),
            )
        }
        runtimeShaderEffect(
            key = GlassRefractionShaderKey,
            shaderString = GlassRefractionShader,
            uniformShaderName = "source",
        ) {
            val highlight = recipe.edgeOptics.highlightAlpha * EDGE_HIGHLIGHT_SCALE *
                if (isDark) recipe.edgeOptics.darkHighlightMultiplier else 1f
            setFloatUniform("content_origin", effectPaddingPx, effectPaddingPx)
            setFloatUniform("content_size", size.width, size.height)
            setFloatUniform(
                "corner_radius",
                recipe.geometry.cornerRadius?.toPx() ?: (size.height / 2f),
            )
            setFloatUniform("refraction_height", refraction.height.toPx())
            setFloatUniform(
                "refraction_amount",
                refraction.amount.toPx() * interaction.refractionScale.coerceAtLeast(0f),
            )
            setFloatUniform("depth_effect", refraction.depthEffect.coerceIn(0f, 1f))
            setFloatUniform("chromatic_aberration", refraction.chromaticAberration.toPx())
            setFloatUniform("noise_coefficient", material.noiseCoefficient.coerceAtLeast(0f))
            setFloatUniform(
                "highlight_alpha",
                (highlight * interaction.highlightScale.coerceAtLeast(0f)).coerceIn(0f, 1f),
            )
            setFloatUniform(
                "highlight_gray",
                recipe.edgeOptics.highlightGray.coerceIn(0f, 1f),
            )
        }
        if (postBlurRadiusPx > 0f) {
            effect(
                BlurEffect(
                    radiusX = postBlurRadiusPx,
                    radiusY = postBlurRadiusPx,
                    edgeTreatment = TileMode.Clamp,
                ),
            )
        }
    },
    onDrawSurface = {
        drawRect(tintComposite.baseTint)
        if (tintComposite.colorizeTint.alpha > 0f) {
            drawRect(
                color = tintComposite.colorizeTint,
                blendMode = BlendMode.Color,
            )
        }
        if (tintComposite.liftTint.alpha > 0f) {
            drawRect(
                color = tintComposite.liftTint,
                blendMode = BlendMode.Screen,
            )
        }
    },
    contentBlendMode = material.blurContentBlendMode,
)

private fun Modifier.fallbackGlassEdge(
    shape: Shape,
    spec: GlassEdgeOpticsSpec,
    isDark: Boolean,
): Modifier {
    val baseAlpha = (if (isDark) spec.fallbackDarkAlpha else spec.fallbackLightAlpha) *
        FALLBACK_EDGE_ALPHA_SCALE
    val edgeColor = grayscaleColor(spec.highlightGray)
    val edgeBrush = Brush.verticalGradient(
        colors = listOf(
            edgeColor.copy(alpha = baseAlpha),
            edgeColor.copy(alpha = baseAlpha * 0.66f),
            edgeColor.copy(alpha = baseAlpha * 0.42f),
        ),
    )
    return border(
        width = spec.fallbackWidth * EDGE_WIDTH_SCALE,
        brush = edgeBrush,
        shape = shape,
    )
}

private fun Modifier.opticalGlassEdge(
    shape: Shape,
    spec: GlassEdgeOpticsSpec,
    isDark: Boolean,
): Modifier {
    val darkMultiplier = if (isDark) spec.darkHighlightMultiplier else 1f
    val alpha = (spec.highlightAlpha * EDGE_HIGHLIGHT_SCALE * darkMultiplier).coerceIn(0f, 1f)
    val edgeColor = grayscaleColor(spec.highlightGray)
    val edgeBrush = Brush.verticalGradient(
        colors = listOf(
            edgeColor.copy(alpha = alpha),
            edgeColor.copy(alpha = alpha * 0.62f),
            edgeColor.copy(alpha = alpha * 0.30f),
        ),
    )
    return border(
        width = spec.fallbackWidth * EDGE_WIDTH_SCALE,
        brush = edgeBrush,
        shape = shape,
    )
}

private fun grayscaleColor(value: Float): Color {
    val gray = value.coerceIn(0f, 1f)
    return Color(red = gray, green = gray, blue = gray, alpha = 1f)
}

private const val EDGE_HIGHLIGHT_SCALE = 0.85f
private const val EDGE_WIDTH_SCALE = 1f
private const val FALLBACK_EDGE_ALPHA_SCALE = 0.45f
