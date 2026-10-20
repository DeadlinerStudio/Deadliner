package com.aritxonly.deadliner.ui.material.glass

import com.aritxonly.deadliner.model.GlassShadowPreset
import androidx.compose.runtime.Composable
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

internal enum class SoftGlassShadowProfile { Standard, TopBarControl }

internal data class SoftGlassShadowTokens(
    val ambientRadius: Dp,
    val ambientSpread: Dp,
    val ambientAlpha: Float,
    val outerRadius: Dp,
    val outerSpread: Dp,
    val outerAlpha: Float,
    val edgeRadius: Dp,
    val edgeSpread: Dp,
    val edgeAlpha: Float,
    val offset: DpOffset = DpOffset.Zero,
)

internal fun softGlassShadowTokens(
    isDark: Boolean,
    useOfficialMiuixGlass: Boolean = false,
    profile: SoftGlassShadowProfile = SoftGlassShadowProfile.Standard,
    preset: GlassShadowPreset = GlassShadowPreset.Default,
): SoftGlassShadowTokens = if (isDark) {
    SoftGlassShadowTokens(
        ambientRadius = 22.dp,
        ambientSpread = (-1).dp,
        ambientAlpha = 0.020f,
        outerRadius = 11.dp,
        outerSpread = (-0.5).dp,
        outerAlpha = 0.038f,
        edgeRadius = 4.dp,
        edgeSpread = (-0.5).dp,
        edgeAlpha = 0.008f,
    )
} else if (!useOfficialMiuixGlass) {
    selfLightShadowTokens(preset, profile)
} else {
    SoftGlassShadowTokens(
        ambientRadius = 24.dp, ambientSpread = (-1).dp, ambientAlpha = 0.025f,
        outerRadius = 12.dp, outerSpread = (-0.5).dp, outerAlpha = 0.050f,
        edgeRadius = 4.5.dp, edgeSpread = (-0.5).dp, edgeAlpha = 0.012f,
    )
}

/** Three bounded lobes keep the experiment at the same rendering cost as the existing shadow. */
private fun selfLightShadowTokens(
    preset: GlassShadowPreset,
    profile: SoftGlassShadowProfile,
): SoftGlassShadowTokens {
    val top = profile == SoftGlassShadowProfile.TopBarControl
    fun recipe(ambient: Int, ambientAlpha: Float, outer: Float, outerAlpha: Float,
        edge: Float, edgeAlpha: Float, topAmbient: Int, topAlpha: Float,
        y: Float = 0f) = SoftGlassShadowTokens(
        ambientRadius = (if (top) topAmbient else ambient).dp,
        ambientSpread = (-2).dp,
        ambientAlpha = if (top) topAlpha else ambientAlpha,
        outerRadius = outer.dp, outerSpread = (-1).dp, outerAlpha = outerAlpha,
        edgeRadius = edge.dp, edgeSpread = (-0.5).dp, edgeAlpha = edgeAlpha,
        offset = DpOffset(0.dp, y.dp),
    )
    return when (preset) {
        // User-approved production baseline (00): wide diffusion with a restrained contact edge.
        GlassShadowPreset.BalancedDiffuse -> recipe(26, .03459375f, 10f, .06490625f, 2f, .0094375f, 34, .043453125f)
        GlassShadowPreset.Clean -> recipe(18, .016f, 7f, .058f, 1.8f, .008f, 24, .021f)
        GlassShadowPreset.Original -> recipe(20, .018f, 10f, .040f, 3f, .006f, 32, .028f)
        GlassShadowPreset.Diffuse -> recipe(20, .036f, 10f, .068f, 3f, .010f, 32, .045f)
        GlassShadowPreset.Compact -> recipe(12, .018f, 5f, .062f, 1.5f, .010f, 16, .024f)
        GlassShadowPreset.Crisp -> recipe(16, .012f, 6f, .070f, 1f, .016f, 20, .016f)
        GlassShadowPreset.Airy -> recipe(26, .022f, 9f, .042f, 2f, .006f, 34, .030f)
        GlassShadowPreset.CloseFloat -> recipe(16, .020f, 7f, .070f, 2f, .010f, 22, .026f, 1f)
        GlassShadowPreset.Elevated -> recipe(22, .020f, 10f, .052f, 2.5f, .008f, 28, .026f, 2f)
        GlassShadowPreset.SoftContact -> recipe(16, .012f, 5f, .040f, 2f, .020f, 22, .016f)
        GlassShadowPreset.Minimal -> recipe(12, .006f, 5f, .025f, 1f, .006f, 16, .009f)
    }
}

@Composable
internal fun Modifier.softGlassShadow(
    shape: Shape,
    isDark: Boolean,
    alpha: Float = 1f,
    profile: SoftGlassShadowProfile = SoftGlassShadowProfile.Standard,
): Modifier {
    val material = LocalAdvancedMaterialSpec.current
    val official = material.useOfficialMiuixGlass
    val host = LocalTopBarGlassShadowHost.current
    // A Dialog may inherit business locals from the bar but owns a different Compose View.
    if (host != null && host.owner === LocalView.current) {
        return registerTopBarGlassShadow(host, shape, isDark, alpha, official, profile, material.shadowPreset)
    }
    return drawSoftGlassShadow(shape, isDark, alpha, official, profile, material.shadowPreset)
}

internal fun Modifier.drawSoftGlassShadow(
    shape: Shape,
    isDark: Boolean,
    alpha: Float,
    useOfficialMiuixGlass: Boolean,
    profile: SoftGlassShadowProfile = SoftGlassShadowProfile.Standard,
    preset: GlassShadowPreset = GlassShadowPreset.Default,
): Modifier {
    val tokens = softGlassShadowTokens(isDark, useOfficialMiuixGlass, profile, preset)
    val resolvedAlpha = alpha.coerceIn(0f, 1f)
    return dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = tokens.ambientRadius,
            spread = tokens.ambientSpread,
            offset = tokens.offset,
            color = Color.Black,
            alpha = tokens.ambientAlpha * resolvedAlpha,
        ),
    ).dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = tokens.outerRadius,
            spread = tokens.outerSpread,
            offset = tokens.offset,
            color = Color.Black,
            alpha = tokens.outerAlpha * resolvedAlpha,
        ),
    ).dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = tokens.edgeRadius,
            spread = tokens.edgeSpread,
            offset = tokens.offset,
            color = Color.Black,
            alpha = tokens.edgeAlpha * resolvedAlpha,
        ),
    )
}
