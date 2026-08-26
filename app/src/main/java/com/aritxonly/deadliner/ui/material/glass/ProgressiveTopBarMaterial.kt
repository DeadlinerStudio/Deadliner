package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.effect
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect
import kotlin.math.ceil
import kotlin.math.max

val LocalImmersiveExperienceEnabled = staticCompositionLocalOf { false }
val LocalImmersiveTopBarEnabled = staticCompositionLocalOf { false }
val LocalProgressiveTopBarMaterialHosted = staticCompositionLocalOf { false }
val LocalTopBarButtonMaterialProgress = staticCompositionLocalOf { 0f }
val LocalTopBarScrollProgress = staticCompositionLocalOf<Float?> { null }

internal const val ProgressiveTopBarBlurRadiusDp = 4f
internal const val ProgressiveTopBarBlurFadeStart = 0.60f
internal const val ProgressiveTopBarTintFadeStart = 0.70f
internal const val ProgressiveTopBarTintIntensity = 0.20f
internal const val ProgressiveTopBarOverlayTopAlpha = 0.94f
internal const val ProgressiveTopBarOverlayEndFraction = 0.68f
internal const val ProgressiveTopBarContentEndFraction = 0.75f
internal const val ProgressiveTopBarMinimumContentHeightDp = 56f
internal const val ProgressiveTopBarMaterialHeightTrimDp = 6f

internal enum class ProgressiveTopBarRenderMode {
    Backdrop,
    GradientFallback,
}

internal data class ProgressiveTopBarHeightMetrics(
    val occupiedHeightPx: Int,
    val materialHeightPx: Int,
)

internal fun resolveProgressiveTopBarHeightMetrics(
    measuredTopBarHeightPx: Int,
    statusBarHeightPx: Int,
    minimumContentHeightPx: Int,
    contentEndFraction: Float = ProgressiveTopBarContentEndFraction,
    materialHeightTrimPx: Int = 0,
): ProgressiveTopBarHeightMetrics {
    if (measuredTopBarHeightPx <= 0) {
        return ProgressiveTopBarHeightMetrics(
            occupiedHeightPx = 0,
            materialHeightPx = 0,
        )
    }
    val occupiedHeight = max(
        measuredTopBarHeightPx.coerceAtLeast(0),
        statusBarHeightPx.coerceAtLeast(0) + minimumContentHeightPx.coerceAtLeast(0),
    )
    val safeContentEndFraction = contentEndFraction.coerceIn(0.01f, 1f)
    val untrimmedMaterialHeight = ceil(occupiedHeight / safeContentEndFraction).toInt()
    return ProgressiveTopBarHeightMetrics(
        occupiedHeightPx = occupiedHeight,
        materialHeightPx = (untrimmedMaterialHeight - materialHeightTrimPx.coerceAtLeast(0))
            .coerceAtLeast(occupiedHeight),
    )
}

internal fun resolveProgressiveTopBarRenderMode(
    backdropAvailable: Boolean,
    runtimeShaderSupported: Boolean,
): ProgressiveTopBarRenderMode = if (backdropAvailable && runtimeShaderSupported) {
    ProgressiveTopBarRenderMode.Backdrop
} else {
    ProgressiveTopBarRenderMode.GradientFallback
}

internal fun progressiveTopBarFadeAlpha(
    verticalFraction: Float,
    fadeStartFraction: Float,
): Float {
    val start = fadeStartFraction.coerceIn(0f, 0.9999f)
    val progress = ((verticalFraction.coerceIn(0f, 1f) - start) / (1f - start))
        .coerceIn(0f, 1f)
    val smoothProgress = progress * progress * (3f - 2f * progress)
    return 1f - smoothProgress
}

/**
 * Draws a lightweight progressive backdrop beneath the top-bar foreground. The background uses
 * one fixed Gaussian blur followed by one shader that fades both the sampled image and its tint.
 * It deliberately avoids the advanced-material and soft-glass optical stacks.
 */
@Composable
fun ProgressiveTopBarMaterial(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier, content = content)
        return
    }

    val density = LocalDensity.current
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val minimumContentHeightPx = with(density) {
        ProgressiveTopBarMinimumContentHeightDp.dp.roundToPx()
    }
    val materialHeightTrimPx = with(density) {
        ProgressiveTopBarMaterialHeightTrimDp.dp.roundToPx()
    }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val contentPlaceables = subcompose(ProgressiveTopBarSlot.Content) {
            CompositionLocalProvider(
                LocalImmersiveTopBarEnabled provides true,
                LocalProgressiveTopBarMaterialHosted provides true,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    content = content,
                )
            }
        }.map { measurable ->
            measurable.measure(constraints.copy(minHeight = 0))
        }
        val contentWidth = contentPlaceables.maxOfOrNull { it.width } ?: constraints.minWidth
        val contentHeight = contentPlaceables.maxOfOrNull { it.height } ?: constraints.minHeight
        val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val heightMetrics = resolveProgressiveTopBarHeightMetrics(
            measuredTopBarHeightPx = height,
            statusBarHeightPx = statusBarHeightPx,
            minimumContentHeightPx = minimumContentHeightPx,
            materialHeightTrimPx = materialHeightTrimPx,
        )
        val materialConstraints = Constraints.fixed(
            width = width,
            height = heightMetrics.materialHeightPx,
        )
        val backgroundPlaceables = subcompose(ProgressiveTopBarSlot.Background) {
            ProgressiveTopBarBackground(modifier = Modifier.fillMaxSize())
        }.map { measurable -> measurable.measure(materialConstraints) }

        layout(width, height) {
            backgroundPlaceables.forEach { it.placeRelative(0, 0) }
            contentPlaceables.forEach { it.placeRelative(0, 0) }
        }
    }
}

@Composable
private fun ProgressiveTopBarBackground(
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val backdrop = LocalAdvancedMaterialBackdrop.current
    val renderMode = resolveProgressiveTopBarRenderMode(
        backdropAvailable = backdrop != null,
        runtimeShaderSupported = isRuntimeShaderSupported(),
    )
    Box(modifier = modifier) {
        if (renderMode == ProgressiveTopBarRenderMode.Backdrop) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .progressiveTopBarBackdrop(
                        backdrop = requireNotNull(backdrop),
                        tint = surface,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to surface.copy(alpha = ProgressiveTopBarOverlayTopAlpha),
                            ProgressiveTopBarOverlayEndFraction to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

private enum class ProgressiveTopBarSlot {
    Background,
    Content,
}

private fun Modifier.progressiveTopBarBackdrop(
    backdrop: LayerBackdrop,
    tint: Color,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { RectangleShape },
    effects = {
        val blurRadiusPx = ProgressiveTopBarBlurRadiusDp.dp.toPx()
        effect(
            BlurEffect(
                radiusX = blurRadiusPx,
                radiusY = blurRadiusPx,
                edgeTreatment = TileMode.Clamp,
            ),
        )
        runtimeShaderEffect(
            key = ProgressiveTopBarShaderKey,
            shaderString = ProgressiveTopBarShader,
            uniformShaderName = "source",
        ) {
            setFloatUniform("size", size.width, size.height)
            setColorUniform("tint", tint)
            setFloatUniform("tint_intensity", ProgressiveTopBarTintIntensity)
            setFloatUniform("blur_fade_start", ProgressiveTopBarBlurFadeStart)
            setFloatUniform("tint_fade_start", ProgressiveTopBarTintFadeStart)
        }
    },
)

private const val ProgressiveTopBarShaderKey = "DeadlinerProgressiveTopBar"

private const val ProgressiveTopBarShader = """
    uniform shader source;
    uniform float2 size;
    layout(color) uniform half4 tint;
    uniform float tint_intensity;
    uniform float blur_fade_start;
    uniform float tint_fade_start;

    half4 main(float2 coord) {
        float normalized_y = clamp(coord.y / max(size.y, 1.0), 0.0, 1.0);
        float blur_alpha = 1.0 - smoothstep(blur_fade_start, 1.0, normalized_y);
        float tint_alpha = 1.0 - smoothstep(tint_fade_start, 1.0, normalized_y);
        half4 blurred = source.eval(coord) * half(blur_alpha);
        half4 tinted = tint * half(tint_alpha);
        return mix(blurred, tinted, half(tint_intensity));
    }
"""
