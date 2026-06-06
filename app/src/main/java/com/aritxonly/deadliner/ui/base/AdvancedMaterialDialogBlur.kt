package com.aritxonly.deadliner.ui.base

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec

@Stable
class AdvancedMaterialDialogBlurHostState {
    var activeDialogs by mutableIntStateOf(0)
        private set

    fun register() {
        activeDialogs += 1
    }

    fun unregister() {
        activeDialogs = (activeDialogs - 1).coerceAtLeast(0)
    }
}

val LocalAdvancedMaterialDialogBlurHostState =
    compositionLocalOf<AdvancedMaterialDialogBlurHostState?> { null }

@Composable
fun rememberAdvancedMaterialDialogBlurHostState(): AdvancedMaterialDialogBlurHostState {
    return remember { AdvancedMaterialDialogBlurHostState() }
}

@Composable
fun ProvideAdvancedMaterialDialogBlurHost(
    content: @Composable () -> Unit
) {
    val state = rememberAdvancedMaterialDialogBlurHostState()
    CompositionLocalProvider(LocalAdvancedMaterialDialogBlurHostState provides state) {
        content()
    }
}

@Composable
fun RegisterAdvancedMaterialDialogBlur(show: Boolean = true) {
    val state = LocalAdvancedMaterialDialogBlurHostState.current
    DisposableEffect(state, show) {
        if (state != null && show) {
            state.register()
            onDispose { state.unregister() }
        } else {
            onDispose { }
        }
    }
}

fun Modifier.graphicsLayerBlurEffect(
    active: Boolean,
    activeScale: Float = 0.98f,
    maxBlurRadius: Float = 24f,
    minSaturation: Float = 0.5f,
): Modifier = composed {
    val blurProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "dialog-blur-progress",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) activeScale else 1f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "dialog-blur-scale",
    )
    val blurRadius = (maxBlurRadius * blurProgress).coerceIn(0f, maxBlurRadius)
    val saturation = 1f - ((1f - minSaturation) * blurProgress)
    val blurEps = 0.5f

    graphicsLayer {
        val effects = mutableListOf<RenderEffect>()

        if (blurRadius >= blurEps) {
            effects += RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                Shader.TileMode.CLAMP
            )
        }
        if (saturation < 1f - 1e-3f) {
            val cm = ColorMatrix().apply { setSaturation(saturation) }
            effects += RenderEffect.createColorFilterEffect(
                ColorMatrixColorFilter(cm)
            )
        }

        renderEffect = when (effects.size) {
            0 -> null
            1 -> effects[0].asComposeRenderEffect()
            else -> RenderEffect.createChainEffect(
                effects[0],
                effects[1]
            ).asComposeRenderEffect()
        }

        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.advancedMaterialDialogBlurHost(
    active: Boolean = false,
    forceActive: Boolean = false,
    activeScale: Float = 0.98f,
    maxBlurRadius: Float = 24f,
    minSaturation: Float = 0.5f,
): Modifier = composed {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val hostState = LocalAdvancedMaterialDialogBlurHostState.current
    val effectiveActive = forceActive || (
        advancedMaterial.enabled && (active || (hostState?.activeDialogs ?: 0) > 0)
    )

    graphicsLayerBlurEffect(
        active = effectiveActive,
        activeScale = activeScale,
        maxBlurRadius = maxBlurRadius,
        minSaturation = minSaturation,
    )
}
