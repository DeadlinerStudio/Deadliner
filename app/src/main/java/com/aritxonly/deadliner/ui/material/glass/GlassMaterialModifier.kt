package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
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
    isDark: Boolean,
    recipe: GlassMaterialRecipe,
    interaction: GlassMaterialInteraction = GlassMaterialInteraction(),
): Modifier {
    val blurSpec = recipe.blur
    val resolvedMaterial = advancedMaterial.copy(
        blurRadius = advancedMaterial.blurRadius * blurSpec.radiusMultiplier,
        blurRadiusX = advancedMaterial.blurRadiusX?.times(blurSpec.radiusMultiplier),
        blurRadiusY = advancedMaterial.blurRadiusY?.times(blurSpec.radiusMultiplier),
        noiseCoefficient = (advancedMaterial.noiseCoefficient * blurSpec.noiseMultiplier)
            .coerceAtLeast(0f),
    )
    val resolvedTint = tint.copy(
        alpha = (tint.alpha * blurSpec.tintAlphaMultiplier).coerceIn(0f, 1f),
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
            tint = resolvedTint,
            recipe = recipe,
            interaction = interaction,
            isDark = isDark,
        ).opticalGlassEdge(shape, recipe.edgeOptics, isDark)

        canDrawBackdrop -> {
            val blurColors = resolvedMaterial.rememberBlurColors(
                additionalBlendColors = listOf(BlendColorEntry(resolvedTint)),
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
            .background(color = resolvedTint, shape = shape)
            .fallbackGlassEdge(shape, recipe.edgeOptics, isDark)
    }

    return this.then(material)
}

/**
 * A single backdrop effect chain keeps the Gaussian blur inside the refractive lens:
 * color treatment -> full material blur -> rounded-rectangle lens -> gentle softening.
 */
private fun Modifier.glassOpticalBackdrop(
    backdrop: LayerBackdrop,
    shape: Shape,
    material: AdvancedMaterialSpec,
    tint: Color,
    recipe: GlassMaterialRecipe,
    interaction: GlassMaterialInteraction,
    isDark: Boolean,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
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
        if (material.resolvedBlurRadiusX > 0f || material.resolvedBlurRadiusY > 0f) {
            effect(
                BlurEffect(
                    radiusX = material.resolvedBlurRadiusX,
                    radiusY = material.resolvedBlurRadiusY,
                    edgeTreatment = TileMode.Clamp,
                ),
            )
        }
        runtimeShaderEffect(
            key = GlassRefractionShaderKey,
            shaderString = GlassRefractionShader,
            uniformShaderName = "source",
        ) {
            val highlight = recipe.edgeOptics.highlightAlpha *
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
    onDrawSurface = { drawRect(tint) },
    contentBlendMode = material.blurContentBlendMode,
)

private fun Modifier.fallbackGlassEdge(
    shape: Shape,
    spec: GlassEdgeOpticsSpec,
    isDark: Boolean,
): Modifier {
    val baseAlpha = if (isDark) spec.fallbackDarkAlpha else spec.fallbackLightAlpha
    val edgeColor = grayscaleColor(spec.highlightGray)
    val edgeBrush = Brush.verticalGradient(
        colors = listOf(
            edgeColor.copy(alpha = baseAlpha),
            edgeColor.copy(alpha = baseAlpha * 0.66f),
            edgeColor.copy(alpha = baseAlpha * 0.42f),
        ),
    )
    return border(width = spec.fallbackWidth, brush = edgeBrush, shape = shape)
}

private fun Modifier.opticalGlassEdge(
    shape: Shape,
    spec: GlassEdgeOpticsSpec,
    isDark: Boolean,
): Modifier {
    val darkMultiplier = if (isDark) spec.darkHighlightMultiplier else 1f
    val alpha = (spec.highlightAlpha * darkMultiplier).coerceIn(0f, 1f)
    val edgeColor = grayscaleColor(spec.highlightGray)
    val edgeBrush = Brush.verticalGradient(
        colors = listOf(
            edgeColor.copy(alpha = alpha),
            edgeColor.copy(alpha = alpha * 0.62f),
            edgeColor.copy(alpha = alpha * 0.30f),
        ),
    )
    return border(width = 0.5.dp, brush = edgeBrush, shape = shape)
}

private fun grayscaleColor(value: Float): Color {
    val gray = value.coerceIn(0f, 1f)
    return Color(red = gray, green = gray, blue = gray, alpha = 1f)
}
