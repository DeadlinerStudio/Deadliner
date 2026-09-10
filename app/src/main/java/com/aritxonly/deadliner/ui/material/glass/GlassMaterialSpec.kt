package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning

@Immutable
data class GlassBlurSpec(
    val radiusMultiplier: Float = 1f,
    /** A modal lens may exceed Miuix texture blur's 150dp cap on the shader-backed path. */
    val maxRadius: Float = 150f,
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

    /** A wider, denser lens so transient sheets read as frosted glass above their page. */
    val BottomSheet = GlassMaterialRecipe(
        blur = GlassBlurSpec(
            // Resolves to 2x the global radius at the default 0.25 soft-glass tuning while still
            // preserving the user's custom tuning in bottomSheet(fineTuning).
            radiusMultiplier = 8f,
            maxRadius = 720f,
            tintAlphaMultiplier = 1f,
        ),
        geometry = GlassGeometrySpec(cornerRadius = 36.dp),
        layering = GlassLayeringSpec(postRefractionBlurRadius = 1.dp),
    )

    /** A compact frosted lens for dialogs, with less distortion than a bottom sheet. */
    val Dialog = GlassMaterialRecipe(
        blur = GlassBlurSpec(
            // Resolves to 1x the global radius at the default 0.25 soft-glass tuning; see
            // dialog(fineTuning).
            radiusMultiplier = 4f,
            maxRadius = 560f,
            tintAlphaMultiplier = 1f,
        ),
        geometry = GlassGeometrySpec(cornerRadius = 40.dp),
        layering = GlassLayeringSpec(postRefractionBlurRadius = 0.75.dp),
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

    fun bottomSheet(
        fineTuning: AdvancedMaterialFineTuning,
    ): GlassMaterialRecipe {
        val tuning = fineTuning.normalized()
        return BottomSheet.copy(
            blur = BottomSheet.blur.copy(
                radiusMultiplier = BottomSheet.blur.radiusMultiplier *
                    tuning.glassBlurRadiusMultiplier,
                noiseMultiplier = tuning.glassNoiseMultiplier,
                tintAlphaMultiplier = BottomSheet.blur.tintAlphaMultiplier * tuning.glassTintAlphaMultiplier,
            ),
            refraction = BottomSheet.refraction.copy(
                enabled = tuning.refractionEnabled,
                height = tuning.refractionHeightDp.dp,
                amount = tuning.refractionAmountDp.dp,
                depthEffect = tuning.refractionDepthEffect,
                chromaticAberration = tuning.chromaticAberrationDp.dp,
            ),
            edgeOptics = BottomSheet.edgeOptics.copy(
                highlightAlpha = tuning.edgeHighlightAlpha,
                highlightGray = tuning.edgeHighlightGray,
                darkHighlightMultiplier = tuning.darkHighlightMultiplier,
                fallbackWidth = tuning.fallbackEdgeWidthDp.dp,
                fallbackLightAlpha = tuning.fallbackLightAlpha,
                fallbackDarkAlpha = tuning.fallbackDarkAlpha,
            ),
            layering = BottomSheet.layering.copy(
                postRefractionBlurEnabled = tuning.postRefractionBlurEnabled,
                postRefractionBlurRadius = tuning.postRefractionBlurRadiusDp.dp,
            ),
        )
    }

    fun dialog(
        fineTuning: AdvancedMaterialFineTuning,
    ): GlassMaterialRecipe {
        val tuning = fineTuning.normalized()
        return Dialog.copy(
            blur = Dialog.blur.copy(
                radiusMultiplier = Dialog.blur.radiusMultiplier *
                    tuning.glassBlurRadiusMultiplier,
                noiseMultiplier = tuning.glassNoiseMultiplier,
                tintAlphaMultiplier = Dialog.blur.tintAlphaMultiplier * tuning.glassTintAlphaMultiplier,
            ),
            refraction = Dialog.refraction.copy(
                enabled = tuning.refractionEnabled,
                height = tuning.refractionHeightDp.dp,
                amount = tuning.refractionAmountDp.dp,
                depthEffect = tuning.refractionDepthEffect,
                chromaticAberration = tuning.chromaticAberrationDp.dp,
            ),
            edgeOptics = Dialog.edgeOptics.copy(
                highlightAlpha = tuning.edgeHighlightAlpha,
                highlightGray = tuning.edgeHighlightGray,
                darkHighlightMultiplier = tuning.darkHighlightMultiplier,
                fallbackWidth = tuning.fallbackEdgeWidthDp.dp,
                fallbackLightAlpha = tuning.fallbackLightAlpha,
                fallbackDarkAlpha = tuning.fallbackDarkAlpha,
            ),
            layering = Dialog.layering.copy(
                postRefractionBlurEnabled = tuning.postRefractionBlurEnabled,
                postRefractionBlurRadius = tuning.postRefractionBlurRadiusDp.dp,
            ),
        )
    }

    /** Uses pure Miuix advanced material for the dialog body plus a restrained gradient border. */
    fun immersiveDialogAdvancedHighlight(
        fineTuning: AdvancedMaterialFineTuning,
    ): GlassMaterialRecipe {
        val tuning = fineTuning.normalized()
        return dialog(tuning).copy(
            refraction = GlassRefractionSpec(enabled = false),
            edgeOptics = GlassEdgeOpticsSpec(
                highlightGray = tuning.edgeHighlightGray,
                fallbackWidth = tuning.fallbackEdgeWidthDp.coerceAtMost(0.5f).dp,
                fallbackLightAlpha = (tuning.fallbackLightAlpha * 0.35f).coerceIn(0f, 0.16f),
                fallbackDarkAlpha = (tuning.fallbackDarkAlpha * 1.25f).coerceIn(0f, 0.10f),
            ),
        )
    }
}

fun AdvancedMaterialFineTuning.softGlassTintColor(isDark: Boolean): Color {
    val tuning = normalized()
    val gray = if (isDark) tuning.glassTintDarkGray else tuning.glassTintLightGray
    return Color(red = gray, green = gray, blue = gray, alpha = 1f)
}
