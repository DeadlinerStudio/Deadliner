package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning

@Immutable
data class GlassBlurSpec(
    val radiusMultiplier: Float = 1f,
    val noiseMultiplier: Float = 1f,
    val tintAlphaMultiplier: Float = 1f,
)

@Immutable
data class GlassRefractionSpec(
    val enabled: Boolean = true,
    val height: Dp = 18.dp,
    val amount: Dp = 18.dp,
    val depthEffect: Float = 0.60f,
    val chromaticAberration: Dp = 1.dp,
)

@Immutable
data class GlassEdgeOpticsSpec(
    val highlightAlpha: Float = 0.95f,
    val highlightGray: Float = 1f,
    val darkHighlightMultiplier: Float = 0.20f,
    val fallbackWidth: Dp = 0.5.dp,
    val fallbackLightAlpha: Float = 0.46f,
    val fallbackDarkAlpha: Float = 0.08f,
)

@Immutable
data class GlassGeometrySpec(
    /** Null resolves to half the rendered height, producing a capsule. */
    val cornerRadius: Dp? = null,
)

@Immutable
data class GlassLayeringSpec(
    val postRefractionBlurEnabled: Boolean = true,
    val postRefractionBlurRadius: Dp = 0.5.dp,
)

@Immutable
data class GlassMaterialRecipe(
    val blur: GlassBlurSpec = GlassBlurSpec(),
    val refraction: GlassRefractionSpec = GlassRefractionSpec(),
    val edgeOptics: GlassEdgeOpticsSpec = GlassEdgeOpticsSpec(),
    val geometry: GlassGeometrySpec = GlassGeometrySpec(),
    val layering: GlassLayeringSpec = GlassLayeringSpec(),
)

@Immutable
data class GlassMaterialInteraction(
    val refractionScale: Float = 1f,
    val highlightScale: Float = 1f,
)

/** Controls how the tint is composited over the sampled glass backdrop. */
enum class GlassTintStyle {
    /** A single neutral wash for navigation capsules and top-bar actions. */
    Neutral,

    /** Preserves chroma with colorization and a restrained screen lift for tinted controls. */
    Chromatic,

    /** Keeps a supplied seed's RGB visible while retaining transparent glass optics. */
    SeedChromatic,
}

object DeadlinerGlassRecipes {
    val FloatingNavigation = GlassMaterialRecipe(
        blur = GlassBlurSpec(
            radiusMultiplier = 0.25f,
            tintAlphaMultiplier = 0.90f,
        ),
    )

    fun floatingNavigation(
        fineTuning: AdvancedMaterialFineTuning,
    ): GlassMaterialRecipe {
        val tuning = fineTuning.normalized()
        return FloatingNavigation.copy(
            blur = FloatingNavigation.blur.copy(
                radiusMultiplier = tuning.glassBlurRadiusMultiplier,
                noiseMultiplier = tuning.glassNoiseMultiplier,
                tintAlphaMultiplier = tuning.glassTintAlphaMultiplier,
            ),
            refraction = FloatingNavigation.refraction.copy(
                enabled = tuning.refractionEnabled,
                height = tuning.refractionHeightDp.dp,
                amount = tuning.refractionAmountDp.dp,
                depthEffect = tuning.refractionDepthEffect,
                chromaticAberration = tuning.chromaticAberrationDp.dp,
            ),
            edgeOptics = FloatingNavigation.edgeOptics.copy(
                highlightAlpha = tuning.edgeHighlightAlpha,
                highlightGray = tuning.edgeHighlightGray,
                darkHighlightMultiplier = tuning.darkHighlightMultiplier,
                fallbackWidth = tuning.fallbackEdgeWidthDp.dp,
                fallbackLightAlpha = tuning.fallbackLightAlpha,
                fallbackDarkAlpha = tuning.fallbackDarkAlpha,
            ),
            layering = FloatingNavigation.layering.copy(
                postRefractionBlurEnabled = tuning.postRefractionBlurEnabled,
                postRefractionBlurRadius = tuning.postRefractionBlurRadiusDp.dp,
            ),
        )
    }
}

fun AdvancedMaterialFineTuning.softGlassTintColor(isDark: Boolean): Color {
    val tuning = normalized()
    val gray = if (isDark) tuning.glassTintDarkGray else tuning.glassTintLightGray
    return Color(red = gray, green = gray, blue = gray, alpha = 1f)
}
