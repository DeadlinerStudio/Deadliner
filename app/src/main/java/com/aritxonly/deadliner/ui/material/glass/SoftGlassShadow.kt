package com.aritxonly.deadliner.ui.material.glass

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
} else {
    SoftGlassShadowTokens(
        ambientRadius = if (useOfficialMiuixGlass) 24.dp else if (profile == SoftGlassShadowProfile.TopBarControl) 32.dp else 20.dp,
        ambientSpread = if (useOfficialMiuixGlass) (-1).dp else (-2).dp,
        ambientAlpha = if (useOfficialMiuixGlass) 0.025f else if (profile == SoftGlassShadowProfile.TopBarControl) 0.028f else 0.018f,
        outerRadius = if (useOfficialMiuixGlass) 12.dp else 10.dp,
        outerSpread = if (useOfficialMiuixGlass) (-0.5).dp else (-1).dp,
        outerAlpha = if (useOfficialMiuixGlass) 0.050f else 0.040f,
        edgeRadius = if (useOfficialMiuixGlass) 4.5.dp else 3.dp,
        edgeSpread = (-0.5).dp,
        edgeAlpha = if (useOfficialMiuixGlass) 0.012f else 0.006f,
    )
}

@Composable
internal fun Modifier.softGlassShadow(
    shape: Shape,
    isDark: Boolean,
    alpha: Float = 1f,
    profile: SoftGlassShadowProfile = SoftGlassShadowProfile.Standard,
): Modifier {
    val official = LocalAdvancedMaterialSpec.current.useOfficialMiuixGlass
    val host = LocalTopBarGlassShadowHost.current
    // A Dialog may inherit business locals from the bar but owns a different Compose View.
    if (host != null && host.owner === LocalView.current) {
        return registerTopBarGlassShadow(host, shape, isDark, alpha, official, profile)
    }
    return drawSoftGlassShadow(shape, isDark, alpha, official, profile)
}

internal fun Modifier.drawSoftGlassShadow(
    shape: Shape,
    isDark: Boolean,
    alpha: Float,
    useOfficialMiuixGlass: Boolean,
    profile: SoftGlassShadowProfile = SoftGlassShadowProfile.Standard,
): Modifier {
    val tokens = softGlassShadowTokens(isDark, useOfficialMiuixGlass, profile)
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
