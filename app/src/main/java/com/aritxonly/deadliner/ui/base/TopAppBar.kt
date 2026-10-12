package com.aritxonly.deadliner.ui.base

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.material.glass.LocalProgressiveTopBarMaterialHosted
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarButtonMaterialProgress
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarScrollProgress
import com.aritxonly.deadliner.ui.material.glass.ProgressiveTopBarMaterial
import com.aritxonly.deadliner.ui.material.glass.SoftGlassDefaults
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import com.aritxonly.deadliner.ui.material.glass.softGlassShadow
import com.aritxonly.deadliner.ui.material.glass.softGlassPressHighlightAlpha
import com.aritxonly.deadliner.ui.material.glass.softGlassPressMaterialInteraction
import com.aritxonly.deadliner.ui.material.glass.softGlassPressScale
import top.yukonga.miuix.kmp.basic.ScrollBehavior as MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

enum class TopAppBarStyle {
    CENTER, LARGE, SMALL
}


@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun TopAppBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    mode: TopAppBarStyle = TopAppBarStyle.CENTER,
    isMainTitle: Boolean = false,
    titleTextStyle: TextStyle? = null,
    forceMaterial3: Boolean = false,
    buttonMaterialRevealProgress: Float? = null,
    material3ScrollBehavior: TopAppBarScrollBehavior? = null,
    miuixScrollBehavior: MiuixScrollBehavior? = null,
) {
    val progressiveMaterialHosted = LocalProgressiveTopBarMaterialHosted.current
    val collapsedFraction = miuixScrollBehavior?.state?.collapsedFraction
    val buttonMaterialTarget = resolveTopBarButtonMaterialProgress(
        collapsedFraction = collapsedFraction,
        scrollProgress = LocalTopBarScrollProgress.current,
        overrideProgress = buttonMaterialRevealProgress,
    )
    val buttonMaterialProgress by animateFloatAsState(
        targetValue = buttonMaterialTarget,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "top-bar-button-material",
    )
    val resolvedNavigationIcon: @Composable () -> Unit = {
        navigationIcon?.let { icon ->
            if (!isMainTitle) {
                ImmersiveNavigationIcon(
                    materialAlpha = buttonMaterialProgress,
                    content = icon,
                )
            } else {
                icon()
            }
        }
    }

    val platformTopBar = @Composable {
        when (mode) {
            TopAppBarStyle.LARGE -> MiuixTopAppBar(
                title = title, largeTitle = title, color = Color.Transparent,
                navigationIcon = resolvedNavigationIcon, actions = actions,
                titleColor = MaterialTheme.colorScheme.onSurface,
                largeTitleColor = MaterialTheme.colorScheme.onSurface,
                scrollBehavior = miuixScrollBehavior,
            )
            else -> SmallTopAppBar(
                title = title, color = Color.Transparent,
                navigationIcon = resolvedNavigationIcon, actions = actions,
                titleColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    CompositionLocalProvider(
        LocalTopBarButtonMaterialProgress provides buttonMaterialProgress,
    ) {
        if (!progressiveMaterialHosted) {
            ProgressiveTopBarMaterial(
                modifier = Modifier.fillMaxWidth(),
            ) {
                platformTopBar()
            }
        } else {
            platformTopBar()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ImmersiveNavigationIcon(
    materialAlpha: Float,
    content: @Composable () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "immersive-navigation-press",
    )
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = softGlassPressHighlightAlpha(pressProgress, isDark),
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    val scale = softGlassPressScale(pressProgress)
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center,
        ) {
            SoftGlassSurface(
                modifier = Modifier
                    .matchParentSize()
                    .softGlassShadow(
                        shape = SoftGlassDefaults.Shape,
                        isDark = isDark,
                        alpha = materialAlpha,
                    ),
                materialAlpha = materialAlpha,
                interaction = softGlassPressMaterialInteraction(pressProgress),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(highlightColor),
                )
            }
            CompositionLocalProvider(
                LocalRippleConfiguration provides null,
                LocalTopBarButtonMaterialHosted provides true,
            ) {
                content()
            }
        }
    }
}
