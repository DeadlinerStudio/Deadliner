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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import kotlin.math.ceil
import kotlin.math.max

val LocalImmersiveExperienceEnabled = staticCompositionLocalOf { false }
val LocalImmersiveTopBarEnabled = staticCompositionLocalOf { false }
val LocalProgressiveTopBarMaterialHosted = staticCompositionLocalOf { false }
val LocalTopBarButtonMaterialProgress = staticCompositionLocalOf { 0f }
val LocalTopBarScrollProgress = staticCompositionLocalOf<Float?> { null }

internal const val ProgressiveTopBarBlurRadiusDp = 4f
internal const val ProgressiveTopBarBlurFadeStart = 0.60f
internal const val ProgressiveTopBarOverlayTopAlpha = 0.94f
internal const val ProgressiveTopBarOverlayEndFraction = 0.80f
internal const val ProgressiveTopBarContentEndFraction = 0.75f
internal const val ProgressiveTopBarMinimumContentHeightDp = 56f
internal const val ProgressiveTopBarMaterialHeightTrimDp = 6f

/** Extra visual clearance below the fully transparent end of the progressive material. */
internal const val ProgressiveTopBarBottomClearanceDp = 12f

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

/**
 * Draws a Miuix progressive backdrop beneath the top-bar foreground. When advanced material is
 * disabled, the existing transparent gradient remains as the intentional lightweight fallback.
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
        (ProgressiveTopBarMaterialHeightTrimDp + ProgressiveTopBarBottomClearanceDp)
            .dp
            .roundToPx()
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
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val backdrop = LocalAdvancedMaterialBackdrop.current
    Box(modifier = modifier) {
        if (advancedMaterial.enabled && backdrop != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .progressiveTopBarBackdrop(
                        backdrop = requireNotNull(backdrop),
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
): Modifier = progressiveTextureBlur(
    backdrop = backdrop,
    shape = RectangleShape,
    blurRadius = ProgressiveTopBarBlurRadiusDp,
    gradient = ProgressiveBlur.Top.copy(
        startFraction = ProgressiveTopBarBlurFadeStart,
    ),
)
