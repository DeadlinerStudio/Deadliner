package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.base.edgeFade
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.shapes.SmoothRoundedCornerShape

val LocalImmersiveExperienceEnabled = staticCompositionLocalOf { false }
val LocalImmersiveTopBarEnabled = staticCompositionLocalOf { false }
val LocalTopBarButtonMaterialProgress = staticCompositionLocalOf { 0f }

private val ProgressiveTopBarShape = SmoothRoundedCornerShape(0.dp)

/**
 * Draws only the top-bar material through the fade mask. Foreground content is rendered in a
 * separate layer and therefore remains fully opaque.
 */
@Composable
fun ProgressiveTopBarMaterial(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = LocalAdvancedMaterialBackdrop.current,
    fadeHeight: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier, content = content)
        return
    }

    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val surfaceTint = MaterialTheme.colorScheme.surface.copy(
        alpha = advancedMaterial.topBarTintAlpha,
    )
    val canBlur = advancedMaterial.enabled && backdrop != null
    Box(modifier = modifier) {
        val backgroundModifier = if (canBlur) {
            val blurColors = advancedMaterial.rememberBlurColors(
                listOf(BlendColorEntry(surfaceTint)),
            )
            Modifier
                .matchParentSize()
                .edgeFade(top = 0.dp, bottom = fadeHeight)
                .advancedTextureBlur(
                    advancedMaterial = advancedMaterial,
                    backdrop = requireNotNull(backdrop),
                    shape = ProgressiveTopBarShape,
                    colors = blurColors,
                )
        } else {
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to surfaceTint.copy(alpha = 0.96f),
                            0.64f to surfaceTint.copy(alpha = 0.72f),
                            1f to Color.Transparent,
                        ),
                    ),
                )
        }
        Box(modifier = backgroundModifier)
        CompositionLocalProvider(LocalImmersiveTopBarEnabled provides true) {
            content()
        }
    }
}
